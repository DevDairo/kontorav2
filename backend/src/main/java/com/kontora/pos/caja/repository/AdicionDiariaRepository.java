package com.kontora.pos.caja.repository;

import com.kontora.pos.caja.domain.AdicionDiaria;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AdicionDiariaRepository extends JpaRepository<AdicionDiaria, UUID> {

    @EntityGraph(attributePaths = {"cajaDiaria", "usuarioRegistro"})
    Optional<AdicionDiaria> findByCajaDiaria_IdCajaDiaria(UUID idCajaDiaria);
}
