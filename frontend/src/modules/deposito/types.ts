export type MovimientoDeposito = {
  idMovimientoDeposito: string;
  tipoMovimientoDeposito: string;
  valorMovimiento: number;
  saldoAnterior: number;
  saldoPosterior: number;
  idCierreCaja: string | null;
  idConsignacionBancaria: string | null;
  idPagoServicio: string | null;
  nombreServicio: string | null;
  idUsuarioRegistro: string;
  nombreUsuarioRegistro: string;
  fechaMovimiento: string;
  observacion: string | null;
};

export type SaldoDeposito = {
  saldoActual: number;
};

export type RegistrarConsignacionBancariaRequest = {
  valorConsignado: number;
  observacion?: string;
};

export type ConsignacionBancaria = {
  idConsignacionBancaria: string;
  valorConsignado: number;
  fechaConsignacion: string;
  idUsuarioRegistro: string;
  nombreUsuarioRegistro: string;
  observacion: string | null;
  estado: string;
  movimientoDeposito: MovimientoDepositoBase;
};

export type RegistrarPagoServicioRequest = {
  idTipoServicio: string;
  valorPagado: number;
  descripcion?: string;
};

export type PagoServicio = {
  idPagoServicio: string;
  idTipoServicio: string;
  nombreServicio: string;
  valorPagado: number;
  descripcion: string | null;
  fechaPago: string;
  idUsuarioRegistro: string;
  nombreUsuarioRegistro: string;
  estado: string;
  movimientoDeposito: MovimientoDepositoBase;
};

export type MovimientoDepositoBase = Omit<
  MovimientoDeposito,
  "idConsignacionBancaria" | "idPagoServicio" | "nombreServicio"
>;
