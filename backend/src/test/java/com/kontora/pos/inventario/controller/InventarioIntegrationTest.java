package com.kontora.pos.inventario.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kontora.pos.caja.repository.CajaDiariaRepository;
import com.kontora.pos.inventario.service.InventarioService;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class InventarioIntegrationTest {

    private static final String PASSWORD = "Clave12345";
    private static final String USUARIO_ADMIN = "test_inventario_admin";
    private static final String USUARIO_GERENTE = "test_inventario_gerente";
    private static final String USUARIO_VENDEDOR = "test_inventario_vendedor";
    private static final LocalDate FECHA_CAJA = LocalDate.of(2200, 2, 1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CajaDiariaRepository cajaDiariaRepository;

    @Autowired
    private InventarioService inventarioService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private UUID idUsuarioAdmin;
    private UUID idCajaDiaria;

    @BeforeEach
    void setUp() {
        limpiarDatosDePrueba();
        idUsuarioAdmin = crearUsuarioConCredencial(USUARIO_ADMIN, "Administrador Inventario", "administrador");
        crearUsuarioConCredencial(USUARIO_GERENTE, "Gerente Inventario", "gerente");
        crearUsuarioConCredencial(USUARIO_VENDEDOR, "Vendedor Inventario", "vendedor");
    }

    @AfterEach
    void tearDown() {
        limpiarDatosDePrueba();
    }

    @Test
    void sinUsuarioAutenticadoNoConsultaInventario() throws Exception {
        mockMvc.perform(get("/api/inventario/existencias/general"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void consultaVentasDeVasosDeLaCajaAbiertaPorTipoYTamano() throws Exception {
        crearCajaAbierta();
        registrarVentaParaResumen("con_licor", 8, 33, false);
        registrarVentaParaResumen("sin_licor", 8, 8, false);
        registrarVentaParaResumen("con_licor", 12, 56, false);
        registrarVentaParaResumen("con_licor", 8, 4, true);
        String tokenGerente = iniciarSesion(USUARIO_GERENTE);

        mockMvc.perform(get("/api/inventario/ventas-vasos/diaria-abierta")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenGerente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].idCajaDiaria").value(idCajaDiaria.toString()))
                .andExpect(jsonPath("$[0].nombreTipo").value("con_licor"))
                .andExpect(jsonPath("$[0].onzas").value(8))
                .andExpect(jsonPath("$[0].vasosVendidos").value(33))
                .andExpect(jsonPath("$[1].nombreTipo").value("sin_licor"))
                .andExpect(jsonPath("$[1].onzas").value(8))
                .andExpect(jsonPath("$[1].vasosVendidos").value(8))
                .andExpect(jsonPath("$[2].nombreTipo").value("con_licor"))
                .andExpect(jsonPath("$[2].onzas").value(12))
                .andExpect(jsonPath("$[2].vasosVendidos").value(56));
    }

    @Test
    void vendedorNoPuedeConsultarVentasDeVasosDeInventario() throws Exception {
        String tokenVendedor = iniciarSesion(USUARIO_VENDEDOR);

        mockMvc.perform(get("/api/inventario/ventas-vasos/diaria-abierta")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenVendedor)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("Solo administrador o gerente puede consultar inventario"));
    }

    @Test
    void registraPaqueteVasosYMovimientos() throws Exception {
        crearCajaAbierta();
        UUID idVaso = idItemInventario("vaso_8oz");
        ajustarStockGeneral(idVaso, 40);
        String token = iniciarSesion(USUARIO_ADMIN);

        String response = mockMvc.perform(post("/api/inventario/paquetes-vasos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "idItemInventario", idVaso.toString(),
                                "cantidadPaquetes", 1,
                                "unidadesRotas", 2))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombreItem").value("vaso_8oz"))
                .andExpect(jsonPath("$.unidadesGeneradas").value(20))
                .andExpect(jsonPath("$.unidadesRotas").value(2))
                .andExpect(jsonPath("$.unidadesDisponibles").value(18))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID idPaquete = UUID.fromString(objectMapper.readValue(response, Map.class).get("idPaqueteVasosAbierto").toString());
        Integer stockGeneral = stockGeneral(idVaso);
        Map<String, Object> existenciaDiaria = existenciaDiaria(idVaso);
        Integer movimientos = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM movimientos_inventario
                WHERE referencia_origen = 'paquetes_vasos_abiertos'
                AND id_referencia_origen = ?
                """, Integer.class, idPaquete);

        assertThat(stockGeneral).isEqualTo(20);
        assertThat(existenciaDiaria.get("cantidad_ingresada")).isEqualTo(20);
        assertThat(existenciaDiaria.get("cantidad_perdida")).isEqualTo(2);
        assertThat(existenciaDiaria.get("cantidad_final_teorica")).isEqualTo(18);
        assertThat(movimientos).isEqualTo(3);

        mockMvc.perform(get("/api/inventario/movimientos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].tipoMovimiento", hasItem("apertura_paquete")))
                .andExpect(jsonPath("$[*].tipoMovimiento", hasItem("perdida")));
    }

    @Test
    void registraConsumoManualYDescuentaStockGeneral() throws Exception {
        crearCajaAbierta();
        UUID idItemManual = idItemInventario("bolsa_chicles");
        ajustarStockGeneral(idItemManual, 10);
        String token = iniciarSesion(USUARIO_ADMIN);

        String response = mockMvc.perform(post("/api/inventario/consumos-diarios")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "idItemInventario", idItemManual.toString(),
                                "cantidadConsumida", 3,
                                "observacion", "Consumo test"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombreItem").value("bolsa_chicles"))
                .andExpect(jsonPath("$.cantidadConsumida").value(3))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID idConsumo = UUID.fromString(objectMapper.readValue(response, Map.class).get("idConsumoDiarioInventario").toString());
        Integer movimientos = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM movimientos_inventario
                WHERE tipo_movimiento = 'consumo_diario'
                AND referencia_origen = 'consumos_diarios_inventario'
                AND id_referencia_origen = ?
                """, Integer.class, idConsumo);

        assertThat(stockGeneral(idItemManual)).isEqualTo(7);
        assertThat(movimientos).isEqualTo(1);
    }

    @Test
    void solicitaAjusteYGerenteApruebaEntradaStockGeneral() throws Exception {
        UUID idItemManual = idItemInventario("bolsa_chicles");
        String tokenAdmin = iniciarSesion(USUARIO_ADMIN);
        String tokenGerente = iniciarSesion(USUARIO_GERENTE);

        UUID idAjuste = solicitarAjuste(tokenAdmin, idItemManual, 12, "entrada", "Reabastecimiento test");
        assertThat(stockGeneral(idItemManual)).isZero();

        mockMvc.perform(post("/api/inventario/ajustes/{idAjusteInventario}/aprobar", idAjuste)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenGerente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "observacionAprobacion", "Aprobado para reabastecimiento"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoAprobacion").value("aprobado"))
                .andExpect(jsonPath("$.nombreUsuarioAprobador").value(USUARIO_GERENTE));

        Integer movimientos = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM movimientos_inventario
                WHERE tipo_stock = 'general'
                AND tipo_movimiento = 'ajuste'
                AND sentido_movimiento = 'entrada'
                AND referencia_origen = 'ajustes_inventario'
                AND id_referencia_origen = ?
                """, Integer.class, idAjuste);
        Integer auditorias = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM auditoria_operaciones
                WHERE tabla_afectada = 'ajustes_inventario'
                AND id_registro_afectado = ?
                AND accion::text IN ('crear', 'aprobar')
                """, Integer.class, idAjuste.toString());

        assertThat(stockGeneral(idItemManual)).isEqualTo(12);
        assertThat(movimientos).isEqualTo(1);
        assertThat(auditorias).isEqualTo(2);

        mockMvc.perform(get("/api/inventario/ajustes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenGerente))
                        .param("estadoAprobacion", "aprobado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].estadoAprobacion", hasItem("aprobado")))
                .andExpect(jsonPath("$[*].idAjusteInventario", hasItem(idAjuste.toString())));
    }

    @Test
    void gerenteRegistraStockGeneralYSeAplicaDirectamente() throws Exception {
        UUID idItemManual = idItemInventario("bolsa_chicles");
        String tokenGerente = iniciarSesion(USUARIO_GERENTE);

        String response = mockMvc.perform(post("/api/inventario/ajustes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenGerente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "idItemInventario", idItemManual.toString(),
                                "tipoStock", "general",
                                "cantidadAjuste", 6,
                                "sentidoAjuste", "entrada",
                                "motivoAjuste", "Conteo fisico gerencial"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoAprobacion").value("aprobado"))
                .andExpect(jsonPath("$.nombreUsuarioSolicitante").value(USUARIO_GERENTE))
                .andExpect(jsonPath("$.nombreUsuarioAprobador").value(USUARIO_GERENTE))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID idAjuste = UUID.fromString(objectMapper.readValue(response, Map.class).get("idAjusteInventario").toString());
        Integer movimientos = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM movimientos_inventario
                WHERE tipo_stock = 'general'
                AND tipo_movimiento = 'ajuste'
                AND sentido_movimiento = 'entrada'
                AND referencia_origen = 'ajustes_inventario'
                AND id_referencia_origen = ?
                """, Integer.class, idAjuste);
        Integer auditorias = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM auditoria_operaciones
                WHERE tabla_afectada = 'ajustes_inventario'
                AND id_registro_afectado = ?
                AND accion::text IN ('crear', 'aprobar')
                """, Integer.class, idAjuste.toString());

        assertThat(stockGeneral(idItemManual)).isEqualTo(6);
        assertThat(movimientos).isEqualTo(1);
        assertThat(auditorias).isEqualTo(2);
    }

    @Test
    void gerenteReabasteceStockDiarioConUnidadesSueltasSinCrearPaquete() throws Exception {
        crearCajaAbierta();
        UUID idVaso = idItemInventario("vaso_8oz");
        ajustarStockGeneral(idVaso, 107);
        String tokenGerente = iniciarSesion(USUARIO_GERENTE);

        String response = mockMvc.perform(post("/api/inventario/ajustes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenGerente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "idItemInventario", idVaso.toString(),
                                "tipoStock", "diario",
                                "cantidadAjuste", 7,
                                "sentidoAjuste", "entrada",
                                "motivoAjuste", "Reabastecimiento de stock diario"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoAprobacion").value("aprobado"))
                .andExpect(jsonPath("$.tipoStock").value("diario"))
                .andExpect(jsonPath("$.idCajaDiaria").value(idCajaDiaria.toString()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID idAjuste = UUID.fromString(
                objectMapper.readValue(response, Map.class).get("idAjusteInventario").toString());
        Map<String, Object> existenciaDiaria = existenciaDiaria(idVaso);
        List<Map<String, Object>> movimientos = jdbcTemplate.queryForList("""
                SELECT tipo_stock::text AS tipo_stock,
                       sentido_movimiento::text AS sentido_movimiento,
                       cantidad AS cantidad_movimiento,
                       observacion
                FROM movimientos_inventario
                WHERE referencia_origen = 'ajustes_inventario'
                AND id_referencia_origen = ?
                ORDER BY tipo_stock::text
                """, idAjuste);
        Integer paquetesCreados = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM paquetes_vasos_abiertos
                WHERE id_caja_diaria = ?
                AND id_item_inventario = ?
                """, Integer.class, idCajaDiaria, idVaso);

        assertThat(stockGeneral(idVaso)).isEqualTo(100);
        assertThat(existenciaDiaria.get("cantidad_ajustada")).isEqualTo(7);
        assertThat(existenciaDiaria.get("cantidad_final_teorica")).isEqualTo(7);
        assertThat(stockGeneral(idVaso)
                + (Integer) existenciaDiaria.get("cantidad_final_teorica")).isEqualTo(107);
        assertThat(movimientos).hasSize(2);
        assertThat(movimientos).anySatisfy(movimiento -> {
            assertThat(movimiento.get("tipo_stock")).isEqualTo("general");
            assertThat(movimiento.get("sentido_movimiento")).isEqualTo("salida");
            assertThat(movimiento.get("cantidad_movimiento")).isEqualTo(7);
            assertThat(movimiento.get("observacion").toString())
                    .contains("reabastecimiento del stock diario");
        });
        assertThat(movimientos).anySatisfy(movimiento -> {
            assertThat(movimiento.get("tipo_stock")).isEqualTo("diario");
            assertThat(movimiento.get("sentido_movimiento")).isEqualTo("entrada");
            assertThat(movimiento.get("cantidad_movimiento")).isEqualTo(7);
            assertThat(movimiento.get("observacion").toString())
                    .contains("reabastecimiento del stock diario");
        });
        assertThat(paquetesCreados).isZero();
    }

    @Test
    void administradorSolicitaReabastecimientoYGerenteLoAprueba() throws Exception {
        crearCajaAbierta();
        UUID idVaso = idItemInventario("vaso_8oz");
        ajustarStockGeneral(idVaso, 30);
        String tokenAdmin = iniciarSesion(USUARIO_ADMIN);
        String tokenGerente = iniciarSesion(USUARIO_GERENTE);

        UUID idAjuste = solicitarAjuste(
                tokenAdmin,
                idVaso,
                "diario",
                5,
                "entrada",
                "Reabastecimiento solicitado por administrador");

        assertThat(stockGeneral(idVaso)).isEqualTo(30);
        assertThat(existenciaDiaria(idVaso).get("cantidad_ajustada")).isEqualTo(0);

        mockMvc.perform(post("/api/inventario/ajustes/{idAjusteInventario}/aprobar", idAjuste)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenGerente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "observacionAprobacion", "Reabastecimiento verificado"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoAprobacion").value("aprobado"))
                .andExpect(jsonPath("$.tipoStock").value("diario"));

        Map<String, Object> existenciaDiaria = existenciaDiaria(idVaso);
        Integer movimientos = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM movimientos_inventario
                WHERE referencia_origen = 'ajustes_inventario'
                AND id_referencia_origen = ?
                """, Integer.class, idAjuste);

        assertThat(stockGeneral(idVaso)).isEqualTo(25);
        assertThat(existenciaDiaria.get("cantidad_ajustada")).isEqualTo(5);
        assertThat(existenciaDiaria.get("cantidad_final_teorica")).isEqualTo(5);
        assertThat(movimientos).isEqualTo(2);
    }

    @Test
    void rechazaReabastecimientoDeStockDiarioSinCajaAbierta() throws Exception {
        UUID idVaso = idItemInventario("vaso_8oz");
        ajustarStockGeneral(idVaso, 10);
        String tokenGerente = iniciarSesion(USUARIO_GERENTE);

        mockMvc.perform(post("/api/inventario/ajustes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenGerente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "idItemInventario", idVaso.toString(),
                                "tipoStock", "diario",
                                "cantidadAjuste", 3,
                                "sentidoAjuste", "entrada",
                                "motivoAjuste", "Reabastecimiento sin caja"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje")
                        .value("No existe caja diaria abierta para operar inventario"));

        assertThat(stockGeneral(idVaso)).isEqualTo(10);
    }

    @Test
    void rechazaReabastecimientoDeStockDiarioConStockGeneralInsuficiente() throws Exception {
        crearCajaAbierta();
        UUID idVaso = idItemInventario("vaso_8oz");
        ajustarStockGeneral(idVaso, 4);
        String tokenGerente = iniciarSesion(USUARIO_GERENTE);

        mockMvc.perform(post("/api/inventario/ajustes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenGerente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "idItemInventario", idVaso.toString(),
                                "tipoStock", "diario",
                                "cantidadAjuste", 7,
                                "sentidoAjuste", "entrada",
                                "motivoAjuste", "Reabastecimiento mayor al stock"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje")
                        .value("Stock general insuficiente para reabastecer el stock diario"));

        assertThat(stockGeneral(idVaso)).isEqualTo(4);
        assertThat(existenciaDiaria(idVaso).get("cantidad_ajustada")).isEqualTo(0);
    }

    @Test
    void gerenteRechazaAjusteSinModificarStockGeneral() throws Exception {
        UUID idItemManual = idItemInventario("bolsa_chicles");
        ajustarStockGeneral(idItemManual, 5);
        String tokenAdmin = iniciarSesion(USUARIO_ADMIN);
        String tokenGerente = iniciarSesion(USUARIO_GERENTE);

        UUID idAjuste = solicitarAjuste(tokenAdmin, idItemManual, 4, "entrada", "Solicitud no aplicada");

        mockMvc.perform(post("/api/inventario/ajustes/{idAjusteInventario}/rechazar", idAjuste)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenGerente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "observacionAprobacion", "No corresponde al inventario fisico"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoAprobacion").value("rechazado"));

        Integer movimientos = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM movimientos_inventario
                WHERE referencia_origen = 'ajustes_inventario'
                AND id_referencia_origen = ?
                """, Integer.class, idAjuste);

        assertThat(stockGeneral(idItemManual)).isEqualTo(5);
        assertThat(movimientos).isZero();
    }

    @Test
    void administradorNoPuedeAprobarAjusteInventario() throws Exception {
        UUID idItemManual = idItemInventario("bolsa_chicles");
        String tokenAdmin = iniciarSesion(USUARIO_ADMIN);
        UUID idAjuste = solicitarAjuste(tokenAdmin, idItemManual, 3, "entrada", "Solicitud test");

        mockMvc.perform(post("/api/inventario/ajustes/{idAjusteInventario}/aprobar", idAjuste)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "observacionAprobacion", "Intento no permitido"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("Solo gerente puede aprobar o rechazar ajustes de inventario"));
    }

    @Test
    void vendedorNoPuedeConsultarAjustesInventario() throws Exception {
        String tokenVendedor = iniciarSesion(USUARIO_VENDEDOR);

        mockMvc.perform(get("/api/inventario/ajustes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenVendedor)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("Solo administrador o gerente puede consultar ajustes de inventario"));
    }

    @Test
    void rechazaAprobacionQueDejariaStockGeneralNegativo() throws Exception {
        UUID idItemManual = idItemInventario("bolsa_chicles");
        ajustarStockGeneral(idItemManual, 2);
        String tokenAdmin = iniciarSesion(USUARIO_ADMIN);
        String tokenGerente = iniciarSesion(USUARIO_GERENTE);
        UUID idAjuste = solicitarAjuste(tokenAdmin, idItemManual, 3, "salida", "Ajuste mayor al stock");

        mockMvc.perform(post("/api/inventario/ajustes/{idAjusteInventario}/aprobar", idAjuste)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenGerente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "observacionAprobacion", "No debe aprobar"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("El ajuste dejaria stock general negativo"));

        String estado = jdbcTemplate.queryForObject("""
                SELECT estado_aprobacion::text
                FROM ajustes_inventario
                WHERE id_ajuste_inventario = ?
                """, String.class, idAjuste);

        assertThat(stockGeneral(idItemManual)).isEqualTo(2);
        assertThat(estado).isEqualTo("pendiente");
    }

    @Test
    void rechazaConsumoManualDeItemAutomaticoPorVenta() throws Exception {
        crearCajaAbierta();
        UUID idVaso = idItemInventario("vaso_8oz");
        String token = iniciarSesion(USUARIO_ADMIN);

        mockMvc.perform(post("/api/inventario/consumos-diarios")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "idItemInventario", idVaso.toString(),
                                "cantidadConsumida", 1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Los consumos manuales no aplican a items automaticos por venta"));
    }

    @Test
    void descuentaVasosPorVentaYRestauraPorAnulacion() throws Exception {
        crearCajaAbierta();
        UUID idVaso = idItemInventario("vaso_8oz");
        ajustarStockGeneral(idVaso, 40);
        String tokenAdmin = iniciarSesion(USUARIO_ADMIN);
        String tokenVendedor = iniciarSesion(USUARIO_VENDEDOR);

        mockMvc.perform(post("/api/inventario/paquetes-vasos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "idItemInventario", idVaso.toString(),
                                "cantidadPaquetes", 1,
                                "unidadesRotas", 0))))
                .andExpect(status().isCreated());

        String ventaResponse = mockMvc.perform(post("/api/ventas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenVendedor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestVentaEfectivoCliente(1))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoVenta").value("registrada"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID idVenta = UUID.fromString(objectMapper.readValue(ventaResponse, Map.class).get("idVenta").toString());
        Map<String, Object> existenciaDespuesVenta = existenciaDiaria(idVaso);
        assertThat(existenciaDespuesVenta.get("cantidad_vendida")).isEqualTo(1);
        assertThat(existenciaDespuesVenta.get("cantidad_final_teorica")).isEqualTo(19);

        mockMvc.perform(post("/api/ventas/{idVenta}/anular", idVenta)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("motivoAnulacion", "Error de prueba"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoVenta").value("anulada"));

        Map<String, Object> existenciaDespuesAnulacion = existenciaDiaria(idVaso);
        Integer movimientosAnulacion = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM movimientos_inventario
                WHERE tipo_movimiento = 'anulacion_venta'
                AND referencia_origen = 'ventas'
                AND id_referencia_origen = ?
                """, Integer.class, idVenta);

        assertThat(existenciaDespuesAnulacion.get("cantidad_vendida")).isEqualTo(0);
        assertThat(existenciaDespuesAnulacion.get("cantidad_final_teorica")).isEqualTo(20);
        assertThat(movimientosAnulacion).isEqualTo(1);
    }

    private Map<String, Object> requestVentaEfectivoCliente(int cantidad) {
        BigDecimal total = BigDecimal.valueOf(8000L * cantidad).setScale(2);
        return Map.of(
                "tipoComprador", "cliente",
                "detalles", List.of(Map.of(
                        "idTipoGranizado", idTipoGranizado("con_licor").toString(),
                        "idTamanoVaso", idTamanoVaso(8).toString(),
                        "cantidad", cantidad)),
                "pagos", List.of(Map.of(
                        "idMetodoPago", idMetodoPago("efectivo").toString(),
                        "valorPago", total,
                "valorRecibidoEfectivo", total)));
    }

    private void registrarVentaParaResumen(String nombreTipo, int onzas, int cantidad, boolean anulada) {
        UUID idVenta;
        if (anulada) {
            idVenta = jdbcTemplate.queryForObject("""
                    INSERT INTO ventas (
                        id_caja_diaria,
                        id_usuario_vendedor,
                        tipo_comprador,
                        estado_venta,
                        subtotal_venta,
                        descuento_promocion,
                        total_venta,
                        motivo_anulacion,
                        fecha_anulacion,
                        id_usuario_anulacion
                    )
                    VALUES (?, ?, 'cliente'::tipo_comprador_enum, 'anulada'::estado_venta_enum, ?, 0, ?,
                            'Anulacion de prueba', NOW(), ?)
                    RETURNING id_venta
                    """, UUID.class, idCajaDiaria, idUsuarioAdmin,
                    BigDecimal.valueOf(cantidad), BigDecimal.valueOf(cantidad), idUsuarioAdmin);
        } else {
            idVenta = jdbcTemplate.queryForObject("""
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
                    """, UUID.class, idCajaDiaria, idUsuarioAdmin,
                    BigDecimal.valueOf(cantidad), BigDecimal.valueOf(cantidad));
        }

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
                """, idVenta, idTipoGranizado(nombreTipo), idTamanoVaso(onzas), cantidad,
                cantidad, BigDecimal.valueOf(cantidad), BigDecimal.valueOf(cantidad));
    }

    private UUID solicitarAjuste(
            String token,
            UUID idItemInventario,
            int cantidadAjuste,
            String sentidoAjuste,
            String motivoAjuste) throws Exception {
        return solicitarAjuste(
                token,
                idItemInventario,
                "general",
                cantidadAjuste,
                sentidoAjuste,
                motivoAjuste);
    }

    private UUID solicitarAjuste(
            String token,
            UUID idItemInventario,
            String tipoStock,
            int cantidadAjuste,
            String sentidoAjuste,
            String motivoAjuste) throws Exception {
        String response = mockMvc.perform(post("/api/inventario/ajustes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "idItemInventario", idItemInventario.toString(),
                                "tipoStock", tipoStock,
                                "cantidadAjuste", cantidadAjuste,
                                "sentidoAjuste", sentidoAjuste,
                                "motivoAjuste", motivoAjuste))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoAprobacion").value("pendiente"))
                .andExpect(jsonPath("$.tipoStock").value(tipoStock))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return UUID.fromString(objectMapper.readValue(response, Map.class).get("idAjusteInventario").toString());
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

    private UUID idItemInventario(String nombreItem) {
        return jdbcTemplate.queryForObject(
                "SELECT id_item_inventario FROM items_inventario WHERE nombre_item = ?",
                UUID.class,
                nombreItem);
    }

    private Integer stockGeneral(UUID idItemInventario) {
        return jdbcTemplate.queryForObject(
                "SELECT cantidad_actual FROM existencias_inventario_general WHERE id_item_inventario = ?",
                Integer.class,
                idItemInventario);
    }

    private Map<String, Object> existenciaDiaria(UUID idItemInventario) {
        return jdbcTemplate.queryForMap("""
                SELECT cantidad_ingresada,
                       cantidad_vendida,
                       cantidad_perdida,
                       cantidad_ajustada,
                       cantidad_final_teorica
                FROM existencias_inventario_diario
                WHERE id_item_inventario = ?
                AND id_caja_diaria = ?
                """, idItemInventario, idCajaDiaria);
    }

    private void ajustarStockGeneral(UUID idItemInventario, int cantidad) {
        jdbcTemplate.update("""
                UPDATE existencias_inventario_general
                SET cantidad_actual = ?
                WHERE id_item_inventario = ?
                """, cantidad, idItemInventario);
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
                VALUES (?, 'abierta'::estado_caja_enum, 300000, ?, 'test_inventario_caja')
                RETURNING id_caja_diaria
                """, UUID.class, FECHA_CAJA, idUsuarioAdmin);
        inventarioService.inicializarStockDiarioParaCaja(
                cajaDiariaRepository.findById(idCajaDiaria).orElseThrow());
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
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_inventario_%'
                )
                OR id_caja_diaria IN (
                    SELECT id_caja_diaria
                    FROM cajas_diarias
                    WHERE fecha_operacion >= DATE '2099-01-01'
                    OR observaciones LIKE 'test_inventario_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM ajustes_inventario
                WHERE id_usuario_solicitante IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_inventario_%'
                )
                OR id_usuario_aprobador IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_inventario_%'
                )
                OR id_caja_diaria IN (
                    SELECT id_caja_diaria
                    FROM cajas_diarias
                    WHERE fecha_operacion >= DATE '2099-01-01'
                    OR observaciones LIKE 'test_inventario_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM ventas
                WHERE id_caja_diaria IN (
                    SELECT id_caja_diaria
                    FROM cajas_diarias
                    WHERE fecha_operacion >= DATE '2099-01-01'
                    OR observaciones LIKE 'test_inventario_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM adiciones_diarias
                WHERE id_usuario_registro IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_inventario_%'
                )
                OR id_caja_diaria IN (
                    SELECT id_caja_diaria
                    FROM cajas_diarias
                    WHERE fecha_operacion >= DATE '2099-01-01'
                    OR observaciones LIKE 'test_inventario_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM pagos_trabajadores_diarios
                WHERE id_usuario_registro IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_inventario_%'
                )
                OR id_caja_diaria IN (
                    SELECT id_caja_diaria
                    FROM cajas_diarias
                    WHERE fecha_operacion >= DATE '2099-01-01'
                    OR observaciones LIKE 'test_inventario_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM consumos_diarios_inventario
                WHERE id_caja_diaria IN (
                    SELECT id_caja_diaria
                    FROM cajas_diarias
                    WHERE fecha_operacion >= DATE '2099-01-01'
                    OR observaciones LIKE 'test_inventario_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM paquetes_vasos_abiertos
                WHERE id_caja_diaria IN (
                    SELECT id_caja_diaria
                    FROM cajas_diarias
                    WHERE fecha_operacion >= DATE '2099-01-01'
                    OR observaciones LIKE 'test_inventario_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM existencias_inventario_diario
                WHERE id_caja_diaria IN (
                    SELECT id_caja_diaria
                    FROM cajas_diarias
                    WHERE fecha_operacion >= DATE '2099-01-01'
                    OR observaciones LIKE 'test_inventario_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM movimientos_deposito
                WHERE id_cierre_caja IN (
                    SELECT cc.id_cierre_caja
                    FROM cierres_caja cc
                    JOIN cajas_diarias cd ON cd.id_caja_diaria = cc.id_caja_diaria
                    WHERE cd.fecha_operacion >= DATE '2099-01-01'
                    OR cd.observaciones LIKE 'test_inventario_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM cierres_caja
                WHERE id_caja_diaria IN (
                    SELECT id_caja_diaria
                    FROM cajas_diarias
                    WHERE fecha_operacion >= DATE '2099-01-01'
                    OR observaciones LIKE 'test_inventario_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM cajas_diarias
                WHERE fecha_operacion >= DATE '2099-01-01'
                OR observaciones LIKE 'test_inventario_%'
                """);
        jdbcTemplate.update("""
                DELETE FROM movimientos_deposito
                WHERE id_cierre_caja IN (
                    SELECT cc.id_cierre_caja
                    FROM cierres_caja cc
                    JOIN cajas_diarias cd ON cd.id_caja_diaria = cc.id_caja_diaria
                    WHERE cd.id_usuario_apertura IN (
                        SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_inventario_%'
                    )
                    OR cd.id_usuario_cierre IN (
                        SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_inventario_%'
                    )
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM ventas
                WHERE id_caja_diaria IN (
                    SELECT id_caja_diaria
                    FROM cajas_diarias
                    WHERE id_usuario_apertura IN (
                        SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_inventario_%'
                    )
                    OR id_usuario_cierre IN (
                        SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_inventario_%'
                    )
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM cierres_caja
                WHERE id_caja_diaria IN (
                    SELECT id_caja_diaria
                    FROM cajas_diarias
                    WHERE id_usuario_apertura IN (
                        SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_inventario_%'
                    )
                    OR id_usuario_cierre IN (
                        SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_inventario_%'
                    )
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM cajas_diarias
                WHERE id_usuario_apertura IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_inventario_%'
                )
                OR id_usuario_cierre IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_inventario_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM auditoria_operaciones
                WHERE id_usuario IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_inventario_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM sesiones_usuario
                WHERE id_usuario IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_inventario_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM credenciales_usuario
                WHERE id_usuario IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_inventario_%'
                )
                """);
        jdbcTemplate.update("DELETE FROM usuarios WHERE nombre_usuario LIKE 'test_inventario_%'");
        jdbcTemplate.update("""
                UPDATE existencias_inventario_general
                SET cantidad_actual = 0
                WHERE id_item_inventario IN (
                    SELECT id_item_inventario
                    FROM items_inventario
                    WHERE nombre_item IN ('vaso_8oz', 'bolsa_chicles')
                )
                """);
    }
}
