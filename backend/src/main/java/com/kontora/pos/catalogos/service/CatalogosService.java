package com.kontora.pos.catalogos.service;

import com.kontora.pos.catalogos.domain.CategoriaInventario;
import com.kontora.pos.catalogos.domain.DiaPromocion;
import com.kontora.pos.catalogos.domain.ItemInventario;
import com.kontora.pos.catalogos.domain.MetodoPago;
import com.kontora.pos.catalogos.domain.PrecioGranizado;
import com.kontora.pos.catalogos.domain.Promocion;
import com.kontora.pos.catalogos.domain.TamanoVaso;
import com.kontora.pos.catalogos.domain.TipoGranizado;
import com.kontora.pos.catalogos.domain.TipoServicio;
import com.kontora.pos.catalogos.domain.UnidadMedida;
import com.kontora.pos.catalogos.dto.CatalogoBasicoResponse;
import com.kontora.pos.catalogos.dto.ItemInventarioResponse;
import com.kontora.pos.catalogos.dto.PrecioGranizadoResponse;
import com.kontora.pos.catalogos.dto.PromocionResponse;
import com.kontora.pos.catalogos.dto.TamanoVasoResponse;
import com.kontora.pos.catalogos.dto.UnidadMedidaResponse;
import com.kontora.pos.catalogos.repository.CategoriaInventarioRepository;
import com.kontora.pos.catalogos.repository.ItemInventarioRepository;
import com.kontora.pos.catalogos.repository.MetodoPagoRepository;
import com.kontora.pos.catalogos.repository.PrecioGranizadoRepository;
import com.kontora.pos.catalogos.repository.PromocionRepository;
import com.kontora.pos.catalogos.repository.TamanoVasoRepository;
import com.kontora.pos.catalogos.repository.TipoGranizadoRepository;
import com.kontora.pos.catalogos.repository.TipoServicioRepository;
import com.kontora.pos.catalogos.repository.UnidadMedidaRepository;
import com.kontora.pos.usuarios.domain.Rol;
import com.kontora.pos.usuarios.repository.RolRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
public class CatalogosService {

    private final RolRepository rolRepository;
    private final MetodoPagoRepository metodoPagoRepository;
    private final TipoGranizadoRepository tipoGranizadoRepository;
    private final TamanoVasoRepository tamanoVasoRepository;
    private final CategoriaInventarioRepository categoriaInventarioRepository;
    private final UnidadMedidaRepository unidadMedidaRepository;
    private final ItemInventarioRepository itemInventarioRepository;
    private final PrecioGranizadoRepository precioGranizadoRepository;
    private final PromocionRepository promocionRepository;
    private final TipoServicioRepository tipoServicioRepository;

    public CatalogosService(
            RolRepository rolRepository,
            MetodoPagoRepository metodoPagoRepository,
            TipoGranizadoRepository tipoGranizadoRepository,
            TamanoVasoRepository tamanoVasoRepository,
            CategoriaInventarioRepository categoriaInventarioRepository,
            UnidadMedidaRepository unidadMedidaRepository,
            ItemInventarioRepository itemInventarioRepository,
            PrecioGranizadoRepository precioGranizadoRepository,
            PromocionRepository promocionRepository,
            TipoServicioRepository tipoServicioRepository) {
        this.rolRepository = rolRepository;
        this.metodoPagoRepository = metodoPagoRepository;
        this.tipoGranizadoRepository = tipoGranizadoRepository;
        this.tamanoVasoRepository = tamanoVasoRepository;
        this.categoriaInventarioRepository = categoriaInventarioRepository;
        this.unidadMedidaRepository = unidadMedidaRepository;
        this.itemInventarioRepository = itemInventarioRepository;
        this.precioGranizadoRepository = precioGranizadoRepository;
        this.promocionRepository = promocionRepository;
        this.tipoServicioRepository = tipoServicioRepository;
    }

