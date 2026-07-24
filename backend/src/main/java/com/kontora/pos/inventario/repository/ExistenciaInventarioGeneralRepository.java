package com.kontora.pos.inventario.repository;

import com.kontora.pos.inventario.domain.ExistenciaInventarioGeneral;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExistenciaInventarioGeneralRepository extends JpaRepository<ExistenciaInventarioGeneral, UUID> {

    @EntityGraph(attributePaths = {
            "itemInventario",
            "itemInventario.categoriaInventario",
            "itemInventario.unidadMedida",
            "itemInventario.tamanoVaso"
    })
    @Query("""
            SELECT e
            FROM ExistenciaInventarioGeneral e
            ORDER BY e.itemInventario.nombreItem
            """)
    List<ExistenciaInventarioGeneral> findAllConItem();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT e
            FROM ExistenciaInventarioGeneral e
            WHERE e.itemInventario.idItemInventario = :idItemInventario
            """)
    Optional<ExistenciaInventarioGeneral> findByItemForUpdate(@Param("idItemInventario") UUID idItemInventario);
}
