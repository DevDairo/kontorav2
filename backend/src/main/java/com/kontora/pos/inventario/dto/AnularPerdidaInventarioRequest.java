package com.kontora.pos.inventario.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AnularPerdidaInventarioRequest(
        @NotBlank @Size(max = 1000) String motivoAnulacion,
        @NotNull
        @AssertTrue(message = "Debe confirmar que el vaso no estaba roto ni fue perdido")
        Boolean confirmaVasoNoRoto
) {
}
