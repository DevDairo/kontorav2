package com.kontora.pos.usuarios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ActualizarEstadoUsuarioRequest(
        @NotBlank(message = "El estado es obligatorio")
        @Pattern(regexp = "^(activo|inactivo|bloqueado)$", message = "El estado de usuario no es valido")
        String estado
) {
}
