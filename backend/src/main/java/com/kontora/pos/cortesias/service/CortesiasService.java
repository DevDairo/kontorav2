package com.kontora.pos.cortesias.service;

import com.kontora.pos.auditoria.service.AuditoriaService;
import com.kontora.pos.caja.domain.CajaDiaria;
import com.kontora.pos.caja.repository.CajaDiariaRepository;
import com.kontora.pos.catalogos.domain.TamanoVaso;
import com.kontora.pos.catalogos.domain.TipoGranizado;
import com.kontora.pos.catalogos.repository.TamanoVasoRepository;
import com.kontora.pos.catalogos.repository.TipoGranizadoRepository;
import com.kontora.pos.common.exception.ApiException;
import com.kontora.pos.common.security.PrincipalUsuario;
import com.kontora.pos.cortesias.domain.Cortesia;
import com.kontora.pos.cortesias.domain.DetalleCortesia;
import com.kontora.pos.cortesias.dto.AnularCortesiaRequest;
import com.kontora.pos.cortesias.dto.CortesiaResponse;
import com.kontora.pos.cortesias.dto.DetalleCortesiaResponse;
import com.kontora.pos.cortesias.dto.RegistrarCortesiaRequest;
import com.kontora.pos.cortesias.dto.RegistrarDetalleCortesiaRequest;
import com.kontora.pos.cortesias.repository.CortesiaRepository;
import com.kontora.pos.cortesias.repository.DetalleCortesiaRepository;
import com.kontora.pos.inventario.service.InventarioService;
import com.kontora.pos.usuarios.domain.Usuario;
import com.kontora.pos.usuarios.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.kontora.pos.common.audit.AuditoriaValores.valores;

@Service
public class CortesiasService {

    private static final String ESTADO_ACTIVO = "activo";
    private static final String ESTADO_CAJA_ABIERTA = "abierta";
    private static final String ESTADO_REGISTRADA = "registrada";
    private static final String ESTADO_ANULADA = "anulada";
    private static final String BENEFICIARIO_TRABAJADOR = "trabajador";
    private static final String BENEFICIARIO_OTRO = "otro";

    private final CajaDiariaRepository cajaDiariaRepository;
    private final UsuarioRepository usuarioRepository;
    private final TipoGranizadoRepository tipoGranizadoRepository;
    private final TamanoVasoRepository tamanoVasoRepository;
    private final CortesiaRepository cortesiaRepository;
    private final DetalleCortesiaRepository detalleCortesiaRepository;
    private final InventarioService inventarioService;
    private final AuditoriaService auditoriaService;

    public CortesiasService(
            CajaDiariaRepository cajaDiariaRepository,
            UsuarioRepository usuarioRepository,
            TipoGranizadoRepository tipoGranizadoRepository,
            TamanoVasoRepository tamanoVasoRepository,
            CortesiaRepository cortesiaRepository,
            DetalleCortesiaRepository detalleCortesiaRepository,
            InventarioService inventarioService,
            AuditoriaService auditoriaService) {
        this.cajaDiariaRepository = cajaDiariaRepository;
        this.usuarioRepository = usuarioRepository;
        this.tipoGranizadoRepository = tipoGranizadoRepository;
        this.tamanoVasoRepository = tamanoVasoRepository;
        this.cortesiaRepository = cortesiaRepository;
        this.detalleCortesiaRepository = detalleCortesiaRepository;
        this.inventarioService = inventarioService;
        this.auditoriaService = auditoriaService;
    }

