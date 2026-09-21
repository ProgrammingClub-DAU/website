import type { ClubRole } from "@/types/api";
import { getClubRoleLabel } from "@/lib/club-roles";
import { cn } from "@/lib/utils";

interface ClubRoleBadgeProps {
  clubRole: ClubRole | null;
  className?: string;
  showIcon?: boolean;
}

export function getClubRoleBadgeStyle(role: ClubRole | null): { bg: string; text: string; border: string; dot: string } {
  switch (role) {
    case "CONVENOR":
      return {
        bg: "rgba(245, 158, 11, 0.12)",
        text: "rgba(253, 230, 138, 0.95)",
        border: "rgba(245, 158, 11, 0.35)",
        dot: "#f59e0b",
      };
    case "DEPUTY_CONVENOR":
      return {
        bg: "rgba(169, 102, 255, 0.12)",
        text: "rgba(241, 227, 255, 0.95)",
        border: "rgba(169, 102, 255, 0.35)",
        dot: "#a966ff",
      };
    case "CORE":
      return {
        bg: "rgba(61, 126, 255, 0.12)",
        text: "rgba(217, 230, 255, 0.95)",
        border: "rgba(61, 126, 255, 0.35)",
        dot: "#3d7eff",
      };
    case "ASSOCIATE_CORE":
      return {
        bg: "rgba(24, 181, 201, 0.12)",
        text: "rgba(203, 247, 252, 0.95)",
        border: "rgba(24, 181, 201, 0.35)",
        dot: "#18b5c9",
      };
    case "BATCH_REPRESENTATIVE":
      return {
        bg: "rgba(47, 157, 100, 0.12)",
        text: "rgba(210, 247, 227, 0.95)",
        border: "rgba(47, 157, 100, 0.35)",
        dot: "#2f9d64",
      };
    case "EX_PC_MEMBER":
    case "EX_CORE":
    case "EX_CDC":
      return {
        bg: "rgba(255, 255, 255, 0.03)",
        text: "rgba(148, 163, 184, 0.8)",
        border: "rgba(255, 255, 255, 0.1)",
        dot: "#94a3b8",
      };
    case "STUDENT":
    default:
      return {
        bg: "rgba(255, 255, 255, 0.04)",
        text: "rgba(203, 213, 225, 0.8)",
        border: "rgba(255, 255, 255, 0.1)",
        dot: "#64748b",
      };
  }
}

export function ClubRoleBadge({ clubRole, className, showIcon = true }: ClubRoleBadgeProps) {
  const style = getClubRoleBadgeStyle(clubRole);
  const label = clubRole ? getClubRoleLabel(clubRole) : "Member";

  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 rounded-full border px-3 py-0.5 text-xs font-medium tracking-wide shadow-[inset_0_1px_0_rgba(255,255,255,0.1)] backdrop-blur-md transition-colors",
        className
      )}
      style={{
        color: style.text,
        borderColor: style.border,
        backgroundColor: style.bg,
      }}
    >
      {showIcon && (
        <span
          className="size-1.5 rounded-full bg-current opacity-80 ring-2 ring-current/25 shrink-0"
          aria-hidden="true"
        />
      )}
      <span>{label}</span>
    </span>
  );
}

export default ClubRoleBadge;

