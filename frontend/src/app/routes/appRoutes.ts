import {
  BadgeDollarSign,
  Boxes,
  ClipboardList,
  Database,
  FileCheck2,
  Landmark,
  LayoutDashboard,
  ListChecks,
  ReceiptText,
  ShieldCheck,
  UsersRound,
  WalletCards,
} from "lucide-react";
import type { ComponentType } from "react";

export type UserRole = "vendedor" | "administrador" | "gerente";
export type RouteStatus = "base" | "pendiente";

export type AppRoute = {
  id: string;
  label: string;
  path: string;
  status: RouteStatus;
  description: string;
  endpoints: string[];
  roleDescriptions?: Partial<Record<UserRole, string>>;
  roleEndpoints?: Partial<Record<UserRole, string[]>>;
  roles: UserRole[];
  icon: ComponentType<{ size?: number; strokeWidth?: number }>;
};

export const roleLabels: Record<UserRole, string> = {
  vendedor: "Vendedor",
  administrador: "Administrador",
  gerente: "Gerente",
};

export const routeStatusLabels: Record<RouteStatus, string> = {
  base: "Base lista",
  pendiente: "Pantalla pendiente",
};

const allRoles: UserRole[] = ["vendedor", "administrador", "gerente"];
const adminRoles: UserRole[] = ["administrador", "gerente"];
const managerRoles: UserRole[] = ["gerente"];

