package com.kontora.pos.ventas.domain;

import com.kontora.pos.catalogos.domain.Promocion;
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

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "detalles_venta")
public class DetalleVenta {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id_detalle_venta", nullable = false)
    private UUID idDetalleVenta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_venta", nullable = false)
    private Venta venta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_granizado", nullable = false)
    private TipoGranizado tipoGranizado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tamano_vaso", nullable = false)
    private TamanoVaso tamanoVaso;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @Column(name = "precio_unitario_normal", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitarioNormal;

    @Column(name = "cantidad_con_promocion", nullable = false)
    private Integer cantidadConPromocion;

    @Column(name = "cantidad_sin_promocion", nullable = false)
    private Integer cantidadSinPromocion;

    @Column(name = "valor_promocional_aplicado", precision = 12, scale = 2)
    private BigDecimal valorPromocionalAplicado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_promocion_aplicada")
    private Promocion promocionAplicada;

    @Column(name = "subtotal_linea", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotalLinea;

    @Column(name = "total_linea", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalLinea;

    public UUID getIdDetalleVenta() {
        return idDetalleVenta;
    }

    public Venta getVenta() {
        return venta;
    }

    public void setVenta(Venta venta) {
        this.venta = venta;
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

    public BigDecimal getPrecioUnitarioNormal() {
        return precioUnitarioNormal;
    }

    public void setPrecioUnitarioNormal(BigDecimal precioUnitarioNormal) {
        this.precioUnitarioNormal = precioUnitarioNormal;
    }

    public Integer getCantidadConPromocion() {
        return cantidadConPromocion;
    }

    public void setCantidadConPromocion(Integer cantidadConPromocion) {
        this.cantidadConPromocion = cantidadConPromocion;
    }

    public Integer getCantidadSinPromocion() {
        return cantidadSinPromocion;
    }

    public void setCantidadSinPromocion(Integer cantidadSinPromocion) {
        this.cantidadSinPromocion = cantidadSinPromocion;
    }

    public BigDecimal getValorPromocionalAplicado() {
        return valorPromocionalAplicado;
    }

    public void setValorPromocionalAplicado(BigDecimal valorPromocionalAplicado) {
        this.valorPromocionalAplicado = valorPromocionalAplicado;
    }

    public Promocion getPromocionAplicada() {
        return promocionAplicada;
    }

    public void setPromocionAplicada(Promocion promocionAplicada) {
        this.promocionAplicada = promocionAplicada;
    }

    public BigDecimal getSubtotalLinea() {
        return subtotalLinea;
    }

    public void setSubtotalLinea(BigDecimal subtotalLinea) {
        this.subtotalLinea = subtotalLinea;
    }

    public BigDecimal getTotalLinea() {
        return totalLinea;
    }

    public void setTotalLinea(BigDecimal totalLinea) {
        this.totalLinea = totalLinea;
    }
}
