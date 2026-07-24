package com.kontora.pos.inventario.dto;

import java.util.UUID;

public record ExistenciaInventarioDiarioResponse(
        UUID idExistenciaDiaria,
        UUID idCajaDiaria,
        UUID idItemInventario,
        String nombreItem,
        UUID idTamanoVaso,
        Integer onzas,
        Integer cantidadInicial,
        Integer cantidadIngresada,
        Integer cantidadVendida,
        Integer cantidadPerdida,
        Integer cantidadAjustada,
        Integer cantidadFinalTeorica,
        Integer cantidadFinalContada,
        Integer diferencia
) {
}
