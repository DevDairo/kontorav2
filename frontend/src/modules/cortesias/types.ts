export type TipoBeneficiarioCortesia = "trabajador" | "otro";

export type RegistrarDetalleCortesiaRequest = {
  idTipoGranizado: string;
  idTamanoVaso: string;
  cantidad: number;
};

export type RegistrarCortesiaRequest = {
  tipoBeneficiario: TipoBeneficiarioCortesia;
  idUsuarioBeneficiario?: string;
  referenciaOtro?: string;
  motivoOtro?: string;
  detalles: RegistrarDetalleCortesiaRequest[];
  confirmaRegistro: true;
};

export type AnularCortesiaRequest = {
  motivoAnulacion: string;
  confirmaNoEntregada: true;
};

export type DetalleCortesia = {
  idDetalleCortesia: string;
  idTipoGranizado: string;
  nombreTipoGranizado: string;
  idTamanoVaso: string;
  onzas: number;
  cantidad: number;
};

export type Cortesia = {
  idCortesia: string;
  idCajaDiaria: string;
  idUsuarioRegistro: string;
  nombreUsuarioRegistro: string;
  tipoBeneficiario: TipoBeneficiarioCortesia;
  idUsuarioBeneficiario: string | null;
  nombreUsuarioBeneficiario: string | null;
  referenciaOtro: string | null;
  motivoOtro: string | null;
  fechaRegistro: string;
  estado: "registrada" | "anulada";
  idUsuarioAnulacion: string | null;
  nombreUsuarioAnulacion: string | null;
  fechaAnulacion: string | null;
  motivoAnulacion: string | null;
  detalles: DetalleCortesia[];
};
