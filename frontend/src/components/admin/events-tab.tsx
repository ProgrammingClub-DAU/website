"use client";

// The Events tab of the admin dashboard, loaded on demand by admin/page.tsx.
import React, { useState, useEffect, useCallback } from "react";
import Link from "next/link";
import Image from "next/image";
import { eventsService, type EventRequest } from "@/lib/services/events";
import { ConfirmDialog } from "@/components/admin/confirm-dialog";
import { openUploadWidget } from "@/lib/cloudinary";
import { DataTable, type Column } from "@/components/ui/data-table";
import { EVENT_TYPE_LABELS, type EventType, type Event } from "@/types/api";
import {
  Calendar,
  Plus,
  Trash2,
  Edit2,
  RotateCcw,
  CheckCircle2,
  XCircle,
  Loader2,
  Upload,
  UserCheck,
} from "lucide-react";

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

// ══════════════════════════════════════════════════════════════════
// TAB 2: EVENTS MANAGEMENT
// ══════════════════════════════════════════════════════════════════

export function EventsTab() {
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
    fetchEvents();
  }, [fetchEvents]);

  const handleOpenCloudinaryCover = () => {
    const opened = openUploadWidget({
      folder: "cpclub/events",
      onUpload: (url) => setFormData((prev) => ({ ...prev, coverImageUrl: url })),
    });
    if (!opened) {
      const url = window.prompt("Photo upload is unavailable. Paste a cover image URL instead:");
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
        const saved = await eventsService.updateEvent(editingEvent.id, payload);
        // The message also says whether its Codeforces contest was added to Contests.
        setStatusMessage({ type: "success", text: saved.message || "Event updated successfully." });
      } else {
        const saved = await eventsService.createEvent(payload);
        setStatusMessage({ type: "success", text: saved.message || "Event created successfully." });
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

  /**
   * Puts an event back to upcoming.
   *
   * Warns when results are still published, rather than silently switching them
   * off. An admin reopening an event to fix its attendance has not asked to
   * un-announce its winners, and quietly clearing settings they would have to
   * rebuild is the worse surprise -- but an "Upcoming" event showing a podium is
   * a contradiction they should get to resolve deliberately.
   */
  const handleReopenEvent = async (event: Event) => {
    const published = [
      event.showContestLink && "the contest link",
      event.showWinners && "the winners",
      event.showAttendeeCount && "the attendance count",
    ].filter(Boolean) as string[];

    if (published.length > 0) {
      const proceed = window.confirm(
        `This event still publishes ${published.join(", ")}. ` +
          "Reopening does not hide them, so the public page will show results for an " +
          "event listed as upcoming. Reopen anyway?"
      );
      if (!proceed) return;
    }

    try {
      await eventsService.reopen(event.id);
      setStatusMessage({ type: "success", text: "Event reopened. Attendance can be edited again." });
      await fetchEvents();
    } catch (err) {
      console.error("Failed to reopen event:", err);
      setStatusMessage({ type: "error", text: "Failed to reopen event." });
    }
  };

  // ── delete ──
  const [deleting, setDeleting] = useState<{
    event: Event;
    attendees: number;
    photos: number;
    winners: number;
  } | null>(null);
  const [deleteBusy, setDeleteBusy] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  /**
   * Opens the delete confirmation, after finding out what the event carries.
   *
   * The counts come from the event's own detail -- an admin sees them whatever
   * the publication switches say -- so the dialog can state what will be lost
   * before anything is sent.
   */
  const openDelete = async (event: Event) => {
    setDeleteError(null);
    try {
      const detail = await eventsService.getEventDetail(event.id);
      setDeleting({
        event,
        attendees: detail.attendeeCount ?? 0,
        photos: detail.photos.length,
        winners: detail.winners.length,
      });
    } catch (err) {
      console.error("Failed to inspect event before deleting:", err);
      setStatusMessage({ type: "error", text: "Could not load the event to delete it." });
    }
  };

  const confirmDelete = async () => {
    if (!deleting) return;
    const { event, attendees, photos, winners } = deleting;
    setDeleteBusy(true);
    setDeleteError(null);
    try {
      // force only when the admin has been shown records and typed the name.
      await eventsService.deleteEvent(event.id, attendees + photos + winners > 0);
      setDeleting(null);
      setStatusMessage({ type: "success", text: `"${event.title}" was deleted.` });
      await fetchEvents();
    } catch (err) {
      // A 409 here means records were added after the dialog opened; the
      // server's message says what, so it is shown as-is.
      const message =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
        "Could not delete the event.";
      setDeleteError(message);
    } finally {
      setDeleteBusy(false);
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
          {/* Completing an event freezes its attendance, so a mis-click used to
              be unrecoverable. Cancelled events reopen the same way. */}
          {e.status !== "UPCOMING" && (
            <button
              onClick={() => handleReopenEvent(e)}
              title="Reopen event - puts it back to upcoming and unfreezes attendance"
              className="rounded-control p-1 text-fg-muted hover:text-emerald-400"
            >
              <RotateCcw className="size-4" />
            </button>
          )}
          <button
            onClick={() => openDelete(e)}
            title="Delete event permanently"
            aria-label={`Delete ${e.title}`}
            className="rounded-control p-1 text-fg-muted hover:text-red-400"
          >
            <Trash2 className="size-4" />
          </button>
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

      <div className="hidden sm:block">
        <DataTable columns={columns} data={events} isLoading={loading} emptyMessage="No events created yet." />
      </div>

      <div className="block sm:hidden space-y-3">
        {loading ? (
          Array.from({ length: 3 }).map((_, i) => (
            <div key={i} className="h-28 animate-pulse rounded-panel bg-surface-2" />
          ))
        ) : events.length === 0 ? (
          <div className="rounded-panel border border-border bg-surface p-6 text-center text-sm text-fg-muted">
            No events created yet.
          </div>
        ) : (
          events.map((e) => {
            const statusColors = {
              UPCOMING: "border-emerald-500/40 bg-emerald-500/10 text-emerald-400",
              COMPLETED: "border-blue-500/40 bg-blue-500/10 text-blue-400",
              CANCELLED: "border-red-500/40 bg-red-500/10 text-red-400",
            }[e.status] || "border-border bg-surface-2 text-fg-muted";
            
            return (
              <div key={e.id} className="rounded-panel border border-border bg-surface p-4 space-y-3">
                <div className="flex items-start justify-between gap-3">
                  <div className="flex items-center gap-3">
                    <div className="size-10 shrink-0 rounded-panel border border-border bg-surface-2 overflow-hidden flex items-center justify-center">
                      {e.coverImageUrl ? (
                        <Image src={e.coverImageUrl} alt={e.title} width={40} height={40} className="size-full object-cover" />
                      ) : (
                        <Calendar className="size-4 text-fg-muted" />
                      )}
                    </div>
                    <div>
                      <Link href={`/admin/events/${e.id}`} className="font-semibold text-foreground hover:underline line-clamp-1">{e.title}</Link>
                      <div className="text-xs text-fg-muted">{e.location}</div>
                    </div>
                  </div>
                  <div className="flex gap-1.5 flex-col items-end">
                    <span className={`inline-flex rounded-full border px-2 py-0.5 text-nano font-bold uppercase tracking-wider ${statusColors}`}>
                      {e.status}
                    </span>
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-2 text-xs pt-1 border-t border-border/50">
                  <div>
                    <div className="text-fg-subtle">Date</div>
                    <div>
                      {new Date(e.eventDate).toLocaleDateString("en-US", { weekday: "short", month: "short", day: "numeric" })}
                    </div>
                  </div>
                  <div className="flex justify-end items-center gap-2">
                    <button onClick={() => openEdit(e)} className="p-2 rounded-control border border-border bg-surface-2 text-fg-muted hover:text-primary transition-colors">
                      <Edit2 className="size-3.5" />
                    </button>
                    <button onClick={() => openDelete(e)} className="p-2 rounded-control border border-border bg-surface-2 text-fg-muted hover:text-red-400 transition-colors">
                      <Trash2 className="size-3.5" />
                    </button>
                  </div>
                </div>
              </div>
            );
          })
        )}
      </div>

      <ConfirmDialog
        open={deleting !== null}
        title={deleting ? `Delete "${deleting.event.title}"?` : ""}
        description={
          deleting && deleting.attendees + deleting.photos + deleting.winners > 0 ? (
            <>
              <p>
                This permanently deletes the event and everything recorded against it:{" "}
                <strong className="text-foreground">
                  {[
                    deleting.attendees > 0 && `${deleting.attendees} attendance record${deleting.attendees === 1 ? "" : "s"}`,
                    deleting.photos > 0 && `${deleting.photos} photo${deleting.photos === 1 ? "" : "s"}`,
                    deleting.winners > 0 && `${deleting.winners} winner${deleting.winners === 1 ? "" : "s"}`,
                  ]
                    .filter(Boolean)
                    .join(", ")}
                </strong>
                . It cannot be undone, and the photos leave the gallery.
              </p>
              <p className="mt-2">
                To take the event down but keep its record, <strong className="text-foreground">cancel</strong> it
                instead.
              </p>
            </>
          ) : (
            <p>Nothing is recorded against this event yet. This cannot be undone.</p>
          )
        }
        confirmText={
          deleting && deleting.attendees + deleting.photos + deleting.winners > 0
            ? deleting.event.title
            : undefined
        }
        confirmLabel="Delete event"
        busy={deleteBusy}
        error={deleteError}
        onConfirm={confirmDelete}
        onCancel={() => {
          if (!deleteBusy) setDeleting(null);
        }}
      />

      {/*
        Modal for Create / Edit.

        The form is taller than a phone screen -- title, description, date,
        location, cover, type, contest URL and three switches. Centred and
        unscrollable, its top and bottom were cut off with no way to reach them,
        so Save could not be pressed at all.

        The backdrop scrolls, and the panel pins to the top on small screens:
        centring a tall panel pushes its head off the top edge.
      */}
      {showCreateModal && (
        <div className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-background/80 p-4 backdrop-blur-sm sm:items-center">
          <div className="my-4 sm:my-auto w-full max-w-lg space-y-4 rounded-panel border border-border bg-surface p-5 shadow-2xl sm:p-6">
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
