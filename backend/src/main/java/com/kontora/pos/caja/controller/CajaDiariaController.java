package com.kontora.pos.caja.controller;

import com.kontora.pos.caja.dto.AbrirCajaDiariaRequest;
import com.kontora.pos.caja.dto.CajaDiariaResponse;
import com.kontora.pos.caja.dto.CerrarCajaRequest;
import com.kontora.pos.caja.dto.CierreCajaResponse;
import com.kontora.pos.caja.dto.ResumenCajaDiariaResponse;
import com.kontora.pos.caja.service.CajaDiariaService;
import com.kontora.pos.caja.service.CierreCajaService;
import com.kontora.pos.common.security.PrincipalUsuario;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/cajas-diarias")
public class CajaDiariaController {

    private final CajaDiariaService cajaDiariaService;
    private final CierreCajaService cierreCajaService;

    public CajaDiariaController(CajaDiariaService cajaDiariaService, CierreCajaService cierreCajaService) {
        this.cajaDiariaService = cajaDiariaService;
        this.cierreCajaService = cierreCajaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CajaDiariaResponse abrirCaja(
            @Valid @RequestBody AbrirCajaDiariaRequest request,
            Authentication authentication) {
        return cajaDiariaService.abrirCaja(request, (PrincipalUsuario) authentication.getPrincipal());
    }

    @GetMapping("/abierta")
    public CajaDiariaResponse obtenerCajaAbierta() {
        return cajaDiariaService.obtenerCajaAbierta();
    }

    @GetMapping("/abierta/resumen")
    public ResumenCajaDiariaResponse obtenerResumenCajaAbierta(Authentication authentication) {
        return cierreCajaService.obtenerResumenCajaAbierta((PrincipalUsuario) authentication.getPrincipal());
    }

    @GetMapping("/fecha/{fechaOperacion}")
    public CajaDiariaResponse obtenerCajaPorFecha(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaOperacion) {
        return cajaDiariaService.obtenerCajaPorFecha(fechaOperacion);
    }

    @PostMapping("/{idCajaDiaria}/cerrar")
    public CierreCajaResponse cerrarCaja(
            @PathVariable UUID idCajaDiaria,
            @Valid @RequestBody CerrarCajaRequest request,
            Authentication authentication) {
        return cierreCajaService.cerrarCaja(
                idCajaDiaria,
                request,
                (PrincipalUsuario) authentication.getPrincipal());
    }

    @GetMapping("/{idCajaDiaria}/cierre")
    public CierreCajaResponse obtenerCierrePorCaja(@PathVariable UUID idCajaDiaria) {
        return cierreCajaService.obtenerCierrePorCaja(idCajaDiaria);
    }
}
