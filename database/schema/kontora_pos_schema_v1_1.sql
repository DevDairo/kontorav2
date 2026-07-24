-- ============================================================
-- Script de creacion de base de datos - Kontora POS
-- SGBD objetivo: PostgreSQL / Supabase
-- Version: 1.0
-- Fecha: 2026-06-24
--
-- Alcance:
-- - Un solo local.
-- - Una sola caja diaria.
-- - Backend externo en VM con autenticacion propia JWT.
-- - Supabase usado como PostgreSQL y almacenamiento de evidencias.
-- - Nombres de tablas y campos en espanol sin tildes.
-- ============================================================

BEGIN;

-- ============================================================
-- 1. EXTENSIONES
-- ============================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ============================================================
-- 2. TIPOS ENUM CONTROLADOS
-- ============================================================

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'estado_basico_enum') THEN
        CREATE TYPE estado_basico_enum AS ENUM ('activo', 'inactivo');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'estado_usuario_enum') THEN
        CREATE TYPE estado_usuario_enum AS ENUM ('activo', 'inactivo', 'bloqueado');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'estado_credencial_enum') THEN
        CREATE TYPE estado_credencial_enum AS ENUM ('activa', 'bloqueada', 'expirada', 'revocada');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'estado_sesion_enum') THEN
        CREATE TYPE estado_sesion_enum AS ENUM ('activa', 'cerrada', 'expirada', 'revocada');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'estado_caja_enum') THEN
        CREATE TYPE estado_caja_enum AS ENUM ('abierta', 'cerrada');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'estado_venta_enum') THEN
        CREATE TYPE estado_venta_enum AS ENUM ('registrada', 'anulada');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'tipo_comprador_enum') THEN
        CREATE TYPE tipo_comprador_enum AS ENUM ('cliente', 'trabajador');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'estado_validacion_transferencia_enum') THEN
        CREATE TYPE estado_validacion_transferencia_enum AS ENUM ('no_aplica', 'pendiente', 'validada', 'rechazada');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'tipo_beneficiario_enum') THEN
        CREATE TYPE tipo_beneficiario_enum AS ENUM ('cliente', 'trabajador', 'todos');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'dia_semana_enum') THEN
        CREATE TYPE dia_semana_enum AS ENUM ('lunes', 'martes', 'miercoles', 'jueves', 'viernes', 'sabado', 'domingo');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'estado_gasto_enum') THEN
        CREATE TYPE estado_gasto_enum AS ENUM ('registrado', 'editado', 'anulado');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'tipo_control_inventario_enum') THEN
        CREATE TYPE tipo_control_inventario_enum AS ENUM ('automatico_por_venta', 'manual_por_consumo');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'tipo_stock_enum') THEN
        CREATE TYPE tipo_stock_enum AS ENUM ('general', 'diario');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'tipo_movimiento_inventario_enum') THEN
        CREATE TYPE tipo_movimiento_inventario_enum AS ENUM (
            'entrada',
            'salida',
            'venta',
            'anulacion_venta',
            'perdida',
            'ajuste',
            'consumo_diario',
            'apertura_paquete'
        );
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'sentido_movimiento_enum') THEN
        CREATE TYPE sentido_movimiento_enum AS ENUM ('entrada', 'salida');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'estado_aprobacion_enum') THEN
        CREATE TYPE estado_aprobacion_enum AS ENUM ('pendiente', 'aprobado', 'rechazado');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'tipo_movimiento_deposito_enum') THEN
        CREATE TYPE tipo_movimiento_deposito_enum AS ENUM (
            'entrada_cierre',
            'salida_consignacion',
            'salida_pago_servicio',
            'ajuste'
        );
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'estado_registro_financiero_enum') THEN
        CREATE TYPE estado_registro_financiero_enum AS ENUM ('registrado', 'anulado');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'tipo_valor_configuracion_enum') THEN
        CREATE TYPE tipo_valor_configuracion_enum AS ENUM ('texto', 'entero', 'decimal', 'booleano');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'tipo_archivo_enum') THEN
        CREATE TYPE tipo_archivo_enum AS ENUM ('imagen', 'pdf', 'otro');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'formato_archivo_enum') THEN
        CREATE TYPE formato_archivo_enum AS ENUM ('jpg', 'jpeg', 'png', 'webp', 'pdf', 'otro');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'accion_auditoria_enum') THEN
        CREATE TYPE accion_auditoria_enum AS ENUM (
            'crear',
            'editar',
            'anular',
            'aprobar',
            'rechazar',
            'abrir',
            'cerrar',
            'validar',
            'revocar',
            'login',
            'logout'
        );
    END IF;
END $$;

-- ============================================================
-- 3. FUNCION GENERAL PARA ACTUALIZAR FECHA DE MODIFICACION
-- ============================================================

CREATE OR REPLACE FUNCTION actualizar_fecha_actualizacion()
RETURNS TRIGGER AS $$
BEGIN
    NEW.fecha_actualizacion = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ============================================================
-- 4. MODULO DE SEGURIDAD Y USUARIOS
-- ============================================================

CREATE TABLE IF NOT EXISTS roles (
    id_rol UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre_rol TEXT NOT NULL UNIQUE,
    estado estado_basico_enum NOT NULL DEFAULT 'activo'
);

COMMENT ON TABLE roles IS 'Catalogo de roles del sistema: vendedor, administrador y gerente.';
COMMENT ON COLUMN roles.id_rol IS 'Llave primaria del rol.';
COMMENT ON COLUMN roles.nombre_rol IS 'Nombre unico del rol dentro del sistema.';

CREATE TABLE IF NOT EXISTS usuarios (
    id_usuario UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_rol UUID NOT NULL REFERENCES roles(id_rol),
    nombre_usuario TEXT NOT NULL UNIQUE,
    nombre_completo TEXT NOT NULL,
    estado estado_usuario_enum NOT NULL DEFAULT 'activo',
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_usuarios_nombre_usuario_no_vacio CHECK (length(trim(nombre_usuario)) > 0),
    CONSTRAINT chk_usuarios_nombre_completo_no_vacio CHECK (length(trim(nombre_completo)) > 0)
);

COMMENT ON TABLE usuarios IS 'Usuarios internos de Kontora POS. El acceso se realiza con nombre_usuario, no con correo electronico.';
COMMENT ON COLUMN usuarios.id_usuario IS 'Llave primaria del usuario.';
COMMENT ON COLUMN usuarios.id_rol IS 'Llave foranea hacia roles.';
COMMENT ON COLUMN usuarios.nombre_usuario IS 'Identificador de inicio de sesion. Puede contener letras o numeros.';

DROP TRIGGER IF EXISTS trg_usuarios_fecha_actualizacion ON usuarios;
CREATE TRIGGER trg_usuarios_fecha_actualizacion
BEFORE UPDATE ON usuarios
FOR EACH ROW
EXECUTE FUNCTION actualizar_fecha_actualizacion();

CREATE TABLE IF NOT EXISTS credenciales_usuario (
    id_credencial_usuario UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_usuario UUID NOT NULL UNIQUE REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    contrasena_hash TEXT NOT NULL,
    requiere_cambio_contrasena BOOLEAN NOT NULL DEFAULT TRUE,
    intentos_fallidos INTEGER NOT NULL DEFAULT 0,
    fecha_ultimo_acceso TIMESTAMPTZ,
    fecha_cambio_contrasena TIMESTAMPTZ,
    estado estado_credencial_enum NOT NULL DEFAULT 'activa',
    CONSTRAINT chk_credenciales_intentos_fallidos CHECK (intentos_fallidos >= 0),
    CONSTRAINT chk_credenciales_hash_no_vacio CHECK (length(trim(contrasena_hash)) > 0)
);

