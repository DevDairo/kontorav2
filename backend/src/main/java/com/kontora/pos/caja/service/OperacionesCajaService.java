package com.kontora.pos.caja.service;

import com.kontora.pos.auditoria.service.AuditoriaService;
import com.kontora.pos.caja.domain.AdicionDiaria;
import com.kontora.pos.caja.domain.CajaDiaria;
import com.kontora.pos.caja.domain.GastoCaja;
import com.kontora.pos.caja.domain.PagoTrabajadoresDiario;
import com.kontora.pos.caja.dto.AdicionDiariaResponse;
import com.kontora.pos.caja.dto.AnularGastoCajaRequest;
import com.kontora.pos.caja.dto.EditarGastoCajaRequest;
import com.kontora.pos.caja.dto.GastoCajaResponse;
import com.kontora.pos.caja.dto.PagoTrabajadoresDiarioResponse;
import com.kontora.pos.caja.dto.RegistrarAdicionDiariaRequest;
import com.kontora.pos.caja.dto.RegistrarGastoCajaRequest;
import com.kontora.pos.caja.dto.RegistrarPagoTrabajadoresDiarioRequest;
import com.kontora.pos.caja.repository.AdicionDiariaRepository;
import com.kontora.pos.caja.repository.CajaDiariaRepository;
import com.kontora.pos.caja.repository.GastoCajaRepository;
import com.kontora.pos.caja.repository.PagoTrabajadoresDiarioRepository;
import com.kontora.pos.common.exception.ApiException;
import com.kontora.pos.common.security.PrincipalUsuario;
import com.kontora.pos.usuarios.domain.Usuario;
import com.kontora.pos.usuarios.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.kontora.pos.common.audit.AuditoriaValores.valores;

@Service
public class OperacionesCajaService {

    private static final String ESTADO_CAJA_ABIERTA = "abierta";
    private static final String ESTADO_GASTO_REGISTRADO = "registrado";
    private static final String ESTADO_GASTO_EDITADO = "editado";
    private static final String ESTADO_GASTO_ANULADO = "anulado";
    private static final BigDecimal VALOR_UNITARIO_ADICION_DEFECTO = new BigDecimal("1000.00");

    private final CajaDiariaRepository cajaDiariaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AdicionDiariaRepository adicionDiariaRepository;
    private final PagoTrabajadoresDiarioRepository pagoTrabajadoresDiarioRepository;
    private final GastoCajaRepository gastoCajaRepository;
    private final EntityManager entityManager;
    private final AuditoriaService auditoriaService;

    public OperacionesCajaService(
            CajaDiariaRepository cajaDiariaRepository,
            UsuarioRepository usuarioRepository,
            AdicionDiariaRepository adicionDiariaRepository,
            PagoTrabajadoresDiarioRepository pagoTrabajadoresDiarioRepository,
            GastoCajaRepository gastoCajaRepository,
            EntityManager entityManager,
            AuditoriaService auditoriaService) {
        this.cajaDiariaRepository = cajaDiariaRepository;
        this.usuarioRepository = usuarioRepository;
        this.adicionDiariaRepository = adicionDiariaRepository;
        this.pagoTrabajadoresDiarioRepository = pagoTrabajadoresDiarioRepository;
        this.gastoCajaRepository = gastoCajaRepository;
        this.entityManager = entityManager;
        this.auditoriaService = auditoriaService;
    }

    @Transactional
    public AdicionDiariaResponse registrarAdicionDiaria(
            RegistrarAdicionDiariaRequest request,
            PrincipalUsuario principalUsuario) {
        CajaDiaria cajaDiaria = obtenerCajaAbierta();
        Usuario usuario = obtenerUsuario(principalUsuario.idUsuario());
        AdicionDiaria adicionDiaria = adicionDiariaRepository
                .findByCajaDiaria_IdCajaDiaria(cajaDiaria.getIdCajaDiaria())
                .orElseGet(AdicionDiaria::new);

        adicionDiaria.setCajaDiaria(cajaDiaria);
        adicionDiaria.setCantidadAdiciones(request.cantidadAdiciones());
        adicionDiaria.setValorUnitario(normalizarMoneda(
                request.valorUnitario() == null ? VALOR_UNITARIO_ADICION_DEFECTO : request.valorUnitario()));
        adicionDiaria.setUsuarioRegistro(usuario);
        adicionDiaria.setFechaRegistro(OffsetDateTime.now());

        AdicionDiaria guardada = adicionDiariaRepository.saveAndFlush(adicionDiaria);
        entityManager.refresh(guardada);
        return toResponse(guardada);
    }

