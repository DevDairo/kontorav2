package com.kontora.pos.caja.repository;

import com.kontora.pos.caja.domain.CajaDiaria;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface CajaDiariaRepository extends JpaRepository<CajaDiaria, UUID> {

    boolean existsByFechaOperacion(LocalDate fechaOperacion);

    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                FROM cajas_diarias
                WHERE estado_caja = CAST(:estadoCaja AS estado_caja_enum)
            )
            """, nativeQuery = true)
    boolean existsByEstadoCaja(@Param("estadoCaja") String estadoCaja);

    @EntityGraph(attributePaths = {"usuarioApertura", "usuarioCierre"})
    Optional<CajaDiaria> findByFechaOperacion(LocalDate fechaOperacion);

    @Query(value = """
            SELECT *
            FROM cajas_diarias
            WHERE estado_caja = CAST(:estadoCaja AS estado_caja_enum)
            ORDER BY fecha_operacion DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<CajaDiaria> findPrimeraPorEstadoCaja(@Param("estadoCaja") String estadoCaja);
}
