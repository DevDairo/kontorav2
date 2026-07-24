package com.kontora.pos.inventario.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PaqueteVasosAbiertoResponse(
        UUID idPaqueteVasosAbierto,
        UUID idCajaDiaria,
        UUID idItemInventario,
        String nombreItem,
        Integer cantidadPaquetes,
        Integer unidadesPorPaquete,
        Integer unidadesGeneradas,
        Integer unidadesRotas,
        Integer unidadesDisponibles,
        UUID idUsuarioRegistro,
        String nombreUsuarioRegistro,
        OffsetDateTime fechaRegistro
) {
}
