"use client";

import React, { useState, useEffect, useCallback, useMemo } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import Image from "next/image";
import { useAuthStore } from "@/store/auth";
import { eventsService } from "@/lib/services/events";
import { DataTable, type Column } from "@/components/ui/data-table";
import { ClubRoleBadge } from "@/components/ui/club-role-badge";
import type { EventDetail, EventAttendee, UserLookup } from "@/types/api";
import {
  ArrowLeft,
  Search,
  UserPlus,
  FileSpreadsheet,
  Trash2,
  AlertTriangle,
  CheckCircle2,
  XCircle,
  Users,
  Calendar,
  MapPin,
  Loader2,
  ShieldAlert,
  Phone,
} from "lucide-react";

export default function EventAttendeesPage() {
  const params = useParams();
  const eventId = Number(params?.id);

  const { user, isAuthenticated } = useAuthStore();

  const [event, setEvent] = useState<EventDetail | null>(null);
  const [attendees, setAttendees] = useState<EventAttendee[]>([]);
  const [loadingEvent, setLoadingEvent] = useState(true);
  const [loadingAttendees, setLoadingAttendees] = useState(true);

  // Search panel state
  const [searchId, setSearchId] = useState("");
  const [searching, setSearching] = useState(false);
  const [searchedUser, setSearchedUser] = useState<UserLookup | null>(null);
  const [searchError, setSearchError] = useState<string | null>(null);
  const [isAdding, setIsAdding] = useState(false);
  const [isExporting, setIsExporting] = useState(false);
  const [statusMessage, setStatusMessage] = useState<{ type: "success" | "error"; text: string } | null>(null);

  // Filter attendees state
  const [attendeeFilter, setAttendeeFilter] = useState("");

  const loadEventData = useCallback(async () => {
    if (!eventId || isNaN(eventId)) return;
    setLoadingEvent(true);
    try {
      const data = await eventsService.getEventDetail(eventId);
      setEvent(data);
    } catch (err) {
      console.error("Failed to load event:", err);
      setStatusMessage({ type: "error", text: "Failed to load event details." });
    } finally {
      setLoadingEvent(false);
    }
  }, [eventId]);

  const loadAttendees = useCallback(async () => {
    if (!eventId || isNaN(eventId)) return;
    setLoadingAttendees(true);
    try {
      const data = await eventsService.getAttendees(eventId);
      setAttendees(data);
    } catch (err) {
      console.error("Failed to load attendees:", err);
    } finally {
      setLoadingAttendees(false);
    }
  }, [eventId]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- data fetch on mount, not derived state
    loadEventData();
    loadAttendees();
  }, [loadEventData, loadAttendees]);


  const handleSearchStudent = async (e: React.FormEvent) => {
    e.preventDefault();
    setSearchError(null);
    setSearchedUser(null);

    const trimmed = searchId.trim();
    if (!trimmed) {
      setSearchError("Please enter a student ID.");
      return;
    }

    const idNum = Number(trimmed);
    if (isNaN(idNum) || idNum <= 0) {
      setSearchError("Student ID must be a valid positive number.");
      return;
    }

    setSearching(true);
    try {
      const result = await eventsService.lookupUser(idNum);
      setSearchedUser(result);
    } catch (err: unknown) {
      console.error("User lookup error:", err);
      setSearchError("No student found with this ID.");
    } finally {
      setSearching(false);
    }
  };

  const isAlreadyAdded = Boolean(
    searchedUser && attendees.some((a) => a.userId === searchedUser.id)
  );

  const handleAddAttendee = async () => {
    if (!searchedUser || !eventId) return;

    setIsAdding(true);
    setStatusMessage(null);
    try {
      await eventsService.addAttendee(eventId, searchedUser.id);
      setStatusMessage({
        type: "success",
        text: `Added ${searchedUser.name} to event!`,
      });
      setSearchedUser(null);
      setSearchId("");
      await loadAttendees();
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        "Failed to add attendee.";
      setStatusMessage({ type: "error", text: msg });
    } finally {
      setIsAdding(false);
    }
  };

  const handleRemoveAttendee = async (userId: number, name: string) => {
    const confirm = window.confirm(`Remove ${name} from this event's attendance list?`);
    if (!confirm) return;

    try {
      await eventsService.removeAttendee(eventId, userId);
      setStatusMessage({ type: "success", text: `Removed ${name} from attendance.` });
      await loadAttendees();
    } catch (err) {
      console.error("Failed to remove attendee:", err);
      setStatusMessage({ type: "error", text: "Failed to remove attendee." });
    }
  };

  const handleExportExcel = async () => {
    if (!eventId) return;
    setIsExporting(true);
    try {
      const res = await eventsService.exportAttendees(eventId);
      const blob = new Blob([res.data], {
        type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
      });
      const downloadUrl = window.URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = downloadUrl;
      const safeTitle = event?.title ? event.title.toLowerCase().replace(/[^a-z0-9]/g, "-") : "event";
      a.download = `${safeTitle}-attendees.xlsx`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      window.URL.revokeObjectURL(downloadUrl);
      setStatusMessage({ type: "success", text: "Excel attendance sheet downloaded." });
    } catch (err) {
      console.error("Excel export error:", err);
      setStatusMessage({ type: "error", text: "Failed to export attendance sheet." });
    } finally {
      setIsExporting(false);
    }
  };

  const filteredAttendees = useMemo(() => {
    const q = attendeeFilter.toLowerCase().trim();
    if (!q) return attendees;
    return attendees.filter(
      (a) =>
        a.name.toLowerCase().includes(q) ||
        a.email.toLowerCase().includes(q) ||
        (a.codeforcesHandle ?? "").toLowerCase().includes(q) ||
        (a.leetcodeHandle ?? "").toLowerCase().includes(q) ||
        String(a.userId).includes(q)
    );
  }, [attendees, attendeeFilter]);

  // Placed after every hook: an early return above useMemo changes the number of
  // hooks React sees between renders, which is the rules-of-hooks error.
  // Auth guards
  if (!isAuthenticated || user?.role !== "ROLE_ADMIN") {
    return (
      <div className="flex min-h-[400px] flex-col items-center justify-center gap-3 text-center">
        <ShieldAlert className="size-10 text-red-500" />
        <h2 className="text-lg font-bold">Admin Access Required</h2>
        <p className="text-xs text-fg-muted max-w-sm">
          You need administrative access to manage event attendees.
        </p>
        <Link
          href="/admin"
          className="mt-2 rounded-control bg-primary px-4 py-2 text-xs font-semibold text-primary-foreground shadow"
        >
          Go to Admin
        </Link>
      </div>
    );
  }

  const attendeeColumns: Column<EventAttendee>[] = [
    {
      key: "userId",
      header: "ID",
      className: "font-mono text-xs text-fg-muted",
      render: (a) => `#${a.userId}`,
    },
    {
      key: "name",
      header: "Student",
      render: (a) => (
        <div className="flex items-center gap-2.5">
          <div className="size-7 shrink-0 rounded-full border border-border bg-surface-2 overflow-hidden flex items-center justify-center">
            {a.avatarUrl ? (
              <Image src={a.avatarUrl} alt={a.name} width={28} height={28} className="size-full object-cover" />
            ) : (
              <Users className="size-3.5 text-fg-muted" />
            )}
          </div>
          <div>
            <Link href={`/profile/${a.userId}`} className="font-semibold text-foreground hover:underline">
              {a.name}
            </Link>
            <div className="font-mono text-micro text-fg-muted">{a.email}</div>
          </div>
        </div>
      ),
    },
    {
      key: "phoneNumber",
      header: "Phone",
      className: "text-xs",
      render: (a) => {
        if (!a.phoneNumber && !a.hasPhone) {
          return (
            <span className="inline-flex items-center gap-1 text-amber-400 font-mono text-micro" title="Phone missing">
              <AlertTriangle className="size-3" /> No Phone
            </span>
          );
        }
        return <span className="font-mono">{a.phoneNumber}</span>;
      },
    },
    {
      key: "clubRole",
      header: "Role",
      render: (a) => <ClubRoleBadge clubRole={a.clubRole} showIcon={false} />,
    },
    {
      key: "cf",
      header: "Codeforces",
      render: (a) =>
        a.codeforcesHandle ? (
          <span className="text-xs">
            @{a.codeforcesHandle} <span className="text-fg-muted">({a.cfRating ?? "—"})</span>
          </span>
        ) : (
          <span className="text-fg-subtle text-xs">--</span>
        ),
    },
    {
      key: "leetcode",
      header: "LeetCode",
      render: (a) =>
        a.leetcodeHandle ? (
          <span className="text-xs">
            @{a.leetcodeHandle} <span className="text-fg-muted">({a.leetcodeRating ?? "—"})</span>
          </span>
        ) : (
          <span className="text-fg-subtle text-xs">--</span>
        ),
    },
    {
      key: "addedAt",
      header: "Recorded At",
      className: "text-xs text-fg-muted",
      render: (a) => (a.addedAt ? new Date(a.addedAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }) : "—"),
    },
    {
      key: "actions",
      header: "Action",
      className: "text-right",
      render: (a) => (
        <button
          onClick={() => handleRemoveAttendee(a.userId, a.name)}
          title="Remove from event"
          className="rounded-control p-1 text-fg-muted hover:text-red-400 transition-colors"
        >
          <Trash2 className="size-4" />
        </button>
      ),
    },
  ];

  return (
    <div className="mx-auto max-w-7xl space-y-6 pb-16 pt-6">
      {/* Back button & Event header */}
      <div className="space-y-3">
        <Link
          href="/admin"
          className="inline-flex items-center gap-1.5 text-xs font-semibold text-fg-muted hover:text-foreground transition-colors"
        >
          <ArrowLeft className="size-3.5" /> Back to Admin Dashboard
        </Link>

        {loadingEvent ? (
          <div className="h-14 animate-pulse rounded-panel bg-surface-2" />
        ) : event ? (
          <div className="flex flex-col gap-3 rounded-panel border border-border bg-surface p-5 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <div className="flex items-center gap-2">
                <h1 className="text-xl font-bold tracking-tight text-foreground">{event.title}</h1>
                <span className="rounded-full border border-primary/30 bg-primary/10 px-2.5 py-0.5 text-nano font-semibold uppercase text-primary">
                  {event.status}
                </span>
              </div>
              <div className="mt-1 flex flex-wrap items-center gap-4 text-xs text-fg-muted">
                <span className="flex items-center gap-1">
                  <Calendar className="size-3.5" />
                  {new Date(event.eventDate).toLocaleString("en-US", {
                    weekday: "short",
                    month: "short",
                    day: "numeric",
                    year: "numeric",
                    hour: "2-digit",
                    minute: "2-digit",
                  })}
                </span>
                <span className="flex items-center gap-1">
                  <MapPin className="size-3.5" /> {event.location}
                </span>
              </div>
            </div>

            <div className="flex items-center gap-3">
              <span className="inline-flex items-center gap-1 rounded-full border border-border bg-surface-2 px-3 py-1 text-xs font-semibold text-foreground">
                <Users className="size-3.5 text-primary" /> {attendees.length} Attendees
              </span>
              <button
                onClick={handleExportExcel}
                disabled={isExporting || attendees.length === 0}
                className="inline-flex items-center gap-1.5 rounded-control bg-emerald-600 px-3.5 py-1.5 text-xs font-semibold text-white shadow-sm hover:bg-emerald-500 disabled:opacity-50 transition-colors"
              >
                {isExporting ? <Loader2 className="size-3.5 animate-spin" /> : <FileSpreadsheet className="size-3.5" />}
                Export to Excel
              </button>
            </div>
          </div>
        ) : null}
      </div>

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

      {/* 2-Column Layout */}
      <div className="grid gap-8 lg:grid-cols-12">
        {/* LEFT COLUMN: Student Search & Add Panel (4 cols) */}
        <div className="space-y-4 lg:col-span-4">
          <div className="rounded-panel border border-border bg-surface p-5 space-y-4">
            <div>
              <h2 className="text-sm font-bold text-foreground flex items-center gap-2">
                <UserPlus className="size-4 text-primary" />
                Record Student Attendance
              </h2>
              <p className="mt-0.5 text-micro text-fg-muted">
                Search member by Student ID to verify details and mark present.
              </p>
            </div>

            <form onSubmit={handleSearchStudent} className="flex gap-2">
              <input
                type="number"
                placeholder="Enter Student ID (e.g. 42)"
                value={searchId}
                onChange={(e) => setSearchId(e.target.value)}
                className="flex-1 rounded-control border border-border bg-surface-2 px-3 py-2 text-xs text-foreground focus:border-primary focus:outline-none"
              />
              <button
                type="submit"
                disabled={searching}
                className="inline-flex items-center gap-1 rounded-control bg-primary px-3 py-2 text-xs font-semibold text-primary-foreground shadow hover:bg-primary/90 disabled:opacity-50"
              >
                {searching ? <Loader2 className="size-3.5 animate-spin" /> : <Search className="size-3.5" />}
                Search
              </button>
            </form>

            {searchError && (
              <div className="flex items-center gap-2 rounded-panel border border-red-500/30 bg-red-500/10 p-2.5 text-xs text-red-400">
                <AlertTriangle className="size-4 shrink-0" />
                <span>{searchError}</span>
              </div>
            )}

            {/* Preview Card */}
            {searchedUser && (
              <div className="space-y-4 rounded-panel border border-border bg-surface-2 p-4 animate-in fade-in-50">
                <div className="flex items-start gap-3">
                  <div className="size-12 shrink-0 rounded-full border border-border bg-background overflow-hidden flex items-center justify-center">
                    {searchedUser.avatarUrl ? (
                      <Image
                        src={searchedUser.avatarUrl}
                        alt={searchedUser.name}
                        width={48}
                        height={48}
                        className="size-full object-cover"
                      />
                    ) : (
                      <Users className="size-6 text-fg-muted" />
                    )}
                  </div>

                  <div className="min-w-0 flex-1">
                    <h3 className="font-bold text-foreground truncate">{searchedUser.name}</h3>
                    <p className="font-mono text-micro text-fg-muted truncate">{searchedUser.email}</p>
                    <div className="mt-1 flex flex-wrap gap-1.5">
                      <ClubRoleBadge clubRole={searchedUser.clubRole} showIcon={false} />
                      {searchedUser.batchYear && (
                        <span className="rounded-full border border-border bg-background px-2 py-0.5 text-nano text-fg-muted">
                          Batch {searchedUser.batchYear}
                        </span>
                      )}
                    </div>
                  </div>
                </div>

                {/* Phone warning or number */}
                <div>
                  {searchedUser.phoneNumber ? (
                    <div className="flex items-center gap-1.5 text-xs text-foreground font-mono">
                      <Phone className="size-3 text-fg-muted" />
                      <span>{searchedUser.phoneNumber}</span>
                    </div>
                  ) : (
                    <div className="flex items-center gap-1.5 rounded border border-amber-500/30 bg-amber-500/10 px-2 py-1 text-micro text-amber-400">
                      <AlertTriangle className="size-3 shrink-0" />
                      <span>No phone number — ask member to update profile</span>
                    </div>
                  )}
                </div>

                {/* Handles */}
                <div className="grid grid-cols-2 gap-2 text-micro">
                  <div className="rounded border border-border bg-background p-2">
                    <div className="text-fg-muted">Codeforces</div>
                    <div className="font-semibold truncate">
                      {searchedUser.codeforcesHandle ? `@${searchedUser.codeforcesHandle}` : "None"}
                    </div>
                    {searchedUser.cfRating && <div className="text-fg-subtle">{searchedUser.cfRating} rating</div>}
                  </div>
                  <div className="rounded border border-border bg-background p-2">
                    <div className="text-fg-muted">LeetCode</div>
                    <div className="font-semibold truncate">
                      {searchedUser.leetcodeHandle ? `@${searchedUser.leetcodeHandle}` : "None"}
                    </div>
                    {searchedUser.leetcodeRating && (
                      <div className="text-fg-subtle">{searchedUser.leetcodeRating} rating</div>
                    )}
                  </div>
                </div>

                {/* Add button */}
                <button
                  type="button"
                  onClick={handleAddAttendee}
                  disabled={isAdding || isAlreadyAdded}
                  className={`w-full inline-flex items-center justify-center gap-2 rounded-control px-4 py-2.5 text-xs font-bold transition-all ${
                    isAlreadyAdded
                      ? "border border-emerald-500/40 bg-emerald-500/10 text-emerald-400 cursor-not-allowed"
                      : "bg-primary text-primary-foreground shadow hover:bg-primary/90 disabled:opacity-50"
                  }`}
                >
                  {isAdding ? (
                    <Loader2 className="size-4 animate-spin" />
                  ) : isAlreadyAdded ? (
                    <>
                      <CheckCircle2 className="size-4" /> Already Added to Event
                    </>
                  ) : (
                    <>
                      <UserPlus className="size-4" /> Add to Event Attendance
                    </>
                  )}
                </button>
              </div>
            )}
          </div>
        </div>

        {/* RIGHT COLUMN: Attendees List (8 cols) */}
        <div className="space-y-4 lg:col-span-8">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div className="relative flex-1 max-w-sm">
              <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-fg-muted" />
              <input
                type="text"
                placeholder="Filter attendees by name, handle, or ID..."
                value={attendeeFilter}
                onChange={(e) => setAttendeeFilter(e.target.value)}
                className="w-full rounded-control border border-border bg-surface-2 py-2 pl-9 pr-4 text-xs text-foreground placeholder:text-fg-muted focus:border-primary focus:outline-none"
              />
            </div>
            <div className="text-xs text-fg-muted">
              Showing {filteredAttendees.length} of {attendees.length} attendees
            </div>
          </div>

          <DataTable
            columns={attendeeColumns}
            data={filteredAttendees}
            isLoading={loadingAttendees}
            emptyMessage="No attendees registered for this event yet."
          />
        </div>
      </div>
    </div>
  );
}
