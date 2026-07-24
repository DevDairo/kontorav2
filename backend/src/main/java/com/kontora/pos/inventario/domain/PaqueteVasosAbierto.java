package com.kontora.pos.inventario.domain;

import com.kontora.pos.caja.domain.CajaDiaria;
import com.kontora.pos.catalogos.domain.ItemInventario;
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

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "paquetes_vasos_abiertos")
public class PaqueteVasosAbierto {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id_paquete_vasos_abierto", nullable = false)
    private UUID idPaqueteVasosAbierto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_caja_diaria", nullable = false)
    private CajaDiaria cajaDiaria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_item_inventario", nullable = false)
    private ItemInventario itemInventario;

    @Column(name = "cantidad_paquetes", nullable = false)
    private Integer cantidadPaquetes;

    @Column(name = "unidades_por_paquete", nullable = false)
    private Integer unidadesPorPaquete;

    @Column(name = "unidades_generadas", insertable = false, updatable = false)
    private Integer unidadesGeneradas;

    @Column(name = "unidades_rotas", nullable = false)
    private Integer unidadesRotas;

    @Column(name = "unidades_disponibles", insertable = false, updatable = false)
    private Integer unidadesDisponibles;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_registro", nullable = false)
    private Usuario usuarioRegistro;

    @Column(name = "fecha_registro", nullable = false)
    private OffsetDateTime fechaRegistro;

    public UUID getIdPaqueteVasosAbierto() {
        return idPaqueteVasosAbierto;
    }

    public CajaDiaria getCajaDiaria() {
        return cajaDiaria;
    }

    public void setCajaDiaria(CajaDiaria cajaDiaria) {
        this.cajaDiaria = cajaDiaria;
    }

    public ItemInventario getItemInventario() {
        return itemInventario;
    }

    public void setItemInventario(ItemInventario itemInventario) {
        this.itemInventario = itemInventario;
    }

    public Integer getCantidadPaquetes() {
        return cantidadPaquetes;
    }

    public void setCantidadPaquetes(Integer cantidadPaquetes) {
        this.cantidadPaquetes = cantidadPaquetes;
    }

    public Integer getUnidadesPorPaquete() {
        return unidadesPorPaquete;
    }

    public void setUnidadesPorPaquete(Integer unidadesPorPaquete) {
        this.unidadesPorPaquete = unidadesPorPaquete;
    }

    public Integer getUnidadesGeneradas() {
        return unidadesGeneradas;
    }

    public Integer getUnidadesRotas() {
        return unidadesRotas;
    }

    public void setUnidadesRotas(Integer unidadesRotas) {
        this.unidadesRotas = unidadesRotas;
    }

    public Integer getUnidadesDisponibles() {
        return unidadesDisponibles;
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
}
