package com.kontora.pos.catalogos.repository;

import com.kontora.pos.catalogos.domain.PrecioGranizado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PrecioGranizadoRepository extends JpaRepository<PrecioGranizado, UUID> {

    @Query(value = """
            SELECT pg.*
            FROM precios_granizado pg
            JOIN tipos_granizado tg ON tg.id_tipo_granizado = pg.id_tipo_granizado
            JOIN tamanos_vaso tv ON tv.id_tamano_vaso = pg.id_tamano_vaso
            WHERE pg.estado = 'activo'
              AND tg.estado = 'activo'
              AND tv.estado = 'activo'
              AND pg.fecha_inicio_vigencia <= :fecha
              AND (pg.fecha_fin_vigencia IS NULL OR pg.fecha_fin_vigencia >= :fecha)
            ORDER BY tg.nombre_tipo, tv.onzas
            """, nativeQuery = true)
    List<PrecioGranizado> findVigentes(@Param("fecha") LocalDate fecha);

    @Query(value = """
            SELECT pg.*
            FROM precios_granizado pg
            WHERE pg.id_tipo_granizado = :idTipoGranizado
              AND pg.id_tamano_vaso = :idTamanoVaso
              AND pg.estado = 'activo'
              AND pg.fecha_inicio_vigencia <= :fecha
              AND (pg.fecha_fin_vigencia IS NULL OR pg.fecha_fin_vigencia >= :fecha)
            ORDER BY pg.fecha_inicio_vigencia DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<PrecioGranizado> findPrecioVigente(
            @Param("idTipoGranizado") UUID idTipoGranizado,
            @Param("idTamanoVaso") UUID idTamanoVaso,
            @Param("fecha") LocalDate fecha);

    @Query("""
            SELECT pg
            FROM PrecioGranizado pg
            JOIN FETCH pg.tipoGranizado
            JOIN FETCH pg.tamanoVaso
            ORDER BY pg.tipoGranizado.nombreTipo, pg.tamanoVaso.onzas, pg.fechaInicioVigencia DESC
            """)
    List<PrecioGranizado> findAllParaGestion();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT pg
            FROM PrecioGranizado pg
            WHERE pg.tipoGranizado.idTipoGranizado = :idTipoGranizado
              AND pg.tamanoVaso.idTamanoVaso = :idTamanoVaso
              AND pg.estado = 'activo'
              AND pg.fechaFinVigencia IS NULL
            """)
    Optional<PrecioGranizado> findPrecioAbiertoForUpdate(
            @Param("idTipoGranizado") UUID idTipoGranizado,
            @Param("idTamanoVaso") UUID idTamanoVaso);
}
