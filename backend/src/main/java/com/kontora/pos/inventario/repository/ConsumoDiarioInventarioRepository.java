package com.kontora.pos.inventario.repository;

import com.kontora.pos.inventario.domain.ConsumoDiarioInventario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ConsumoDiarioInventarioRepository extends JpaRepository<ConsumoDiarioInventario, UUID> {
}