    @Transactional
    public CortesiaResponse registrar(
            RegistrarCortesiaRequest request,
            PrincipalUsuario principalUsuario) {
        validarRolGestion(principalUsuario);
        if (!Boolean.TRUE.equals(request.confirmaRegistro())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Debe confirmar el registro de la cortesia");
        }

        CajaDiaria cajaDiaria = obtenerCajaAbierta();
        Usuario usuarioRegistro = obtenerUsuario(principalUsuario.idUsuario());
        String tipoBeneficiario = normalizarTipoBeneficiario(request.tipoBeneficiario());
        Usuario usuarioBeneficiario = resolverBeneficiario(
                tipoBeneficiario,
                request.idUsuarioBeneficiario());
        String referenciaOtro = BENEFICIARIO_OTRO.equals(tipoBeneficiario)
                ? normalizarOpcional(request.referenciaOtro())
                : null;
        String motivoOtro = BENEFICIARIO_OTRO.equals(tipoBeneficiario)
                ? normalizarRequerido(request.motivoOtro(), "motivoOtro")
                : null;

        Cortesia cortesia = new Cortesia();
        cortesia.setCajaDiaria(cajaDiaria);
        cortesia.setUsuarioRegistro(usuarioRegistro);
        cortesia.setTipoBeneficiario(tipoBeneficiario);
        cortesia.setUsuarioBeneficiario(usuarioBeneficiario);
        cortesia.setReferenciaOtro(referenciaOtro);
        cortesia.setMotivoOtro(motivoOtro);
        cortesia.setFechaRegistro(OffsetDateTime.now());
        cortesia.setEstado(ESTADO_REGISTRADA);
        Cortesia cortesiaGuardada = cortesiaRepository.saveAndFlush(cortesia);

        List<DetalleCortesia> detalles = crearDetalles(cortesiaGuardada, request.detalles());
        detalleCortesiaRepository.saveAllAndFlush(detalles);
        inventarioService.descontarVasosPorCortesia(cortesiaGuardada, detalles, usuarioRegistro);

        auditoriaService.registrar(
                usuarioRegistro,
                "cortesias",
                cortesiaGuardada.getIdCortesia(),
                "crear",
                null,
                snapshot(cortesiaGuardada, detalles),
                "Registro de cortesia con salida de stock diario");
        return toResponse(cortesiaGuardada, detalles);
    }