COMMENT ON TABLE credenciales_usuario IS 'Credenciales de autenticacion. Guarda hash seguro, nunca contrasenas en texto plano.';
COMMENT ON COLUMN credenciales_usuario.id_usuario IS 'Relacion 1:1 con usuarios.';
COMMENT ON COLUMN credenciales_usuario.contrasena_hash IS 'Hash de contrasena generado por el backend, recomendado BCrypt o Argon2.';

CREATE TABLE IF NOT EXISTS sesiones_usuario (
    id_sesion_usuario UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_usuario UUID NOT NULL REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    token_identificador TEXT NOT NULL UNIQUE,
    fecha_inicio TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    fecha_expiracion TIMESTAMPTZ NOT NULL,
    fecha_cierre TIMESTAMPTZ,
    estado_sesion estado_sesion_enum NOT NULL DEFAULT 'activa',
    direccion_ip TEXT,
    user_agent TEXT,
    CONSTRAINT chk_sesiones_fechas CHECK (fecha_expiracion > fecha_inicio),
    CONSTRAINT chk_sesiones_token_no_vacio CHECK (length(trim(token_identificador)) > 0)
);

COMMENT ON TABLE sesiones_usuario IS 'Sesiones de usuario para controlar JWT activos, cerrados, expirados o revocados.';
COMMENT ON COLUMN sesiones_usuario.token_identificador IS 'Identificador unico del token JWT, por ejemplo el claim jti. No debe almacenar el JWT completo.';

CREATE INDEX IF NOT EXISTS idx_sesiones_usuario_id_usuario ON sesiones_usuario(id_usuario);
CREATE INDEX IF NOT EXISTS idx_sesiones_usuario_estado ON sesiones_usuario(estado_sesion);

-- ============================================================
-- 5. MODULO DE CAJA DIARIA
-- ============================================================

CREATE TABLE IF NOT EXISTS cajas_diarias (
    id_caja_diaria UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    fecha_operacion DATE NOT NULL UNIQUE,
    estado_caja estado_caja_enum NOT NULL DEFAULT 'abierta',
    valor_base NUMERIC(12,2) NOT NULL DEFAULT 300000,
    fecha_apertura TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    fecha_cierre TIMESTAMPTZ,
    id_usuario_apertura UUID NOT NULL REFERENCES usuarios(id_usuario),
    id_usuario_cierre UUID REFERENCES usuarios(id_usuario),
    observaciones TEXT,
    CONSTRAINT chk_cajas_valor_base CHECK (valor_base >= 0),
    CONSTRAINT chk_cajas_cierre_consistente CHECK (
        (estado_caja = 'abierta' AND fecha_cierre IS NULL AND id_usuario_cierre IS NULL)
        OR
        (estado_caja = 'cerrada' AND fecha_cierre IS NOT NULL AND id_usuario_cierre IS NOT NULL)
    )
);

COMMENT ON TABLE cajas_diarias IS 'Representa la caja o jornada diaria. Debe ser abierta por administrador o gerente antes de vender.';
COMMENT ON COLUMN cajas_diarias.valor_base IS 'Base fija de caja. Se excluye del efectivo contado y no se suma al deposito.';

CREATE INDEX IF NOT EXISTS idx_cajas_diarias_estado ON cajas_diarias(estado_caja);

CREATE TABLE IF NOT EXISTS cierres_caja (
    id_cierre_caja UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_caja_diaria UUID NOT NULL UNIQUE REFERENCES cajas_diarias(id_caja_diaria),
    total_ventas NUMERIC(12,2) NOT NULL DEFAULT 0,
    total_ventas_efectivo NUMERIC(12,2) NOT NULL DEFAULT 0,
    total_ventas_transferencia NUMERIC(12,2) NOT NULL DEFAULT 0,
    total_transferencias_pendientes NUMERIC(12,2) NOT NULL DEFAULT 0,
    total_transferencias_validadas NUMERIC(12,2) NOT NULL DEFAULT 0,
    total_transferencias_rechazadas NUMERIC(12,2) NOT NULL DEFAULT 0,
    total_gastos NUMERIC(12,2) NOT NULL DEFAULT 0,
    total_adiciones NUMERIC(12,2) NOT NULL DEFAULT 0,
    total_pago_trabajadores NUMERIC(12,2) NOT NULL DEFAULT 0,
    efectivo_esperado_sin_base NUMERIC(12,2) NOT NULL DEFAULT 0,
    efectivo_contado_sin_base NUMERIC(12,2) NOT NULL DEFAULT 0,
    diferencia_caja NUMERIC(12,2) NOT NULL DEFAULT 0,
    valor_a_deposito NUMERIC(12,2) NOT NULL DEFAULT 0,
    fecha_cierre TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    id_usuario_cierre UUID NOT NULL REFERENCES usuarios(id_usuario),
    observaciones TEXT,
    CONSTRAINT chk_cierres_totales_no_negativos CHECK (
        total_ventas >= 0 AND
        total_ventas_efectivo >= 0 AND
        total_ventas_transferencia >= 0 AND
        total_transferencias_pendientes >= 0 AND
        total_transferencias_validadas >= 0 AND
        total_transferencias_rechazadas >= 0 AND
        total_gastos >= 0 AND
        total_adiciones >= 0 AND
        total_pago_trabajadores >= 0 AND
        efectivo_esperado_sin_base >= 0 AND
        efectivo_contado_sin_base >= 0 AND
        valor_a_deposito >= 0
    )
);

COMMENT ON TABLE cierres_caja IS 'Resumen financiero de la jornada. Una caja diaria solo puede tener un cierre.';
COMMENT ON COLUMN cierres_caja.valor_a_deposito IS 'Valor que entra al deposito. Corresponde al efectivo restante menos la base.';

-- ============================================================
-- 6. MODULO DE VENTAS, PRECIOS Y PROMOCIONES
-- ============================================================

CREATE TABLE IF NOT EXISTS tipos_granizado (
    id_tipo_granizado UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre_tipo TEXT NOT NULL UNIQUE,
    estado estado_basico_enum NOT NULL DEFAULT 'activo'
);

COMMENT ON TABLE tipos_granizado IS 'Catalogo de tipos de granizado: con licor y sin licor.';

CREATE TABLE IF NOT EXISTS tamanos_vaso (
    id_tamano_vaso UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    onzas INTEGER NOT NULL UNIQUE,
    estado estado_basico_enum NOT NULL DEFAULT 'activo',
    CONSTRAINT chk_tamanos_vaso_onzas CHECK (onzas > 0)
);

COMMENT ON TABLE tamanos_vaso IS 'Catalogo de tamanos de vaso en onzas.';

CREATE TABLE IF NOT EXISTS precios_granizado (
    id_precio_granizado UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_tipo_granizado UUID NOT NULL REFERENCES tipos_granizado(id_tipo_granizado),
    id_tamano_vaso UUID NOT NULL REFERENCES tamanos_vaso(id_tamano_vaso),
    valor_precio NUMERIC(12,2) NOT NULL,
    fecha_inicio_vigencia DATE NOT NULL,
    fecha_fin_vigencia DATE,
    estado estado_basico_enum NOT NULL DEFAULT 'activo',
    id_usuario_creacion UUID REFERENCES usuarios(id_usuario),
    CONSTRAINT chk_precios_valor CHECK (valor_precio > 0),
    CONSTRAINT chk_precios_vigencia CHECK (
        fecha_fin_vigencia IS NULL OR fecha_fin_vigencia >= fecha_inicio_vigencia
    )
);

COMMENT ON TABLE precios_granizado IS 'Precios historicos de carta por tipo de granizado y tamano de vaso.';
COMMENT ON COLUMN precios_granizado.fecha_fin_vigencia IS 'Campo opcional. Nulo representa precio vigente sin fecha final definida.';

CREATE UNIQUE INDEX IF NOT EXISTS uq_precio_vigente_tipo_tamano
ON precios_granizado(id_tipo_granizado, id_tamano_vaso)
WHERE estado = 'activo' AND fecha_fin_vigencia IS NULL;

