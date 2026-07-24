package com.kontora.pos.evidencias.service;

import com.kontora.pos.auditoria.service.AuditoriaService;
import com.kontora.pos.caja.domain.GastoCaja;
import com.kontora.pos.caja.repository.GastoCajaRepository;
import com.kontora.pos.common.exception.ApiException;
import com.kontora.pos.common.security.PrincipalUsuario;
import com.kontora.pos.deposito.domain.ConsignacionBancaria;
import com.kontora.pos.deposito.domain.PagoServicio;
import com.kontora.pos.deposito.repository.ConsignacionBancariaRepository;
import com.kontora.pos.deposito.repository.PagoServicioRepository;
import com.kontora.pos.evidencias.domain.ArchivoEvidencia;
import com.kontora.pos.evidencias.dto.ArchivoEvidenciaDescargada;
import com.kontora.pos.evidencias.dto.ArchivoEvidenciaResponse;
import com.kontora.pos.evidencias.repository.ArchivoEvidenciaRepository;
import com.kontora.pos.evidencias.storage.ArchivoAlmacenado;
import com.kontora.pos.evidencias.storage.ArchivoDescargado;
import com.kontora.pos.evidencias.storage.EvidenciaStorageClient;
import com.kontora.pos.usuarios.domain.Usuario;
import com.kontora.pos.usuarios.repository.UsuarioRepository;
import com.kontora.pos.ventas.domain.PagoVenta;
import com.kontora.pos.ventas.repository.PagoVentaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import static com.kontora.pos.common.audit.AuditoriaValores.valores;

@Service
public class EvidenciasService {

    private static final String ESTADO_ACTIVO = "activo";
    private static final String ESTADO_GASTO_ANULADO = "anulado";
    private static final String METODO_TRANSFERENCIA = "transferencia";
    private static final String TIPO_IMAGEN = "imagen";
    private static final String TIPO_PDF = "pdf";
    private static final String TIPO_OTRO = "otro";
    private static final String FORMATO_JPG = "jpg";
    private static final String FORMATO_JPEG = "jpeg";
    private static final String FORMATO_PNG = "png";
    private static final String FORMATO_WEBP = "webp";
    private static final String FORMATO_PDF = "pdf";
    private static final String FORMATO_OTRO = "otro";
    private static final String CONTENT_TYPE_JPEG = "image/jpeg";
    private static final String CONTENT_TYPE_PDF = "application/pdf";
    private static final String CONTENT_TYPE_OCTET_STREAM = "application/octet-stream";
    private static final Set<String> FORMATOS_IMAGEN_COMPRESIBLES = Set.of(FORMATO_JPG, FORMATO_JPEG, FORMATO_PNG);

    private final ArchivoEvidenciaRepository archivoEvidenciaRepository;
    private final PagoVentaRepository pagoVentaRepository;
    private final GastoCajaRepository gastoCajaRepository;
    private final ConsignacionBancariaRepository consignacionBancariaRepository;
    private final PagoServicioRepository pagoServicioRepository;
    private final UsuarioRepository usuarioRepository;
    private final EvidenciaStorageClient storageClient;
    private final AuditoriaService auditoriaService;

    public EvidenciasService(
            ArchivoEvidenciaRepository archivoEvidenciaRepository,
            PagoVentaRepository pagoVentaRepository,
            GastoCajaRepository gastoCajaRepository,
            ConsignacionBancariaRepository consignacionBancariaRepository,
            PagoServicioRepository pagoServicioRepository,
            UsuarioRepository usuarioRepository,
            EvidenciaStorageClient storageClient,
            AuditoriaService auditoriaService) {
        this.archivoEvidenciaRepository = archivoEvidenciaRepository;
        this.pagoVentaRepository = pagoVentaRepository;
        this.gastoCajaRepository = gastoCajaRepository;
        this.consignacionBancariaRepository = consignacionBancariaRepository;
        this.pagoServicioRepository = pagoServicioRepository;
        this.usuarioRepository = usuarioRepository;
        this.storageClient = storageClient;
        this.auditoriaService = auditoriaService;
    }

