package com.kontora.pos.catalogos.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class GestionCatalogosIntegrationTest {

    private static final String PASSWORD = "Clave12345";
    private static final String PREFIJO_PRUEBAS = "catalogosgestion%";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final String sufijoPrueba = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String prefijo = "catalogosgestion" + sufijoPrueba;
    private final String usuarioAdministrador = prefijo + "admin";
    private final String usuarioGerente = prefijo + "gerente";
    private final String usuarioVendedor = prefijo + "vendedor";
    private final String itemPrincipal = prefijo + "item";
    private final String itemConStock = prefijo + "stock";
    private UUID idPrecioBase;
    private LocalDate fechaFinPrecioBase;

    @BeforeEach
    void setUp() {
        limpiarDatosPrueba();
        restaurarPrecioBaseSemilla();
        crearUsuarioConCredencial(usuarioAdministrador, "Administrador gestion catalogos", "administrador");
        crearUsuarioConCredencial(usuarioGerente, "Gerente gestion catalogos", "gerente");
        crearUsuarioConCredencial(usuarioVendedor, "Vendedor gestion catalogos", "vendedor");
        idPrecioBase = jdbcTemplate.queryForObject("""
                SELECT pg.id_precio_granizado
                FROM precios_granizado pg
                JOIN tipos_granizado tg ON tg.id_tipo_granizado = pg.id_tipo_granizado
                JOIN tamanos_vaso tv ON tv.id_tamano_vaso = pg.id_tamano_vaso
                WHERE tg.nombre_tipo = 'con_licor'
                  AND tv.onzas = 8
                  AND pg.estado = 'activo'
                  AND pg.fecha_fin_vigencia IS NULL
                """, UUID.class);
        fechaFinPrecioBase = jdbcTemplate.queryForObject(
                "SELECT fecha_fin_vigencia FROM precios_granizado WHERE id_precio_granizado = ?",
                LocalDate.class,
                idPrecioBase);
    }

    @AfterEach
    void tearDown() {
        limpiarDatosPrueba();
        if (idPrecioBase != null) {
            jdbcTemplate.update(
                    "UPDATE precios_granizado SET fecha_fin_vigencia = ? WHERE id_precio_granizado = ?",
                    fechaFinPrecioBase,
                    idPrecioBase);
        }
    }

    @Test
    void administradorCreaEditaEInhabilitaItemSinStock() throws Exception {
        String tokenAdministrador = iniciarSesion(usuarioAdministrador);
        UUID categoriaDesechables = idCategoria("desechables");
        UUID unidadUnidad = idUnidad("unidad");

        String respuesta = mockMvc.perform(post("/api/catalogos/gestion/items-inventario")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdministrador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "idCategoriaInventario", categoriaDesechables,
                                "idUnidadMedida", unidadUnidad,
                                "nombreItem", itemPrincipal,
                                "tipoControl", "manual_por_consumo",
                                "manejaPaquetes", false))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombreItem").value(itemPrincipal))
                .andExpect(jsonPath("$.estado").value("activo"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID idItem = UUID.fromString(objectMapper.readValue(respuesta, Map.class).get("idItemInventario").toString());

        Integer existenciaInicial = jdbcTemplate.queryForObject(
                "SELECT cantidad_actual FROM existencias_inventario_general WHERE id_item_inventario = ?",
                Integer.class,
                idItem);
        assertThat(existenciaInicial).isZero();

        mockMvc.perform(put("/api/catalogos/gestion/items-inventario/{idItemInventario}", idItem)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdministrador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "idCategoriaInventario", categoriaDesechables,
                                "idUnidadMedida", unidadUnidad,
                                "nombreItem", itemPrincipal + "editado",
                                "tipoControl", "manual_por_consumo",
                                "manejaPaquetes", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreItem").value(itemPrincipal + "editado"));

        mockMvc.perform(put("/api/catalogos/gestion/items-inventario/{idItemInventario}/estado", idItem)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdministrador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("estado", "inactivo"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("inactivo"));

        String activos = mockMvc.perform(get("/api/catalogos/items-inventario")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdministrador)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(activos).doesNotContain(itemPrincipal + "editado");

        mockMvc.perform(get("/api/catalogos/gestion/items-inventario")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdministrador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.idItemInventario == '" + idItem + "')].estado").value("inactivo"));

        Integer auditorias = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM auditoria_operaciones
                WHERE tabla_afectada = 'items_inventario'
                  AND id_registro_afectado = ?
                """, Integer.class, idItem.toString());
        assertThat(auditorias).isEqualTo(3);
    }

    @Test
    void noInhabilitaConStockNiCambiaEstructuraDespuesDeMovimiento() throws Exception {
        String tokenAdministrador = iniciarSesion(usuarioAdministrador);
        UUID categoriaDesechables = idCategoria("desechables");
        UUID categoriaDulces = idCategoria("dulces");
        UUID unidadUnidad = idUnidad("unidad");
        UUID idItem = crearItemManual(tokenAdministrador, itemConStock, categoriaDesechables, unidadUnidad);
        UUID idUsuario = idUsuario(usuarioAdministrador);

        jdbcTemplate.update(
                "UPDATE existencias_inventario_general SET cantidad_actual = 3 WHERE id_item_inventario = ?",
                idItem);
        mockMvc.perform(put("/api/catalogos/gestion/items-inventario/{idItemInventario}/estado", idItem)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdministrador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("estado", "inactivo"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("No puedes inhabilitar un item mientras conserve stock general"));

        jdbcTemplate.update("UPDATE existencias_inventario_general SET cantidad_actual = 0 WHERE id_item_inventario = ?", idItem);
        jdbcTemplate.update("""
                INSERT INTO movimientos_inventario (
                    id_item_inventario,
                    tipo_stock,
                    tipo_movimiento,
                    cantidad,
                    sentido_movimiento,
                    referencia_origen,
                    id_referencia_origen,
                    id_usuario_registro
                )
                VALUES (?, 'general'::tipo_stock_enum, 'ajuste'::tipo_movimiento_inventario_enum, 1,
                    'entrada'::sentido_movimiento_enum, 'prueba_catalogos', gen_random_uuid(), ?)
                """, idItem, idUsuario);

        mockMvc.perform(put("/api/catalogos/gestion/items-inventario/{idItemInventario}", idItem)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdministrador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "idCategoriaInventario", categoriaDulces,
                                "idUnidadMedida", unidadUnidad,
                                "nombreItem", itemConStock,
                                "tipoControl", "manual_por_consumo",
                                "manejaPaquetes", false))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(
                        "No puedes cambiar categoria, unidad, control, tamano o paquetes despues de registrar movimientos"));
    }

    @Test
    void gerenteRegistraNuevaVigenciaDePrecioSinModificarLaAnterior() throws Exception {
        String tokenGerente = iniciarSesion(usuarioGerente);
        UUID idTipoGranizado = idTipoGranizado("con_licor");
        UUID idTamanoVaso = idTamanoVaso(8);
        LocalDate fechaNuevaVigencia = LocalDate.now().plusDays(2);

        String respuesta = mockMvc.perform(post("/api/catalogos/gestion/precios-granizado")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenGerente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "idTipoGranizado", idTipoGranizado,
                                "idTamanoVaso", idTamanoVaso,
                                "valorPrecio", new BigDecimal("13500.00"),
                                "fechaInicioVigencia", fechaNuevaVigencia))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.valorPrecio").value(13500.00))
                .andExpect(jsonPath("$.fechaInicioVigencia").value(fechaNuevaVigencia.toString()))
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID idNuevoPrecio = UUID.fromString(objectMapper.readValue(respuesta, Map.class).get("idPrecioGranizado").toString());

        LocalDate cierrePrecioAnterior = jdbcTemplate.queryForObject(
                "SELECT fecha_fin_vigencia FROM precios_granizado WHERE id_precio_granizado = ?",
                LocalDate.class,
                idPrecioBase);
        assertThat(cierrePrecioAnterior).isEqualTo(fechaNuevaVigencia.minusDays(1));

        mockMvc.perform(get("/api/catalogos/precios-granizado/vigentes")
                        .param("fecha", fechaNuevaVigencia.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenGerente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.idPrecioGranizado == '" + idNuevoPrecio + "')].valorPrecio").value(13500.00));

        Integer auditorias = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM auditoria_operaciones
                WHERE tabla_afectada = 'precios_granizado'
                  AND id_registro_afectado IN (?, ?)
                """, Integer.class, idPrecioBase.toString(), idNuevoPrecio.toString());
        assertThat(auditorias).isEqualTo(2);
    }

    @Test
    void vendedorNoPuedeGestionarCatalogos() throws Exception {
        String tokenVendedor = iniciarSesion(usuarioVendedor);

        mockMvc.perform(get("/api/catalogos/gestion/items-inventario")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenVendedor)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("Solo administrador o gerente puede gestionar catalogos"));
    }

    private UUID crearItemManual(String token, String nombreItem, UUID idCategoria, UUID idUnidad) throws Exception {
        String respuesta = mockMvc.perform(post("/api/catalogos/gestion/items-inventario")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "idCategoriaInventario", idCategoria,
                                "idUnidadMedida", idUnidad,
                                "nombreItem", nombreItem,
                                "tipoControl", "manual_por_consumo",
                                "manejaPaquetes", false))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return UUID.fromString(objectMapper.readValue(respuesta, Map.class).get("idItemInventario").toString());
    }

    private String iniciarSesion(String nombreUsuario) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nombreUsuario", nombreUsuario,
                                "contrasena", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readValue(response, Map.class).get("token").toString();
    }

    private String json(Map<String, Object> values) throws Exception {
        return objectMapper.writeValueAsString(values);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private UUID idCategoria(String nombreCategoria) {
        return jdbcTemplate.queryForObject(
                "SELECT id_categoria_inventario FROM categorias_inventario WHERE nombre_categoria = ?",
                UUID.class,
                nombreCategoria);
    }

    private UUID idUnidad(String nombreUnidad) {
        return jdbcTemplate.queryForObject(
                "SELECT id_unidad_medida FROM unidades_medida WHERE nombre_unidad = ?",
                UUID.class,
                nombreUnidad);
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

    private UUID idUsuario(String nombreUsuario) {
        return jdbcTemplate.queryForObject(
                "SELECT id_usuario FROM usuarios WHERE nombre_usuario = ?",
                UUID.class,
                nombreUsuario);
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

    private void limpiarDatosPrueba() {
        jdbcTemplate.update("""
                DELETE FROM auditoria_operaciones
                WHERE id_usuario IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE ?
                )
                """, PREFIJO_PRUEBAS);
        jdbcTemplate.update("""
                DELETE FROM movimientos_inventario
                WHERE id_item_inventario IN (
                    SELECT id_item_inventario FROM items_inventario WHERE nombre_item LIKE ?
                )
                """, PREFIJO_PRUEBAS);
        jdbcTemplate.update("""
                DELETE FROM existencias_inventario_general
                WHERE id_item_inventario IN (
                    SELECT id_item_inventario FROM items_inventario WHERE nombre_item LIKE ?
                )
                """, PREFIJO_PRUEBAS);
        jdbcTemplate.update("""
                DELETE FROM precios_granizado
                WHERE id_usuario_creacion IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE ?
                )
                """, PREFIJO_PRUEBAS);
        jdbcTemplate.update("DELETE FROM items_inventario WHERE nombre_item LIKE ?", PREFIJO_PRUEBAS);
        jdbcTemplate.update("""
                DELETE FROM sesiones_usuario
                WHERE id_usuario IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE ?
                )
                """, PREFIJO_PRUEBAS);
        jdbcTemplate.update("""
                DELETE FROM credenciales_usuario
                WHERE id_usuario IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE ?
                )
                """, PREFIJO_PRUEBAS);
        jdbcTemplate.update("DELETE FROM usuarios WHERE nombre_usuario LIKE ?", PREFIJO_PRUEBAS);
    }

    private void restaurarPrecioBaseSemilla() {
        jdbcTemplate.update("""
                UPDATE precios_granizado
                SET fecha_fin_vigencia = NULL
                WHERE id_precio_granizado = (
                    SELECT pg.id_precio_granizado
                    FROM precios_granizado pg
                    JOIN tipos_granizado tg ON tg.id_tipo_granizado = pg.id_tipo_granizado
                    JOIN tamanos_vaso tv ON tv.id_tamano_vaso = pg.id_tamano_vaso
                    WHERE tg.nombre_tipo = 'con_licor'
                      AND tv.onzas = 8
                    ORDER BY pg.fecha_inicio_vigencia, pg.id_precio_granizado
                    LIMIT 1
                )
                """);
    }
}
