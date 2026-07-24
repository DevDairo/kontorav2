package com.kontora.pos.ventas.service;

import com.kontora.pos.auditoria.service.AuditoriaService;
import com.kontora.pos.caja.domain.CajaDiaria;
import com.kontora.pos.caja.repository.CajaDiariaRepository;
import com.kontora.pos.catalogos.domain.MetodoPago;
import com.kontora.pos.catalogos.domain.PrecioGranizado;
import com.kontora.pos.catalogos.domain.Promocion;
import com.kontora.pos.catalogos.repository.MetodoPagoRepository;
import com.kontora.pos.catalogos.repository.PrecioGranizadoRepository;
import com.kontora.pos.catalogos.repository.PromocionRepository;
import com.kontora.pos.common.exception.ApiException;
import com.kontora.pos.common.security.PrincipalUsuario;
import com.kontora.pos.inventario.service.InventarioService;
import com.kontora.pos.usuarios.domain.Usuario;
import com.kontora.pos.usuarios.repository.UsuarioRepository;
import com.kontora.pos.ventas.domain.DetalleVenta;
import com.kontora.pos.ventas.domain.PagoVenta;
import com.kontora.pos.ventas.domain.Venta;
import com.kontora.pos.ventas.dto.AnularVentaRequest;
import com.kontora.pos.ventas.dto.DetalleVentaResponse;
import com.kontora.pos.ventas.dto.PagoVentaResponse;
import com.kontora.pos.ventas.dto.RegistrarDetalleVentaRequest;
import com.kontora.pos.ventas.dto.RegistrarPagoVentaRequest;
import com.kontora.pos.ventas.dto.RegistrarVentaRequest;
import com.kontora.pos.ventas.dto.TrabajadorVentaResponse;
import com.kontora.pos.ventas.dto.VentaResponse;
import com.kontora.pos.ventas.repository.DetalleVentaRepository;
import com.kontora.pos.ventas.repository.PagoVentaRepository;
import com.kontora.pos.ventas.repository.VentaRepository;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static com.kontora.pos.common.audit.AuditoriaValores.valores;

@Service
public class VentasService {

    private static final String ESTADO_CAJA_ABIERTA = "abierta";
    private static final String ESTADO_VENTA_REGISTRADA = "registrada";
    private static final String ESTADO_VENTA_ANULADA = "anulada";
    private static final String METODO_EFECTIVO = "efectivo";
    private static final String METODO_TRANSFERENCIA = "transferencia";
    private static final String TIPO_CLIENTE = "cliente";
    private static final String TIPO_TRABAJADOR = "trabajador";
    private static final String ESTADO_USUARIO_ACTIVO = "activo";
    private static final String ROL_ADMINISTRADOR = "administrador";
    private static final String ROL_GERENTE = "gerente";

    private final CajaDiariaRepository cajaDiariaRepository;
    private final UsuarioRepository usuarioRepository;
    private final MetodoPagoRepository metodoPagoRepository;
    private final PrecioGranizadoRepository precioGranizadoRepository;
    private final PromocionRepository promocionRepository;
    private final VentaRepository ventaRepository;
    private final DetalleVentaRepository detalleVentaRepository;
    private final PagoVentaRepository pagoVentaRepository;
    private final InventarioService inventarioService;
    private final EntityManager entityManager;
    private final AuditoriaService auditoriaService;

    public VentasService(
            CajaDiariaRepository cajaDiariaRepository,
            UsuarioRepository usuarioRepository,
            MetodoPagoRepository metodoPagoRepository,
            PrecioGranizadoRepository precioGranizadoRepository,
            PromocionRepository promocionRepository,
            VentaRepository ventaRepository,
            DetalleVentaRepository detalleVentaRepository,
            PagoVentaRepository pagoVentaRepository,
            InventarioService inventarioService,
            EntityManager entityManager,
            AuditoriaService auditoriaService) {
        this.cajaDiariaRepository = cajaDiariaRepository;
        this.usuarioRepository = usuarioRepository;
        this.metodoPagoRepository = metodoPagoRepository;
        this.precioGranizadoRepository = precioGranizadoRepository;
        this.promocionRepository = promocionRepository;
        this.ventaRepository = ventaRepository;
        this.detalleVentaRepository = detalleVentaRepository;
        this.pagoVentaRepository = pagoVentaRepository;
        this.inventarioService = inventarioService;
        this.entityManager = entityManager;
        this.auditoriaService = auditoriaService;
    }

