"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import { Check, UserPlus, UserMinus, Loader2, Users, AlertCircle } from "lucide-react";
import { eventsService } from "@/lib/services/events";
import { useAuthStore } from "@/store/auth";
import type { RegistrationStatus } from "@/types/api";

interface RegistrationButtonProps {
  eventId: number;
}

export function RegistrationButton({ eventId }: RegistrationButtonProps) {
  const { isAuthenticated } = useAuthStore();
  const [status, setStatus] = useState<RegistrationStatus | null>(null);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);

  useEffect(() => {
    if (!isAuthenticated) {
      setLoading(false);
      return;
    }

    let ignore = false;
    eventsService
      .getRegistrationStatus(eventId)
      .then((data) => {
        if (!ignore) setStatus(data);
      })
      .catch((err) => {
        console.error("Failed to load registration status:", err);
      })
      .finally(() => {
        if (!ignore) setLoading(false);
      });

    return () => {
      ignore = true;
    };
  }, [eventId, isAuthenticated]);

  const handleRegister = async () => {
    if (actionLoading) return;
    setActionLoading(true);
    try {
      const res = await eventsService.registerForEvent(eventId);
      setStatus(res);
    } catch (err: unknown) {
      console.error("Failed to RSVP:", err);
      alert("Failed to RSVP for event. It may be full or closed.");
    } finally {
      setActionLoading(false);
    }
  };

  const handleUnregister = async () => {
    if (actionLoading) return;
    if (!window.confirm("Cancel your RSVP for this event?")) return;

    setActionLoading(true);
    try {
      await eventsService.unregisterFromEvent(eventId);
      setStatus((prev) =>
        prev
          ? {
              ...prev,
              registered: false,
              count: Math.max(0, prev.count - 1),
            }
          : null
      );
    } catch (err) {
      console.error("Failed to cancel RSVP:", err);
      alert("Failed to cancel RSVP.");
    } finally {
      setActionLoading(false);
    }
  };

  if (!isAuthenticated) {
    return (
      <div className="flex flex-col sm:flex-row sm:items-center gap-3">
        <Link
          href="/login"
          className="inline-flex items-center justify-center gap-2 rounded-xl bg-primary px-5 py-2.5 text-xs font-bold text-primary-foreground hover:bg-primary-hover transition-colors shadow-md shadow-primary/20"
        >
          <UserPlus className="size-4" />
          Sign in to RSVP
        </Link>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="inline-flex items-center gap-2 rounded-xl border border-white/10 bg-white/5 px-4 py-2 text-xs font-mono text-fg-muted">
        <Loader2 className="size-3.5 animate-spin" />
        Checking RSVP status...
      </div>
    );
  }

  if (!status) return null;

  const isFull = status.capacity != null && status.count >= status.capacity;

  return (
    <div className="space-y-2">
      <div className="flex flex-wrap items-center gap-3">
        {status.registered ? (
          <div className="flex items-center gap-2">
            <span className="inline-flex items-center gap-1.5 rounded-xl border border-emerald-500/30 bg-emerald-500/10 px-3.5 py-2 text-xs font-semibold text-emerald-400">
              <Check className="size-4" />
              You&apos;re Attending
            </span>
            <button
              type="button"
              onClick={handleUnregister}
              disabled={actionLoading}
              className="inline-flex items-center gap-1 rounded-xl border border-white/10 bg-surface-2 px-3 py-2 text-xs font-medium text-fg-muted hover:border-rose-500/30 hover:bg-rose-500/10 hover:text-rose-400 transition-all disabled:opacity-50"
            >
              <UserMinus className="size-3.5" />
              Cancel RSVP
            </button>
          </div>
        ) : !status.open ? (
          <span className="inline-flex items-center gap-1.5 rounded-xl border border-white/10 bg-white/5 px-4 py-2 text-xs font-medium text-fg-muted">
            <AlertCircle className="size-3.5" />
            RSVPs Closed
          </span>
        ) : isFull ? (
          <span className="inline-flex items-center gap-1.5 rounded-xl border border-amber-500/30 bg-amber-500/10 px-4 py-2 text-xs font-semibold text-amber-300">
            Event Full
          </span>
        ) : (
          <button
            type="button"
            onClick={handleRegister}
            disabled={actionLoading}
            className="inline-flex items-center justify-center gap-2 rounded-xl bg-primary px-5 py-2.5 text-xs font-bold text-primary-foreground hover:bg-primary-hover transition-colors shadow-md shadow-primary/20 disabled:opacity-50"
          >
            {actionLoading ? (
              <Loader2 className="size-4 animate-spin" />
            ) : (
              <UserPlus className="size-4" />
            )}
            {actionLoading ? "Registering..." : "RSVP for Event"}
          </button>
        )}
      </div>

      {/* Headcount tracker */}
      <div className="text-micro font-mono text-fg-muted flex items-center gap-1.5">
        <Users className="size-3.5" />
        <span>
          {status.count} {status.count === 1 ? "person" : "people"} registered
          {status.capacity != null && ` (limit: ${status.capacity})`}
        </span>
      </div>
    </div>
  );
}
