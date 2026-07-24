export type ExistenciaInventarioGeneral = {
  idExistenciaGeneral: string;
  idItemInventario: string;
  nombreItem: string;
  tipoControl: string;
  idTamanoVaso: string | null;
  onzas: number | null;
  cantidadActual: number;
  fechaActualizacion: string;
};

export type ExistenciaInventarioDiario = {
  idExistenciaDiaria: string;
  idCajaDiaria: string;
  idItemInventario: string;
  nombreItem: string;
  idTamanoVaso: string | null;
  onzas: number | null;
  cantidadInicial: number;
  cantidadIngresada: number;
  cantidadVendida: number;
  cantidadPerdida: number;
  cantidadAjustada: number;
  cantidadFinalTeorica: number;
  cantidadFinalContada: number | null;
  diferencia: number | null;
};

export type AjusteInventario = {
  idAjusteInventario: string;
  idItemInventario: string;
  nombreItem: string;
  idCajaDiaria: string | null;
  tipoStock: "general" | "diario";
  cantidadAjuste: number;
  sentidoAjuste: "entrada" | "salida";
  motivoAjuste: string;
  estadoAprobacion: "pendiente" | "aprobado" | "rechazado";
  idUsuarioSolicitante: string;
  nombreUsuarioSolicitante: string;
  idUsuarioAprobador: string | null;
  nombreUsuarioAprobador: string | null;
  fechaSolicitud: string;
  fechaAprobacion: string | null;
  observacionAprobacion: string | null;
};

export type SolicitarAjusteInventarioRequest = {
  idItemInventario: string;
  tipoStock: "general" | "diario";
  cantidadAjuste: number;
  sentidoAjuste: "entrada" | "salida";
  motivoAjuste: string;
};

export type ResolverAjusteInventarioRequest = {
  observacionAprobacion?: string;
};

export type RegistrarPaqueteVasosRequest = {
  idItemInventario: string;
  cantidadPaquetes: number;
  unidadesRotas?: number;
};

export type PaqueteVasosAbiertoResponse = {
  idPaqueteVasosAbierto: string;
  idCajaDiaria: string;
  idItemInventario: string;
  nombreItem: string;
  cantidadPaquetes: number;
  unidadesPorPaquete: number;
  unidadesGeneradas: number;
  unidadesRotas: number;
  unidadesDisponibles: number;
  idUsuarioRegistro: string;
  nombreUsuarioRegistro: string;
  fechaRegistro: string;
};

export type RegistrarConsumoDiarioInventarioRequest = {
  idItemInventario: string;
  cantidadConsumida: number;
  observacion?: string;
};

export type ConsumoDiarioInventarioResponse = {
  idConsumoDiarioInventario: string;
  idCajaDiaria: string;
  idItemInventario: string;
  nombreItem: string;
  cantidadConsumida: number;
  idUsuarioRegistro: string;
  nombreUsuarioRegistro: string;
  fechaRegistro: string;
  observacion: string | null;
};

export type VentasVasosDiarias = {
  idCajaDiaria: string;
  nombreTipo: string;
  onzas: number;
  vasosVendidos: number;
};

export type InventarioSnapshot = {
  ajustes: AjusteInventario[];
  existenciasGenerales: ExistenciaInventarioGeneral[];
  existenciasDiarias: ExistenciaInventarioDiario[];
  ventasVasosDiarias: VentasVasosDiarias[];
};
