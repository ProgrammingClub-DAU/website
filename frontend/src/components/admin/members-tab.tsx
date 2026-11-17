"use client";

// The Members tab of the admin dashboard. Moved out of admin/page.tsx, which
// held every tab in one 1,800-line file; the page now loads each tab on demand.
import { useState, useEffect, useCallback, useMemo } from "react";
import Link from "next/link";
import Image from "next/image";
import { useAuthStore, type ApiResponse } from "@/store/auth";
import apiClient from "@/lib/axios";
import { DataTable, type Column } from "@/components/ui/data-table";
import { ClubRoleBadge } from "@/components/ui/club-role-badge";
import { CLUB_ROLE_LABELS } from "@/lib/club-roles";
import { ACADEMIC_YEAR_LABELS, type AcademicYear, type ClubRole } from "@/types/api";
import {
  Users,
  Trash2,
  CheckCircle2,
  AlertTriangle,
  XCircle,
  Search,
} from "lucide-react";

const ALL_CLUB_ROLES: ClubRole[] = [
  "CONVENOR",
  "DEPUTY_CONVENOR",
  "CORE",
  "ASSOCIATE_CORE",
  "BATCH_REPRESENTATIVE",
  "EX_PC_MEMBER",
  "EX_CORE",
  "EX_CDC",
  "STUDENT",
];

/**
 * One row of GET /api/users/all, which returns UserResponseDto.
 *
 * Not the UserLookup shape used by the attendee panel: that one calls the
 * Codeforces rating cfRating, while this endpoint calls it rating, and it also
 * carries the platform role that the Promote and Demote buttons act on.
 */
interface AdminMember {
  id: number;
  name: string;
  email: string;
  avatarUrl: string | null;
  phoneNumber: string | null;
  codeforcesHandle: string | null;
  rating: number | null;
  leetcodeHandle: string | null;
  leetcodeRating: number | null;
  clubRole: ClubRole | null;
  batchYear: number | null;
  academicYear: AcademicYear | null;
  profileComplete: boolean;
  role: string;
}

// ══════════════════════════════════════════════════════════════════
// TAB 1: MEMBERS MANAGEMENT
// ══════════════════════════════════════════════════════════════════

