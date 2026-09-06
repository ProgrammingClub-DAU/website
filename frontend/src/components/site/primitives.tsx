import Link from "next/link";
import type { ReactNode } from "react";

import { CF_RANKS, rankColor, type CfRankKey } from "@/lib/cf-ranks";
import { cn } from "@/lib/utils";
import GradientText from "@/components/site/gradient-text";

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
    <section className={cn("container-page", className)}>
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

/**
 * The heading every content page opens with.
 *
 * Eight pages repeated the same class string and would otherwise each need the
 * gradient applied by hand. Keeping it here means the treatment is one decision,
 * and the element stays a real `h1` — the gradient is applied to the text inside
 * it, never by replacing the heading.
 */
export function PageTitle({ children, className }: { children: ReactNode; className?: string }) {
  return (
    <h1
      className={cn(
        "mt-6 text-[clamp(2.125rem,5.4vw,3.5rem)] leading-[1.02] font-[510] tracking-[-0.02em] text-balance",
        className
      )}
    >
      <GradientText>{children}</GradientText>
    </h1>
  );
}
