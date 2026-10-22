"use client";

import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import axios from "axios";
import {
  RefreshCw,
  CheckCircle2,
  AlertTriangle,
  Play,
  Loader2,
  Activity,
  XCircle,
  CircleDashed,
  CircleSlash,
} from "lucide-react";
import { syncService } from "@/lib/services/sync";
import { cn, formatServerTimestamp, parseServerTimestamp, timeAgo } from "@/lib/utils";
import type { SyncHealthDto, SyncHealthStatus, SyncJobType, SyncRunRecord } from "@/types/api";

const ALL_SYNC_JOBS: { job: SyncJobType; label: string; description: string }[] = [
  {
    job: "CF_RATINGS",
    label: "CF Ratings",
    description: "Current and peak Codeforces rating. Drives the main leaderboard. Every 6 hours.",
  },
  {
    job: "CF_SUBMISSIONS",
    label: "CF Submissions",
    description: "New accepted solves from Codeforces. Every 6 hours.",
  },
  {
    job: "CF_CONTESTS",
    label: "CF Contest History",
    description: "Codeforces contest participations, ranks and rating changes. Every 6 hours.",
  },
  {
    job: "LC_RATINGS",
    label: "LeetCode Ratings",
    description: "LeetCode contest rating and solved totals. Every 6 hours.",
  },
  {
    job: "LC_CONTESTS",
    label: "LeetCode Contests",
    description: "LeetCode contest rankings and ratings. Every 6 hours.",
  },
  {
    job: "LC_TOTALS",
    label: "LeetCode Totals Snapshot",
    description: "Daily Easy / Medium / Hard baseline for weekly and monthly boards.",
  },
  {
    job: "CF_PROBLEMSET",
    label: "CF Problemset",
    description: "Problem catalogue, ratings, tags and solve counts. Weekly, Sunday 03:00 IST.",
  },
  {
    job: "EXTERNAL_CONTESTS",
    label: "Contest Calendar",
    description: "Upcoming contests from Codeforces, LeetCode, CodeChef and AtCoder. Counts are platforms.",
  },
  {
    job: "CLUB_CONTEST",
    label: "Club Contest Standings",
    description: "Live standings and upsolve matrix for club contests, while one is running.",
  },
];

const JOB_LABEL = new Map(ALL_SYNC_JOBS.map((j) => [j.job, j.label]));

const STATUS_STYLE: Record<SyncHealthStatus, { label: string; className: string; icon: React.ReactNode }> = {
  RUNNING: {
    label: "Running",
    className: "bg-sky-500/15 border-sky-500/30 text-sky-300",
    icon: <Loader2 className="size-3 animate-spin" />,
  },
  HEALTHY: {
    label: "Healthy",
    className: "bg-emerald-500/15 border-emerald-500/30 text-emerald-300",
    icon: <CheckCircle2 className="size-3" />,
  },
  DEGRADED: {
    label: "Partial",
    className: "bg-amber-500/15 border-amber-500/30 text-amber-300",
    icon: <AlertTriangle className="size-3" />,
  },
  FAILING: {
    label: "Failing",
    className: "bg-rose-500/15 border-rose-500/30 text-rose-300",
    icon: <XCircle className="size-3" />,
  },
  STALE: {
    label: "Stale",
    className: "bg-orange-500/15 border-orange-500/30 text-orange-300",
    icon: <AlertTriangle className="size-3" />,
  },
  NEVER_RUN: {
    label: "Not run yet",
    className: "bg-white/5 border-white/10 text-fg-muted",
    icon: <CircleDashed className="size-3" />,
  },
};

/** Polling interval while any job runs, so the panel updates without a manual refresh. */
const POLL_MS = 5000;

function StatusBadge({ status }: { status: SyncHealthStatus }) {
  const style = STATUS_STYLE[status] ?? STATUS_STYLE.NEVER_RUN;
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1 whitespace-nowrap rounded-full border px-2 py-0.5 font-mono text-[10px] font-bold",
        style.className
      )}
    >
      {style.icon}
      {style.label}
    </span>
  );
}

