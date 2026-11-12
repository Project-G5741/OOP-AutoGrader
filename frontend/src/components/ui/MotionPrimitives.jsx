import { useEffect, useRef, useState } from 'react';
import { motion, useMotionValue, useReducedMotion, useSpring, useTransform } from 'framer-motion';
import LottieAnimation from './LottieAnimation';
import emptyOrbitData from '../../assets/lottie/empty-orbit.json';
import gradingEngineData from '../../assets/lottie/grading-engine.json';

/**
 * Perspective 3D stage card (single-level crisp surface, never nested inside another card).
 * Applies gentle pointer-tracked tilt on desktop pointers and lifts smoothly on hover.
 */
export function StageCard3D({
  children,
  className = '',
  as: Component = 'div',
  maxTilt = 4,
  hoverLift = -3,
  onClick,
  ...rest
}) {
  const reducedMotion = useReducedMotion();
  const cardRef = useRef(null);
  const x = useMotionValue(0);
  const y = useMotionValue(0);

  const springConfig = { stiffness: 260, damping: 24, mass: 0.7 };
  const rotateX = useSpring(useTransform(y, [-0.5, 0.5], [maxTilt, -maxTilt]), springConfig);
  const rotateY = useSpring(useTransform(x, [-0.5, 0.5], [-maxTilt, maxTilt]), springConfig);

  const handleMouseMove = (event) => {
    if (reducedMotion || !cardRef.current) return;
    if (typeof window !== 'undefined' && window.matchMedia?.('(pointer: coarse)').matches) {
      return;
    }
    const rect = cardRef.current.getBoundingClientRect();
    if (!rect.width || !rect.height) return;
    x.set((event.clientX - rect.left) / rect.width - 0.5);
    y.set((event.clientY - rect.top) / rect.height - 0.5);
  };

  const handleMouseLeave = () => {
    x.set(0);
    y.set(0);
  };

  const MotionTag = motion[Component] || motion.div;

  const wrapHeight = /\bh-full\b/.test(className) ? ' h-full' : '';

  return (
    <div className={`perspective-1000${wrapHeight}`}>
      <MotionTag
        ref={cardRef}
        onClick={onClick}
        onMouseMove={handleMouseMove}
        onMouseLeave={handleMouseLeave}
        style={reducedMotion ? undefined : { rotateX, rotateY, transformStyle: 'preserve-3d' }}
        whileHover={reducedMotion ? undefined : { y: hoverLift }}
        whileTap={onClick && !reducedMotion ? { scale: 0.99 } : undefined}
        transition={{ duration: 0.2, ease: [0.22, 1, 0.36, 1] }}
        className={`rounded-2xl border border-border bg-surface shadow-stage-1 transition-shadow duration-200 hover:shadow-stage-2 ${className}`}
        {...rest}
      >
        {children}
      </MotionTag>
    </div>
  );
}

/** Smooth container entrance for page sections and panels. */
export function AnimatedEntrance({ children, delay = 0, className = '' }) {
  const reducedMotion = useReducedMotion();
  if (reducedMotion) {
    return <div className={className}>{children}</div>;
  }
  return (
    <motion.div
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.26, delay, ease: [0.22, 1, 0.36, 1] }}
      className={className}
    >
      {children}
    </motion.div>
  );
}

/** Stagger grid for KPI cards and report panels. */
export function StaggerGroup({ children, className = '' }) {
  const reducedMotion = useReducedMotion();
  if (reducedMotion) {
    return <div className={className}>{children}</div>;
  }
  return (
    <motion.div
      initial="hidden"
      animate="visible"
      variants={{
        hidden: {},
        visible: { transition: { staggerChildren: 0.055 } },
      }}
      className={className}
    >
      {children}
    </motion.div>
  );
}

export function StaggerItem({ children, className = '' }) {
  const reducedMotion = useReducedMotion();
  if (reducedMotion) {
    return <div className={className}>{children}</div>;
  }
  return (
    <motion.div
      variants={{
        hidden: { opacity: 0, y: 12, scale: 0.98 },
        visible: { opacity: 1, y: 0, scale: 1, transition: { duration: 0.28, ease: [0.22, 1, 0.36, 1] } },
      }}
      className={className}
    >
      {children}
    </motion.div>
  );
}

/** Animated KPI counter when value is a finite number. */
export function AnimatedCounter({ value, formatter }) {
  const reducedMotion = useReducedMotion();
  const numeric = typeof value === 'number' && Number.isFinite(value) ? value : null;
  const [display, setDisplay] = useState(numeric ?? 0);

  useEffect(() => {
    if (numeric === null || reducedMotion) {
      if (numeric !== null) setDisplay(numeric);
      return undefined;
    }
    let rafId;
    const duration = 420;
    const startVal = 0;
    const startTime = performance.now();

    const step = (now) => {
      const progress = Math.min(1, (now - startTime) / duration);
      const eased = 1 - Math.pow(1 - progress, 3);
      setDisplay(Math.round(startVal + (numeric - startVal) * eased));
      if (progress < 1) {
        rafId = requestAnimationFrame(step);
      }
    };
    rafId = requestAnimationFrame(step);
    return () => cancelAnimationFrame(rafId);
  }, [numeric, reducedMotion]);

  if (numeric === null) return value ?? '—';
  return formatter ? formatter(display) : display;
}

/** Unified Corporate EdTech empty state with bundled Lottie animation. */
export function EmptyState({
  title,
  description,
  icon = null,
  action = null,
  compact = false,
  className = '',
}) {
  return (
    <div
      className={`flex flex-col items-center justify-center text-center ${
        compact ? 'py-6 px-4' : 'py-10 px-6'
      } ${className}`}
    >
      <div className={compact ? 'h-20 w-20' : 'h-28 w-28'}>
        <LottieAnimation animationData={emptyOrbitData} className="h-full w-full" />
      </div>
      {icon && (
        <div className="-mt-2 mb-2 inline-flex h-8 w-8 items-center justify-center rounded-lg bg-navy-light text-navy-text dark:bg-surface-secondary">
          {icon}
        </div>
      )}
      {title && <p className="mt-1 text-sm font-semibold text-foreground">{title}</p>}
      {description && (
        <p className="mt-1 max-w-md text-xs text-foreground-secondary sm:text-sm">{description}</p>
      )}
      {action && <div className="mt-4">{action}</div>}
    </div>
  );
}

/** Active grading visual used during student submission upload or lecturer bulk execution. */
export function GradingActivityVisual({
  title = 'Compiling & grading…',
  subtitle = 'Running declaration, MMD model, and isolated testcases',
  className = '',
}) {
  return (
    <div className={`flex flex-col items-center justify-center py-6 text-center ${className}`}>
      <div className="h-24 w-24">
        <LottieAnimation animationData={gradingEngineData} className="h-full w-full" />
      </div>
      <p className="mt-2 text-sm font-semibold text-foreground">{title}</p>
      {subtitle && <p className="mt-1 text-xs text-foreground-secondary">{subtitle}</p>}
    </div>
  );
}
