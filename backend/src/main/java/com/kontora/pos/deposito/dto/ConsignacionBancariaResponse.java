package com.kontora.pos.deposito.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ConsignacionBancariaResponse(
        UUID idConsignacionBancaria,
        BigDecimal valorConsignado,
        OffsetDateTime fechaConsignacion,
        UUID idUsuarioRegistro,
        String nombreUsuarioRegistro,
        String observacion,
        String estado,
        MovimientoDepositoResponse movimientoDeposito
) {
}
