"use client";

// One CP Practice Group student, for admins: their activity, ratings, topics,
// recent work and the admins' private notes. The API refuses everyone else.

import { useEffect, useMemo, useState, type ComponentType, type ReactNode } from "react";
import Image from "next/image";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import {
  Activity,
  ArrowLeft,
  CalendarDays,
  Code2,
  ExternalLink,
  Loader2,
  NotebookPen,
  Save,
  ShieldAlert,
  Trash2,
  Trophy,
  TrendingUp,
} from "lucide-react";
import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { useAuthStore } from "@/store/auth";
import { cpGroupService } from "@/lib/services/cp-group";
import { statsService } from "@/lib/services/stats";
import { apiErrorMessage } from "@/lib/api-error";
import { TopicStrengthChart } from "@/components/site/topic-strength-chart";
import { CF_COLOR, ChartLegend, ChartTip, DailySolvesChart, LC_COLOR } from "@/components/site/daily-solves-chart";
import { RecentSolvesList } from "@/components/site/recent-solves-list";
import { daysSinceIst, initials, lastActiveText, needsNudge, signed } from "@/lib/cp-group";
import { timeAgo } from "@/lib/practice";
import { cn, serverDate } from "@/lib/utils";
import type { CpGroupMemberDetail, TopicStrengthResponse } from "@/types/api";

const BACK_HREF = "/admin?tab=cpGroup";

export default function CpPracticeStudentPage() {
  const { user, isAuthenticated } = useAuthStore();
  const isAdmin = isAuthenticated && user?.role === "ROLE_ADMIN";
  const params = useParams<{ id: string }>();
  const userId = Number(params.id);
  const router = useRouter();

  const [detail, setDetail] = useState<CpGroupMemberDetail | null>(null);
  const [topics, setTopics] = useState<TopicStrengthResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    if (!isAdmin || !Number.isFinite(userId)) return;
    let ignore = false;
    cpGroupService
      .detail(userId)
      .then((d) => {
        if (!ignore) setDetail(d);
      })
      .catch((err) => {
        if (!ignore) setError(apiErrorMessage(err, "Could not load this student."));
      });
    statsService
      .getTopics(userId)
      .then((t) => {
        if (!ignore) setTopics(t);
      })
      .catch(() => undefined);
    return () => {
      ignore = true;
    };
  }, [isAdmin, userId, reloadKey]);

  if (!isAdmin) {
    return (
      <div className="flex min-h-[400px] flex-col items-center justify-center gap-3 text-center">
        <ShieldAlert className="size-10 text-red-500" aria-hidden />
        <h2 className="text-lg font-bold">Admins only</h2>
        <p className="max-w-sm text-xs text-fg-muted">This page is only for club administrators.</p>
        <Link href={isAuthenticated ? "/" : "/login"} className="mt-2 rounded-control bg-primary px-4 py-2 text-xs font-semibold text-primary-foreground">
          {isAuthenticated ? "Go home" : "Sign in"}
        </Link>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-7xl space-y-6 px-4 pt-6 pb-16 sm:px-6">
      <Link href={BACK_HREF} className="inline-flex items-center gap-1.5 text-sm text-fg-muted hover:text-foreground">
        <ArrowLeft className="size-4" aria-hidden />
        CP Practice Group
      </Link>

      {error ? (
        <div role="alert" className="rounded-2xl border border-rose-500/30 bg-rose-500/10 p-6 text-sm text-rose-700 dark:text-rose-200">
          {error}
        </div>
      ) : !detail ? (
        <div className="flex items-center justify-center gap-2 py-20 text-sm text-fg-muted">
          <Loader2 className="size-4 animate-spin" aria-hidden />
          Loading student…
        </div>
      ) : (
        <StudentView
          detail={detail}
          topics={topics}
          onChanged={() => setReloadKey((k) => k + 1)}
          onRemoved={() => router.push(BACK_HREF)}
        />
      )}
    </div>
  );
}

