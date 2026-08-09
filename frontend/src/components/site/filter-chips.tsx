"use client";

import { cn } from "@/lib/utils";

export function FilterChips({
  options,
  value,
  onChange,
  label,
}: {
  options: readonly string[];
  value: string;
  onChange: (next: string) => void;
  label: string;
}) {
  return (
    <div role="group" aria-label={label} className="flex flex-wrap gap-2">
      {options.map((option) => {
        const active = option === value;
        return (
          <button
            key={option}
            type="button"
            aria-pressed={active}
            onClick={() => onChange(option)}
            className={cn(
              "cursor-pointer rounded-full border px-3.5 py-1.5 font-mono text-xs tracking-[0.08em] whitespace-nowrap uppercase transition-colors",
              "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring",
              active
                ? "border-primary bg-primary/15 text-foreground"
                : "border-border text-fg-muted hover:border-hairline-strong hover:text-foreground"
            )}
          >
            {option}
          </button>
        );
      })}
    </div>
  );
}
