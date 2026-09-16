/**
 * The people who run the club.
 *
 * Two groups, because that is how the club is organised: the core team, and the
 * batch representatives. Convenor and Deputy Convenor sit inside the core team
 * rather than in a section of their own -- they are the top of it, not a
 * separate body.
 *
 * Each card says who someone is and where to find them: photo, name, post, and
 * a way through to Codeforces and LinkedIn. No rating, no rank title, no year of
 * study. This page answers "who is on the committee"; a Specialist badge beside
 * a Convenor answers a different question badly, and standing belongs on the
 * leaderboard.
 */

import Link from "next/link";
import Image from "next/image";
import { ExternalLink } from "lucide-react";

import { PlatformGlyph, PROFILE_ACCENT } from "@/components/site/platform-glyph";
import { CLUB_ROLE_LABELS } from "@/lib/club-roles";
import { profileUrl } from "@/lib/platform-profiles";
import type { ClubRole, PublicMember } from "@/types/api";

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
 * The page's two sections, top to bottom.
 *
 * The server returns the committee in hierarchy order, so within the core team
 * the Convenor comes first and the Associate Core last without this file
 * restating the ranking. All it decides is which posts share a heading.
 */
const SECTIONS: { title: string; posts: ClubRole[] }[] = [
  {
    title: "Core team",
    posts: ["CONVENOR", "DEPUTY_CONVENOR", "CORE", "ASSOCIATE_CORE"],
  },
  {
    title: "Batch representatives",
    posts: ["BATCH_REPRESENTATIVE"],
  },
];

/** The two senior posts, given a quiet accent so they read as the head of the team. */
const SENIOR: ClubRole[] = ["CONVENOR", "DEPUTY_CONVENOR"];

export function MembersDirectory({
  team,
  unreachable = false,
}: {
  team: PublicMember[];
  /** True when the server could not be reached, as opposed to having nobody to show. */
  unreachable?: boolean;
}) {
  if (unreachable) {
    /*
      An empty committee and an unreachable server are different facts, and
      saying the first when the second is true is a claim about the club.
    */
    return (
      <div className="rounded-panel border border-dashed border-destructive/40 bg-destructive/5 py-12 text-center">
        <p className="text-sm text-destructive">Could not load the committee.</p>
        <p className="mt-1 text-xs text-fg-muted">
          The server may be waking up. Reload in a few seconds.
        </p>
      </div>
    );
  }

  if (team.length === 0) {
    return (
      <div className="rounded-panel border border-dashed border-border py-12 text-center">
        <p className="text-sm text-fg-muted">No committee members listed yet.</p>
        <p className="mt-1 text-xs text-fg-subtle">
          People appear here once they are given a post in the admin panel.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-14">
      {SECTIONS.map((section) => {
        const people = team.filter((m) => m.clubRole && section.posts.includes(m.clubRole));
        if (people.length === 0) return null;

        return (
          <section key={section.title}>
            <div className="mb-6 flex items-baseline gap-3 border-b border-hairline pb-3">
              <h2 className="font-heading text-lg font-medium tracking-tight text-foreground">
                {section.title}
              </h2>
              <span className="font-mono text-micro text-fg-subtle">{people.length}</span>
            </div>

            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
              {people.map((member) => (
                <MemberCard key={member.id} member={member} />
              ))}
            </div>
          </section>
        );
      })}
    </div>
  );
}

/**
 * One member.
 *
 * The card links to their profile through a stretched anchor on the name rather
 * than a wrapper, because the platform capsules are links too and an anchor
 * inside an anchor is invalid and behaves differently in every browser. The
 * capsules sit above it on the z axis so they stay clickable.
 */
function MemberCard({ member }: { member: PublicMember }) {
  const senior = member.clubRole !== null && SENIOR.includes(member.clubRole);

  const links = (
    [
      { platform: "codeforces" as const, value: member.codeforcesHandle, label: "Codeforces" },
      { platform: "linkedin" as const, value: member.linkedinUrl, label: "LinkedIn" },
    ] as const
  )
    .map((link) => ({ ...link, href: profileUrl(link.platform, link.value) }))
    .filter((link) => link.href !== null);

  return (
    <article
      className={`group relative flex flex-col rounded-panel border bg-surface-2 p-5 transition-all hover:-translate-y-0.5 hover:border-hairline-strong hover:shadow-panel ${
        senior ? "border-primary/35" : "border-border"
      }`}
    >
      <div className="flex size-16 items-center justify-center overflow-hidden rounded-full border border-border bg-surface-3">
        {member.avatarUrl ? (
          <Image
            src={member.avatarUrl}
            alt=""
            width={64}
            height={64}
            className="size-full object-cover"
          />
        ) : (
          <span className="font-mono text-lg text-fg-muted">{initialsOf(member.name)}</span>
        )}
      </div>

      <h3 className="mt-4 text-base font-semibold tracking-tight text-foreground">
        <Link href={`/profile/${member.id}`} className="after:absolute after:inset-0">
          {member.name}
        </Link>
      </h3>

      {member.clubRole && (
        <p
          className={`mt-0.5 font-mono text-micro tracking-caps uppercase ${
            senior ? "text-primary" : "text-fg-muted"
          }`}
        >
          {CLUB_ROLE_LABELS[member.clubRole]}
        </p>
      )}

      {links.length > 0 && (
        <div className="relative z-10 mt-4 flex flex-wrap gap-2">
          {links.map((link) => (
            <a
              key={link.platform}
              href={link.href!}
              target="_blank"
              rel="noopener noreferrer"
              aria-label={`${member.name} on ${link.label}`}
              className="inline-flex items-center gap-1.5 rounded-full border border-border bg-background/40 px-2.5 py-1 font-mono text-micro text-fg-muted transition-colors hover:border-hairline-strong hover:text-foreground"
            >
              {/* The platform's own colour, so the two read apart at a glance. */}
              <span style={{ color: PROFILE_ACCENT[link.platform] }} className="flex">
                <PlatformGlyph platform={link.platform} className="size-3.5" />
              </span>
              {link.label}
              <ExternalLink className="size-2.5 opacity-50" />
            </a>
          ))}
        </div>
      )}
    </article>
  );
}
