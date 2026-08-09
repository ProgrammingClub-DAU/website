import Link from "next/link";
import type { ReactNode } from "react";

import { CF_RANKS, rankColor, type CfRankKey } from "@/lib/cf-ranks";
import { cn } from "@/lib/utils";

export function Eyebrow({ children, className }: { children: ReactNode; className?: string }) {
  return (
    <div
      className={cn(
        "font-mono text-[13px] tracking-[0.14em] text-fg-muted uppercase",
        className
      )}
    >
      {children}
    </div>
  );
}

export function SectionHeader({
  eyebrow,
  title,
  action,
  className,
}: {
  eyebrow?: string;
  title: string;
  action?: { href: string; label: string };
  className?: string;
}) {
  return (
    <div
      className={cn(
        "flex flex-wrap items-end justify-between gap-4",
        className
      )}
    >
      <div>
        {eyebrow && <Eyebrow>{eyebrow}</Eyebrow>}
        <h2 className="mt-4 text-[clamp(1.625rem,3.4vw,2.25rem)] font-semibold tracking-[-0.02em] text-balance">
          {title}
        </h2>
      </div>
      {action && (
        <Link
          href={action.href}
          className="rounded-control py-1.5 font-mono text-[13px] tracking-[0.06em] text-primary uppercase transition-colors hover:opacity-80 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-ring"
        >
          {action.label} →
        </Link>
      )}
    </div>
  );
}

/** The signature motif: a Codeforces rank color, used only as a small dot or ring. */
export function RankDot({
  rank,
  size = 8,
  ring = true,
  className,
}: {
  rank: CfRankKey;
  size?: number;
  ring?: boolean;
  className?: string;
}) {
  return (
    <span
      className={cn("block shrink-0 rounded-full", className)}
      style={{
        width: size,
        height: size,
        background: rankColor(rank),
        boxShadow: ring ? `0 0 0 3px color-mix(in srgb, ${rankColor(rank)} 16%, transparent)` : undefined,
      }}
    />
  );
}

export function RankLegend({ className }: { className?: string }) {
  return (
    <ul className={cn("flex flex-wrap gap-x-4 gap-y-2.5", className)}>
      {CF_RANKS.map((r) => (
        <li key={r.key} className="flex items-center gap-1.5">
          <span
            className="size-1.5 rounded-full"
            style={{ background: r.color }}
            aria-hidden
          />
          <span className="font-mono text-[11px] tracking-[0.08em] text-fg-subtle uppercase">
            {r.name}
          </span>
        </li>
      ))}
    </ul>
  );
}

export function Section({
  children,
  className,
}: {
  children: ReactNode;
  className?: string;
}) {
  return (
    <section className={cn("mx-auto max-w-[1240px] px-6", className)}>
      {children}
    </section>
  );
}

export function SampleBadge({ children = "Sample data" }: { children?: ReactNode }) {
  return (
    <span className="rounded-full border border-border px-2.5 py-1 font-mono text-[11px] tracking-[0.1em] text-fg-subtle uppercase">
      {children}
    </span>
  );
}

export function EmptyState({ title, hint }: { title: string; hint: string }) {
  return (
    <div className="rounded-panel border border-dashed border-border px-4 py-14 text-center">
      <p className="text-[17px] font-semibold">{title}</p>
      <p className="mt-2 text-[15px] text-fg-muted">{hint}</p>
    </div>
  );
}
