package com.kontora.pos.consultas.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class ConsultasOperativasIntegrationTest {

    private static final String PASSWORD = "Clave12345";
    private static final String USUARIO_ADMIN = "test_consultas_admin";
    private static final String USUARIO_GERENTE = "test_consultas_gerente";
    private static final String USUARIO_VENDEDOR = "test_consultas_vendedor";
    private static final String USUARIO_OTRO_VENDEDOR = "test_consultas_otro_vendedor";
    private static final LocalDate FECHA_CAJA = LocalDate.of(2310, 1, 1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private UUID idUsuarioAdmin;
    private UUID idUsuarioGerente;
    private UUID idUsuarioVendedor;
    private UUID idUsuarioOtroVendedor;

    @BeforeEach
    void setUp() {
        limpiarDatosDePrueba();
        idUsuarioAdmin = crearUsuarioConCredencial(USUARIO_ADMIN, "Administrador Consultas", "administrador");
        idUsuarioGerente = crearUsuarioConCredencial(USUARIO_GERENTE, "Gerente Consultas", "gerente");
        idUsuarioVendedor = crearUsuarioConCredencial(USUARIO_VENDEDOR, "Vendedor Consultas", "vendedor");
        idUsuarioOtroVendedor = crearUsuarioConCredencial(USUARIO_OTRO_VENDEDOR, "Otro Vendedor Consultas", "vendedor");
    }

    @AfterEach
    void tearDown() {
        limpiarDatosDePrueba();
    }

    @Test
    void vendedorSoloConsultaSusVentasGastosYTransferencias() throws Exception {
        UUID idCajaDiaria = crearCajaAbierta();
        UUID idVentaVendedor = crearVentaTransferencia(
                idCajaDiaria,
                idUsuarioVendedor,
                new BigDecimal("8000.00"),
                "pendiente");
        crearDetalleVenta(idVentaVendedor, "sin_licor", 8, 2);
        crearDetalleVenta(idVentaVendedor, "con_licor", 12, 21);
        crearVentaTransferencia(idCajaDiaria, idUsuarioOtroVendedor, new BigDecimal("12000.00"), "pendiente");
        crearGasto(idCajaDiaria, idUsuarioVendedor, new BigDecimal("3000.00"), "Gasto propio consultas");
        crearGasto(idCajaDiaria, idUsuarioOtroVendedor, new BigDecimal("5000.00"), "Gasto ajeno consultas");
        String tokenVendedor = iniciarSesion(USUARIO_VENDEDOR);

        mockMvc.perform(get("/api/consultas/ventas")
                        .param("fechaInicio", FECHA_CAJA.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenVendedor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nombreUsuarioVendedor").value(USUARIO_VENDEDOR))
                .andExpect(jsonPath("$[0].totalVenta").value(8000.00))
                .andExpect(jsonPath("$[0].fechaVenta").isNotEmpty())
                .andExpect(jsonPath("$[0].detalles", hasSize(2)))
                .andExpect(jsonPath("$[0].detalles[0].nombreTipo").value("sin_licor"))
                .andExpect(jsonPath("$[0].detalles[0].onzas").value(8))
                .andExpect(jsonPath("$[0].detalles[0].cantidad").value(2))
                .andExpect(jsonPath("$[0].detalles[1].nombreTipo").value("con_licor"))
                .andExpect(jsonPath("$[0].detalles[1].onzas").value(12))
                .andExpect(jsonPath("$[0].detalles[1].cantidad").value(21));

        mockMvc.perform(get("/api/consultas/gastos")
                        .param("fechaInicio", FECHA_CAJA.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenVendedor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].descripcion").value("Gasto propio consultas"));

        mockMvc.perform(get("/api/consultas/transferencias")
                        .param("estadoValidacion", "pendiente")
                        .param("fechaInicio", FECHA_CAJA.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenVendedor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nombreUsuarioVendedor").value(USUARIO_VENDEDOR))
                .andExpect(jsonPath("$[0].estadoValidacion").value("pendiente"));
    }

    @Test
    void consultaTransferenciasValidadasPorPeriodo() throws Exception {
        UUID idCajaDiaria = crearCajaAbierta();
        crearVentaTransferencia(idCajaDiaria, idUsuarioVendedor, new BigDecimal("11000.00"), "validada");
        crearVentaTransferencia(idCajaDiaria, idUsuarioVendedor, new BigDecimal("7000.00"), "pendiente");
        String tokenGerente = iniciarSesion(USUARIO_GERENTE);

        mockMvc.perform(get("/api/consultas/transferencias")
                        .param("estadoValidacion", "validada")
                        .param("fechaInicio", FECHA_CAJA.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenGerente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].estadoValidacion").value("validada"))
                .andExpect(jsonPath("$[0].valorPago").value(11000.00));
    }

    @Test
    void consultaTransferenciasExcluyeVentasAnuladas() throws Exception {
        UUID idCajaDiaria = crearCajaAbierta();
        crearVentaTransferencia(idCajaDiaria, idUsuarioVendedor, new BigDecimal("11000.00"), "pendiente");
        UUID idVentaAnulada = crearVentaTransferencia(
                idCajaDiaria,
                idUsuarioVendedor,
                new BigDecimal("7000.00"),
                "pendiente");
        jdbcTemplate.update(
                """
                UPDATE ventas
                SET estado_venta = 'anulada'::estado_venta_enum,
                    motivo_anulacion = 'Anulacion de prueba',
                    fecha_anulacion = ?,
                    id_usuario_anulacion = ?
                WHERE id_venta = ?
                """,
                fechaPrueba(),
                idUsuarioAdmin,
                idVentaAnulada);
        String tokenGerente = iniciarSesion(USUARIO_GERENTE);

        mockMvc.perform(get("/api/consultas/transferencias")
                        .param("estadoValidacion", "pendiente")
                        .param("fechaInicio", FECHA_CAJA.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenGerente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].valorPago").value(11000.00));
    }

    @Test
    void vendedorNoConsultaCierreDepositoNiAuditoria() throws Exception {
        String tokenVendedor = iniciarSesion(USUARIO_VENDEDOR);

        mockMvc.perform(get("/api/consultas/cierre")
                        .param("fecha", FECHA_CAJA.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenVendedor)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/consultas/deposito/movimientos")
                        .param("fechaInicio", FECHA_CAJA.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenVendedor)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/consultas/auditoria")
                        .param("fechaInicio", FECHA_CAJA.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenVendedor)))
                .andExpect(status().isForbidden());
    }

    @Test
    void administradorConsultaCierreYDepositoPeroNoAuditoria() throws Exception {
        UUID idCajaDiaria = crearCajaAbierta();
        crearVentaTransferencia(idCajaDiaria, idUsuarioVendedor, new BigDecimal("9000.00"), "pendiente");
        crearPrerequisitosCierre(idCajaDiaria);
        UUID idCierreCaja = crearCierreCaja(idCajaDiaria, new BigDecimal("9000.00"));
        crearMovimientoDeposito(idCierreCaja, new BigDecimal("9000.00"));
        crearAuditoria(idUsuarioAdmin, "ventas", "anular", "Auditoria operativa consultas");
        crearAuditoria(idUsuarioAdmin, "sesiones_usuario", "login", "Auditoria seguridad consultas");
        String tokenAdmin = iniciarSesion(USUARIO_ADMIN);

        mockMvc.perform(get("/api/consultas/cierre")
                        .param("fecha", FECHA_CAJA.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fechaOperacion").value(FECHA_CAJA.toString()))
                .andExpect(jsonPath("$.totalVentas").value(9000.00))
                .andExpect(jsonPath("$.valorADeposito").value(9000.00));

        mockMvc.perform(get("/api/consultas/deposito/movimientos")
                        .param("fechaInicio", FECHA_CAJA.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].tipoMovimientoDeposito").value("entrada_cierre"));

        mockMvc.perform(get("/api/consultas/auditoria")
                        .param("fechaInicio", FECHA_CAJA.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("Solo el gerente puede consultar auditoria"));
    }

    @Test
    void gerenteConsultaAuditoriaCompletaYTransferenciasRechazadas() throws Exception {
        UUID idCajaDiaria = crearCajaAbierta();
        crearVentaTransferencia(idCajaDiaria, idUsuarioVendedor, new BigDecimal("7000.00"), "rechazada");
        crearAuditoria(idUsuarioGerente, "sesiones_usuario", "login", "Auditoria completa consultas");
        String tokenGerente = iniciarSesion(USUARIO_GERENTE);

        mockMvc.perform(get("/api/consultas/auditoria")
                        .param("fechaInicio", FECHA_CAJA.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenGerente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].tablaAfectada", hasItem("sesiones_usuario")));

        mockMvc.perform(get("/api/consultas/transferencias")
                        .param("estadoValidacion", "rechazada")
                        .param("fechaInicio", FECHA_CAJA.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenGerente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].estadoValidacion").value("rechazada"))
                .andExpect(jsonPath("$[0].nombreUsuarioVendedor").value(USUARIO_VENDEDOR));
    }

    @Test
    void consultaInventarioActualYMovimientosInventario() throws Exception {
        UUID idCajaDiaria = crearCajaAbierta();
        UUID idItemInventario = idItemInventario("vaso_8oz");
        ajustarStockGeneral(idItemInventario, 25);
        crearExistenciaDiaria(idCajaDiaria, idItemInventario);
        crearMovimientoInventario(idCajaDiaria, idItemInventario);
        String tokenVendedor = iniciarSesion(USUARIO_VENDEDOR);

        mockMvc.perform(get("/api/consultas/inventario/actual")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenVendedor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nombreItem", hasItem("vaso_8oz")))
                .andExpect(jsonPath("$[*].cantidadActualGeneral", hasItem(25)))
                .andExpect(jsonPath("$[*].cantidadCortesiaDiaria", hasItem(0)));

        mockMvc.perform(get("/api/consultas/inventario/movimientos")
                        .param("fechaInicio", FECHA_CAJA.toString())
                        .param("idCajaDiaria", idCajaDiaria.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenVendedor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].tipoMovimiento").value("apertura_paquete"));
    }

    @Test
    void administradorConsultaVentasDeVasosPorTipoTamanoYExcluyeAnuladas() throws Exception {
        UUID idCajaDiaria = crearCajaAbierta();
        UUID ventaConLicor8 = crearVentaTransferencia(idCajaDiaria, idUsuarioVendedor, new BigDecimal("33000.00"), "pendiente");
        UUID ventaSinLicor8 = crearVentaTransferencia(idCajaDiaria, idUsuarioVendedor, new BigDecimal("8000.00"), "pendiente");
        UUID ventaConLicor12 = crearVentaTransferencia(idCajaDiaria, idUsuarioVendedor, new BigDecimal("56000.00"), "pendiente");
        UUID ventaAnulada = crearVentaTransferencia(idCajaDiaria, idUsuarioVendedor, new BigDecimal("4000.00"), "pendiente");
        crearDetalleVenta(ventaConLicor8, "con_licor", 8, 33);
        crearDetalleVenta(ventaSinLicor8, "sin_licor", 8, 8);
        crearDetalleVenta(ventaConLicor12, "con_licor", 12, 56);
        crearDetalleVenta(ventaAnulada, "con_licor", 8, 4);
        jdbcTemplate.update("""
                UPDATE ventas
                SET estado_venta = 'anulada'::estado_venta_enum,
                    motivo_anulacion = 'Anulacion de prueba',
                    fecha_anulacion = ?,
                    id_usuario_anulacion = ?
                WHERE id_venta = ?
                """, fechaPrueba(), idUsuarioAdmin, ventaAnulada);
        String tokenAdmin = iniciarSesion(USUARIO_ADMIN);
        String tokenVendedor = iniciarSesion(USUARIO_VENDEDOR);

        mockMvc.perform(get("/api/consultas/inventario/ventas-vasos")
                        .param("fechaInicio", FECHA_CAJA.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].fechaOperacion").value(FECHA_CAJA.toString()))
                .andExpect(jsonPath("$[0].nombreTipo").value("con_licor"))
                .andExpect(jsonPath("$[0].onzas").value(8))
                .andExpect(jsonPath("$[0].vasosVendidos").value(33))
                .andExpect(jsonPath("$[1].nombreTipo").value("sin_licor"))
                .andExpect(jsonPath("$[1].vasosVendidos").value(8))
                .andExpect(jsonPath("$[2].nombreTipo").value("con_licor"))
                .andExpect(jsonPath("$[2].onzas").value(12))
                .andExpect(jsonPath("$[2].vasosVendidos").value(56));

        mockMvc.perform(get("/api/consultas/inventario/ventas-vasos")
                        .param("fechaInicio", FECHA_CAJA.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenVendedor)))
                .andExpect(status().isForbidden());
    }

    private UUID crearCajaAbierta() {
        return jdbcTemplate.queryForObject("""
                INSERT INTO cajas_diarias (
                    fecha_operacion,
                    estado_caja,
                    valor_base,
                    id_usuario_apertura,
                    observaciones
                )
                VALUES (?, 'abierta'::estado_caja_enum, 300000, ?, 'test_consultas_caja')
                RETURNING id_caja_diaria
                """, UUID.class, FECHA_CAJA, idUsuarioAdmin);
    }

    private UUID crearVentaTransferencia(
            UUID idCajaDiaria,
            UUID idUsuarioVendedor,
            BigDecimal valorPago,
            String estadoValidacion) {
        UUID idVenta = jdbcTemplate.queryForObject("""
                INSERT INTO ventas (
                    id_caja_diaria,
                    id_usuario_vendedor,
                    tipo_comprador,
                    estado_venta,
                    subtotal_venta,
                    descuento_promocion,
                    total_venta
                )
                VALUES (?, ?, 'cliente'::tipo_comprador_enum, 'registrada'::estado_venta_enum, ?, 0, ?)
                RETURNING id_venta
                """, UUID.class, idCajaDiaria, idUsuarioVendedor, valorPago, valorPago);
        UUID idMetodoTransferencia = jdbcTemplate.queryForObject(
                "SELECT id_metodo_pago FROM metodos_pago WHERE nombre_metodo = 'transferencia'",
                UUID.class);
        if ("pendiente".equals(estadoValidacion)) {
            jdbcTemplate.update("""
                    INSERT INTO pagos_venta (
                        id_venta,
                        id_metodo_pago,
                        valor_pago,
                        estado_validacion
                    )
                    VALUES (?, ?, ?, 'pendiente'::estado_validacion_transferencia_enum)
                    """, idVenta, idMetodoTransferencia, valorPago);
        } else {
            jdbcTemplate.update("""
                    INSERT INTO pagos_venta (
                        id_venta,
                        id_metodo_pago,
                        valor_pago,
                        estado_validacion,
                        id_usuario_validacion,
                        fecha_validacion,
                        observacion_validacion
                    )
                    VALUES (?, ?, ?, CAST(? AS estado_validacion_transferencia_enum), ?, ?, 'Transferencia revisada en prueba')
                    """, idVenta, idMetodoTransferencia, valorPago, estadoValidacion, idUsuarioAdmin, fechaPrueba());
        }
        return idVenta;
    }

    private void crearGasto(UUID idCajaDiaria, UUID idUsuarioRegistro, BigDecimal valorGasto, String descripcion) {
        jdbcTemplate.update("""
                INSERT INTO gastos_caja (
                    id_caja_diaria,
                    valor_gasto,
                    descripcion,
                    estado_gasto,
                    id_usuario_registro,
                    fecha_registro
                )
                VALUES (?, ?, ?, 'registrado'::estado_gasto_enum, ?, ?)
                """, idCajaDiaria, valorGasto, descripcion, idUsuarioRegistro, fechaPrueba());
    }

    private void crearDetalleVenta(UUID idVenta, String nombreTipo, int onzas, int cantidad) {
        UUID idTipoGranizado = jdbcTemplate.queryForObject(
                "SELECT id_tipo_granizado FROM tipos_granizado WHERE nombre_tipo = ?",
                UUID.class,
                nombreTipo);
        UUID idTamanoVaso = jdbcTemplate.queryForObject(
                "SELECT id_tamano_vaso FROM tamanos_vaso WHERE onzas = ?",
                UUID.class,
                onzas);
        jdbcTemplate.update("""
                INSERT INTO detalles_venta (
                    id_venta,
                    id_tipo_granizado,
                    id_tamano_vaso,
                    cantidad,
                    precio_unitario_normal,
                    cantidad_con_promocion,
                    cantidad_sin_promocion,
                    subtotal_linea,
                    total_linea
                )
                VALUES (?, ?, ?, ?, 1, 0, ?, ?, ?)
                """, idVenta, idTipoGranizado, idTamanoVaso, cantidad,
                cantidad, BigDecimal.valueOf(cantidad), BigDecimal.valueOf(cantidad));
    }

    private void crearPrerequisitosCierre(UUID idCajaDiaria) {
        jdbcTemplate.update("""
                INSERT INTO adiciones_diarias (
                    id_caja_diaria,
                    cantidad_adiciones,
                    valor_unitario,
                    id_usuario_registro,
                    fecha_registro
                )
                VALUES (?, 0, 1000, ?, ?)
                """, idCajaDiaria, idUsuarioAdmin, fechaPrueba());
        jdbcTemplate.update("""
                INSERT INTO pagos_trabajadores_diarios (
                    id_caja_diaria,
                    valor_total_pagado,
                    descripcion,
                    id_usuario_registro,
                    fecha_registro,
                    confirmado_para_cierre
                )
                VALUES (?, 0, 'Pago cero consultas', ?, ?, true)
                """, idCajaDiaria, idUsuarioAdmin, fechaPrueba());
    }

    private UUID crearCierreCaja(UUID idCajaDiaria, BigDecimal valorDeposito) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO cierres_caja (
                    id_caja_diaria,
                    total_ventas,
                    total_ventas_efectivo,
                    total_ventas_transferencia,
                    total_transferencias_pendientes,
                    total_transferencias_validadas,
                    total_transferencias_rechazadas,
                    total_gastos,
                    total_adiciones,
                    total_pago_trabajadores,
                    efectivo_esperado_sin_base,
                    efectivo_contado_sin_base,
                    diferencia_caja,
                    valor_a_deposito,
                    fecha_cierre,
                    id_usuario_cierre,
                    observaciones
                )
                VALUES (?, ?, 0, ?, ?, 0, 0, 0, 0, 0, 0, ?, ?, ?, ?, ?, 'test_consultas_cierre')
                RETURNING id_cierre_caja
                """, UUID.class,
                idCajaDiaria,
                valorDeposito,
                valorDeposito,
                valorDeposito,
                valorDeposito,
                valorDeposito,
                valorDeposito,
                fechaPrueba(),
                idUsuarioAdmin);
    }

    private void crearMovimientoDeposito(UUID idCierreCaja, BigDecimal valorMovimiento) {
        jdbcTemplate.update("""
                INSERT INTO movimientos_deposito (
                    tipo_movimiento_deposito,
                    valor_movimiento,
                    saldo_anterior,
                    saldo_posterior,
                    id_cierre_caja,
                    id_usuario_registro,
                    fecha_movimiento,
                    observacion
                )
                VALUES ('entrada_cierre'::tipo_movimiento_deposito_enum, ?, 0, ?, ?, ?, ?, 'Movimiento deposito consultas')
                """, valorMovimiento, valorMovimiento, idCierreCaja, idUsuarioAdmin, fechaPrueba());
    }

    private void crearAuditoria(UUID idUsuario, String tablaAfectada, String accion, String descripcion) {
        jdbcTemplate.update("""
                INSERT INTO auditoria_operaciones (
                    id_usuario,
                    tabla_afectada,
                    id_registro_afectado,
                    accion,
                    valor_nuevo,
                    fecha_accion,
                    direccion_ip,
                    descripcion
                )
                VALUES (?, ?, ?, CAST(? AS accion_auditoria_enum), '{"test": true}'::jsonb, ?, '127.0.0.1', ?)
                """, idUsuario, tablaAfectada, UUID.randomUUID().toString(), accion, fechaPrueba(), descripcion);
    }

    private void crearExistenciaDiaria(UUID idCajaDiaria, UUID idItemInventario) {
        jdbcTemplate.update("""
                INSERT INTO existencias_inventario_diario (
                    id_caja_diaria,
                    id_item_inventario,
                    cantidad_inicial,
                    cantidad_ingresada,
                    cantidad_vendida,
                    cantidad_perdida,
                    cantidad_ajustada,
                    cantidad_final_teorica
                )
                VALUES (?, ?, 0, 20, 3, 1, 0, 16)
                """, idCajaDiaria, idItemInventario);
    }

    private void crearMovimientoInventario(UUID idCajaDiaria, UUID idItemInventario) {
        jdbcTemplate.update("""
                INSERT INTO movimientos_inventario (
                    id_item_inventario,
                    id_caja_diaria,
                    tipo_stock,
                    tipo_movimiento,
                    cantidad,
                    sentido_movimiento,
                    referencia_origen,
                    id_referencia_origen,
                    observacion,
                    id_usuario_registro,
                    fecha_movimiento
                )
                VALUES (
                    ?,
                    ?,
                    'diario'::tipo_stock_enum,
                    'apertura_paquete'::tipo_movimiento_inventario_enum,
                    20,
                    'entrada'::sentido_movimiento_enum,
                    'paquetes_vasos_abiertos',
                    ?,
                    'Movimiento inventario consultas',
                    ?,
                    ?
                )
                """, idItemInventario, idCajaDiaria, UUID.randomUUID(), idUsuarioAdmin, fechaPrueba());
    }

    private UUID idItemInventario(String nombreItem) {
        return jdbcTemplate.queryForObject(
                "SELECT id_item_inventario FROM items_inventario WHERE nombre_item = ?",
                UUID.class,
                nombreItem);
    }

    private void ajustarStockGeneral(UUID idItemInventario, int cantidad) {
        jdbcTemplate.update("""
                UPDATE existencias_inventario_general
                SET cantidad_actual = ?
                WHERE id_item_inventario = ?
                """, cantidad, idItemInventario);
    }

    private OffsetDateTime fechaPrueba() {
        return FECHA_CAJA.atTime(12, 0).atOffset(ZoneOffset.UTC);
    }

    private String iniciarSesion(String nombreUsuario) throws Exception {
        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nombreUsuario", nombreUsuario,
                                "contrasena", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readValue(loginResponse, Map.class).get("token").toString();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private UUID crearUsuarioConCredencial(String nombreUsuario, String nombreCompleto, String nombreRol) {
        UUID idRol = jdbcTemplate.queryForObject(
                "SELECT id_rol FROM roles WHERE nombre_rol = ?",
                UUID.class,
                nombreRol);
        UUID idUsuario = jdbcTemplate.queryForObject("""
                INSERT INTO usuarios (id_rol, nombre_usuario, nombre_completo, estado)
                VALUES (?, ?, ?, 'activo'::estado_usuario_enum)
                RETURNING id_usuario
                """, UUID.class, idRol, nombreUsuario, nombreCompleto);
        jdbcTemplate.update("""
                INSERT INTO credenciales_usuario (
                    id_usuario,
                    contrasena_hash,
                    requiere_cambio_contrasena,
                    intentos_fallidos,
                    estado
                )
                VALUES (?, ?, false, 0, 'activa'::estado_credencial_enum)
                """, idUsuario, passwordEncoder.encode(PASSWORD));
        return idUsuario;
    }

    private void limpiarDatosDePrueba() {
        jdbcTemplate.update("""
                DELETE FROM auditoria_operaciones
                WHERE id_usuario IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_consultas_%'
                )
                OR descripcion LIKE '%consultas%'
                """);
        jdbcTemplate.update("""
                DELETE FROM archivos_evidencia
                WHERE id_pago_venta IN (
                    SELECT pv.id_pago_venta
                    FROM pagos_venta pv
                    JOIN ventas v ON v.id_venta = pv.id_venta
                    JOIN cajas_diarias cd ON cd.id_caja_diaria = v.id_caja_diaria
                    WHERE cd.fecha_operacion = DATE '2310-01-01'
                    OR cd.observaciones LIKE 'test_consultas_%'
                )
                OR id_gasto_caja IN (
                    SELECT gc.id_gasto_caja
                    FROM gastos_caja gc
                    JOIN cajas_diarias cd ON cd.id_caja_diaria = gc.id_caja_diaria
                    WHERE cd.fecha_operacion = DATE '2310-01-01'
                    OR cd.observaciones LIKE 'test_consultas_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM movimientos_inventario
                WHERE id_caja_diaria IN (
                    SELECT id_caja_diaria FROM cajas_diarias
                    WHERE fecha_operacion = DATE '2310-01-01'
                    OR observaciones LIKE 'test_consultas_%'
                )
                OR id_usuario_registro IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_consultas_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM movimientos_deposito
                WHERE id_cierre_caja IN (
                    SELECT cc.id_cierre_caja
                    FROM cierres_caja cc
                    JOIN cajas_diarias cd ON cd.id_caja_diaria = cc.id_caja_diaria
                    WHERE cd.fecha_operacion = DATE '2310-01-01'
                    OR cd.observaciones LIKE 'test_consultas_%'
                )
                OR id_usuario_registro IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_consultas_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM cierres_caja
                WHERE id_caja_diaria IN (
                    SELECT id_caja_diaria FROM cajas_diarias
                    WHERE fecha_operacion = DATE '2310-01-01'
                    OR observaciones LIKE 'test_consultas_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM gastos_caja
                WHERE id_caja_diaria IN (
                    SELECT id_caja_diaria FROM cajas_diarias
                    WHERE fecha_operacion = DATE '2310-01-01'
                    OR observaciones LIKE 'test_consultas_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM adiciones_diarias
                WHERE id_caja_diaria IN (
                    SELECT id_caja_diaria FROM cajas_diarias
                    WHERE fecha_operacion = DATE '2310-01-01'
                    OR observaciones LIKE 'test_consultas_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM pagos_trabajadores_diarios
                WHERE id_caja_diaria IN (
                    SELECT id_caja_diaria FROM cajas_diarias
                    WHERE fecha_operacion = DATE '2310-01-01'
                    OR observaciones LIKE 'test_consultas_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM existencias_inventario_diario
                WHERE id_caja_diaria IN (
                    SELECT id_caja_diaria FROM cajas_diarias
                    WHERE fecha_operacion = DATE '2310-01-01'
                    OR observaciones LIKE 'test_consultas_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM ventas
                WHERE id_caja_diaria IN (
                    SELECT id_caja_diaria FROM cajas_diarias
                    WHERE fecha_operacion = DATE '2310-01-01'
                    OR observaciones LIKE 'test_consultas_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM cajas_diarias
                WHERE fecha_operacion = DATE '2310-01-01'
                OR observaciones LIKE 'test_consultas_%'
                """);
        jdbcTemplate.update("""
                DELETE FROM sesiones_usuario
                WHERE id_usuario IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_consultas_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM credenciales_usuario
                WHERE id_usuario IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_consultas_%'
                )
                """);
        jdbcTemplate.update("DELETE FROM usuarios WHERE nombre_usuario LIKE 'test_consultas_%'");
        jdbcTemplate.update("""
                UPDATE existencias_inventario_general
                SET cantidad_actual = 0
                WHERE id_item_inventario IN (
                    SELECT id_item_inventario
                    FROM items_inventario
                    WHERE nombre_item = 'vaso_8oz'
                )
                """);
    }
}
