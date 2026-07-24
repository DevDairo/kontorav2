package com.kontora.pos.deposito.domain;

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
@Table(name = "consignaciones_bancarias")
public class ConsignacionBancaria {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id_consignacion_bancaria", nullable = false)
    private UUID idConsignacionBancaria;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_movimiento_deposito", nullable = false, unique = true)
    private MovimientoDeposito movimientoDeposito;

    @Column(name = "valor_consignado", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorConsignado;

    @Column(name = "fecha_consignacion", nullable = false)
    private OffsetDateTime fechaConsignacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_registro", nullable = false)
    private Usuario usuarioRegistro;

    @Column(name = "observacion")
    private String observacion;

    @Column(name = "estado", nullable = false, columnDefinition = "estado_registro_financiero_enum")
    @ColumnTransformer(write = "?::estado_registro_financiero_enum")
    private String estado;

    public UUID getIdConsignacionBancaria() {
        return idConsignacionBancaria;
    }

    public MovimientoDeposito getMovimientoDeposito() {
        return movimientoDeposito;
    }

    public void setMovimientoDeposito(MovimientoDeposito movimientoDeposito) {
        this.movimientoDeposito = movimientoDeposito;
    }

    public BigDecimal getValorConsignado() {
        return valorConsignado;
    }

    public void setValorConsignado(BigDecimal valorConsignado) {
        this.valorConsignado = valorConsignado;
    }

    public OffsetDateTime getFechaConsignacion() {
        return fechaConsignacion;
    }

    public void setFechaConsignacion(OffsetDateTime fechaConsignacion) {
        this.fechaConsignacion = fechaConsignacion;
    }

    public Usuario getUsuarioRegistro() {
        return usuarioRegistro;
    }

    public void setUsuarioRegistro(Usuario usuarioRegistro) {
        this.usuarioRegistro = usuarioRegistro;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
