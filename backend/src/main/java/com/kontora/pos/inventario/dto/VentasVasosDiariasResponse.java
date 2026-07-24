package com.kontora.pos.inventario.dto;

import java.util.UUID;

public record VentasVasosDiariasResponse(
        UUID idCajaDiaria,
        String nombreTipo,
        Integer onzas,
        Long vasosVendidos
) {
}
