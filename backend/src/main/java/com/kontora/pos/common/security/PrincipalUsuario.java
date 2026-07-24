package com.kontora.pos.common.security;

import java.util.UUID;

public record PrincipalUsuario(
        UUID idUsuario,
        String nombreUsuario,
        String nombreCompleto,
        String nombreRol,
        String tokenIdentificador
) {
}

