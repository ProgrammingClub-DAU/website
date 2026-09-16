"use client";

/**
 * The public members page.
 *
 * Two lists arrive: the committee, already in club hierarchy order from the
 * server, and the searchable membership. They are kept apart because they answer
 * different questions -- "who runs this club" and "who is in it" -- and because
 * only the first is small enough to send whole.
 *
 * Nothing here is invented. An earlier version filled the gaps the API left with
 * defaults: every card claimed "B.Tech ICT", an ACTIVE badge nobody could turn
 * off, and Solved and Contest counters showing a dash. On a real club's public
 * site those read as facts about real people. A field the server does not send
 * is now simply absent.
 */

import { useState, useMemo } from "react";
import Link from "next/link";
import Image from "next/image";
import { Search, ArrowRight, Users, ExternalLink } from "lucide-react";

import BorderGlow from "@/components/site/border-glow";
import { Input } from "@/components/ui/input";
import { RankDot } from "@/components/site/primitives";
import { PlatformGlyph, PROFILE_ACCENT } from "@/components/site/platform-glyph";
import { rankColor, ratingToRank, CF_RANKS } from "@/lib/cf-ranks";
import { CLUB_ROLE_LABELS } from "@/lib/club-roles";
import { profileUrl } from "@/lib/platform-profiles";
import { ACADEMIC_YEAR_LABELS, type ClubRole, type PublicMember } from "@/types/api";

function rankName(key: string): string {
  return CF_RANKS.find((r) => r.key === key)?.name ?? key;
}

function initialsOf(name: string): string {
  return name
    .split(" ")
    .filter(Boolean)
    .map((part) => part[0])
    .join("")
    .slice(0, 2)
    .toUpperCase();
}

/**
 * The page's sections, top to bottom.
 *
 * Two, not one per post. The club reads as a core team and its batch
 * representatives, so four headings split the committee more finely than the
 * club actually thinks of itself -- and with a small team it left headings
 * standing over one or two cards each.
 *
 * Rank still shows: the server returns the team in hierarchy order, so within
 * the core section the Convenor comes first and the Associate Core last, and
 * every card carries its own post as a badge.
 */
const SECTIONS: { title: string; subtitle: string; posts: ClubRole[] }[] = [
  {
    title: "CORE TEAM",
    subtitle: "Convenor, Deputy Convenor and the core team who run the club.",
    posts: ["CONVENOR", "DEPUTY_CONVENOR", "CORE", "ASSOCIATE_CORE"],
  },
  {
    title: "BATCH REPRESENTATIVES",
    subtitle: "The link between each admission batch and the club.",
    posts: ["BATCH_REPRESENTATIVE"],
  },
];

/**
 * Posts whose cards are given the accent treatment.
 *
 * With leadership no longer having a section of its own, this is what keeps the
 * two senior posts from reading as just the first two of a long grid.
 */
const HIGHLIGHTED: ClubRole[] = ["CONVENOR", "DEPUTY_CONVENOR"];

const FILTERS = ["All", "Committee", "Members"] as const;
type Filter = (typeof FILTERS)[number];

