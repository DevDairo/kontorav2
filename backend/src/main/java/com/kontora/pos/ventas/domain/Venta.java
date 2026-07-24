package com.kontora.pos.ventas.domain;

import com.kontora.pos.caja.domain.CajaDiaria;
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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ventas")
public class Venta {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id_venta", nullable = false)
    private UUID idVenta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_caja_diaria", nullable = false)
    private CajaDiaria cajaDiaria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_vendedor", nullable = false)
    private Usuario usuarioVendedor;

    @Column(name = "tipo_comprador", nullable = false, columnDefinition = "tipo_comprador_enum")
    @ColumnTransformer(write = "?::tipo_comprador_enum")
    private String tipoComprador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_comprador")
    private Usuario usuarioComprador;

    @Column(name = "numero_venta", insertable = false, updatable = false)
    private Long numeroVenta;

    @Column(name = "fecha_venta", nullable = false)
    private OffsetDateTime fechaVenta;

    @Column(name = "estado_venta", nullable = false, columnDefinition = "estado_venta_enum")
    @ColumnTransformer(write = "?::estado_venta_enum")
    private String estadoVenta;

    @Column(name = "subtotal_venta", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotalVenta;

    @Column(name = "descuento_promocion", nullable = false, precision = 12, scale = 2)
    private BigDecimal descuentoPromocion;

    @Column(name = "total_venta", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalVenta;

    @Column(name = "motivo_anulacion")
    private String motivoAnulacion;

    @Column(name = "fecha_anulacion")
    private OffsetDateTime fechaAnulacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_anulacion")
    private Usuario usuarioAnulacion;

    public UUID getIdVenta() {
        return idVenta;
    }

    public CajaDiaria getCajaDiaria() {
        return cajaDiaria;
    }

    public void setCajaDiaria(CajaDiaria cajaDiaria) {
        this.cajaDiaria = cajaDiaria;
    }

    public Usuario getUsuarioVendedor() {
        return usuarioVendedor;
    }

    public void setUsuarioVendedor(Usuario usuarioVendedor) {
        this.usuarioVendedor = usuarioVendedor;
    }

    public String getTipoComprador() {
        return tipoComprador;
    }

    public void setTipoComprador(String tipoComprador) {
        this.tipoComprador = tipoComprador;
    }

    public Usuario getUsuarioComprador() {
        return usuarioComprador;
    }

    public void setUsuarioComprador(Usuario usuarioComprador) {
        this.usuarioComprador = usuarioComprador;
    }

    public Long getNumeroVenta() {
        return numeroVenta;
    }

    public OffsetDateTime getFechaVenta() {
        return fechaVenta;
    }

    public void setFechaVenta(OffsetDateTime fechaVenta) {
        this.fechaVenta = fechaVenta;
    }

    public String getEstadoVenta() {
        return estadoVenta;
    }

    public void setEstadoVenta(String estadoVenta) {
        this.estadoVenta = estadoVenta;
    }

    public BigDecimal getSubtotalVenta() {
        return subtotalVenta;
    }

    public void setSubtotalVenta(BigDecimal subtotalVenta) {
        this.subtotalVenta = subtotalVenta;
    }

    public BigDecimal getDescuentoPromocion() {
        return descuentoPromocion;
    }

    public void setDescuentoPromocion(BigDecimal descuentoPromocion) {
        this.descuentoPromocion = descuentoPromocion;
    }

    public BigDecimal getTotalVenta() {
        return totalVenta;
    }

    public void setTotalVenta(BigDecimal totalVenta) {
        this.totalVenta = totalVenta;
    }

    public String getMotivoAnulacion() {
        return motivoAnulacion;
    }

    public void setMotivoAnulacion(String motivoAnulacion) {
        this.motivoAnulacion = motivoAnulacion;
    }

    public OffsetDateTime getFechaAnulacion() {
        return fechaAnulacion;
    }

    public void setFechaAnulacion(OffsetDateTime fechaAnulacion) {
        this.fechaAnulacion = fechaAnulacion;
    }

    public Usuario getUsuarioAnulacion() {
        return usuarioAnulacion;
    }

    public void setUsuarioAnulacion(Usuario usuarioAnulacion) {
        this.usuarioAnulacion = usuarioAnulacion;
    }
}
