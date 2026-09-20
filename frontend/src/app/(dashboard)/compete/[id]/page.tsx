/* eslint-disable react-hooks/set-state-in-effect, react-hooks/exhaustive-deps */
"use client";

import { useEffect, useState, useRef } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import Confetti from "react-confetti";
import axios from 'axios';
import apiClient from '@/lib/axios';
import type { Match, ProblemCell, Team } from "@/components/compete/types";

type SolveLog = {
  contestId: number;
  index: string;
  team: string;
  problem: ProblemCell;
  timestamp?: string | number;
};

const teamColors: Record<string, string> = {
  red: "bg-red-500/30 border-red-500/50 text-red-100",
  blue: "bg-blue-500/30 border-blue-500/50 text-blue-100",
  green: "bg-green-500/30 border-green-500/50 text-green-100",
  purple: "bg-purple-500/30 border-purple-500/50 text-purple-100",
  orange: "bg-orange-500/30 border-orange-500/50 text-orange-100",
  pink: "bg-pink-500/30 border-pink-500/50 text-pink-100",
  yellow: "bg-yellow-500/30 border-yellow-500/50 text-yellow-100",
  teal: "bg-teal-500/30 border-teal-500/50 text-teal-100",
};

type GridSize = 3 | 4 | 5 | 6;

const gridClasses = {
  3: "grid-cols-3",
  4: "grid-cols-4",
  5: "grid-cols-5",
  6: "grid-cols-6",
};

type LogEntry = {
  key: string;
  message: string;
  team: string;
};

type SolvedInfo = {
  team: string;
};

type Winner = {
  team: string;
  type: "row" | "col" | "diag" | "anti-diag";
  index: number;
  keys: string[];
} | null;

function useWindowSize() {
  const isClient = typeof window !== "undefined";
  const [size, setSize] = useState({
    width: isClient ? window.innerWidth : 0,
    height: isClient ? window.innerHeight : 0,
  });
  useEffect(() => {
    if (!isClient) return;
    function onResize() {
      setSize({ width: window.innerWidth, height: window.innerHeight });
    }
    window.addEventListener("resize", onResize);
    onResize();
    return () => window.removeEventListener("resize", onResize);
  }, [isClient]);
  return size;
}

function notifyBrowser(title: string, body?: string) {
  if (typeof window === "undefined" || !("Notification" in window)) return;
  if (Notification.permission === "granted") {
    try {
      new Notification(title, { body });
    } catch (e) {}
    return;
  }
  if (Notification.permission !== "denied") {
    Notification.requestPermission().then((permission) => {
      if (permission === "granted") {
        try {
          new Notification(title, { body });
        } catch (e) {}
      }
    });
  }
}

function normalizeProblemsFromServer(raw: ProblemCell[]) {
  if (!Array.isArray(raw)) return [];
  const active = raw.filter((p) => p && p.active !== false);
  const hasPosition = active.every((p) => typeof p.position === "number");

  if (hasPosition) {
    const byPos = new Map<number, ProblemCell>();
    for (const p of active) {
      if (!byPos.has(p.position)) byPos.set(p.position, p);
    }
    return Array.from(byPos.entries())
      .sort((a, b) => a[0] - b[0])
      .map(([_, p]) => p);
  }

  const seen = new Set<string>();
  const result: ProblemCell[] = [];
  for (const p of active) {
    const key = `${p.contestId}-${p.index}`;
    if (!seen.has(key)) {
      seen.add(key);
      result.push(p);
    }
  }
  return result;
}

function formatTime(ts?: string | number) {
  if (!ts) return "";
  const d = new Date(ts);
  if (isNaN(d.getTime())) return String(ts);
  return d.toLocaleTimeString([], { hour: "numeric", minute: "2-digit", hour12: true });
}

function formatDuration(ms: number) {
  const totalSeconds = Math.max(0, Math.floor(ms / 1000));
  const hours = String(Math.floor(totalSeconds / 3600)).padStart(2, "0");
  const minutes = String(Math.floor((totalSeconds % 3600) / 60)).padStart(2, "0");
  const seconds = String(totalSeconds % 60).padStart(2, "0");
  return `${hours}:${minutes}:${seconds}`;
}

function CountdownToStart({ startTime }: { startTime: Date }) {
  const [timeLeft, setTimeLeft] = useState(() => startTime.getTime() - Date.now());

  useEffect(() => {
    const interval = setInterval(() => {
      setTimeLeft(startTime.getTime() - Date.now());
    }, 1000);
    return () => clearInterval(interval);
  }, [startTime]);

  if (timeLeft <= 0) {
    return <p className="text-white/70">Match is starting now...</p>;
  }

  return (
    <p className="text-yellow-400 font-medium">
      Match starts in {formatDuration(timeLeft)}
    </p>
  );
}

