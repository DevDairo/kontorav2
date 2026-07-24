package com.kontora.pos.ventas.dto;

import java.util.UUID;

public record TrabajadorVentaResponse(
        UUID idUsuario,
        String nombreUsuario,
        String nombreCompleto
) {
}
