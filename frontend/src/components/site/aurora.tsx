"use client";

// Vendored from React Bits (Aurora-TS-TW). Local changes marked `// local:`.
//
// local: upstream opens a WebGL context and a render loop that never stops —
// no reduced-motion check, no off-screen pause. Both are added below; the
// identity spec requires them, and they matter more here than on the Canvas 2D
// backdrops because this holds a GPU context open.
//
// colorStops must be real colours: ogl's Color parses them, and a
// `var(--token)` would reach it as an unparseable string. The caller resolves
// the tokens.
import { useEffect, useRef } from 'react';
import { Renderer, Program, Mesh, Color, Triangle } from 'ogl';

const VERT = `#version 300 es
in vec2 position;
void main() {
  gl_Position = vec4(position, 0.0, 1.0);
}
`;

const FRAG = `#version 300 es
precision highp float;

uniform float uTime;
uniform float uAmplitude;
uniform vec3 uColorStops[3];
uniform vec2 uResolution;
uniform float uBlend;
uniform float uLightMode;

out vec4 fragColor;

vec3 permute(vec3 x) {
  return mod(((x * 34.0) + 1.0) * x, 289.0);
}

float snoise(vec2 v){
  const vec4 C = vec4(
      0.211324865405187, 0.366025403784439,
      -0.577350269189626, 0.024390243902439
  );
  vec2 i  = floor(v + dot(v, C.yy));
  vec2 x0 = v - i + dot(i, C.xx);
  vec2 i1 = (x0.x > x0.y) ? vec2(1.0, 0.0) : vec2(0.0, 1.0);
  vec4 x12 = x0.xyxy + C.xxzz;
  x12.xy -= i1;
  i = mod(i, 289.0);

  vec3 p = permute(
      permute(i.y + vec3(0.0, i1.y, 1.0))
    + i.x + vec3(0.0, i1.x, 1.0)
  );

  vec3 m = max(
      0.5 - vec3(
          dot(x0, x0),
          dot(x12.xy, x12.xy),
          dot(x12.zw, x12.zw)
      ), 
      0.0
  );
  m = m * m;
  m = m * m;

  vec3 x = 2.0 * fract(p * C.www) - 1.0;
  vec3 h = abs(x) - 0.5;
  vec3 ox = floor(x + 0.5);
  vec3 a0 = x - ox;
  m *= 1.79284291400159 - 0.85373472095314 * (a0*a0 + h*h);

  vec3 g;
  g.x  = a0.x  * x0.x  + h.x  * x0.y;
  g.yz = a0.yz * x12.xz + h.yz * x12.yw;
  return 130.0 * dot(m, g);
}

struct ColorStop {
  vec3 color;
  float position;
};

#define COLOR_RAMP(colors, factor, finalColor) {              \
  int index = 0;                                            \
  for (int i = 0; i < 2; i++) {                               \
     ColorStop currentColor = colors[i];                    \
     bool isInBetween = currentColor.position <= factor;    \
     index = int(mix(float(index), float(i), float(isInBetween))); \
  }                                                         \
  ColorStop currentColor = colors[index];                   \
  ColorStop nextColor = colors[index + 1];                  \
  float range = nextColor.position - currentColor.position; \
  float lerpFactor = (factor - currentColor.position) / range; \
  finalColor = mix(currentColor.color, nextColor.color, lerpFactor); \
}

void main() {
  vec2 uv = gl_FragCoord.xy / uResolution;
  
  ColorStop colors[3];
  colors[0] = ColorStop(uColorStops[0], 0.0);
  colors[1] = ColorStop(uColorStops[1], 0.5);
  colors[2] = ColorStop(uColorStops[2], 1.0);
  
  vec3 rampColor;
  COLOR_RAMP(colors, uv.x, rampColor);
  
  float height = snoise(vec2(uv.x * 2.0 + uTime * 0.1, uTime * 0.25)) * 0.5 * uAmplitude;
  height = exp(height);
  height = (uv.y * 2.0 - height + 0.2);
  float intensity = 0.6 * height;
  
  float midPoint = 0.20;
  float auroraAlpha = smoothstep(midPoint - uBlend * 0.5, midPoint + uBlend * 0.5, intensity);
  
  vec3 auroraColor = intensity * rampColor;
  
  if (uLightMode > 0.5) {
    float energy = clamp(max(intensity, 0.0), 0.0, 1.0);
    float coverage = clamp(auroraAlpha * (0.55 + 0.45 * energy), 0.0, 0.86);
    vec3 chroma = pow(clamp(rampColor, 0.0, 1.0), vec3(1.2));
    float chromaPeak = max(chroma.r, max(chroma.g, chroma.b));
    chroma /= max(chromaPeak, 0.0001);
    fragColor = vec4(mix(vec3(1.0), chroma, min(coverage * 1.08, 0.94)), 1.0);
  } else {
    fragColor = vec4(auroraColor * auroraAlpha, auroraAlpha);
  }
}
`;

interface AuroraProps {
  colorStops?: string[];
  amplitude?: number;
  blend?: number;
  time?: number;
  speed?: number;
  lightMode?: boolean;
}

