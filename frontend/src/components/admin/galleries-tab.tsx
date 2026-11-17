"use client";

// The Galleries tab of the admin dashboard, loaded on demand by admin/page.tsx.
import React, { useState, useEffect, useCallback } from "react";
import Image from "next/image";
import { eventsService } from "@/lib/services/events";
import { galleryService, type MemberGalleryPhotoRequest } from "@/lib/services/gallery";
import { openUploadWidget } from "@/lib/cloudinary";
import { type Event, type EventPhoto, type MemberGalleryPhoto } from "@/types/api";
import { Trash2, XCircle, Loader2, Upload } from "lucide-react";
import { serverDate } from "@/lib/utils";

// ══════════════════════════════════════════════════════════════════
// TAB 3: GALLERIES MANAGEMENT
// ══════════════════════════════════════════════════════════════════

export function GalleriesTab() {
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
    loadYears();
  }, [loadYears]);

  useEffect(() => {
    if (selectedYear) {
      loadPhotos(selectedYear);
    }
  }, [selectedYear, loadPhotos]);

  const handleOpenCloudinary = () => {
    const opened = openUploadWidget({
      folder: `cpclub/batch_${uploadBatchYear}`,
      onUpload: (url) => setUploadImageUrl(url),
    });
    if (!opened) {
      const url = window.prompt("Photo upload is unavailable. Paste an image URL instead:");
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
                    {serverDate(photo.uploadedAt).toLocaleDateString()}
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
      loadPhotos(selectedEventId);
    }
  }, [selectedEventId, loadPhotos]);

  /**
   * Uploads several photos in one go.
   *
   * Each file is attached to the event the moment it finishes uploading, rather
   * than collected and saved at the end. A long batch on a slow connection then
   * keeps whatever succeeded if the tab is closed half way, and one failed file
   * does not take the rest with it.
   *
   * The caption box, if filled, applies to every photo in the batch.
   */
  const handleUploadMany = () => {
    if (!selectedEventId) return;
    const eventId = selectedEventId;
    const caption = uploadCaption.trim() || null;
    let added = 0;
    let failed = 0;

    setStatusMessage(null);
    const opened = openUploadWidget({
      folder: `cpclub/events/${eventId}`,
      multiple: true,
      maxFiles: 20,
      onUpload: (url) => {
        setIsUploading(true);
        eventsService
          .addEventPhoto(eventId, { imageUrl: url, caption })
          .then(() => {
            added += 1;
          })
          .catch((err) => {
            failed += 1;
            console.error("Failed to attach an event photo:", err);
          })
          .finally(() => {
            setIsUploading(false);
            void loadPhotos(eventId);
            setStatusMessage(
              failed === 0
                ? { type: "success", text: `${added} photo${added === 1 ? "" : "s"} added to the event.` }
                : { type: "error", text: `${added} added, ${failed} could not be saved. Try those again.` }
            );
          });
      },
      onClose: () => setUploadCaption(""),
    });

    if (!opened) {
      setStatusMessage({
        type: "error",
        text: "The uploader has not loaded yet. Reload the page, or paste an image URL below.",
      });
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
            Add Event Photos
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
                Caption <span className="text-fg-subtle">(optional, applies to every photo you pick)</span>
              </label>
              <input
                type="text"
                maxLength={255}
                placeholder="e.g. Problem discussion session"
                value={uploadCaption}
                onChange={(e) => setUploadCaption(e.target.value)}
                className="w-full rounded-control border border-border bg-surface-2 px-3 py-2 text-xs text-foreground focus:border-primary focus:outline-none"
              />
            </div>

            <button
              type="button"
              onClick={handleUploadMany}
              disabled={!selectedEventId}
              className="flex w-full flex-col items-center gap-1 rounded-panel border border-dashed border-border py-6 text-center transition-colors hover:border-primary disabled:opacity-50"
            >
              {isUploading ? (
                <Loader2 className="size-5 animate-spin text-primary" />
              ) : (
                <Upload className="size-5 text-primary" />
              )}
              <span className="text-xs font-semibold text-foreground">Upload photos</span>
              <span className="text-nano text-fg-subtle">Pick up to 20 at once. JPG, PNG or WebP, 5 MB each.</span>
            </button>

            {/* The fallback for a photo that is already online somewhere. */}
            <details>
              <summary className="cursor-pointer text-xs text-fg-muted hover:text-foreground">
                Or add one by URL
              </summary>
              <div className="mt-2 flex gap-2">
                <input
                  type="url"
                  placeholder="https://..."
                  value={uploadImageUrl}
                  onChange={(e) => setUploadImageUrl(e.target.value)}
                  className="min-w-0 flex-1 rounded-control border border-border bg-surface-2 px-3 py-2 text-xs text-foreground focus:border-primary focus:outline-none"
                />
                <button
                  type="submit"
                  disabled={isUploading || !selectedEventId || !uploadImageUrl}
                  className="shrink-0 rounded-control bg-primary px-3 py-2 text-xs font-semibold text-primary-foreground disabled:opacity-50"
                >
                  Add
                </button>
              </div>
            </details>
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
                    {serverDate(photo.uploadedAt).toLocaleDateString()}
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
