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
@Table(name = "ajustes_inventario")
public class AjusteInventario {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id_ajuste_inventario", nullable = false)
    private UUID idAjusteInventario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_item_inventario", nullable = false)
    private ItemInventario itemInventario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_caja_diaria")
    private CajaDiaria cajaDiaria;

    @Column(name = "tipo_stock", nullable = false, columnDefinition = "tipo_stock_enum")
    @ColumnTransformer(write = "?::tipo_stock_enum")
    private String tipoStock;

    @Column(name = "cantidad_ajuste", nullable = false)
    private Integer cantidadAjuste;

    @Column(name = "sentido_ajuste", nullable = false, columnDefinition = "sentido_movimiento_enum")
    @ColumnTransformer(write = "?::sentido_movimiento_enum")
    private String sentidoAjuste;

    @Column(name = "motivo_ajuste", nullable = false)
    private String motivoAjuste;

    @Column(name = "estado_aprobacion", nullable = false, columnDefinition = "estado_aprobacion_enum")
    @ColumnTransformer(write = "?::estado_aprobacion_enum")
    private String estadoAprobacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_solicitante", nullable = false)
    private Usuario usuarioSolicitante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_aprobador")
    private Usuario usuarioAprobador;

    @Column(name = "fecha_solicitud", nullable = false)
    private OffsetDateTime fechaSolicitud;

    @Column(name = "fecha_aprobacion")
    private OffsetDateTime fechaAprobacion;

    @Column(name = "observacion_aprobacion")
    private String observacionAprobacion;

    public UUID getIdAjusteInventario() {
        return idAjusteInventario;
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

    public Integer getCantidadAjuste() {
        return cantidadAjuste;
    }

    public void setCantidadAjuste(Integer cantidadAjuste) {
        this.cantidadAjuste = cantidadAjuste;
    }

    public String getSentidoAjuste() {
        return sentidoAjuste;
    }

    public void setSentidoAjuste(String sentidoAjuste) {
        this.sentidoAjuste = sentidoAjuste;
    }

    public String getMotivoAjuste() {
        return motivoAjuste;
    }

    public void setMotivoAjuste(String motivoAjuste) {
        this.motivoAjuste = motivoAjuste;
    }

    public String getEstadoAprobacion() {
        return estadoAprobacion;
    }

    public void setEstadoAprobacion(String estadoAprobacion) {
        this.estadoAprobacion = estadoAprobacion;
    }

    public Usuario getUsuarioSolicitante() {
        return usuarioSolicitante;
    }

    public void setUsuarioSolicitante(Usuario usuarioSolicitante) {
        this.usuarioSolicitante = usuarioSolicitante;
    }

    public Usuario getUsuarioAprobador() {
        return usuarioAprobador;
    }

    public void setUsuarioAprobador(Usuario usuarioAprobador) {
        this.usuarioAprobador = usuarioAprobador;
    }

    public OffsetDateTime getFechaSolicitud() {
        return fechaSolicitud;
    }

    public void setFechaSolicitud(OffsetDateTime fechaSolicitud) {
        this.fechaSolicitud = fechaSolicitud;
    }

    public OffsetDateTime getFechaAprobacion() {
        return fechaAprobacion;
    }

    public void setFechaAprobacion(OffsetDateTime fechaAprobacion) {
        this.fechaAprobacion = fechaAprobacion;
    }

    public String getObservacionAprobacion() {
        return observacionAprobacion;
    }

    public void setObservacionAprobacion(String observacionAprobacion) {
        this.observacionAprobacion = observacionAprobacion;
    }
}
