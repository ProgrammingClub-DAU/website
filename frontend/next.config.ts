import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  images: {
    // next/image refuses any host that is not listed here: in development it throws,
    // and in production /_next/image answers 400 and the picture never renders.
    // Every host below is one the app already points an <Image> at.
    remotePatterns: [
      // Member avatars, event covers and gallery photos, uploaded from the browser.
      { protocol: "https", hostname: "res.cloudinary.com" },
      // Codeforces profile pictures, read through the user.info proxy.
      { protocol: "https", hostname: "userpic.codeforces.org" },
      { protocol: "https", hostname: "st.codeforces.com" },
    ],
  },
};

export default nextConfig;