export default function Aurora(props: AuroraProps) {
  const { colorStops = ['#5227FF', '#7cff67', '#5227FF'], amplitude = 1.0, blend = 0.5, lightMode = false } = props;
  // local: upstream assigned this during render, which React's lint rules
  // reject — a ref written mid-render can leave the loop reading a value the
  // committed tree never had. The render loop reads it a frame later either
  // way, so syncing after commit changes nothing but the correctness.
  const propsRef = useRef<AuroraProps>(props);
  useEffect(() => {
    propsRef.current = props;
  });

  const ctnDom = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const ctn = ctnDom.current;
    if (!ctn) return;

    // local: upstream assumes WebGL is available. Where it is not — an old
    // browser, a blocked or exhausted context, some remote sessions — the
    // constructor throws, and an unhandled throw in a client component takes
    // the whole hero to an error boundary. The page reads perfectly well
    // without a background, so failing quietly is the right outcome.
    let renderer: Renderer;
    try {
      renderer = new Renderer({
        alpha: true,
        premultipliedAlpha: true,
        antialias: true
      });
    } catch {
      return;
    }
    const gl = renderer.gl;
    gl.clearColor(0, 0, 0, 0);
    gl.enable(gl.BLEND);
    gl.blendFunc(gl.ONE, gl.ONE_MINUS_SRC_ALPHA);
    gl.canvas.style.backgroundColor = 'transparent';

    const geometry = new Triangle(gl);
    if (geometry.attributes.uv) {
      delete geometry.attributes.uv;
    }

    const colorStopsArray = colorStops.map(hex => {
      const c = new Color(hex);
      return [c.r, c.g, c.b];
    });

    const program = new Program(gl, {
      vertex: VERT,
      fragment: FRAG,
      uniforms: {
        uTime: { value: 0 },
        uAmplitude: { value: amplitude },
        uColorStops: { value: colorStopsArray },
        uResolution: { value: [ctn.offsetWidth, ctn.offsetHeight] },
        uBlend: { value: blend },
        uLightMode: { value: lightMode ? 1 : 0 }
      }
    });

    const mesh = new Mesh(gl, { geometry, program });
    ctn.appendChild(gl.canvas);

    // local: moved below the program it reads. Upstream declared this above and
    // registered the listener before the program existed, which only held
    // because a resize cannot fire synchronously in between.
    function resize() {
      const width = ctn!.offsetWidth;
      const height = ctn!.offsetHeight;
      renderer.setSize(width, height);
      program.uniforms.uResolution.value = [width, height];
    }
    window.addEventListener('resize', resize);

    // local: the two guards. prefers-reduced-motion in globals.css clamps CSS
    // animation durations and does nothing at all to requestAnimationFrame, and
    // without an observer this keeps the GPU busy while the reader is at the
    // foot of the page.
    const motionQuery = window.matchMedia('(prefers-reduced-motion: reduce)');
    let onScreen = true;

    let animateId: number | null = null;

    // local: split out of `update`. This draws exactly one frame and schedules
    // nothing, so reduced motion can keep the wash on screen instead of leaving
    // an empty rectangle where the hero's background should be, and the
    // visibility guard owns all the scheduling.
    const drawFrame = (t: number) => {
      const { time = t * 0.01, speed = 1.0 } = propsRef.current;
      program.uniforms.uTime.value = time * speed * 0.1;
      program.uniforms.uAmplitude.value = propsRef.current.amplitude ?? 1.0;
      program.uniforms.uBlend.value = propsRef.current.blend ?? blend;
      program.uniforms.uLightMode.value = (propsRef.current.lightMode ?? lightMode) ? 1 : 0;
      const stops = propsRef.current.colorStops ?? colorStops;
      program.uniforms.uColorStops.value = stops.map((hex: string) => {
        const c = new Color(hex);
        return [c.r, c.g, c.b];
      });
      renderer.render({ scene: mesh });
    };

    const update = (t: number) => {
      drawFrame(t);
      animateId = requestAnimationFrame(update);
    };

    const start = () => {
      if (animateId !== null) return;
      if (motionQuery.matches || !onScreen) return;
      animateId = requestAnimationFrame(update);
    };

    const stop = () => {
      if (animateId === null) return;
      cancelAnimationFrame(animateId);
      animateId = null;
    };

    const onMotionChange = () => {
      if (motionQuery.matches) {
        stop();
        drawFrame(performance.now());
      } else {
        start();
      }
    };

    const observer = new IntersectionObserver(
      entries => {
        onScreen = entries[0].isIntersecting;
        if (onScreen) start();
        else stop();
      },
      { threshold: 0 }
    );
    observer.observe(ctn);

    motionQuery.addEventListener('change', onMotionChange);

    resize();
    if (motionQuery.matches) drawFrame(performance.now());
    else start();

    return () => {
      stop();
      observer.disconnect();
      motionQuery.removeEventListener('change', onMotionChange);
      window.removeEventListener('resize', resize);
      if (ctn && gl.canvas.parentNode === ctn) {
        ctn.removeChild(gl.canvas);
      }
      gl.getExtension('WEBGL_lose_context')?.loseContext();
    };
    // Only `amplitude` re-runs this: it is the one prop baked into the program
    // at construction. Everything else is read from propsRef inside the loop,
    // so it takes effect on the next frame without tearing down the WebGL
    // context.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [amplitude]);

  return <div ref={ctnDom} className="w-full h-full" />;
}
