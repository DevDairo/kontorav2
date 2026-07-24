package com.kontora.pos.ventas.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record RegistrarPagoVentaRequest(
        @NotNull UUID idMetodoPago,
        @NotNull @DecimalMin("0.01") BigDecimal valorPago,
        @DecimalMin("0.01") BigDecimal valorRecibidoEfectivo
) {
}
