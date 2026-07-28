import { BadgeDollarSign, Minus, Paperclip, Plus, ReceiptText, RefreshCw, Trash2, XCircle } from "lucide-react";
import { FormEvent, useCallback, useEffect, useMemo, useRef, useState } from "react";
import type { UserRole } from "../../../app/routes/appRoutes";
import { consultarVentas } from "../../consultas/services/consultasService";
import type { ConsultaVenta } from "../../consultas/types";
import { obtenerCatalogosFormulario } from "../../catalogos/services/catalogosService";
import type { CatalogosFormulario, Promocion } from "../../catalogos/types";
import { cargarEvidenciaPagoVenta } from "../../evidencias/services/evidenciasService";
import type { ArchivoEvidenciaResponse } from "../../evidencias/types";
import { ConfirmationDialog } from "../../../shared/components/ConfirmationDialog";
import { ApiClientError } from "../../../shared/services/apiClient";
import { formatDisplayName } from "../../../shared/utils/displayText";
import { obtenerCajaAbierta } from "../../caja/services/cajaService";
import { anularVenta, consultarVentaParaAnulacion, listarTrabajadoresVenta, registrarVenta } from "../services/ventasService";
import type { RegistrarPagoVentaRequest, TipoComprador, TrabajadorVenta, VentaResponse } from "../types";
import {
  formatHoraVenta,
  resumenMetodosPago,
  resumenPaquetesVasos,
  resumenTiposVenta,
  resumenTiposVaso,
} from "../utils/ventaDisplay";
import { AdicionesDiariasPanel } from "./AdicionesDiariasPanel";

type PaymentMode = "efectivo" | "transferencia" | "mixto";
type LoadState = "loading" | "success" | "error";

type VentaLinea = {
  id: string;
  idTipoGranizado: string;
  idTamanoVaso: string;
  cantidad: number;
};

type LineaCalculada = VentaLinea & {
  nombreTipo: string;
  onzas: number;
  precioUnitario: number;
  subtotal: number;
  total: number;
  promocion: Promocion | null;
  cantidadConPromocion: number;
  cantidadSinPromocion: number;
  consumeBeneficioTrabajador: boolean;
};

type VentasPanelProps = {
  token: string;
  role: UserRole | null;
};

const dayNames = ["domingo", "lunes", "martes", "miercoles", "jueves", "viernes", "sabado"];

function newLineId() {
  return globalThis.crypto?.randomUUID?.() ?? `${Date.now()}-${Math.random()}`;
}

function toMoney(value: number) {
  return Number.isFinite(value) ? Math.round(value * 100) / 100 : 0;
}

function parseMoney(value: string) {
  return toMoney(Number(value || 0));
}

function parseOptionalMoney(value: string, fallback: number) {
  return value.trim() === "" ? fallback : parseMoney(value);
}

function formatCurrency(value: number | null | undefined) {
  return new Intl.NumberFormat("es-CO", {
    currency: "COP",
    maximumFractionDigits: 0,
    style: "currency",
  }).format(Number(value ?? 0));
}

function messageFor(error: unknown) {
  if (error instanceof ApiClientError) {
    return error.message;
  }

  return error instanceof Error ? error.message : "No fue posible registrar la venta";
}

function promotionApplies(promocion: Promocion, tipoComprador: TipoComprador, fecha: string) {
  if (promocion.tipoBeneficiario !== tipoComprador && promocion.tipoBeneficiario !== "todos") {
    return false;
  }

  if (tipoComprador === "trabajador") {
    return true;
  }

  const day = dayNames[new Date(`${fecha}T00:00:00`).getDay()];
  return promocion.diasPromocion.includes(day);
}

function findPromotion(
  catalogos: CatalogosFormulario | null,
  line: VentaLinea,
  tipoComprador: TipoComprador,
  fecha: string,
) {
  return (
    catalogos?.promocionesVigentes.find(
      (item) =>
        item.idTipoGranizado === line.idTipoGranizado &&
        item.idTamanoVaso === line.idTamanoVaso &&
        promotionApplies(item, tipoComprador, fecha),
    ) ?? null
  );
}

function calculateLine(
  line: VentaLinea,
  catalogos: CatalogosFormulario | null,
  tipoComprador: TipoComprador,
  fecha: string,
  beneficioTrabajadorDisponible: boolean,
): LineaCalculada | null {
  const price = catalogos?.preciosVigentes.find(
    (item) => item.idTipoGranizado === line.idTipoGranizado && item.idTamanoVaso === line.idTamanoVaso,
  );

  if (!price) {
    return null;
  }

  const promocionGeneral =
    tipoComprador === "trabajador" ? findPromotion(catalogos, line, "cliente", fecha) : null;
  const promocionTrabajador =
    tipoComprador === "trabajador" && beneficioTrabajadorDisponible
      ? findPromotion(catalogos, line, "trabajador", fecha)
      : null;
  const promotion =
    tipoComprador === "trabajador"
      ? promocionGeneral ?? promocionTrabajador
      : findPromotion(catalogos, line, "cliente", fecha);
  const consumeBeneficioTrabajador =
    tipoComprador === "trabajador" && promocionGeneral === null && promocionTrabajador !== null;

  const subtotal = toMoney(price.valorPrecio * line.cantidad);

  if (!promotion || line.cantidad < promotion.cantidadRequerida) {
    return {
      ...line,
      cantidadConPromocion: 0,
      cantidadSinPromocion: line.cantidad,
      consumeBeneficioTrabajador: false,
      nombreTipo: price.nombreTipo,
      onzas: price.onzas,
      precioUnitario: price.valorPrecio,
      promocion: null,
      subtotal,
      total: subtotal,
    };
  }

  const availableGroups = Math.floor(line.cantidad / promotion.cantidadRequerida);
  const groups = consumeBeneficioTrabajador ? Math.min(availableGroups, 1) : availableGroups;
  const cantidadConPromocion = groups * promotion.cantidadRequerida;
  const cantidadSinPromocion = line.cantidad - cantidadConPromocion;
  const total = toMoney(groups * promotion.valorPromocional + cantidadSinPromocion * price.valorPrecio);

  return {
    ...line,
    cantidadConPromocion,
    cantidadSinPromocion,
    consumeBeneficioTrabajador,
    nombreTipo: price.nombreTipo,
    onzas: price.onzas,
    precioUnitario: price.valorPrecio,
    promocion: promotion,
    subtotal,
    total,
  };
}

