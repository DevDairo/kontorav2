package com.kontora.pos.ventas.service;

import com.kontora.pos.auditoria.service.AuditoriaService;
import com.kontora.pos.caja.domain.CajaDiaria;
import com.kontora.pos.caja.repository.CajaDiariaRepository;
import com.kontora.pos.catalogos.repository.MetodoPagoRepository;
import com.kontora.pos.catalogos.repository.PrecioGranizadoRepository;
import com.kontora.pos.catalogos.repository.PromocionRepository;
import com.kontora.pos.common.exception.ApiException;
import com.kontora.pos.common.security.PrincipalUsuario;
import com.kontora.pos.inventario.service.InventarioService;
import com.kontora.pos.usuarios.domain.Usuario;
import com.kontora.pos.usuarios.repository.UsuarioRepository;
import com.kontora.pos.ventas.dto.AnularVentaRequest;
import com.kontora.pos.ventas.dto.RegistrarDetalleVentaRequest;
import com.kontora.pos.ventas.dto.RegistrarPagoVentaRequest;
import com.kontora.pos.ventas.dto.RegistrarVentaRequest;
import com.kontora.pos.ventas.repository.DetalleVentaRepository;
import com.kontora.pos.ventas.repository.PagoVentaRepository;
import com.kontora.pos.ventas.repository.VentaRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VentasServiceTest {

    @Test
    void consultaPreciosConLaFechaOperacionDeLaCajaAbierta() {
        LocalDate fechaCaja = LocalDate.of(2200, 1, 1);
        UUID idUsuario = UUID.randomUUID();
        UUID idTipoGranizado = UUID.randomUUID();
        UUID idTamanoVaso = UUID.randomUUID();
        UUID idMetodoPago = UUID.randomUUID();

        CajaDiariaRepository cajaDiariaRepository = mock(CajaDiariaRepository.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        PrecioGranizadoRepository precioGranizadoRepository = mock(PrecioGranizadoRepository.class);
        CajaDiaria cajaDiaria = mock(CajaDiaria.class);

        when(cajaDiaria.getFechaOperacion()).thenReturn(fechaCaja);
        when(cajaDiariaRepository.findPrimeraPorEstadoCaja("abierta")).thenReturn(Optional.of(cajaDiaria));
        when(usuarioRepository.findById(idUsuario)).thenReturn(Optional.of(mock(Usuario.class)));

        VentasService service = new VentasService(
                cajaDiariaRepository,
                usuarioRepository,
                mock(MetodoPagoRepository.class),
                precioGranizadoRepository,
                mock(PromocionRepository.class),
                mock(VentaRepository.class),
                mock(DetalleVentaRepository.class),
                mock(PagoVentaRepository.class),
                mock(InventarioService.class),
                mock(EntityManager.class),
                mock(AuditoriaService.class));
        PrincipalUsuario principalUsuario = new PrincipalUsuario(
                idUsuario,
                "test_vendedor",
                "Vendedor de prueba",
                "vendedor",
                "token-prueba");
        RegistrarVentaRequest request = new RegistrarVentaRequest(
                "cliente",
                null,
                List.of(new RegistrarDetalleVentaRequest(idTipoGranizado, idTamanoVaso, 1)),
                List.of(new RegistrarPagoVentaRequest(idMetodoPago, new BigDecimal("8000.00"), new BigDecimal("8000.00"))));

        assertThatThrownBy(() -> service.registrarVenta(request, principalUsuario))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getMessage()).isEqualTo("No existe precio vigente para el tipo y tamano indicados");
                });
        verify(precioGranizadoRepository).findPrecioVigente(idTipoGranizado, idTamanoVaso, fechaCaja);
    }

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
