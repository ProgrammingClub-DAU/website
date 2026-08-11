"use client";

import { useState, useMemo, useRef, useEffect } from "react";
import Link from "next/link";
import Image from "next/image";
import {
  Search,
  ArrowRight,
  ShieldCheck,
  Zap,
  Crown,
  Users,
  GraduationCap,
  ChevronLeft,
  ChevronRight,
  Code2,
  ExternalLink,
} from "lucide-react";

import { Input } from "@/components/ui/input";
import { rankColor, CF_RANKS } from "@/lib/cf-ranks";
import type { Member } from "@/types/api";

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

  // Filtered members list for Search & Category filter
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

  // Section categorizations from full members data
  const leadership = useMemo(
    () => members.filter((m) => m.clubRoleCategory === "Leadership"),
    [members]
  );
  const coreTeam = useMemo(
    () => members.filter((m) => m.clubRoleCategory === "Core"),
    [members]
  );
  const associateCore = useMemo(
    () => members.filter((m) => m.clubRoleCategory === "Associate Core"),
    [members]
  );
  const batchReps = useMemo(
    () => members.filter((m) => m.clubRoleCategory === "Batch Representative"),
    [members]
  );

  // Web Dev Team (exactly 6)
  const webDevTeam = useMemo(
    () => members.filter((m) => m.isWebDev),
    [members]
  );

  // Dynamic header stats
  const totalParticipants = 818;
  const officialMembersCount = members.length;
  const totalEvents = 20;
  const totalContests = 14;

  const isFiltering = q !== "" || activeFilter !== "All Members";

  return (
    <div className="space-y-12">
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

      {/* ── 1. 👑 LEADERSHIP SECTION (2-column layout only, 2 cards) ── */}
      {!isFiltering && leadership.length >= 2 && (
        <section className="space-y-4">
          <div className="flex items-center gap-2">
            <span className="text-xl">👑</span>
            <div>
              <h2 className="text-xl font-bold tracking-tight">LEADERSHIP</h2>
              <p className="text-xs text-fg-muted">Convenor & Deputy Convenor guiding the DAU Programming Club.</p>
            </div>
          </div>

          <div className="grid gap-6 md:grid-cols-2">
            {leadership.slice(0, 2).map((m) => (
              <LeadershipCard key={m.name} member={m} />
            ))}
          </div>
        </section>
      )}

      {/* ── 2. ⚡ CORE TEAM SECTION (Carousel, 4 Members) ── */}
      {!isFiltering && coreTeam.length > 0 && (
        <SectionCarousel
          icon="⚡"
          title="CORE TEAM"
          subtitle="Leading contest operations, problem setting, technical infrastructure, and editorials."
          items={coreTeam}
          renderCard={(m) => <MemberCard member={m} size="medium" />}
          itemsPerPage={{ default: 3, sm: 2, lg: 4 }}
        />
      )}

      {/* ── 3. ◈ ASSOCIATE CORE SECTION (Carousel, 6 Members) ── */}
      {!isFiltering && associateCore.length > 0 && (
        <SectionCarousel
          icon="◈"
          title="ASSOCIATE CORE"
          subtitle="Running workshops, managing outreach, and organizing practice sessions."
          items={associateCore}
          renderCard={(m) => <MemberCard member={m} size="medium" />}
          itemsPerPage={{ default: 3, sm: 2, lg: 3 }}
        />
      )}

      {/* ── 4. 🎓 BATCH REPRESENTATIVES SECTION (Carousel, 12 Members) ── */}
      {!isFiltering && batchReps.length > 0 && (
        <SectionCarousel
          icon="🎓"
          title="BATCH REPRESENTATIVES"
          subtitle="Connecting student batches across B.Tech & M.Tech to Programming Club events."
          items={batchReps}
          renderCard={(m) => <MemberCard member={m} size="compact" />}
          itemsPerPage={{ default: 5, sm: 2, md: 3, lg: 5 }}
        />
      )}

      {/* ── 5. 🌐 COMMUNITY PARTICIPANTS & FILTERED GRID ── */}
      <section className="space-y-6 pt-4 border-t border-border/60">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <div className="flex items-center gap-2">
              <span className="text-xl">🌐</span>
              <h2 className="text-xl font-bold tracking-tight">COMMUNITY DIRECTORY</h2>
            </div>
            <p className="text-xs text-fg-muted">Search and filter across all official club members.</p>
          </div>

          {/* Search Input */}
          <div className="relative min-w-64">
            <Search className="pointer-events-none absolute top-1/2 left-3.5 size-4 -translate-y-1/2 text-fg-muted" />
            <Input
              type="search"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Search by name, handle, or role..."
              className="h-10 rounded-control border-border bg-surface-2 pl-9 text-xs placeholder:text-fg-muted"
            />
          </div>
        </div>

        {/* Filter Chips */}
        <div className="flex flex-wrap items-center justify-between gap-3">
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
          <span className="font-mono text-[11px] tracking-[0.1em] text-fg-subtle uppercase">
            Showing {filteredMembers.length} of {members.length} members
          </span>
        </div>

        {/* Filtered Grid Output */}
        {filteredMembers.length === 0 ? (
          <div className="rounded-panel border border-dashed border-border py-12 text-center text-sm text-fg-muted">
            No members match your search criteria.
          </div>
        ) : (
          <div className="grid gap-4 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4">
            {filteredMembers.map((m) => (
              <MemberCard key={`filtered-${m.name}`} member={m} size="medium" />
            ))}
          </div>
        )}
      </section>

      {/* ── 6. ✨ WEBSITE DEVELOPMENT TEAM SECTION (Special 3x2 Grid) ── */}
      <section className="space-y-6 pt-8 border-t border-border/80">
        <div className="text-center max-w-xl mx-auto space-y-1">
          <div className="inline-flex items-center gap-1.5 rounded-full border border-primary/30 bg-primary/10 px-3 py-1 text-xs font-mono font-semibold text-primary">
            <Code2 className="size-3.5" />
            DEVELOPMENT TEAM
          </div>
          <h2 className="text-2xl font-bold tracking-tight text-foreground">✨ WEBSITE DEVELOPMENT TEAM</h2>
          <p className="text-xs text-fg-muted">
            The 6 engineering team members who designed, built, and maintain the Programming Club platform.
          </p>
        </div>

        <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {webDevTeam.map((m) => (
            <WebDevCard key={`dev-${m.name}`} member={m} />
          ))}
        </div>
      </section>

      {/* ── Bottom CTA ── */}
      <div className="mt-12 rounded-panel border border-border bg-gradient-to-r from-surface-2 via-surface-3 to-surface-2 p-8 text-center">
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

// ── 👑 1. LEADERSHIP CARD COMPONENT (Prominent 2-Column Top Section) ──
function LeadershipCard({ member }: { member: Member }) {
  const color = rankColor(member.cf);
  const isConvenor = member.role.toLowerCase().includes("convenor") && !member.role.toLowerCase().includes("deputy");

  return (
    <div
      className={`group relative flex flex-col items-center justify-between rounded-panel border p-7 text-center transition-all duration-300 hover:-translate-y-1.5 hover:shadow-xl ${
        isConvenor
          ? "border-amber-500/40 bg-gradient-to-b from-amber-500/10 via-surface-2 to-surface-2 shadow-amber-500/5 hover:border-amber-500/70 hover:shadow-amber-500/10"
          : "border-primary/40 bg-gradient-to-b from-primary/10 via-surface-2 to-surface-2 shadow-primary/5 hover:border-primary/70 hover:shadow-primary/10"
      }`}
    >
      <div className="flex flex-col items-center w-full">
        {/* Centered Large Profile Photo */}
        <div className="relative mb-5">
          <div
            className="flex size-28 shrink-0 items-center justify-center rounded-full border-4 shadow-lg transition-transform duration-300 group-hover:scale-105"
            style={{
              borderColor: color,
              boxShadow: `0 0 24px color-mix(in srgb, ${color} 30%, transparent)`,
            }}
          >
            {member.avatarUrl ? (
              <Image
                src={member.avatarUrl}
                alt={member.name}
                width={112}
                height={112}
                className="size-full rounded-full object-cover"
              />
            ) : (
              <span className="font-mono text-2xl font-bold text-foreground">
                {member.initials}
              </span>
            )}
          </div>
          {/* Active status pulse */}
          <span
            className="absolute bottom-1 right-1 size-4 rounded-full border-2 border-background bg-emerald-400 shadow-sm"
            title="Active Leader"
          />
        </div>

        {/* Name & Handle */}
        <h3 className="text-xl font-extrabold tracking-tight group-hover:underline" style={{ color }}>
          {member.name}
        </h3>
        <p className="mt-0.5 font-mono text-xs text-fg-muted">
          @{member.codeforcesHandle || member.name.toLowerCase().replace(/\s+/g, "")}
        </p>

        {/* Role Badge */}
        <div className="mt-3">
          <span
            className="inline-flex items-center gap-1.5 rounded-full border px-4 py-1 font-mono text-xs font-bold uppercase tracking-wider shadow-xs"
            style={{
              borderColor: color,
              color,
              backgroundColor: `color-mix(in srgb, ${color} 12%, transparent)`,
            }}
          >
            <Crown className="size-3.5" />
            {member.role}
          </span>
        </div>

        {/* Academic Details */}
        <p className="mt-2.5 font-mono text-xs text-fg-muted flex items-center gap-1">
          <GraduationCap className="size-3.5 text-fg-subtle" />
          <span>{member.degree || "B.Tech ICT"} • {member.gradYear || "2026"}</span>
        </p>

        {/* Short bio */}
        <p className="mt-3 text-xs leading-relaxed text-fg-muted max-w-sm">
          {member.about}
        </p>
      </div>

      {/* CP Stats Box */}
      <div className="mt-6 w-full pt-4 border-t border-border/60">
        <div className="grid grid-cols-3 gap-2 rounded-panel border border-border/50 bg-background/60 p-3 text-center">
          <div>
            <div className="text-base font-extrabold font-mono" style={{ color }}>
              {member.rating ?? "—"}
            </div>
            <div className="text-[10px] text-fg-muted uppercase tracking-wider">Rating</div>
          </div>
          <div>
            <div className="text-base font-extrabold font-mono text-foreground">
              {member.solvedCount ?? "—"}
            </div>
            <div className="text-[10px] text-fg-muted uppercase tracking-wider">Solved</div>
          </div>
          <div>
            <div className="text-base font-extrabold font-mono text-foreground">
              {member.contestCount ?? "—"}
            </div>
            <div className="text-[10px] text-fg-muted uppercase tracking-wider">Contests</div>
          </div>
        </div>

        {/* Title & View Profile Link */}
        <div className="mt-4 flex items-center justify-between text-xs px-1">
          <span className="font-bold text-xs uppercase tracking-wider flex items-center gap-1" style={{ color }}>
            🏆 {getRankName(member.cf)}
          </span>
          <Link
            href="/profile"
            className="inline-flex items-center gap-1 font-semibold text-primary hover:underline transition-all"
          >
            <span>View Profile</span>
            <ArrowRight className="size-3.5" />
          </Link>
        </div>
      </div>
    </div>
  );
}

// ── STANDARD MEMBER CARD COMPONENT (Used in Carousels & Grid) ──
function MemberCard({
  member,
  size = "medium",
}: {
  member: Member;
  size?: "compact" | "medium";
}) {
  const color = rankColor(member.cf);
  const isCompact = size === "compact";

  return (
    <div className="group flex h-full flex-col justify-between rounded-panel border border-border bg-surface-2 p-5 text-center transition-all duration-200 hover:-translate-y-1 hover:border-hairline-strong hover:shadow-panel">
      <div className="flex flex-col items-center">
        {/* Centered Profile Photo */}
        <div className="relative mb-3">
          <div
            className={`flex shrink-0 items-center justify-center rounded-full border-2 bg-surface-3 transition-transform group-hover:scale-105 ${
              isCompact ? "size-16" : "size-20"
            }`}
            style={{ borderColor: color }}
          >
            {member.avatarUrl ? (
              <Image
                src={member.avatarUrl}
                alt={member.name}
                width={isCompact ? 64 : 80}
                height={isCompact ? 64 : 80}
                className="size-full rounded-full object-cover"
              />
            ) : (
              <span className={`font-mono font-bold text-foreground ${isCompact ? "text-sm" : "text-base"}`}>
                {member.initials}
              </span>
            )}
          </div>
        </div>

        {/* Name & Handle */}
        <h3 className={`font-bold tracking-tight truncate max-w-full group-hover:underline ${isCompact ? "text-sm" : "text-base"}`} style={{ color }}>
          {member.name}
        </h3>
        <p className="font-mono text-[11px] text-fg-muted">
          @{member.codeforcesHandle || member.name.toLowerCase().replace(/\s+/g, "")}
        </p>

        {/* Role Badge */}
        <div className="mt-2">
          <span
            className="inline-flex items-center gap-1 rounded-full border px-2.5 py-0.5 font-mono text-[10px] font-medium uppercase"
            style={{
              borderColor: color,
              color,
              backgroundColor: `color-mix(in srgb, ${color} 10%, transparent)`,
            }}
          >
            {member.role}
          </span>
        </div>

        {/* Academic Details */}
        <p className="mt-1.5 font-mono text-[11px] text-fg-muted">
          {member.degree || "B.Tech ICT"} • {member.gradYear || member.batch.replace("B.Tech ’", "20")}
        </p>
      </div>

      {/* Footer: CP Stats & Profile Link */}
      <div className="mt-4 pt-3 border-t border-border/50 space-y-2.5">
        {/* CP Statistics Box */}
        <div className="grid grid-cols-3 gap-1 rounded-control border border-border/40 bg-background/50 p-2 text-center font-mono text-xs">
          <div>
            <div className="font-bold text-xs" style={{ color }}>
              {member.rating ?? "—"}
            </div>
            <div className="text-[9px] text-fg-muted uppercase">Rating</div>
          </div>
          <div>
            <div className="font-bold text-xs text-foreground">
              {member.solvedCount ?? "—"}
            </div>
            <div className="text-[9px] text-fg-muted uppercase">Solved</div>
          </div>
          <div>
            <div className="font-bold text-xs text-foreground">
              {member.contestCount ?? "—"}
            </div>
            <div className="text-[9px] text-fg-muted uppercase">Contests</div>
          </div>
        </div>

        {/* Rank Title & View Profile Link */}
        <div className="flex items-center justify-between text-[11px] px-0.5">
          <span className="font-semibold text-[10px] uppercase tracking-wider truncate max-w-[110px]" style={{ color }}>
            🏆 {getRankName(member.cf)}
          </span>
          <Link
            href="/profile"
            className="inline-flex items-center gap-0.5 font-medium text-primary hover:underline shrink-0"
          >
            <span>View</span>
            <ArrowRight className="size-3" />
          </Link>
        </div>
      </div>
    </div>
  );
}

// ── ✨ 6. WEBSITE DEVELOPMENT TEAM CARD (Special Gradient 3x2 Grid) ──
function WebDevCard({ member }: { member: Member }) {
  return (
    <div className="group relative flex flex-col justify-between rounded-panel border border-primary/30 bg-gradient-to-b from-primary/10 via-surface-2 to-surface-3/80 p-6 text-center shadow-lg transition-all duration-300 hover:-translate-y-1.5 hover:border-primary/60 hover:shadow-primary/10">
      {/* Dev Badge Ribbon */}
      <div className="absolute top-3 right-3 flex items-center gap-1 rounded-full border border-primary/40 bg-primary/20 px-2.5 py-0.5 text-[10px] font-mono font-bold text-primary">
        <Code2 className="size-3" />
        WEB DEV
      </div>

      <div className="flex flex-col items-center">
        {/* Large Centered Profile Photo */}
        <div className="relative mb-4">
          <div
            className="flex size-24 shrink-0 items-center justify-center rounded-full border-3 bg-surface-3 shadow-md transition-transform duration-300 group-hover:scale-105"
            style={{
              borderColor: "var(--primary)",
              boxShadow: "0 0 20px rgba(138,43,226,0.25)",
            }}
          >
            {member.avatarUrl ? (
              <Image
                src={member.avatarUrl}
                alt={member.name}
                width={96}
                height={96}
                className="size-full rounded-full object-cover"
              />
            ) : (
              <span className="font-mono text-xl font-bold text-foreground">
                {member.initials}
              </span>
            )}
          </div>
        </div>

        {/* Name & Handle */}
        <h3 className="text-lg font-bold tracking-tight text-foreground group-hover:text-primary transition-colors">
          {member.name}
        </h3>
        <p className="font-mono text-xs text-fg-muted">
          @{member.codeforcesHandle || member.name.toLowerCase().replace(/\s+/g, "")}
        </p>

        {/* Dev Contribution Role */}
        <div className="mt-2.5">
          <span className="inline-flex items-center gap-1 rounded-full border border-primary/30 bg-surface-1 px-3 py-1 text-xs font-semibold text-primary">
            {member.devRole || "Website Developer"}
          </span>
        </div>

        {/* Academic Details */}
        <p className="mt-2 font-mono text-xs text-fg-muted">
          {member.degree || "B.Tech ICT"} • {member.gradYear || "2028"}
        </p>
      </div>

      {/* Social Links & View Profile */}
      <div className="mt-5 pt-3 border-t border-border/60 flex items-center justify-between">
        <div className="flex items-center gap-2">
          {member.githubUrl && (
            <a
              href={member.githubUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="rounded-full p-1.5 text-fg-muted hover:bg-surface-3 hover:text-foreground transition-colors"
              title="GitHub Profile"
            >
              <Image src="/github-logo.png" alt="GitHub" width={16} height={16} className="object-contain invert" />
            </a>
          )}
          {member.linkedinUrl && (
            <a
              href={member.linkedinUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="rounded-full p-1.5 text-fg-muted hover:bg-surface-3 hover:text-foreground transition-colors"
              title="LinkedIn Profile"
            >
              <Image src="/linkedin-logo.avif" alt="LinkedIn" width={16} height={16} className="object-contain" />
            </a>
          )}
        </div>

        <Link
          href="/profile"
          className="inline-flex items-center gap-1 text-xs font-semibold text-primary hover:underline"
        >
          <span>View Profile</span>
          <ExternalLink className="size-3" />
        </Link>
      </div>
    </div>
  );
}

// ── REUSABLE HORIZONTAL CAROUSEL COMPONENT ──
function SectionCarousel<T>({
  icon,
  title,
  subtitle,
  items,
  renderCard,
  itemsPerPage,
}: {
  icon: string;
  title: string;
  subtitle: string;
  items: T[];
  renderCard: (item: T) => React.ReactNode;
  itemsPerPage: { default: number; sm?: number; md?: number; lg?: number };
}) {
  const containerRef = useRef<HTMLDivElement>(null);
  const [canScrollLeft, setCanScrollLeft] = useState(false);
  const [canScrollRight, setCanScrollRight] = useState(true);

  const checkScroll = () => {
    if (!containerRef.current) return;
    const { scrollLeft, scrollWidth, clientWidth } = containerRef.current;
    setCanScrollLeft(scrollLeft > 5);
    setCanScrollRight(scrollLeft < scrollWidth - clientWidth - 5);
  };

  useEffect(() => {
    checkScroll();
    window.addEventListener("resize", checkScroll);
    return () => window.removeEventListener("resize", checkScroll);
  }, [items]);

  const scroll = (direction: "left" | "right") => {
    if (!containerRef.current) return;
    const width = containerRef.current.clientWidth;
    const scrollAmount = direction === "left" ? -width * 0.75 : width * 0.75;
    containerRef.current.scrollBy({ left: scrollAmount, behavior: "smooth" });
  };

  return (
    <section className="space-y-4">
      {/* Section Header with Carousel Controls */}
      <div className="flex items-center justify-between gap-4">
        <div className="flex items-center gap-2">
          <span className="text-xl">{icon}</span>
          <div>
            <h2 className="text-xl font-bold tracking-tight">{title}</h2>
            <p className="text-xs text-fg-muted">{subtitle}</p>
          </div>
        </div>

        {/* Controls */}
        <div className="flex items-center gap-2 shrink-0">
          <button
            onClick={() => scroll("left")}
            disabled={!canScrollLeft}
            className="flex size-9 items-center justify-center rounded-full border border-border bg-surface-2 text-fg-muted hover:border-hairline-strong hover:text-foreground disabled:opacity-30 disabled:cursor-not-allowed transition-all"
            aria-label="Previous items"
          >
            <ChevronLeft className="size-4" />
          </button>
          <button
            onClick={() => scroll("right")}
            disabled={!canScrollRight}
            className="flex size-9 items-center justify-center rounded-full border border-border bg-surface-2 text-fg-muted hover:border-hairline-strong hover:text-foreground disabled:opacity-30 disabled:cursor-not-allowed transition-all"
            aria-label="Next items"
          >
            <ChevronRight className="size-4" />
          </button>
        </div>
      </div>

      {/* Horizontal Scroll Track */}
      <div
        ref={containerRef}
        onScroll={checkScroll}
        className="flex gap-4 overflow-x-auto pb-4 pt-1 scrollbar-none snap-x snap-mandatory"
        style={{ scrollbarWidth: "none", msOverflowStyle: "none" }}
      >
        {items.map((item, i) => (
          <div
            key={i}
            className="snap-start shrink-0 w-[280px] sm:w-[300px] lg:w-[calc(100%/3-11px)]"
            style={{
              width: itemsPerPage.lg === 4
                ? "calc(25% - 12px)"
                : itemsPerPage.lg === 5
                ? "calc(20% - 13px)"
                : undefined,
              minWidth: itemsPerPage.lg === 5 ? "220px" : "260px",
            }}
          >
            {renderCard(item)}
          </div>
        ))}
      </div>
    </section>
  );
}
