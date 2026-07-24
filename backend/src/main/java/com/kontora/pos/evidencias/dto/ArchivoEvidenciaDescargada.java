package com.kontora.pos.evidencias.dto;

public record ArchivoEvidenciaDescargada(
        byte[] contenido,
        String contentType,
        String nombreArchivo
) {
}
