import type { NextConfig } from "next";

const nextConfig: NextConfig = {
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
