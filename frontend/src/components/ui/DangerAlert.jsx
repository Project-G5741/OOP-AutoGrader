import { AlertCircle } from 'lucide-react';
import { cn } from './cn';

/**
 * Destructive inline alert (semantic error tokens). Use for high-severity student-facing notices.
 */
export default function DangerAlert({ children, className = '' }) {
  if (children == null || children === '') {
    return null;
  }

  return (
    <div
      role="alert"
      className={cn(
        'flex items-start gap-3 rounded-xl border border-error/40 bg-error-bg px-4 py-3 text-sm text-error-text',
        className,
      )}
    >
      <AlertCircle className="mt-0.5 h-5 w-5 shrink-0 text-error" aria-hidden />
      <p className="min-w-0 font-semibold">{children}</p>
    </div>
  );
}
