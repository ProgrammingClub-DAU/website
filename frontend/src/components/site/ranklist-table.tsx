"use client";

import React, { useState, useMemo } from "react";
import Image from "next/image";
import Link from "next/link";
import { Search, ExternalLink, Clock } from "lucide-react";
import type { ClubContestDetailDto, RanklistRowDto } from "@/types/api";
import { serverDate } from "@/lib/utils";

interface RanklistTableProps {
  data: ClubContestDetailDto;
}

export function RanklistTable({ data }: RanklistTableProps) {
  const [search, setSearch] = useState("");

  const filteredRows = useMemo(() => {
    if (!search.trim()) return data.rows;
    const q = search.toLowerCase();
    return data.rows.filter(
      (r) =>
        r.member.name.toLowerCase().includes(q) ||
        (r.member.codeforcesHandle && r.member.codeforcesHandle.toLowerCase().includes(q))
    );
  }, [data.rows, search]);

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
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search member..."
            className="w-full rounded-xl border border-white/10 bg-surface-2 pl-9 pr-3 py-2 text-xs text-foreground placeholder:text-fg-subtle focus:border-primary/50 focus:outline-none"
          />
        </div>
      </div>

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
            {filteredRows.length === 0 ? (
              <tr>
                <td colSpan={5 + data.problems.length} className="py-12 text-center text-fg-muted font-mono text-xs">
                  {search ? "No members match your search." : "No standings recorded for this contest."}
                </td>
              </tr>
            ) : (
              filteredRows.map((row: RanklistRowDto) => {
                return (
                  <tr key={row.member.id} className="hover:bg-white/[0.02] transition-colors">
                    {/* Rank */}
                    <td className="py-3 px-4 text-center font-mono">
                      {row.rank === 1 ? (
                        <span className="inline-flex items-center justify-center size-6 rounded-full bg-amber-400/20 text-amber-300 font-bold border border-amber-400/40">
                          1
                        </span>
                      ) : row.rank === 2 ? (
                        <span className="inline-flex items-center justify-center size-6 rounded-full bg-slate-300/20 text-slate-200 font-bold border border-slate-300/40">
                          2
                        </span>
                      ) : row.rank === 3 ? (
                        <span className="inline-flex items-center justify-center size-6 rounded-full bg-amber-600/20 text-amber-500 font-bold border border-amber-600/40">
                          3
                        </span>
                      ) : (
                        <span className="text-fg-muted">={row.rank}</span>
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
