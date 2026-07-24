package com.kontora.pos.caja.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record GastoCajaResponse(
        UUID idGastoCaja,
        UUID idCajaDiaria,
        BigDecimal valorGasto,
        String descripcion,
        String estadoGasto,
        UUID idUsuarioRegistro,
        String nombreUsuarioRegistro,
        OffsetDateTime fechaRegistro,
        UUID idUsuarioUltimaEdicion,
        String nombreUsuarioUltimaEdicion,
        OffsetDateTime fechaUltimaEdicion,
        String motivoEdicion,
        UUID idUsuarioAnulacion,
        String nombreUsuarioAnulacion,
        OffsetDateTime fechaAnulacion,
        String motivoAnulacion
) {
}
