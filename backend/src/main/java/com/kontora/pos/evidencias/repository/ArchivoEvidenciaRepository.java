package com.kontora.pos.evidencias.repository;

import com.kontora.pos.evidencias.domain.ArchivoEvidencia;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ArchivoEvidenciaRepository extends JpaRepository<ArchivoEvidencia, UUID> {

    @EntityGraph(attributePaths = {
            "pagoVenta",
            "pagoVenta.venta",
            "pagoVenta.venta.usuarioVendedor",
            "pagoVenta.metodoPago",
            "gastoCaja",
            "gastoCaja.usuarioRegistro",
            "consignacionBancaria",
            "consignacionBancaria.usuarioRegistro",
            "pagoServicio",
            "pagoServicio.usuarioRegistro",
            "usuarioSubida"
    })
    Optional<ArchivoEvidencia> findByIdArchivoEvidencia(UUID idArchivoEvidencia);

    @EntityGraph(attributePaths = {"pagoVenta", "pagoVenta.venta", "pagoVenta.venta.usuarioVendedor", "pagoVenta.metodoPago", "usuarioSubida"})
    List<ArchivoEvidencia> findByPagoVenta_IdPagoVentaOrderByFechaSubidaDesc(UUID idPagoVenta);

    @EntityGraph(attributePaths = {"gastoCaja", "gastoCaja.usuarioRegistro", "usuarioSubida"})
    List<ArchivoEvidencia> findByGastoCaja_IdGastoCajaOrderByFechaSubidaDesc(UUID idGastoCaja);

    @EntityGraph(attributePaths = {"consignacionBancaria", "consignacionBancaria.usuarioRegistro", "usuarioSubida"})
    List<ArchivoEvidencia> findByConsignacionBancaria_IdConsignacionBancariaOrderByFechaSubidaDesc(UUID idConsignacionBancaria);

    @EntityGraph(attributePaths = {"pagoServicio", "pagoServicio.usuarioRegistro", "usuarioSubida"})
    List<ArchivoEvidencia> findByPagoServicio_IdPagoServicioOrderByFechaSubidaDesc(UUID idPagoServicio);
}
