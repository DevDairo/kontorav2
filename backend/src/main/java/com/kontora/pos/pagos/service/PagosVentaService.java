package com.kontora.pos.pagos.service;

import com.kontora.pos.auditoria.service.AuditoriaService;
import com.kontora.pos.common.exception.ApiException;
import com.kontora.pos.common.security.PrincipalUsuario;
import com.kontora.pos.pagos.dto.ValidarTransferenciaRequest;
import com.kontora.pos.usuarios.domain.Usuario;
import com.kontora.pos.usuarios.repository.UsuarioRepository;
import com.kontora.pos.ventas.domain.PagoVenta;
import com.kontora.pos.ventas.dto.PagoVentaResponse;
import com.kontora.pos.ventas.repository.PagoVentaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static com.kontora.pos.common.audit.AuditoriaValores.valores;

@Service
public class PagosVentaService {

    private static final String METODO_TRANSFERENCIA = "transferencia";
    private static final String ESTADO_PENDIENTE = "pendiente";
    private static final String ESTADO_VALIDADA = "validada";
    private static final String ESTADO_RECHAZADA = "rechazada";
    private static final String ESTADO_VENTA_REGISTRADA = "registrada";

    private final PagoVentaRepository pagoVentaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;

    public PagosVentaService(
            PagoVentaRepository pagoVentaRepository,
            UsuarioRepository usuarioRepository,
            AuditoriaService auditoriaService) {
        this.pagoVentaRepository = pagoVentaRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditoriaService = auditoriaService;
    }

    @Transactional
    public PagoVentaResponse validarTransferencia(
            UUID idPagoVenta,
            ValidarTransferenciaRequest request,
            PrincipalUsuario principalUsuario) {
        return cambiarEstadoTransferencia(
                idPagoVenta,
                request,
                principalUsuario,
                ESTADO_VALIDADA,
                "validar",
                "Validacion de transferencia");
    }

    @Transactional
    public PagoVentaResponse rechazarTransferencia(
            UUID idPagoVenta,
            ValidarTransferenciaRequest request,
            PrincipalUsuario principalUsuario) {
        return cambiarEstadoTransferencia(
                idPagoVenta,
                request,
                principalUsuario,
                ESTADO_RECHAZADA,
                "rechazar",
                "Rechazo de transferencia");
    }

    private PagoVentaResponse cambiarEstadoTransferencia(
            UUID idPagoVenta,
            ValidarTransferenciaRequest request,
            PrincipalUsuario principalUsuario,
            String estadoNuevo,
            String accionAuditoria,
            String descripcionAuditoria) {
        validarRolGerente(principalUsuario);
        PagoVenta pagoVenta = pagoVentaRepository.findByIdPagoVenta(idPagoVenta)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Pago de venta no encontrado"));
        validarTransferenciaPendiente(pagoVenta);
        Usuario usuarioValidacion = usuarioRepository.findById(principalUsuario.idUsuario())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Usuario autenticado no encontrado"));
        Map<String, Object> valorAnterior = snapshotPagoVenta(pagoVenta);

        pagoVenta.setEstadoValidacion(estadoNuevo);
        pagoVenta.setUsuarioValidacion(usuarioValidacion);
        pagoVenta.setFechaValidacion(OffsetDateTime.now());
        pagoVenta.setObservacionValidacion(normalizarObservacion(request));

        PagoVenta pagoGuardado = pagoVentaRepository.saveAndFlush(pagoVenta);
        auditoriaService.registrar(
                usuarioValidacion,
                "pagos_venta",
                pagoGuardado.getIdPagoVenta(),
                accionAuditoria,
                valorAnterior,
                snapshotPagoVenta(pagoGuardado),
                descripcionAuditoria);
        return toResponse(pagoGuardado);
    }

    private void validarRolGerente(PrincipalUsuario principalUsuario) {
        String rol = principalUsuario.nombreRol().toLowerCase(Locale.ROOT);
        if (!"gerente".equals(rol)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo gerente puede validar transferencias");
        }
    }

    private void validarTransferenciaPendiente(PagoVenta pagoVenta) {
        if (!ESTADO_VENTA_REGISTRADA.equals(pagoVenta.getVenta().getEstadoVenta())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "No se puede validar ni rechazar una transferencia de una venta anulada");
        }
        if (!METODO_TRANSFERENCIA.equals(pagoVenta.getMetodoPago().getNombreMetodo())) {
            throw new ApiException(HttpStatus.CONFLICT, "Solo pagos por transferencia pueden validarse o rechazarse");
        }
        if (!ESTADO_PENDIENTE.equals(pagoVenta.getEstadoValidacion())) {
            throw new ApiException(HttpStatus.CONFLICT, "Solo se pueden validar o rechazar transferencias pendientes");
        }
    }

    private String normalizarObservacion(ValidarTransferenciaRequest request) {
        if (request == null || request.observacionValidacion() == null || request.observacionValidacion().isBlank()) {
            return null;
        }
        return request.observacionValidacion().trim();
    }

    private Map<String, Object> snapshotPagoVenta(PagoVenta pagoVenta) {
        Usuario usuarioValidacion = pagoVenta.getUsuarioValidacion();
        return valores(
                "id_pago_venta", pagoVenta.getIdPagoVenta(),
                "id_venta", pagoVenta.getVenta().getIdVenta(),
                "id_metodo_pago", pagoVenta.getMetodoPago().getIdMetodoPago(),
                "nombre_metodo", pagoVenta.getMetodoPago().getNombreMetodo(),
                "valor_pago", pagoVenta.getValorPago(),
                "estado_validacion", pagoVenta.getEstadoValidacion(),
                "id_usuario_validacion", usuarioValidacion == null ? null : usuarioValidacion.getIdUsuario(),
                "fecha_validacion", pagoVenta.getFechaValidacion(),
                "observacion_validacion", pagoVenta.getObservacionValidacion());
    }

    private PagoVentaResponse toResponse(PagoVenta pagoVenta) {
        Usuario usuarioValidacion = pagoVenta.getUsuarioValidacion();
        return new PagoVentaResponse(
                pagoVenta.getIdPagoVenta(),
                pagoVenta.getMetodoPago().getIdMetodoPago(),
                pagoVenta.getMetodoPago().getNombreMetodo(),
                pagoVenta.getValorPago(),
                pagoVenta.getValorRecibidoEfectivo(),
                pagoVenta.getCambioEntregado(),
                pagoVenta.getEstadoValidacion(),
                pagoVenta.getFechaRegistro(),
                usuarioValidacion == null ? null : usuarioValidacion.getIdUsuario(),
                usuarioValidacion == null ? null : usuarioValidacion.getNombreUsuario(),
                pagoVenta.getFechaValidacion(),
                pagoVenta.getObservacionValidacion());
    }
}
