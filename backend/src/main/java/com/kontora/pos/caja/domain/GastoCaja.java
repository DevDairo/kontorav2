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
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "gastos_caja")
public class GastoCaja {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id_gasto_caja", nullable = false)
    private UUID idGastoCaja;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_caja_diaria", nullable = false)
    private CajaDiaria cajaDiaria;

    @Column(name = "valor_gasto", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorGasto;

    @Column(name = "descripcion", nullable = false)
    private String descripcion;

    @Column(name = "estado_gasto", nullable = false, columnDefinition = "estado_gasto_enum")
    @ColumnTransformer(write = "?::estado_gasto_enum")
    private String estadoGasto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_registro", nullable = false)
    private Usuario usuarioRegistro;

    @Column(name = "fecha_registro", nullable = false)
    private OffsetDateTime fechaRegistro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_ultima_edicion")
    private Usuario usuarioUltimaEdicion;

    @Column(name = "fecha_ultima_edicion")
    private OffsetDateTime fechaUltimaEdicion;

    @Column(name = "motivo_edicion")
    private String motivoEdicion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_anulacion")
    private Usuario usuarioAnulacion;

    @Column(name = "fecha_anulacion")
    private OffsetDateTime fechaAnulacion;

    @Column(name = "motivo_anulacion")
    private String motivoAnulacion;

    public UUID getIdGastoCaja() {
        return idGastoCaja;
    }

    public CajaDiaria getCajaDiaria() {
        return cajaDiaria;
    }

    public void setCajaDiaria(CajaDiaria cajaDiaria) {
        this.cajaDiaria = cajaDiaria;
    }

    public BigDecimal getValorGasto() {
        return valorGasto;
    }

    public void setValorGasto(BigDecimal valorGasto) {
        this.valorGasto = valorGasto;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getEstadoGasto() {
        return estadoGasto;
    }

    public void setEstadoGasto(String estadoGasto) {
        this.estadoGasto = estadoGasto;
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

    public Usuario getUsuarioUltimaEdicion() {
        return usuarioUltimaEdicion;
    }

    public void setUsuarioUltimaEdicion(Usuario usuarioUltimaEdicion) {
        this.usuarioUltimaEdicion = usuarioUltimaEdicion;
    }

    public OffsetDateTime getFechaUltimaEdicion() {
        return fechaUltimaEdicion;
    }

    public void setFechaUltimaEdicion(OffsetDateTime fechaUltimaEdicion) {
        this.fechaUltimaEdicion = fechaUltimaEdicion;
    }

    public String getMotivoEdicion() {
        return motivoEdicion;
    }

    public void setMotivoEdicion(String motivoEdicion) {
        this.motivoEdicion = motivoEdicion;
    }

    public Usuario getUsuarioAnulacion() {
        return usuarioAnulacion;
    }

    public void setUsuarioAnulacion(Usuario usuarioAnulacion) {
        this.usuarioAnulacion = usuarioAnulacion;
    }

    public OffsetDateTime getFechaAnulacion() {
        return fechaAnulacion;
    }

    public void setFechaAnulacion(OffsetDateTime fechaAnulacion) {
        this.fechaAnulacion = fechaAnulacion;
    }

    public String getMotivoAnulacion() {
        return motivoAnulacion;
    }

    public void setMotivoAnulacion(String motivoAnulacion) {
        this.motivoAnulacion = motivoAnulacion;
    }
}