    @Transactional
    public ArchivoEvidenciaResponse cargarEvidenciaPagoVenta(
            UUID idPagoVenta,
            MultipartFile archivo,
            PrincipalUsuario principalUsuario) {
        PagoVenta pagoVenta = obtenerPagoVenta(idPagoVenta);
        if (!METODO_TRANSFERENCIA.equals(pagoVenta.getMetodoPago().getNombreMetodo())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Solo se pueden cargar evidencias de pagos por transferencia");
        }
        validarAccesoPagoVenta(pagoVenta, principalUsuario);
        return guardarEvidencia(
                archivo,
                principalUsuario,
                "pagos-venta",
                idPagoVenta,
                evidencia -> evidencia.setPagoVenta(pagoVenta));
    }

    @Transactional
    public ArchivoEvidenciaResponse cargarAjusteEvidenciaPagoVenta(
            UUID idPagoVenta,
            MultipartFile archivo,
            PrincipalUsuario principalUsuario) {
        validarGerente(principalUsuario);
        PagoVenta pagoVenta = obtenerPagoVenta(idPagoVenta);
        if (!METODO_TRANSFERENCIA.equals(pagoVenta.getMetodoPago().getNombreMetodo())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Solo se pueden ajustar evidencias de pagos por transferencia");
        }

        // The prior support remains attached; the adjustment is an additional auditable record.
        ArchivoEvidenciaResponse evidencia = guardarEvidencia(
                archivo,
                principalUsuario,
                "pagos-venta",
                idPagoVenta,
                registro -> registro.setPagoVenta(pagoVenta));
        auditoriaService.registrar(
                principalUsuario.idUsuario(),
                "archivos_evidencia",
                evidencia.idArchivoEvidencia(),
                "crear",
                null,
                snapshotAjusteEvidencia(evidencia),
                "Ajuste de evidencia de transferencia por gerente");
        return evidencia;
    }

    @Transactional
    public ArchivoEvidenciaResponse cargarEvidenciaGastoCaja(
            UUID idGastoCaja,
            MultipartFile archivo,
            PrincipalUsuario principalUsuario) {
        GastoCaja gastoCaja = obtenerGastoCaja(idGastoCaja);
        if (ESTADO_GASTO_ANULADO.equals(gastoCaja.getEstadoGasto())) {
            throw new ApiException(HttpStatus.CONFLICT, "No se puede cargar evidencia para un gasto anulado");
        }
        validarAccesoGastoCaja(gastoCaja, principalUsuario);
        return guardarEvidencia(
                archivo,
                principalUsuario,
                "gastos-caja",
                idGastoCaja,
                evidencia -> evidencia.setGastoCaja(gastoCaja));
    }

    @Transactional
    public ArchivoEvidenciaResponse cargarEvidenciaConsignacionBancaria(
            UUID idConsignacionBancaria,
            MultipartFile archivo,
            PrincipalUsuario principalUsuario) {
        validarRolAdministrativo(principalUsuario, "Solo administrador o gerente puede gestionar evidencias de deposito");
        ConsignacionBancaria consignacion = obtenerConsignacionBancaria(idConsignacionBancaria);
        return guardarEvidencia(
                archivo,
                principalUsuario,
                "consignaciones-bancarias",
                idConsignacionBancaria,
                evidencia -> evidencia.setConsignacionBancaria(consignacion));
    }

    @Transactional
    public ArchivoEvidenciaResponse cargarEvidenciaPagoServicio(
            UUID idPagoServicio,
            MultipartFile archivo,
            PrincipalUsuario principalUsuario) {
        validarRolAdministrativo(principalUsuario, "Solo administrador o gerente puede gestionar evidencias de deposito");
        PagoServicio pagoServicio = obtenerPagoServicio(idPagoServicio);
        return guardarEvidencia(
                archivo,
                principalUsuario,
                "pagos-servicios",
                idPagoServicio,
                evidencia -> evidencia.setPagoServicio(pagoServicio));
    }

