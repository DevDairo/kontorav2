package com.kontora.pos.caja.controller;

import com.kontora.pos.caja.dto.AdicionDiariaResponse;
import com.kontora.pos.caja.service.OperacionesCajaService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OperacionesCajaControllerTest {

    @Test
    void respondeSinContenidoCuandoCajaAbiertaNoTieneAdiciones() {
        OperacionesCajaService service = mock(OperacionesCajaService.class);
        when(service.obtenerAdicionDiariaCajaAbierta()).thenReturn(Optional.empty());
        OperacionesCajaController controller = new OperacionesCajaController(service);

        ResponseEntity<AdicionDiariaResponse> response = controller.obtenerAdicionDiariaCajaAbierta();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
    }
}
