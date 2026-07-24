package com.kontora.pos.inventario.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RegistrarConsumoDiarioInventarioRequest(
        @NotNull UUID idItemInventario,
        @NotNull @Min(1) Integer cantidadConsumida,
        String observacion
) {
}
