"use client";

import { useEffect, useState } from "react";
import { Lock, Target } from "lucide-react";
import { cpGroupService } from "@/lib/services/cp-group";
import type { CpGroupMyMembership } from "@/types/api";

/**
 * "CP Practice Group" on a member's own profile, when they are in it. Rendered for
 * the owner only: the group is private, so nobody else sees who is in it, and the
 * API only ever answers for the signed-in member.
 */
export function CpGroupBadge() {
  const [membership, setMembership] = useState<CpGroupMyMembership | null>(null);

  useEffect(() => {
    let ignore = false;
    cpGroupService
      .myMembership()
      .then((m) => {
        if (!ignore) setMembership(m);
      })
      .catch(() => undefined);
    return () => {
      ignore = true;
    };
  }, []);

  if (!membership?.member) return null;

  return (
    <span
      title="You're in the club's CP Practice Group. Only you can see this badge."
      className="inline-flex items-center gap-1.5 rounded-full border border-primary/40 bg-gradient-to-r from-primary/15 to-amber-500/15 px-3 py-0.5 text-xs font-semibold text-foreground"
    >
      <Target className="size-3 text-primary" aria-hidden />
      CP Practice Group
      {membership.batchYear != null && (
        <span className="font-mono text-[10px] font-medium text-fg-muted">· {membership.batchYear}</span>
      )}
      <Lock className="size-2.5 text-fg-subtle" aria-label="Only you can see this" />
    </span>
  );
}
