package com.kontora.pos.inventario.repository;

import com.kontora.pos.inventario.domain.AjusteInventario;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AjusteInventarioRepository extends JpaRepository<AjusteInventario, UUID> {

    @EntityGraph(attributePaths = {
            "itemInventario",
            "itemInventario.tamanoVaso",
            "cajaDiaria",
            "usuarioSolicitante",
            "usuarioAprobador"
    })
    List<AjusteInventario> findAllByOrderByFechaSolicitudDesc();

    @Query(value = """
            SELECT *
            FROM ajustes_inventario
            WHERE estado_aprobacion = CAST(:estadoAprobacion AS estado_aprobacion_enum)
            ORDER BY fecha_solicitud DESC
            """, nativeQuery = true)
    List<AjusteInventario> findByEstadoAprobacionOrderByFechaSolicitudDesc(
            @Param("estadoAprobacion") String estadoAprobacion);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {
            "itemInventario",
            "itemInventario.tamanoVaso",
            "cajaDiaria",
            "usuarioSolicitante",
            "usuarioAprobador"
    })
    @Query("""
            SELECT a
            FROM AjusteInventario a
            WHERE a.idAjusteInventario = :idAjusteInventario
            """)
    Optional<AjusteInventario> findByIdForUpdate(@Param("idAjusteInventario") UUID idAjusteInventario);
}
