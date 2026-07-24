package com.kontora.pos.catalogos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnTransformer;

import java.util.UUID;

@Entity
@Table(name = "categorias_inventario")
public class CategoriaInventario {

    @Id
    @Column(name = "id_categoria_inventario", nullable = false)
    private UUID idCategoriaInventario;

    @Column(name = "nombre_categoria", nullable = false, unique = true)
    private String nombreCategoria;

    @Column(name = "estado", nullable = false, columnDefinition = "estado_basico_enum")
    @ColumnTransformer(write = "?::estado_basico_enum")
    private String estado;

    public UUID getIdCategoriaInventario() {
        return idCategoriaInventario;
    }

    public String getNombreCategoria() {
        return nombreCategoria;
    }

    public String getEstado() {
        return estado;
    }
}
