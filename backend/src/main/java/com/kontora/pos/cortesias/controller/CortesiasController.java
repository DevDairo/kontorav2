package com.kontora.pos.cortesias.controller;

import com.kontora.pos.common.security.PrincipalUsuario;
import com.kontora.pos.cortesias.dto.AnularCortesiaRequest;
import com.kontora.pos.cortesias.dto.CortesiaResponse;
import com.kontora.pos.cortesias.dto.RegistrarCortesiaRequest;
import com.kontora.pos.cortesias.service.CortesiasService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/cortesias")
public class CortesiasController {

    private final CortesiasService cortesiasService;

    public CortesiasController(CortesiasService cortesiasService) {
        this.cortesiasService = cortesiasService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CortesiaResponse registrar(
            @Valid @RequestBody RegistrarCortesiaRequest request,
            Authentication authentication) {
        return cortesiasService.registrar(
                request,
                (PrincipalUsuario) authentication.getPrincipal());
    }

    @GetMapping("/caja-abierta")
    public List<CortesiaResponse> consultarCajaAbierta(Authentication authentication) {
        return cortesiasService.consultarCajaAbierta(
                (PrincipalUsuario) authentication.getPrincipal());
    }

    @GetMapping
    public List<CortesiaResponse> consultarPeriodo(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaInicio,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaFin,
            Authentication authentication) {
        return cortesiasService.consultarPeriodo(
                fechaInicio,
                fechaFin,
                (PrincipalUsuario) authentication.getPrincipal());
    }

    @PostMapping("/{idCortesia}/anular")
    public CortesiaResponse anular(
            @PathVariable UUID idCortesia,
            @Valid @RequestBody AnularCortesiaRequest request,
            Authentication authentication) {
        return cortesiasService.anular(
                idCortesia,
                request,
                (PrincipalUsuario) authentication.getPrincipal());
    }
}
