# Guia de componentes frontend

## Referencia visual

La referencia visual inicial proviene de la maqueta ubicada fuera del repo:

```text
C:\Users\corre\Downloads\kontora-pos-maqueta\kontora-pos-maqueta\index.html
C:\Users\corre\Downloads\kontora-pos-maqueta\kontora-pos-maqueta\styles.css
```

La maqueta no es contrato funcional. El frontend real se implementa con React, TypeScript y Vite.

## Principios visuales aplicados

- Layout operativo con sidebar, topbar y area de trabajo.
- Tarjetas compactas para modulos y resumenes operativos.
- Bordes de 8px o menos.
- Paleta clara con acentos azul, verde, cian, morado y naranja.
- Iconos de `lucide-react` para navegacion y botones.
- Componentes preparados para escritorio y movil.

## Componentes base

### `LoginPage`

Ubicacion: `frontend/src/modules/auth/components/LoginPage.tsx`.

Responsabilidades:

- Renderizar el formulario de inicio de sesion.
- Validar campos vacios antes de enviar.
- Consumir `POST /api/auth/login` mediante el provider de autenticacion.
- Mostrar errores devueltos por el backend.
- Presentar un panel decorativo de rayas en escritorio, oculto en movil para priorizar el formulario.

### `AuthProvider`

Ubicacion: `frontend/src/modules/auth/context/AuthContext.tsx`.

Responsabilidades:

- Mantener estado de sesion autenticada.
- Guardar y limpiar el token en `sessionStorage`.
- Reconstruir sesion con `GET /api/auth/me`.
- Ejecutar logout contra `POST /api/auth/logout`.
- Exponer `useAuth` para pantallas y shell.

### `AppShell`

Ubicacion: `frontend/src/shared/components/AppShell.tsx`.

Responsabilidades:

- Renderizar marca, navegacion principal filtrada por rol y topbar.
- Mostrar usuario autenticado y rol.
- Mantener navegacion lateral en escritorio y un menu desplegable en movil.
- Permitir navegacion interna entre vistas base.
- Ejecutar cierre de sesion desde la topbar.
- Contener la pantalla activa.

### `HealthCheckPanel`

Ubicacion: `frontend/src/shared/components/HealthCheckPanel.tsx`.

Responsabilidades:

- Consumir el estado del hook `useHealthCheck`.
- Mostrar URL final de `GET /api/health`.
- Permitir reintento manual.

### `ModuleOverview`

Ubicacion: `frontend/src/shared/components/ModuleOverview.tsx`.

Responsabilidades:

- Mostrar la navegacion visible para el rol autenticado.
- Mostrar modulos disponibles con descripciones orientadas a la operacion.
- Permitir navegar a vistas base de modulos pendientes.

### `RouteWorkspace`

Ubicacion: `frontend/src/shared/components/RouteWorkspace.tsx`.

Responsabilidades:

- Renderizar una vista base para cada modulo pendiente.
- Mostrar descripcion de la pantalla segun rol cuando aplica.
- Mostrar endpoints backend documentados sin ejecutar operaciones de negocio.
- Recordar que el filtro de rol en frontend es de experiencia y no reemplaza permisos backend.

### `appRoutes`

Ubicacion: `frontend/src/app/routes/appRoutes.ts`.

Responsabilidades:

- Centralizar rutas visibles del layout principal.
- Definir roles visibles: `vendedor`, `administrador`, `gerente`.
- Definir estado de pantalla: `base` o `pendiente`.
- Asociar cada ruta con endpoints reales ya documentados.
- Normalizar `nombreRol` recibido desde `/api/auth/me`.

### `CajaAbiertaPanel`

Ubicacion: `frontend/src/modules/caja/components/CajaAbiertaPanel.tsx`.

Responsabilidades:

- Consumir `GET /api/cajas-diarias/abierta` con token real.
- Mostrar estado de carga, caja abierta, ausencia de caja y error.
- Mostrar datos reales de la caja diaria abierta.
- Mostrar formulario de apertura solo para `administrador` y `gerente` cuando no existe caja abierta.
- Ejecutar `POST /api/cajas-diarias` sin asumir que el frontend tiene la decision final de permisos.
- Usar valor base inicial de `300000`, manteniendolo editable antes de la apertura.
- Pedir confirmacion antes de abrir y explicar que la jornada no admite otra caja abierta en paralelo.

### `ConfirmationDialog`

Ubicacion: `frontend/src/shared/components/ConfirmationDialog.tsx`.

