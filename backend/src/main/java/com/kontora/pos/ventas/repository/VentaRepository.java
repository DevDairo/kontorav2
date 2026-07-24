package com.kontora.pos.ventas.repository;

import com.kontora.pos.ventas.domain.Venta;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface VentaRepository extends JpaRepository<Venta, UUID> {

    @EntityGraph(attributePaths = {"cajaDiaria", "usuarioVendedor", "usuarioComprador"})
    Optional<Venta> findByIdVenta(UUID idVenta);

    @Query(value = """
            SELECT COALESCE(SUM(total_venta), 0)
            FROM ventas
            WHERE id_caja_diaria = :idCajaDiaria
            AND estado_venta = 'registrada'
            """, nativeQuery = true)
    BigDecimal sumarVentasRegistradasPorCaja(@Param("idCajaDiaria") UUID idCajaDiaria);
}
