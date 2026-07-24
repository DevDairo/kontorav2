package com.kontora.pos.inventario.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ExistenciaInventarioGeneralResponse(
        UUID idExistenciaGeneral,
        UUID idItemInventario,
        String nombreItem,
        String tipoControl,
        UUID idTamanoVaso,
        Integer onzas,
        Integer cantidadActual,
        OffsetDateTime fechaActualizacion
) {
}
