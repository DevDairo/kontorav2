package com.kontora.pos.cortesias.dto;

import java.util.UUID;

public record DetalleCortesiaResponse(
        UUID idDetalleCortesia,
        UUID idTipoGranizado,
        String nombreTipoGranizado,
        UUID idTamanoVaso,
        Integer onzas,
        Integer cantidad
) {
}
