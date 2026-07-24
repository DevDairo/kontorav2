import { formatDisplayName } from "../../../shared/utils/displayText";

type DetalleVentaDisplay = {
  cantidad: number;
  nombreTipo: string;
  onzas: number;
};

type PagoVentaDisplay = {
  nombreMetodo: string;
};

const VASOS_POR_PAQUETE = 20;

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
  const cantidadesPorTamano = new Map<number, number>();
  (detalles ?? []).forEach((detalle) => {
    cantidadesPorTamano.set(detalle.onzas, (cantidadesPorTamano.get(detalle.onzas) ?? 0) + detalle.cantidad);
  });

  const vasos = Array.from(cantidadesPorTamano.entries())
    .sort(([onzasA], [onzasB]) => onzasA - onzasB)
    .map(([onzas, cantidad]) => `${onzas} oz (${cantidad} ${cantidad === 1 ? "vaso" : "vasos"})`);
  return vasos.length > 0 ? vasos.join(", ") : "Sin detalle";
}

export function resumenPaquetesVasos(detalles: DetalleVentaDisplay[] | null | undefined) {
  const cantidadesPorTamano = new Map<number, number>();
  (detalles ?? []).forEach((detalle) => {
    cantidadesPorTamano.set(detalle.onzas, (cantidadesPorTamano.get(detalle.onzas) ?? 0) + detalle.cantidad);
  });

  const equivalencias = Array.from(cantidadesPorTamano.entries())
    .sort(([onzasA], [onzasB]) => onzasA - onzasB)
    .map(([onzas, cantidad]) => {
      const paquetes = Math.floor(cantidad / VASOS_POR_PAQUETE);
      const vasosRestantes = cantidad % VASOS_POR_PAQUETE;
      const partes: string[] = [];

      if (paquetes > 0) {
        partes.push(`${paquetes} ${paquetes === 1 ? "paquete" : "paquetes"}`);
      }
      if (vasosRestantes > 0 || paquetes === 0) {
        partes.push(`${vasosRestantes} ${vasosRestantes === 1 ? "vaso" : "vasos"}`);
      }

      return `${onzas} oz: ${partes.join(" + ")}`;
    });
  return equivalencias.length > 0 ? equivalencias.join(", ") : "Sin detalle";
}

export function resumenMetodosPago(pagos: PagoVentaDisplay[] | null | undefined) {
  const metodos = Array.from(new Set((pagos ?? []).map((pago) => formatDisplayName(pago.nombreMetodo))));
  return metodos.length > 0 ? metodos.join(", ") : "Sin registro";
}
