"use client";

/**
 * Profile Dashboard Page
 *
 * This is a client component because it uses interactive libraries (recharts, react-activity-calendar).
 * It receives data from a server-side wrapper and renders:
 * - Profile header with avatar, name (colored by CF rank), club role, social links, and key stats
 * - Practice Activity heat map + Current/Max Streak
 * - Contest rating graph over time (recharts line chart)
 * - Achievements (auto-generated + user-managed manual achievements)
 * - Club Activity & Event Performance with IPC-Contests / Other Competitions filters
 * - Platform-wise breakdown with official logos
 */

import { useState, useMemo, useEffect, useCallback } from "react";
import { dashboardService } from "@/lib/services/dashboard";
import { useAuthStore } from "@/store/auth";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { rankColor, CF_RANKS } from "@/lib/cf-ranks";
import type { Profile, EventParticipation, ClubEventType, ManualAchievement } from "@/types/api";
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
import { useRouter } from "next/navigation";
import {
  User,
  Trophy,
  Code,
  Calendar,
  Award,
  Target,
  ChevronDown,
  Swords,
  Crown,
  Star,
  ExternalLink,
  Plus,
  Pencil,
  Trash2,
  X,
  Flame,
  Save,
  LogOut,
} from "lucide-react";
import Image from "next/image";

// ── Platform Icons ──
function CodeforcesIcon({ className = "size-5" }: { className?: string }) {
  return <Image src="/codeforces-logo.png" alt="Codeforces" width={24} height={24} className={`object-contain ${className}`} />;
}
function LeetCodeIcon({ className = "size-5" }: { className?: string }) {
  return <Image src="/leetcode-logo.png" alt="LeetCode" width={24} height={24} className={`object-contain ${className}`} />;
}
function CodeChefIcon({ className = "size-5" }: { className?: string }) {
  return <Image src="/codechef-logo.png" alt="CodeChef" width={24} height={24} className={`object-contain ${className}`} />;
}
function AtCoderIcon({ className = "size-5" }: { className?: string }) {
  return <Image src="/atcoder-logo.png" alt="AtCoder" width={24} height={24} className={`object-contain ${className}`} />;
}

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
    case "ICPC": return <Crown className="size-4" />;
    case "Flagship": return <Star className="size-4" />;
    default: return <Calendar className="size-4" />;
  }
}

function getEventTypeColor(type: ClubEventType): string {
  switch (type) {
    case "Contest": return "var(--cf-expert)";
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
  events.forEach((e) => {
    if (e.achievement) {
      achievements.push({ icon: e.achievement.split(" ")[0], label: `${e.eventName} \u2014 ${e.achievement.substring(e.achievement.indexOf(" ") + 1)}` });
    }
  });
  const totalEvents = events.length;
  if (totalEvents >= 10) achievements.push({ icon: "\ud83c\udfaf", label: "Participated in 10+ Club Events" });
  else if (totalEvents >= 5) achievements.push({ icon: "\ud83c\udfaf", label: "Participated in 5+ Club Events" });
  const contests = events.filter((e) => e.eventType === "Contest" || e.eventType === "Flagship" || e.eventType === "ICPC");
  const top3Count = contests.filter((e) => e.rank !== null && e.rank <= 3).length;
  if (top3Count >= 3) achievements.push({ icon: "\ud83c\udfc6", label: "Top 3 Finisher \u2014 3+ Contests" });
  return achievements;
}

// ── Streak calculator ──
function calculateStreaks(activityData: { date: string; count: number }[]): { current: number; max: number } {
  if (!activityData || activityData.length === 0) return { current: 0, max: 0 };
  const sorted = [...activityData].sort((a, b) => a.date.localeCompare(b.date));
  let maxStreak = 0;
  let currentStreak = 0;
  let tempStreak = 0;
  for (let i = 0; i < sorted.length; i++) {
    if (sorted[i].count > 0) {
      tempStreak++;
      if (tempStreak > maxStreak) maxStreak = tempStreak;
    } else {
      tempStreak = 0;
    }
  }
  // Current streak: count backwards from the last day
  for (let i = sorted.length - 1; i >= 0; i--) {
    if (sorted[i].count > 0) {
      currentStreak++;
    } else {
      break;
    }
  }
  return { current: currentStreak, max: maxStreak };
}

// ── Filter type ──
type EventFilter = "IPC - Contests" | "Other Competitions";
const EVENT_FILTERS: EventFilter[] = ["IPC - Contests", "Other Competitions"];

const EVENTS_PER_PAGE = 5;

// ── Modal Overlay component ──
function ModalOverlay({ open, onClose, title, children }: { open: boolean; onClose: () => void; title: string; children: React.ReactNode }) {
  if (!open) return null;
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm" onClick={onClose}>
      <div
        className="relative w-full max-w-lg max-h-[90vh] overflow-y-auto rounded-panel border border-border bg-surface-2 p-6 shadow-xl mx-4"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between mb-5">
          <h3 className="text-lg font-bold tracking-tight">{title}</h3>
          <button onClick={onClose} className="rounded-full p-1.5 text-fg-muted hover:bg-surface-3 hover:text-foreground transition-colors">
            <X className="size-5" />
          </button>
        </div>
        {children}
      </div>
    </div>
  );
}

