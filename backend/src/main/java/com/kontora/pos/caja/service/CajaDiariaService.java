package com.kontora.pos.caja.service;

import com.kontora.pos.auditoria.service.AuditoriaService;
import com.kontora.pos.caja.domain.CajaDiaria;
import com.kontora.pos.caja.dto.AbrirCajaDiariaRequest;
import com.kontora.pos.caja.dto.CajaDiariaResponse;
import com.kontora.pos.caja.repository.CajaDiariaRepository;
import com.kontora.pos.common.exception.ApiException;
import com.kontora.pos.common.security.PrincipalUsuario;
import com.kontora.pos.inventario.service.InventarioService;
import com.kontora.pos.usuarios.domain.Usuario;
import com.kontora.pos.usuarios.repository.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Locale;

import static com.kontora.pos.common.audit.AuditoriaValores.valores;

@Service
public class CajaDiariaService {

    private static final String ESTADO_ABIERTA = "abierta";

    private final CajaDiariaRepository cajaDiariaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;
    private final InventarioService inventarioService;

    public CajaDiariaService(
            CajaDiariaRepository cajaDiariaRepository,
            UsuarioRepository usuarioRepository,
            AuditoriaService auditoriaService,
            InventarioService inventarioService) {
        this.cajaDiariaRepository = cajaDiariaRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditoriaService = auditoriaService;
        this.inventarioService = inventarioService;
    }

    @Transactional
    public CajaDiariaResponse abrirCaja(AbrirCajaDiariaRequest request, PrincipalUsuario principalUsuario) {
        validarRolApertura(principalUsuario);
        if (cajaDiariaRepository.existsByFechaOperacion(request.fechaOperacion())) {
            throw cajaDuplicada();
        }
        if (cajaDiariaRepository.existsByEstadoCaja(ESTADO_ABIERTA)) {
            throw cajaAbierta();
        }

        Usuario usuarioApertura = usuarioRepository.findById(principalUsuario.idUsuario())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Usuario autenticado no encontrado"));

        CajaDiaria cajaDiaria = new CajaDiaria();
        cajaDiaria.setFechaOperacion(request.fechaOperacion());
        cajaDiaria.setEstadoCaja(ESTADO_ABIERTA);
        cajaDiaria.setValorBase(request.valorBase());
        cajaDiaria.setFechaApertura(OffsetDateTime.now());
        cajaDiaria.setUsuarioApertura(usuarioApertura);
        cajaDiaria.setObservaciones(normalizarObservaciones(request.observaciones()));

        CajaDiaria cajaGuardada;
        try {
            cajaGuardada = cajaDiariaRepository.saveAndFlush(cajaDiaria);
        } catch (DataIntegrityViolationException exception) {
            if (cajaDiariaRepository.existsByFechaOperacion(request.fechaOperacion())) {
                throw cajaDuplicada();
            }
            if (cajaDiariaRepository.existsByEstadoCaja(ESTADO_ABIERTA)) {
                throw cajaAbierta();
            }
            throw cajaDuplicada();
        }
        inventarioService.inicializarStockDiarioParaCaja(cajaGuardada);
        auditoriaService.registrar(
                usuarioApertura,
                "cajas_diarias",
                cajaGuardada.getIdCajaDiaria(),
                "abrir",
                null,
                valores(
                        "fecha_operacion", cajaGuardada.getFechaOperacion(),
                        "estado_caja", cajaGuardada.getEstadoCaja(),
                        "valor_base", cajaGuardada.getValorBase(),
                        "fecha_apertura", cajaGuardada.getFechaApertura(),
                        "id_usuario_apertura", usuarioApertura.getIdUsuario(),
                        "observaciones", cajaGuardada.getObservaciones()),
                "Apertura de caja diaria");
        return toResponse(cajaGuardada);
    }

    @Transactional(readOnly = true)
    public CajaDiariaResponse obtenerCajaAbierta() {
        return cajaDiariaRepository.findPrimeraPorEstadoCaja(ESTADO_ABIERTA)
                .map(this::toResponse)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No existe una caja diaria abierta"));
    }

    @Transactional(readOnly = true)
    public CajaDiariaResponse obtenerCajaPorFecha(java.time.LocalDate fechaOperacion) {
        return cajaDiariaRepository.findByFechaOperacion(fechaOperacion)
                .map(this::toResponse)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No existe caja diaria para la fecha indicada"));
    }

    private void validarRolApertura(PrincipalUsuario principalUsuario) {
        String rol = principalUsuario.nombreRol().toLowerCase(Locale.ROOT);
        if (!"administrador".equals(rol) && !"gerente".equals(rol)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo administrador o gerente puede abrir caja diaria");
        }
    }

    private ApiException cajaDuplicada() {
        return new ApiException(HttpStatus.CONFLICT, "Ya existe una caja diaria para la fecha indicada");
    }

    private ApiException cajaAbierta() {
        return new ApiException(HttpStatus.CONFLICT, "Ya existe una caja diaria abierta. Cierra la jornada actual antes de abrir otra");
    }

    private String normalizarObservaciones(String observaciones) {
        if (observaciones == null || observaciones.isBlank()) {
            return null;
        }
        return observaciones.trim();
    }

    private CajaDiariaResponse toResponse(CajaDiaria cajaDiaria) {
        Usuario usuarioApertura = cajaDiaria.getUsuarioApertura();
        Usuario usuarioCierre = cajaDiaria.getUsuarioCierre();
        return new CajaDiariaResponse(
                cajaDiaria.getIdCajaDiaria(),
                cajaDiaria.getFechaOperacion(),
                cajaDiaria.getEstadoCaja(),
                cajaDiaria.getValorBase(),
                cajaDiaria.getFechaApertura(),
                cajaDiaria.getFechaCierre(),
                usuarioApertura.getIdUsuario(),
                usuarioApertura.getNombreUsuario(),
                usuarioCierre == null ? null : usuarioCierre.getIdUsuario(),
                usuarioCierre == null ? null : usuarioCierre.getNombreUsuario(),
                cajaDiaria.getObservaciones());
    }
}
