import { useEffect, useMemo, useRef, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { defaultDashboardPath, readStoredUser, ROUTES } from '../utils/authRoutes';
import { takePriorLocation } from '../utils/rateLimitJail';

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

  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-4 bg-surface p-8 text-center text-foreground">
      <h1 ref={headingRef} tabIndex={-1} className="text-2xl font-semibold outline-none">
        Too many requests
      </h1>
      <p className="max-w-md text-foreground-secondary" aria-live="polite">
        You sent requests too quickly. Slow down and wait a moment — we will take you back
        automatically in {remaining}s.
      </p>
    </div>
  );
}
