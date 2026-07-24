package com.kontora.pos.catalogos.service;

import static com.kontora.pos.common.audit.AuditoriaValores.valores;

import com.kontora.pos.auditoria.service.AuditoriaService;
import com.kontora.pos.catalogos.domain.CategoriaInventario;
import com.kontora.pos.catalogos.domain.ItemInventario;
import com.kontora.pos.catalogos.domain.PrecioGranizado;
import com.kontora.pos.catalogos.domain.TamanoVaso;
import com.kontora.pos.catalogos.domain.TipoGranizado;
import com.kontora.pos.catalogos.domain.UnidadMedida;
import com.kontora.pos.catalogos.dto.ActualizarEstadoItemInventarioRequest;
import com.kontora.pos.catalogos.dto.ActualizarItemInventarioRequest;
import com.kontora.pos.catalogos.dto.CrearItemInventarioRequest;
import com.kontora.pos.catalogos.dto.CrearPrecioGranizadoRequest;
import com.kontora.pos.catalogos.dto.ItemInventarioResponse;
import com.kontora.pos.catalogos.dto.PrecioGranizadoResponse;
import com.kontora.pos.catalogos.repository.CategoriaInventarioRepository;
import com.kontora.pos.catalogos.repository.ItemInventarioRepository;
import com.kontora.pos.catalogos.repository.PrecioGranizadoRepository;
import com.kontora.pos.catalogos.repository.TamanoVasoRepository;
import com.kontora.pos.catalogos.repository.TipoGranizadoRepository;
import com.kontora.pos.catalogos.repository.UnidadMedidaRepository;
import com.kontora.pos.common.exception.ApiException;
import com.kontora.pos.common.security.PrincipalUsuario;
import com.kontora.pos.inventario.domain.ExistenciaInventarioGeneral;
import com.kontora.pos.inventario.repository.ExistenciaInventarioGeneralRepository;
import com.kontora.pos.inventario.repository.MovimientoInventarioRepository;
import com.kontora.pos.usuarios.domain.Usuario;
import com.kontora.pos.usuarios.repository.UsuarioRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GestionCatalogosService {

    private static final String ESTADO_ACTIVO = "activo";
    private static final String ESTADO_INACTIVO = "inactivo";
    private static final String ROL_ADMINISTRADOR = "administrador";
    private static final String ROL_GERENTE = "gerente";
    private static final String TIPO_AUTOMATICO_POR_VENTA = "automatico_por_venta";
    private static final String TIPO_MANUAL_POR_CONSUMO = "manual_por_consumo";

    private final ItemInventarioRepository itemInventarioRepository;
    private final PrecioGranizadoRepository precioGranizadoRepository;
    private final CategoriaInventarioRepository categoriaInventarioRepository;
    private final UnidadMedidaRepository unidadMedidaRepository;
    private final TamanoVasoRepository tamanoVasoRepository;
    private final TipoGranizadoRepository tipoGranizadoRepository;
    private final ExistenciaInventarioGeneralRepository existenciaInventarioGeneralRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;

    public GestionCatalogosService(
            ItemInventarioRepository itemInventarioRepository,
            PrecioGranizadoRepository precioGranizadoRepository,
            CategoriaInventarioRepository categoriaInventarioRepository,
            UnidadMedidaRepository unidadMedidaRepository,
            TamanoVasoRepository tamanoVasoRepository,
            TipoGranizadoRepository tipoGranizadoRepository,
            ExistenciaInventarioGeneralRepository existenciaInventarioGeneralRepository,
            MovimientoInventarioRepository movimientoInventarioRepository,
            UsuarioRepository usuarioRepository,
            AuditoriaService auditoriaService) {
        this.itemInventarioRepository = itemInventarioRepository;
        this.precioGranizadoRepository = precioGranizadoRepository;
        this.categoriaInventarioRepository = categoriaInventarioRepository;
        this.unidadMedidaRepository = unidadMedidaRepository;
        this.tamanoVasoRepository = tamanoVasoRepository;
        this.tipoGranizadoRepository = tipoGranizadoRepository;
        this.existenciaInventarioGeneralRepository = existenciaInventarioGeneralRepository;
        this.movimientoInventarioRepository = movimientoInventarioRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditoriaService = auditoriaService;
    }

    @Transactional(readOnly = true)
    public List<ItemInventarioResponse> listarItemsParaGestion(PrincipalUsuario principalUsuario) {
        validarRolAdministrativo(principalUsuario);
        return itemInventarioRepository.findAllParaGestion().stream()
                .map(this::aRespuesta)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PrecioGranizadoResponse> listarPreciosParaGestion(PrincipalUsuario principalUsuario) {
        validarRolAdministrativo(principalUsuario);
        return precioGranizadoRepository.findAllParaGestion().stream()
                .map(this::aRespuesta)
                .toList();
    }

    @Transactional
    public ItemInventarioResponse crearItem(CrearItemInventarioRequest request, PrincipalUsuario principalUsuario) {
        validarRolAdministrativo(principalUsuario);
        DatosItem datos = validarDatosItem(
                request.idCategoriaInventario(),
                request.idUnidadMedida(),
                request.idTamanoVaso(),
                request.tipoControl(),
                request.manejaPaquetes(),
                request.unidadesPorPaquete());
        String nombreItem = normalizarTexto(request.nombreItem());
        validarNombreDisponible(nombreItem, null);
        OffsetDateTime ahora = OffsetDateTime.now(ZoneOffset.UTC);

        ItemInventario item = new ItemInventario();
        asignarDatosItem(item, datos, nombreItem);
        item.setEstado(ESTADO_ACTIVO);
        item.setFechaCreacion(ahora);
        ItemInventario itemGuardado = itemInventarioRepository.saveAndFlush(item);

        ExistenciaInventarioGeneral existencia = new ExistenciaInventarioGeneral();
        existencia.setItemInventario(itemGuardado);
        existencia.setCantidadActual(0);
        existencia.setFechaActualizacion(ahora);
        existenciaInventarioGeneralRepository.saveAndFlush(existencia);

        auditoriaService.registrar(
                principalUsuario.idUsuario(),
                "items_inventario",
                itemGuardado.getIdItemInventario(),
                "crear",
                null,
                valoresItem(itemGuardado),
                "Creacion de item de inventario con existencia general en cero");
        return aRespuesta(itemGuardado);
    }

    @Transactional
    public ItemInventarioResponse actualizarItem(
            UUID idItemInventario,
            ActualizarItemInventarioRequest request,
            PrincipalUsuario principalUsuario) {
        validarRolAdministrativo(principalUsuario);
        ItemInventario item = obtenerItem(idItemInventario);
        DatosItem datos = validarDatosItem(
                request.idCategoriaInventario(),
                request.idUnidadMedida(),
                request.idTamanoVaso(),
                request.tipoControl(),
                request.manejaPaquetes(),
                request.unidadesPorPaquete());
        String nombreItem = normalizarTexto(request.nombreItem());
        validarNombreDisponible(nombreItem, item.getIdItemInventario());

        if (tieneCambiosEstructurales(item, datos)
                && movimientoInventarioRepository.existsByItemInventario_IdItemInventario(item.getIdItemInventario())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "No puedes cambiar categoria, unidad, control, tamano o paquetes despues de registrar movimientos");
        }

        var valorAnterior = valoresItem(item);
        asignarDatosItem(item, datos, nombreItem);
        ItemInventario itemGuardado = itemInventarioRepository.saveAndFlush(item);

        auditoriaService.registrar(
                principalUsuario.idUsuario(),
                "items_inventario",
                itemGuardado.getIdItemInventario(),
                "editar",
                valorAnterior,
                valoresItem(itemGuardado),
                "Actualizacion de item de inventario");
        return aRespuesta(itemGuardado);
    }

    @Transactional
    public ItemInventarioResponse actualizarEstadoItem(
            UUID idItemInventario,
            ActualizarEstadoItemInventarioRequest request,
            PrincipalUsuario principalUsuario) {
        validarRolAdministrativo(principalUsuario);
        ItemInventario item = obtenerItem(idItemInventario);
        String estado = request.estado().trim().toLowerCase(Locale.ROOT);

        if (estado.equals(item.getEstado())) {
            return aRespuesta(item);
        }
        if (ESTADO_INACTIVO.equals(estado)) {
            ExistenciaInventarioGeneral existencia = existenciaInventarioGeneralRepository
                    .findByItemForUpdate(item.getIdItemInventario())
                    .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "El item no tiene existencia general registrada"));
            if (existencia.getCantidadActual() != 0) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "No puedes inhabilitar un item mientras conserve stock general");
            }
        }

        var valorAnterior = valoresItem(item);
        item.setEstado(estado);
        ItemInventario itemGuardado = itemInventarioRepository.saveAndFlush(item);

        auditoriaService.registrar(
                principalUsuario.idUsuario(),
                "items_inventario",
                itemGuardado.getIdItemInventario(),
                "editar",
                valorAnterior,
                valoresItem(itemGuardado),
                "Cambio de estado de item de inventario");
        return aRespuesta(itemGuardado);
    }

    @Transactional
    public PrecioGranizadoResponse crearPrecio(CrearPrecioGranizadoRequest request, PrincipalUsuario principalUsuario) {
        validarRolAdministrativo(principalUsuario);
        if (request.fechaInicioVigencia().isBefore(LocalDate.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La vigencia del nuevo precio no puede iniciar en una fecha pasada");
        }

        TipoGranizado tipoGranizado = obtenerTipoGranizadoActivo(request.idTipoGranizado());
        TamanoVaso tamanoVaso = obtenerTamanoVasoActivo(request.idTamanoVaso());
        PrecioGranizado precioAbierto = precioGranizadoRepository
                .findPrecioAbiertoForUpdate(tipoGranizado.getIdTipoGranizado(), tamanoVaso.getIdTamanoVaso())
                .orElse(null);

        if (precioAbierto != null) {
            if (!request.fechaInicioVigencia().isAfter(precioAbierto.getFechaInicioVigencia())) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "La vigencia debe iniciar despues del precio abierto actual o programado");
            }
            var valorAnterior = valoresPrecio(precioAbierto);
            precioAbierto.setFechaFinVigencia(request.fechaInicioVigencia().minusDays(1));
            PrecioGranizado precioCerrado = precioGranizadoRepository.saveAndFlush(precioAbierto);
            auditoriaService.registrar(
                    principalUsuario.idUsuario(),
                    "precios_granizado",
                    precioCerrado.getIdPrecioGranizado(),
                    "editar",
                    valorAnterior,
                    valoresPrecio(precioCerrado),
                    "Cierre de vigencia por nuevo precio");
        }

        Usuario usuarioCreacion = usuarioRepository.findById(principalUsuario.idUsuario())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Usuario autenticado no encontrado"));
        PrecioGranizado nuevoPrecio = new PrecioGranizado();
        nuevoPrecio.setTipoGranizado(tipoGranizado);
        nuevoPrecio.setTamanoVaso(tamanoVaso);
        nuevoPrecio.setValorPrecio(request.valorPrecio());
        nuevoPrecio.setFechaInicioVigencia(request.fechaInicioVigencia());
        nuevoPrecio.setFechaFinVigencia(null);
        nuevoPrecio.setEstado(ESTADO_ACTIVO);
        nuevoPrecio.setUsuarioCreacion(usuarioCreacion);
        PrecioGranizado precioGuardado = precioGranizadoRepository.saveAndFlush(nuevoPrecio);

        auditoriaService.registrar(
                principalUsuario.idUsuario(),
                "precios_granizado",
                precioGuardado.getIdPrecioGranizado(),
                "crear",
                null,
                valoresPrecio(precioGuardado),
                "Creacion de nueva vigencia de precio");
        return aRespuesta(precioGuardado);
    }

    private void validarRolAdministrativo(PrincipalUsuario principalUsuario) {
        String rol = principalUsuario.nombreRol().trim().toLowerCase(Locale.ROOT);
        if (!ROL_ADMINISTRADOR.equals(rol) && !ROL_GERENTE.equals(rol)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo administrador o gerente puede gestionar catalogos");
        }
    }

    private DatosItem validarDatosItem(
            UUID idCategoriaInventario,
            UUID idUnidadMedida,
            UUID idTamanoVaso,
            String tipoControl,
            boolean manejaPaquetes,
            Integer unidadesPorPaquete) {
        CategoriaInventario categoria = obtenerCategoriaActiva(idCategoriaInventario);
        UnidadMedida unidad = obtenerUnidadActiva(idUnidadMedida);
        String tipoControlNormalizado = tipoControl.trim().toLowerCase(Locale.ROOT);

        if (TIPO_AUTOMATICO_POR_VENTA.equals(tipoControlNormalizado)) {
            TamanoVaso tamano = idTamanoVaso == null ? null : obtenerTamanoVasoActivo(idTamanoVaso);
            if (!"vasos".equals(categoria.getNombreCategoria()) || tamano == null || !manejaPaquetes
                    || !Integer.valueOf(20).equals(unidadesPorPaquete)) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "Los vasos automaticos requieren categoria vasos, tamano y paquetes de 20 unidades");
            }
            return new DatosItem(categoria, unidad, tamano, tipoControlNormalizado, true, 20);
        }

        if (TIPO_MANUAL_POR_CONSUMO.equals(tipoControlNormalizado)) {
            if (idTamanoVaso != null || manejaPaquetes || unidadesPorPaquete != null) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "Los items manuales no usan tamano de vaso ni apertura de paquetes");
            }
            return new DatosItem(categoria, unidad, null, tipoControlNormalizado, false, null);
        }

        throw new ApiException(HttpStatus.BAD_REQUEST, "El tipo de control no es valido");
    }

    private void asignarDatosItem(ItemInventario item, DatosItem datos, String nombreItem) {
        item.setCategoriaInventario(datos.categoria());
        item.setUnidadMedida(datos.unidad());
        item.setTamanoVaso(datos.tamanoVaso());
        item.setNombreItem(nombreItem);
        item.setTipoControl(datos.tipoControl());
        item.setManejaPaquetes(datos.manejaPaquetes());
        item.setUnidadesPorPaquete(datos.unidadesPorPaquete());
    }

    private boolean tieneCambiosEstructurales(ItemInventario item, DatosItem datos) {
        return !item.getCategoriaInventario().getIdCategoriaInventario().equals(datos.categoria().getIdCategoriaInventario())
                || !item.getUnidadMedida().getIdUnidadMedida().equals(datos.unidad().getIdUnidadMedida())
                || !Objects.equals(idTamano(item.getTamanoVaso()), idTamano(datos.tamanoVaso()))
                || !item.getTipoControl().equals(datos.tipoControl())
                || item.isManejaPaquetes() != datos.manejaPaquetes()
                || !Objects.equals(item.getUnidadesPorPaquete(), datos.unidadesPorPaquete());
    }

    private UUID idTamano(TamanoVaso tamanoVaso) {
        return tamanoVaso == null ? null : tamanoVaso.getIdTamanoVaso();
    }

    private void validarNombreDisponible(String nombreItem, UUID idItemActual) {
        itemInventarioRepository.findByNombreItem(nombreItem).ifPresent(item -> {
            if (!item.getIdItemInventario().equals(idItemActual)) {
                throw new ApiException(HttpStatus.CONFLICT, "El nombre del item ya existe");
            }
        });
    }

    private ItemInventario obtenerItem(UUID idItemInventario) {
        return itemInventarioRepository.findById(idItemInventario)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Item de inventario no encontrado"));
    }

    private CategoriaInventario obtenerCategoriaActiva(UUID idCategoriaInventario) {
        CategoriaInventario categoria = categoriaInventarioRepository.findById(idCategoriaInventario)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "La categoria de inventario no existe"));
        if (!ESTADO_ACTIVO.equals(categoria.getEstado())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La categoria de inventario debe estar activa");
        }
        return categoria;
    }

    private UnidadMedida obtenerUnidadActiva(UUID idUnidadMedida) {
        UnidadMedida unidad = unidadMedidaRepository.findById(idUnidadMedida)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "La unidad de medida no existe"));
        if (!ESTADO_ACTIVO.equals(unidad.getEstado())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La unidad de medida debe estar activa");
        }
        return unidad;
    }

    private TamanoVaso obtenerTamanoVasoActivo(UUID idTamanoVaso) {
        TamanoVaso tamanoVaso = tamanoVasoRepository.findById(idTamanoVaso)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "El tamano de vaso no existe"));
        if (!ESTADO_ACTIVO.equals(tamanoVaso.getEstado())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El tamano de vaso debe estar activo");
        }
        return tamanoVaso;
    }

    private TipoGranizado obtenerTipoGranizadoActivo(UUID idTipoGranizado) {
        TipoGranizado tipoGranizado = tipoGranizadoRepository.findById(idTipoGranizado)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "El tipo de granizado no existe"));
        if (!ESTADO_ACTIVO.equals(tipoGranizado.getEstado())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El tipo de granizado debe estar activo");
        }
        return tipoGranizado;
    }

    private String normalizarTexto(String valor) {
        return valor.trim();
    }

    private ItemInventarioResponse aRespuesta(ItemInventario item) {
        TamanoVaso tamanoVaso = item.getTamanoVaso();
        return new ItemInventarioResponse(
                item.getIdItemInventario(),
                item.getNombreItem(),
                item.getTipoControl(),
                item.isManejaPaquetes(),
                item.getUnidadesPorPaquete(),
                item.getEstado(),
                item.getFechaCreacion(),
                item.getCategoriaInventario().getIdCategoriaInventario(),
                item.getCategoriaInventario().getNombreCategoria(),
                item.getUnidadMedida().getIdUnidadMedida(),
                item.getUnidadMedida().getNombreUnidad(),
                item.getUnidadMedida().getAbreviatura(),
                tamanoVaso == null ? null : tamanoVaso.getIdTamanoVaso(),
                tamanoVaso == null ? null : tamanoVaso.getOnzas());
    }

    private PrecioGranizadoResponse aRespuesta(PrecioGranizado precio) {
        return new PrecioGranizadoResponse(
                precio.getIdPrecioGranizado(),
                precio.getTipoGranizado().getIdTipoGranizado(),
                precio.getTipoGranizado().getNombreTipo(),
                precio.getTamanoVaso().getIdTamanoVaso(),
                precio.getTamanoVaso().getOnzas(),
                precio.getValorPrecio(),
                precio.getFechaInicioVigencia(),
                precio.getFechaFinVigencia(),
                precio.getEstado());
    }

    private java.util.Map<String, Object> valoresItem(ItemInventario item) {
        return valores(
                "id_item_inventario", item.getIdItemInventario(),
                "nombre_item", item.getNombreItem(),
                "id_categoria_inventario", item.getCategoriaInventario().getIdCategoriaInventario(),
                "id_unidad_medida", item.getUnidadMedida().getIdUnidadMedida(),
                "id_tamano_vaso", idTamano(item.getTamanoVaso()),
                "tipo_control", item.getTipoControl(),
                "maneja_paquetes", item.isManejaPaquetes(),
                "unidades_por_paquete", item.getUnidadesPorPaquete(),
                "estado", item.getEstado());
    }

    private java.util.Map<String, Object> valoresPrecio(PrecioGranizado precio) {
        return valores(
                "id_precio_granizado", precio.getIdPrecioGranizado(),
                "id_tipo_granizado", precio.getTipoGranizado().getIdTipoGranizado(),
                "id_tamano_vaso", precio.getTamanoVaso().getIdTamanoVaso(),
                "valor_precio", precio.getValorPrecio(),
                "fecha_inicio_vigencia", precio.getFechaInicioVigencia(),
                "fecha_fin_vigencia", precio.getFechaFinVigencia(),
                "estado", precio.getEstado());
    }

    private record DatosItem(
            CategoriaInventario categoria,
            UnidadMedida unidad,
            TamanoVaso tamanoVaso,
            String tipoControl,
            boolean manejaPaquetes,
            Integer unidadesPorPaquete) {
    }
}
