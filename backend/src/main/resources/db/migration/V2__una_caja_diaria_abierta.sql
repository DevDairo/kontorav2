CREATE UNIQUE INDEX IF NOT EXISTS uq_cajas_diarias_una_abierta
ON cajas_diarias (estado_caja)
WHERE estado_caja = 'abierta'::estado_caja_enum;