function calculateLines(
  lines: VentaLinea[],
  catalogos: CatalogosFormulario | null,
  tipoComprador: TipoComprador,
  fecha: string,
  beneficioTrabajadorDisponible: boolean,
) {
  const calculated: LineaCalculada[] = [];
  let beneficioRestante = beneficioTrabajadorDisponible;

  for (const line of lines) {
    const result = calculateLine(line, catalogos, tipoComprador, fecha, beneficioRestante);
    if (!result) {
      continue;
    }
    calculated.push(result);
    if (result.consumeBeneficioTrabajador) {
      beneficioRestante = false;
    }
  }

  return calculated;
}

export function VentasPanel({ role, token }: VentasPanelProps) {
  const [catalogos, setCatalogos] = useState<CatalogosFormulario | null>(null);
  const [fechaOperacion, setFechaOperacion] = useState<string | null>(null);
  const [loadState, setLoadState] = useState<LoadState>("loading");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [submitMessage, setSubmitMessage] = useState<string | null>(null);
  const [evidenceMessage, setEvidenceMessage] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isUploadingEvidence, setIsUploadingEvidence] = useState(false);
  const [lastSale, setLastSale] = useState<VentaResponse | null>(null);
  const [lastEvidence, setLastEvidence] = useState<ArchivoEvidenciaResponse | null>(null);
  const [lastChangeEstimate, setLastChangeEstimate] = useState(0);
  const [tipoComprador, setTipoComprador] = useState<TipoComprador>("cliente");
  const [idUsuarioComprador, setIdUsuarioComprador] = useState("");
  const [trabajadores, setTrabajadores] = useState<TrabajadorVenta[]>([]);
  const [idTipoGranizado, setIdTipoGranizado] = useState("");
  const [idTamanoVaso, setIdTamanoVaso] = useState("");
  const [cantidad, setCantidad] = useState("1");
  const [lineas, setLineas] = useState<VentaLinea[]>([]);
  const [paymentMode, setPaymentMode] = useState<PaymentMode>("efectivo");
  const [efectivoParcial, setEfectivoParcial] = useState("");
  const [transferenciaParcial, setTransferenciaParcial] = useState("");
  const [valorRecibido, setValorRecibido] = useState("");
  const [evidenciaTransferencia, setEvidenciaTransferencia] = useState<File | null>(null);
  const evidenciaTransferenciaInputRef = useRef<HTMLInputElement>(null);
  const canCancelSales = role === "administrador" || role === "gerente";
  const [ventasJornada, setVentasJornada] = useState<ConsultaVenta[]>([]);
  const [isLoadingSales, setIsLoadingSales] = useState(false);
  const [idVentaAnulacion, setIdVentaAnulacion] = useState("");
  const [ventaDetalleAnulacion, setVentaDetalleAnulacion] = useState<VentaResponse | null>(null);
  const [isLoadingCancellationDetail, setIsLoadingCancellationDetail] = useState(false);
  const cancellationDetailRequest = useRef(0);
  const [motivoAnulacion, setMotivoAnulacion] = useState("");
  const [cancellationError, setCancellationError] = useState<string | null>(null);
  const [cancellationMessage, setCancellationMessage] = useState<string | null>(null);
  const [pendingCancellation, setPendingCancellation] = useState<VentaResponse | null>(null);
  const [isCancelling, setIsCancelling] = useState(false);
  const lastSalePanelRef = useRef<HTMLElement>(null);

  const loadCatalogos = useCallback(async () => {
    setLoadState("loading");
    setErrorMessage(null);

    try {
      const cajaAbierta = await obtenerCajaAbierta(token);
      const [response, trabajadoresResponse] = await Promise.all([
        obtenerCatalogosFormulario(token, cajaAbierta.fechaOperacion),
        listarTrabajadoresVenta(token),
      ]);
      setFechaOperacion(cajaAbierta.fechaOperacion);
      setCatalogos(response);
      setTrabajadores(trabajadoresResponse);
      setLoadState("success");
      setIdTipoGranizado((current) => current || (response.tiposGranizado[0]?.id ?? ""));
      setIdTamanoVaso((current) => current || (response.tamanosVaso[0]?.idTamanoVaso ?? ""));
    } catch (error) {
      setFechaOperacion(null);
      setLoadState("error");
      setErrorMessage(messageFor(error));
    }
  }, [token]);

  const loadVentasAnulables = useCallback(async () => {
    setIsLoadingSales(true);
    setCancellationError(null);

    try {
      const cajaAbierta = await obtenerCajaAbierta(token);
      const response = await consultarVentas(token, {
        fechaFin: cajaAbierta.fechaOperacion,
        fechaInicio: cajaAbierta.fechaOperacion,
      });
      const registradas = response.filter((venta) => venta.estadoVenta === "registrada");
      setVentasJornada(registradas);
      setIdVentaAnulacion((current) => (registradas.some((venta) => venta.idVenta === current) ? current : ""));
      setVentaDetalleAnulacion((current) =>
        current && registradas.some((venta) => venta.idVenta === current.idVenta) ? current : null,
      );
    } catch (error) {
      setCancellationError(`No fue posible cargar las ventas de la jornada: ${messageFor(error)}`);
    } finally {
      setIsLoadingSales(false);
    }
  }, [token]);

  const loadDetalleVentaAnulacion = useCallback(
    async (idVenta: string) => {
      const requestId = cancellationDetailRequest.current + 1;
      cancellationDetailRequest.current = requestId;
      setVentaDetalleAnulacion(null);
      setIsLoadingCancellationDetail(Boolean(idVenta));

      if (!idVenta) {
        return;
      }

      try {
        const response = await consultarVentaParaAnulacion(token, idVenta);
        if (requestId === cancellationDetailRequest.current) {
          setVentaDetalleAnulacion(response);
        }
      } catch (error) {
        if (requestId === cancellationDetailRequest.current) {
          setCancellationError(`No fue posible consultar el detalle de la venta: ${messageFor(error)}`);
        }
      } finally {
        if (requestId === cancellationDetailRequest.current) {
          setIsLoadingCancellationDetail(false);
        }
      }
    },
    [token],
  );

  useEffect(() => {
    void loadCatalogos();
  }, [loadCatalogos]);

  useEffect(() => {
    if (canCancelSales) {
      void loadVentasAnulables();
    }
  }, [canCancelSales, loadVentasAnulables]);

  useEffect(() => {
    if (paymentMode === "efectivo") {
      setEvidenciaTransferencia(null);
    }
  }, [paymentMode]);

  useEffect(() => {
    if (!lastSale) {
      return;
    }

    const animationFrame = requestAnimationFrame(() => {
      lastSalePanelRef.current?.scrollIntoView({
        behavior: window.matchMedia("(prefers-reduced-motion: reduce)").matches ? "auto" : "smooth",
        block: "start",
      });
    });

    return () => cancelAnimationFrame(animationFrame);
  }, [lastSale]);

  const trabajadorSeleccionado = useMemo(
    () => trabajadores.find((trabajador) => trabajador.idUsuario === idUsuarioComprador) ?? null,
    [idUsuarioComprador, trabajadores],
  );
  const lineasCalculadas = useMemo(() => {
    if (!fechaOperacion) {
      return [];
    }
    return calculateLines(
      lineas,
      catalogos,
      tipoComprador,
      fechaOperacion,
      trabajadorSeleccionado?.beneficioPromocionDisponible ?? false,
    );
  }, [catalogos, fechaOperacion, lineas, tipoComprador, trabajadorSeleccionado?.beneficioPromocionDisponible]);

  const subtotalEstimado = useMemo(
    () => toMoney(lineasCalculadas.reduce((total, line) => total + line.subtotal, 0)),
    [lineasCalculadas],
  );
  const totalEstimado = useMemo(
    () => toMoney(lineasCalculadas.reduce((total, line) => total + line.total, 0)),
    [lineasCalculadas],
  );
  const descuentoEstimado = toMoney(subtotalEstimado - totalEstimado);

  const metodoEfectivo = catalogos?.metodosPago.find((metodo) => metodo.nombre === "efectivo");
  const metodoTransferencia = catalogos?.metodosPago.find((metodo) => metodo.nombre === "transferencia");

  const efectivoMixtoRecibido = paymentMode === "mixto" ? parseMoney(efectivoParcial) : 0;
  const transferenciaIngresada =
    paymentMode === "transferencia"
      ? totalEstimado
      : paymentMode === "mixto"
        ? parseMoney(transferenciaParcial)
        : 0;
  const transferenciaAplicada =
    paymentMode === "transferencia" || paymentMode === "mixto"
      ? toMoney(Math.min(transferenciaIngresada, totalEstimado))
      : 0;
  const transferenciaSobrante =
    paymentMode === "transferencia" || paymentMode === "mixto"
      ? toMoney(Math.max(transferenciaIngresada - totalEstimado, 0))
      : 0;
  const saldoDespuesTransferencia = toMoney(Math.max(totalEstimado - transferenciaAplicada, 0));
  const efectivoMixtoAplicado =
    paymentMode === "mixto" ? toMoney(Math.min(efectivoMixtoRecibido, saldoDespuesTransferencia)) : 0;
  const pagoMixtoInvalido =
    paymentMode === "mixto" &&
    totalEstimado > 0 &&
    (transferenciaIngresada <= 0 ||
      transferenciaIngresada >= totalEstimado ||
      efectivoMixtoRecibido < saldoDespuesTransferencia);

  const pagos = useMemo<RegistrarPagoVentaRequest[]>(() => {
    if (!metodoEfectivo || !metodoTransferencia || totalEstimado <= 0) {
      return [];
    }

    if (paymentMode === "efectivo") {
      return [
        {
          idMetodoPago: metodoEfectivo.id,
          valorPago: totalEstimado,
          valorRecibidoEfectivo: parseOptionalMoney(valorRecibido, totalEstimado),
        },
      ];
    }

    if (paymentMode === "transferencia") {
      return transferenciaAplicada > 0
        ? [{ idMetodoPago: metodoTransferencia.id, valorPago: transferenciaAplicada }]
        : [];
    }

    return [
      efectivoMixtoAplicado > 0
        ? {
            idMetodoPago: metodoEfectivo.id,
            valorPago: efectivoMixtoAplicado,
            valorRecibidoEfectivo: efectivoMixtoRecibido,
          }
        : null,
      transferenciaAplicada > 0
        ? { idMetodoPago: metodoTransferencia.id, valorPago: transferenciaAplicada }
        : null,
    ].filter((pago): pago is RegistrarPagoVentaRequest => Boolean(pago));
  }, [
    efectivoMixtoAplicado,
    efectivoMixtoRecibido,
    metodoEfectivo,
    metodoTransferencia,
    paymentMode,
    totalEstimado,
    transferenciaAplicada,
    valorRecibido,
  ]);

  const totalPagos = useMemo(() => toMoney(pagos.reduce((total, pago) => total + pago.valorPago, 0)), [pagos]);
  const paymentMismatch = totalEstimado > 0 && totalPagos !== totalEstimado;
  const paymentHasBlockingError =
    totalEstimado > 0 && (paymentMismatch || transferenciaSobrante > 0 || pagoMixtoInvalido);
  const pagoEfectivo = useMemo(
    () => pagos.find((pago) => pago.idMetodoPago === metodoEfectivo?.id),
    [metodoEfectivo?.id, pagos],
  );
  const hasTransferPayment = useMemo(
    () => pagos.some((pago) => pago.idMetodoPago === metodoTransferencia?.id),
    [metodoTransferencia?.id, pagos],
  );
  const diferenciaEfectivo =
    paymentMode === "mixto"
      ? toMoney(efectivoMixtoRecibido - efectivoMixtoAplicado)
      : pagoEfectivo
        ? toMoney((pagoEfectivo.valorRecibidoEfectivo ?? 0) - pagoEfectivo.valorPago)
        : 0;
  const showChangePreview =
    paymentMode === "mixto" ? !pagoMixtoInvalido && efectivoMixtoRecibido > 0 : Boolean(pagoEfectivo);
  const diferenciaPagos = toMoney(totalPagos - totalEstimado);
  const faltantePago = toMoney(Math.max(totalEstimado - totalPagos, 0));
  const paymentAlertMessage =
    totalEstimado <= 0 || !paymentHasBlockingError
      ? null
      : pagoMixtoInvalido && transferenciaIngresada <= 0
        ? "En pago mixto registra un valor de transferencia mayor que cero."
        : pagoMixtoInvalido && transferenciaIngresada >= totalEstimado
          ? "En pago mixto la transferencia debe ser menor que el total. Usa Transferencia para cubrir la venta completa."
          : pagoMixtoInvalido
            ? `Faltan ${formatCurrency(saldoDespuesTransferencia - efectivoMixtoRecibido)} en efectivo para completar el pago mixto.`
      : transferenciaSobrante > 0
        ? `La transferencia supera el total por ${formatCurrency(transferenciaSobrante)}. Ajusta el valor antes de registrar.`
        : faltantePago > 0
          ? `Faltan ${formatCurrency(faltantePago)} para completar el total.`
          : diferenciaPagos < 0
        ? `Faltan ${formatCurrency(Math.abs(diferenciaPagos))} para completar el total.`
        : `Los pagos superan el total por ${formatCurrency(diferenciaPagos)}.`;
  const cambioDevueltoBackend = useMemo(
    () => toMoney(lastSale?.pagos.reduce((total, pago) => total + (pago.cambioEntregado ?? 0), 0) ?? 0),
    [lastSale?.pagos],
  );
  const pagoTransferenciaRegistrado = lastSale?.pagos.find((pago) => pago.nombreMetodo === "transferencia");
  const detalleAnulacionSeleccionado =
    ventaDetalleAnulacion?.idVenta === idVentaAnulacion ? ventaDetalleAnulacion : null;

  function addLine() {
    const parsedQuantity = Number(cantidad);

    if (!idTipoGranizado || !idTamanoVaso || !Number.isInteger(parsedQuantity) || parsedQuantity < 1) {
      setSubmitMessage("Selecciona producto, tamano y cantidad valida.");
      return;
    }

    setSubmitMessage(null);
    setLineas((current) => {
      const existing = current.find(
        (line) => line.idTipoGranizado === idTipoGranizado && line.idTamanoVaso === idTamanoVaso,
      );
      if (existing) {
        return current.map((line) =>
          line.id === existing.id ? { ...line, cantidad: line.cantidad + parsedQuantity } : line,
        );
      }
      return [
        ...current,
        {
          cantidad: parsedQuantity,
          id: newLineId(),
          idTamanoVaso,
          idTipoGranizado,
        },
      ];
    });
  }

  function removeLine(id: string) {
    setLineas((current) => current.filter((line) => line.id !== id));
  }

  function adjustQuantity(id: string, delta: number) {
    setLineas((current) =>
      current.map((line) =>
        line.id === id ? { ...line, cantidad: Math.max(1, line.cantidad + delta) } : line,
      ),
    );
  }

  function buildRequest() {
    return {
      detalles: lineas.map((line) => ({
        cantidad: line.cantidad,
        idTamanoVaso: line.idTamanoVaso,
        idTipoGranizado: line.idTipoGranizado,
      })),
      idUsuarioComprador: tipoComprador === "trabajador" ? idUsuarioComprador.trim() : undefined,
      pagos,
      tipoComprador,
    };
  }

  async function uploadEvidenceForSale(sale: VentaResponse, archivo: File) {
    const pagoTransferencia = sale.pagos.find((pago) => pago.nombreMetodo === "transferencia");

    if (!pagoTransferencia) {
      setEvidenceMessage("La venta quedo registrada sin pago por transferencia; no se envio evidencia.");
      return;
    }

    setIsUploadingEvidence(true);

    try {
      const evidencia = await cargarEvidenciaPagoVenta(token, pagoTransferencia.idPagoVenta, archivo);
      setLastEvidence(evidencia);
      setEvidenceMessage("Comprobante de transferencia adjunto correctamente.");
      setEvidenciaTransferencia(null);
      if (evidenciaTransferenciaInputRef.current) {
        evidenciaTransferenciaInputRef.current.value = "";
      }
    } catch (error) {
      setEvidenceMessage(`Venta registrada; evidencia pendiente: ${messageFor(error)}`);
    } finally {
      setIsUploadingEvidence(false);
    }
  }

  async function retryEvidenceUpload() {
    if (!lastSale || !evidenciaTransferencia) {
      return;
    }

    await uploadEvidenceForSale(lastSale, evidenciaTransferencia);
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const evidenciaSeleccionada =
      evidenciaTransferenciaInputRef.current?.files?.[0] ?? evidenciaTransferencia;
    setSubmitMessage(null);
    setEvidenceMessage(null);
    setLastSale(null);
    setLastEvidence(null);
    setLastChangeEstimate(0);

    if (lineas.length === 0) {
      setSubmitMessage("Agrega al menos un detalle de venta.");
      return;
    }

    if (tipoComprador === "trabajador" && !idUsuarioComprador) {
      setSubmitMessage("Selecciona el usuario beneficiario que realiza la compra.");
      return;
    }

    if (!metodoEfectivo || !metodoTransferencia) {
      setSubmitMessage("No estan disponibles los metodos de pago requeridos.");
      return;
    }

    if (paymentHasBlockingError || pagos.length === 0) {
      setSubmitMessage(paymentAlertMessage ?? "La suma de pagos debe coincidir con el total estimado.");
      return;
    }

    if (pagoEfectivo?.valorRecibidoEfectivo !== undefined && pagoEfectivo.valorRecibidoEfectivo < pagoEfectivo.valorPago) {
      setSubmitMessage("El valor recibido en efectivo no puede ser menor al pago.");
      return;
    }

    if (evidenciaSeleccionada && !hasTransferPayment) {
      setSubmitMessage("Adjunta evidencia solo cuando la venta tenga pago por transferencia.");
      return;
    }

    setIsSubmitting(true);

    try {
      const response = await registrarVenta(token, buildRequest());
      const consumioBeneficioTrabajador = lineasCalculadas.some((line) => line.consumeBeneficioTrabajador);
      setLastSale(response);
      setLastChangeEstimate(Math.max(diferenciaEfectivo, 0));
      setLineas([]);
      setEfectivoParcial("");
      setTransferenciaParcial("");
      setValorRecibido("");
      setSubmitMessage(null);
      if (consumioBeneficioTrabajador && idUsuarioComprador) {
        setTrabajadores((current) =>
          current.map((trabajador) =>
            trabajador.idUsuario === idUsuarioComprador
              ? { ...trabajador, beneficioPromocionDisponible: false }
              : trabajador,
          ),
        );
      }
      void listarTrabajadoresVenta(token).then(setTrabajadores).catch(() => undefined);

      if (canCancelSales) {
        void loadVentasAnulables();
      }

      if (evidenciaSeleccionada) {
        await uploadEvidenceForSale(response, evidenciaSeleccionada);
      }
    } catch (error) {
      setSubmitMessage(messageFor(error));
    } finally {
      setIsSubmitting(false);
    }
  }

  function solicitarAnulacion() {
    setCancellationError(null);
    setCancellationMessage(null);

    if (!idVentaAnulacion) {
      setCancellationError("Selecciona una venta registrada de la jornada.");
      return;
    }
    if (!detalleAnulacionSeleccionado) {
      setCancellationError("Espera a que el sistema cargue los vasos y pagos de la venta seleccionada.");
      return;
    }
    if (!motivoAnulacion.trim()) {
      setCancellationError("Indica el motivo de la anulacion.");
      return;
    }

    setPendingCancellation(detalleAnulacionSeleccionado);
  }

  async function confirmarAnulacion() {
    if (!pendingCancellation) {
      return;
    }

    setIsCancelling(true);
    setCancellationError(null);

    try {
      const response = await anularVenta(token, pendingCancellation.idVenta, {
        motivoAnulacion: motivoAnulacion.trim(),
      });
      setLastSale(response);
      setLastEvidence(null);
      setEvidenceMessage(null);
      setLastChangeEstimate(0);
      setCancellationMessage(`Venta #${response.numeroVenta} anulada. Los vasos se devolvieron al stock diario.`);
      setIdVentaAnulacion("");
      setVentaDetalleAnulacion(null);
      setMotivoAnulacion("");
      await loadVentasAnulables();
    } catch (error) {
      setCancellationError(messageFor(error));
    } finally {
      setIsCancelling(false);
      setPendingCancellation(null);
    }
  }

  return (
    <>
      <section className="section-heading" aria-labelledby="ventas-title">
        <div>
          <p className="eyebrow">Ventas y pagos</p>
          <h1 id="ventas-title">Registro de venta</h1>
          <p className="lead">Registra productos, pagos y adiciones de la jornada.</p>
        </div>
        <button
          className="ghost-button"
          type="button"
          onClick={() => {
            void loadCatalogos();
            if (canCancelSales) {
              void loadVentasAnulables();
            }
          }}
          disabled={loadState === "loading"}
        >
          <RefreshCw size={17} strokeWidth={2.2} />
          Reintentar
        </button>
      </section>

      {errorMessage && loadState === "error" ? (
        <div className="form-alert" role="status">
          <ReceiptText size={18} strokeWidth={2.2} />
          <span>{errorMessage}</span>
        </div>
      ) : null}

      <form className="ventas-grid" onSubmit={handleSubmit}>
        <section className="panel ventas-form-panel" aria-labelledby="venta-detalle-title">
          <div className="panel-title">
            <div>
              <h2 id="venta-detalle-title">Detalle</h2>
              <p>{fechaOperacion ?? "Sin caja abierta"}</p>
            </div>
            <span className="badge">{loadState === "loading" ? "Cargando" : `${lineas.length}`}</span>
          </div>

          <div className="ventas-controls">
            <label className="field-label">
              Comprador
              <div className="field-control plain">
                <select
                  value={tipoComprador}
                  onChange={(event) => setTipoComprador(event.target.value as TipoComprador)}
                >
                  <option value="cliente">cliente</option>
                  <option value="trabajador">trabajador</option>
                </select>
              </div>
            </label>

            {tipoComprador === "trabajador" ? (
              <label className="field-label">
                Usuario beneficiario
                <div className="field-control plain">
                  <select
                    value={idUsuarioComprador}
                    onChange={(event) => setIdUsuarioComprador(event.target.value)}
                    disabled={loadState === "loading"}
                  >
                    <option value="">Selecciona un usuario activo</option>
                    {trabajadores.map((trabajador) => (
                      <option key={trabajador.idUsuario} value={trabajador.idUsuario}>
                        {trabajador.nombreCompleto} ({trabajador.nombreUsuario})
                        {!trabajador.beneficioPromocionDisponible ? " - beneficio usado" : ""}
                      </option>
                    ))}
                  </select>
                </div>
                {trabajadorSeleccionado ? (
                  <small className="field-hint">
                    {trabajadorSeleccionado.beneficioPromocionDisponible
                      ? "Beneficio 2x disponible para esta caja operativa."
                      : "El beneficio especial de esta caja ya fue utilizado. En dias de promocion general se conserva la regla por pares."}
                  </small>
                ) : null}
              </label>
            ) : null}

            <label className="field-label">
              Tipo
              <div className="field-control plain">
                <select value={idTipoGranizado} onChange={(event) => setIdTipoGranizado(event.target.value)}>
                  {catalogos?.tiposGranizado.map((tipo) => (
                    <option key={tipo.id} value={tipo.id}>
                      {formatDisplayName(tipo.nombre)}
                    </option>
                  ))}
                </select>
              </div>
            </label>

            <label className="field-label">
              Tamano
              <div className="field-control plain">
                <select value={idTamanoVaso} onChange={(event) => setIdTamanoVaso(event.target.value)}>
                  {catalogos?.tamanosVaso.map((tamano) => (
                    <option key={tamano.idTamanoVaso} value={tamano.idTamanoVaso}>
                      {tamano.onzas} oz
                    </option>
                  ))}
                </select>
              </div>
            </label>

            <label className="field-label">
              Cantidad
              <div className="field-control plain">
                <input
                  min="1"
                  step="1"
                  type="number"
                  value={cantidad}
                  onChange={(event) => setCantidad(event.target.value)}
                />
              </div>
            </label>
          </div>

          <button className="ghost-button" type="button" onClick={addLine} disabled={loadState === "loading"}>
            <Plus size={17} strokeWidth={2.2} />
            Agregar
          </button>

          <ul className="venta-line-list">
            {lineasCalculadas.map((line) => (
              <li className="venta-line" key={line.id}>
                <div>
                  <strong>
                    {formatDisplayName(line.nombreTipo)} · {line.onzas} oz
                  </strong>
                  <small>
                    {line.promocion?.nombrePromocion ?? "precio normal"} · {formatCurrency(line.total)}
                  </small>
                </div>
                <div className="quantity-stepper" aria-label="Cantidad">
                  <button type="button" onClick={() => adjustQuantity(line.id, -1)}>
                    <Minus size={14} strokeWidth={2.4} />
                  </button>
                  <span>{line.cantidad}</span>
                  <button type="button" onClick={() => adjustQuantity(line.id, 1)}>
                    <Plus size={14} strokeWidth={2.4} />
                  </button>
                </div>
                <button className="icon-button" type="button" onClick={() => removeLine(line.id)} aria-label="Eliminar detalle">
                  <Trash2 size={16} strokeWidth={2.2} />
                </button>
              </li>
            ))}
          </ul>
        </section>

        <section className="panel ventas-payment-panel" aria-labelledby="venta-pago-title">
          <div className="panel-title">
            <div>
              <h2 id="venta-pago-title">Pago</h2>
              <p>Total estimado {formatCurrency(totalEstimado)}</p>
            </div>
            <BadgeDollarSign size={22} strokeWidth={2.2} />
          </div>

          <div className="venta-totals">
            <div>
              <span>Subtotal</span>
              <strong>{formatCurrency(subtotalEstimado)}</strong>
            </div>
            <div>
              <span>Descuento</span>
              <strong>{formatCurrency(descuentoEstimado)}</strong>
            </div>
            <div>
              <span>Total</span>
              <strong>{formatCurrency(totalEstimado)}</strong>
            </div>
          </div>

          <div className="payment-mode-grid" role="group" aria-label="Metodo de pago">
            <button
              className={paymentMode === "efectivo" ? "active" : ""}
              type="button"
              onClick={() => setPaymentMode("efectivo")}
            >
              Efectivo
            </button>
            <button
              className={paymentMode === "transferencia" ? "active" : ""}
              type="button"
              onClick={() => setPaymentMode("transferencia")}
            >
              Transferencia
            </button>
            <button
              className={paymentMode === "mixto" ? "active" : ""}
              type="button"
              onClick={() => setPaymentMode("mixto")}
            >
              Mixto
            </button>
          </div>

          {paymentMode === "mixto" ? (
            <div className="ventas-controls two-columns">
              <label className="field-label">
                Efectivo recibido
                <div className="field-control plain">
                  <input
                    min="0"
                    step="100"
                    type="number"
                    value={efectivoParcial}
                    onChange={(event) => setEfectivoParcial(event.target.value)}
                  />
                </div>
              </label>
              <label className="field-label">
                Valor transferencia
                <div className="field-control plain">
                  <input
                    min="0"
                    step="100"
                    type="number"
                    value={transferenciaParcial}
                    onChange={(event) => setTransferenciaParcial(event.target.value)}
                  />
                </div>
              </label>
            </div>
          ) : null}

          {paymentMode === "transferencia" ? (
            <label className="field-label">
              Valor transferencia
              <div className="field-control plain">
                <input
                  min="0"
                  step="100"
                  type="number"
                  value={totalEstimado > 0 ? String(totalEstimado) : ""}
                  readOnly
                />
              </div>
            </label>
          ) : null}

          {paymentMode === "efectivo" && pagoEfectivo ? (
            <label className="field-label">
              Valor recibido efectivo
              <div className="field-control plain">
                <input
                  min="0"
                  step="100"
                  type="number"
                  value={valorRecibido}
                  onChange={(event) => setValorRecibido(event.target.value)}
                  placeholder={String(totalEstimado)}
                />
              </div>
            </label>
          ) : null}

          {paymentMode !== "efectivo" ? (
            <label className="field-label">
              Comprobante transferencia
              <div className="file-control">
                <Paperclip size={18} strokeWidth={2.2} />
                <input
                  accept="image/*,.pdf"
                  name="evidenciaTransferencia"
                  ref={evidenciaTransferenciaInputRef}
                  type="file"
                  onChange={(event) => setEvidenciaTransferencia(event.target.files?.[0] ?? null)}
                />
              </div>
              <small className="field-hint">
                {evidenciaTransferencia
                  ? `Listo para adjuntar: ${evidenciaTransferencia.name}`
                  : "El comprobante se adjunta despues de registrar la venta."}
              </small>
            </label>
          ) : null}

          {paymentMode !== "efectivo" ? (
            <div className="payment-breakdown">
              <div>
                <span>Transferencia a registrar</span>
                <strong>{formatCurrency(transferenciaAplicada)}</strong>
              </div>
              {paymentMode === "mixto" ? (
                <div>
                  <span>Efectivo a registrar</span>
                  <strong>{formatCurrency(efectivoMixtoAplicado)}</strong>
                </div>
              ) : null}
            </div>
          ) : null}

          {showChangePreview ? (
            <div className={`change-preview ${diferenciaEfectivo < 0 ? "danger" : ""}`} role="status">
              <span>{diferenciaEfectivo < 0 ? "Falta efectivo" : "Cambio/devolucion estimada"}</span>
              <strong>{formatCurrency(Math.abs(diferenciaEfectivo))}</strong>
            </div>
          ) : null}

          {paymentAlertMessage ? (
            <div className="form-alert" role="status">
              <ReceiptText size={18} strokeWidth={2.2} />
              <span>{paymentAlertMessage}</span>
            </div>
          ) : null}

          {submitMessage ? (
            <div className="form-alert" role="status">
              <ReceiptText size={18} strokeWidth={2.2} />
              <span>{submitMessage}</span>
            </div>
          ) : null}

          <button
            className="primary-button full"
            type="submit"
            disabled={isSubmitting || loadState === "loading" || totalEstimado <= 0 || paymentHasBlockingError}
          >
            <BadgeDollarSign size={18} strokeWidth={2.2} />
            {isSubmitting ? (isUploadingEvidence ? "Adjuntando evidencia" : "Registrando") : "Registrar venta"}
          </button>
        </section>
      </form>

      {lastSale ? (
        <section ref={lastSalePanelRef} className="panel venta-result" aria-labelledby="venta-result-title">
          <div className="panel-title">
            <div>
              <h2 id="venta-result-title">Venta {lastSale.estadoVenta === "anulada" ? "anulada" : "registrada"} #{lastSale.numeroVenta}</h2>
              <p>{lastSale.estadoVenta}</p>
            </div>
            <span className={`badge ${lastSale.estadoVenta === "anulada" ? "danger" : "success"}`}>{formatCurrency(lastSale.totalVenta)}</span>
          </div>
          <div className="venta-result-grid">
            <div>
              <span>Subtotal</span>
              <strong>{formatCurrency(lastSale.subtotalVenta)}</strong>
            </div>
            <div>
              <span>Descuento</span>
              <strong>{formatCurrency(lastSale.descuentoPromocion)}</strong>
            </div>
            <div>
              <span>Hora de venta</span>
              <strong>{formatHoraVenta(lastSale.fechaVenta)}</strong>
            </div>
            <div>
              <span>Tipo de venta</span>
              <strong>{resumenTiposVenta(lastSale.detalles)}</strong>
            </div>
            <div>
              <span>Tipo de vaso</span>
              <strong>{resumenTiposVaso(lastSale.detalles)}</strong>
            </div>
            <div>
              <span>Vasos vendidos</span>
              <strong>{resumenPaquetesVasos(lastSale.detalles)}</strong>
            </div>
            <div>
              <span>Metodo de pago</span>
              <strong>{resumenMetodosPago(lastSale.pagos)}</strong>
            </div>
            <div>
              <span>Estado de los pagos</span>
              <strong>
                {lastSale.pagos
                  .map((pago) =>
                    pago.cambioEntregado !== null
                      ? `${pago.nombreMetodo}: ${pago.estadoValidacion} · cambio ${formatCurrency(pago.cambioEntregado)}`
                      : `${pago.nombreMetodo}: ${pago.estadoValidacion}`,
                  )
                  .join(" · ")}
              </strong>
            </div>
            {pagoTransferenciaRegistrado ? (
              <div>
                <span>Pago transferencia</span>
                <strong>{pagoTransferenciaRegistrado.idPagoVenta}</strong>
              </div>
            ) : null}
            {lastChangeEstimate > 0 && cambioDevueltoBackend === 0 ? (
              <div>
                <span>Cambio/devolucion estimada</span>
                <strong>{formatCurrency(lastChangeEstimate)}</strong>
              </div>
            ) : null}
            {lastEvidence ? (
              <div>
                <span>Evidencia</span>
                <strong>{lastEvidence.nombreArchivo}</strong>
              </div>
            ) : null}
          </div>
          {evidenceMessage ? (
            <div className={lastEvidence ? "success-alert" : "form-alert venta-evidence-alert"} role="status">
              <ReceiptText size={18} strokeWidth={2.2} />
              <span>{evidenceMessage}</span>
            </div>
          ) : null}
          {evidenciaTransferencia && !lastEvidence && pagoTransferenciaRegistrado ? (
            <button
              className="ghost-button"
              type="button"
              onClick={retryEvidenceUpload}
              disabled={isUploadingEvidence}
            >
              <Paperclip size={17} strokeWidth={2.2} />
              {isUploadingEvidence ? "Adjuntando" : "Reintentar evidencia"}
            </button>
          ) : null}
        </section>
      ) : null}

      {canCancelSales ? (
        <section className="panel venta-cancellation-panel" aria-labelledby="venta-cancellation-title">
          <div className="panel-title">
            <div>
              <h2 id="venta-cancellation-title">Anular venta de la jornada</h2>
              <p>Disponible mientras la caja diaria permanezca abierta.</p>
            </div>
            <XCircle size={22} strokeWidth={2.2} />
          </div>

          {cancellationError ? (
            <div className="form-alert" role="status">
              <ReceiptText size={18} strokeWidth={2.2} />
              <span>{cancellationError}</span>
            </div>
          ) : null}
          {cancellationMessage ? (
            <div className="success-alert" role="status">
              <ReceiptText size={18} strokeWidth={2.2} />
              <span>{cancellationMessage}</span>
            </div>
          ) : null}

          {ventasJornada.length === 0 && !isLoadingSales ? (
            <p className="empty-state">No hay ventas registradas disponibles para anular en esta jornada.</p>
          ) : (
            <div className="ventas-controls venta-cancellation-fields">
              <label className="field-label">
                Venta registrada
                <div className="field-control plain">
                  <select
                    value={idVentaAnulacion}
                    onChange={(event) => {
                      const idVenta = event.target.value;
                      setIdVentaAnulacion(idVenta);
                      setCancellationError(null);
                      void loadDetalleVentaAnulacion(idVenta);
                    }}
                    disabled={isLoadingSales || isCancelling}
                  >
                    <option value="">Selecciona una venta</option>
                    {ventasJornada.map((venta) => (
                      <option key={venta.idVenta} value={venta.idVenta}>
                        #{venta.numeroVenta} · {formatHoraVenta(venta.fechaVenta)} · {venta.nombreUsuarioVendedor} · {formatCurrency(venta.totalVenta)}
                      </option>
                    ))}
                  </select>
                </div>
              </label>
              <label className="field-label">
                Motivo de anulacion
                <div className="field-control plain">
                  <input
                    maxLength={300}
                    value={motivoAnulacion}
                    onChange={(event) => setMotivoAnulacion(event.target.value)}
                    placeholder="Ej. Venta duplicada"
                    disabled={isCancelling}
                  />
                </div>
              </label>
            </div>
          )}

          {isLoadingCancellationDetail ? <p className="empty-state">Verificando vasos y pagos de la venta seleccionada.</p> : null}

          {detalleAnulacionSeleccionado ? (
            <div className="venta-result-grid venta-cancellation-summary">
              <div>
                <span>Vendedor</span>
                <strong>{detalleAnulacionSeleccionado.nombreUsuarioVendedor}</strong>
              </div>
              <div>
                <span>Total de venta</span>
                <strong>{formatCurrency(detalleAnulacionSeleccionado.totalVenta)}</strong>
              </div>
              <div>
                <span>Hora de venta</span>
                <strong>{formatHoraVenta(detalleAnulacionSeleccionado.fechaVenta)}</strong>
              </div>
              <div>
                <span>Tipo de venta</span>
                <strong>{resumenTiposVenta(detalleAnulacionSeleccionado.detalles)}</strong>
              </div>
              <div>
                <span>Tipo de vaso</span>
                <strong>{resumenTiposVaso(detalleAnulacionSeleccionado.detalles)}</strong>
              </div>
              <div>
                <span>Vasos vendidos</span>
                <strong>{resumenPaquetesVasos(detalleAnulacionSeleccionado.detalles)}</strong>
              </div>
              <div>
                <span>Metodo de pago</span>
                <strong>{resumenMetodosPago(detalleAnulacionSeleccionado.pagos)}</strong>
              </div>
            </div>
          ) : null}

          <div className="venta-cancellation-actions">
            <button
              className="ghost-button"
              type="button"
              onClick={() => void loadVentasAnulables()}
              disabled={isLoadingSales || isCancelling}
            >
              <RefreshCw size={17} strokeWidth={2.2} />
              {isLoadingSales ? "Actualizando" : "Actualizar ventas"}
            </button>
            <button
              className="danger-button"
              type="button"
              onClick={solicitarAnulacion}
              disabled={isLoadingSales || isLoadingCancellationDetail || isCancelling || !detalleAnulacionSeleccionado}
            >
              <XCircle size={17} strokeWidth={2.2} />
              Anular venta
            </button>
          </div>
        </section>
      ) : null}

      <AdicionesDiariasPanel token={token} />

      <ConfirmationDialog
        confirmLabel="Anular venta"
        description={
          pendingCancellation
            ? `Anularas la venta #${pendingCancellation.numeroVenta}, registrada a las ${formatHoraVenta(pendingCancellation.fechaVenta)}, por ${formatCurrency(pendingCancellation.totalVenta)}. Tipo de venta: ${resumenTiposVenta(pendingCancellation.detalles)}. Tipo de vaso: ${resumenTiposVaso(pendingCancellation.detalles)}. Metodo de pago: ${resumenMetodosPago(pendingCancellation.pagos)}. El sistema restaurara los vasos de esta venta al stock diario.`
            : ""
        }
        isConfirming={isCancelling}
        onCancel={() => setPendingCancellation(null)}
        onConfirm={() => void confirmarAnulacion()}
        open={pendingCancellation !== null}
        title="Confirmar anulacion de venta"
      />
    </>
  );
}
