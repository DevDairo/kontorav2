export type FiltroAuditoria = {
  accion?: string;
  fechaFin?: string;
  fechaInicio: string;
  tablaAfectada?: string;
};

export type ConsultaAuditoria = {
  accion: string;
  descripcion: string | null;
  direccionIp: string | null;
  fechaAccion: string;
  idAuditoriaOperacion: string;
  idRegistroAfectado: string | null;
  idUsuario: string;
  nombreUsuario: string;
  tablaAfectada: string;
  valorAnterior: string | null;
  valorNuevo: string | null;
};
