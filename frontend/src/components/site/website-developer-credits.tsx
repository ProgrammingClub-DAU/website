import Link from "next/link";
import Image from "next/image";
import { ArrowUpRight, Globe, Sparkles, Crown } from "lucide-react";

import { GitHubMark } from "@/components/site/github-mark";
import { PlatformGlyph } from "@/components/site/platform-glyph";
import { CONTRIBUTOR_PROFILES } from "@/lib/content/members";
import { profileUrl } from "@/lib/platform-profiles";
import type { PublicMember } from "@/types/api";

function getInitials(name: string): string {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .map((part) => part[0])
    .join("")
    .slice(0, 2)
    .toUpperCase();
}

/** Pre-computed rich gradient palettes for avatar rings */
const AVATAR_GRADIENTS = [
  "from-rose-500 via-purple-600 to-indigo-600",
  "from-indigo-500 via-blue-600 to-cyan-500",
  "from-violet-600 via-fuchsia-600 to-rose-500",
  "from-cyan-400 via-blue-600 to-indigo-700",
  "from-amber-500 via-rose-600 to-purple-600",
  "from-emerald-400 via-teal-600 to-indigo-700",
];

/** Hardcoded creator emails list as specified */
export const CREATOR_EMAILS = [
  "202401152@dau.ac.in",
  "202401474@dau.ac.in",
  "202401226@dau.ac.in",
  "202401041@dau.ac.in",
  "202401178@dau.ac.in",
  "202403019@dau.ac.in",
];

interface WebsiteDeveloperCreditsProps {
  creators?: PublicMember[];
}

