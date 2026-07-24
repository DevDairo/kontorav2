package com.kontora.pos.pagos.controller;

import com.kontora.pos.common.security.PrincipalUsuario;
import com.kontora.pos.pagos.dto.ValidarTransferenciaRequest;
import com.kontora.pos.pagos.service.PagosVentaService;
import com.kontora.pos.ventas.dto.PagoVentaResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/pagos-venta")
public class PagosVentaController {

    private final PagosVentaService pagosVentaService;

    public PagosVentaController(PagosVentaService pagosVentaService) {
        this.pagosVentaService = pagosVentaService;
    }

    @PostMapping("/{idPagoVenta}/validar")
    public PagoVentaResponse validarTransferencia(
            @PathVariable UUID idPagoVenta,
            @Valid @RequestBody(required = false) ValidarTransferenciaRequest request,
            Authentication authentication) {
        return pagosVentaService.validarTransferencia(
                idPagoVenta,
                request,
                (PrincipalUsuario) authentication.getPrincipal());
    }

    @PostMapping("/{idPagoVenta}/rechazar")
    public PagoVentaResponse rechazarTransferencia(
            @PathVariable UUID idPagoVenta,
            @Valid @RequestBody(required = false) ValidarTransferenciaRequest request,
            Authentication authentication) {
        return pagosVentaService.rechazarTransferencia(
                idPagoVenta,
                request,
                (PrincipalUsuario) authentication.getPrincipal());
    }
}
