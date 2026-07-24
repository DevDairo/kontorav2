package com.kontora.pos.inventario.dto;

import jakarta.validation.constraints.Size;

public record ResolverAjusteInventarioRequest(
        @Size(max = 1000) String observacionAprobacion
) {
}
