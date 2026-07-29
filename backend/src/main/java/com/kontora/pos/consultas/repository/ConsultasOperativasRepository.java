package com.kontora.pos.consultas.repository;

import com.kontora.pos.consultas.dto.ConsultaAuditoriaResponse;
import com.kontora.pos.consultas.dto.ConsultaCierreDiarioResponse;
import com.kontora.pos.consultas.dto.ConsultaDetalleVentaResponse;
import com.kontora.pos.consultas.dto.ConsultaGastoCajaResponse;
import com.kontora.pos.consultas.dto.ConsultaInventarioActualResponse;
import com.kontora.pos.consultas.dto.ConsultaMovimientoDepositoResponse;
import com.kontora.pos.consultas.dto.ConsultaMovimientoInventarioResponse;
import com.kontora.pos.consultas.dto.ConsultaTransferenciaResponse;
import com.kontora.pos.consultas.dto.ConsultaVentaResponse;
import com.kontora.pos.consultas.dto.ConsultaVentasVasosResponse;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ConsultasOperativasRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ConsultasOperativasRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ConsultaVentaResponse> consultarVentas(
            LocalDate fechaInicio,
            LocalDate fechaFin,
            UUID idUsuarioVendedor) {
        MapSqlParameterSource params = parametrosPeriodo(fechaInicio, fechaFin);
        String filtroVendedor = "";
        if (idUsuarioVendedor != null) {
            params.addValue("idUsuarioVendedor", idUsuarioVendedor);
            filtroVendedor = "AND v.id_usuario_vendedor = :idUsuarioVendedor\n";
        }

        String sql = """
                SELECT
                    v.id_venta,
                    v.id_caja_diaria,
                    cd.fecha_operacion,
                    v.numero_venta,
                    v.fecha_venta,
                    v.estado_venta::text AS estado_venta,
                    v.tipo_comprador::text AS tipo_comprador,
                    uv.id_usuario AS id_usuario_vendedor,
                    uv.nombre_usuario AS nombre_usuario_vendedor,
                    v.subtotal_venta,
                    v.descuento_promocion,
                    v.total_venta,
                    COALESCE(SUM(pv.valor_pago), 0) AS total_pagado,
                    COALESCE(SUM(CASE WHEN mp.nombre_metodo = 'efectivo' THEN pv.valor_pago ELSE 0 END), 0) AS total_efectivo,
                    COALESCE(SUM(CASE WHEN mp.nombre_metodo = 'transferencia' THEN pv.valor_pago ELSE 0 END), 0) AS total_transferencia
                FROM ventas v
                JOIN cajas_diarias cd ON cd.id_caja_diaria = v.id_caja_diaria
                JOIN usuarios uv ON uv.id_usuario = v.id_usuario_vendedor
                LEFT JOIN pagos_venta pv ON pv.id_venta = v.id_venta
                LEFT JOIN metodos_pago mp ON mp.id_metodo_pago = pv.id_metodo_pago
                WHERE cd.fecha_operacion BETWEEN :fechaInicio AND :fechaFin
                %s
                GROUP BY
                    v.id_venta,
                    v.id_caja_diaria,
                    cd.fecha_operacion,
                    v.numero_venta,
                    v.fecha_venta,
                    v.estado_venta,
                    v.tipo_comprador,
                    uv.id_usuario,
                    uv.nombre_usuario,
                    v.subtotal_venta,
                    v.descuento_promocion,
                    v.total_venta
                ORDER BY cd.fecha_operacion DESC, v.fecha_venta DESC
                """.formatted(filtroVendedor);
        List<ConsultaVentaResponse> ventas = jdbcTemplate.query(sql, params, ventaMapper());
        if (ventas.isEmpty()) {
            return ventas;
        }

        Map<UUID, List<ConsultaDetalleVentaResponse>> detallesPorVenta =
                consultarDetallesVentas(ventas.stream().map(ConsultaVentaResponse::idVenta).toList());
        return ventas.stream()
                .map(venta -> venta.conDetalles(detallesPorVenta.getOrDefault(venta.idVenta(), List.of())))
                .toList();
    }

    private Map<UUID, List<ConsultaDetalleVentaResponse>> consultarDetallesVentas(List<UUID> idsVentas) {
        String sql = """
                SELECT
                    dv.id_venta,
                    dv.id_detalle_venta,
                    tg.nombre_tipo,
                    tv.onzas,
                    dv.cantidad
                FROM detalles_venta dv
                JOIN tipos_granizado tg ON tg.id_tipo_granizado = dv.id_tipo_granizado
                JOIN tamanos_vaso tv ON tv.id_tamano_vaso = dv.id_tamano_vaso
                WHERE dv.id_venta IN (:idsVentas)
                ORDER BY dv.id_venta, tv.onzas, tg.nombre_tipo
                """;
        MapSqlParameterSource params = new MapSqlParameterSource("idsVentas", idsVentas);
        return jdbcTemplate.query(sql, params, rs -> {
            Map<UUID, List<ConsultaDetalleVentaResponse>> detalles = new HashMap<>();
            while (rs.next()) {
                UUID idVenta = uuid(rs, "id_venta");
                detalles.computeIfAbsent(idVenta, ignored -> new ArrayList<>())
                        .add(new ConsultaDetalleVentaResponse(
                                uuid(rs, "id_detalle_venta"),
                                rs.getString("nombre_tipo"),
                                integer(rs, "onzas"),
                                integer(rs, "cantidad")));
            }
            return detalles;
        });
    }

    public Optional<ConsultaCierreDiarioResponse> consultarCierrePorFecha(LocalDate fechaOperacion) {
        MapSqlParameterSource params = new MapSqlParameterSource("fechaOperacion", fechaOperacion);
        String sql = """
                SELECT
                    cd.id_caja_diaria,
                    cd.fecha_operacion,
                    cd.estado_caja::text AS estado_caja,
                    cd.valor_base,
                    cc.id_cierre_caja,
                    cc.total_ventas,
                    cc.total_ventas_efectivo,
                    cc.total_ventas_transferencia,
                    cc.total_transferencias_pendientes,
                    cc.total_transferencias_validadas,
                    cc.total_transferencias_rechazadas,
                    cc.total_gastos,
                    cc.total_adiciones,
                    cc.total_pago_trabajadores,
                    cc.efectivo_esperado_sin_base,
                    cc.efectivo_contado_sin_base,
                    cc.diferencia_caja,
                    cc.valor_a_deposito,
                    cc.fecha_cierre,
                    uc.id_usuario AS id_usuario_cierre,
                    uc.nombre_usuario AS nombre_usuario_cierre,
                    cc.observaciones
                FROM cajas_diarias cd
                JOIN cierres_caja cc ON cc.id_caja_diaria = cd.id_caja_diaria
                JOIN usuarios uc ON uc.id_usuario = cc.id_usuario_cierre
                WHERE cd.fecha_operacion = :fechaOperacion
                """;
        return jdbcTemplate.query(sql, params, cierreMapper()).stream().findFirst();
    }

    public List<ConsultaGastoCajaResponse> consultarGastos(
            LocalDate fechaInicio,
            LocalDate fechaFin,
            UUID idUsuarioRegistro) {
        MapSqlParameterSource params = parametrosPeriodo(fechaInicio, fechaFin);
        String filtroUsuario = "";
        if (idUsuarioRegistro != null) {
            params.addValue("idUsuarioRegistro", idUsuarioRegistro);
            filtroUsuario = "AND gc.id_usuario_registro = :idUsuarioRegistro\n";
        }

        String sql = """
                SELECT
                    gc.id_gasto_caja,
                    gc.id_caja_diaria,
                    cd.fecha_operacion,
                    gc.valor_gasto,
                    gc.descripcion,
                    gc.estado_gasto::text AS estado_gasto,
                    ur.id_usuario AS id_usuario_registro,
                    ur.nombre_usuario AS nombre_usuario_registro,
                    gc.fecha_registro,
                    ue.id_usuario AS id_usuario_ultima_edicion,
                    ue.nombre_usuario AS nombre_usuario_ultima_edicion,
                    gc.fecha_ultima_edicion,
                    gc.motivo_edicion,
                    ua.id_usuario AS id_usuario_anulacion,
                    ua.nombre_usuario AS nombre_usuario_anulacion,
                    gc.fecha_anulacion,
                    gc.motivo_anulacion
                FROM gastos_caja gc
                JOIN cajas_diarias cd ON cd.id_caja_diaria = gc.id_caja_diaria
                JOIN usuarios ur ON ur.id_usuario = gc.id_usuario_registro
                LEFT JOIN usuarios ue ON ue.id_usuario = gc.id_usuario_ultima_edicion
                LEFT JOIN usuarios ua ON ua.id_usuario = gc.id_usuario_anulacion
                WHERE cd.fecha_operacion BETWEEN :fechaInicio AND :fechaFin
                %s
                ORDER BY cd.fecha_operacion DESC, gc.fecha_registro DESC
                """.formatted(filtroUsuario);
        return jdbcTemplate.query(sql, params, gastoMapper());
    }

    public List<ConsultaInventarioActualResponse> consultarInventarioActual() {
        String sql = """
                WITH caja_abierta AS (
                    SELECT id_caja_diaria
                    FROM cajas_diarias
                    WHERE estado_caja = 'abierta'
                    ORDER BY fecha_operacion DESC
                    LIMIT 1
                )
                SELECT
                    ii.id_item_inventario,
                    ii.nombre_item,
                    ci.nombre_categoria,
                    um.nombre_unidad,
                    ii.tipo_control::text AS tipo_control,
                    tv.id_tamano_vaso,
                    tv.onzas,
                    COALESCE(eg.cantidad_actual, 0) AS cantidad_actual_general,
                    eg.fecha_actualizacion AS fecha_actualizacion_general,
                    ed.id_caja_diaria AS id_caja_diaria_abierta,
                    ed.cantidad_inicial AS cantidad_inicial_diaria,
                    ed.cantidad_ingresada AS cantidad_ingresada_diaria,
                    ed.cantidad_vendida AS cantidad_vendida_diaria,
                    ed.cantidad_perdida AS cantidad_perdida_diaria,
                    ed.cantidad_cortesia AS cantidad_cortesia_diaria,
                    ed.cantidad_ajustada AS cantidad_ajustada_diaria,
                    ed.cantidad_final_teorica AS cantidad_final_teorica_diaria
                FROM items_inventario ii
                JOIN categorias_inventario ci ON ci.id_categoria_inventario = ii.id_categoria_inventario
                JOIN unidades_medida um ON um.id_unidad_medida = ii.id_unidad_medida
                LEFT JOIN tamanos_vaso tv ON tv.id_tamano_vaso = ii.id_tamano_vaso
                LEFT JOIN existencias_inventario_general eg ON eg.id_item_inventario = ii.id_item_inventario
                LEFT JOIN caja_abierta ca ON true
                LEFT JOIN existencias_inventario_diario ed
                    ON ed.id_item_inventario = ii.id_item_inventario
                    AND ed.id_caja_diaria = ca.id_caja_diaria
                WHERE ii.estado = 'activo'
                ORDER BY ci.nombre_categoria, ii.nombre_item
                """;
        return jdbcTemplate.query(sql, inventarioActualMapper());
    }

    public List<ConsultaMovimientoInventarioResponse> consultarMovimientosInventario(
            LocalDate fechaInicio,
            LocalDate fechaFin,
            UUID idCajaDiaria,
            UUID idItemInventario) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        StringBuilder filtros = new StringBuilder();
        if (fechaInicio != null && fechaFin != null) {
            params.addValue("fechaInicio", fechaInicio);
            params.addValue("fechaFin", fechaFin);
            filtros.append("AND mi.fecha_movimiento::date BETWEEN :fechaInicio AND :fechaFin\n");
        }
        if (idCajaDiaria != null) {
            params.addValue("idCajaDiaria", idCajaDiaria);
            filtros.append("AND mi.id_caja_diaria = :idCajaDiaria\n");
        }
        if (idItemInventario != null) {
            params.addValue("idItemInventario", idItemInventario);
            filtros.append("AND mi.id_item_inventario = :idItemInventario\n");
        }

        String sql = """
                SELECT
                    mi.id_movimiento_inventario,
                    mi.id_item_inventario,
                    ii.nombre_item,
                    mi.id_caja_diaria,
                    cd.fecha_operacion,
                    mi.tipo_stock::text AS tipo_stock,
                    mi.tipo_movimiento::text AS tipo_movimiento,
                    mi.cantidad,
                    mi.sentido_movimiento::text AS sentido_movimiento,
                    mi.referencia_origen,
                    mi.id_referencia_origen,
                    mi.observacion,
                    ur.id_usuario AS id_usuario_registro,
                    ur.nombre_usuario AS nombre_usuario_registro,
                    mi.fecha_movimiento
                FROM movimientos_inventario mi
                JOIN items_inventario ii ON ii.id_item_inventario = mi.id_item_inventario
                JOIN usuarios ur ON ur.id_usuario = mi.id_usuario_registro
                LEFT JOIN cajas_diarias cd ON cd.id_caja_diaria = mi.id_caja_diaria
                WHERE 1 = 1
                %s
                ORDER BY mi.fecha_movimiento DESC
                """.formatted(filtros);
        return jdbcTemplate.query(sql, params, movimientoInventarioMapper());
    }

    public List<ConsultaVentasVasosResponse> consultarVentasVasos(
            LocalDate fechaInicio,
            LocalDate fechaFin) {
        MapSqlParameterSource params = parametrosPeriodo(fechaInicio, fechaFin);
        String sql = """
                SELECT
                    v.id_caja_diaria,
                    cd.fecha_operacion,
                    tg.nombre_tipo,
                    tv.onzas,
                    SUM(dv.cantidad) AS vasos_vendidos
                FROM ventas v
                JOIN cajas_diarias cd ON cd.id_caja_diaria = v.id_caja_diaria
                JOIN detalles_venta dv ON dv.id_venta = v.id_venta
                JOIN tipos_granizado tg ON tg.id_tipo_granizado = dv.id_tipo_granizado
                JOIN tamanos_vaso tv ON tv.id_tamano_vaso = dv.id_tamano_vaso
                WHERE cd.fecha_operacion BETWEEN :fechaInicio AND :fechaFin
                AND v.estado_venta = 'registrada'::estado_venta_enum
                GROUP BY v.id_caja_diaria, cd.fecha_operacion, tg.nombre_tipo, tv.onzas
                ORDER BY cd.fecha_operacion DESC, tv.onzas ASC, tg.nombre_tipo ASC
                """;
        return jdbcTemplate.query(sql, params, ventasVasosMapper());
    }

    public List<ConsultaMovimientoDepositoResponse> consultarMovimientosDeposito(
            LocalDate fechaInicio,
            LocalDate fechaFin) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        StringBuilder filtros = new StringBuilder();
        if (fechaInicio != null && fechaFin != null) {
            params.addValue("fechaInicio", fechaInicio);
            params.addValue("fechaFin", fechaFin);
            filtros.append("AND md.fecha_movimiento::date BETWEEN :fechaInicio AND :fechaFin\n");
        }
        String sql = """
                SELECT
                    md.id_movimiento_deposito,
                    md.tipo_movimiento_deposito::text AS tipo_movimiento_deposito,
                    md.valor_movimiento,
                    md.saldo_anterior,
                    md.saldo_posterior,
                    md.id_cierre_caja,
                    cb.id_consignacion_bancaria,
                    ps.id_pago_servicio,
                    ts.nombre_servicio,
                    ur.id_usuario AS id_usuario_registro,
                    ur.nombre_usuario AS nombre_usuario_registro,
                    md.fecha_movimiento,
                    md.observacion
                FROM movimientos_deposito md
                JOIN usuarios ur ON ur.id_usuario = md.id_usuario_registro
                LEFT JOIN consignaciones_bancarias cb ON cb.id_movimiento_deposito = md.id_movimiento_deposito
                LEFT JOIN pagos_servicios ps ON ps.id_movimiento_deposito = md.id_movimiento_deposito
                LEFT JOIN tipos_servicio ts ON ts.id_tipo_servicio = ps.id_tipo_servicio
                WHERE 1 = 1
                %s
                ORDER BY md.fecha_movimiento DESC, md.id_movimiento_deposito DESC
                """.formatted(filtros);
        return jdbcTemplate.query(sql, params, movimientoDepositoMapper());
    }

    public List<ConsultaTransferenciaResponse> consultarTransferencias(
            List<String> estadosValidacion,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            UUID idUsuarioVendedor) {
        MapSqlParameterSource params = new MapSqlParameterSource("estadosValidacion", estadosValidacion);
        StringBuilder filtros = new StringBuilder();
        if (fechaInicio != null && fechaFin != null) {
            params.addValue("fechaInicio", fechaInicio);
            params.addValue("fechaFin", fechaFin);
            filtros.append("AND cd.fecha_operacion BETWEEN :fechaInicio AND :fechaFin\n");
        }
        if (idUsuarioVendedor != null) {
            params.addValue("idUsuarioVendedor", idUsuarioVendedor);
            filtros.append("AND v.id_usuario_vendedor = :idUsuarioVendedor\n");
        }

        String sql = """
                SELECT
                    pv.id_pago_venta,
                    v.id_venta,
                    v.id_caja_diaria,
                    cd.fecha_operacion,
                    v.numero_venta,
                    uv.id_usuario AS id_usuario_vendedor,
                    uv.nombre_usuario AS nombre_usuario_vendedor,
                    pv.valor_pago,
                    pv.estado_validacion::text AS estado_validacion,
                    pv.fecha_registro,
                    uval.id_usuario AS id_usuario_validacion,
                    uval.nombre_usuario AS nombre_usuario_validacion,
                    pv.fecha_validacion,
                    pv.observacion_validacion,
                    COUNT(ae.id_archivo_evidencia) AS cantidad_evidencias
                FROM pagos_venta pv
                JOIN ventas v ON v.id_venta = pv.id_venta
                JOIN cajas_diarias cd ON cd.id_caja_diaria = v.id_caja_diaria
                JOIN usuarios uv ON uv.id_usuario = v.id_usuario_vendedor
                JOIN metodos_pago mp ON mp.id_metodo_pago = pv.id_metodo_pago
                LEFT JOIN usuarios uval ON uval.id_usuario = pv.id_usuario_validacion
                LEFT JOIN archivos_evidencia ae
                    ON ae.id_pago_venta = pv.id_pago_venta
                    AND ae.estado = 'activo'
                WHERE mp.nombre_metodo = 'transferencia'
                AND v.estado_venta = 'registrada'
                AND pv.estado_validacion::text IN (:estadosValidacion)
                %s
                GROUP BY
                    pv.id_pago_venta,
                    v.id_venta,
                    v.id_caja_diaria,
                    cd.fecha_operacion,
                    v.numero_venta,
                    uv.id_usuario,
                    uv.nombre_usuario,
                    pv.valor_pago,
                    pv.estado_validacion,
                    pv.fecha_registro,
                    uval.id_usuario,
                    uval.nombre_usuario,
                    pv.fecha_validacion,
                    pv.observacion_validacion
                ORDER BY cd.fecha_operacion DESC, pv.fecha_registro DESC
                """.formatted(filtros);
        return jdbcTemplate.query(sql, params, transferenciaMapper());
    }

    public List<ConsultaAuditoriaResponse> consultarAuditoria(
            LocalDate fechaInicio,
            LocalDate fechaFin,
            String tablaAfectada,
            String accion,
            boolean incluirSeguridad) {
        MapSqlParameterSource params = parametrosPeriodo(fechaInicio, fechaFin);
        StringBuilder filtros = new StringBuilder();
        if (tablaAfectada != null) {
            params.addValue("tablaAfectada", tablaAfectada);
            filtros.append("AND ao.tabla_afectada = :tablaAfectada\n");
        }
        if (accion != null) {
            params.addValue("accion", accion);
            filtros.append("AND ao.accion::text = :accion\n");
        }
        if (!incluirSeguridad) {
            filtros.append("AND ao.tabla_afectada <> 'sesiones_usuario'\n");
        }

        String sql = """
                SELECT
                    ao.id_auditoria_operacion,
                    ao.id_usuario,
                    u.nombre_usuario,
                    ao.tabla_afectada,
                    ao.id_registro_afectado,
                    ao.accion::text AS accion,
                    ao.valor_anterior::text AS valor_anterior,
                    ao.valor_nuevo::text AS valor_nuevo,
                    ao.fecha_accion,
                    ao.direccion_ip,
                    ao.descripcion
                FROM auditoria_operaciones ao
                LEFT JOIN usuarios u ON u.id_usuario = ao.id_usuario
                WHERE ao.fecha_accion::date BETWEEN :fechaInicio AND :fechaFin
                %s
                ORDER BY ao.fecha_accion DESC
                """.formatted(filtros);
        return jdbcTemplate.query(sql, params, auditoriaMapper());
    }

    private MapSqlParameterSource parametrosPeriodo(LocalDate fechaInicio, LocalDate fechaFin) {
        return new MapSqlParameterSource()
                .addValue("fechaInicio", fechaInicio)
                .addValue("fechaFin", fechaFin);
    }

    private RowMapper<ConsultaVentaResponse> ventaMapper() {
        return (rs, rowNum) -> new ConsultaVentaResponse(
                uuid(rs, "id_venta"),
                uuid(rs, "id_caja_diaria"),
                localDate(rs, "fecha_operacion"),
                longValue(rs, "numero_venta"),
                offsetDateTime(rs, "fecha_venta"),
                rs.getString("estado_venta"),
                rs.getString("tipo_comprador"),
                uuid(rs, "id_usuario_vendedor"),
                rs.getString("nombre_usuario_vendedor"),
                rs.getBigDecimal("subtotal_venta"),
                rs.getBigDecimal("descuento_promocion"),
                rs.getBigDecimal("total_venta"),
                rs.getBigDecimal("total_pagado"),
                rs.getBigDecimal("total_efectivo"),
                rs.getBigDecimal("total_transferencia"),
                List.of());
    }

    private RowMapper<ConsultaCierreDiarioResponse> cierreMapper() {
        return (rs, rowNum) -> new ConsultaCierreDiarioResponse(
                uuid(rs, "id_caja_diaria"),
                localDate(rs, "fecha_operacion"),
                rs.getString("estado_caja"),
                rs.getBigDecimal("valor_base"),
                uuid(rs, "id_cierre_caja"),
                rs.getBigDecimal("total_ventas"),
                rs.getBigDecimal("total_ventas_efectivo"),
                rs.getBigDecimal("total_ventas_transferencia"),
                rs.getBigDecimal("total_transferencias_pendientes"),
                rs.getBigDecimal("total_transferencias_validadas"),
                rs.getBigDecimal("total_transferencias_rechazadas"),
                rs.getBigDecimal("total_gastos"),
                rs.getBigDecimal("total_adiciones"),
                rs.getBigDecimal("total_pago_trabajadores"),
                rs.getBigDecimal("efectivo_esperado_sin_base"),
                rs.getBigDecimal("efectivo_contado_sin_base"),
                rs.getBigDecimal("diferencia_caja"),
                rs.getBigDecimal("valor_a_deposito"),
                offsetDateTime(rs, "fecha_cierre"),
                uuid(rs, "id_usuario_cierre"),
                rs.getString("nombre_usuario_cierre"),
                rs.getString("observaciones"));
    }

    private RowMapper<ConsultaGastoCajaResponse> gastoMapper() {
        return (rs, rowNum) -> new ConsultaGastoCajaResponse(
                uuid(rs, "id_gasto_caja"),
                uuid(rs, "id_caja_diaria"),
                localDate(rs, "fecha_operacion"),
                rs.getBigDecimal("valor_gasto"),
                rs.getString("descripcion"),
                rs.getString("estado_gasto"),
                uuid(rs, "id_usuario_registro"),
                rs.getString("nombre_usuario_registro"),
                offsetDateTime(rs, "fecha_registro"),
                uuid(rs, "id_usuario_ultima_edicion"),
                rs.getString("nombre_usuario_ultima_edicion"),
                offsetDateTime(rs, "fecha_ultima_edicion"),
                rs.getString("motivo_edicion"),
                uuid(rs, "id_usuario_anulacion"),
                rs.getString("nombre_usuario_anulacion"),
                offsetDateTime(rs, "fecha_anulacion"),
                rs.getString("motivo_anulacion"));
    }

    private RowMapper<ConsultaInventarioActualResponse> inventarioActualMapper() {
        return (rs, rowNum) -> new ConsultaInventarioActualResponse(
                uuid(rs, "id_item_inventario"),
                rs.getString("nombre_item"),
                rs.getString("nombre_categoria"),
                rs.getString("nombre_unidad"),
                rs.getString("tipo_control"),
                uuid(rs, "id_tamano_vaso"),
                integer(rs, "onzas"),
                integer(rs, "cantidad_actual_general"),
                offsetDateTime(rs, "fecha_actualizacion_general"),
                uuid(rs, "id_caja_diaria_abierta"),
                integer(rs, "cantidad_inicial_diaria"),
                integer(rs, "cantidad_ingresada_diaria"),
                integer(rs, "cantidad_vendida_diaria"),
                integer(rs, "cantidad_perdida_diaria"),
                integer(rs, "cantidad_cortesia_diaria"),
                integer(rs, "cantidad_ajustada_diaria"),
                integer(rs, "cantidad_final_teorica_diaria"));
    }

    private RowMapper<ConsultaMovimientoInventarioResponse> movimientoInventarioMapper() {
        return (rs, rowNum) -> new ConsultaMovimientoInventarioResponse(
                uuid(rs, "id_movimiento_inventario"),
                uuid(rs, "id_item_inventario"),
                rs.getString("nombre_item"),
                uuid(rs, "id_caja_diaria"),
                localDate(rs, "fecha_operacion"),
                rs.getString("tipo_stock"),
                rs.getString("tipo_movimiento"),
                integer(rs, "cantidad"),
                rs.getString("sentido_movimiento"),
                rs.getString("referencia_origen"),
                uuid(rs, "id_referencia_origen"),
                rs.getString("observacion"),
                uuid(rs, "id_usuario_registro"),
                rs.getString("nombre_usuario_registro"),
                offsetDateTime(rs, "fecha_movimiento"));
    }

    private RowMapper<ConsultaVentasVasosResponse> ventasVasosMapper() {
        return (rs, rowNum) -> new ConsultaVentasVasosResponse(
                uuid(rs, "id_caja_diaria"),
                localDate(rs, "fecha_operacion"),
                rs.getString("nombre_tipo"),
                integer(rs, "onzas"),
                longValue(rs, "vasos_vendidos"));
    }

    private RowMapper<ConsultaMovimientoDepositoResponse> movimientoDepositoMapper() {
        return (rs, rowNum) -> new ConsultaMovimientoDepositoResponse(
                uuid(rs, "id_movimiento_deposito"),
                rs.getString("tipo_movimiento_deposito"),
                rs.getBigDecimal("valor_movimiento"),
                rs.getBigDecimal("saldo_anterior"),
                rs.getBigDecimal("saldo_posterior"),
                uuid(rs, "id_cierre_caja"),
                uuid(rs, "id_consignacion_bancaria"),
                uuid(rs, "id_pago_servicio"),
                rs.getString("nombre_servicio"),
                uuid(rs, "id_usuario_registro"),
                rs.getString("nombre_usuario_registro"),
                offsetDateTime(rs, "fecha_movimiento"),
                rs.getString("observacion"));
    }

    private RowMapper<ConsultaTransferenciaResponse> transferenciaMapper() {
        return (rs, rowNum) -> new ConsultaTransferenciaResponse(
                uuid(rs, "id_pago_venta"),
                uuid(rs, "id_venta"),
                uuid(rs, "id_caja_diaria"),
                localDate(rs, "fecha_operacion"),
                longValue(rs, "numero_venta"),
                uuid(rs, "id_usuario_vendedor"),
                rs.getString("nombre_usuario_vendedor"),
                rs.getBigDecimal("valor_pago"),
                rs.getString("estado_validacion"),
                offsetDateTime(rs, "fecha_registro"),
                uuid(rs, "id_usuario_validacion"),
                rs.getString("nombre_usuario_validacion"),
                offsetDateTime(rs, "fecha_validacion"),
                rs.getString("observacion_validacion"),
                longValue(rs, "cantidad_evidencias"));
    }

    private RowMapper<ConsultaAuditoriaResponse> auditoriaMapper() {
        return (rs, rowNum) -> new ConsultaAuditoriaResponse(
                uuid(rs, "id_auditoria_operacion"),
                uuid(rs, "id_usuario"),
                rs.getString("nombre_usuario"),
                rs.getString("tabla_afectada"),
                rs.getString("id_registro_afectado"),
                rs.getString("accion"),
                rs.getString("valor_anterior"),
                rs.getString("valor_nuevo"),
                offsetDateTime(rs, "fecha_accion"),
                rs.getString("direccion_ip"),
                rs.getString("descripcion"));
    }

    private UUID uuid(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, UUID.class);
    }

    private LocalDate localDate(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, LocalDate.class);
    }

    private OffsetDateTime offsetDateTime(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, OffsetDateTime.class);
    }

    private Integer integer(ResultSet rs, String column) throws SQLException {
        Object value = rs.getObject(column);
        return value == null ? null : ((Number) value).intValue();
    }

    private Long longValue(ResultSet rs, String column) throws SQLException {
        Object value = rs.getObject(column);
        return value == null ? null : ((Number) value).longValue();
    }
}
