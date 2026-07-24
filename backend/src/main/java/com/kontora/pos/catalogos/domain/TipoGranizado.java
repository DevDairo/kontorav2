package com.kontora.pos.catalogos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnTransformer;

import java.util.UUID;

@Entity
@Table(name = "tipos_granizado")
public class TipoGranizado {

    @Id
    @Column(name = "id_tipo_granizado", nullable = false)
    private UUID idTipoGranizado;

    @Column(name = "nombre_tipo", nullable = false, unique = true)
    private String nombreTipo;

    @Column(name = "estado", nullable = false, columnDefinition = "estado_basico_enum")
    @ColumnTransformer(write = "?::estado_basico_enum")
    private String estado;

    public UUID getIdTipoGranizado() {
        return idTipoGranizado;
    }

    public String getNombreTipo() {
        return nombreTipo;
    }

    public String getEstado() {
        return estado;
    }
}