    @Transactional(readOnly = true)
    public Optional<AdicionDiariaResponse> obtenerAdicionDiariaCajaAbierta() {
        CajaDiaria cajaDiaria = obtenerCajaAbierta();
        return adicionDiariaRepository.findByCajaDiaria_IdCajaDiaria(cajaDiaria.getIdCajaDiaria())
                .map(this::toResponse);
    }

    @Transactional
    public PagoTrabajadoresDiarioResponse registrarPagoTrabajadoresDiario(
            RegistrarPagoTrabajadoresDiarioRequest request,
            PrincipalUsuario principalUsuario) {
        validarRolAdministrativo(principalUsuario, "Solo administrador o gerente puede registrar pago diario a trabajadores");
        CajaDiaria cajaDiaria = obtenerCajaAbierta();
        Usuario usuario = obtenerUsuario(principalUsuario.idUsuario());
        boolean confirmado = Boolean.TRUE.equals(request.confirmadoParaCierre());
        BigDecimal valorTotalPagado = normalizarMoneda(request.valorTotalPagado());
        if (valorTotalPagado.compareTo(BigDecimal.ZERO) == 0 && !confirmado) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Pago a trabajadores en cero requiere confirmacion explicita");
        }

        PagoTrabajadoresDiario pago = pagoTrabajadoresDiarioRepository
                .findByCajaDiaria_IdCajaDiaria(cajaDiaria.getIdCajaDiaria())
                .orElseGet(PagoTrabajadoresDiario::new);
        pago.setCajaDiaria(cajaDiaria);
        pago.setValorTotalPagado(valorTotalPagado);
        pago.setDescripcion(normalizarTextoOpcional(request.descripcion()));
        pago.setUsuarioRegistro(usuario);
        pago.setFechaRegistro(OffsetDateTime.now());
        pago.setConfirmadoParaCierre(confirmado);

