package com.kontora.pos.auditoria.domain;

import com.fasterxml.jackson.databind.JsonNode;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "auditoria_operaciones")
public class AuditoriaOperacion {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id_auditoria_operacion", nullable = false)
    private UUID idAuditoriaOperacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Column(name = "tabla_afectada", nullable = false)
    private String tablaAfectada;

    @Column(name = "id_registro_afectado")
    private String idRegistroAfectado;

    @Column(name = "accion", nullable = false, columnDefinition = "accion_auditoria_enum")
    @ColumnTransformer(write = "?::accion_auditoria_enum")
    private String accion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "valor_anterior", columnDefinition = "jsonb")
    private JsonNode valorAnterior;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "valor_nuevo", columnDefinition = "jsonb")
    private JsonNode valorNuevo;

    @Column(name = "fecha_accion", nullable = false)
    private OffsetDateTime fechaAccion;

    @Column(name = "direccion_ip")
    private String direccionIp;

    @Column(name = "descripcion")
    private String descripcion;

    public UUID getIdAuditoriaOperacion() {
        return idAuditoriaOperacion;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getTablaAfectada() {
        return tablaAfectada;
    }

    public void setTablaAfectada(String tablaAfectada) {
        this.tablaAfectada = tablaAfectada;
    }

    public String getIdRegistroAfectado() {
        return idRegistroAfectado;
    }

    public void setIdRegistroAfectado(String idRegistroAfectado) {
        this.idRegistroAfectado = idRegistroAfectado;
    }

    public String getAccion() {
        return accion;
    }

    public void setAccion(String accion) {
        this.accion = accion;
    }

    public JsonNode getValorAnterior() {
        return valorAnterior;
    }

    public void setValorAnterior(JsonNode valorAnterior) {
        this.valorAnterior = valorAnterior;
    }

    public JsonNode getValorNuevo() {
        return valorNuevo;
    }

    public void setValorNuevo(JsonNode valorNuevo) {
        this.valorNuevo = valorNuevo;
    }

    public OffsetDateTime getFechaAccion() {
        return fechaAccion;
    }

    public void setFechaAccion(OffsetDateTime fechaAccion) {
        this.fechaAccion = fechaAccion;
    }

    public String getDireccionIp() {
        return direccionIp;
    }

    public void setDireccionIp(String direccionIp) {
        this.direccionIp = direccionIp;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}