    @Transactional(readOnly = true)
    public List<CortesiaResponse> consultarCajaAbierta(PrincipalUsuario principalUsuario) {
        validarRolGestion(principalUsuario);
        CajaDiaria cajaDiaria = obtenerCajaAbierta();
        return cortesiaRepository
                .findByCajaDiaria_IdCajaDiariaOrderByFechaRegistroDesc(cajaDiaria.getIdCajaDiaria())
                .stream()
                .map(cortesia -> toResponse(
                        cortesia,
                        detalleCortesiaRepository
                                .findByCortesia_IdCortesiaOrderByIdDetalleCortesia(cortesia.getIdCortesia())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CortesiaResponse> consultarPeriodo(
            LocalDate fechaInicio,
            LocalDate fechaFin,
            PrincipalUsuario principalUsuario) {
        validarRolGestion(principalUsuario);
        LocalDate fin = validarPeriodo(fechaInicio, fechaFin);
        return cortesiaRepository.findParaConsulta(fechaInicio, fin)
                .stream()
                .map(cortesia -> toResponse(
                        cortesia,
                        detalleCortesiaRepository
                                .findByCortesia_IdCortesiaOrderByIdDetalleCortesia(
                                        cortesia.getIdCortesia())))
                .toList();
    }

    @Transactional
    public CortesiaResponse anular(
            UUID idCortesia,
            AnularCortesiaRequest request,
            PrincipalUsuario principalUsuario) {
        validarRolGestion(principalUsuario);
        if (!Boolean.TRUE.equals(request.confirmaNoEntregada())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Debe confirmar que la cortesia no fue entregada ni consumida");
        }

        Usuario usuarioAnulacion = obtenerUsuario(principalUsuario.idUsuario());
        Cortesia cortesia = cortesiaRepository.findByIdForUpdate(idCortesia)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Cortesia no encontrada"));
        validarCajaAbierta(cortesia.getCajaDiaria());
        if (!ESTADO_REGISTRADA.equals(cortesia.getEstado())) {
            throw new ApiException(HttpStatus.CONFLICT, "La cortesia ya fue anulada");
        }

        List<DetalleCortesia> detalles = detalleCortesiaRepository
                .findByCortesia_IdCortesiaOrderByIdDetalleCortesia(idCortesia);
        Map<String, Object> valorAnterior = snapshot(cortesia, detalles);
        inventarioService.restaurarVasosPorAnulacionCortesia(
                cortesia,
                detalles,
                usuarioAnulacion);

        cortesia.setEstado(ESTADO_ANULADA);
        cortesia.setUsuarioAnulacion(usuarioAnulacion);
        cortesia.setFechaAnulacion(OffsetDateTime.now());
        cortesia.setMotivoAnulacion(
                normalizarRequerido(request.motivoAnulacion(), "motivoAnulacion"));
        Cortesia cortesiaGuardada = cortesiaRepository.saveAndFlush(cortesia);

        auditoriaService.registrar(
                usuarioAnulacion,
                "cortesias",
                cortesiaGuardada.getIdCortesia(),
                "anular",
                valorAnterior,
                snapshot(cortesiaGuardada, detalles),
                "Anulacion de cortesia no entregada y restauracion del stock diario");
        return toResponse(cortesiaGuardada, detalles);
    }

    private List<DetalleCortesia> crearDetalles(
            Cortesia cortesia,
            List<RegistrarDetalleCortesiaRequest> solicitudes) {
        Set<ClaveDetalle> claves = new HashSet<>();
        return solicitudes.stream()
                .map(solicitud -> {
                    ClaveDetalle clave = new ClaveDetalle(
                            solicitud.idTipoGranizado(),
                            solicitud.idTamanoVaso());
                    if (!claves.add(clave)) {
                        throw new ApiException(
                                HttpStatus.BAD_REQUEST,
                                "No se puede repetir el mismo tipo de granizado y tamano en una cortesia");
                    }

                    TipoGranizado tipoGranizado = tipoGranizadoRepository
                            .findById(solicitud.idTipoGranizado())
                            .orElseThrow(() -> new ApiException(
                                    HttpStatus.BAD_REQUEST,
                                    "Tipo de granizado no encontrado"));
                    TamanoVaso tamanoVaso = tamanoVasoRepository
                            .findById(solicitud.idTamanoVaso())
                            .orElseThrow(() -> new ApiException(
                                    HttpStatus.BAD_REQUEST,
                                    "Tamano de vaso no encontrado"));
                    if (!ESTADO_ACTIVO.equals(tipoGranizado.getEstado())
                            || !ESTADO_ACTIVO.equals(tamanoVaso.getEstado())) {
                        throw new ApiException(
                                HttpStatus.BAD_REQUEST,
                                "La cortesia solo admite tipos de granizado y tamanos activos");
                    }

                    DetalleCortesia detalle = new DetalleCortesia();
                    detalle.setCortesia(cortesia);
                    detalle.setTipoGranizado(tipoGranizado);
                    detalle.setTamanoVaso(tamanoVaso);
                    detalle.setCantidad(solicitud.cantidad());
                    return detalle;
                })
                .toList();
    }

    private Usuario resolverBeneficiario(String tipoBeneficiario, UUID idUsuarioBeneficiario) {
        if (BENEFICIARIO_OTRO.equals(tipoBeneficiario)) {
            if (idUsuarioBeneficiario != null) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "idUsuarioBeneficiario no aplica cuando el beneficiario es otro");
            }
            return null;
        }
        if (idUsuarioBeneficiario == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Debe seleccionar el trabajador beneficiario");
        }
        Usuario beneficiario = usuarioRepository.findById(idUsuarioBeneficiario)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "Trabajador beneficiario no encontrado"));
        if (!ESTADO_ACTIVO.equals(beneficiario.getEstado())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "El trabajador beneficiario esta inactivo");
        }
        return beneficiario;
    }

    private CajaDiaria obtenerCajaAbierta() {
        return cajaDiariaRepository.findPrimeraPorEstadoCaja(ESTADO_CAJA_ABIERTA)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.CONFLICT,
                        "No existe caja diaria abierta para registrar cortesias"));
    }

    private void validarCajaAbierta(CajaDiaria cajaDiaria) {
        CajaDiaria cajaAbierta = obtenerCajaAbierta();
        if (!cajaAbierta.getIdCajaDiaria().equals(cajaDiaria.getIdCajaDiaria())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "La cortesia solo puede anularse mientras su misma caja esta abierta");
        }
    }

    private Usuario obtenerUsuario(UUID idUsuario) {
        return usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "Usuario autenticado no encontrado"));
    }

    private void validarRolGestion(PrincipalUsuario principalUsuario) {
        String rol = principalUsuario.nombreRol().toLowerCase(Locale.ROOT);
        if (!"administrador".equals(rol) && !"gerente".equals(rol)) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "Solo administrador o gerente puede gestionar cortesias");
        }
    }

    private LocalDate validarPeriodo(LocalDate fechaInicio, LocalDate fechaFin) {
        if (fechaInicio == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "fechaInicio es obligatoria");
        }
        LocalDate fin = fechaFin == null ? fechaInicio : fechaFin;
        if (fin.isBefore(fechaInicio)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "fechaFin no puede ser anterior a fechaInicio");
        }
        return fin;
    }

    private String normalizarTipoBeneficiario(String valor) {
        String normalizado = normalizarRequerido(valor, "tipoBeneficiario")
                .toLowerCase(Locale.ROOT);
        if (!BENEFICIARIO_TRABAJADOR.equals(normalizado)
                && !BENEFICIARIO_OTRO.equals(normalizado)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "tipoBeneficiario debe ser trabajador u otro");
        }
        return normalizado;
    }

    private String normalizarRequerido(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, campo + " es obligatorio");
        }
        return valor.trim();
    }

    private String normalizarOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private Map<String, Object> snapshot(Cortesia cortesia, List<DetalleCortesia> detalles) {
        Usuario beneficiario = cortesia.getUsuarioBeneficiario();
        Usuario usuarioAnulacion = cortesia.getUsuarioAnulacion();
        return valores(
                "id_cortesia", cortesia.getIdCortesia(),
                "id_caja_diaria", cortesia.getCajaDiaria().getIdCajaDiaria(),
                "id_usuario_registro", cortesia.getUsuarioRegistro().getIdUsuario(),
                "tipo_beneficiario", cortesia.getTipoBeneficiario(),
                "id_usuario_beneficiario",
                beneficiario == null ? null : beneficiario.getIdUsuario(),
                "referencia_otro", cortesia.getReferenciaOtro(),
                "motivo_otro", cortesia.getMotivoOtro(),
                "fecha_registro", cortesia.getFechaRegistro(),
                "estado", cortesia.getEstado(),
                "id_usuario_anulacion",
                usuarioAnulacion == null ? null : usuarioAnulacion.getIdUsuario(),
                "fecha_anulacion", cortesia.getFechaAnulacion(),
                "motivo_anulacion", cortesia.getMotivoAnulacion(),
                "detalles", detalles.stream().map(this::snapshotDetalle).toList());
    }

    private Map<String, Object> snapshotDetalle(DetalleCortesia detalle) {
        return valores(
                "id_detalle_cortesia", detalle.getIdDetalleCortesia(),
                "id_tipo_granizado", detalle.getTipoGranizado().getIdTipoGranizado(),
                "id_tamano_vaso", detalle.getTamanoVaso().getIdTamanoVaso(),
                "cantidad", detalle.getCantidad());
    }

    private CortesiaResponse toResponse(Cortesia cortesia, List<DetalleCortesia> detalles) {
        Usuario beneficiario = cortesia.getUsuarioBeneficiario();
        Usuario usuarioAnulacion = cortesia.getUsuarioAnulacion();
        return new CortesiaResponse(
                cortesia.getIdCortesia(),
                cortesia.getCajaDiaria().getIdCajaDiaria(),
                cortesia.getUsuarioRegistro().getIdUsuario(),
                cortesia.getUsuarioRegistro().getNombreUsuario(),
                cortesia.getTipoBeneficiario(),
                beneficiario == null ? null : beneficiario.getIdUsuario(),
                beneficiario == null ? null : beneficiario.getNombreCompleto(),
                cortesia.getReferenciaOtro(),
                cortesia.getMotivoOtro(),
                cortesia.getFechaRegistro(),
                cortesia.getEstado(),
                usuarioAnulacion == null ? null : usuarioAnulacion.getIdUsuario(),
                usuarioAnulacion == null ? null : usuarioAnulacion.getNombreUsuario(),
                cortesia.getFechaAnulacion(),
                cortesia.getMotivoAnulacion(),
                detalles.stream().map(this::toResponse).toList());
    }

    private DetalleCortesiaResponse toResponse(DetalleCortesia detalle) {
        return new DetalleCortesiaResponse(
                detalle.getIdDetalleCortesia(),
                detalle.getTipoGranizado().getIdTipoGranizado(),
                detalle.getTipoGranizado().getNombreTipo(),
                detalle.getTamanoVaso().getIdTamanoVaso(),
                detalle.getTamanoVaso().getOnzas(),
                detalle.getCantidad());
    }

    private record ClaveDetalle(UUID idTipoGranizado, UUID idTamanoVaso) {
    }
}
