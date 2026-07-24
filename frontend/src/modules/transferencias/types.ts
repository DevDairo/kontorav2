export type EstadoTransferencia = "pendiente" | "validada" | "rechazada";

export type FiltroTransferencias = {
  fechaFin?: string;
  fechaInicio?: string;
  estadoValidacion: EstadoTransferencia;
};

export type ConsultaTransferencia = {
  idPagoVenta: string;
  idVenta: string;
  idCajaDiaria: string;
  fechaOperacion: string;
  numeroVenta: number;
  idUsuarioVendedor: string;
  nombreUsuarioVendedor: string;
  valorPago: number;
  estadoValidacion: EstadoTransferencia;
  fechaRegistro: string;
  idUsuarioValidacion: string | null;
  nombreUsuarioValidacion: string | null;
  fechaValidacion: string | null;
  observacionValidacion: string | null;
  cantidadEvidencias: number;
};

export type DecidirTransferenciaRequest = {
  observacionValidacion?: string;
};