export const appRoutes: AppRoute[] = [
  {
    id: "inicio",
    label: "Inicio",
    path: "/",
    status: "base",
    description: "Panel principal construido con la sesion confirmada por el sistema.",
    endpoints: ["GET /api/auth/me", "GET /api/health"],
    roles: allRoles,
    icon: LayoutDashboard,
  },
  {
    id: "ventas",
    label: "Ventas",
    path: "/ventas",
    status: "base",
    description: "Registro de ventas y consulta operativa segun visibilidad del usuario autenticado.",
    endpoints: [
      "GET /api/ventas/trabajadores",
      "POST /api/ventas",
      "POST /api/ventas/{idVenta}/anular",
      "GET /api/consultas/ventas",
      "GET /api/cortesias/caja-abierta",
      "POST /api/cortesias",
      "POST /api/cortesias/{idCortesia}/anular",
    ],
    roleDescriptions: {
      vendedor: "Registra ventas y consulta sus operaciones de la jornada.",
      administrador: "Registra ventas y cortesias; puede anularlas mientras la caja correspondiente este abierta.",
      gerente: "Registra ventas y cortesias; puede anularlas mientras la caja correspondiente este abierta.",
    },
    roleEndpoints: {
      vendedor: ["GET /api/ventas/trabajadores", "POST /api/ventas", "GET /api/consultas/ventas"],
      administrador: [
        "GET /api/ventas/trabajadores",
        "POST /api/ventas",
        "POST /api/ventas/{idVenta}/anular",
        "GET /api/consultas/ventas",
        "GET /api/cortesias/caja-abierta",
        "POST /api/cortesias",
        "POST /api/cortesias/{idCortesia}/anular",
      ],
      gerente: [
        "GET /api/ventas/trabajadores",
        "POST /api/ventas",
        "POST /api/ventas/{idVenta}/anular",
        "GET /api/consultas/ventas",
        "GET /api/cortesias/caja-abierta",
        "POST /api/cortesias",
        "POST /api/cortesias/{idCortesia}/anular",
      ],
    },
    roles: allRoles,
    icon: BadgeDollarSign,
  },
  {
    id: "caja",
    label: "Caja",
    path: "/caja",
    status: "base",
    description: "Apertura, control de efectivo y operaciones de la caja diaria autorizadas por el sistema.",
    endpoints: [
      "GET /api/cajas-diarias/abierta",
      "POST /api/cajas-diarias",
      "GET /api/cajas-diarias/abierta/resumen",
      "GET /api/operaciones-caja/adiciones-diarias/abierta",
      "POST /api/operaciones-caja/adiciones-diarias",
      "GET /api/operaciones-caja/pagos-trabajadores-diarios/abierta",
    ],
    roleDescriptions: {
      vendedor: "Consulta de la caja abierta para la operacion diaria.",
      administrador: "Consulta, apertura y control financiero de caja diaria validado por el sistema.",
      gerente: "Consulta, apertura y control financiero de caja diaria validado por el sistema.",
    },
    roleEndpoints: {
      vendedor: ["GET /api/cajas-diarias/abierta"],
    },
    roles: allRoles,
    icon: WalletCards,
  },
  {
    id: "inventario",
    label: "Inventario",
    path: "/inventario",
    status: "base",
    description: "Operacion diaria de vasos, consumos y ajustes de inventario autorizados por el sistema.",
    endpoints: [
      "GET /api/inventario/existencias/general",
      "GET /api/inventario/existencias/diarias/abierta",
      "GET /api/inventario/ventas-vasos/diaria-abierta",
      "POST /api/inventario/paquetes-vasos",
      "POST /api/inventario/consumos-diarios",
      "GET /api/inventario/ajustes",
      "POST /api/inventario/ajustes",
      "POST /api/inventario/ajustes/{idAjusteInventario}/aprobar",
      "POST /api/inventario/ajustes/{idAjusteInventario}/rechazar",
      "GET /api/inventario/perdidas-vasos",
      "POST /api/inventario/perdidas-vasos",
      "POST /api/inventario/perdidas-vasos/{idPerdidaInventario}/anular",
      "POST /api/evidencias/perdidas-inventario/{idPerdidaInventario}",
    ],
    roleDescriptions: {
      administrador: "Registra paquetes, devoluciones, consumos y vasos rotos con evidencia diferida.",
      gerente: "Gestiona movimientos de stock, vasos rotos y evidencias incluso despues del cierre.",
    },
    roleEndpoints: {
      administrador: [
        "GET /api/inventario/existencias/general",
        "GET /api/inventario/existencias/diarias/abierta",
        "GET /api/inventario/ventas-vasos/diaria-abierta",
        "POST /api/inventario/paquetes-vasos",
        "POST /api/inventario/consumos-diarios",
        "GET /api/inventario/ajustes",
        "POST /api/inventario/ajustes",
        "GET /api/inventario/perdidas-vasos",
        "POST /api/inventario/perdidas-vasos",
        "POST /api/inventario/perdidas-vasos/{idPerdidaInventario}/anular",
        "POST /api/evidencias/perdidas-inventario/{idPerdidaInventario}",
      ],
    },
    roles: adminRoles,
    icon: Boxes,
  },
  {
    id: "gastos",
    label: "Gastos",
    path: "/gastos",
    status: "base",
    description: "Registro de gastos y pago diario a trabajadores de la caja abierta.",
    endpoints: [
      "GET /api/operaciones-caja/gastos-caja/abierta",
      "POST /api/operaciones-caja/gastos-caja",
      "PUT /api/operaciones-caja/gastos-caja/{idGastoCaja}",
      "POST /api/operaciones-caja/gastos-caja/{idGastoCaja}/anular",
      "GET /api/evidencias/gastos-caja/{idGastoCaja}",
      "POST /api/evidencias/gastos-caja/{idGastoCaja}",
      "GET /api/evidencias/{idArchivoEvidencia}/descargar",
      "GET /api/operaciones-caja/pagos-trabajadores-diarios/abierta",
      "POST /api/operaciones-caja/pagos-trabajadores-diarios",
      "POST /api/operaciones-caja/pagos-trabajadores-diarios/{idPagoTrabajadoresDiario}/confirmar",
    ],
    roleDescriptions: {
      vendedor: "Registra gastos de caja y consulta los gastos de la jornada abierta.",
      administrador: "Registra, edita y anula gastos; tambien administra el pago diario a trabajadores.",
      gerente: "Registra, edita y anula gastos; tambien administra el pago diario a trabajadores.",
    },
    roleEndpoints: {
      vendedor: [
        "GET /api/operaciones-caja/gastos-caja/abierta",
        "POST /api/operaciones-caja/gastos-caja",
        "GET /api/evidencias/gastos-caja/{idGastoCaja}",
        "POST /api/evidencias/gastos-caja/{idGastoCaja}",
        "GET /api/evidencias/{idArchivoEvidencia}/descargar",
      ],
      administrador: [
        "GET /api/operaciones-caja/gastos-caja/abierta",
        "POST /api/operaciones-caja/gastos-caja",
        "PUT /api/operaciones-caja/gastos-caja/{idGastoCaja}",
        "POST /api/operaciones-caja/gastos-caja/{idGastoCaja}/anular",
        "GET /api/evidencias/gastos-caja/{idGastoCaja}",
        "POST /api/evidencias/gastos-caja/{idGastoCaja}",
        "GET /api/evidencias/{idArchivoEvidencia}/descargar",
        "GET /api/operaciones-caja/pagos-trabajadores-diarios/abierta",
        "POST /api/operaciones-caja/pagos-trabajadores-diarios",
        "POST /api/operaciones-caja/pagos-trabajadores-diarios/{idPagoTrabajadoresDiario}/confirmar",
      ],
      gerente: [
        "GET /api/operaciones-caja/gastos-caja/abierta",
        "POST /api/operaciones-caja/gastos-caja",
        "PUT /api/operaciones-caja/gastos-caja/{idGastoCaja}",
        "POST /api/operaciones-caja/gastos-caja/{idGastoCaja}/anular",
        "GET /api/evidencias/gastos-caja/{idGastoCaja}",
        "POST /api/evidencias/gastos-caja/{idGastoCaja}",
        "GET /api/evidencias/{idArchivoEvidencia}/descargar",
        "GET /api/operaciones-caja/pagos-trabajadores-diarios/abierta",
        "POST /api/operaciones-caja/pagos-trabajadores-diarios",
        "POST /api/operaciones-caja/pagos-trabajadores-diarios/{idPagoTrabajadoresDiario}/confirmar",
      ],
    },
    roles: allRoles,
    icon: ReceiptText,
  },
  {
    id: "transferencias",
    label: "Transferencias",
    path: "/transferencias",
    status: "base",
    description: "Consulta de transferencias y comprobantes; la validacion queda reservada al gerente.",
    endpoints: [
      "GET /api/consultas/transferencias",
      "GET /api/evidencias/pagos-venta/{idPagoVenta}",
      "GET /api/evidencias/{idArchivoEvidencia}/descargar",
      "POST /api/evidencias/pagos-venta/{idPagoVenta}/ajustes",
      "POST /api/pagos-venta/{idPagoVenta}/validar",
      "POST /api/pagos-venta/{idPagoVenta}/rechazar",
    ],
    roleDescriptions: {
      vendedor: "Consulta el estado de sus transferencias; el comprobante se adjunta al registrar la venta.",
      administrador: "Consulta transferencias y descarga comprobantes para verificacion.",
      gerente: "Consulta y descarga comprobantes; adjunta ajustes de evidencia y valida o rechaza transferencias pendientes.",
    },
    roleEndpoints: {
      vendedor: ["GET /api/consultas/transferencias"],
      administrador: [
        "GET /api/consultas/transferencias",
        "GET /api/evidencias/pagos-venta/{idPagoVenta}",
        "GET /api/evidencias/{idArchivoEvidencia}/descargar",
      ],
      gerente: [
        "GET /api/consultas/transferencias",
        "GET /api/evidencias/pagos-venta/{idPagoVenta}",
        "GET /api/evidencias/{idArchivoEvidencia}/descargar",
        "POST /api/evidencias/pagos-venta/{idPagoVenta}/ajustes",
        "POST /api/pagos-venta/{idPagoVenta}/validar",
        "POST /api/pagos-venta/{idPagoVenta}/rechazar",
      ],
    },
    roles: allRoles,
    icon: ListChecks,
  },
  {
    id: "catalogos",
    label: "Catalogos",
    path: "/catalogos",
    status: "base",
    description: "Catalogos activos usados por formularios operativos.",
    endpoints: [
      "GET /api/catalogos/metodos-pago",
      "GET /api/catalogos/tipos-granizado",
      "GET /api/catalogos/precios-granizado/vigentes",
      "GET /api/catalogos/promociones/vigentes",
      "GET /api/catalogos/items-inventario",
    ],
    roles: adminRoles,
    icon: Database,
  },
  {
    id: "cierre",
    label: "Cierre",
    path: "/cierre",
    status: "base",
    description: "Arqueo de caja, diferencia y deposito automatico calculados por el sistema.",
    endpoints: [
      "GET /api/cajas-diarias/abierta",
      "GET /api/cajas-diarias/abierta/resumen",
      "POST /api/cajas-diarias/{idCajaDiaria}/cerrar",
      "GET /api/cajas-diarias/{idCajaDiaria}/cierre",
      "GET /api/consultas/cierre?fecha={fechaOperacion}",
    ],
    roles: adminRoles,
    icon: FileCheck2,
  },
  {
    id: "deposito",
    label: "Deposito",
    path: "/deposito",
    status: "base",
    description: "Saldo real, consignaciones bancarias y pagos de servicios descontados por el sistema.",
    endpoints: [
      "GET /api/deposito/saldo",
      "POST /api/deposito/consignaciones-bancarias",
      "POST /api/deposito/pagos-servicios",
      "POST /api/evidencias/consignaciones-bancarias/{idConsignacionBancaria}",
      "POST /api/evidencias/pagos-servicios/{idPagoServicio}",
    ],
    roles: adminRoles,
    icon: Landmark,
  },
  {
    id: "consultas",
    label: "Consultas y evidencias",
    path: "/consultas",
    status: "base",
    description: "Consultas operativas por periodo y gestion de evidencias de gastos y deposito segun el rol.",
    endpoints: [
      "GET /api/consultas/ventas",
      "GET /api/consultas/gastos",
      "GET /api/consultas/inventario/actual",
      "GET /api/consultas/inventario/movimientos",
      "GET /api/consultas/inventario/ventas-vasos",
      "GET /api/consultas/cierre",
      "GET /api/consultas/deposito/movimientos",
      "GET /api/cortesias",
      "GET /api/inventario/perdidas-vasos",
      "GET /api/evidencias/perdidas-inventario/{idPerdidaInventario}",
      "POST /api/evidencias/perdidas-inventario/{idPerdidaInventario}",
      "GET /api/evidencias/gastos-caja/{idGastoCaja}",
      "GET /api/evidencias/consignaciones-bancarias/{idConsignacionBancaria}",
      "GET /api/evidencias/pagos-servicios/{idPagoServicio}",
      "GET /api/evidencias/{idArchivoEvidencia}/descargar",
      "POST /api/evidencias/gastos-caja/{idGastoCaja}",
      "POST /api/evidencias/consignaciones-bancarias/{idConsignacionBancaria}",
      "POST /api/evidencias/pagos-servicios/{idPagoServicio}",
    ],
    roleDescriptions: {
      vendedor: "Consulta ventas, gastos propios y sus evidencias por periodo.",
      administrador: "Consulta la operacion, cortesias, perdidas y administra sus evidencias autorizadas.",
      gerente: "Consulta la operacion, cortesias, perdidas y administra sus evidencias autorizadas.",
    },
    roleEndpoints: {
      vendedor: [
        "GET /api/consultas/ventas",
        "GET /api/consultas/gastos",
        "GET /api/evidencias/gastos-caja/{idGastoCaja}",
        "GET /api/evidencias/{idArchivoEvidencia}/descargar",
      ],
    },
    roles: allRoles,
    icon: ClipboardList,
  },
  {
    id: "usuarios",
    label: "Usuarios",
    path: "/usuarios",
    status: "base",
    description: "Gestion gerencial de usuarios, roles y estados de acceso con trazabilidad.",
    endpoints: [
      "GET /api/usuarios",
      "GET /api/usuarios/roles",
      "POST /api/usuarios",
      "PUT /api/usuarios/{idUsuario}",
      "PUT /api/usuarios/{idUsuario}/estado",
      "PUT /api/usuarios/{idUsuario}/contrasena",
    ],
    roles: managerRoles,
    icon: UsersRound,
  },
  {
    id: "auditoria",
    label: "Auditoria",
    path: "/auditoria",
    status: "base",
    description: "Consulta gerencial de trazabilidad y eventos sensibles registrados por el sistema.",
    endpoints: ["GET /api/consultas/auditoria"],
    roles: managerRoles,
    icon: ShieldCheck,
  },
];

export function normalizeRole(roleName: string): UserRole | null {
  const normalized = roleName.trim().toLowerCase();
  return allRoles.find((role) => role === normalized) ?? null;
}

export function getVisibleRoutes(roleName: string): AppRoute[] {
  const role = normalizeRole(roleName);

  if (!role) {
    return appRoutes.filter((route) => route.id === "inicio");
  }

  return appRoutes.filter((route) => route.roles.includes(role));
}

export function findRouteByPath(routes: AppRoute[], path: string): AppRoute | undefined {
  return routes.find((route) => route.path === path);
}

export function getRouteDescriptionForRole(route: AppRoute, role: UserRole | null): string {
  if (!role) {
    return route.description;
  }

  return route.roleDescriptions?.[role] ?? route.description;
}

export function getRouteEndpointsForRole(route: AppRoute, role: UserRole | null): string[] {
  if (!role) {
    return route.endpoints;
  }

  return route.roleEndpoints?.[role] ?? route.endpoints;
}
