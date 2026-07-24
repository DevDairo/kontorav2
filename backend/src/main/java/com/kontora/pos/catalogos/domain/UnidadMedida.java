package com.kontora.pos.catalogos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnTransformer;

import java.util.UUID;

@Entity
@Table(name = "unidades_medida")
public class UnidadMedida {

    @Id
    @Column(name = "id_unidad_medida", nullable = false)
    private UUID idUnidadMedida;

    @Column(name = "nombre_unidad", nullable = false, unique = true)
    private String nombreUnidad;

    @Column(name = "abreviatura", nullable = false, unique = true)
    private String abreviatura;

    @Column(name = "estado", nullable = false, columnDefinition = "estado_basico_enum")
    @ColumnTransformer(write = "?::estado_basico_enum")
    private String estado;

    public UUID getIdUnidadMedida() {
        return idUnidadMedida;
    }

    public String getNombreUnidad() {
        return nombreUnidad;
    }

    public String getAbreviatura() {
        return abreviatura;
    }

    public String getEstado() {
        return estado;
    }
}
