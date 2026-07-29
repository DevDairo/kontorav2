package com.kontora.pos.cortesias.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CortesiaResponse(
        UUID idCortesia,
        UUID idCajaDiaria,
        UUID idUsuarioRegistro,
        String nombreUsuarioRegistro,
        String tipoBeneficiario,
        UUID idUsuarioBeneficiario,
        String nombreUsuarioBeneficiario,
        String referenciaOtro,
        String motivoOtro,
        OffsetDateTime fechaRegistro,
        String estado,
        UUID idUsuarioAnulacion,
        String nombreUsuarioAnulacion,
        OffsetDateTime fechaAnulacion,
        String motivoAnulacion,
        List<DetalleCortesiaResponse> detalles
) {
}
