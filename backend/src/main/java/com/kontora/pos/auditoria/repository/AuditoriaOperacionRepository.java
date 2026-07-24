package com.kontora.pos.auditoria.repository;

import com.kontora.pos.auditoria.domain.AuditoriaOperacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditoriaOperacionRepository extends JpaRepository<AuditoriaOperacion, UUID> {
}
