package com.kontora.pos.cortesias.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AnularCortesiaRequest(
        @NotBlank @Size(max = 1000) String motivoAnulacion,
        @NotNull
        @AssertTrue(message = "Debe confirmar que la cortesia no fue entregada ni consumida")
        Boolean confirmaNoEntregada
) {
}