export default function MatchPage() {
  const params = useParams();
  const id = Array.isArray(params?.id) ? params.id[0] : params?.id;

  const [showLog, setShowLog] = useState(true);
  const [problems, setProblems] = useState<ProblemCell[]>([]);
  const [loading, setLoading] = useState(true);
  const [gridSize, setGridSize] = useState<GridSize>(5);
  const [solved, setSolved] = useState<Record<string, SolvedInfo>>({});
  const [log, setLog] = useState<LogEntry[]>([]);
  const [match, setMatch] = useState<Match | null>(null);
  const [now, setNow] = useState(new Date());

  const [winner, setWinner] = useState<Winner>(null);
  const [matchLocked, setMatchLocked] = useState(false);
  const [confettiActive, setConfettiActive] = useState(false);
  const [positionOwners, setPositionOwners] = useState<Record<number, string>>({});
  const [showRatings, setShowRatings] = useState<boolean>(true);

  const { width, height } = useWindowSize();
  const notifiedRef = useRef<Set<string>>(new Set());

  function persistNotified(matchId: string) {
    const key = `notified_${matchId}`;
    try {
      localStorage.setItem(key, JSON.stringify(Array.from(notifiedRef.current)));
    } catch {}
  }

  useEffect(() => {
    if (match && typeof match.showRatings === "boolean") {
      setShowRatings(Boolean(match.showRatings));
    }
  }, [match?.showRatings]);

  useEffect(() => {
    if (!match?.id) return;
    const key = `notified_${match.id}`;
    const raw = localStorage.getItem(key);
    try {
      const arr = raw ? JSON.parse(raw) : [];
      notifiedRef.current = new Set(arr);
    } catch {
      notifiedRef.current = new Set();
    }
  }, [match?.id]);

  useEffect(() => {
    if (!id) return;
    setLoading(true);

    const fetchMatch = async () => {
      try {
        const res = await apiClient.get(`/api/compete/matches/${id}`);
        const matchObj = res.data;
        setMatch(matchObj);

        try {
          const solvedMap: Record<string, SolvedInfo> = {};
          const newLogEntries: LogEntry[] = [];
          const posOwners: Record<number, string> = {};

          const teamsFromServer = matchObj.teams ?? [];

          (matchObj.solveLog ?? []).forEach((entry: SolveLog) => {
            const key = `${entry.contestId}-${entry.index}`;
            const { displayName, teamKey } = resolveTeamDisplayAndKey(entry.team, teamsFromServer);
            // Bug #7 fix: use same string key format as poll handler so cells color on initial load
            solvedMap[key] = { team: teamKey };
            if (entry.problem && typeof entry.problem.position === "number") {
              posOwners[entry.problem.position] = teamKey;
            }

            const problemName = entry.problem?.name ?? `Problem ${entry.index}`;
            const contestAndIndex = `${entry.contestId}${entry.index}`;
            const solveTime = entry.timestamp;
            newLogEntries.push({
              key,
              message: `${displayName} solved ${problemName} (${contestAndIndex}) at ${formatTime(solveTime)}`,
              team: teamKey,
            });
          });
          setPositionOwners(posOwners);

          setLog((prev) => {
            const combined = [...newLogEntries, ...prev];
            const uniqueMap = new Map<string, LogEntry>();
            for (const e of combined) {
              if (!uniqueMap.has(e.key)) uniqueMap.set(e.key, e);
            }
            return Array.from(uniqueMap.values());
          });
          setSolved(solvedMap);
        } catch (e) {
          console.warn("Could not build solved/log from matchObj.solveLog", e);
        }
      } catch (err) {
        console.error("Error fetching match", err);
      } finally {
        setLoading(false);
      }
    };

    fetchMatch();
  }, [id]);

  useEffect(() => {
    if (!match) return;
    const dbGrid = match.gridSize;
    if (typeof dbGrid === "number" && [3, 4, 5, 6].includes(dbGrid)) {
      setGridSize(dbGrid as GridSize);
    } else {
      const len = match.problems?.length ?? problems.length ?? 25;
      const computed = [3, 4, 5, 6].find((n) => n * n === len) ?? 5;
      setGridSize(computed as GridSize);
    }
    const normalized = normalizeProblemsFromServer(match.problems || []);
    setProblems(normalized);
    setLoading(false);
  }, [match]);

  function resolveTeamDisplayAndKey(teamIdentifier: string | undefined, teamsListParam?: Team[]) {
    const teamsList = teamsListParam ?? (match?.teams ?? []);
    if (!teamIdentifier) return { displayName: "Unknown", teamKey: "unknown" };

    const search = teamIdentifier.toLowerCase();
    const teamObj = teamsList.find(
      (t) => (t.color ?? "").toLowerCase() === search || (t.name ?? "").toLowerCase() === search
    );

    const displayName = teamObj?.name ?? teamIdentifier;
    const teamKey = (teamObj?.color ?? teamIdentifier ?? "unknown").toLowerCase();

    return { displayName, teamKey };
  }

  useEffect(() => {
    if (matchLocked) return;
    if (!match?.id) return;

    const matchStart = new Date(match.startTime);
    const matchEnd = new Date(matchStart.getTime() + match.durationMinutes * 60 * 1000);

    const fetchPoll = async () => {
      const currentTime = new Date();
      if (currentTime < matchStart || currentTime > matchEnd) {
        return;
      }
      try {
        const pollRes = await apiClient.post(`/api/compete/matches/${match.id}/poll`);
        const pollData = pollRes.data;

        const oldlength = Array.isArray(match?.problems) ? match!.problems!.length : 0;
        const serverProblemsLen = Array.isArray(pollData?.problems) ? pollData.problems.length : 0;
        const serverSolveLen = Array.isArray(pollData?.solveLog) ? pollData.solveLog.length : 0;
        const localSolveLen = Array.isArray(match?.solveLog) ? match!.solveLog!.length : 0;

        const shouldApply = serverProblemsLen !== oldlength || serverSolveLen !== localSolveLen;

        if (pollData && shouldApply) {
          setMatch(pollData);

          const solvedMap: Record<string, SolvedInfo> = {};
          const newLogEntries: LogEntry[] = [];
          const posOwners: Record<number, string> = {};
          const problemUpdates: Record<string, { name?: string; rating?: number; contestId?: number; index?: string }> = {};

          const teamsFromServer = pollData.teams ?? [];
          (pollData.solveLog || []).forEach((entry: SolveLog) => {
            const key = `${entry.contestId}-${entry.index}`;
            const { displayName, teamKey } = resolveTeamDisplayAndKey(entry.team, teamsFromServer);
            solvedMap[key] = { team: teamKey };

            if (entry.problem && typeof entry.problem.position === "number") {
              posOwners[entry.problem.position] = teamKey;
            }

            if (entry.problem) {
              problemUpdates[key] = {
                name: entry.problem.name ?? undefined,
                rating: entry.problem.rating ?? undefined,
                contestId: entry.contestId,
                index: entry.index,
              };
            }

            const problemName = entry.problem?.name ?? `Problem ${entry.index}`;
            const contestAndIndex = `${entry.contestId}${entry.index}`;
            const solveTime = entry.timestamp;
            newLogEntries.push({
              key,
              message: `${displayName} solved ${problemName} (${contestAndIndex}) at ${formatTime(solveTime)}`,
              team: teamKey,
            });
          });

          if (pollData.problems && Array.isArray(pollData.problems)) {
            const normalized = normalizeProblemsFromServer(pollData.problems);
            setProblems(normalized);
          }

          setSolved(solvedMap);
          setPositionOwners(posOwners);

          setLog((prevLog) => {
            const combined = [...newLogEntries, ...prevLog];
            const uniqueMap = new Map<string, LogEntry>();
            for (const entry of combined) {
              if (!uniqueMap.has(entry.key)) uniqueMap.set(entry.key, entry);
            }
            const deduped = Array.from(uniqueMap.values());

            const prevMessages = new Set(prevLog.map((x) => x.message));
            newLogEntries.forEach((ne) => {
              if (!prevMessages.has(ne.message) && !notifiedRef.current.has(ne.message)) {
                notifyBrowser("Solve reported", ne.message);
                notifiedRef.current.add(ne.message);
              }
            });
            if (pollData.id) persistNotified(pollData.id);

            return deduped;
          });
        }
      } catch (err) {
        console.error("Polling submissions failed", err);
      }
    };

    const interval = setInterval(fetchPoll, 20000);
    return () => clearInterval(interval);
  }, [match?.id, match?.startTime, match?.durationMinutes, matchLocked]);

  useEffect(() => {
    const interval = setInterval(() => {
      setNow(new Date());
    }, 1000);
    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    if (!match) return;
    const start = new Date(match.startTime);
    const end = new Date(start.getTime() + match.durationMinutes * 60 * 1000);
    const currentTime = new Date();

    if (currentTime >= end) {
      setMatchLocked(true);
    }
  }, [match, now]);





  useEffect(() => {
    if (!problems || problems.length === 0) return;
    if (matchLocked) return;
    if (!match) return;
    
    const matchStart = new Date(match.startTime);
    const timeout = match.timeoutMinutes ?? 0;
    const timeStartWinner = new Date(matchStart.getTime() + timeout * 60 * 1000);
    const currentTime = new Date();
    if (currentTime < timeStartWinner) return;

    const w = findWinnerFromSolved(solved, problems, gridSize);
    if (w && !winner) {
      setWinner(w);
      setConfettiActive(true);
      setMatchLocked(true);
      const teamKey = (w.team ?? "").toLowerCase();
      const teamObj = match?.teams?.find(
        (t) => (t.color ?? "").toLowerCase() === teamKey || (t.name ?? "").toLowerCase() === teamKey
      );
      const displayName = teamObj?.name ?? teamKey ?? "Unknown";

      setLog((prev) => {
        const finalMsg = `${displayName} completed ${
          w.type === "row" ? "a row" : w.type === "col" ? "a column" : w.type === "diag" ? "the main diagonal" : "the anti-diagonal"
        } and won the match!`;
        notifyBrowser(`${displayName} won!`, finalMsg);
        return [{ message: finalMsg, team: w.team.toLowerCase(), key: "winner-msg" }, ...prev];
      });
      
      // Update duration locally
      setMatch((prev) => (prev ? { ...prev, durationMinutes: 1 } : prev));
      // Bug #10 fix: propagate win to server so all other clients see match end on next poll
      if (match?.id) {
        apiClient.patch(`/api/compete/matches/${match.id}/duration`, { durationMinutes: 1 }).catch(() => {});
      }
    }
  }, [solved, problems, winner, positionOwners, match, matchLocked, gridSize]);

  function findWinnerFromSolved(solvedMap: Record<string, SolvedInfo>, problemsArr: ProblemCell[], gSize: number): Winner {
    if (!problemsArr || problemsArr.length === 0) return null;

    const size = gSize;
    if (problemsArr.length !== size * size) return null;
    
    const ownerGrid: (string | null)[][] = Array.from({ length: size }, () => Array(size).fill(null));
    for (let i = 0; i < problemsArr.length; i++) {
      const r = Math.floor(i / size);
      const c = i % size;
      ownerGrid[r][c] = solvedMap[i]?.team ?? positionOwners[i] ?? null;
    }

    // rows
    for (let r = 0; r < size; r++) {
      const first = ownerGrid[r][0];
      if (first && ownerGrid[r].every((cell) => cell === first)) {
        const keys = Array.from({ length: size }, (_, c) => `${problemsArr[r * size + c].contestId}-${problemsArr[r * size + c].index}`);
        return { team: first, type: "row", index: r, keys };
      }
    }

    // columns
    for (let c = 0; c < size; c++) {
      const first = ownerGrid[0][c];
      if (first && ownerGrid.every((row) => row[c] === first)) {
        const keys = Array.from({ length: size }, (_, r) => `${problemsArr[r * size + c].contestId}-${problemsArr[r * size + c].index}`);
        return { team: first, type: "col", index: c, keys };
      }
    }

    // main diagonal
    const firstDiag = ownerGrid[0][0];
    if (firstDiag && ownerGrid.every((row, i) => row[i] === firstDiag)) {
      const keys = Array.from({ length: size }, (_, i) => `${problemsArr[i * size + i].contestId}-${problemsArr[i * size + i].index}`);
      return { team: firstDiag, type: "diag", index: 0, keys };
    }

    // anti-diagonal
    const firstAnti = ownerGrid[0][size - 1];
    if (firstAnti && ownerGrid.every((row, i) => row[size - 1 - i] === firstAnti)) {
      const keys = Array.from({ length: size }, (_, i) => `${problemsArr[i * size + (size - 1 - i)].contestId}-${problemsArr[i * size + (size - 1 - i)].index}`);
      return { team: firstAnti, type: "anti-diag", index: 1, keys };
    }

    return null;
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-[#111318] flex items-center justify-center">
        <svg className="animate-spin h-8 w-8 text-indigo-500" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
          <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
          <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z"></path>
        </svg>
      </div>
    );
  }

  if (!match) {
    return (
      <main className="min-h-screen bg-[#111318] p-6 text-white flex items-center justify-center">
        <div className="text-center rounded-2xl border border-white/[0.08] bg-white/[0.04] backdrop-blur-xl p-12">
          <h2 className="text-xl font-bold mb-2">Match Not Found</h2>
          <p className="text-white/60 text-sm">The match you are looking for does not exist or could not be loaded.</p>
        </div>
      </main>
    );
  }

  const matchStart = new Date(match.startTime);
  const matchEnd = new Date(matchStart.getTime() + match.durationMinutes * 60 * 1000);
  const currentTime = new Date();

  const matchHasStarted = currentTime >= matchStart;
  const matchHasEnded = currentTime >= matchEnd;
  const matchOngoing = matchHasStarted && !matchHasEnded && !matchLocked;

  return (
    <main className="min-h-screen bg-[#111318] text-white overflow-hidden flex flex-col items-center">
      {winner && confettiActive && <Confetti width={width} height={height} recycle={false} numberOfPieces={300} />}

      <header className="w-full border-b border-white/10 bg-white/5 backdrop-blur-md sticky top-0 z-20">
        <div className="max-w-6xl mx-auto flex justify-between items-center px-4 py-4">
          <Link href="/compete">
            <h1 className="text-2xl font-extrabold bg-gradient-to-r from-indigo-500 via-purple-500 to-pink-500 text-transparent bg-clip-text tracking-wide font-heading">
              Bingo CP
            </h1>
          </Link>
        </div>
      </header>

      <div className="text-center mt-8 mb-6">
        {!matchHasStarted && <CountdownToStart startTime={matchStart} />}
        {matchHasEnded && <p className="text-red-400 font-medium">Match has ended.</p>}
        {matchOngoing && match && (
          <p className="text-green-400 font-medium">
            Match ends in {formatDuration(matchEnd.getTime() - Date.now())}
          </p>
        )}
      </div>

      {matchHasStarted && (
        <div className="flex-1 w-full flex justify-center pb-24">
          <div className={`grid ${gridClasses[gridSize]} gap-3 justify-items-center`}>
            {problems.map((problem, idx) => {
              const key = `${problem.contestId}-${problem.index}`;
              const solvedInfo = solved[key] || solved[idx];
              const ownerTeam = solvedInfo?.team ?? positionOwners[problem.position ?? idx];
              const isWinningCell = winner?.keys?.includes(key);

              const cellStyle = ownerTeam
                ? (teamColors[ownerTeam] || "bg-white/10 border-white/20 text-white")
                : "bg-white/[0.04] border-white/[0.08] text-white/90 hover:bg-white/[0.08] hover:border-white/[0.15]";

              return (
                <div
                  key={key}
                  onClick={() => window.open(`https://codeforces.com/contest/${problem.contestId}/problem/${problem.index}`, "_blank")}
                  className={`relative w-28 h-28 sm:w-36 sm:h-32 p-3 flex flex-col justify-center items-center text-center rounded-2xl border backdrop-blur-md shadow-sm cursor-pointer transition-all duration-300 ${cellStyle} ${isWinningCell ? "ring-4 ring-yellow-400 scale-105 z-10 shadow-[0_0_30px_rgba(250,204,21,0.4)]" : "hover:-translate-y-1 hover:shadow-lg"}`}
                >
                  {showRatings && (
                    <div className="text-xs sm:text-sm font-semibold opacity-80 mb-1">
                      {problem.rating} - {problem.index}
                    </div>
                  )}
                  <div className={`text-xs sm:text-sm font-medium line-clamp-3 leading-snug ${showRatings ? "opacity-90" : ""}`}>
                    {problem.name}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {showLog && (
        <div className="fixed bottom-4 left-4 w-72 max-h-[40vh] sm:max-h-[80vh] overflow-y-auto rounded-2xl border border-white/10 bg-[#1a1d24]/90 backdrop-blur-xl p-4 shadow-xl z-30">
          <h2 className="text-base font-semibold mb-3 text-white">Solve Log</h2>
          {log.length === 0 ? (
            <p className="text-sm text-white/40">No solves yet</p>
          ) : (
            <ul className="space-y-3 text-sm">
              {log.map((entry, idx) => {
                const badgeColor = teamColors[entry.team]?.split(" ")[0] || "bg-white/10";
                return (
                  <li key={entry.key + idx} className="flex gap-2 items-start">
                    <span className={`inline-block mt-0.5 w-2 h-2 rounded-full shrink-0 ${badgeColor.replace('/30', '')}`}></span>
                    <span className="text-white/80 leading-tight">{entry.message}</span>
                  </li>
                );
              })}
            </ul>
          )}
        </div>
      )}
    </main>
  );
}
