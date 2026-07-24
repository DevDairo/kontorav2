package com.kontora.pos.caja.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PagoTrabajadoresDiarioResponse(
        UUID idPagoTrabajadoresDiario,
        UUID idCajaDiaria,
        BigDecimal valorTotalPagado,
        String descripcion,
        UUID idUsuarioRegistro,
        String nombreUsuarioRegistro,
        OffsetDateTime fechaRegistro,
        boolean confirmadoParaCierre
) {
}
