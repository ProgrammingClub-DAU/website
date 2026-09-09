import type { ClubRole } from "@/types/api";

export const CLUB_ROLE_LABELS: Record<ClubRole, string> = {
  CONVENOR: "Convenor",
  DEPUTY_CONVENOR: "Deputy Convenor",
  CORE: "Core Member",
  ASSOCIATE_CORE: "Associate Core Member",
  BATCH_REPRESENTATIVE: "Batch Representative",
  EX_PC_MEMBER: "Ex PC Member",
  EX_CORE: "Ex Core",
  EX_CDC: "Ex CDC",
  STUDENT: "Club Participant",
};

export function getClubRoleLabel(clubRole: ClubRole): string {
  return CLUB_ROLE_LABELS[clubRole];
}
