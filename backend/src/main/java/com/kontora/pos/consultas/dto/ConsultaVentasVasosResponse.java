package com.kontora.pos.consultas.dto;

import java.time.LocalDate;
import java.util.UUID;

public record ConsultaVentasVasosResponse(
        UUID idCajaDiaria,
        LocalDate fechaOperacion,
        String nombreTipo,
        Integer onzas,
        Long vasosVendidos
) {
}
