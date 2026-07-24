package com.kontora.pos.consultas.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ConsultaMovimientoDepositoResponse(
        UUID idMovimientoDeposito,
        String tipoMovimientoDeposito,
        BigDecimal valorMovimiento,
        BigDecimal saldoAnterior,
        BigDecimal saldoPosterior,
        UUID idCierreCaja,
        UUID idConsignacionBancaria,
        UUID idPagoServicio,
        String nombreServicio,
        UUID idUsuarioRegistro,
        String nombreUsuarioRegistro,
        OffsetDateTime fechaMovimiento,
        String observacion
) {
}
