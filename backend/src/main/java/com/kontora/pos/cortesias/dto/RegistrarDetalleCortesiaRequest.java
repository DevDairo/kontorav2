package com.kontora.pos.cortesias.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RegistrarDetalleCortesiaRequest(
        @NotNull UUID idTipoGranizado,
        @NotNull UUID idTamanoVaso,
        @NotNull @Min(1) Integer cantidad
) {
}
