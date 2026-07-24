package com.kontora.pos.consultas.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ConsultaInventarioActualResponse(
        UUID idItemInventario,
        String nombreItem,
        String nombreCategoria,
        String nombreUnidad,
        String tipoControl,
        UUID idTamanoVaso,
        Integer onzas,
        Integer cantidadActualGeneral,
        OffsetDateTime fechaActualizacionGeneral,
        UUID idCajaDiariaAbierta,
        Integer cantidadInicialDiaria,
        Integer cantidadIngresadaDiaria,
        Integer cantidadVendidaDiaria,
        Integer cantidadPerdidaDiaria,
        Integer cantidadAjustadaDiaria,
        Integer cantidadFinalTeoricaDiaria
) {
}
