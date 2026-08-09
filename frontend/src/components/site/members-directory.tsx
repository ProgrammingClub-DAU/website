"use client";

import { useState } from "react";
import { Search } from "lucide-react";

import { Input } from "@/components/ui/input";
import { FilterChips } from "@/components/site/filter-chips";
import { EmptyState, RankDot } from "@/components/site/primitives";
import { memberGroups, type Member } from "@/lib/content/members";

export function MembersDirectory({ members }: { members: Member[] }) {
  const [query, setQuery] = useState("");
  const [group, setGroup] = useState<string>("Everyone");

  const q = query.trim().toLowerCase();
  const visible = members.filter((m) => {
    const groupOk = group === "Everyone" || m.group === group;
    const queryOk =
      !q || m.name.toLowerCase().includes(q) || m.role.toLowerCase().includes(q);
    return groupOk && queryOk;
  });

  return (
    <>
      <div className="flex flex-wrap items-center gap-3">
        <div className="relative min-w-60 flex-1">
          <Search
            className="pointer-events-none absolute top-1/2 left-3.5 size-3.5 -translate-y-1/2 text-fg-subtle"
            aria-hidden
          />
          <Input
            type="search"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search by name"
            aria-label="Search members by name or role"
            className="h-10 rounded-control pl-9"
          />
        </div>
        <FilterChips
          label="Filter by group"
          options={memberGroups}
          value={group}
          onChange={setGroup}
        />
      </div>

      <p
        aria-live="polite"
        className="mt-5 font-mono text-[11px] tracking-[0.1em] text-fg-subtle uppercase"
      >
        {String(visible.length).padStart(2, "0")} of{" "}
        {String(members.length).padStart(2, "0")} shown
      </p>

      {visible.length === 0 ? (
        <div className="mt-8">
          <EmptyState
            title="No members match that search."
            hint="Clear the search box or switch back to Everyone."
          />
        </div>
      ) : (
        <ul className="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {visible.map((member) => (
            <li
              key={member.name}
              className="flex flex-col gap-4 rounded-panel border border-hairline bg-surface p-5.5 transition-all hover:-translate-y-0.5 hover:border-border hover:bg-surface-3 hover:shadow-panel"
            >
              <div className="flex items-center gap-4">
                <span className="flex size-16 shrink-0 items-center justify-center rounded-full border border-border bg-surface-2 font-mono text-lg text-fg-muted">
                  {member.initials}
                </span>
                <div className="min-w-0 flex-1">
                  <p className="text-base font-semibold tracking-tight text-pretty">
                    {member.name}
                  </p>
                  <span className="mt-2 inline-flex items-center gap-1.5 rounded-full border border-primary px-2 py-0.5 font-mono text-[10px] tracking-[0.1em] text-primary uppercase">
                    <RankDot rank={member.cf} size={5} ring={false} />
                    {member.role}
                  </span>
                  <p className="mt-2 font-mono text-[11px] tracking-[0.06em] text-fg-subtle uppercase">
                    {member.batch}
                  </p>
                </div>
              </div>
              <p className="border-t border-hairline pt-3.5 text-sm leading-[1.5] text-fg-muted text-pretty">
                {member.about}
              </p>
            </li>
          ))}
        </ul>
      )}
    </>
  );
}
