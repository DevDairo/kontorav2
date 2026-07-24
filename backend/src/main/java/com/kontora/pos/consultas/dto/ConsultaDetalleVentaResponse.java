package com.kontora.pos.consultas.dto;

import java.util.UUID;

public record ConsultaDetalleVentaResponse(
        UUID idDetalleVenta,
        String nombreTipo,
        Integer onzas,
        Integer cantidad
) {
}
