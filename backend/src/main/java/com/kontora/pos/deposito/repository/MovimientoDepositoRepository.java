package com.kontora.pos.deposito.repository;

import com.kontora.pos.deposito.domain.MovimientoDeposito;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface MovimientoDepositoRepository extends JpaRepository<MovimientoDeposito, UUID> {

    @Query(value = """
            SELECT COALESCE((
                SELECT saldo_posterior
                FROM movimientos_deposito
                ORDER BY fecha_movimiento DESC, id_movimiento_deposito DESC
                LIMIT 1
            ), 0)
            """, nativeQuery = true)
    BigDecimal obtenerSaldoActual();

    @EntityGraph(attributePaths = {"cierreCaja", "usuarioRegistro"})
    Optional<MovimientoDeposito> findByCierreCaja_IdCierreCaja(UUID idCierreCaja);
}
