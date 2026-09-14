import Image from "next/image";
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
      <div className="container-page grid grid-cols-1 gap-10 py-14 sm:grid-cols-2 lg:grid-cols-4">
        <div>
          <div className="flex items-center gap-2.5">
            {/*
              Larger than the navbar mark (32px vs 26px): the footer is the one
              place the identity gets to sit still rather than compete with
              navigation. Still the monogram crop, not the full badge — the rim
              text is unreadable below roughly 100px.

              alt="" because the club name sits immediately beside it.
            */}
            <Image
              src="/logo-mark.png"
              alt=""
              width={32}
              height={32}
              className="size-8 shrink-0 rounded-full"
            />
            <span className="flex items-baseline gap-2">
              <span className="text-sm font-semibold tracking-tight">{site.name}</span>
              <span className="font-mono text-xs font-medium tracking-wide text-fg-muted">
                {site.suffix}
              </span>
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
          {/* A Codeforces group link and a contact line used to sit here as two
              greyed-out spans reading "[PLACEHOLDER]", on every page of the
              site, because neither destination was known. Dead entries in a
              footer are worse than a short footer: they advertise places to go
              and then refuse to go there. Add each back as a real <a> once its
              URL exists. */}
        </div>
      </div>

      <div className="container-page pb-10">
        <div className="border-t border-hairline pt-6 font-mono text-[11px] tracking-[0.08em] text-fg-subtle uppercase">
          © {new Date().getFullYear()} {site.fullName}
        </div>
      </div>
    </footer>
  );
}
