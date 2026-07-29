package com.kontora.pos.inventario.service;

import com.kontora.pos.auditoria.service.AuditoriaService;
import com.kontora.pos.caja.domain.CajaDiaria;
import com.kontora.pos.caja.repository.CajaDiariaRepository;
import com.kontora.pos.catalogos.domain.ItemInventario;
import com.kontora.pos.catalogos.domain.TamanoVaso;
import com.kontora.pos.catalogos.repository.ItemInventarioRepository;
import com.kontora.pos.common.exception.ApiException;
import com.kontora.pos.common.security.PrincipalUsuario;
import com.kontora.pos.evidencias.dto.ArchivoEvidenciaResponse;
import com.kontora.pos.evidencias.service.EvidenciasService;
import com.kontora.pos.inventario.domain.PaqueteVasosAbierto;
import com.kontora.pos.inventario.domain.PerdidaInventario;
import com.kontora.pos.inventario.dto.AnularPerdidaInventarioRequest;
import com.kontora.pos.inventario.dto.PerdidaInventarioResponse;
import com.kontora.pos.inventario.dto.RegistrarPerdidaInventarioRequest;
import com.kontora.pos.inventario.repository.PaqueteVasosAbiertoRepository;
import com.kontora.pos.inventario.repository.PerdidaInventarioRepository;
import com.kontora.pos.usuarios.domain.Usuario;
import com.kontora.pos.usuarios.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static com.kontora.pos.common.audit.AuditoriaValores.valores;

@Service
public class PerdidasInventarioService {

    private static final String ESTADO_CAJA_ABIERTA = "abierta";
    private static final String ESTADO_REGISTRADA = "registrada";
    private static final String ESTADO_ANULADA = "anulada";

    private final CajaDiariaRepository cajaDiariaRepository;
    private final ItemInventarioRepository itemInventarioRepository;
    private final PaqueteVasosAbiertoRepository paqueteVasosAbiertoRepository;
    private final PerdidaInventarioRepository perdidaInventarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final InventarioService inventarioService;
    private final EvidenciasService evidenciasService;
    private final AuditoriaService auditoriaService;

    public PerdidasInventarioService(
            CajaDiariaRepository cajaDiariaRepository,
            ItemInventarioRepository itemInventarioRepository,
            PaqueteVasosAbiertoRepository paqueteVasosAbiertoRepository,
            PerdidaInventarioRepository perdidaInventarioRepository,
            UsuarioRepository usuarioRepository,
            InventarioService inventarioService,
            EvidenciasService evidenciasService,
            AuditoriaService auditoriaService) {
        this.cajaDiariaRepository = cajaDiariaRepository;
        this.itemInventarioRepository = itemInventarioRepository;
        this.paqueteVasosAbiertoRepository = paqueteVasosAbiertoRepository;
        this.perdidaInventarioRepository = perdidaInventarioRepository;
        this.usuarioRepository = usuarioRepository;
        this.inventarioService = inventarioService;
        this.evidenciasService = evidenciasService;
        this.auditoriaService = auditoriaService;
    }

    @Transactional
    public PerdidaInventarioResponse registrar(
            RegistrarPerdidaInventarioRequest request,
            PrincipalUsuario principalUsuario) {
        validarRolGestion(principalUsuario);
        if (!Boolean.TRUE.equals(request.confirmaRegistro())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Debe confirmar el registro de la perdida");
        }

        CajaDiaria cajaDiaria = obtenerCajaAbierta();
        Usuario usuarioRegistro = obtenerUsuario(principalUsuario.idUsuario());
        ItemInventario item = obtenerVasoActivoPorTamano(request.idTamanoVaso());
        PaqueteVasosAbierto paquete = obtenerYValidarPaquete(
                request.idPaqueteVasosAbierto(),
                cajaDiaria,
                item,
                request.cantidad());

        PerdidaInventario perdida = new PerdidaInventario();
        perdida.setCajaDiaria(cajaDiaria);
        perdida.setItemInventario(item);
        perdida.setPaqueteVasosAbierto(paquete);
        perdida.setCantidad(request.cantidad());
        perdida.setMotivo(normalizarRequerido(request.motivo(), "motivo"));
        perdida.setUsuarioRegistro(usuarioRegistro);
        perdida.setFechaRegistro(OffsetDateTime.now());
        perdida.setEstado(ESTADO_REGISTRADA);
        PerdidaInventario perdidaGuardada = perdidaInventarioRepository.saveAndFlush(perdida);

        inventarioService.registrarPerdidaVasos(perdidaGuardada, usuarioRegistro);

        auditoriaService.registrar(
                usuarioRegistro,
                "perdidas_inventario",
                perdidaGuardada.getIdPerdidaInventario(),
                "crear",
                null,
                snapshot(perdidaGuardada, List.of()),
                "Registro de perdida de vasos pendiente de evidencia fotografica");
        return toResponse(perdidaGuardada, List.of());
    }

