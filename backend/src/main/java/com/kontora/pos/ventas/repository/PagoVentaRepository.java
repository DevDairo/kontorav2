package com.kontora.pos.ventas.repository;

import com.kontora.pos.ventas.domain.PagoVenta;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PagoVentaRepository extends JpaRepository<PagoVenta, UUID> {

    @EntityGraph(attributePaths = "metodoPago")
    List<PagoVenta> findByVenta_IdVenta(UUID idVenta);

    @EntityGraph(attributePaths = {"venta", "venta.usuarioVendedor", "metodoPago", "usuarioValidacion"})
    Optional<PagoVenta> findByIdPagoVenta(UUID idPagoVenta);

    @Query(value = """
            SELECT COALESCE(SUM(pv.valor_pago), 0)
            FROM pagos_venta pv
            JOIN ventas v ON v.id_venta = pv.id_venta
            JOIN metodos_pago mp ON mp.id_metodo_pago = pv.id_metodo_pago
            WHERE v.id_caja_diaria = :idCajaDiaria
            AND v.estado_venta = 'registrada'
            AND mp.nombre_metodo = :nombreMetodo
            """, nativeQuery = true)
    BigDecimal sumarPagosRegistradosPorCajaYMetodo(
            @Param("idCajaDiaria") UUID idCajaDiaria,
            @Param("nombreMetodo") String nombreMetodo);

    @Query(value = """
            SELECT COALESCE(SUM(pv.valor_pago), 0)
            FROM pagos_venta pv
            JOIN ventas v ON v.id_venta = pv.id_venta
            JOIN metodos_pago mp ON mp.id_metodo_pago = pv.id_metodo_pago
            WHERE v.id_caja_diaria = :idCajaDiaria
            AND v.estado_venta = 'registrada'
            AND mp.nombre_metodo = 'transferencia'
            AND pv.estado_validacion = CAST(:estadoValidacion AS estado_validacion_transferencia_enum)
            """, nativeQuery = true)
    BigDecimal sumarTransferenciasRegistradasPorCajaYEstado(
            @Param("idCajaDiaria") UUID idCajaDiaria,
            @Param("estadoValidacion") String estadoValidacion);
}
