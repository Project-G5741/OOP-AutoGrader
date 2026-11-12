import { useEffect, useRef } from 'react';
import { useReducedMotion } from 'framer-motion';
import lottie from 'lottie-web/build/player/lottie_light';

/**
 * CSP-safe offline Lottie player using lottie_light (no eval/expressions).
 * Freezes on a calm representative frame when `prefers-reduced-motion` is active.
 */
export default function LottieAnimation({
  animationData,
  loop = true,
  autoplay = true,
  className = '',
  label = '',
  staticFrame = 30,
}) {
  const containerRef = useRef(null);
  const reducedMotion = useReducedMotion();

  useEffect(() => {
    const container = containerRef.current;
    if (!container || !animationData) return undefined;

    let anim;
    try {
      anim = lottie.loadAnimation({
        container,
        renderer: 'svg',
        loop: reducedMotion ? false : loop,
        autoplay: reducedMotion ? false : autoplay,
        animationData,
        rendererSettings: {
          preserveAspectRatio: 'xMidYMid meet',
        },
      });
      if (reducedMotion) {
        const total = Math.max(1, (anim.totalFrames || 60) - 1);
        anim.goToAndStop(Math.min(staticFrame, total), true);
      }
    } catch {
      // Fall back gracefully if renderer fails in constrained webviews.
    }

    return () => {
      if (anim) {
        anim.destroy();
      }
    };
  }, [animationData, loop, autoplay, reducedMotion, staticFrame]);

  return (
    <div
      ref={containerRef}
      className={className}
      role={label ? 'img' : undefined}
      aria-label={label || undefined}
      aria-hidden={label ? undefined : true}
    />
  );
}
