package com.kontora.pos.caja.repository;

import com.kontora.pos.caja.domain.CierreCaja;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CierreCajaRepository extends JpaRepository<CierreCaja, UUID> {

    boolean existsByCajaDiaria_IdCajaDiaria(UUID idCajaDiaria);

    @EntityGraph(attributePaths = {"cajaDiaria", "usuarioCierre"})
    Optional<CierreCaja> findByCajaDiaria_IdCajaDiaria(UUID idCajaDiaria);
}
