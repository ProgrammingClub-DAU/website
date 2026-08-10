"use client";

/**
 * Profile Dashboard Page
 *
 * This is a client component because it uses interactive libraries (recharts, react-activity-calendar).
 * It receives data from a server-side wrapper and renders:
 * - Profile header with avatar, name (colored by CF rank), club role, and key stats
 * - Club Activity & Event Performance (stats summary, achievements, event list with filters)
 * - GitHub-style activity heat map (green dots for DSA practice days)
 * - Contest rating graph over time (recharts line chart)
 * - Platform-wise breakdown of problems solved
 */

import { useState, useMemo } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { rankColor, CF_RANKS } from "@/lib/cf-ranks";
import type { Profile, EventParticipation, ClubEventType } from "@/types/api";
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  CartesianGrid,
} from "recharts";
import dynamic from "next/dynamic";
const ActivityCalendar = dynamic(
  () => import("react-activity-calendar").then((mod) => mod.ActivityCalendar),
  { ssr: false }
);
import {
  User,
  Trophy,
  Code,
  Calendar,
  Award,
  Target,
  ChevronDown,
  Zap,
  BookOpen,
  Swords,
  Crown,
  Star,
} from "lucide-react";
import Image from "next/image";

// ── Rank color helper for rating values (Codeforces thresholds) ──
function ratingToRankName(rating: number): string {
  if (rating >= 2400) return "Grandmaster";
  if (rating >= 2100) return "Master";
  if (rating >= 1900) return "Candidate Master";
  if (rating >= 1600) return "Expert";
  if (rating >= 1400) return "Specialist";
  if (rating >= 1200) return "Pupil";
  return "Newbie";
}

// ── Event type icon and color ──
function getEventTypeIcon(type: ClubEventType) {
  switch (type) {
    case "Contest": return <Swords className="size-4" />;
    case "Workshop": return <BookOpen className="size-4" />;
    case "ICPC": return <Crown className="size-4" />;
    case "Flagship": return <Star className="size-4" />;
    default: return <Zap className="size-4" />;
  }
}

function getEventTypeColor(type: ClubEventType): string {
  switch (type) {
    case "Contest": return "var(--cf-expert)";
    case "Workshop": return "var(--cf-pupil)";
    case "ICPC": return "var(--cf-master)";
    case "Flagship": return "var(--cf-grandmaster)";
    default: return "var(--cf-specialist)";
  }
}

// ── Club role styling ──
function getClubRoleBadgeStyle(role: string): { bg: string; text: string; border: string } {
  switch (role) {
    case "Convenor":
      return { bg: "rgba(255,215,0,0.1)", text: "#ffd700", border: "rgba(255,215,0,0.3)" };
    case "Deputy Convenor":
      return { bg: "rgba(192,192,192,0.1)", text: "#c0c0c0", border: "rgba(192,192,192,0.3)" };
    case "Core Member":
      return { bg: "rgba(138,43,226,0.1)", text: "#aa6dff", border: "rgba(138,43,226,0.3)" };
    case "Associate Core Member":
      return { bg: "rgba(30,144,255,0.1)", text: "#5ba3ff", border: "rgba(30,144,255,0.3)" };
    case "Batch Representative":
      return { bg: "rgba(0,206,209,0.1)", text: "#40e0d0", border: "rgba(0,206,209,0.3)" };
    default:
      return { bg: "rgba(128,128,128,0.08)", text: "var(--fg-muted)", border: "var(--border)" };
  }
}

// ── Generate achievements from event participations ──
function generateAchievements(events: EventParticipation[]): { icon: string; label: string }[] {
  const achievements: { icon: string; label: string }[] = [];

  // Top finishes
  events.forEach((e) => {
    if (e.achievement) {
      achievements.push({ icon: e.achievement.split(" ")[0], label: `${e.eventName} \u2014 ${e.achievement.substring(e.achievement.indexOf(" ") + 1)}` });
    }
  });

  // Milestone badges
  const totalEvents = events.length;
  if (totalEvents >= 10) achievements.push({ icon: "\ud83c\udfaf", label: "Participated in 10+ Club Events" });
  else if (totalEvents >= 5) achievements.push({ icon: "\ud83c\udfaf", label: "Participated in 5+ Club Events" });

  const contests = events.filter((e) => e.eventType === "Contest" || e.eventType === "Flagship" || e.eventType === "ICPC");
  const top3Count = contests.filter((e) => e.rank !== null && e.rank <= 3).length;
  if (top3Count >= 3) achievements.push({ icon: "\ud83c\udfc6", label: "Top 3 Finisher \u2014 3+ Contests" });

  return achievements;
}