    @Transactional(readOnly = true)
    public ArchivoEvidenciaResponse obtenerEvidencia(UUID idArchivoEvidencia, PrincipalUsuario principalUsuario) {
        ArchivoEvidencia evidencia = archivoEvidenciaRepository.findByIdArchivoEvidencia(idArchivoEvidencia)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Evidencia no encontrada"));
        validarAccesoEvidencia(evidencia, principalUsuario);
        return toResponse(evidencia);
    }

    @Transactional(readOnly = true)
    public ArchivoEvidenciaDescargada descargarEvidencia(UUID idArchivoEvidencia, PrincipalUsuario principalUsuario) {
        ArchivoEvidencia evidencia = archivoEvidenciaRepository.findByIdArchivoEvidencia(idArchivoEvidencia)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Evidencia no encontrada"));
        validarAccesoDescarga(evidencia, principalUsuario);
        ArchivoDescargado archivo = storageClient.descargar(evidencia.getUrlArchivo());
        return new ArchivoEvidenciaDescargada(
                archivo.contenido(),
                archivo.contentType(),
                evidencia.getNombreArchivo());
    }

    @Transactional(readOnly = true)
    public java.util.List<ArchivoEvidenciaResponse> listarPorPagoVenta(UUID idPagoVenta, PrincipalUsuario principalUsuario) {
        PagoVenta pagoVenta = obtenerPagoVenta(idPagoVenta);
        validarAccesoPagoVenta(pagoVenta, principalUsuario);
        return archivoEvidenciaRepository.findByPagoVenta_IdPagoVentaOrderByFechaSubidaDesc(idPagoVenta)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public java.util.List<ArchivoEvidenciaResponse> listarPorGastoCaja(UUID idGastoCaja, PrincipalUsuario principalUsuario) {
        GastoCaja gastoCaja = obtenerGastoCaja(idGastoCaja);
        validarAccesoGastoCaja(gastoCaja, principalUsuario);
        return archivoEvidenciaRepository.findByGastoCaja_IdGastoCajaOrderByFechaSubidaDesc(idGastoCaja)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public java.util.List<ArchivoEvidenciaResponse> listarPorConsignacionBancaria(
            UUID idConsignacionBancaria,
            PrincipalUsuario principalUsuario) {
        validarRolAdministrativo(principalUsuario, "Solo administrador o gerente puede consultar evidencias de deposito");
        obtenerConsignacionBancaria(idConsignacionBancaria);
        return archivoEvidenciaRepository.findByConsignacionBancaria_IdConsignacionBancariaOrderByFechaSubidaDesc(idConsignacionBancaria)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public java.util.List<ArchivoEvidenciaResponse> listarPorPagoServicio(
            UUID idPagoServicio,
            PrincipalUsuario principalUsuario) {
        validarRolAdministrativo(principalUsuario, "Solo administrador o gerente puede consultar evidencias de deposito");
        obtenerPagoServicio(idPagoServicio);
        return archivoEvidenciaRepository.findByPagoServicio_IdPagoServicioOrderByFechaSubidaDesc(idPagoServicio)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private ArchivoEvidenciaResponse guardarEvidencia(
            MultipartFile archivo,
            PrincipalUsuario principalUsuario,
            String carpeta,
            UUID idProceso,
            Consumer<ArchivoEvidencia> asignarRelacion) {
        Usuario usuarioSubida = obtenerUsuario(principalUsuario.idUsuario());
        ArchivoProcesado archivoProcesado = procesarArchivo(archivo);
        String rutaArchivo = construirRutaArchivo(carpeta, idProceso, archivoProcesado.formatoArchivo());
        ArchivoAlmacenado archivoAlmacenado = storageClient.subir(
                rutaArchivo,
                archivoProcesado.contentType(),
                archivoProcesado.contenido());

        ArchivoEvidencia evidencia = new ArchivoEvidencia();
        asignarRelacion.accept(evidencia);
        evidencia.setUrlArchivo(archivoAlmacenado.urlArchivo());
        evidencia.setNombreArchivo(archivoProcesado.nombreArchivo());
        evidencia.setTipoArchivo(archivoProcesado.tipoArchivo());
        evidencia.setFormatoArchivo(archivoProcesado.formatoArchivo());
        evidencia.setTamanoOriginalKb(archivoProcesado.tamanoOriginalKb());
        evidencia.setTamanoComprimidoKb(archivoProcesado.tamanoComprimidoKb());
        evidencia.setFueComprimido(archivoProcesado.fueComprimido());
        evidencia.setFechaSubida(OffsetDateTime.now());
        evidencia.setUsuarioSubida(usuarioSubida);
        evidencia.setEstado(ESTADO_ACTIVO);

        return toResponse(archivoEvidenciaRepository.saveAndFlush(evidencia));
    }

    private ArchivoProcesado procesarArchivo(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El archivo de evidencia es obligatorio");
        }

        byte[] contenidoOriginal;
        try {
            contenidoOriginal = archivo.getBytes();
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No fue posible leer el archivo de evidencia");
        }

        String nombreOriginal = normalizarNombreArchivo(archivo.getOriginalFilename());
        String contentTypeOriginal = contentTypeSeguro(archivo.getContentType());
        String formato = formatoArchivo(nombreOriginal, contentTypeOriginal);
        int tamanoOriginalKb = calcularTamanoKb(contenidoOriginal.length);

        if (FORMATOS_IMAGEN_COMPRESIBLES.contains(formato)) {
            byte[] contenidoComprimido = comprimirImagen(contenidoOriginal);
            return new ArchivoProcesado(
                    reemplazarExtension(nombreOriginal, FORMATO_JPG),
                    TIPO_IMAGEN,
                    FORMATO_JPG,
                    CONTENT_TYPE_JPEG,
                    contenidoComprimido,
                    tamanoOriginalKb,
                    calcularTamanoKb(contenidoComprimido.length),
                    true);
        }

        if (FORMATO_WEBP.equals(formato) || contentTypeOriginal.startsWith("image/")) {
            return new ArchivoProcesado(
                    nombreOriginal,
                    TIPO_IMAGEN,
                    FORMATO_WEBP.equals(formato) ? FORMATO_WEBP : FORMATO_OTRO,
                    contentTypeOriginal,
                    contenidoOriginal,
                    tamanoOriginalKb,
                    null,
                    false);
        }

        if (FORMATO_PDF.equals(formato)) {
            return new ArchivoProcesado(
                    nombreOriginal,
                    TIPO_PDF,
                    FORMATO_PDF,
                    CONTENT_TYPE_PDF,
                    contenidoOriginal,
                    tamanoOriginalKb,
                    null,
                    false);
        }

        return new ArchivoProcesado(
                nombreOriginal,
                TIPO_OTRO,
                FORMATO_OTRO,
                contentTypeOriginal,
                contenidoOriginal,
                tamanoOriginalKb,
                null,
                false);
    }

    private byte[] comprimirImagen(byte[] contenidoOriginal) {
        BufferedImage imagenOriginal;
        try {
            imagenOriginal = ImageIO.read(new ByteArrayInputStream(contenidoOriginal));
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No fue posible procesar la imagen de evidencia");
        }
        if (imagenOriginal == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El archivo enviado no es una imagen valida");
        }

        BufferedImage imagenRgb = new BufferedImage(
                imagenOriginal.getWidth(),
                imagenOriginal.getHeight(),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = imagenRgb.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, imagenRgb.getWidth(), imagenRgb.getHeight());
        graphics.drawImage(imagenOriginal, 0, 0, null);
        graphics.dispose();

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(FORMATO_JPG);
        if (!writers.hasNext()) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "No hay compresor JPEG disponible");
        }

        ImageWriter writer = writers.next();
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             ImageOutputStream imageOutput = ImageIO.createImageOutputStream(output)) {
            ImageWriteParam params = writer.getDefaultWriteParam();
            if (params.canWriteCompressed()) {
                params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                params.setCompressionQuality(0.82f);
            }
            writer.setOutput(imageOutput);
            writer.write(null, new IIOImage(imagenRgb, null, null), params);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No fue posible comprimir la imagen de evidencia");
        } finally {
            writer.dispose();
        }
    }

