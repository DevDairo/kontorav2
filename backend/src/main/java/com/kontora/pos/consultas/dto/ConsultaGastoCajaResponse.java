package com.kontora.pos.consultas.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ConsultaGastoCajaResponse(
        UUID idGastoCaja,
        UUID idCajaDiaria,
        LocalDate fechaOperacion,
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
