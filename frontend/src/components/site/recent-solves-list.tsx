import type { RecentSolve } from "@/types/api";
import { timeAgo } from "@/lib/practice";
import { cn, serverDate } from "@/lib/utils";

/** The latest Codeforces solves, newest first, each linking to the problem. */
export function RecentSolvesList({ solves, columns = 1 }: { solves: RecentSolve[]; columns?: 1 | 2 }) {
  if (solves.length === 0) {
    return <p className="text-sm text-fg-muted">No Codeforces solves synced yet.</p>;
  }
  return (
    <ul className={cn("divide-y divide-hairline", columns === 2 && "md:grid md:grid-cols-2 md:gap-x-8 md:divide-y-0")}>
      {solves.map((p) => (
        <li
          key={`${p.problemKey}-${p.solvedAt}`}
          className={cn("flex items-center justify-between gap-3 py-2.5", columns === 2 && "md:border-b md:border-hairline")}
        >
          <div className="min-w-0">
            {p.url ? (
              <a href={p.url} target="_blank" rel="noreferrer" className="block truncate text-sm font-medium text-foreground hover:text-primary">
                <span className="mr-2 font-mono text-xs text-fg-muted">{p.problemKey}</span>
                {p.name}
              </a>
            ) : (
              <span className="block truncate text-sm font-medium text-foreground">{p.name}</span>
            )}
            <span className="font-mono text-[11px] text-fg-subtle">{timeAgo(serverDate(p.solvedAt))}</span>
          </div>
          {p.rating && (
            <span className="shrink-0 rounded-md border border-amber-500/30 bg-amber-500/10 px-2 py-0.5 font-mono text-[11px] font-bold text-amber-700 dark:text-amber-300">
              ★ {p.rating}
            </span>
          )}
        </li>
      ))}
    </ul>
  );
}
