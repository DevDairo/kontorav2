package com.kontora.pos.deposito.service;

import com.kontora.pos.deposito.repository.MovimientoDepositoRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class DepositoSaldoService {

    private static final long BLOQUEO_SALDO_DEPOSITO = 8_804_202_607_100L;

    private final MovimientoDepositoRepository movimientoDepositoRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public DepositoSaldoService(MovimientoDepositoRepository movimientoDepositoRepository) {
        this.movimientoDepositoRepository = movimientoDepositoRepository;
    }

    @Transactional(readOnly = true)
    public BigDecimal obtenerSaldoActual() {
        return normalizarMoneda(movimientoDepositoRepository.obtenerSaldoActual());
    }

    @Transactional
    public BigDecimal bloquearYObtenerSaldoActual() {
        entityManager.createNativeQuery("""
                SELECT 1
                FROM (SELECT pg_advisory_xact_lock(%d)) AS bloqueo
                """.formatted(BLOQUEO_SALDO_DEPOSITO))
                .getSingleResult();
        return obtenerSaldoActual();
    }

    private BigDecimal normalizarMoneda(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
}
