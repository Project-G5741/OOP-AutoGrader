import { useEffect, useState } from 'react';

const VARIANTS = {
  success:
    'border-emerald-300 bg-emerald-100 text-emerald-800 dark:border-emerald-600 dark:bg-emerald-900/60 dark:text-emerald-100',
  error:
    'border-red-300 bg-red-100 text-red-800 dark:border-red-600 dark:bg-red-900/60 dark:text-red-100',
};

const EXIT_MS = 300;

export default function Toast({ message, type = 'success', onDismiss, durationMs = 3000 }) {
  const [exiting, setExiting] = useState(false);

  useEffect(() => {
    setExiting(false);
    const exitTimer = setTimeout(() => setExiting(true), durationMs);
    return () => clearTimeout(exitTimer);
  }, [message, durationMs]);

  useEffect(() => {
    if (!exiting) return undefined;
    const dismissTimer = setTimeout(onDismiss, EXIT_MS);
    return () => clearTimeout(dismissTimer);
  }, [exiting, onDismiss]);

  if (!message) return null;

  return (
    <div
      role="status"
      className={`fixed right-10 top-16 z-[100] max-w-sm rounded-lg border px-4 py-3 text-sm shadow-lg ${
        exiting ? 'animate-toast-out' : 'animate-toast-in'
      } ${VARIANTS[type] || VARIANTS.success}`}
    >
      {message}
    </div>
  );
}
