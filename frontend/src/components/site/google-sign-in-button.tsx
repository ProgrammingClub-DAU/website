"use client";

import { useEffect, useRef, useState } from "react";

import {
  GOOGLE_CLIENT_ID,
  loadGoogleIdentityServices,
  type GoogleCredentialResponse,
} from "@/lib/google-sign-in";

interface GoogleSignInButtonProps {
  /** Called with the ID token once somebody completes Google's flow. */
  onCredential: (idToken: string) => void;
  /** Shown while the parent is exchanging a credential it already has. */
  busy?: boolean;
  busyLabel?: string;
}

/**
 * Google's own sign-in button.
 *
 * Google draws this one itself, into a node we hand it. That is not a stylistic
 * choice: a button that posts to Google has to be Google's, both because their
 * terms require it and because people are entitled to recognise the thing they
 * are trusting with their university account.
 *
 * The consequence is that this component owns a piece of DOM React does not
 * manage, so it renders into a ref and clears that node before drawing again.
 */
export function GoogleSignInButton({ onCredential, busy, busyLabel }: GoogleSignInButtonProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const [loadError, setLoadError] = useState<string | null>(null);

  // Held in a ref so a re-render with a new closure does not force the button to
  // be torn down and redrawn, which would flicker.
  const onCredentialRef = useRef(onCredential);
  useEffect(() => {
    onCredentialRef.current = onCredential;
  }, [onCredential]);

  useEffect(() => {
    // Nothing to load without a client ID. The missing-configuration message
    // is rendered below rather than pushed into state: the value is fixed at
    // build time, so it is something this component knows, not something that
    // happens to it.
    if (!GOOGLE_CLIENT_ID) return;

    let cancelled = false;

    loadGoogleIdentityServices()
      .then((googleId) => {
        if (cancelled || !containerRef.current) return;

        googleId.initialize({
          client_id: GOOGLE_CLIENT_ID,
          callback: (response: GoogleCredentialResponse) => {
            onCredentialRef.current(response.credential);
          },
          // Nobody is signed in silently. Landing on the site already
          // authenticated, without having chosen to, is startling.
          auto_select: false,
        });

        // Google appends to this node rather than replacing its contents, so a
        // second effect run in development would otherwise stack two buttons.
        containerRef.current.replaceChildren();

        googleId.renderButton(containerRef.current, {
          type: "standard",
          theme: "filled_black",
          size: "large",
          text: "continue_with",
          shape: "pill",
          logo_alignment: "left",
          // Google wants a pixel width, so the container is measured. Clamped
          // because it rejects anything wider than 400, and anything under
          // about 200 crops the label.
          width: Math.min(400, Math.max(200, Math.round(containerRef.current.clientWidth || 320))),
        });
      })
      .catch(() => {
        if (!cancelled) {
          setLoadError(
            "Could not reach Google to sign you in. Check your connection, or any extension blocking Google, and reload.",
          );
        }
      });

    return () => {
      cancelled = true;
    };
  }, []);

  const message = GOOGLE_CLIENT_ID
    ? loadError
    : "Google sign-in is not configured for this deployment. Please tell the club committee.";

  if (message) {
    return (
      <div
        role="alert"
        className="rounded-lg border border-destructive/20 bg-destructive/10 p-3 text-center font-mono text-xs text-destructive"
      >
        {message}
      </div>
    );
  }

  return (
    <div className="relative flex min-h-[44px] w-full items-center justify-center">
      <div ref={containerRef} className="flex w-full justify-center" aria-hidden={busy} />

      {busy && (
        <div className="absolute inset-0 flex items-center justify-center rounded-pill bg-surface-2/85 font-mono text-xs text-fg-muted">
          {busyLabel ?? "Signing in..."}
        </div>
      )}
    </div>
  );
}
