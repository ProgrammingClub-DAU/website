"use client";

import React, { useState, useMemo } from "react";
import Image from "next/image";
import Link from "next/link";
import { Search } from "lucide-react";
import type { UpsolveMatrixDto, UpsolveStatus } from "@/types/api";

interface UpsolveMatrixProps {
  data: UpsolveMatrixDto;
}

export function UpsolveMatrix({ data }: UpsolveMatrixProps) {
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

  return (
    <div className="space-y-6">
      {/* Header and Legend */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 p-5 rounded-2xl border border-white/10 bg-[#0d0d16]/80 backdrop-blur-md">
        <div>
          <h2 className="text-xl font-bold tracking-tight text-white">
            {data.contestName} — Upsolve Matrix
          </h2>
          <div className="flex flex-wrap items-center gap-4 mt-2 text-xs">
            <span className="flex items-center gap-1.5 text-emerald-400 font-mono">
              <span className="size-2.5 rounded-full bg-emerald-500" />
              Solved in Contest
            </span>
            <span className="flex items-center gap-1.5 text-cyan-400 font-mono">
              <span className="size-2.5 rounded-full bg-cyan-500" />
              Upsolved Later
            </span>
            <span className="flex items-center gap-1.5 text-fg-subtle font-mono">
              <span className="size-2.5 rounded-full bg-white/20" />
              Unsolved
            </span>
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

      {/* Grid */}
      <div className="overflow-x-auto rounded-2xl border border-white/10 bg-[#0d0d16]/70 shadow-xl">
        <table className="w-full text-left text-xs border-collapse">
          <thead>
            <tr className="border-b border-white/10 bg-white/[0.02] text-nano font-mono uppercase tracking-caps text-fg-muted">
              <th className="py-3 px-4 min-w-[200px]">Member</th>
              {data.problems.map((prob) => (
                <th key={prob.index} className="py-3 px-3 text-center min-w-[70px]">
                  <div className="font-bold text-white">{prob.index}</div>
                  {prob.rating && (
                    <div className="text-[10px] text-fg-subtle">{prob.rating}</div>
                  )}
                  {prob.upsolveRate != null && (
                    <div className="text-[9px] text-cyan-400 font-normal">
                      {Math.round(prob.upsolveRate * 100)}%
                    </div>
                  )}
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-white/5">
            {filteredRows.length === 0 ? (
              <tr>
                <td colSpan={1 + data.problems.length} className="py-12 text-center text-fg-muted font-mono text-xs">
                  {search ? "No members match your search." : "No participants recorded."}
                </td>
              </tr>
            ) : (
              filteredRows.map((row) => (
                <tr key={row.member.id} className="hover:bg-white/[0.02] transition-colors">
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

                  {row.cells.map((status: UpsolveStatus, idx) => (
                    <td key={idx} className="py-3 px-3 text-center">
                      {status === "SOLVED_IN_CONTEST" ? (
                        <span
                          title="Solved during contest"
                          className="inline-flex items-center justify-center size-7 rounded-lg bg-emerald-500/20 border border-emerald-500/40 text-emerald-400 font-bold font-mono text-[11px]"
                        >
                          +
                        </span>
                      ) : status === "UPSOLVED" ? (
                        <span
                          title="Upsolved after contest"
                          className="inline-flex items-center justify-center size-7 rounded-lg bg-cyan-500/20 border border-cyan-500/40 text-cyan-300 font-bold font-mono text-[10px]"
                        >
                          +U
                        </span>
                      ) : (
                        <span className="inline-flex items-center justify-center size-7 text-white/10 font-mono text-xs">
                          ·
                        </span>
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
