"use client";

import React from "react";
import { Download, ExternalLink } from "lucide-react";
import { calendarService } from "@/lib/services/calendar";

interface AddToCalendarProps {
  eventId?: number | string;
  title: string;
  description?: string | null;
  location?: string | null;
  startDateIso: string;
  durationMinutes?: number;
}

export function AddToCalendar({
  eventId,
  title,
  description = "",
  location = "",
  startDateIso,
  durationMinutes = 120,
}: AddToCalendarProps) {
  // Parse start and end date for Google Calendar URL (YYYYMMDDTHHmmssZ)
  const startDate = new Date(startDateIso);
  const endDate = new Date(startDate.getTime() + durationMinutes * 60 * 1000);

  const formatGoogleDate = (d: Date) => {
    return d.toISOString().replace(/[-:]/g, "").replace(/\.\d{3}/, "");
  };

  const googleCalendarUrl = `https://calendar.google.com/calendar/render?action=TEMPLATE&text=${encodeURIComponent(
    title
  )}&dates=${formatGoogleDate(startDate)}/${formatGoogleDate(endDate)}&details=${encodeURIComponent(
    description || ""
  )}&location=${encodeURIComponent(location || "")}`;

  const icsDownloadUrl = eventId ? calendarService.getEventIcsUrl(eventId) : null;

  return (
    <div className="flex flex-wrap items-center gap-2">
      {/* Google Calendar Link */}
      <a
        href={googleCalendarUrl}
        target="_blank"
        rel="noreferrer"
        className="inline-flex items-center gap-1.5 rounded-xl border border-white/10 bg-surface-2 px-3 py-1.5 text-xs font-medium text-foreground hover:border-primary/40 hover:bg-surface-3 transition-colors shadow-xs"
      >
        <ExternalLink className="size-3.5 text-primary" />
        Google Calendar
      </a>

      {/* ICS Download */}
      {icsDownloadUrl && (
        <a
          href={icsDownloadUrl}
          download={`${title.toLowerCase().replace(/[^a-z0-9]/g, "-")}.ics`}
          className="inline-flex items-center gap-1.5 rounded-xl border border-white/10 bg-surface-2 px-3 py-1.5 text-xs font-medium text-foreground hover:border-hairline-strong hover:bg-surface-3 transition-colors shadow-xs"
        >
          <Download className="size-3.5 text-fg-muted" />
          Apple / Outlook (.ics)
        </a>
      )}
    </div>
  );
}
