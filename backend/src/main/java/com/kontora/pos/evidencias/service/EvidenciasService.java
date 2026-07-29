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
import com.kontora.pos.inventario.domain.PerdidaInventario;
import com.kontora.pos.inventario.repository.PerdidaInventarioRepository;
import com.kontora.pos.usuarios.domain.Usuario;
import com.kontora.pos.usuarios.repository.UsuarioRepository;
import com.kontora.pos.ventas.domain.PagoVenta;
import com.kontora.pos.ventas.repository.PagoVentaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
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
import java.nio.charset.StandardCharsets;
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
    private static final String ESTADO_PERDIDA_REGISTRADA = "registrada";
    private static final String METODO_TRANSFERENCIA = "transferencia";
    private static final String TIPO_IMAGEN = "imagen";
    private static final String TIPO_PDF = "pdf";
    private static final String TIPO_OTRO = "otro";
    private static final String FORMATO_JPG = "jpg";
    private static final String FORMATO_JPEG = "jpeg";
    private static final String FORMATO_PNG = "png";
    private static final String FORMATO_WEBP = "webp";
    private static final String FORMATO_PDF = "pdf";
    private static final String FORMATO_HEIC = "heic";
    private static final String FORMATO_HEIF = "heif";
    private static final String FORMATO_AVIF = "avif";
    private static final String FORMATO_GIF = "gif";
    private static final String FORMATO_BMP = "bmp";
    private static final String FORMATO_TIFF = "tiff";
    private static final String FORMATO_OTRO = "otro";
    private static final String CONTENT_TYPE_JPEG = "image/jpeg";
    private static final String CONTENT_TYPE_PNG = "image/png";
    private static final String CONTENT_TYPE_WEBP = "image/webp";
    private static final String CONTENT_TYPE_HEIC = "image/heic";
    private static final String CONTENT_TYPE_HEIF = "image/heif";
    private static final String CONTENT_TYPE_AVIF = "image/avif";
    private static final String CONTENT_TYPE_GIF = "image/gif";
    private static final String CONTENT_TYPE_BMP = "image/bmp";
    private static final String CONTENT_TYPE_TIFF = "image/tiff";
    private static final String CONTENT_TYPE_PDF = "application/pdf";
    private static final String CONTENT_TYPE_OCTET_STREAM = "application/octet-stream";
    private static final Set<String> FORMATOS_IMAGEN_COMPRESIBLES = Set.of(FORMATO_JPG, FORMATO_JPEG, FORMATO_PNG);
    private static final Set<String> FORMATOS_IMAGEN_MOVIL = Set.of(
            FORMATO_JPG,
            FORMATO_JPEG,
            FORMATO_PNG,
            FORMATO_WEBP,
            FORMATO_HEIC,
            FORMATO_HEIF,
            FORMATO_AVIF,
            FORMATO_GIF,
            FORMATO_BMP,
            FORMATO_TIFF);
    private static final Set<String> MARCAS_HEIC = Set.of(
            "heic", "heix", "hevc", "hevx", "heim", "heis", "hevm", "hevs");
    private static final Set<String> MARCAS_AVIF = Set.of("avif", "avis");

    private final ArchivoEvidenciaRepository archivoEvidenciaRepository;
    private final PagoVentaRepository pagoVentaRepository;
    private final GastoCajaRepository gastoCajaRepository;
    private final ConsignacionBancariaRepository consignacionBancariaRepository;
    private final PagoServicioRepository pagoServicioRepository;
    private final PerdidaInventarioRepository perdidaInventarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final EvidenciaStorageClient storageClient;
    private final AuditoriaService auditoriaService;

    public EvidenciasService(
            ArchivoEvidenciaRepository archivoEvidenciaRepository,
            PagoVentaRepository pagoVentaRepository,
            GastoCajaRepository gastoCajaRepository,
            ConsignacionBancariaRepository consignacionBancariaRepository,
            PagoServicioRepository pagoServicioRepository,
            PerdidaInventarioRepository perdidaInventarioRepository,
            UsuarioRepository usuarioRepository,
            EvidenciaStorageClient storageClient,
            AuditoriaService auditoriaService) {
        this.archivoEvidenciaRepository = archivoEvidenciaRepository;
        this.pagoVentaRepository = pagoVentaRepository;
        this.gastoCajaRepository = gastoCajaRepository;
        this.consignacionBancariaRepository = consignacionBancariaRepository;
        this.pagoServicioRepository = pagoServicioRepository;
        this.perdidaInventarioRepository = perdidaInventarioRepository;
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

    @Transactional
    public ArchivoEvidenciaResponse cargarEvidenciaPerdidaInventario(
            UUID idPerdidaInventario,
            MultipartFile archivo,
            PrincipalUsuario principalUsuario) {
        validarRolAdministrativo(
                principalUsuario,
                "Solo administrador o gerente puede gestionar evidencias de perdidas");
        PerdidaInventario perdida = obtenerPerdidaInventario(idPerdidaInventario);
        if (!ESTADO_PERDIDA_REGISTRADA.equals(perdida.getEstado())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "No se pueden agregar evidencias a una perdida anulada");
        }
        return guardarEvidenciaPerdida(perdida, archivo, principalUsuario);
    }

    @Transactional
    public ArchivoEvidenciaResponse guardarEvidenciaPerdida(
            PerdidaInventario perdida,
            MultipartFile archivo,
            PrincipalUsuario principalUsuario) {
        validarRolAdministrativo(
                principalUsuario,
                "Solo administrador o gerente puede gestionar evidencias de perdidas");
        ArchivoEvidenciaResponse evidencia = guardarEvidencia(
                archivo,
                principalUsuario,
                "perdidas-inventario",
                perdida.getIdPerdidaInventario(),
                registro -> registro.setPerdidaInventario(perdida),
                true);
        auditoriaService.registrar(
                principalUsuario.idUsuario(),
                "archivos_evidencia",
                evidencia.idArchivoEvidencia(),
                "crear",
                null,
                snapshotEvidenciaPerdida(evidencia),
                "Evidencia fotografica de perdida de vasos");
        return evidencia;
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

    @Transactional(readOnly = true)
    public java.util.List<ArchivoEvidenciaResponse> listarPorPerdidaInventario(
            UUID idPerdidaInventario,
            PrincipalUsuario principalUsuario) {
        validarRolAdministrativo(
                principalUsuario,
                "Solo administrador o gerente puede consultar evidencias de perdidas");
        obtenerPerdidaInventario(idPerdidaInventario);
        return archivoEvidenciaRepository
                .findByPerdidaInventario_IdPerdidaInventarioOrderByFechaSubidaDesc(
                        idPerdidaInventario)
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
        return guardarEvidencia(
                archivo,
                principalUsuario,
                carpeta,
                idProceso,
                asignarRelacion,
                false);
    }

    private ArchivoEvidenciaResponse guardarEvidencia(
            MultipartFile archivo,
            PrincipalUsuario principalUsuario,
            String carpeta,
            UUID idProceso,
            Consumer<ArchivoEvidencia> asignarRelacion,
            boolean requiereImagen) {
        Usuario usuarioSubida = obtenerUsuario(principalUsuario.idUsuario());
        ArchivoProcesado archivoProcesado = procesarArchivo(archivo);
        if (requiereImagen && !TIPO_IMAGEN.equals(archivoProcesado.tipoArchivo())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "La evidencia de una perdida debe ser una imagen");
        }
        String rutaArchivo = construirRutaArchivo(carpeta, idProceso, archivoProcesado.extensionAlmacenamiento());
        ArchivoAlmacenado archivoAlmacenado = storageClient.subir(
                rutaArchivo,
                archivoProcesado.contentType(),
                archivoProcesado.contenido());
        boolean compensacionRegistrada = registrarEliminacionSiHayRollback(
                archivoAlmacenado.urlArchivo());

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

        try {
            return toResponse(archivoEvidenciaRepository.saveAndFlush(evidencia));
        } catch (RuntimeException exception) {
            if (!compensacionRegistrada) {
                eliminarCompensatoriamente(archivoAlmacenado.urlArchivo());
            }
            throw exception;
        }
    }

    private boolean registrarEliminacionSiHayRollback(String urlArchivo) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            return false;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                            eliminarCompensatoriamente(urlArchivo);
                        }
                    }
                });
        return true;
    }

    private void eliminarCompensatoriamente(String urlArchivo) {
        try {
            storageClient.eliminar(urlArchivo);
        } catch (RuntimeException ignored) {
            // La operacion original debe conservar su error; el objeto huerfano puede depurarse operativamente.
        }
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
        String formato = detectarFormatoArchivo(nombreOriginal, contentTypeOriginal, contenidoOriginal);
        int tamanoOriginalKb = calcularTamanoKb(contenidoOriginal.length);

        if (FORMATOS_IMAGEN_COMPRESIBLES.contains(formato)) {
            byte[] contenidoComprimido = comprimirImagen(contenidoOriginal);
            return new ArchivoProcesado(
                    reemplazarExtension(nombreOriginal, FORMATO_JPG),
                    TIPO_IMAGEN,
                    FORMATO_JPG,
                    FORMATO_JPG,
                    CONTENT_TYPE_JPEG,
                    contenidoComprimido,
                    tamanoOriginalKb,
                    calcularTamanoKb(contenidoComprimido.length),
                    true);
        }

        if (FORMATOS_IMAGEN_MOVIL.contains(formato) || contentTypeOriginal.startsWith("image/")) {
            return new ArchivoProcesado(
                    nombreOriginal,
                    TIPO_IMAGEN,
                    FORMATO_WEBP.equals(formato) ? FORMATO_WEBP : FORMATO_OTRO,
                    extensionAlmacenamiento(formato, nombreOriginal),
                    contentTypeImagenSeguro(formato, contentTypeOriginal),
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
                extensionAlmacenamiento(formato, nombreOriginal),
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

        BufferedImage imagenOrientada = OrientacionImagenExif.normalizar(imagenOriginal, contenidoOriginal);
        BufferedImage imagenRgb = new BufferedImage(
                imagenOrientada.getWidth(),
                imagenOrientada.getHeight(),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = imagenRgb.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, imagenRgb.getWidth(), imagenRgb.getHeight());
        graphics.drawImage(imagenOrientada, 0, 0, null);
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

    private PerdidaInventario obtenerPerdidaInventario(UUID idPerdidaInventario) {
        return perdidaInventarioRepository
                .findByIdPerdidaInventario(idPerdidaInventario)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "Perdida de inventario no encontrada"));
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
            return;
        }
        if (evidencia.getPerdidaInventario() != null) {
            validarRolAdministrativo(
                    principalUsuario,
                    "Solo administrador o gerente puede consultar evidencias de perdidas");
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

    private Map<String, Object> snapshotEvidenciaPerdida(ArchivoEvidenciaResponse evidencia) {
        return valores(
                "id_archivo_evidencia", evidencia.idArchivoEvidencia(),
                "id_perdida_inventario", evidencia.idPerdidaInventario(),
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

    private String detectarFormatoArchivo(String nombreArchivo, String contentType, byte[] contenido) {
        String formatoPorFirma = detectarFormatoPorFirma(contenido);
        if (!FORMATO_OTRO.equals(formatoPorFirma)) {
            return formatoPorFirma;
        }

        String extension = extension(nombreArchivo);
        if (FORMATO_JPG.equals(extension) || FORMATO_JPEG.equals(extension) || FORMATO_PNG.equals(extension)
                || FORMATO_WEBP.equals(extension) || FORMATO_PDF.equals(extension)
                || FORMATO_HEIC.equals(extension) || FORMATO_HEIF.equals(extension) || "hif".equals(extension)
                || FORMATO_AVIF.equals(extension) || FORMATO_GIF.equals(extension) || FORMATO_BMP.equals(extension)
                || FORMATO_TIFF.equals(extension) || "tif".equals(extension)) {
            if ("hif".equals(extension)) {
                return FORMATO_HEIF;
            }
            if ("tif".equals(extension)) {
                return FORMATO_TIFF;
            }
            return extension;
        }
        return switch (contentType) {
            case CONTENT_TYPE_JPEG -> FORMATO_JPG;
            case CONTENT_TYPE_PNG -> FORMATO_PNG;
            case CONTENT_TYPE_WEBP -> FORMATO_WEBP;
            case CONTENT_TYPE_HEIC, "image/heic-sequence" -> FORMATO_HEIC;
            case CONTENT_TYPE_HEIF, "image/heif-sequence" -> FORMATO_HEIF;
            case CONTENT_TYPE_AVIF -> FORMATO_AVIF;
            case CONTENT_TYPE_GIF -> FORMATO_GIF;
            case CONTENT_TYPE_BMP, "image/x-ms-bmp" -> FORMATO_BMP;
            case CONTENT_TYPE_TIFF -> FORMATO_TIFF;
            case CONTENT_TYPE_PDF -> FORMATO_PDF;
            default -> FORMATO_OTRO;
        };
    }

    private String detectarFormatoPorFirma(byte[] contenido) {
        if (empiezaCon(contenido, 0xFF, 0xD8, 0xFF)) {
            return FORMATO_JPG;
        }
        if (empiezaCon(contenido, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) {
            return FORMATO_PNG;
        }
        if (empiezaConAscii(contenido, 0, "%PDF-")) {
            return FORMATO_PDF;
        }
        if (empiezaConAscii(contenido, 0, "GIF87a") || empiezaConAscii(contenido, 0, "GIF89a")) {
            return FORMATO_GIF;
        }
        if (empiezaConAscii(contenido, 0, "BM")) {
            return FORMATO_BMP;
        }
        if ((empiezaConAscii(contenido, 0, "II") && empiezaConDesde(contenido, 2, 0x2A, 0x00))
                || (empiezaConAscii(contenido, 0, "MM") && empiezaConDesde(contenido, 2, 0x00, 0x2A))) {
            return FORMATO_TIFF;
        }
        if (empiezaConAscii(contenido, 0, "RIFF") && empiezaConAscii(contenido, 8, "WEBP")) {
            return FORMATO_WEBP;
        }
        return detectarFormatoIsoBaseMedia(contenido);
    }

    private String detectarFormatoIsoBaseMedia(byte[] contenido) {
        if (!empiezaConAscii(contenido, 4, "ftyp") || contenido.length < 12) {
            return FORMATO_OTRO;
        }

        boolean marcaHeifGenerica = false;
        for (int indice = 8; indice + 4 <= Math.min(contenido.length, 64); indice += 4) {
            if (indice == 12) {
                continue;
            }
            String marca = new String(contenido, indice, 4, StandardCharsets.US_ASCII);
            if (MARCAS_AVIF.contains(marca)) {
                return FORMATO_AVIF;
            }
            if (MARCAS_HEIC.contains(marca)) {
                return FORMATO_HEIC;
            }
            if ("mif1".equals(marca) || "msf1".equals(marca)) {
                marcaHeifGenerica = true;
            }
        }
        return marcaHeifGenerica ? FORMATO_HEIF : FORMATO_OTRO;
    }

    private String contentTypeImagenSeguro(String formato, String contentTypeOriginal) {
        return switch (formato) {
            case FORMATO_JPG, FORMATO_JPEG -> CONTENT_TYPE_JPEG;
            case FORMATO_PNG -> CONTENT_TYPE_PNG;
            case FORMATO_WEBP -> CONTENT_TYPE_WEBP;
            case FORMATO_HEIC -> CONTENT_TYPE_HEIC;
            case FORMATO_HEIF -> CONTENT_TYPE_HEIF;
            case FORMATO_AVIF -> CONTENT_TYPE_AVIF;
            case FORMATO_GIF -> CONTENT_TYPE_GIF;
            case FORMATO_BMP -> CONTENT_TYPE_BMP;
            case FORMATO_TIFF -> CONTENT_TYPE_TIFF;
            default -> contentTypeOriginal;
        };
    }

    private boolean empiezaCon(byte[] contenido, int... bytesEsperados) {
        return empiezaConDesde(contenido, 0, bytesEsperados);
    }

    private boolean empiezaConDesde(byte[] contenido, int inicio, int... bytesEsperados) {
        if (inicio < 0 || contenido.length < inicio + bytesEsperados.length) {
            return false;
        }
        for (int indice = 0; indice < bytesEsperados.length; indice++) {
            if ((contenido[inicio + indice] & 0xFF) != bytesEsperados[indice]) {
                return false;
            }
        }
        return true;
    }

    private boolean empiezaConAscii(byte[] contenido, int inicio, String valor) {
        byte[] bytesEsperados = valor.getBytes(StandardCharsets.US_ASCII);
        if (inicio < 0 || contenido.length < inicio + bytesEsperados.length) {
            return false;
        }
        for (int indice = 0; indice < bytesEsperados.length; indice++) {
            if (contenido[inicio + indice] != bytesEsperados[indice]) {
                return false;
            }
        }
        return true;
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

    private String extensionAlmacenamiento(String formatoDetectado, String nombreArchivo) {
        if (!FORMATO_OTRO.equals(formatoDetectado)) {
            return formatoDetectado;
        }
        String extension = extension(nombreArchivo);
        return FORMATO_OTRO.equals(extension) ? "bin" : extension;
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
                evidencia.getPerdidaInventario() == null
                        ? null
                        : evidencia.getPerdidaInventario().getIdPerdidaInventario(),
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
            String extensionAlmacenamiento,
            String contentType,
            byte[] contenido,
            Integer tamanoOriginalKb,
            Integer tamanoComprimidoKb,
            boolean fueComprimido
    ) {
    }
}
