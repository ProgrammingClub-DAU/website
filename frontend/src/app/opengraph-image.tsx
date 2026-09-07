import { readFile } from "node:fs/promises";
import { join } from "node:path";

import { ImageResponse } from "next/og";

import { site } from "@/lib/site";

/**
 * The share card behind every link to this site.
 *
 * Before this existed the root layout declared `twitter.card =
 * "summary_large_image"` and a full `openGraph` block but supplied no image at
 * all, so every link posted to WhatsApp, LinkedIn, or Discord rendered as a
 * blank grey rectangle. For a club that recruits by sharing its link, that was
 * the most expensive gap on the site.
 *
 * Rendered at build time rather than shipped as a flat PNG so the text stays
 * crisp and tracks `site.ts` — the club name and tagline cannot drift out of
 * sync with the rest of the site the way a hand-exported image would.
 */
export const alt = `${site.fullName} — ${site.tagline}`;
export const size = { width: 1200, height: 630 };
export const contentType = "image/png";

// Pulled from globals.css so the card cannot drift from the site's dark theme.
const BG = "#08090a";
const FG = "#f7f8f8";
const MUTED = "#8a8f98";
const GRADIENT = "linear-gradient(100deg, #00868b, #2a5fe0, #9333c4)";

export default async function Image() {
  // The full badge, not the monogram: at 260px the "PROGRAMMING CLUB" and
  // university text around the rim is perfectly legible, and the white disc
  // reads strongly against the near-black background.
  const logo = await readFile(join(process.cwd(), "public/logo.png"));
  const logoSrc = `data:image/png;base64,${logo.toString("base64")}`;

  return new ImageResponse(
    (
      <div
        style={{
          width: "100%",
          height: "100%",
          display: "flex",
          flexDirection: "column",
          backgroundColor: BG,
        }}
      >
        <div style={{ display: "flex", height: 10, width: "100%", background: GRADIENT }} />
        <div
          style={{
            display: "flex",
            flex: 1,
            alignItems: "center",
            padding: "0 84px",
          }}
        >
          <img src={logoSrc} width={260} height={260} alt="" />
          <div
            style={{
              display: "flex",
              flexDirection: "column",
              marginLeft: 64,
            }}
          >
            <div style={{ display: "flex", fontSize: 76, color: FG, letterSpacing: -2 }}>
              {site.name}
            </div>
            <div style={{ display: "flex", fontSize: 44, color: MUTED, marginTop: 6 }}>
              {site.suffix}
            </div>
            <div
              style={{
                display: "flex",
                width: 132,
                height: 6,
                marginTop: 30,
                background: GRADIENT,
              }}
            />
            <div
              style={{
                display: "flex",
                fontSize: 27,
                color: MUTED,
                marginTop: 30,
                maxWidth: 560,
                lineHeight: 1.4,
              }}
            >
              {site.university}
            </div>
          </div>
        </div>
      </div>
    ),
    size
  );
}