CREATE TABLE IF NOT EXISTS promociones (
    id_promocion UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre_promocion TEXT NOT NULL UNIQUE,
    id_tipo_granizado UUID NOT NULL REFERENCES tipos_granizado(id_tipo_granizado),
    id_tamano_vaso UUID NOT NULL REFERENCES tamanos_vaso(id_tamano_vaso),
    tipo_beneficiario tipo_beneficiario_enum NOT NULL DEFAULT 'cliente',
    cantidad_requerida INTEGER NOT NULL DEFAULT 2,
    valor_promocional NUMERIC(12,2) NOT NULL,
    fecha_inicio_vigencia DATE NOT NULL,
    fecha_fin_vigencia DATE,
    estado estado_basico_enum NOT NULL DEFAULT 'activo',
    CONSTRAINT chk_promociones_cantidad CHECK (cantidad_requerida > 1),
    CONSTRAINT chk_promociones_valor CHECK (valor_promocional > 0),
    CONSTRAINT chk_promociones_vigencia CHECK (
        fecha_fin_vigencia IS NULL OR fecha_fin_vigencia >= fecha_inicio_vigencia
    )
);

COMMENT ON TABLE promociones IS 'Promociones configurables. Clientes aplican por dia; trabajadores pueden aplicar cualquier dia segun regla del backend.';
COMMENT ON COLUMN promociones.tipo_beneficiario IS 'Permite diferenciar promociones para cliente, trabajador o todos.';

CREATE TABLE IF NOT EXISTS dias_promocion (
    id_dia_promocion UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_promocion UUID NOT NULL REFERENCES promociones(id_promocion) ON DELETE CASCADE,
    dia_semana dia_semana_enum NOT NULL,
    CONSTRAINT uq_dias_promocion UNIQUE (id_promocion, dia_semana)
);

COMMENT ON TABLE dias_promocion IS 'Dias de la semana en que aplica una promocion. Para cliente aplica martes y miercoles.';

CREATE TABLE IF NOT EXISTS ventas (
    id_venta UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_caja_diaria UUID NOT NULL REFERENCES cajas_diarias(id_caja_diaria),
    id_usuario_vendedor UUID NOT NULL REFERENCES usuarios(id_usuario),
    tipo_comprador tipo_comprador_enum NOT NULL DEFAULT 'cliente',
    id_usuario_comprador UUID REFERENCES usuarios(id_usuario),
    numero_venta BIGINT GENERATED BY DEFAULT AS IDENTITY UNIQUE,
    fecha_venta TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    estado_venta estado_venta_enum NOT NULL DEFAULT 'registrada',
    subtotal_venta NUMERIC(12,2) NOT NULL DEFAULT 0,
    descuento_promocion NUMERIC(12,2) NOT NULL DEFAULT 0,
    total_venta NUMERIC(12,2) NOT NULL DEFAULT 0,
    motivo_anulacion TEXT,
    fecha_anulacion TIMESTAMPTZ,
    id_usuario_anulacion UUID REFERENCES usuarios(id_usuario),
    CONSTRAINT chk_ventas_totales CHECK (
        subtotal_venta >= 0 AND descuento_promocion >= 0 AND total_venta >= 0
    ),
    CONSTRAINT chk_ventas_comprador_trabajador CHECK (
        (tipo_comprador = 'cliente' AND id_usuario_comprador IS NULL)
        OR
        (tipo_comprador = 'trabajador' AND id_usuario_comprador IS NOT NULL)
    ),
    CONSTRAINT chk_ventas_anulacion_consistente CHECK (
        (estado_venta = 'registrada' AND fecha_anulacion IS NULL AND id_usuario_anulacion IS NULL)
        OR
        (estado_venta = 'anulada' AND fecha_anulacion IS NOT NULL AND id_usuario_anulacion IS NOT NULL)
    )
);

COMMENT ON TABLE ventas IS 'Cabecera de venta. Una venta pertenece a una caja diaria y puede tener varios pagos.';
COMMENT ON COLUMN ventas.tipo_comprador IS 'Diferencia cliente normal y trabajador, necesario para promociones de trabajadores cualquier dia.';

CREATE INDEX IF NOT EXISTS idx_ventas_caja ON ventas(id_caja_diaria);
CREATE INDEX IF NOT EXISTS idx_ventas_vendedor ON ventas(id_usuario_vendedor);
CREATE INDEX IF NOT EXISTS idx_ventas_fecha ON ventas(fecha_venta);

CREATE TABLE IF NOT EXISTS detalles_venta (
    id_detalle_venta UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_venta UUID NOT NULL REFERENCES ventas(id_venta) ON DELETE CASCADE,
    id_tipo_granizado UUID NOT NULL REFERENCES tipos_granizado(id_tipo_granizado),
    id_tamano_vaso UUID NOT NULL REFERENCES tamanos_vaso(id_tamano_vaso),
    cantidad INTEGER NOT NULL,
    precio_unitario_normal NUMERIC(12,2) NOT NULL,
    cantidad_con_promocion INTEGER NOT NULL DEFAULT 0,
    cantidad_sin_promocion INTEGER NOT NULL DEFAULT 0,
    valor_promocional_aplicado NUMERIC(12,2),
    id_promocion_aplicada UUID REFERENCES promociones(id_promocion),
    subtotal_linea NUMERIC(12,2) NOT NULL DEFAULT 0,
    total_linea NUMERIC(12,2) NOT NULL DEFAULT 0,
    CONSTRAINT chk_detalles_cantidades CHECK (
        cantidad > 0 AND cantidad_con_promocion >= 0 AND cantidad_sin_promocion >= 0
    ),
    CONSTRAINT chk_detalles_cantidades_suman CHECK (
        cantidad = cantidad_con_promocion + cantidad_sin_promocion
    ),
    CONSTRAINT chk_detalles_valores CHECK (
        precio_unitario_normal > 0 AND subtotal_linea >= 0 AND total_linea >= 0
    ),
    CONSTRAINT chk_detalles_promocion_consistente CHECK (
        (cantidad_con_promocion = 0 AND id_promocion_aplicada IS NULL)
        OR
        (cantidad_con_promocion > 0 AND id_promocion_aplicada IS NOT NULL AND valor_promocional_aplicado IS NOT NULL)
    )
);

COMMENT ON TABLE detalles_venta IS 'Detalle transaccional de la venta. Permite auditoria, calculo de promociones y descuento automatico de vasos.';

CREATE INDEX IF NOT EXISTS idx_detalles_venta_id_venta ON detalles_venta(id_venta);

-- ============================================================
-- 7. MODULO DE PAGOS
-- ============================================================

CREATE TABLE IF NOT EXISTS metodos_pago (
    id_metodo_pago UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre_metodo TEXT NOT NULL UNIQUE,
    estado estado_basico_enum NOT NULL DEFAULT 'activo'
);

COMMENT ON TABLE metodos_pago IS 'Catalogo de metodos reales de pago. El pago hibrido se representa con dos registros: efectivo y transferencia.';

CREATE TABLE IF NOT EXISTS pagos_venta (
    id_pago_venta UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_venta UUID NOT NULL REFERENCES ventas(id_venta) ON DELETE CASCADE,
    id_metodo_pago UUID NOT NULL REFERENCES metodos_pago(id_metodo_pago),
    valor_pago NUMERIC(12,2) NOT NULL,
    valor_recibido_efectivo NUMERIC(12,2),
    cambio_entregado NUMERIC(12,2),
    estado_validacion estado_validacion_transferencia_enum NOT NULL DEFAULT 'no_aplica',
    id_usuario_validacion UUID REFERENCES usuarios(id_usuario),
    fecha_validacion TIMESTAMPTZ,
    observacion_validacion TEXT,
    fecha_registro TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_pagos_valor CHECK (valor_pago > 0),
    CONSTRAINT chk_pagos_efectivo CHECK (
        valor_recibido_efectivo IS NULL OR valor_recibido_efectivo >= valor_pago
    ),
    CONSTRAINT chk_pagos_cambio CHECK (
        cambio_entregado IS NULL OR cambio_entregado >= 0
    ),
    CONSTRAINT chk_pagos_validacion_consistente CHECK (
        (estado_validacion IN ('no_aplica', 'pendiente') AND id_usuario_validacion IS NULL AND fecha_validacion IS NULL)
        OR
        (estado_validacion IN ('validada', 'rechazada') AND id_usuario_validacion IS NOT NULL AND fecha_validacion IS NOT NULL)
    )
);

