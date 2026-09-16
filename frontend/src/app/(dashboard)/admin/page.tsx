"use client";

import React, { useState, useEffect, useCallback, useMemo } from "react";
import Link from "next/link";
import Image from "next/image";
import Script from "next/script";
import { useAuthStore, type ApiResponse } from "@/store/auth";
import apiClient from "@/lib/axios";
import { eventsService, type EventRequest } from "@/lib/services/events";
import { galleryService, type MemberGalleryPhotoRequest } from "@/lib/services/gallery";
import { AdminTabs, type AdminTab } from "@/components/site/admin-tabs";
import { DataTable, type Column } from "@/components/ui/data-table";
import { ClubRoleBadge } from "@/components/ui/club-role-badge";
import { CLUB_ROLE_LABELS } from "@/lib/club-roles";
import {
  ACADEMIC_YEAR_LABELS,
  EVENT_TYPE_LABELS,
  type EventType,
  type AcademicYear,
  type ClubRole,
  type Event,
  type EventPhoto,
  type MemberGalleryPhoto,
} from "@/types/api";
import {
  Users,
  Calendar,
  Plus,
  Trash2,
  Edit2,
  CheckCircle2,
  AlertTriangle,
  XCircle,
  Shield,
  ShieldAlert,
  Loader2,
  Upload,
  UserCheck,
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
/**
 * The event form's fields.
 *
 * Strings rather than nulls for the optional URLs: this is what an input
 * element holds, and converting once on submit beats a null check on every
 * keystroke.
 */
interface EventFormState {
  title: string;
  description: string;
  eventDate: string;
  location: string;
  coverImageUrl: string;
  eventType: EventType | "";
  codeforcesContestUrl: string;
  showContestLink: boolean;
  showWinners: boolean;
  showAttendeeCount: boolean;
}

/** A new event publishes nothing until somebody decides it should. */
const EMPTY_EVENT_FORM: EventFormState = {
  title: "",
  description: "",
  eventDate: "",
  location: "",
  coverImageUrl: "",
  eventType: "",
  codeforcesContestUrl: "",
  showContestLink: false,
  showWinners: false,
  showAttendeeCount: false,
};

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

export default function AdminDashboardPage() {
  const { user, isAuthenticated } = useAuthStore();
  const [activeTab, setActiveTab] = useState<AdminTab>("members");

  // Authentication & authorization guard
  if (!isAuthenticated) {
    return (
      <div className="flex min-h-[400px] flex-col items-center justify-center gap-3 text-center">
        <ShieldAlert className="size-10 text-amber-500" />
        <h2 className="text-lg font-bold">Authentication Required</h2>
        <p className="text-xs text-fg-muted max-w-sm">
          Please log in with an administrator account to access this dashboard.
        </p>
        <Link
          href="/login"
          className="mt-2 rounded-control bg-primary px-4 py-2 text-xs font-semibold text-primary-foreground shadow"
        >
          Sign In
        </Link>
      </div>
    );
  }

  if (user?.role !== "ROLE_ADMIN") {
    return (
      <div className="flex min-h-[400px] flex-col items-center justify-center gap-3 text-center">
        <ShieldAlert className="size-10 text-red-500" />
        <h2 className="text-lg font-bold">Access Denied</h2>
        <p className="text-xs text-fg-muted max-w-sm">
          You do not have administrative privileges to view this page.
        </p>
        <Link
          href="/leaderboard"
          className="mt-2 rounded-control border border-border bg-surface-2 px-4 py-2 text-xs font-semibold hover:text-foreground"
        >
          Return to Leaderboard
        </Link>
      </div>
    );
  }

  return (
    <>
      <Script src="https://upload-widget.cloudinary.com/global/all.js" strategy="lazyOnload" />
      <div className="mx-auto max-w-7xl space-y-6 pb-16 pt-6">
        <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-foreground">
              Admin Dashboard
            </h1>
            <p className="text-xs text-fg-muted">
              Manage club members, schedule events, record attendance, and curate photo galleries.
            </p>
          </div>
          <div className="flex items-center gap-2">
            <span className="inline-flex items-center gap-1.5 rounded-full border border-primary/30 bg-primary/10 px-3 py-1 text-xs font-semibold text-primary">
              <Shield className="size-3.5" /> Administrator Mode
            </span>
          </div>
        </div>

        <AdminTabs activeTab={activeTab} onChange={setActiveTab} />

        {activeTab === "members" && <MembersTab />}
        {activeTab === "events" && <EventsTab />}
        {activeTab === "galleries" && <GalleriesTab />}
      </div>
    </>
  );
}

// ══════════════════════════════════════════════════════════════════
// TAB 1: MEMBERS MANAGEMENT
// ══════════════════════════════════════════════════════════════════

function MembersTab() {
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
    // eslint-disable-next-line react-hooks/set-state-in-effect -- data fetch on mount, not derived state
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

      <DataTable columns={columns} data={filteredMembers} isLoading={loading} emptyMessage="No members found." />
    </div>
  );
}

