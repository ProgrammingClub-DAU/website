"use client";

/**
 * The people who run the club.
 *
 * Displays the committee grouped into two clean sections:
 * 1. Core Team (Convenor, Deputy Convenor, Core, Associate Core)
 * 2. Batch Representatives (All batch reps)
 *
 * Each member is presented in a square-ish dark glass card with a 128px avatar,
 * name, role badge, and 4 platform links.
 */

import Link from "next/link";
import Image from "next/image";
import { Users, Shield, GraduationCap } from "lucide-react";

import { PlatformGlyph } from "@/components/site/platform-glyph";
import { ClubRoleBadge } from "@/components/ui/club-role-badge";
import { profileUrl } from "@/lib/platform-profiles";
import type { PublicMember } from "@/types/api";

function initialsOf(name: string): string {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .map((part) => part[0])
    .join("")
    .slice(0, 2)
    .toUpperCase();
}

/** Pre-computed unique vibrant gradient combinations for avatar placeholders */
const AVATAR_GRADIENTS = [
  "from-indigo-600 via-purple-600 to-pink-500",
  "from-cyan-500 via-blue-600 to-indigo-700",
  "from-emerald-500 via-teal-600 to-cyan-700",
  "from-amber-500 via-orange-600 to-rose-600",
  "from-fuchsia-600 via-purple-600 to-blue-600",
  "from-blue-600 via-indigo-600 to-violet-700",
];

const CORE_ROLE_ORDER: Record<string, number> = {
  CONVENOR: 1,
  DEPUTY_CONVENOR: 2,
  CORE: 3,
  ASSOCIATE_CORE: 4,
};

export function MembersDirectory({
  team,
  unreachable = false,
}: {
  team: PublicMember[];
  unreachable?: boolean;
}) {
  if (unreachable && team.length === 0) {
    return (
      <div className="rounded-2xl border border-dashed border-destructive/40 bg-destructive/5 p-12 text-center backdrop-blur-sm">
        <p className="text-base font-semibold text-destructive">Could not load the committee.</p>
        <p className="mt-2 text-sm text-fg-muted">
          The server may be waking up or synchronizing. Please refresh in a few seconds.
        </p>
      </div>
    );
  }

  if (team.length === 0) {
    return (
      <div className="rounded-2xl border border-dashed border-border p-12 text-center backdrop-blur-sm">
        <Users className="mx-auto size-10 text-fg-subtle opacity-60" />
        <p className="mt-3 text-base font-semibold text-fg-muted">No committee members listed yet.</p>
        <p className="mt-1 text-sm text-fg-subtle">
          Members will appear here once appointed in the administration panel.
        </p>
      </div>
    );
  }

  // 1. Core Members: Convenor, Deputy Convenor, Core, Associate Core
  const coreMembers = team
    .filter((m) => m.clubRole && ["CONVENOR", "DEPUTY_CONVENOR", "CORE", "ASSOCIATE_CORE"].includes(m.clubRole))
    .sort((a, b) => {
      const orderA = CORE_ROLE_ORDER[a.clubRole || ""] || 99;
      const orderB = CORE_ROLE_ORDER[b.clubRole || ""] || 99;
      if (orderA !== orderB) return orderA - orderB;
      return a.id - b.id;
    });

  // 2. Batch Representatives
  const batchReps = team
    .filter((m) => m.clubRole === "BATCH_REPRESENTATIVE")
    .sort((a, b) => a.id - b.id);

  // 3. Fallback for any other committee members (if added in the future)
  const otherMembers = team.filter(
    (m) =>
      !m.clubRole ||
      (!["CONVENOR", "DEPUTY_CONVENOR", "CORE", "ASSOCIATE_CORE", "BATCH_REPRESENTATIVE"].includes(m.clubRole))
  );

  return (
    <div className="space-y-16">
      {/* ── Section 1: Core Team ── */}
      {coreMembers.length > 0 && (
        <section>
          <div className="mb-8 flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
            <div className="flex items-center gap-3">
              <div className="flex size-9 items-center justify-center rounded-xl border border-white/10 bg-white/5 text-white/80">
                <Shield className="size-4" />
              </div>
              <div>
                <h2 className="font-heading text-2xl font-bold tracking-tight text-white sm:text-3xl">
                  Core Team
                </h2>
                {/* <p className="text-xs text-white/50">
                  Convenor, Deputy Convenor, Core &amp; Associate Core Members
                </p> */}
              </div>
            </div>
            <div className="hidden sm:block h-px flex-1 ml-6 bg-gradient-to-r from-white/15 to-transparent" />
          </div>

          <div className="flex flex-wrap justify-center gap-6">
            {coreMembers.map((member, index) => (
              <MemberCard
                key={member.id}
                member={member}
                avatarGradient={AVATAR_GRADIENTS[index % AVATAR_GRADIENTS.length]}
              />
            ))}
          </div>
        </section>
      )}

      {/* ── Section 2: Batch Representatives ── */}
      {batchReps.length > 0 && (
        <section>
          <div className="mb-8 flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
            <div className="flex items-center gap-3">
              <div className="flex size-9 items-center justify-center rounded-xl border border-white/10 bg-white/5 text-white/80">
                <GraduationCap className="size-4" />
              </div>
              <div>
                <h2 className="font-heading text-2xl font-bold tracking-tight text-white sm:text-3xl">
                  Batch Representatives
                </h2>
                {/* <p className="text-xs text-white/50">
                  Student Year Liaisons &amp; Batch Ambassadors
                </p> */}
              </div>
            </div>
            <div className="hidden sm:block h-px flex-1 ml-6 bg-gradient-to-r from-white/15 to-transparent" />
          </div>

          <div className="flex flex-wrap justify-center gap-6">
            {batchReps.map((member, index) => (
              <MemberCard
                key={member.id}
                member={member}
                avatarGradient={AVATAR_GRADIENTS[(index + 3) % AVATAR_GRADIENTS.length]}
              />
            ))}
          </div>
        </section>
      )}

      {/* ── Fallback: Other Committee Members ── */}
      {otherMembers.length > 0 && (
        <section>
          <div className="mb-8 flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
            <div className="flex items-center gap-3">
              <div className="flex size-9 items-center justify-center rounded-xl border border-white/10 bg-white/5 text-white/80">
                <Users className="size-4" />
              </div>
              <div>
                <h2 className="font-heading text-2xl font-bold tracking-tight text-white sm:text-3xl">
                  Committee Members
                </h2>
                <p className="text-xs text-white/50">
                  Student Leadership &amp; Operations
                </p>
              </div>
            </div>
            <div className="hidden sm:block h-px flex-1 ml-6 bg-gradient-to-r from-white/15 to-transparent" />
          </div>

          <div className="flex flex-wrap justify-center gap-6">
            {otherMembers.map((member, index) => (
              <MemberCard
                key={member.id}
                member={member}
                avatarGradient={AVATAR_GRADIENTS[index % AVATAR_GRADIENTS.length]}
              />
            ))}
          </div>
        </section>
      )}
    </div>
  );
}

