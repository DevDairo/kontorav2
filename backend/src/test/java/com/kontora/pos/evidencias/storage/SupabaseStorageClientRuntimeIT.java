package com.kontora.pos.evidencias.storage;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SupabaseStorageClientRuntimeIT {

    @Test
    void cargaYDescargaContraStorageRealPorLaRedDocker() throws Exception {
        String apiUrl = variableRequerida("SUPABASE_STORAGE_API_URL");
        String serviceRoleKey = variableRequerida("STORAGE_SERVICE_ROLE_KEY");
        String bucket = variableRequerida("SUPABASE_STORAGE_BUCKET");
        String rutaArchivo = "pruebas/backend-runtime-" + UUID.randomUUID() + ".pdf";
        byte[] contenido = "%PDF-1.4\n% Kontora Storage runtime test\n".getBytes(StandardCharsets.UTF_8);

        SupabaseStorageProperties properties = new SupabaseStorageProperties();
        properties.setApiUrl(apiUrl);
        properties.setServiceRoleKey(serviceRoleKey);
        properties.setBucket(bucket);
        SupabaseStorageClient client = new SupabaseStorageClient(properties);

        boolean objetoCreado = false;
        try {
            ArchivoAlmacenado almacenado = client.subir(
                    rutaArchivo,
                    "application/pdf",
                    contenido);
            objetoCreado = true;

            assertThat(almacenado.urlArchivo())
                    .isEqualTo("supabase://" + bucket + "/" + rutaArchivo);

            ArchivoDescargado descargado = client.descargar(almacenado.urlArchivo());

            assertThat(descargado.contenido()).isEqualTo(contenido);
            assertThat(descargado.contentType()).startsWith("application/pdf");
        } finally {
            if (objetoCreado) {
                eliminarObjeto(apiUrl, serviceRoleKey, bucket, rutaArchivo);
            }
        }
    }

    private void eliminarObjeto(
            String apiUrl,
            String serviceRoleKey,
            String bucket,
            String rutaArchivo) throws Exception {
        String baseUrl = apiUrl.replaceAll("/+$", "");
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create(baseUrl + "/object/" + bucket + "/" + rutaArchivo))
                .header("Authorization", "Bearer " + serviceRoleKey)
                .header("apikey", serviceRoleKey)
                .DELETE()
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode())
                .withFailMessage(
                        "No fue posible eliminar el objeto temporal. HTTP %s: %s",
                        response.statusCode(),
                        response.body())
                .isBetween(200, 299);
    }

    private String variableRequerida(String nombre) {
        String value = System.getenv(nombre);
        assertThat(value)
                .withFailMessage("Falta la variable de entorno %s", nombre)
                .isNotBlank();
        return value.trim();
    }
}
