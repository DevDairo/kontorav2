package com.kontora.pos.catalogos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PrecioGranizadoResponse(
        UUID idPrecioGranizado,
        UUID idTipoGranizado,
        String nombreTipo,
        UUID idTamanoVaso,
        Integer onzas,
        BigDecimal valorPrecio,
        LocalDate fechaInicioVigencia,
        LocalDate fechaFinVigencia,
        String estado
) {
}
