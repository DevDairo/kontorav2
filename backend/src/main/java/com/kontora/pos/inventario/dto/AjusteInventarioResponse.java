package com.kontora.pos.inventario.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AjusteInventarioResponse(
        UUID idAjusteInventario,
        UUID idItemInventario,
        String nombreItem,
        UUID idCajaDiaria,
        String tipoStock,
        Integer cantidadAjuste,
        String sentidoAjuste,
        String motivoAjuste,
        String estadoAprobacion,
        UUID idUsuarioSolicitante,
        String nombreUsuarioSolicitante,
        UUID idUsuarioAprobador,
        String nombreUsuarioAprobador,
        OffsetDateTime fechaSolicitud,
        OffsetDateTime fechaAprobacion,
        String observacionAprobacion
) {
}
