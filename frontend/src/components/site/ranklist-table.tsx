"use client";

import React, { useState, useMemo } from "react";
import Image from "next/image";
import Link from "next/link";
import { Search, ExternalLink, Clock, Trophy } from "lucide-react";
import type { AcademicYear, ClubContestDetailDto, RanklistRowDto } from "@/types/api";
import { ACADEMIC_YEAR_LABELS } from "@/types/api";
import { cn, serverDate } from "@/lib/utils";

interface RanklistTableProps {
  data: ClubContestDetailDto;
}

type Division = "ALL" | AcademicYear;

const DIVISIONS: { key: Division; label: string }[] = [
  { key: "ALL", label: "Everyone" },
  { key: "FIRST_YEAR", label: ACADEMIC_YEAR_LABELS.FIRST_YEAR },
  { key: "SECOND_YEAR_ONWARDS", label: ACADEMIC_YEAR_LABELS.SECOND_YEAR_ONWARDS },
];

const MEDAL = ["🥇", "🥈", "🥉"];

/** A row with its rank among the rows shown, which for a division is the division rank. */
interface ShownRow {
  row: RanklistRowDto;
  rank: number;
  tied: boolean;
}

/**
 * Ranks rows that are already in club order. Members tied in the club ranklist
 * stay tied; the next distinct rank skips, as in the club ranklist itself.
 */
function rankWithin(rows: RanklistRowDto[]): ShownRow[] {
  const ranked: ShownRow[] = [];
  rows.forEach((row, i) => {
    const prev = ranked[i - 1];
    const rank = prev && prev.row.rank === row.rank ? prev.rank : i + 1;
    ranked.push({ row, rank, tied: false });
  });
  ranked.forEach((r, i) => {
    r.tied = ranked[i - 1]?.rank === r.rank || ranked[i + 1]?.rank === r.rank;
  });
  return ranked;
}

