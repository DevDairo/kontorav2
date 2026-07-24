# Kontora POS - Indice Operativo

## Estado del proyecto

- Fase 3, desarrollo backend: terminada y validada.
- Fase 4, desarrollo frontend: terminada y validada manualmente.
- Infraestructura local separada y validada: PostgreSQL, Storage, backend,
  frontend Nginx y Cloudflare Tunnel se ejecutan en contenedores distintos.
- Publicacion de demostracion validada mediante `https://kontora-pos.store`.
  El VPS requiere secretos propios, restauracion o inicializacion de datos y
  rotacion del token del tunnel.
- La fuente historica de decisiones, pruebas y requisitos se conserva en [historico-desarrollo](./historico-desarrollo/).

## Modulos activos

| Orden | Documento | Ruta principal | Roles |
| --- | --- | --- | --- |
| 01 | [Login y sesion](./01-login-y-sesion.md) | `/login` | Todos |
| 02 | [Caja diaria](./02-caja-diaria.md) | `/caja` | Todos, con acciones administrativas |
| 03 | [Catalogos](./03-catalogos.md) | `/catalogos` | Administrador, gerente |
| 04 | [Ventas y pagos](./04-ventas-y-pagos.md) | `/ventas` | Todos |
| 05 | [Inventario operativo](./05-inventario-operativo.md) | `/inventario` | Administrador, gerente |
| 06 | [Gastos y pago a trabajadores](./06-gastos-y-pago-trabajadores.md) | `/gastos` | Todos, con acciones administrativas |
| 07 | [Cierre de caja](./07-cierre-de-caja.md) | `/cierre` | Administrador, gerente |
| 08 | [Deposito y servicios](./08-deposito-y-servicios.md) | `/deposito` | Administrador, gerente |
| 10 | [Transferencias](./10-transferencias.md) | `/transferencias` | Todos, con validacion gerencial |
| 11 | [Consultas y evidencias](./11-consultas.md) | `/consultas` | Segun rol; adjuntos administrativos para administrador y gerente |
| 12 | [Usuarios](./12-usuarios.md) | `/usuarios` | Gerente |
| 13 | [Auditoria](./13-auditoria.md) | `/auditoria` | Gerente |
| 14 | [Credenciales del gerente inicial](./14-credenciales-gerente-inicial.md) | Configuracion | Responsable de despliegue |
| 15 | [Cambios recientes validados](./15-cambios-recientes-validados.md) | Trazabilidad | Responsable tecnico y validacion funcional |

La capacidad transversal de [Evidencias](./09-evidencias.md) permanece documentada, pero ya no tiene una pantalla independiente: sus funciones de gastos y deposito estan integradas en `/consultas`; los comprobantes de venta permanecen en Transferencias.

## Convenciones

- El backend es la autoridad para permisos, transacciones y reglas de negocio.
- El frontend adapta la experiencia por rol, pero no reemplaza la autorizacion backend.
- Las fechas de operacion de caja no son necesariamente la fecha tecnica de registro de una auditoria.
- El schema, migraciones, DTOs y controladores reales prevalecen sobre cualquier descripcion resumida.
- Los secretos se definen solo en archivos `.env` ignorados por Git.
- Los documentos dentro de `historico-desarrollo` conservan decisiones
  anteriores, incluso referencias a Vercel o Supabase Cloud. No son una guia
  operativa para el despliegue actual.

## Fuentes de referencia

- Requisitos funcionales: `historico-desarrollo/requirements/source/Requisitos_Kontora_POS_Reconstruido.md`.
- Schema canonico: `database/schema/kontora_pos_schema_v1_1.sql`.
- Migracion base: `backend/src/main/resources/db/migration/V1__schema_inicial_kontora_pos.sql`.
- Guia de ejecucion y despliegue: [README principal](../README.md).
