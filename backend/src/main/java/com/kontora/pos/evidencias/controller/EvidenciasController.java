package com.kontora.pos.evidencias.controller;

import com.kontora.pos.common.security.PrincipalUsuario;
import com.kontora.pos.evidencias.dto.ArchivoEvidenciaDescargada;
import com.kontora.pos.evidencias.dto.ArchivoEvidenciaResponse;
import com.kontora.pos.evidencias.service.EvidenciasService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/evidencias")
public class EvidenciasController {

    private final EvidenciasService evidenciasService;

    public EvidenciasController(EvidenciasService evidenciasService) {
        this.evidenciasService = evidenciasService;
    }

    @PostMapping(path = "/pagos-venta/{idPagoVenta}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ArchivoEvidenciaResponse cargarEvidenciaPagoVenta(
            @PathVariable UUID idPagoVenta,
            @RequestPart("archivo") MultipartFile archivo,
            Authentication authentication) {
        return evidenciasService.cargarEvidenciaPagoVenta(
                idPagoVenta,
                archivo,
                (PrincipalUsuario) authentication.getPrincipal());
    }

    @PostMapping(path = "/gastos-caja/{idGastoCaja}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ArchivoEvidenciaResponse cargarEvidenciaGastoCaja(
            @PathVariable UUID idGastoCaja,
            @RequestPart("archivo") MultipartFile archivo,
            Authentication authentication) {
        return evidenciasService.cargarEvidenciaGastoCaja(
                idGastoCaja,
                archivo,
                (PrincipalUsuario) authentication.getPrincipal());
    }

    @PostMapping(path = "/consignaciones-bancarias/{idConsignacionBancaria}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ArchivoEvidenciaResponse cargarEvidenciaConsignacionBancaria(
            @PathVariable UUID idConsignacionBancaria,
            @RequestPart("archivo") MultipartFile archivo,
            Authentication authentication) {
        return evidenciasService.cargarEvidenciaConsignacionBancaria(
                idConsignacionBancaria,
                archivo,
                (PrincipalUsuario) authentication.getPrincipal());
    }

    @PostMapping(path = "/pagos-servicios/{idPagoServicio}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ArchivoEvidenciaResponse cargarEvidenciaPagoServicio(
            @PathVariable UUID idPagoServicio,
            @RequestPart("archivo") MultipartFile archivo,
            Authentication authentication) {
        return evidenciasService.cargarEvidenciaPagoServicio(
                idPagoServicio,
                archivo,
                (PrincipalUsuario) authentication.getPrincipal());
    }

    @GetMapping("/{idArchivoEvidencia}")
    public ArchivoEvidenciaResponse obtenerEvidencia(
            @PathVariable UUID idArchivoEvidencia,
            Authentication authentication) {
        return evidenciasService.obtenerEvidencia(
                idArchivoEvidencia,
                (PrincipalUsuario) authentication.getPrincipal());
    }

    @PostMapping(path = "/pagos-venta/{idPagoVenta}/ajustes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ArchivoEvidenciaResponse cargarAjusteEvidenciaPagoVenta(
            @PathVariable UUID idPagoVenta,
            @RequestPart("archivo") MultipartFile archivo,
            Authentication authentication) {
        return evidenciasService.cargarAjusteEvidenciaPagoVenta(
                idPagoVenta,
                archivo,
                (PrincipalUsuario) authentication.getPrincipal());
    }

    @GetMapping("/{idArchivoEvidencia}/descargar")
    public ResponseEntity<byte[]> descargarEvidencia(
            @PathVariable UUID idArchivoEvidencia,
            Authentication authentication) {
        ArchivoEvidenciaDescargada evidencia = evidenciasService.descargarEvidencia(
                idArchivoEvidencia,
                (PrincipalUsuario) authentication.getPrincipal());
        MediaType contentType = mediaTypeSeguro(evidencia.contentType());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(contentType);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(evidencia.nombreArchivo(), StandardCharsets.UTF_8)
                .build());
        return new ResponseEntity<>(evidencia.contenido(), headers, HttpStatus.OK);
    }

    @GetMapping("/pagos-venta/{idPagoVenta}")
    public List<ArchivoEvidenciaResponse> listarPorPagoVenta(
            @PathVariable UUID idPagoVenta,
            Authentication authentication) {
        return evidenciasService.listarPorPagoVenta(
                idPagoVenta,
                (PrincipalUsuario) authentication.getPrincipal());
    }

    @GetMapping("/gastos-caja/{idGastoCaja}")
    public List<ArchivoEvidenciaResponse> listarPorGastoCaja(
            @PathVariable UUID idGastoCaja,
            Authentication authentication) {
        return evidenciasService.listarPorGastoCaja(
                idGastoCaja,
                (PrincipalUsuario) authentication.getPrincipal());
    }

    @GetMapping("/consignaciones-bancarias/{idConsignacionBancaria}")
    public List<ArchivoEvidenciaResponse> listarPorConsignacionBancaria(
            @PathVariable UUID idConsignacionBancaria,
            Authentication authentication) {
        return evidenciasService.listarPorConsignacionBancaria(
                idConsignacionBancaria,
                (PrincipalUsuario) authentication.getPrincipal());
    }

    @GetMapping("/pagos-servicios/{idPagoServicio}")
    public List<ArchivoEvidenciaResponse> listarPorPagoServicio(
            @PathVariable UUID idPagoServicio,
            Authentication authentication) {
        return evidenciasService.listarPorPagoServicio(
                idPagoServicio,
                (PrincipalUsuario) authentication.getPrincipal());
    }

    private MediaType mediaTypeSeguro(String contentType) {
        try {
            return MediaType.parseMediaType(contentType);
        } catch (IllegalArgumentException exception) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
