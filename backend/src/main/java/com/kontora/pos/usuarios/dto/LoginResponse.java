package com.kontora.pos.usuarios.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record LoginResponse(
        String token,
        String tipoToken,
        long expiraEnMinutos,
        OffsetDateTime fechaExpiracion,
        UUID idUsuario,
        String nombreUsuario,
        String nombreCompleto,
        String nombreRol,
        boolean requiereCambioContrasena
) {
}

