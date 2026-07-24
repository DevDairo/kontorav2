package com.kontora.pos.inventario.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MovimientoInventarioResponse(
        UUID idMovimientoInventario,
        UUID idItemInventario,
        String nombreItem,
        UUID idCajaDiaria,
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
