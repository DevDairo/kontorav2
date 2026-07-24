package com.kontora.pos.catalogos.dto;

import java.util.UUID;

public record TamanoVasoResponse(
        UUID idTamanoVaso,
        Integer onzas,
        String estado
) {
}
