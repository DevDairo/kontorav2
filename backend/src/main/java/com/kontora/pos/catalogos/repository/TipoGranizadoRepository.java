package com.kontora.pos.catalogos.repository;

import com.kontora.pos.catalogos.domain.TipoGranizado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface TipoGranizadoRepository extends JpaRepository<TipoGranizado, UUID> {

    @Query(value = "SELECT * FROM tipos_granizado WHERE estado = 'activo' ORDER BY nombre_tipo", nativeQuery = true)
    List<TipoGranizado> findActivos();
}
