-- ============================================================
-- CORTESIAS SEPARADAS DE VENTAS Y DEL CIERRE FINANCIERO
-- ============================================================

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_type AS t
        JOIN pg_namespace AS n ON n.oid = t.typnamespace
        WHERE t.typname = 'estado_cortesia_enum'
          AND n.nspname = current_schema()
    ) THEN
        CREATE TYPE estado_cortesia_enum AS ENUM ('registrada', 'anulada');
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_type AS t
        JOIN pg_namespace AS n ON n.oid = t.typnamespace
        WHERE t.typname = 'tipo_beneficiario_cortesia_enum'
          AND n.nspname = current_schema()
    ) THEN
        CREATE TYPE tipo_beneficiario_cortesia_enum AS ENUM ('trabajador', 'otro');
    END IF;
END $$;

ALTER TYPE tipo_movimiento_inventario_enum ADD VALUE IF NOT EXISTS 'cortesia';
ALTER TYPE tipo_movimiento_inventario_enum ADD VALUE IF NOT EXISTS 'anulacion_cortesia';

ALTER TABLE existencias_inventario_diario
ADD COLUMN IF NOT EXISTS cantidad_cortesia INTEGER NOT NULL DEFAULT 0;

ALTER TABLE existencias_inventario_diario
DROP CONSTRAINT IF EXISTS chk_existencias_diario_cortesia;

ALTER TABLE existencias_inventario_diario
ADD CONSTRAINT chk_existencias_diario_cortesia
CHECK (cantidad_cortesia >= 0);

COMMENT ON COLUMN existencias_inventario_diario.cantidad_cortesia IS
'Vasos entregados como cortesia durante la jornada. Disminuyen el stock diario y no generan valores financieros.';

CREATE TABLE IF NOT EXISTS cortesias (
    id_cortesia UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_caja_diaria UUID NOT NULL REFERENCES cajas_diarias(id_caja_diaria),
    id_usuario_registro UUID NOT NULL REFERENCES usuarios(id_usuario),
    tipo_beneficiario tipo_beneficiario_cortesia_enum NOT NULL,
    id_usuario_beneficiario UUID REFERENCES usuarios(id_usuario),
    referencia_otro TEXT,
    motivo_otro TEXT,
    fecha_registro TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    estado estado_cortesia_enum NOT NULL DEFAULT 'registrada',
    id_usuario_anulacion UUID REFERENCES usuarios(id_usuario),
    fecha_anulacion TIMESTAMPTZ,
    motivo_anulacion TEXT,
    CONSTRAINT chk_cortesias_beneficiario CHECK (
        (
            tipo_beneficiario = 'trabajador'
            AND id_usuario_beneficiario IS NOT NULL
            AND motivo_otro IS NULL
        )
        OR
        (
            tipo_beneficiario = 'otro'
            AND id_usuario_beneficiario IS NULL
            AND motivo_otro IS NOT NULL
            AND length(trim(motivo_otro)) > 0
        )
    ),
    CONSTRAINT chk_cortesias_referencia_otro CHECK (
        referencia_otro IS NULL OR length(trim(referencia_otro)) > 0
    ),
    CONSTRAINT chk_cortesias_anulacion CHECK (
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

COMMENT ON TABLE cortesias IS
'Operaciones sin valor financiero que entregan producto y descuentan vasos del stock diario.';

CREATE TABLE IF NOT EXISTS detalles_cortesia (
    id_detalle_cortesia UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_cortesia UUID NOT NULL REFERENCES cortesias(id_cortesia) ON DELETE CASCADE,
    id_tipo_granizado UUID NOT NULL REFERENCES tipos_granizado(id_tipo_granizado),
    id_tamano_vaso UUID NOT NULL REFERENCES tamanos_vaso(id_tamano_vaso),
    cantidad INTEGER NOT NULL,
    CONSTRAINT chk_detalles_cortesia_cantidad CHECK (cantidad > 0),
    CONSTRAINT uq_detalles_cortesia_tipo_tamano UNIQUE (
        id_cortesia,
        id_tipo_granizado,
        id_tamano_vaso
    )
);

COMMENT ON TABLE detalles_cortesia IS
'Detalle fisico de la cortesia. No contiene precios, promociones ni formas de pago.';

CREATE INDEX IF NOT EXISTS idx_cortesias_caja_fecha
ON cortesias(id_caja_diaria, fecha_registro DESC);

CREATE INDEX IF NOT EXISTS idx_cortesias_beneficiario
ON cortesias(id_usuario_beneficiario)
WHERE id_usuario_beneficiario IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_cortesias_estado
ON cortesias(estado);

CREATE INDEX IF NOT EXISTS idx_detalles_cortesia_cortesia
ON detalles_cortesia(id_cortesia);

DROP TRIGGER IF EXISTS trg_validar_caja_abierta_cortesias ON cortesias;
CREATE TRIGGER trg_validar_caja_abierta_cortesias
BEFORE INSERT OR UPDATE ON cortesias
FOR EACH ROW
EXECUTE FUNCTION validar_caja_abierta_por_columna('id_caja_diaria');

CREATE OR REPLACE FUNCTION validar_caja_abierta_detalle_cortesia()
RETURNS TRIGGER AS $$
DECLARE
    v_estado estado_caja_enum;
BEGIN
    SELECT cd.estado_caja
    INTO v_estado
    FROM cortesias c
    JOIN cajas_diarias cd ON cd.id_caja_diaria = c.id_caja_diaria
    WHERE c.id_cortesia = NEW.id_cortesia;

    IF v_estado IS NULL THEN
        RAISE EXCEPTION 'La cortesia indicada no existe: %', NEW.id_cortesia;
    END IF;

    IF v_estado <> 'abierta' THEN
        RAISE EXCEPTION 'No se puede modificar una cortesia cuya caja esta cerrada.';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_validar_caja_abierta_detalles_cortesia ON detalles_cortesia;
CREATE TRIGGER trg_validar_caja_abierta_detalles_cortesia
BEFORE INSERT OR UPDATE ON detalles_cortesia
FOR EACH ROW
EXECUTE FUNCTION validar_caja_abierta_detalle_cortesia();
