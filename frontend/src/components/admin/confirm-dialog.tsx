"use client";

/**
 * A confirmation for destructive admin actions.
 *
 * Replaces window.confirm for deletes, for two reasons. A browser confirm box
 * cannot say clearly what will be lost, and it is answered by reflex -- Enter
 * dismisses it. For the deletes that take real records with them, this dialog
 * can also require the admin to type the item's name, which is the one kind of
 * confirmation that cannot be clicked through by accident.
 *
 * Native <dialog>: focus is trapped and restored, Escape cancels, and the page
 * behind is inert, without any code here.
 */

import { useEffect, useRef, useState } from "react";
import { AlertTriangle, Loader2 } from "lucide-react";

export interface ConfirmDialogProps {
  open: boolean;
  title: string;
  /** What will happen, in plain words. */
  description: React.ReactNode;
  /** When set, the confirm button stays disabled until this is typed exactly. */
  confirmText?: string;
  confirmLabel?: string;
  busy?: boolean;
  /** Shown inside the dialog, e.g. a refusal from the server. */
  error?: string | null;
  onConfirm: () => void;
  onCancel: () => void;
}

export function ConfirmDialog({
  open,
  title,
  description,
  confirmText,
  confirmLabel = "Delete",
  busy = false,
  error,
  onConfirm,
  onCancel,
}: ConfirmDialogProps) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const [typed, setTyped] = useState("");

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;
    if (open && !dialog.open) dialog.showModal();
    if (!open && dialog.open) dialog.close();
  }, [open]);

  const matches = !confirmText || typed.trim() === confirmText.trim();

  return (
    <dialog
      ref={dialogRef}
      onClose={() => {
        setTyped("");
        onCancel();
      }}
      // Escape while a request is in flight would close the dialog on a delete
      // that is still happening; hold it open until the answer arrives.
      onCancel={(event) => {
        if (busy) event.preventDefault();
      }}
      className="m-auto w-[min(92vw,460px)] rounded-panel border border-border bg-surface p-0 text-foreground shadow-2xl backdrop:bg-black/70 backdrop:backdrop-blur-sm"
    >
      <form
        method="dialog"
        onSubmit={(event) => {
          event.preventDefault();
          if (matches && !busy) onConfirm();
        }}
        className="space-y-4 p-5 sm:p-6"
      >
        <div className="flex items-start gap-3">
          <span className="mt-0.5 rounded-full bg-red-500/10 p-2 text-red-400">
            <AlertTriangle className="size-4" />
          </span>
          <div className="min-w-0 space-y-1.5">
            <h2 className="text-base font-semibold">{title}</h2>
            <div className="text-sm leading-relaxed text-fg-muted">{description}</div>
          </div>
        </div>

        {confirmText && (
          <div>
            <label htmlFor="confirm-dialog-text" className="mb-1 block text-xs text-fg-muted">
              Type <span className="font-mono font-semibold text-foreground">{confirmText}</span> to confirm
            </label>
            <input
              id="confirm-dialog-text"
              autoComplete="off"
              autoFocus
              value={typed}
              onChange={(e) => setTyped(e.target.value)}
              className="w-full rounded-control border border-border bg-surface-2 px-3 py-2 text-sm focus:border-red-400 focus:outline-none"
            />
          </div>
        )}

        {error && (
          <p role="alert" className="rounded-control border border-red-500/30 bg-red-500/10 p-2.5 text-xs text-red-400">
            {error}
          </p>
        )}

        <div className="flex justify-end gap-2 border-t border-border pt-4">
          <button
            type="button"
            onClick={() => dialogRef.current?.close()}
            disabled={busy}
            className="rounded-control border border-border px-4 py-2 text-sm text-fg-muted hover:text-foreground disabled:opacity-50"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={!matches || busy}
            className="inline-flex items-center gap-2 rounded-control bg-red-600 px-4 py-2 text-sm font-semibold text-white hover:bg-red-500 disabled:opacity-40"
          >
            {busy && <Loader2 className="size-4 animate-spin" />}
            {confirmLabel}
          </button>
        </div>
      </form>
    </dialog>
  );
}
