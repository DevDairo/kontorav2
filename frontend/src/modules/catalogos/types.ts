export type CatalogoBasico = {
  id: string;
  nombre: string;
  estado: string;
};

export type TamanoVaso = {
  idTamanoVaso: string;
  onzas: number;
  estado: string;
};

export type UnidadMedida = {
  idUnidadMedida: string;
  nombreUnidad: string;
  abreviatura: string;
  estado: string;
};

export type ItemInventario = {
  idItemInventario: string;
  nombreItem: string;
  tipoControl: string;
  manejaPaquetes: boolean;
  unidadesPorPaquete: number | null;
  estado: string;
  fechaCreacion: string;
  idCategoriaInventario: string;
  nombreCategoria: string;
  idUnidadMedida: string;
  nombreUnidad: string;
  abreviaturaUnidad: string;
  idTamanoVaso: string | null;
  onzas: number | null;
};

export type PrecioGranizado = {
  idPrecioGranizado: string;
  idTipoGranizado: string;
  nombreTipo: string;
  idTamanoVaso: string;
  onzas: number;
  valorPrecio: number;
  fechaInicioVigencia: string;
  fechaFinVigencia: string | null;
  estado: string;
};

export type Promocion = {
  idPromocion: string;
  nombrePromocion: string;
  idTipoGranizado: string;
  nombreTipo: string;
  idTamanoVaso: string;
  onzas: number;
  tipoBeneficiario: string;
  cantidadRequerida: number;
  valorPromocional: number;
  fechaInicioVigencia: string;
  fechaFinVigencia: string | null;
  estado: string;
  diasPromocion: string[];
};

export type CatalogosFormulario = {
  metodosPago: CatalogoBasico[];
  tiposGranizado: CatalogoBasico[];
  tamanosVaso: TamanoVaso[];
  categoriasInventario: CatalogoBasico[];
  unidadesMedida: UnidadMedida[];
  itemsInventario: ItemInventario[];
  preciosVigentes: PrecioGranizado[];
  promocionesVigentes: Promocion[];
  tiposServicio: CatalogoBasico[];
};

export type ItemInventarioGestionRequest = {
  idCategoriaInventario: string;
  idTamanoVaso?: string;
  idUnidadMedida: string;
  manejaPaquetes: boolean;
  nombreItem: string;
  tipoControl: "automatico_por_venta" | "manual_por_consumo";
  unidadesPorPaquete?: number;
};

export type PrecioGranizadoGestionRequest = {
  fechaInicioVigencia: string;
  idTamanoVaso: string;
  idTipoGranizado: string;
  valorPrecio: number;
};
