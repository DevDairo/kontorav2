package com.kontora.pos.usuarios.controller;

import com.kontora.pos.common.security.PrincipalUsuario;
import com.kontora.pos.usuarios.dto.LoginRequest;
import com.kontora.pos.usuarios.dto.LoginResponse;
import com.kontora.pos.usuarios.dto.UsuarioAutenticadoResponse;
import com.kontora.pos.usuarios.service.AutenticacionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AutenticacionController {

    private final AutenticacionService autenticacionService;

    public AutenticacionController(AutenticacionService autenticacionService) {
        this.autenticacionService = autenticacionService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        return autenticacionService.login(request, obtenerIp(servletRequest), servletRequest.getHeader("User-Agent"));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Authentication authentication) {
        autenticacionService.logout((PrincipalUsuario) authentication.getPrincipal());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public UsuarioAutenticadoResponse me(Authentication authentication) {
        return autenticacionService.obtenerUsuarioAutenticado((PrincipalUsuario) authentication.getPrincipal());
    }

    private String obtenerIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

