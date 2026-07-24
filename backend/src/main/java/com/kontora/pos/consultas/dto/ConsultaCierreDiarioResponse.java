package com.kontora.pos.consultas.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ConsultaCierreDiarioResponse(
        UUID idCajaDiaria,
        LocalDate fechaOperacion,
        String estadoCaja,
        BigDecimal valorBase,
        UUID idCierreCaja,
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
        String observaciones
) {
}
