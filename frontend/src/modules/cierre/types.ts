export type ResumenCajaDiaria = {
  idCajaDiaria: string;
  fechaOperacion: string;
  valorBase: number;
  totalVentas: number;
  totalVentasEfectivo: number;
  totalVentasTransferencia: number;
  totalTransferenciasPendientes: number;
  totalTransferenciasValidadas: number;
  totalTransferenciasRechazadas: number;
  totalGastos: number;
  totalAdiciones: number;
  adicionDiariaRegistrada: boolean;
  totalPagoTrabajadores: number;
  pagoTrabajadoresRegistrado: boolean;
  pagoTrabajadoresConfirmado: boolean;
  efectivoEsperadoSinBase: number;
  listoParaCierre: boolean;
};

export type CerrarCajaRequest = {
  efectivoContadoSinBase: number;
  observaciones?: string;
};

export type MovimientoDeposito = {
  idMovimientoDeposito: string;
  tipoMovimientoDeposito: string;
  valorMovimiento: number;
  saldoAnterior: number;
  saldoPosterior: number;
  idCierreCaja: string | null;
  idUsuarioRegistro: string;
  nombreUsuarioRegistro: string;
  fechaMovimiento: string;
  observacion: string | null;
};

export type CierreCaja = {
  idCierreCaja: string;
  idCajaDiaria: string;
  totalVentas: number;
  totalVentasEfectivo: number;
  totalVentasTransferencia: number;
  totalTransferenciasPendientes: number;
  totalTransferenciasValidadas: number;
  totalTransferenciasRechazadas: number;
  totalGastos: number;
  totalAdiciones: number;
  totalPagoTrabajadores: number;
  efectivoEsperadoSinBase: number;
  efectivoContadoSinBase: number;
  diferenciaCaja: number;
  valorADeposito: number;
  fechaCierre: string;
  idUsuarioCierre: string;
  nombreUsuarioCierre: string;
  observaciones: string | null;
  movimientoDeposito: MovimientoDeposito | null;
};

export type ConsultaCierreDiario = {
  idCajaDiaria: string;
  fechaOperacion: string;
  estadoCaja: string;
  valorBase: number;
  idCierreCaja: string;
  totalVentas: number;
  totalVentasEfectivo: number;
  totalVentasTransferencia: number;
  totalTransferenciasPendientes: number;
  totalTransferenciasValidadas: number;
  totalTransferenciasRechazadas: number;
  totalGastos: number;
  totalAdiciones: number;
  totalPagoTrabajadores: number;
  efectivoEsperadoSinBase: number;
  efectivoContadoSinBase: number;
  diferenciaCaja: number;
  valorADeposito: number;
  fechaCierre: string;
  idUsuarioCierre: string;
  nombreUsuarioCierre: string;
  observaciones: string | null;
};
