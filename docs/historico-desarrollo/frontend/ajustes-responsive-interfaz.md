# Ajustes responsive y limpieza de interfaz

Fecha: 2026-07-11.

## Objetivo

Reducir ruido visual y mantener una experiencia consistente entre escritorio y movil, sin modificar contratos backend, schema, roles ni reglas de negocio.

## Cambios transversales

- La navegacion de escritorio conserva el panel lateral. En pantallas de hasta 900 px se convierte en un menu desplegable activado con icono de barras.
- El usuario autenticado y el cierre de sesion permanecen en la esquina superior derecha; se eliminan de la navegacion los indicadores de estado de pantalla.
- Se retiraron de las superficies operativas los textos tecnicos visibles de API, endpoints e integracion local.
- Inicio presenta modulos disponibles con descripciones de negocio, sin exponer contratos internos.
- Los filtros de Transferencias, Evidencias y Consultas comparten la barra visual de Catalogos y usan la accion `Consultar`.

## Inicio de sesion

- Se retiro la maqueta simulada del panel derecho.
- El panel decorativo se reemplazo por un patron diagonal continuo que llena todo el espacio disponible.
- El fondo del patron usa `#f5f8fc`, igual al fondo establecido de la aplicacion.
- El panel decorativo continua oculto en movil para priorizar el formulario de acceso.

## Modulos ajustados

- Ventas: el encabezado deja de mostrar el endpoint y recibe Adiciones diarias. El formulario se habilita solo con caja abierta y conserva el endpoint real de adiciones.
- Caja: conserva el resumen y la proyeccion financiera como lectura; el formulario de Adiciones ya no se duplica en esta ruta.
- Inventario y Catalogos: los nombres visibles reemplazan guiones bajos por espacios y capitalizacion legible. Inventario oculta identificadores tecnicos y rutas API del stock diario y ajustes.
- Transferencias y Evidencias: los paneles de lista y detalle mantienen alturas uniformes en escritorio.
- Consultas: conserva las vistas de solo lectura; el filtro explicito usa `Consultar`.

## Alcance preservado

- Los permisos finales siguen siendo responsabilidad del backend.
- No se modificaron endpoints, DTOs, schema ni logica financiera.
- Supabase sigue preparado solo para despliegue y no recibe secretos ni acceso directo desde frontend.

## Validacion tecnica

- `npx tsc -b --pretty false`: exitoso.
- `npm run build`: exitoso.
- La pantalla `/login` se reviso visualmente en escritorio con el patron aplicado.
