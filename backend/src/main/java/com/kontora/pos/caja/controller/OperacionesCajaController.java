package com.kontora.pos.caja.controller;

import com.kontora.pos.caja.dto.AdicionDiariaResponse;
import com.kontora.pos.caja.dto.AnularGastoCajaRequest;
import com.kontora.pos.caja.dto.EditarGastoCajaRequest;
import com.kontora.pos.caja.dto.GastoCajaResponse;
import com.kontora.pos.caja.dto.PagoTrabajadoresDiarioResponse;
import com.kontora.pos.caja.dto.RegistrarAdicionDiariaRequest;
import com.kontora.pos.caja.dto.RegistrarGastoCajaRequest;
import com.kontora.pos.caja.dto.RegistrarPagoTrabajadoresDiarioRequest;
import com.kontora.pos.caja.service.OperacionesCajaService;
import com.kontora.pos.common.security.PrincipalUsuario;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/operaciones-caja")
public class OperacionesCajaController {

    private final OperacionesCajaService operacionesCajaService;

    public OperacionesCajaController(OperacionesCajaService operacionesCajaService) {
        this.operacionesCajaService = operacionesCajaService;
    }

    @PostMapping("/adiciones-diarias")
    public AdicionDiariaResponse registrarAdicionDiaria(
            @Valid @RequestBody RegistrarAdicionDiariaRequest request,
            Authentication authentication) {
        return operacionesCajaService.registrarAdicionDiaria(
                request,
                (PrincipalUsuario) authentication.getPrincipal());
    }

    @GetMapping("/adiciones-diarias/abierta")
    public ResponseEntity<AdicionDiariaResponse> obtenerAdicionDiariaCajaAbierta() {
        return operacionesCajaService.obtenerAdicionDiariaCajaAbierta()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/pagos-trabajadores-diarios")
    public PagoTrabajadoresDiarioResponse registrarPagoTrabajadoresDiario(
            @Valid @RequestBody RegistrarPagoTrabajadoresDiarioRequest request,
            Authentication authentication) {
        return operacionesCajaService.registrarPagoTrabajadoresDiario(
                request,
                (PrincipalUsuario) authentication.getPrincipal());
    }

    @PostMapping("/pagos-trabajadores-diarios/{idPagoTrabajadoresDiario}/confirmar")
    public PagoTrabajadoresDiarioResponse confirmarPagoTrabajadoresDiario(
            @PathVariable UUID idPagoTrabajadoresDiario,
            Authentication authentication) {
        return operacionesCajaService.confirmarPagoTrabajadoresDiario(
                idPagoTrabajadoresDiario,
                (PrincipalUsuario) authentication.getPrincipal());
    }

    @GetMapping("/pagos-trabajadores-diarios/abierta")
    public PagoTrabajadoresDiarioResponse obtenerPagoTrabajadoresCajaAbierta() {
        return operacionesCajaService.obtenerPagoTrabajadoresCajaAbierta();
    }

    @PostMapping("/gastos-caja")
    @ResponseStatus(HttpStatus.CREATED)
    public GastoCajaResponse registrarGastoCaja(
            @Valid @RequestBody RegistrarGastoCajaRequest request,
            Authentication authentication) {
        return operacionesCajaService.registrarGastoCaja(
                request,
                (PrincipalUsuario) authentication.getPrincipal());
    }

    @PutMapping("/gastos-caja/{idGastoCaja}")
    public GastoCajaResponse editarGastoCaja(
            @PathVariable UUID idGastoCaja,
            @Valid @RequestBody EditarGastoCajaRequest request,
            Authentication authentication) {
        return operacionesCajaService.editarGastoCaja(
                idGastoCaja,
                request,
                (PrincipalUsuario) authentication.getPrincipal());
    }

    @PostMapping("/gastos-caja/{idGastoCaja}/anular")
    public GastoCajaResponse anularGastoCaja(
            @PathVariable UUID idGastoCaja,
            @Valid @RequestBody AnularGastoCajaRequest request,
            Authentication authentication) {
        return operacionesCajaService.anularGastoCaja(
                idGastoCaja,
                request,
                (PrincipalUsuario) authentication.getPrincipal());
    }

    @GetMapping("/gastos-caja/abierta")
    public List<GastoCajaResponse> listarGastosCajaAbierta() {
        return operacionesCajaService.listarGastosCajaAbierta();
    }
}
