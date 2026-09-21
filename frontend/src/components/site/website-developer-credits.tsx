import Link from "next/link";
import Image from "next/image";
import { ArrowUpRight, Globe } from "lucide-react";

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

/** Subtle gradient palette — one per card slot, avoids neon overload */
const AVATAR_GRADIENTS = [
  "from-indigo-600 via-violet-600 to-purple-700",
  "from-blue-600 via-indigo-600 to-violet-700",
  "from-violet-600 via-purple-600 to-fuchsia-700",
  "from-sky-500 via-blue-600 to-indigo-700",
  "from-indigo-500 via-blue-600 to-cyan-700",
  "from-purple-600 via-violet-700 to-indigo-800",
];

interface WebsiteDeveloperCreditsProps {
  creators?: PublicMember[];
}

export function WebsiteDeveloperCredits({ creators = [] }: WebsiteDeveloperCreditsProps) {
  return (
    <section id="credits" className="relative overflow-hidden rounded-2xl border border-border/40 bg-surface/40 backdrop-blur-xl shadow-2xl">
      {/* Subtle ambient background */}
      <div
        aria-hidden="true"
        className="pointer-events-none absolute inset-0 -z-10 bg-[radial-gradient(ellipse_80%_60%_at_50%_0%,rgba(99,102,241,0.07),transparent)]"
      />

      {/* Header */}
      <div className="flex flex-col gap-4 border-b border-border/40 px-6 py-6 sm:flex-row sm:items-center sm:justify-between sm:px-10 sm:py-8">
        <div>
          <div className="inline-flex items-center gap-2 rounded-full border border-indigo-500/25 bg-indigo-500/10 px-3 py-1 font-mono text-[10px] uppercase tracking-widest text-indigo-400 mb-3">
            <Globe className="size-3" />
            Open Source
          </div>
          <h2 className="font-heading text-2xl font-bold tracking-tight text-foreground sm:text-3xl">
            Who Built This Website
          </h2>
          <p className="mt-1.5 text-sm text-fg-muted">
            The engineers who designed and developed this platform.
          </p>
        </div>

        <a
          href="https://github.com/ProgrammingClub-DAU/website"
          target="_blank"
          rel="noopener noreferrer"
          className="inline-flex shrink-0 items-center gap-2 rounded-xl border border-border/50 bg-surface/60 px-4 py-2.5 font-mono text-xs text-foreground/80 transition-all duration-200 hover:border-border hover:bg-surface hover:text-foreground hover:shadow-lg"
        >
          <GitHubMark className="size-4" />
          <span>View on GitHub</span>
          <ArrowUpRight className="size-3.5 opacity-60" />
        </a>
      </div>

      {/* Developer Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-px bg-border/30 p-px">
        {CONTRIBUTOR_PROFILES.map((contributor, index) => {
          const avatarGradient = AVATAR_GRADIENTS[index % AVATAR_GRADIENTS.length];

          const matchedCreator = creators.find(
            (c) =>
              c.name.toLowerCase().trim().includes(contributor.name.toLowerCase().trim()) ||
              contributor.name.toLowerCase().trim().includes(c.name.toLowerCase().trim()),
          );

          const profileHref = matchedCreator?.id ? `/profile/${matchedCreator.id}` : undefined;

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
              className="group relative flex flex-col gap-4 bg-surface/60 p-6 transition-all duration-200 hover:bg-surface/90 first:rounded-tl-[11px] last:rounded-br-[11px]"
            >
              {/* Top row: avatar + name + badge */}
              <div className="flex items-center gap-4">
                {/* Avatar */}
                <div
                  className="relative shrink-0 overflow-hidden rounded-full ring-2 ring-white/10 transition-all duration-300 group-hover:ring-indigo-500/40 group-hover:shadow-[0_0_16px_rgba(99,102,241,0.25)]"
                  style={{ width: "64px", height: "64px" }}
                >
                  {matchedCreator?.avatarUrl ? (
                    <Image
                      src={matchedCreator.avatarUrl}
                      alt={contributor.name}
                      width={64}
                      height={64}
                      className="size-full rounded-full object-cover"
                    />
                  ) : (
                    <div
                      className={`flex size-full items-center justify-center bg-gradient-to-br ${avatarGradient} font-mono font-bold text-lg text-white`}
                    >
                      {getInitials(contributor.name)}
                    </div>
                  )}
                </div>

                {/* Name + role tag */}
                <div className="min-w-0 flex-1">
                  <h3 className="font-heading text-base font-bold tracking-tight text-foreground leading-tight truncate group-hover:text-indigo-300 transition-colors duration-200">
                    {contributor.name}
                  </h3>
                  <span className="mt-1.5 inline-flex items-center gap-1.5 rounded-full border border-indigo-500/25 bg-indigo-500/10 px-2.5 py-0.5 text-[10px] font-medium tracking-wide text-indigo-400">
                    <span className="size-1 rounded-full bg-indigo-400 shrink-0" aria-hidden="true" />
                    Website Architect
                  </span>
                </div>
              </div>

              {/* Bottom row: platform links + profile CTA */}
              <div className="flex items-center justify-between border-t border-white/[0.06] pt-4">
                {/* Platform icon links */}
                <div className="flex items-center gap-2">
                  {links.length > 0 ? (
                    links.slice(0, 4).map((link) => (
                      <a
                        key={link.platform}
                        href={link.href!}
                        target="_blank"
                        rel="noopener noreferrer"
                        aria-label={`${contributor.name} on ${link.label}`}
                        title={link.label}
                        className="inline-flex size-7 items-center justify-center rounded-lg border border-white/10 bg-white/5 text-white/50 transition-all duration-200 hover:scale-110 hover:border-indigo-500/40 hover:bg-indigo-500/10 hover:text-indigo-300"
                      >
                        <PlatformGlyph platform={link.platform} className="size-3.5" />
                      </a>
                    ))
                  ) : (
                    <span className="font-mono text-[9px] text-white/25 uppercase tracking-wider">
                      No profiles
                    </span>
                  )}
                </div>

                {/* Profile link CTA */}
                {profileHref ? (
                  <Link
                    href={profileHref}
                    className="inline-flex items-center gap-1.5 rounded-full border border-indigo-500/25 bg-indigo-500/10 px-3 py-1 text-xs font-medium text-indigo-300 transition-all duration-200 hover:border-indigo-500/50 hover:bg-indigo-500/20 hover:text-indigo-200"
                  >
                    View Profile
                    <ArrowUpRight className="size-3" />
                  </Link>
                ) : (
                  <span className="font-mono text-[9px] text-white/20 uppercase tracking-wider">
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
