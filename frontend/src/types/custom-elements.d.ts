import type { DetailedHTMLProps, HTMLAttributes } from "react";

/**
 * Custom elements rendered from JSX.
 *
 * React passes unknown lowercase-hyphenated tags straight through to the DOM,
 * but TypeScript will not accept one until it is declared here.
 */
declare module "react" {
  namespace JSX {
    interface IntrinsicElements {
      /**
       * The opening identity, defined by `/club-intro.js`.
       *
       * Attributes are strings rather than booleans on purpose: the component
       * reads them with `hasAttribute`, and React omits an attribute entirely
       * when its value is `false`, which would silently disable `overlay` and
       * `once`. `overlay=""` is present and empty, which is what it checks for.
       */
      "programming-club-intro": DetailedHTMLProps<
        HTMLAttributes<HTMLElement>,
        HTMLElement
      > & {
        /** Fill the window and dismiss after completion. */
        overlay?: string;
        /** Remember completion for this tab session. */
        once?: string;
        /** Id of the content to make inert while the overlay is up. */
        for?: string;
        /** Override the sessionStorage key. */
        "storage-key"?: string;
        /** Hold the finished identity and offer a replay button. */
        preview?: string;
        /** Play the logo alone, without the wordmark, and finish sooner. */
        "mark-only"?: string;
      };
    }
  }
}