    @Transactional(readOnly = true)
    public List<PerdidaInventarioResponse> consultarCajaAbierta(
            PrincipalUsuario principalUsuario) {
        validarRolGestion(principalUsuario);
        CajaDiaria cajaDiaria = obtenerCajaAbierta();
        return perdidaInventarioRepository
                .findByCajaDiaria_IdCajaDiariaOrderByFechaRegistroDesc(cajaDiaria.getIdCajaDiaria())
                .stream()
                .map(perdida -> toResponse(
                        perdida,
                        evidenciasService.listarPorPerdidaInventario(
                                perdida.getIdPerdidaInventario(),
                                principalUsuario)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PerdidaInventarioResponse> consultarPeriodo(
            LocalDate fechaInicio,
            LocalDate fechaFin,
            PrincipalUsuario principalUsuario) {
        validarRolGestion(principalUsuario);
        LocalDate fin = validarPeriodo(fechaInicio, fechaFin);
        return perdidaInventarioRepository.findParaConsulta(fechaInicio, fin)
                .stream()
                .map(perdida -> toResponse(
                        perdida,
                        evidenciasService.listarPorPerdidaInventario(
                                perdida.getIdPerdidaInventario(),
                                principalUsuario)))
                .toList();
    }

    @Transactional
    public PerdidaInventarioResponse anular(
            UUID idPerdidaInventario,
            AnularPerdidaInventarioRequest request,
            PrincipalUsuario principalUsuario) {
        validarRolGestion(principalUsuario);
        if (!Boolean.TRUE.equals(request.confirmaVasoNoRoto())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Debe confirmar que el vaso no estaba roto ni fue perdido");
        }

        Usuario usuarioAnulacion = obtenerUsuario(principalUsuario.idUsuario());
        PerdidaInventario perdida = perdidaInventarioRepository
                .findByIdForUpdate(idPerdidaInventario)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "Perdida de inventario no encontrada"));
        validarMismaCajaAbierta(perdida.getCajaDiaria());
        if (!ESTADO_REGISTRADA.equals(perdida.getEstado())) {
            throw new ApiException(HttpStatus.CONFLICT, "La perdida ya fue anulada");
        }

        List<ArchivoEvidenciaResponse> evidencias = evidenciasService
                .listarPorPerdidaInventario(idPerdidaInventario, principalUsuario);
        Map<String, Object> valorAnterior = snapshot(perdida, evidencias);
        restaurarCantidadRotaPaquete(perdida);
        inventarioService.restaurarPerdidaVasos(perdida, usuarioAnulacion);

        perdida.setEstado(ESTADO_ANULADA);
        perdida.setUsuarioAnulacion(usuarioAnulacion);
        perdida.setFechaAnulacion(OffsetDateTime.now());
        perdida.setMotivoAnulacion(
                normalizarRequerido(request.motivoAnulacion(), "motivoAnulacion"));
        PerdidaInventario perdidaGuardada = perdidaInventarioRepository.saveAndFlush(perdida);

        auditoriaService.registrar(
                usuarioAnulacion,
                "perdidas_inventario",
                perdidaGuardada.getIdPerdidaInventario(),
                "anular",
                valorAnterior,
                snapshot(perdidaGuardada, evidencias),
                "Anulacion de perdida registrada por error y restauracion del stock diario");
        return toResponse(perdidaGuardada, evidencias);
    }

    private ItemInventario obtenerVasoActivoPorTamano(UUID idTamanoVaso) {
        return itemInventarioRepository.findVasoActivoPorTamano(idTamanoVaso)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "No existe un vaso activo para el tamano seleccionado"));
    }

    private PaqueteVasosAbierto obtenerYValidarPaquete(
            UUID idPaquete,
            CajaDiaria cajaDiaria,
            ItemInventario item,
            int cantidadPerdida) {
        if (idPaquete == null) {
            return null;
        }
        PaqueteVasosAbierto paquete = paqueteVasosAbiertoRepository.findByIdForUpdate(idPaquete)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "Paquete de vasos abierto no encontrado"));
        if (!cajaDiaria.getIdCajaDiaria().equals(paquete.getCajaDiaria().getIdCajaDiaria())
                || !item.getIdItemInventario().equals(
                        paquete.getItemInventario().getIdItemInventario())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "El paquete seleccionado no corresponde a la caja y al vaso de la perdida");
        }
        int nuevasUnidadesRotas = paquete.getUnidadesRotas() + cantidadPerdida;
        if (nuevasUnidadesRotas > paquete.getUnidadesGeneradas()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "La perdida supera las unidades generadas por el paquete seleccionado");
        }
        paquete.setUnidadesRotas(nuevasUnidadesRotas);
        return paquete;
    }

    private void restaurarCantidadRotaPaquete(PerdidaInventario perdida) {
        if (perdida.getPaqueteVasosAbierto() == null) {
            return;
        }
        PaqueteVasosAbierto paquete = paqueteVasosAbiertoRepository
                .findByIdForUpdate(
                        perdida.getPaqueteVasosAbierto().getIdPaqueteVasosAbierto())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.CONFLICT,
                        "No existe el paquete relacionado con la perdida"));
        if (paquete.getUnidadesRotas() < perdida.getCantidad()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "La anulacion dejaria las unidades rotas del paquete negativas");
        }
        paquete.setUnidadesRotas(paquete.getUnidadesRotas() - perdida.getCantidad());
    }

    private CajaDiaria obtenerCajaAbierta() {
        return cajaDiariaRepository.findPrimeraPorEstadoCaja(ESTADO_CAJA_ABIERTA)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.CONFLICT,
                        "No existe caja diaria abierta para registrar perdidas"));
    }

    private void validarMismaCajaAbierta(CajaDiaria cajaDiaria) {
        CajaDiaria cajaAbierta = obtenerCajaAbierta();
        if (!cajaAbierta.getIdCajaDiaria().equals(cajaDiaria.getIdCajaDiaria())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "La perdida solo puede anularse mientras su misma caja esta abierta");
        }
    }

    private Usuario obtenerUsuario(UUID idUsuario) {
        return usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "Usuario autenticado no encontrado"));
    }

    private void validarRolGestion(PrincipalUsuario principalUsuario) {
        String rol = principalUsuario.nombreRol().toLowerCase(Locale.ROOT);
        if (!"administrador".equals(rol) && !"gerente".equals(rol)) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "Solo administrador o gerente puede gestionar perdidas de vasos");
        }
    }

    private LocalDate validarPeriodo(LocalDate fechaInicio, LocalDate fechaFin) {
        if (fechaInicio == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "fechaInicio es obligatoria");
        }
        LocalDate fin = fechaFin == null ? fechaInicio : fechaFin;
        if (fin.isBefore(fechaInicio)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "fechaFin no puede ser anterior a fechaInicio");
        }
        return fin;
    }

    private String normalizarRequerido(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, campo + " es obligatorio");
        }
        return valor.trim();
    }

    private Map<String, Object> snapshot(
            PerdidaInventario perdida,
            List<ArchivoEvidenciaResponse> evidencias) {
        Usuario usuarioAnulacion = perdida.getUsuarioAnulacion();
        return valores(
                "id_perdida_inventario", perdida.getIdPerdidaInventario(),
                "id_caja_diaria", perdida.getCajaDiaria().getIdCajaDiaria(),
                "id_item_inventario", perdida.getItemInventario().getIdItemInventario(),
                "id_paquete_vasos_abierto",
                perdida.getPaqueteVasosAbierto() == null
                        ? null
                        : perdida.getPaqueteVasosAbierto().getIdPaqueteVasosAbierto(),
                "cantidad", perdida.getCantidad(),
                "motivo", perdida.getMotivo(),
                "id_usuario_registro", perdida.getUsuarioRegistro().getIdUsuario(),
                "fecha_registro", perdida.getFechaRegistro(),
                "estado", perdida.getEstado(),
                "id_usuario_anulacion",
                usuarioAnulacion == null ? null : usuarioAnulacion.getIdUsuario(),
                "fecha_anulacion", perdida.getFechaAnulacion(),
                "motivo_anulacion", perdida.getMotivoAnulacion(),
                "evidencias", evidencias.stream()
                        .map(ArchivoEvidenciaResponse::idArchivoEvidencia)
                        .toList());
    }

    private PerdidaInventarioResponse toResponse(
            PerdidaInventario perdida,
            List<ArchivoEvidenciaResponse> evidencias) {
        ItemInventario item = perdida.getItemInventario();
        TamanoVaso tamanoVaso = item.getTamanoVaso();
        Usuario usuarioAnulacion = perdida.getUsuarioAnulacion();
        return new PerdidaInventarioResponse(
                perdida.getIdPerdidaInventario(),
                perdida.getCajaDiaria().getIdCajaDiaria(),
                item.getIdItemInventario(),
                item.getNombreItem(),
                tamanoVaso == null ? null : tamanoVaso.getIdTamanoVaso(),
                tamanoVaso == null ? null : tamanoVaso.getOnzas(),
                perdida.getPaqueteVasosAbierto() == null
                        ? null
                        : perdida.getPaqueteVasosAbierto().getIdPaqueteVasosAbierto(),
                perdida.getCantidad(),
                perdida.getMotivo(),
                perdida.getUsuarioRegistro().getIdUsuario(),
                perdida.getUsuarioRegistro().getNombreUsuario(),
                perdida.getFechaRegistro(),
                perdida.getEstado(),
                usuarioAnulacion == null ? null : usuarioAnulacion.getIdUsuario(),
                usuarioAnulacion == null ? null : usuarioAnulacion.getNombreUsuario(),
                perdida.getFechaAnulacion(),
                perdida.getMotivoAnulacion(),
                evidencias);
    }
}
