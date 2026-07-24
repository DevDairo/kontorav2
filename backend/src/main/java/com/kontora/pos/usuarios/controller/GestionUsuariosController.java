package com.kontora.pos.usuarios.controller;

import com.kontora.pos.common.security.PrincipalUsuario;
import com.kontora.pos.usuarios.dto.ActualizarEstadoUsuarioRequest;
import com.kontora.pos.usuarios.dto.ActualizarUsuarioRequest;
import com.kontora.pos.usuarios.dto.CrearUsuarioRequest;
import com.kontora.pos.usuarios.dto.RestablecerContrasenaUsuarioRequest;
import com.kontora.pos.usuarios.dto.RolGestionResponse;
import com.kontora.pos.usuarios.dto.UsuarioGestionResponse;
import com.kontora.pos.usuarios.service.GestionUsuariosService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/usuarios")
public class GestionUsuariosController {

    private final GestionUsuariosService gestionUsuariosService;

    public GestionUsuariosController(GestionUsuariosService gestionUsuariosService) {
        this.gestionUsuariosService = gestionUsuariosService;
    }

    @GetMapping
    public List<UsuarioGestionResponse> listarUsuarios(Authentication authentication) {
        return gestionUsuariosService.listarUsuarios(principal(authentication));
    }

    @GetMapping("/roles")
    public List<RolGestionResponse> listarRolesActivos(Authentication authentication) {
        return gestionUsuariosService.listarRolesActivos(principal(authentication));
    }

    @PostMapping
    public ResponseEntity<UsuarioGestionResponse> crearUsuario(
            @Valid @RequestBody CrearUsuarioRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(gestionUsuariosService.crearUsuario(request, principal(authentication)));
    }

    @PutMapping("/{idUsuario}")
    public UsuarioGestionResponse actualizarUsuario(
            @PathVariable UUID idUsuario,
            @Valid @RequestBody ActualizarUsuarioRequest request,
            Authentication authentication) {
        return gestionUsuariosService.actualizarUsuario(idUsuario, request, principal(authentication));
    }

    @PutMapping("/{idUsuario}/estado")
    public UsuarioGestionResponse actualizarEstado(
            @PathVariable UUID idUsuario,
            @Valid @RequestBody ActualizarEstadoUsuarioRequest request,
            Authentication authentication) {
        return gestionUsuariosService.actualizarEstado(idUsuario, request, principal(authentication));
    }

    @PutMapping("/{idUsuario}/contrasena")
    public ResponseEntity<Void> restablecerContrasena(
            @PathVariable UUID idUsuario,
            @Valid @RequestBody RestablecerContrasenaUsuarioRequest request,
            Authentication authentication) {
        gestionUsuariosService.restablecerContrasena(idUsuario, request, principal(authentication));
        return ResponseEntity.noContent().build();
    }

    private PrincipalUsuario principal(Authentication authentication) {
        return (PrincipalUsuario) authentication.getPrincipal();
    }
}
