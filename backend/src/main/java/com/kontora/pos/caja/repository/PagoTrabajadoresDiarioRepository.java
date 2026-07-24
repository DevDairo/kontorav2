package com.kontora.pos.caja.repository;

import com.kontora.pos.caja.domain.PagoTrabajadoresDiario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PagoTrabajadoresDiarioRepository extends JpaRepository<PagoTrabajadoresDiario, UUID> {

    @EntityGraph(attributePaths = {"cajaDiaria", "usuarioRegistro"})
    Optional<PagoTrabajadoresDiario> findByCajaDiaria_IdCajaDiaria(UUID idCajaDiaria);
}
