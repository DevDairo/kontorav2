package com.kontora.pos.evidencias.storage;

import com.kontora.pos.common.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SupabaseStorageClientTest {

    @Test
    void rechazaCargaCuandoFaltaConfiguracionRest() {
        SupabaseStorageProperties properties = new SupabaseStorageProperties();
        SupabaseStorageClient client = new SupabaseStorageClient(properties);

        assertThatThrownBy(() -> client.subir("pagos-venta/archivo.jpg", "image/jpeg", new byte[]{1}))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    org.assertj.core.api.Assertions.assertThat(exception.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    org.assertj.core.api.Assertions.assertThat(exception.getMessage()).isEqualTo("Supabase Storage no esta configurado");
                });
    }

    @Test
    void rechazaDescargaCuandoFaltaConfiguracionRest() {
        SupabaseStorageProperties properties = new SupabaseStorageProperties();
        SupabaseStorageClient client = new SupabaseStorageClient(properties);

        assertThatThrownBy(() -> client.descargar("supabase://evidencias/pagos-venta/archivo.jpg"))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    org.assertj.core.api.Assertions.assertThat(exception.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    org.assertj.core.api.Assertions.assertThat(exception.getMessage()).isEqualTo("Supabase Storage no esta configurado");
                });
    }

    @Test
    void usaSinPrefijoLaUrlDirectaDelContenedorStorage() {
        SupabaseStorageProperties properties = new SupabaseStorageProperties();
        properties.setApiUrl("http://storage:5000/");

        SupabaseStorageClient client = new SupabaseStorageClient(properties);

        assertThat(client.storageApiBaseUrl()).isEqualTo("http://storage:5000");
    }

    @Test
    void conservaCompatibilidadConLaUrlDeProyectoSupabaseCloud() {
        SupabaseStorageProperties properties = new SupabaseStorageProperties();
        properties.setProjectUrl("https://proyecto.supabase.co/");

        SupabaseStorageClient client = new SupabaseStorageClient(properties);

        assertThat(client.storageApiBaseUrl())
                .isEqualTo("https://proyecto.supabase.co/storage/v1");
    }
}