COMMENT ON TABLE pagos_venta IS 'Pagos asociados a una venta. Soporta efectivo, transferencia e hibrido mediante multiples filas.';
COMMENT ON COLUMN pagos_venta.estado_validacion IS 'Para transferencias: pendiente, validada o rechazada. Para efectivo: no_aplica.';

CREATE INDEX IF NOT EXISTS idx_pagos_venta_id_venta ON pagos_venta(id_venta);
CREATE INDEX IF NOT EXISTS idx_pagos_venta_validacion ON pagos_venta(estado_validacion);

-- ============================================================
-- 8. ADICIONES, PAGOS A TRABAJADORES Y GASTOS DE CAJA
-- ============================================================

CREATE TABLE IF NOT EXISTS adiciones_diarias (
    id_adicion_diaria UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_caja_diaria UUID NOT NULL UNIQUE REFERENCES cajas_diarias(id_caja_diaria) ON DELETE CASCADE,
    cantidad_adiciones INTEGER NOT NULL DEFAULT 0,
    valor_unitario NUMERIC(12,2) NOT NULL DEFAULT 1000,
    valor_total NUMERIC(12,2) GENERATED ALWAYS AS (cantidad_adiciones * valor_unitario) STORED,
    id_usuario_registro UUID NOT NULL REFERENCES usuarios(id_usuario),
    fecha_registro TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_adiciones_cantidad CHECK (cantidad_adiciones >= 0),
    CONSTRAINT chk_adiciones_valor CHECK (valor_unitario >= 0)
);

COMMENT ON TABLE adiciones_diarias IS 'Registro unico de adiciones por caja diaria. Editable mientras la caja este abierta.';

CREATE TABLE IF NOT EXISTS pagos_trabajadores_diarios (
    id_pago_trabajadores_diario UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_caja_diaria UUID NOT NULL UNIQUE REFERENCES cajas_diarias(id_caja_diaria) ON DELETE CASCADE,
    valor_total_pagado NUMERIC(12,2) NOT NULL,
    descripcion TEXT,
    id_usuario_registro UUID NOT NULL REFERENCES usuarios(id_usuario),
    fecha_registro TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    confirmado_para_cierre BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_pagos_trabajadores_valor CHECK (valor_total_pagado >= 0)
);

COMMENT ON TABLE pagos_trabajadores_diarios IS 'Pago total diario a trabajadores. Es obligatorio antes de cerrar caja.';
COMMENT ON COLUMN pagos_trabajadores_diarios.confirmado_para_cierre IS 'Debe ser verdadero para permitir cierre. Si el valor es cero, requiere confirmacion explicita.';

CREATE TABLE IF NOT EXISTS gastos_caja (
    id_gasto_caja UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_caja_diaria UUID NOT NULL REFERENCES cajas_diarias(id_caja_diaria) ON DELETE CASCADE,
    valor_gasto NUMERIC(12,2) NOT NULL,
    descripcion TEXT NOT NULL,
    estado_gasto estado_gasto_enum NOT NULL DEFAULT 'registrado',
    id_usuario_registro UUID NOT NULL REFERENCES usuarios(id_usuario),
    fecha_registro TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    id_usuario_ultima_edicion UUID REFERENCES usuarios(id_usuario),
    fecha_ultima_edicion TIMESTAMPTZ,
    motivo_edicion TEXT,
    id_usuario_anulacion UUID REFERENCES usuarios(id_usuario),
    fecha_anulacion TIMESTAMPTZ,
    motivo_anulacion TEXT,
    CONSTRAINT chk_gastos_valor CHECK (valor_gasto > 0),
    CONSTRAINT chk_gastos_descripcion_no_vacia CHECK (length(trim(descripcion)) > 0),
    CONSTRAINT chk_gastos_anulacion_consistente CHECK (
        (estado_gasto <> 'anulado' AND fecha_anulacion IS NULL AND id_usuario_anulacion IS NULL)
        OR
        (estado_gasto = 'anulado' AND fecha_anulacion IS NOT NULL AND id_usuario_anulacion IS NOT NULL)
    )
);

COMMENT ON TABLE gastos_caja IS 'Gastos operativos tomados de la caja diaria. Vendedor registra; administrador o gerente edita/anula.';

CREATE INDEX IF NOT EXISTS idx_gastos_caja_id_caja ON gastos_caja(id_caja_diaria);

-- ============================================================
-- 9. MODULO DE INVENTARIO
-- ============================================================

CREATE TABLE IF NOT EXISTS categorias_inventario (
    id_categoria_inventario UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre_categoria TEXT NOT NULL UNIQUE,
    estado estado_basico_enum NOT NULL DEFAULT 'activo'
);

COMMENT ON TABLE categorias_inventario IS 'Categorias de inventario: vasos, dulces, desechables, producto con licor y producto sin licor.';

CREATE TABLE IF NOT EXISTS unidades_medida (
    id_unidad_medida UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre_unidad TEXT NOT NULL UNIQUE,
    abreviatura TEXT NOT NULL UNIQUE,
    estado estado_basico_enum NOT NULL DEFAULT 'activo'
);

COMMENT ON TABLE unidades_medida IS 'Catalogo de unidades de medida: unidad, bolsa, paquete, rollo.';

CREATE TABLE IF NOT EXISTS items_inventario (
    id_item_inventario UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_categoria_inventario UUID NOT NULL REFERENCES categorias_inventario(id_categoria_inventario),
    id_unidad_medida UUID NOT NULL REFERENCES unidades_medida(id_unidad_medida),
    id_tamano_vaso UUID REFERENCES tamanos_vaso(id_tamano_vaso),
    nombre_item TEXT NOT NULL UNIQUE,
    tipo_control tipo_control_inventario_enum NOT NULL,
    maneja_paquetes BOOLEAN NOT NULL DEFAULT FALSE,
    unidades_por_paquete INTEGER,
    estado estado_basico_enum NOT NULL DEFAULT 'activo',
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_items_paquetes CHECK (
        (maneja_paquetes = FALSE AND unidades_por_paquete IS NULL)
        OR
        (maneja_paquetes = TRUE AND unidades_por_paquete IS NOT NULL AND unidades_por_paquete > 0)
    )
);

COMMENT ON TABLE items_inventario IS 'Productos inventariables. Los vasos se descuentan automaticamente por venta; otros productos por consumo diario manual.';
COMMENT ON COLUMN items_inventario.maneja_paquetes IS 'Para vasos, permite registrar apertura de paquetes de 20 unidades.';

CREATE TABLE IF NOT EXISTS existencias_inventario_general (
    id_existencia_general UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_item_inventario UUID NOT NULL UNIQUE REFERENCES items_inventario(id_item_inventario),
    cantidad_actual INTEGER NOT NULL DEFAULT 0,
    fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_existencias_general_cantidad CHECK (cantidad_actual >= 0)
);

COMMENT ON TABLE existencias_inventario_general IS 'Stock general disponible para todos los items de inventario.';

DROP TRIGGER IF EXISTS trg_existencias_general_fecha_actualizacion ON existencias_inventario_general;
CREATE TRIGGER trg_existencias_general_fecha_actualizacion
BEFORE UPDATE ON existencias_inventario_general
FOR EACH ROW
EXECUTE FUNCTION actualizar_fecha_actualizacion();

