package com.kontora.pos.catalogos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnTransformer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "promociones")
public class Promocion {

    @Id
    @Column(name = "id_promocion", nullable = false)
    private UUID idPromocion;

    @Column(name = "nombre_promocion", nullable = false, unique = true)
    private String nombrePromocion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_granizado", nullable = false)
    private TipoGranizado tipoGranizado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tamano_vaso", nullable = false)
    private TamanoVaso tamanoVaso;

    @Column(name = "tipo_beneficiario", nullable = false, columnDefinition = "tipo_beneficiario_enum")
    @ColumnTransformer(write = "?::tipo_beneficiario_enum")
    private String tipoBeneficiario;

    @Column(name = "cantidad_requerida", nullable = false)
    private Integer cantidadRequerida;

    @Column(name = "valor_promocional", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorPromocional;

    @Column(name = "fecha_inicio_vigencia", nullable = false)
    private LocalDate fechaInicioVigencia;

    @Column(name = "fecha_fin_vigencia")
    private LocalDate fechaFinVigencia;

    @Column(name = "estado", nullable = false, columnDefinition = "estado_basico_enum")
    @ColumnTransformer(write = "?::estado_basico_enum")
    private String estado;

    @OneToMany(mappedBy = "promocion")
    @OrderBy("diaSemana ASC")
    private List<DiaPromocion> diasPromocion;

    public UUID getIdPromocion() {
        return idPromocion;
    }

    public String getNombrePromocion() {
        return nombrePromocion;
    }

    public TipoGranizado getTipoGranizado() {
        return tipoGranizado;
    }

    public TamanoVaso getTamanoVaso() {
        return tamanoVaso;
    }

    public String getTipoBeneficiario() {
        return tipoBeneficiario;
    }

    public Integer getCantidadRequerida() {
        return cantidadRequerida;
    }

    public BigDecimal getValorPromocional() {
        return valorPromocional;
    }

    public LocalDate getFechaInicioVigencia() {
        return fechaInicioVigencia;
    }

    public LocalDate getFechaFinVigencia() {
        return fechaFinVigencia;
    }

    public String getEstado() {
        return estado;
    }

    public List<DiaPromocion> getDiasPromocion() {
        return diasPromocion;
    }
}
