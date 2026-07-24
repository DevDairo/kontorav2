package com.kontora.pos.inventario.controller;

import com.kontora.pos.common.security.PrincipalUsuario;
import com.kontora.pos.inventario.dto.AjusteInventarioResponse;
import com.kontora.pos.inventario.dto.ConsumoDiarioInventarioResponse;
import com.kontora.pos.inventario.dto.ExistenciaInventarioDiarioResponse;
import com.kontora.pos.inventario.dto.ExistenciaInventarioGeneralResponse;
import com.kontora.pos.inventario.dto.MovimientoInventarioResponse;
import com.kontora.pos.inventario.dto.PaqueteVasosAbiertoResponse;
import com.kontora.pos.inventario.dto.RegistrarConsumoDiarioInventarioRequest;
import com.kontora.pos.inventario.dto.RegistrarPaqueteVasosRequest;
import com.kontora.pos.inventario.dto.ResolverAjusteInventarioRequest;
import com.kontora.pos.inventario.dto.SolicitarAjusteInventarioRequest;
import com.kontora.pos.inventario.dto.VentasVasosDiariasResponse;
import com.kontora.pos.inventario.service.InventarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/inventario")
public class InventarioController {

    private final InventarioService inventarioService;

    public InventarioController(InventarioService inventarioService) {
        this.inventarioService = inventarioService;
    }

    @GetMapping("/existencias/general")
    public List<ExistenciaInventarioGeneralResponse> consultarExistenciasGenerales() {
        return inventarioService.consultarExistenciasGenerales();
    }

    @GetMapping("/existencias/diarias/abierta")
    public List<ExistenciaInventarioDiarioResponse> consultarExistenciasDiariasCajaAbierta() {
        return inventarioService.consultarExistenciasDiariasCajaAbierta();
    }

    @GetMapping("/ventas-vasos/diaria-abierta")
    public List<VentasVasosDiariasResponse> consultarVentasVasosDiariaAbierta(Authentication authentication) {
        return inventarioService.consultarVentasVasosDiariaAbierta(
                (PrincipalUsuario) authentication.getPrincipal());
    }

    @GetMapping("/existencias/diarias/caja/{idCajaDiaria}")
    public List<ExistenciaInventarioDiarioResponse> consultarExistenciasDiariasPorCaja(
            @PathVariable UUID idCajaDiaria) {
        return inventarioService.consultarExistenciasDiariasPorCaja(idCajaDiaria);
    }

    @PostMapping("/paquetes-vasos")
    @ResponseStatus(HttpStatus.CREATED)
    public PaqueteVasosAbiertoResponse registrarPaqueteVasos(
            @Valid @RequestBody RegistrarPaqueteVasosRequest request,
            Authentication authentication) {
        return inventarioService.registrarPaqueteVasos(
                request,
                (PrincipalUsuario) authentication.getPrincipal());
    }

    @PostMapping("/consumos-diarios")
    @ResponseStatus(HttpStatus.CREATED)
    public ConsumoDiarioInventarioResponse registrarConsumoDiario(
            @Valid @RequestBody RegistrarConsumoDiarioInventarioRequest request,
            Authentication authentication) {
        return inventarioService.registrarConsumoDiario(
                request,
                (PrincipalUsuario) authentication.getPrincipal());
    }

    @GetMapping("/movimientos")
    public List<MovimientoInventarioResponse> consultarMovimientos(
            @RequestParam(required = false) UUID idCajaDiaria,
            @RequestParam(required = false) UUID idItemInventario) {
        return inventarioService.consultarMovimientos(idCajaDiaria, idItemInventario);
    }

    @GetMapping("/ajustes")
    public List<AjusteInventarioResponse> consultarAjustes(
            @RequestParam(required = false) String estadoAprobacion,
            Authentication authentication) {
        return inventarioService.consultarAjustes(
                estadoAprobacion,
                (PrincipalUsuario) authentication.getPrincipal());
    }

    @PostMapping("/ajustes")
    @ResponseStatus(HttpStatus.CREATED)
    public AjusteInventarioResponse solicitarAjuste(
            @Valid @RequestBody SolicitarAjusteInventarioRequest request,
            Authentication authentication) {
        return inventarioService.solicitarAjusteInventario(
                request,
                (PrincipalUsuario) authentication.getPrincipal());
    }

    @PostMapping("/ajustes/{idAjusteInventario}/aprobar")
    public AjusteInventarioResponse aprobarAjuste(
            @PathVariable UUID idAjusteInventario,
            @Valid @RequestBody(required = false) ResolverAjusteInventarioRequest request,
            Authentication authentication) {
        return inventarioService.aprobarAjusteInventario(
                idAjusteInventario,
                request,
                (PrincipalUsuario) authentication.getPrincipal());
    }

    @PostMapping("/ajustes/{idAjusteInventario}/rechazar")
    public AjusteInventarioResponse rechazarAjuste(
            @PathVariable UUID idAjusteInventario,
            @Valid @RequestBody(required = false) ResolverAjusteInventarioRequest request,
            Authentication authentication) {
        return inventarioService.rechazarAjusteInventario(
                idAjusteInventario,
                request,
                (PrincipalUsuario) authentication.getPrincipal());
    }
}
