package com.kontora.pos.deposito.repository;

import com.kontora.pos.deposito.domain.PagoServicio;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PagoServicioRepository extends JpaRepository<PagoServicio, UUID> {

    @EntityGraph(attributePaths = {"movimientoDeposito", "tipoServicio", "usuarioRegistro"})
    Optional<PagoServicio> findByIdPagoServicio(UUID idPagoServicio);
}
