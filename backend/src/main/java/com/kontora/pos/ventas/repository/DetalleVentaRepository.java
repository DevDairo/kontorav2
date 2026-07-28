package com.kontora.pos.ventas.repository;

import com.kontora.pos.ventas.domain.DetalleVenta;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DetalleVentaRepository extends JpaRepository<DetalleVenta, UUID> {

    @EntityGraph(attributePaths = {"tipoGranizado", "tamanoVaso", "promocionAplicada"})
    List<DetalleVenta> findByVenta_IdVenta(UUID idVenta);

    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                FROM detalles_venta dv
                JOIN ventas v ON v.id_venta = dv.id_venta
                JOIN promociones p ON p.id_promocion = dv.id_promocion_aplicada
                WHERE v.id_caja_diaria = :idCajaDiaria
                  AND v.id_usuario_comprador = :idUsuarioComprador
                  AND v.estado_venta = 'registrada'
                  AND p.tipo_beneficiario IN ('trabajador', 'todos')
                  AND dv.cantidad_con_promocion > 0
            )
            """, nativeQuery = true)
    boolean existeBeneficioTrabajadorAplicado(
            @Param("idCajaDiaria") UUID idCajaDiaria,
            @Param("idUsuarioComprador") UUID idUsuarioComprador);
}
