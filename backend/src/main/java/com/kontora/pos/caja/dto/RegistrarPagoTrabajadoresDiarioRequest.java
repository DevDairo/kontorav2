package com.kontora.pos.caja.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RegistrarPagoTrabajadoresDiarioRequest(
        @NotNull @DecimalMin(value = "0.00") BigDecimal valorTotalPagado,
        String descripcion,
        Boolean confirmadoParaCierre
) {
}