    private PagoVenta obtenerPagoVenta(UUID idPagoVenta) {
        return pagoVentaRepository.findByIdPagoVenta(idPagoVenta)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Pago de venta no encontrado"));
    }

    private GastoCaja obtenerGastoCaja(UUID idGastoCaja) {
        return gastoCajaRepository.findByIdGastoCaja(idGastoCaja)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Gasto de caja no encontrado"));
    }

    private ConsignacionBancaria obtenerConsignacionBancaria(UUID idConsignacionBancaria) {
        return consignacionBancariaRepository.findByIdConsignacionBancaria(idConsignacionBancaria)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Consignacion bancaria no encontrada"));
    }

    private PagoServicio obtenerPagoServicio(UUID idPagoServicio) {
        return pagoServicioRepository.findByIdPagoServicio(idPagoServicio)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Pago de servicio no encontrado"));
    }

    private Usuario obtenerUsuario(UUID idUsuario) {
        return usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Usuario autenticado no encontrado"));
    }

    private void validarAccesoEvidencia(ArchivoEvidencia evidencia, PrincipalUsuario principalUsuario) {
        if (evidencia.getPagoVenta() != null) {
            validarAccesoPagoVenta(evidencia.getPagoVenta(), principalUsuario);
            return;
        }
        if (evidencia.getGastoCaja() != null) {
            validarAccesoGastoCaja(evidencia.getGastoCaja(), principalUsuario);
            return;
        }
        if (evidencia.getConsignacionBancaria() != null || evidencia.getPagoServicio() != null) {
            validarRolAdministrativo(principalUsuario, "Solo administrador o gerente puede consultar evidencias de deposito");
        }
    }

