package com.kontora.pos.evidencias.storage;

public interface EvidenciaStorageClient {

    ArchivoAlmacenado subir(String rutaArchivo, String contentType, byte[] contenido);

    ArchivoDescargado descargar(String urlArchivo);
}
