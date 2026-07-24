package com.kontora.pos.inventario.domain;

import com.kontora.pos.caja.domain.CajaDiaria;
import com.kontora.pos.catalogos.domain.ItemInventario;
import com.kontora.pos.usuarios.domain.Usuario;
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
@Table(name = "movimientos_inventario")
public class MovimientoInventario {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id_movimiento_inventario", nullable = false)
    private UUID idMovimientoInventario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_item_inventario", nullable = false)
    private ItemInventario itemInventario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_caja_diaria")
    private CajaDiaria cajaDiaria;

    @Column(name = "tipo_stock", nullable = false, columnDefinition = "tipo_stock_enum")
    @ColumnTransformer(write = "?::tipo_stock_enum")
    private String tipoStock;

    @Column(name = "tipo_movimiento", nullable = false, columnDefinition = "tipo_movimiento_inventario_enum")
    @ColumnTransformer(write = "?::tipo_movimiento_inventario_enum")
    private String tipoMovimiento;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @Column(name = "sentido_movimiento", nullable = false, columnDefinition = "sentido_movimiento_enum")
    @ColumnTransformer(write = "?::sentido_movimiento_enum")
    private String sentidoMovimiento;

    @Column(name = "referencia_origen")
    private String referenciaOrigen;

    @Column(name = "id_referencia_origen")
    private UUID idReferenciaOrigen;

    @Column(name = "observacion")
    private String observacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_registro", nullable = false)
    private Usuario usuarioRegistro;

    @Column(name = "fecha_movimiento", nullable = false)
    private OffsetDateTime fechaMovimiento;

    public UUID getIdMovimientoInventario() {
        return idMovimientoInventario;
    }

    public ItemInventario getItemInventario() {
        return itemInventario;
    }

    public void setItemInventario(ItemInventario itemInventario) {
        this.itemInventario = itemInventario;
    }

    public CajaDiaria getCajaDiaria() {
        return cajaDiaria;
    }

    public void setCajaDiaria(CajaDiaria cajaDiaria) {
        this.cajaDiaria = cajaDiaria;
    }

    public String getTipoStock() {
        return tipoStock;
    }

    public void setTipoStock(String tipoStock) {
        this.tipoStock = tipoStock;
    }

    public String getTipoMovimiento() {
        return tipoMovimiento;
    }

    public void setTipoMovimiento(String tipoMovimiento) {
        this.tipoMovimiento = tipoMovimiento;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public String getSentidoMovimiento() {
        return sentidoMovimiento;
    }

    public void setSentidoMovimiento(String sentidoMovimiento) {
        this.sentidoMovimiento = sentidoMovimiento;
    }

    public String getReferenciaOrigen() {
        return referenciaOrigen;
    }

    public void setReferenciaOrigen(String referenciaOrigen) {
        this.referenciaOrigen = referenciaOrigen;
    }

    public UUID getIdReferenciaOrigen() {
        return idReferenciaOrigen;
    }

    public void setIdReferenciaOrigen(UUID idReferenciaOrigen) {
        this.idReferenciaOrigen = idReferenciaOrigen;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public Usuario getUsuarioRegistro() {
        return usuarioRegistro;
    }

    public void setUsuarioRegistro(Usuario usuarioRegistro) {
        this.usuarioRegistro = usuarioRegistro;
    }

    public OffsetDateTime getFechaMovimiento() {
        return fechaMovimiento;
    }

    public void setFechaMovimiento(OffsetDateTime fechaMovimiento) {
        this.fechaMovimiento = fechaMovimiento;
    }
}
