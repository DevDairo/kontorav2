import { AlertTriangle, X } from "lucide-react";
import { useEffect } from "react";

type ConfirmationDialogProps = {
  confirmLabel: string;
  description: string;
  isConfirming?: boolean;
  onCancel: () => void;
  onConfirm: () => void;
  open: boolean;
  title: string;
};

export function ConfirmationDialog({
  confirmLabel,
  description,
  isConfirming = false,
  onCancel,
  onConfirm,
  open,
  title,
}: ConfirmationDialogProps) {
  useEffect(() => {
    if (!open) {
      return;
    }

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape" && !isConfirming) {
        onCancel();
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [isConfirming, onCancel, open]);

  if (!open) {
    return null;
  }

  return (
    <div className="confirmation-backdrop">
      <section className="confirmation-dialog" aria-describedby="confirmation-description" aria-labelledby="confirmation-title" aria-modal="true" role="alertdialog">
        <div className="confirmation-dialog-heading">
          <AlertTriangle size={22} strokeWidth={2.2} />
          <div>
            <h2 id="confirmation-title">{title}</h2>
            <p id="confirmation-description">{description}</p>
          </div>
          <button aria-label="Cancelar confirmacion" className="icon-only-button" disabled={isConfirming} onClick={onCancel} title="Cancelar" type="button">
            <X size={18} strokeWidth={2.2} />
          </button>
        </div>
        <div className="confirmation-dialog-actions">
          <button className="ghost-button" disabled={isConfirming} onClick={onCancel} type="button">
            Cancelar
          </button>
          <button className="primary-button" disabled={isConfirming} onClick={onConfirm} type="button">
            {isConfirming ? "Procesando" : confirmLabel}
          </button>
        </div>
      </section>
    </div>
  );
}
