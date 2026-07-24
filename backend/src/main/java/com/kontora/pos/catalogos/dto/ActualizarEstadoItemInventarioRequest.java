package com.kontora.pos.catalogos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ActualizarEstadoItemInventarioRequest(
        @NotBlank(message = "El estado es obligatorio")
        @Pattern(regexp = "^(activo|inactivo)$", message = "El estado del item no es valido")
        String estado
) {
}
