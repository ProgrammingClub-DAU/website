"use client";

// The CP Practice Group tab of the admin dashboard: who is in the group, how each
// student is doing over the last 7 and 30 days, and adding students by batch.
// Admin-only end to end: the API refuses everyone else.

import { useEffect, useMemo, useState, type ComponentType, type ReactNode } from "react";
import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  Activity,
  AlertTriangle,
  ArrowDown,
  ArrowUp,
  ChevronRight,
  Loader2,
  Lock,
  NotebookPen,
  Plus,
  Search,
  Target,
  Users,
  Zap,
} from "lucide-react";
import apiClient from "@/lib/axios";
import { apiErrorMessage } from "@/lib/api-error";
import { cpGroupService } from "@/lib/services/cp-group";
import { daysSinceIst, initials, lastActiveText, needsNudge, signed } from "@/lib/cp-group";
import { cn } from "@/lib/utils";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetFooter,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet";
import type { ApiResponse } from "@/store/auth";
import type { CpGroupMemberRow } from "@/types/api";

type SortKey =
  | "name"
  | "batch"
  | "cf7"
  | "cf30"
  | "cfAll"
  | "lc7"
  | "lc30"
  | "lcAll"
  | "rating"
  | "contests"
  | "lastActive";
type StatusFilter = "all" | "active" | "inactive";

/** The value a column sorts by; missing values sort last either way. */
function sortValue(row: CpGroupMemberRow, key: SortKey): number | string | null {
  switch (key) {
    case "name":
      return row.name.toLowerCase();
    case "batch":
      return row.batchYear;
    case "cf7":
      return row.codeforcesHandle ? row.cfSolved7d : null;
    case "cf30":
      return row.codeforcesHandle ? row.cfSolved30d : null;
    case "cfAll":
      return row.codeforcesHandle ? row.cfSolvedAll : null;
    case "lc7":
      return row.lcSolved7d;
    case "lc30":
      return row.lcSolved30d;
    case "lcAll":
      return row.lcSolvedAll;
    case "rating":
      return row.cfRating;
    case "contests":
      return row.contests30d;
    case "lastActive": {
      const days = daysSinceIst(row.lastActiveOn);
      return days == null ? null : -days;
    }
  }
}

