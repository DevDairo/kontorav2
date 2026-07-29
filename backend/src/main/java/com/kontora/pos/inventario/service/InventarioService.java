package com.kontora.pos.inventario.service;

import com.kontora.pos.auditoria.service.AuditoriaService;
import com.kontora.pos.caja.domain.CajaDiaria;
import com.kontora.pos.caja.repository.CajaDiariaRepository;
import com.kontora.pos.catalogos.domain.ItemInventario;
import com.kontora.pos.catalogos.domain.TamanoVaso;
import com.kontora.pos.catalogos.repository.ItemInventarioRepository;
import com.kontora.pos.common.exception.ApiException;
import com.kontora.pos.common.security.PrincipalUsuario;
import com.kontora.pos.cortesias.domain.Cortesia;
import com.kontora.pos.cortesias.domain.DetalleCortesia;
import com.kontora.pos.inventario.domain.AjusteInventario;
import com.kontora.pos.inventario.domain.ConsumoDiarioInventario;
import com.kontora.pos.inventario.domain.ExistenciaInventarioDiario;
import com.kontora.pos.inventario.domain.ExistenciaInventarioGeneral;
import com.kontora.pos.inventario.domain.MovimientoInventario;
import com.kontora.pos.inventario.domain.PaqueteVasosAbierto;
import com.kontora.pos.inventario.domain.PerdidaInventario;
import com.kontora.pos.inventario.dto.AjusteInventarioResponse;
import com.kontora.pos.inventario.dto.ConsumoDiarioInventarioResponse;
import com.kontora.pos.inventario.dto.ExistenciaInventarioDiarioResponse;
import com.kontora.pos.inventario.dto.ExistenciaInventarioGeneralResponse;
import com.kontora.pos.inventario.dto.MovimientoInventarioResponse;
import com.kontora.pos.inventario.dto.PaqueteVasosAbiertoResponse;
import com.kontora.pos.inventario.dto.RegistrarConsumoDiarioInventarioRequest;
import com.kontora.pos.inventario.dto.RegistrarPaqueteVasosRequest;
import com.kontora.pos.inventario.dto.ResolverAjusteInventarioRequest;
import com.kontora.pos.inventario.dto.SolicitarAjusteInventarioRequest;
import com.kontora.pos.inventario.dto.VentasVasosDiariasResponse;
import com.kontora.pos.inventario.repository.AjusteInventarioRepository;
import com.kontora.pos.inventario.repository.ConsumoDiarioInventarioRepository;
import com.kontora.pos.inventario.repository.ExistenciaInventarioDiarioRepository;
import com.kontora.pos.inventario.repository.ExistenciaInventarioGeneralRepository;
import com.kontora.pos.inventario.repository.MovimientoInventarioRepository;
import com.kontora.pos.inventario.repository.PaqueteVasosAbiertoRepository;
import com.kontora.pos.inventario.repository.VentasVasosDiariasRepository;
import com.kontora.pos.usuarios.domain.Usuario;
import com.kontora.pos.usuarios.repository.UsuarioRepository;
import com.kontora.pos.ventas.domain.DetalleVenta;
import com.kontora.pos.ventas.domain.Venta;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static com.kontora.pos.common.audit.AuditoriaValores.valores;

@Service
public class InventarioService {

    private static final String ESTADO_ACTIVO = "activo";
    private static final String ESTADO_CAJA_ABIERTA = "abierta";
    private static final String TIPO_CONTROL_AUTOMATICO = "automatico_por_venta";
    private static final String TIPO_STOCK_GENERAL = "general";
    private static final String TIPO_STOCK_DIARIO = "diario";
    private static final String MOVIMIENTO_APERTURA_PAQUETE = "apertura_paquete";
    private static final String MOVIMIENTO_PERDIDA = "perdida";
    private static final String MOVIMIENTO_ANULACION_PERDIDA = "anulacion_perdida";
    private static final String MOVIMIENTO_VENTA = "venta";
    private static final String MOVIMIENTO_ANULACION_VENTA = "anulacion_venta";
    private static final String MOVIMIENTO_CORTESIA = "cortesia";
    private static final String MOVIMIENTO_ANULACION_CORTESIA = "anulacion_cortesia";
    private static final String MOVIMIENTO_CONSUMO_DIARIO = "consumo_diario";
    private static final String MOVIMIENTO_AJUSTE = "ajuste";
    private static final String SENTIDO_ENTRADA = "entrada";
    private static final String SENTIDO_SALIDA = "salida";
    private static final String ESTADO_AJUSTE_PENDIENTE = "pendiente";
    private static final String ESTADO_AJUSTE_APROBADO = "aprobado";
    private static final String ESTADO_AJUSTE_RECHAZADO = "rechazado";
    private static final String REFERENCIA_PAQUETES = "paquetes_vasos_abiertos";
    private static final String REFERENCIA_PERDIDAS = "perdidas_inventario";
    private static final String REFERENCIA_CONSUMOS = "consumos_diarios_inventario";
    private static final String REFERENCIA_VENTAS = "ventas";
    private static final String REFERENCIA_CORTESIAS = "cortesias";
    private static final String REFERENCIA_AJUSTES = "ajustes_inventario";

    private final CajaDiariaRepository cajaDiariaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ItemInventarioRepository itemInventarioRepository;
    private final AjusteInventarioRepository ajusteInventarioRepository;
    private final ExistenciaInventarioGeneralRepository existenciaGeneralRepository;
    private final ExistenciaInventarioDiarioRepository existenciaDiarioRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final PaqueteVasosAbiertoRepository paqueteVasosAbiertoRepository;
    private final ConsumoDiarioInventarioRepository consumoDiarioInventarioRepository;
    private final VentasVasosDiariasRepository ventasVasosDiariasRepository;
    private final EntityManager entityManager;
    private final AuditoriaService auditoriaService;

    public InventarioService(
            CajaDiariaRepository cajaDiariaRepository,
            UsuarioRepository usuarioRepository,
            ItemInventarioRepository itemInventarioRepository,
            AjusteInventarioRepository ajusteInventarioRepository,
            ExistenciaInventarioGeneralRepository existenciaGeneralRepository,
            ExistenciaInventarioDiarioRepository existenciaDiarioRepository,
            MovimientoInventarioRepository movimientoInventarioRepository,
            PaqueteVasosAbiertoRepository paqueteVasosAbiertoRepository,
            ConsumoDiarioInventarioRepository consumoDiarioInventarioRepository,
            VentasVasosDiariasRepository ventasVasosDiariasRepository,
            EntityManager entityManager,
            AuditoriaService auditoriaService) {
        this.cajaDiariaRepository = cajaDiariaRepository;
        this.usuarioRepository = usuarioRepository;
        this.itemInventarioRepository = itemInventarioRepository;
        this.ajusteInventarioRepository = ajusteInventarioRepository;
        this.existenciaGeneralRepository = existenciaGeneralRepository;
        this.existenciaDiarioRepository = existenciaDiarioRepository;
        this.movimientoInventarioRepository = movimientoInventarioRepository;
        this.paqueteVasosAbiertoRepository = paqueteVasosAbiertoRepository;
        this.consumoDiarioInventarioRepository = consumoDiarioInventarioRepository;
        this.ventasVasosDiariasRepository = ventasVasosDiariasRepository;
        this.entityManager = entityManager;
        this.auditoriaService = auditoriaService;
    }

