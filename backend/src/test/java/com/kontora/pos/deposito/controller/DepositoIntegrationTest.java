package com.kontora.pos.deposito.controller;

import com.fasterxml.jackson.databind.JsonNode;
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
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class DepositoIntegrationTest {

    private static final String PASSWORD = "Clave12345";
    private static final String USUARIO_ADMIN = "test_deposito_admin";
    private static final String USUARIO_GERENTE = "test_deposito_gerente";
    private static final String USUARIO_VENDEDOR = "test_deposito_vendedor";

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
        idUsuarioAdmin = crearUsuarioConCredencial(USUARIO_ADMIN, "Administrador Deposito", "administrador");
        idUsuarioGerente = crearUsuarioConCredencial(USUARIO_GERENTE, "Gerente Deposito", "gerente");
        idUsuarioVendedor = crearUsuarioConCredencial(USUARIO_VENDEDOR, "Vendedor Deposito", "vendedor");
    }

    @AfterEach
    void tearDown() {
        limpiarDatosDePrueba();
    }

    @Test
    void administradorRegistraConsignacionYDescuentaSaldo() throws Exception {
        BigDecimal saldoDisponible = crearEntradaDeposito(new BigDecimal("100000.00"));
        String tokenAdmin = iniciarSesion(USUARIO_ADMIN);

        String response = mockMvc.perform(post("/api/deposito/consignaciones-bancarias")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "valorConsignado", new BigDecimal("25000.00"),
                                "observacion", "Consignacion banco prueba"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.valorConsignado").value(25000.00))
                .andExpect(jsonPath("$.estado").value("registrado"))
                .andExpect(jsonPath("$.movimientoDeposito.tipoMovimientoDeposito").value("salida_consignacion"))
                .andExpect(jsonPath("$.movimientoDeposito.saldoAnterior").value(saldoDisponible.doubleValue()))
                .andExpect(jsonPath("$.movimientoDeposito.saldoPosterior").value(saldoDisponible.subtract(new BigDecimal("25000.00")).doubleValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode payload = objectMapper.readTree(response);
        UUID idConsignacion = UUID.fromString(payload.get("idConsignacionBancaria").asText());
        UUID idMovimiento = UUID.fromString(payload.path("movimientoDeposito").path("idMovimientoDeposito").asText());

        Integer consignaciones = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM consignaciones_bancarias WHERE id_consignacion_bancaria = ? AND id_movimiento_deposito = ?",
                Integer.class,
                idConsignacion,
                idMovimiento);
        Integer auditorias = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM auditoria_operaciones
                WHERE id_usuario = ?
                  AND tabla_afectada IN ('movimientos_deposito', 'consignaciones_bancarias')
                """, Integer.class, idUsuarioAdmin);

        mockMvc.perform(get("/api/deposito/saldo")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldoActual").value(saldoDisponible.subtract(new BigDecimal("25000.00")).doubleValue()));

        mockMvc.perform(get("/api/consultas/deposito/movimientos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].idMovimientoDeposito", hasItem(idMovimiento.toString())))
                .andExpect(jsonPath("$[*].idConsignacionBancaria", hasItem(idConsignacion.toString())));

        org.assertj.core.api.Assertions.assertThat(consignaciones).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(auditorias).isEqualTo(2);
    }

    @Test
    void gerenteRegistraPagoServicioYDescuentaSaldo() throws Exception {
        BigDecimal saldoDisponible = crearEntradaDeposito(new BigDecimal("70000.00"));
        UUID idTipoServicio = jdbcTemplate.queryForObject("""
                SELECT id_tipo_servicio
                FROM tipos_servicio
                WHERE estado = 'activo'::estado_basico_enum
                ORDER BY nombre_servicio
                LIMIT 1
                """, UUID.class);
        String nombreServicio = jdbcTemplate.queryForObject(
                "SELECT nombre_servicio FROM tipos_servicio WHERE id_tipo_servicio = ?",
                String.class,
                idTipoServicio);
        String tokenGerente = iniciarSesion(USUARIO_GERENTE);

        String response = mockMvc.perform(post("/api/deposito/pagos-servicios")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenGerente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "idTipoServicio", idTipoServicio,
                                "valorPagado", new BigDecimal("12000.00"),
                                "descripcion", "Pago mensual de prueba"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idTipoServicio").value(idTipoServicio.toString()))
                .andExpect(jsonPath("$.nombreServicio").value(nombreServicio))
                .andExpect(jsonPath("$.valorPagado").value(12000.00))
                .andExpect(jsonPath("$.movimientoDeposito.tipoMovimientoDeposito").value("salida_pago_servicio"))
                .andExpect(jsonPath("$.movimientoDeposito.saldoAnterior").value(saldoDisponible.doubleValue()))
                .andExpect(jsonPath("$.movimientoDeposito.saldoPosterior").value(saldoDisponible.subtract(new BigDecimal("12000.00")).doubleValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode payload = objectMapper.readTree(response);
        UUID idPagoServicio = UUID.fromString(payload.get("idPagoServicio").asText());
        UUID idMovimiento = UUID.fromString(payload.path("movimientoDeposito").path("idMovimientoDeposito").asText());

        Integer pagos = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pagos_servicios WHERE id_pago_servicio = ? AND id_movimiento_deposito = ?",
                Integer.class,
                idPagoServicio,
                idMovimiento);
        Integer auditorias = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM auditoria_operaciones
                WHERE id_usuario = ?
                  AND tabla_afectada IN ('movimientos_deposito', 'pagos_servicios')
                """, Integer.class, idUsuarioGerente);

        org.assertj.core.api.Assertions.assertThat(pagos).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(auditorias).isEqualTo(2);
    }

    @Test
    void rechazaSalidaCuandoElSaldoNoEsSuficiente() throws Exception {
        BigDecimal saldoActual = obtenerSaldoActual();
        String tokenAdmin = iniciarSesion(USUARIO_ADMIN);

        mockMvc.perform(post("/api/deposito/consignaciones-bancarias")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "valorConsignado", saldoActual.add(new BigDecimal("0.01"))))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Saldo insuficiente en deposito para registrar la salida"));
    }

    @Test
    void vendedorNoConsultaNiRegistraOperacionesDeDeposito() throws Exception {
        String tokenVendedor = iniciarSesion(USUARIO_VENDEDOR);

        mockMvc.perform(get("/api/deposito/saldo")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenVendedor)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/deposito/consignaciones-bancarias")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenVendedor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("valorConsignado", new BigDecimal("1.00")))))
                .andExpect(status().isForbidden());
    }

    private BigDecimal crearEntradaDeposito(BigDecimal valorMovimiento) {
        BigDecimal saldoAnterior = obtenerSaldoActual();
        BigDecimal saldoPosterior = saldoAnterior.add(valorMovimiento);
        jdbcTemplate.update("""
                INSERT INTO movimientos_deposito (
                    tipo_movimiento_deposito,
                    valor_movimiento,
                    saldo_anterior,
                    saldo_posterior,
                    id_usuario_registro,
                    fecha_movimiento,
                    observacion
                )
                VALUES ('entrada_cierre'::tipo_movimiento_deposito_enum, ?, ?, ?, ?, NOW(), 'Entrada deposito prueba')
                """, valorMovimiento, saldoAnterior, saldoPosterior, idUsuarioAdmin);
        return saldoPosterior;
    }

    private BigDecimal obtenerSaldoActual() {
        return jdbcTemplate.queryForObject("""
                SELECT COALESCE((
                    SELECT saldo_posterior
                    FROM movimientos_deposito
                    ORDER BY fecha_movimiento DESC, id_movimiento_deposito DESC
                    LIMIT 1
                ), 0)
                """, BigDecimal.class);
    }

    private String iniciarSesion(String nombreUsuario) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nombreUsuario", nombreUsuario,
                                "contrasena", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readValue(response, Map.class).get("token").toString();
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
                WHERE id_consignacion_bancaria IN (
                    SELECT id_consignacion_bancaria
                    FROM consignaciones_bancarias
                    WHERE id_usuario_registro IN (
                        SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_deposito_%'
                    )
                )
                OR id_pago_servicio IN (
                    SELECT id_pago_servicio
                    FROM pagos_servicios
                    WHERE id_usuario_registro IN (
                        SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_deposito_%'
                    )
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM auditoria_operaciones
                WHERE id_usuario IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_deposito_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM consignaciones_bancarias
                WHERE id_usuario_registro IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_deposito_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM pagos_servicios
                WHERE id_usuario_registro IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_deposito_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM movimientos_deposito
                WHERE id_usuario_registro IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_deposito_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM sesiones_usuario
                WHERE id_usuario IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_deposito_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM credenciales_usuario
                WHERE id_usuario IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_deposito_%'
                )
                """);
        jdbcTemplate.update("DELETE FROM usuarios WHERE nombre_usuario LIKE 'test_deposito_%'");
    }
}
