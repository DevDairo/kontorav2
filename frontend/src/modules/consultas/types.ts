export type FiltroPeriodo = {
  fechaFin?: string;
  fechaInicio: string;
};

export type ConsultaDetalleVenta = {
  idDetalleVenta: string;
  nombreTipo: string;
  onzas: number;
  cantidad: number;
};

export type ConsultaVenta = {
  idVenta: string;
  idCajaDiaria: string;
  fechaOperacion: string;
  numeroVenta: number;
  fechaVenta: string;
  estadoVenta: string;
  tipoComprador: string;
  idUsuarioVendedor: string;
  nombreUsuarioVendedor: string;
  subtotalVenta: number;
  descuentoPromocion: number;
  totalVenta: number;
  totalPagado: number;
  totalEfectivo: number;
  totalTransferencia: number;
  detalles: ConsultaDetalleVenta[];
};

export type ConsultaGastoCaja = {
  idGastoCaja: string;
  idCajaDiaria: string;
  fechaOperacion: string;
  valorGasto: number;
  descripcion: string;
  estadoGasto: string;
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

export type ConsultaInventarioActual = {
  idItemInventario: string;
  nombreItem: string;
  nombreCategoria: string;
  nombreUnidad: string;
  tipoControl: string;
  idTamanoVaso: string | null;
  onzas: number | null;
  cantidadActualGeneral: number | null;
  fechaActualizacionGeneral: string | null;
  idCajaDiariaAbierta: string | null;
  cantidadInicialDiaria: number | null;
  cantidadIngresadaDiaria: number | null;
  cantidadVendidaDiaria: number | null;
  cantidadPerdidaDiaria: number | null;
  cantidadCortesiaDiaria: number | null;
  cantidadAjustadaDiaria: number | null;
  cantidadFinalTeoricaDiaria: number | null;
};

export type ConsultaMovimientoInventario = {
  idMovimientoInventario: string;
  idItemInventario: string;
  nombreItem: string;
  idCajaDiaria: string | null;
  fechaOperacion: string | null;
  tipoStock: string;
  tipoMovimiento: string;
  cantidad: number;
  sentidoMovimiento: string;
  referenciaOrigen: string;
  idReferenciaOrigen: string | null;
  observacion: string | null;
  idUsuarioRegistro: string;
  nombreUsuarioRegistro: string;
  fechaMovimiento: string;
};

export type ConsultaVentasVasos = {
  idCajaDiaria: string;
  fechaOperacion: string;
  nombreTipo: string;
  onzas: number;
  vasosVendidos: number;
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

export type ConsultaMovimientoDeposito = {
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