export function CpGroupTab() {
  const router = useRouter();
  const [rows, setRows] = useState<CpGroupMemberRow[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);
  const [batch, setBatch] = useState<number | "all">("all");
  const [status, setStatus] = useState<StatusFilter>("all");
  const [query, setQuery] = useState("");
  const [sort, setSort] = useState<{ key: SortKey; desc: boolean }>({ key: "cf7", desc: true });
  const [adding, setAdding] = useState(false);

  useEffect(() => {
    let ignore = false;
    cpGroupService
      .list()
      .then((list) => {
        if (!ignore) {
          setRows(list);
          setError(null);
        }
      })
      .catch((err) => {
        if (!ignore) setError(apiErrorMessage(err, "Could not load the CP Practice Group."));
      });
    return () => {
      ignore = true;
    };
  }, [reloadKey]);

  const batches = useMemo(() => {
    const counts = new Map<number, number>();
    (rows ?? []).forEach((r) => counts.set(r.batchYear, (counts.get(r.batchYear) ?? 0) + 1));
    return [...counts.entries()].sort((a, b) => a[0] - b[0]);
  }, [rows]);

  // The overview follows the batch picked, so "Batch 2029" reads as that batch.
  const inBatch = useMemo(
    () => (rows ?? []).filter((r) => batch === "all" || r.batchYear === batch),
    [rows, batch]
  );

  const shown = useMemo(() => {
    const q = query.trim().toLowerCase();
    const filtered = inBatch.filter((r) => {
      const idle = needsNudge(daysSinceIst(r.lastActiveOn));
      if (status === "active" && idle) return false;
      if (status === "inactive" && !idle) return false;
      if (!q) return true;
      return [r.name, r.email, r.codeforcesHandle, r.leetcodeHandle].some((v) => v?.toLowerCase().includes(q));
    });
    return filtered.sort((a, b) => {
      const va = sortValue(a, sort.key);
      const vb = sortValue(b, sort.key);
      if (va == null && vb == null) return a.name.localeCompare(b.name);
      if (va == null) return 1;
      if (vb == null) return -1;
      const order = va < vb ? -1 : va > vb ? 1 : a.name.localeCompare(b.name);
      return sort.desc ? -order : order;
    });
  }, [inBatch, status, query, sort]);

  const overview = useMemo(() => {
    const cfWeek = inBatch.reduce((sum, r) => sum + r.cfSolved7d, 0);
    const lcWeek = inBatch.reduce((sum, r) => sum + (r.lcSolved7d ?? 0), 0);
    const idle = inBatch.filter((r) => needsNudge(daysSinceIst(r.lastActiveOn))).length;
    return { cfWeek, lcWeek, idle, active: inBatch.length - idle };
  }, [inBatch]);

  const toggleSort = (key: SortKey) =>
    setSort((s) => (s.key === key ? { key, desc: !s.desc } : { key, desc: key !== "name" && key !== "batch" }));

  if (error) {
    return (
      <div role="alert" className="rounded-2xl border border-rose-500/30 bg-rose-500/10 p-6 text-sm text-rose-700 dark:text-rose-200">
        {error}
      </div>
    );
  }

  if (!rows) {
    return (
      <div className="flex items-center justify-center gap-2 py-20 text-sm text-fg-muted">
        <Loader2 className="size-4 animate-spin" aria-hidden />
        Loading the CP Practice Group…
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h2 className="flex items-center gap-2 text-xl font-bold tracking-tight text-foreground">
            <Target className="size-5 text-primary" aria-hidden />
            CP Practice Group
          </h2>
          <p className="mt-1 flex items-center gap-1.5 text-xs text-fg-muted">
            <Lock className="size-3" aria-hidden />
            Private: only admins can see this group. Students see just a badge on their own profile.
          </p>
        </div>
        <button
          type="button"
          onClick={() => setAdding(true)}
          className="inline-flex items-center justify-center gap-2 rounded-control bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground shadow-sm transition-colors hover:bg-primary-hover"
        >
          <Plus className="size-4" aria-hidden />
          Add students
        </button>
      </div>

      {rows.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-border bg-card/60 px-6 py-16 text-center">
          <Users className="mx-auto size-10 text-fg-subtle" aria-hidden />
          <p className="mt-3 text-base font-semibold text-foreground">No students in the group yet</p>
          <p className="mx-auto mt-1 max-w-md text-sm text-fg-muted">
            Add the students who are serious about CP, batch by batch. You&apos;ll see their solves for the
            last week, the last month and overall, on Codeforces and LeetCode.
          </p>
          <button
            type="button"
            onClick={() => setAdding(true)}
            className="mt-5 inline-flex items-center gap-2 rounded-control bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground"
          >
            <Plus className="size-4" aria-hidden />
            Add students
          </button>
        </div>
      ) : (
        <>
          {/* Batch picker */}
          <div className="flex flex-wrap items-center gap-2">
            <BatchPill active={batch === "all"} onClick={() => setBatch("all")} label="All batches" count={rows.length} />
            {batches.map(([year, count]) => (
              <BatchPill key={year} active={batch === year} onClick={() => setBatch(year)} label={`Batch ${year}`} count={count} />
            ))}
          </div>

          {/* Overview */}
          <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
            <StatCard
              icon={Users}
              label={batch === "all" ? "Students" : `Batch ${batch}`}
              value={inBatch.length}
              hint={batch === "all" ? `${batches.length} batch${batches.length === 1 ? "" : "es"}` : "in this batch"}
            />
            <StatCard
              icon={Zap}
              label="Solved this week"
              value={overview.cfWeek + overview.lcWeek}
              hint={`CF ${overview.cfWeek} · LC ${overview.lcWeek}`}
            />
            <StatCard
              icon={Activity}
              label="Active this week"
              value={`${overview.active}/${inBatch.length}`}
              hint={
                <span className="mt-1.5 block h-1.5 overflow-hidden rounded-full bg-foreground/10">
                  <span
                    className="block h-full rounded-full bg-emerald-500"
                    style={{ width: `${inBatch.length ? (overview.active / inBatch.length) * 100 : 0}%` }}
                  />
                </span>
              }
            />
            <button
              type="button"
              onClick={() => setStatus(status === "inactive" ? "all" : "inactive")}
              className={cn(
                "rounded-2xl border p-4 text-left transition-colors",
                overview.idle > 0
                  ? "border-amber-500/40 bg-amber-500/10 hover:bg-amber-500/15"
                  : "border-border bg-card/70 hover:bg-surface-2"
              )}
            >
              <p className="flex items-center gap-1.5 text-xs font-medium text-fg-muted">
                <AlertTriangle className={cn("size-3.5", overview.idle > 0 && "text-amber-600 dark:text-amber-400")} aria-hidden />
                Need a nudge
              </p>
              <p className="mt-1 font-mono text-2xl font-black tabular-nums text-foreground">{overview.idle}</p>
              <p className="mt-0.5 text-[11px] text-fg-muted">
                {status === "inactive" ? "Showing them · click to show all" : "No activity in 7+ days · click to show"}
              </p>
            </button>
          </div>

          {/* Filters */}
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div className="flex rounded-control border border-border bg-surface-2 p-1 text-xs font-medium">
              {(["all", "active", "inactive"] as const).map((s) => (
                <button
                  key={s}
                  type="button"
                  onClick={() => setStatus(s)}
                  className={cn(
                    "rounded-[6px] px-3 py-1.5 transition-colors",
                    status === s ? "bg-card text-foreground shadow-xs" : "text-fg-muted hover:text-foreground"
                  )}
                >
                  {s === "inactive" ? "Need a nudge" : s === "active" ? "Active" : "Everyone"}
                </button>
              ))}
            </div>
            <label className="relative block w-full sm:max-w-xs">
              <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-fg-subtle" aria-hidden />
              <span className="sr-only">Search students</span>
              <input
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="Search name, email or handle"
                className="w-full rounded-control border border-border bg-card py-2 pr-3 pl-9 text-sm text-foreground placeholder:text-fg-subtle focus:border-primary focus:outline-none"
              />
            </label>
          </div>

          {/* Phones: a card per student. The table needs a wide screen. */}
          <ul className="space-y-3 md:hidden">
            {shown.length === 0 ? (
              <li className="rounded-2xl border border-border bg-card/70 px-4 py-10 text-center text-sm text-fg-muted">
                No students match these filters.
              </li>
            ) : (
              shown.map((r) => <StudentCard key={r.userId} row={r} />)
            )}
          </ul>

          {/* Table */}
          <div className="hidden overflow-x-auto rounded-2xl border border-border bg-card/70 md:block">
            <table className="w-full min-w-[980px] border-collapse text-sm">
              <thead>
                <tr className="border-b border-border text-[11px] tracking-caps text-fg-muted uppercase">
                  <th rowSpan={2} className="px-4 py-2 text-left font-medium">
                    <SortButton k="name" sort={sort} onSort={toggleSort}>Student</SortButton>
                  </th>
                  <th rowSpan={2} className="px-2 py-2 text-left font-medium">
                    <SortButton k="batch" sort={sort} onSort={toggleSort}>Batch</SortButton>
                  </th>
                  <th colSpan={3} className="border-l border-border px-2 pt-2 text-center font-semibold text-primary">
                    Codeforces solved
                  </th>
                  <th colSpan={3} className="border-l border-border px-2 pt-2 text-center font-semibold text-amber-700 dark:text-amber-400">
                    LeetCode solved
                  </th>
                  <th rowSpan={2} className="border-l border-border px-3 py-2 text-left font-medium">
                    <SortButton k="rating" sort={sort} onSort={toggleSort}>Rating · 30d</SortButton>
                  </th>
                  <th rowSpan={2} className="px-2 py-2 text-right font-medium">
                    <SortButton k="contests" sort={sort} onSort={toggleSort}>Contests 30d</SortButton>
                  </th>
                  <th rowSpan={2} className="px-3 py-2 text-left font-medium">
                    <SortButton k="lastActive" sort={sort} onSort={toggleSort}>Last active</SortButton>
                  </th>
                  <th rowSpan={2} aria-label="Open" />
                </tr>
                <tr className="border-b border-border text-[11px] text-fg-muted">
                  {(["cf7", "cf30", "cfAll", "lc7", "lc30", "lcAll"] as const).map((k, i) => (
                    <th key={k} className={cn("px-2 pb-2 text-right font-medium", i % 3 === 0 && "border-l border-border")}>
                      <SortButton k={k} sort={sort} onSort={toggleSort} align="right">
                        {k.endsWith("7") ? "7d" : k.endsWith("30") ? "30d" : "All"}
                      </SortButton>
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {shown.length === 0 ? (
                  <tr>
                    <td colSpan={13} className="px-4 py-12 text-center text-sm text-fg-muted">
                      No students match these filters.
                    </td>
                  </tr>
                ) : (
                  shown.map((r) => {
                    const days = daysSinceIst(r.lastActiveOn);
                    const idle = needsNudge(days);
                    const href = `/admin/cp-practice/${r.userId}`;
                    return (
                      <tr
                        key={r.userId}
                        onClick={() => router.push(href)}
                        className="cursor-pointer border-b border-hairline transition-colors last:border-0 hover:bg-surface-2/70"
                      >
                        <td className="px-4 py-3">
                          <div className="flex items-center gap-3">
                            <Avatar name={r.name} url={r.avatarUrl} />
                            <div className="min-w-0">
                              <Link
                                href={href}
                                onClick={(e) => e.stopPropagation()}
                                className="flex items-center gap-1.5 font-semibold text-foreground hover:text-primary"
                              >
                                <span className="truncate">{r.name}</span>
                                {r.hasNotes && <NotebookPen className="size-3 shrink-0 text-fg-subtle" aria-label="Has notes" />}
                              </Link>
                              <p className="truncate font-mono text-[11px] text-fg-subtle">
                                {r.codeforcesHandle ? `CF @${r.codeforcesHandle}` : "No CF"}
                                {" · "}
                                {r.leetcodeHandle ? `LC @${r.leetcodeHandle}` : "No LC"}
                              </p>
                            </div>
                          </div>
                        </td>
                        <td className="px-2 py-3">
                          <span className="rounded-full border border-primary/25 bg-primary/10 px-2 py-0.5 font-mono text-[11px] font-semibold text-primary">
                            {r.batchYear}
                          </span>
                        </td>
                        <Num value={r.codeforcesHandle ? r.cfSolved7d : null} strong border />
                        <Num value={r.codeforcesHandle ? r.cfSolved30d : null} />
                        <Num value={r.codeforcesHandle ? r.cfSolvedAll : null} muted />
                        <Num value={r.lcSolved7d} strong border />
                        <Num value={r.lcSolved30d} />
                        <Num value={r.lcSolvedAll} muted />
                        <td className="border-l border-hairline px-3 py-3 font-mono text-xs">
                          <RatingLine label="CF" rating={r.cfRating} change={r.cfRatingChange30d} />
                          <RatingLine label="LC" rating={r.lcRating} change={r.lcRatingChange30d} />
                        </td>
                        <td className="px-2 py-3 text-right font-mono tabular-nums text-foreground">{r.contests30d}</td>
                        <td className="px-3 py-3">
                          <span
                            className={cn(
                              "inline-flex items-center gap-1.5 rounded-full px-2 py-0.5 text-xs font-medium whitespace-nowrap",
                              idle
                                ? "bg-amber-500/15 text-amber-700 dark:text-amber-400"
                                : "bg-emerald-500/15 text-emerald-700 dark:text-emerald-400"
                            )}
                          >
                            <span className={cn("size-1.5 rounded-full", idle ? "bg-amber-500" : "bg-emerald-500")} />
                            {lastActiveText(days)}
                          </span>
                        </td>
                        <td className="pr-3 text-fg-subtle">
                          <ChevronRight className="size-4" aria-hidden />
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
          <p className="text-[11px] text-fg-subtle">
            Codeforces counts distinct problems from regular contests (no gym). LeetCode counts the rise in
            each student&apos;s total since that day; &quot;—&quot; means no handle, or tracking doesn&apos;t
            reach back that far yet. Click a student for their full page.
          </p>
        </>
      )}

      <AddStudentsSheet
        open={adding}
        onOpenChange={setAdding}
        members={rows}
        batches={batches.map(([year]) => year)}
        onDone={() => setReloadKey((k) => k + 1)}
      />
    </div>
  );
}

/** A student on a phone: the same numbers as a table row, stacked. */
function StudentCard({ row: r }: { row: CpGroupMemberRow }) {
  const days = daysSinceIst(r.lastActiveOn);
  const idle = needsNudge(days);
  const cell = (label: string, value: number | null) => (
    <span className="flex flex-col items-center">
      <span className="font-mono text-base font-bold tabular-nums text-foreground">{value ?? "—"}</span>
      <span className="text-[10px] text-fg-subtle">{label}</span>
    </span>
  );
  return (
    <li>
      <Link href={`/admin/cp-practice/${r.userId}`} className="block rounded-2xl border border-border bg-card/70 p-4 transition-colors hover:bg-surface-2/70">
        <div className="flex items-center gap-3">
          <Avatar name={r.name} url={r.avatarUrl} />
          <div className="min-w-0 flex-1">
            <p className="flex items-center gap-1.5 truncate font-semibold text-foreground">
              {r.name}
              {r.hasNotes && <NotebookPen className="size-3 shrink-0 text-fg-subtle" aria-label="Has notes" />}
            </p>
            <p className="text-[11px] text-fg-subtle">
              Batch {r.batchYear} · {r.contests30d} contest{r.contests30d === 1 ? "" : "s"} in 30d
            </p>
          </div>
          <span
            className={cn(
              "shrink-0 rounded-full px-2 py-0.5 text-[11px] font-medium",
              idle ? "bg-amber-500/15 text-amber-700 dark:text-amber-400" : "bg-emerald-500/15 text-emerald-700 dark:text-emerald-400"
            )}
          >
            {lastActiveText(days)}
          </span>
        </div>
        <div className="mt-3 grid grid-cols-2 gap-2">
          <div className="rounded-xl bg-primary/5 p-2">
            <p className="mb-1 text-center text-[10px] font-semibold tracking-caps text-primary uppercase">Codeforces</p>
            <div className="grid grid-cols-3">
              {cell("7d", r.codeforcesHandle ? r.cfSolved7d : null)}
              {cell("30d", r.codeforcesHandle ? r.cfSolved30d : null)}
              {cell("All", r.codeforcesHandle ? r.cfSolvedAll : null)}
            </div>
          </div>
          <div className="rounded-xl bg-amber-500/5 p-2">
            <p className="mb-1 text-center text-[10px] font-semibold tracking-caps text-amber-700 uppercase dark:text-amber-400">LeetCode</p>
            <div className="grid grid-cols-3">
              {cell("7d", r.lcSolved7d)}
              {cell("30d", r.lcSolved30d)}
              {cell("All", r.lcSolvedAll)}
            </div>
          </div>
        </div>
      </Link>
    </li>
  );
}

function BatchPill({ active, onClick, label, count }: { active: boolean; onClick: () => void; label: string; count: number }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={cn(
        "inline-flex items-center gap-2 rounded-full border px-3 py-1.5 text-xs font-semibold transition-colors",
        active
          ? "border-primary bg-primary text-primary-foreground"
          : "border-border bg-surface-2 text-fg-muted hover:text-foreground"
      )}
    >
      {label}
      <span className={cn("rounded-full px-1.5 font-mono text-[10px]", active ? "bg-primary-foreground/20" : "bg-foreground/10")}>
        {count}
      </span>
    </button>
  );
}

function StatCard({
  icon: Icon,
  label,
  value,
  hint,
}: {
  icon: ComponentType<{ className?: string }>;
  label: string;
  value: number | string;
  hint: ReactNode;
}) {
  return (
    <div className="rounded-2xl border border-border bg-card/70 p-4">
      <p className="flex items-center gap-1.5 text-xs font-medium text-fg-muted">
        <Icon className="size-3.5 text-primary" />
        {label}
      </p>
      <p className="mt-1 font-mono text-2xl font-black tabular-nums text-foreground">{value}</p>
      <div className="mt-0.5 text-[11px] text-fg-muted">{hint}</div>
    </div>
  );
}

function SortButton({
  k,
  sort,
  onSort,
  align,
  children,
}: {
  k: SortKey;
  sort: { key: SortKey; desc: boolean };
  onSort: (key: SortKey) => void;
  align?: "right";
  children: ReactNode;
}) {
  const active = sort.key === k;
  return (
    <button
      type="button"
      onClick={() => onSort(k)}
      className={cn(
        "inline-flex items-center gap-1 uppercase transition-colors hover:text-foreground",
        align === "right" && "flex-row-reverse",
        active && "text-foreground"
      )}
    >
      {children}
      {active && (sort.desc ? <ArrowDown className="size-3" aria-hidden /> : <ArrowUp className="size-3" aria-hidden />)}
    </button>
  );
}

function Num({ value, strong, muted, border }: { value: number | null; strong?: boolean; muted?: boolean; border?: boolean }) {
  return (
    <td
      className={cn(
        "px-2 py-3 text-right font-mono tabular-nums",
        border && "border-l border-hairline",
        strong ? "font-bold text-foreground" : muted ? "text-fg-subtle" : "text-fg-muted"
      )}
    >
      {value == null ? "—" : value}
    </td>
  );
}

function RatingLine({ label, rating, change }: { label: string; rating: number | null; change: number | null }) {
  if (rating == null) return null;
  return (
    <p className="whitespace-nowrap">
      <span className="text-fg-subtle">{label} </span>
      <span className="font-semibold text-foreground">{rating}</span>
      {change != null && change !== 0 && (
        <span className={cn("ml-1", change > 0 ? "text-emerald-600 dark:text-emerald-400" : "text-rose-600 dark:text-rose-400")}>
          {signed(change)}
        </span>
      )}
    </p>
  );
}

function Avatar({ name, url }: { name: string; url: string | null }) {
  return (
    <span className="flex size-9 shrink-0 items-center justify-center overflow-hidden rounded-full border border-border bg-surface-3 text-xs font-bold text-fg-muted">
      {url ? <Image src={url} alt="" width={36} height={36} className="size-full object-cover" /> : initials(name)}
    </span>
  );
}

interface SiteUser {
  id: number;
  name: string;
  email: string;
  avatarUrl: string | null;
  codeforcesHandle: string | null;
  batchYear: number | null;
}

/** The slide-over for adding students to a batch of the group. */
function AddStudentsSheet({
  open,
  onOpenChange,
  members,
  batches,
  onDone,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  members: CpGroupMemberRow[] | null;
  batches: number[];
  onDone: () => void;
}) {
  const [users, setUsers] = useState<SiteUser[] | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [selected, setSelected] = useState<Set<number>>(new Set());
  const [groupBatch, setGroupBatch] = useState("");
  const [profileBatch, setProfileBatch] = useState<string>("any");
  const [search, setSearch] = useState("");
  const [hideMembers, setHideMembers] = useState(true);
  const [saving, setSaving] = useState(false);
  const [result, setResult] = useState<string | null>(null);

  useEffect(() => {
    if (!open || users) return;
    let ignore = false;
    apiClient
      .get<ApiResponse<SiteUser[]>>("/api/users/all")
      .then((res) => {
        if (!ignore) setUsers(res.data.data);
      })
      .catch((err) => {
        if (!ignore) setLoadError(apiErrorMessage(err, "Could not load students."));
      });
    return () => {
      ignore = true;
    };
  }, [open, users]);

  const memberBatch = useMemo(() => new Map((members ?? []).map((m) => [m.userId, m.batchYear])), [members]);
  const profileBatches = useMemo(
    () => [...new Set((users ?? []).map((u) => u.batchYear).filter((y): y is number => y != null))].sort(),
    [users]
  );

  const visible = useMemo(() => {
    const q = search.trim().toLowerCase();
    return (users ?? [])
      .filter((u) => !(hideMembers && memberBatch.has(u.id)))
      .filter((u) => profileBatch === "any" || String(u.batchYear ?? "") === profileBatch)
      .filter((u) => !q || [u.name, u.email, u.codeforcesHandle].some((v) => v?.toLowerCase().includes(q)))
      .sort((a, b) => a.name.localeCompare(b.name));
  }, [users, search, hideMembers, memberBatch, profileBatch]);

  const year = Number(groupBatch);
  const validYear = Number.isInteger(year) && year >= 2000 && year <= 2100;
  const allVisibleSelected = visible.length > 0 && visible.every((u) => selected.has(u.id));

  const toggle = (id: number) =>
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });

  const toggleAllVisible = () =>
    setSelected((prev) => {
      const next = new Set(prev);
      visible.forEach((u) => (allVisibleSelected ? next.delete(u.id) : next.add(u.id)));
      return next;
    });

  const submit = async () => {
    if (!validYear || selected.size === 0) return;
    setSaving(true);
    setResult(null);
    try {
      const { added, moved } = await cpGroupService.add([...selected], year);
      setResult(
        `Added ${added} to batch ${year}` + (moved ? `, moved ${moved} already in the group.` : ".")
      );
      setSelected(new Set());
      onDone();
    } catch (err) {
      setResult(apiErrorMessage(err, "Could not add the students."));
    } finally {
      setSaving(false);
    }
  };

  return (
    <Sheet
      open={open}
      onOpenChange={(next) => {
        onOpenChange(next);
        if (!next) setResult(null);
      }}
    >
      <SheetContent side="right" className="w-full gap-0 p-0 sm:max-w-md">
        <SheetHeader className="border-b border-border p-5">
          <SheetTitle>Add students to the group</SheetTitle>
          <SheetDescription>
            Pick students, choose their group batch, and add them. A student already in the group moves to the
            batch you choose.
          </SheetDescription>
        </SheetHeader>

        <div className="space-y-4 border-b border-border p-5">
          <div>
            <label htmlFor="cp-batch" className="text-xs font-semibold text-foreground">
              Group batch
            </label>
            <div className="mt-1.5 flex flex-wrap items-center gap-2">
              <input
                id="cp-batch"
                inputMode="numeric"
                value={groupBatch}
                onChange={(e) => setGroupBatch(e.target.value.replace(/\D/g, "").slice(0, 4))}
                placeholder="e.g. 2029"
                className="w-28 rounded-control border border-border bg-card px-3 py-2 font-mono text-sm text-foreground focus:border-primary focus:outline-none"
              />
              {batches.map((b) => (
                <button
                  key={b}
                  type="button"
                  onClick={() => setGroupBatch(String(b))}
                  className={cn(
                    "rounded-full border px-2.5 py-1 font-mono text-xs",
                    groupBatch === String(b)
                      ? "border-primary bg-primary/10 text-primary"
                      : "border-border text-fg-muted hover:text-foreground"
                  )}
                >
                  {b}
                </button>
              ))}
            </div>
          </div>

          <div className="flex flex-wrap items-center gap-2">
            <label className="relative min-w-0 flex-1">
              <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-fg-subtle" aria-hidden />
              <span className="sr-only">Search students</span>
              <input
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="Search name, email, handle"
                className="w-full rounded-control border border-border bg-card py-2 pr-3 pl-9 text-sm text-foreground focus:border-primary focus:outline-none"
              />
            </label>
            <select
              value={profileBatch}
              onChange={(e) => setProfileBatch(e.target.value)}
              aria-label="Profile batch"
              className="rounded-control border border-border bg-card px-2 py-2 text-sm text-foreground"
            >
              <option value="any">Any profile batch</option>
              {profileBatches.map((y) => (
                <option key={y} value={String(y)}>
                  Profile batch {y}
                </option>
              ))}
            </select>
          </div>

          <div className="flex items-center justify-between text-xs text-fg-muted">
            <label className="inline-flex items-center gap-2">
              <input type="checkbox" checked={hideMembers} onChange={(e) => setHideMembers(e.target.checked)} />
              Hide students already in the group
            </label>
            <button type="button" onClick={toggleAllVisible} className="font-semibold text-primary hover:underline">
              {allVisibleSelected ? "Clear shown" : `Select all shown (${visible.length})`}
            </button>
          </div>
        </div>

        <ul className="min-h-0 flex-1 overflow-y-auto">
          {loadError ? (
            <li className="p-5 text-sm text-rose-600">{loadError}</li>
          ) : !users ? (
            <li className="flex items-center gap-2 p-5 text-sm text-fg-muted">
              <Loader2 className="size-4 animate-spin" aria-hidden /> Loading students…
            </li>
          ) : visible.length === 0 ? (
            <li className="p-5 text-sm text-fg-muted">No students match.</li>
          ) : (
            visible.map((u) => {
              const inGroup = memberBatch.get(u.id);
              return (
                <li key={u.id}>
                  <label className="flex cursor-pointer items-center gap-3 border-b border-hairline px-5 py-2.5 hover:bg-surface-2/70">
                    <input type="checkbox" checked={selected.has(u.id)} onChange={() => toggle(u.id)} />
                    <Avatar name={u.name} url={u.avatarUrl} />
                    <span className="min-w-0 flex-1">
                      <span className="block truncate text-sm font-semibold text-foreground">{u.name}</span>
                      <span className="block truncate text-[11px] text-fg-subtle">
                        {u.email}
                        {u.codeforcesHandle && ` · CF @${u.codeforcesHandle}`}
                      </span>
                    </span>
                    <span className="flex shrink-0 flex-col items-end gap-0.5 font-mono text-[10px] text-fg-subtle">
                      {u.batchYear && <span>Profile {u.batchYear}</span>}
                      {inGroup && <span className="font-semibold text-primary">In group · {inGroup}</span>}
                    </span>
                  </label>
                </li>
              );
            })
          )}
        </ul>

        <SheetFooter className="border-t border-border p-5">
          {result && <p className="text-xs text-fg-muted">{result}</p>}
          <button
            type="button"
            disabled={!validYear || selected.size === 0 || saving}
            onClick={submit}
            className="inline-flex w-full items-center justify-center gap-2 rounded-control bg-primary px-4 py-2.5 text-sm font-semibold text-primary-foreground transition-opacity disabled:opacity-50"
          >
            {saving && <Loader2 className="size-4 animate-spin" aria-hidden />}
            {selected.size === 0
              ? "Select students to add"
              : !validYear
                ? "Enter a group batch"
                : `Add ${selected.size} student${selected.size === 1 ? "" : "s"} to batch ${year}`}
          </button>
        </SheetFooter>
      </SheetContent>
    </Sheet>
  );
}
