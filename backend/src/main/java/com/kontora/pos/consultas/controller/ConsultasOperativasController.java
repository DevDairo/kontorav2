package com.kontora.pos.consultas.controller;

import com.kontora.pos.common.security.PrincipalUsuario;
import com.kontora.pos.consultas.dto.ConsultaAuditoriaResponse;
import com.kontora.pos.consultas.dto.ConsultaCierreDiarioResponse;
import com.kontora.pos.consultas.dto.ConsultaGastoCajaResponse;
import com.kontora.pos.consultas.dto.ConsultaInventarioActualResponse;
import com.kontora.pos.consultas.dto.ConsultaMovimientoDepositoResponse;
import com.kontora.pos.consultas.dto.ConsultaMovimientoInventarioResponse;
import com.kontora.pos.consultas.dto.ConsultaTransferenciaResponse;
import com.kontora.pos.consultas.dto.ConsultaVentaResponse;
import com.kontora.pos.consultas.dto.ConsultaVentasVasosResponse;
import com.kontora.pos.consultas.service.ConsultasOperativasService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/consultas")
public class ConsultasOperativasController {

    private final ConsultasOperativasService consultasService;

    public ConsultasOperativasController(ConsultasOperativasService consultasService) {
        this.consultasService = consultasService;
    }

    @GetMapping("/ventas")
    public List<ConsultaVentaResponse> consultarVentas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            Authentication authentication) {
        return consultasService.consultarVentas(
                fechaInicio,
                fechaFin,
                (PrincipalUsuario) authentication.getPrincipal());
    }

    @GetMapping("/cierre")
    public ConsultaCierreDiarioResponse consultarCierrePorFecha(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            Authentication authentication) {
        return consultasService.consultarCierrePorFecha(
                fecha,
                (PrincipalUsuario) authentication.getPrincipal());
    }

    @GetMapping("/gastos")
    public List<ConsultaGastoCajaResponse> consultarGastos(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            Authentication authentication) {
        return consultasService.consultarGastos(
                fechaInicio,
                fechaFin,
                (PrincipalUsuario) authentication.getPrincipal());
    }

    @GetMapping("/inventario/actual")
    public List<ConsultaInventarioActualResponse> consultarInventarioActual() {
        return consultasService.consultarInventarioActual();
    }

    @GetMapping("/inventario/movimientos")
    public List<ConsultaMovimientoInventarioResponse> consultarMovimientosInventario(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) UUID idCajaDiaria,
            @RequestParam(required = false) UUID idItemInventario) {
        return consultasService.consultarMovimientosInventario(
                fechaInicio,
                fechaFin,
                idCajaDiaria,
                idItemInventario);
    }

    @GetMapping("/inventario/ventas-vasos")
    public List<ConsultaVentasVasosResponse> consultarVentasVasos(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            Authentication authentication) {
        return consultasService.consultarVentasVasos(
                fechaInicio,
                fechaFin,
                (PrincipalUsuario) authentication.getPrincipal());
    }

    @GetMapping("/deposito/movimientos")
    public List<ConsultaMovimientoDepositoResponse> consultarMovimientosDeposito(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            Authentication authentication) {
        return consultasService.consultarMovimientosDeposito(
                fechaInicio,
                fechaFin,
                (PrincipalUsuario) authentication.getPrincipal());
    }

    @GetMapping("/transferencias")
    public List<ConsultaTransferenciaResponse> consultarTransferencias(
            @RequestParam(required = false) String estadoValidacion,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            Authentication authentication) {
        return consultasService.consultarTransferencias(
                estadoValidacion,
                fechaInicio,
                fechaFin,
                (PrincipalUsuario) authentication.getPrincipal());
    }

    @GetMapping("/auditoria")
    public List<ConsultaAuditoriaResponse> consultarAuditoria(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) String tablaAfectada,
            @RequestParam(required = false) String accion,
            Authentication authentication) {
        return consultasService.consultarAuditoria(
                fechaInicio,
                fechaFin,
                tablaAfectada,
                accion,
                (PrincipalUsuario) authentication.getPrincipal());
    }
}