function runStatus(r: SyncRunRecord): SyncHealthStatus | "INTERRUPTED" {
  if (r.finishedAt == null) {
    const started = parseServerTimestamp(r.startedAt);
    // Same rule as the backend: an unfinished run older than two hours was killed by a restart.
    return started && Date.now() - started.getTime() > 2 * 60 * 60 * 1000 ? "INTERRUPTED" : "RUNNING";
  }
  const failed = r.membersFailed > 0 || r.errorSample != null;
  if (!failed) return "HEALTHY";
  return r.membersOk > 0 ? "DEGRADED" : "FAILING";
}

function errorMessage(err: unknown, fallback: string): string {
  if (axios.isAxiosError(err)) {
    const message = (err.response?.data as { message?: string } | undefined)?.message;
    if (message) return message;
  }
  return fallback;
}

export function SyncManager() {
  const [health, setHealth] = useState<SyncHealthDto[]>([]);
  const [runs, setRuns] = useState<SyncRunRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [refreshing, setRefreshing] = useState(false);
  const [triggeringJob, setTriggeringJob] = useState<SyncJobType | null>(null);
  const [message, setMessage] = useState<{ tone: "ok" | "error"; text: string } | null>(null);
  const [expandedRun, setExpandedRun] = useState<number | null>(null);
  const mounted = useRef(true);

  const loadData = useCallback(async () => {
    setRefreshing(true);
    try {
      const [healthData, runsData] = await Promise.all([
        syncService.getHealth(),
        syncService.getRuns(undefined, 25),
      ]);
      if (!mounted.current) return;
      setHealth(healthData);
      setRuns(runsData);
      setLoadError(null);
    } catch (err) {
      if (mounted.current) setLoadError(errorMessage(err, "Could not load sync status."));
    } finally {
      if (mounted.current) {
        setLoading(false);
        setRefreshing(false);
      }
    }
  }, []);

  useEffect(() => {
    mounted.current = true;
    loadData();
    return () => {
      mounted.current = false;
    };
  }, [loadData]);

  const healthByJob = useMemo(() => {
    const map = new Map<SyncJobType, SyncHealthDto>();
    health.forEach((h) => map.set(h.job, h));
    return map;
  }, [health]);

  const anyRunning = health.some((h) => h.running);

  // Poll while a job is running so status, counts and errors appear as it finishes.
  useEffect(() => {
    if (!anyRunning) return;
    const id = window.setInterval(loadData, POLL_MS);
    return () => window.clearInterval(id);
  }, [anyRunning, loadData]);

  const handleTrigger = async (job: SyncJobType) => {
    setTriggeringJob(job);
    setMessage(null);
    try {
      await syncService.trigger(job);
      setMessage({ tone: "ok", text: `${JOB_LABEL.get(job) ?? job} queued. This panel updates as it runs.` });
      await loadData();
    } catch (err) {
      setMessage({ tone: "error", text: errorMessage(err, `Could not start ${JOB_LABEL.get(job) ?? job}.`) });
    } finally {
      setTriggeringJob(null);
    }
  };

  const summary = useMemo(() => {
    const counts = { problems: 0, running: 0 };
    ALL_SYNC_JOBS.forEach(({ job }) => {
      const status = healthByJob.get(job)?.status;
      if (status === "FAILING" || status === "STALE" || status === "DEGRADED") counts.problems++;
      if (status === "RUNNING") counts.running++;
    });
    return counts;
  }, [healthByJob]);

  if (loading) {
    return (
      <div className="p-12 text-center font-mono text-xs text-fg-muted animate-pulse">
        Checking sync health...
      </div>
    );
  }

  return (
    <div className="space-y-8 md:space-y-10">
      {/* Header card */}
      <div className="rounded-2xl border border-white/10 bg-[#0d0d16]/80 p-4 sm:p-5 backdrop-blur-md flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="min-w-0">
          <h3 className="text-base font-bold text-white flex items-center gap-2">
            <Activity className="size-4 text-primary" />
            Background Synchronization
          </h3>
          <p className="text-xs text-fg-muted mt-0.5">
            {summary.running > 0
              ? `${summary.running} job(s) running — this panel refreshes every few seconds.`
              : summary.problems > 0
              ? `${summary.problems} job(s) need attention. Expand a failed run below to see why.`
              : "All jobs healthy. Times are IST."}
          </p>
        </div>

        <button
          type="button"
          onClick={loadData}
          disabled={refreshing}
          className="inline-flex items-center justify-center gap-1.5 rounded-xl border border-white/10 bg-surface-2 px-3.5 py-2 text-xs font-mono text-foreground hover:bg-surface-3 transition-colors shadow-xs disabled:opacity-60"
        >
          <RefreshCw className={cn("size-3.5", refreshing && "animate-spin")} />
          Refresh
        </button>
      </div>

      {loadError && (
        <div role="alert" className="rounded-xl border border-rose-500/30 bg-rose-500/10 p-3 text-xs font-mono text-rose-200 flex items-center gap-2">
          <XCircle className="size-4 shrink-0" />
          {loadError}
        </div>
      )}

      {message && (
        <div
          role="status"
          className={cn(
            "rounded-xl border p-3 text-xs font-mono flex items-center gap-2",
            message.tone === "ok"
              ? "bg-white/5 border-white/10 text-white"
              : "bg-rose-500/10 border-rose-500/30 text-rose-200"
          )}
        >
          {message.tone === "ok" ? (
            <CheckCircle2 className="size-4 text-emerald-400 shrink-0" />
          ) : (
            <XCircle className="size-4 shrink-0" />
          )}
          {message.text}
        </div>
      )}

      {/* Jobs */}
      <div className="space-y-4">
        <h4 className="text-sm font-bold uppercase tracking-wider text-white">Jobs</h4>

        <div className="overflow-hidden rounded-2xl border border-white/10 bg-[#0d0d16]/70 shadow-xl">
          {/* Column header, desktop only; rows become cards on small screens. */}
          <div className="hidden md:grid grid-cols-[minmax(0,2.2fr)_110px_minmax(0,1.3fr)_90px_120px] gap-3 border-b border-white/10 bg-white/[0.02] px-4 py-3 text-nano font-mono uppercase tracking-caps text-fg-muted">
            <span>Job</span>
            <span>Status</span>
            <span>Last run</span>
            <span className="text-center">OK / Fail</span>
            <span className="text-right">Action</span>
          </div>

          <ul className="divide-y divide-white/5">
            {ALL_SYNC_JOBS.map(({ job, label, description }) => {
              const h = healthByJob.get(job);
              const status: SyncHealthStatus = h?.status ?? "NEVER_RUN";
              const isRunning = status === "RUNNING";
              const isTriggering = triggeringJob === job;

              return (
                <li
                  key={job}
                  className="grid grid-cols-1 md:grid-cols-[minmax(0,2.2fr)_110px_minmax(0,1.3fr)_90px_120px] gap-2 md:gap-3 px-4 py-3.5 hover:bg-white/[0.02] transition-colors md:items-center"
                >
                  <div className="min-w-0">
                    <div className="flex items-center justify-between gap-2 md:block">
                      <span className="font-semibold text-sm text-white">{label}</span>
                      <span className="md:hidden">
                        <StatusBadge status={status} />
                      </span>
                    </div>
                    <p className="text-[11px] text-fg-muted">{description}</p>
                    {h?.errorSample && status !== "HEALTHY" && (
                      <p className="mt-1 text-[11px] font-mono text-rose-300/90 break-words line-clamp-2" title={h.errorSample}>
                        {h.errorSample}
                      </p>
                    )}
                  </div>

                  <div className="hidden md:block">
                    <StatusBadge status={status} />
                  </div>

                  <div className="font-mono text-[11px] text-fg-muted">
                    {h?.startedAt ? (
                      <>
                        <span className="text-white/80">{timeAgo(h.startedAt)}</span>
                        <span className="block text-fg-subtle">
                          {formatServerTimestamp(h.startedAt)}
                        </span>
                        {h.lastSuccessfulRun && h.lastSuccessfulRun !== h.startedAt && (
                          <span className="block text-fg-subtle">
                            Last good: {timeAgo(h.lastSuccessfulRun)}
                          </span>
                        )}
                      </>
                    ) : (
                      "Never"
                    )}
                  </div>

                  <div className="font-mono tabular-nums text-xs md:text-center">
                    {h && status !== "NEVER_RUN" ? (
                      <span>
                        <span className="md:hidden text-fg-muted">OK / Fail: </span>
                        <strong className="text-emerald-400">{h.membersOk}</strong> /{" "}
                        <strong className={h.membersFailed > 0 ? "text-rose-400" : "text-fg-muted"}>
                          {h.membersFailed}
                        </strong>
                      </span>
                    ) : (
                      <span className="hidden md:inline">—</span>
                    )}
                  </div>

                  <div className="md:text-right">
                    <button
                      type="button"
                      onClick={() => handleTrigger(job)}
                      disabled={isTriggering || isRunning}
                      aria-label={`Run ${label} now`}
                      className="inline-flex w-full md:w-auto items-center justify-center gap-1.5 rounded-xl border border-primary/30 bg-primary/10 px-3 py-2 md:py-1.5 text-xs font-mono font-medium text-primary hover:bg-primary/20 transition-all shadow-xs disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      {isTriggering || isRunning ? (
                        <Loader2 className="size-3.5 animate-spin" />
                      ) : (
                        <Play className="size-3.5" />
                      )}
                      {isTriggering ? "Queuing..." : isRunning ? "Running" : "Run now"}
                    </button>
                  </div>
                </li>
              );
            })}
          </ul>
        </div>
      </div>

      {/* Recent runs */}
      <div className="space-y-4">
        <h4 className="text-sm font-bold uppercase tracking-wider text-white">
          Recent runs ({runs.length})
        </h4>

        <div className="overflow-hidden rounded-2xl border border-white/10 bg-[#0d0d16]/70 shadow-xl">
          {runs.length === 0 ? (
            <p className="py-8 text-center text-xs font-mono text-fg-muted">No run history recorded.</p>
          ) : (
            <ul className="divide-y divide-white/5 font-mono text-[11px]">
              {runs.map((r) => {
                const status = runStatus(r);
                const expanded = expandedRun === r.id;
                return (
                  <li key={r.id}>
                    <button
                      type="button"
                      onClick={() => setExpandedRun(expanded ? null : r.id)}
                      disabled={!r.errorSample}
                      aria-expanded={r.errorSample ? expanded : undefined}
                      className="grid w-full grid-cols-[auto_minmax(0,1fr)_auto] sm:grid-cols-[56px_minmax(0,1.2fr)_minmax(0,1.3fr)_100px_80px] items-center gap-x-3 gap-y-1 px-4 py-3 text-left hover:bg-white/[0.02] disabled:cursor-default"
                    >
                      <span className="text-fg-muted">#{r.id}</span>
                      <span className="font-semibold text-white truncate">{JOB_LABEL.get(r.job) ?? r.job}</span>
                      <span className="sm:hidden justify-self-end">
                        {status === "INTERRUPTED" ? (
                          <span className="inline-flex items-center gap-1 text-fg-muted">
                            <CircleSlash className="size-3" /> Interrupted
                          </span>
                        ) : (
                          <StatusBadge status={status} />
                        )}
                      </span>
                      <span className="col-span-3 sm:col-span-1 text-fg-muted">
                        {formatServerTimestamp(r.startedAt)}
                      </span>
                      <span className="hidden sm:block">
                        {status === "INTERRUPTED" ? (
                          <span className="inline-flex items-center gap-1 text-fg-muted">
                            <CircleSlash className="size-3" /> Interrupted
                          </span>
                        ) : (
                          <StatusBadge status={status} />
                        )}
                      </span>
                      <span className="col-span-3 sm:col-span-1 tabular-nums sm:text-center">
                        <span className="text-emerald-400">{r.membersOk}</span> /{" "}
                        <span className={r.membersFailed > 0 ? "text-rose-400" : "text-fg-subtle"}>
                          {r.membersFailed}
                        </span>
                      </span>
                      {r.errorSample && (
                        <span
                          className={cn(
                            "col-span-3 sm:col-span-5 text-rose-300/80",
                            expanded ? "whitespace-pre-wrap break-words" : "truncate"
                          )}
                        >
                          {r.errorSample}
                        </span>
                      )}
                    </button>
                  </li>
                );
              })}
            </ul>
          )}
        </div>
      </div>
    </div>
  );
}
