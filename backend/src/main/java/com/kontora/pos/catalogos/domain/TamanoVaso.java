package com.kontora.pos.catalogos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnTransformer;

import java.util.UUID;

@Entity
@Table(name = "tamanos_vaso")
public class TamanoVaso {

    @Id
    @Column(name = "id_tamano_vaso", nullable = false)
    private UUID idTamanoVaso;

    @Column(name = "onzas", nullable = false, unique = true)
    private Integer onzas;

    @Column(name = "estado", nullable = false, columnDefinition = "estado_basico_enum")
    @ColumnTransformer(write = "?::estado_basico_enum")
    private String estado;

    public UUID getIdTamanoVaso() {
        return idTamanoVaso;
    }

    public Integer getOnzas() {
        return onzas;
    }

    public String getEstado() {
        return estado;
    }
}
