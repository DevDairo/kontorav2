package com.kontora.pos.inventario.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record RegistrarPerdidaInventarioRequest(
        @NotNull UUID idTamanoVaso,
        UUID idPaqueteVasosAbierto,
        @NotNull @Min(1) Integer cantidad,
        @NotBlank @Size(max = 1000) String motivo,
        @NotNull
        @AssertTrue(message = "Debe confirmar el registro de la perdida")
        Boolean confirmaRegistro
) {
}
