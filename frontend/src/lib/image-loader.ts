/**
 * Image loader for every `next/image` on the site.
 *
 * Vercel's built-in optimizer is not used. On the Hobby plan it allows 5,000
 * image transformations a month, counting every distinct image x width x format,
 * and the site renders an avatar or photo in nearly every list: the club went past
 * the limit and Vercel paused the whole project. Every image here is already on a
 * host that resizes on request, so the resizing is left to it:
 *
 * - Cloudinary (avatars, event covers, gallery photos): a `c_limit,w_<width>`
 *   transformation with automatic format and quality. Cloudinary caches each
 *   derived size, so a size is produced once, not once per view.
 * - Google profile photos: Google's own `=s<size>-c` suffix.
 * - Anything else (the site's own files, Codeforces userpics) is served as is.
 */

const CLOUDINARY_UPLOAD = /^(https:\/\/res\.cloudinary\.com\/[^/]+\/image\/upload\/)(.+)$/;
const GOOGLE_PHOTO = /^https:\/\/lh\d+\.googleusercontent\.com\//;

/**
 * A Cloudinary URL scaled down to at most `width` pixels, in the best format the
 * browser accepts. Any other URL is returned unchanged. Never scales up.
 */
export function cloudinaryUrl(src: string, width: number): string {
  const match = src.match(CLOUDINARY_UPLOAD);
  if (!match || !hasNoTransformation(match[2])) return src;
  return `${match[1]}c_limit,w_${Math.round(width)},q_auto,f_auto/${match[2]}`;
}

/**
 * Whether the part after "/upload/" starts with the version ("v1712345678/...") or
 * a plain folder -- as every upload-widget URL does -- rather than a transformation
 * such as "c_fill,w_400/". Prefixing a resize to an existing chain would run it
 * first and let the existing step scale the image back up, so such URLs are left alone.
 */
function hasNoTransformation(afterUpload: string): boolean {
  const first = afterUpload.split("/")[0];
  return /^v\d+$/.test(first) || !/[_,]/.test(first);
}

export default function imageLoader({ src, width }: { src: string; width: number; quality?: number }): string {
  const resized = cloudinaryUrl(src, width);
  if (resized !== src) {
    return resized;
  }
  if (GOOGLE_PHOTO.test(src)) {
    // "...=s96-c" -> "...=s<width>-c". Google serves the size asked for.
    return `${src.replace(/=[^/=]*$/, "")}=s${Math.round(width)}-c`;
  }
  // Served unchanged. The width parameter is ignored by these hosts; it is only
  // there so the srcset entries differ, which Next.js checks for.
  return `${src}${src.includes("?") ? "&" : "?"}w=${Math.round(width)}`;
}
