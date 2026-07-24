package com.kontora.pos.usuarios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RestablecerContrasenaUsuarioRequest(
        @NotBlank(message = "La nueva contrasena es obligatoria")
        @Size(min = 8, max = 72, message = "La nueva contrasena debe tener entre 8 y 72 caracteres")
        String nuevaContrasena
) {
}
