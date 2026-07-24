package com.kontora.pos.consultas.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ConsultaMovimientoInventarioResponse(
        UUID idMovimientoInventario,
        UUID idItemInventario,
        String nombreItem,
        UUID idCajaDiaria,
        LocalDate fechaOperacion,
        String tipoStock,
        String tipoMovimiento,
        Integer cantidad,
        String sentidoMovimiento,
        String referenciaOrigen,
        UUID idReferenciaOrigen,
        String observacion,
        UUID idUsuarioRegistro,
        String nombreUsuarioRegistro,
        OffsetDateTime fechaMovimiento
) {
}