    @Transactional(readOnly = true)
    public List<ExistenciaInventarioGeneralResponse> consultarExistenciasGenerales() {
        return existenciaGeneralRepository.findAllConItem().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ExistenciaInventarioDiarioResponse> consultarExistenciasDiariasCajaAbierta() {
        CajaDiaria cajaDiaria = obtenerCajaAbierta();
        return consultarExistenciasDiariasPorCaja(cajaDiaria.getIdCajaDiaria());
    }

    @Transactional(readOnly = true)
    public List<ExistenciaInventarioDiarioResponse> consultarExistenciasDiariasPorCaja(UUID idCajaDiaria) {
        validarCajaExiste(idCajaDiaria);
        return existenciaDiarioRepository.findByCaja(idCajaDiaria).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VentasVasosDiariasResponse> consultarVentasVasosDiariaAbierta(
            PrincipalUsuario principalUsuario) {
        validarRolConsultaInventario(principalUsuario);
        return cajaDiariaRepository.findPrimeraPorEstadoCaja(ESTADO_CAJA_ABIERTA)
                .map(cajaDiaria -> ventasVasosDiariasRepository.consultarPorCajaAbierta(cajaDiaria.getIdCajaDiaria()))
                .orElseGet(List::of);
    }

    @Transactional(readOnly = true)
    public List<MovimientoInventarioResponse> consultarMovimientos(UUID idCajaDiaria, UUID idItemInventario) {
        List<MovimientoInventario> movimientos;
        if (idCajaDiaria != null && idItemInventario != null) {
            movimientos = movimientoInventarioRepository
                    .findByCajaDiaria_IdCajaDiariaAndItemInventario_IdItemInventarioOrderByFechaMovimientoDesc(
                            idCajaDiaria,
                            idItemInventario);
        } else if (idCajaDiaria != null) {
            movimientos = movimientoInventarioRepository.findByCajaDiaria_IdCajaDiariaOrderByFechaMovimientoDesc(idCajaDiaria);
        } else if (idItemInventario != null) {
            movimientos = movimientoInventarioRepository.findByItemInventario_IdItemInventarioOrderByFechaMovimientoDesc(idItemInventario);
        } else {
            movimientos = movimientoInventarioRepository.findAllByOrderByFechaMovimientoDesc();
        }
        return movimientos.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<AjusteInventarioResponse> consultarAjustes(
            String estadoAprobacion,
            PrincipalUsuario principalUsuario) {
        validarRolConsultaAjustes(principalUsuario);
        String estadoNormalizado = normalizarEstadoAprobacionFiltro(estadoAprobacion);
        List<AjusteInventario> ajustes = estadoNormalizado == null
                ? ajusteInventarioRepository.findAllByOrderByFechaSolicitudDesc()
                : ajusteInventarioRepository.findByEstadoAprobacionOrderByFechaSolicitudDesc(estadoNormalizado);
        return ajustes.stream().map(this::toResponse).toList();
    }

    @Transactional
    public void inicializarStockDiarioParaCaja(CajaDiaria cajaDiaria) {
        for (ItemInventario item : itemInventarioRepository.findActivosParaOperacion()) {
            if (!esItemVasoConPaquetes(item)
                    || existenciaDiarioRepository.findByCajaAndItemForUpdate(
                            cajaDiaria.getIdCajaDiaria(),
                            item.getIdItemInventario()).isPresent()) {
                continue;
            }

            nuevaExistenciaDiaria(cajaDiaria, item, remanenteDiarioAnterior(cajaDiaria, item));
        }
    }

    @Transactional
    public AjusteInventarioResponse solicitarAjusteInventario(
            SolicitarAjusteInventarioRequest request,
            PrincipalUsuario principalUsuario) {
        validarRolSolicitudAjuste(principalUsuario);
        Usuario usuarioSolicitante = obtenerUsuario(principalUsuario.idUsuario());
        ItemInventario item = obtenerItemActivo(request.idItemInventario());
        String tipoStock = normalizarTipoStock(request.tipoStock());
        String sentidoAjuste = normalizarSentidoAjuste(request.sentidoAjuste());
        CajaDiaria cajaDiaria = null;
        if (TIPO_STOCK_DIARIO.equals(tipoStock)) {
            cajaDiaria = validarReabastecimientoStockDiario(
                    item,
                    sentidoAjuste,
                    request.cantidadAjuste());
        }

        AjusteInventario ajuste = new AjusteInventario();
        ajuste.setItemInventario(item);
        ajuste.setCajaDiaria(cajaDiaria);
        ajuste.setTipoStock(tipoStock);
        ajuste.setCantidadAjuste(request.cantidadAjuste());
        ajuste.setSentidoAjuste(sentidoAjuste);
        ajuste.setMotivoAjuste(request.motivoAjuste().trim());
        ajuste.setEstadoAprobacion(ESTADO_AJUSTE_PENDIENTE);
        ajuste.setUsuarioSolicitante(usuarioSolicitante);
        ajuste.setFechaSolicitud(OffsetDateTime.now());

        AjusteInventario ajusteGuardado = ajusteInventarioRepository.saveAndFlush(ajuste);
        auditoriaService.registrar(
                usuarioSolicitante,
                "ajustes_inventario",
                ajusteGuardado.getIdAjusteInventario(),
                "crear",
                null,
                snapshotAjuste(ajusteGuardado),
                "Solicitud de ajuste de inventario");

        if (esGerente(principalUsuario)) {
            Map<String, Object> valorAnterior = snapshotAjuste(ajusteGuardado);
            aplicarAjusteInventario(ajusteGuardado, usuarioSolicitante, ajusteGuardado.getMotivoAjuste());
            ajusteGuardado.setEstadoAprobacion(ESTADO_AJUSTE_APROBADO);
            ajusteGuardado.setUsuarioAprobador(usuarioSolicitante);
            ajusteGuardado.setFechaAprobacion(OffsetDateTime.now());
            ajusteGuardado.setObservacionAprobacion("Aplicado directamente por gerente");

            AjusteInventario ajusteAplicado = ajusteInventarioRepository.saveAndFlush(ajusteGuardado);
            auditoriaService.registrar(
                    usuarioSolicitante,
                    "ajustes_inventario",
                    ajusteAplicado.getIdAjusteInventario(),
                    "aprobar",
                    valorAnterior,
                    snapshotAjuste(ajusteAplicado),
                    "Aplicacion directa de ajuste de inventario por gerente");
            return toResponse(ajusteAplicado);
        }

        return toResponse(ajusteGuardado);
    }

    @Transactional
    public AjusteInventarioResponse aprobarAjusteInventario(
            UUID idAjusteInventario,
            ResolverAjusteInventarioRequest request,
            PrincipalUsuario principalUsuario) {
        validarRolAprobacionAjuste(principalUsuario);
        Usuario usuarioAprobador = obtenerUsuario(principalUsuario.idUsuario());
        AjusteInventario ajuste = obtenerAjustePendienteBloqueado(idAjusteInventario);
        Map<String, Object> valorAnterior = snapshotAjuste(ajuste);

        aplicarAjusteInventario(
                ajuste,
                usuarioAprobador,
                normalizarObservacion(request == null ? null : request.observacionAprobacion()));

        ajuste.setEstadoAprobacion(ESTADO_AJUSTE_APROBADO);
        ajuste.setUsuarioAprobador(usuarioAprobador);
        ajuste.setFechaAprobacion(OffsetDateTime.now());
        ajuste.setObservacionAprobacion(normalizarObservacion(request == null ? null : request.observacionAprobacion()));

        AjusteInventario ajusteGuardado = ajusteInventarioRepository.saveAndFlush(ajuste);
        auditoriaService.registrar(
                usuarioAprobador,
                "ajustes_inventario",
                ajusteGuardado.getIdAjusteInventario(),
                "aprobar",
                valorAnterior,
                snapshotAjuste(ajusteGuardado),
                "Aprobacion de ajuste de inventario");
        return toResponse(ajusteGuardado);
    }

    @Transactional
    public AjusteInventarioResponse rechazarAjusteInventario(
            UUID idAjusteInventario,
            ResolverAjusteInventarioRequest request,
            PrincipalUsuario principalUsuario) {
        validarRolAprobacionAjuste(principalUsuario);
        Usuario usuarioAprobador = obtenerUsuario(principalUsuario.idUsuario());
        AjusteInventario ajuste = obtenerAjustePendienteBloqueado(idAjusteInventario);
        Map<String, Object> valorAnterior = snapshotAjuste(ajuste);

        ajuste.setEstadoAprobacion(ESTADO_AJUSTE_RECHAZADO);
        ajuste.setUsuarioAprobador(usuarioAprobador);
        ajuste.setFechaAprobacion(OffsetDateTime.now());
        ajuste.setObservacionAprobacion(normalizarObservacion(request == null ? null : request.observacionAprobacion()));

        AjusteInventario ajusteGuardado = ajusteInventarioRepository.saveAndFlush(ajuste);
        auditoriaService.registrar(
                usuarioAprobador,
                "ajustes_inventario",
                ajusteGuardado.getIdAjusteInventario(),
                "rechazar",
                valorAnterior,
                snapshotAjuste(ajusteGuardado),
                "Rechazo de ajuste de inventario");
        return toResponse(ajusteGuardado);
    }

    @Transactional
    public PaqueteVasosAbiertoResponse registrarPaqueteVasos(
            RegistrarPaqueteVasosRequest request,
            PrincipalUsuario principalUsuario) {
        validarRolGestionInventario(principalUsuario);
        CajaDiaria cajaDiaria = obtenerCajaAbierta();
        Usuario usuario = obtenerUsuario(principalUsuario.idUsuario());
        ItemInventario item = obtenerItemActivo(request.idItemInventario());
        validarItemVasoConPaquetes(item);

        int cantidadPaquetes = request.cantidadPaquetes();
        int unidadesPorPaquete = item.getUnidadesPorPaquete();
        int unidadesGeneradas = cantidadPaquetes * unidadesPorPaquete;
        int unidadesRotas = request.unidadesRotas() == null ? 0 : request.unidadesRotas();
        if (unidadesRotas > 0) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Los vasos rotos deben registrarse como perdida y adjuntar su evidencia antes del cierre");
        }

        ExistenciaInventarioGeneral existenciaGeneral = obtenerExistenciaGeneralBloqueada(item);
        validarStockSuficiente(existenciaGeneral.getCantidadActual(), unidadesGeneradas, "Stock general insuficiente para abrir paquetes de vasos");

        PaqueteVasosAbierto paquete = new PaqueteVasosAbierto();
        paquete.setCajaDiaria(cajaDiaria);
        paquete.setItemInventario(item);
        paquete.setCantidadPaquetes(cantidadPaquetes);
        paquete.setUnidadesPorPaquete(unidadesPorPaquete);
        paquete.setUnidadesRotas(unidadesRotas);
        paquete.setUsuarioRegistro(usuario);
        paquete.setFechaRegistro(OffsetDateTime.now());

        PaqueteVasosAbierto paqueteGuardado = paqueteVasosAbiertoRepository.saveAndFlush(paquete);
        entityManager.refresh(paqueteGuardado);

        existenciaGeneral.setCantidadActual(existenciaGeneral.getCantidadActual() - unidadesGeneradas);

        ExistenciaInventarioDiario existenciaDiaria = obtenerOCrearExistenciaDiaria(cajaDiaria, item);
        existenciaDiaria.setCantidadIngresada(valor(existenciaDiaria.getCantidadIngresada()) + unidadesGeneradas);
        recalcularCantidadFinalTeorica(existenciaDiaria);

        registrarMovimiento(
                item,
                cajaDiaria,
                TIPO_STOCK_GENERAL,
                MOVIMIENTO_APERTURA_PAQUETE,
                unidadesGeneradas,
                SENTIDO_SALIDA,
                REFERENCIA_PAQUETES,
                paqueteGuardado.getIdPaqueteVasosAbierto(),
                "Salida de stock general por apertura de paquetes",
                usuario);
        registrarMovimiento(
                item,
                cajaDiaria,
                TIPO_STOCK_DIARIO,
                MOVIMIENTO_APERTURA_PAQUETE,
                unidadesGeneradas,
                SENTIDO_ENTRADA,
                REFERENCIA_PAQUETES,
                paqueteGuardado.getIdPaqueteVasosAbierto(),
                "Ingreso al stock diario por apertura de paquetes",
                usuario);
        return toResponse(paqueteGuardado);
    }

    @Transactional
    public ConsumoDiarioInventarioResponse registrarConsumoDiario(
            RegistrarConsumoDiarioInventarioRequest request,
            PrincipalUsuario principalUsuario) {
        validarRolGestionInventario(principalUsuario);
        CajaDiaria cajaDiaria = obtenerCajaAbierta();
        Usuario usuario = obtenerUsuario(principalUsuario.idUsuario());
        ItemInventario item = obtenerItemActivo(request.idItemInventario());
        if (TIPO_CONTROL_AUTOMATICO.equals(item.getTipoControl())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Los consumos manuales no aplican a items automaticos por venta");
        }

        ExistenciaInventarioGeneral existenciaGeneral = obtenerExistenciaGeneralBloqueada(item);
        validarStockSuficiente(
                existenciaGeneral.getCantidadActual(),
                request.cantidadConsumida(),
                "Stock general insuficiente para registrar consumo diario");

        ConsumoDiarioInventario consumo = new ConsumoDiarioInventario();
        consumo.setCajaDiaria(cajaDiaria);
        consumo.setItemInventario(item);
        consumo.setCantidadConsumida(request.cantidadConsumida());
        consumo.setUsuarioRegistro(usuario);
        consumo.setFechaRegistro(OffsetDateTime.now());
        consumo.setObservacion(normalizarObservacion(request.observacion()));

        ConsumoDiarioInventario consumoGuardado = consumoDiarioInventarioRepository.saveAndFlush(consumo);
        existenciaGeneral.setCantidadActual(existenciaGeneral.getCantidadActual() - request.cantidadConsumida());
        registrarMovimiento(
                item,
                cajaDiaria,
                TIPO_STOCK_GENERAL,
                MOVIMIENTO_CONSUMO_DIARIO,
                request.cantidadConsumida(),
                SENTIDO_SALIDA,
                REFERENCIA_CONSUMOS,
                consumoGuardado.getIdConsumoDiarioInventario(),
                consumoGuardado.getObservacion(),
                usuario);

        return toResponse(consumoGuardado);
    }

    public void descontarVasosPorVenta(Venta venta, List<DetalleVenta> detalles, Usuario usuarioRegistro) {
        for (DetalleVenta detalle : detalles) {
            ItemInventario item = obtenerVasoPorTamano(detalle.getTamanoVaso().getIdTamanoVaso());
            ExistenciaInventarioDiario existenciaDiaria = obtenerExistenciaDiariaBloqueada(
                    venta.getCajaDiaria(),
                    item,
                    "No existe stock diario para el vaso vendido");
            int cantidad = detalle.getCantidad();
            validarStockSuficiente(
                    stockDiarioDisponible(existenciaDiaria),
                    cantidad,
                    "Stock diario insuficiente para registrar la venta");

            existenciaDiaria.setCantidadVendida(valor(existenciaDiaria.getCantidadVendida()) + cantidad);
            recalcularCantidadFinalTeorica(existenciaDiaria);
            registrarMovimiento(
                    item,
                    venta.getCajaDiaria(),
                    TIPO_STOCK_DIARIO,
                    MOVIMIENTO_VENTA,
                    cantidad,
                    SENTIDO_SALIDA,
                    REFERENCIA_VENTAS,
                    venta.getIdVenta(),
                    "Salida automatica por venta",
                    usuarioRegistro);
        }
    }

    public void restaurarVasosPorAnulacion(Venta venta, List<DetalleVenta> detalles, Usuario usuarioRegistro) {
        for (DetalleVenta detalle : detalles) {
            ItemInventario item = obtenerVasoPorTamano(detalle.getTamanoVaso().getIdTamanoVaso());
            ExistenciaInventarioDiario existenciaDiaria = obtenerExistenciaDiariaBloqueada(
                    venta.getCajaDiaria(),
                    item,
                    "No existe stock diario para restaurar la venta anulada");
            int cantidad = detalle.getCantidad();
            if (valor(existenciaDiaria.getCantidadVendida()) < cantidad) {
                throw new ApiException(HttpStatus.CONFLICT, "La anulacion dejaria cantidad_vendida negativa");
            }

            existenciaDiaria.setCantidadVendida(existenciaDiaria.getCantidadVendida() - cantidad);
            recalcularCantidadFinalTeorica(existenciaDiaria);
            registrarMovimiento(
                    item,
                    venta.getCajaDiaria(),
                    TIPO_STOCK_DIARIO,
                    MOVIMIENTO_ANULACION_VENTA,
                    cantidad,
                    SENTIDO_ENTRADA,
                    REFERENCIA_VENTAS,
                    venta.getIdVenta(),
                    "Restauracion de stock diario por anulacion de venta",
                    usuarioRegistro);
        }
    }

    @Transactional
    public void descontarVasosPorCortesia(
            Cortesia cortesia,
            List<DetalleCortesia> detalles,
            Usuario usuarioRegistro) {
        for (DetalleCortesia detalle : detalles) {
            ItemInventario item = obtenerVasoPorTamano(
                    detalle.getTamanoVaso().getIdTamanoVaso());
            ExistenciaInventarioDiario existenciaDiaria = obtenerExistenciaDiariaBloqueada(
                    cortesia.getCajaDiaria(),
                    item,
                    "No existe stock diario para el vaso entregado como cortesia");
            int cantidad = detalle.getCantidad();
            validarStockSuficiente(
                    stockDiarioDisponible(existenciaDiaria),
                    cantidad,
                    "Stock diario insuficiente para registrar la cortesia");

            existenciaDiaria.setCantidadCortesia(
                    valor(existenciaDiaria.getCantidadCortesia()) + cantidad);
            recalcularCantidadFinalTeorica(existenciaDiaria);
            registrarMovimiento(
                    item,
                    cortesia.getCajaDiaria(),
                    TIPO_STOCK_DIARIO,
                    MOVIMIENTO_CORTESIA,
                    cantidad,
                    SENTIDO_SALIDA,
                    REFERENCIA_CORTESIAS,
                    cortesia.getIdCortesia(),
                    "Salida de stock diario por cortesia",
                    usuarioRegistro);
        }
    }

    @Transactional
    public void restaurarVasosPorAnulacionCortesia(
            Cortesia cortesia,
            List<DetalleCortesia> detalles,
            Usuario usuarioRegistro) {
        for (DetalleCortesia detalle : detalles) {
            ItemInventario item = obtenerVasoPorTamano(
                    detalle.getTamanoVaso().getIdTamanoVaso());
            ExistenciaInventarioDiario existenciaDiaria = obtenerExistenciaDiariaBloqueada(
                    cortesia.getCajaDiaria(),
                    item,
                    "No existe stock diario para restaurar la cortesia anulada");
            int cantidad = detalle.getCantidad();
            if (valor(existenciaDiaria.getCantidadCortesia()) < cantidad) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "La anulacion dejaria cantidad_cortesia negativa");
            }

            existenciaDiaria.setCantidadCortesia(
                    existenciaDiaria.getCantidadCortesia() - cantidad);
            recalcularCantidadFinalTeorica(existenciaDiaria);
            registrarMovimiento(
                    item,
                    cortesia.getCajaDiaria(),
                    TIPO_STOCK_DIARIO,
                    MOVIMIENTO_ANULACION_CORTESIA,
                    cantidad,
                    SENTIDO_ENTRADA,
                    REFERENCIA_CORTESIAS,
                    cortesia.getIdCortesia(),
                    "Restauracion de stock diario por anulacion de cortesia no entregada",
                    usuarioRegistro);
        }
    }

