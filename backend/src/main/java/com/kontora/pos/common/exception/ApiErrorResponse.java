package com.kontora.pos.common.exception;

import java.time.OffsetDateTime;

public record ApiErrorResponse(
        String mensaje,
        int estado,
        OffsetDateTime fecha
) {
}

