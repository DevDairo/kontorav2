package com.kontora.pos.catalogos.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CrearPrecioGranizadoRequest(
        @NotNull(message = "El tipo de granizado es obligatorio")
        UUID idTipoGranizado,
        @NotNull(message = "El tamano de vaso es obligatorio")
        UUID idTamanoVaso,
        @NotNull(message = "El valor del precio es obligatorio")
        @DecimalMin(value = "0.01", message = "El valor del precio debe ser mayor que cero")
        @Digits(integer = 10, fraction = 2, message = "El valor del precio no es valido")
        BigDecimal valorPrecio,
        @NotNull(message = "La fecha de inicio es obligatoria")
        LocalDate fechaInicioVigencia
) {
}
