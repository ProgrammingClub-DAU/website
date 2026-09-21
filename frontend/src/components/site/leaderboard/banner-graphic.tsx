"use client";

import React from "react";
import type { BannerConfig } from "@/lib/banner-config";

interface BannerGraphicProps {
  banner: BannerConfig;
  className?: string;
  showEffects?: boolean;
  withScrim?: boolean;
}

export const BannerGraphic: React.FC<BannerGraphicProps> = ({
  banner,
  className = "",
  showEffects = true,
  withScrim = true,
}) => {
  const { colors, animation, id } = banner;

  return (
    <div
      className={`absolute inset-0 size-full overflow-hidden pointer-events-none select-none ${className}`}
      style={{
        background: colors.gradient,
      }}
      aria-hidden="true"
    >
      <svg
        viewBox="0 0 480 120"
        preserveAspectRatio="none"
        className="absolute inset-0 size-full"
        xmlns="http://www.w3.org/2000/svg"
      >
        <defs>
          {/* Radial glow filter */}
          <filter id={`glow-${id}`} x="-20%" y="-20%" width="140%" height="140%">
            <feGaussianBlur stdDeviation="12" result="blur" />
            <feComposite in="SourceGraphic" in2="blur" operator="over" />
          </filter>

          {/* Linear gradient for shimmer sweep */}
          <linearGradient id={`shimmer-grad-${id}`} x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="white" stopOpacity="0" />
            <stop offset="50%" stopColor="white" stopOpacity="0.18" />
            <stop offset="100%" stopColor="white" stopOpacity="0" />
          </linearGradient>

          {/* Accent radial glow */}
          <radialGradient id={`accent-radial-${id}`} cx="85%" cy="30%" r="65%">
            <stop offset="0%" stopColor={colors.accent} stopOpacity="0.5" />
            <stop offset="60%" stopColor={colors.accent} stopOpacity="0.12" />
            <stop offset="100%" stopColor={colors.accent} stopOpacity="0" />
          </radialGradient>
        </defs>

        {/* Ambient background glow circle */}
        <circle cx="400" cy="20" r="140" fill={`url(#accent-radial-${id})`} />

        {/* Base geometric polygons inspired by the reference design */}
        <polygon
          points="0,120 180,120 280,0 100,0"
          fill={colors.accent}
          opacity="0.15"
        />
        <polygon
          points="240,120 380,120 440,0 300,0"
          fill={colors.accent}
          opacity="0.08"
        />

        {/* Animation & Theme-specific artwork */}
        {id === "creator-vip" && (
          <g>
            {/* Pulsing crimson flame pillars & embers */}
            <g fill={colors.accent} opacity="0.38">
              <path d="M 320 120 Q 335 50, 350 120 Z" />
              <path d="M 355 120 Q 375 35, 395 120 Z" />
              <path d="M 400 120 Q 425 20, 445 120 Z" />
              <path d="M 450 120 Q 465 55, 480 120 Z" />
            </g>
            {/* VIP Crown watermark rays */}
            <g stroke={colors.accent} strokeWidth="2" fill="none" opacity="0.4">
              <polygon points="410,35 425,15 440,35 455,15 470,35 440,48" fill={colors.accent} fillOpacity="0.25" />
            </g>
            {/* Ruby stardust particles */}
            <circle cx="340" cy="40" r="3" fill="#ff4d6d" opacity="0.8" className="animate-ping" style={{ animationDuration: "3s" }} />
            <circle cx="390" cy="25" r="2.5" fill="#ff758f" opacity="0.9" className="animate-pulse" />
            <circle cx="450" cy="65" r="3.5" fill="#ff4d6d" opacity="0.75" className="animate-pulse" />
          </g>
        )}

        {animation === "sunburst" && (
          <g opacity="0.28">
            {/* Sunburst rays emanating from top right */}
            {Array.from({ length: 8 }).map((_, i) => {
              const angle = (i * 22) - 30;
              return (
                <line
                  key={i}
                  x1="420"
                  y1="10"
                  x2={420 + Math.cos((angle * Math.PI) / 180) * 350}
                  y2={10 + Math.sin((angle * Math.PI) / 180) * 350}
                  stroke={colors.accent}
                  strokeWidth="6"
                  strokeDasharray="18 12"
                  opacity="0.6"
                />
              );
            })}
          </g>
        )}

        {animation === "circuits" && (
          <g stroke={colors.accent} strokeWidth="1.5" fill="none" opacity="0.32">
            <path d="M 280 110 L 330 110 L 360 80 L 440 80" />
            <circle cx="440" cy="80" r="3" fill={colors.accent} />
            <path d="M 220 20 L 260 20 L 290 50 L 380 50" />
            <circle cx="380" cy="50" r="2.5" fill={colors.accent} />
            <path d="M 310 120 L 340 90 L 400 90" />
          </g>
        )}

        {animation === "waves" && (
          <g fill="none" stroke={colors.accent} opacity="0.22">
            <path
              d="M 120 120 Q 240 40, 360 90 T 480 30"
              strokeWidth="2.5"
            />
            <path
              d="M 80 120 Q 200 60, 320 100 T 480 50"
              strokeWidth="1.5"
            />
          </g>
        )}

        {animation === "flames" && id !== "creator-vip" && (
          <g fill={colors.accent} opacity="0.25">
            <path d="M 360 120 Q 375 70, 390 120 Z" />
            <path d="M 400 120 Q 420 50, 435 120 Z" />
            <path d="M 440 120 Q 455 75, 470 120 Z" />
          </g>
        )}

        {/* Twinkling star sparkle polygons for ranks, creator-vip & high tiers */}
        {(showEffects || banner.isRankBanner || banner.id === "creator-vip" || banner.minRating >= 1600) && (
          <g fill={colors.accent}>
            {/* Sparkle 1 */}
            <polygon
              points="140,25 144,33 152,35 144,37 140,45 136,37 128,35 136,33"
              opacity="0.8"
              className="animate-pulse"
              style={{ animationDuration: "2.8s" }}
            />
            {/* Sparkle 2 */}
            <polygon
              points="380,20 383,27 390,29 383,31 380,38 377,31 370,29 377,27"
              opacity="0.75"
              className="animate-pulse"
              style={{ animationDuration: "3.4s" }}
            />
            {/* Sparkle 3 (small) */}
            <polygon
              points="440,90 442,94 447,95 442,96 440,100 438,96 433,95 438,94"
              opacity="0.6"
            />
          </g>
        )}

        {/* Diagonal Light Shimmer Sweep Polygon */}
        {showEffects && (
          <polygon
            points="-80,120 -30,120 90,0 40,0"
            fill={`url(#shimmer-grad-${id})`}
            className="animate-shimmer-sweep"
          />
        )}
      </svg>

      {/* Scrim overlay for WCAG AA readability */}
      {withScrim && (
        <div
          className="absolute inset-0 size-full pointer-events-none"
          style={{ background: colors.scrim }}
        />
      )}

      {/* Subtle border shine on top edge */}
      <div
        className="absolute inset-x-0 top-0 h-[1px] opacity-40"
        style={{
          background: `linear-gradient(90deg, transparent 0%, ${colors.accent} 50%, transparent 100%)`,
        }}
      />

      <style jsx>{`
        @keyframes shimmer-sweep {
          0% {
            transform: translateX(-100px);
            opacity: 0;
          }
          20% {
            opacity: 1;
          }
          80% {
            opacity: 1;
          }
          100% {
            transform: translateX(580px);
            opacity: 0;
          }
        }
        .animate-shimmer-sweep {
          animation: shimmer-sweep 4.2s cubic-bezier(0.4, 0, 0.2, 1) infinite;
        }
      `}</style>
    </div>
  );
};
