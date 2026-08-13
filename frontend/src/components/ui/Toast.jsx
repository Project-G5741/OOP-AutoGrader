import { useEffect, useState } from 'react';

const VARIANTS = {
  success: 'border-success/40 bg-success-bg text-success-text',
  error: 'border-error/40 bg-error-bg text-error-text',
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