    @Transactional(readOnly = true)
    public List<CatalogoBasicoResponse> obtenerRoles() {
        return rolRepository.findActivos().stream()
                .map(this::toCatalogoBasico)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CatalogoBasicoResponse> obtenerMetodosPago() {
        return metodoPagoRepository.findActivos().stream()
                .map(this::toCatalogoBasico)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CatalogoBasicoResponse> obtenerTiposGranizado() {
        return tipoGranizadoRepository.findActivos().stream()
                .map(this::toCatalogoBasico)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TamanoVasoResponse> obtenerTamanosVaso() {
        return tamanoVasoRepository.findActivos().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CatalogoBasicoResponse> obtenerCategoriasInventario() {
        return categoriaInventarioRepository.findActivas().stream()
                .map(this::toCatalogoBasico)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UnidadMedidaResponse> obtenerUnidadesMedida() {
        return unidadMedidaRepository.findActivas().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ItemInventarioResponse> obtenerItemsInventario() {
        return itemInventarioRepository.findActivosParaOperacion().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PrecioGranizadoResponse> obtenerPreciosVigentes(LocalDate fecha) {
        return precioGranizadoRepository.findVigentes(fecha).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PromocionResponse> obtenerPromocionesVigentes(LocalDate fecha) {
        return promocionRepository.findVigentes(fecha).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CatalogoBasicoResponse> obtenerTiposServicio() {
        return tipoServicioRepository.findActivos().stream()
                .map(this::toCatalogoBasico)
                .toList();
    }

    private CatalogoBasicoResponse toCatalogoBasico(Rol rol) {
        return new CatalogoBasicoResponse(rol.getIdRol(), rol.getNombreRol(), rol.getEstado());
    }

    private CatalogoBasicoResponse toCatalogoBasico(MetodoPago metodoPago) {
        return new CatalogoBasicoResponse(metodoPago.getIdMetodoPago(), metodoPago.getNombreMetodo(), metodoPago.getEstado());
    }

    private CatalogoBasicoResponse toCatalogoBasico(TipoGranizado tipoGranizado) {
        return new CatalogoBasicoResponse(tipoGranizado.getIdTipoGranizado(), tipoGranizado.getNombreTipo(), tipoGranizado.getEstado());
    }

    private CatalogoBasicoResponse toCatalogoBasico(CategoriaInventario categoriaInventario) {
        return new CatalogoBasicoResponse(
                categoriaInventario.getIdCategoriaInventario(),
                categoriaInventario.getNombreCategoria(),
                categoriaInventario.getEstado());
    }

    private CatalogoBasicoResponse toCatalogoBasico(TipoServicio tipoServicio) {
        return new CatalogoBasicoResponse(
                tipoServicio.getIdTipoServicio(),
                tipoServicio.getNombreServicio(),
                tipoServicio.getEstado());
    }

    private TamanoVasoResponse toResponse(TamanoVaso tamanoVaso) {
        return new TamanoVasoResponse(tamanoVaso.getIdTamanoVaso(), tamanoVaso.getOnzas(), tamanoVaso.getEstado());
    }

    private UnidadMedidaResponse toResponse(UnidadMedida unidadMedida) {
        return new UnidadMedidaResponse(
                unidadMedida.getIdUnidadMedida(),
                unidadMedida.getNombreUnidad(),
                unidadMedida.getAbreviatura(),
                unidadMedida.getEstado());
    }

    private ItemInventarioResponse toResponse(ItemInventario itemInventario) {
        TamanoVaso tamanoVaso = itemInventario.getTamanoVaso();
        return new ItemInventarioResponse(
                itemInventario.getIdItemInventario(),
                itemInventario.getNombreItem(),
                itemInventario.getTipoControl(),
                itemInventario.isManejaPaquetes(),
                itemInventario.getUnidadesPorPaquete(),
                itemInventario.getEstado(),
                itemInventario.getFechaCreacion(),
                itemInventario.getCategoriaInventario().getIdCategoriaInventario(),
                itemInventario.getCategoriaInventario().getNombreCategoria(),
                itemInventario.getUnidadMedida().getIdUnidadMedida(),
                itemInventario.getUnidadMedida().getNombreUnidad(),
                itemInventario.getUnidadMedida().getAbreviatura(),
                tamanoVaso == null ? null : tamanoVaso.getIdTamanoVaso(),
                tamanoVaso == null ? null : tamanoVaso.getOnzas());
    }

    private PrecioGranizadoResponse toResponse(PrecioGranizado precioGranizado) {
        return new PrecioGranizadoResponse(
                precioGranizado.getIdPrecioGranizado(),
                precioGranizado.getTipoGranizado().getIdTipoGranizado(),
                precioGranizado.getTipoGranizado().getNombreTipo(),
                precioGranizado.getTamanoVaso().getIdTamanoVaso(),
                precioGranizado.getTamanoVaso().getOnzas(),
                precioGranizado.getValorPrecio(),
                precioGranizado.getFechaInicioVigencia(),
                precioGranizado.getFechaFinVigencia(),
                precioGranizado.getEstado());
    }

    private PromocionResponse toResponse(Promocion promocion) {
        return new PromocionResponse(
                promocion.getIdPromocion(),
                promocion.getNombrePromocion(),
                promocion.getTipoGranizado().getIdTipoGranizado(),
                promocion.getTipoGranizado().getNombreTipo(),
                promocion.getTamanoVaso().getIdTamanoVaso(),
                promocion.getTamanoVaso().getOnzas(),
                promocion.getTipoBeneficiario(),
                promocion.getCantidadRequerida(),
                promocion.getValorPromocional(),
                promocion.getFechaInicioVigencia(),
                promocion.getFechaFinVigencia(),
                promocion.getEstado(),
                promocion.getDiasPromocion().stream()
                        .map(DiaPromocion::getDiaSemana)
                        .sorted(Comparator.comparingInt(this::ordenDiaSemana))
                        .toList());
    }

    private int ordenDiaSemana(String diaSemana) {
        return switch (diaSemana) {
            case "lunes" -> 1;
            case "martes" -> 2;
            case "miercoles" -> 3;
            case "jueves" -> 4;
            case "viernes" -> 5;
            case "sabado" -> 6;
            case "domingo" -> 7;
            default -> 8;
        };
    }
}
