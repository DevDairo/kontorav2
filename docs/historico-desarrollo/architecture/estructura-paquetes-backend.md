# Estructura de paquetes backend

Paquete raiz:

```text
com.kontora.pos
```

Estructura inicial:

```text
com.kontora.pos
├── common
│   ├── audit
│   ├── config
│   ├── controller
│   ├── exception
│   ├── response
│   └── security
├── usuarios
├── caja
├── catalogos
├── ventas
├── pagos
├── inventario
├── deposito
├── evidencias
└── auditoria
```

## Regla de crecimiento

Cada modulo crecera internamente con subpaquetes `domain`, `dto`, `repository`, `service` y `controller` cuando empiece su implementacion funcional.

No se deben crear paquetes paralelos con nombres alternativos a los modulos definidos en la documentacion.

