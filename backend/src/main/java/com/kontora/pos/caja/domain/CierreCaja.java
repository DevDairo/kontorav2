package com.kontora.pos.caja.domain;

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
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "cierres_caja")
public class CierreCaja {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id_cierre_caja", nullable = false)
    private UUID idCierreCaja;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_caja_diaria", nullable = false, unique = true)
    private CajaDiaria cajaDiaria;

    @Column(name = "total_ventas", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalVentas;

    @Column(name = "total_ventas_efectivo", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalVentasEfectivo;

    @Column(name = "total_ventas_transferencia", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalVentasTransferencia;

    @Column(name = "total_transferencias_pendientes", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalTransferenciasPendientes;

    @Column(name = "total_transferencias_validadas", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalTransferenciasValidadas;

    @Column(name = "total_transferencias_rechazadas", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalTransferenciasRechazadas;

    @Column(name = "total_gastos", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalGastos;

    @Column(name = "total_adiciones", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAdiciones;

    @Column(name = "total_pago_trabajadores", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPagoTrabajadores;

    @Column(name = "efectivo_esperado_sin_base", nullable = false, precision = 12, scale = 2)
    private BigDecimal efectivoEsperadoSinBase;

    @Column(name = "efectivo_contado_sin_base", nullable = false, precision = 12, scale = 2)
    private BigDecimal efectivoContadoSinBase;

    @Column(name = "diferencia_caja", nullable = false, precision = 12, scale = 2)
    private BigDecimal diferenciaCaja;

    @Column(name = "valor_a_deposito", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorADeposito;

    @Column(name = "fecha_cierre", nullable = false)
    private OffsetDateTime fechaCierre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_cierre", nullable = false)
    private Usuario usuarioCierre;

    @Column(name = "observaciones")
    private String observaciones;

    public UUID getIdCierreCaja() {
        return idCierreCaja;
    }

    public CajaDiaria getCajaDiaria() {
        return cajaDiaria;
    }

    public void setCajaDiaria(CajaDiaria cajaDiaria) {
        this.cajaDiaria = cajaDiaria;
    }

    public BigDecimal getTotalVentas() {
        return totalVentas;
    }

    public void setTotalVentas(BigDecimal totalVentas) {
        this.totalVentas = totalVentas;
    }

    public BigDecimal getTotalVentasEfectivo() {
        return totalVentasEfectivo;
    }

    public void setTotalVentasEfectivo(BigDecimal totalVentasEfectivo) {
        this.totalVentasEfectivo = totalVentasEfectivo;
    }

    public BigDecimal getTotalVentasTransferencia() {
        return totalVentasTransferencia;
    }

    public void setTotalVentasTransferencia(BigDecimal totalVentasTransferencia) {
        this.totalVentasTransferencia = totalVentasTransferencia;
    }

    public BigDecimal getTotalTransferenciasPendientes() {
        return totalTransferenciasPendientes;
    }

    public void setTotalTransferenciasPendientes(BigDecimal totalTransferenciasPendientes) {
        this.totalTransferenciasPendientes = totalTransferenciasPendientes;
    }

    public BigDecimal getTotalTransferenciasValidadas() {
        return totalTransferenciasValidadas;
    }

    public void setTotalTransferenciasValidadas(BigDecimal totalTransferenciasValidadas) {
        this.totalTransferenciasValidadas = totalTransferenciasValidadas;
    }

    public BigDecimal getTotalTransferenciasRechazadas() {
        return totalTransferenciasRechazadas;
    }

    public void setTotalTransferenciasRechazadas(BigDecimal totalTransferenciasRechazadas) {
        this.totalTransferenciasRechazadas = totalTransferenciasRechazadas;
    }

    public BigDecimal getTotalGastos() {
        return totalGastos;
    }

    public void setTotalGastos(BigDecimal totalGastos) {
        this.totalGastos = totalGastos;
    }

    public BigDecimal getTotalAdiciones() {
        return totalAdiciones;
    }

    public void setTotalAdiciones(BigDecimal totalAdiciones) {
        this.totalAdiciones = totalAdiciones;
    }

    public BigDecimal getTotalPagoTrabajadores() {
        return totalPagoTrabajadores;
    }

    public void setTotalPagoTrabajadores(BigDecimal totalPagoTrabajadores) {
        this.totalPagoTrabajadores = totalPagoTrabajadores;
    }

    public BigDecimal getEfectivoEsperadoSinBase() {
        return efectivoEsperadoSinBase;
    }

    public void setEfectivoEsperadoSinBase(BigDecimal efectivoEsperadoSinBase) {
        this.efectivoEsperadoSinBase = efectivoEsperadoSinBase;
    }

    public BigDecimal getEfectivoContadoSinBase() {
        return efectivoContadoSinBase;
    }

    public void setEfectivoContadoSinBase(BigDecimal efectivoContadoSinBase) {
        this.efectivoContadoSinBase = efectivoContadoSinBase;
    }

    public BigDecimal getDiferenciaCaja() {
        return diferenciaCaja;
    }

    public void setDiferenciaCaja(BigDecimal diferenciaCaja) {
        this.diferenciaCaja = diferenciaCaja;
    }

    public BigDecimal getValorADeposito() {
        return valorADeposito;
    }

    public void setValorADeposito(BigDecimal valorADeposito) {
        this.valorADeposito = valorADeposito;
    }

    public OffsetDateTime getFechaCierre() {
        return fechaCierre;
    }

    public void setFechaCierre(OffsetDateTime fechaCierre) {
        this.fechaCierre = fechaCierre;
    }

    public Usuario getUsuarioCierre() {
        return usuarioCierre;
    }

    public void setUsuarioCierre(Usuario usuarioCierre) {
        this.usuarioCierre = usuarioCierre;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}