    @Transactional
    public void registrarPerdidaVasos(
            PerdidaInventario perdida,
            Usuario usuarioRegistro) {
        ItemInventario item = perdida.getItemInventario();
        validarItemVasoConPaquetes(item);
        ExistenciaInventarioDiario existenciaDiaria = obtenerExistenciaDiariaBloqueada(
                perdida.getCajaDiaria(),
                item,
                "No existe stock diario para registrar la perdida de vasos");
        int cantidad = perdida.getCantidad();
        validarStockSuficiente(
                stockDiarioDisponible(existenciaDiaria),
                cantidad,
                "Stock diario insuficiente para registrar la perdida de vasos");

        existenciaDiaria.setCantidadPerdida(
                valor(existenciaDiaria.getCantidadPerdida()) + cantidad);
        recalcularCantidadFinalTeorica(existenciaDiaria);
        registrarMovimiento(
                item,
                perdida.getCajaDiaria(),
                TIPO_STOCK_DIARIO,
                MOVIMIENTO_PERDIDA,
                cantidad,
                SENTIDO_SALIDA,
                REFERENCIA_PERDIDAS,
                perdida.getIdPerdidaInventario(),
                perdida.getMotivo(),
                usuarioRegistro);
    }

    @Transactional
    public void restaurarPerdidaVasos(
            PerdidaInventario perdida,
            Usuario usuarioRegistro) {
        ItemInventario item = perdida.getItemInventario();
        ExistenciaInventarioDiario existenciaDiaria = obtenerExistenciaDiariaBloqueada(
                perdida.getCajaDiaria(),
                item,
                "No existe stock diario para restaurar la perdida anulada");
        int cantidad = perdida.getCantidad();
        if (valor(existenciaDiaria.getCantidadPerdida()) < cantidad) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "La anulacion dejaria cantidad_perdida negativa");
        }

        existenciaDiaria.setCantidadPerdida(
                existenciaDiaria.getCantidadPerdida() - cantidad);
        recalcularCantidadFinalTeorica(existenciaDiaria);
        registrarMovimiento(
                item,
                perdida.getCajaDiaria(),
                TIPO_STOCK_DIARIO,
                MOVIMIENTO_ANULACION_PERDIDA,
                cantidad,
                SENTIDO_ENTRADA,
                REFERENCIA_PERDIDAS,
                perdida.getIdPerdidaInventario(),
                "Restauracion por anulacion de perdida registrada por error",
                usuarioRegistro);
    }

    private CajaDiaria obtenerCajaAbierta() {
        return cajaDiariaRepository.findPrimeraPorEstadoCaja(ESTADO_CAJA_ABIERTA)
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "No existe caja diaria abierta para operar inventario"));
    }

    private AjusteInventario obtenerAjustePendienteBloqueado(UUID idAjusteInventario) {
        AjusteInventario ajuste = ajusteInventarioRepository.findByIdForUpdate(idAjusteInventario)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ajuste de inventario no encontrado"));
        if (!ESTADO_AJUSTE_PENDIENTE.equals(ajuste.getEstadoAprobacion())) {
            throw new ApiException(HttpStatus.CONFLICT, "El ajuste de inventario ya fue resuelto");
        }
        return ajuste;
    }

    private void aplicarAjusteInventario(
            AjusteInventario ajuste,
            Usuario usuarioAprobador,
            String observacionAprobacion) {
        if (TIPO_STOCK_GENERAL.equals(ajuste.getTipoStock())) {
            aplicarAjusteStockGeneral(ajuste, usuarioAprobador, observacionAprobacion);
            return;
        }
        if (TIPO_STOCK_DIARIO.equals(ajuste.getTipoStock())) {
            aplicarTransferenciaStockDiario(ajuste, usuarioAprobador, observacionAprobacion);
            return;
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, "Tipo de stock no soportado para ajuste");
    }

    private void aplicarAjusteStockGeneral(
            AjusteInventario ajuste,
            Usuario usuarioAprobador,
            String observacionAprobacion) {
        ExistenciaInventarioGeneral existenciaGeneral = obtenerExistenciaGeneralBloqueada(ajuste.getItemInventario());
        int cantidadActual = valor(existenciaGeneral.getCantidadActual());
        int cantidadAjuste = ajuste.getCantidadAjuste();
        int nuevaCantidad = SENTIDO_ENTRADA.equals(ajuste.getSentidoAjuste())
                ? cantidadActual + cantidadAjuste
                : cantidadActual - cantidadAjuste;
        if (nuevaCantidad < 0) {
            throw new ApiException(HttpStatus.CONFLICT, "El ajuste dejaria stock general negativo");
        }

        existenciaGeneral.setCantidadActual(nuevaCantidad);
        registrarMovimiento(
                ajuste.getItemInventario(),
                ajuste.getCajaDiaria(),
                TIPO_STOCK_GENERAL,
                MOVIMIENTO_AJUSTE,
                cantidadAjuste,
                ajuste.getSentidoAjuste(),
                REFERENCIA_AJUSTES,
                ajuste.getIdAjusteInventario(),
                observacionMovimientoAjuste(ajuste, observacionAprobacion),
                usuarioAprobador);
    }

    private void aplicarTransferenciaStockDiario(
            AjusteInventario ajuste,
            Usuario usuarioAprobador,
            String observacionAprobacion) {
        ItemInventario item = ajuste.getItemInventario();
        validarItemVasoConPaquetes(item);

        CajaDiaria cajaDiaria = ajuste.getCajaDiaria();
        CajaDiaria cajaAbierta = obtenerCajaAbierta();
        if (cajaDiaria == null
                || !cajaAbierta.getIdCajaDiaria().equals(cajaDiaria.getIdCajaDiaria())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "La caja asociada al reabastecimiento del stock diario ya no esta abierta");
        }

        int cantidad = ajuste.getCantidadAjuste();
        ExistenciaInventarioGeneral existenciaGeneral = obtenerExistenciaGeneralBloqueada(item);
        ExistenciaInventarioDiario existenciaDiaria = obtenerExistenciaDiariaBloqueada(
                cajaDiaria,
                item,
                "No existe stock diario para realizar la transferencia");

        boolean entradaAlDiario = SENTIDO_ENTRADA.equals(ajuste.getSentidoAjuste());
        if (entradaAlDiario) {
            validarStockSuficiente(
                    existenciaGeneral.getCantidadActual(),
                    cantidad,
                    "Stock general insuficiente para reabastecer el stock diario");
            existenciaGeneral.setCantidadActual(existenciaGeneral.getCantidadActual() - cantidad);
            existenciaDiaria.setCantidadAjustada(valor(existenciaDiaria.getCantidadAjustada()) + cantidad);
        } else {
            validarStockSuficiente(
                    stockDiarioDisponible(existenciaDiaria),
                    cantidad,
                    "Stock diario insuficiente para devolver vasos al stock general");
            existenciaDiaria.setCantidadAjustada(valor(existenciaDiaria.getCantidadAjustada()) - cantidad);
            existenciaGeneral.setCantidadActual(existenciaGeneral.getCantidadActual() + cantidad);
        }
        recalcularCantidadFinalTeorica(existenciaDiaria);

        String observacion = observacionMovimientoAjuste(ajuste, observacionAprobacion);
        registrarMovimiento(
                item,
                cajaDiaria,
                TIPO_STOCK_GENERAL,
                MOVIMIENTO_AJUSTE,
                cantidad,
                entradaAlDiario ? SENTIDO_SALIDA : SENTIDO_ENTRADA,
                REFERENCIA_AJUSTES,
                ajuste.getIdAjusteInventario(),
                entradaAlDiario
                        ? "Salida de stock general por reabastecimiento del stock diario: " + observacion
                        : "Reintegro al stock general desde el stock diario: " + observacion,
                usuarioAprobador);
        registrarMovimiento(
                item,
                cajaDiaria,
                TIPO_STOCK_DIARIO,
                MOVIMIENTO_AJUSTE,
                cantidad,
                entradaAlDiario ? SENTIDO_ENTRADA : SENTIDO_SALIDA,
                REFERENCIA_AJUSTES,
                ajuste.getIdAjusteInventario(),
                entradaAlDiario
                        ? "Ingreso por reabastecimiento del stock diario: " + observacion
                        : "Devolucion del stock diario al stock general: " + observacion,
                usuarioAprobador);
    }

    private void validarCajaExiste(UUID idCajaDiaria) {
        if (!cajaDiariaRepository.existsById(idCajaDiaria)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "No existe caja diaria para consultar inventario");
        }
    }

    private Usuario obtenerUsuario(UUID idUsuario) {
        return usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Usuario autenticado no encontrado"));
    }

    private ItemInventario obtenerItemActivo(UUID idItemInventario) {
        ItemInventario item = itemInventarioRepository.findById(idItemInventario)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Item de inventario no encontrado"));
        if (!ESTADO_ACTIVO.equals(item.getEstado())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Item de inventario inactivo");
        }
        return item;
    }

    private ItemInventario obtenerVasoPorTamano(UUID idTamanoVaso) {
        return itemInventarioRepository.findVasoActivoPorTamano(idTamanoVaso)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "No existe item de inventario activo para el tamano de vaso vendido"));
    }

    private void validarItemVasoConPaquetes(ItemInventario item) {
        if (!esItemVasoConPaquetes(item)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "La operacion solo admite vasos configurados para manejo por paquetes");
        }
    }

    private boolean esItemVasoConPaquetes(ItemInventario item) {
        return TIPO_CONTROL_AUTOMATICO.equals(item.getTipoControl())
                && item.isManejaPaquetes()
                && item.getTamanoVaso() != null
                && item.getUnidadesPorPaquete() != null
                && item.getUnidadesPorPaquete() > 0;
    }

    private void validarRolGestionInventario(PrincipalUsuario principalUsuario) {
        String rol = principalUsuario.nombreRol().toLowerCase(Locale.ROOT);
        if (!"administrador".equals(rol) && !"gerente".equals(rol)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo administrador o gerente puede modificar inventario operativo");
        }
    }

    private void validarRolSolicitudAjuste(PrincipalUsuario principalUsuario) {
        String rol = principalUsuario.nombreRol().toLowerCase(Locale.ROOT);
        if (!"administrador".equals(rol) && !"gerente".equals(rol)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo administrador o gerente puede solicitar ajustes de inventario");
        }
    }

    private void validarRolConsultaAjustes(PrincipalUsuario principalUsuario) {
        String rol = principalUsuario.nombreRol().toLowerCase(Locale.ROOT);
        if (!"administrador".equals(rol) && !"gerente".equals(rol)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo administrador o gerente puede consultar ajustes de inventario");
        }
    }

    private void validarRolConsultaInventario(PrincipalUsuario principalUsuario) {
        String rol = principalUsuario.nombreRol().toLowerCase(Locale.ROOT);
        if (!"administrador".equals(rol) && !"gerente".equals(rol)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo administrador o gerente puede consultar inventario");
        }
    }

    private void validarRolAprobacionAjuste(PrincipalUsuario principalUsuario) {
        String rol = principalUsuario.nombreRol().toLowerCase(Locale.ROOT);
        if (!"gerente".equals(rol)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo gerente puede aprobar o rechazar ajustes de inventario");
        }
    }

    private boolean esGerente(PrincipalUsuario principalUsuario) {
        return "gerente".equals(principalUsuario.nombreRol().toLowerCase(Locale.ROOT));
    }

    private String normalizarTipoStock(String tipoStock) {
        String valorNormalizado = normalizarRequerido(tipoStock, "tipoStock");
        if (!TIPO_STOCK_GENERAL.equals(valorNormalizado)
                && !TIPO_STOCK_DIARIO.equals(valorNormalizado)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "tipoStock debe ser general o diario");
        }
        return valorNormalizado;
    }

    private CajaDiaria validarReabastecimientoStockDiario(
            ItemInventario item,
            String sentidoAjuste,
            int cantidad) {
        validarItemVasoConPaquetes(item);

        CajaDiaria cajaDiaria = obtenerCajaAbierta();
        ExistenciaInventarioGeneral existenciaGeneral = obtenerExistenciaGeneralBloqueada(item);
        ExistenciaInventarioDiario existenciaDiaria = obtenerExistenciaDiariaBloqueada(
                cajaDiaria,
                item,
                "No existe stock diario para realizar la transferencia");
        if (SENTIDO_ENTRADA.equals(sentidoAjuste)) {
            validarStockSuficiente(
                    existenciaGeneral.getCantidadActual(),
                    cantidad,
                    "Stock general insuficiente para reabastecer el stock diario");
        } else {
            validarStockSuficiente(
                    stockDiarioDisponible(existenciaDiaria),
                    cantidad,
                    "Stock diario insuficiente para devolver vasos al stock general");
        }
        return cajaDiaria;
    }

    private String normalizarSentidoAjuste(String sentidoAjuste) {
        String valorNormalizado = normalizarRequerido(sentidoAjuste, "sentidoAjuste");
        if (!SENTIDO_ENTRADA.equals(valorNormalizado) && !SENTIDO_SALIDA.equals(valorNormalizado)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "sentidoAjuste debe ser entrada o salida");
        }
        return valorNormalizado;
    }

    private String normalizarEstadoAprobacionFiltro(String estadoAprobacion) {
        if (estadoAprobacion == null || estadoAprobacion.isBlank()) {
            return null;
        }
        String valorNormalizado = estadoAprobacion.trim().toLowerCase(Locale.ROOT);
        if (!ESTADO_AJUSTE_PENDIENTE.equals(valorNormalizado)
                && !ESTADO_AJUSTE_APROBADO.equals(valorNormalizado)
                && !ESTADO_AJUSTE_RECHAZADO.equals(valorNormalizado)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "estadoAprobacion debe ser pendiente, aprobado o rechazado");
        }
        return valorNormalizado;
    }

    private String normalizarRequerido(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, campo + " es obligatorio");
        }
        return valor.trim().toLowerCase(Locale.ROOT);
    }

    private ExistenciaInventarioGeneral obtenerExistenciaGeneralBloqueada(ItemInventario item) {
        return existenciaGeneralRepository.findByItemForUpdate(item.getIdItemInventario())
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "No existe existencia general para el item de inventario"));
    }

    private ExistenciaInventarioDiario obtenerOCrearExistenciaDiaria(CajaDiaria cajaDiaria, ItemInventario item) {
        return existenciaDiarioRepository.findByCajaAndItemForUpdate(
                        cajaDiaria.getIdCajaDiaria(),
                        item.getIdItemInventario())
                .orElseGet(() -> nuevaExistenciaDiaria(cajaDiaria, item));
    }

    private ExistenciaInventarioDiario obtenerExistenciaDiariaBloqueada(CajaDiaria cajaDiaria, ItemInventario item, String mensaje) {
        return existenciaDiarioRepository.findByCajaAndItemForUpdate(
                        cajaDiaria.getIdCajaDiaria(),
                        item.getIdItemInventario())
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, mensaje));
    }

    private ExistenciaInventarioDiario nuevaExistenciaDiaria(CajaDiaria cajaDiaria, ItemInventario item) {
        return nuevaExistenciaDiaria(cajaDiaria, item, 0);
    }

    private ExistenciaInventarioDiario nuevaExistenciaDiaria(CajaDiaria cajaDiaria, ItemInventario item, int cantidadInicial) {
        ExistenciaInventarioDiario existenciaDiaria = new ExistenciaInventarioDiario();
        existenciaDiaria.setCajaDiaria(cajaDiaria);
        existenciaDiaria.setItemInventario(item);
        existenciaDiaria.setCantidadInicial(cantidadInicial);
        existenciaDiaria.setCantidadIngresada(0);
        existenciaDiaria.setCantidadVendida(0);
        existenciaDiaria.setCantidadPerdida(0);
        existenciaDiaria.setCantidadCortesia(0);
        existenciaDiaria.setCantidadAjustada(0);
        existenciaDiaria.setCantidadFinalTeorica(cantidadInicial);
        return existenciaDiarioRepository.saveAndFlush(existenciaDiaria);
    }

    private int remanenteDiarioAnterior(CajaDiaria cajaDiaria, ItemInventario item) {
        return existenciaDiarioRepository
                .findUltimaAnteriorPorItem(item.getIdItemInventario(), cajaDiaria.getFechaOperacion())
                .map((existenciaAnterior) -> existenciaAnterior.getCantidadFinalContada() != null
                        ? existenciaAnterior.getCantidadFinalContada()
                        : valor(existenciaAnterior.getCantidadFinalTeorica()))
                .orElse(0);
    }

    private void validarStockSuficiente(Integer cantidadActual, int cantidadRequerida, String mensaje) {
        if (valor(cantidadActual) < cantidadRequerida) {
            throw new ApiException(HttpStatus.CONFLICT, mensaje);
        }
    }

    private int stockDiarioDisponible(ExistenciaInventarioDiario existenciaDiaria) {
        return valor(existenciaDiaria.getCantidadFinalTeorica());
    }

    private void recalcularCantidadFinalTeorica(ExistenciaInventarioDiario existenciaDiaria) {
        int cantidadFinal = valor(existenciaDiaria.getCantidadInicial())
                + valor(existenciaDiaria.getCantidadIngresada())
                - valor(existenciaDiaria.getCantidadVendida())
                - valor(existenciaDiaria.getCantidadPerdida())
                - valor(existenciaDiaria.getCantidadCortesia())
                + valor(existenciaDiaria.getCantidadAjustada());
        if (cantidadFinal < 0) {
            throw new ApiException(HttpStatus.CONFLICT, "El movimiento dejaria stock diario negativo");
        }
        existenciaDiaria.setCantidadFinalTeorica(cantidadFinal);
    }

    private MovimientoInventario registrarMovimiento(
            ItemInventario item,
            CajaDiaria cajaDiaria,
            String tipoStock,
            String tipoMovimiento,
            int cantidad,
            String sentido,
            String referenciaOrigen,
            UUID idReferenciaOrigen,
            String observacion,
            Usuario usuario) {
        if (referenciaOrigen == null || referenciaOrigen.isBlank() || idReferenciaOrigen == null) {
            throw new IllegalStateException("Los movimientos de inventario requieren referencia de origen");
        }
        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setItemInventario(item);
        movimiento.setCajaDiaria(cajaDiaria);
        movimiento.setTipoStock(tipoStock);
        movimiento.setTipoMovimiento(tipoMovimiento);
        movimiento.setCantidad(cantidad);
        movimiento.setSentidoMovimiento(sentido);
        movimiento.setReferenciaOrigen(referenciaOrigen);
        movimiento.setIdReferenciaOrigen(idReferenciaOrigen);
        movimiento.setObservacion(normalizarObservacion(observacion));
        movimiento.setUsuarioRegistro(usuario);
        movimiento.setFechaMovimiento(OffsetDateTime.now());
        return movimientoInventarioRepository.save(movimiento);
    }

    private String normalizarObservacion(String observacion) {
        if (observacion == null || observacion.isBlank()) {
            return null;
        }
        return observacion.trim();
    }

    private String observacionMovimientoAjuste(AjusteInventario ajuste, String observacionAprobacion) {
        if (observacionAprobacion != null && !observacionAprobacion.isBlank()) {
            return observacionAprobacion.trim();
        }
        return ajuste.getMotivoAjuste();
    }

    private int valor(Integer valor) {
        return valor == null ? 0 : valor;
    }

    private Map<String, Object> snapshotAjuste(AjusteInventario ajuste) {
        Usuario usuarioAprobador = ajuste.getUsuarioAprobador();
        return valores(
                "id_ajuste_inventario", ajuste.getIdAjusteInventario(),
                "id_item_inventario", ajuste.getItemInventario().getIdItemInventario(),
                "id_caja_diaria", ajuste.getCajaDiaria() == null ? null : ajuste.getCajaDiaria().getIdCajaDiaria(),
                "tipo_stock", ajuste.getTipoStock(),
                "cantidad_ajuste", ajuste.getCantidadAjuste(),
                "sentido_ajuste", ajuste.getSentidoAjuste(),
                "motivo_ajuste", ajuste.getMotivoAjuste(),
                "estado_aprobacion", ajuste.getEstadoAprobacion(),
                "id_usuario_solicitante", ajuste.getUsuarioSolicitante().getIdUsuario(),
                "id_usuario_aprobador", usuarioAprobador == null ? null : usuarioAprobador.getIdUsuario(),
                "fecha_solicitud", ajuste.getFechaSolicitud(),
                "fecha_aprobacion", ajuste.getFechaAprobacion(),
                "observacion_aprobacion", ajuste.getObservacionAprobacion());
    }

    private ExistenciaInventarioGeneralResponse toResponse(ExistenciaInventarioGeneral existencia) {
        ItemInventario item = existencia.getItemInventario();
        TamanoVaso tamanoVaso = item.getTamanoVaso();
        return new ExistenciaInventarioGeneralResponse(
                existencia.getIdExistenciaGeneral(),
                item.getIdItemInventario(),
                item.getNombreItem(),
                item.getTipoControl(),
                tamanoVaso == null ? null : tamanoVaso.getIdTamanoVaso(),
                tamanoVaso == null ? null : tamanoVaso.getOnzas(),
                existencia.getCantidadActual(),
                existencia.getFechaActualizacion());
    }

    private AjusteInventarioResponse toResponse(AjusteInventario ajuste) {
        Usuario usuarioAprobador = ajuste.getUsuarioAprobador();
        return new AjusteInventarioResponse(
                ajuste.getIdAjusteInventario(),
                ajuste.getItemInventario().getIdItemInventario(),
                ajuste.getItemInventario().getNombreItem(),
                ajuste.getCajaDiaria() == null ? null : ajuste.getCajaDiaria().getIdCajaDiaria(),
                ajuste.getTipoStock(),
                ajuste.getCantidadAjuste(),
                ajuste.getSentidoAjuste(),
                ajuste.getMotivoAjuste(),
                ajuste.getEstadoAprobacion(),
                ajuste.getUsuarioSolicitante().getIdUsuario(),
                ajuste.getUsuarioSolicitante().getNombreUsuario(),
                usuarioAprobador == null ? null : usuarioAprobador.getIdUsuario(),
                usuarioAprobador == null ? null : usuarioAprobador.getNombreUsuario(),
                ajuste.getFechaSolicitud(),
                ajuste.getFechaAprobacion(),
                ajuste.getObservacionAprobacion());
    }

    private ExistenciaInventarioDiarioResponse toResponse(ExistenciaInventarioDiario existencia) {
        ItemInventario item = existencia.getItemInventario();
        TamanoVaso tamanoVaso = item.getTamanoVaso();
        return new ExistenciaInventarioDiarioResponse(
                existencia.getIdExistenciaDiaria(),
                existencia.getCajaDiaria().getIdCajaDiaria(),
                item.getIdItemInventario(),
                item.getNombreItem(),
                tamanoVaso == null ? null : tamanoVaso.getIdTamanoVaso(),
                tamanoVaso == null ? null : tamanoVaso.getOnzas(),
                existencia.getCantidadInicial(),
                existencia.getCantidadIngresada(),
                existencia.getCantidadVendida(),
                existencia.getCantidadPerdida(),
                existencia.getCantidadCortesia(),
                existencia.getCantidadAjustada(),
                existencia.getCantidadFinalTeorica(),
                existencia.getCantidadFinalContada(),
                existencia.getDiferencia());
    }

    private MovimientoInventarioResponse toResponse(MovimientoInventario movimiento) {
        return new MovimientoInventarioResponse(
                movimiento.getIdMovimientoInventario(),
                movimiento.getItemInventario().getIdItemInventario(),
                movimiento.getItemInventario().getNombreItem(),
                movimiento.getCajaDiaria() == null ? null : movimiento.getCajaDiaria().getIdCajaDiaria(),
                movimiento.getTipoStock(),
                movimiento.getTipoMovimiento(),
                movimiento.getCantidad(),
                movimiento.getSentidoMovimiento(),
                movimiento.getReferenciaOrigen(),
                movimiento.getIdReferenciaOrigen(),
                movimiento.getObservacion(),
                movimiento.getUsuarioRegistro().getIdUsuario(),
                movimiento.getUsuarioRegistro().getNombreUsuario(),
                movimiento.getFechaMovimiento());
    }

    private PaqueteVasosAbiertoResponse toResponse(PaqueteVasosAbierto paquete) {
        return new PaqueteVasosAbiertoResponse(
                paquete.getIdPaqueteVasosAbierto(),
                paquete.getCajaDiaria().getIdCajaDiaria(),
                paquete.getItemInventario().getIdItemInventario(),
                paquete.getItemInventario().getNombreItem(),
                paquete.getCantidadPaquetes(),
                paquete.getUnidadesPorPaquete(),
                paquete.getUnidadesGeneradas(),
                paquete.getUnidadesRotas(),
                paquete.getUnidadesDisponibles(),
                paquete.getUsuarioRegistro().getIdUsuario(),
                paquete.getUsuarioRegistro().getNombreUsuario(),
                paquete.getFechaRegistro());
    }

    private ConsumoDiarioInventarioResponse toResponse(ConsumoDiarioInventario consumo) {
        return new ConsumoDiarioInventarioResponse(
                consumo.getIdConsumoDiarioInventario(),
                consumo.getCajaDiaria().getIdCajaDiaria(),
                consumo.getItemInventario().getIdItemInventario(),
                consumo.getItemInventario().getNombreItem(),
                consumo.getCantidadConsumida(),
                consumo.getUsuarioRegistro().getIdUsuario(),
                consumo.getUsuarioRegistro().getNombreUsuario(),
                consumo.getFechaRegistro(),
                consumo.getObservacion());
    }
}
