package com.kontora.pos.ventas.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record RegistrarVentaRequest(
        @NotBlank String tipoComprador,
        UUID idUsuarioComprador,
        @NotEmpty List<@Valid RegistrarDetalleVentaRequest> detalles,
        @NotEmpty List<@Valid RegistrarPagoVentaRequest> pagos
) {
}
