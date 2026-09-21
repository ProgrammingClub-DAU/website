/* eslint-disable react-hooks/set-state-in-effect, react-hooks/exhaustive-deps, react-hooks/immutability */
"use client";

import { useEffect, useState, useRef } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import Confetti from "react-confetti";
import axios from 'axios';
import apiClient from '@/lib/axios';
import type { Match, ProblemCell, Team } from "@/components/compete/types";

type SolveLog = {
  handle?: string;
  team: string;
  problem: { contestId: number; index: string; name?: string; position?: number };
  timestamp?: string | number;
};

const teamColors: Record<string, string> = {
  red: "bg-red-500 text-white border-red-600",
  blue: "bg-blue-500 text-white border-blue-600",
  green: "bg-green-500 text-white border-green-600",
  purple: "bg-purple-500 text-white border-purple-600",
  orange: "bg-orange-500 text-white border-orange-600",
  pink: "bg-pink-500 text-white border-pink-600",
  yellow: "bg-yellow-500 text-white border-yellow-600",
  teal: "bg-teal-500 text-white border-teal-600",
};

type GridSize = 3 | 4 | 5 | 6;

const gridClasses = {
  3: "grid-cols-3",
  4: "grid-cols-4",
  5: "grid-cols-5",
  6: "grid-cols-6",
};

const gridWidths = {
  3: "max-w-2xl",
  4: "max-w-4xl",
  5: "max-w-5xl",
  6: "max-w-6xl",
};

type LogEntry = {
  key: string;
  message: string;
  team: string;
  timestamp: number;
};

