package com.kontora.pos.catalogos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CrearItemInventarioRequest(
        @NotNull(message = "La categoria es obligatoria")
        UUID idCategoriaInventario,
        @NotNull(message = "La unidad de medida es obligatoria")
        UUID idUnidadMedida,
        UUID idTamanoVaso,
        @NotBlank(message = "El nombre del item es obligatorio")
        @Size(max = 120, message = "El nombre del item no puede superar 120 caracteres")
        String nombreItem,
        @NotBlank(message = "El tipo de control es obligatorio")
        @Pattern(
                regexp = "^(automatico_por_venta|manual_por_consumo)$",
                message = "El tipo de control no es valido")
        String tipoControl,
        boolean manejaPaquetes,
        Integer unidadesPorPaquete
) {
}
