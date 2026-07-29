package com.kontora.pos.inventario.controller;

import com.kontora.pos.common.security.PrincipalUsuario;
import com.kontora.pos.inventario.dto.AnularPerdidaInventarioRequest;
import com.kontora.pos.inventario.dto.PerdidaInventarioResponse;
import com.kontora.pos.inventario.dto.RegistrarPerdidaInventarioRequest;
import com.kontora.pos.inventario.service.PerdidasInventarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/inventario/perdidas-vasos")
public class PerdidasInventarioController {

    private final PerdidasInventarioService perdidasInventarioService;

    public PerdidasInventarioController(PerdidasInventarioService perdidasInventarioService) {
        this.perdidasInventarioService = perdidasInventarioService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PerdidaInventarioResponse registrar(
            @Valid @RequestBody RegistrarPerdidaInventarioRequest request,
            Authentication authentication) {
        return perdidasInventarioService.registrar(
                request,
                (PrincipalUsuario) authentication.getPrincipal());
    }

    @GetMapping("/caja-abierta")
    public List<PerdidaInventarioResponse> consultarCajaAbierta(Authentication authentication) {
        return perdidasInventarioService.consultarCajaAbierta(
                (PrincipalUsuario) authentication.getPrincipal());
    }

    @GetMapping
    public List<PerdidaInventarioResponse> consultarPeriodo(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaInicio,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaFin,
            Authentication authentication) {
        return perdidasInventarioService.consultarPeriodo(
                fechaInicio,
                fechaFin,
                (PrincipalUsuario) authentication.getPrincipal());
    }

    @PostMapping("/{idPerdidaInventario}/anular")
    public PerdidaInventarioResponse anular(
            @PathVariable UUID idPerdidaInventario,
            @Valid @RequestBody AnularPerdidaInventarioRequest request,
            Authentication authentication) {
        return perdidasInventarioService.anular(
                idPerdidaInventario,
                request,
                (PrincipalUsuario) authentication.getPrincipal());
    }
}
