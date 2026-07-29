package com.kontora.pos.inventario.repository;

import com.kontora.pos.inventario.domain.PaqueteVasosAbierto;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PaqueteVasosAbiertoRepository extends JpaRepository<PaqueteVasosAbierto, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"cajaDiaria", "itemInventario"})
    @Query("""
            SELECT p
            FROM PaqueteVasosAbierto p
            WHERE p.idPaqueteVasosAbierto = :idPaqueteVasosAbierto
            """)
    Optional<PaqueteVasosAbierto> findByIdForUpdate(
            @Param("idPaqueteVasosAbierto") UUID idPaqueteVasosAbierto);
}
