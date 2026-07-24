package com.kontora.pos.ventas.repository;

import com.kontora.pos.ventas.domain.DetalleVenta;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DetalleVentaRepository extends JpaRepository<DetalleVenta, UUID> {

    @EntityGraph(attributePaths = {"tipoGranizado", "tamanoVaso", "promocionAplicada"})
    List<DetalleVenta> findByVenta_IdVenta(UUID idVenta);
}