    @Transactional
    public VentaResponse registrarVenta(RegistrarVentaRequest request, PrincipalUsuario principalUsuario) {
        LocalDate fechaOperacion = LocalDate.now();
        CajaDiaria cajaDiaria = obtenerCajaAbierta();
        Usuario usuarioVendedor = obtenerUsuario(principalUsuario.idUsuario(), "Usuario vendedor no encontrado");
        String tipoComprador = normalizarTipoComprador(request.tipoComprador());
        Usuario usuarioComprador = obtenerUsuarioComprador(tipoComprador, request.idUsuarioComprador());

        List<DetalleCalculado> detallesCalculados = calcularDetalles(request.detalles(), tipoComprador, fechaOperacion);
        BigDecimal subtotalVenta = sumaSubtotal(detallesCalculados);
        BigDecimal totalVenta = sumaTotal(detallesCalculados);
        BigDecimal descuentoPromocion = subtotalVenta.subtract(totalVenta).setScale(2, RoundingMode.HALF_UP);

        validarPagos(request.pagos(), totalVenta);

        Venta venta = new Venta();
        venta.setCajaDiaria(cajaDiaria);
        venta.setUsuarioVendedor(usuarioVendedor);
        venta.setTipoComprador(tipoComprador);
        venta.setUsuarioComprador(usuarioComprador);
        venta.setFechaVenta(OffsetDateTime.now());
        venta.setEstadoVenta(ESTADO_VENTA_REGISTRADA);
        venta.setSubtotalVenta(subtotalVenta);
        venta.setDescuentoPromocion(descuentoPromocion);
        venta.setTotalVenta(totalVenta);

        Venta ventaGuardada = ventaRepository.saveAndFlush(venta);
        entityManager.refresh(ventaGuardada);

        List<DetalleVenta> detallesGuardados = detalleVentaRepository.saveAll(
                detallesCalculados.stream()
                        .map(detalle -> detalle.toEntity(ventaGuardada))
                        .toList());
        List<PagoVenta> pagosGuardados = pagoVentaRepository.saveAll(
                request.pagos().stream()
                        .map(pago -> crearPagoVenta(ventaGuardada, pago))
                        .toList());
        inventarioService.descontarVasosPorVenta(ventaGuardada, detallesGuardados, usuarioVendedor);

        return toResponse(ventaGuardada, detallesGuardados, pagosGuardados);
    }

    @Transactional(readOnly = true)
    public List<TrabajadorVentaResponse> listarTrabajadores() {
        return usuarioRepository.findAllByOrderByNombreCompletoAsc()
                .stream()
                .filter(this::esUsuarioBeneficiario)
                .map(usuario -> new TrabajadorVentaResponse(
                        usuario.getIdUsuario(),
                        usuario.getNombreUsuario(),
                        usuario.getNombreCompleto()))
                .toList();
    }

    @Transactional(readOnly = true)
    public VentaResponse consultarVentaParaAnulacion(UUID idVenta, PrincipalUsuario principalUsuario) {
        validarPermisoAnulacion(principalUsuario);
        Venta venta = obtenerVentaAnulable(idVenta);
        List<DetalleVenta> detalles = detalleVentaRepository.findByVenta_IdVenta(idVenta);
        List<PagoVenta> pagos = pagoVentaRepository.findByVenta_IdVenta(idVenta);
        return toResponse(venta, detalles, pagos);
    }