// ══════════════════════════════════════════════════════════════════
// TAB 2: EVENTS MANAGEMENT
// ══════════════════════════════════════════════════════════════════

function EventsTab() {
  const [events, setEvents] = useState<Event[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [editingEvent, setEditingEvent] = useState<Event | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [statusMessage, setStatusMessage] = useState<{ type: "success" | "error"; text: string } | null>(null);

  const [formData, setFormData] = useState<EventFormState>(EMPTY_EVENT_FORM);

  const fetchEvents = useCallback(async () => {
    setLoading(true);
    try {
      const data = await eventsService.getAllAdmin();
      setEvents(data);
    } catch (err) {
      console.error("Failed to load events:", err);
      setStatusMessage({ type: "error", text: "Failed to load events." });
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- data fetch on mount, not derived state
    fetchEvents();
  }, [fetchEvents]);

  const handleOpenCloudinaryCover = () => {
    const cloudName = process.env.NEXT_PUBLIC_CLOUDINARY_CLOUD_NAME || "stdcydx1";
    const uploadPreset = process.env.NEXT_PUBLIC_CLOUDINARY_UPLOAD_PRESET || "cpclub_unsigned";

    if (window.cloudinary) {
      const widget = window.cloudinary.createUploadWidget(
        {
          cloudName,
          uploadPreset,
          folder: "cpclub/events",
          maxFiles: 1,
          clientAllowedFormats: ["jpg", "png", "webp", "jpeg"],
        },
        (error, result) => {
          if (!error && result && result.event === "success") {
            setFormData((prev) => ({ ...prev, coverImageUrl: result.info.secure_url }));
          }
        }
      );
      widget.open();
    } else {
      const url = window.prompt("Enter cover image URL:");
      if (url) setFormData((prev) => ({ ...prev, coverImageUrl: url.trim() }));
    }
  };

  const handleSaveEvent = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.title || !formData.eventDate || !formData.location) {
      setStatusMessage({ type: "error", text: "Please fill in all required fields." });
      return;
    }

    setIsSubmitting(true);
    setStatusMessage(null);
    try {
      const payload: EventRequest = {
        title: formData.title.trim(),
        description: formData.description.trim() || null,
        // datetime-local gives "YYYY-MM-DDTHH:mm"; the API expects a LocalDateTime,
        // so send exactly what was typed rather than a UTC instant.
        eventDate: formData.eventDate.length === 16 ? `${formData.eventDate}:00` : formData.eventDate,
        location: formData.location.trim(),
        coverImageUrl: formData.coverImageUrl.trim() || null,
        eventType: formData.eventType || null,
        codeforcesContestUrl: formData.codeforcesContestUrl.trim() || null,
        showContestLink: formData.showContestLink,
        showWinners: formData.showWinners,
        showAttendeeCount: formData.showAttendeeCount,
      };

      if (editingEvent) {
        await eventsService.updateEvent(editingEvent.id, payload);
        setStatusMessage({ type: "success", text: "Event updated successfully." });
      } else {
        await eventsService.createEvent(payload);
        setStatusMessage({ type: "success", text: "Event created successfully." });
      }

      setShowCreateModal(false);
      setEditingEvent(null);
      setFormData(EMPTY_EVENT_FORM);
      await fetchEvents();
    } catch (err) {
      console.error("Failed to save event:", err);
      setStatusMessage({ type: "error", text: "Failed to save event." });
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleMarkCompleted = async (id: number) => {
    try {
      await eventsService.markCompleted(id);
      setStatusMessage({ type: "success", text: "Event marked as COMPLETED." });
      await fetchEvents();
    } catch (err) {
      console.error("Failed to mark completed:", err);
      setStatusMessage({ type: "error", text: "Failed to update event status." });
    }
  };

  const handleCancelEvent = async (id: number) => {
    const confirm = window.confirm("Are you sure you want to cancel this event?");
    if (!confirm) return;
    try {
      await eventsService.cancel(id);
      setStatusMessage({ type: "success", text: "Event marked as CANCELLED." });
      await fetchEvents();
    } catch (err) {
      console.error("Failed to cancel event:", err);
      setStatusMessage({ type: "error", text: "Failed to cancel event." });
    }
  };

  const openEdit = (ev: Event) => {
    setEditingEvent(ev);
    setFormData({
      title: ev.title,
      description: ev.description || "",
      eventDate: ev.eventDate ? ev.eventDate.slice(0, 16) : "",
      location: ev.location,
      coverImageUrl: ev.coverImageUrl || "",
      eventType: ev.eventType ?? "",
      codeforcesContestUrl: ev.codeforcesContestUrl || "",
      showContestLink: ev.showContestLink,
      showWinners: ev.showWinners,
      showAttendeeCount: ev.showAttendeeCount,
    });
    setShowCreateModal(true);
  };

  const columns: Column<Event>[] = [
    {
      key: "cover",
      header: "Banner",
      render: (e) => (
        <div className="size-10 rounded-panel border border-border bg-surface-2 overflow-hidden flex items-center justify-center">
          {e.coverImageUrl ? (
            <Image src={e.coverImageUrl} alt={e.title} width={40} height={40} className="size-full object-cover" />
          ) : (
            <Calendar className="size-4 text-fg-muted" />
          )}
        </div>
      ),
    },
    {
      key: "title",
      header: "Title & Location",
      render: (e) => (
        <div>
          <div className="font-semibold text-foreground">{e.title}</div>
          <div className="text-xs text-fg-muted">{e.location}</div>
        </div>
      ),
    },
    {
      key: "eventDate",
      header: "Date & Time",
      className: "text-xs",
      render: (e) => (
        <div>
          {new Date(e.eventDate).toLocaleDateString("en-US", {
            weekday: "short",
            month: "short",
            day: "numeric",
            year: "numeric",
          })}
          <div className="text-fg-subtle text-micro">
            {new Date(e.eventDate).toLocaleTimeString("en-US", { hour: "2-digit", minute: "2-digit" })}
          </div>
        </div>
      ),
    },
    {
      key: "status",
      header: "Status",
      render: (e) => {
        const colors = {
          UPCOMING: "border-emerald-500/40 bg-emerald-500/10 text-emerald-400",
          COMPLETED: "border-blue-500/40 bg-blue-500/10 text-blue-400",
          CANCELLED: "border-red-500/40 bg-red-500/10 text-red-400",
        }[e.status] || "border-border bg-surface-2 text-fg-muted";
        return (
          <span className={`inline-block rounded-full border px-2.5 py-0.5 text-nano font-semibold uppercase ${colors}`}>
            {e.status}
          </span>
        );
      },
    },
    {
      key: "attendees",
      header: "Attendees",
      render: (e) => (
        <Link
          href={`/admin/events/${e.id}`}
          className="inline-flex items-center gap-1 rounded-control border border-border bg-surface-2 px-2.5 py-1 text-xs font-medium text-foreground hover:border-primary/50 hover:text-primary transition-colors"
        >
          <UserCheck className="size-3.5" />
          <span>Manage</span>
        </Link>
      ),
    },
    {
      key: "actions",
      header: "Actions",
      className: "text-right",
      render: (e) => (
        <div className="flex items-center justify-end gap-1.5">
          <button
            onClick={() => openEdit(e)}
            title="Edit event"
            className="rounded-control p-1 text-fg-muted hover:text-foreground"
          >
            <Edit2 className="size-4" />
          </button>
          {e.status === "UPCOMING" && (
            <>
              <button
                onClick={() => handleMarkCompleted(e.id)}
                title="Mark as completed"
                className="rounded-control p-1 text-fg-muted hover:text-blue-400"
              >
                <CheckCircle2 className="size-4" />
              </button>
              <button
                onClick={() => handleCancelEvent(e.id)}
                title="Cancel event"
                className="rounded-control p-1 text-fg-muted hover:text-red-400"
              >
                <XCircle className="size-4" />
              </button>
            </>
          )}
        </div>
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

      <div className="flex items-center justify-between">
        <h2 className="text-base font-semibold text-foreground">All Club Events</h2>
        <button
          onClick={() => {
            setEditingEvent(null);
            setFormData(EMPTY_EVENT_FORM);
            setShowCreateModal(true);
          }}
          className="inline-flex items-center gap-1.5 rounded-control bg-primary px-3.5 py-1.5 text-xs font-semibold text-primary-foreground shadow-sm hover:bg-primary/90"
        >
          <Plus className="size-4" />
          Create Event
        </button>
      </div>

      <DataTable columns={columns} data={events} isLoading={loading} emptyMessage="No events created yet." />

      {/* Modal for Create / Edit */}
      {showCreateModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-background/80 backdrop-blur-sm p-4">
          <div className="w-full max-w-lg rounded-panel border border-border bg-surface p-6 shadow-2xl space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="text-base font-bold text-foreground">
                {editingEvent ? "Edit Event" : "Create New Event"}
              </h3>
              <button
                onClick={() => setShowCreateModal(false)}
                className="text-fg-muted hover:text-foreground"
              >
                <XCircle className="size-5" />
              </button>
            </div>

            <form onSubmit={handleSaveEvent} className="space-y-3">
              <div>
                <label className="block text-xs font-medium text-fg-muted mb-1">
                  Event Title <span className="text-red-400">*</span>
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. ICPC Orientation & Mock Contest"
                  value={formData.title}
                  onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                  className="w-full rounded-control border border-border bg-surface-2 px-3 py-2 text-xs text-foreground focus:border-primary focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-xs font-medium text-fg-muted mb-1">Description</label>
                <textarea
                  rows={3}
                  placeholder="Event details, schedule, prerequisites..."
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  className="w-full rounded-control border border-border bg-surface-2 px-3 py-2 text-xs text-foreground focus:border-primary focus:outline-none"
                />
              </div>

              <div className="grid gap-3 sm:grid-cols-2">
                <div>
                  <label className="block text-xs font-medium text-fg-muted mb-1">
                    Date & Time <span className="text-red-400">*</span>
                  </label>
                  <input
                    type="datetime-local"
                    required
                    value={formData.eventDate}
                    onChange={(e) => setFormData({ ...formData, eventDate: e.target.value })}
                    className="w-full rounded-control border border-border bg-surface-2 px-3 py-2 text-xs text-foreground focus:border-primary focus:outline-none"
                  />
                </div>

                <div>
                  <label className="block text-xs font-medium text-fg-muted mb-1">
                    Location <span className="text-red-400">*</span>
                  </label>
                  <input
                    type="text"
                    required
                    placeholder="Lab 301 / Online"
                    value={formData.location}
                    onChange={(e) => setFormData({ ...formData, location: e.target.value })}
                    className="w-full rounded-control border border-border bg-surface-2 px-3 py-2 text-xs text-foreground focus:border-primary focus:outline-none"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-medium text-fg-muted mb-1">Cover Image URL</label>
                <div className="flex gap-2">
                  <input
                    type="url"
                    placeholder="https://res.cloudinary.com/..."
                    value={formData.coverImageUrl}
                    onChange={(e) => setFormData({ ...formData, coverImageUrl: e.target.value })}
                    className="flex-1 rounded-control border border-border bg-surface-2 px-3 py-2 text-xs text-foreground focus:border-primary focus:outline-none"
                  />
                  <button
                    type="button"
                    onClick={handleOpenCloudinaryCover}
                    className="inline-flex items-center gap-1 rounded-control border border-border bg-surface-2 px-3 py-2 text-xs text-fg-muted hover:text-foreground"
                  >
                    <Upload className="size-3.5" />
                    Upload
                  </button>
                </div>
              </div>

              <div>
                <label
                  htmlFor="event-type"
                  className="mb-1 block text-xs font-medium text-fg-muted"
                >
                  Event type
                </label>
                <select
                  id="event-type"
                  value={formData.eventType}
                  onChange={(e) =>
                    setFormData({ ...formData, eventType: e.target.value as EventType | "" })
                  }
                  className="w-full rounded-control border border-border bg-surface-2 px-3 py-2 text-xs text-foreground focus:border-primary focus:outline-none"
                >
                  <option value="">No type</option>
                  {(Object.keys(EVENT_TYPE_LABELS) as EventType[]).map((type) => (
                    <option key={type} value={type}>
                      {EVENT_TYPE_LABELS[type]}
                    </option>
                  ))}
                </select>
                <p className="mt-1 text-nano text-fg-subtle">
                  The badge on the public timeline. Leave unset and the event simply carries
                  none.
                </p>
              </div>

              <div>
                <label className="block text-xs font-medium text-fg-muted mb-1">
                  Codeforces contest URL
                </label>
                <input
                  type="url"
                  placeholder="https://codeforces.com/contest/1234"
                  value={formData.codeforcesContestUrl}
                  onChange={(e) =>
                    setFormData({ ...formData, codeforcesContestUrl: e.target.value })
                  }
                  className="w-full rounded-control border border-border bg-surface-2 px-3 py-2 text-xs text-foreground focus:border-primary focus:outline-none"
                />
                <p className="mt-1 text-nano text-fg-subtle">
                  Leave empty for a workshop or talk. Without a contest, the public page
                  shows no results section at all.
                </p>
              </div>

              {/* Three switches, not one: the club shares the link when the round
                  opens, the winners only once it closes, and sometimes never
                  publishes the turnout. Nothing here is on by default. */}
              <fieldset className="rounded-control border border-border bg-surface-2 p-3">
                <legend className="px-1 font-mono text-micro tracking-caps-wide text-fg-subtle uppercase">
                  Visible to the public
                </legend>

                <div className="space-y-2 pt-1">
                  {(
                    [
                      {
                        key: "showContestLink" as const,
                        label: "Contest link",
                        hint: "Publish once the round is open.",
                      },
                      {
                        key: "showWinners" as const,
                        label: "Winners",
                        hint: "Record them any time; this announces them.",
                      },
                      {
                        key: "showAttendeeCount" as const,
                        label: "Number who attended",
                        hint: "Turnout is hidden unless you publish it.",
                      },
                    ]
                  ).map((item) => (
                    <label key={item.key} className="flex cursor-pointer items-start gap-2.5">
                      <input
                        type="checkbox"
                        checked={formData[item.key]}
                        onChange={(e) =>
                          setFormData({ ...formData, [item.key]: e.target.checked })
                        }
                        className="mt-0.5 size-3.5 accent-[var(--primary)]"
                      />
                      <span>
                        <span className="block text-xs text-foreground">{item.label}</span>
                        <span className="block text-nano text-fg-subtle">{item.hint}</span>
                      </span>
                    </label>
                  ))}
                </div>
              </fieldset>

              <div className="flex justify-end gap-2 pt-3">
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="rounded-control border border-border px-4 py-2 text-xs text-fg-muted hover:text-foreground"
                  disabled={isSubmitting}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="inline-flex items-center gap-1.5 rounded-control bg-primary px-4 py-2 text-xs font-semibold text-primary-foreground shadow hover:bg-primary/90 disabled:opacity-50"
                >
                  {isSubmitting && <Loader2 className="size-3.5 animate-spin" />}
                  {editingEvent ? "Save Changes" : "Create Event"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

// ══════════════════════════════════════════════════════════════════
// TAB 3: GALLERIES MANAGEMENT
// ══════════════════════════════════════════════════════════════════

function GalleriesTab() {
  const [subTab, setSubTab] = useState<"members" | "events">("members");

  return (
    <div className="space-y-6">
      <div className="flex gap-2 border-b border-border/60 pb-3">
        <button
          onClick={() => setSubTab("members")}
          className={`rounded-full px-3.5 py-1 text-xs font-medium transition-colors ${
            subTab === "members"
              ? "bg-primary text-primary-foreground"
              : "border border-border text-fg-muted hover:text-foreground"
          }`}
        >
          Member Batch Gallery
        </button>
        <button
          onClick={() => setSubTab("events")}
          className={`rounded-full px-3.5 py-1 text-xs font-medium transition-colors ${
            subTab === "events"
              ? "bg-primary text-primary-foreground"
              : "border border-border text-fg-muted hover:text-foreground"
          }`}
        >
          Event Galleries
        </button>
      </div>

      {subTab === "members" ? <MemberGalleryManager /> : <EventGalleryManager />}
    </div>
  );
}

function MemberGalleryManager() {
  const [photos, setPhotos] = useState<MemberGalleryPhoto[]>([]);
  const [availableYears, setAvailableYears] = useState<number[]>([]);
  const [selectedYear, setSelectedYear] = useState<number>(new Date().getFullYear());
  const [loading, setLoading] = useState(false);
  const [uploadBatchYear, setUploadBatchYear] = useState<number>(new Date().getFullYear());
  const [uploadCaption, setUploadCaption] = useState("");
  const [uploadImageUrl, setUploadImageUrl] = useState("");
  const [isUploading, setIsUploading] = useState(false);
  const [statusMessage, setStatusMessage] = useState<{ type: "success" | "error"; text: string } | null>(null);

  const loadYears = useCallback(async () => {
    try {
      const years = await galleryService.getAvailableBatchYears();
      setAvailableYears(years);
      if (years.length > 0 && !years.includes(selectedYear)) {
        setSelectedYear(years[0]);
      }
    } catch (err) {
      console.error("Failed to load batch years:", err);
    }
  }, [selectedYear]);

  const loadPhotos = useCallback(async (year: number) => {
    setLoading(true);
    try {
      const data = await galleryService.getPhotosByBatch(year);
      setPhotos(data);
    } catch (err) {
      console.error("Failed to load batch photos:", err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- data fetch on mount, not derived state
    loadYears();
  }, [loadYears]);

  useEffect(() => {
    if (selectedYear) {
      // eslint-disable-next-line react-hooks/set-state-in-effect -- data fetch on mount, not derived state
      loadPhotos(selectedYear);
    }
  }, [selectedYear, loadPhotos]);

  const handleOpenCloudinary = () => {
    const cloudName = process.env.NEXT_PUBLIC_CLOUDINARY_CLOUD_NAME || "stdcydx1";
    const uploadPreset = process.env.NEXT_PUBLIC_CLOUDINARY_UPLOAD_PRESET || "cpclub_unsigned";

    if (window.cloudinary) {
      const widget = window.cloudinary.createUploadWidget(
        {
          cloudName,
          uploadPreset,
          folder: `cpclub/batch_${uploadBatchYear}`,
          maxFiles: 1,
          clientAllowedFormats: ["jpg", "png", "webp", "jpeg"],
        },
        (error, result) => {
          if (!error && result && result.event === "success") {
            setUploadImageUrl(result.info.secure_url);
          }
        }
      );
      widget.open();
    } else {
      const url = window.prompt("Enter image URL:");
      if (url) setUploadImageUrl(url.trim());
    }
  };

  const handleUploadPhoto = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!uploadImageUrl || !uploadBatchYear) {
      setStatusMessage({ type: "error", text: "Image URL and Batch Year are required." });
      return;
    }

    setIsUploading(true);
    setStatusMessage(null);
    try {
      const payload: MemberGalleryPhotoRequest = {
        batchYear: Number(uploadBatchYear),
        imageUrl: uploadImageUrl.trim(),
        caption: uploadCaption.trim() || null,
      };
      await galleryService.addMemberPhoto(payload);
      setStatusMessage({ type: "success", text: "Photo uploaded to batch gallery!" });
      setUploadImageUrl("");
      setUploadCaption("");
      await loadYears();
      setSelectedYear(uploadBatchYear);
      await loadPhotos(uploadBatchYear);
    } catch (err) {
      console.error("Failed to upload batch photo:", err);
      setStatusMessage({ type: "error", text: "Failed to upload photo." });
    } finally {
      setIsUploading(false);
    }
  };

  const handleDeletePhoto = async (id: number) => {
    const confirm = window.confirm("Are you sure you want to delete this photo?");
    if (!confirm) return;

    try {
      await galleryService.deleteMemberPhoto(id);
      setStatusMessage({ type: "success", text: "Photo deleted." });
      await loadPhotos(selectedYear);
      await loadYears();
    } catch (err) {
      console.error("Failed to delete photo:", err);
      setStatusMessage({ type: "error", text: "Failed to delete photo." });
    }
  };

  return (
    <div className="grid gap-8 lg:grid-cols-12">
      {/* Upload Form (4 cols) */}
      <div className="lg:col-span-4">
        <div className="rounded-panel border border-border bg-surface p-5 space-y-4">
          <h3 className="text-sm font-bold text-foreground flex items-center gap-2">
            <Upload className="size-4 text-primary" />
            Upload Batch Photo
          </h3>

          <form onSubmit={handleUploadPhoto} className="space-y-3">
            <div>
              <label className="block text-xs font-medium text-fg-muted mb-1">
                Batch Year <span className="text-red-400">*</span>
              </label>
              <input
                type="number"
                min={2000}
                max={2100}
                required
                value={uploadBatchYear}
                onChange={(e) => setUploadBatchYear(Number(e.target.value))}
                className="w-full rounded-control border border-border bg-surface-2 px-3 py-2 text-xs text-foreground focus:border-primary focus:outline-none"
              />
            </div>

            <div>
              <label className="block text-xs font-medium text-fg-muted mb-1">
                Image Source <span className="text-red-400">*</span>
              </label>
              <div className="flex gap-2">
                <input
                  type="url"
                  placeholder="https://..."
                  required
                  value={uploadImageUrl}
                  onChange={(e) => setUploadImageUrl(e.target.value)}
                  className="flex-1 rounded-control border border-border bg-surface-2 px-3 py-2 text-xs text-foreground focus:border-primary focus:outline-none"
                />
                <button
                  type="button"
                  onClick={handleOpenCloudinary}
                  className="rounded-control border border-border bg-surface-2 px-3 py-2 text-xs text-fg-muted hover:text-foreground"
                >
                  Upload
                </button>
              </div>
            </div>

            <div>
              <label className="block text-xs font-medium text-fg-muted mb-1">Caption</label>
              <input
                type="text"
                placeholder="e.g. Batch 2024 Welcome Meet"
                value={uploadCaption}
                onChange={(e) => setUploadCaption(e.target.value)}
                className="w-full rounded-control border border-border bg-surface-2 px-3 py-2 text-xs text-foreground focus:border-primary focus:outline-none"
              />
            </div>

            <button
              type="submit"
              disabled={isUploading}
              className="w-full inline-flex items-center justify-center gap-1.5 rounded-control bg-primary px-4 py-2 text-xs font-semibold text-primary-foreground shadow hover:bg-primary/90 disabled:opacity-50"
            >
              {isUploading && <Loader2 className="size-3.5 animate-spin" />}
              Add to Batch Gallery
            </button>
          </form>
        </div>
      </div>

      {/* Photos Grid & Year Filter (8 cols) */}
      <div className="space-y-4 lg:col-span-8">
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

        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <span className="text-xs font-medium text-fg-muted">Select Batch:</span>
            <select
              value={selectedYear}
              onChange={(e) => setSelectedYear(Number(e.target.value))}
              className="rounded-control border border-border bg-surface-2 px-3 py-1.5 text-xs text-foreground focus:border-primary focus:outline-none"
            >
              {availableYears.length === 0 && <option value={selectedYear}>{selectedYear}</option>}
              {availableYears.map((yr) => (
                <option key={yr} value={yr}>
                  Batch {yr}
                </option>
              ))}
            </select>
          </div>
          <span className="text-xs text-fg-muted">{photos.length} photos</span>
        </div>

        {loading ? (
          <div className="flex h-48 items-center justify-center text-xs text-fg-muted">
            <Loader2 className="mr-2 size-4 animate-spin text-primary" /> Loading photos...
          </div>
        ) : photos.length === 0 ? (
          <div className="rounded-panel border border-dashed border-border/80 p-8 text-center text-xs text-fg-muted">
            No photos uploaded for Batch {selectedYear}. Use the upload form on the left.
          </div>
        ) : (
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-3">
            {photos.map((photo) => (
              <div
                key={photo.id}
                className="group relative rounded-panel border border-border bg-surface-2 overflow-hidden"
              >
                <div className="aspect-video w-full bg-background">
                  <Image
                    src={photo.imageUrl}
                    alt={photo.caption || `Batch ${photo.batchYear}`}
                    width={320}
                    height={180}
                    className="size-full object-cover"
                  />
                </div>
                <div className="p-2.5">
                  <p className="truncate text-xs font-medium text-foreground">
                    {photo.caption || `Batch ${photo.batchYear}`}
                  </p>
                  <p className="text-micro text-fg-subtle">
                    {new Date(photo.uploadedAt).toLocaleDateString()}
                  </p>
                </div>
                <button
                  onClick={() => handleDeletePhoto(photo.id)}
                  title="Delete photo"
                  className="absolute top-2 right-2 rounded-full bg-background/80 p-1.5 text-fg-muted opacity-0 backdrop-blur-sm transition-opacity hover:bg-red-500 hover:text-white group-hover:opacity-100"
                >
                  <Trash2 className="size-3.5" />
                </button>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

function EventGalleryManager() {
  const [events, setEvents] = useState<Event[]>([]);
  const [selectedEventId, setSelectedEventId] = useState<number | null>(null);
  const [photos, setPhotos] = useState<EventPhoto[]>([]);
  const [loading, setLoading] = useState(false);
  const [uploadImageUrl, setUploadImageUrl] = useState("");
  const [uploadCaption, setUploadCaption] = useState("");
  const [isUploading, setIsUploading] = useState(false);
  const [statusMessage, setStatusMessage] = useState<{ type: "success" | "error"; text: string } | null>(null);

  useEffect(() => {
    eventsService.getAllAdmin().then((list) => {
      setEvents(list);
      if (list.length > 0) setSelectedEventId(list[0].id);
    });
  }, []);

  const loadPhotos = useCallback(async (eventId: number) => {
    setLoading(true);
    try {
      const data = await eventsService.getEventPhotos(eventId);
      setPhotos(data);
    } catch (err) {
      console.error("Failed to load event photos:", err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (selectedEventId) {
      // eslint-disable-next-line react-hooks/set-state-in-effect -- data fetch on mount, not derived state
      loadPhotos(selectedEventId);
    }
  }, [selectedEventId, loadPhotos]);

  const handleOpenCloudinary = () => {
    const cloudName = process.env.NEXT_PUBLIC_CLOUDINARY_CLOUD_NAME || "stdcydx1";
    const uploadPreset = process.env.NEXT_PUBLIC_CLOUDINARY_UPLOAD_PRESET || "cpclub_unsigned";

    if (window.cloudinary) {
      const widget = window.cloudinary.createUploadWidget(
        {
          cloudName,
          uploadPreset,
          folder: `cpclub/events/${selectedEventId}`,
          maxFiles: 1,
          clientAllowedFormats: ["jpg", "png", "webp", "jpeg"],
        },
        (error, result) => {
          if (!error && result && result.event === "success") {
            setUploadImageUrl(result.info.secure_url);
          }
        }
      );
      widget.open();
    } else {
      const url = window.prompt("Enter image URL:");
      if (url) setUploadImageUrl(url.trim());
    }
  };

  const handleAddPhoto = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedEventId || !uploadImageUrl) {
      setStatusMessage({ type: "error", text: "Select an event and provide an image URL." });
      return;
    }

    setIsUploading(true);
    setStatusMessage(null);
    try {
      await eventsService.addEventPhoto(selectedEventId, {
        imageUrl: uploadImageUrl.trim(),
        caption: uploadCaption.trim() || null,
      });
      setStatusMessage({ type: "success", text: "Event photo added!" });
      setUploadImageUrl("");
      setUploadCaption("");
      await loadPhotos(selectedEventId);
    } catch (err) {
      console.error("Failed to add event photo:", err);
      setStatusMessage({ type: "error", text: "Failed to upload event photo." });
    } finally {
      setIsUploading(false);
    }
  };

  const handleDeletePhoto = async (photoId: number) => {
    const confirm = window.confirm("Are you sure you want to delete this event photo?");
    if (!confirm) return;

    try {
      await eventsService.deleteEventPhoto(photoId);
      setStatusMessage({ type: "success", text: "Photo deleted." });
      if (selectedEventId) await loadPhotos(selectedEventId);
    } catch (err) {
      console.error("Failed to delete photo:", err);
      setStatusMessage({ type: "error", text: "Failed to delete photo." });
    }
  };

  return (
    <div className="grid gap-8 lg:grid-cols-12">
      {/* Upload Form */}
      <div className="lg:col-span-4">
        <div className="rounded-panel border border-border bg-surface p-5 space-y-4">
          <h3 className="text-sm font-bold text-foreground flex items-center gap-2">
            <Upload className="size-4 text-primary" />
            Add Event Photo
          </h3>

          <form onSubmit={handleAddPhoto} className="space-y-3">
            <div>
              <label className="block text-xs font-medium text-fg-muted mb-1">Select Event</label>
              <select
                value={selectedEventId || ""}
                onChange={(e) => setSelectedEventId(Number(e.target.value))}
                className="w-full rounded-control border border-border bg-surface-2 px-3 py-2 text-xs text-foreground focus:border-primary focus:outline-none"
              >
                {events.map((ev) => (
                  <option key={ev.id} value={ev.id}>
                    {ev.title} ({ev.status})
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-medium text-fg-muted mb-1">
                Image Source <span className="text-red-400">*</span>
              </label>
              <div className="flex gap-2">
                <input
                  type="url"
                  placeholder="https://..."
                  required
                  value={uploadImageUrl}
                  onChange={(e) => setUploadImageUrl(e.target.value)}
                  className="flex-1 rounded-control border border-border bg-surface-2 px-3 py-2 text-xs text-foreground focus:border-primary focus:outline-none"
                />
                <button
                  type="button"
                  onClick={handleOpenCloudinary}
                  className="rounded-control border border-border bg-surface-2 px-3 py-2 text-xs text-fg-muted hover:text-foreground"
                >
                  Upload
                </button>
              </div>
            </div>

            <div>
              <label className="block text-xs font-medium text-fg-muted mb-1">Caption</label>
              <input
                type="text"
                placeholder="e.g. Problem discussion session"
                value={uploadCaption}
                onChange={(e) => setUploadCaption(e.target.value)}
                className="w-full rounded-control border border-border bg-surface-2 px-3 py-2 text-xs text-foreground focus:border-primary focus:outline-none"
              />
            </div>

            <button
              type="submit"
              disabled={isUploading || !selectedEventId}
              className="w-full inline-flex items-center justify-center gap-1.5 rounded-control bg-primary px-4 py-2 text-xs font-semibold text-primary-foreground shadow hover:bg-primary/90 disabled:opacity-50"
            >
              {isUploading && <Loader2 className="size-3.5 animate-spin" />}
              Upload to Event
            </button>
          </form>
        </div>
      </div>

      {/* Photos Grid */}
      <div className="space-y-4 lg:col-span-8">
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

        <div className="flex items-center justify-between">
          <span className="text-xs font-semibold text-foreground">
            {events.find((e) => e.id === selectedEventId)?.title || "Event Photos"}
          </span>
          <span className="text-xs text-fg-muted">{photos.length} photos</span>
        </div>

        {loading ? (
          <div className="flex h-48 items-center justify-center text-xs text-fg-muted">
            <Loader2 className="mr-2 size-4 animate-spin text-primary" /> Loading event photos...
          </div>
        ) : photos.length === 0 ? (
          <div className="rounded-panel border border-dashed border-border/80 p-8 text-center text-xs text-fg-muted">
            No photos uploaded for this event yet.
          </div>
        ) : (
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-3">
            {photos.map((photo) => (
              <div
                key={photo.id}
                className="group relative rounded-panel border border-border bg-surface-2 overflow-hidden"
              >
                <div className="aspect-video w-full bg-background">
                  <Image
                    src={photo.imageUrl}
                    alt={photo.caption || "Event photo"}
                    width={320}
                    height={180}
                    className="size-full object-cover"
                  />
                </div>
                <div className="p-2.5">
                  <p className="truncate text-xs font-medium text-foreground">{photo.caption || "—"}</p>
                  <p className="text-micro text-fg-subtle">
                    {new Date(photo.uploadedAt).toLocaleDateString()}
                  </p>
                </div>
                <button
                  onClick={() => handleDeletePhoto(photo.id)}
                  title="Delete photo"
                  className="absolute top-2 right-2 rounded-full bg-background/80 p-1.5 text-fg-muted opacity-0 backdrop-blur-sm transition-opacity hover:bg-red-500 hover:text-white group-hover:opacity-100"
                >
                  <Trash2 className="size-3.5" />
                </button>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
