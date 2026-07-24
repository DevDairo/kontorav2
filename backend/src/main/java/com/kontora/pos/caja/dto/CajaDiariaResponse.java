package com.kontora.pos.caja.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CajaDiariaResponse(
        UUID idCajaDiaria,
        LocalDate fechaOperacion,
        String estadoCaja,
        BigDecimal valorBase,
        OffsetDateTime fechaApertura,
        OffsetDateTime fechaCierre,
        UUID idUsuarioApertura,
        String nombreUsuarioApertura,
        UUID idUsuarioCierre,
        String nombreUsuarioCierre,
        String observaciones
) {
}
