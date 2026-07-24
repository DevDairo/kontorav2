package com.kontora.pos.catalogos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.GenericGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "items_inventario")
public class ItemInventario {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id_item_inventario", nullable = false)
    private UUID idItemInventario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_categoria_inventario", nullable = false)
    private CategoriaInventario categoriaInventario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_unidad_medida", nullable = false)
    private UnidadMedida unidadMedida;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tamano_vaso")
    private TamanoVaso tamanoVaso;

    @Column(name = "nombre_item", nullable = false, unique = true)
    private String nombreItem;

    @Column(name = "tipo_control", nullable = false, columnDefinition = "tipo_control_inventario_enum")
    @ColumnTransformer(write = "?::tipo_control_inventario_enum")
    private String tipoControl;

    @Column(name = "maneja_paquetes", nullable = false)
    private boolean manejaPaquetes;

    @Column(name = "unidades_por_paquete")
    private Integer unidadesPorPaquete;

    @Column(name = "estado", nullable = false, columnDefinition = "estado_basico_enum")
    @ColumnTransformer(write = "?::estado_basico_enum")
    private String estado;

    @Column(name = "fecha_creacion", nullable = false)
    private OffsetDateTime fechaCreacion;

    public UUID getIdItemInventario() {
        return idItemInventario;
    }

    public CategoriaInventario getCategoriaInventario() {
        return categoriaInventario;
    }

    public void setCategoriaInventario(CategoriaInventario categoriaInventario) {
        this.categoriaInventario = categoriaInventario;
    }

    public UnidadMedida getUnidadMedida() {
        return unidadMedida;
    }

    public void setUnidadMedida(UnidadMedida unidadMedida) {
        this.unidadMedida = unidadMedida;
    }

    public TamanoVaso getTamanoVaso() {
        return tamanoVaso;
    }

    public void setTamanoVaso(TamanoVaso tamanoVaso) {
        this.tamanoVaso = tamanoVaso;
    }

    public String getNombreItem() {
        return nombreItem;
    }

    public void setNombreItem(String nombreItem) {
        this.nombreItem = nombreItem;
    }

    public String getTipoControl() {
        return tipoControl;
    }

    public void setTipoControl(String tipoControl) {
        this.tipoControl = tipoControl;
    }

    public boolean isManejaPaquetes() {
        return manejaPaquetes;
    }

    public void setManejaPaquetes(boolean manejaPaquetes) {
        this.manejaPaquetes = manejaPaquetes;
    }

    public Integer getUnidadesPorPaquete() {
        return unidadesPorPaquete;
    }

    public void setUnidadesPorPaquete(Integer unidadesPorPaquete) {
        this.unidadesPorPaquete = unidadesPorPaquete;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(OffsetDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}
