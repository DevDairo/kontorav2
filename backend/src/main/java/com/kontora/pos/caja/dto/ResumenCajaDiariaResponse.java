package com.kontora.pos.caja.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ResumenCajaDiariaResponse(
        UUID idCajaDiaria,
        LocalDate fechaOperacion,
        BigDecimal valorBase,
        BigDecimal totalVentas,
        BigDecimal totalVentasEfectivo,
        BigDecimal totalVentasTransferencia,
        BigDecimal totalTransferenciasPendientes,
        BigDecimal totalTransferenciasValidadas,
        BigDecimal totalTransferenciasRechazadas,
        BigDecimal totalGastos,
        BigDecimal totalAdiciones,
        boolean adicionDiariaRegistrada,
        BigDecimal totalPagoTrabajadores,
        boolean pagoTrabajadoresRegistrado,
        boolean pagoTrabajadoresConfirmado,
        BigDecimal efectivoEsperadoSinBase,
        boolean listoParaCierre
) {
}
