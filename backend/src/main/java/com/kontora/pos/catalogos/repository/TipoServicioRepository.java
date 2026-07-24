package com.kontora.pos.catalogos.repository;

import com.kontora.pos.catalogos.domain.TipoServicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface TipoServicioRepository extends JpaRepository<TipoServicio, UUID> {

    @Query(value = "SELECT * FROM tipos_servicio WHERE estado = 'activo' ORDER BY nombre_servicio", nativeQuery = true)
    List<TipoServicio> findActivos();
}
