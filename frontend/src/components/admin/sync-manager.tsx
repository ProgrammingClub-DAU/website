"use client";

import React, { useState, useEffect } from "react";
import {
  RefreshCw,
  CheckCircle2,
  AlertTriangle,
  Play,
  Loader2,
  Activity,
  XCircle,
} from "lucide-react";
import { syncService } from "@/lib/services/sync";
import type { SyncHealthDto, SyncJobType, SyncRunRecord } from "@/types/api";

const ALL_SYNC_JOBS: { job: SyncJobType; label: string; description: string }[] = [
  {
    job: "CF_SUBMISSIONS",
    label: "CF Submissions Sync",
    description: "Fetches new verified accepted solves from Codeforces every 6 hours",
  },
  {
    job: "CF_PROBLEMSET",
    label: "CF Problemset Sync",
    description: "Updates public Codeforces problem catalog, ratings, tags, and solved counts",
  },
  {
    job: "CF_CONTESTS",
    label: "CF Contest History",
    description: "Syncs official Codeforces contest participations, ranks, and rating changes",
  },
  {
    job: "LC_CONTESTS",
    label: "LeetCode Contests",
    description: "Syncs member contest rankings and ratings from LeetCode",
  },
  {
    job: "LC_TOTALS",
    label: "LeetCode Totals Snapshot",
    description: "Daily snapshot of Easy, Medium, and Hard solve totals",
  },
  {
    job: "EXTERNAL_CONTESTS",
    label: "External Contest Calendar",
    description: "Pulls upcoming contest schedules from CF, LC, CodeChef, and AtCoder",
  },
  {
    job: "CLUB_CONTEST",
    label: "Club Contests Standings",
    description: "Syncs live standings and upsolve matrix for official club contests",
  },
];

