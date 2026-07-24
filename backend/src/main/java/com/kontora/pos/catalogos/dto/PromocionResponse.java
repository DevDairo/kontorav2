package com.kontora.pos.catalogos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PromocionResponse(
        UUID idPromocion,
        String nombrePromocion,
        UUID idTipoGranizado,
        String nombreTipo,
        UUID idTamanoVaso,
        Integer onzas,
        String tipoBeneficiario,
        Integer cantidadRequerida,
        BigDecimal valorPromocional,
        LocalDate fechaInicioVigencia,
        LocalDate fechaFinVigencia,
        String estado,
        List<String> diasPromocion
) {
}
