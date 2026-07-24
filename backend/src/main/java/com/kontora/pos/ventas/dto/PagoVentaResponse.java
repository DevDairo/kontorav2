package com.kontora.pos.ventas.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PagoVentaResponse(
        UUID idPagoVenta,
        UUID idMetodoPago,
        String nombreMetodo,
        BigDecimal valorPago,
        BigDecimal valorRecibidoEfectivo,
        BigDecimal cambioEntregado,
        String estadoValidacion,
        OffsetDateTime fechaRegistro,
        UUID idUsuarioValidacion,
        String nombreUsuarioValidacion,
        OffsetDateTime fechaValidacion,
        String observacionValidacion
) {
}
