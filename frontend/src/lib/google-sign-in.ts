/**
 * Loading Google's Identity Services script, once.
 *
 * Google's sign-in button is not a React component; it is drawn by a script that
 * attaches itself to `window.google` and renders into a DOM node we hand it. That
 * script must be on the page exactly once -- calling `initialize` twice is
 * harmless, but two copies of the script race each other -- so the loader below
 * caches its own promise and every caller awaits the same one.
 *
 * Only the small part of the API this site uses is typed. The full surface covers
 * One Tap, revocation and the token flow, none of which are wired up here.
 */

/** What Google hands back when someone finishes signing in. */
export interface GoogleCredentialResponse {
  /** The signed ID token. It is sent to our backend and verified there. */
  credential: string;
}

interface GoogleAccountsId {
  initialize(config: {
    client_id: string;
    callback: (response: GoogleCredentialResponse) => void;
    /** Skips Google's own "select an account" prompt on a second visit. */
    auto_select?: boolean;
    cancel_on_tap_outside?: boolean;
  }): void;

  renderButton(
    parent: HTMLElement,
    options: {
      type?: "standard" | "icon";
      theme?: "outline" | "filled_blue" | "filled_black";
      size?: "small" | "medium" | "large";
      text?: "signin_with" | "signup_with" | "continue_with" | "signin";
      shape?: "rectangular" | "pill" | "circle" | "square";
      logo_alignment?: "left" | "center";
      /** Pixels. Google rejects anything above 400. */
      width?: number;
    },
  ): void;

  disableAutoSelect(): void;
}

declare global {
  interface Window {
    google?: { accounts: { id: GoogleAccountsId } };
  }
}

const SCRIPT_SRC = "https://accounts.google.com/gsi/client";

/** Shared across every caller, so the script is requested once per page load. */
let pending: Promise<GoogleAccountsId> | null = null;

/**
 * Loads Google's script and resolves with its sign-in API.
 *
 * Rejects if the script cannot be fetched -- which in practice means the network
 * is down, or an extension or network policy is blocking Google. That is worth
 * surfacing rather than leaving an empty space where the button should be, since
 * it is now the only way into the site.
 *
 * @returns the `google.accounts.id` API, once it is ready to use
 */
export function loadGoogleIdentityServices(): Promise<GoogleAccountsId> {
  if (typeof window === "undefined") {
    return Promise.reject(new Error("Google sign-in is only available in the browser."));
  }

  if (window.google?.accounts?.id) {
    return Promise.resolve(window.google.accounts.id);
  }

  if (pending) return pending;

  pending = new Promise<GoogleAccountsId>((resolve, reject) => {
    const fail = () => {
      // Cleared so a later attempt -- a retry, or simply another visit to the
      // page -- can try again instead of replaying this failure forever.
      pending = null;
      reject(new Error("Could not load Google sign-in."));
    };

    const existing = document.querySelector<HTMLScriptElement>(`script[src="${SCRIPT_SRC}"]`);
    const script = existing ?? document.createElement("script");

    const onLoad = () => {
      if (window.google?.accounts?.id) {
        resolve(window.google.accounts.id);
      } else {
        fail();
      }
    };

    script.addEventListener("load", onLoad, { once: true });
    script.addEventListener("error", fail, { once: true });

    if (!existing) {
      script.src = SCRIPT_SRC;
      script.async = true;
      script.defer = true;
      document.head.appendChild(script);
    }
  });

  return pending;
}

/**
 * The OAuth client ID, read from the environment at build time.
 *
 * Public by design: a client ID identifies the application to Google and is
 * visible in every sign-in request. What keeps it from being useful to anyone
 * else is the list of authorised origins on Google's side, plus the backend
 * refusing any token whose audience is not this ID.
 */
export const GOOGLE_CLIENT_ID = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID ?? "";
