package com.kontora.pos.caja.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AbrirCajaDiariaRequest(
        @NotNull LocalDate fechaOperacion,
        @NotNull @DecimalMin("0.00") BigDecimal valorBase,
        @Size(max = 1000) String observaciones
) {
}
