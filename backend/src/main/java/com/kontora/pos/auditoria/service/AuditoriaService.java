package com.kontora.pos.auditoria.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kontora.pos.auditoria.domain.AuditoriaOperacion;
import com.kontora.pos.auditoria.repository.AuditoriaOperacionRepository;
import com.kontora.pos.common.exception.ApiException;
import com.kontora.pos.usuarios.domain.Usuario;
import com.kontora.pos.usuarios.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class AuditoriaService {

    private final AuditoriaOperacionRepository auditoriaOperacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final ObjectMapper objectMapper;

    public AuditoriaService(
            AuditoriaOperacionRepository auditoriaOperacionRepository,
            UsuarioRepository usuarioRepository,
            ObjectMapper objectMapper) {
        this.auditoriaOperacionRepository = auditoriaOperacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.objectMapper = objectMapper;
    }

    public AuditoriaOperacion registrar(
            UUID idUsuario,
            String tablaAfectada,
            UUID idRegistroAfectado,
            String accion,
            Map<String, Object> valorAnterior,
            Map<String, Object> valorNuevo,
            String descripcion) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Usuario autenticado no encontrado"));
        return registrar(usuario, tablaAfectada, idRegistroAfectado, accion, valorAnterior, valorNuevo, descripcion);
    }

    public AuditoriaOperacion registrar(
            Usuario usuario,
            String tablaAfectada,
            UUID idRegistroAfectado,
            String accion,
            Map<String, Object> valorAnterior,
            Map<String, Object> valorNuevo,
            String descripcion) {
        AuditoriaOperacion auditoria = new AuditoriaOperacion();
        auditoria.setUsuario(usuario);
        auditoria.setTablaAfectada(tablaAfectada);
        auditoria.setIdRegistroAfectado(idRegistroAfectado == null ? null : idRegistroAfectado.toString());
        auditoria.setAccion(accion);
        auditoria.setValorAnterior(toJson(valorAnterior));
        auditoria.setValorNuevo(toJson(valorNuevo));
        auditoria.setFechaAccion(OffsetDateTime.now());
        auditoria.setDireccionIp(obtenerDireccionIpActual());
        auditoria.setDescripcion(normalizarDescripcion(descripcion));
        return auditoriaOperacionRepository.save(auditoria);
    }

    private JsonNode toJson(Map<String, Object> valores) {
        if (valores == null) {
            return null;
        }
        return objectMapper.valueToTree(valores);
    }

    private String obtenerDireccionIpActual() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String normalizarDescripcion(String descripcion) {
        if (descripcion == null || descripcion.isBlank()) {
            return null;
        }
        return descripcion.trim();
    }
}
