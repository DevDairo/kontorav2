package com.kontora.pos.evidencias.storage;

import com.kontora.pos.common.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
public class SupabaseStorageClient implements EvidenciaStorageClient {

    private final SupabaseStorageProperties properties;
    private final HttpClient httpClient;

    public SupabaseStorageClient(SupabaseStorageProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public ArchivoAlmacenado subir(String rutaArchivo, String contentType, byte[] contenido) {
        validarConfiguracion();
        String bucket = properties.getBucket().trim();
        String serviceRoleKey = properties.getServiceRoleKey().trim();
        URI uri = URI.create(storageApiBaseUrl() + "/object/" + encode(bucket) + "/" + encodePath(rutaArchivo));

        HttpRequest request = HttpRequest.newBuilder(uri)
                .header("Authorization", "Bearer " + serviceRoleKey)
                .header("apikey", serviceRoleKey)
                .header("Content-Type", contentType)
                .header("x-upsert", "false")
                .POST(HttpRequest.BodyPublishers.ofByteArray(contenido))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "Supabase Storage rechazo la carga de evidencia");
            }
            return new ArchivoAlmacenado("supabase://" + bucket + "/" + rutaArchivo);
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "No fue posible conectar con Supabase Storage");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Carga de evidencia interrumpida");
        }
    }

    @Override
    public ArchivoDescargado descargar(String urlArchivo) {
        validarConfiguracion();
        String bucket = properties.getBucket().trim();
        String rutaArchivo = extraerRutaArchivo(urlArchivo, bucket);
        String serviceRoleKey = properties.getServiceRoleKey().trim();
        URI uri = URI.create(storageApiBaseUrl() + "/object/" + encode(bucket) + "/" + encodePath(rutaArchivo));

        HttpRequest request = HttpRequest.newBuilder(uri)
                .header("Authorization", "Bearer " + serviceRoleKey)
                .header("apikey", serviceRoleKey)
                .GET()
                .build();

        try {
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (objetoNoEncontrado(response)) {
                throw new ApiException(HttpStatus.NOT_FOUND, "Archivo de evidencia no encontrado en Storage");
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "Supabase Storage rechazo la descarga de evidencia");
            }
            String contentType = response.headers()
                    .firstValue("Content-Type")
                    .orElse("application/octet-stream");
            return new ArchivoDescargado(response.body(), contentType);
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "No fue posible conectar con Supabase Storage");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Descarga de evidencia interrumpida");
        }
    }

    private void validarConfiguracion() {
        if ((isBlank(properties.getApiUrl()) && isBlank(properties.getProjectUrl()))
                || isBlank(properties.getServiceRoleKey())
                || isBlank(properties.getBucket())) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Supabase Storage no esta configurado");
        }
    }

    String storageApiBaseUrl() {
        if (!isBlank(properties.getApiUrl())) {
            return quitarBarrasFinales(properties.getApiUrl().trim());
        }

        String baseUrl = quitarBarrasFinales(properties.getProjectUrl().trim());
        if (baseUrl.endsWith("/storage/v1")) {
            return baseUrl;
        }
        return baseUrl + "/storage/v1";
    }

    private String quitarBarrasFinales(String value) {
        String baseUrl = value;
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String encodePath(String path) {
        return Arrays.stream(path.split("/"))
                .map(this::encode)
                .collect(Collectors.joining("/"));
    }

    private String extraerRutaArchivo(String urlArchivo, String bucket) {
        String prefijo = "supabase://" + bucket + "/";
        if (urlArchivo == null || !urlArchivo.startsWith(prefijo) || urlArchivo.length() == prefijo.length()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "La ruta de evidencia almacenada no es valida");
        }
        return urlArchivo.substring(prefijo.length());
    }

    private boolean objetoNoEncontrado(HttpResponse<byte[]> response) {
        if (response.statusCode() == HttpStatus.NOT_FOUND.value()) {
            return true;
        }
        if (response.statusCode() != HttpStatus.BAD_REQUEST.value()) {
            return false;
        }

        String body = new String(response.body(), StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        return body.contains("\"statuscode\":\"404\"") || body.contains("\"statuscode\":404") || body.contains("\"error\":\"not_found\"");
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20");
    }
}
