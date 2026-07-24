package com.kontora.pos.inventario.repository;

import com.kontora.pos.inventario.domain.MovimientoInventario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, UUID> {

    @EntityGraph(attributePaths = {"itemInventario", "itemInventario.tamanoVaso", "cajaDiaria", "usuarioRegistro"})
    List<MovimientoInventario> findAllByOrderByFechaMovimientoDesc();

    @EntityGraph(attributePaths = {"itemInventario", "itemInventario.tamanoVaso", "cajaDiaria", "usuarioRegistro"})
    List<MovimientoInventario> findByCajaDiaria_IdCajaDiariaOrderByFechaMovimientoDesc(UUID idCajaDiaria);

    @EntityGraph(attributePaths = {"itemInventario", "itemInventario.tamanoVaso", "cajaDiaria", "usuarioRegistro"})
    List<MovimientoInventario> findByItemInventario_IdItemInventarioOrderByFechaMovimientoDesc(UUID idItemInventario);

    @EntityGraph(attributePaths = {"itemInventario", "itemInventario.tamanoVaso", "cajaDiaria", "usuarioRegistro"})
    List<MovimientoInventario> findByCajaDiaria_IdCajaDiariaAndItemInventario_IdItemInventarioOrderByFechaMovimientoDesc(
            UUID idCajaDiaria,
            UUID idItemInventario);

    boolean existsByItemInventario_IdItemInventario(UUID idItemInventario);
}
