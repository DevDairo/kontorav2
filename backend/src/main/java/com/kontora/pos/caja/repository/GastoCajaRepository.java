package com.kontora.pos.caja.repository;

import com.kontora.pos.caja.domain.GastoCaja;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GastoCajaRepository extends JpaRepository<GastoCaja, UUID> {

    @EntityGraph(attributePaths = {
            "cajaDiaria",
            "usuarioRegistro",
            "usuarioUltimaEdicion",
            "usuarioAnulacion"
    })
    List<GastoCaja> findByCajaDiaria_IdCajaDiariaOrderByFechaRegistroDesc(UUID idCajaDiaria);

    @EntityGraph(attributePaths = {
            "cajaDiaria",
            "usuarioRegistro",
            "usuarioUltimaEdicion",
            "usuarioAnulacion"
    })
    Optional<GastoCaja> findByIdGastoCaja(UUID idGastoCaja);

    @Query(value = """
            SELECT COALESCE(SUM(valor_gasto), 0)
            FROM gastos_caja
            WHERE id_caja_diaria = :idCajaDiaria
            AND estado_gasto <> 'anulado'
            """, nativeQuery = true)
    BigDecimal sumarGastosVigentesPorCaja(@Param("idCajaDiaria") UUID idCajaDiaria);
}
