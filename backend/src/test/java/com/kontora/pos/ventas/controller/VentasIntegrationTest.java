package com.kontora.pos.ventas.controller;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class VentasIntegrationTest {

    private static final String PASSWORD = "Clave12345";
    private static final String USUARIO_ADMIN = "test_ventas_admin";
    private static final String USUARIO_GERENTE = "test_ventas_gerente";
    private static final String USUARIO_VENDEDOR = "test_ventas_vendedor";
    private static final String USUARIO_TRABAJADOR = "test_ventas_trabajador";
    private static final LocalDate FECHA_CAJA = LocalDate.of(2200, 1, 1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private UUID idUsuarioAdmin;
    private UUID idUsuarioGerente;
    private UUID idUsuarioTrabajador;

    @BeforeEach
    void setUp() {
        limpiarDatosDePrueba();
        idUsuarioAdmin = crearUsuarioConCredencial(USUARIO_ADMIN, "Administrador Ventas", "administrador");
        idUsuarioGerente = crearUsuarioConCredencial(USUARIO_GERENTE, "Gerente Ventas", "gerente");
        crearUsuarioConCredencial(USUARIO_VENDEDOR, "Vendedor Ventas", "vendedor");
        idUsuarioTrabajador = crearUsuarioConCredencial(USUARIO_TRABAJADOR, "Trabajador Ventas", "vendedor");
    }

    @AfterEach
    void tearDown() {
        limpiarDatosDePrueba();
    }

    @Test
    void registraVentaNormalConPagoEfectivo() throws Exception {
        crearCajaAbierta();
        String token = iniciarSesion(USUARIO_VENDEDOR);

        String response = mockMvc.perform(post("/api/ventas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestVentaEfectivoCliente())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoComprador").value("cliente"))
                .andExpect(jsonPath("$.estadoVenta").value("registrada"))
                .andExpect(jsonPath("$.subtotalVenta").value(8000.00))
                .andExpect(jsonPath("$.descuentoPromocion").value(0.00))
                .andExpect(jsonPath("$.totalVenta").value(8000.00))
                .andExpect(jsonPath("$.detalles[0].cantidad").value(1))
                .andExpect(jsonPath("$.detalles[0].precioUnitarioNormal").value(8000.00))
                .andExpect(jsonPath("$.pagos[0].valorPago").value(8000.00))
                .andExpect(jsonPath("$.pagos[0].valorRecibidoEfectivo").value(10000.00))
                .andExpect(jsonPath("$.pagos[0].cambioEntregado").value(2000.00))
                .andExpect(jsonPath("$.pagos[0].estadoValidacion").value("no_aplica"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID idVenta = UUID.fromString(objectMapper.readValue(response, Map.class).get("idVenta").toString());
        Integer detalles = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM detalles_venta WHERE id_venta = ?",
                Integer.class,
                idVenta);
        Integer pagos = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pagos_venta WHERE id_venta = ?",
                Integer.class,
                idVenta);
        assertThat(detalles).isEqualTo(1);
        assertThat(pagos).isEqualTo(1);
    }

    @Test
    void registraVentaHibridaConPromocionTrabajadorYTransferenciaPendiente() throws Exception {
        crearCajaAbierta();
        String token = iniciarSesion(USUARIO_VENDEDOR);

        mockMvc.perform(post("/api/ventas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestVentaHibridaTrabajador())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoComprador").value("trabajador"))
                .andExpect(jsonPath("$.idUsuarioComprador").value(idUsuarioTrabajador.toString()))
                .andExpect(jsonPath("$.subtotalVenta").value(24000.00))
                .andExpect(jsonPath("$.descuentoPromocion").value(4000.00))
                .andExpect(jsonPath("$.totalVenta").value(20000.00))
                .andExpect(jsonPath("$.detalles[0].cantidad").value(3))
                .andExpect(jsonPath("$.detalles[0].cantidadConPromocion").value(2))
                .andExpect(jsonPath("$.detalles[0].cantidadSinPromocion").value(1))
                .andExpect(jsonPath("$.detalles[0].valorPromocionalAplicado").value(12000.00))
                .andExpect(jsonPath("$.pagos[0].estadoValidacion").value("no_aplica"))
                .andExpect(jsonPath("$.pagos[1].estadoValidacion").value("pendiente"));

        BigDecimal transferenciasPendientes = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(pv.valor_pago), 0)
                FROM pagos_venta pv
                JOIN metodos_pago mp ON mp.id_metodo_pago = pv.id_metodo_pago
                WHERE mp.nombre_metodo = 'transferencia'
                AND pv.estado_validacion = 'pendiente'
                """, BigDecimal.class);
        assertThat(transferenciasPendientes).isEqualByComparingTo("15000.00");
    }

    @Test
    void consultaDetalleParaAnulacionMuestraVasosYPagosSoloParaRolesAutorizados() throws Exception {
        crearCajaAbierta();
        String tokenVendedor = iniciarSesion(USUARIO_VENDEDOR);
        String tokenAdmin = iniciarSesion(USUARIO_ADMIN);

        String response = mockMvc.perform(post("/api/ventas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenVendedor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestVentaHibridaTrabajador())))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID idVenta = UUID.fromString(objectMapper.readValue(response, Map.class).get("idVenta").toString());

        mockMvc.perform(get("/api/ventas/{idVenta}/anulacion", idVenta)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoVenta").value("registrada"))
                .andExpect(jsonPath("$.detalles[0].nombreTipo").value("con_licor"))
                .andExpect(jsonPath("$.detalles[0].onzas").value(8))
                .andExpect(jsonPath("$.detalles[0].cantidad").value(3))
                .andExpect(jsonPath("$.pagos[0].nombreMetodo").value("efectivo"))
                .andExpect(jsonPath("$.pagos[0].valorPago").value(5000.00))
                .andExpect(jsonPath("$.pagos[1].nombreMetodo").value("transferencia"))
                .andExpect(jsonPath("$.pagos[1].valorPago").value(15000.00));

        mockMvc.perform(get("/api/ventas/{idVenta}/anulacion", idVenta)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenVendedor)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("Solo administrador o gerente puede anular ventas"));
    }

    @Test
    void gerenteNoValidaTransferenciaDeUnaVentaAnulada() throws Exception {
        crearCajaAbierta();
        String tokenVendedor = iniciarSesion(USUARIO_VENDEDOR);
        String tokenAdmin = iniciarSesion(USUARIO_ADMIN);
        String tokenGerente = iniciarSesion(USUARIO_GERENTE);

        String response = mockMvc.perform(post("/api/ventas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenVendedor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestVentaHibridaTrabajador())))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID idVenta = UUID.fromString(objectMapper.readValue(response, Map.class).get("idVenta").toString());
        UUID idPagoTransferencia = jdbcTemplate.queryForObject("""
                SELECT pv.id_pago_venta
                FROM pagos_venta pv
                JOIN metodos_pago mp ON mp.id_metodo_pago = pv.id_metodo_pago
                WHERE pv.id_venta = ?
                AND mp.nombre_metodo = 'transferencia'
                """, UUID.class, idVenta);

        mockMvc.perform(post("/api/ventas/{idVenta}/anular", idVenta)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("motivoAnulacion", "Prueba de anulacion"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoVenta").value("anulada"));

        mockMvc.perform(post("/api/pagos-venta/{idPagoVenta}/validar", idPagoTransferencia)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenGerente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje")
                        .value("No se puede validar ni rechazar una transferencia de una venta anulada"));
    }

    @Test
    void listaBeneficiariosActivosYRegistraPromocionParaGerente() throws Exception {
        String token = iniciarSesion(USUARIO_VENDEDOR);

        mockMvc.perform(get("/api/ventas/trabajadores")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.nombreUsuario == 'test_ventas_admin')]").exists())
                .andExpect(jsonPath("$[?(@.nombreUsuario == 'test_ventas_gerente')]").exists())
                .andExpect(jsonPath("$[?(@.nombreUsuario == 'test_ventas_vendedor')]").exists())
                .andExpect(jsonPath("$[?(@.nombreUsuario == 'test_ventas_trabajador')]").exists());

        crearCajaAbierta();
        Map<String, Object> request = new HashMap<>(requestVentaHibridaTrabajador());
        request.put("idUsuarioComprador", idUsuarioGerente.toString());

        mockMvc.perform(post("/api/ventas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idUsuarioComprador").value(idUsuarioGerente.toString()))
                .andExpect(jsonPath("$.descuentoPromocion").value(4000.00))
                .andExpect(jsonPath("$.detalles[0].cantidadConPromocion").value(2));
    }

    @Test
    void rechazaVentaCuandoLaSumaDePagosNoCoincideConTotal() throws Exception {
        crearCajaAbierta();
        String token = iniciarSesion(USUARIO_VENDEDOR);

        mockMvc.perform(post("/api/ventas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestVentaConPagoIncorrecto())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("La suma de pagos debe coincidir con total_venta"));
    }

    private Map<String, Object> requestVentaEfectivoCliente() {
        return Map.of(
                "tipoComprador", "cliente",
                "detalles", List.of(Map.of(
                        "idTipoGranizado", idTipoGranizado("con_licor").toString(),
                        "idTamanoVaso", idTamanoVaso(8).toString(),
                        "cantidad", 1)),
                "pagos", List.of(Map.of(
                        "idMetodoPago", idMetodoPago("efectivo").toString(),
                        "valorPago", new BigDecimal("8000.00"),
                        "valorRecibidoEfectivo", new BigDecimal("10000.00"))));
    }

    private Map<String, Object> requestVentaHibridaTrabajador() {
        return Map.of(
                "tipoComprador", "trabajador",
                "idUsuarioComprador", idUsuarioTrabajador.toString(),
                "detalles", List.of(Map.of(
                        "idTipoGranizado", idTipoGranizado("con_licor").toString(),
                        "idTamanoVaso", idTamanoVaso(8).toString(),
                        "cantidad", 3)),
                "pagos", List.of(
                        Map.of(
                                "idMetodoPago", idMetodoPago("efectivo").toString(),
                                "valorPago", new BigDecimal("5000.00"),
                                "valorRecibidoEfectivo", new BigDecimal("5000.00")),
                        Map.of(
                                "idMetodoPago", idMetodoPago("transferencia").toString(),
                                "valorPago", new BigDecimal("15000.00"))));
    }

    private Map<String, Object> requestVentaConPagoIncorrecto() {
        return Map.of(
                "tipoComprador", "cliente",
                "detalles", List.of(Map.of(
                        "idTipoGranizado", idTipoGranizado("con_licor").toString(),
                        "idTamanoVaso", idTamanoVaso(8).toString(),
                        "cantidad", 1)),
                "pagos", List.of(Map.of(
                        "idMetodoPago", idMetodoPago("efectivo").toString(),
                        "valorPago", new BigDecimal("7000.00"),
                        "valorRecibidoEfectivo", new BigDecimal("7000.00"))));
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

    private void crearCajaAbierta() {
        UUID idCajaDiaria = jdbcTemplate.queryForObject("""
                INSERT INTO cajas_diarias (
                    fecha_operacion,
                    estado_caja,
                    valor_base,
                    id_usuario_apertura,
                    observaciones
                )
                VALUES (?, 'abierta'::estado_caja_enum, 300000, ?, 'test_ventas_caja')
                RETURNING id_caja_diaria
                """, UUID.class, FECHA_CAJA, idUsuarioAdmin);
        crearStockDiarioVaso(idCajaDiaria, 8, 20);
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
                DELETE FROM movimientos_inventario
                WHERE id_usuario_registro IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_ventas_%'
                )
                OR id_caja_diaria IN (
                    SELECT id_caja_diaria
                    FROM cajas_diarias
                    WHERE fecha_operacion >= DATE '2099-01-01'
                    OR observaciones LIKE 'test_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM ventas
                WHERE id_caja_diaria IN (
                    SELECT id_caja_diaria
                    FROM cajas_diarias
                    WHERE fecha_operacion >= DATE '2099-01-01'
                    OR observaciones LIKE 'test_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM existencias_inventario_diario
                WHERE id_caja_diaria IN (
                    SELECT id_caja_diaria
                    FROM cajas_diarias
                    WHERE fecha_operacion >= DATE '2099-01-01'
                    OR observaciones LIKE 'test_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM movimientos_deposito
                WHERE id_cierre_caja IN (
                    SELECT cc.id_cierre_caja
                    FROM cierres_caja cc
                    JOIN cajas_diarias cd ON cd.id_caja_diaria = cc.id_caja_diaria
                    WHERE cd.fecha_operacion >= DATE '2099-01-01'
                    OR cd.observaciones LIKE 'test_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM cierres_caja
                WHERE id_caja_diaria IN (
                    SELECT id_caja_diaria
                    FROM cajas_diarias
                    WHERE fecha_operacion >= DATE '2099-01-01'
                    OR observaciones LIKE 'test_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM cajas_diarias
                WHERE fecha_operacion >= DATE '2099-01-01'
                OR observaciones LIKE 'test_%'
                """);
        jdbcTemplate.update("""
                DELETE FROM auditoria_operaciones
                WHERE id_usuario IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_ventas_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM sesiones_usuario
                WHERE id_usuario IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_ventas_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM credenciales_usuario
                WHERE id_usuario IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_ventas_%'
                )
                """);
        jdbcTemplate.update("DELETE FROM usuarios WHERE nombre_usuario LIKE 'test_ventas_%'");
    }
}
