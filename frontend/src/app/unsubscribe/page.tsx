"use client";

import React, { Suspense, useEffect, useState } from "react";
import { useSearchParams } from "next/navigation";
import Link from "next/link";
import { BellOff, CheckCircle2, AlertTriangle, Loader2, ArrowLeft, User } from "lucide-react";
import { notificationsService, type UnsubscribeResult } from "@/lib/services/notifications";

function UnsubscribeContent() {
  const searchParams = useSearchParams();
  const token = searchParams.get("token");

  const [loading, setLoading] = useState(true);
  const [result, setResult] = useState<UnsubscribeResult | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!token) {
      setError("No unsubscribe token provided. Please check the link from your email.");
      setLoading(false);
      return;
    }

    let isMounted = true;
    notificationsService
      .unsubscribe(token)
      .then((data) => {
        if (isMounted) {
          setResult(data);
          setError(null);
        }
      })
      .catch((err: unknown) => {
        if (isMounted) {
          const msg =
            (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
            "This unsubscribe link is invalid, expired, or has already been used.";
          setError(msg);
        }
      })
      .finally(() => {
        if (isMounted) setLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, [token]);

  const getKindLabel = (kind: string) => {
    switch (kind.toUpperCase()) {
      case "EVENTS":
        return "Event Reminders (24-hour RSVP notifications)";
      case "CONTESTS":
        return "Weekly Contest Digests";
      default:
        return kind;
    }
  };

  return (
    <div className="relative mx-auto max-w-lg overflow-hidden rounded-2xl border border-white/10 bg-surface/80 p-8 shadow-2xl backdrop-blur-xl">
      <div className="absolute -right-20 -top-20 size-48 rounded-full bg-primary/10 blur-3xl pointer-events-none" />

      {loading ? (
        <div className="flex flex-col items-center justify-center py-12 text-center">
          <div className="rounded-full border border-primary/20 bg-primary/10 p-4">
            <Loader2 className="size-8 animate-spin text-primary" />
          </div>
          <h2 className="mt-5 font-mono text-sm uppercase tracking-caps text-fg-muted">
            Updating email preferences
          </h2>
          <p className="mt-2 text-xs text-fg-subtle">
            Processing your unsubscribe request...
          </p>
        </div>
      ) : error ? (
        <div className="flex flex-col items-center text-center">
          <div className="rounded-full border border-rose-500/30 bg-rose-500/10 p-4 text-rose-400">
            <AlertTriangle className="size-8" />
          </div>
          <h1 className="mt-5 text-xl font-bold tracking-tight text-foreground">
            Could Not Unsubscribe
          </h1>
          <p className="mt-2 text-sm text-fg-muted max-w-sm">{error}</p>

          <div className="mt-8 flex flex-wrap items-center justify-center gap-3">
            <Link
              href="/"
              className="inline-flex items-center gap-2 rounded-xl border border-white/10 bg-surface-2 px-4 py-2 text-xs font-semibold text-foreground hover:bg-surface-3 transition-colors"
            >
              <ArrowLeft className="size-3.5" /> Back to Home
            </Link>
            <Link
              href="/profile"
              className="inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2 text-xs font-bold text-primary-foreground hover:bg-primary-hover transition-colors shadow-md shadow-primary/20"
            >
              <User className="size-3.5" /> Notification Preferences
            </Link>
          </div>
        </div>
      ) : (
        <div className="flex flex-col items-center text-center">
          <div className="rounded-full border border-emerald-500/30 bg-emerald-500/10 p-4 text-emerald-400">
            <CheckCircle2 className="size-8" />
          </div>
          <h1 className="mt-5 text-xl font-bold tracking-tight text-foreground">
            Unsubscribed Successfully
          </h1>

          <div className="mt-3 rounded-xl border border-white/10 bg-surface-2/60 p-4 w-full text-left">
            <div className="flex items-center gap-2 text-xs font-mono text-primary font-semibold">
              <BellOff className="size-3.5" />
              <span>Turned Off</span>
            </div>
            <p className="mt-1 text-sm font-medium text-foreground">
              {result ? getKindLabel(result.kind) : "Email notifications"}
            </p>
            <p className="mt-2 text-micro text-fg-muted leading-relaxed">
              You will no longer receive these emails at your registered DAU address.
            </p>
          </div>

          <p className="mt-4 text-xs text-fg-subtle max-w-sm">
            Changed your mind? You can re-enable email notifications anytime in your member profile settings.
          </p>

          <div className="mt-6 flex flex-wrap items-center justify-center gap-3">
            <Link
              href="/"
              className="inline-flex items-center gap-2 rounded-xl border border-white/10 bg-surface-2 px-4 py-2 text-xs font-semibold text-foreground hover:bg-surface-3 transition-colors"
            >
              <ArrowLeft className="size-3.5" /> Back to Home
            </Link>
            <Link
              href="/profile"
              className="inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2 text-xs font-bold text-primary-foreground hover:bg-primary-hover transition-colors shadow-md shadow-primary/20"
            >
              <User className="size-3.5" /> Manage in Profile
            </Link>
          </div>
        </div>
      )}
    </div>
  );
}

export default function UnsubscribePage() {
  return (
    <div className="flex min-h-[calc(100vh-14rem)] items-center justify-center px-4 py-16">
      <Suspense
        fallback={
          <div className="flex flex-col items-center justify-center rounded-2xl border border-white/10 bg-surface p-8 text-center">
            <Loader2 className="size-6 animate-spin text-primary" />
            <span className="mt-3 font-mono text-xs text-fg-muted">Loading preferences...</span>
          </div>
        }
      >
        <UnsubscribeContent />
      </Suspense>
    </div>
  );
}
