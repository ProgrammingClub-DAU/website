"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import axios from 'axios';
import apiClient from '@/lib/axios';
import { TeamsForm, TeamInput } from "./teams-form";

export function MatchCreationForm() {
  const router = useRouter();
  const [teams, setTeams] = useState<TeamInput[]>([]);

  const today = new Date().toISOString().split("T")[0]; // YYYY-MM-DD

  const [date, setDate] = useState(today);
  const [time, setTime] = useState("13:00"); // 24-hour format
  const [duration, setDuration] = useState("1:00");
  const [minRating, setMinRating] = useState(800);
  const [maxRating, setMaxRating] = useState(1600);
  const [selectedMode, setSelectedMode] = useState<"classic" | "replace">("classic");
  const [selectedGridSize, setSelectedGridSize] = useState<number>(5);
  const [replaceIncrement, setReplaceIncrement] = useState<number>(100);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [timeoutMinutes, setTimeoutMinutes] = useState("0:00");
  const [showRatings, setShowRatings] = useState<boolean>(true);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (teams.length < 2) {
      alert("At least 2 teams are required.");
      return;
    }
    const usedColors = new Set();
    for (const team of teams) {
      if (!team.name.trim()) {
        alert("Each team must have a name.");
        return;
      }
      if (!team.color || usedColors.has(team.color)) {
        alert("Each team must have a unique color.");
        return;
      }
      usedColors.add(team.color);

      if (team.members.length < 1 || team.members.length > 16) {
        alert("Each team must have 1 to 16 members.");
        return;
      }
      if (team.members.some((m) => !m.trim())) {
        alert("All member handles must be filled.");
        return;
      }
      for (const member of team.members) {
        try {
          const res = await axios.get(`https://codeforces.com/api/user.info?handles=${member}`);
          if (res.data.status !== "OK") {
            alert(`Invalid handle: ${member}`);
            return;
          }
        } catch {
          alert(`Failed to validate handle: ${member}`);
          return;
        }
      }
    }

    const allHandles = teams.flatMap((team) => team.members.map((h) => h.trim().toLowerCase()));
    const uniqueHandles = new Set(allHandles);
    if (uniqueHandles.size !== allHandles.length) {
      alert("Each Codeforces handle must be unique across all teams.");
      return;
    }

    const teamNames = teams.map((t) => t.name.trim().toLowerCase());
    const uniqueNames = new Set(teamNames);
    if (uniqueNames.size !== teamNames.length) {
      alert("Each team must have a unique name.");
      return;
    }

    const [hours, minutes] = duration.split(":").map(Number);
    if (isNaN(hours) || isNaN(minutes)) {
      alert("Invalid duration format. Please use hh:mm.");
      return;
    }

    let timeoutMinutesValue = null;
    if (timeoutMinutes) {
      const [hoursT, minutesT] = timeoutMinutes.split(":").map(Number);
      if (isNaN(hoursT) || isNaN(minutesT)) {
        alert("Invalid timeout format. Please use hh:mm.");
        return;
      }
      timeoutMinutesValue = hoursT * 60 + minutesT;
    }

    if (isNaN(minRating) || isNaN(maxRating) || isNaN(replaceIncrement)) {
      alert("Invalid rating or increment format.");
      return;
    }
    if (!Number.isInteger(minRating) || !Number.isInteger(maxRating) || !Number.isInteger(replaceIncrement)) {
      alert("Ratings and increments must be integers.");
      return;
    }
    if (maxRating < 800 || maxRating > 3500 || minRating < 800 || minRating > 3500) {
      alert("Ratings must be in the range [800, 3500].");
      return;
    }
    if (replaceIncrement < 100 || replaceIncrement > 2700) {
      alert("Replace value must be in the range [100, 2700].");
      return;
    }
    if (minRating % 100 !== 0 || maxRating % 100 !== 0 || replaceIncrement % 100 !== 0) {
      alert("Ratings and increments must be in steps of 100.");
      return;
    }
    if (maxRating < minRating) {
      alert("Maximum rating must be greater than or equal to minimum rating.");
      return;
    }

    const durationMinutes = hours * 60 + minutes;
    if (durationMinutes > 420) {
      alert("Duration cannot exceed 7 hours (420 minutes).");
      return;
    }

    const startTime = new Date(`${date}T${time}`);
    if (isNaN(startTime.getTime())) {
      alert("Invalid start time.");
      return;
    }
    if (startTime.getTime() < Date.now()) {
      alert("Match cannot start in the past.");
      return;
    }

    const matchData = {
      startTime: startTime.toISOString(),
      durationMinutes,
      minRating,
      maxRating,
      mode: selectedMode,
      gridSize: selectedGridSize,
      replaceIncrement: selectedMode === "replace" ? replaceIncrement : undefined,
      teams: teams,
      timeoutMinutes: timeoutMinutesValue,
      problems: [],
      solveLog: [],
      showRatings,
    };

    setIsSubmitting(true);
    try {
      const res = await apiClient.post("/api/compete/matches", matchData);
      const created = res.data;
      const newMatchId = created?.id ?? created?.match?.id;
      if (!newMatchId) {
        alert("Could not create match, no id returned");
        setIsSubmitting(false);
        return;
      }
      router.push(`/compete/${newMatchId}`);
    } catch (err) {
      console.error("Error creating match", err);
      alert("Error creating match: " + (axios.isAxiosError(err) && err.response?.data?.message ? err.response.data.message : (err as Error).message));
      setIsSubmitting(false);
    }
  };

  const inputClass = "flex h-10 w-full rounded-md border border-white/10 bg-white/5 px-3 py-2 text-sm text-white placeholder:text-white/50 focus:outline-none focus:ring-2 focus:ring-white/20 focus:border-transparent";
  const labelClass = "text-sm font-medium text-white/90 mb-1 block";

  return (
    <form onSubmit={handleSubmit} className="flex flex-col lg:flex-row gap-8 w-full">
      <div className="flex-1 w-full">
        <h3 className="text-xl font-bold text-white mb-4">Teams</h3>
        <TeamsForm onTeamsChange={setTeams} />
      </div>

      <div className="flex-1 w-full">
        <div className="rounded-2xl border border-white/[0.08] bg-white/[0.04] backdrop-blur-xl p-6 space-y-4">
          <h3 className="text-xl font-bold text-white mb-4">Match Options</h3>
          
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className={labelClass}>Date</label>
              <input
                type="date"
                value={date}
                onChange={(e) => setDate(e.target.value)}
                className={inputClass}
                required
              />
            </div>
            <div>
              <label className={labelClass}>Time</label>
              <input
                type="time"
                value={time}
                onChange={(e) => setTime(e.target.value)}
                className={inputClass}
                step="60"
                required
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className={labelClass}>Duration (hh:mm)</label>
              <input
                type="text"
                placeholder="1:30"
                value={duration}
                onChange={(e) => setDuration(e.target.value)}
                className={inputClass}
                pattern="^\d+:\d{2}$"
                required
              />
            </div>
            <div>
              <label className={labelClass}>Timeout (hh:mm)</label>
              <input
                type="text"
                placeholder="0:00"
                value={timeoutMinutes}
                onChange={(e) => setTimeoutMinutes(e.target.value)}
                className={inputClass}
                pattern="^\d+:\d{2}$"
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className={labelClass}>Min Rating</label>
              <input
                type="text"
                value={minRating}
                onChange={(e) => setMinRating(Number(e.target.value))}
                className={inputClass}
                pattern="[0-9]+"
                required
              />
            </div>
            <div>
              <label className={labelClass}>Max Rating</label>
              <input
                type="text"
                value={maxRating}
                onChange={(e) => setMaxRating(Number(e.target.value))}
                className={inputClass}
                pattern="[0-9]+"
                required
              />
            </div>
          </div>
          
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className={labelClass}>Game Mode</label>
              <select
                value={selectedMode}
                onChange={(e) => setSelectedMode(e.target.value as "classic" | "replace")}
                className={inputClass + " bg-[#1a1d24]"}
              >
                <option value="classic">Classic</option>
                <option value="replace">Replace</option>
              </select>
            </div>
            <div>
              <label className={labelClass}>Grid Size</label>
              <select
                value={selectedGridSize}
                onChange={(e) => setSelectedGridSize(parseInt(e.target.value))}
                className={inputClass + " bg-[#1a1d24]"}
              >
                {[3, 4, 5, 6].map((size) => (
                  <option key={size} value={size}>
                    {size} x {size}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {selectedMode === "replace" && (
            <div>
              <label className={labelClass}>Replace Increment</label>
              <input
                type="text"
                value={replaceIncrement}
                onChange={(e) => setReplaceIncrement(Number(e.target.value))}
                className={inputClass}
                pattern="[0-9]+"
                required
              />
            </div>
          )}

          <div>
            <label className={labelClass}>Show Problem Ratings</label>
            <div className="inline-flex rounded-md shadow-sm">
              <button
                type="button"
                onClick={() => setShowRatings(true)}
                className={`px-4 py-2 text-sm font-medium rounded-l-md border border-white/10 ${showRatings ? 'bg-indigo-600 text-white' : 'bg-white/5 text-white/70 hover:bg-white/10'}`}
              >
                Show
              </button>
              <button
                type="button"
                onClick={() => setShowRatings(false)}
                className={`px-4 py-2 text-sm font-medium rounded-r-md border border-white/10 border-l-0 ${!showRatings ? 'bg-indigo-600 text-white' : 'bg-white/5 text-white/70 hover:bg-white/10'}`}
              >
                Hide
              </button>
            </div>
          </div>

          <div className="pt-4 mt-4 border-t border-white/10">
            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full inline-flex justify-center items-center gap-2 px-6 py-3 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-semibold shadow-lg transition-colors disabled:opacity-70 disabled:cursor-not-allowed"
            >
              {isSubmitting ? (
                <>
                  <svg className="animate-spin h-5 w-5" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z"></path>
                  </svg>
                  Creating...
                </>
              ) : (
                "Create Match"
              )}
            </button>
          </div>
        </div>
      </div>
    </form>
  );
}
