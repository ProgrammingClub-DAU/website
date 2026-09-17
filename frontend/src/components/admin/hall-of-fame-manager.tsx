"use client";

/**
 * The admin side of the Hall of Fame: list, create, edit, delete.
 *
 * An entry is edited as a whole -- heading, dates, write-up, and the ordered
 * lists of links and photos -- and saved in one request, matching how the API
 * replaces it. Nothing is written until Save, so closing the editor discards
 * the draft cleanly.
 */

import { useCallback, useEffect, useState } from "react";
import Image from "next/image";
import axios from "axios";
import {
  ArrowDown,
  ArrowUp,
  CalendarDays,
  Edit2,
  ExternalLink,
  ImagePlus,
  Link2,
  Loader2,
  Plus,
  Trash2,
  Trophy,
  X,
} from "lucide-react";

import { openUploadWidget } from "@/lib/cloudinary";
import { hallOfFameService, type HallOfFameEntryRequest } from "@/lib/services/hall-of-fame";
import type { HallOfFameEntry } from "@/types/api";

interface LinkDraft {
  key: string;
  label: string;
  url: string;
}

interface PhotoDraft {
  key: string;
  imageUrl: string;
  caption: string;
}

interface Draft {
  heading: string;
  subheading: string;
  details: string;
  achievedOn: string;
  links: LinkDraft[];
  photos: PhotoDraft[];
}

const LIMITS = { links: 10, photos: 30 };

/** Matches the server's rule, so a bad URL is caught while it is being typed. */
const WEB_URL = /^https?:\/\/\S+$/;

let keySeed = 0;
const newKey = () => `k${++keySeed}`;

/** Today in the admin's own timezone. toISOString would give the UTC date, which
 *  in India is still yesterday until 05:30. */
function today(): string {
  const now = new Date();
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
}

function emptyDraft(): Draft {
  return { heading: "", subheading: "", details: "", achievedOn: today(), links: [], photos: [] };
}

function draftFrom(entry: HallOfFameEntry): Draft {
  return {
    heading: entry.heading,
    subheading: entry.subheading ?? "",
    details: entry.details ?? "",
    achievedOn: entry.achievedOn,
    links: entry.links.map((l) => ({ key: newKey(), label: l.label, url: l.url })),
    photos: entry.photos.map((p) => ({ key: newKey(), imageUrl: p.imageUrl, caption: p.caption ?? "" })),
  };
}

function formatDay(isoDate: string): string {
  const [y, m, d] = isoDate.split("-").map(Number);
  return new Date(Date.UTC(y, m - 1, d)).toLocaleDateString("en-IN", {
    day: "numeric",
    month: "short",
    year: "numeric",
    timeZone: "UTC",
  });
}

function move<T>(list: T[], from: number, to: number): T[] {
  if (to < 0 || to >= list.length) return list;
  const next = [...list];
  const [item] = next.splice(from, 1);
  next.splice(to, 0, item);
  return next;
}

/** The server explains validation failures; show its words rather than a generic line. */
function errorMessage(err: unknown, fallback: string): string {
  if (axios.isAxiosError(err)) {
    const data = err.response?.data as { message?: string; data?: Record<string, string> } | undefined;
    // Field errors arrive as a map; the first one is the useful sentence.
    const firstField = data?.data && typeof data.data === "object" ? Object.values(data.data)[0] : undefined;
    return firstField ?? data?.message ?? fallback;
  }
  return fallback;
}

