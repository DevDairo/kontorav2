package com.kontora.pos.catalogos.repository;

import com.kontora.pos.catalogos.domain.CategoriaInventario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface CategoriaInventarioRepository extends JpaRepository<CategoriaInventario, UUID> {

    @Query(value = "SELECT * FROM categorias_inventario WHERE estado = 'activo' ORDER BY nombre_categoria", nativeQuery = true)
    List<CategoriaInventario> findActivas();
}
