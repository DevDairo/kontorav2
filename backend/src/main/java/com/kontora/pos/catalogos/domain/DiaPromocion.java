package com.kontora.pos.catalogos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnTransformer;

import java.util.UUID;

@Entity
@Table(name = "dias_promocion")
public class DiaPromocion {

    @Id
    @Column(name = "id_dia_promocion", nullable = false)
    private UUID idDiaPromocion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_promocion", nullable = false)
    private Promocion promocion;

    @Column(name = "dia_semana", nullable = false, columnDefinition = "dia_semana_enum")
    @ColumnTransformer(write = "?::dia_semana_enum")
    private String diaSemana;

    public UUID getIdDiaPromocion() {
        return idDiaPromocion;
    }

    public Promocion getPromocion() {
        return promocion;
    }

    public String getDiaSemana() {
        return diaSemana;
    }
}
