package com.kontora.pos.caja.dto;

import jakarta.validation.constraints.NotBlank;

public record AnularGastoCajaRequest(
        @NotBlank String motivoAnulacion
) {
}
