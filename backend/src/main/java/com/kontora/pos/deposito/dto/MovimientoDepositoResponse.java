package com.kontora.pos.deposito.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MovimientoDepositoResponse(
        UUID idMovimientoDeposito,
        String tipoMovimientoDeposito,
        BigDecimal valorMovimiento,
        BigDecimal saldoAnterior,
        BigDecimal saldoPosterior,
        UUID idCierreCaja,
        UUID idUsuarioRegistro,
        String nombreUsuarioRegistro,
        OffsetDateTime fechaMovimiento,
        String observacion
) {
}
