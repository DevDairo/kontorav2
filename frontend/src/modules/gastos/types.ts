export type AdicionDiaria = {
  idAdicionDiaria: string;
  idCajaDiaria: string;
  cantidadAdiciones: number;
  valorUnitario: number;
  valorTotal: number;
  idUsuarioRegistro: string;
  nombreUsuarioRegistro: string;
  fechaRegistro: string;
};

export type RegistrarAdicionDiariaRequest = {
  cantidadAdiciones: number;
  valorUnitario?: number;
};

export type GastoCaja = {
  idGastoCaja: string;
  idCajaDiaria: string;
  valorGasto: number;
  descripcion: string;
  estadoGasto: "registrado" | "editado" | "anulado";
  idUsuarioRegistro: string;
  nombreUsuarioRegistro: string;
  fechaRegistro: string;
  idUsuarioUltimaEdicion: string | null;
  nombreUsuarioUltimaEdicion: string | null;
  fechaUltimaEdicion: string | null;
  motivoEdicion: string | null;
  idUsuarioAnulacion: string | null;
  nombreUsuarioAnulacion: string | null;
  fechaAnulacion: string | null;
  motivoAnulacion: string | null;
};

export type RegistrarGastoCajaRequest = {
  valorGasto: number;
  descripcion: string;
};

export type EditarGastoCajaRequest = RegistrarGastoCajaRequest & {
  motivoEdicion: string;
};

export type AnularGastoCajaRequest = {
  motivoAnulacion: string;
};

export type PagoTrabajadoresDiario = {
  idPagoTrabajadoresDiario: string;
  idCajaDiaria: string;
  valorTotalPagado: number;
  descripcion: string | null;
  idUsuarioRegistro: string;
  nombreUsuarioRegistro: string;
  fechaRegistro: string;
  confirmadoParaCierre: boolean;
};

export type RegistrarPagoTrabajadoresDiarioRequest = {
  valorTotalPagado: number;
  descripcion?: string;
  confirmadoParaCierre?: boolean;
};

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

export type GastosSnapshot = {
  adicion: AdicionDiaria | null;
  gastos: GastoCaja[];
  pagoTrabajadores: PagoTrabajadoresDiario | null;
  resumenCaja: ResumenCajaDiaria | null;
};