// ── Filter type ──
type EventFilter = "All" | "Contest" | "Workshop" | "ICPC" | "Flagship" | "Other";
const EVENT_FILTERS: EventFilter[] = ["All", "Contest", "Workshop", "ICPC", "Flagship"];

const EVENTS_PER_PAGE = 5;

export default function ProfileDashboard({ profile }: { profile: Profile }) {
  // ── State ──
  const [eventFilter, setEventFilter] = useState<EventFilter>("All");
  const [showAllEvents, setShowAllEvents] = useState(false);

  // Resolve the CF rank color using the existing utility
  const getCfRank = (rating: number): import("@/lib/cf-ranks").CfRankKey => {
    if (rating >= 2400) return "grandmaster";
    if (rating >= 2100) return "master";
    if (rating >= 1900) return "candidate";
    if (rating >= 1600) return "expert";
    if (rating >= 1400) return "specialist";
    if (rating >= 1200) return "pupil";
    return "newbie";
  };
  const actualCfRank = getCfRank(profile.rating);
  const nameColor = rankColor(actualCfRank);
  const rankName = CF_RANKS.find((r) => r.key === actualCfRank)?.name ?? actualCfRank;
  const totalSolved = profile.platformStats?.reduce((acc, curr) => acc + curr.solved, 0) || 0;

  // ── Club activity computed values ──
  const eventParticipations = useMemo(() => profile.eventParticipations ?? [], [profile.eventParticipations]);
  const sortedEvents = useMemo(() =>
    [...eventParticipations].sort((a, b) => new Date(b.eventDate).getTime() - new Date(a.eventDate).getTime()),
    [eventParticipations]
  );

  const filteredEvents = useMemo(() => {
    if (eventFilter === "All") return sortedEvents;
    return sortedEvents.filter((e) => e.eventType === eventFilter);
  }, [sortedEvents, eventFilter]);

  const visibleEvents = showAllEvents ? filteredEvents : filteredEvents.slice(0, EVENTS_PER_PAGE);

  const clubStats = useMemo(() => {
    const contests = eventParticipations.filter((e) => e.eventType === "Contest" || e.eventType === "Flagship" || e.eventType === "ICPC");
    const workshops = eventParticipations.filter((e) => e.eventType === "Workshop");
    const rankedEvents = eventParticipations.filter((e) => e.rank !== null);
    const bestRank = rankedEvents.length > 0 ? Math.min(...rankedEvents.map((e) => e.rank!)) : null;
    const achievements = generateAchievements(eventParticipations);
    return {
      totalEvents: eventParticipations.length,
      totalContests: contests.length,
      totalWorkshops: workshops.length,
      bestRank,
      achievementCount: achievements.length,
      achievements,
    };
  }, [eventParticipations]);

  const clubRoleStyle = getClubRoleBadgeStyle(profile.clubRole ?? "Club Participant");
  const isOfficialMember = profile.clubRole && profile.clubRole !== "Club Participant";

  return (
    <div className="space-y-8">
      {/* ── Profile Header ── */}
      <Card>
        <CardContent className="pt-6">
          <div className="flex flex-col items-center gap-6 sm:flex-row sm:items-start">
            {/* Avatar */}
            <div
              className="flex size-24 shrink-0 items-center justify-center rounded-full border-2 bg-surface-2"
              style={{ borderColor: nameColor }}
            >
              {profile.avatarUrl ? (
                <Image
                  src={profile.avatarUrl}
                  alt={profile.name}
                  width={96}
                  height={96}
                  className="size-full rounded-full object-cover"
                />
              ) : (
                <User className="size-10 text-fg-muted" />
              )}
            </div>

            {/* Name + handle + rank + club role */}
            <div className="flex-1 text-center sm:text-left">
              <h2
                className="text-2xl font-bold tracking-tight"
                style={{ color: nameColor }}
              >
                {profile.name}
              </h2>
              <p className="mt-1 text-sm text-fg-muted">@{profile.codeforcesHandle}</p>
              <div className="mt-3 flex flex-wrap items-center justify-center gap-3 sm:justify-start">
                <span
                  className="inline-flex items-center gap-1.5 rounded-full border px-3 py-1 text-xs font-medium capitalize"
                  style={{ color: nameColor, borderColor: nameColor }}
                >
                  <Trophy className="size-3" />
                  {rankName}
                </span>
                {/* Club role badge */}
                <span
                  className="inline-flex items-center gap-1.5 rounded-full border px-3 py-1 text-xs font-medium"
                  style={{
                    color: clubRoleStyle.text,
                    borderColor: clubRoleStyle.border,
                    backgroundColor: clubRoleStyle.bg,
                  }}
                >
                  {isOfficialMember ? <Award className="size-3" /> : <Target className="size-3" />}
                  {profile.clubRole ?? "Club Participant"}
                </span>
                <span className="text-sm text-fg-muted">
                  Rating: <strong className="text-foreground">{profile.rating}</strong>
                  <span className="text-fg-subtle"> (max {profile.maxRating})</span>
                </span>
              </div>
            </div>

            {/* Quick stats cards */}
            <div className="grid grid-cols-2 gap-3 text-center">
              <div className="rounded-panel border border-border bg-surface-2 px-4 py-3">
                <div className="text-2xl font-bold">{totalSolved}</div>
                <div className="text-[11px] text-fg-muted">Total Solved</div>
              </div>
              <div className="rounded-panel border border-border bg-surface-2 px-4 py-3">
                <div className="text-2xl font-bold">{profile.ratingHistory?.length || 0}</div>
                <div className="text-[11px] text-fg-muted">Contests</div>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* ── Club Stats Summary ── */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-base">
            <Zap className="size-4" />
            Club Stats
          </CardTitle>
        </CardHeader>
        <CardContent>
          {eventParticipations.length > 0 ? (
            <div className="grid grid-cols-2 gap-4 sm:grid-cols-5">
              <div className="rounded-panel border border-border bg-surface-2 p-4 text-center transition-all hover:-translate-y-0.5 hover:border-hairline-strong">
                <div className="text-2xl font-bold">{clubStats.totalEvents}</div>
                <div className="mt-1 text-xs text-fg-muted">Events Participated</div>
              </div>
              <div className="rounded-panel border border-border bg-surface-2 p-4 text-center transition-all hover:-translate-y-0.5 hover:border-hairline-strong">
                <div className="text-2xl font-bold">{clubStats.totalContests}</div>
                <div className="mt-1 text-xs text-fg-muted">Contests</div>
              </div>
              <div className="rounded-panel border border-border bg-surface-2 p-4 text-center transition-all hover:-translate-y-0.5 hover:border-hairline-strong">
                <div className="text-2xl font-bold">{clubStats.totalWorkshops}</div>
                <div className="mt-1 text-xs text-fg-muted">Workshops</div>
              </div>
              <div className="rounded-panel border border-border bg-surface-2 p-4 text-center transition-all hover:-translate-y-0.5 hover:border-hairline-strong">
                <div className="text-2xl font-bold">{clubStats.bestRank !== null ? `#${clubStats.bestRank}` : "\u2014"}</div>
                <div className="mt-1 text-xs text-fg-muted">Best Rank</div>
              </div>
              <div className="rounded-panel border border-border bg-surface-2 p-4 text-center transition-all hover:-translate-y-0.5 hover:border-hairline-strong">
                <div className="text-2xl font-bold">{clubStats.achievementCount}</div>
                <div className="mt-1 text-xs text-fg-muted">Achievements</div>
              </div>
            </div>
          ) : (
            <p className="py-6 text-center text-sm text-fg-muted">
              No club activities yet. Participate in an event to see your stats here.
            </p>
          )}
        </CardContent>
      </Card>

      {/* ── Achievements ── */}
      {clubStats.achievements.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-base">
              <Award className="size-4" />
              Achievements
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid gap-3 sm:grid-cols-2">
              {clubStats.achievements.map((a, i) => (
                <div
                  key={i}
                  className="flex items-center gap-3 rounded-panel border border-border bg-surface-2 px-4 py-3 transition-all hover:-translate-y-0.5 hover:border-hairline-strong"
                >
                  <span className="text-xl">{a.icon}</span>
                  <span className="text-sm font-medium">{a.label}</span>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}

      {/* ── Club Activity & Event Performance ── */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-base">
            <Calendar className="size-4" />
            Club Activity & Event Performance
          </CardTitle>
        </CardHeader>
        <CardContent>
          {eventParticipations.length > 0 ? (
            <>
              {/* Filter tabs */}
              <div className="mb-6 flex flex-wrap gap-2">
                {EVENT_FILTERS.map((filter) => (
                  <button
                    key={filter}
                    onClick={() => { setEventFilter(filter); setShowAllEvents(false); }}
                    className="rounded-full border px-3 py-1.5 text-xs font-medium transition-all"
                    style={{
                      borderColor: eventFilter === filter ? "var(--primary)" : "var(--border)",
                      backgroundColor: eventFilter === filter ? "rgba(138,43,226,0.12)" : "transparent",
                      color: eventFilter === filter ? "var(--primary)" : "var(--fg-muted)",
                    }}
                  >
                    {filter === "All" ? "All Events" : filter}
                  </button>
                ))}
              </div>

              {/* Event cards */}
              {filteredEvents.length > 0 ? (
                <div className="space-y-3">
                  {visibleEvents.map((event) => (
                    <div
                      key={event.eventId}
                      className="flex items-center gap-4 rounded-panel border border-border bg-surface-2 p-4 transition-all hover:-translate-y-0.5 hover:border-hairline-strong"
                    >
                      {/* Event type icon */}
                      <div
                        className="flex size-10 shrink-0 items-center justify-center rounded-full border"
                        style={{
                          borderColor: getEventTypeColor(event.eventType),
                          color: getEventTypeColor(event.eventType),
                          backgroundColor: `color-mix(in srgb, ${getEventTypeColor(event.eventType)} 10%, transparent)`,
                        }}
                      >
                        {getEventTypeIcon(event.eventType)}
                      </div>

                      {/* Event info */}
                      <div className="min-w-0 flex-1">
                        <div className="flex items-center gap-2">
                          <span className="truncate text-sm font-semibold">{event.eventName}</span>
                          {event.achievement && (
                            <span className="shrink-0 text-sm">{event.achievement.split(" ")[0]}</span>
                          )}
                        </div>
                        <div className="mt-1 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-fg-muted">
                          <span>{new Date(event.eventDate).toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric" })}</span>
                          <span
                            className="rounded-full border px-2 py-0.5 text-[10px] font-medium"
                            style={{
                              borderColor: getEventTypeColor(event.eventType),
                              color: getEventTypeColor(event.eventType),
                            }}
                          >
                            {event.eventType}
                          </span>
                        </div>
                      </div>

                      {/* Rank / status */}
                      <div className="shrink-0 text-right">
                        {event.rank !== null ? (
                          <>
                            <div className="text-lg font-bold" style={{ color: event.rank <= 3 ? "var(--cf-master)" : "var(--foreground)" }}>
                              #{event.rank}
                            </div>
                            <div className="text-[10px] text-fg-muted">/ {event.totalParticipants}</div>
                          </>
                        ) : (
                          <div className="text-xs font-medium text-fg-muted">Participated</div>
                        )}
                      </div>
                    </div>
                  ))}

                  {/* Show more button */}
                  {filteredEvents.length > EVENTS_PER_PAGE && !showAllEvents && (
                    <button
                      onClick={() => setShowAllEvents(true)}
                      className="flex w-full items-center justify-center gap-1.5 rounded-panel border border-border py-3 text-xs font-medium text-fg-muted transition-all hover:border-hairline-strong hover:text-foreground"
                    >
                      <ChevronDown className="size-3.5" />
                      Show {filteredEvents.length - EVENTS_PER_PAGE} more events
                    </button>
                  )}
                </div>
              ) : (
                <p className="py-6 text-center text-sm text-fg-muted">
                  No {eventFilter.toLowerCase()} events found.
                </p>
              )}
            </>
          ) : (
            <p className="py-6 text-center text-sm text-fg-muted">
              No club activities yet. Participate in an event to see your activity here.
            </p>
          )}
        </CardContent>
      </Card>

      {/* ── Platform Stats ── */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-base">
            <Code className="size-4" />
            Problems Solved by Platform
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
            {profile.platformStats?.map((ps) => (
              <div
                key={ps.platform}
                className="rounded-panel border border-border bg-surface-2 p-4 text-center transition-all hover:-translate-y-0.5 hover:border-hairline-strong"
              >
                <div className="text-2xl font-bold">{ps.solved}</div>
                <div className="mt-1 text-xs text-fg-muted">{ps.platform}</div>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>

      {/* ── Rating Graph ── */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-base">
            <Trophy className="size-4" />
            Contest Rating History
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="h-[300px] w-full">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={profile.ratingHistory}>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--hairline)" />
                <XAxis
                  dataKey="date"
                  tick={{ fontSize: 11, fill: "var(--fg-muted)" }}
                  tickFormatter={(v: string) => {
                    const d = new Date(v);
                    return d.toLocaleDateString("en-US", { month: "short", year: "2-digit" });
                  }}
                />
                <YAxis
                  domain={["dataMin - 100", "dataMax + 100"]}
                  tick={{ fontSize: 11, fill: "var(--fg-muted)" }}
                />
                <Tooltip
                  contentStyle={{
                    backgroundColor: "var(--surface-2)",
                    border: "1px solid var(--border)",
                    borderRadius: "8px",
                    fontSize: "12px",
                  }}
                  labelFormatter={(label: unknown) => {
                    if (!label) return "";
                    const d = new Date(label as string | number);
                    return d.toLocaleDateString("en-US", { month: "long", day: "numeric", year: "numeric" });
                  }}
                  formatter={(value: unknown, _name: unknown, props: { payload?: { contestName?: string } }) => {
                    if (value == null) return ["", ""];
                    const contestName = props.payload?.contestName ?? "";
                    return [
                      `${value} (${ratingToRankName(Number(value))})`,
                      contestName,
                    ];
                  }}
                />
                <Line
                  type="monotone"
                  dataKey="rating"
                  stroke="var(--primary)"
                  strokeWidth={2}
                  dot={{ r: 4, fill: "var(--primary)" }}
                  activeDot={{ r: 6 }}
                />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </CardContent>
      </Card>

      {/* ── Activity Heat Map (GitHub-style green dots) ── */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-base">
            <Calendar className="size-4" />
            Practice Activity
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="overflow-x-auto">
            <ActivityCalendar
              data={profile.activityData}
              theme={{
                dark: ["#161b22", "#0e4429", "#006d32", "#26a641", "#39d353"],
                light: ["#ebedf0", "#9be9a8", "#40c463", "#30a14e", "#216e39"],
              }}
              labels={{
                totalCount: "{{count}} problems solved in the last year",
              }}
              blockSize={12}
              blockMargin={3}
              fontSize={12}
            />
          </div>
        </CardContent>
      </Card>

      {/* ── Account Info ── */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Account Details</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3 text-sm">
          <div className="flex justify-between">
            <span className="text-fg-muted">Email</span>
            <span>{profile.email}</span>
          </div>
          <Separator />
          <div className="flex justify-between">
            <span className="text-fg-muted">Codeforces Handle</span>
            <span style={{ color: nameColor }}>@{profile.codeforcesHandle}</span>
          </div>
          <Separator />
          <div className="flex justify-between">
            <span className="text-fg-muted">Member Since</span>
            <span>{new Date(profile.createdAt).toLocaleDateString("en-US", { month: "long", year: "numeric" })}</span>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
