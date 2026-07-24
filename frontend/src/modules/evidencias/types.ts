export type ArchivoEvidenciaResponse = {
  idArchivoEvidencia: string;
  idPagoVenta: string | null;
  idGastoCaja: string | null;
  idConsignacionBancaria: string | null;
  idPagoServicio: string | null;
  urlArchivo: string;
  nombreArchivo: string;
  tipoArchivo: string;
  formatoArchivo: string;
  tamanoOriginalKb: number;
  tamanoComprimidoKb: number | null;
  fueComprimido: boolean;
  fechaSubida: string;
  idUsuarioSubida: string;
  nombreUsuarioSubida: string;
  estado: string;
};
