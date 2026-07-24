package com.kontora.pos.ventas.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record DetalleVentaResponse(
        UUID idDetalleVenta,
        UUID idTipoGranizado,
        String nombreTipo,
        UUID idTamanoVaso,
        Integer onzas,
        Integer cantidad,
        BigDecimal precioUnitarioNormal,
        Integer cantidadConPromocion,
        Integer cantidadSinPromocion,
        BigDecimal valorPromocionalAplicado,
        UUID idPromocionAplicada,
        String nombrePromocionAplicada,
        BigDecimal subtotalLinea,
        BigDecimal totalLinea
) {
}
