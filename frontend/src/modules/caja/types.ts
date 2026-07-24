export type CajaDiaria = {
  idCajaDiaria: string;
  fechaOperacion: string;
  estadoCaja: string;
  valorBase: number;
  fechaApertura: string;
  fechaCierre: string | null;
  idUsuarioApertura: string;
  nombreUsuarioApertura: string;
  idUsuarioCierre: string | null;
  nombreUsuarioCierre: string | null;
  observaciones: string | null;
};

export type AbrirCajaDiariaRequest = {
  fechaOperacion: string;
  valorBase: number;
  observaciones?: string | null;
};