// ── Main Entry ──
export default function ProfileDashboard() {
  const [profile, setProfile] = useState<Profile | null>(null);
  const [loading, setLoading] = useState(true);
  const { user } = useAuthStore();

  const userId = user?.id || "1";

  const loadProfile = useCallback(async () => {
    try {
      const data = await dashboardService.getProfile(userId);
      setProfile(data);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  }, [userId]);

  useEffect(() => {
    let isMounted = true;
    dashboardService.getProfile(userId).then((data) => {
      if (isMounted) {
        setProfile(data);
        setLoading(false);
      }
    }).catch((e) => {
      console.error(e);
      if (isMounted) setLoading(false);
    });
    return () => { isMounted = false; };
  }, [userId]);

  if (loading) {
    return <div className="animate-pulse flex flex-col items-center justify-center min-h-[400px] text-fg-muted font-mono text-sm tracking-wider uppercase">Loading profile data...</div>;
  }

  if (!profile) {
    return <div className="flex flex-col items-center justify-center min-h-[400px] text-fg-muted font-mono text-sm tracking-wider uppercase">Please log in to view your profile.</div>;
  }

  return <ProfileDashboardContent profile={profile} userId={userId} onProfileUpdate={loadProfile} />;
}

function ProfileDashboardContent({ profile, userId, onProfileUpdate }: { profile: Profile; userId: string; onProfileUpdate: () => void }) {
  const router = useRouter();
  const { logout } = useAuthStore();

  const handleLogout = () => {
    logout();
    router.push("/");
  };
  // ── State ──
  const [eventFilter, setEventFilter] = useState<EventFilter>("IPC - Contests");
  const [showAllEvents, setShowAllEvents] = useState(false);
  const [editProfileOpen, setEditProfileOpen] = useState(false);
  const [addAchievementOpen, setAddAchievementOpen] = useState(false);
  const [editingAchievement, setEditingAchievement] = useState<ManualAchievement | null>(null);
  const [saving, setSaving] = useState(false);

  // Resolve the CF rank color
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

  // Total Solved = Codeforces + LeetCode only
  const totalSolved = useMemo(() => {
    if (!profile.platformStats) return 0;
    return profile.platformStats
      .filter((ps) => ps.platform === "Codeforces" || ps.platform === "LeetCode")
      .reduce((acc, curr) => acc + (curr.solved || 0), 0);
  }, [profile.platformStats]);

  // Streaks
  const streaks = useMemo(() => calculateStreaks(profile.activityData), [profile.activityData]);

  // Platform cards
  const platformCards = useMemo(() => {
    const statsMap = new Map(profile.platformStats?.map((ps) => [ps.platform, ps]) || []);
    const cfStat = statsMap.get("Codeforces");
    const lcStat = statsMap.get("LeetCode");
    const ccStat = statsMap.get("CodeChef");
    const acStat = statsMap.get("AtCoder");

    const cfHandle = cfStat?.handle || profile.cpHandles?.codeforces || profile.codeforcesHandle;
    const lcHandle = lcStat?.handle || profile.cpHandles?.leetcode;
    const ccHandle = ccStat?.handle || profile.cpHandles?.codechef;
    const acHandle = acStat?.handle || profile.cpHandles?.atcoder;

    return [
      {
        name: "Codeforces",
        icon: <CodeforcesIcon className="size-6" />,
        handle: cfHandle,
        url: cfHandle ? `https://codeforces.com/profile/${encodeURIComponent(cfHandle)}` : null,
        showSolved: true,
        solved: cfStat?.solved,
        rating: cfStat?.rating ?? profile.rating,
        maxRating: cfStat?.maxRating ?? profile.maxRating,
      },
      {
        name: "LeetCode",
        icon: <LeetCodeIcon className="size-6" />,
        handle: lcHandle,
        url: lcHandle ? `https://leetcode.com/u/${encodeURIComponent(lcHandle)}/` : null,
        showSolved: true,
        solved: lcStat?.solved,
        rating: lcStat?.rating,
      },
      {
        name: "CodeChef",
        icon: <CodeChefIcon className="size-6" />,
        handle: ccHandle,
        url: ccHandle ? `https://www.codechef.com/users/${encodeURIComponent(ccHandle)}` : null,
        showSolved: false,
        rating: ccStat?.rating,
      },
      {
        name: "AtCoder",
        icon: <AtCoderIcon className="size-6" />,
        handle: acHandle,
        url: acHandle ? `https://atcoder.jp/users/${encodeURIComponent(acHandle)}` : null,
        showSolved: false,
        rating: acStat?.rating,
      },
    ];
  }, [profile]);

  // ── Club activity computed values ──
  const eventParticipations = useMemo(() => profile.eventParticipations ?? [], [profile.eventParticipations]);
  const sortedEvents = useMemo(() =>
    [...eventParticipations].sort((a, b) => new Date(b.eventDate).getTime() - new Date(a.eventDate).getTime()),
    [eventParticipations]
  );

  const filteredEvents = useMemo(() => {
    if (eventFilter === "IPC - Contests") {
      return sortedEvents.filter((e) => e.eventType === "Contest" || e.eventType === "ICPC" || e.eventType === "Flagship");
    }
    return sortedEvents.filter((e) => e.eventType !== "Contest" && e.eventType !== "ICPC" && e.eventType !== "Flagship");
  }, [sortedEvents, eventFilter]);

  const visibleEvents = showAllEvents ? filteredEvents : filteredEvents.slice(0, EVENTS_PER_PAGE);

  const autoAchievements = useMemo(() => generateAchievements(eventParticipations), [eventParticipations]);
  const manualAchievements = profile.manualAchievements ?? [];

  const clubRoleStyle = getClubRoleBadgeStyle(profile.clubRole ?? "Club Participant");
  const isOfficialMember = profile.clubRole && profile.clubRole !== "Club Participant";

  // Social links
  const socialLinks = profile.socialLinks ?? {};
  const hasSocials = socialLinks.github || socialLinks.linkedin || socialLinks.instagram;

  // ── Achievement handlers ──
  const handleSaveAchievement = async (ach: ManualAchievement) => {
    setSaving(true);
    const existing = [...manualAchievements];
    const idx = existing.findIndex((a) => a.id === ach.id);
    if (idx >= 0) {
      existing[idx] = ach;
    } else {
      existing.push(ach);
    }
    await dashboardService.updateProfile(userId, { manualAchievements: existing });
    onProfileUpdate();
    setSaving(false);
    setAddAchievementOpen(false);
    setEditingAchievement(null);
  };

  const handleDeleteAchievement = async (id: string) => {
    setSaving(true);
    const filtered = manualAchievements.filter((a) => a.id !== id);
    await dashboardService.updateProfile(userId, { manualAchievements: filtered });
    onProfileUpdate();
    setSaving(false);
  };

  // ── Edit Profile handler ──
  const handleSaveProfile = async (data: {
    name: string;
    socialLinks: { github?: string; linkedin?: string; instagram?: string };
    cpHandles: { codeforces?: string; leetcode?: string; codechef?: string; atcoder?: string };
  }) => {
    setSaving(true);
    await dashboardService.updateProfile(userId, {
      name: data.name,
      socialLinks: data.socialLinks,
      cpHandles: data.cpHandles,
    });
    onProfileUpdate();
    setSaving(false);
    setEditProfileOpen(false);
  };

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
                <Image src={profile.avatarUrl} alt={profile.name} width={96} height={96} className="size-full rounded-full object-cover" />
              ) : (
                <User className="size-10 text-fg-muted" />
              )}
            </div>

            {/* Name + handle + rank + club role + social links */}
            <div className="flex-1 text-center sm:text-left">
              <div className="flex items-center justify-center gap-2 sm:justify-start">
                <h2 className="text-2xl font-bold tracking-tight" style={{ color: nameColor }}>
                  {profile.name}
                </h2>
                <button
                  onClick={() => setEditProfileOpen(true)}
                  className="rounded-full p-1.5 text-fg-muted hover:bg-surface-3 hover:text-primary transition-colors"
                  title="Edit Profile"
                >
                  <Pencil className="size-3.5" />
                </button>
              </div>
              <a
                href={profile.codeforcesHandle ? `https://codeforces.com/profile/${encodeURIComponent(profile.codeforcesHandle)}` : "#"}
                target="_blank"
                rel="noopener noreferrer"
                className="mt-1 inline-flex items-center gap-1.5 text-sm text-fg-muted hover:text-primary hover:underline transition-colors"
              >
                <span>@{profile.codeforcesHandle}</span>
                <ExternalLink className="size-3" />
              </a>

              {/* Social Links */}
              {hasSocials && (
                <div className="mt-2 flex items-center justify-center gap-2 sm:justify-start">
                  {socialLinks.github && (
                    <a href={socialLinks.github} target="_blank" rel="noopener noreferrer" title="GitHub"
                      className="flex size-8 items-center justify-center rounded-full border border-border bg-surface-2 hover:bg-surface-3 hover:border-hairline-strong transition-all">
                      <Image src="/github-logo.png" alt="GitHub" width={18} height={18} className="object-contain invert" />
                    </a>
                  )}
                  {socialLinks.linkedin && (
                    <a href={socialLinks.linkedin} target="_blank" rel="noopener noreferrer" title="LinkedIn"
                      className="flex size-8 items-center justify-center rounded-full border border-border bg-surface-2 hover:bg-surface-3 hover:border-hairline-strong transition-all">
                      <Image src="/linkedin-logo.avif" alt="LinkedIn" width={18} height={18} className="object-contain" />
                    </a>
                  )}
                  {socialLinks.instagram && (
                    <a href={socialLinks.instagram} target="_blank" rel="noopener noreferrer" title="Instagram"
                      className="flex size-8 items-center justify-center rounded-full border border-border bg-surface-2 hover:bg-surface-3 hover:border-hairline-strong transition-all">
                      <Image src="/instagram-logo.avif" alt="Instagram" width={18} height={18} className="object-contain" />
                    </a>
                  )}
                </div>
              )}

              <div className="mt-3 flex flex-wrap items-center justify-center gap-3 sm:justify-start">
                <span
                  className="inline-flex items-center gap-1.5 rounded-full border px-3 py-1 text-xs font-medium capitalize"
                  style={{ color: nameColor, borderColor: nameColor }}
                >
                  <Trophy className="size-3" />
                  {rankName}
                </span>
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

            {/* Quick stats cards & Logout */}
            <div className="flex flex-col items-center gap-3 sm:items-end">
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

              <button
                onClick={handleLogout}
                className="inline-flex items-center gap-1.5 rounded-full border border-red-500/30 bg-red-500/10 px-4 py-1.5 text-xs font-semibold text-red-400 hover:bg-red-500/20 hover:border-red-500/50 transition-all"
                title="Log out of your account"
              >
                <LogOut className="size-3.5" />
                Log Out
              </button>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* ── Competitive Programming Profiles ── */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-base">
            <Code className="size-4" />
            Competitive Programming Profiles
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {platformCards.map((pc) => {
              const hasUrl = Boolean(pc.url);
              return (
                <a
                  key={pc.name}
                  href={pc.url || "#"}
                  target={hasUrl ? "_blank" : undefined}
                  rel={hasUrl ? "noopener noreferrer" : undefined}
                  onClick={(e) => { if (!hasUrl) e.preventDefault(); }}
                  className={`group relative flex flex-col justify-between rounded-panel border border-border bg-surface-2 p-5 transition-all ${hasUrl
                    ? "hover:-translate-y-1 hover:border-hairline-strong hover:shadow-panel hover:bg-surface-3/40"
                    : "opacity-75 cursor-default"
                    }`}
                >
                  <div>
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2.5">
                        <div className="flex size-9 shrink-0 items-center justify-center rounded-lg border border-border/60 bg-background/80 p-1.5 shadow-xs">
                          {pc.icon}
                        </div>
                        <span className="font-semibold text-sm tracking-tight text-foreground">{pc.name}</span>
                      </div>
                      {hasUrl && (
                        <ExternalLink className="size-4 text-fg-muted opacity-0 transition-opacity group-hover:opacity-100 group-hover:text-primary" />
                      )}
                    </div>
                    <div className="mt-4">
                      <div className="text-[10px] font-mono text-fg-muted uppercase tracking-wider">Handle / Profile</div>
                      <div className="mt-0.5 text-sm font-semibold font-mono text-foreground truncate group-hover:text-primary transition-colors">
                        {pc.handle ? `@${pc.handle}` : "\u2014"}
                      </div>
                    </div>
                  </div>

                  {/* Statistics */}
                  <div className="mt-5 pt-3 border-t border-border/50 grid grid-cols-2 gap-2 text-xs">
                    {pc.showSolved ? (
                      <>
                        <div>
                          <div className="text-[10px] text-fg-muted uppercase tracking-wider">Solved</div>
                          <div className="font-bold text-foreground font-mono mt-0.5">{pc.solved != null ? pc.solved : "\u2014"}</div>
                        </div>
                        <div>
                          <div className="text-[10px] text-fg-muted uppercase tracking-wider">Rating</div>
                          <div className="font-bold text-foreground font-mono mt-0.5">
                            {pc.rating != null ? (
                              <span>
                                {pc.rating}
                                {pc.maxRating != null && (
                                  <span className="text-[10px] font-normal text-fg-subtle"> (max {pc.maxRating})</span>
                                )}
                              </span>
                            ) : "\u2014"}
                          </div>
                        </div>
                      </>
                    ) : (
                      <div className="col-span-2 text-center">
                        <div className="text-[10px] text-fg-muted uppercase tracking-wider">Contest Rating</div>
                        <div className="font-bold text-foreground font-mono mt-1 text-lg">{pc.rating != null ? pc.rating : "\u2014"}</div>
                      </div>
                    )}
                  </div>
                </a>
              );
            })}
          </div>
        </CardContent>
      </Card>

      {/* ── Practice Activity + Streaks ── */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-base">
            <Calendar className="size-4" />
            Practice Activity
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex flex-col gap-6 lg:flex-row lg:items-start">
            <div className="flex-1 overflow-x-auto">
              <ActivityCalendar
                data={profile.activityData}
                theme={{
                  dark: ["#161b22", "#0e4429", "#006d32", "#26a641", "#39d353"],
                  light: ["#ebedf0", "#9be9a8", "#40c463", "#30a14e", "#216e39"],
                }}
                labels={{ totalCount: "{{count}} problems solved in the last year" }}
                blockSize={12}
                blockMargin={3}
                fontSize={12}
              />
            </div>
            {/* Streak Cards */}
            <div className="flex flex-row gap-3 lg:flex-col lg:min-w-[140px] shrink-0">
              <div className="flex-1 rounded-panel border border-border bg-surface-2 p-4 text-center transition-all hover:-translate-y-0.5 hover:border-hairline-strong">
                <Flame className="size-5 text-orange-400 mx-auto mb-1" />
                <div className="text-2xl font-bold text-orange-400">{streaks.current}</div>
                <div className="mt-0.5 text-[10px] text-fg-muted uppercase tracking-wider">Current Streak</div>
              </div>
              <div className="flex-1 rounded-panel border border-border bg-surface-2 p-4 text-center transition-all hover:-translate-y-0.5 hover:border-hairline-strong">
                <Trophy className="size-5 text-amber-400 mx-auto mb-1" />
                <div className="text-2xl font-bold text-amber-400">{streaks.max}</div>
                <div className="mt-0.5 text-[10px] text-fg-muted uppercase tracking-wider">Max Streak</div>
              </div>
            </div>
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
                <YAxis domain={["dataMin - 100", "dataMax + 100"]} tick={{ fontSize: 11, fill: "var(--fg-muted)" }} />
                <Tooltip
                  contentStyle={{ backgroundColor: "var(--surface-2)", border: "1px solid var(--border)", borderRadius: "8px", fontSize: "12px" }}
                  labelFormatter={(label: unknown) => {
                    if (!label) return "";
                    const d = new Date(label as string | number);
                    return d.toLocaleDateString("en-US", { month: "long", day: "numeric", year: "numeric" });
                  }}
                  formatter={(value: unknown, _name: unknown, props: { payload?: { contestName?: string } }) => {
                    if (value == null) return ["", ""];
                    const contestName = props.payload?.contestName ?? "";
                    return [`${value} (${ratingToRankName(Number(value))})`, contestName];
                  }}
                />
                <Line type="monotone" dataKey="rating" stroke="var(--primary)" strokeWidth={2} dot={{ r: 4, fill: "var(--primary)" }} activeDot={{ r: 6 }} />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </CardContent>
      </Card>

      {/* ── Achievements (Auto + Manual) ── */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle className="flex items-center gap-2 text-base">
              <Award className="size-4" />
              Achievements
            </CardTitle>
            <button
              onClick={() => { setEditingAchievement(null); setAddAchievementOpen(true); }}
              className="inline-flex items-center gap-1.5 rounded-full border border-border px-3 py-1.5 text-xs font-medium text-fg-muted hover:border-primary hover:text-primary transition-all"
            >
              <Plus className="size-3" />
              Add Achievement
            </button>
          </div>
        </CardHeader>
        <CardContent>
          {autoAchievements.length === 0 && manualAchievements.length === 0 ? (
            <p className="py-6 text-center text-sm text-fg-muted">No achievements yet. Add your first achievement!</p>
          ) : (
            <div className="grid gap-3 sm:grid-cols-2">
              {/* Auto-generated event achievements */}
              {autoAchievements.map((a, i) => (
                <div key={`auto-${i}`} className="flex items-center gap-3 rounded-panel border border-border bg-surface-2 px-4 py-3 transition-all hover:-translate-y-0.5 hover:border-hairline-strong">
                  <span className="text-xl">{a.icon}</span>
                  <div className="min-w-0 flex-1">
                    <span className="text-sm font-medium">{a.label}</span>
                    <div className="text-[10px] text-fg-subtle uppercase tracking-wider mt-0.5">Event Achievement</div>
                  </div>
                </div>
              ))}
              {/* Manual user achievements */}
              {manualAchievements.map((a) => (
                <div key={a.id} className="group flex items-center gap-3 rounded-panel border border-border bg-surface-2 px-4 py-3 transition-all hover:-translate-y-0.5 hover:border-hairline-strong">
                  <span className="text-xl">{a.icon || "⭐"}</span>
                  <div className="min-w-0 flex-1">
                    <span className="text-sm font-medium">{a.title}</span>
                    {a.description && <p className="text-xs text-fg-muted mt-0.5 line-clamp-1">{a.description}</p>}
                    {a.dateOrYear && <div className="text-[10px] text-fg-subtle mt-0.5">{a.dateOrYear}</div>}
                  </div>
                  <div className="flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity shrink-0">
                    <button onClick={() => { setEditingAchievement(a); setAddAchievementOpen(true); }}
                      className="rounded-full p-1 text-fg-muted hover:text-primary transition-colors"><Pencil className="size-3" /></button>
                    <button onClick={() => handleDeleteAchievement(a.id)}
                      className="rounded-full p-1 text-fg-muted hover:text-red-400 transition-colors"><Trash2 className="size-3" /></button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      {/* ── Club Activity & Event Performance ── */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-base">
            <Swords className="size-4" />
            Club Activity & Event Performance
          </CardTitle>
        </CardHeader>
        <CardContent>
          {eventParticipations.length > 0 ? (
            <>
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
                    {filter}
                  </button>
                ))}
              </div>

              {filteredEvents.length > 0 ? (
                <div className="space-y-3">
                  {visibleEvents.map((event) => (
                    <div key={event.eventId}
                      className="flex items-center gap-4 rounded-panel border border-border bg-surface-2 p-4 transition-all hover:-translate-y-0.5 hover:border-hairline-strong">
                      <div className="flex size-10 shrink-0 items-center justify-center rounded-full border"
                        style={{
                          borderColor: getEventTypeColor(event.eventType),
                          color: getEventTypeColor(event.eventType),
                          backgroundColor: `color-mix(in srgb, ${getEventTypeColor(event.eventType)} 10%, transparent)`,
                        }}>
                        {getEventTypeIcon(event.eventType)}
                      </div>
                      <div className="min-w-0 flex-1">
                        <div className="flex items-center gap-2">
                          <span className="truncate text-sm font-semibold">{event.eventName}</span>
                          {event.achievement && <span className="shrink-0 text-sm">{event.achievement.split(" ")[0]}</span>}
                        </div>
                        <div className="mt-1 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-fg-muted">
                          <span>{new Date(event.eventDate).toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric" })}</span>
                          <span className="rounded-full border px-2 py-0.5 text-[10px] font-medium"
                            style={{ borderColor: getEventTypeColor(event.eventType), color: getEventTypeColor(event.eventType) }}>
                            {event.eventType}
                          </span>
                        </div>
                      </div>
                      <div className="shrink-0 text-right">
                        {event.rank !== null ? (
                          <>
                            <div className="text-lg font-bold" style={{ color: event.rank <= 3 ? "var(--cf-master)" : "var(--foreground)" }}>#{event.rank}</div>
                            <div className="text-[10px] text-fg-muted">/ {event.totalParticipants}</div>
                          </>
                        ) : (
                          <div className="text-xs font-medium text-fg-muted">Participated</div>
                        )}
                      </div>
                    </div>
                  ))}
                  {filteredEvents.length > EVENTS_PER_PAGE && !showAllEvents && (
                    <button onClick={() => setShowAllEvents(true)}
                      className="flex w-full items-center justify-center gap-1.5 rounded-panel border border-border py-3 text-xs font-medium text-fg-muted transition-all hover:border-hairline-strong hover:text-foreground">
                      <ChevronDown className="size-3.5" />
                      Show {filteredEvents.length - EVENTS_PER_PAGE} more events
                    </button>
                  )}
                </div>
              ) : (
                <p className="py-6 text-center text-sm text-fg-muted">No events in this category.</p>
              )}
            </>
          ) : (
            <p className="py-6 text-center text-sm text-fg-muted">No club activities yet. Participate in an event to see your activity here.</p>
          )}
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
          <div className="flex justify-between items-center">
            <span className="text-fg-muted">Codeforces Handle</span>
            <a href={`https://codeforces.com/profile/${encodeURIComponent(profile.codeforcesHandle)}`}
              target="_blank" rel="noopener noreferrer" className="hover:underline flex items-center gap-1.5" style={{ color: nameColor }}>
              <span>@{profile.codeforcesHandle}</span>
              <ExternalLink className="size-3" />
            </a>
          </div>
          <Separator />
          <div className="flex justify-between">
            <span className="text-fg-muted">Member Since</span>
            <span>{new Date(profile.createdAt).toLocaleDateString("en-US", { month: "long", year: "numeric" })}</span>
          </div>
        </CardContent>
      </Card>

      {/* ── Edit Profile Modal ── */}
      <EditProfileModal
        open={editProfileOpen}
        onClose={() => setEditProfileOpen(false)}
        profile={profile}
        onSave={handleSaveProfile}
        saving={saving}
      />

      {/* ── Add/Edit Achievement Modal ── */}
      <AchievementModal
        open={addAchievementOpen}
        onClose={() => { setAddAchievementOpen(false); setEditingAchievement(null); }}
        achievement={editingAchievement}
        onSave={handleSaveAchievement}
        saving={saving}
      />
    </div>
  );
}

// ── Edit Profile Modal Component ──
function EditProfileModal({
  open, onClose, profile, onSave, saving,
}: {
  open: boolean;
  onClose: () => void;
  profile: Profile;
  onSave: (data: {
    name: string;
    socialLinks: { github?: string; linkedin?: string; instagram?: string };
    cpHandles: { codeforces?: string; leetcode?: string; codechef?: string; atcoder?: string };
  }) => void;
  saving: boolean;
}) {
  const [name, setName] = useState(profile.name);
  const [github, setGithub] = useState(profile.socialLinks?.github ?? "");
  const [linkedin, setLinkedin] = useState(profile.socialLinks?.linkedin ?? "");
  const [instagram, setInstagram] = useState(profile.socialLinks?.instagram ?? "");
  const [cfHandle, setCfHandle] = useState(profile.cpHandles?.codeforces ?? profile.codeforcesHandle ?? "");
  const [lcHandle, setLcHandle] = useState(profile.cpHandles?.leetcode ?? "");
  const [ccHandle, setCcHandle] = useState(profile.cpHandles?.codechef ?? "");
  const [acHandle, setAcHandle] = useState(profile.cpHandles?.atcoder ?? "");

  const [prevOpen, setPrevOpen] = useState(open);

  if (open !== prevOpen) {
    setPrevOpen(open);
    if (open) {
      setName(profile.name);
      setGithub(profile.socialLinks?.github ?? "");
      setLinkedin(profile.socialLinks?.linkedin ?? "");
      setInstagram(profile.socialLinks?.instagram ?? "");
      setCfHandle(profile.cpHandles?.codeforces ?? profile.codeforcesHandle ?? "");
      setLcHandle(profile.cpHandles?.leetcode ?? "");
      setCcHandle(profile.cpHandles?.codechef ?? "");
      setAcHandle(profile.cpHandles?.atcoder ?? "");
    }
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSave({
      name: name.trim() || profile.name,
      socialLinks: {
        github: github.trim() || undefined,
        linkedin: linkedin.trim() || undefined,
        instagram: instagram.trim() || undefined,
      },
      cpHandles: {
        codeforces: cfHandle.trim() || undefined,
        leetcode: lcHandle.trim() || undefined,
        codechef: ccHandle.trim() || undefined,
        atcoder: acHandle.trim() || undefined,
      },
    });
  };

  const inputClass = "w-full rounded-control border border-border bg-background px-3 py-2 text-sm text-foreground placeholder:text-fg-subtle focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary/30";

  return (
    <ModalOverlay open={open} onClose={onClose} title="Edit Profile">
      <form onSubmit={handleSubmit} className="space-y-5">
        {/* Name */}
        <div>
          <label className="block text-xs font-medium text-fg-muted mb-1.5">Display Name</label>
          <input type="text" value={name} onChange={(e) => setName(e.target.value)} className={inputClass} placeholder="Your display name" />
        </div>

        <Separator />
        <p className="text-xs font-semibold text-fg-muted uppercase tracking-wider">Social Accounts</p>

        <div>
          <label className="block text-xs font-medium text-fg-muted mb-1.5">GitHub URL</label>
          <input type="url" value={github} onChange={(e) => setGithub(e.target.value)} className={inputClass} placeholder="https://github.com/username" />
        </div>
        <div>
          <label className="block text-xs font-medium text-fg-muted mb-1.5">LinkedIn URL</label>
          <input type="url" value={linkedin} onChange={(e) => setLinkedin(e.target.value)} className={inputClass} placeholder="https://linkedin.com/in/username" />
        </div>
        <div>
          <label className="block text-xs font-medium text-fg-muted mb-1.5">Instagram URL</label>
          <input type="url" value={instagram} onChange={(e) => setInstagram(e.target.value)} className={inputClass} placeholder="https://instagram.com/username" />
        </div>

        <Separator />
        <p className="text-xs font-semibold text-fg-muted uppercase tracking-wider">Competitive Programming Handles</p>

        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="block text-xs font-medium text-fg-muted mb-1.5">Codeforces</label>
            <input type="text" value={cfHandle} onChange={(e) => setCfHandle(e.target.value)} className={inputClass} placeholder="handle" />
          </div>
          <div>
            <label className="block text-xs font-medium text-fg-muted mb-1.5">LeetCode</label>
            <input type="text" value={lcHandle} onChange={(e) => setLcHandle(e.target.value)} className={inputClass} placeholder="username" />
          </div>
          <div>
            <label className="block text-xs font-medium text-fg-muted mb-1.5">CodeChef</label>
            <input type="text" value={ccHandle} onChange={(e) => setCcHandle(e.target.value)} className={inputClass} placeholder="handle" />
          </div>
          <div>
            <label className="block text-xs font-medium text-fg-muted mb-1.5">AtCoder</label>
            <input type="text" value={acHandle} onChange={(e) => setAcHandle(e.target.value)} className={inputClass} placeholder="handle" />
          </div>
        </div>

        <div className="flex justify-end gap-3 pt-2">
          <button type="button" onClick={onClose}
            className="rounded-full border border-border px-4 py-2 text-xs font-medium text-fg-muted hover:bg-surface-3 transition-colors">
            Cancel
          </button>
          <button type="submit" disabled={saving}
            className="inline-flex items-center gap-1.5 rounded-full bg-primary px-4 py-2 text-xs font-semibold text-primary-foreground hover:opacity-90 disabled:opacity-50 transition-all">
            <Save className="size-3" />
            {saving ? "Saving..." : "Save Changes"}
          </button>
        </div>
      </form>
    </ModalOverlay>
  );
}

// ── Achievement Modal Component ──
function AchievementModal({
  open, onClose, achievement, onSave, saving,
}: {
  open: boolean;
  onClose: () => void;
  achievement: ManualAchievement | null;
  onSave: (a: ManualAchievement) => void;
  saving: boolean;
}) {
  const [title, setTitle] = useState(achievement?.title ?? "");
  const [description, setDescription] = useState(achievement?.description ?? "");
  const [icon, setIcon] = useState(achievement?.icon ?? "🏆");
  const [dateOrYear, setDateOrYear] = useState(achievement?.dateOrYear ?? "");

  const [prevOpen, setPrevOpen] = useState(open);
  const [prevAch, setPrevAch] = useState(achievement);

  if (open !== prevOpen || achievement !== prevAch) {
    setPrevOpen(open);
    setPrevAch(achievement);
    if (open) {
      setTitle(achievement?.title ?? "");
      setDescription(achievement?.description ?? "");
      setIcon(achievement?.icon ?? "🏆");
      setDateOrYear(achievement?.dateOrYear ?? "");
    }
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim()) return;
    onSave({
      id: achievement?.id ?? `ach-${Date.now()}`,
      title: title.trim(),
      description: description.trim() || undefined,
      icon: icon.trim() || "⭐",
      dateOrYear: dateOrYear.trim() || undefined,
    });
  };

  const inputClass = "w-full rounded-control border border-border bg-background px-3 py-2 text-sm text-foreground placeholder:text-fg-subtle focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary/30";

  return (
    <ModalOverlay open={open} onClose={onClose} title={achievement ? "Edit Achievement" : "Add Achievement"}>
      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="block text-xs font-medium text-fg-muted mb-1.5">Emoji / Icon</label>
          <input type="text" value={icon} onChange={(e) => setIcon(e.target.value)} className={`${inputClass} w-20`} placeholder="🏆" maxLength={4} />
        </div>
        <div>
          <label className="block text-xs font-medium text-fg-muted mb-1.5">Achievement Title *</label>
          <input type="text" value={title} onChange={(e) => setTitle(e.target.value)} className={inputClass} placeholder="e.g. Hackathon Winner" required />
        </div>
        <div>
          <label className="block text-xs font-medium text-fg-muted mb-1.5">Description (optional)</label>
          <input type="text" value={description} onChange={(e) => setDescription(e.target.value)} className={inputClass} placeholder="Brief description" />
        </div>
        <div>
          <label className="block text-xs font-medium text-fg-muted mb-1.5">Date / Year (optional)</label>
          <input type="text" value={dateOrYear} onChange={(e) => setDateOrYear(e.target.value)} className={inputClass} placeholder="e.g. 2026" />
        </div>

        <div className="flex justify-end gap-3 pt-2">
          <button type="button" onClick={onClose}
            className="rounded-full border border-border px-4 py-2 text-xs font-medium text-fg-muted hover:bg-surface-3 transition-colors">
            Cancel
          </button>
          <button type="submit" disabled={saving || !title.trim()}
            className="inline-flex items-center gap-1.5 rounded-full bg-primary px-4 py-2 text-xs font-semibold text-primary-foreground hover:opacity-90 disabled:opacity-50 transition-all">
            <Save className="size-3" />
            {saving ? "Saving..." : achievement ? "Update" : "Add"}
          </button>
        </div>
      </form>
    </ModalOverlay>
  );
}
