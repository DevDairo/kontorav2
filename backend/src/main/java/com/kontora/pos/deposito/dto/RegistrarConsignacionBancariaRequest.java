package com.kontora.pos.deposito.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RegistrarConsignacionBancariaRequest(
        @NotNull(message = "valorConsignado es obligatorio")
        @Positive(message = "valorConsignado debe ser mayor que cero")
        @Digits(integer = 10, fraction = 2, message = "valorConsignado debe tener maximo dos decimales")
        BigDecimal valorConsignado,
        @Size(max = 1000, message = "observacion no puede superar 1000 caracteres")
        String observacion
) {
}
