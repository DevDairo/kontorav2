package com.kontora.pos.usuarios.repository;

import com.kontora.pos.usuarios.domain.Usuario;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    @EntityGraph(attributePaths = "rol")
    Optional<Usuario> findByNombreUsuario(String nombreUsuario);

    @EntityGraph(attributePaths = "rol")
    List<Usuario> findAllByOrderByNombreCompletoAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM Usuario u WHERE u.idUsuario = :idUsuario")
    Optional<Usuario> findByIdParaActualizar(@Param("idUsuario") UUID idUsuario);

    boolean existsByNombreUsuario(String nombreUsuario);
}
