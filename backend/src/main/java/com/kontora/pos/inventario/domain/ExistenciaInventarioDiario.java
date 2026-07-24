package com.kontora.pos.inventario.domain;

import com.kontora.pos.caja.domain.CajaDiaria;
import com.kontora.pos.catalogos.domain.ItemInventario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.GenericGenerator;

import java.util.UUID;

@Entity
@Table(name = "existencias_inventario_diario")
public class ExistenciaInventarioDiario {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id_existencia_diaria", nullable = false)
    private UUID idExistenciaDiaria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_caja_diaria", nullable = false)
    private CajaDiaria cajaDiaria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_item_inventario", nullable = false)
    private ItemInventario itemInventario;

    @Column(name = "cantidad_inicial", nullable = false)
    private Integer cantidadInicial;

    @Column(name = "cantidad_ingresada", nullable = false)
    private Integer cantidadIngresada;

    @Column(name = "cantidad_vendida", nullable = false)
    private Integer cantidadVendida;

    @Column(name = "cantidad_perdida", nullable = false)
    private Integer cantidadPerdida;

    @Column(name = "cantidad_ajustada", nullable = false)
    private Integer cantidadAjustada;

    @Column(name = "cantidad_final_teorica")
    private Integer cantidadFinalTeorica;

    @Column(name = "cantidad_final_contada")
    private Integer cantidadFinalContada;

    @Column(name = "diferencia")
    private Integer diferencia;

    public UUID getIdExistenciaDiaria() {
        return idExistenciaDiaria;
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

    public Integer getCantidadInicial() {
        return cantidadInicial;
    }

    public void setCantidadInicial(Integer cantidadInicial) {
        this.cantidadInicial = cantidadInicial;
    }

    public Integer getCantidadIngresada() {
        return cantidadIngresada;
    }

    public void setCantidadIngresada(Integer cantidadIngresada) {
        this.cantidadIngresada = cantidadIngresada;
    }

    public Integer getCantidadVendida() {
        return cantidadVendida;
    }

    public void setCantidadVendida(Integer cantidadVendida) {
        this.cantidadVendida = cantidadVendida;
    }

    public Integer getCantidadPerdida() {
        return cantidadPerdida;
    }

    public void setCantidadPerdida(Integer cantidadPerdida) {
        this.cantidadPerdida = cantidadPerdida;
    }

    public Integer getCantidadAjustada() {
        return cantidadAjustada;
    }

    public void setCantidadAjustada(Integer cantidadAjustada) {
        this.cantidadAjustada = cantidadAjustada;
    }

    public Integer getCantidadFinalTeorica() {
        return cantidadFinalTeorica;
    }

    public void setCantidadFinalTeorica(Integer cantidadFinalTeorica) {
        this.cantidadFinalTeorica = cantidadFinalTeorica;
    }

    public Integer getCantidadFinalContada() {
        return cantidadFinalContada;
    }

    public Integer getDiferencia() {
        return diferencia;
    }
}