function StudentView({
  detail,
  topics,
  onChanged,
  onRemoved,
}: {
  detail: CpGroupMemberDetail;
  topics: TopicStrengthResponse | null;
  onChanged: () => void;
  onRemoved: () => void;
}) {
  const s = detail.summary;
  const days = daysSinceIst(s.lastActiveOn);
  const idle = needsNudge(days);

  const [batch, setBatch] = useState(String(s.batchYear));
  const [savingBatch, setSavingBatch] = useState(false);
  const [removing, setRemoving] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);

  const batchYear = Number(batch);
  const batchChanged = batchYear !== s.batchYear && Number.isInteger(batchYear) && batchYear >= 2000 && batchYear <= 2100;

  const saveBatch = async () => {
    setSavingBatch(true);
    setActionError(null);
    try {
      await cpGroupService.update(s.userId, { batchYear });
      onChanged();
    } catch (err) {
      setActionError(apiErrorMessage(err, "Could not change the batch."));
    } finally {
      setSavingBatch(false);
    }
  };

  const remove = async () => {
    if (!window.confirm(`Remove ${s.name} from the CP Practice Group? Their notes are deleted too.`)) return;
    setRemoving(true);
    setActionError(null);
    try {
      await cpGroupService.remove(s.userId);
      onRemoved();
    } catch (err) {
      setActionError(apiErrorMessage(err, "Could not remove the student."));
      setRemoving(false);
    }
  };

  const ratingPoints = useMemo(
    () =>
      detail.contests
        .filter((c) => c.rated && c.newRating != null)
        .map((c) => ({
          t: serverDate(c.startedAt).getTime(),
          name: c.contestName,
          cf: c.platform === "CODEFORCES" ? c.newRating : undefined,
          lc: c.platform === "LEETCODE" ? c.newRating : undefined,
        }))
        .sort((a, b) => a.t - b.t),
    [detail.contests]
  );

  return (
    <div className="space-y-6">
      {/* Header */}
      <section className="relative overflow-hidden rounded-3xl border border-border bg-card/80 p-6">
        <div aria-hidden className="pointer-events-none absolute -top-24 -right-24 size-72 rounded-full bg-primary/10 blur-3xl" />
        <div className="relative flex flex-col gap-6 lg:flex-row lg:items-center lg:justify-between">
          <div className="flex items-center gap-4">
            <span className="flex size-16 shrink-0 items-center justify-center overflow-hidden rounded-full border-2 border-primary/30 bg-surface-3 text-lg font-bold text-fg-muted">
              {s.avatarUrl ? (
                <Image src={s.avatarUrl} alt="" width={64} height={64} className="size-full object-cover" />
              ) : (
                initials(s.name)
              )}
            </span>
            <div className="min-w-0">
              <h1 className="truncate text-2xl font-bold tracking-tight text-foreground">{s.name}</h1>
              <p className="truncate text-xs text-fg-muted">{s.email}</p>
              <div className="mt-2 flex flex-wrap items-center gap-2 text-xs">
                {s.codeforcesHandle && (
                  <ExternalChip href={`https://codeforces.com/profile/${s.codeforcesHandle}`} label={`CF @${s.codeforcesHandle}`} />
                )}
                {s.leetcodeHandle && (
                  <ExternalChip href={`https://leetcode.com/u/${s.leetcodeHandle}`} label={`LC @${s.leetcodeHandle}`} />
                )}
                <Link href={`/profile/${s.userId}`} className="text-primary hover:underline">
                  Site profile
                </Link>
              </div>
              <p className="mt-2 text-[11px] text-fg-subtle">
                In the group since {serverDate(s.addedAt).toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "numeric" })}
                {detail.addedBy && ` · added by ${detail.addedBy}`}
                {s.profileBatchYear && ` · profile batch ${s.profileBatchYear}`}
              </p>
            </div>
          </div>

          <div className="flex flex-wrap items-end gap-3">
            <div>
              <label htmlFor="group-batch" className="block text-[11px] font-semibold text-fg-muted">
                Group batch
              </label>
              <div className="mt-1 flex items-center gap-2">
                <input
                  id="group-batch"
                  inputMode="numeric"
                  value={batch}
                  onChange={(e) => setBatch(e.target.value.replace(/\D/g, "").slice(0, 4))}
                  className="w-24 rounded-control border border-border bg-card px-3 py-2 font-mono text-sm text-foreground focus:border-primary focus:outline-none"
                />
                <button
                  type="button"
                  disabled={!batchChanged || savingBatch}
                  onClick={saveBatch}
                  className="inline-flex items-center gap-1.5 rounded-control bg-primary px-3 py-2 text-xs font-semibold text-primary-foreground disabled:opacity-40"
                >
                  {savingBatch ? <Loader2 className="size-3.5 animate-spin" aria-hidden /> : <Save className="size-3.5" aria-hidden />}
                  Save
                </button>
              </div>
            </div>
            <button
              type="button"
              onClick={remove}
              disabled={removing}
              className="inline-flex items-center gap-1.5 rounded-control border border-rose-500/40 px-3 py-2 text-xs font-semibold text-rose-600 transition-colors hover:bg-rose-500/10 disabled:opacity-50 dark:text-rose-400"
            >
              <Trash2 className="size-3.5" aria-hidden />
              Remove from group
            </button>
          </div>
        </div>
        {actionError && <p className="relative mt-3 text-xs text-rose-600 dark:text-rose-400">{actionError}</p>}
      </section>

      {/* Numbers */}
      <section className="grid grid-cols-2 gap-3 md:grid-cols-3 xl:grid-cols-6">
        <SolvedTile
          label="Codeforces solved"
          color="text-primary"
          handle={s.codeforcesHandle}
          week={s.cfSolved7d}
          month={s.cfSolved30d}
          all={s.cfSolvedAll}
        />
        <SolvedTile
          label="LeetCode solved"
          color="text-amber-700 dark:text-amber-400"
          handle={s.leetcodeHandle}
          week={s.lcSolved7d}
          month={s.lcSolved30d}
          all={s.lcSolvedAll}
        />
        <Tile icon={TrendingUp} label="CF rating" value={s.cfRating ?? "—"} change={s.cfRatingChange30d} />
        <Tile icon={TrendingUp} label="LC rating" value={s.lcRating ?? "—"} change={s.lcRatingChange30d} />
        <Tile icon={Trophy} label="Contests (30 days)" value={s.contests30d} />
        <Tile
          icon={Activity}
          label="Last active"
          value={lastActiveText(days)}
          tone={idle ? "warn" : "good"}
          small
        />
      </section>

      {/* Activity */}
      <section className="rounded-2xl border border-border bg-card/80 p-5">
        <div className="flex flex-wrap items-end justify-between gap-2">
          <h2 className="flex items-center gap-2 text-sm font-bold uppercase tracking-wider text-foreground">
            <CalendarDays className="size-4 text-primary" aria-hidden />
            Problems solved, last 30 days
          </h2>
          <ChartLegend />
        </div>
        <div className="mt-4">
          <DailySolvesChart daily={detail.daily} />
        </div>
      </section>

      <div className="grid gap-6 lg:grid-cols-2">
        {/* Rating history */}
        <section className="rounded-2xl border border-border bg-card/80 p-5">
          <div className="flex flex-wrap items-end justify-between gap-2">
            <h2 className="flex items-center gap-2 text-sm font-bold uppercase tracking-wider text-foreground">
              <TrendingUp className="size-4 text-primary" aria-hidden />
              Rating history
            </h2>
            <ChartLegend />
          </div>
          {ratingPoints.length === 0 ? (
            <p className="mt-6 text-center text-sm text-fg-muted">No rated contests yet.</p>
          ) : (
            <div className="mt-4 h-56 w-full">
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={ratingPoints} margin={{ top: 4, right: 8, left: -16, bottom: 0 }}>
                  <CartesianGrid vertical={false} stroke="var(--hairline)" />
                  <XAxis
                    dataKey="t"
                    type="number"
                    scale="time"
                    domain={["dataMin", "dataMax"]}
                    tickFormatter={(t: number) => new Date(t).toLocaleDateString("en-IN", { month: "short", year: "2-digit" })}
                    tick={{ fill: "var(--fg-subtle)", fontSize: 10 }}
                    tickLine={false}
                    axisLine={false}
                  />
                  <YAxis domain={["auto", "auto"]} tick={{ fill: "var(--fg-subtle)", fontSize: 10 }} tickLine={false} axisLine={false} />
                  <Tooltip
                    content={({ active, payload }) => {
                      if (!active || !payload?.length) return null;
                      const p = payload[0].payload as (typeof ratingPoints)[number];
                      return (
                        <ChartTip title={p.name}>
                          <p>{new Date(p.t).toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "numeric" })}</p>
                          <p>
                            {p.cf != null ? "Codeforces" : "LeetCode"}: <strong>{p.cf ?? p.lc}</strong>
                          </p>
                        </ChartTip>
                      );
                    }}
                  />
                  <Line dataKey="cf" stroke={CF_COLOR} strokeWidth={2} dot={{ r: 2 }} connectNulls />
                  <Line dataKey="lc" stroke={LC_COLOR} strokeWidth={2} dot={{ r: 2 }} connectNulls />
                </LineChart>
              </ResponsiveContainer>
            </div>
          )}
        </section>

        <NotesCard detail={detail} onSaved={onChanged} />
      </div>

      {topics && topics.reason == null && <TopicStrengthChart data={topics} viewer="admin" />}

      <div className="grid gap-6 lg:grid-cols-2">
        <section className="rounded-2xl border border-border bg-card/80 p-5">
          <h2 className="flex items-center gap-2 text-sm font-bold uppercase tracking-wider text-foreground">
            <Code2 className="size-4 text-primary" aria-hidden />
            Recent Codeforces solves
          </h2>
          <div className="mt-3">
            <RecentSolvesList solves={detail.recentSolves} />
          </div>
        </section>

        <section className="rounded-2xl border border-border bg-card/80 p-5">
          <h2 className="flex items-center gap-2 text-sm font-bold uppercase tracking-wider text-foreground">
            <Trophy className="size-4 text-primary" aria-hidden />
            Recent contests
          </h2>
          {detail.contests.length === 0 ? (
            <p className="mt-4 text-sm text-fg-muted">No contests on record yet.</p>
          ) : (
            <ul className="mt-3 divide-y divide-hairline">
              {detail.contests.slice(0, 12).map((c) => {
                const change = c.rated && c.oldRating != null && c.newRating != null ? c.newRating - c.oldRating : null;
                return (
                  <li key={`${c.platform}-${c.contestName}-${c.startedAt}`} className="flex items-center justify-between gap-3 py-2.5">
                    <div className="min-w-0">
                      <p className="truncate text-sm font-medium text-foreground">{c.contestName}</p>
                      <p className="font-mono text-[11px] text-fg-subtle">
                        <span className={c.platform === "LEETCODE" ? "text-amber-700 dark:text-amber-400" : "text-primary"}>
                          {c.platform === "LEETCODE" ? "LC" : "CF"}
                        </span>
                        {" · "}
                        {serverDate(c.startedAt).toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "numeric" })}
                        {c.rank != null && ` · rank ${c.rank}`}
                        {!c.rated && " · unrated"}
                      </p>
                    </div>
                    {change != null && (
                      <span
                        className={cn(
                          "shrink-0 font-mono text-sm font-bold",
                          change > 0 ? "text-emerald-600 dark:text-emerald-400" : change < 0 ? "text-rose-600 dark:text-rose-400" : "text-fg-muted"
                        )}
                      >
                        {signed(change)}
                      </span>
                    )}
                  </li>
                );
              })}
            </ul>
          )}
        </section>
      </div>
    </div>
  );
}

