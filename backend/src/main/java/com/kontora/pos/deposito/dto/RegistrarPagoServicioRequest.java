package com.kontora.pos.deposito.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record RegistrarPagoServicioRequest(
        @NotNull(message = "idTipoServicio es obligatorio")
        UUID idTipoServicio,
        @NotNull(message = "valorPagado es obligatorio")
        @Positive(message = "valorPagado debe ser mayor que cero")
        @Digits(integer = 10, fraction = 2, message = "valorPagado debe tener maximo dos decimales")
        BigDecimal valorPagado,
        @Size(max = 1000, message = "descripcion no puede superar 1000 caracteres")
        String descripcion
) {
}