CREATE TABLE IF NOT EXISTS existencias_inventario_diario (
    id_existencia_diaria UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_caja_diaria UUID NOT NULL REFERENCES cajas_diarias(id_caja_diaria) ON DELETE CASCADE,
    id_item_inventario UUID NOT NULL REFERENCES items_inventario(id_item_inventario),
    cantidad_inicial INTEGER NOT NULL DEFAULT 0,
    cantidad_ingresada INTEGER NOT NULL DEFAULT 0,
    cantidad_vendida INTEGER NOT NULL DEFAULT 0,
    cantidad_perdida INTEGER NOT NULL DEFAULT 0,
    cantidad_ajustada INTEGER NOT NULL DEFAULT 0,
    cantidad_final_teorica INTEGER,
    cantidad_final_contada INTEGER,
    diferencia INTEGER,
    CONSTRAINT uq_existencias_diario UNIQUE (id_caja_diaria, id_item_inventario),
    CONSTRAINT chk_existencias_diario_cantidades CHECK (
        cantidad_inicial >= 0 AND
        cantidad_ingresada >= 0 AND
        cantidad_vendida >= 0 AND
        cantidad_perdida >= 0 AND
        (cantidad_final_contada IS NULL OR cantidad_final_contada >= 0)
    )
);

COMMENT ON TABLE existencias_inventario_diario IS 'Stock diario operativo. En el alcance actual aplica solo para vasos.';
COMMENT ON COLUMN existencias_inventario_diario.cantidad_ajustada IS 'Puede ser positiva o negativa, siempre que provenga de ajuste aprobado.';

CREATE TABLE IF NOT EXISTS movimientos_inventario (
    id_movimiento_inventario UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_item_inventario UUID NOT NULL REFERENCES items_inventario(id_item_inventario),
    id_caja_diaria UUID REFERENCES cajas_diarias(id_caja_diaria) ON DELETE SET NULL,
    tipo_stock tipo_stock_enum NOT NULL,
    tipo_movimiento tipo_movimiento_inventario_enum NOT NULL,
    cantidad INTEGER NOT NULL,
    sentido_movimiento sentido_movimiento_enum NOT NULL,
    referencia_origen TEXT,
    id_referencia_origen UUID,
    observacion TEXT,
    id_usuario_registro UUID NOT NULL REFERENCES usuarios(id_usuario),
    fecha_movimiento TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_movimientos_inventario_cantidad CHECK (cantidad > 0)
);

COMMENT ON TABLE movimientos_inventario IS 'Libro de movimientos de inventario. Ningun cambio de stock debe ocurrir sin registrar movimiento.';
COMMENT ON COLUMN movimientos_inventario.id_referencia_origen IS 'Referencia polimorfica opcional al registro origen: venta, ajuste, paquete o consumo diario.';

CREATE INDEX IF NOT EXISTS idx_movimientos_inventario_item ON movimientos_inventario(id_item_inventario);
CREATE INDEX IF NOT EXISTS idx_movimientos_inventario_caja ON movimientos_inventario(id_caja_diaria);
CREATE INDEX IF NOT EXISTS idx_movimientos_inventario_fecha ON movimientos_inventario(fecha_movimiento);

CREATE TABLE IF NOT EXISTS paquetes_vasos_abiertos (
    id_paquete_vasos_abierto UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_caja_diaria UUID NOT NULL REFERENCES cajas_diarias(id_caja_diaria) ON DELETE CASCADE,
    id_item_inventario UUID NOT NULL REFERENCES items_inventario(id_item_inventario),
    cantidad_paquetes INTEGER NOT NULL,
    unidades_por_paquete INTEGER NOT NULL DEFAULT 20,
    unidades_generadas INTEGER GENERATED ALWAYS AS (cantidad_paquetes * unidades_por_paquete) STORED,
    unidades_rotas INTEGER NOT NULL DEFAULT 0,
    unidades_disponibles INTEGER GENERATED ALWAYS AS ((cantidad_paquetes * unidades_por_paquete) - unidades_rotas) STORED,
    id_usuario_registro UUID NOT NULL REFERENCES usuarios(id_usuario),
    fecha_registro TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_paquetes_cantidad CHECK (cantidad_paquetes > 0),
    CONSTRAINT chk_paquetes_unidades CHECK (unidades_por_paquete > 0),
    CONSTRAINT chk_paquetes_rotas CHECK (unidades_rotas >= 0 AND unidades_rotas <= cantidad_paquetes * unidades_por_paquete)
);

COMMENT ON TABLE paquetes_vasos_abiertos IS 'Registro de paquetes de vasos abiertos durante la caja diaria. Cada paquete genera 20 vasos disponibles menos perdidas.';

CREATE TABLE IF NOT EXISTS consumos_diarios_inventario (
    id_consumo_diario_inventario UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_caja_diaria UUID NOT NULL REFERENCES cajas_diarias(id_caja_diaria) ON DELETE CASCADE,
    id_item_inventario UUID NOT NULL REFERENCES items_inventario(id_item_inventario),
    cantidad_consumida INTEGER NOT NULL,
    id_usuario_registro UUID NOT NULL REFERENCES usuarios(id_usuario),
    fecha_registro TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    observacion TEXT,
    CONSTRAINT chk_consumos_cantidad CHECK (cantidad_consumida > 0)
);

COMMENT ON TABLE consumos_diarios_inventario IS 'Consumo manual diario de dulces, desechables y bolsas de producto con/sin licor.';

CREATE TABLE IF NOT EXISTS ajustes_inventario (
    id_ajuste_inventario UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_item_inventario UUID NOT NULL REFERENCES items_inventario(id_item_inventario),
    id_caja_diaria UUID REFERENCES cajas_diarias(id_caja_diaria) ON DELETE SET NULL,
    tipo_stock tipo_stock_enum NOT NULL,
    cantidad_ajuste INTEGER NOT NULL,
    sentido_ajuste sentido_movimiento_enum NOT NULL,
    motivo_ajuste TEXT NOT NULL,
    estado_aprobacion estado_aprobacion_enum NOT NULL DEFAULT 'pendiente',
    id_usuario_solicitante UUID NOT NULL REFERENCES usuarios(id_usuario),
    id_usuario_aprobador UUID REFERENCES usuarios(id_usuario),
    fecha_solicitud TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    fecha_aprobacion TIMESTAMPTZ,
    observacion_aprobacion TEXT,
    CONSTRAINT chk_ajustes_cantidad CHECK (cantidad_ajuste > 0),
    CONSTRAINT chk_ajustes_motivo_no_vacio CHECK (length(trim(motivo_ajuste)) > 0),
    CONSTRAINT chk_ajustes_aprobacion_consistente CHECK (
        (estado_aprobacion = 'pendiente' AND id_usuario_aprobador IS NULL AND fecha_aprobacion IS NULL)
        OR
        (estado_aprobacion IN ('aprobado', 'rechazado') AND id_usuario_aprobador IS NOT NULL AND fecha_aprobacion IS NOT NULL)
    )
);

COMMENT ON TABLE ajustes_inventario IS 'Solicitudes de ajuste manual de inventario. Requieren aprobacion del gerente.';

-- ============================================================
-- 10. MODULO DE DEPOSITO, CONSIGNACIONES Y SERVICIOS
-- ============================================================

CREATE TABLE IF NOT EXISTS movimientos_deposito (
    id_movimiento_deposito UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tipo_movimiento_deposito tipo_movimiento_deposito_enum NOT NULL,
    valor_movimiento NUMERIC(12,2) NOT NULL,
    saldo_anterior NUMERIC(12,2) NOT NULL,
    saldo_posterior NUMERIC(12,2) NOT NULL,
    id_cierre_caja UUID UNIQUE REFERENCES cierres_caja(id_cierre_caja),
    id_usuario_registro UUID NOT NULL REFERENCES usuarios(id_usuario),
    fecha_movimiento TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    observacion TEXT,
    CONSTRAINT chk_movimientos_deposito_valores CHECK (
        valor_movimiento > 0 AND saldo_anterior >= 0 AND saldo_posterior >= 0
    )
);

