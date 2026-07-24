package com.kontora.pos.catalogos.repository;

import com.kontora.pos.catalogos.domain.MetodoPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface MetodoPagoRepository extends JpaRepository<MetodoPago, UUID> {

    @Query(value = "SELECT * FROM metodos_pago WHERE estado = 'activo' ORDER BY nombre_metodo", nativeQuery = true)
    List<MetodoPago> findActivos();
}