    @Transactional
    public VentaResponse anularVenta(UUID idVenta, AnularVentaRequest request, PrincipalUsuario principalUsuario) {
        validarPermisoAnulacion(principalUsuario);
        Venta venta = obtenerVentaAnulable(idVenta);

        Usuario usuarioAnulacion = obtenerUsuario(principalUsuario.idUsuario(), "Usuario anulacion no encontrado");
        Map<String, Object> valorAnterior = snapshotVenta(venta);
        List<DetalleVenta> detalles = detalleVentaRepository.findByVenta_IdVenta(idVenta);
        inventarioService.restaurarVasosPorAnulacion(venta, detalles, usuarioAnulacion);

        venta.setEstadoVenta(ESTADO_VENTA_ANULADA);
        venta.setMotivoAnulacion(request.motivoAnulacion().trim());
        venta.setFechaAnulacion(OffsetDateTime.now());
        venta.setUsuarioAnulacion(usuarioAnulacion);
        Venta ventaGuardada = ventaRepository.saveAndFlush(venta);
        List<PagoVenta> pagos = pagoVentaRepository.findByVenta_IdVenta(idVenta);
        auditoriaService.registrar(
                usuarioAnulacion,
                "ventas",
                ventaGuardada.getIdVenta(),
                "anular",
                valorAnterior,
                snapshotVenta(ventaGuardada),
                "Anulacion de venta");

        return toResponse(ventaGuardada, detalles, pagos);
    }

    private Venta obtenerVentaAnulable(UUID idVenta) {
        Venta venta = ventaRepository.findByIdVenta(idVenta)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Venta no encontrada"));
        if (!ESTADO_VENTA_REGISTRADA.equals(venta.getEstadoVenta())) {
            throw new ApiException(HttpStatus.CONFLICT, "Solo se pueden anular ventas registradas");
        }
        if (!ESTADO_CAJA_ABIERTA.equals(venta.getCajaDiaria().getEstadoCaja())) {
            throw new ApiException(HttpStatus.CONFLICT, "No se puede anular una venta con caja diaria cerrada");
        }
        return venta;
    }

    private void validarPermisoAnulacion(PrincipalUsuario principalUsuario) {
        String rol = principalUsuario.nombreRol().trim().toLowerCase(Locale.ROOT);
        if (!ROL_ADMINISTRADOR.equals(rol) && !ROL_GERENTE.equals(rol)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo administrador o gerente puede anular ventas");
        }
    }

