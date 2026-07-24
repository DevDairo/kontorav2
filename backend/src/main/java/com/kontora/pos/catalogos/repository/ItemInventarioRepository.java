package com.kontora.pos.catalogos.repository;

import com.kontora.pos.catalogos.domain.ItemInventario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItemInventarioRepository extends JpaRepository<ItemInventario, UUID> {

    @Query(value = """
            SELECT ii.*
            FROM items_inventario ii
            JOIN categorias_inventario ci ON ci.id_categoria_inventario = ii.id_categoria_inventario
            JOIN unidades_medida um ON um.id_unidad_medida = ii.id_unidad_medida
            LEFT JOIN tamanos_vaso tv ON tv.id_tamano_vaso = ii.id_tamano_vaso
            WHERE ii.estado = 'activo'
              AND ci.estado = 'activo'
              AND um.estado = 'activo'
              AND (tv.id_tamano_vaso IS NULL OR tv.estado = 'activo')
            ORDER BY ii.nombre_item
            """, nativeQuery = true)
    List<ItemInventario> findActivosParaOperacion();

    @Query("""
            SELECT ii
            FROM ItemInventario ii
            JOIN FETCH ii.categoriaInventario
            JOIN FETCH ii.unidadMedida
            LEFT JOIN FETCH ii.tamanoVaso
            ORDER BY ii.nombreItem
            """)
    List<ItemInventario> findAllParaGestion();

    Optional<ItemInventario> findByNombreItem(String nombreItem);

    @Query(value = """
            SELECT ii.*
            FROM items_inventario ii
            WHERE ii.id_tamano_vaso = :idTamanoVaso
              AND ii.tipo_control = 'automatico_por_venta'
              AND ii.estado = 'activo'
            LIMIT 1
            """, nativeQuery = true)
    Optional<ItemInventario> findVasoActivoPorTamano(@Param("idTamanoVaso") UUID idTamanoVaso);
}
