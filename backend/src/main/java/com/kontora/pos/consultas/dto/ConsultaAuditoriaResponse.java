package com.kontora.pos.consultas.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ConsultaAuditoriaResponse(
        UUID idAuditoriaOperacion,
        UUID idUsuario,
        String nombreUsuario,
        String tablaAfectada,
        String idRegistroAfectado,
        String accion,
        String valorAnterior,
        String valorNuevo,
        OffsetDateTime fechaAccion,
        String direccionIp,
        String descripcion
) {
}
