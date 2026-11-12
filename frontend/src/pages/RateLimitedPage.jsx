import { useEffect, useMemo, useRef, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { ClockCountdown } from '@phosphor-icons/react';
import { defaultDashboardPath, readStoredUser, ROUTES } from '../utils/authRoutes';
import { takePriorLocation } from '../utils/rateLimitJail';
import { StageCard3D } from '../components/ui/MotionPrimitives';

export default function RateLimitedPage() {
  const [searchParams] = useSearchParams();
  const waitSeconds = useMemo(() => {
    const raw = Number.parseInt(searchParams.get('wait') || '5', 10);
    if (!Number.isFinite(raw) || raw < 1) {
      return 5;
    }
    return Math.min(raw, 60);
  }, [searchParams]);
  const [remaining, setRemaining] = useState(waitSeconds);
  const returnToRef = useRef(null);
  const headingRef = useRef(null);

  useEffect(() => {
    const user = readStoredUser();
    const fallback = user
      ? defaultDashboardPath(user.roles, user.inCurrentTerm)
      : ROUTES.login;
    returnToRef.current = takePriorLocation(fallback);
    headingRef.current?.focus();
  }, []);

  useEffect(() => {
    if (remaining <= 0) {
      window.location.replace(returnToRef.current || ROUTES.login);
      return undefined;
    }
    const id = window.setTimeout(() => setRemaining((s) => s - 1), 1000);
    return () => window.clearTimeout(id);
  }, [remaining]);

  const ratio = waitSeconds > 0 ? Math.max(0, remaining) / waitSeconds : 0;
  const circumference = 2 * Math.PI * 34;
  const dashOffset = circumference * (1 - ratio);

  return (
    <div className="flex min-h-screen items-center justify-center bg-background p-6 text-foreground">
      <StageCard3D className="w-full max-w-md p-8 text-center">
        <div className="relative mx-auto mb-5 flex h-24 w-24 items-center justify-center">
          <svg className="h-24 w-24 -rotate-90" viewBox="0 0 80 80" aria-hidden="true">
            <circle
              cx="40"
              cy="40"
              r="34"
              fill="none"
              stroke="var(--surface-tertiary)"
              strokeWidth="5"
            />
            <circle
              cx="40"
              cy="40"
              r="34"
              fill="none"
              stroke="var(--warning)"
              strokeWidth="5"
              strokeLinecap="round"
              strokeDasharray={circumference}
              strokeDashoffset={dashOffset}
              style={{ transition: 'stroke-dashoffset 0.9s linear' }}
            />
          </svg>
          <span className="absolute flex flex-col items-center justify-center">
            <ClockCountdown size={22} weight="duotone" className="text-warning" aria-hidden="true" />
            <span className="mt-0.5 font-mono text-sm font-bold tabular-nums text-foreground">
              {remaining}s
            </span>
          </span>
        </div>

        <h1 ref={headingRef} tabIndex={-1} className="text-2xl font-bold tracking-tight text-foreground outline-none">
          Too many requests
        </h1>
        <p className="mt-2 text-sm leading-relaxed text-foreground-secondary" aria-live="polite">
          You sent requests too quickly. Slow down and wait a moment — we will take you back
          automatically in {remaining}s.
        </p>
      </StageCard3D>
    </div>
  );
}
