package com.kontora.pos.cortesias.repository;

import com.kontora.pos.cortesias.domain.Cortesia;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface CortesiaRepository extends JpaRepository<Cortesia, UUID> {

    @EntityGraph(attributePaths = {
            "cajaDiaria",
            "usuarioRegistro",
            "usuarioBeneficiario",
            "usuarioAnulacion"
    })
    List<Cortesia> findByCajaDiaria_IdCajaDiariaOrderByFechaRegistroDesc(UUID idCajaDiaria);

    @EntityGraph(attributePaths = {
            "cajaDiaria",
            "usuarioRegistro",
            "usuarioBeneficiario",
            "usuarioAnulacion"
    })
    @Query("""
            SELECT c
            FROM Cortesia c
            WHERE c.cajaDiaria.fechaOperacion BETWEEN :fechaInicio AND :fechaFin
            ORDER BY c.cajaDiaria.fechaOperacion DESC, c.fechaRegistro DESC
            """)
    List<Cortesia> findParaConsulta(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {
            "cajaDiaria",
            "usuarioRegistro",
            "usuarioBeneficiario",
            "usuarioAnulacion"
    })
    @Query("""
            SELECT c
            FROM Cortesia c
            WHERE c.idCortesia = :idCortesia
            """)
    Optional<Cortesia> findByIdForUpdate(@Param("idCortesia") UUID idCortesia);
}
