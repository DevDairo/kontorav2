package com.kontora.pos.deposito.controller;

import com.kontora.pos.common.security.PrincipalUsuario;
import com.kontora.pos.deposito.dto.ConsignacionBancariaResponse;
import com.kontora.pos.deposito.dto.PagoServicioResponse;
import com.kontora.pos.deposito.dto.RegistrarConsignacionBancariaRequest;
import com.kontora.pos.deposito.dto.RegistrarPagoServicioRequest;
import com.kontora.pos.deposito.dto.SaldoDepositoResponse;
import com.kontora.pos.deposito.service.DepositoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/deposito")
public class DepositoController {

    private final DepositoService depositoService;

    public DepositoController(DepositoService depositoService) {
        this.depositoService = depositoService;
    }

    @GetMapping("/saldo")
    public SaldoDepositoResponse obtenerSaldo(Authentication authentication) {
        return depositoService.obtenerSaldoActual((PrincipalUsuario) authentication.getPrincipal());
    }

    @PostMapping("/consignaciones-bancarias")
    @ResponseStatus(HttpStatus.CREATED)
    public ConsignacionBancariaResponse registrarConsignacion(
            @Valid @RequestBody RegistrarConsignacionBancariaRequest request,
            Authentication authentication) {
        return depositoService.registrarConsignacion(
                request,
                (PrincipalUsuario) authentication.getPrincipal());
    }

    @PostMapping("/pagos-servicios")
    @ResponseStatus(HttpStatus.CREATED)
    public PagoServicioResponse registrarPagoServicio(
            @Valid @RequestBody RegistrarPagoServicioRequest request,
            Authentication authentication) {
        return depositoService.registrarPagoServicio(
                request,
                (PrincipalUsuario) authentication.getPrincipal());
    }
}
