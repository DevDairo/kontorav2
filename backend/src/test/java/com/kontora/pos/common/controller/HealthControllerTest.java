package com.kontora.pos.common.controller;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HealthControllerTest {

    @Test
    void healthResponseMatchesContract() throws Exception {
        Class<?> controllerType = Class.forName("com.kontora.pos.common.controller.HealthController");
        Object controller = controllerType.getConstructor().newInstance();

        Object response = controllerType.getMethod("health").invoke(controller);
        Object status = response.getClass().getMethod("status").invoke(response);
        Object service = response.getClass().getMethod("service").invoke(response);

        assertThat(status).isEqualTo("ok");
        assertThat(service).isEqualTo("kontora-pos-backend");
    }
}
