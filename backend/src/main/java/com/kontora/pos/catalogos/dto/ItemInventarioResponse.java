package com.kontora.pos.catalogos.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ItemInventarioResponse(
        UUID idItemInventario,
        String nombreItem,
        String tipoControl,
        boolean manejaPaquetes,
        Integer unidadesPorPaquete,
        String estado,
        OffsetDateTime fechaCreacion,
        UUID idCategoriaInventario,
        String nombreCategoria,
        UUID idUnidadMedida,
        String nombreUnidad,
        String abreviaturaUnidad,
        UUID idTamanoVaso,
        Integer onzas
) {
}
