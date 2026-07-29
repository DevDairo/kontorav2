package com.kontora.pos.cortesias.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record RegistrarCortesiaRequest(
        @NotBlank String tipoBeneficiario,
        UUID idUsuarioBeneficiario,
        @Size(max = 250) String referenciaOtro,
        @Size(max = 1000) String motivoOtro,
        @NotEmpty List<@Valid RegistrarDetalleCortesiaRequest> detalles,
        @NotNull
        @AssertTrue(message = "Debe confirmar el registro de la cortesia")
        Boolean confirmaRegistro
) {
}
