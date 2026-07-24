package com.kontora.pos.ventas.service;

import com.kontora.pos.auditoria.service.AuditoriaService;
import com.kontora.pos.caja.repository.CajaDiariaRepository;
import com.kontora.pos.catalogos.repository.MetodoPagoRepository;
import com.kontora.pos.catalogos.repository.PrecioGranizadoRepository;
import com.kontora.pos.catalogos.repository.PromocionRepository;
import com.kontora.pos.common.exception.ApiException;
import com.kontora.pos.common.security.PrincipalUsuario;
import com.kontora.pos.inventario.service.InventarioService;
import com.kontora.pos.usuarios.repository.UsuarioRepository;
import com.kontora.pos.ventas.dto.AnularVentaRequest;
import com.kontora.pos.ventas.repository.DetalleVentaRepository;
import com.kontora.pos.ventas.repository.PagoVentaRepository;
import com.kontora.pos.ventas.repository.VentaRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VentasServiceTest {

    @Test
    void noRegistraVentaSinCajaAbierta() {
        CajaDiariaRepository cajaDiariaRepository = mock(CajaDiariaRepository.class);
        when(cajaDiariaRepository.findPrimeraPorEstadoCaja("abierta")).thenReturn(Optional.empty());
        VentasService service = new VentasService(
                cajaDiariaRepository,
                mock(UsuarioRepository.class),
                mock(MetodoPagoRepository.class),
                mock(PrecioGranizadoRepository.class),
                mock(PromocionRepository.class),
                mock(VentaRepository.class),
                mock(DetalleVentaRepository.class),
                mock(PagoVentaRepository.class),
                mock(InventarioService.class),
                mock(EntityManager.class),
                mock(AuditoriaService.class));
        PrincipalUsuario principalUsuario = new PrincipalUsuario(
                UUID.randomUUID(),
                "test_vendedor",
                "Vendedor de prueba",
                "vendedor",
                "token-prueba");

        assertThatThrownBy(() -> service.registrarVenta(null, principalUsuario))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getMessage()).isEqualTo("No existe caja diaria abierta para registrar venta");
                });
    }

    @Test
    void vendedorNoPuedeAnularVenta() {
        VentasService service = new VentasService(
                mock(CajaDiariaRepository.class),
                mock(UsuarioRepository.class),
                mock(MetodoPagoRepository.class),
                mock(PrecioGranizadoRepository.class),
                mock(PromocionRepository.class),
                mock(VentaRepository.class),
                mock(DetalleVentaRepository.class),
                mock(PagoVentaRepository.class),
                mock(InventarioService.class),
                mock(EntityManager.class),
                mock(AuditoriaService.class));
        PrincipalUsuario principalUsuario = new PrincipalUsuario(
                UUID.randomUUID(),
                "test_vendedor",
                "Vendedor de prueba",
                "vendedor",
                "token-prueba");

        assertThatThrownBy(() -> service.anularVenta(UUID.randomUUID(), new AnularVentaRequest("Registro duplicado"), principalUsuario))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(exception.getMessage()).isEqualTo("Solo administrador o gerente puede anular ventas");
                });
    }
}
