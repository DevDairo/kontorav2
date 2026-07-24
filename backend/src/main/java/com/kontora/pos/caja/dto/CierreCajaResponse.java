package com.kontora.pos.caja.dto;

import com.kontora.pos.deposito.dto.MovimientoDepositoResponse;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CierreCajaResponse(
        UUID idCierreCaja,
        UUID idCajaDiaria,
        BigDecimal totalVentas,
        BigDecimal totalVentasEfectivo,
        BigDecimal totalVentasTransferencia,
        BigDecimal totalTransferenciasPendientes,
        BigDecimal totalTransferenciasValidadas,
        BigDecimal totalTransferenciasRechazadas,
        BigDecimal totalGastos,
        BigDecimal totalAdiciones,
        BigDecimal totalPagoTrabajadores,
        BigDecimal efectivoEsperadoSinBase,
        BigDecimal efectivoContadoSinBase,
        BigDecimal diferenciaCaja,
        BigDecimal valorADeposito,
        OffsetDateTime fechaCierre,
        UUID idUsuarioCierre,
        String nombreUsuarioCierre,
        String observaciones,
        MovimientoDepositoResponse movimientoDeposito
) {
}