        return toResponse(pagoTrabajadoresDiarioRepository.saveAndFlush(pago));
    }

    @Transactional
    public PagoTrabajadoresDiarioResponse confirmarPagoTrabajadoresDiario(
            UUID idPagoTrabajadoresDiario,
            PrincipalUsuario principalUsuario) {
        validarRolAdministrativo(principalUsuario, "Solo administrador o gerente puede confirmar pago diario a trabajadores");
        PagoTrabajadoresDiario pago = pagoTrabajadoresDiarioRepository.findById(idPagoTrabajadoresDiario)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Pago diario a trabajadores no encontrado"));
        validarCajaAbierta(pago.getCajaDiaria());
        pago.setConfirmadoParaCierre(true);
        pago.setUsuarioRegistro(obtenerUsuario(principalUsuario.idUsuario()));
        pago.setFechaRegistro(OffsetDateTime.now());
        return toResponse(pagoTrabajadoresDiarioRepository.saveAndFlush(pago));
    }

    @Transactional(readOnly = true)
    public PagoTrabajadoresDiarioResponse obtenerPagoTrabajadoresCajaAbierta() {
        CajaDiaria cajaDiaria = obtenerCajaAbierta();
        return pagoTrabajadoresDiarioRepository.findByCajaDiaria_IdCajaDiaria(cajaDiaria.getIdCajaDiaria())
                .map(this::toResponse)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No existe pago diario a trabajadores para la caja abierta"));
    }

    @Transactional
    public GastoCajaResponse registrarGastoCaja(
            RegistrarGastoCajaRequest request,
            PrincipalUsuario principalUsuario) {
        CajaDiaria cajaDiaria = obtenerCajaAbierta();
        Usuario usuario = obtenerUsuario(principalUsuario.idUsuario());

        GastoCaja gasto = new GastoCaja();
        gasto.setCajaDiaria(cajaDiaria);
        gasto.setValorGasto(normalizarMoneda(request.valorGasto()));
        gasto.setDescripcion(request.descripcion().trim());
        gasto.setEstadoGasto(ESTADO_GASTO_REGISTRADO);
        gasto.setUsuarioRegistro(usuario);
        gasto.setFechaRegistro(OffsetDateTime.now());

        return toResponse(gastoCajaRepository.saveAndFlush(gasto));
    }

    @Transactional
    public GastoCajaResponse editarGastoCaja(
            UUID idGastoCaja,
            EditarGastoCajaRequest request,
            PrincipalUsuario principalUsuario) {
        validarRolAdministrativo(principalUsuario, "Solo administrador o gerente puede editar gastos de caja");
        GastoCaja gasto = obtenerGasto(idGastoCaja);
        validarGastoEditable(gasto);
        Usuario usuario = obtenerUsuario(principalUsuario.idUsuario());
        Map<String, Object> valorAnterior = snapshotGasto(gasto);

        gasto.setValorGasto(normalizarMoneda(request.valorGasto()));
        gasto.setDescripcion(request.descripcion().trim());
        gasto.setEstadoGasto(ESTADO_GASTO_EDITADO);
        gasto.setUsuarioUltimaEdicion(usuario);
        gasto.setFechaUltimaEdicion(OffsetDateTime.now());
        gasto.setMotivoEdicion(request.motivoEdicion().trim());

        GastoCaja gastoGuardado = gastoCajaRepository.saveAndFlush(gasto);
        auditoriaService.registrar(
                usuario,
                "gastos_caja",
                gastoGuardado.getIdGastoCaja(),
                "editar",
                valorAnterior,
                snapshotGasto(gastoGuardado),
                "Edicion de gasto de caja");
        return toResponse(gastoGuardado);
    }

    @Transactional
    public GastoCajaResponse anularGastoCaja(
            UUID idGastoCaja,
            AnularGastoCajaRequest request,
            PrincipalUsuario principalUsuario) {
        validarRolAdministrativo(principalUsuario, "Solo administrador o gerente puede anular gastos de caja");
        GastoCaja gasto = obtenerGasto(idGastoCaja);
        validarGastoEditable(gasto);
        Usuario usuario = obtenerUsuario(principalUsuario.idUsuario());
        Map<String, Object> valorAnterior = snapshotGasto(gasto);

        gasto.setEstadoGasto(ESTADO_GASTO_ANULADO);
        gasto.setUsuarioAnulacion(usuario);
        gasto.setFechaAnulacion(OffsetDateTime.now());
        gasto.setMotivoAnulacion(request.motivoAnulacion().trim());

        GastoCaja gastoGuardado = gastoCajaRepository.saveAndFlush(gasto);
        auditoriaService.registrar(
                usuario,
                "gastos_caja",
                gastoGuardado.getIdGastoCaja(),
                "anular",
                valorAnterior,
                snapshotGasto(gastoGuardado),
                "Anulacion de gasto de caja");
        return toResponse(gastoGuardado);
    }

    @Transactional(readOnly = true)
    public List<GastoCajaResponse> listarGastosCajaAbierta() {
        CajaDiaria cajaDiaria = obtenerCajaAbierta();
        return gastoCajaRepository.findByCajaDiaria_IdCajaDiariaOrderByFechaRegistroDesc(cajaDiaria.getIdCajaDiaria())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private CajaDiaria obtenerCajaAbierta() {
        return cajaDiariaRepository.findPrimeraPorEstadoCaja(ESTADO_CAJA_ABIERTA)
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "No existe caja diaria abierta para operaciones de caja"));
    }

    private Usuario obtenerUsuario(UUID idUsuario) {
        return usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Usuario autenticado no encontrado"));
    }

    private GastoCaja obtenerGasto(UUID idGastoCaja) {
        return gastoCajaRepository.findByIdGastoCaja(idGastoCaja)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Gasto de caja no encontrado"));
    }

    private void validarGastoEditable(GastoCaja gasto) {
        validarCajaAbierta(gasto.getCajaDiaria());
        if (ESTADO_GASTO_ANULADO.equals(gasto.getEstadoGasto())) {
            throw new ApiException(HttpStatus.CONFLICT, "No se puede editar o anular un gasto ya anulado");
        }
    }

    private void validarCajaAbierta(CajaDiaria cajaDiaria) {
        if (!ESTADO_CAJA_ABIERTA.equals(cajaDiaria.getEstadoCaja())) {
            throw new ApiException(HttpStatus.CONFLICT, "La operacion no se puede realizar porque la caja diaria esta cerrada");
        }
    }

    private void validarRolAdministrativo(PrincipalUsuario principalUsuario, String mensaje) {
        String rol = principalUsuario.nombreRol().toLowerCase(Locale.ROOT);
        if (!"administrador".equals(rol) && !"gerente".equals(rol)) {
            throw new ApiException(HttpStatus.FORBIDDEN, mensaje);
        }
    }

    private BigDecimal normalizarMoneda(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizarTextoOpcional(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.trim();
    }

    private Map<String, Object> snapshotGasto(GastoCaja gasto) {
        Usuario usuarioUltimaEdicion = gasto.getUsuarioUltimaEdicion();
        Usuario usuarioAnulacion = gasto.getUsuarioAnulacion();
        return valores(
                "id_gasto_caja", gasto.getIdGastoCaja(),
                "id_caja_diaria", gasto.getCajaDiaria().getIdCajaDiaria(),
                "valor_gasto", gasto.getValorGasto(),
                "descripcion", gasto.getDescripcion(),
                "estado_gasto", gasto.getEstadoGasto(),
                "id_usuario_registro", gasto.getUsuarioRegistro().getIdUsuario(),
                "id_usuario_ultima_edicion", usuarioUltimaEdicion == null ? null : usuarioUltimaEdicion.getIdUsuario(),
                "fecha_ultima_edicion", gasto.getFechaUltimaEdicion(),
                "motivo_edicion", gasto.getMotivoEdicion(),
                "id_usuario_anulacion", usuarioAnulacion == null ? null : usuarioAnulacion.getIdUsuario(),
                "fecha_anulacion", gasto.getFechaAnulacion(),
                "motivo_anulacion", gasto.getMotivoAnulacion());
    }

    private AdicionDiariaResponse toResponse(AdicionDiaria adicionDiaria) {
        return new AdicionDiariaResponse(
                adicionDiaria.getIdAdicionDiaria(),
                adicionDiaria.getCajaDiaria().getIdCajaDiaria(),
                adicionDiaria.getCantidadAdiciones(),
                adicionDiaria.getValorUnitario(),
                adicionDiaria.getValorTotal(),
                adicionDiaria.getUsuarioRegistro().getIdUsuario(),
                adicionDiaria.getUsuarioRegistro().getNombreUsuario(),
                adicionDiaria.getFechaRegistro());
    }

    private PagoTrabajadoresDiarioResponse toResponse(PagoTrabajadoresDiario pago) {
        return new PagoTrabajadoresDiarioResponse(
                pago.getIdPagoTrabajadoresDiario(),
                pago.getCajaDiaria().getIdCajaDiaria(),
                pago.getValorTotalPagado(),
                pago.getDescripcion(),
                pago.getUsuarioRegistro().getIdUsuario(),
                pago.getUsuarioRegistro().getNombreUsuario(),
                pago.getFechaRegistro(),
                pago.isConfirmadoParaCierre());
    }

    private GastoCajaResponse toResponse(GastoCaja gasto) {
        Usuario usuarioUltimaEdicion = gasto.getUsuarioUltimaEdicion();
        Usuario usuarioAnulacion = gasto.getUsuarioAnulacion();
        return new GastoCajaResponse(
                gasto.getIdGastoCaja(),
                gasto.getCajaDiaria().getIdCajaDiaria(),
                gasto.getValorGasto(),
                gasto.getDescripcion(),
                gasto.getEstadoGasto(),
                gasto.getUsuarioRegistro().getIdUsuario(),
                gasto.getUsuarioRegistro().getNombreUsuario(),
                gasto.getFechaRegistro(),
                usuarioUltimaEdicion == null ? null : usuarioUltimaEdicion.getIdUsuario(),
                usuarioUltimaEdicion == null ? null : usuarioUltimaEdicion.getNombreUsuario(),
                gasto.getFechaUltimaEdicion(),
                gasto.getMotivoEdicion(),
                usuarioAnulacion == null ? null : usuarioAnulacion.getIdUsuario(),
                usuarioAnulacion == null ? null : usuarioAnulacion.getNombreUsuario(),
                gasto.getFechaAnulacion(),
                gasto.getMotivoAnulacion());
    }
}