/**
 * Uniform dark-glass member card — no neon, no role-based colors.
 * Centered 128px avatar, name, role badge, 4 platform links at bottom.
 */
function MemberCard({
  member,
  avatarGradient,
}: {
  member: PublicMember;
  avatarGradient: string;
}) {
  const PLATFORMS = [
    { platform: "codeforces" as const, value: member.codeforcesHandle, label: "Codeforces" },
    { platform: "linkedin" as const, value: member.linkedinUrl, label: "LinkedIn" },
    { platform: "github" as const, value: member.githubUrl, label: "GitHub" },
    { platform: "leetcode" as const, value: member.leetcodeHandle, label: "LeetCode" },
  ] as const;

  const links = PLATFORMS
    .map((l) => ({ ...l, href: profileUrl(l.platform, l.value) }))
    .filter((l) => l.href !== null);

  return (
    <article
      className={`
        group relative flex flex-col items-center
        rounded-2xl border border-white/[0.08] bg-white/[0.04] backdrop-blur-xl
        p-6 sm:p-7 pb-8 min-h-[360px]
        w-full sm:w-[calc(50%-14px)] lg:w-[calc(25%-18px)]
        transition-all duration-300 cursor-pointer
        hover:-translate-y-2 hover:border-white/[0.18] hover:bg-white/[0.07]
        hover:shadow-[0_20px_40px_-12px_rgba(0,0,0,0.7)]
      `}
    >
      {/* Subtle top edge highlight on hover */}
      <div
        aria-hidden="true"
        className="absolute inset-x-0 top-0 h-px rounded-t-2xl bg-gradient-to-r from-transparent via-white/25 to-transparent opacity-0 transition-opacity duration-300 group-hover:opacity-100"
      />

      {/* ─── Centered Avatar (152px) ─── */}
      <div className="mt-2 flex flex-col items-center gap-3.5 flex-1 w-full">
        <div
          className="relative shrink-0 overflow-hidden rounded-full p-[2.5px] bg-white/10 transition-transform duration-300 group-hover:scale-105 group-hover:bg-white/25 shadow-lg"
          style={{ width: "152px", height: "152px" }}
        >
          <div className="size-full overflow-hidden rounded-full bg-[#111318]">
            {member.avatarUrl ? (
              <Image
                src={member.avatarUrl}
                alt={member.name}
                width={152}
                height={152}
                className="size-full object-cover"
              />
            ) : (
              <div
                className={`flex size-full items-center justify-center bg-gradient-to-br ${avatarGradient} font-mono font-bold text-2xl text-white`}
              >
                {initialsOf(member.name)}
              </div>
            )}
          </div>
        </div>

        {/* Name */}
        <div className="text-center px-1">
          <h3 className="font-heading text-lg sm:text-xl font-bold tracking-tight text-white/90 transition-colors group-hover:text-white leading-tight line-clamp-2">
            <Link href={`/profile/${member.id}`} className="after:absolute after:inset-0">
              {member.name}
            </Link>
          </h3>

          {/* Role badge (tag only, no academic year) */}
          <div className="mt-2.5 flex items-center justify-center">
            <ClubRoleBadge clubRole={member.clubRole} />
          </div>
        </div>
      </div>

      {/* ─── Bottom: Platform Links ─── */}
      <div className="relative z-10 mt-auto pt-4 w-full border-t border-white/[0.08]">
        <div className="flex items-center justify-center gap-2.5">
          {links.length > 0 ? (
            links.slice(0, 4).map((link) => (
              <a
                key={link.platform}
                href={link.href!}
                target="_blank"
                rel="noopener noreferrer"
                aria-label={`${member.name} on ${link.label}`}
                title={link.label}
                className="inline-flex size-7 items-center justify-center rounded-lg border border-white/10 bg-white/5
                  text-white/50 transition-all duration-200 hover:scale-110 hover:border-white/25 hover:text-white hover:bg-white/10"
                onClick={(e) => e.stopPropagation()}
              >
                <PlatformGlyph platform={link.platform} className="size-3.5" />
              </a>
            ))
          ) : (
            <span className="font-mono text-[9px] text-white/25 uppercase tracking-wider">
              No profiles linked
            </span>
          )}
        </div>
      </div>
    </article>
  );
}

