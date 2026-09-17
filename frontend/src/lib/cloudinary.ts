/**
 * Opening the Cloudinary upload widget.
 *
 * The widget was set up by hand in four places, each with its own copy of the
 * cloud name, preset and allowed formats. New code goes through this instead,
 * and this is the only one that supports picking several files at once.
 *
 * The script itself is loaded by the page that needs it
 * (`https://upload-widget.cloudinary.com/global/all.js`, `lazyOnload`).
 */

interface UploadResult {
  event: string;
  info: { secure_url: string; original_filename?: string };
}

interface UploadWidget {
  open: () => void;
  close?: () => void;
}

declare global {
  interface Window {
    cloudinary?: {
      createUploadWidget: (
        options: Record<string, unknown>,
        callback: (error: unknown, result: UploadResult) => void
      ) => UploadWidget;
    };
  }
}

/**
 * Public by design -- the widget posts straight to Cloudinary from the browser,
 * so neither is a secret. The fallbacks match the values the existing upload
 * code has always used, so this changes nothing for a deployment that has not
 * set the variables.
 */
const CLOUD_NAME = process.env.NEXT_PUBLIC_CLOUDINARY_CLOUD_NAME || "stdcydx1";
const UPLOAD_PRESET = process.env.NEXT_PUBLIC_CLOUDINARY_UPLOAD_PRESET || "cpclub_unsigned";

export interface OpenUploadOptions {
  /** Folder under the account, e.g. `cpclub/hall-of-fame`. */
  folder: string;
  /** Allow picking several files. Each one reports separately through onUpload. */
  multiple?: boolean;
  /** Upper bound when multiple. */
  maxFiles?: number;
  /** Called once per finished file, in the order they finish. */
  onUpload: (url: string) => void;
  /** Called once when the widget closes, after every upload has reported. */
  onClose?: () => void;
}

/**
 * Opens the widget.
 *
 * @returns false when the widget script has not loaded -- the caller should offer
 *          a paste-a-URL fallback rather than failing silently
 */
export function openUploadWidget(options: OpenUploadOptions): boolean {
  if (typeof window === "undefined" || !window.cloudinary) return false;

  const multiple = options.multiple ?? false;

  const widget = window.cloudinary.createUploadWidget(
    {
      cloudName: CLOUD_NAME,
      uploadPreset: UPLOAD_PRESET,
      folder: options.folder,
      multiple,
      maxFiles: multiple ? (options.maxFiles ?? 20) : 1,
      clientAllowedFormats: ["jpg", "jpeg", "png", "webp"],
      // Five megabytes matches the preset's own limit, and stops a phone photo
      // at full resolution failing after a long upload instead of before it.
      maxFileSize: 5_000_000,
      sources: ["local", "url", "camera"],
    },
    (error, result) => {
      if (error || !result) return;
      if (result.event === "success") options.onUpload(result.info.secure_url);
      if (result.event === "close") options.onClose?.();
    }
  );

  widget.open();
  return true;
}
