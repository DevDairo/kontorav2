package com.kontora.pos.evidencias.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ArchivoEvidenciaResponse(
        UUID idArchivoEvidencia,
        UUID idPagoVenta,
        UUID idGastoCaja,
        UUID idConsignacionBancaria,
        UUID idPagoServicio,
        String urlArchivo,
        String nombreArchivo,
        String tipoArchivo,
        String formatoArchivo,
        Integer tamanoOriginalKb,
        Integer tamanoComprimidoKb,
        boolean fueComprimido,
        OffsetDateTime fechaSubida,
        UUID idUsuarioSubida,
        String nombreUsuarioSubida,
        String estado
) {
}
