/**
 * A feature that is being built but is not live yet.
 *
 * One card, used everywhere the site mentions unfinished work, so every such
 * place says the same honest thing: this is in development. No dates, because
 * none are promised, and no sample numbers, because a preview filled with
 * invented data reads as real data.
 *
 * `items` are shown as plain labels, never as buttons. A control that looks
 * clickable and does nothing is worse than no control.
 *
 * No "use client": it has no state, so it renders inside server pages and
 * client components alike.
 */

import { Hammer } from "lucide-react";

import { cn } from "@/lib/utils";

export function InDevelopment({
  title,
  body,
  items,
  headingLevel = "h2",
  className,
}: {
  title: string;
  body?: string;
  /** What is coming, as short labels. */
  items?: readonly string[];
  /** Match the page outline: h2 in a page section, h3 inside a card grid. */
  headingLevel?: "h2" | "h3";
  className?: string;
}) {
  const Heading = headingLevel;
  return (
    <div
      className={cn(
        "rounded-panel border border-dashed border-border bg-surface/60 p-6 sm:p-7",
        className
      )}
    >
      <span className="inline-flex items-center gap-1.5 rounded-full border border-border px-2.5 py-1 font-mono text-micro tracking-caps-wide text-fg-subtle uppercase">
        <Hammer className="size-3" aria-hidden />
        In development
      </span>
      <Heading className="mt-4 text-lg font-semibold tracking-tight text-pretty">{title}</Heading>
      {body && (
        <p className="mt-2 max-w-[60ch] text-body leading-[1.5] text-fg-muted text-pretty">
          {body}
        </p>
      )}
      {items && items.length > 0 && (
        <ul className="mt-5 flex flex-wrap gap-2">
          {items.map((item) => (
            <li
              key={item}
              className="rounded-full border border-hairline bg-surface-2 px-3 py-1 text-xs text-fg-muted"
            >
              {item}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