COMMENT ON TABLE movimientos_deposito IS 'Libro historico del deposito. Fuente principal para reconstruir el saldo acumulado.';

CREATE INDEX IF NOT EXISTS idx_movimientos_deposito_fecha ON movimientos_deposito(fecha_movimiento);
CREATE INDEX IF NOT EXISTS idx_movimientos_deposito_tipo ON movimientos_deposito(tipo_movimiento_deposito);

CREATE TABLE IF NOT EXISTS consignaciones_bancarias (
    id_consignacion_bancaria UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_movimiento_deposito UUID NOT NULL UNIQUE REFERENCES movimientos_deposito(id_movimiento_deposito),
    valor_consignado NUMERIC(12,2) NOT NULL,
    fecha_consignacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    id_usuario_registro UUID NOT NULL REFERENCES usuarios(id_usuario),
    observacion TEXT,
    estado estado_registro_financiero_enum NOT NULL DEFAULT 'registrado',
    CONSTRAINT chk_consignaciones_valor CHECK (valor_consignado > 0)
);

COMMENT ON TABLE consignaciones_bancarias IS 'Consignaciones realizadas desde el deposito por administrador o gerente.';

CREATE TABLE IF NOT EXISTS tipos_servicio (
    id_tipo_servicio UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre_servicio TEXT NOT NULL UNIQUE,
    estado estado_basico_enum NOT NULL DEFAULT 'activo'
);

COMMENT ON TABLE tipos_servicio IS 'Catalogo de servicios pagables desde el deposito: arriendo, energia, agua, internet u otros.';

CREATE TABLE IF NOT EXISTS pagos_servicios (
    id_pago_servicio UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_movimiento_deposito UUID NOT NULL UNIQUE REFERENCES movimientos_deposito(id_movimiento_deposito),
    id_tipo_servicio UUID NOT NULL REFERENCES tipos_servicio(id_tipo_servicio),
    valor_pagado NUMERIC(12,2) NOT NULL,
    descripcion TEXT,
    fecha_pago TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    id_usuario_registro UUID NOT NULL REFERENCES usuarios(id_usuario),
    estado estado_registro_financiero_enum NOT NULL DEFAULT 'registrado',
    CONSTRAINT chk_pagos_servicios_valor CHECK (valor_pagado > 0)
);

COMMENT ON TABLE pagos_servicios IS 'Pagos de servicios realizados desde el deposito.';

-- ============================================================
-- 11. ARCHIVOS DE EVIDENCIA
-- ============================================================

CREATE TABLE IF NOT EXISTS archivos_evidencia (
    id_archivo_evidencia UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_pago_venta UUID REFERENCES pagos_venta(id_pago_venta) ON DELETE CASCADE,
    id_gasto_caja UUID REFERENCES gastos_caja(id_gasto_caja) ON DELETE CASCADE,
    id_consignacion_bancaria UUID REFERENCES consignaciones_bancarias(id_consignacion_bancaria) ON DELETE CASCADE,
    id_pago_servicio UUID REFERENCES pagos_servicios(id_pago_servicio) ON DELETE CASCADE,
    url_archivo TEXT NOT NULL,
    nombre_archivo TEXT NOT NULL,
    tipo_archivo tipo_archivo_enum NOT NULL,
    formato_archivo formato_archivo_enum NOT NULL,
    tamano_original_kb INTEGER,
    tamano_comprimido_kb INTEGER,
    fue_comprimido BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_subida TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    id_usuario_subida UUID NOT NULL REFERENCES usuarios(id_usuario),
    estado estado_basico_enum NOT NULL DEFAULT 'activo',
    CONSTRAINT chk_archivos_relacion_unica CHECK (
        num_nonnulls(id_pago_venta, id_gasto_caja, id_consignacion_bancaria, id_pago_servicio) = 1
    ),
    CONSTRAINT chk_archivos_url_no_vacia CHECK (length(trim(url_archivo)) > 0),
    CONSTRAINT chk_archivos_nombre_no_vacio CHECK (length(trim(nombre_archivo)) > 0),
    CONSTRAINT chk_archivos_tamanos CHECK (
        (tamano_original_kb IS NULL OR tamano_original_kb >= 0) AND
        (tamano_comprimido_kb IS NULL OR tamano_comprimido_kb >= 0)
    )
);

COMMENT ON TABLE archivos_evidencia IS 'Referencias a fotografias o documentos guardados en almacenamiento externo. No guarda binarios en la BD.';
COMMENT ON COLUMN archivos_evidencia.fue_comprimido IS 'Indica si el backend comprimio la imagen antes de subirla al almacenamiento.';

-- ============================================================
-- 12. CONFIGURACION Y AUDITORIA
-- ============================================================

CREATE TABLE IF NOT EXISTS configuraciones_sistema (
    id_configuracion_sistema UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre_configuracion TEXT NOT NULL,
    valor_configuracion TEXT NOT NULL,
    tipo_valor tipo_valor_configuracion_enum NOT NULL,
    fecha_inicio_vigencia DATE NOT NULL,
    fecha_fin_vigencia DATE,
    estado estado_basico_enum NOT NULL DEFAULT 'activo',
    id_usuario_registro UUID REFERENCES usuarios(id_usuario),
    fecha_registro TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_configuracion_nombre_no_vacio CHECK (length(trim(nombre_configuracion)) > 0),
    CONSTRAINT chk_configuracion_valor_no_vacio CHECK (length(trim(valor_configuracion)) > 0),
    CONSTRAINT chk_configuracion_vigencia CHECK (
        fecha_fin_vigencia IS NULL OR fecha_fin_vigencia >= fecha_inicio_vigencia
    )
);

COMMENT ON TABLE configuraciones_sistema IS 'Configuraciones historicas del sistema: base, valor de adicion, pagos u otros parametros.';

CREATE UNIQUE INDEX IF NOT EXISTS uq_configuracion_vigente
ON configuraciones_sistema(nombre_configuracion)
WHERE estado = 'activo' AND fecha_fin_vigencia IS NULL;

CREATE TABLE IF NOT EXISTS auditoria_operaciones (
    id_auditoria_operacion UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_usuario UUID REFERENCES usuarios(id_usuario),
    tabla_afectada TEXT NOT NULL,
    id_registro_afectado TEXT,
    accion accion_auditoria_enum NOT NULL,
    valor_anterior JSONB,
    valor_nuevo JSONB,
    fecha_accion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    direccion_ip TEXT,
    descripcion TEXT,
    CONSTRAINT chk_auditoria_tabla_no_vacia CHECK (length(trim(tabla_afectada)) > 0)
);

COMMENT ON TABLE auditoria_operaciones IS 'Trazabilidad de acciones sensibles: ventas, caja, gastos, inventario, deposito, configuracion y seguridad.';

CREATE INDEX IF NOT EXISTS idx_auditoria_usuario ON auditoria_operaciones(id_usuario);
CREATE INDEX IF NOT EXISTS idx_auditoria_tabla ON auditoria_operaciones(tabla_afectada);
CREATE INDEX IF NOT EXISTS idx_auditoria_fecha ON auditoria_operaciones(fecha_accion);


-- ============================================================
-- 13. REGLAS CRITICAS EN BASE DE DATOS
-- ============================================================

-- Valida que la caja asociada a una operacion se encuentre abierta.
-- Se usa para operaciones que no deben realizarse despues del cierre.
CREATE OR REPLACE FUNCTION validar_caja_abierta_por_columna()
RETURNS TRIGGER AS $$
DECLARE
    v_id_caja UUID;
    v_estado estado_caja_enum;
