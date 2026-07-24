package com.kontora.pos.usuarios.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UsuarioGestionResponse(
        UUID idUsuario,
        String nombreUsuario,
        String nombreCompleto,
        UUID idRol,
        String nombreRol,
        String estado,
        OffsetDateTime fechaCreacion,
        OffsetDateTime fechaActualizacion
) {
}
