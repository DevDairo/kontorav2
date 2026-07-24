package com.kontora.pos.inventario.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SolicitarAjusteInventarioRequest(
        @NotNull UUID idItemInventario,
        @NotBlank String tipoStock,
        @NotNull @Min(1) Integer cantidadAjuste,
        @NotBlank String sentidoAjuste,
        @NotBlank @Size(max = 1000) String motivoAjuste
) {
}
