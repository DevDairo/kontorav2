package com.kontora.pos.consultas.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ConsultaTransferenciaResponse(
        UUID idPagoVenta,
        UUID idVenta,
        UUID idCajaDiaria,
        LocalDate fechaOperacion,
        Long numeroVenta,
        UUID idUsuarioVendedor,
        String nombreUsuarioVendedor,
        BigDecimal valorPago,
        String estadoValidacion,
        OffsetDateTime fechaRegistro,
        UUID idUsuarioValidacion,
        String nombreUsuarioValidacion,
        OffsetDateTime fechaValidacion,
        String observacionValidacion,
        Long cantidadEvidencias
) {
}