    private void validarAccesoDescarga(ArchivoEvidencia evidencia, PrincipalUsuario principalUsuario) {
        if (evidencia.getPagoVenta() != null) {
            validarRolAdministrativo(principalUsuario, "Solo administrador o gerente puede descargar evidencias de transferencias");
            return;
        }
        validarAccesoEvidencia(evidencia, principalUsuario);
    }

    private void validarAccesoPagoVenta(PagoVenta pagoVenta, PrincipalUsuario principalUsuario) {
        if (tieneRolAdministrativo(principalUsuario)) {
            return;
        }
        UUID idVendedor = pagoVenta.getVenta().getUsuarioVendedor().getIdUsuario();
        if (!idVendedor.equals(principalUsuario.idUsuario())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "No autorizado para acceder a evidencias de este pago");
        }
    }

    private void validarAccesoGastoCaja(GastoCaja gastoCaja, PrincipalUsuario principalUsuario) {
        if (tieneRolAdministrativo(principalUsuario)) {
            return;
        }
        UUID idUsuarioRegistro = gastoCaja.getUsuarioRegistro().getIdUsuario();
        if (!idUsuarioRegistro.equals(principalUsuario.idUsuario())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "No autorizado para acceder a evidencias de este gasto");
        }
    }

    private void validarRolAdministrativo(PrincipalUsuario principalUsuario, String mensaje) {
        if (!tieneRolAdministrativo(principalUsuario)) {
            throw new ApiException(HttpStatus.FORBIDDEN, mensaje);
        }
    }

    private void validarGerente(PrincipalUsuario principalUsuario) {
        if (!"gerente".equals(principalUsuario.nombreRol().toLowerCase(Locale.ROOT))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo el gerente puede adjuntar ajustes de evidencias de transferencias");
        }
    }

    private boolean tieneRolAdministrativo(PrincipalUsuario principalUsuario) {
        String rol = principalUsuario.nombreRol().toLowerCase(Locale.ROOT);
        return "administrador".equals(rol) || "gerente".equals(rol);
    }

    private Map<String, Object> snapshotAjusteEvidencia(ArchivoEvidenciaResponse evidencia) {
        return valores(
                "id_archivo_evidencia", evidencia.idArchivoEvidencia(),
                "id_pago_venta", evidencia.idPagoVenta(),
                "nombre_archivo", evidencia.nombreArchivo(),
                "tipo_archivo", evidencia.tipoArchivo(),
                "formato_archivo", evidencia.formatoArchivo(),
                "id_usuario_subida", evidencia.idUsuarioSubida());
    }

    private String construirRutaArchivo(String carpeta, UUID idProceso, String formatoArchivo) {
        return carpeta + "/" + idProceso + "/" + UUID.randomUUID() + "." + formatoArchivo;
    }

    private int calcularTamanoKb(int bytes) {
        return (bytes + 1023) / 1024;
    }

    private String normalizarNombreArchivo(String nombreArchivo) {
        String nombre = nombreArchivo == null || nombreArchivo.isBlank()
                ? "evidencia"
                : nombreArchivo.replace("\\", "/");
        int index = nombre.lastIndexOf('/');
        if (index >= 0) {
            nombre = nombre.substring(index + 1);
        }
        nombre = nombre.trim().replaceAll("[^A-Za-z0-9._-]", "_");
        if (nombre.isBlank() || ".".equals(nombre)) {
            return "evidencia";
        }
        return nombre;
    }

    private String contentTypeSeguro(String contentType) {
        return contentType == null || contentType.isBlank()
                ? CONTENT_TYPE_OCTET_STREAM
                : contentType.toLowerCase(Locale.ROOT);
    }

    private String formatoArchivo(String nombreArchivo, String contentType) {
        String extension = extension(nombreArchivo);
        if (FORMATO_JPG.equals(extension) || FORMATO_JPEG.equals(extension) || FORMATO_PNG.equals(extension)
                || FORMATO_WEBP.equals(extension) || FORMATO_PDF.equals(extension)) {
            return extension;
        }
        return switch (contentType) {
            case "image/jpeg" -> FORMATO_JPG;
            case "image/png" -> FORMATO_PNG;
            case "image/webp" -> FORMATO_WEBP;
            case CONTENT_TYPE_PDF -> FORMATO_PDF;
            default -> FORMATO_OTRO;
        };
    }

    private String extension(String nombreArchivo) {
        int index = nombreArchivo.lastIndexOf('.');
        if (index < 0 || index == nombreArchivo.length() - 1) {
            return FORMATO_OTRO;
        }
        return nombreArchivo.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private String reemplazarExtension(String nombreArchivo, String nuevaExtension) {
        int index = nombreArchivo.lastIndexOf('.');
        if (index < 0) {
            return nombreArchivo + "." + nuevaExtension;
        }
        return nombreArchivo.substring(0, index + 1) + nuevaExtension;
    }

    private ArchivoEvidenciaResponse toResponse(ArchivoEvidencia evidencia) {
        Usuario usuarioSubida = evidencia.getUsuarioSubida();
        return new ArchivoEvidenciaResponse(
                evidencia.getIdArchivoEvidencia(),
                evidencia.getPagoVenta() == null ? null : evidencia.getPagoVenta().getIdPagoVenta(),
                evidencia.getGastoCaja() == null ? null : evidencia.getGastoCaja().getIdGastoCaja(),
                evidencia.getConsignacionBancaria() == null
                        ? null
                        : evidencia.getConsignacionBancaria().getIdConsignacionBancaria(),
                evidencia.getPagoServicio() == null ? null : evidencia.getPagoServicio().getIdPagoServicio(),
                evidencia.getUrlArchivo(),
                evidencia.getNombreArchivo(),
                evidencia.getTipoArchivo(),
                evidencia.getFormatoArchivo(),
                evidencia.getTamanoOriginalKb(),
                evidencia.getTamanoComprimidoKb(),
                evidencia.isFueComprimido(),
                evidencia.getFechaSubida(),
                usuarioSubida.getIdUsuario(),
                usuarioSubida.getNombreUsuario(),
                evidencia.getEstado());
    }

    private record ArchivoProcesado(
            String nombreArchivo,
            String tipoArchivo,
            String formatoArchivo,
            String contentType,
            byte[] contenido,
            Integer tamanoOriginalKb,
            Integer tamanoComprimidoKb,
            boolean fueComprimido
    ) {
    }
}
