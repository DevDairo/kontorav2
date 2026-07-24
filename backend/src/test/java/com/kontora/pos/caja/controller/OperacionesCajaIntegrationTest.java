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
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class OperacionesCajaIntegrationTest {

    private static final String PASSWORD = "Clave12345";
    private static final String USUARIO_ADMIN = "test_operaciones_admin";
    private static final String USUARIO_GERENTE = "test_operaciones_gerente";
    private static final String USUARIO_VENDEDOR = "test_operaciones_vendedor";
    private static final LocalDate FECHA_CAJA = LocalDate.of(2200, 3, 1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private UUID idUsuarioAdmin;

    @BeforeEach
    void setUp() {
        limpiarDatosDePrueba();
        idUsuarioAdmin = crearUsuarioConCredencial(USUARIO_ADMIN, "Administrador Operaciones", "administrador");
        crearUsuarioConCredencial(USUARIO_GERENTE, "Gerente Operaciones", "gerente");
        crearUsuarioConCredencial(USUARIO_VENDEDOR, "Vendedor Operaciones", "vendedor");
    }

    @AfterEach
    void tearDown() {
        limpiarDatosDePrueba();
    }

    @Test
    void sinUsuarioAutenticadoNoPuedeConsultarOperacionesDeCaja() throws Exception {
        mockMvc.perform(get("/api/operaciones-caja/gastos-caja/abierta"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registraYEditaAdicionesDiariasDeCajaAbierta() throws Exception {
        crearCajaAbierta();
        String tokenVendedor = iniciarSesion(USUARIO_VENDEDOR);

        mockMvc.perform(post("/api/operaciones-caja/adiciones-diarias")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenVendedor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "cantidadAdiciones", 3,
                                "valorUnitario", new BigDecimal("1000.00")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadAdiciones").value(3))
                .andExpect(jsonPath("$.valorUnitario").value(1000.00))
                .andExpect(jsonPath("$.valorTotal").value(3000.00));

        String response = mockMvc.perform(post("/api/operaciones-caja/adiciones-diarias")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenVendedor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "cantidadAdiciones", 5,
                                "valorUnitario", new BigDecimal("1500.00")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadAdiciones").value(5))
                .andExpect(jsonPath("$.valorUnitario").value(1500.00))
                .andExpect(jsonPath("$.valorTotal").value(7500.00))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID idCajaDiaria = UUID.fromString(objectMapper.readValue(response, Map.class).get("idCajaDiaria").toString());
        Integer registros = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM adiciones_diarias WHERE id_caja_diaria = ?",
                Integer.class,
                idCajaDiaria);
        assertThat(registros).isEqualTo(1);

        mockMvc.perform(get("/api/operaciones-caja/adiciones-diarias/abierta")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenVendedor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorTotal").value(7500.00));
    }

    @Test
    void vendedorRegistraGastoPeroNoPuedeEditarNiAnular() throws Exception {
        crearCajaAbierta();
        String tokenVendedor = iniciarSesion(USUARIO_VENDEDOR);

        String response = mockMvc.perform(post("/api/operaciones-caja/gastos-caja")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenVendedor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestGasto("12000.00", "Compra hielo"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.valorGasto").value(12000.00))
                .andExpect(jsonPath("$.descripcion").value("Compra hielo"))
                .andExpect(jsonPath("$.estadoGasto").value("registrado"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID idGasto = UUID.fromString(objectMapper.readValue(response, Map.class).get("idGastoCaja").toString());

        mockMvc.perform(put("/api/operaciones-caja/gastos-caja/{idGastoCaja}", idGasto)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenVendedor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestEditarGasto("15000.00", "Compra hielo y bolsas"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("Solo administrador o gerente puede editar gastos de caja"));

        mockMvc.perform(post("/api/operaciones-caja/gastos-caja/{idGastoCaja}/anular", idGasto)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenVendedor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("motivoAnulacion", "Error test"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("Solo administrador o gerente puede anular gastos de caja"));
    }

    @Test
    void administradorEditaYGerenteAnulaGastoCaja() throws Exception {
        crearCajaAbierta();
        String tokenVendedor = iniciarSesion(USUARIO_VENDEDOR);
        String tokenAdmin = iniciarSesion(USUARIO_ADMIN);
        String tokenGerente = iniciarSesion(USUARIO_GERENTE);

        String response = mockMvc.perform(post("/api/operaciones-caja/gastos-caja")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenVendedor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestGasto("12000.00", "Compra hielo"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID idGasto = UUID.fromString(objectMapper.readValue(response, Map.class).get("idGastoCaja").toString());

        mockMvc.perform(put("/api/operaciones-caja/gastos-caja/{idGastoCaja}", idGasto)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestEditarGasto("15000.00", "Compra hielo y bolsas"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorGasto").value(15000.00))
                .andExpect(jsonPath("$.descripcion").value("Compra hielo y bolsas"))
                .andExpect(jsonPath("$.estadoGasto").value("editado"))
                .andExpect(jsonPath("$.nombreUsuarioUltimaEdicion").value(USUARIO_ADMIN));

        mockMvc.perform(post("/api/operaciones-caja/gastos-caja/{idGastoCaja}/anular", idGasto)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenGerente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("motivoAnulacion", "Duplicado"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoGasto").value("anulado"))
                .andExpect(jsonPath("$.motivoAnulacion").value("Duplicado"))
                .andExpect(jsonPath("$.nombreUsuarioAnulacion").value(USUARIO_GERENTE));

        mockMvc.perform(get("/api/operaciones-caja/gastos-caja/abierta")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].estadoGasto", hasItem("anulado")));
    }

    @Test
    void registraYConfirmaPagoTrabajadores() throws Exception {
        crearCajaAbierta();
        String tokenAdmin = iniciarSesion(USUARIO_ADMIN);

        String response = mockMvc.perform(post("/api/operaciones-caja/pagos-trabajadores-diarios")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "valorTotalPagado", new BigDecimal("50000.00"),
                                "descripcion", "Pago turno",
                                "confirmadoParaCierre", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorTotalPagado").value(50000.00))
                .andExpect(jsonPath("$.confirmadoParaCierre").value(false))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID idPago = UUID.fromString(objectMapper.readValue(response, Map.class).get("idPagoTrabajadoresDiario").toString());
        mockMvc.perform(post("/api/operaciones-caja/pagos-trabajadores-diarios/{idPagoTrabajadoresDiario}/confirmar", idPago)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmadoParaCierre").value(true));

        Boolean confirmado = jdbcTemplate.queryForObject(
                "SELECT confirmado_para_cierre FROM pagos_trabajadores_diarios WHERE id_pago_trabajadores_diario = ?",
                Boolean.class,
                idPago);
        assertThat(confirmado).isTrue();
    }

    @Test
    void pagoTrabajadoresEnCeroRequiereConfirmacionExplicita() throws Exception {
        crearCajaAbierta();
        String tokenAdmin = iniciarSesion(USUARIO_ADMIN);

        mockMvc.perform(post("/api/operaciones-caja/pagos-trabajadores-diarios")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "valorTotalPagado", new BigDecimal("0.00"),
                                "descripcion", "Sin pago",
                                "confirmadoParaCierre", false))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Pago a trabajadores en cero requiere confirmacion explicita"));

        mockMvc.perform(post("/api/operaciones-caja/pagos-trabajadores-diarios")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "valorTotalPagado", new BigDecimal("0.00"),
                                "descripcion", "Sin pago",
                                "confirmadoParaCierre", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorTotalPagado").value(0.00))
                .andExpect(jsonPath("$.confirmadoParaCierre").value(true));
    }

    @Test
    void vendedorNoPuedeRegistrarPagoTrabajadores() throws Exception {
        crearCajaAbierta();
        String tokenVendedor = iniciarSesion(USUARIO_VENDEDOR);

        mockMvc.perform(post("/api/operaciones-caja/pagos-trabajadores-diarios")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenVendedor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "valorTotalPagado", new BigDecimal("50000.00"),
                                "confirmadoParaCierre", true))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("Solo administrador o gerente puede registrar pago diario a trabajadores"));
    }

    private Map<String, Object> requestGasto(String valorGasto, String descripcion) {
        return Map.of(
                "valorGasto", new BigDecimal(valorGasto),
                "descripcion", descripcion);
    }

    private Map<String, Object> requestEditarGasto(String valorGasto, String descripcion) {
        return Map.of(
                "valorGasto", new BigDecimal(valorGasto),
                "descripcion", descripcion,
                "motivoEdicion", "Correccion test");
    }

    private void crearCajaAbierta() {
        jdbcTemplate.update("""
                INSERT INTO cajas_diarias (
                    fecha_operacion,
                    estado_caja,
                    valor_base,
                    id_usuario_apertura,
                    observaciones
                )
                VALUES (?, 'abierta'::estado_caja_enum, 300000, ?, 'test_operaciones_caja')
                """, FECHA_CAJA, idUsuarioAdmin);
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
                DELETE FROM archivos_evidencia
                WHERE id_gasto_caja IN (
                    SELECT id_gasto_caja
                    FROM gastos_caja
                    WHERE id_caja_diaria IN (
                        SELECT id_caja_diaria
                        FROM cajas_diarias
                        WHERE fecha_operacion >= DATE '2099-01-01'
                        OR observaciones LIKE 'test_operaciones_%'
                    )
                    OR id_usuario_registro IN (
                        SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_operaciones_%'
                    )
                    OR id_usuario_ultima_edicion IN (
                        SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_operaciones_%'
                    )
                    OR id_usuario_anulacion IN (
                        SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_operaciones_%'
                    )
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM gastos_caja
                WHERE id_caja_diaria IN (
                    SELECT id_caja_diaria
                    FROM cajas_diarias
                    WHERE fecha_operacion >= DATE '2099-01-01'
                    OR observaciones LIKE 'test_operaciones_%'
                )
                OR id_usuario_registro IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_operaciones_%'
                )
                OR id_usuario_ultima_edicion IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_operaciones_%'
                )
                OR id_usuario_anulacion IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_operaciones_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM adiciones_diarias
                WHERE id_caja_diaria IN (
                    SELECT id_caja_diaria
                    FROM cajas_diarias
                    WHERE fecha_operacion >= DATE '2099-01-01'
                    OR observaciones LIKE 'test_operaciones_%'
                )
                OR id_usuario_registro IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_operaciones_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM pagos_trabajadores_diarios
                WHERE id_caja_diaria IN (
                    SELECT id_caja_diaria
                    FROM cajas_diarias
                    WHERE fecha_operacion >= DATE '2099-01-01'
                    OR observaciones LIKE 'test_operaciones_%'
                )
                OR id_usuario_registro IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_operaciones_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM ventas
                WHERE id_caja_diaria IN (
                    SELECT id_caja_diaria
                    FROM cajas_diarias
                    WHERE fecha_operacion >= DATE '2099-01-01'
                    OR observaciones LIKE 'test_operaciones_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM movimientos_deposito
                WHERE id_cierre_caja IN (
                    SELECT cc.id_cierre_caja
                    FROM cierres_caja cc
                    JOIN cajas_diarias cd ON cd.id_caja_diaria = cc.id_caja_diaria
                    WHERE cd.fecha_operacion >= DATE '2099-01-01'
                    OR cd.observaciones LIKE 'test_operaciones_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM cierres_caja
                WHERE id_caja_diaria IN (
                    SELECT id_caja_diaria
                    FROM cajas_diarias
                    WHERE fecha_operacion >= DATE '2099-01-01'
                    OR observaciones LIKE 'test_operaciones_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM cajas_diarias
                WHERE fecha_operacion >= DATE '2099-01-01'
                OR observaciones LIKE 'test_operaciones_%'
                """);
        jdbcTemplate.update("""
                DELETE FROM auditoria_operaciones
                WHERE id_usuario IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_operaciones_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM sesiones_usuario
                WHERE id_usuario IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_operaciones_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM credenciales_usuario
                WHERE id_usuario IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_operaciones_%'
                )
                """);
        jdbcTemplate.update("DELETE FROM usuarios WHERE nombre_usuario LIKE 'test_operaciones_%'");
    }
}
