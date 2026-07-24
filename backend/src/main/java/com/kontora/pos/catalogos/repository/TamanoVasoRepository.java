package com.kontora.pos.catalogos.repository;

import com.kontora.pos.catalogos.domain.TamanoVaso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface TamanoVasoRepository extends JpaRepository<TamanoVaso, UUID> {

    @Query(value = "SELECT * FROM tamanos_vaso WHERE estado = 'activo' ORDER BY onzas", nativeQuery = true)
    List<TamanoVaso> findActivos();
}
