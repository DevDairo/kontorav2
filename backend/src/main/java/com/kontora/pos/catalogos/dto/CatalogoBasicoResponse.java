package com.kontora.pos.catalogos.dto;

import java.util.UUID;

public record CatalogoBasicoResponse(
        UUID id,
        String nombre,
        String estado
) {
}
