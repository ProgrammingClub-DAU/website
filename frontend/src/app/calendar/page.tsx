"use client";

import React, { useEffect, useState, useMemo } from "react";
import {
  Calendar as CalendarIcon,
  ExternalLink,
  Clock,
  Copy,
  Check,
} from "lucide-react";
import { calendarService } from "@/lib/services/calendar";
import { AddToCalendar } from "@/components/site/add-to-calendar";
import type { CalendarItemDto } from "@/types/api";
import { serverDate } from "@/lib/utils";

type PlatformFilter = "ALL" | "CODEFORCES" | "LEETCODE" | "CODECHEF" | "ATCODER" | "CLUB_EVENT";

const PLATFORM_FILTERS: { key: PlatformFilter; label: string; color: string }[] = [
  { key: "ALL", label: "All Platforms", color: "text-white" },
  { key: "CLUB_EVENT", label: "DAU Events", color: "text-amber-400" },
  { key: "CODEFORCES", label: "Codeforces", color: "text-blue-400" },
  { key: "LEETCODE", label: "LeetCode", color: "text-yellow-400" },
  { key: "CODECHEF", label: "CodeChef", color: "text-amber-600" },
  { key: "ATCODER", label: "AtCoder", color: "text-slate-300" },
];

export default function CalendarPage() {
  const [selectedPlatform, setSelectedPlatform] = useState<PlatformFilter>("ALL");
  const [items, setItems] = useState<CalendarItemDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [copiedFeed, setCopiedFeed] = useState(false);

  useEffect(() => {
    let ignore = false;
    const now = new Date();
    const from = new Date(now.getTime() - 24 * 60 * 60 * 1000).toISOString();
    const to = new Date(now.getTime() + 30 * 24 * 60 * 60 * 1000).toISOString();

    setLoading(true);
    calendarService
      .getRange(from, to)
      .then((res) => {
        if (!ignore) setItems(res || []);
      })
      .catch((err) => {
        console.error("Failed to load calendar events:", err);
      })
      .finally(() => {
        if (!ignore) setLoading(false);
      });

    return () => {
      ignore = true;
    };
  }, []);

  const filteredItems = useMemo(() => {
    if (selectedPlatform === "ALL") return items;
    if (selectedPlatform === "CLUB_EVENT") {
      return items.filter((item) => item.type === "CLUB_EVENT");
    }
    return items.filter(
      (item) => item.platform?.toUpperCase() === selectedPlatform
    );
  }, [items, selectedPlatform]);

  // Group items by date string in IST
  const groupedByDate = useMemo(() => {
    const map = new Map<string, CalendarItemDto[]>();
    for (const item of filteredItems) {
      const dateKey = serverDate(item.startsAt).toLocaleDateString("en-IN", {
        timeZone: "Asia/Kolkata",
        weekday: "short",
        day: "numeric",
        month: "short",
        year: "numeric",
      });
      if (!map.has(dateKey)) map.set(dateKey, []);
      map.get(dateKey)!.push(item);
    }
    return Array.from(map.entries());
  }, [filteredItems]);

  const handleCopyFeed = () => {
    const feedUrl = calendarService.getClubIcsUrl();
    navigator.clipboard.writeText(feedUrl);
    setCopiedFeed(true);
    setTimeout(() => setCopiedFeed(false), 2000);
  };

  return (
    <div className="container mx-auto max-w-6xl px-4 py-8 space-y-10">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div className="space-y-3">
          <div className="inline-flex items-center gap-1.5 rounded-full border border-primary/30 bg-primary/10 px-3 py-1 text-xs font-mono uppercase tracking-caps text-primary">
            <CalendarIcon className="size-3.5" />
            Schedule
          </div>
          <h1 className="text-3xl sm:text-4xl font-black tracking-tight text-white">
            Contest & Event Calendar
          </h1>
          <p className="text-sm text-fg-muted max-w-2xl">
            Upcoming competitive programming rounds from Codeforces, LeetCode, CodeChef, AtCoder, and official DAU club workshops.
          </p>
        </div>

        {/* Subscribe Action */}
        <div className="flex items-center gap-2">
          <button
            onClick={handleCopyFeed}
            className="inline-flex items-center gap-1.5 rounded-xl border border-white/10 bg-surface-2 px-4 py-2.5 text-xs font-mono font-medium text-foreground hover:bg-surface-3 transition-colors shadow-xs"
          >
            {copiedFeed ? (
              <Check className="size-3.5 text-emerald-400" />
            ) : (
              <Copy className="size-3.5 text-fg-muted" />
            )}
            {copiedFeed ? "Feed URL Copied!" : "Subscribe (.ics Feed)"}
          </button>
        </div>
      </div>

      {/* Platform Filter Buttons */}
      <div className="flex flex-wrap items-center gap-2 border-b border-white/10 pb-4">
        {PLATFORM_FILTERS.map((f) => {
          const isActive = selectedPlatform === f.key;
          return (
            <button
              key={f.key}
              onClick={() => setSelectedPlatform(f.key)}
              className={`rounded-xl px-3.5 py-1.5 text-xs font-mono font-semibold transition-all ${
                isActive
                  ? "bg-primary text-primary-foreground shadow-md shadow-primary/20"
                  : "border border-white/10 bg-white/[0.02] text-fg-muted hover:border-white/20 hover:text-white"
              }`}
            >
              {f.label}
            </button>
          );
        })}
      </div>

      {/* Events Stream */}
      {loading ? (
        <div className="rounded-2xl border border-white/10 bg-[#0d0d16]/70 p-16 text-center font-mono text-xs text-fg-muted animate-pulse">
          Fetching upcoming contest schedule...
        </div>
      ) : groupedByDate.length === 0 ? (
        <div className="rounded-2xl border border-white/10 bg-[#0d0d16]/70 p-16 text-center font-mono text-xs text-fg-muted">
          No upcoming contests or events found for the selected filter.
        </div>
      ) : (
        <div className="space-y-8">
          {groupedByDate.map(([dateString, dateItems]) => (
            <div key={dateString} className="space-y-3">
              <div className="sticky top-20 z-10 py-1.5 px-3 rounded-lg bg-surface/90 backdrop-blur-md border border-white/5 inline-block">
                <span className="text-xs font-mono font-bold uppercase tracking-caps text-primary">
                  {dateString}
                </span>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {dateItems.map((item) => {
                  const startTime = serverDate(item.startsAt).toLocaleTimeString("en-IN", {
                    timeZone: "Asia/Kolkata",
                    hour: "2-digit",
                    minute: "2-digit",
                  });
                  const isClub = item.type === "CLUB_EVENT";
                  const platformName = isClub ? "DAU Event" : item.platform || "Contest";

                  return (
                    <div
                      key={item.id}
                      className="group rounded-2xl border border-white/10 bg-[#0d0d16]/80 p-5 backdrop-blur-md hover:border-white/20 transition-all flex flex-col justify-between shadow-lg"
                    >
                      <div className="space-y-3">
                        <div className="flex items-center justify-between gap-2">
                          <span
                            className={`text-nano font-mono font-bold px-2 py-0.5 rounded-full border ${
                              isClub
                                ? "bg-amber-500/15 text-amber-300 border-amber-500/30"
                                : item.platform === "CODEFORCES"
                                ? "bg-blue-500/15 text-blue-300 border-blue-500/30"
                                : item.platform === "LEETCODE"
                                ? "bg-yellow-500/15 text-yellow-300 border-yellow-500/30"
                                : "bg-white/10 text-white border-white/20"
                            }`}
                          >
                            {platformName}
                          </span>
                          <span className="text-micro font-mono text-fg-muted flex items-center gap-1">
                            <Clock className="size-3" />
                            {startTime} IST
                          </span>
                        </div>

                        <div>
                          <h3 className="text-base font-bold text-white group-hover:text-primary transition-colors">
                            {item.title}
                          </h3>
                        </div>
                      </div>

                      <div className="mt-5 pt-3 border-t border-white/5 flex flex-wrap items-center justify-between gap-2">
                        <AddToCalendar
                          eventId={isClub ? item.id.replace("event-", "") : undefined}
                          title={item.title}
                          startDateIso={serverDate(item.startsAt).toISOString()}
                        />

                        {item.url && (
                          <a
                            href={item.url}
                            target="_blank"
                            rel="noreferrer"
                            className="inline-flex items-center gap-1 text-xs font-mono text-fg-muted hover:text-white transition-colors"
                          >
                            Details
                            <ExternalLink className="size-3.5" />
                          </a>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
