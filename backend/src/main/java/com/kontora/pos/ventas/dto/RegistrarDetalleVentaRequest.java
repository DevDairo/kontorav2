package com.kontora.pos.ventas.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RegistrarDetalleVentaRequest(
        @NotNull UUID idTipoGranizado,
        @NotNull UUID idTamanoVaso,
        @NotNull @Min(1) Integer cantidad
) {
}
