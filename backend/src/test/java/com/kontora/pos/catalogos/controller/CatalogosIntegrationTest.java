package com.kontora.pos.catalogos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
class CatalogosIntegrationTest {

    private static final String PASSWORD = "Clave12345";
    private static final String USUARIO_ADMIN = "test_catalogos_admin";
    private static final String ITEM_INACTIVO = "test_catalogo_item_inactivo";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        limpiarDatosDePrueba();
        crearUsuarioConCredencial(USUARIO_ADMIN, "Administrador Catalogos", "administrador");
        crearItemInactivo();
    }

    @Test
    void sinUsuarioAutenticadoNoConsultaCatalogosInternos() throws Exception {
        mockMvc.perform(get("/api/catalogos/tipos-granizado"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void consultaCatalogosBaseActivos() throws Exception {
        String token = iniciarSesion();

        mockMvc.perform(get("/api/catalogos/roles")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nombre", hasItem("administrador")))
                .andExpect(jsonPath("$[*].nombre", hasItem("gerente")))
                .andExpect(jsonPath("$[*].nombre", hasItem("vendedor")));

        mockMvc.perform(get("/api/catalogos/metodos-pago")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nombre", hasItem("efectivo")))
                .andExpect(jsonPath("$[*].nombre", hasItem("transferencia")));

        mockMvc.perform(get("/api/catalogos/tipos-granizado")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nombre", hasItem("con_licor")))
                .andExpect(jsonPath("$[*].nombre", hasItem("sin_licor")));

        mockMvc.perform(get("/api/catalogos/tamanos-vaso")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].onzas", hasItem(8)))
                .andExpect(jsonPath("$[*].onzas", hasItem(32)));

        mockMvc.perform(get("/api/catalogos/tipos-servicio")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nombre", hasItem("arriendo")))
                .andExpect(jsonPath("$[*].nombre", hasItem("internet")));
    }

    @Test
    void consultaPreciosYPromocionesVigentes() throws Exception {
        String token = iniciarSesion();

        mockMvc.perform(get("/api/catalogos/precios-granizado/vigentes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombreTipo").value("con_licor"))
                .andExpect(jsonPath("$[0].onzas").value(8))
                .andExpect(jsonPath("$[0].valorPrecio").value(8000.00));

        String promociones = mockMvc.perform(get("/api/catalogos/promociones/vigentes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value("activo"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(promociones).contains("promocion_2x_8oz_cliente");
        assertThat(promociones).contains("martes");
        assertThat(promociones).contains("miercoles");
    }

    @Test
    void itemsInactivosNoAparecenEnOperacionesNormales() throws Exception {
        String token = iniciarSesion();

        String items = mockMvc.perform(get("/api/catalogos/items-inventario")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nombreItem", hasItem("vaso_8oz")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(items).doesNotContain(ITEM_INACTIVO);
    }

    private String iniciarSesion() throws Exception {
        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nombreUsuario", USUARIO_ADMIN,
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

    private void crearItemInactivo() {
        jdbcTemplate.update("""
                INSERT INTO items_inventario (
                    id_categoria_inventario,
                    id_unidad_medida,
                    nombre_item,
                    tipo_control,
                    maneja_paquetes,
                    unidades_por_paquete,
                    estado
                )
                SELECT
                    ci.id_categoria_inventario,
                    um.id_unidad_medida,
                    ?,
                    'manual_por_consumo'::tipo_control_inventario_enum,
                    false,
                    null,
                    'inactivo'::estado_basico_enum
                FROM categorias_inventario ci
                JOIN unidades_medida um ON um.nombre_unidad = 'unidad'
                WHERE ci.nombre_categoria = 'desechables'
                ON CONFLICT (nombre_item) DO NOTHING
                """, ITEM_INACTIVO);
    }

    private void limpiarDatosDePrueba() {
        jdbcTemplate.update("DELETE FROM items_inventario WHERE nombre_item = ?", ITEM_INACTIVO);
        jdbcTemplate.update("""
                DELETE FROM auditoria_operaciones
                WHERE id_usuario IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_catalogos_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM sesiones_usuario
                WHERE id_usuario IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_catalogos_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM credenciales_usuario
                WHERE id_usuario IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_catalogos_%'
                )
                """);
        jdbcTemplate.update("DELETE FROM usuarios WHERE nombre_usuario LIKE 'test_catalogos_%'");
    }
}
