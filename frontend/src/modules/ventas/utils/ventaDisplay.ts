import { formatDisplayName } from "../../../shared/utils/displayText";

type DetalleVentaDisplay = {
  cantidad: number;
  nombreTipo: string;
  onzas: number;
};

type PagoVentaDisplay = {
  nombreMetodo: string;
};

export function formatHoraVenta(fechaVenta: string) {
  return new Intl.DateTimeFormat("es-CO", {
    hour: "numeric",
    minute: "2-digit",
  }).format(new Date(fechaVenta));
}

export function resumenTiposVenta(detalles: DetalleVentaDisplay[] | null | undefined) {
  const tipos = Array.from(new Set((detalles ?? []).map((detalle) => formatDisplayName(detalle.nombreTipo))));
  return tipos.length > 0 ? tipos.join(", ") : "Sin detalle";
}

export function resumenTiposVaso(detalles: DetalleVentaDisplay[] | null | undefined) {
  const tamanos = Array.from(new Set((detalles ?? []).map((detalle) => detalle.onzas)))
    .sort((onzasA, onzasB) => onzasA - onzasB)
    .map((onzas) => `${onzas} oz`);
  return tamanos.length > 0 ? tamanos.join(", ") : "Sin detalle";
}

export function resumenCantidadVasos(detalles: DetalleVentaDisplay[] | null | undefined) {
  const cantidad = (detalles ?? []).reduce((total, detalle) => total + detalle.cantidad, 0);
  return cantidad > 0 ? `${cantidad} ${cantidad === 1 ? "vaso" : "vasos"}` : "Sin detalle";
}

export function resumenMetodosPago(pagos: PagoVentaDisplay[] | null | undefined) {
  const metodos = Array.from(new Set((pagos ?? []).map((pago) => formatDisplayName(pago.nombreMetodo))));
  return metodos.length > 0 ? metodos.join(", ") : "Sin registro";
}
