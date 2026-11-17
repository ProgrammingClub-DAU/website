import type { NextConfig } from "next";

/**
 * Sent with every page and asset. These are the safe, no-breakage ones.
 *
 * Deliberately left out:
 * - Content-Security-Policy: Google Sign-In, Cloudinary uploads and the inline
 *   styles of the animated backgrounds all need allowances; a CSP that has not
 *   been tested against each of them breaks sign-in first.
 * - Cross-Origin-Opener-Policy: `same-origin` cuts the window link the Google
 *   sign-in popup reports back through.
 * Strict-Transport-Security is already sent by Vercel on every HTTPS response.
 */
const securityHeaders = [
  // Do not guess a content type: a file served as text is never run as a script.
  { key: "X-Content-Type-Options", value: "nosniff" },
  // Other sites get the origin only, never the path (member handles, contest ids).
  { key: "Referrer-Policy", value: "strict-origin-when-cross-origin" },
  // Nobody may show the site in a frame, which is what clickjacking needs.
  { key: "X-Frame-Options", value: "DENY" },
  // The site uses none of these; a script that somehow got in cannot either.
  // Not camera: the Cloudinary upload widget (lib/cloudinary.ts) offers it as a
  // photo source, from inside its own frame.
  { key: "Permissions-Policy", value: "microphone=(), geolocation=(), payment=(), usb=()" },
];

const nextConfig: NextConfig = {
  poweredByHeader: false,
  async headers() {
    return [{ source: "/:path*", headers: securityHeaders }];
  },
  images: {
    // Images are resized by the hosts they live on, not by Vercel: see
    // src/lib/image-loader.ts. Vercel's optimizer counts every image x width x
    // format against a 5,000-a-month Hobby limit, and going over it paused the
    // whole project.
    loader: "custom",
    loaderFile: "./src/lib/image-loader.ts",
    // Fewer widths in each srcset, so each image has fewer variants to produce
    // and cache. Nothing on the site is wider than 1920px, and avatars are small.
    deviceSizes: [640, 828, 1200, 1920],
    imageSizes: [32, 48, 64, 96, 128, 256],
    // Every host an <Image> points at. With a custom loader these are no longer
    // fetched through Vercel, but the list still documents where images come from.
    remotePatterns: [
      // Member avatars, event covers and gallery photos, uploaded from the browser.
      { protocol: "https", hostname: "res.cloudinary.com" },
      // Avatars taken from Google at first sign-in.
      { protocol: "https", hostname: "lh3.googleusercontent.com" },
      // Codeforces profile pictures, read through the user.info proxy.
      { protocol: "https", hostname: "userpic.codeforces.org" },
      { protocol: "https", hostname: "st.codeforces.com" },
    ],
  },
};

export default nextConfig;
