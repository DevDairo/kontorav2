package com.kontora.pos.usuarios.repository;

import com.kontora.pos.usuarios.domain.CredencialUsuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CredencialUsuarioRepository extends JpaRepository<CredencialUsuario, UUID> {

    Optional<CredencialUsuario> findByUsuario_IdUsuario(UUID idUsuario);
}

