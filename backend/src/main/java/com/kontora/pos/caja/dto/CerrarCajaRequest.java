package com.kontora.pos.caja.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CerrarCajaRequest(
        @NotNull @DecimalMin("0.00") BigDecimal efectivoContadoSinBase,
        @Size(max = 1000) String observaciones
) {
}
