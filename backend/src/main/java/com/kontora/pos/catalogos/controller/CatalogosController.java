package com.kontora.pos.catalogos.controller;

import com.kontora.pos.catalogos.dto.CatalogoBasicoResponse;
import com.kontora.pos.catalogos.dto.ItemInventarioResponse;
import com.kontora.pos.catalogos.dto.PrecioGranizadoResponse;
import com.kontora.pos.catalogos.dto.PromocionResponse;
import com.kontora.pos.catalogos.dto.TamanoVasoResponse;
import com.kontora.pos.catalogos.dto.UnidadMedidaResponse;
import com.kontora.pos.catalogos.service.CatalogosService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/catalogos")
public class CatalogosController {

    private final CatalogosService catalogosService;

    public CatalogosController(CatalogosService catalogosService) {
        this.catalogosService = catalogosService;
    }

    @GetMapping("/roles")
    public List<CatalogoBasicoResponse> roles() {
        return catalogosService.obtenerRoles();
    }

    @GetMapping("/metodos-pago")
    public List<CatalogoBasicoResponse> metodosPago() {
        return catalogosService.obtenerMetodosPago();
    }

    @GetMapping("/tipos-granizado")
    public List<CatalogoBasicoResponse> tiposGranizado() {
        return catalogosService.obtenerTiposGranizado();
    }

    @GetMapping("/tamanos-vaso")
    public List<TamanoVasoResponse> tamanosVaso() {
        return catalogosService.obtenerTamanosVaso();
    }

    @GetMapping("/categorias-inventario")
    public List<CatalogoBasicoResponse> categoriasInventario() {
        return catalogosService.obtenerCategoriasInventario();
    }

    @GetMapping("/unidades-medida")
    public List<UnidadMedidaResponse> unidadesMedida() {
        return catalogosService.obtenerUnidadesMedida();
    }

    @GetMapping("/items-inventario")
    public List<ItemInventarioResponse> itemsInventario() {
        return catalogosService.obtenerItemsInventario();
    }

    @GetMapping("/precios-granizado/vigentes")
    public List<PrecioGranizadoResponse> preciosVigentes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return catalogosService.obtenerPreciosVigentes(fecha == null ? LocalDate.now() : fecha);
    }

    @GetMapping("/promociones/vigentes")
    public List<PromocionResponse> promocionesVigentes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return catalogosService.obtenerPromocionesVigentes(fecha == null ? LocalDate.now() : fecha);
    }

    @GetMapping("/tipos-servicio")
    public List<CatalogoBasicoResponse> tiposServicio() {
        return catalogosService.obtenerTiposServicio();
    }
}
