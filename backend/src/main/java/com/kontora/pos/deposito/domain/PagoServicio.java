package com.kontora.pos.deposito.domain;

import com.kontora.pos.catalogos.domain.TipoServicio;
import com.kontora.pos.usuarios.domain.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "pagos_servicios")
public class PagoServicio {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id_pago_servicio", nullable = false)
    private UUID idPagoServicio;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_movimiento_deposito", nullable = false, unique = true)
    private MovimientoDeposito movimientoDeposito;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_servicio", nullable = false)
    private TipoServicio tipoServicio;

    @Column(name = "valor_pagado", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorPagado;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "fecha_pago", nullable = false)
    private OffsetDateTime fechaPago;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_registro", nullable = false)
    private Usuario usuarioRegistro;

    @Column(name = "estado", nullable = false, columnDefinition = "estado_registro_financiero_enum")
    @ColumnTransformer(write = "?::estado_registro_financiero_enum")
    private String estado;

    public UUID getIdPagoServicio() {
        return idPagoServicio;
    }

    public MovimientoDeposito getMovimientoDeposito() {
        return movimientoDeposito;
    }

    public void setMovimientoDeposito(MovimientoDeposito movimientoDeposito) {
        this.movimientoDeposito = movimientoDeposito;
    }

    public TipoServicio getTipoServicio() {
        return tipoServicio;
    }

    public void setTipoServicio(TipoServicio tipoServicio) {
        this.tipoServicio = tipoServicio;
    }

    public BigDecimal getValorPagado() {
        return valorPagado;
    }

    public void setValorPagado(BigDecimal valorPagado) {
        this.valorPagado = valorPagado;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public OffsetDateTime getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(OffsetDateTime fechaPago) {
        this.fechaPago = fechaPago;
    }

    public Usuario getUsuarioRegistro() {
        return usuarioRegistro;
    }

    public void setUsuarioRegistro(Usuario usuarioRegistro) {
        this.usuarioRegistro = usuarioRegistro;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
