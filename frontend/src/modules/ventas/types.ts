export type TipoComprador = "cliente" | "trabajador";

export type RegistrarDetalleVentaRequest = {
  idTipoGranizado: string;
  idTamanoVaso: string;
  cantidad: number;
};

export type RegistrarPagoVentaRequest = {
  idMetodoPago: string;
  valorPago: number;
  valorRecibidoEfectivo?: number;
};

export type RegistrarVentaRequest = {
  tipoComprador: TipoComprador;
  idUsuarioComprador?: string;
  detalles: RegistrarDetalleVentaRequest[];
  pagos: RegistrarPagoVentaRequest[];
};

export type AnularVentaRequest = {
  motivoAnulacion: string;
};

export type TrabajadorVenta = {
  idUsuario: string;
  nombreCompleto: string;
  nombreUsuario: string;
};

export type DetalleVentaResponse = {
  idDetalleVenta: string;
  idTipoGranizado: string;
  nombreTipo: string;
  idTamanoVaso: string;
  onzas: number;
  cantidad: number;
  precioUnitarioNormal: number;
  cantidadConPromocion: number;
  cantidadSinPromocion: number;
  valorPromocionalAplicado: number | null;
  idPromocionAplicada: string | null;
  nombrePromocionAplicada: string | null;
  subtotalLinea: number;
  totalLinea: number;
};

export type PagoVentaResponse = {
  idPagoVenta: string;
  idMetodoPago: string;
  nombreMetodo: string;
  valorPago: number;
  valorRecibidoEfectivo: number | null;
  cambioEntregado: number | null;
  estadoValidacion: string;
  fechaRegistro: string;
  idUsuarioValidacion: string | null;
  nombreUsuarioValidacion: string | null;
  fechaValidacion: string | null;
  observacionValidacion: string | null;
};

export type VentaResponse = {
  idVenta: string;
  idCajaDiaria: string;
  idUsuarioVendedor: string;
  nombreUsuarioVendedor: string;
  tipoComprador: TipoComprador;
  idUsuarioComprador: string | null;
  numeroVenta: number;
  fechaVenta: string;
  estadoVenta: string;
  subtotalVenta: number;
  descuentoPromocion: number;
  totalVenta: number;
  detalles: DetalleVentaResponse[];
  pagos: PagoVentaResponse[];
};
