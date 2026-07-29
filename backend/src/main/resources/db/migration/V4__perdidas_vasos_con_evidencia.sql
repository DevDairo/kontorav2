-- ============================================================
-- PERDIDAS DE VASOS CON EVIDENCIA FOTOGRAFICA
-- ============================================================

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_type AS t
        JOIN pg_namespace AS n ON n.oid = t.typnamespace
        WHERE t.typname = 'estado_perdida_inventario_enum'
          AND n.nspname = current_schema()
    ) THEN
        CREATE TYPE estado_perdida_inventario_enum AS ENUM ('registrada', 'anulada');
    END IF;
END $$;

ALTER TYPE tipo_movimiento_inventario_enum ADD VALUE IF NOT EXISTS 'anulacion_perdida';

CREATE TABLE IF NOT EXISTS perdidas_inventario (
    id_perdida_inventario UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_caja_diaria UUID NOT NULL REFERENCES cajas_diarias(id_caja_diaria),
    id_item_inventario UUID NOT NULL REFERENCES items_inventario(id_item_inventario),
    id_paquete_vasos_abierto UUID REFERENCES paquetes_vasos_abiertos(id_paquete_vasos_abierto),
    cantidad INTEGER NOT NULL,
    motivo TEXT NOT NULL,
    id_usuario_registro UUID NOT NULL REFERENCES usuarios(id_usuario),
    fecha_registro TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    estado estado_perdida_inventario_enum NOT NULL DEFAULT 'registrada',
    id_usuario_anulacion UUID REFERENCES usuarios(id_usuario),
    fecha_anulacion TIMESTAMPTZ,
    motivo_anulacion TEXT,
    CONSTRAINT chk_perdidas_inventario_cantidad CHECK (cantidad > 0),
    CONSTRAINT chk_perdidas_inventario_motivo CHECK (length(trim(motivo)) > 0),
    CONSTRAINT chk_perdidas_inventario_anulacion CHECK (
        (
            estado = 'registrada'
            AND id_usuario_anulacion IS NULL
            AND fecha_anulacion IS NULL
            AND motivo_anulacion IS NULL
        )
        OR
        (
            estado = 'anulada'
            AND id_usuario_anulacion IS NOT NULL
            AND fecha_anulacion IS NOT NULL
            AND motivo_anulacion IS NOT NULL
            AND length(trim(motivo_anulacion)) > 0
        )
    )
);

COMMENT ON TABLE perdidas_inventario IS
'Vasos rotos o perdidos registrados durante una caja abierta. Descuentan el stock diario y admiten evidencia fotografica posterior al cierre.';

CREATE INDEX IF NOT EXISTS idx_perdidas_inventario_caja_fecha
ON perdidas_inventario(id_caja_diaria, fecha_registro DESC);

CREATE INDEX IF NOT EXISTS idx_perdidas_inventario_item
ON perdidas_inventario(id_item_inventario);

CREATE INDEX IF NOT EXISTS idx_perdidas_inventario_paquete
ON perdidas_inventario(id_paquete_vasos_abierto)
WHERE id_paquete_vasos_abierto IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_perdidas_inventario_estado
ON perdidas_inventario(estado);

DROP TRIGGER IF EXISTS trg_validar_caja_abierta_perdidas_inventario ON perdidas_inventario;
CREATE TRIGGER trg_validar_caja_abierta_perdidas_inventario
BEFORE INSERT OR UPDATE ON perdidas_inventario
FOR EACH ROW
EXECUTE FUNCTION validar_caja_abierta_por_columna('id_caja_diaria');

CREATE OR REPLACE FUNCTION validar_paquete_perdida_inventario()
RETURNS TRIGGER AS $$
DECLARE
    v_id_caja UUID;
    v_id_item UUID;
BEGIN
    IF NEW.id_paquete_vasos_abierto IS NULL THEN
        RETURN NEW;
    END IF;

    SELECT id_caja_diaria, id_item_inventario
    INTO v_id_caja, v_id_item
    FROM paquetes_vasos_abiertos
    WHERE id_paquete_vasos_abierto = NEW.id_paquete_vasos_abierto;

    IF v_id_caja IS NULL THEN
        RAISE EXCEPTION 'El paquete de vasos indicado no existe: %', NEW.id_paquete_vasos_abierto;
    END IF;

    IF v_id_caja <> NEW.id_caja_diaria OR v_id_item <> NEW.id_item_inventario THEN
        RAISE EXCEPTION 'El paquete no corresponde a la caja y al item de la perdida.';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_validar_paquete_perdida_inventario ON perdidas_inventario;
CREATE TRIGGER trg_validar_paquete_perdida_inventario
BEFORE INSERT OR UPDATE ON perdidas_inventario
FOR EACH ROW
EXECUTE FUNCTION validar_paquete_perdida_inventario();

ALTER TABLE archivos_evidencia
ADD COLUMN IF NOT EXISTS id_perdida_inventario UUID
REFERENCES perdidas_inventario(id_perdida_inventario) ON DELETE CASCADE;

ALTER TABLE archivos_evidencia
DROP CONSTRAINT IF EXISTS chk_archivos_relacion_unica;

ALTER TABLE archivos_evidencia
ADD CONSTRAINT chk_archivos_relacion_unica CHECK (
    num_nonnulls(
        id_pago_venta,
        id_gasto_caja,
        id_consignacion_bancaria,
        id_pago_servicio,
        id_perdida_inventario
    ) = 1
);

CREATE INDEX IF NOT EXISTS idx_archivos_evidencia_perdida
ON archivos_evidencia(id_perdida_inventario)
WHERE id_perdida_inventario IS NOT NULL;

COMMENT ON COLUMN archivos_evidencia.id_perdida_inventario IS
'Relacion con una perdida de vasos. La evidencia permanece vinculada aun cuando la perdida sea anulada.';
