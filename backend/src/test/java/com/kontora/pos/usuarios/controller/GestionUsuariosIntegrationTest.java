package com.kontora.pos.usuarios.controller;

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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class GestionUsuariosIntegrationTest {

    private static final String PASSWORD = "Clave12345";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final String sufijoPrueba = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String prefijoUsuario = "usuariosit" + sufijoPrueba;
    private final String usuarioGerente = prefijoUsuario + "gerente";
    private final String usuarioAdministrador = prefijoUsuario + "admin";
    private final String usuarioVendedor = prefijoUsuario + "vendedor";
    private final String usuarioCreado = prefijoUsuario + "nuevo";

    @BeforeEach
    void setUp() {
        limpiarUsuariosDePrueba();
        crearUsuarioConCredencial(usuarioGerente, "Gerente de usuarios", "gerente", "activo");
        crearUsuarioConCredencial(usuarioAdministrador, "Administrador de usuarios", "administrador", "activo");
        crearUsuarioConCredencial(usuarioVendedor, "Vendedor de usuarios", "vendedor", "activo");
    }

    @AfterEach
    void tearDown() {
        limpiarUsuariosDePrueba();
    }

    @Test
    void gerenteCreaEditaYBloqueaUsuarioSinEliminarHistorial() throws Exception {
        String tokenGerente = iniciarSesion(usuarioGerente);
        String respuestaCreacion = mockMvc.perform(post("/api/usuarios")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenGerente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nombreCompleto", "Usuario Nuevo",
                                "nombreUsuario", usuarioCreado,
                                "nombreRol", "vendedor",
                                "contrasena", "ClaveNueva123"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombreUsuario").value(usuarioCreado))
                .andExpect(jsonPath("$.nombreRol").value("vendedor"))
                .andExpect(jsonPath("$.estado").value("activo"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID idUsuarioCreado = UUID.fromString(objectMapper.readValue(respuestaCreacion, Map.class).get("idUsuario").toString());

        String hash = jdbcTemplate.queryForObject("""
                SELECT cu.contrasena_hash
                FROM credenciales_usuario cu
                WHERE cu.id_usuario = ?
                """, String.class, idUsuarioCreado);
        assertThat(passwordEncoder.matches("ClaveNueva123", hash)).isTrue();

        String tokenUsuarioCreado = iniciarSesion(usuarioCreado, "ClaveNueva123");

        mockMvc.perform(put("/api/usuarios/{idUsuario}", idUsuarioCreado)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenGerente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nombreCompleto", "Usuario Editado",
                                "nombreUsuario", usuarioCreado,
                                "nombreRol", "administrador"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreCompleto").value("Usuario Editado"))
                .andExpect(jsonPath("$.nombreRol").value("administrador"));

        mockMvc.perform(put("/api/usuarios/{idUsuario}/estado", idUsuarioCreado)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenGerente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("estado", "bloqueado"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("bloqueado"));

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenUsuarioCreado)))
                .andExpect(status().isUnauthorized());

        Integer eventosUsuario = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM auditoria_operaciones
                WHERE tabla_afectada = 'usuarios'
                  AND id_registro_afectado = ?
                """, Integer.class, idUsuarioCreado.toString());
        assertThat(eventosUsuario).isEqualTo(3);
    }

    @Test
    void administradorYVendedorNoPuedenGestionarUsuarios() throws Exception {
        String tokenAdministrador = iniciarSesion(usuarioAdministrador);
        String tokenVendedor = iniciarSesion(usuarioVendedor);
        UUID idUsuarioGerente = idUsuario(usuarioGerente);

        mockMvc.perform(get("/api/usuarios")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdministrador)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("Solo el gerente puede gestionar usuarios"));

        mockMvc.perform(put("/api/usuarios/{idUsuario}/contrasena", idUsuarioGerente)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdministrador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nuevaContrasena", "ClaveNueva123"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("Solo el gerente puede gestionar usuarios"));

        mockMvc.perform(post("/api/usuarios")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenVendedor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nombreCompleto", "Usuario No Permitido",
                                "nombreUsuario", prefijoUsuario + "nopermitido",
                                "nombreRol", "vendedor",
                                "contrasena", "ClaveNueva123"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("Solo el gerente puede gestionar usuarios"));
    }

    @Test
    void gerenteConsultaUsuariosYRolesActivos() throws Exception {
        String tokenGerente = iniciarSesion(usuarioGerente);

        mockMvc.perform(get("/api/usuarios")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenGerente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.nombreUsuario == '" + usuarioGerente + "')]").exists());

        mockMvc.perform(get("/api/usuarios/roles")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenGerente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.nombreRol == 'gerente')]").exists());
    }

    @Test
    void gerenteRestableceContrasenaYRevocaSesionesActivasDelUsuario() throws Exception {
        crearUsuarioConCredencial(usuarioCreado, "Usuario para clave", "vendedor", "activo");
        UUID idUsuarioCreado = idUsuario(usuarioCreado);
        String tokenGerente = iniciarSesion(usuarioGerente);
        String tokenUsuario = iniciarSesion(usuarioCreado);
        jdbcTemplate.update("""
                UPDATE credenciales_usuario
                SET intentos_fallidos = 2
                WHERE id_usuario = ?
                """, idUsuarioCreado);

        mockMvc.perform(put("/api/usuarios/{idUsuario}/contrasena", idUsuarioCreado)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenGerente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nuevaContrasena", "ClaveReemplazo123"))))
                .andExpect(status().isNoContent());

        Map<String, Object> credencial = jdbcTemplate.queryForMap("""
                SELECT contrasena_hash, intentos_fallidos, fecha_cambio_contrasena
                FROM credenciales_usuario
                WHERE id_usuario = ?
                """, idUsuarioCreado);
        assertThat(passwordEncoder.matches("ClaveReemplazo123", (String) credencial.get("contrasena_hash"))).isTrue();
        assertThat((Integer) credencial.get("intentos_fallidos")).isZero();
        assertThat(credencial.get("fecha_cambio_contrasena")).isNotNull();

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenUsuario)))
                .andExpect(status().isUnauthorized());
        iniciarSesion(usuarioCreado, "ClaveReemplazo123");

        Integer sesionesRevocadas = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM sesiones_usuario
                WHERE id_usuario = ?
                  AND estado_sesion = 'revocada'::estado_sesion_enum
                """, Integer.class, idUsuarioCreado);
        assertThat(sesionesRevocadas).isEqualTo(1);

        String valorNuevoAuditoria = jdbcTemplate.queryForObject("""
                SELECT valor_nuevo::text
                FROM auditoria_operaciones
                WHERE tabla_afectada = 'credenciales_usuario'
                ORDER BY fecha_accion DESC
                LIMIT 1
                """, String.class);
        assertThat(valorNuevoAuditoria).doesNotContain("ClaveReemplazo123");
    }

    private String iniciarSesion(String nombreUsuario) throws Exception {
        return iniciarSesion(nombreUsuario, PASSWORD);
    }

    private String iniciarSesion(String nombreUsuario, String contrasena) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nombreUsuario", nombreUsuario,
                                "contrasena", contrasena))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readValue(response, Map.class).get("token").toString();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private UUID idUsuario(String nombreUsuario) {
        return jdbcTemplate.queryForObject(
                "SELECT id_usuario FROM usuarios WHERE nombre_usuario = ?",
                UUID.class,
                nombreUsuario);
    }

    private void crearUsuarioConCredencial(String nombreUsuario, String nombreCompleto, String nombreRol, String estadoUsuario) {
        UUID idRol = jdbcTemplate.queryForObject(
                "SELECT id_rol FROM roles WHERE nombre_rol = ?",
                UUID.class,
                nombreRol);
        UUID idUsuario = jdbcTemplate.queryForObject("""
                INSERT INTO usuarios (id_rol, nombre_usuario, nombre_completo, estado)
                VALUES (?, ?, ?, ?::estado_usuario_enum)
                RETURNING id_usuario
                """, UUID.class, idRol, nombreUsuario, nombreCompleto, estadoUsuario);
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

    private void limpiarUsuariosDePrueba() {
        List<String> usuarios = List.of(usuarioGerente, usuarioAdministrador, usuarioVendedor, usuarioCreado);
        jdbcTemplate.update("""
                DELETE FROM auditoria_operaciones
                WHERE id_usuario IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario IN (?, ?, ?, ?)
                )
                """, usuarios.toArray());
        jdbcTemplate.update("""
                DELETE FROM sesiones_usuario
                WHERE id_usuario IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario IN (?, ?, ?, ?)
                )
                """, usuarios.toArray());
        jdbcTemplate.update("""
                DELETE FROM credenciales_usuario
                WHERE id_usuario IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario IN (?, ?, ?, ?)
                )
                """, usuarios.toArray());
        jdbcTemplate.update(
                "DELETE FROM usuarios WHERE nombre_usuario IN (?, ?, ?, ?)",
                usuarios.toArray());
    }
}
