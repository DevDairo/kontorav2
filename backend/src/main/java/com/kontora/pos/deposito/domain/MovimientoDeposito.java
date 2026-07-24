package com.kontora.pos.deposito.domain;

import com.kontora.pos.caja.domain.CierreCaja;
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
@Table(name = "movimientos_deposito")
public class MovimientoDeposito {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id_movimiento_deposito", nullable = false)
    private UUID idMovimientoDeposito;

    @Column(name = "tipo_movimiento_deposito", nullable = false, columnDefinition = "tipo_movimiento_deposito_enum")
    @ColumnTransformer(write = "?::tipo_movimiento_deposito_enum")
    private String tipoMovimientoDeposito;

    @Column(name = "valor_movimiento", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorMovimiento;

    @Column(name = "saldo_anterior", nullable = false, precision = 12, scale = 2)
    private BigDecimal saldoAnterior;

    @Column(name = "saldo_posterior", nullable = false, precision = 12, scale = 2)
    private BigDecimal saldoPosterior;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cierre_caja", unique = true)
    private CierreCaja cierreCaja;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_registro", nullable = false)
    private Usuario usuarioRegistro;

    @Column(name = "fecha_movimiento", nullable = false)
    private OffsetDateTime fechaMovimiento;

    @Column(name = "observacion")
    private String observacion;

    public UUID getIdMovimientoDeposito() {
        return idMovimientoDeposito;
    }

    public String getTipoMovimientoDeposito() {
        return tipoMovimientoDeposito;
    }

    public void setTipoMovimientoDeposito(String tipoMovimientoDeposito) {
        this.tipoMovimientoDeposito = tipoMovimientoDeposito;
    }

    public BigDecimal getValorMovimiento() {
        return valorMovimiento;
    }

    public void setValorMovimiento(BigDecimal valorMovimiento) {
        this.valorMovimiento = valorMovimiento;
    }

    public BigDecimal getSaldoAnterior() {
        return saldoAnterior;
    }

    public void setSaldoAnterior(BigDecimal saldoAnterior) {
        this.saldoAnterior = saldoAnterior;
    }

    public BigDecimal getSaldoPosterior() {
        return saldoPosterior;
    }

    public void setSaldoPosterior(BigDecimal saldoPosterior) {
        this.saldoPosterior = saldoPosterior;
    }

    public CierreCaja getCierreCaja() {
        return cierreCaja;
    }

    public void setCierreCaja(CierreCaja cierreCaja) {
        this.cierreCaja = cierreCaja;
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

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }
}
