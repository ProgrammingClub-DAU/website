import type { ClubRole } from "@/types/api";
import { getClubRoleLabel } from "@/lib/club-roles";
import { Award, Target } from "lucide-react";
import { cn } from "@/lib/utils";

interface ClubRoleBadgeProps {
  clubRole: ClubRole | null;
  className?: string;
  showIcon?: boolean;
}

export function getClubRoleBadgeStyle(role: ClubRole | null): { bg: string; text: string; border: string } {
  switch (role) {
    case "CONVENOR":
      return { bg: "rgba(255,215,0,0.12)", text: "#ffd700", border: "rgba(255,215,0,0.35)" };
    case "DEPUTY_CONVENOR":
      return { bg: "rgba(192,192,192,0.12)", text: "#c0c0c0", border: "rgba(192,192,192,0.35)" };
    case "CORE":
      return { bg: "rgba(138,43,226,0.12)", text: "#aa6dff", border: "rgba(138,43,226,0.35)" };
    case "ASSOCIATE_CORE":
      return { bg: "rgba(30,144,255,0.12)", text: "#5ba3ff", border: "rgba(30,144,255,0.35)" };
    case "BATCH_REPRESENTATIVE":
      return { bg: "rgba(16,185,129,0.12)", text: "#10b981", border: "rgba(16,185,129,0.35)" };
    case "EX_PC_MEMBER":
    case "EX_CORE":
    case "EX_CDC":
      return { bg: "transparent", text: "var(--fg-muted)", border: "var(--border)" };
    case "STUDENT":
    default:
      return { bg: "rgba(128,128,128,0.08)", text: "var(--fg-muted)", border: "var(--border)" };
  }
}

export function ClubRoleBadge({ clubRole, className, showIcon = true }: ClubRoleBadgeProps) {
  const style = getClubRoleBadgeStyle(clubRole);
  const isOfficial = clubRole !== null && clubRole !== "STUDENT";
  const label = clubRole ? getClubRoleLabel(clubRole) : "Member";

  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 rounded-full border px-2.5 py-0.5 text-xs font-medium transition-colors",
        className
      )}
      style={{
        color: style.text,
        borderColor: style.border,
        backgroundColor: style.bg,
      }}
    >
      {showIcon && (isOfficial ? <Award className="size-3" /> : <Target className="size-3" />)}
      <span>{label}</span>
    </span>
  );
}

export default ClubRoleBadge;