export function MembersTab() {
  const { user: currentUser } = useAuthStore();
  const [members, setMembers] = useState<AdminMember[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [actionLoadingId, setActionLoadingId] = useState<number | null>(null);
  const [statusMessage, setStatusMessage] = useState<{ type: "success" | "error"; text: string } | null>(null);

  const fetchMembers = useCallback(async () => {
    setLoading(true);
    try {
      const res = await apiClient.get<ApiResponse<AdminMember[]>>("/api/users/all");
      setMembers(res.data?.data || []);
    } catch (err) {
      console.error("Failed to load members:", err);
      setStatusMessage({ type: "error", text: "Failed to fetch member list." });
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchMembers();
  }, [fetchMembers]);

  const handleRoleChange = async (userId: number, newRole: ClubRole) => {
    setActionLoadingId(userId);
    setStatusMessage(null);
    try {
      await apiClient.put(`/api/users/${userId}/club-role`, { clubRole: newRole });
      setStatusMessage({ type: "success", text: "Club role updated successfully." });
      await fetchMembers();
    } catch (err) {
      console.error("Failed to update club role:", err);
      setStatusMessage({ type: "error", text: "Failed to update club role." });
    } finally {
      setActionLoadingId(null);
    }
  };

  const handlePlatformRoleToggle = async (userId: number, currentRole: string) => {
    const nextRole = currentRole === "ROLE_ADMIN" ? "ROLE_USER" : "ROLE_ADMIN";
    const confirm = window.confirm(`Change platform permission to ${nextRole}?`);
    if (!confirm) return;

    setActionLoadingId(userId);
    setStatusMessage(null);
    try {
      await apiClient.put(`/api/users/${userId}/role`, { role: nextRole });
      setStatusMessage({ type: "success", text: `Platform role changed to ${nextRole}.` });
      await fetchMembers();
    } catch (err) {
      console.error("Failed to toggle platform role:", err);
      setStatusMessage({ type: "error", text: "Failed to change platform role." });
    } finally {
      setActionLoadingId(null);
    }
  };

  const handleDeleteUser = async (userId: number, name: string) => {
    const confirm = window.confirm(`Are you sure you want to delete user "${name}" (ID: ${userId})? This action cannot be undone.`);
    if (!confirm) return;

    setActionLoadingId(userId);
    setStatusMessage(null);
    try {
      await apiClient.delete(`/api/users/${userId}`);
      setStatusMessage({ type: "success", text: `User "${name}" deleted.` });
      await fetchMembers();
    } catch (err) {
      console.error("Failed to delete user:", err);
      setStatusMessage({ type: "error", text: "Failed to delete user." });
    } finally {
      setActionLoadingId(null);
    }
  };

  const filteredMembers = useMemo(() => {
    const q = search.toLowerCase().trim();
    if (!q) return members;
    return members.filter(
      (m) =>
        m.name.toLowerCase().includes(q) ||
        m.email.toLowerCase().includes(q) ||
        (m.codeforcesHandle ?? "").toLowerCase().includes(q) ||
        (m.leetcodeHandle ?? "").toLowerCase().includes(q) ||
        String(m.id).includes(q)
    );
  }, [members, search]);

  const columns: Column<AdminMember>[] = [
    {
      key: "id",
      header: "ID",
      className: "font-mono text-xs text-fg-muted",
      render: (m) => `#${m.id}`,
    },
    {
      key: "avatar",
      header: "Avatar",
      render: (m) => (
        <div className="size-8 rounded-full border border-border bg-surface-2 overflow-hidden flex items-center justify-center">
          {m.avatarUrl ? (
            <Image src={m.avatarUrl} alt={m.name} width={32} height={32} className="size-full object-cover" />
          ) : (
            <Users className="size-4 text-fg-muted" />
          )}
        </div>
      ),
    },
    {
      key: "name",
      header: "Member",
      render: (m) => (
        <div>
          <Link href={`/profile/${m.id}`} className="font-semibold text-foreground hover:underline hover:text-primary">
            {m.name}
          </Link>
          <div className="font-mono text-micro text-fg-muted">{m.email}</div>
        </div>
      ),
    },
    {
      key: "phoneNumber",
      header: "Phone",
      className: "font-mono text-xs",
      render: (m) => m.phoneNumber || <span className="text-fg-subtle">--</span>,
    },
    {
      key: "cf",
      header: "Codeforces",
      render: (m) => (
        <div className="text-xs">
          {m.codeforcesHandle ? (
            <div>
              <span className="font-medium">@{m.codeforcesHandle}</span>
              <span className="ml-1.5 text-fg-muted">({m.rating ?? "—"})</span>
            </div>
          ) : (
            <span className="text-fg-subtle">--</span>
          )}
        </div>
      ),
    },
    {
      key: "leetcode",
      header: "LeetCode",
      render: (m) => (
        <div className="text-xs">
          {m.leetcodeHandle ? (
            <div>
              <span className="font-medium">@{m.leetcodeHandle}</span>
              <span className="ml-1.5 text-fg-muted">({m.leetcodeRating ?? "—"})</span>
            </div>
          ) : (
            <span className="text-fg-subtle">--</span>
          )}
        </div>
      ),
    },
    {
      key: "clubRole",
      header: "Club Role",
      render: (m) => (
        <div className="flex items-center gap-2">
          <ClubRoleBadge clubRole={m.clubRole} showIcon={false} />
          <select
            value={m.clubRole || "STUDENT"}
            disabled={actionLoadingId === m.id}
            onChange={(e) => handleRoleChange(m.id, e.target.value as ClubRole)}
            className="rounded-control border border-border bg-surface-2 px-2 py-1 text-xs text-foreground focus:border-primary focus:outline-none"
          >
            {ALL_CLUB_ROLES.map((role) => (
              <option key={role} value={role}>
                {CLUB_ROLE_LABELS[role]}
              </option>
            ))}
          </select>
        </div>
      ),
    },
    {
      key: "academicYear",
      header: "Year",
      className: "text-xs",
      render: (m) => (m.academicYear ? ACADEMIC_YEAR_LABELS[m.academicYear] : "--"),
    },
    {
      key: "profileComplete",
      header: "Profile",
      render: (m) =>
        m.profileComplete ? (
          <span className="inline-flex items-center gap-1 text-nano font-semibold text-emerald-400">
            <CheckCircle2 className="size-3.5" /> Complete
          </span>
        ) : (
          <span className="inline-flex items-center gap-1 text-nano text-fg-subtle">
            <AlertTriangle className="size-3.5" /> Incomplete
          </span>
        ),
    },
    {
      key: "batchYear",
      header: "Batch",
      className: "text-xs",
      render: (m) => m.batchYear ?? "--",
    },
    {
      key: "role",
      header: "Platform Role",
      render: (m) => {
        const isAdmin = m.role === "ROLE_ADMIN";
        const isSelf = currentUser?.id === m.id;
        return (
          <button
            onClick={() => handlePlatformRoleToggle(m.id, m.role || "ROLE_USER")}
            disabled={actionLoadingId === m.id || isSelf}
            title={isSelf ? "You cannot change your own platform role" : undefined}
            className={`rounded-full border px-2.5 py-0.5 text-nano font-semibold transition-all ${
              isAdmin
                ? "border-red-500/40 bg-red-500/10 text-red-400 hover:bg-red-500/20"
                : "border-border bg-surface-2 text-fg-muted hover:border-primary/40 hover:text-foreground"
            }`}
          >
            {isAdmin ? "Admin" : "User"}
          </button>
        );
      },
    },
    {
      key: "actions",
      header: "Actions",
      className: "text-right",
      render: (m) => (
        <button
          onClick={() => handleDeleteUser(m.id, m.name)}
          disabled={actionLoadingId === m.id || currentUser?.id === m.id}
          title={currentUser?.id === m.id ? "You cannot delete your own account" : "Delete user"}
          className="rounded-control p-1 text-fg-muted hover:bg-red-500/10 hover:text-red-400 transition-colors"
        >
          <Trash2 className="size-4" />
        </button>
      ),
    },
  ];

  return (
    <div className="space-y-4">
      {statusMessage && (
        <div
          className={`flex items-center justify-between rounded-panel border p-3 text-xs ${
            statusMessage.type === "success"
              ? "border-emerald-500/30 bg-emerald-500/10 text-emerald-400"
              : "border-red-500/30 bg-red-500/10 text-red-400"
          }`}
        >
          <span>{statusMessage.text}</span>
          <button onClick={() => setStatusMessage(null)}>
            <XCircle className="size-4" />
          </button>
        </div>
      )}

      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="relative flex-1 max-w-sm">
          <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-fg-muted" />
          <input
            type="text"
            placeholder="Search by ID, name, email, or handle..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-full rounded-control border border-border bg-surface-2 py-2 pl-9 pr-4 text-xs text-foreground placeholder:text-fg-muted focus:border-primary focus:outline-none"
          />
        </div>
        <div className="text-xs text-fg-muted">
          Showing {filteredMembers.length} of {members.length} members
        </div>
      </div>

      <div className="hidden sm:block">
        <DataTable columns={columns} data={filteredMembers} isLoading={loading} emptyMessage="No members found." />
      </div>

      <div className="block sm:hidden space-y-3">
        {loading ? (
          Array.from({ length: 3 }).map((_, i) => (
            <div key={i} className="h-32 animate-pulse rounded-panel bg-surface-2" />
          ))
        ) : filteredMembers.length === 0 ? (
          <div className="rounded-panel border border-border bg-surface p-6 text-center text-sm text-fg-muted">
            No members found.
          </div>
        ) : (
          filteredMembers.map((m) => (
            <div key={m.id} className="rounded-panel border border-border bg-surface p-4 space-y-3">
              <div className="flex items-start justify-between gap-3">
                <div className="flex items-center gap-3">
                  <div className="size-10 shrink-0 rounded-full border border-border bg-surface-2 overflow-hidden flex items-center justify-center">
                    {m.avatarUrl ? (
                      <Image src={m.avatarUrl} alt={m.name} width={40} height={40} className="size-full object-cover" />
                    ) : (
                      <Users className="size-5 text-fg-muted" />
                    )}
                  </div>
                  <div>
                    <div className="font-semibold text-foreground line-clamp-1">{m.name}</div>
                    <div className="font-mono text-xs text-fg-muted">#{m.id} {m.academicYear ? `• ${ACADEMIC_YEAR_LABELS[m.academicYear]}` : ""}</div>
                  </div>
                </div>
                <div className="flex gap-2">
                  <button onClick={() => handleDeleteUser(m.id, m.name)} disabled={actionLoadingId === m.id || currentUser?.id === m.id} title={currentUser?.id === m.id ? "You cannot delete your own account" : "Delete user"} className="p-1 text-fg-muted hover:text-red-400 transition-colors disabled:opacity-50">
                    <Trash2 className="size-4" />
                  </button>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-2 text-xs">
                <div className="rounded border border-border/50 bg-surface-2/50 p-2 space-y-1">
                  <div className="text-fg-subtle">Role</div>
                  <div><ClubRoleBadge clubRole={m.clubRole} showIcon={false} /></div>
                </div>
                <div className="rounded border border-border/50 bg-surface-2/50 p-2 space-y-1">
                  <div className="text-fg-subtle">Platform</div>
                  <div className="font-semibold">{m.role.replace("ROLE_", "")}</div>
                </div>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
