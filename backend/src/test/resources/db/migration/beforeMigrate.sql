DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid = t.typnamespace WHERE t.typname = 'estado_basico_enum' AND n.nspname = 'kontora_pos_test') THEN
        CREATE TYPE estado_basico_enum AS ENUM ('activo', 'inactivo');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid = t.typnamespace WHERE t.typname = 'estado_usuario_enum' AND n.nspname = 'kontora_pos_test') THEN
        CREATE TYPE estado_usuario_enum AS ENUM ('activo', 'inactivo', 'bloqueado');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid = t.typnamespace WHERE t.typname = 'estado_credencial_enum' AND n.nspname = 'kontora_pos_test') THEN
        CREATE TYPE estado_credencial_enum AS ENUM ('activa', 'bloqueada', 'expirada', 'revocada');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid = t.typnamespace WHERE t.typname = 'estado_sesion_enum' AND n.nspname = 'kontora_pos_test') THEN
        CREATE TYPE estado_sesion_enum AS ENUM ('activa', 'cerrada', 'expirada', 'revocada');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid = t.typnamespace WHERE t.typname = 'estado_caja_enum' AND n.nspname = 'kontora_pos_test') THEN
        CREATE TYPE estado_caja_enum AS ENUM ('abierta', 'cerrada');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid = t.typnamespace WHERE t.typname = 'estado_venta_enum' AND n.nspname = 'kontora_pos_test') THEN
        CREATE TYPE estado_venta_enum AS ENUM ('registrada', 'anulada');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid = t.typnamespace WHERE t.typname = 'tipo_comprador_enum' AND n.nspname = 'kontora_pos_test') THEN
        CREATE TYPE tipo_comprador_enum AS ENUM ('cliente', 'trabajador');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid = t.typnamespace WHERE t.typname = 'estado_validacion_transferencia_enum' AND n.nspname = 'kontora_pos_test') THEN
        CREATE TYPE estado_validacion_transferencia_enum AS ENUM ('no_aplica', 'pendiente', 'validada', 'rechazada');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid = t.typnamespace WHERE t.typname = 'tipo_beneficiario_enum' AND n.nspname = 'kontora_pos_test') THEN
        CREATE TYPE tipo_beneficiario_enum AS ENUM ('cliente', 'trabajador', 'todos');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid = t.typnamespace WHERE t.typname = 'dia_semana_enum' AND n.nspname = 'kontora_pos_test') THEN
        CREATE TYPE dia_semana_enum AS ENUM ('lunes', 'martes', 'miercoles', 'jueves', 'viernes', 'sabado', 'domingo');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid = t.typnamespace WHERE t.typname = 'estado_gasto_enum' AND n.nspname = 'kontora_pos_test') THEN
        CREATE TYPE estado_gasto_enum AS ENUM ('registrado', 'editado', 'anulado');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid = t.typnamespace WHERE t.typname = 'tipo_control_inventario_enum' AND n.nspname = 'kontora_pos_test') THEN
        CREATE TYPE tipo_control_inventario_enum AS ENUM ('automatico_por_venta', 'manual_por_consumo');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid = t.typnamespace WHERE t.typname = 'tipo_stock_enum' AND n.nspname = 'kontora_pos_test') THEN
        CREATE TYPE tipo_stock_enum AS ENUM ('general', 'diario');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid = t.typnamespace WHERE t.typname = 'tipo_movimiento_inventario_enum' AND n.nspname = 'kontora_pos_test') THEN
        CREATE TYPE tipo_movimiento_inventario_enum AS ENUM ('entrada', 'salida', 'venta', 'anulacion_venta', 'perdida', 'ajuste', 'consumo_diario', 'apertura_paquete');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid = t.typnamespace WHERE t.typname = 'sentido_movimiento_enum' AND n.nspname = 'kontora_pos_test') THEN
        CREATE TYPE sentido_movimiento_enum AS ENUM ('entrada', 'salida');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid = t.typnamespace WHERE t.typname = 'estado_aprobacion_enum' AND n.nspname = 'kontora_pos_test') THEN
        CREATE TYPE estado_aprobacion_enum AS ENUM ('pendiente', 'aprobado', 'rechazado');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid = t.typnamespace WHERE t.typname = 'tipo_movimiento_deposito_enum' AND n.nspname = 'kontora_pos_test') THEN
        CREATE TYPE tipo_movimiento_deposito_enum AS ENUM ('entrada_cierre', 'salida_consignacion', 'salida_pago_servicio', 'ajuste');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid = t.typnamespace WHERE t.typname = 'estado_registro_financiero_enum' AND n.nspname = 'kontora_pos_test') THEN
        CREATE TYPE estado_registro_financiero_enum AS ENUM ('registrado', 'anulado');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid = t.typnamespace WHERE t.typname = 'tipo_valor_configuracion_enum' AND n.nspname = 'kontora_pos_test') THEN
        CREATE TYPE tipo_valor_configuracion_enum AS ENUM ('texto', 'entero', 'decimal', 'booleano');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid = t.typnamespace WHERE t.typname = 'tipo_archivo_enum' AND n.nspname = 'kontora_pos_test') THEN
        CREATE TYPE tipo_archivo_enum AS ENUM ('imagen', 'pdf', 'otro');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid = t.typnamespace WHERE t.typname = 'formato_archivo_enum' AND n.nspname = 'kontora_pos_test') THEN
        CREATE TYPE formato_archivo_enum AS ENUM ('jpg', 'jpeg', 'png', 'webp', 'pdf', 'otro');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid = t.typnamespace WHERE t.typname = 'accion_auditoria_enum' AND n.nspname = 'kontora_pos_test') THEN
        CREATE TYPE accion_auditoria_enum AS ENUM ('crear', 'editar', 'anular', 'aprobar', 'rechazar', 'abrir', 'cerrar', 'validar', 'revocar', 'login', 'logout');
    END IF;
END $$;