    private CajaDiaria obtenerCajaAbierta() {
        return cajaDiariaRepository.findPrimeraPorEstadoCaja(ESTADO_CAJA_ABIERTA)
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "No existe caja diaria abierta para registrar venta"));
    }

    private Usuario obtenerUsuario(UUID idUsuario, String mensaje) {
        return usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, mensaje));
    }

    private String normalizarTipoComprador(String tipoComprador) {
        String normalizado = tipoComprador.trim().toLowerCase(Locale.ROOT);
        if (!TIPO_CLIENTE.equals(normalizado) && !TIPO_TRABAJADOR.equals(normalizado)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "tipoComprador debe ser cliente o trabajador");
        }
        return normalizado;
    }

    private Usuario obtenerUsuarioComprador(String tipoComprador, UUID idUsuarioComprador) {
        if (TIPO_CLIENTE.equals(tipoComprador)) {
            if (idUsuarioComprador != null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "idUsuarioComprador solo aplica para ventas a trabajador");
            }
            return null;
        }
        if (idUsuarioComprador == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "idUsuarioComprador es obligatorio para ventas a trabajador");
        }
        Usuario usuarioComprador = obtenerUsuario(idUsuarioComprador, "Usuario comprador trabajador no encontrado");
        if (!esUsuarioBeneficiario(usuarioComprador)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El comprador debe ser un usuario activo");
        }
        return usuarioComprador;
    }

    private boolean esUsuarioBeneficiario(Usuario usuario) {
        return ESTADO_USUARIO_ACTIVO.equals(usuario.getEstado());
    }

    private List<DetalleCalculado> calcularDetalles(
            List<RegistrarDetalleVentaRequest> detalles,
            String tipoComprador,
            LocalDate fechaOperacion) {
        return consolidarDetalles(detalles).entrySet().stream()
                .map(entry -> calcularDetalle(entry.getKey(), entry.getValue(), tipoComprador, fechaOperacion))
                .toList();
    }

    private Map<DetalleKey, Integer> consolidarDetalles(List<RegistrarDetalleVentaRequest> detalles) {
        Map<DetalleKey, Integer> consolidados = new LinkedHashMap<>();
        for (RegistrarDetalleVentaRequest detalle : detalles) {
            DetalleKey key = new DetalleKey(detalle.idTipoGranizado(), detalle.idTamanoVaso());
            consolidados.merge(key, detalle.cantidad(), Integer::sum);
        }
        return consolidados;
    }

    private DetalleCalculado calcularDetalle(
            DetalleKey key,
            Integer cantidad,
            String tipoComprador,
            LocalDate fechaOperacion) {
        PrecioGranizado precio = precioGranizadoRepository
                .findPrecioVigente(key.idTipoGranizado(), key.idTamanoVaso(), fechaOperacion)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "No existe precio vigente para el tipo y tamano indicados"));

        BigDecimal precioUnitario = normalizarMoneda(precio.getValorPrecio());
        BigDecimal subtotalLinea = normalizarMoneda(precioUnitario.multiply(BigDecimal.valueOf(cantidad)));
        Promocion promocion = seleccionarPromocion(key, tipoComprador, fechaOperacion);

        if (promocion == null || cantidad < promocion.getCantidadRequerida()) {
            return new DetalleCalculado(precio, cantidad, precioUnitario, 0, cantidad, null, null, subtotalLinea, subtotalLinea);
        }

        int conjuntosPromocion = cantidad / promocion.getCantidadRequerida();
        int cantidadConPromocion = conjuntosPromocion * promocion.getCantidadRequerida();
        int cantidadSinPromocion = cantidad - cantidadConPromocion;
        BigDecimal totalPromocional = normalizarMoneda(
                promocion.getValorPromocional().multiply(BigDecimal.valueOf(conjuntosPromocion)));
        BigDecimal totalSinPromocion = normalizarMoneda(precioUnitario.multiply(BigDecimal.valueOf(cantidadSinPromocion)));
        BigDecimal totalLinea = normalizarMoneda(totalPromocional.add(totalSinPromocion));

        return new DetalleCalculado(
                precio,
                cantidad,
                precioUnitario,
                cantidadConPromocion,
                cantidadSinPromocion,
                normalizarMoneda(promocion.getValorPromocional()),
                promocion,
                subtotalLinea,
                totalLinea);
    }

    private Promocion seleccionarPromocion(DetalleKey key, String tipoComprador, LocalDate fechaOperacion) {
        return promocionRepository.findVigentesParaVenta(
                        key.idTipoGranizado(),
                        key.idTamanoVaso(),
                        tipoComprador,
                        fechaOperacion)
                .stream()
                .filter(promocion -> promocionAplica(promocion, tipoComprador, fechaOperacion))
                .findFirst()
                .orElse(null);
    }

    private boolean promocionAplica(Promocion promocion, String tipoComprador, LocalDate fechaOperacion) {
        if (TIPO_TRABAJADOR.equals(tipoComprador)) {
            return true;
        }
        String diaSemana = diaSemana(fechaOperacion.getDayOfWeek());
        return promocion.getDiasPromocion().stream()
                .anyMatch(dia -> diaSemana.equals(dia.getDiaSemana()));
    }

    private String diaSemana(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "lunes";
            case TUESDAY -> "martes";
            case WEDNESDAY -> "miercoles";
            case THURSDAY -> "jueves";
            case FRIDAY -> "viernes";
            case SATURDAY -> "sabado";
            case SUNDAY -> "domingo";
        };
    }

    private BigDecimal sumaSubtotal(List<DetalleCalculado> detalles) {
        return normalizarMoneda(detalles.stream()
                .map(DetalleCalculado::subtotalLinea)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private BigDecimal sumaTotal(List<DetalleCalculado> detalles) {
        return normalizarMoneda(detalles.stream()
                .map(DetalleCalculado::totalLinea)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private void validarPagos(List<RegistrarPagoVentaRequest> pagos, BigDecimal totalVenta) {
        BigDecimal totalPagos = normalizarMoneda(pagos.stream()
                .map(RegistrarPagoVentaRequest::valorPago)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        if (totalPagos.compareTo(totalVenta) != 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La suma de pagos debe coincidir con total_venta");
        }

        for (RegistrarPagoVentaRequest pago : pagos) {
            MetodoPago metodoPago = obtenerMetodoPago(pago.idMetodoPago());
            String nombreMetodo = metodoPago.getNombreMetodo();
            if (METODO_EFECTIVO.equals(nombreMetodo)) {
                BigDecimal recibido = pago.valorRecibidoEfectivo() == null ? pago.valorPago() : pago.valorRecibidoEfectivo();
                if (recibido.compareTo(pago.valorPago()) < 0) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "valorRecibidoEfectivo no puede ser menor que valorPago");
                }
            } else if (METODO_TRANSFERENCIA.equals(nombreMetodo)) {
                if (pago.valorRecibidoEfectivo() != null) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "valorRecibidoEfectivo solo aplica para efectivo");
                }
            } else {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Metodo de pago no soportado para ventas");
            }
        }
    }

    private PagoVenta crearPagoVenta(Venta venta, RegistrarPagoVentaRequest request) {
        MetodoPago metodoPago = obtenerMetodoPago(request.idMetodoPago());
        PagoVenta pagoVenta = new PagoVenta();
        pagoVenta.setVenta(venta);
        pagoVenta.setMetodoPago(metodoPago);
        pagoVenta.setValorPago(normalizarMoneda(request.valorPago()));
        pagoVenta.setFechaRegistro(OffsetDateTime.now());

        if (METODO_EFECTIVO.equals(metodoPago.getNombreMetodo())) {
            BigDecimal recibido = request.valorRecibidoEfectivo() == null
                    ? request.valorPago()
                    : request.valorRecibidoEfectivo();
            pagoVenta.setValorRecibidoEfectivo(normalizarMoneda(recibido));
            pagoVenta.setCambioEntregado(normalizarMoneda(recibido.subtract(request.valorPago())));
            pagoVenta.setEstadoValidacion("no_aplica");
        } else {
            pagoVenta.setEstadoValidacion("pendiente");
        }
        return pagoVenta;
    }

    private MetodoPago obtenerMetodoPago(UUID idMetodoPago) {
        MetodoPago metodoPago = metodoPagoRepository.findById(idMetodoPago)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Metodo de pago no encontrado"));
        if (!"activo".equals(metodoPago.getEstado())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Metodo de pago inactivo");
        }
        return metodoPago;
    }

    private VentaResponse toResponse(Venta venta, List<DetalleVenta> detalles, List<PagoVenta> pagos) {
        return new VentaResponse(
                venta.getIdVenta(),
                venta.getCajaDiaria().getIdCajaDiaria(),
                venta.getUsuarioVendedor().getIdUsuario(),
                venta.getUsuarioVendedor().getNombreUsuario(),
                venta.getTipoComprador(),
                venta.getUsuarioComprador() == null ? null : venta.getUsuarioComprador().getIdUsuario(),
                venta.getNumeroVenta(),
                venta.getFechaVenta(),
                venta.getEstadoVenta(),
                venta.getSubtotalVenta(),
                venta.getDescuentoPromocion(),
                venta.getTotalVenta(),
                detalles.stream().map(this::toResponse).toList(),
                pagos.stream().map(this::toResponse).toList());
    }

    private DetalleVentaResponse toResponse(DetalleVenta detalle) {
        Promocion promocion = detalle.getPromocionAplicada();
        return new DetalleVentaResponse(
                detalle.getIdDetalleVenta(),
                detalle.getTipoGranizado().getIdTipoGranizado(),
                detalle.getTipoGranizado().getNombreTipo(),
                detalle.getTamanoVaso().getIdTamanoVaso(),
                detalle.getTamanoVaso().getOnzas(),
                detalle.getCantidad(),
                detalle.getPrecioUnitarioNormal(),
                detalle.getCantidadConPromocion(),
                detalle.getCantidadSinPromocion(),
                detalle.getValorPromocionalAplicado(),
                promocion == null ? null : promocion.getIdPromocion(),
                promocion == null ? null : promocion.getNombrePromocion(),
                detalle.getSubtotalLinea(),
                detalle.getTotalLinea());
    }

    private PagoVentaResponse toResponse(PagoVenta pago) {
        return new PagoVentaResponse(
                pago.getIdPagoVenta(),
                pago.getMetodoPago().getIdMetodoPago(),
                pago.getMetodoPago().getNombreMetodo(),
                pago.getValorPago(),
                pago.getValorRecibidoEfectivo(),
                pago.getCambioEntregado(),
                pago.getEstadoValidacion(),
                pago.getFechaRegistro(),
                pago.getUsuarioValidacion() == null ? null : pago.getUsuarioValidacion().getIdUsuario(),
                pago.getUsuarioValidacion() == null ? null : pago.getUsuarioValidacion().getNombreUsuario(),
                pago.getFechaValidacion(),
                pago.getObservacionValidacion());
    }

    private BigDecimal normalizarMoneda(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    private Map<String, Object> snapshotVenta(Venta venta) {
        Usuario usuarioComprador = venta.getUsuarioComprador();
        Usuario usuarioAnulacion = venta.getUsuarioAnulacion();
        return valores(
                "id_venta", venta.getIdVenta(),
                "id_caja_diaria", venta.getCajaDiaria().getIdCajaDiaria(),
                "id_usuario_vendedor", venta.getUsuarioVendedor().getIdUsuario(),
                "tipo_comprador", venta.getTipoComprador(),
                "id_usuario_comprador", usuarioComprador == null ? null : usuarioComprador.getIdUsuario(),
                "numero_venta", venta.getNumeroVenta(),
                "estado_venta", venta.getEstadoVenta(),
                "subtotal_venta", venta.getSubtotalVenta(),
                "descuento_promocion", venta.getDescuentoPromocion(),
                "total_venta", venta.getTotalVenta(),
                "motivo_anulacion", venta.getMotivoAnulacion(),
                "fecha_anulacion", venta.getFechaAnulacion(),
                "id_usuario_anulacion", usuarioAnulacion == null ? null : usuarioAnulacion.getIdUsuario());
    }

    private record DetalleKey(UUID idTipoGranizado, UUID idTamanoVaso) {
    }

    private record DetalleCalculado(
            PrecioGranizado precio,
            Integer cantidad,
            BigDecimal precioUnitarioNormal,
            Integer cantidadConPromocion,
            Integer cantidadSinPromocion,
            BigDecimal valorPromocionalAplicado,
            Promocion promocionAplicada,
            BigDecimal subtotalLinea,
            BigDecimal totalLinea
    ) {
        private DetalleVenta toEntity(Venta venta) {
            DetalleVenta detalle = new DetalleVenta();
            detalle.setVenta(venta);
            detalle.setTipoGranizado(precio.getTipoGranizado());
            detalle.setTamanoVaso(precio.getTamanoVaso());
            detalle.setCantidad(cantidad);
            detalle.setPrecioUnitarioNormal(precioUnitarioNormal);
            detalle.setCantidadConPromocion(cantidadConPromocion);
            detalle.setCantidadSinPromocion(cantidadSinPromocion);
            detalle.setValorPromocionalAplicado(valorPromocionalAplicado);
            detalle.setPromocionAplicada(promocionAplicada);
            detalle.setSubtotalLinea(subtotalLinea);
            detalle.setTotalLinea(totalLinea);
            return detalle;
        }
    }
}
