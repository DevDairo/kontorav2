package com.kontora.pos.catalogos.controller;

import com.kontora.pos.catalogos.dto.ActualizarEstadoItemInventarioRequest;
import com.kontora.pos.catalogos.dto.ActualizarItemInventarioRequest;
import com.kontora.pos.catalogos.dto.CrearItemInventarioRequest;
import com.kontora.pos.catalogos.dto.CrearPrecioGranizadoRequest;
import com.kontora.pos.catalogos.dto.ItemInventarioResponse;
import com.kontora.pos.catalogos.dto.PrecioGranizadoResponse;
import com.kontora.pos.catalogos.service.GestionCatalogosService;
import com.kontora.pos.common.security.PrincipalUsuario;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalogos/gestion")
public class GestionCatalogosController {

    private final GestionCatalogosService gestionCatalogosService;

    public GestionCatalogosController(GestionCatalogosService gestionCatalogosService) {
        this.gestionCatalogosService = gestionCatalogosService;
    }

    @GetMapping("/items-inventario")
    public List<ItemInventarioResponse> listarItems(Authentication authentication) {
        return gestionCatalogosService.listarItemsParaGestion(principal(authentication));
    }

    @PostMapping("/items-inventario")
    public ResponseEntity<ItemInventarioResponse> crearItem(
            @Valid @RequestBody CrearItemInventarioRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(gestionCatalogosService.crearItem(request, principal(authentication)));
    }

    @PutMapping("/items-inventario/{idItemInventario}")
    public ItemInventarioResponse actualizarItem(
            @PathVariable UUID idItemInventario,
            @Valid @RequestBody ActualizarItemInventarioRequest request,
            Authentication authentication) {
        return gestionCatalogosService.actualizarItem(idItemInventario, request, principal(authentication));
    }

    @PutMapping("/items-inventario/{idItemInventario}/estado")
    public ItemInventarioResponse actualizarEstadoItem(
            @PathVariable UUID idItemInventario,
            @Valid @RequestBody ActualizarEstadoItemInventarioRequest request,
            Authentication authentication) {
        return gestionCatalogosService.actualizarEstadoItem(idItemInventario, request, principal(authentication));
    }

    @GetMapping("/precios-granizado")
    public List<PrecioGranizadoResponse> listarPrecios(Authentication authentication) {
        return gestionCatalogosService.listarPreciosParaGestion(principal(authentication));
    }

    @PostMapping("/precios-granizado")
    public ResponseEntity<PrecioGranizadoResponse> crearPrecio(
            @Valid @RequestBody CrearPrecioGranizadoRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(gestionCatalogosService.crearPrecio(request, principal(authentication)));
    }

    private PrincipalUsuario principal(Authentication authentication) {
        return (PrincipalUsuario) authentication.getPrincipal();
    }
}