export function SyncManager() {
  const [health, setHealth] = useState<SyncHealthDto[]>([]);
  const [runs, setRuns] = useState<SyncRunRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [triggeringJob, setTriggeringJob] = useState<SyncJobType | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const loadData = async () => {
    try {
      const [healthData, runsData] = await Promise.all([
        syncService.getHealth(),
        syncService.getRuns(undefined, 15),
      ]);
      setHealth(healthData);
      setRuns(runsData);
    } catch (err) {
      console.error("Failed to load sync status:", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleTrigger = async (job: SyncJobType) => {
    setTriggeringJob(job);
    setMessage(null);
    try {
      const runId = await syncService.trigger(job);
      setMessage(`Triggered ${job} run #${runId}. It will process in the background.`);
      await loadData();
    } catch (err) {
      console.error("Failed to trigger job:", err);
      setMessage(`Failed to dispatch ${job}.`);
    } finally {
      setTriggeringJob(null);
    }
  };

  if (loading) {
    return (
      <div className="p-12 text-center font-mono text-xs text-fg-muted animate-pulse">
        Checking sync health...
      </div>
    );
  }

  // Create lookup for health by job
  const healthByJob = new Map<SyncJobType, SyncHealthDto>();
  health.forEach((h) => healthByJob.set(h.job, h));

  return (
    <div className="space-y-10">
      {/* Header card */}
      <div className="rounded-2xl border border-white/10 bg-[#0d0d16]/80 p-5 backdrop-blur-md flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h3 className="text-base font-bold text-white flex items-center gap-2">
            <Activity className="size-4 text-primary" />
            Background Synchronization Pipeline
          </h3>
          <p className="text-xs text-fg-muted mt-0.5">
            Monitor staleness, execution health, and dispatch ad-hoc sync runs.
          </p>
        </div>

        <button
          type="button"
          onClick={loadData}
          className="inline-flex items-center gap-1.5 rounded-xl border border-white/10 bg-surface-2 px-3.5 py-1.5 text-xs font-mono text-foreground hover:bg-surface-3 transition-colors shadow-xs"
        >
          <RefreshCw className="size-3.5" />
          Refresh Status
        </button>
      </div>

      {message && (
        <div className="rounded-xl bg-white/5 border border-white/10 p-3 text-xs font-mono text-white flex items-center gap-2">
          <CheckCircle2 className="size-4 text-emerald-400 shrink-0" />
          {message}
        </div>
      )}

      {/* Sync Jobs Table */}
      <div className="space-y-4">
        <h4 className="text-sm font-bold uppercase tracking-wider text-white">
          Active Background Jobs
        </h4>

        <div className="overflow-x-auto rounded-2xl border border-white/10 bg-[#0d0d16]/70 shadow-xl">
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="border-b border-white/10 bg-white/[0.02] text-nano font-mono uppercase tracking-caps text-fg-muted">
                <th className="py-3 px-4">Job</th>
                <th className="py-3 px-3">Status</th>
                <th className="py-3 px-3">Last Successful Run</th>
                <th className="py-3 px-3 text-center">OK / Fail</th>
                <th className="py-3 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/5">
              {ALL_SYNC_JOBS.map(({ job, label, description }) => {
                const h = healthByJob.get(job);
                const isTriggering = triggeringJob === job;
                const isStale = h?.stale ?? false;

                return (
                  <tr key={job} className="hover:bg-white/[0.02] transition-colors">
                    <td className="py-3 px-4">
                      <div className="font-semibold text-white">{label}</div>
                      <div className="text-[11px] text-fg-muted max-w-sm">{description}</div>
                    </td>

                    <td className="py-3 px-3">
                      {h ? (
                        isStale ? (
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-amber-500/15 border border-amber-500/30 text-amber-300 font-mono text-[10px] font-bold">
                            <AlertTriangle className="size-3" />
                            Stale
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-emerald-500/15 border border-emerald-500/30 text-emerald-300 font-mono text-[10px] font-bold">
                            <CheckCircle2 className="size-3" />
                            Healthy
                          </span>
                        )
                      ) : (
                        <span className="text-fg-subtle font-mono text-nano">Not Run Yet</span>
                      )}
                    </td>

                    <td className="py-3 px-3 font-mono text-fg-muted text-[11px]">
                      {h?.lastSuccessfulRun
                        ? new Date(h.lastSuccessfulRun).toLocaleString("en-IN", { timeZone: "Asia/Kolkata" })
                        : "Never"}
                    </td>

                    <td className="py-3 px-3 text-center font-mono tabular-nums text-xs">
                      {h ? (
                        <span>
                          <strong className="text-emerald-400">{h.membersOk}</strong> /{" "}
                          <strong className={h.membersFailed > 0 ? "text-rose-400" : "text-fg-muted"}>
                            {h.membersFailed}
                          </strong>
                        </span>
                      ) : (
                        "—"
                      )}
                    </td>

                    <td className="py-3 px-4 text-right">
                      <button
                        type="button"
                        onClick={() => handleTrigger(job)}
                        disabled={isTriggering}
                        className="inline-flex items-center gap-1.5 rounded-xl border border-primary/30 bg-primary/10 px-3 py-1.5 text-xs font-mono font-medium text-primary hover:bg-primary/20 transition-all shadow-xs disabled:opacity-50"
                      >
                        {isTriggering ? (
                          <Loader2 className="size-3.5 animate-spin" />
                        ) : (
                          <Play className="size-3.5" />
                        )}
                        {isTriggering ? "Dispatching..." : "Run Now"}
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      {/* Recent Runs History */}
      <div className="space-y-4">
        <h4 className="text-sm font-bold uppercase tracking-wider text-white">
          Recent Run Executions ({runs.length})
        </h4>

        <div className="overflow-x-auto rounded-2xl border border-white/10 bg-[#0d0d16]/70 shadow-xl">
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="border-b border-white/10 bg-white/[0.02] text-nano font-mono uppercase tracking-caps text-fg-muted">
                <th className="py-3 px-4">Run #</th>
                <th className="py-3 px-3">Job</th>
                <th className="py-3 px-3">Started</th>
                <th className="py-3 px-3">Status</th>
                <th className="py-3 px-3 text-center">OK / Fail</th>
                <th className="py-3 px-4">Error Sample</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/5 font-mono text-[11px]">
              {runs.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-8 text-center text-fg-muted">
                    No run history recorded.
                  </td>
                </tr>
              ) : (
                runs.map((r) => {
                  const hasFinished = r.finishedAt != null;
                  const hasFailed = r.membersFailed > 0 || r.errorSample != null;

                  return (
                    <tr key={r.id} className="hover:bg-white/[0.02] transition-colors">
                      <td className="py-3 px-4 text-fg-muted">#{r.id}</td>
                      <td className="py-3 px-3 font-semibold text-white">{r.job}</td>
                      <td className="py-3 px-3 text-fg-muted">
                        {new Date(r.startedAt).toLocaleString("en-IN", { timeZone: "Asia/Kolkata" })}
                      </td>
                      <td className="py-3 px-3">
                        {!hasFinished ? (
                          <span className="text-amber-400 flex items-center gap-1">
                            <Loader2 className="size-3 animate-spin" />
                            Running
                          </span>
                        ) : hasFailed ? (
                          <span className="text-rose-400 flex items-center gap-1">
                            <XCircle className="size-3" />
                            Failed
                          </span>
                        ) : (
                          <span className="text-emerald-400 flex items-center gap-1">
                            <CheckCircle2 className="size-3" />
                            Completed
                          </span>
                        )}
                      </td>
                      <td className="py-3 px-3 text-center tabular-nums">
                        <span className="text-emerald-400">{r.membersOk}</span> /{" "}
                        <span className={r.membersFailed > 0 ? "text-rose-400" : "text-fg-subtle"}>
                          {r.membersFailed}
                        </span>
                      </td>
                      <td className="py-3 px-4 text-fg-subtle max-w-xs truncate">
                        {r.errorSample || "None"}
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
