package com.kontora.pos.usuarios.repository;

import com.kontora.pos.usuarios.domain.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RolRepository extends JpaRepository<Rol, UUID> {

    @Query(value = "SELECT * FROM roles WHERE estado = 'activo' ORDER BY nombre_rol", nativeQuery = true)
    List<Rol> findActivos();

    Optional<Rol> findByNombreRol(String nombreRol);
}
