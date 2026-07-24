package com.kontora.pos.ventas.domain;

import com.kontora.pos.catalogos.domain.MetodoPago;
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
@Table(name = "pagos_venta")
public class PagoVenta {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id_pago_venta", nullable = false)
    private UUID idPagoVenta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_venta", nullable = false)
    private Venta venta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_metodo_pago", nullable = false)
    private MetodoPago metodoPago;

    @Column(name = "valor_pago", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorPago;

    @Column(name = "valor_recibido_efectivo", precision = 12, scale = 2)
    private BigDecimal valorRecibidoEfectivo;

    @Column(name = "cambio_entregado", precision = 12, scale = 2)
    private BigDecimal cambioEntregado;

    @Column(name = "estado_validacion", nullable = false, columnDefinition = "estado_validacion_transferencia_enum")
    @ColumnTransformer(write = "?::estado_validacion_transferencia_enum")
    private String estadoValidacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_validacion")
    private Usuario usuarioValidacion;

    @Column(name = "fecha_validacion")
    private OffsetDateTime fechaValidacion;

    @Column(name = "observacion_validacion")
    private String observacionValidacion;

    @Column(name = "fecha_registro", nullable = false)
    private OffsetDateTime fechaRegistro;

    public UUID getIdPagoVenta() {
        return idPagoVenta;
    }

    public Venta getVenta() {
        return venta;
    }

    public void setVenta(Venta venta) {
        this.venta = venta;
    }

    public MetodoPago getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(MetodoPago metodoPago) {
        this.metodoPago = metodoPago;
    }

    public BigDecimal getValorPago() {
        return valorPago;
    }

    public void setValorPago(BigDecimal valorPago) {
        this.valorPago = valorPago;
    }

    public BigDecimal getValorRecibidoEfectivo() {
        return valorRecibidoEfectivo;
    }

    public void setValorRecibidoEfectivo(BigDecimal valorRecibidoEfectivo) {
        this.valorRecibidoEfectivo = valorRecibidoEfectivo;
    }

    public BigDecimal getCambioEntregado() {
        return cambioEntregado;
    }

    public void setCambioEntregado(BigDecimal cambioEntregado) {
        this.cambioEntregado = cambioEntregado;
    }

    public String getEstadoValidacion() {
        return estadoValidacion;
    }

    public void setEstadoValidacion(String estadoValidacion) {
        this.estadoValidacion = estadoValidacion;
    }

    public Usuario getUsuarioValidacion() {
        return usuarioValidacion;
    }

    public void setUsuarioValidacion(Usuario usuarioValidacion) {
        this.usuarioValidacion = usuarioValidacion;
    }

    public OffsetDateTime getFechaValidacion() {
        return fechaValidacion;
    }

    public void setFechaValidacion(OffsetDateTime fechaValidacion) {
        this.fechaValidacion = fechaValidacion;
    }

    public String getObservacionValidacion() {
        return observacionValidacion;
    }

    public void setObservacionValidacion(String observacionValidacion) {
        this.observacionValidacion = observacionValidacion;
    }

    public OffsetDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(OffsetDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }
}
