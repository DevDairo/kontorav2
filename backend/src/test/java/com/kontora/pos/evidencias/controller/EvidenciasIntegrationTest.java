package com.kontora.pos.evidencias.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kontora.pos.evidencias.storage.ArchivoAlmacenado;
import com.kontora.pos.evidencias.storage.ArchivoDescargado;
import com.kontora.pos.evidencias.storage.EvidenciaStorageClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class EvidenciasIntegrationTest {

    private static final String PASSWORD = "Clave12345";
    private static final String USUARIO_ADMIN = "test_evidencias_admin";
    private static final String USUARIO_GERENTE = "test_evidencias_gerente";
    private static final String USUARIO_VENDEDOR = "test_evidencias_vendedor";
    private static final String USUARIO_OTRO_VENDEDOR = "test_evidencias_otro_vendedor";
    private static final LocalDate FECHA_CAJA = LocalDate.of(2200, 5, 1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EvidenciaStorageClient storageClient;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private UUID idUsuarioAdmin;
    private UUID idUsuarioGerente;
    private UUID idUsuarioVendedor;
    private UUID idUsuarioOtroVendedor;
    private UUID idCajaDiaria;

    @BeforeEach
    void setUp() {
        limpiarDatosDePrueba();
        reset(storageClient);
        when(storageClient.subir(anyString(), anyString(), any(byte[].class)))
                .thenAnswer(invocation -> new ArchivoAlmacenado(
                        "supabase://test/" + invocation.getArgument(0, String.class)));
        when(storageClient.descargar(anyString()))
                .thenReturn(new ArchivoDescargado(
                        "evidencia descargada".getBytes(StandardCharsets.UTF_8),
                        MediaType.APPLICATION_PDF_VALUE));

        idUsuarioAdmin = crearUsuarioConCredencial(USUARIO_ADMIN, "Administrador Evidencias", "administrador");
        idUsuarioGerente = crearUsuarioConCredencial(USUARIO_GERENTE, "Gerente Evidencias", "gerente");
        idUsuarioVendedor = crearUsuarioConCredencial(USUARIO_VENDEDOR, "Vendedor Evidencias", "vendedor");
        idUsuarioOtroVendedor = crearUsuarioConCredencial(USUARIO_OTRO_VENDEDOR, "Otro Vendedor Evidencias", "vendedor");
    }

    @AfterEach
    void tearDown() {
        limpiarDatosDePrueba();
    }

    @Test
    void sinUsuarioAutenticadoNoPuedeConsultarEvidencias() throws Exception {
        mockMvc.perform(get("/api/evidencias/pagos-venta/{idPagoVenta}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cargaEvidenciaDeTransferenciaComprimeImagenYGuardaMetadata() throws Exception {
        crearCajaAbierta();
        UUID idPagoTransferencia = crearVentaConPago(idUsuarioVendedor, "transferencia", "pendiente", new BigDecimal("15000.00"));
        String tokenVendedor = iniciarSesion(USUARIO_VENDEDOR);

        String response = mockMvc.perform(multipart("/api/evidencias/pagos-venta/{idPagoVenta}", idPagoTransferencia)
                        .file(imagenPng("transferencia.png"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenVendedor)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idPagoVenta").value(idPagoTransferencia.toString()))
                .andExpect(jsonPath("$.tipoArchivo").value("imagen"))
                .andExpect(jsonPath("$.formatoArchivo").value("jpg"))
                .andExpect(jsonPath("$.nombreArchivo").value("transferencia.jpg"))
                .andExpect(jsonPath("$.fueComprimido").value(true))
                .andExpect(jsonPath("$.nombreUsuarioSubida").value(USUARIO_VENDEDOR))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID idArchivoEvidencia = UUID.fromString(
                objectMapper.readValue(response, Map.class).get("idArchivoEvidencia").toString());
        Integer registros = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM archivos_evidencia
                WHERE id_archivo_evidencia = ?
                AND id_pago_venta = ?
                AND id_gasto_caja IS NULL
                AND id_consignacion_bancaria IS NULL
                AND id_pago_servicio IS NULL
                AND tipo_archivo::text = 'imagen'
                AND formato_archivo::text = 'jpg'
                AND fue_comprimido = TRUE
                """, Integer.class, idArchivoEvidencia, idPagoTransferencia);
        assertThat(registros).isEqualTo(1);

        mockMvc.perform(get("/api/evidencias/{idArchivoEvidencia}", idArchivoEvidencia)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenVendedor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idArchivoEvidencia").value(idArchivoEvidencia.toString()));

        mockMvc.perform(get("/api/evidencias/pagos-venta/{idPagoVenta}", idPagoTransferencia)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenVendedor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idArchivoEvidencia").value(idArchivoEvidencia.toString()));

        verify(storageClient).subir(
                startsWith("pagos-venta/" + idPagoTransferencia + "/"),
                eq(MediaType.IMAGE_JPEG_VALUE),
                any(byte[].class));
    }

    @Test
    void gerenteAdjuntaAjusteDeTransferenciaYConservaSoportesAnteriores() throws Exception {
        crearCajaAbierta();
        UUID idPagoTransferencia = crearVentaConPago(idUsuarioVendedor, "transferencia", "pendiente", new BigDecimal("15000.00"));
        UUID idEvidenciaAnterior = insertarEvidenciaPagoVenta(idPagoTransferencia, idUsuarioVendedor);
        String tokenAdministrador = iniciarSesion(USUARIO_ADMIN);
        String tokenGerente = iniciarSesion(USUARIO_GERENTE);

        mockMvc.perform(multipart("/api/evidencias/pagos-venta/{idPagoVenta}/ajustes", idPagoTransferencia)
                        .file(pdf("soporte-corregido.pdf"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdministrador)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("Solo el gerente puede adjuntar ajustes de evidencias de transferencias"));

        mockMvc.perform(multipart("/api/evidencias/pagos-venta/{idPagoVenta}/ajustes", idPagoTransferencia)
                        .file(pdf("soporte-corregido.pdf"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenGerente)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idPagoVenta").value(idPagoTransferencia.toString()))
                .andExpect(jsonPath("$.nombreArchivo").value("soporte-corregido.pdf"))
                .andExpect(jsonPath("$.nombreUsuarioSubida").value(USUARIO_GERENTE));

        Integer evidencias = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM archivos_evidencia
                WHERE id_pago_venta = ?
                """, Integer.class, idPagoTransferencia);
        Integer evidenciaAnterior = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM archivos_evidencia
                WHERE id_archivo_evidencia = ?
                AND id_pago_venta = ?
                """, Integer.class, idEvidenciaAnterior, idPagoTransferencia);
        Integer auditorias = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM auditoria_operaciones
                WHERE id_usuario = ?
                AND tabla_afectada = 'archivos_evidencia'
                AND accion = 'crear'::accion_auditoria_enum
                AND descripcion = 'Ajuste de evidencia de transferencia por gerente'
                """, Integer.class, idUsuarioGerente);

        assertThat(evidencias).isEqualTo(2);
        assertThat(evidenciaAnterior).isEqualTo(1);
        assertThat(auditorias).isEqualTo(1);
        verify(storageClient, times(1)).subir(anyString(), eq(MediaType.APPLICATION_PDF_VALUE), any(byte[].class));
    }

    @Test
    void rechazaEvidenciaParaPagoEnEfectivo() throws Exception {
        crearCajaAbierta();
        UUID idPagoEfectivo = crearVentaConPago(idUsuarioVendedor, "efectivo", "no_aplica", new BigDecimal("8000.00"));
        String tokenVendedor = iniciarSesion(USUARIO_VENDEDOR);

        mockMvc.perform(multipart("/api/evidencias/pagos-venta/{idPagoVenta}", idPagoEfectivo)
                        .file(imagenPng("efectivo.png"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenVendedor)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Solo se pueden cargar evidencias de pagos por transferencia"));

        verify(storageClient, never()).subir(anyString(), anyString(), any(byte[].class));
    }

    @Test
    void vendedorNoPuedeConsultarEvidenciasDePagoDeOtroVendedor() throws Exception {
        crearCajaAbierta();
        UUID idPagoOtroVendedor = crearVentaConPago(idUsuarioOtroVendedor, "transferencia", "pendiente", new BigDecimal("12000.00"));
        insertarEvidenciaPagoVenta(idPagoOtroVendedor, idUsuarioOtroVendedor);
        String tokenVendedor = iniciarSesion(USUARIO_VENDEDOR);

        mockMvc.perform(get("/api/evidencias/pagos-venta/{idPagoVenta}", idPagoOtroVendedor)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenVendedor)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("No autorizado para acceder a evidencias de este pago"));
    }

    @Test
    void administradorDescargaEvidenciaTransferenciaYVendedorNo() throws Exception {
        crearCajaAbierta();
        UUID idPagoVendedor = crearVentaConPago(idUsuarioVendedor, "transferencia", "pendiente", new BigDecimal("12000.00"));
        UUID idArchivoEvidencia = insertarEvidenciaPagoVenta(idPagoVendedor, idUsuarioVendedor);
        String tokenVendedor = iniciarSesion(USUARIO_VENDEDOR);
        String tokenAdmin = iniciarSesion(USUARIO_ADMIN);

        mockMvc.perform(get("/api/evidencias/{idArchivoEvidencia}/descargar", idArchivoEvidencia)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenVendedor)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("Solo administrador o gerente puede descargar evidencias de transferencias"));

        mockMvc.perform(get("/api/evidencias/{idArchivoEvidencia}/descargar", idArchivoEvidencia)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes("evidencia descargada".getBytes(StandardCharsets.UTF_8)))
                .andExpect(header().exists(HttpHeaders.CONTENT_DISPOSITION));

        verify(storageClient, times(1)).descargar("supabase://test/manual.jpg");
    }

    @Test
    void administradorCargaEvidenciasDeGastoConsignacionYPagoServicio() throws Exception {
        crearCajaAbierta();
        UUID idGastoCaja = crearGastoCaja();
        UUID idConsignacionBancaria = crearConsignacionBancaria();
        UUID idPagoServicio = crearPagoServicio();
        String tokenAdmin = iniciarSesion(USUARIO_ADMIN);

        mockMvc.perform(multipart("/api/evidencias/gastos-caja/{idGastoCaja}", idGastoCaja)
                        .file(pdf("gasto.pdf"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idGastoCaja").value(idGastoCaja.toString()))
                .andExpect(jsonPath("$.tipoArchivo").value("pdf"))
                .andExpect(jsonPath("$.formatoArchivo").value("pdf"))
                .andExpect(jsonPath("$.fueComprimido").value(false));

        mockMvc.perform(multipart("/api/evidencias/consignaciones-bancarias/{idConsignacionBancaria}", idConsignacionBancaria)
                        .file(pdf("consignacion.pdf"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idConsignacionBancaria").value(idConsignacionBancaria.toString()));

        mockMvc.perform(multipart("/api/evidencias/pagos-servicios/{idPagoServicio}", idPagoServicio)
                        .file(pdf("servicio.pdf"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idPagoServicio").value(idPagoServicio.toString()));

        Integer registros = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM archivos_evidencia
                WHERE id_usuario_subida = ?
                AND (
                    id_gasto_caja = ?
                    OR id_consignacion_bancaria = ?
                    OR id_pago_servicio = ?
                )
                """, Integer.class, idUsuarioAdmin, idGastoCaja, idConsignacionBancaria, idPagoServicio);
        assertThat(registros).isEqualTo(3);
        verify(storageClient, times(3)).subir(anyString(), eq(MediaType.APPLICATION_PDF_VALUE), any(byte[].class));
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
                VALUES (?, 'abierta'::estado_caja_enum, 300000, ?, 'test_evidencias_caja')
                RETURNING id_caja_diaria
                """, UUID.class, FECHA_CAJA, idUsuarioAdmin);
    }

    private UUID crearVentaConPago(UUID idUsuarioVendedor, String nombreMetodo, String estadoValidacion, BigDecimal valorPago) {
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
        UUID idMetodoPago = idMetodoPago(nombreMetodo);
        if ("no_aplica".equals(estadoValidacion)) {
            return jdbcTemplate.queryForObject("""
                    INSERT INTO pagos_venta (
                        id_venta,
                        id_metodo_pago,
                        valor_pago,
                        valor_recibido_efectivo,
                        cambio_entregado,
                        estado_validacion
                    )
                    VALUES (?, ?, ?, ?, 0, 'no_aplica'::estado_validacion_transferencia_enum)
                    RETURNING id_pago_venta
                    """, UUID.class, idVenta, idMetodoPago, valorPago, valorPago);
        }

        return jdbcTemplate.queryForObject("""
                INSERT INTO pagos_venta (
                    id_venta,
                    id_metodo_pago,
                    valor_pago,
                    estado_validacion
                )
                VALUES (?, ?, ?, CAST(? AS estado_validacion_transferencia_enum))
                RETURNING id_pago_venta
                """, UUID.class, idVenta, idMetodoPago, valorPago, estadoValidacion);
    }

    private UUID crearGastoCaja() {
        return jdbcTemplate.queryForObject("""
                INSERT INTO gastos_caja (
                    id_caja_diaria,
                    valor_gasto,
                    descripcion,
                    estado_gasto,
                    id_usuario_registro
                )
                VALUES (?, 18000, 'Gasto evidencia test', 'registrado'::estado_gasto_enum, ?)
                RETURNING id_gasto_caja
                """, UUID.class, idCajaDiaria, idUsuarioVendedor);
    }

    private UUID crearConsignacionBancaria() {
        UUID idMovimientoDeposito = crearMovimientoDeposito("salida_consignacion", new BigDecimal("50000.00"));
        return jdbcTemplate.queryForObject("""
                INSERT INTO consignaciones_bancarias (
                    id_movimiento_deposito,
                    valor_consignado,
                    id_usuario_registro,
                    observacion,
                    estado
                )
                VALUES (?, 50000, ?, 'Consignacion evidencia test', 'registrado'::estado_registro_financiero_enum)
                RETURNING id_consignacion_bancaria
                """, UUID.class, idMovimientoDeposito, idUsuarioAdmin);
    }

    private UUID crearPagoServicio() {
        UUID idMovimientoDeposito = crearMovimientoDeposito("salida_pago_servicio", new BigDecimal("35000.00"));
        return jdbcTemplate.queryForObject("""
                INSERT INTO pagos_servicios (
                    id_movimiento_deposito,
                    id_tipo_servicio,
                    valor_pagado,
                    descripcion,
                    id_usuario_registro,
                    estado
                )
                VALUES (?, ?, 35000, 'Energia evidencia test', ?, 'registrado'::estado_registro_financiero_enum)
                RETURNING id_pago_servicio
                """, UUID.class, idMovimientoDeposito, idTipoServicio("energia"), idUsuarioAdmin);
    }

    private UUID crearMovimientoDeposito(String tipoMovimiento, BigDecimal valorMovimiento) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO movimientos_deposito (
                    tipo_movimiento_deposito,
                    valor_movimiento,
                    saldo_anterior,
                    saldo_posterior,
                    id_usuario_registro,
                    observacion
                )
                VALUES (CAST(? AS tipo_movimiento_deposito_enum), ?, 100000, 50000, ?, 'Movimiento evidencia test')
                RETURNING id_movimiento_deposito
                """, UUID.class, tipoMovimiento, valorMovimiento, idUsuarioAdmin);
    }

    private UUID insertarEvidenciaPagoVenta(UUID idPagoVenta, UUID idUsuarioSubida) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO archivos_evidencia (
                    id_pago_venta,
                    url_archivo,
                    nombre_archivo,
                    tipo_archivo,
                    formato_archivo,
                    tamano_original_kb,
                    tamano_comprimido_kb,
                    fue_comprimido,
                    id_usuario_subida,
                    estado
                )
                VALUES (
                    ?,
                    'supabase://test/manual.jpg',
                    'manual.jpg',
                    'imagen'::tipo_archivo_enum,
                    'jpg'::formato_archivo_enum,
                    1,
                    1,
                    TRUE,
                    ?,
                    'activo'::estado_basico_enum
                )
                RETURNING id_archivo_evidencia
                """, UUID.class, idPagoVenta, idUsuarioSubida);
    }

    private UUID idMetodoPago(String nombreMetodo) {
        return jdbcTemplate.queryForObject(
                "SELECT id_metodo_pago FROM metodos_pago WHERE nombre_metodo = ?",
                UUID.class,
                nombreMetodo);
    }

    private UUID idTipoServicio(String nombreServicio) {
        return jdbcTemplate.queryForObject(
                "SELECT id_tipo_servicio FROM tipos_servicio WHERE nombre_servicio = ?",
                UUID.class,
                nombreServicio);
    }

    private String iniciarSesion(String nombreUsuario) throws Exception {
        String loginResponse = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/login")
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

    private MockMultipartFile imagenPng(String nombreArchivo) throws Exception {
        BufferedImage image = new BufferedImage(12, 12, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.BLUE);
        graphics.fillRect(0, 0, 12, 12);
        graphics.setColor(Color.WHITE);
        graphics.fillOval(3, 3, 6, 6);
        graphics.dispose();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return new MockMultipartFile("archivo", nombreArchivo, MediaType.IMAGE_PNG_VALUE, output.toByteArray());
    }

    private MockMultipartFile pdf(String nombreArchivo) {
        return new MockMultipartFile(
                "archivo",
                nombreArchivo,
                MediaType.APPLICATION_PDF_VALUE,
                "%PDF-1.4\n% evidencia test\n".getBytes(StandardCharsets.UTF_8));
    }

    private void limpiarDatosDePrueba() {
        jdbcTemplate.update("""
                DELETE FROM archivos_evidencia
                WHERE id_usuario_subida IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_evidencias_%'
                )
                OR url_archivo LIKE 'supabase://test/%'
                """);
        jdbcTemplate.update("""
                DELETE FROM consignaciones_bancarias
                WHERE id_usuario_registro IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_evidencias_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM pagos_servicios
                WHERE id_usuario_registro IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_evidencias_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM movimientos_deposito
                WHERE id_usuario_registro IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_evidencias_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM ventas
                WHERE id_caja_diaria IN (
                    SELECT id_caja_diaria
                    FROM cajas_diarias
                    WHERE fecha_operacion >= DATE '2099-01-01'
                    OR observaciones LIKE 'test_evidencias_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM gastos_caja
                WHERE id_caja_diaria IN (
                    SELECT id_caja_diaria
                    FROM cajas_diarias
                    WHERE fecha_operacion >= DATE '2099-01-01'
                    OR observaciones LIKE 'test_evidencias_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM cajas_diarias
                WHERE fecha_operacion >= DATE '2099-01-01'
                OR observaciones LIKE 'test_evidencias_%'
                """);
        jdbcTemplate.update("""
                DELETE FROM auditoria_operaciones
                WHERE id_usuario IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_evidencias_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM sesiones_usuario
                WHERE id_usuario IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_evidencias_%'
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM credenciales_usuario
                WHERE id_usuario IN (
                    SELECT id_usuario FROM usuarios WHERE nombre_usuario LIKE 'test_evidencias_%'
                )
                """);
        jdbcTemplate.update("DELETE FROM usuarios WHERE nombre_usuario LIKE 'test_evidencias_%'");
    }
}
