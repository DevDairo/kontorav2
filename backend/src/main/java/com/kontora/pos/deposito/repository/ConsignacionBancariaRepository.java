package com.kontora.pos.deposito.repository;

import com.kontora.pos.deposito.domain.ConsignacionBancaria;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ConsignacionBancariaRepository extends JpaRepository<ConsignacionBancaria, UUID> {

    @EntityGraph(attributePaths = {"movimientoDeposito", "usuarioRegistro"})
    Optional<ConsignacionBancaria> findByIdConsignacionBancaria(UUID idConsignacionBancaria);
}