BEGIN
    v_id_caja := (to_jsonb(NEW) ->> TG_ARGV[0])::UUID;

    IF v_id_caja IS NULL THEN
        RETURN NEW;
    END IF;

    SELECT estado_caja
    INTO v_estado
    FROM cajas_diarias
    WHERE id_caja_diaria = v_id_caja;

    IF v_estado IS NULL THEN
        RAISE EXCEPTION 'La caja diaria indicada no existe: %', v_id_caja;
    END IF;

    IF v_estado <> 'abierta' THEN
        RAISE EXCEPTION 'La operacion no se puede realizar porque la caja diaria % esta cerrada.', v_id_caja;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_validar_caja_abierta_ventas ON ventas;
CREATE TRIGGER trg_validar_caja_abierta_ventas
BEFORE INSERT OR UPDATE ON ventas
FOR EACH ROW
EXECUTE FUNCTION validar_caja_abierta_por_columna('id_caja_diaria');

DROP TRIGGER IF EXISTS trg_validar_caja_abierta_adiciones ON adiciones_diarias;
CREATE TRIGGER trg_validar_caja_abierta_adiciones
BEFORE INSERT OR UPDATE ON adiciones_diarias
FOR EACH ROW
EXECUTE FUNCTION validar_caja_abierta_por_columna('id_caja_diaria');

DROP TRIGGER IF EXISTS trg_validar_caja_abierta_pagos_trabajadores ON pagos_trabajadores_diarios;
CREATE TRIGGER trg_validar_caja_abierta_pagos_trabajadores
BEFORE INSERT OR UPDATE ON pagos_trabajadores_diarios
FOR EACH ROW
EXECUTE FUNCTION validar_caja_abierta_por_columna('id_caja_diaria');

DROP TRIGGER IF EXISTS trg_validar_caja_abierta_gastos ON gastos_caja;
CREATE TRIGGER trg_validar_caja_abierta_gastos
BEFORE INSERT OR UPDATE ON gastos_caja
FOR EACH ROW
EXECUTE FUNCTION validar_caja_abierta_por_columna('id_caja_diaria');

DROP TRIGGER IF EXISTS trg_validar_caja_abierta_paquetes ON paquetes_vasos_abiertos;
CREATE TRIGGER trg_validar_caja_abierta_paquetes
BEFORE INSERT OR UPDATE ON paquetes_vasos_abiertos
FOR EACH ROW
EXECUTE FUNCTION validar_caja_abierta_por_columna('id_caja_diaria');

DROP TRIGGER IF EXISTS trg_validar_caja_abierta_consumos ON consumos_diarios_inventario;
CREATE TRIGGER trg_validar_caja_abierta_consumos
BEFORE INSERT OR UPDATE ON consumos_diarios_inventario
FOR EACH ROW
EXECUTE FUNCTION validar_caja_abierta_por_columna('id_caja_diaria');

DROP TRIGGER IF EXISTS trg_validar_caja_abierta_existencias_diarias ON existencias_inventario_diario;
CREATE TRIGGER trg_validar_caja_abierta_existencias_diarias
BEFORE INSERT OR UPDATE ON existencias_inventario_diario
FOR EACH ROW
EXECUTE FUNCTION validar_caja_abierta_por_columna('id_caja_diaria');

-- Valida que una venta asociada a un detalle o pago pertenezca a una caja abierta.
CREATE OR REPLACE FUNCTION validar_venta_en_caja_abierta()
RETURNS TRIGGER AS $$
DECLARE
    v_estado estado_caja_enum;
BEGIN
    SELECT cd.estado_caja
    INTO v_estado
    FROM ventas v
    JOIN cajas_diarias cd ON cd.id_caja_diaria = v.id_caja_diaria
    WHERE v.id_venta = NEW.id_venta;

    IF v_estado IS NULL THEN
        RAISE EXCEPTION 'La venta indicada no existe: %', NEW.id_venta;
    END IF;

    IF v_estado <> 'abierta' THEN
        RAISE EXCEPTION 'No se puede registrar o modificar este detalle/pago porque la caja de la venta esta cerrada.';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_validar_caja_abierta_detalles_venta ON detalles_venta;
CREATE TRIGGER trg_validar_caja_abierta_detalles_venta
BEFORE INSERT OR UPDATE ON detalles_venta
FOR EACH ROW
EXECUTE FUNCTION validar_venta_en_caja_abierta();

DROP TRIGGER IF EXISTS trg_validar_caja_abierta_pagos_venta_insert ON pagos_venta;
CREATE TRIGGER trg_validar_caja_abierta_pagos_venta_insert
BEFORE INSERT ON pagos_venta
FOR EACH ROW
EXECUTE FUNCTION validar_venta_en_caja_abierta();

-- Valida las condiciones minimas para cerrar una caja diaria.
CREATE OR REPLACE FUNCTION validar_cierre_caja()
RETURNS TRIGGER AS $$
DECLARE
    v_estado estado_caja_enum;
    v_pago_trabajadores_confirmado BOOLEAN;
BEGIN
    SELECT estado_caja
    INTO v_estado
    FROM cajas_diarias
    WHERE id_caja_diaria = NEW.id_caja_diaria;

    IF v_estado IS NULL THEN
        RAISE EXCEPTION 'La caja diaria indicada no existe: %', NEW.id_caja_diaria;
    END IF;

    IF v_estado <> 'abierta' THEN
        RAISE EXCEPTION 'No se puede cerrar una caja que no este abierta.';
    END IF;

    SELECT confirmado_para_cierre
    INTO v_pago_trabajadores_confirmado
    FROM pagos_trabajadores_diarios
    WHERE id_caja_diaria = NEW.id_caja_diaria;

    IF COALESCE(v_pago_trabajadores_confirmado, FALSE) = FALSE THEN
        RAISE EXCEPTION 'No se puede cerrar caja sin registrar y confirmar el pago diario de trabajadores.';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_validar_cierre_caja ON cierres_caja;
CREATE TRIGGER trg_validar_cierre_caja
BEFORE INSERT ON cierres_caja
FOR EACH ROW
EXECUTE FUNCTION validar_cierre_caja();

-- Al insertar un cierre, la caja asociada queda cerrada automaticamente.
CREATE OR REPLACE FUNCTION cerrar_caja_despues_de_cierre()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE cajas_diarias
    SET estado_caja = 'cerrada',
        fecha_cierre = NEW.fecha_cierre,
        id_usuario_cierre = NEW.id_usuario_cierre
    WHERE id_caja_diaria = NEW.id_caja_diaria;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_cerrar_caja_despues_de_cierre ON cierres_caja;
CREATE TRIGGER trg_cerrar_caja_despues_de_cierre
AFTER INSERT ON cierres_caja
FOR EACH ROW
EXECUTE FUNCTION cerrar_caja_despues_de_cierre();

-- ============================================================
-- 14. DATOS INICIALES / CATALOGOS BASE
-- ============================================================

INSERT INTO roles (nombre_rol, estado)
VALUES
    ('vendedor', 'activo'),
    ('administrador', 'activo'),
    ('gerente', 'activo')
ON CONFLICT (nombre_rol) DO NOTHING;

INSERT INTO metodos_pago (nombre_metodo, estado)
VALUES
    ('efectivo', 'activo'),
    ('transferencia', 'activo')
ON CONFLICT (nombre_metodo) DO NOTHING;

INSERT INTO tipos_granizado (nombre_tipo, estado)
VALUES
    ('con_licor', 'activo'),
    ('sin_licor', 'activo')
ON CONFLICT (nombre_tipo) DO NOTHING;

INSERT INTO tamanos_vaso (onzas, estado)
VALUES
    (8, 'activo'),
    (12, 'activo'),
    (16, 'activo'),
    (20, 'activo'),
    (24, 'activo'),
    (32, 'activo')
ON CONFLICT (onzas) DO NOTHING;

INSERT INTO categorias_inventario (nombre_categoria, estado)
VALUES
    ('vasos', 'activo'),
    ('dulces', 'activo'),
    ('desechables', 'activo'),
    ('producto_con_licor', 'activo'),
    ('producto_sin_licor', 'activo')
ON CONFLICT (nombre_categoria) DO NOTHING;

