package com.kontora.pos.catalogos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnTransformer;

import java.util.UUID;

@Entity
@Table(name = "tipos_servicio")
public class TipoServicio {

    @Id
    @Column(name = "id_tipo_servicio", nullable = false)
    private UUID idTipoServicio;

    @Column(name = "nombre_servicio", nullable = false, unique = true)
    private String nombreServicio;

    @Column(name = "estado", nullable = false, columnDefinition = "estado_basico_enum")
    @ColumnTransformer(write = "?::estado_basico_enum")
    private String estado;

    public UUID getIdTipoServicio() {
        return idTipoServicio;
    }

    public String getNombreServicio() {
        return nombreServicio;
    }

    public String getEstado() {
        return estado;
    }
}
