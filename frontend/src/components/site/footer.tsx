import Link from "next/link";

import { site } from "@/lib/site";

const columns = [
  {
    heading: "Site",
    links: [
      { href: "/", label: "Home" },
      { href: "/about", label: "About" },
      { href: "/events", label: "Events" },
      { href: "/hall-of-fame", label: "Hall of Fame" },
    ],
  },
  {
    heading: "Members",
    links: [
      { href: "/blog", label: "Blog" },
      { href: "/members", label: "Directory" },
      // Leaderboard is intentionally absent until Role 3 builds /leaderboard —
      // a footer link to a 404 is worse than no link.
      { href: "/login", label: "Login" },
    ],
  },
];

const linkClass =
  "rounded-control text-sm text-fg-muted transition-colors hover:text-foreground focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-ring";

export function Footer() {
  return (
    <footer className="border-t border-hairline">
      <div className="mx-auto grid max-w-[1240px] grid-cols-1 gap-10 px-6 py-14 sm:grid-cols-2 lg:grid-cols-4">
        <div>
          <div className="flex items-baseline gap-2">
            <span className="text-sm font-semibold tracking-tight">{site.name}</span>
            <span className="font-mono text-xs font-medium tracking-wide text-fg-muted">
              {site.suffix}
            </span>
          </div>
          <p className="mt-3.5 max-w-[32ch] text-sm leading-relaxed text-fg-subtle">
            {site.tagline}
          </p>
        </div>

        {columns.map((col) => (
          <div key={col.heading} className="flex flex-col gap-3">
            <div className="font-mono text-[11px] tracking-[0.12em] text-fg-subtle uppercase">
              {col.heading}
            </div>
            {col.links.map((l) => (
              <Link key={l.href} href={l.href} className={linkClass}>
                {l.label}
              </Link>
            ))}
          </div>
        ))}

        <div className="flex flex-col gap-3">
          <div className="font-mono text-[11px] tracking-[0.12em] text-fg-subtle uppercase">
            Elsewhere
          </div>
          <a href={site.github} className={linkClass} target="_blank" rel="noopener noreferrer">
            GitHub org
          </a>
          <span className="text-sm leading-relaxed text-fg-subtle">
            [PLACEHOLDER] Codeforces group
          </span>
          <span className="text-sm leading-relaxed text-fg-subtle">
            [PLACEHOLDER] Contact for joining
          </span>
        </div>
      </div>

      <div className="mx-auto max-w-[1240px] px-6 pb-10">
        <div className="border-t border-hairline pt-6 font-mono text-[11px] tracking-[0.08em] text-fg-subtle uppercase">
          © {new Date().getFullYear()} {site.fullName}
        </div>
      </div>
    </footer>
  );
}