INSERT INTO unidades_medida (nombre_unidad, abreviatura, estado)
VALUES
    ('unidad', 'und', 'activo'),
    ('bolsa', 'bolsa', 'activo'),
    ('paquete', 'paq', 'activo'),
    ('rollo', 'rollo', 'activo')
ON CONFLICT (nombre_unidad) DO NOTHING;

INSERT INTO tipos_servicio (nombre_servicio, estado)
VALUES
    ('arriendo', 'activo'),
    ('energia', 'activo'),
    ('agua', 'activo'),
    ('internet', 'activo'),
    ('otro', 'activo')
ON CONFLICT (nombre_servicio) DO NOTHING;

-- Precios iniciales de carta.
INSERT INTO precios_granizado (
    id_tipo_granizado,
    id_tamano_vaso,
    valor_precio,
    fecha_inicio_vigencia,
    estado
)
SELECT tg.id_tipo_granizado, tv.id_tamano_vaso, datos.valor_precio, CURRENT_DATE, 'activo'::estado_basico_enum
FROM (
    VALUES
        ('con_licor', 8, 8000::numeric),
        ('con_licor', 12, 12000::numeric),
        ('con_licor', 16, 15000::numeric),
        ('con_licor', 20, 18000::numeric),
        ('con_licor', 24, 21000::numeric),
        ('con_licor', 32, 28000::numeric),
        ('sin_licor', 8, 6000::numeric),
        ('sin_licor', 12, 8000::numeric),
        ('sin_licor', 16, 11000::numeric),
        ('sin_licor', 20, 13000::numeric),
        ('sin_licor', 24, 15000::numeric),
        ('sin_licor', 32, 20000::numeric)
) AS datos(nombre_tipo, onzas, valor_precio)
JOIN tipos_granizado tg ON tg.nombre_tipo = datos.nombre_tipo
JOIN tamanos_vaso tv ON tv.onzas = datos.onzas
ON CONFLICT DO NOTHING;

-- Promociones iniciales para granizados con licor.
INSERT INTO promociones (
    nombre_promocion,
    id_tipo_granizado,
    id_tamano_vaso,
    tipo_beneficiario,
    cantidad_requerida,
    valor_promocional,
    fecha_inicio_vigencia,
    estado
)
SELECT
    'promocion_2x_' || datos.onzas || 'oz_cliente',
    tg.id_tipo_granizado,
    tv.id_tamano_vaso,
    'cliente'::tipo_beneficiario_enum,
    2,
    datos.valor_promocional,
    CURRENT_DATE,
    'activo'::estado_basico_enum
FROM (
    VALUES
        (8, 12000::numeric),
        (12, 17000::numeric),
        (16, 22000::numeric),
        (20, 27000::numeric),
        (24, 31000::numeric),
        (32, 41000::numeric)
) AS datos(onzas, valor_promocional)
JOIN tipos_granizado tg ON tg.nombre_tipo = 'con_licor'
JOIN tamanos_vaso tv ON tv.onzas = datos.onzas
ON CONFLICT DO NOTHING;

INSERT INTO promociones (
    nombre_promocion,
    id_tipo_granizado,
    id_tamano_vaso,
    tipo_beneficiario,
    cantidad_requerida,
    valor_promocional,
    fecha_inicio_vigencia,
    estado
)
SELECT
    'promocion_2x_' || datos.onzas || 'oz_trabajador',
    tg.id_tipo_granizado,
    tv.id_tamano_vaso,
    'trabajador'::tipo_beneficiario_enum,
    2,
    datos.valor_promocional,
    CURRENT_DATE,
    'activo'::estado_basico_enum
FROM (
    VALUES
        (8, 12000::numeric),
        (12, 17000::numeric),
        (16, 22000::numeric),
        (20, 27000::numeric),
        (24, 31000::numeric),
        (32, 41000::numeric)
) AS datos(onzas, valor_promocional)
JOIN tipos_granizado tg ON tg.nombre_tipo = 'con_licor'
JOIN tamanos_vaso tv ON tv.onzas = datos.onzas
ON CONFLICT DO NOTHING;

-- Dias de promocion para clientes: martes y miercoles.
INSERT INTO dias_promocion (id_promocion, dia_semana)
SELECT p.id_promocion, d.dia::dia_semana_enum
FROM promociones p
CROSS JOIN (VALUES ('martes'), ('miercoles')) AS d(dia)
WHERE p.tipo_beneficiario = 'cliente'
ON CONFLICT (id_promocion, dia_semana) DO NOTHING;

-- Items base de vasos. El stock general y diario se registra en unidades; la interfaz puede recibir paquetes.
INSERT INTO items_inventario (
    id_categoria_inventario,
    id_unidad_medida,
    id_tamano_vaso,
    nombre_item,
    tipo_control,
    maneja_paquetes,
    unidades_por_paquete,
    estado
)
SELECT
    ci.id_categoria_inventario,
    um.id_unidad_medida,
    tv.id_tamano_vaso,
    'vaso_' || tv.onzas || 'oz',
    'automatico_por_venta'::tipo_control_inventario_enum,
    TRUE,
    20,
    'activo'::estado_basico_enum
FROM tamanos_vaso tv
JOIN categorias_inventario ci ON ci.nombre_categoria = 'vasos'
JOIN unidades_medida um ON um.nombre_unidad = 'unidad'
ON CONFLICT (nombre_item) DO NOTHING;

-- Items base de dulces y desechables.
INSERT INTO items_inventario (
    id_categoria_inventario,
    id_unidad_medida,
    nombre_item,
    tipo_control,
    maneja_paquetes,
    unidades_por_paquete,
    estado
)
SELECT ci.id_categoria_inventario, um.id_unidad_medida, datos.nombre_item, datos.tipo_control::tipo_control_inventario_enum, FALSE, NULL, 'activo'::estado_basico_enum
FROM (
    VALUES
        ('dulces', 'bolsa', 'bolsa_chicles', 'manual_por_consumo'),
        ('dulces', 'bolsa', 'bolsa_bombones', 'manual_por_consumo'),
        ('dulces', 'bolsa', 'bolsa_gomas', 'manual_por_consumo'),
        ('desechables', 'bolsa', 'bolsa_pitillos', 'manual_por_consumo'),
        ('desechables', 'bolsa', 'bolsas_ziploc', 'manual_por_consumo'),
        ('desechables', 'bolsa', 'bolsas_negras', 'manual_por_consumo'),
        ('desechables', 'bolsa', 'bolsas_papeleras', 'manual_por_consumo'),
        ('desechables', 'bolsa', 'bolsas_copas_prueba', 'manual_por_consumo'),
        ('desechables', 'unidad', 'servilletas', 'manual_por_consumo'),
        ('desechables', 'rollo', 'rollo_papel_chicle', 'manual_por_consumo')
) AS datos(nombre_categoria, nombre_unidad, nombre_item, tipo_control)
JOIN categorias_inventario ci ON ci.nombre_categoria = datos.nombre_categoria
JOIN unidades_medida um ON um.nombre_unidad = datos.nombre_unidad
ON CONFLICT (nombre_item) DO NOTHING;

-- Configuraciones iniciales.
INSERT INTO configuraciones_sistema (
    nombre_configuracion,
    valor_configuracion,
    tipo_valor,
    fecha_inicio_vigencia,
    estado
)
VALUES
    ('valor_base_caja', '300000', 'decimal', CURRENT_DATE, 'activo'),
    ('valor_adicion', '1000', 'decimal', CURRENT_DATE, 'activo')
ON CONFLICT DO NOTHING;

-- Crear existencias generales iniciales en cero para todos los items existentes.
INSERT INTO existencias_inventario_general (id_item_inventario, cantidad_actual)
SELECT ii.id_item_inventario, 0
FROM items_inventario ii
ON CONFLICT (id_item_inventario) DO NOTHING;

COMMIT;

-- ============================================================
-- FIN DEL SCRIPT
-- ============================================================
