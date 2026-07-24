package com.kontora.pos.usuarios.dto;

import java.util.UUID;

public record RolGestionResponse(
        UUID idRol,
        String nombreRol
) {
}
