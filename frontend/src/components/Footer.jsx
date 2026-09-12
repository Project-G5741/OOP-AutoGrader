import { useEffect, useState } from 'react';
import { Users } from 'lucide-react';
import { fetchPresenceCount, leavePresence, PRESENCE_POLL_MS } from '../utils/presence';
import { formatNumber } from '../utils/formatters';

const FOOTER_LABEL = 'Object-Oriented Programming';

function ActiveUsersCount() {
  const [count, setCount] = useState(null);

  useEffect(() => {
    let cancelled = false;

    const load = async () => {
      try {
        const next = await fetchPresenceCount();
        if (!cancelled && typeof next === 'number') {
          setCount((prev) => (prev === next ? prev : next));
        }
      } catch {
        /* keep the last successful count */
      }
    };

    load();
    const id = window.setInterval(load, PRESENCE_POLL_MS);
    const onVisible = () => {
      if (document.visibilityState === 'visible') {
        load();
      }
    };
    const onPageHide = () => leavePresence();
    document.addEventListener('visibilitychange', onVisible);
    window.addEventListener('pagehide', onPageHide);
    return () => {
      cancelled = true;
      window.clearInterval(id);
      document.removeEventListener('visibilitychange', onVisible);
      window.removeEventListener('pagehide', onPageHide);
    };
  }, []);

  if (count === null) {
    return null;
  }

  const display = formatNumber(count);
  const label = `Active Users: ${display}`;
  return (
    <p
      className="flex shrink-0 items-center gap-1.5 text-xs text-foreground-muted sm:absolute sm:left-0 sm:top-1/2 sm:-translate-y-1/2 sm:text-sm"
      role="status"
      aria-atomic="true"
    >
      <Users className="h-4 w-4 shrink-0 text-primary" aria-hidden="true" />
      <span className="sr-only">{label}</span>
      <span aria-hidden="true">Active Users:</span>
      <span
        aria-hidden="true"
        className="inline-flex items-center justify-center rounded-md bg-primary px-2 py-0.5 text-[11px] font-semibold tabular-nums leading-none text-white"
      >
        {display}
      </span>
    </p>
  );
}

export default function Footer({ variant = 'default' }) {
  if (variant === 'login') {
    return (
      <div className="mt-5 text-center text-[0.95rem] text-foreground-muted">
        {FOOTER_LABEL}
      </div>
    );
  }

  if (variant === 'compact') {
    return (
      <div className="px-4 pb-4 text-center sm:px-6">
        <p className="text-xs text-foreground-disabled">{FOOTER_LABEL}</p>
      </div>
    );
  }

  return (
    <footer className="mt-8 shrink-0 overflow-x-clip border-t border-border py-5">
      <div className="relative flex flex-col items-center gap-2 sm:block sm:h-6">
        <ActiveUsersCount />
        <p className="text-center text-sm text-foreground-muted">{FOOTER_LABEL}</p>
      </div>
    </footer>
  );
}
