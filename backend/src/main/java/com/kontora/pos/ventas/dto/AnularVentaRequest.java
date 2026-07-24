package com.kontora.pos.ventas.dto;

import jakarta.validation.constraints.NotBlank;

public record AnularVentaRequest(
        @NotBlank String motivoAnulacion
) {
}
