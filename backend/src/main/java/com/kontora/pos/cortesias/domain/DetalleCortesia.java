package com.kontora.pos.cortesias.domain;

import com.kontora.pos.catalogos.domain.TamanoVaso;
import com.kontora.pos.catalogos.domain.TipoGranizado;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.GenericGenerator;

import java.util.UUID;

@Entity
@Table(name = "detalles_cortesia")
public class DetalleCortesia {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id_detalle_cortesia", nullable = false)
    private UUID idDetalleCortesia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cortesia", nullable = false)
    private Cortesia cortesia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_granizado", nullable = false)
    private TipoGranizado tipoGranizado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tamano_vaso", nullable = false)
    private TamanoVaso tamanoVaso;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    public UUID getIdDetalleCortesia() {
        return idDetalleCortesia;
    }

    public Cortesia getCortesia() {
        return cortesia;
    }

    public void setCortesia(Cortesia cortesia) {
        this.cortesia = cortesia;
    }

    public TipoGranizado getTipoGranizado() {
        return tipoGranizado;
    }

    public void setTipoGranizado(TipoGranizado tipoGranizado) {
        this.tipoGranizado = tipoGranizado;
    }

    public TamanoVaso getTamanoVaso() {
        return tamanoVaso;
    }

    public void setTamanoVaso(TamanoVaso tamanoVaso) {
        this.tamanoVaso = tamanoVaso;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }
}
