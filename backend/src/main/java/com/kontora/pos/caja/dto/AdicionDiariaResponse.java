package com.kontora.pos.caja.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AdicionDiariaResponse(
        UUID idAdicionDiaria,
        UUID idCajaDiaria,
        Integer cantidadAdiciones,
        BigDecimal valorUnitario,
        BigDecimal valorTotal,
        UUID idUsuarioRegistro,
        String nombreUsuarioRegistro,
        OffsetDateTime fechaRegistro
) {
}
