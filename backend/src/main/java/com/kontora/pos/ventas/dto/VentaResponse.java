package com.kontora.pos.ventas.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record VentaResponse(
        UUID idVenta,
        UUID idCajaDiaria,
        UUID idUsuarioVendedor,
        String nombreUsuarioVendedor,
        String tipoComprador,
        UUID idUsuarioComprador,
        Long numeroVenta,
        OffsetDateTime fechaVenta,
        String estadoVenta,
        BigDecimal subtotalVenta,
        BigDecimal descuentoPromocion,
        BigDecimal totalVenta,
        List<DetalleVentaResponse> detalles,
        List<PagoVentaResponse> pagos
) {
}
