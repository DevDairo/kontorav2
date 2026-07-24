package com.kontora.pos.common.audit;

import java.util.LinkedHashMap;
import java.util.Map;

public final class AuditoriaValores {

    private AuditoriaValores() {
    }

    public static Map<String, Object> valores(Object... pares) {
        if (pares.length % 2 != 0) {
            throw new IllegalArgumentException("Los valores de auditoria deben enviarse en pares llave/valor");
        }
        Map<String, Object> valores = new LinkedHashMap<>();
        for (int i = 0; i < pares.length; i += 2) {
            valores.put((String) pares[i], pares[i + 1]);
        }
        return valores;
    }
}
