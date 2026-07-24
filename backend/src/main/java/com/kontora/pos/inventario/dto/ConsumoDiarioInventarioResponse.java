package com.kontora.pos.inventario.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ConsumoDiarioInventarioResponse(
        UUID idConsumoDiarioInventario,
        UUID idCajaDiaria,
        UUID idItemInventario,
        String nombreItem,
        Integer cantidadConsumida,
        UUID idUsuarioRegistro,
        String nombreUsuarioRegistro,
        OffsetDateTime fechaRegistro,
        String observacion
) {
}
