import { AlertCircle, Download, Eye, FileImage, FileText, LoaderCircle } from "lucide-react";
import { useEffect, useMemo, useRef, useState } from "react";
import { descargarEvidencia, messageForEvidenceDownload } from "../services/evidenciasService";
import type { ArchivoEvidenciaResponse } from "../types";

type EvidenceGalleryProps = {
  allowFileAccess?: boolean;
  autoScrollToPreview?: boolean;
  emptyMessage?: string;
  evidencias: ArchivoEvidenciaResponse[];
  token: string;
};

type PreviewState = "idle" | "loading" | "success" | "error";

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat("es-CO", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

function formatFileSize(value: number | null) {
  return value === null ? "Sin dato" : `${value} KB`;
}

function isImage(evidencia: ArchivoEvidenciaResponse, contentType: string) {
  return contentType.startsWith("image/")
    || ["jpg", "jpeg", "png", "webp"].includes(evidencia.formatoArchivo.toLowerCase());
}

function isPdf(evidencia: ArchivoEvidenciaResponse, contentType: string) {
  return contentType === "application/pdf" || evidencia.formatoArchivo.toLowerCase() === "pdf";
}

export function EvidenceGallery({
  allowFileAccess = true,
  autoScrollToPreview = false,
  emptyMessage = "No hay evidencias registradas.",
  evidencias,
  token,
}: EvidenceGalleryProps) {
  const [selectedEvidenceId, setSelectedEvidenceId] = useState<string | null>(null);
  const [previewState, setPreviewState] = useState<PreviewState>("idle");
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [previewContentType, setPreviewContentType] = useState("");
  const [previewError, setPreviewError] = useState<string | null>(null);
  const previewPanelRef = useRef<HTMLDivElement>(null);
  const lastScrolledEvidenceIdRef = useRef<string | null>(null);

  const selectedEvidence = useMemo(
    () => evidencias.find((item) => item.idArchivoEvidencia === selectedEvidenceId) ?? evidencias[0] ?? null,
    [evidencias, selectedEvidenceId],
  );

  useEffect(() => {
    if (evidencias.length === 0) {
      setSelectedEvidenceId(null);
      return;
    }

    if (!selectedEvidenceId || !evidencias.some((item) => item.idArchivoEvidencia === selectedEvidenceId)) {
      setSelectedEvidenceId(evidencias[0].idArchivoEvidencia);
    }
  }, [evidencias, selectedEvidenceId]);

  useEffect(() => {
    if (!selectedEvidence || !allowFileAccess) {
      setPreviewState("idle");
      setPreviewUrl(null);
      setPreviewContentType("");
      setPreviewError(null);
      return;
    }

    let active = true;
    let objectUrl: string | null = null;

    setPreviewState("loading");
    setPreviewUrl(null);
    setPreviewContentType("");
    setPreviewError(null);

    void descargarEvidencia(token, selectedEvidence.idArchivoEvidencia)
      .then((archivo) => {
        if (!active) {
          return;
        }

        objectUrl = URL.createObjectURL(archivo);
        setPreviewUrl(objectUrl);
        setPreviewContentType(archivo.type);
        setPreviewState("success");
      })
      .catch((error: unknown) => {
        if (!active) {
          return;
        }

        setPreviewState("error");
        setPreviewError(messageForEvidenceDownload(error));
      });

    return () => {
      active = false;
      if (objectUrl) {
        URL.revokeObjectURL(objectUrl);
      }
    };
  }, [allowFileAccess, selectedEvidence, token]);

  useEffect(() => {
    if (!selectedEvidence) {
      lastScrolledEvidenceIdRef.current = null;
      return;
    }
    if (!autoScrollToPreview || lastScrolledEvidenceIdRef.current === selectedEvidence.idArchivoEvidencia) {
      return;
    }

    lastScrolledEvidenceIdRef.current = selectedEvidence.idArchivoEvidencia;
    const animationFrame = requestAnimationFrame(() => {
      previewPanelRef.current?.scrollIntoView({
        behavior: window.matchMedia("(prefers-reduced-motion: reduce)").matches ? "auto" : "smooth",
        block: "start",
      });
    });

    return () => cancelAnimationFrame(animationFrame);
  }, [autoScrollToPreview, selectedEvidence]);

  function descargarSeleccionada() {
    if (!selectedEvidence || !previewUrl) {
      return;
    }

    const enlace = document.createElement("a");
    enlace.href = previewUrl;
    enlace.download = selectedEvidence.nombreArchivo;
    document.body.appendChild(enlace);
    enlace.click();
    enlace.remove();
  }

  if (evidencias.length === 0) {
    return <p className="empty-copy">{emptyMessage}</p>;
  }

  const canPreviewImage = Boolean(
    selectedEvidence && previewUrl && isImage(selectedEvidence, previewContentType),
  );
  const canPreviewPdf = Boolean(
    selectedEvidence && previewUrl && isPdf(selectedEvidence, previewContentType),
  );
  const showFileList = evidencias.length > 1 || !allowFileAccess;

  return (
    <section
      className={`evidence-gallery ${showFileList ? "multiple" : "single"}`}
      aria-label="Visor de evidencias"
    >
      {showFileList ? (
        <ul className="evidence-gallery-list" aria-label="Archivos disponibles">
          {evidencias.map((evidencia) => {
            const selected = evidencia.idArchivoEvidencia === selectedEvidence?.idArchivoEvidencia;

            return (
              <li key={evidencia.idArchivoEvidencia}>
                <button
                  className={`evidence-gallery-item ${selected ? "selected" : ""}`}
                  type="button"
                  onClick={() => setSelectedEvidenceId(evidencia.idArchivoEvidencia)}
                  aria-pressed={selected}
                  disabled={!allowFileAccess}
                >
                  {evidencia.formatoArchivo.toLowerCase() === "pdf"
                    ? <FileText size={20} aria-hidden="true" />
                    : <FileImage size={20} aria-hidden="true" />}
                  <span>
                    <strong>{evidencia.nombreArchivo}</strong>
                    <small>
                      {evidencia.formatoArchivo.toUpperCase()} · {formatFileSize(evidencia.tamanoOriginalKb)}
                    </small>
                    <small>{formatDateTime(evidencia.fechaSubida)} · {evidencia.nombreUsuarioSubida}</small>
                  </span>
                  {allowFileAccess ? <Eye size={18} aria-hidden="true" /> : null}
                </button>
              </li>
            );
          })}
        </ul>
      ) : null}

      {allowFileAccess ? <div ref={previewPanelRef} className="evidence-preview-panel" aria-live="polite">
        <header className="evidence-preview-heading">
          <div>
            <span className="eyebrow">Vista previa</span>
            <strong>{selectedEvidence?.nombreArchivo}</strong>
          </div>
          <button
            className="ghost-button compact"
            type="button"
            onClick={descargarSeleccionada}
            disabled={previewState !== "success" || !previewUrl}
          >
            <Download size={17} aria-hidden="true" />
            Descargar
          </button>
        </header>

        <div className="evidence-preview-frame">
          {previewState === "loading" ? (
            <div className="evidence-preview-placeholder">
              <LoaderCircle className="spin" size={28} aria-hidden="true" />
              <span>Cargando evidencia...</span>
            </div>
          ) : null}

          {previewState === "error" ? (
            <div className="evidence-preview-placeholder error" role="alert">
              <AlertCircle size={28} aria-hidden="true" />
              <span>{previewError}</span>
            </div>
          ) : null}

          {previewState === "success" && canPreviewImage ? (
            <img src={previewUrl ?? undefined} alt={`Evidencia ${selectedEvidence?.nombreArchivo ?? ""}`} />
          ) : null}

          {previewState === "success" && canPreviewPdf ? (
            <iframe src={previewUrl ?? undefined} title={`Evidencia ${selectedEvidence?.nombreArchivo ?? ""}`} />
          ) : null}

          {previewState === "success" && !canPreviewImage && !canPreviewPdf ? (
            <div className="evidence-preview-placeholder">
              <FileText size={32} aria-hidden="true" />
              <span>Este formato no admite vista previa. Usa Descargar para abrirlo.</span>
            </div>
          ) : null}
        </div>
      </div> : (
        <div ref={previewPanelRef} className="evidence-preview-panel evidence-preview-restricted">
          <div className="evidence-preview-placeholder">
            <FileText size={32} aria-hidden="true" />
            <span>Tu rol puede consultar el estado del soporte, pero no abrir ni descargar el archivo.</span>
          </div>
        </div>
      )}
    </section>
  );
}
