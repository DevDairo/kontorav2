package com.kontora.pos.usuarios.repository;

import com.kontora.pos.usuarios.domain.SesionUsuario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SesionUsuarioRepository extends JpaRepository<SesionUsuario, UUID> {

    @EntityGraph(attributePaths = {"usuario", "usuario.rol"})
    Optional<SesionUsuario> findByTokenIdentificador(String tokenIdentificador);

    List<SesionUsuario> findAllByUsuario_IdUsuario(UUID idUsuario);
}

