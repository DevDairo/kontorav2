package com.kontora.pos.caja.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record EditarGastoCajaRequest(
        @NotNull @DecimalMin(value = "0.01") BigDecimal valorGasto,
        @NotBlank String descripcion,
        @NotBlank String motivoEdicion
) {
}