function NotesCard({ detail, onSaved }: { detail: CpGroupMemberDetail; onSaved: () => void }) {
  const [notes, setNotes] = useState(detail.notes ?? "");
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const changed = notes.trim() !== (detail.notes ?? "");

  const save = async () => {
    setSaving(true);
    setMessage(null);
    try {
      await cpGroupService.update(detail.summary.userId, { notes });
      setMessage("Saved.");
      onSaved();
    } catch (err) {
      setMessage(apiErrorMessage(err, "Could not save the notes."));
    } finally {
      setSaving(false);
    }
  };

  return (
    <section className="flex flex-col rounded-2xl border border-border bg-card/80 p-5">
      <h2 className="flex items-center gap-2 text-sm font-bold uppercase tracking-wider text-foreground">
        <NotebookPen className="size-4 text-primary" aria-hidden />
        Admin notes
      </h2>
      <p className="mt-1 text-[11px] text-fg-subtle">Private to admins. The student never sees these.</p>
      <textarea
        value={notes}
        onChange={(e) => setNotes(e.target.value.slice(0, 5000))}
        rows={7}
        placeholder="e.g. Strong in greedy, weak in DP. Talked on 12 Nov; aiming for Specialist by March."
        className="mt-3 w-full flex-1 resize-y rounded-xl border border-border bg-card p-3 text-sm text-foreground placeholder:text-fg-subtle focus:border-primary focus:outline-none"
      />
      <div className="mt-3 flex flex-wrap items-center justify-between gap-2">
        <p className="text-[11px] text-fg-subtle">
          {message ??
            (detail.notesUpdatedAt
              ? `Last edited ${timeAgo(serverDate(detail.notesUpdatedAt))}${detail.notesUpdatedBy ? ` by ${detail.notesUpdatedBy}` : ""}`
              : "No notes yet.")}
        </p>
        <button
          type="button"
          disabled={!changed || saving}
          onClick={save}
          className="inline-flex items-center gap-1.5 rounded-control bg-primary px-3 py-2 text-xs font-semibold text-primary-foreground disabled:opacity-40"
        >
          {saving ? <Loader2 className="size-3.5 animate-spin" aria-hidden /> : <Save className="size-3.5" aria-hidden />}
          Save notes
        </button>
      </div>
    </section>
  );
}

