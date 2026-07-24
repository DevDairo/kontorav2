package com.kontora.pos.deposito.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PagoServicioResponse(
        UUID idPagoServicio,
        UUID idTipoServicio,
        String nombreServicio,
        BigDecimal valorPagado,
        String descripcion,
        OffsetDateTime fechaPago,
        UUID idUsuarioRegistro,
        String nombreUsuarioRegistro,
        String estado,
        MovimientoDepositoResponse movimientoDeposito
) {
}
