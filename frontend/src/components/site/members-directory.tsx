"use client";

import { useState, useMemo } from "react";
import Link from "next/link";
import { Search, ArrowRight, ShieldCheck, Zap, Crown, Users, GraduationCap } from "lucide-react";

import { Input } from "@/components/ui/input";
import { RankDot } from "@/components/site/primitives";
import { rankColor, CF_RANKS } from "@/lib/cf-ranks";
import type { Member } from "@/types/api";
import Image from "next/image";

function getRankName(key: string): string {
  return CF_RANKS.find((r) => r.key === key)?.name ?? key;
}

const FILTER_CATEGORIES = [
  "All Members",
  "Core",
  "Associate Core",
  "Batch Representatives",
] as const;

export function MembersDirectory({ members }: { members: Member[] }) {
  const [query, setQuery] = useState("");
  const [activeFilter, setActiveFilter] = useState<string>("All Members");

  const q = query.trim().toLowerCase();

  // Filtered members list
  const filteredMembers = useMemo(() => {
    return members.filter((m) => {
      // Query search
      const matchesSearch =
        !q ||
        m.name.toLowerCase().includes(q) ||
        (m.codeforcesHandle && m.codeforcesHandle.toLowerCase().includes(q)) ||
        m.role.toLowerCase().includes(q);

      if (!matchesSearch) return false;

      // Filter category
      if (activeFilter === "All Members") return true;
      if (activeFilter === "Core") return m.clubRoleCategory === "Leadership" || m.clubRoleCategory === "Core";
      if (activeFilter === "Associate Core") return m.clubRoleCategory === "Associate Core";
      if (activeFilter === "Batch Representatives") return m.clubRoleCategory === "Batch Representative";

      return true;
    });
  }, [members, q, activeFilter]);

  // Section categorizations
  const leadership = useMemo(
    () => filteredMembers.filter((m) => m.clubRoleCategory === "Leadership"),
    [filteredMembers]
  );
  const coreTeam = useMemo(
    () => filteredMembers.filter((m) => m.clubRoleCategory === "Core"),
    [filteredMembers]
  );
  const associateCore = useMemo(
    () => filteredMembers.filter((m) => m.clubRoleCategory === "Associate Core"),
    [filteredMembers]
  );
  const batchReps = useMemo(
    () => filteredMembers.filter((m) => m.clubRoleCategory === "Batch Representative"),
    [filteredMembers]
  );

  // Dynamic header stats
  const totalParticipants = 818;
  const officialMembersCount = members.length;
  const totalEvents = 20;
  const totalContests = 14;

  const isFiltering = q !== "" || activeFilter !== "All Members";

  return (
    <div className="space-y-10">
      {/* ── Compact Header Stats Row ── */}
      <div className="flex flex-wrap items-center justify-between gap-4 rounded-panel border border-border bg-surface-2 px-6 py-4">
        <div className="flex flex-wrap items-center gap-6 sm:gap-8 font-mono text-xs">
          <div className="flex items-center gap-2">
            <Users className="size-4 text-primary" />
            <span><strong className="text-foreground">{totalParticipants}</strong> Participants</span>
          </div>
          <div className="flex items-center gap-2">
            <Crown className="size-4 text-amber-400" />
            <span><strong className="text-foreground">{officialMembersCount}</strong> Official Members</span>
          </div>
          <div className="flex items-center gap-2">
            <Zap className="size-4 text-cf-master" />
            <span><strong className="text-foreground">{totalEvents}</strong> Events</span>
          </div>
          <div className="flex items-center gap-2">
            <ShieldCheck className="size-4 text-cf-candidate" />
            <span><strong className="text-foreground">{totalContests}</strong> Contests</span>
          </div>
        </div>
      </div>

      {/* ── Search & Filter Controls ── */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        {/* Search input */}
        <div className="relative min-w-64 flex-1">
          <Search className="pointer-events-none absolute top-1/2 left-3.5 size-4 -translate-y-1/2 text-fg-muted" />
          <Input
            type="search"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search members by name or handle..."
            className="h-10 rounded-control border-border bg-surface-2 pl-9 text-xs placeholder:text-fg-muted"
          />
        </div>

        {/* Filter Chips */}
        <div className="flex flex-wrap gap-1.5">
          {FILTER_CATEGORIES.map((filter) => (
            <button
              key={filter}
              onClick={() => setActiveFilter(filter)}
              className={`rounded-full border px-3 py-1.5 text-xs font-medium transition-all ${
                activeFilter === filter
                  ? "border-primary bg-primary/10 text-primary"
                  : "border-border text-fg-muted hover:border-hairline-strong hover:text-foreground"
              }`}
            >
              {filter}
            </button>
          ))}
        </div>
      </div>

      {/* Results count text */}
      <p className="font-mono text-[11px] tracking-[0.1em] text-fg-subtle uppercase">
        Showing {filteredMembers.length} of {members.length} members
      </p>

      {/* ── Tiered Member Grids ── */}
      {filteredMembers.length === 0 ? (
        <div className="rounded-panel border border-dashed border-border py-12 text-center text-sm text-fg-muted">
          No members match your search criteria.
        </div>
      ) : (
        <div className="space-y-12">
          {/* 👑 LEADERSHIP SECTION */}
          {(!isFiltering || leadership.length > 0) && (
            <SectionGroup
              title="👑 LEADERSHIP"
              subtitle="Convenor & Deputy Convenor driving club strategy and activities."
            >
              <div className="grid gap-4 md:grid-cols-2">
                {leadership.map((m) => (
                  <MemberCard key={m.name} member={m} isProminent />
                ))}
              </div>
            </SectionGroup>
          )}

          {/* ⚡ CORE TEAM SECTION */}
          {(!isFiltering || coreTeam.length > 0) && (
            <SectionGroup
              title="⚡ CORE TEAM"
              subtitle="Leading contest operations, problem setting, technical services, and editorials."
            >
              <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
                {coreTeam.map((m) => (
                  <MemberCard key={m.name} member={m} />
                ))}
              </div>
            </SectionGroup>
          )}

          {/* ◈ ASSOCIATE CORE SECTION */}
          {(!isFiltering || associateCore.length > 0) && (
            <SectionGroup
              title="◈ ASSOCIATE CORE"
              subtitle="Running workshops, managing outreach, and organizing practice sessions."
            >
              <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {associateCore.map((m) => (
                  <MemberCard key={m.name} member={m} />
                ))}
              </div>
            </SectionGroup>
          )}

          {/* 🎓 BATCH REPRESENTATIVES SECTION */}
          {(!isFiltering || batchReps.length > 0) && (
            <SectionGroup
              title="🎓 BATCH REPRESENTATIVES"
              subtitle="Connecting student batches across B.Tech & M.Tech to Programming Club events."
            >
              <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {batchReps.map((m) => (
                  <MemberCard key={m.name} member={m} />
                ))}
              </div>
            </SectionGroup>
          )}
        </div>
      )}

      {/* ── Bottom CTA ── */}
      <div className="mt-16 rounded-panel border border-border bg-gradient-to-r from-surface-2 via-surface-3 to-surface-2 p-8 text-center">
        <h3 className="font-mono text-xs font-bold tracking-[0.14em] text-primary uppercase">
          WANT TO BE PART OF IT?
        </h3>
        <p className="mt-2 text-2xl font-bold tracking-tight">
          Participate. Learn. Compete. Build.
        </p>
        <p className="mt-2 text-sm text-fg-muted">
          Join our next programming contest or workshop and get on the leaderboard.
        </p>
        <div className="mt-6">
          <Link
            href="/events"
            className="inline-flex items-center gap-2 rounded-full bg-primary px-6 py-2.5 text-xs font-semibold text-primary-foreground shadow-md transition-all hover:opacity-90 hover:shadow-lg"
          >
            <span>VIEW EVENTS</span>
            <ArrowRight className="size-3.5" />
          </Link>
        </div>
      </div>
    </div>
  );
}

// ── Section Group Wrapper ──
function SectionGroup({
  title,
  subtitle,
  children,
}: {
  title: string;
  subtitle: string;
  children: React.ReactNode;
}) {
  return (
    <div className="space-y-4">
      <div>
        <h2 className="text-base font-bold tracking-wide">{title}</h2>
        <p className="text-xs text-fg-muted">{subtitle}</p>
      </div>
      {children}
    </div>
  );
}

// ── Member Card Component ──
function MemberCard({
  member,
  isProminent = false,
  isStudent = false,
}: {
  member: Member;
  isProminent?: boolean;
  isStudent?: boolean;
}) {
  const color = rankColor(member.cf);
  const handleDisplay = member.codeforcesHandle ? `@${member.codeforcesHandle}` : `@${member.name.toLowerCase().replace(/\s+/g, "")}`;
  const roleTitle = isStudent ? "Student Participant" : member.role;
  const hasStats = member.rating !== undefined || member.solvedCount !== undefined;

  return (
    <div
      className={`group flex flex-col justify-between rounded-panel border border-border bg-surface-2 p-5 transition-all hover:-translate-y-1 hover:border-hairline-strong hover:shadow-panel ${
        isProminent ? "border-primary/40 bg-gradient-to-b from-primary/5 via-surface-2 to-surface-2" : ""
      }`}
    >
      <div>
        {/* Header: Avatar + Active Indicator */}
        <div className="flex items-start justify-between gap-3">
          <div
            className="flex size-14 shrink-0 items-center justify-center rounded-full border-2 bg-surface-3"
            style={{ borderColor: color }}
          >
            {member.avatarUrl ? (
              <Image
                src={member.avatarUrl}
                alt={member.name}
                width={56}
                height={56}
                className="size-full rounded-full object-cover"
              />
            ) : (
              <span className="font-mono text-base font-bold text-foreground">
                {member.initials}
              </span>
            )}
          </div>

          {/* Active status indicator */}
          <div className="flex items-center gap-1.5 rounded-full border border-border/60 bg-background/50 px-2.5 py-1 text-[10px] font-mono text-fg-muted">
            <span
              className={`size-1.5 rounded-full ${
                member.isActive !== false ? "bg-emerald-400 animate-pulse" : "bg-fg-subtle"
              }`}
            />
            <span>{member.isActive !== false ? "ACTIVE" : "INACTIVE"}</span>
          </div>
        </div>

        {/* Name & Handle */}
        <div className="mt-4">
          <h3
            className="text-base font-bold tracking-tight truncate group-hover:underline"
            style={{ color }}
          >
            {member.name}
          </h3>
          <p className="text-xs text-fg-muted font-mono">{handleDisplay}</p>
        </div>

        {/* Role badge & Degree */}
        <div className="mt-3 space-y-1.5">
          <span
            className="inline-flex items-center gap-1 rounded-full border px-2.5 py-0.5 font-mono text-[10px] font-medium uppercase"
            style={{
              borderColor: isStudent ? "var(--border)" : color,
              color: isStudent ? "var(--fg-muted)" : color,
              backgroundColor: `color-mix(in srgb, ${color} 8%, transparent)`,
            }}
          >
            <RankDot rank={member.cf} size={4} ring={false} />
            {roleTitle}
          </span>
          <p className="text-[11px] text-fg-muted flex items-center gap-1">
            <GraduationCap className="size-3" />
            <span>{member.degree || "B.Tech ICT"} • {member.gradYear || member.batch.replace("B.Tech ’", "20")}</span>
          </p>
        </div>

        {/* Short Note */}
        <p className="mt-3 text-xs leading-relaxed text-fg-muted line-clamp-2">
          {member.about}
        </p>
      </div>

      {/* Footer: CP Stats & Profile Link */}
      <div className="mt-4 pt-3 border-t border-border/60 space-y-3">
        {/* CP Statistics (Gracefully hidden if missing) */}
        {hasStats && (
          <div className="grid grid-cols-3 gap-2 text-center bg-background/40 p-2 rounded-control border border-border/40">
            <div>
              <div className="text-xs font-bold" style={{ color }}>
                {member.rating ?? "—"}
              </div>
              <div className="text-[9px] text-fg-muted">Rating</div>
            </div>
            <div>
              <div className="text-xs font-bold">
                {member.solvedCount ?? "—"}
              </div>
              <div className="text-[9px] text-fg-muted">Solved</div>
            </div>
            <div>
              <div className="text-xs font-bold">
                {member.contestCount ?? "—"}
              </div>
              <div className="text-[9px] text-fg-muted">Contests</div>
            </div>
          </div>
        )}

        {/* Rank Title Badge & View Profile Link */}
        <div className="flex items-center justify-between text-xs">
          <span className="text-[10px] font-semibold uppercase tracking-wider" style={{ color }}>
            🏆 {getRankName(member.cf)}
          </span>
          <Link
            href="/profile"
            className="flex items-center gap-1 font-medium text-primary hover:underline text-xs"
          >
            <span>View Profile</span>
            <ArrowRight className="size-3" />
          </Link>
        </div>
      </div>
    </div>
  );
}