export function HallOfFameManager() {
  const [entries, setEntries] = useState<HallOfFameEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [status, setStatus] = useState<{ type: "success" | "error"; text: string } | null>(null);

  // null: editor closed. "new": creating. A number: editing that entry.
  const [editing, setEditing] = useState<number | "new" | null>(null);
  const [draft, setDraft] = useState<Draft>(emptyDraft);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  /** Re-reads the list after a save or delete. */
  const load = useCallback(async () => {
    try {
      setEntries(await hallOfFameService.list());
    } catch (err) {
      setStatus({ type: "error", text: errorMessage(err, "Could not load the Hall of Fame.") });
    }
  }, []);

  // First load. State is only set from the promise, never synchronously in the
  // effect, and a response that arrives after the tab was switched is dropped.
  useEffect(() => {
    let cancelled = false;
    hallOfFameService
      .list()
      .then((list) => {
        if (!cancelled) setEntries(list);
      })
      .catch((err) => {
        if (!cancelled) {
          setStatus({ type: "error", text: errorMessage(err, "Could not load the Hall of Fame.") });
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const openNew = () => {
    setDraft(emptyDraft());
    setFormError(null);
    setEditing("new");
  };

  const openEdit = (entry: HallOfFameEntry) => {
    setDraft(draftFrom(entry));
    setFormError(null);
    setEditing(entry.id);
  };

  const close = () => {
    if (saving) return;
    setEditing(null);
  };

  // ── links ──────────────────────────────────────────────────────────────────
  const addLink = () =>
    setDraft((d) =>
      d.links.length >= LIMITS.links ? d : { ...d, links: [...d.links, { key: newKey(), label: "", url: "" }] }
    );
  const updateLink = (key: string, patch: Partial<LinkDraft>) =>
    setDraft((d) => ({ ...d, links: d.links.map((l) => (l.key === key ? { ...l, ...patch } : l)) }));
  const removeLink = (key: string) =>
    setDraft((d) => ({ ...d, links: d.links.filter((l) => l.key !== key) }));

  // ── photos ─────────────────────────────────────────────────────────────────
  const addPhotoUrl = (imageUrl: string) =>
    setDraft((d) =>
      d.photos.length >= LIMITS.photos
        ? d
        : { ...d, photos: [...d.photos, { key: newKey(), imageUrl, caption: "" }] }
    );

  const uploadPhotos = () => {
    const remaining = LIMITS.photos - draft.photos.length;
    if (remaining <= 0) return;

    const opened = openUploadWidget({
      folder: "cpclub/hall-of-fame",
      multiple: true,
      maxFiles: remaining,
      // Each finished file lands in the draft as it completes, so a slow
      // connection shows progress rather than nothing until the end.
      onUpload: addPhotoUrl,
    });

    if (!opened) {
      const url = window.prompt("The uploader has not loaded. Paste an image URL instead:");
      if (url && WEB_URL.test(url.trim())) addPhotoUrl(url.trim());
    }
  };

  const updatePhoto = (key: string, caption: string) =>
    setDraft((d) => ({ ...d, photos: d.photos.map((p) => (p.key === key ? { ...p, caption } : p)) }));
  const removePhoto = (key: string) =>
    setDraft((d) => ({ ...d, photos: d.photos.filter((p) => p.key !== key) }));

  // ── save / delete ──────────────────────────────────────────────────────────
  const validate = (): string | null => {
    if (!draft.heading.trim()) return "Add a heading.";
    if (!draft.achievedOn) return "Pick the date it happened.";
    for (const link of draft.links) {
      if (!link.label.trim()) return "Every link needs a label.";
      if (!WEB_URL.test(link.url.trim())) return `"${link.label || "A link"}" needs a URL starting with https://`;
    }
    return null;
  };

  const save = async (event: React.FormEvent) => {
    event.preventDefault();
    const problem = validate();
    if (problem) {
      setFormError(problem);
      return;
    }

    const payload: HallOfFameEntryRequest = {
      heading: draft.heading.trim(),
      subheading: draft.subheading.trim() || null,
      details: draft.details.trim() || null,
      achievedOn: draft.achievedOn,
      links: draft.links.map((l) => ({ label: l.label.trim(), url: l.url.trim() })),
      photos: draft.photos.map((p) => ({ imageUrl: p.imageUrl, caption: p.caption.trim() || null })),
    };

    setSaving(true);
    setFormError(null);
    try {
      if (editing === "new") {
        await hallOfFameService.create(payload);
        setStatus({ type: "success", text: "Entry added to the Hall of Fame." });
      } else if (typeof editing === "number") {
        await hallOfFameService.update(editing, payload);
        setStatus({ type: "success", text: "Entry updated." });
      }
      setEditing(null);
      await load();
    } catch (err) {
      setFormError(errorMessage(err, "Could not save the entry."));
    } finally {
      setSaving(false);
    }
  };

  const remove = async (entry: HallOfFameEntry) => {
    const confirmed = window.confirm(
      `Delete "${entry.heading}" from the Hall of Fame? Its photos leave the gallery too. This cannot be undone.`
    );
    if (!confirmed) return;
    try {
      await hallOfFameService.remove(entry.id);
      setStatus({ type: "success", text: "Entry deleted." });
      await load();
    } catch (err) {
      setStatus({ type: "error", text: errorMessage(err, "Could not delete the entry.") });
    }
  };

  const inputClass =
    "w-full rounded-control border border-border bg-surface-2 px-3 py-2 text-sm text-foreground placeholder:text-fg-subtle focus:border-primary focus:outline-none";

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold text-foreground">Hall of Fame</h2>
          <p className="text-xs text-fg-muted">
            Achievements, newest first. Photos added here also appear in the public gallery.
          </p>
        </div>
        <button
          type="button"
          onClick={openNew}
          className="inline-flex items-center gap-2 rounded-control bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground transition-opacity hover:opacity-90"
        >
          <Plus className="size-4" />
          New entry
        </button>
      </div>

      {status && (
        <div
          role="status"
          className={`flex items-start justify-between gap-3 rounded-panel border p-3 text-xs ${
            status.type === "success"
              ? "border-emerald-500/30 bg-emerald-500/10 text-emerald-400"
              : "border-red-500/30 bg-red-500/10 text-red-400"
          }`}
        >
          <span>{status.text}</span>
          <button type="button" onClick={() => setStatus(null)} aria-label="Dismiss">
            <X className="size-4" />
          </button>
        </div>
      )}

      {loading ? (
        <div className="space-y-2">
          {[0, 1, 2].map((i) => (
            <div key={i} className="h-20 animate-pulse rounded-panel bg-surface-2" />
          ))}
        </div>
      ) : entries.length === 0 ? (
        <div className="rounded-panel border border-dashed border-border py-12 text-center">
          <Trophy className="mx-auto size-6 text-fg-subtle" />
          <p className="mt-3 text-sm text-fg-muted">No entries yet.</p>
          <p className="mt-1 text-xs text-fg-subtle">Add the club&apos;s first achievement.</p>
        </div>
      ) : (
        <ul className="space-y-2">
          {entries.map((entry) => (
            <li
              key={entry.id}
              className="flex items-center gap-4 rounded-panel border border-border bg-surface p-3 sm:p-4"
            >
              <div className="relative size-14 shrink-0 overflow-hidden rounded-control bg-surface-2">
                {entry.photos[0] ? (
                  <Image src={entry.photos[0].imageUrl} alt="" fill sizes="56px" className="object-cover" />
                ) : (
                  <Trophy className="absolute inset-0 m-auto size-5 text-fg-subtle" />
                )}
              </div>

              <div className="min-w-0 flex-1">
                <p className="truncate text-sm font-semibold text-foreground">{entry.heading}</p>
                <p className="mt-0.5 flex flex-wrap items-center gap-x-3 gap-y-0.5 text-xs text-fg-muted">
                  <span className="inline-flex items-center gap-1">
                    <CalendarDays className="size-3" />
                    {formatDay(entry.achievedOn)}
                  </span>
                  <span>
                    {entry.photos.length} {entry.photos.length === 1 ? "photo" : "photos"}
                  </span>
                  <span>
                    {entry.links.length} {entry.links.length === 1 ? "link" : "links"}
                  </span>
                </p>
              </div>

              <div className="flex shrink-0 items-center gap-1">
                <a
                  href={`/hall-of-fame/${entry.id}`}
                  target="_blank"
                  rel="noopener noreferrer"
                  title="View on the site"
                  className="rounded-control p-2 text-fg-muted hover:text-foreground"
                >
                  <ExternalLink className="size-4" />
                </a>
                <button
                  type="button"
                  onClick={() => openEdit(entry)}
                  title="Edit"
                  className="rounded-control p-2 text-fg-muted hover:text-foreground"
                >
                  <Edit2 className="size-4" />
                </button>
                <button
                  type="button"
                  onClick={() => remove(entry)}
                  title="Delete"
                  className="rounded-control p-2 text-fg-muted hover:text-red-400"
                >
                  <Trash2 className="size-4" />
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}

      {/* The editor. The backdrop scrolls and the panel pins to the top on small
          screens -- the form is taller than a phone, and a centred, fixed panel
          cuts off its own Save button. */}
      {editing !== null && (
        <div className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-background/80 p-4 backdrop-blur-sm sm:items-center">
          <form
            onSubmit={save}
            className="my-auto w-full max-w-2xl space-y-5 rounded-panel border border-border bg-surface p-5 shadow-2xl sm:p-6"
          >
            <div className="flex items-center justify-between gap-3">
              <h3 className="text-base font-bold text-foreground">
                {editing === "new" ? "New Hall of Fame entry" : "Edit entry"}
              </h3>
              <button type="button" onClick={close} aria-label="Close" className="text-fg-muted hover:text-foreground">
                <X className="size-5" />
              </button>
            </div>

            <div className="grid gap-4 sm:grid-cols-[1fr_11rem]">
              <div>
                <label htmlFor="hof-heading" className="mb-1 block text-xs font-medium text-fg-muted">
                  Heading <span className="text-red-400">*</span>
                </label>
                <input
                  id="hof-heading"
                  required
                  maxLength={200}
                  placeholder="ICPC Amritapuri Regional -- Rank 42"
                  value={draft.heading}
                  onChange={(e) => setDraft({ ...draft, heading: e.target.value })}
                  className={inputClass}
                />
              </div>
              <div>
                <label htmlFor="hof-date" className="mb-1 block text-xs font-medium text-fg-muted">
                  Date <span className="text-red-400">*</span>
                </label>
                <input
                  id="hof-date"
                  type="date"
                  required
                  value={draft.achievedOn}
                  onChange={(e) => setDraft({ ...draft, achievedOn: e.target.value })}
                  className={inputClass}
                />
              </div>
            </div>

            <div>
              <label htmlFor="hof-subheading" className="mb-1 block text-xs font-medium text-fg-muted">
                Subheading
              </label>
              <input
                id="hof-subheading"
                maxLength={300}
                placeholder="Team DAU Alpha -- Ravi Patel, Anita Shah, Priya Mehta"
                value={draft.subheading}
                onChange={(e) => setDraft({ ...draft, subheading: e.target.value })}
                className={inputClass}
              />
            </div>

            <div>
              <label htmlFor="hof-details" className="mb-1 block text-xs font-medium text-fg-muted">
                Details
              </label>
              <textarea
                id="hof-details"
                rows={4}
                maxLength={5000}
                placeholder="What happened, and why it matters."
                value={draft.details}
                onChange={(e) => setDraft({ ...draft, details: e.target.value })}
                className={`${inputClass} resize-y`}
              />
            </div>

            {/* ── links ── */}
            <fieldset className="space-y-2">
              <div className="flex items-center justify-between">
                <legend className="text-xs font-medium text-fg-muted">
                  Links <span className="text-fg-subtle">({draft.links.length}/{LIMITS.links})</span>
                </legend>
                <button
                  type="button"
                  onClick={addLink}
                  disabled={draft.links.length >= LIMITS.links}
                  className="inline-flex items-center gap-1 text-xs font-semibold text-primary disabled:opacity-40"
                >
                  <Link2 className="size-3.5" /> Add link
                </button>
              </div>

              {draft.links.length === 0 && (
                <p className="text-xs text-fg-subtle">A ranklist, an editorial, a news post.</p>
              )}

              {draft.links.map((link, i) => (
                <div key={link.key} className="flex flex-wrap items-center gap-2 sm:flex-nowrap">
                  <input
                    aria-label={`Link ${i + 1} label`}
                    placeholder="Ranklist"
                    maxLength={100}
                    value={link.label}
                    onChange={(e) => updateLink(link.key, { label: e.target.value })}
                    className={`${inputClass} sm:w-40`}
                  />
                  <input
                    aria-label={`Link ${i + 1} URL`}
                    type="url"
                    placeholder="https://..."
                    maxLength={512}
                    value={link.url}
                    onChange={(e) => updateLink(link.key, { url: e.target.value })}
                    aria-invalid={link.url !== "" && !WEB_URL.test(link.url.trim())}
                    className={`${inputClass} flex-1 aria-[invalid=true]:border-red-400`}
                  />
                  <div className="flex items-center">
                    <button
                      type="button"
                      onClick={() => setDraft((d) => ({ ...d, links: move(d.links, i, i - 1) }))}
                      disabled={i === 0}
                      aria-label="Move up"
                      className="p-1.5 text-fg-muted hover:text-foreground disabled:opacity-30"
                    >
                      <ArrowUp className="size-3.5" />
                    </button>
                    <button
                      type="button"
                      onClick={() => setDraft((d) => ({ ...d, links: move(d.links, i, i + 1) }))}
                      disabled={i === draft.links.length - 1}
                      aria-label="Move down"
                      className="p-1.5 text-fg-muted hover:text-foreground disabled:opacity-30"
                    >
                      <ArrowDown className="size-3.5" />
                    </button>
                    <button
                      type="button"
                      onClick={() => removeLink(link.key)}
                      aria-label="Remove link"
                      className="p-1.5 text-fg-muted hover:text-red-400"
                    >
                      <X className="size-4" />
                    </button>
                  </div>
                </div>
              ))}
            </fieldset>

            {/* ── photos ── */}
            <fieldset className="space-y-2">
              <div className="flex items-center justify-between">
                <legend className="text-xs font-medium text-fg-muted">
                  Photos <span className="text-fg-subtle">({draft.photos.length}/{LIMITS.photos})</span>
                </legend>
                <button
                  type="button"
                  onClick={uploadPhotos}
                  disabled={draft.photos.length >= LIMITS.photos}
                  className="inline-flex items-center gap-1 text-xs font-semibold text-primary disabled:opacity-40"
                >
                  <ImagePlus className="size-3.5" /> Upload photos
                </button>
              </div>

              {draft.photos.length === 0 ? (
                <button
                  type="button"
                  onClick={uploadPhotos}
                  className="flex w-full flex-col items-center gap-1 rounded-panel border border-dashed border-border py-8 text-center transition-colors hover:border-primary"
                >
                  <ImagePlus className="size-5 text-fg-subtle" />
                  <span className="text-xs text-fg-muted">Select one or more photos</span>
                  <span className="text-nano text-fg-subtle">JPG, PNG or WebP, up to 5 MB each</span>
                </button>
              ) : (
                <ul className="grid gap-2 sm:grid-cols-2">
                  {draft.photos.map((photo, i) => (
                    <li key={photo.key} className="flex gap-2 rounded-control border border-border bg-surface-2 p-2">
                      <div className="relative size-16 shrink-0 overflow-hidden rounded-xs bg-surface-3">
                        <Image src={photo.imageUrl} alt="" fill sizes="64px" className="object-cover" />
                        {i === 0 && (
                          <span className="absolute inset-x-0 bottom-0 bg-black/70 text-center font-mono text-nano text-white">
                            COVER
                          </span>
                        )}
                      </div>
                      <div className="flex min-w-0 flex-1 flex-col gap-1">
                        <input
                          aria-label={`Photo ${i + 1} caption`}
                          placeholder="Caption (optional)"
                          maxLength={300}
                          value={photo.caption}
                          onChange={(e) => updatePhoto(photo.key, e.target.value)}
                          className="w-full rounded-xs border border-border bg-surface px-2 py-1 text-xs text-foreground focus:border-primary focus:outline-none"
                        />
                        <div className="flex items-center">
                          <button
                            type="button"
                            onClick={() => setDraft((d) => ({ ...d, photos: move(d.photos, i, i - 1) }))}
                            disabled={i === 0}
                            aria-label="Move earlier"
                            className="p-1 text-fg-muted hover:text-foreground disabled:opacity-30"
                          >
                            <ArrowUp className="size-3.5" />
                          </button>
                          <button
                            type="button"
                            onClick={() => setDraft((d) => ({ ...d, photos: move(d.photos, i, i + 1) }))}
                            disabled={i === draft.photos.length - 1}
                            aria-label="Move later"
                            className="p-1 text-fg-muted hover:text-foreground disabled:opacity-30"
                          >
                            <ArrowDown className="size-3.5" />
                          </button>
                          <button
                            type="button"
                            onClick={() => removePhoto(photo.key)}
                            aria-label="Remove photo"
                            className="ml-auto p-1 text-fg-muted hover:text-red-400"
                          >
                            <Trash2 className="size-3.5" />
                          </button>
                        </div>
                      </div>
                    </li>
                  ))}
                </ul>
              )}
              {draft.photos.length > 0 && (
                <p className="text-nano text-fg-subtle">
                  The first photo is the cover on the timeline. Removing a photo here takes it out of
                  the gallery too; the file itself stays on Cloudinary.
                </p>
              )}
            </fieldset>

            {formError && (
              <p role="alert" className="rounded-control border border-red-500/30 bg-red-500/10 p-2.5 text-xs text-red-400">
                {formError}
              </p>
            )}

            <div className="flex justify-end gap-2 border-t border-border pt-4">
              <button
                type="button"
                onClick={close}
                disabled={saving}
                className="rounded-control border border-border px-4 py-2 text-sm text-fg-muted hover:text-foreground disabled:opacity-50"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={saving}
                className="inline-flex items-center gap-2 rounded-control bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground hover:opacity-90 disabled:opacity-50"
              >
                {saving && <Loader2 className="size-4 animate-spin" />}
                {editing === "new" ? "Add entry" : "Save changes"}
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
}
