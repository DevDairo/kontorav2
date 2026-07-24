package com.kontora.pos.caja.controller;

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
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class CierreCajaIntegrationTest {

    private static final String PASSWORD = "Clave12345";
    private static final String USUARIO_ADMIN = "test_cierre_admin";
    private static final String USUARIO_GERENTE = "test_cierre_gerente";
    private static final String USUARIO_VENDEDOR = "test_cierre_vendedor";
    private static final LocalDate FECHA_CAJA = LocalDate.of(2200, 4, 1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private UUID idUsuarioAdmin;
    private UUID idUsuarioVendedor;
    private UUID idCajaDiaria;
    private UUID idVentaRegistrada;

    @BeforeEach
    void setUp() {
        limpiarDatosDePrueba();
        idUsuarioAdmin = crearUsuarioConCredencial(USUARIO_ADMIN, "Administrador Cierre", "administrador");
        crearUsuarioConCredencial(USUARIO_GERENTE, "Gerente Cierre", "gerente");
        idUsuarioVendedor = crearUsuarioConCredencial(USUARIO_VENDEDOR, "Vendedor Cierre", "vendedor");
    }

    @AfterEach
    void tearDown() {
        limpiarDatosDePrueba();
    }

    @Test
    void cierraCajaCalculaTotalesYCreaMovimientoDeposito() throws Exception {
        crearCajaAbierta();
        crearOperacionesParaCierre(true, true);
        String tokenAdmin = iniciarSesion(USUARIO_ADMIN);

        BigDecimal saldoAnteriorEsperado = jdbcTemplate.queryForObject("""
                SELECT COALESCE((
                    SELECT saldo_posterior
                    FROM movimientos_deposito
                    ORDER BY fecha_movimiento DESC, id_movimiento_deposito DESC
                    LIMIT 1
                ), 0)
                """, BigDecimal.class);
        BigDecimal saldoPosteriorEsperado = saldoAnteriorEsperado.add(new BigDecimal("44000.00"));

        String response = mockMvc.perform(post("/api/cajas-diarias/{idCajaDiaria}/cerrar", idCajaDiaria)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "efectivoContadoSinBase", new BigDecimal("44000.00"),
                                "observaciones", " Cierre test "))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idCajaDiaria").value(idCajaDiaria.toString()))
                .andExpect(jsonPath("$.totalVentas").value(150000.00))
                .andExpect(jsonPath("$.totalVentasEfectivo").value(70000.00))
                .andExpect(jsonPath("$.totalVentasTransferencia").value(80000.00))
                .andExpect(jsonPath("$.totalTransferenciasPendientes").value(30000.00))
                .andExpect(jsonPath("$.totalTransferenciasValidadas").value(40000.00))
                .andExpect(jsonPath("$.totalTransferenciasRechazadas").value(10000.00))
                .andExpect(jsonPath("$.totalGastos").value(15000.00))
                .andExpect(jsonPath("$.totalAdiciones").value(10000.00))
                .andExpect(jsonPath("$.totalPagoTrabajadores").value(20000.00))
                .andExpect(jsonPath("$.efectivoEsperadoSinBase").value(45000.00))
                .andExpect(jsonPath("$.efectivoContadoSinBase").value(44000.00))
                .andExpect(jsonPath("$.diferenciaCaja").value(-1000.00))
                .andExpect(jsonPath("$.valorADeposito").value(44000.00))
                .andExpect(jsonPath("$.observaciones").value("Cierre test"))
                .andExpect(jsonPath("$.movimientoDeposito.tipoMovimientoDeposito").value("entrada_cierre"))
                .andExpect(jsonPath("$.movimientoDeposito.valorMovimiento").value(44000.00))
                .andExpect(jsonPath("$.movimientoDeposito.saldoAnterior").value(saldoAnteriorEsperado.doubleValue()))
                .andExpect(jsonPath("$.movimientoDeposito.saldoPosterior").value(saldoPosteriorEsperado.doubleValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID idCierreCaja = UUID.fromString(objectMapper.readValue(response, Map.class).get("idCierreCaja").toString());
        String estadoCaja = jdbcTemplate.queryForObject(
                "SELECT estado_caja::text FROM cajas_diarias WHERE id_caja_diaria = ?",
                String.class,
                idCajaDiaria);
        assertThat(estadoCaja).isEqualTo("cerrada");

        BigDecimal valorMovimiento = jdbcTemplate.queryForObject(
                "SELECT valor_movimiento FROM movimientos_deposito WHERE id_cierre_caja = ?",
                BigDecimal.class,
                idCierreCaja);
        assertThat(valorMovimiento).isEqualByComparingTo("44000.00");

        mockMvc.perform(get("/api/cajas-diarias/{idCajaDiaria}/cierre", idCajaDiaria)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idCierreCaja").value(idCierreCaja.toString()))
                .andExpect(jsonPath("$.movimientoDeposito.valorMovimiento").value(44000.00));
    }

    @Test
    void despuesDelCierreNoPermiteAnularVentasDeCajaCerrada() throws Exception {
        crearCajaAbierta();
        crearOperacionesParaCierre(true, true);
        String tokenAdmin = iniciarSesion(USUARIO_ADMIN);

        mockMvc.perform(post("/api/cajas-diarias/{idCajaDiaria}/cerrar", idCajaDiaria)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "efectivoContadoSinBase", new BigDecimal("45000.00")))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/ventas/{idVenta}/anular", idVentaRegistrada)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("motivoAnulacion", "Intento posterior al cierre"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("No se puede anular una venta con caja diaria cerrada"));
    }

    @Test
    void noCierraCajaSinAdicionesDiarias() throws Exception {
        crearCajaAbierta();
        crearOperacionesParaCierre(false, true);
        String tokenAdmin = iniciarSesion(USUARIO_ADMIN);

        mockMvc.perform(post("/api/cajas-diarias/{idCajaDiaria}/cerrar", idCajaDiaria)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "efectivoContadoSinBase", new BigDecimal("44000.00")))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Debe existir registro de adiciones diarias antes del cierre"));
    }

    @Test
    void noCierraCajaSinPagoTrabajadoresConfirmado() throws Exception {
        crearCajaAbierta();
        crearOperacionesParaCierre(true, false);
        String tokenAdmin = iniciarSesion(USUARIO_ADMIN);

        mockMvc.perform(post("/api/cajas-diarias/{idCajaDiaria}/cerrar", idCajaDiaria)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "efectivoContadoSinBase", new BigDecimal("44000.00")))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("No se puede cerrar caja sin registrar y confirmar el pago diario de trabajadores"));
    }

    @Test
    void vendedorNoPuedeCerrarCaja() throws Exception {
        crearCajaAbierta();
        crearOperacionesParaCierre(true, true);
        String tokenVendedor = iniciarSesion(USUARIO_VENDEDOR);

        mockMvc.perform(post("/api/cajas-diarias/{idCajaDiaria}/cerrar", idCajaDiaria)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenVendedor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "efectivoContadoSinBase", new BigDecimal("44000.00")))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("Solo administrador o gerente puede cerrar caja diaria"));
    }

    @Test
    void consultaResumenCajaAbiertaConLaMismaFormulaDelCierre() throws Exception {
        crearCajaAbierta();
        crearOperacionesParaCierre(true, true);
        String tokenAdmin = iniciarSesion(USUARIO_ADMIN);

        mockMvc.perform(get("/api/cajas-diarias/abierta/resumen")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idCajaDiaria").value(idCajaDiaria.toString()))
                .andExpect(jsonPath("$.valorBase").value(300000.00))
                .andExpect(jsonPath("$.totalVentas").value(150000.00))
                .andExpect(jsonPath("$.totalVentasEfectivo").value(70000.00))
                .andExpect(jsonPath("$.totalVentasTransferencia").value(80000.00))
                .andExpect(jsonPath("$.totalTransferenciasPendientes").value(30000.00))
                .andExpect(jsonPath("$.totalTransferenciasValidadas").value(40000.00))
                .andExpect(jsonPath("$.totalTransferenciasRechazadas").value(10000.00))
                .andExpect(jsonPath("$.totalGastos").value(15000.00))
                .andExpect(jsonPath("$.totalAdiciones").value(10000.00))
                .andExpect(jsonPath("$.adicionDiariaRegistrada").value(true))
                .andExpect(jsonPath("$.totalPagoTrabajadores").value(20000.00))
                .andExpect(jsonPath("$.pagoTrabajadoresRegistrado").value(true))
                .andExpect(jsonPath("$.pagoTrabajadoresConfirmado").value(true))
                .andExpect(jsonPath("$.efectivoEsperadoSinBase").value(45000.00))
                .andExpect(jsonPath("$.listoParaCierre").value(true));

        String tokenGerente = iniciarSesion(USUARIO_GERENTE);
        mockMvc.perform(get("/api/cajas-diarias/abierta/resumen")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenGerente)))
                .andExpect(status().isOk());
    }

    @Test
    void vendedorNoPuedeConsultarResumenCajaAbierta() throws Exception {
        crearCajaAbierta();
        String tokenVendedor = iniciarSesion(USUARIO_VENDEDOR);

        mockMvc.perform(get("/api/cajas-diarias/abierta/resumen")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenVendedor)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("Solo administrador o gerente puede consultar el resumen de caja"));
    }

    private void crearCajaAbierta() {
        idCajaDiaria = jdbcTemplate.queryForObject("""
                INSERT INTO cajas_diarias (
                    fecha_operacion,
                    estado_caja,
                    valor_base,
                    id_usuario_apertura,
                    observaciones
                )
                VALUES (?, 'abierta'::estado_caja_enum, 300000, ?, 'test_cierre_caja')
                RETURNING id_caja_diaria
                """, UUID.class, FECHA_CAJA, idUsuarioAdmin);
        crearStockDiarioVaso(idCajaDiaria, 8, 20);
    }

    private void crearOperacionesParaCierre(boolean crearAdiciones, boolean pagoTrabajadoresConfirmado) {
        if (crearAdiciones) {
            jdbcTemplate.update("""
                    INSERT INTO adiciones_diarias (
                        id_caja_diaria,
                        cantidad_adiciones,
                        valor_unitario,
                        id_usuario_registro
                    )
                    VALUES (?, 10, 1000, ?)
                    """, idCajaDiaria, idUsuarioVendedor);
        }

        jdbcTemplate.update("""
                INSERT INTO pagos_trabajadores_diarios (
                    id_caja_diaria,
                    valor_total_pagado,
                    descripcion,
                    id_usuario_registro,
                    confirmado_para_cierre
                )
                VALUES (?, 20000, 'Pago cierre test', ?, ?)
                """, idCajaDiaria, idUsuarioAdmin, pagoTrabajadoresConfirmado);

        jdbcTemplate.update("""
                INSERT INTO gastos_caja (
                    id_caja_diaria,
                    valor_gasto,
                    descripcion,
                    estado_gasto,
                    id_usuario_registro,
                    id_usuario_anulacion,
                    fecha_anulacion,
                    motivo_anulacion
                )
                VALUES
                    (?, 15000, 'Gasto vigente', 'registrado'::estado_gasto_enum, ?, NULL, NULL, NULL),
                    (?, 3000, 'Gasto anulado', 'anulado'::estado_gasto_enum, ?, ?, NOW(), 'Anulado test')
                """, idCajaDiaria, idUsuarioVendedor, idCajaDiaria, idUsuarioVendedor, idUsuarioAdmin);

        idVentaRegistrada = crearVentaRegistrada(new BigDecimal("100000.00"));
        crearPagoVenta(idVentaRegistrada, "efectivo", new BigDecimal("70000.00"), "no_aplica");
        crearPagoVenta(idVentaRegistrada, "transferencia", new BigDecimal("30000.00"), "pendiente");

        UUID idVentaTransferenciaValidada = crearVentaRegistrada(new BigDecimal("40000.00"));
        crearPagoVenta(idVentaTransferenciaValidada, "transferencia", new BigDecimal("40000.00"), "validada");

        UUID idVentaTransferenciaRechazada = crearVentaRegistrada(new BigDecimal("10000.00"));
        crearPagoVenta(idVentaTransferenciaRechazada, "transferencia", new BigDecimal("10000.00"), "rechazada");
    }

    private UUID crearVentaRegistrada(BigDecimal totalVenta) {
        return jdbcTemplate.queryForObject("""
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
                """, UUID.class, idCajaDiaria, idUsuarioVendedor, totalVenta, totalVenta);
    }

    private void crearPagoVenta(UUID idVenta, String nombreMetodo, BigDecimal valorPago, String estadoValidacion) {
        UUID idMetodoPago = idMetodoPago(nombreMetodo);
        if ("validada".equals(estadoValidacion) || "rechazada".equals(estadoValidacion)) {
            jdbcTemplate.update("""
                    INSERT INTO pagos_venta (
                        id_venta,
                        id_metodo_pago,
                        valor_pago,
                        estado_validacion,
                        id_usuario_validacion,
                        fecha_validacion
                    )
                    VALUES (?, ?, ?, CAST(? AS estado_validacion_transferencia_enum), ?, NOW())
                    """, idVenta, idMetodoPago, valorPago, estadoValidacion, idUsuarioAdmin);
            return;
        }

        jdbcTemplate.update("""
                INSERT INTO pagos_venta (
                    id_venta,
                    id_metodo_pago,
                    valor_pago,
                    valor_recibido_efectivo,
                    cambio_entregado,
                    estado_validacion
                )
                VALUES (
                    ?,
                    ?,
                    ?,
                    CASE WHEN ? = 'no_aplica' THEN ? ELSE NULL END,
                    CASE WHEN ? = 'no_aplica' THEN 0 ELSE NULL END,
                    CAST(? AS estado_validacion_transferencia_enum)
                )
                """, idVenta, idMetodoPago, valorPago, estadoValidacion, valorPago, estadoValidacion, estadoValidacion);
    }

    private void crearStockDiarioVaso(UUID idCajaDiaria, int onzas, int cantidadInicial) {
        UUID idItemInventario = jdbcTemplate.queryForObject("""
                SELECT ii.id_item_inventario
                FROM items_inventario ii
                JOIN tamanos_vaso tv ON tv.id_tamano_vaso = ii.id_tamano_vaso
                WHERE tv.onzas = ?
                AND ii.tipo_control = 'automatico_por_venta'
                """, UUID.class, onzas);
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
                VALUES (?, ?, ?, 0, 0, 0, 0, ?)
                """, idCajaDiaria, idItemInventario, cantidadInicial, cantidadInicial);
    }

    private UUID idTipoGranizado(String nombreTipo) {
        return jdbcTemplate.queryForObject(
                "SELECT id_tipo_granizado FROM tipos_granizado WHERE nombre_tipo = ?",
                UUID.class,
                nombreTipo);
    }

    private UUID idTamanoVaso(int onzas) {
        return jdbcTemplate.queryForObject(
                "SELECT id_tamano_vaso FROM tamanos_vaso WHERE onzas = ?",
                UUID.class,
                onzas);
    }

    private UUID idMetodoPago(String nombreMetodo) {
        return jdbcTemplate.queryForObject(
                "SELECT id_metodo_pago FROM metodos_pago WHERE nombre_metodo = ?",
                UUID.class,
                nombreMetodo);
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
                DELETE FROM movimientos_deposito
                WHERE id_cierre_caja IN (
                    SELECT id_cierre_caja
                    FROM cierres_caja
                    WHERE id_caja_diaria IN (
                        SELECT id_caja_diaria
                        FROM cajas_diarias
                        WHERE fecha_operacion >= DATE '2099-01-01'
                        OR observaciones LIKE 'test_cierre_%'
                    )
                )
                OR id_usuario_registro IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_cierre_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM movimientos_inventario
                WHERE id_usuario_registro IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_cierre_%'
                )
                OR id_caja_diaria IN (
                    SELECT id_caja_diaria
                    FROM cajas_diarias
                    WHERE fecha_operacion >= DATE '2099-01-01'
                    OR observaciones LIKE 'test_cierre_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM ventas
                WHERE id_caja_diaria IN (
                    SELECT id_caja_diaria
                    FROM cajas_diarias
                    WHERE fecha_operacion >= DATE '2099-01-01'
                    OR observaciones LIKE 'test_cierre_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM gastos_caja
                WHERE id_caja_diaria IN (
                    SELECT id_caja_diaria
                    FROM cajas_diarias
                    WHERE fecha_operacion >= DATE '2099-01-01'
                    OR observaciones LIKE 'test_cierre_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM adiciones_diarias
                WHERE id_caja_diaria IN (
                    SELECT id_caja_diaria
                    FROM cajas_diarias
                    WHERE fecha_operacion >= DATE '2099-01-01'
                    OR observaciones LIKE 'test_cierre_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM pagos_trabajadores_diarios
                WHERE id_caja_diaria IN (
                    SELECT id_caja_diaria
                    FROM cajas_diarias
                    WHERE fecha_operacion >= DATE '2099-01-01'
                    OR observaciones LIKE 'test_cierre_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM existencias_inventario_diario
                WHERE id_caja_diaria IN (
                    SELECT id_caja_diaria
                    FROM cajas_diarias
                    WHERE fecha_operacion >= DATE '2099-01-01'
                    OR observaciones LIKE 'test_cierre_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM cierres_caja
                WHERE id_caja_diaria IN (
                    SELECT id_caja_diaria
                    FROM cajas_diarias
                    WHERE fecha_operacion >= DATE '2099-01-01'
                    OR observaciones LIKE 'test_cierre_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM cajas_diarias
                WHERE fecha_operacion >= DATE '2099-01-01'
                OR observaciones LIKE 'test_cierre_%'
                """);
        jdbcTemplate.update("""
                DELETE FROM auditoria_operaciones
                WHERE id_usuario IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_cierre_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM sesiones_usuario
                WHERE id_usuario IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_cierre_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM credenciales_usuario
                WHERE id_usuario IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_cierre_%'
                )
                """);
        jdbcTemplate.update("DELETE FROM usuarios WHERE nombre_usuario LIKE 'test_cierre_%'");
    }
}
