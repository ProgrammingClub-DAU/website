"use client";

import React, { useState, useEffect, useCallback } from "react";
import Link from "next/link";
import Image from "next/image";
import { Award, RefreshCw, Loader2, CheckCircle2 } from "lucide-react";
import { awardsService } from "@/lib/services/awards";
import { AwardBadge } from "@/components/site/award-badge";
import type { WeeklyAwardDto } from "@/types/api";
import { serverDate } from "@/lib/utils";

export function AwardsManager() {
  const [weekStart, setWeekStart] = useState("");
  const [awards, setAwards] = useState<WeeklyAwardDto[]>([]);
  const [loading, setLoading] = useState(false);
  const [recomputing, setRecomputing] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  const loadWeekAwards = useCallback(async (dateStr: string) => {
    setLoading(true);
    setMessage(null);
    try {
      const res = await awardsService.getWeek(dateStr);
      setAwards(res);
    } catch (err) {
      console.error("Failed to load week awards:", err);
      setAwards([]);
    } finally {
      setLoading(false);
    }
  }, []);

  // Helper to find previous Monday IST in YYYY-MM-DD
  useEffect(() => {
    const now = new Date();
    // Monday of current/preceding week
    const day = now.getDay();
    const diff = now.getDate() - day + (day === 0 ? -6 : 1);
    const monday = new Date(now.setDate(diff));
    const yyyy = monday.getFullYear();
    const mm = String(monday.getMonth() + 1).padStart(2, "0");
    const dd = String(monday.getDate()).padStart(2, "0");
    const formatted = `${yyyy}-${mm}-${dd}`;
    setWeekStart(formatted);
    loadWeekAwards(formatted);
  }, [loadWeekAwards]);

  const handleRecompute = async () => {
    if (!weekStart) return;
    setRecomputing(true);
    setMessage(null);
    try {
      const res = await awardsService.recompute(weekStart);
      setAwards(res);
      setMessage(`Successfully computed ${res.length} awards for week starting ${weekStart}.`);
    } catch (err) {
      console.error("Failed to recompute awards:", err);
      setMessage("Failed to recompute awards for this week.");
    } finally {
      setRecomputing(false);
    }
  };

  return (
    <div className="space-y-8">
      {/* Control Card */}
      <div className="rounded-2xl border border-white/10 bg-[#0d0d16]/80 p-5 backdrop-blur-md space-y-4">
        <div className="border-b border-white/10 pb-3">
          <h3 className="text-base font-bold text-white flex items-center gap-2">
            <Award className="size-4 text-amber-400" />
            Weekly Honors & Awards Management
          </h3>
          <p className="text-xs text-fg-muted mt-0.5">
            Compute or audit Coder of the Week, Most Solved, and Biggest Rating Jump for any closed week.
          </p>
        </div>

        <div className="flex flex-col sm:flex-row sm:items-end gap-4">
          <div className="flex-1">
            <label className="block text-nano font-mono uppercase tracking-caps text-fg-muted mb-1">
              Week Start Date (Monday IST)
            </label>
            <input
              type="date"
              value={weekStart}
              onChange={(e) => {
                setWeekStart(e.target.value);
                loadWeekAwards(e.target.value);
              }}
              className="w-full rounded-xl border border-white/10 bg-surface-2 px-3 py-2 text-xs text-foreground focus:border-primary/50 focus:outline-none"
            />
          </div>

          <button
            type="button"
            onClick={handleRecompute}
            disabled={recomputing || !weekStart}
            className="rounded-xl bg-amber-500 px-5 py-2.5 text-xs font-bold text-black hover:bg-amber-400 transition-colors shadow-sm disabled:opacity-50 flex items-center justify-center gap-1.5"
          >
            {recomputing ? (
              <Loader2 className="size-3.5 animate-spin" />
            ) : (
              <RefreshCw className="size-3.5" />
            )}
            {recomputing ? "Computing..." : "Recompute Week Awards"}
          </button>
        </div>

        {message && (
          <div className="rounded-lg bg-white/5 border border-white/10 p-3 text-xs font-mono text-white flex items-center gap-2">
            <CheckCircle2 className="size-4 text-emerald-400 shrink-0" />
            {message}
          </div>
        )}
      </div>

      {/* Awards List */}
      <div className="space-y-4">
        <h4 className="text-sm font-bold uppercase tracking-wider text-white">
          Awards for Week ({awards.length})
        </h4>

        {loading ? (
          <div className="p-12 text-center font-mono text-xs text-fg-muted animate-pulse">
            Loading awards...
          </div>
        ) : awards.length === 0 ? (
          <div className="rounded-2xl border border-white/10 bg-[#0d0d16]/70 p-12 text-center text-fg-muted font-mono text-xs">
            No awards computed yet for week starting {weekStart}. Click &quot;Recompute Week Awards&quot; above to run the evaluation.
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {awards.map((award) => (
              <div
                key={award.id}
                className="rounded-2xl border border-white/10 bg-[#0d0d16]/80 p-5 backdrop-blur-md space-y-4 shadow-lg"
              >
                <AwardBadge
                  awardType={award.awardType}
                  metricValue={award.metricValue}
                  platform={award.platform}
                  size="md"
                />

                <Link href={`/profile/${award.userId}`} className="flex items-center gap-3 group">
                  <div className="size-10 rounded-full overflow-hidden bg-surface-3 border border-white/10 shrink-0">
                    {award.avatarUrl ? (
                      <Image
                        src={award.avatarUrl}
                        alt={award.userName}
                        width={40}
                        height={40}
                        className="size-full object-cover"
                      />
                    ) : (
                      <div className="size-full flex items-center justify-center text-xs font-bold text-fg-muted">
                        {award.userName.slice(0, 2).toUpperCase()}
                      </div>
                    )}
                  </div>
                  <div>
                    <h5 className="font-semibold text-white group-hover:text-primary transition-colors text-sm">
                      {award.userName}
                    </h5>
                    <div className="text-micro font-mono text-fg-muted">
                      {award.codeforcesHandle ? `@${award.codeforcesHandle}` : "Member"}
                    </div>
                  </div>
                </Link>

                <div className="pt-3 border-t border-white/5 text-[11px] font-mono text-fg-subtle">
                  Computed: {serverDate(award.computedAt).toLocaleString("en-IN", { timeZone: "Asia/Kolkata" })} IST
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
