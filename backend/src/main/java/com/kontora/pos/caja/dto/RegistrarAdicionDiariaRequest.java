package com.kontora.pos.caja.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RegistrarAdicionDiariaRequest(
        @NotNull @Min(0) Integer cantidadAdiciones,
        @DecimalMin(value = "0.00") BigDecimal valorUnitario
) {
}
