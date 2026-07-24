package com.kontora.pos.catalogos.dto;

import java.util.UUID;

public record UnidadMedidaResponse(
        UUID idUnidadMedida,
        String nombreUnidad,
        String abreviatura,
        String estado
) {
}
