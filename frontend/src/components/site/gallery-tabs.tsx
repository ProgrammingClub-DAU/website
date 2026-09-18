/**
 * The two halves of the gallery, as tabs at the top of each.
 *
 * Batch photos used to be reachable only through a small "Batch photos" text
 * link under the gallery's introduction, which read as a label rather than a
 * way in, so most visitors never found them. Tabs make both halves visible
 * from either side, and the one you are on is marked.
 *
 * Plain links, not client-side tabs: each half is its own page with its own
 * address, and this works without JavaScript.
 */

import Link from "next/link";
import { Images, Users } from "lucide-react";

import { cn } from "@/lib/utils";

const TABS = [
  // `short` on phones: both full labels side by side overflow a 360px screen.
  { key: "events", href: "/gallery", label: "Events & achievements", short: "Events", icon: Images },
  { key: "batches", href: "/gallery/batches", label: "Batch photos", short: "Batch photos", icon: Users },
] as const;

export function GalleryTabs({ active }: { active: (typeof TABS)[number]["key"] }) {
  return (
    <nav
      aria-label="Gallery sections"
      className="mt-8 inline-flex max-w-full rounded-panel border border-border bg-surface-2 p-1"
    >
      {TABS.map(({ key, href, label, short, icon: Icon }) => {
        const current = key === active;
        return (
          <Link
            key={key}
            href={href}
            aria-current={current ? "page" : undefined}
            className={cn(
              "inline-flex items-center gap-2 rounded-control px-3.5 py-2 text-sm font-semibold whitespace-nowrap transition-colors sm:px-4",
              "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring",
              current
                ? "bg-primary text-primary-foreground shadow-sm"
                : "text-fg-muted hover:text-foreground"
            )}
          >
            <Icon className="size-4 shrink-0" aria-hidden />
            <span className="sm:hidden">{short}</span>
            <span className="hidden sm:inline">{label}</span>
          </Link>
        );
      })}
    </nav>
  );
}