export function WebsiteDeveloperCredits({ creators = [] }: WebsiteDeveloperCreditsProps) {
  return (
    <section
      id="credits"
      className="relative overflow-hidden rounded-3xl border border-rose-500/25 bg-[#090912]/90 backdrop-blur-2xl shadow-[0_25px_60px_-15px_rgba(0,0,0,0.8)]"
    >
      {/* ── Ambient Background Lighting ── */}
      <div
        aria-hidden="true"
        className="pointer-events-none absolute inset-0 -z-10 bg-[radial-gradient(ellipse_90%_70%_at_50%_-10%,rgba(244,63,94,0.12),rgba(99,102,241,0.06),transparent)]"
      />
      <div
        aria-hidden="true"
        className="pointer-events-none absolute bottom-0 right-0 -z-10 size-96 rounded-full bg-rose-600/10 blur-3xl"
      />

      {/* ── Header Section ── */}
      <div className="flex flex-col gap-4 border-b border-white/10 px-6 py-8 sm:flex-row sm:items-center sm:justify-between sm:px-10 sm:py-10">
        <div>
          <div className="inline-flex items-center gap-2 rounded-full border border-rose-500/35 bg-gradient-to-r from-rose-500/15 to-purple-500/15 px-3.5 py-1 font-mono text-[11px] font-bold uppercase tracking-widest text-rose-400 shadow-[0_0_15px_rgba(244,63,94,0.25)]">
            <Crown className="size-3.5 text-rose-400" />
            <span>Platform Creators • VIP Showcase</span>
          </div>

          <h2 className="mt-3 font-heading text-3xl font-black tracking-tight text-white sm:text-4xl bg-gradient-to-r from-white via-rose-100 to-purple-200 bg-clip-text text-transparent">
            Who Built This Website
          </h2>
          <p className="mt-2 text-sm md:text-base text-white/70 max-w-2xl">
            The 6 platform engineers who designed, architected, and built this entire web application.
          </p>
        </div>

        <a
          href="https://github.com/ProgrammingClub-DAU/website"
          target="_blank"
          rel="noopener noreferrer"
          className="inline-flex shrink-0 items-center gap-2 rounded-2xl border border-white/15 bg-white/[0.05] px-5 py-3 font-mono text-xs font-semibold text-white transition-all duration-300 hover:border-rose-500/50 hover:bg-rose-500/10 hover:shadow-[0_0_20px_rgba(244,63,94,0.3)]"
        >
          <GitHubMark className="size-4" />
          <span>View Source on GitHub</span>
          <ArrowUpRight className="size-4 opacity-70" />
        </a>
      </div>

      {/* ── VIP Creators Grid ── */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 sm:gap-6 p-6 sm:p-8">
        {CONTRIBUTOR_PROFILES.map((contributor, index) => {
          const avatarGradient = AVATAR_GRADIENTS[index % AVATAR_GRADIENTS.length];

          // Match by name or index from backend creators array
          const matchedCreator = creators.find(
            (c) =>
              c.name.toLowerCase().trim().includes(contributor.name.toLowerCase().trim()) ||
              contributor.name.toLowerCase().trim().includes(c.name.toLowerCase().trim()),
          ) ?? creators[index];

          const profileHref = matchedCreator?.id ? `/profile/${matchedCreator.id}` : undefined;
          const displayName = matchedCreator?.name || contributor.name;

          const PLATFORMS = [
            { platform: "codeforces" as const, value: matchedCreator?.codeforcesHandle, label: "Codeforces" },
            { platform: "github" as const, value: matchedCreator?.githubUrl, label: "GitHub" },
            { platform: "linkedin" as const, value: matchedCreator?.linkedinUrl, label: "LinkedIn" },
            { platform: "leetcode" as const, value: matchedCreator?.leetcodeHandle, label: "LeetCode" },
          ] as const;

          const links = PLATFORMS
            .map((l) => ({ ...l, href: profileUrl(l.platform, l.value ?? null) }))
            .filter((l) => l.href !== null);

          return (
            <div
              key={contributor.name}
              className={`group relative flex flex-col justify-between overflow-hidden rounded-2xl border border-white/10 bg-[#12121e]/80 p-6 backdrop-blur-xl transition-all duration-300 hover:-translate-y-1.5 hover:border-rose-500/50 hover:bg-[#161626] hover:shadow-[0_20px_40px_-10px_rgba(244,63,94,0.3)] ${
                profileHref ? "cursor-pointer" : ""
              }`}
            >
              {/* Glowing top border beam on hover */}
              <div
                aria-hidden="true"
                className="absolute inset-x-0 top-0 h-0.5 bg-gradient-to-r from-rose-500 via-purple-500 to-cyan-400 opacity-60 transition-opacity duration-300 group-hover:opacity-100"
              />

              {/* ── Top Row: Avatar + Name + Equal Builder Role ── */}
              <div>
                <div className="flex items-center gap-4">
                  {/* Avatar Container with Gradient Ring & VIP Badge */}
                  <div className="relative shrink-0">
                    <div
                      className={`relative size-16 rounded-full p-[2.5px] bg-gradient-to-br ${avatarGradient} shadow-md transition-transform duration-300 group-hover:scale-105 group-hover:shadow-[0_0_20px_rgba(244,63,94,0.4)]`}
                    >
                      <div className="size-full overflow-hidden rounded-full bg-[#0b0b14]">
                        {matchedCreator?.avatarUrl ? (
                          <Image
                            src={matchedCreator.avatarUrl}
                            alt={displayName}
                            width={64}
                            height={64}
                            className="size-full object-cover"
                          />
                        ) : (
                          <div
                            className={`flex size-full items-center justify-center bg-gradient-to-br ${avatarGradient} font-mono font-extrabold text-lg text-white`}
                          >
                            {getInitials(displayName)}
                          </div>
                        )}
                      </div>
                    </div>
                    {/* VIP Crown Overlay */}
                    <div className="absolute -top-1 -right-1 flex size-5 items-center justify-center rounded-full bg-rose-600 text-white shadow-md ring-2 ring-[#0b0b14]">
                      <Crown className="size-3" />
                    </div>
                  </div>

                  {/* Name + Equal Builder Title */}
                  <div className="min-w-0 flex-1">
                    <h3 className="font-heading text-lg font-bold tracking-tight text-white leading-tight truncate transition-colors duration-200 group-hover:text-rose-300">
                      {profileHref ? (
                        <Link href={profileHref} className="after:absolute after:inset-0">
                          {displayName}
                        </Link>
                      ) : (
                        displayName
                      )}
                    </h3>

                    {/* Equal Builder Title — No hierarchy, all builders are equal */}
                    <div className="mt-1.5 inline-flex items-center gap-1.5 rounded-full border border-rose-500/30 bg-gradient-to-r from-rose-500/15 to-purple-500/15 px-2.5 py-0.5 font-mono text-[10px] font-semibold text-rose-300 shadow-[0_0_10px_rgba(244,63,94,0.2)]">
                      <Sparkles className="size-3 text-rose-400 shrink-0" />
                      <span>Website Creator</span>
                    </div>
                  </div>
                </div>

                {/* Optional headline preview if desired */}
                {contributor.headline && (
                  <p className="mt-4 text-xs text-white/60 line-clamp-2 leading-relaxed">
                    {contributor.headline}
                  </p>
                )}
              </div>

              {/* ── Bottom Row: Platform Icon Links + View Profile CTA ── */}
              <div className="relative z-10 mt-6 flex items-center justify-between border-t border-white/10 pt-4">
                {/* Platform Links */}
                <div className="flex items-center gap-2">
                  {links.length > 0 ? (
                    links.slice(0, 4).map((link) => (
                      <a
                        key={link.platform}
                        href={link.href!}
                        target="_blank"
                        rel="noopener noreferrer"
                        aria-label={`${displayName} on ${link.label}`}
                        title={link.label}
                        onClick={(e) => e.stopPropagation()}
                        className="inline-flex size-7 items-center justify-center rounded-lg border border-white/15 bg-white/5 text-white/60 transition-all duration-200 hover:scale-110 hover:border-rose-400 hover:bg-rose-500/20 hover:text-white hover:shadow-[0_0_10px_rgba(244,63,94,0.3)]"
                      >
                        <PlatformGlyph platform={link.platform} className="size-3.5" />
                      </a>
                    ))
                  ) : (
                    <span className="font-mono text-[9px] text-white/30 uppercase tracking-wider">
                      No profiles linked
                    </span>
                  )}
                </div>

                {/* View Profile Link */}
                {profileHref ? (
                  <Link
                    href={profileHref}
                    className="inline-flex items-center gap-1 rounded-full border border-rose-500/30 bg-rose-500/15 px-3 py-1 font-mono text-xs font-semibold text-rose-300 transition-all duration-200 hover:border-rose-400 hover:bg-rose-500/30 hover:text-white hover:shadow-[0_0_12px_rgba(244,63,94,0.35)]"
                  >
                    View Profile
                    <ArrowUpRight className="size-3" />
                  </Link>
                ) : (
                  <span className="font-mono text-[9px] text-white/30 uppercase tracking-wider">
                    Profile unavailable
                  </span>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </section>
  );
}

