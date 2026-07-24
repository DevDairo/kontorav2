package com.kontora.pos.catalogos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnTransformer;

import java.util.UUID;

@Entity
@Table(name = "metodos_pago")
public class MetodoPago {

    @Id
    @Column(name = "id_metodo_pago", nullable = false)
    private UUID idMetodoPago;

    @Column(name = "nombre_metodo", nullable = false, unique = true)
    private String nombreMetodo;

    @Column(name = "estado", nullable = false, columnDefinition = "estado_basico_enum")
    @ColumnTransformer(write = "?::estado_basico_enum")
    private String estado;

    public UUID getIdMetodoPago() {
        return idMetodoPago;
    }

    public String getNombreMetodo() {
        return nombreMetodo;
    }

    public String getEstado() {
        return estado;
    }
}