function SolvedTile({
  label,
  color,
  handle,
  week,
  month,
  all,
}: {
  label: string;
  color: string;
  handle: string | null;
  week: number | null;
  month: number | null;
  all: number | null;
}) {
  const show = (v: number | null) => (handle == null || v == null ? "—" : v);
  return (
    <div className="rounded-2xl border border-border bg-card/80 p-4">
      <p className={cn("text-xs font-semibold", color)}>{label}</p>
      <p className="mt-1 font-mono text-2xl font-black tabular-nums text-foreground">
        {show(week)}
        <span className="ml-1 text-xs font-medium text-fg-muted">this week</span>
      </p>
      <p className="mt-1 font-mono text-[11px] text-fg-muted">
        30d <strong className="text-foreground">{show(month)}</strong> · all <strong className="text-foreground">{show(all)}</strong>
      </p>
    </div>
  );
}

function Tile({
  icon: Icon,
  label,
  value,
  change,
  tone,
  small,
}: {
  icon: ComponentType<{ className?: string }>;
  label: string;
  value: ReactNode;
  change?: number | null;
  tone?: "good" | "warn";
  small?: boolean;
}) {
  return (
    <div
      className={cn(
        "rounded-2xl border p-4",
        tone === "warn" ? "border-amber-500/40 bg-amber-500/10" : tone === "good" ? "border-emerald-500/30 bg-emerald-500/5" : "border-border bg-card/80"
      )}
    >
      <p className="flex items-center gap-1.5 text-xs font-medium text-fg-muted">
        <Icon className="size-3.5" />
        {label}
      </p>
      <p className={cn("mt-1 font-mono font-black tabular-nums text-foreground", small ? "text-lg" : "text-2xl")}>{value}</p>
      {change != null && (
        <p className={cn("mt-0.5 font-mono text-[11px]", change > 0 ? "text-emerald-600 dark:text-emerald-400" : change < 0 ? "text-rose-600 dark:text-rose-400" : "text-fg-muted")}>
          {signed(change)} in 30 days
        </p>
      )}
    </div>
  );
}

function ExternalChip({ href, label }: { href: string; label: string }) {
  return (
    <a
      href={href}
      target="_blank"
      rel="noreferrer"
      className="inline-flex items-center gap-1 rounded-full border border-border bg-surface-2 px-2.5 py-0.5 font-mono text-[11px] text-fg-muted hover:text-foreground"
    >
      {label}
      <ExternalLink className="size-3" aria-hidden />
    </a>
  );
}
