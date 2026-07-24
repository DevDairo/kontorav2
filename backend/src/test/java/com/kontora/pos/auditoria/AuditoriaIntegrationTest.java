package com.kontora.pos.auditoria;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class AuditoriaIntegrationTest {

    private static final String PASSWORD = "Clave12345";
    private static final String USUARIO_ADMIN = "test_auditoria_admin";
    private static final String USUARIO_GERENTE = "test_auditoria_gerente";
    private static final String USUARIO_VENDEDOR = "test_auditoria_vendedor";
    private static final LocalDate FECHA_CAJA = LocalDate.of(2300, 1, 1);

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

    @BeforeEach
    void setUp() {
        limpiarDatosDePrueba();
        idUsuarioAdmin = crearUsuarioConCredencial(USUARIO_ADMIN, "Administrador Auditoria", "administrador");
        idUsuarioGerente = crearUsuarioConCredencial(USUARIO_GERENTE, "Gerente Auditoria", "gerente");
        idUsuarioVendedor = crearUsuarioConCredencial(USUARIO_VENDEDOR, "Vendedor Auditoria", "vendedor");
    }

    @AfterEach
    void tearDown() {
        limpiarDatosDePrueba();
    }

    @Test
    void loginYLogoutGeneranAuditoriaConUsuarioEIp() throws Exception {
        String token = iniciarSesion(USUARIO_VENDEDOR, "10.10.10.10");

        Integer loginAuditado = contarAuditorias("sesiones_usuario", "login", idUsuarioVendedor);
        assertThat(loginAuditado).isEqualTo(1);

        String ipAuditada = jdbcTemplate.queryForObject("""
                SELECT direccion_ip
                FROM auditoria_operaciones
                WHERE id_usuario = ?
                AND tabla_afectada = 'sesiones_usuario'
                AND accion = 'login'::accion_auditoria_enum
                ORDER BY fecha_accion DESC
                LIMIT 1
                """, String.class, idUsuarioVendedor);
        assertThat(ipAuditada).isEqualTo("10.10.10.10");

        mockMvc.perform(post("/api/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());

        Integer logoutAuditado = contarAuditorias("sesiones_usuario", "logout", idUsuarioVendedor);
        assertThat(logoutAuditado).isEqualTo(1);
    }

    @Test
    void aperturaCierreYMovimientoDepositoQuedanAuditados() throws Exception {
        String tokenAdmin = iniciarSesion(USUARIO_ADMIN);

        String response = mockMvc.perform(post("/api/cajas-diarias")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fechaOperacion", FECHA_CAJA.toString(),
                                "valorBase", new BigDecimal("300000.00"),
                                "observaciones", "test_auditoria_caja"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID idCajaDiaria = UUID.fromString(objectMapper.readValue(response, Map.class).get("idCajaDiaria").toString());

        crearAdicionesYPagoTrabajadores(idCajaDiaria);

        mockMvc.perform(post("/api/cajas-diarias/{idCajaDiaria}/cerrar", idCajaDiaria)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "efectivoContadoSinBase", new BigDecimal("5000.00"),
                                "observaciones", "cierre auditado"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorADeposito").value(5000.00))
                .andExpect(jsonPath("$.movimientoDeposito.tipoMovimientoDeposito").value("entrada_cierre"));

        assertThat(contarAuditorias("cajas_diarias", "abrir", idUsuarioAdmin)).isEqualTo(1);
        assertThat(contarAuditorias("cierres_caja", "cerrar", idUsuarioAdmin)).isEqualTo(1);
        assertThat(contarAuditorias("movimientos_deposito", "crear", idUsuarioAdmin)).isEqualTo(1);
    }

    @Test
    void edicionYAnulacionDeGastoRegistranValoresAnteriorYNuevo() throws Exception {
        UUID idCajaDiaria = crearCajaAbierta();
        String tokenGerente = iniciarSesion(USUARIO_GERENTE);

        String gastoResponse = mockMvc.perform(post("/api/operaciones-caja/gastos-caja")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenGerente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "valorGasto", new BigDecimal("12000.00"),
                                "descripcion", "Gasto auditoria"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID idGastoCaja = UUID.fromString(objectMapper.readValue(gastoResponse, Map.class).get("idGastoCaja").toString());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/operaciones-caja/gastos-caja/{idGastoCaja}", idGastoCaja)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenGerente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "valorGasto", new BigDecimal("15000.00"),
                                "descripcion", "Gasto auditoria editado",
                                "motivoEdicion", "Correccion valor"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoGasto").value("editado"));

        mockMvc.perform(post("/api/operaciones-caja/gastos-caja/{idGastoCaja}/anular", idGastoCaja)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenGerente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "motivoAnulacion", "No correspondia"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoGasto").value("anulado"));

        String estadoAnteriorEdicion = valorJsonAuditoria("gastos_caja", "editar", idGastoCaja, "valor_anterior", "estado_gasto");
        String estadoNuevoEdicion = valorJsonAuditoria("gastos_caja", "editar", idGastoCaja, "valor_nuevo", "estado_gasto");
        String estadoNuevoAnulacion = valorJsonAuditoria("gastos_caja", "anular", idGastoCaja, "valor_nuevo", "estado_gasto");

        assertThat(idCajaDiaria).isNotNull();
        assertThat(estadoAnteriorEdicion).isEqualTo("registrado");
        assertThat(estadoNuevoEdicion).isEqualTo("editado");
        assertThat(estadoNuevoAnulacion).isEqualTo("anulado");
    }

    @Test
    void validarYRechazarTransferenciasActualizaPagoYAuditaOperacion() throws Exception {
        UUID idCajaDiaria = crearCajaAbierta();
        UUID idPagoValidar = crearPagoTransferencia(idCajaDiaria, new BigDecimal("9000.00"));
        UUID idPagoRechazar = crearPagoTransferencia(idCajaDiaria, new BigDecimal("7000.00"));
        String tokenAdmin = iniciarSesion(USUARIO_ADMIN);
        String tokenGerente = iniciarSesion(USUARIO_GERENTE);

        mockMvc.perform(post("/api/pagos-venta/{idPagoVenta}/validar", idPagoValidar)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("Solo gerente puede validar transferencias"));

        mockMvc.perform(post("/api/pagos-venta/{idPagoVenta}/validar", idPagoValidar)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenGerente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "observacionValidacion", "Transferencia confirmada"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoValidacion").value("validada"))
                .andExpect(jsonPath("$.idUsuarioValidacion").value(idUsuarioGerente.toString()));

        mockMvc.perform(post("/api/pagos-venta/{idPagoVenta}/rechazar", idPagoRechazar)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenGerente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "observacionValidacion", "Comprobante invalido"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoValidacion").value("rechazada"))
                .andExpect(jsonPath("$.idUsuarioValidacion").value(idUsuarioGerente.toString()));

        assertThat(valorJsonAuditoria("pagos_venta", "validar", idPagoValidar, "valor_anterior", "estado_validacion"))
                .isEqualTo("pendiente");
        assertThat(valorJsonAuditoria("pagos_venta", "validar", idPagoValidar, "valor_nuevo", "estado_validacion"))
                .isEqualTo("validada");
        assertThat(valorJsonAuditoria("pagos_venta", "rechazar", idPagoRechazar, "valor_nuevo", "estado_validacion"))
                .isEqualTo("rechazada");
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
                VALUES (?, 'abierta'::estado_caja_enum, 300000, ?, 'test_auditoria_caja')
                RETURNING id_caja_diaria
                """, UUID.class, FECHA_CAJA, idUsuarioAdmin);
    }

    private void crearAdicionesYPagoTrabajadores(UUID idCajaDiaria) {
        jdbcTemplate.update("""
                INSERT INTO adiciones_diarias (
                    id_caja_diaria,
                    cantidad_adiciones,
                    valor_unitario,
                    id_usuario_registro
                )
                VALUES (?, 5, 1000, ?)
                """, idCajaDiaria, idUsuarioAdmin);
        jdbcTemplate.update("""
                INSERT INTO pagos_trabajadores_diarios (
                    id_caja_diaria,
                    valor_total_pagado,
                    descripcion,
                    id_usuario_registro,
                    confirmado_para_cierre
                )
                VALUES (?, 0, 'Pago cero auditado', ?, true)
                """, idCajaDiaria, idUsuarioAdmin);
    }

    private UUID crearPagoTransferencia(UUID idCajaDiaria, BigDecimal valorPago) {
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
        return jdbcTemplate.queryForObject("""
                INSERT INTO pagos_venta (
                    id_venta,
                    id_metodo_pago,
                    valor_pago,
                    estado_validacion
                )
                VALUES (?, ?, ?, 'pendiente'::estado_validacion_transferencia_enum)
                RETURNING id_pago_venta
                """, UUID.class, idVenta, idMetodoTransferencia, valorPago);
    }

    private Integer contarAuditorias(String tablaAfectada, String accion, UUID idUsuario) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM auditoria_operaciones
                WHERE id_usuario = ?
                AND tabla_afectada = ?
                AND accion = CAST(? AS accion_auditoria_enum)
                """, Integer.class, idUsuario, tablaAfectada, accion);
    }

    private String valorJsonAuditoria(
            String tablaAfectada,
            String accion,
            UUID idRegistroAfectado,
            String columnaJson,
            String atributo) {
        return jdbcTemplate.queryForObject(String.format("""
                SELECT %s ->> ?
                FROM auditoria_operaciones
                WHERE tabla_afectada = ?
                AND accion = CAST(? AS accion_auditoria_enum)
                AND id_registro_afectado = ?
                ORDER BY fecha_accion DESC
                LIMIT 1
                """, columnaJson), String.class, atributo, tablaAfectada, accion, idRegistroAfectado.toString());
    }

    private String iniciarSesion(String nombreUsuario) throws Exception {
        return iniciarSesion(nombreUsuario, null);
    }

    private String iniciarSesion(String nombreUsuario, String forwardedFor) throws Exception {
        var request = post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "nombreUsuario", nombreUsuario,
                        "contrasena", PASSWORD)));
        if (forwardedFor != null) {
            request.header("X-Forwarded-For", forwardedFor);
        }
        String loginResponse = mockMvc.perform(request)
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
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_auditoria_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM pagos_venta
                WHERE id_venta IN (
                    SELECT id_venta
                    FROM ventas
                    WHERE id_caja_diaria IN (
                        SELECT id_caja_diaria FROM cajas_diarias
                        WHERE fecha_operacion = DATE '2300-01-01'
                        OR observaciones LIKE 'test_auditoria_%'
                    )
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM detalles_venta
                WHERE id_venta IN (
                    SELECT id_venta
                    FROM ventas
                    WHERE id_caja_diaria IN (
                        SELECT id_caja_diaria FROM cajas_diarias
                        WHERE fecha_operacion = DATE '2300-01-01'
                        OR observaciones LIKE 'test_auditoria_%'
                    )
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM movimientos_deposito
                WHERE id_cierre_caja IN (
                    SELECT cc.id_cierre_caja
                    FROM cierres_caja cc
                    JOIN cajas_diarias cd ON cd.id_caja_diaria = cc.id_caja_diaria
                    WHERE cd.fecha_operacion = DATE '2300-01-01'
                    OR cd.observaciones LIKE 'test_auditoria_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM cierres_caja
                WHERE id_caja_diaria IN (
                    SELECT id_caja_diaria
                    FROM cajas_diarias
                    WHERE fecha_operacion = DATE '2300-01-01'
                    OR observaciones LIKE 'test_auditoria_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM gastos_caja
                WHERE id_caja_diaria IN (
                    SELECT id_caja_diaria
                    FROM cajas_diarias
                    WHERE fecha_operacion = DATE '2300-01-01'
                    OR observaciones LIKE 'test_auditoria_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM gastos_caja
                WHERE id_usuario_registro IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_auditoria_%'
                )
                OR id_usuario_ultima_edicion IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_auditoria_%'
                )
                OR id_usuario_anulacion IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_auditoria_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM adiciones_diarias
                WHERE id_caja_diaria IN (
                    SELECT id_caja_diaria
                    FROM cajas_diarias
                    WHERE fecha_operacion = DATE '2300-01-01'
                    OR observaciones LIKE 'test_auditoria_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM pagos_trabajadores_diarios
                WHERE id_caja_diaria IN (
                    SELECT id_caja_diaria
                    FROM cajas_diarias
                    WHERE fecha_operacion = DATE '2300-01-01'
                    OR observaciones LIKE 'test_auditoria_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM ventas
                WHERE id_caja_diaria IN (
                    SELECT id_caja_diaria
                    FROM cajas_diarias
                    WHERE fecha_operacion = DATE '2300-01-01'
                    OR observaciones LIKE 'test_auditoria_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM cajas_diarias
                WHERE fecha_operacion = DATE '2300-01-01'
                OR observaciones LIKE 'test_auditoria_%'
                """);
        jdbcTemplate.update("""
                DELETE FROM sesiones_usuario
                WHERE id_usuario IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_auditoria_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM credenciales_usuario
                WHERE id_usuario IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_auditoria_%'
                )
                """);
        jdbcTemplate.update("DELETE FROM usuarios WHERE nombre_usuario LIKE 'test_auditoria_%'");
    }
}
