import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { createPortal } from 'react-dom';

const VARIANTS = {
  success: 'border-success/40 bg-success-bg text-success-text',
  error: 'border-error/40 bg-error-bg text-error-text',
  warning: 'border-warning/40 bg-warning-bg text-warning-text',
};

const EXIT_MS = 300;

const ToastContext = createContext(null);

export function normalizeToast(arg, type = 'success') {
  if (arg == null) return null;
  if (typeof arg === 'object') {
    const message = arg.message;
    if (!message) return null;
    return {
      message,
      type: arg.type || 'success',
      actionLabel: arg.actionLabel || '',
      onAction: typeof arg.onAction === 'function' ? arg.onAction : null,
      durationMs: arg.durationMs,
      persist: Boolean(arg.persist),
    };
  }
  return { message: String(arg), type, actionLabel: '', onAction: null, persist: false };
}

export default function Toast({
  message,
  type = 'success',
  onDismiss,
  durationMs = 3000,
  actionLabel = '',
  onAction,
  persist = false,
}) {
  const [exiting, setExiting] = useState(false);

  useEffect(() => {
    setExiting(false);
    if (persist) return undefined;
    const exitTimer = setTimeout(() => setExiting(true), durationMs || 3000);
    return () => clearTimeout(exitTimer);
  }, [message, durationMs, persist]);

  useEffect(() => {
    if (!exiting) return undefined;
    const dismissTimer = setTimeout(onDismiss, EXIT_MS);
    return () => clearTimeout(dismissTimer);
  }, [exiting, onDismiss]);

  if (!message) return null;

  return (
    <div
      role={type === 'error' || type === 'warning' ? 'alert' : 'status'}
      className={`fixed right-10 top-16 z-[110] max-w-sm rounded-lg border px-4 py-3 text-sm shadow-lg ${
        exiting ? 'animate-toast-out' : 'animate-toast-in'
      } ${VARIANTS[type] || VARIANTS.success}`}
    >
      <div className="flex items-start gap-3">
        <p className="min-w-0 flex-1">{message}</p>
        <div className="flex shrink-0 flex-col items-end gap-1">
          {actionLabel ? (
            <button
              type="button"
              className="font-semibold underline underline-offset-2 hover:opacity-80"
              onClick={(event) => {
                event.stopPropagation();
                onAction?.();
              }}
            >
              {actionLabel}
            </button>
          ) : null}
          {persist ? (
            <button
              type="button"
              className="text-xs opacity-70 hover:opacity-100"
              onClick={(event) => {
                event.stopPropagation();
                onDismiss?.();
              }}
            >
              Dismiss
            </button>
          ) : null}
        </div>
      </div>
    </div>
  );
}

export function ToastProvider({ children }) {
  const [toast, setToast] = useState(null);
  const showToast = useCallback((arg, type) => {
    setToast(normalizeToast(arg, type));
  }, []);
  const dismiss = useCallback(() => setToast(null), []);

  return (
    <ToastContext.Provider value={showToast}>
      {children}
      {toast && typeof document !== 'undefined'
        ? createPortal(
            <Toast
              message={toast.message}
              type={toast.type}
              actionLabel={toast.actionLabel}
              onAction={toast.onAction}
              durationMs={toast.durationMs}
              persist={toast.persist}
              onDismiss={dismiss}
            />,
            document.body,
          )
        : null}
    </ToastContext.Provider>
  );
}

export function useToast() {
  const showToast = useContext(ToastContext);
  if (!showToast) {
    throw new Error('useToast must be used within ToastProvider');
  }
  return showToast;
}
