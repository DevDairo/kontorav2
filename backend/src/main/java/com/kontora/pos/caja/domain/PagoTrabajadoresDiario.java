package com.kontora.pos.caja.domain;

import com.kontora.pos.usuarios.domain.Usuario;
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
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "pagos_trabajadores_diarios")
public class PagoTrabajadoresDiario {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id_pago_trabajadores_diario", nullable = false)
    private UUID idPagoTrabajadoresDiario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_caja_diaria", nullable = false)
    private CajaDiaria cajaDiaria;

    @Column(name = "valor_total_pagado", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorTotalPagado;

    @Column(name = "descripcion")
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_registro", nullable = false)
    private Usuario usuarioRegistro;

    @Column(name = "fecha_registro", nullable = false)
    private OffsetDateTime fechaRegistro;

    @Column(name = "confirmado_para_cierre", nullable = false)
    private boolean confirmadoParaCierre;

    public UUID getIdPagoTrabajadoresDiario() {
        return idPagoTrabajadoresDiario;
    }

    public CajaDiaria getCajaDiaria() {
        return cajaDiaria;
    }

    public void setCajaDiaria(CajaDiaria cajaDiaria) {
        this.cajaDiaria = cajaDiaria;
    }

    public BigDecimal getValorTotalPagado() {
        return valorTotalPagado;
    }

    public void setValorTotalPagado(BigDecimal valorTotalPagado) {
        this.valorTotalPagado = valorTotalPagado;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Usuario getUsuarioRegistro() {
        return usuarioRegistro;
    }

    public void setUsuarioRegistro(Usuario usuarioRegistro) {
        this.usuarioRegistro = usuarioRegistro;
    }

    public OffsetDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(OffsetDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public boolean isConfirmadoParaCierre() {
        return confirmadoParaCierre;
    }

    public void setConfirmadoParaCierre(boolean confirmadoParaCierre) {
        this.confirmadoParaCierre = confirmadoParaCierre;
    }
}
