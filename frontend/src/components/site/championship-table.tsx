"use client";

import React, { useState, useMemo } from "react";
import Image from "next/image";
import Link from "next/link";
import { Search, Trophy, Calendar } from "lucide-react";
import type { ChampionshipDto } from "@/types/api";

interface ChampionshipTableProps {
  data: ChampionshipDto;
}

export function ChampionshipTable({ data }: ChampionshipTableProps) {
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

  // Extract distinct contest names from the breakdown of the first row (or empty)
  const contests = useMemo(() => {
    if (!data.rows || data.rows.length === 0) return [];
    return data.rows[0].breakdown.map((b) => ({
      clubContestId: b.clubContestId,
      name: b.contestName,
    }));
  }, [data.rows]);

  return (
    <div className="space-y-6">
      {/* Season Overview Card */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 p-5 rounded-2xl border border-white/10 bg-[#0d0d16]/80 backdrop-blur-md">
        <div>
          <div className="flex items-center gap-2 mb-1.5">
            <span className="text-nano font-mono uppercase tracking-caps px-2 py-0.5 rounded-full bg-amber-500/20 text-amber-300 border border-amber-500/30 flex items-center gap-1">
              <Trophy className="size-3" />
              Championship
            </span>
            <span className="text-nano font-mono uppercase tracking-caps px-2 py-0.5 rounded-full bg-white/5 text-fg-muted border border-white/10">
              {data.season.countBest != null
                ? `Best ${data.season.countBest} Contests Counted`
                : "All Contests Counted"}
            </span>
          </div>
          <h2 className="text-xl font-bold tracking-tight text-white">{data.season.name}</h2>
          <p className="mt-1 text-micro font-mono text-fg-muted flex items-center gap-1">
            <Calendar className="size-3.5" />
            {data.season.startsOn} to {data.season.endsOn}
          </p>
        </div>

        {/* Search */}
        <div className="relative w-full md:w-64">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-fg-muted" />
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search standings..."
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
              <th className="py-3 px-3 text-center w-24">Total Pts</th>
              <th className="py-3 px-3 text-center w-16">Scored</th>
              <th className="py-3 px-3 text-center w-16">Best</th>
              {contests.map((c) => (
                <th key={c.clubContestId} className="py-3 px-2 text-center min-w-[70px]">
                  <Link
                    href={`/contests/${c.clubContestId}`}
                    className="hover:text-primary transition-colors block line-clamp-1"
                    title={c.name}
                  >
                    {c.name}
                  </Link>
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-white/5">
            {filteredRows.length === 0 ? (
              <tr>
                <td colSpan={5 + contests.length} className="py-12 text-center text-fg-muted font-mono text-xs">
                  {search ? "No members match your search." : "No championship standings yet for this season."}
                </td>
              </tr>
            ) : (
              filteredRows.map((row) => (
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
                  </td>

                  {/* Total Points */}
                  <td className="py-3 px-3 text-center font-mono font-bold text-amber-300 tabular-nums text-sm">
                    {row.total}
                  </td>

                  {/* Contests with Points */}
                  <td className="py-3 px-3 text-center font-mono text-fg-muted tabular-nums">
                    {row.contestsWithPoints}
                  </td>

                  {/* Best Rank */}
                  <td className="py-3 px-3 text-center font-mono text-fg-subtle tabular-nums">
                    {row.bestRank > 0 ? `#${row.bestRank}` : "—"}
                  </td>

                  {/* Contest breakdown */}
                  {row.breakdown.map((b) => (
                    <td key={b.clubContestId} className="py-3 px-2 text-center">
                      {b.points > 0 ? (
                        <span
                          title={`Rank #${b.clubRank} (${b.points} pts)${!b.counted ? " - Dropped (not best K)" : ""}`}
                          className={`inline-flex items-center justify-center px-1.5 py-0.5 rounded text-[11px] font-mono tabular-nums ${
                            b.counted
                              ? "bg-amber-400/15 text-amber-300 font-semibold border border-amber-400/30"
                              : "bg-white/5 text-fg-subtle line-through opacity-60"
                          }`}
                        >
                          {b.points}
                        </span>
                      ) : (
                        <span className="text-white/10 font-mono text-xs">·</span>
                      )}
                    </td>
                  ))}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
