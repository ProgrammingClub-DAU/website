"use client";

// Vendored from React Bits (StarBorder-TS-TW). Local changes marked `// local:`.
import React from 'react';

type StarBorderProps<T extends React.ElementType> = React.ComponentPropsWithoutRef<T> & {
  as?: T;
  className?: string;
  children?: React.ReactNode;
  color?: string;
  speed?: React.CSSProperties['animationDuration'];
  thickness?: number;
  backgroundColor?: string;
  textColor?: string;
  borderColor?: string;
  /** local: classes for the inner surface, so the caller controls sizing. */
  innerClassName?: string;
};

const StarBorder = <T extends React.ElementType = 'button'>({
  as,
  className = '',
  color = 'var(--cf-candidate)',
  speed = '6s',
  // local: thicker by default. At 1px the sweep is barely visible against the
  // button fill; 2px reads without turning into a frame.
  thickness = 2,
  backgroundColor = 'var(--primary)',
  textColor = 'var(--primary-foreground)',
  borderColor = 'transparent',
  innerClassName = '',
  children,
  ...rest
}: StarBorderProps<T>) => {
  const Component = as || 'button';

  // local: upstream spread `rest` through `as any` twice. Naming the type once
  // keeps the caller's props checked and lets the style merge below see `style`.
  const restProps = rest as React.ComponentPropsWithoutRef<T> & {
    style?: React.CSSProperties;
  };

  return (
    <Component
      className={`relative inline-block overflow-hidden rounded-[20px] ${className}`}
      {...restProps}
      style={{
        padding: `${thickness}px 0`,
        ...restProps.style
      }}
    >
      <div
        className="absolute w-[300%] h-[50%] opacity-70 bottom-[-11px] right-[-250%] rounded-full animate-star-movement-bottom z-0"
        style={{
          background: `radial-gradient(circle, ${color}, transparent 10%)`,
          animationDuration: speed
        }}
      ></div>
      <div
        className="absolute w-[300%] h-[50%] opacity-70 top-[-10px] left-[-250%] rounded-full animate-star-movement-top z-0"
        style={{
          background: `radial-gradient(circle, ${color}, transparent 10%)`,
          animationDuration: speed
        }}
      ></div>
      <div
        className={`relative z-1 border text-center ${innerClassName || "text-[16px] py-[16px] px-[26px] rounded-[20px]"}`}
        style={{ background: backgroundColor, color: textColor, borderColor }}
      >
        {children}
      </div>
    </Component>
  );
};

export default StarBorder;

// tailwind.config.js
// module.exports = {
//   theme: {
//     extend: {
//       animation: {
//         'star-movement-bottom': 'star-movement-bottom linear infinite alternate',
//         'star-movement-top': 'star-movement-top linear infinite alternate',
//       },
//       keyframes: {
//         'star-movement-bottom': {
//           '0%': { transform: 'translate(0%, 0%)', opacity: '1' },
//           '100%': { transform: 'translate(-100%, 0%)', opacity: '0' },
//         },
//         'star-movement-top': {
//           '0%': { transform: 'translate(0%, 0%)', opacity: '1' },
//           '100%': { transform: 'translate(100%, 0%)', opacity: '0' },
//         },
//       },
//     },
//   }
// }
