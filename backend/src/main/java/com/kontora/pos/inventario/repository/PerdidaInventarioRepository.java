package com.kontora.pos.inventario.repository;

import com.kontora.pos.inventario.domain.PerdidaInventario;
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

public interface PerdidaInventarioRepository extends JpaRepository<PerdidaInventario, UUID> {

    @EntityGraph(attributePaths = {
            "cajaDiaria",
            "itemInventario",
            "itemInventario.tamanoVaso",
            "paqueteVasosAbierto",
            "usuarioRegistro",
            "usuarioAnulacion"
    })
    List<PerdidaInventario> findByCajaDiaria_IdCajaDiariaOrderByFechaRegistroDesc(UUID idCajaDiaria);

    @EntityGraph(attributePaths = {
            "cajaDiaria",
            "itemInventario",
            "itemInventario.tamanoVaso",
            "paqueteVasosAbierto",
            "usuarioRegistro",
            "usuarioAnulacion"
    })
    @Query("""
            SELECT p
            FROM PerdidaInventario p
            WHERE p.cajaDiaria.fechaOperacion BETWEEN :fechaInicio AND :fechaFin
            ORDER BY p.cajaDiaria.fechaOperacion DESC, p.fechaRegistro DESC
            """)
    List<PerdidaInventario> findParaConsulta(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin);

    @EntityGraph(attributePaths = {
            "cajaDiaria",
            "itemInventario",
            "itemInventario.tamanoVaso",
            "paqueteVasosAbierto",
            "usuarioRegistro",
            "usuarioAnulacion"
    })
    Optional<PerdidaInventario> findByIdPerdidaInventario(UUID idPerdidaInventario);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {
            "cajaDiaria",
            "itemInventario",
            "itemInventario.tamanoVaso",
            "paqueteVasosAbierto",
            "usuarioRegistro",
            "usuarioAnulacion"
    })
    @Query("""
            SELECT p
            FROM PerdidaInventario p
            WHERE p.idPerdidaInventario = :idPerdidaInventario
            """)
    Optional<PerdidaInventario> findByIdForUpdate(
            @Param("idPerdidaInventario") UUID idPerdidaInventario);

}
