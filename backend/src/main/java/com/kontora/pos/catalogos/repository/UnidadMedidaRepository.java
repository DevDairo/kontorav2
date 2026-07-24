package com.kontora.pos.catalogos.repository;

import com.kontora.pos.catalogos.domain.UnidadMedida;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface UnidadMedidaRepository extends JpaRepository<UnidadMedida, UUID> {

    @Query(value = "SELECT * FROM unidades_medida WHERE estado = 'activo' ORDER BY nombre_unidad", nativeQuery = true)
    List<UnidadMedida> findActivas();
}
