package com.kontora.pos.inventario.repository;

import com.kontora.pos.inventario.domain.PaqueteVasosAbierto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaqueteVasosAbiertoRepository extends JpaRepository<PaqueteVasosAbierto, UUID> {
}
