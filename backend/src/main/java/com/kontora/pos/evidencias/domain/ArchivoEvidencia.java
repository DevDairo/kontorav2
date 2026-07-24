package com.kontora.pos.evidencias.domain;

import com.kontora.pos.caja.domain.GastoCaja;
import com.kontora.pos.deposito.domain.ConsignacionBancaria;
import com.kontora.pos.deposito.domain.PagoServicio;
import com.kontora.pos.usuarios.domain.Usuario;
import com.kontora.pos.ventas.domain.PagoVenta;
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

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "archivos_evidencia")
public class ArchivoEvidencia {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id_archivo_evidencia", nullable = false)
    private UUID idArchivoEvidencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pago_venta")
    private PagoVenta pagoVenta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_gasto_caja")
    private GastoCaja gastoCaja;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_consignacion_bancaria")
    private ConsignacionBancaria consignacionBancaria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pago_servicio")
    private PagoServicio pagoServicio;

    @Column(name = "url_archivo", nullable = false)
    private String urlArchivo;

    @Column(name = "nombre_archivo", nullable = false)
    private String nombreArchivo;

    @Column(name = "tipo_archivo", nullable = false, columnDefinition = "tipo_archivo_enum")
    @ColumnTransformer(write = "?::tipo_archivo_enum")
    private String tipoArchivo;

    @Column(name = "formato_archivo", nullable = false, columnDefinition = "formato_archivo_enum")
    @ColumnTransformer(write = "?::formato_archivo_enum")
    private String formatoArchivo;

    @Column(name = "tamano_original_kb")
    private Integer tamanoOriginalKb;

    @Column(name = "tamano_comprimido_kb")
    private Integer tamanoComprimidoKb;

    @Column(name = "fue_comprimido", nullable = false)
    private boolean fueComprimido;

    @Column(name = "fecha_subida", nullable = false)
    private OffsetDateTime fechaSubida;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_subida", nullable = false)
    private Usuario usuarioSubida;

    @Column(name = "estado", nullable = false, columnDefinition = "estado_basico_enum")
    @ColumnTransformer(write = "?::estado_basico_enum")
    private String estado;

    public UUID getIdArchivoEvidencia() {
        return idArchivoEvidencia;
    }

    public PagoVenta getPagoVenta() {
        return pagoVenta;
    }

    public void setPagoVenta(PagoVenta pagoVenta) {
        this.pagoVenta = pagoVenta;
    }

    public GastoCaja getGastoCaja() {
        return gastoCaja;
    }

    public void setGastoCaja(GastoCaja gastoCaja) {
        this.gastoCaja = gastoCaja;
    }

    public ConsignacionBancaria getConsignacionBancaria() {
        return consignacionBancaria;
    }

    public void setConsignacionBancaria(ConsignacionBancaria consignacionBancaria) {
        this.consignacionBancaria = consignacionBancaria;
    }

    public PagoServicio getPagoServicio() {
        return pagoServicio;
    }

    public void setPagoServicio(PagoServicio pagoServicio) {
        this.pagoServicio = pagoServicio;
    }

    public String getUrlArchivo() {
        return urlArchivo;
    }

    public void setUrlArchivo(String urlArchivo) {
        this.urlArchivo = urlArchivo;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }

    public String getTipoArchivo() {
        return tipoArchivo;
    }

    public void setTipoArchivo(String tipoArchivo) {
        this.tipoArchivo = tipoArchivo;
    }

    public String getFormatoArchivo() {
        return formatoArchivo;
    }

    public void setFormatoArchivo(String formatoArchivo) {
        this.formatoArchivo = formatoArchivo;
    }

    public Integer getTamanoOriginalKb() {
        return tamanoOriginalKb;
    }

    public void setTamanoOriginalKb(Integer tamanoOriginalKb) {
        this.tamanoOriginalKb = tamanoOriginalKb;
    }

    public Integer getTamanoComprimidoKb() {
        return tamanoComprimidoKb;
    }

    public void setTamanoComprimidoKb(Integer tamanoComprimidoKb) {
        this.tamanoComprimidoKb = tamanoComprimidoKb;
    }

    public boolean isFueComprimido() {
        return fueComprimido;
    }

    public void setFueComprimido(boolean fueComprimido) {
        this.fueComprimido = fueComprimido;
    }

    public OffsetDateTime getFechaSubida() {
        return fechaSubida;
    }

    public void setFechaSubida(OffsetDateTime fechaSubida) {
        this.fechaSubida = fechaSubida;
    }

    public Usuario getUsuarioSubida() {
        return usuarioSubida;
    }

    public void setUsuarioSubida(Usuario usuarioSubida) {
        this.usuarioSubida = usuarioSubida;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