export function RanklistTable({ data }: RanklistTableProps) {
  const [search, setSearch] = useState("");
  const [division, setDivision] = useState<Division>("ALL");

  // The club ranklist is ordered by rank already; sorted here so the division
  // ranks below never depend on that.
  const ordered = useMemo(() => [...data.rows].sort((a, b) => a.rank - b.rank), [data.rows]);

  const byDivision = useMemo(
    () => ({
      FIRST_YEAR: rankWithin(ordered.filter((r) => r.member.academicYear === "FIRST_YEAR")),
      SECOND_YEAR_ONWARDS: rankWithin(ordered.filter((r) => r.member.academicYear === "SECOND_YEAR_ONWARDS")),
    }),
    [ordered]
  );
  const yearNotSet = ordered.filter((r) => !r.member.academicYear).length;
  const hasDivisions = byDivision.FIRST_YEAR.length + byDivision.SECOND_YEAR_ONWARDS.length > 0;

  const shownRows = useMemo(() => {
    const base = division === "ALL" ? rankWithin(ordered) : byDivision[division];
    if (!search.trim()) return base;
    const q = search.toLowerCase();
    return base.filter(
      ({ row }) =>
        row.member.name.toLowerCase().includes(q) ||
        (row.member.codeforcesHandle && row.member.codeforcesHandle.toLowerCase().includes(q))
    );
  }, [ordered, byDivision, division, search]);

  const durationHours = Math.floor(data.contest.durationSeconds / 3600);
  const durationMins = Math.floor((data.contest.durationSeconds % 3600) / 60);

  return (
    <div className="space-y-6">
      {/* Contest Overview Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 p-5 rounded-2xl border border-white/10 bg-[#0d0d16]/80 backdrop-blur-md">
        <div>
          <div className="flex flex-wrap items-center gap-2 mb-1.5">
            <span className="text-nano font-mono uppercase tracking-caps px-2 py-0.5 rounded-full bg-primary/20 text-primary border border-primary/30">
              {data.contest.kind}
            </span>
            {data.contest.seasonName && (
              <span className="text-nano font-mono uppercase tracking-caps px-2 py-0.5 rounded-full bg-white/5 text-fg-muted border border-white/10">
                {data.contest.seasonName}
              </span>
            )}
          </div>
          <h2 className="text-xl font-bold tracking-tight text-white flex items-center gap-2">
            {data.contest.name}
            {data.contest.url && (
              <a
                href={data.contest.url}
                target="_blank"
                rel="noreferrer"
                className="text-fg-muted hover:text-white transition-colors"
                title="View contest on Codeforces"
              >
                <ExternalLink className="size-4" />
              </a>
            )}
          </h2>
          <div className="flex flex-wrap items-center gap-4 mt-2 text-micro font-mono text-fg-muted">
            <span className="flex items-center gap-1">
              <Clock className="size-3.5" />
              Duration: {durationHours}h {durationMins > 0 ? `${durationMins}m` : ""}
            </span>
            <span>Participants: {data.contest.participantCount}</span>
            {data.contest.lastSyncedAt && (
              <span>Last synced: {serverDate(data.contest.lastSyncedAt).toLocaleString("en-IN", { timeZone: "Asia/Kolkata" })} IST</span>
            )}
          </div>
        </div>

        {/* Search */}
        <div className="relative w-full md:w-64">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-fg-muted" />
          <input
            type="search"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search member..."
            aria-label="Search members"
            className="w-full rounded-xl border border-white/10 bg-surface-2 pl-9 pr-3 py-2 text-xs text-foreground placeholder:text-fg-subtle focus:border-primary/50 focus:outline-none"
          />
        </div>
      </div>

      {/* Year-wise prize podiums: the club gives 1st years and 2nd year onwards separate prizes. */}
      {hasDivisions && (
        <div className="grid gap-4 md:grid-cols-2">
          {(["FIRST_YEAR", "SECOND_YEAR_ONWARDS"] as AcademicYear[]).map((year) => {
            const top = byDivision[year].filter((r) => r.rank <= 3);
            return (
              <section
                key={year}
                aria-label={`Top 3, ${ACADEMIC_YEAR_LABELS[year]}`}
                className="rounded-2xl border border-white/10 bg-[#0d0d16]/80 p-4"
              >
                <h3 className="mb-3 flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-white">
                  <Trophy className="size-3.5 text-amber-400" />
                  Top 3 · {ACADEMIC_YEAR_LABELS[year]}
                </h3>
                {top.length === 0 ? (
                  <p className="text-xs text-fg-muted">No {ACADEMIC_YEAR_LABELS[year]} participants yet.</p>
                ) : (
                  <ol className="space-y-2">
                    {top.map(({ row, rank, tied }) => (
                      <li key={row.member.id} className="flex items-center gap-3 text-sm">
                        <span className="w-6 text-center text-lg" aria-label={`${tied ? "Tied " : ""}place ${rank}`}>
                          {MEDAL[rank - 1]}
                        </span>
                        <Link
                          href={`/profile/${row.member.id}`}
                          className="min-w-0 flex-1 truncate font-medium text-white hover:text-primary"
                        >
                          {row.member.name}
                          {tied && <span className="ml-1 text-[10px] text-fg-muted">(tied)</span>}
                        </Link>
                        <span className="shrink-0 font-mono text-xs text-fg-muted tabular-nums">
                          {row.solvedCount} solved · club #{row.rank}
                        </span>
                      </li>
                    ))}
                  </ol>
                )}
              </section>
            );
          })}
        </div>
      )}
      {hasDivisions && yearNotSet > 0 && (
        <p className="text-xs text-amber-300/90">
          {yearNotSet} participant{yearNotSet === 1 ? " has" : "s have"} not set their year on their
          profile, so {yearNotSet === 1 ? "is" : "are"} not in either year-wise top 3.
        </p>
      )}

      {/* Division filter */}
      {hasDivisions && (
        <div className="flex flex-wrap gap-2" role="group" aria-label="Show ranking for">
          {DIVISIONS.map((d) => (
            <button
              key={d.key}
              type="button"
              aria-pressed={division === d.key}
              onClick={() => setDivision(d.key)}
              className={cn(
                "rounded-full border px-3 py-1 text-xs font-semibold transition-colors",
                division === d.key
                  ? "border-primary bg-primary/15 text-primary"
                  : "border-white/10 bg-white/[0.03] text-fg-muted hover:text-white"
              )}
            >
              {d.label}
            </button>
          ))}
        </div>
      )}

      {/* Standings Table */}
      <div className="overflow-x-auto rounded-2xl border border-white/10 bg-[#0d0d16]/70 shadow-xl">
        <table className="w-full text-left text-xs border-collapse">
          <thead>
            <tr className="border-b border-white/10 bg-white/[0.02] text-nano font-mono uppercase tracking-caps text-fg-muted">
              <th className="py-3 px-4 w-14 text-center">#</th>
              <th className="py-3 px-4 min-w-[180px]">Member</th>
              <th className="py-3 px-3 text-center w-16">Solved</th>
              <th className="py-3 px-3 text-center w-20">Points</th>
              <th className="py-3 px-3 text-center w-16">Penalty</th>
              {data.problems.map((prob) => (
                <th key={prob.problemIndex} className="py-3 px-2 text-center min-w-[50px]">
                  <div className="font-bold text-white">{prob.problemIndex}</div>
                  {prob.rating && (
                    <div className="text-[10px] font-normal text-fg-subtle">{prob.rating}</div>
                  )}
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-white/5">
            {shownRows.length === 0 ? (
              <tr>
                <td colSpan={5 + data.problems.length} className="py-12 text-center text-fg-muted font-mono text-xs">
                  {search
                    ? "No members match your search."
                    : division !== "ALL"
                    ? "No participants from this year."
                    : "No standings recorded for this contest."}
                </td>
              </tr>
            ) : (
              shownRows.map(({ row, rank, tied }) => {
                return (
                  <tr key={row.member.id} className="hover:bg-white/[0.02] transition-colors">
                    {/* Rank: within the division when one is selected */}
                    <td className="py-3 px-4 text-center font-mono">
                      {rank === 1 ? (
                        <span className="inline-flex items-center justify-center size-6 rounded-full bg-amber-400/20 text-amber-300 font-bold border border-amber-400/40">
                          1
                        </span>
                      ) : rank === 2 ? (
                        <span className="inline-flex items-center justify-center size-6 rounded-full bg-slate-300/20 text-slate-200 font-bold border border-slate-300/40">
                          2
                        </span>
                      ) : rank === 3 ? (
                        <span className="inline-flex items-center justify-center size-6 rounded-full bg-amber-600/20 text-amber-500 font-bold border border-amber-600/40">
                          3
                        </span>
                      ) : (
                        // "=" only for a real tie; it used to prefix every rank from 4 down.
                        <span className="text-fg-muted">{tied ? `=${rank}` : rank}</span>
                      )}
                      {division !== "ALL" && (
                        <div className="mt-0.5 text-[10px] text-fg-subtle">club #{row.rank}</div>
                      )}
                    </td>

                    {/* Member */}
                    <td className="py-3 px-4">
                      <div className="flex items-center gap-2.5">
                        <Link href={`/profile/${row.member.id}`} className="group flex items-center gap-2.5">
                          <div className="size-7 rounded-full overflow-hidden bg-surface-3 border border-white/10 shrink-0">
                            {row.member.avatarUrl ? (
                              <Image
                                src={row.member.avatarUrl}
                                alt={row.member.name}
                                width={28}
                                height={28}
                                className="size-full object-cover"
                              />
                            ) : (
                              <div className="size-full flex items-center justify-center text-[10px] font-bold text-fg-muted">
                                {row.member.name.slice(0, 2).toUpperCase()}
                              </div>
                            )}
                          </div>
                          <div>
                            <div className="font-medium text-white group-hover:text-primary transition-colors">
                              {row.member.name}
                            </div>
                            {row.member.codeforcesHandle && (
                              <div className="text-[11px] font-mono text-fg-muted">
                                @{row.member.codeforcesHandle}
                              </div>
                            )}
                          </div>
                        </Link>
                      </div>
                    </td>

                    {/* Solved */}
                    <td className="py-3 px-3 text-center font-mono font-bold text-white tabular-nums">
                      {row.solvedCount}
                    </td>

                    {/* CF Points */}
                    <td className="py-3 px-3 text-center font-mono text-fg-muted tabular-nums">
                      {row.cfPoints}
                    </td>

                    {/* Penalty */}
                    <td className="py-3 px-3 text-center font-mono text-fg-subtle tabular-nums">
                      {row.penalty}
                    </td>

                    {/* Problem Cells */}
                    {row.cells.map((cell, idx) => (
                      <td key={idx} className="py-3 px-2 text-center">
                        {cell.solved ? (
                          <span className="inline-flex items-center justify-center size-7 rounded-lg bg-emerald-500/20 border border-emerald-500/40 text-emerald-400 font-bold font-mono text-[11px]">
                            +
                          </span>
                        ) : cell.attempted ? (
                          <span className="inline-flex items-center justify-center size-7 rounded-lg bg-rose-500/10 border border-rose-500/20 text-rose-400 font-mono text-[11px]">
                            -
                          </span>
                        ) : (
                          <span className="inline-flex items-center justify-center size-7 text-white/10 font-mono text-xs">
                            ·
                          </span>
                        )}
                      </td>
                    ))}
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
