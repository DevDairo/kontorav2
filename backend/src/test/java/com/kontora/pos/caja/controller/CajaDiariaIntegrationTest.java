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
class CajaDiariaIntegrationTest {

    private static final String PASSWORD = "Clave12345";
    private static final LocalDate FECHA_OPERACION = LocalDate.of(2099, 12, 31);
    private static final LocalDate FECHA_DUPLICADA = LocalDate.of(2099, 12, 30);
    private static final LocalDate FECHA_REMANENTE_ANTERIOR = LocalDate.of(2099, 12, 28);
    private static final LocalDate FECHA_SIGUIENTE = LocalDate.of(2100, 1, 1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final String sufijoPrueba = UUID.randomUUID().toString().substring(0, 8);
    private final String usuarioAdmin = "test_caja_it_" + sufijoPrueba + "_admin";
    private final String usuarioGerente = "test_caja_it_" + sufijoPrueba + "_gerente";
    private final String usuarioVendedor = "test_caja_it_" + sufijoPrueba + "_vendedor";

    @BeforeEach
    void setUp() {
        limpiarDatosDePrueba();
        crearUsuarioConCredencial(usuarioAdmin, "Administrador Caja", "administrador");
        crearUsuarioConCredencial(usuarioGerente, "Gerente Caja", "gerente");
        crearUsuarioConCredencial(usuarioVendedor, "Vendedor Caja", "vendedor");
    }

    @AfterEach
    void tearDown() {
        limpiarDatosDePrueba();
    }

    @Test
    void sinUsuarioAutenticadoNoPuedeAbrirCaja() throws Exception {
        mockMvc.perform(post("/api/cajas-diarias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestApertura(FECHA_OPERACION))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void vendedorNoPuedeAbrirCaja() throws Exception {
        String tokenVendedor = iniciarSesion(usuarioVendedor);

        mockMvc.perform(post("/api/cajas-diarias")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenVendedor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestApertura(FECHA_OPERACION))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("Solo administrador o gerente puede abrir caja diaria"));
    }

    @Test
    void administradorAbreCajaYPermiteConsultarla() throws Exception {
        String tokenAdmin = iniciarSesion(usuarioAdmin);

        mockMvc.perform(post("/api/cajas-diarias")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestApertura(FECHA_OPERACION))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fechaOperacion").value(FECHA_OPERACION.toString()))
                .andExpect(jsonPath("$.estadoCaja").value("abierta"))
                .andExpect(jsonPath("$.valorBase").value(300000.00))
                .andExpect(jsonPath("$.nombreUsuarioApertura").value(usuarioAdmin));

        Integer cajasCreadas = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cajas_diarias WHERE fecha_operacion = ?",
                Integer.class,
                FECHA_OPERACION);
        assertThat(cajasCreadas).isEqualTo(1);

        mockMvc.perform(get("/api/cajas-diarias/fecha/{fechaOperacion}", FECHA_OPERACION)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fechaOperacion").value(FECHA_OPERACION.toString()))
                .andExpect(jsonPath("$.estadoCaja").value("abierta"));

        mockMvc.perform(get("/api/cajas-diarias/abierta")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoCaja").value("abierta"));
    }

    @Test
    void aperturaInicializaStockDiarioConRemanenteAnteriorDeVasos() throws Exception {
        String tokenAdmin = iniciarSesion(usuarioAdmin);
        UUID idItemVaso = idItemInventario("vaso_8oz");
        crearCajaAnteriorConStockDiario(idItemVaso);

        mockMvc.perform(post("/api/cajas-diarias")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestApertura(FECHA_OPERACION))))
                .andExpect(status().isCreated());

        Map<String, Object> existenciaDiaria = jdbcTemplate.queryForMap("""
                SELECT
                    eid.cantidad_inicial,
                    eid.cantidad_ingresada,
                    eid.cantidad_vendida,
                    eid.cantidad_perdida,
                    eid.cantidad_ajustada,
                    eid.cantidad_final_teorica
                FROM existencias_inventario_diario eid
                JOIN cajas_diarias cd ON cd.id_caja_diaria = eid.id_caja_diaria
                WHERE cd.fecha_operacion = ?
                AND eid.id_item_inventario = ?
                """, FECHA_OPERACION, idItemVaso);
        Integer existenciasVasos = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM existencias_inventario_diario eid
                JOIN cajas_diarias cd ON cd.id_caja_diaria = eid.id_caja_diaria
                JOIN items_inventario ii ON ii.id_item_inventario = eid.id_item_inventario
                WHERE cd.fecha_operacion = ?
                AND ii.tipo_control = 'automatico_por_venta'
                AND ii.id_tamano_vaso IS NOT NULL
                """, Integer.class, FECHA_OPERACION);

        assertThat(existenciaDiaria.get("cantidad_inicial")).isEqualTo(13);
        assertThat(existenciaDiaria.get("cantidad_ingresada")).isEqualTo(0);
        assertThat(existenciaDiaria.get("cantidad_vendida")).isEqualTo(0);
        assertThat(existenciaDiaria.get("cantidad_perdida")).isEqualTo(0);
        assertThat(existenciaDiaria.get("cantidad_ajustada")).isEqualTo(0);
        assertThat(existenciaDiaria.get("cantidad_final_teorica")).isEqualTo(13);
        assertThat(existenciasVasos).isPositive();
    }

    @Test
    void noPermiteAbrirDosCajasParaLaMismaFecha() throws Exception {
        String tokenGerente = iniciarSesion(usuarioGerente);

        mockMvc.perform(post("/api/cajas-diarias")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenGerente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestApertura(FECHA_DUPLICADA))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/cajas-diarias")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenGerente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestApertura(FECHA_DUPLICADA))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Ya existe una caja diaria para la fecha indicada"));
    }

    @Test
    void noPermiteAbrirOtraJornadaMientrasExisteUnaCajaAbierta() throws Exception {
        String tokenGerente = iniciarSesion(usuarioGerente);

        mockMvc.perform(post("/api/cajas-diarias")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenGerente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestApertura(FECHA_OPERACION))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/cajas-diarias")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenGerente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestApertura(FECHA_SIGUIENTE))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Ya existe una caja diaria abierta. Cierra la jornada actual antes de abrir otra"));
    }

    @Test
    void permiteAbrirLaJornadaSiguienteConLaAnteriorCerrada() throws Exception {
        String tokenGerente = iniciarSesion(usuarioGerente);
        crearCajaCerrada(FECHA_OPERACION);

        mockMvc.perform(post("/api/cajas-diarias")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenGerente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestApertura(FECHA_SIGUIENTE))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fechaOperacion").value(FECHA_SIGUIENTE.toString()))
                .andExpect(jsonPath("$.estadoCaja").value("abierta"));
    }

    private Map<String, Object> requestApertura(LocalDate fechaOperacion) {
        return Map.of(
                "fechaOperacion", fechaOperacion.toString(),
                "valorBase", new BigDecimal("300000.00"),
                "observaciones", "test_caja_apertura");
    }

    private UUID idItemInventario(String nombreItem) {
        return jdbcTemplate.queryForObject(
                "SELECT id_item_inventario FROM items_inventario WHERE nombre_item = ?",
                UUID.class,
                nombreItem);
    }

    private void crearCajaAnteriorConStockDiario(UUID idItemVaso) {
        UUID idCajaAnterior = jdbcTemplate.queryForObject("""
                INSERT INTO cajas_diarias (
                    fecha_operacion,
                    estado_caja,
                    valor_base,
                    id_usuario_apertura,
                    observaciones
                )
                VALUES (?, 'abierta'::estado_caja_enum, 300000, ?, 'test_caja_remanente_anterior')
                RETURNING id_caja_diaria
                """, UUID.class, FECHA_REMANENTE_ANTERIOR, idUsuarioPorNombre(usuarioAdmin));
        jdbcTemplate.update("""
                INSERT INTO existencias_inventario_diario (
                    id_caja_diaria,
                    id_item_inventario,
                    cantidad_inicial,
                    cantidad_ingresada,
                    cantidad_vendida,
                    cantidad_perdida,
                    cantidad_ajustada,
                    cantidad_final_teorica,
                    cantidad_final_contada,
                    diferencia
                )
                VALUES (?, ?, 5, 20, 10, 2, 1, 14, 13, -1)
                """, idCajaAnterior, idItemVaso);
        jdbcTemplate.update("""
                UPDATE cajas_diarias
                SET estado_caja = 'cerrada'::estado_caja_enum,
                    fecha_cierre = NOW(),
                    id_usuario_cierre = id_usuario_apertura
                WHERE id_caja_diaria = ?
                """, idCajaAnterior);
    }

    private void crearCajaCerrada(LocalDate fechaOperacion) {
        UUID idUsuario = idUsuarioPorNombre(usuarioGerente);
        jdbcTemplate.update("""
                INSERT INTO cajas_diarias (
                    fecha_operacion,
                    estado_caja,
                    valor_base,
                    fecha_cierre,
                    id_usuario_apertura,
                    id_usuario_cierre,
                    observaciones
                )
                VALUES (?, 'cerrada'::estado_caja_enum, 300000, NOW(), ?, ?, 'test_caja_jornada_cerrada')
                """, fechaOperacion, idUsuario, idUsuario);
    }

    private UUID idUsuarioPorNombre(String nombreUsuario) {
        return jdbcTemplate.queryForObject(
                "SELECT id_usuario FROM usuarios WHERE nombre_usuario = ?",
                UUID.class,
                nombreUsuario);
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

    private void crearUsuarioConCredencial(String nombreUsuario, String nombreCompleto, String nombreRol) {
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
    }

    private void limpiarDatosDePrueba() {
        jdbcTemplate.update("""
                DELETE FROM movimientos_inventario
                WHERE id_usuario_registro IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario IN (?, ?, ?)
                )
                OR id_caja_diaria IN (
                    SELECT id_caja_diaria
                    FROM cajas_diarias
                    WHERE id_usuario_apertura IN (
                        SELECT id_usuario FROM usuarios WHERE nombre_usuario IN (?, ?, ?)
                    )
                    OR id_usuario_cierre IN (
                        SELECT id_usuario FROM usuarios WHERE nombre_usuario IN (?, ?, ?)
                    )
                )
                """, usuarioAdmin, usuarioGerente, usuarioVendedor,
                usuarioAdmin, usuarioGerente, usuarioVendedor,
                usuarioAdmin, usuarioGerente, usuarioVendedor);
        jdbcTemplate.update("""
                DELETE FROM ventas
                WHERE id_usuario_vendedor IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario IN (?, ?, ?)
                )
                OR id_usuario_anulacion IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario IN (?, ?, ?)
                )
                OR id_caja_diaria IN (
                    SELECT id_caja_diaria
                    FROM cajas_diarias
                    WHERE id_usuario_apertura IN (
                        SELECT id_usuario FROM usuarios WHERE nombre_usuario IN (?, ?, ?)
                    )
                    OR id_usuario_cierre IN (
                        SELECT id_usuario FROM usuarios WHERE nombre_usuario IN (?, ?, ?)
                    )
                )
                """, usuarioAdmin, usuarioGerente, usuarioVendedor,
                usuarioAdmin, usuarioGerente, usuarioVendedor,
                usuarioAdmin, usuarioGerente, usuarioVendedor,
                usuarioAdmin, usuarioGerente, usuarioVendedor);
        jdbcTemplate.update("""
                DELETE FROM movimientos_deposito
                WHERE id_cierre_caja IN (
                    SELECT cc.id_cierre_caja
                    FROM cierres_caja cc
                    JOIN cajas_diarias cd ON cd.id_caja_diaria = cc.id_caja_diaria
                    WHERE cd.id_usuario_apertura IN (
                        SELECT id_usuario FROM usuarios WHERE nombre_usuario IN (?, ?, ?)
                    )
                    OR cd.id_usuario_cierre IN (
                        SELECT id_usuario FROM usuarios WHERE nombre_usuario IN (?, ?, ?)
                    )
                )
                """, usuarioAdmin, usuarioGerente, usuarioVendedor,
                usuarioAdmin, usuarioGerente, usuarioVendedor);
        jdbcTemplate.update("""
                DELETE FROM cierres_caja
                WHERE id_caja_diaria IN (
                    SELECT id_caja_diaria
                    FROM cajas_diarias
                    WHERE id_usuario_apertura IN (
                        SELECT id_usuario FROM usuarios WHERE nombre_usuario IN (?, ?, ?)
                    )
                    OR id_usuario_cierre IN (
                        SELECT id_usuario FROM usuarios WHERE nombre_usuario IN (?, ?, ?)
                    )
                )
                """, usuarioAdmin, usuarioGerente, usuarioVendedor,
                usuarioAdmin, usuarioGerente, usuarioVendedor);
        jdbcTemplate.update("""
                DELETE FROM cajas_diarias
                WHERE id_usuario_apertura IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario IN (?, ?, ?)
                )
                OR id_usuario_cierre IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario IN (?, ?, ?)
                )
                """, usuarioAdmin, usuarioGerente, usuarioVendedor,
                usuarioAdmin, usuarioGerente, usuarioVendedor);
        jdbcTemplate.update("""
                DELETE FROM auditoria_operaciones
                WHERE id_usuario IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario IN (?, ?, ?)
                )
                """, usuarioAdmin, usuarioGerente, usuarioVendedor);
        jdbcTemplate.update("""
                DELETE FROM ajustes_inventario
                WHERE id_usuario_solicitante IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario IN (?, ?, ?)
                )
                OR id_usuario_aprobador IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario IN (?, ?, ?)
                )
                """, usuarioAdmin, usuarioGerente, usuarioVendedor,
                usuarioAdmin, usuarioGerente, usuarioVendedor);
        jdbcTemplate.update("""
                DELETE FROM sesiones_usuario
                WHERE id_usuario IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario IN (?, ?, ?)
                )
                """, usuarioAdmin, usuarioGerente, usuarioVendedor);
        jdbcTemplate.update("""
                DELETE FROM credenciales_usuario
                WHERE id_usuario IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario IN (?, ?, ?)
                )
                """, usuarioAdmin, usuarioGerente, usuarioVendedor);
        jdbcTemplate.update(
                "DELETE FROM usuarios WHERE nombre_usuario IN (?, ?, ?)",
                usuarioAdmin,
                usuarioGerente,
                usuarioVendedor);
    }
}