Responsabilidades:

- Presentar una confirmacion accesible antes de operaciones irreversibles o sensibles.
- Permitir cancelar sin ejecutar la solicitud de negocio.
- Deshabilitar las acciones mientras backend procesa la confirmacion.
- Cerrar con `Escape` cuando no existe una solicitud en curso.

### `CierreCajaPanel`

Ubicacion: `frontend/src/modules/cierre/components/CierreCajaPanel.tsx`.

Responsabilidades:

- Consultar caja abierta y resumen financiero para administrador y gerente.
- Solicitar efectivo contado sin base, mostrar diferencia estimada y confirmar antes de cerrar.
- Mostrar el resultado persistido, incluido el movimiento de deposito cuando aplica.
- Recuperar un cierre por `fechaOperacion` y volver a la operacion actual sin perder el historial.
- Mantener el cierre de una jornada nocturna asociado a su `fechaOperacion`, aunque `fechaCierre` ocurra el dia siguiente.

### `DepositoPanel`

Ubicacion: `frontend/src/modules/deposito/components/DepositoPanel.tsx`.

Responsabilidades:

- Consultar saldo actual e historial de movimientos para administrador y gerente.
- Mostrar por separado entradas por cierre y salidas por consignacion o pago de servicio.
- Registrar salidas solo despues de una confirmacion y sin permitir valores superiores al saldo devuelto por backend.
- Solicitar evidencia para cada salida y conservar el reintento cuando Supabase Storage no este configurado localmente.
- Consultar tipos de servicio activos desde Catalogos y filtrar el historial por periodo o completo.

### `CatalogosPanel`

Ubicacion: `frontend/src/modules/catalogos/components/CatalogosPanel.tsx`.

Responsabilidades:

- Consumir catalogos autenticados desde `GET /api/catalogos/...`.
- Mostrar conteos de metodos de pago, tipos de granizado, items y promociones vigentes.
- Mostrar precios vigentes, promociones, inventario activo y listas base.
- Permitir filtrar localmente por granizado, item o promocion.
- Permitir cambiar la fecha de vigencia para precios y promociones sin crear ni modificar datos.
- Mostrar la vista `Gestion` solo a administrador y gerente.

### `CatalogosGestionPanel`

Ubicacion: `frontend/src/modules/catalogos/components/CatalogosGestionPanel.tsx`.

Responsabilidades:

- Consultar items activos e inactivos y el historial de precios con token activo.
- Dar de alta consumibles manuales o vasos automaticos, con stock general inicial definido por backend y paquetes fijos de 20 unidades para vasos.
- Permitir editar los datos autorizados, solicitar confirmacion para cambios de estado y mostrar la respuesta real de las restricciones de inventario.
- Registrar vigencias de precios y mostrar el historial sin sobrescribir valores anteriores.
- Mantener separados el control de inventario de vasos y la configuracion de vigencias de precios por tipo de granizado y tamano.

### `VentasPanel`

Ubicacion: `frontend/src/modules/ventas/components/VentasPanel.tsx`.

Responsabilidades:

- Consultar tipos, tamanos, precios, promociones y metodos de pago con token activo.
- Construir detalles de venta y mostrar subtotal, descuento y total estimados.
- Preparar pagos en efectivo, transferencia o mixtos sin exceder el total de la venta.
- Mostrar el cambio estimado antes de registrar y el cambio devuelto por `VentaResponse` despues de registrar.
- Adjuntar comprobantes de transferencia mediante el backend sin exponer secretos de Supabase.

### `InventarioPanel`

Ubicacion: `frontend/src/modules/inventario/components/InventarioPanel.tsx`.

Responsabilidades:

- Consultar stock general, stock diario opcional, movimientos y ajustes con token activo.
- Mantener disponible el control de stock general cuando no existe caja abierta.
- Deshabilitar solo paquetes de vasos y consumos diarios mientras no exista caja abierta.
- Mostrar aplicacion directa para gerente y solicitud de ajuste para administrador.
- Mantener las acciones de aprobar y rechazar visibles solo para gerente.

## Convenciones iniciales

- Componentes React en PascalCase.
- Hooks con prefijo `use`.
- Servicios API en `src/shared/services`.
- Servicios por modulo en `src/modules/[modulo]/services` cuando consumen contratos propios del modulo.
- Tipos API compartidos en `src/shared/types`.
- Un modulo visual no debe inventar endpoints ni campos.
- Los formularios deben validar experiencia de usuario, no reemplazar reglas backend.
