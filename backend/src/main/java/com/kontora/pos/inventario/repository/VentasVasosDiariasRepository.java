package com.kontora.pos.inventario.repository;

import com.kontora.pos.inventario.dto.VentasVasosDiariasResponse;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class VentasVasosDiariasRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public VentasVasosDiariasRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<VentasVasosDiariasResponse> consultarPorCajaAbierta(UUID idCajaDiaria) {
        String sql = """
                SELECT
                    v.id_caja_diaria,
                    tg.nombre_tipo,
                    tv.onzas,
                    SUM(dv.cantidad) AS vasos_vendidos
                FROM ventas v
                JOIN detalles_venta dv ON dv.id_venta = v.id_venta
                JOIN tipos_granizado tg ON tg.id_tipo_granizado = dv.id_tipo_granizado
                JOIN tamanos_vaso tv ON tv.id_tamano_vaso = dv.id_tamano_vaso
                WHERE v.id_caja_diaria = :idCajaDiaria
                AND v.estado_venta = 'registrada'::estado_venta_enum
                GROUP BY v.id_caja_diaria, tg.nombre_tipo, tv.onzas
                ORDER BY tv.onzas ASC, tg.nombre_tipo ASC
                """;
        MapSqlParameterSource params = new MapSqlParameterSource("idCajaDiaria", idCajaDiaria);

        return jdbcTemplate.query(sql, params, (resultSet, rowNumber) -> new VentasVasosDiariasResponse(
                resultSet.getObject("id_caja_diaria", UUID.class),
                resultSet.getString("nombre_tipo"),
                resultSet.getInt("onzas"),
                resultSet.getLong("vasos_vendidos")));
    }
}
