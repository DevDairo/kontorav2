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
@Table(name = "consumos_diarios_inventario")
public class ConsumoDiarioInventario {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id_consumo_diario_inventario", nullable = false)
    private UUID idConsumoDiarioInventario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_caja_diaria", nullable = false)
    private CajaDiaria cajaDiaria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_item_inventario", nullable = false)
    private ItemInventario itemInventario;

    @Column(name = "cantidad_consumida", nullable = false)
    private Integer cantidadConsumida;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_registro", nullable = false)
    private Usuario usuarioRegistro;

    @Column(name = "fecha_registro", nullable = false)
    private OffsetDateTime fechaRegistro;

    @Column(name = "observacion")
    private String observacion;

    public UUID getIdConsumoDiarioInventario() {
        return idConsumoDiarioInventario;
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

    public Integer getCantidadConsumida() {
        return cantidadConsumida;
    }

    public void setCantidadConsumida(Integer cantidadConsumida) {
        this.cantidadConsumida = cantidadConsumida;
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

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }
}
