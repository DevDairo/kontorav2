package com.kontora.pos.inventario.dto;

import com.kontora.pos.evidencias.dto.ArchivoEvidenciaResponse;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PerdidaInventarioResponse(
        UUID idPerdidaInventario,
        UUID idCajaDiaria,
        UUID idItemInventario,
        String nombreItem,
        UUID idTamanoVaso,
        Integer onzas,
        UUID idPaqueteVasosAbierto,
        Integer cantidad,
        String motivo,
        UUID idUsuarioRegistro,
        String nombreUsuarioRegistro,
        OffsetDateTime fechaRegistro,
        String estado,
        UUID idUsuarioAnulacion,
        String nombreUsuarioAnulacion,
        OffsetDateTime fechaAnulacion,
        String motivoAnulacion,
        List<ArchivoEvidenciaResponse> evidencias
) {
}
