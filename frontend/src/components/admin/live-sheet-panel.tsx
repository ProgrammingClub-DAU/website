"use client";

/**
 * The event's tab in the club's live attendance sheet.
 *
 * The server rewrites the tab a few seconds after every change to the
 * attendance list, on its own. This panel only reports on that: a link to the
 * tab, when it last updated, and -- when Google refused -- what to fix, in the
 * server's words. "Sync now" is for events recorded before the sheet was set up,
 * and for retrying straight after fixing a sharing problem.
 *
 * Renders nothing when the live sheet is not set up on the server, so the page
 * looks exactly as it did before for a deployment that does not use it.
 */

import { useCallback, useEffect, useState } from "react";
import { AlertTriangle, CheckCircle2, ExternalLink, Loader2, RefreshCw, Sheet } from "lucide-react";

import { eventsService } from "@/lib/services/events";
import type { LiveSheetStatus } from "@/types/api";

/** The server waits ~4s after a change before writing; check back after that. */
const RECHECK_AFTER_CHANGE_MS = 7_000;

/** "just now", "3 min ago", "2 h ago" -- enough to tell a working sheet from a stale one. */
function ago(iso: string): string {
  const seconds = Math.max(0, Math.round((Date.now() - new Date(iso).getTime()) / 1000));
  if (seconds < 45) return "just now";
  const minutes = Math.round(seconds / 60);
  if (minutes < 60) return `${minutes} min ago`;
  const hours = Math.round(minutes / 60);
  if (hours < 24) return `${hours} h ago`;
  return new Date(iso).toLocaleString("en-IN", { day: "numeric", month: "short", hour: "2-digit", minute: "2-digit" });
}

export function LiveSheetPanel({
  eventId,
  attendeeCount,
}: {
  eventId: number;
  /** Changes whenever the list does, which is when the sheet is about to update. */
  attendeeCount: number;
}) {
  const [status, setStatus] = useState<LiveSheetStatus | null>(null);
  const [syncing, setSyncing] = useState(false);
  const [requestError, setRequestError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    try {
      setStatus(await eventsService.getLiveSheetStatus(eventId));
      setRequestError(null);
    } catch {
      // Not worth an alarm on page load: attendance itself is unaffected.
      setRequestError("Could not load the live sheet status.");
    }
  }, [eventId]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- data fetch on mount, not derived state
    refresh();
  }, [refresh]);

  // After the list changes, look again once the server has had time to write.
  useEffect(() => {
    const timer = setTimeout(refresh, RECHECK_AFTER_CHANGE_MS);
    return () => clearTimeout(timer);
  }, [attendeeCount, refresh]);

  const handleSyncNow = async () => {
    setSyncing(true);
    try {
      setStatus(await eventsService.syncLiveSheet(eventId));
      setRequestError(null);
    } catch {
      setRequestError("The sync request failed. Try again in a moment.");
    } finally {
      setSyncing(false);
    }
  };

  if (!status) return null;

  // Configured but unusable: only admins see this page, and they are the people
  // who can pass the message on to whoever runs the server.
  if (!status.enabled) {
    if (!status.setupProblem) return null;
    return (
      <div className="flex items-start gap-2 rounded-panel border border-amber-500/30 bg-amber-500/10 p-3 text-xs text-amber-400">
        <AlertTriangle className="mt-0.5 size-4 shrink-0" />
        <span>
          <span className="font-semibold">Live Google Sheet is not working.</span> {status.setupProblem}
        </span>
      </div>
    );
  }

  const error = status.lastError ?? requestError;

  return (
    <div className="flex flex-col gap-3 rounded-panel border border-border bg-surface p-4 sm:flex-row sm:items-center sm:justify-between">
      <div className="flex min-w-0 items-start gap-3">
        <Sheet className="mt-0.5 size-4 shrink-0 text-emerald-500" />
        <div className="min-w-0 text-xs">
          <p className="font-semibold text-foreground">Live attendance sheet</p>
          {error ? (
            <p className="mt-1 flex items-start gap-1.5 text-red-400">
              <AlertTriangle className="mt-px size-3.5 shrink-0" />
              <span>{error}</span>
            </p>
          ) : status.lastSyncedAt ? (
            <p className="mt-1 flex items-center gap-1.5 text-fg-muted">
              <CheckCircle2 className="size-3.5 text-emerald-500" />
              Updated {ago(status.lastSyncedAt)}. Updates on its own when attendance changes.
            </p>
          ) : (
            <p className="mt-1 text-fg-muted">
              Updates on its own when attendance changes. Use Sync now to fill it in for
              attendance taken before.
            </p>
          )}
        </div>
      </div>

      <div className="flex shrink-0 flex-wrap items-center gap-2">
        {status.sheetUrl && (
          <a
            href={status.sheetUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center gap-1.5 rounded-control border border-border bg-surface-2 px-3 py-1.5 text-xs font-semibold text-foreground hover:border-primary hover:text-primary transition-colors"
          >
            Open sheet <ExternalLink className="size-3" />
          </a>
        )}
        <button
          onClick={handleSyncNow}
          disabled={syncing}
          className="inline-flex items-center gap-1.5 rounded-control border border-border bg-surface-2 px-3 py-1.5 text-xs font-semibold text-foreground hover:border-primary hover:text-primary disabled:opacity-50 transition-colors"
        >
          {syncing ? <Loader2 className="size-3.5 animate-spin" /> : <RefreshCw className="size-3.5" />}
          Sync now
        </button>
      </div>
    </div>
  );
}
