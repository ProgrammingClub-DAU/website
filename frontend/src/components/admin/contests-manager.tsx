"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import {
  Trophy,
  Plus,
  RefreshCw,
  Trash2,
  Calendar,
  ExternalLink,
  Loader2,
} from "lucide-react";
import { contestService } from "@/lib/services/contests";
import { seasonService } from "@/lib/services/seasons";
import type {
  ClubContestSummaryDto,
  SeasonDto,
  ClubContestKind,
  ClubContestCreateRequest,
  SeasonRequest,
} from "@/types/api";
import { serverDate } from "@/lib/utils";
import { apiErrorMessage } from "@/lib/api-error";

/** Codeforces numbers every gym, mashup and group contest from this id up. */
const GYM_ID_FLOOR = 100000;

export function ContestsManager() {
  const [contests, setContests] = useState<ClubContestSummaryDto[]>([]);
  const [seasons, setSeasons] = useState<SeasonDto[]>([]);
  const [loading, setLoading] = useState(true);

  // Contest Form State
  const [cfContestId, setCfContestId] = useState("");
  const [kind, setKind] = useState<ClubContestKind>("OFFICIAL");
  const [selectedSeasonId, setSelectedSeasonId] = useState<string>("");
  const [isAddingContest, setIsAddingContest] = useState(false);
  const [contestError, setContestError] = useState<string | null>(null);

  // Season Form State
  const [seasonName, setSeasonName] = useState("");
  const [startsOn, setStartsOn] = useState("");
  const [endsOn, setEndsOn] = useState("");
  const [countBest, setCountBest] = useState("");
  const [isAddingSeason, setIsAddingSeason] = useState(false);
  const [seasonError, setSeasonError] = useState<string | null>(null);

  // Sync state
  const [syncingId, setSyncingId] = useState<number | null>(null);

  const loadData = async () => {
    try {
      const [contestList, seasonList] = await Promise.all([
        contestService.list(),
        seasonService.list(),
      ]);
      setContests(contestList);
      setSeasons(seasonList);
    } catch (err) {
      console.error("Failed to load contests data:", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  /**
   * Accepts a bare id or a pasted link -- codeforces.com/contest/1843,
   * codeforces.com/gym/724781, codeforces.com/group/<code>/contest/123456 -- and
   * picks the kind the id implies. Codeforces numbers every gym, mashup and group
   * contest from 100000, and the backend refuses a kind that does not match the id;
   * with "Official Round" preselected, every gym id used to be refused.
   */
  const handleContestInput = (value: string) => {
    const trimmed = value.trim();
    const link = trimmed.match(/codeforces\.com\/(group\/[^/]+\/contest|gym|contest)\/(\d+)/i);
    const id = link ? link[2] : trimmed;
    setCfContestId(id);
    const numId = parseInt(id, 10);
    if (!Number.isFinite(numId)) return;
    if (link?.[1].toLowerCase().startsWith("group")) {
      setKind("GROUP");
    } else if (numId >= GYM_ID_FLOOR) {
      if (kind === "OFFICIAL") setKind("GYM");
    } else {
      setKind("OFFICIAL");
    }
  };

  const handleCreateContest = async (e: React.FormEvent) => {
    e.preventDefault();
    setContestError(null);
    const numId = parseInt(cfContestId, 10);
    if (isNaN(numId) || numId <= 0) {
      setContestError("Enter a valid Codeforces contest ID or paste the contest link.");
      return;
    }

    setIsAddingContest(true);
    try {
      const payload: ClubContestCreateRequest = {
        cfContestId: numId,
        kind,
        seasonId: selectedSeasonId ? parseInt(selectedSeasonId, 10) : null,
      };
      await contestService.create(payload);
      setCfContestId("");
      await loadData();
    } catch (err: unknown) {
      console.error("Failed to add contest:", err);
      // The backend says exactly why: wrong kind for the id, API key not configured,
      // no access to a private gym, no start time yet...
      setContestError(apiErrorMessage(err, "Failed to add contest. Check the Codeforces contest ID."));
    } finally {
      setIsAddingContest(false);
    }
  };

  const handleCreateSeason = async (e: React.FormEvent) => {
    e.preventDefault();
    setSeasonError(null);
    if (!seasonName.trim() || !startsOn || !endsOn) {
      setSeasonError("Please fill in season name, start date, and end date.");
      return;
    }

    setIsAddingSeason(true);
    try {
      const payload: SeasonRequest = {
        name: seasonName.trim(),
        startsOn,
        endsOn,
        countBest: countBest.trim() ? parseInt(countBest, 10) : null,
      };
      await seasonService.create(payload);
      setSeasonName("");
      setStartsOn("");
      setEndsOn("");
      setCountBest("");
      await loadData();
    } catch (err: unknown) {
      console.error("Failed to create season:", err);
      setSeasonError("Failed to create season. Please check your inputs.");
    } finally {
      setIsAddingSeason(false);
    }
  };

  const handleSyncContest = async (id: number) => {
    setSyncingId(id);
    try {
      await contestService.sync(id);
      await loadData();
    } catch (err) {
      console.error("Sync failed:", err);
      alert("Failed to sync contest with Codeforces.");
    } finally {
      setSyncingId(null);
    }
  };

  const handleDeleteContest = async (id: number) => {
    if (!window.confirm("Remove this club contest?")) return;
    try {
      await contestService.remove(id);
      setContests((prev) => prev.filter((c) => c.id !== id));
    } catch (err) {
      console.error("Failed to remove contest:", err);
      alert("Failed to delete contest.");
    }
  };

  const handleDeleteSeason = async (id: number) => {
    if (!window.confirm("Delete this season? Its contests will remain.")) return;
    try {
      await seasonService.remove(id);
      setSeasons((prev) => prev.filter((s) => s.id !== id));
    } catch (err) {
      console.error("Failed to remove season:", err);
      alert("Failed to delete season.");
    }
  };

  if (loading) {
    return (
      <div className="p-12 text-center font-mono text-xs text-fg-muted animate-pulse">
        Loading contests and seasons...
      </div>
    );
  }

  return (
    <div className="space-y-10">
      {/* Forms Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Register Contest Form */}
        <div className="rounded-2xl border border-white/10 bg-[#0d0d16]/80 p-5 backdrop-blur-md space-y-4">
          <div className="border-b border-white/10 pb-3">
            <h3 className="text-base font-bold text-white flex items-center gap-2">
              <Trophy className="size-4 text-primary" />
              Register Club Contest
            </h3>
            <p className="text-xs text-fg-muted mt-0.5">
              Link an official, gym, mashup, or group round from Codeforces.
            </p>
          </div>

          <form onSubmit={handleCreateContest} className="space-y-3">
            {contestError && (
              <div className="rounded-lg bg-rose-500/10 border border-rose-500/20 p-2.5 text-xs text-rose-400">
                {contestError}
              </div>
            )}

            <div>
              <label
                htmlFor="cf-contest-id"
                className="block text-nano font-mono uppercase tracking-caps text-fg-muted mb-1"
              >
                Codeforces Contest ID or link
              </label>
              <input
                id="cf-contest-id"
                type="text"
                inputMode="numeric"
                value={cfContestId}
                onChange={(e) => handleContestInput(e.target.value)}
                onPaste={(e) => {
                  e.preventDefault();
                  handleContestInput(e.clipboardData.getData("text"));
                }}
                placeholder="1843, 724781, or codeforces.com/gym/724781"
                required
                className="w-full rounded-xl border border-white/10 bg-surface-2 px-3 py-2 text-xs text-foreground placeholder:text-fg-subtle focus:border-primary/50 focus:outline-none"
              />
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-nano font-mono uppercase tracking-caps text-fg-muted mb-1">
                  Contest Kind
                </label>
                <select
                  value={kind}
                  onChange={(e) => setKind(e.target.value as ClubContestKind)}
                  className="w-full rounded-xl border border-white/10 bg-surface-2 px-3 py-2 text-xs text-foreground focus:border-primary/50 focus:outline-none"
                >
                  <option value="OFFICIAL">Official Round</option>
                  <option value="GYM">Gym Contest</option>
                  <option value="MASHUP">Mashup</option>
                  <option value="GROUP">Group Round</option>
                </select>
                {parseInt(cfContestId, 10) >= GYM_ID_FLOOR && (
                  <p className="mt-1 text-[10px] text-fg-muted">
                    Gym, mashup and group contests are read with the club&apos;s Codeforces API key. For a
                    private gym, the key&apos;s account must be a manager of the gym.
                  </p>
                )}
              </div>

              <div>
                <label className="block text-nano font-mono uppercase tracking-caps text-fg-muted mb-1">
                  Championship Season
                </label>
                <select
                  value={selectedSeasonId}
                  onChange={(e) => setSelectedSeasonId(e.target.value)}
                  className="w-full rounded-xl border border-white/10 bg-surface-2 px-3 py-2 text-xs text-foreground focus:border-primary/50 focus:outline-none"
                >
                  <option value="">None (Independent)</option>
                  {seasons.map((s) => (
                    <option key={s.id} value={s.id}>
                      {s.name}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <button
              type="submit"
              disabled={isAddingContest}
              className="w-full rounded-xl bg-primary py-2.5 text-xs font-bold text-primary-foreground hover:bg-primary-hover transition-colors shadow-sm disabled:opacity-50 flex items-center justify-center gap-1.5"
            >
              {isAddingContest ? <Loader2 className="size-3.5 animate-spin" /> : <Plus className="size-3.5" />}
              {isAddingContest ? "Registering & Syncing..." : "Register Contest"}
            </button>
          </form>
        </div>

        {/* Create Season Form */}
        <div className="rounded-2xl border border-white/10 bg-[#0d0d16]/80 p-5 backdrop-blur-md space-y-4">
          <div className="border-b border-white/10 pb-3">
            <h3 className="text-base font-bold text-white flex items-center gap-2">
              <Calendar className="size-4 text-amber-400" />
              Create Championship Season
            </h3>
            <p className="text-xs text-fg-muted mt-0.5">
              Group contests together into a seasonal leaderboard.
            </p>
          </div>

          <form onSubmit={handleCreateSeason} className="space-y-3">
            {seasonError && (
              <div className="rounded-lg bg-rose-500/10 border border-rose-500/20 p-2.5 text-xs text-rose-400">
                {seasonError}
              </div>
            )}

            <div>
              <label className="block text-nano font-mono uppercase tracking-caps text-fg-muted mb-1">
                Season Name
              </label>
              <input
                type="text"
                value={seasonName}
                onChange={(e) => setSeasonName(e.target.value)}
                placeholder="e.g. Autumn 2026 Championship"
                required
                className="w-full rounded-xl border border-white/10 bg-surface-2 px-3 py-2 text-xs text-foreground placeholder:text-fg-subtle focus:border-primary/50 focus:outline-none"
              />
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-nano font-mono uppercase tracking-caps text-fg-muted mb-1">
                  Start Date
                </label>
                <input
                  type="date"
                  value={startsOn}
                  onChange={(e) => setStartsOn(e.target.value)}
                  required
                  className="w-full rounded-xl border border-white/10 bg-surface-2 px-3 py-2 text-xs text-foreground focus:border-primary/50 focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-nano font-mono uppercase tracking-caps text-fg-muted mb-1">
                  End Date
                </label>
                <input
                  type="date"
                  value={endsOn}
                  onChange={(e) => setEndsOn(e.target.value)}
                  required
                  className="w-full rounded-xl border border-white/10 bg-surface-2 px-3 py-2 text-xs text-foreground focus:border-primary/50 focus:outline-none"
                />
              </div>
            </div>

            <div>
              <label className="block text-nano font-mono uppercase tracking-caps text-fg-muted mb-1">
                Count Best K Contests (Optional)
              </label>
              <input
                type="number"
                value={countBest}
                onChange={(e) => setCountBest(e.target.value)}
                placeholder="Leave blank to count all contests"
                className="w-full rounded-xl border border-white/10 bg-surface-2 px-3 py-2 text-xs text-foreground placeholder:text-fg-subtle focus:border-primary/50 focus:outline-none"
              />
            </div>

            <button
              type="submit"
              disabled={isAddingSeason}
              className="w-full rounded-xl bg-amber-500 py-2.5 text-xs font-bold text-black hover:bg-amber-400 transition-colors shadow-sm disabled:opacity-50 flex items-center justify-center gap-1.5"
            >
              {isAddingSeason ? <Loader2 className="size-3.5 animate-spin" /> : <Plus className="size-3.5" />}
              {isAddingSeason ? "Creating Season..." : "Create Season"}
            </button>
          </form>
        </div>
      </div>

      {/* Contests Table */}
      <div className="space-y-4">
        <div className="flex items-center justify-between border-b border-white/10 pb-3">
          <h3 className="text-base font-bold text-white">Registered Contests ({contests.length})</h3>
        </div>

        <div className="overflow-x-auto rounded-2xl border border-white/10 bg-[#0d0d16]/70 shadow-xl">
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="border-b border-white/10 bg-white/[0.02] text-nano font-mono uppercase tracking-caps text-fg-muted">
                <th className="py-3 px-4">Contest</th>
                <th className="py-3 px-3">Kind</th>
                <th className="py-3 px-3">Season</th>
                <th className="py-3 px-3 text-center">Participants</th>
                <th className="py-3 px-3">Last Synced</th>
                <th className="py-3 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/5 font-mono">
              {contests.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-8 text-center text-fg-muted">
                    No club contests added yet.
                  </td>
                </tr>
              ) : (
                contests.map((c) => (
                  <tr key={c.id} className="hover:bg-white/[0.02] transition-colors">
                    <td className="py-3 px-4">
                      <div className="font-sans font-medium text-white flex items-center gap-1.5">
                        {c.name}
                        {c.url && (
                          <a href={c.url} target="_blank" rel="noreferrer" className="text-fg-subtle hover:text-white">
                            <ExternalLink className="size-3" />
                          </a>
                        )}
                      </div>
                      <div className="text-[11px] text-fg-muted">CF #{c.cfContestId}</div>
                    </td>
                    <td className="py-3 px-3">
                      <span className="text-[10px] px-2 py-0.5 rounded-full bg-white/5 border border-white/10 text-fg-muted">
                        {c.kind}
                      </span>
                    </td>
                    <td className="py-3 px-3 text-fg-muted">{c.seasonName || "—"}</td>
                    <td className="py-3 px-3 text-center text-white tabular-nums">{c.participantCount}</td>
                    <td className="py-3 px-3 text-fg-subtle text-[11px]">
                      {c.lastSyncedAt ? serverDate(c.lastSyncedAt).toLocaleString("en-IN", { timeZone: "Asia/Kolkata" }) : "Never"}
                    </td>
                    <td className="py-3 px-4 text-right space-x-2">
                      <button
                        type="button"
                        onClick={() => handleSyncContest(c.id)}
                        disabled={syncingId === c.id}
                        className="inline-flex items-center gap-1 rounded-lg border border-primary/30 bg-primary/10 px-2.5 py-1 text-[11px] text-primary hover:bg-primary/20 transition-all disabled:opacity-50"
                      >
                        <RefreshCw className={`size-3 ${syncingId === c.id ? "animate-spin" : ""}`} />
                        Sync
                      </button>
                      <button
                        type="button"
                        onClick={() => handleDeleteContest(c.id)}
                        className="inline-flex items-center gap-1 rounded-lg border border-rose-500/30 bg-rose-500/10 px-2.5 py-1 text-[11px] text-rose-400 hover:bg-rose-500/20 transition-all"
                      >
                        <Trash2 className="size-3" />
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Seasons Table */}
      <div className="space-y-4">
        <div className="flex items-center justify-between border-b border-white/10 pb-3">
          <h3 className="text-base font-bold text-white">Championship Seasons ({seasons.length})</h3>
        </div>

        <div className="overflow-x-auto rounded-2xl border border-white/10 bg-[#0d0d16]/70 shadow-xl">
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="border-b border-white/10 bg-white/[0.02] text-nano font-mono uppercase tracking-caps text-fg-muted">
                <th className="py-3 px-4">Season</th>
                <th className="py-3 px-3">Timeline</th>
                <th className="py-3 px-3">Rule</th>
                <th className="py-3 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/5 font-mono">
              {seasons.length === 0 ? (
                <tr>
                  <td colSpan={4} className="py-8 text-center text-fg-muted">
                    No seasons created yet.
                  </td>
                </tr>
              ) : (
                seasons.map((s) => (
                  <tr key={s.id} className="hover:bg-white/[0.02] transition-colors">
                    <td className="py-3 px-4 font-sans font-medium text-white">{s.name}</td>
                    <td className="py-3 px-3 text-fg-muted">
                      {s.startsOn} to {s.endsOn}
                    </td>
                    <td className="py-3 px-3 text-amber-300">
                      {s.countBest != null ? `Best ${s.countBest}` : "All Counted"}
                    </td>
                    <td className="py-3 px-4 text-right space-x-2">
                      <Link
                        href={`/contests/seasons/${s.id}`}
                        className="inline-flex items-center gap-1 rounded-lg border border-white/10 bg-white/5 px-2.5 py-1 text-[11px] text-white hover:bg-white/10 transition-all"
                      >
                        Standings
                      </Link>
                      <button
                        type="button"
                        onClick={() => handleDeleteSeason(s.id)}
                        className="inline-flex items-center gap-1 rounded-lg border border-rose-500/30 bg-rose-500/10 px-2.5 py-1 text-[11px] text-rose-400 hover:bg-rose-500/20 transition-all"
                      >
                        <Trash2 className="size-3" />
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
