package com.kontora.pos.usuarios.dto;

import java.util.UUID;

public record UsuarioAutenticadoResponse(
        UUID idUsuario,
        String nombreUsuario,
        String nombreCompleto,
        String nombreRol
) {
}