export function MembersDirectory({
  team,
  members,
  total,
  unreachable = false,
}: {
  team: PublicMember[];
  members: PublicMember[];
  total: number;
  /** True when the server could not be reached at all, as opposed to having nobody to show. */
  unreachable?: boolean;
}) {
  const [query, setQuery] = useState("");
  const [filter, setFilter] = useState<Filter>("All");

  const q = query.trim().toLowerCase();

  const matches = useMemo(() => {
    return (m: PublicMember) =>
      !q ||
      m.name.toLowerCase().includes(q) ||
      (m.codeforcesHandle ?? "").toLowerCase().includes(q) ||
      (m.clubRole ? CLUB_ROLE_LABELS[m.clubRole].toLowerCase().includes(q) : false);
  }, [q]);

  const committee = useMemo(() => team.filter(matches), [team, matches]);

  /**
   * Everyone the committee sections do not already show.
   *
   * The two endpoints overlap -- a Convenor is also in the directory -- so
   * without this every office bearer appears twice on the page.
   */
  const others = useMemo(() => {
    const onCommittee = new Set(team.map((m) => m.id));
    return members.filter((m) => !onCommittee.has(m.id)).filter(matches);
  }, [members, team, matches]);

  const showCommittee = filter === "All" || filter === "Committee";
  const showOthers = filter === "All" || filter === "Members";

  const visibleCount =
    (showCommittee ? committee.length : 0) + (showOthers ? others.length : 0);

  return (
    <div className="space-y-10">
      <div className="flex flex-wrap items-center justify-between gap-4 rounded-panel border border-border bg-surface-2 px-6 py-4">
        <div className="flex flex-wrap items-center gap-6 font-mono text-xs sm:gap-8">
          <div className="flex items-center gap-2">
            <Users className="size-4 text-primary" />
            <span>
              <strong className="text-foreground">{total}</strong> Registered Members
            </span>
          </div>
          {team.length > 0 && (
            <div className="flex items-center gap-2 text-fg-muted">
              <span>
                <strong className="text-foreground">{team.length}</strong> on the committee
              </span>
            </div>
          )}
        </div>
      </div>

      {/* Search & filters */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="relative min-w-64 flex-1">
          <Search className="pointer-events-none absolute top-1/2 left-3.5 size-4 -translate-y-1/2 text-fg-muted" />
          <Input
            type="search"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search members by name, handle or post..."
            className="pl-10"
          />
        </div>

        <div className="flex gap-2">
          {FILTERS.map((option) => (
            <button
              key={option}
              type="button"
              onClick={() => setFilter(option)}
              className={`rounded-full border px-3.5 py-1.5 font-mono text-micro tracking-caps-wide uppercase transition-colors ${
                filter === option
                  ? "border-primary bg-primary/10 text-foreground"
                  : "border-border bg-surface-2 text-fg-muted hover:text-foreground"
              }`}
            >
              {option}
            </button>
          ))}
        </div>
      </div>

      {unreachable ? (
        /*
          An empty list and an unreachable server are different facts, and saying
          the first when the second is true is a lie about the club. This page is
          public, so it says which one it is.
        */
        <div className="rounded-panel border border-dashed border-destructive/40 bg-destructive/5 py-12 text-center">
          <p className="text-sm text-destructive">Could not load the member list.</p>
          <p className="mt-1 text-xs text-fg-muted">
            The server may be waking up. Reload in a few seconds.
          </p>
        </div>
      ) : visibleCount === 0 ? (
        <div className="rounded-panel border border-dashed border-border py-12 text-center text-sm text-fg-muted">
          {total === 0
            ? "No members yet. The directory fills up as people sign in."
            : "No members match your search."}
        </div>
      ) : (
        <div className="space-y-12">
          {showCommittee &&
            SECTIONS.map((section) => {
              const people = committee.filter(
                (m) => m.clubRole && section.posts.includes(m.clubRole),
              );
              if (people.length === 0) return null;

              return (
                <SectionGroup
                  key={section.title}
                  title={section.title}
                  subtitle={section.subtitle}
                  count={people.length}
                >
                  <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
                    {people.map((m) => (
                      <MemberCard
                        key={m.id}
                        member={m}
                        prominent={m.clubRole !== null && HIGHLIGHTED.includes(m.clubRole)}
                      />
                    ))}
                  </div>
                </SectionGroup>
              );
            })}

          {showOthers && others.length > 0 && (
            <SectionGroup
              title="MEMBERS"
              subtitle="Everyone else on the club platform."
              count={others.length}
            >
              <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
                {others.map((m) => (
                  <MemberCard key={m.id} member={m} />
                ))}
              </div>

              {/* Said plainly rather than implying the page is everyone. */}
              {!q && members.length < total - team.length && (
                <p className="mt-5 text-center font-mono text-xs text-fg-subtle">
                  Showing {members.length} of {total}. Search to find someone not listed.
                </p>
              )}
            </SectionGroup>
          )}
        </div>
      )}

      <BorderGlow className="mt-16" contentClassName="p-8 text-center">
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
      </BorderGlow>
    </div>
  );
}

function SectionGroup({
  title,
  subtitle,
  count,
  children,
}: {
  title: string;
  subtitle: string;
  count: number;
  children: React.ReactNode;
}) {
  return (
    <section>
      <div className="mb-5 flex flex-wrap items-baseline gap-x-3 gap-y-1 border-b border-border pb-3">
        <h2 className="font-mono text-xs font-bold tracking-caps-wide text-primary uppercase">
          {title}
        </h2>
        <span className="font-mono text-micro text-fg-subtle">{count}</span>
        <p className="w-full text-sm text-fg-muted sm:w-auto">{subtitle}</p>
      </div>
      {children}
    </section>
  );
}

/**
 * One member.
 *
 * The whole card is the link to their profile, via a stretched anchor on the
 * name rather than a wrapper: the platform capsules are links too, and an anchor
 * inside an anchor is invalid and behaves differently in every browser. The
 * capsules sit above the stretched link on the z axis so they stay clickable.
 */
function MemberCard({ member, prominent = false }: { member: PublicMember; prominent?: boolean }) {
  const rank = ratingToRank(member.rating);
  const color = rankColor(rank);

  const links = (
    [
      { platform: "codeforces" as const, value: member.codeforcesHandle, label: "Codeforces" },
      { platform: "linkedin" as const, value: member.linkedinUrl, label: "LinkedIn" },
    ] satisfies { platform: "codeforces" | "linkedin"; value: string | null; label: string }[]
  )
    .map((link) => ({ ...link, href: profileUrl(link.platform, link.value) }))
    .filter((link) => link.href !== null);

  return (
    <article
      className={`group relative flex flex-col justify-between rounded-panel border border-border bg-surface-2 p-5 transition-all hover:-translate-y-1 hover:border-hairline-strong hover:shadow-panel ${
        prominent
          ? "border-primary/40 bg-gradient-to-b from-primary/5 via-surface-2 to-surface-2"
          : ""
      }`}
    >
      <div>
        <div className="flex items-start justify-between gap-3">
          <div
            className="flex size-14 shrink-0 items-center justify-center overflow-hidden rounded-full border-2 bg-surface-3"
            style={{ borderColor: color }}
          >
            {member.avatarUrl ? (
              <Image
                src={member.avatarUrl}
                alt=""
                width={56}
                height={56}
                className="size-full rounded-full object-cover"
              />
            ) : (
              <span className="font-mono text-base font-bold text-foreground">
                {initialsOf(member.name)}
              </span>
            )}
          </div>

          {member.rating !== null && (
            <div className="text-right">
              <div className="font-mono text-sm font-bold" style={{ color }}>
                {member.rating}
              </div>
              <div className="font-mono text-nano text-fg-subtle uppercase">{rankName(rank)}</div>
            </div>
          )}
        </div>

        <div className="mt-4">
          <h3 className="truncate text-base font-bold tracking-tight" style={{ color }}>
            <Link
              href={`/profile/${member.id}`}
              className="after:absolute after:inset-0 group-hover:underline"
            >
              {member.name}
            </Link>
          </h3>
          {member.codeforcesHandle && (
            <p className="truncate font-mono text-xs text-fg-muted">@{member.codeforcesHandle}</p>
          )}
        </div>

        <div className="mt-3 flex flex-wrap items-center gap-2">
          {member.clubRole && (
            <span
              className="inline-flex items-center gap-1 rounded-full border px-2.5 py-0.5 font-mono text-micro font-medium uppercase"
              style={{
                borderColor: color,
                color,
                backgroundColor: `color-mix(in srgb, ${color} 8%, transparent)`,
              }}
            >
              <RankDot rank={rank} size={4} ring={false} />
              {CLUB_ROLE_LABELS[member.clubRole]}
            </span>
          )}
          {member.academicYear && (
            <span className="font-mono text-micro text-fg-subtle">
              {ACADEMIC_YEAR_LABELS[member.academicYear]}
            </span>
          )}
        </div>
      </div>

      {/* Straight to the platform, without going through the profile first. */}
      <div className="mt-4 border-t border-border/60 pt-3">
        {links.length > 0 ? (
          <div className="relative z-10 flex flex-wrap gap-2">
            {links.map((link) => (
              <a
                key={link.platform}
                href={link.href!}
                target="_blank"
                rel="noopener noreferrer"
                aria-label={`${member.name} on ${link.label}`}
                className="inline-flex items-center gap-1.5 rounded-full border border-border bg-background/50 px-2.5 py-1 font-mono text-micro text-fg-muted transition-colors hover:border-hairline-strong hover:text-foreground"
              >
                {/* The glyph carries the platform's own colour, so the two
                    capsules read apart at a glance instead of as grey pills. */}
                <span style={{ color: PROFILE_ACCENT[link.platform] }} className="flex">
                  <PlatformGlyph platform={link.platform} className="size-3.5" />
                </span>
                {link.label}
                <ExternalLink className="size-2.5 opacity-60" />
              </a>
            ))}
          </div>
        ) : (
          <p className="font-mono text-micro text-fg-subtle">No linked profiles yet</p>
        )}
      </div>
    </article>
  );
}