type SolvedInfo = {
  team: string;
  handle?: string;
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
    } catch {
      // Ignored
    }
    return;
  }
  if (Notification.permission !== "denied") {
    Notification.requestPermission().then((permission) => {
      if (permission === "granted") {
        try {
          new Notification(title, { body });
        } catch {
          // Ignored
        }
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
      .map(([, p]) => p);
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
    return <p className="text-muted-foreground">Match is starting now...</p>;
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
  const problemsRef = useRef<ProblemCell[]>([]);
  const solvedRef = useRef<Record<string, SolvedInfo>>({});
  const matchRef = useRef<Match | null>(null);

  useEffect(() => { matchRef.current = match; }, [match]);

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
            const contestId = entry.problem?.contestId;
            const index = entry.problem?.index;
            if (!contestId || !index) return;
            
            const key = `${contestId}-${index}`;
            const { displayName, teamKey } = resolveTeamDisplayAndKey(entry.team, teamsFromServer);
            solvedMap[key] = { team: teamKey, handle: entry.handle };
            if (entry.problem && typeof entry.problem.position === "number") {
              posOwners[entry.problem.position] = teamKey;
            }

            const problemName = entry.problem?.name ?? `Problem ${index}`;
            const contestAndIndex = `${contestId}${index}`;
            const solveTime = entry.timestamp;
            const solverName = entry.handle ? `${entry.handle} (${displayName})` : displayName;
            newLogEntries.push({
              key,
              message: `${solverName} solved ${problemName} (${contestAndIndex}) at ${formatTime(solveTime)}`,
              team: teamKey,
              timestamp: solveTime ? new Date(solveTime).getTime() : Date.now(),
            });
          });
          setPositionOwners(posOwners);

          setLog((prev) => {
            const combined = [...newLogEntries, ...prev];
            const uniqueMap = new Map<string, LogEntry>();
            for (const e of combined) {
              if (!uniqueMap.has(e.key)) uniqueMap.set(e.key, e);
            }
            return Array.from(uniqueMap.values()).sort((a, b) => b.timestamp - a.timestamp);
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
    problemsRef.current = normalized;
    setLoading(false);
  }, [match]);

  // Keep refs in sync with state for use in polling callback
  useEffect(() => { solvedRef.current = solved; }, [solved]);

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

    let isPolling = false;

    const clientSidePoll = async () => {
      const currentMatch = matchRef.current;
      if (!currentMatch) return;

      const currentTime = new Date();
      if (currentTime < matchStart || currentTime > matchEnd) return;
      if (isPolling) return;
      isPolling = true;

      try {

      // Step 1: Poll Codeforces directly from the browser for each player
      const allHandles = (currentMatch.teams ?? []).flatMap((t) => t.members ?? []);
      const currentProblems = problemsRef.current;
      const currentSolved = solvedRef.current;
      const boardKeys = new Set(
        currentProblems.map((p) => `${p.contestId}-${p.index}`)
      );

      let reportedAnySolve = false;

      for (const handle of allHandles) {
        try {
          const cfRes = await axios.get(
            `https://codeforces.com/api/user.status?handle=${handle}&from=1&count=20`
          );
          if (cfRes.data.status !== "OK") continue;

          for (const sub of cfRes.data.result) {
            if (sub.verdict !== "OK") continue;
            if (!sub.problem?.contestId) continue;

            const key = `${sub.problem.contestId}-${sub.problem.index}`;
            if (!boardKeys.has(key)) continue; // Not on our board
            if (currentSolved[key]) continue; // Already solved

            // Check submission is after match start
            if (sub.creationTimeSeconds) {
              const subTime = new Date(sub.creationTimeSeconds * 1000);
              if (subTime < matchStart) continue;
            }

            // Report this solve to the backend
            try {
              await apiClient.post(`/api/compete/matches/${match.id}/report-solve`, {
                handle,
                contestId: sub.problem.contestId,
                index: sub.problem.index,
                creationTimeSeconds: sub.creationTimeSeconds,
              });
              reportedAnySolve = true;
            } catch (reportErr) {
              console.warn("Failed to report solve", reportErr);
            }
          }
        } catch (cfErr) {
          console.warn(`Failed to poll CF for ${handle}`, cfErr);
        }
        // Small delay to respect Codeforces API limit (5 requests per second)
        await new Promise(r => setTimeout(r, 200));
      }

      // Step 2: Fetch latest match state from backend (to sync across all clients)
      try {
        const pollRes = await apiClient.get(`/api/compete/matches/${match.id}`);
        const pollData = pollRes.data;

        const oldlength = Array.isArray(match?.problems) ? match!.problems!.length : 0;
        const serverProblemsLen = Array.isArray(pollData?.problems) ? pollData.problems.length : 0;
        const serverSolveLen = Array.isArray(pollData?.solveLog) ? pollData.solveLog.length : 0;
        const localSolveLen = Array.isArray(match?.solveLog) ? match!.solveLog!.length : 0;

        const shouldApply = serverProblemsLen !== oldlength || serverSolveLen !== localSolveLen || reportedAnySolve;

        if (pollData && shouldApply) {
          setMatch(pollData);

          const solvedMap: Record<string, SolvedInfo> = {};
          const newLogEntries: LogEntry[] = [];
          const posOwners: Record<number, string> = {};

          const teamsFromServer = pollData.teams ?? [];
          (pollData.solveLog || []).forEach((entry: SolveLog) => {
            const contestId = entry.problem?.contestId;
            const index = entry.problem?.index;
            if (!contestId || !index) return;

            const key = `${contestId}-${index}`;
            const { displayName, teamKey } = resolveTeamDisplayAndKey(entry.team, teamsFromServer);
            solvedMap[key] = { team: teamKey, handle: entry.handle };

            if (entry.problem && typeof entry.problem.position === "number") {
              posOwners[entry.problem.position] = teamKey;
            }

            const problemName = entry.problem?.name ?? `Problem ${index}`;
            const contestAndIndex = `${contestId}${index}`;
            const solveTime = entry.timestamp;
            const solverName = entry.handle ? `${entry.handle} (${displayName})` : displayName;
            newLogEntries.push({
              key,
              message: `${solverName} solved ${problemName} (${contestAndIndex}) at ${formatTime(solveTime)}`,
              team: teamKey,
              timestamp: solveTime ? new Date(solveTime).getTime() : Date.now(),
            });
          });

          if (pollData.problems && Array.isArray(pollData.problems)) {
            const normalized = normalizeProblemsFromServer(pollData.problems);
            setProblems(normalized);
            problemsRef.current = normalized;
          }

          setSolved(solvedMap);
          solvedRef.current = solvedMap;
          setPositionOwners(posOwners);

          setLog((prevLog) => {
            const combined = [...newLogEntries, ...prevLog];
            const uniqueMap = new Map<string, LogEntry>();
            for (const entry of combined) {
              if (!uniqueMap.has(entry.key)) uniqueMap.set(entry.key, entry);
            }
            const deduped = Array.from(uniqueMap.values()).sort((a, b) => b.timestamp - a.timestamp);

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
        console.error("Failed to fetch match state", err);
      }
      } finally {
        isPolling = false;
      }
    };

    const interval = setInterval(clientSidePoll, 20000);
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
        return [{ message: finalMsg, team: w.team.toLowerCase(), key: "winner-msg", timestamp: Date.now() }, ...prev];
      });
      
      // Update duration locally
      setMatch((prev) => (prev ? { ...prev, durationMinutes: 1 } : prev));
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
      <div className="min-h-screen bg-background flex items-center justify-center">
        <svg className="animate-spin h-8 w-8 text-indigo-500" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
          <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
          <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z"></path>
        </svg>
      </div>
    );
  }

  if (!match) {
    return (
      <main className="min-h-screen bg-background p-6 text-foreground flex items-center justify-center">
        <div className="text-center rounded-2xl border border-border bg-card p-12 shadow-sm">
          <h2 className="text-xl font-bold mb-2">Match Not Found</h2>
          <p className="text-muted-foreground text-sm">The match you are looking for does not exist or could not be loaded.</p>
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

  const scoreboard = (() => {
    const scores: Record<string, { name: string; count: number }> = {};
    (match.teams || []).forEach((t) => {
      scores[t.color] = { name: t.name, count: 0 };
    });
    problems.forEach((problem, idx) => {
      const key = `${problem.contestId}-${problem.index}`;
      const ownerTeam = solved[key]?.team ?? positionOwners[problem.position ?? idx];
      if (ownerTeam && scores[ownerTeam]) {
        scores[ownerTeam].count++;
      }
    });
    return Object.entries(scores).sort((a, b) => b[1].count - a[1].count);
  })();

  return (
    <main className="min-h-screen bg-background text-foreground overflow-x-hidden flex flex-col items-center">
      {winner && confettiActive && (
        <>
          <Confetti width={width} height={height} recycle={false} numberOfPieces={300} />
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm transition-opacity">
            <div className={`p-8 rounded-2xl shadow-2xl border text-center animate-in zoom-in-95 duration-300 ${teamColors[winner.team] || "bg-card text-foreground"}`}>
              <h2 className="text-4xl font-extrabold mb-4 drop-shadow-md">
                🏆 Winner!
              </h2>
              <p className="text-xl font-semibold opacity-90">
                Team {match?.teams?.find(t => t.color.toLowerCase() === winner.team.toLowerCase())?.name || winner.team} won the match!
              </p>
              <button
                onClick={() => setConfettiActive(false)}
                className="mt-6 px-6 py-2 rounded-full bg-white/20 hover:bg-white/30 text-white font-medium transition-colors"
              >
                Close
              </button>
            </div>
          </div>
        </>
      )}

      <header className="w-full border-b border-border bg-card/50 backdrop-blur-md sticky top-0 z-20">
        <div className="max-w-7xl mx-auto flex justify-between items-center px-4 py-4">
          <Link href="/compete" className="flex items-center gap-2">
            <span className="font-extrabold text-2xl bg-clip-text text-transparent bg-gradient-to-r from-indigo-500 to-pink-500 font-heading">
              Bingo CP
            </span>
          </Link>
          <nav className="flex items-center gap-6 hidden sm:flex">
            <Link href="/compete" className="text-sm font-medium text-muted-foreground hover:text-foreground transition-colors">Home</Link>
            <Link href="/compete/create" className="text-sm font-medium text-foreground">ICPC Mode</Link>
            <Link href="/compete/ioi" className="text-sm font-medium text-muted-foreground hover:text-foreground transition-colors">IOI Mode</Link>
            <Link href="/compete/help" className="text-sm font-medium text-muted-foreground hover:text-foreground transition-colors">Help</Link>
          </nav>
        </div>
      </header>

      <div className="text-center mt-8 mb-6">
        {!matchHasStarted && <CountdownToStart startTime={matchStart} />}
        {matchHasEnded && <p className="text-destructive font-medium">Match has ended.</p>}
        {matchOngoing && match && (
          <p className="text-green-500 font-medium">
            Match ends in {formatDuration(matchEnd.getTime() - now.getTime())}
          </p>
        )}
      </div>

      {matchHasStarted && (
        <div className="flex flex-wrap justify-center gap-2 mb-6 px-4">
          {scoreboard.map(([color, data]) => {
            const teamStyle = teamColors[color] || "bg-secondary text-foreground border-border";
            return (
              <div key={color} className={`px-3 py-1.5 rounded-full text-xs font-semibold shadow-sm border ${teamStyle}`}>
                {data.name}: {data.count}
              </div>
            );
          })}
        </div>
      )}


      {matchHasStarted && (
        <div className="flex-1 w-full flex justify-center pb-32 px-4">
          <div className={`w-full ${gridWidths[gridSize as keyof typeof gridWidths] || 'max-w-5xl'} mx-auto`}>
            <div className={`grid ${gridClasses[gridSize as keyof typeof gridClasses]} gap-2 sm:gap-3 justify-items-center`}>
              {problems.map((problem, idx) => {
                const key = `${problem.contestId}-${problem.index}`;
                const solvedInfo = solved[key] || solved[idx];
                const ownerTeam = solvedInfo?.team ?? positionOwners[problem.position ?? idx];
                const isWinningCell = winner?.keys?.includes(key);

                const cellStyle = ownerTeam
                  ? (teamColors[ownerTeam] || "bg-secondary border-border text-foreground")
                  : "bg-card border-border text-foreground hover:bg-accent hover:border-accent-foreground";

                return (
                  <div
                    key={key}
                    onClick={() => window.open(`https://codeforces.com/contest/${problem.contestId}/problem/${problem.index}`, "_blank")}
                    className={`relative w-full aspect-square p-2 sm:p-3 flex flex-col justify-center items-center text-center rounded-xl border backdrop-blur-md shadow-sm cursor-pointer transition-all duration-300 ${cellStyle} ${isWinningCell ? "ring-4 ring-yellow-400 scale-105 z-10 shadow-[0_0_30px_rgba(250,204,21,0.4)]" : "hover:-translate-y-1 hover:shadow-lg"}`}
                  >
                    {showRatings && (
                      <div className="text-[10px] sm:text-xs font-semibold opacity-80 mb-1">
                        {problem.rating} - {problem.index}
                      </div>
                    )}
                    <div className={`text-xs sm:text-sm font-medium line-clamp-3 leading-snug ${showRatings ? "opacity-90" : ""}`}>
                      {problem.name}
                    </div>
                    {(solvedInfo?.handle || problem.claimedBy) && (
                      <div className="absolute bottom-1 right-2 text-[9px] sm:text-[10px] opacity-75 font-semibold">
                        ✓ {solvedInfo?.handle || problem.claimedBy}
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      )}

      {/* Floating Log Toggle */}
      <button
        onClick={() => setShowLog(!showLog)}
        className="fixed bottom-4 left-4 z-40 bg-card border border-border px-4 py-2 rounded-full shadow-md text-sm font-medium hover:bg-accent transition-colors"
      >
        {showLog ? "Hide Log" : "Show Log"}
      </button>

      {/* Solve Log Panel */}
      {showLog && (
        <div className="fixed bottom-16 left-4 w-72 max-h-[40vh] sm:max-h-[70vh] overflow-y-auto rounded-2xl border border-border bg-card/95 backdrop-blur-xl p-4 shadow-xl z-30">
          <h2 className="text-base font-semibold mb-3 text-foreground">Solve Log</h2>
          {log.length === 0 ? (
            <p className="text-sm text-muted-foreground">No solves yet</p>
          ) : (
            <ul className="space-y-2 text-sm">
              {log.map((entry, idx) => {
                const teamStyle = teamColors[entry.team] || "bg-secondary text-foreground border-border";
                return (
                  <li key={entry.key + idx} className={`px-3 py-2 rounded-lg border shadow-sm ${teamStyle}`}>
                    <span className="leading-tight break-words">{entry.message}</span>
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
