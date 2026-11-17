import { useEffect, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { X } from 'lucide-react';
import ModalOverlay from './ModalOverlay';
import { resolveDialogConfig } from '../../config/dialogConfig';

/**
 * Universal Popup Dialog Primitive
 * Adheres strictly to Swiss Modernism 2.0, zero-gradient rule, single-level card,
 * and Corporate EdTech 60-30-10 hierarchy.
 *
 * Supports three severity tiers:
 * - 'safety': positive, informative, safe actions (Teal/Green, ShieldCheck)
 * - 'warning': caution, point deductions, non-destructive confirmations (Amber, Warning)
 * - 'dangerous': destructive actions, token invalidation, deletions (Crimson, WarningOctagon)
 */
export default function PopupDialog({
  isOpen = true,
  type = 'alert',
  level = 'safety',
  title,
  message,
  confirmText,
  cancelText,
  onConfirm,
  onCancel,
  onClose,
  showClose = false,
}) {
  const confirmBtnRef = useRef(null);
  const config = resolveDialogConfig(level);
  const isConfirm = type === 'confirm';

  useEffect(() => {
    if (!isOpen) return;

    // Focus confirm button when dialog appears
    const timer = setTimeout(() => {
      confirmBtnRef.current?.focus();
    }, 50);

    const handleKeyDown = (event) => {
      if (event.key === 'Escape') {
        event.stopPropagation();
        if (isConfirm) {
          onCancel?.();
        } else {
          onClose?.();
        }
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => {
      clearTimeout(timer);
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [isOpen, isConfirm, onCancel, onClose]);

  if (!isOpen) return null;

  const handleBackdropClick = () => {
    if (isConfirm) {
      onCancel?.();
    } else {
      onClose?.();
    }
  };

  const IconComponent = config.icon;

  return (
    <ModalOverlay className="!z-[120]" onBackdropClick={handleBackdropClick}>
      <motion.div
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="popup-dialog-title"
        aria-describedby="popup-dialog-desc"
        initial={{ opacity: 0, scale: 0.96, y: 8 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.96, y: 8 }}
        transition={{ duration: 0.18, ease: [0.22, 1, 0.36, 1] }}
        className={`w-full max-w-md rounded-2xl bg-surface border border-border p-6 shadow-stage-3 ${config.borderAccent} relative overflow-hidden`}
        onClick={(event) => event.stopPropagation()}
      >
        {/* Top Header Row with Level Badge and optional Close */}
        <div className="flex items-center justify-between gap-3 mb-4">
          <span
            className={`inline-flex items-center rounded-md border px-2.5 py-0.5 text-[11px] font-bold uppercase tracking-wider ${config.badgeClass}`}
          >
            {config.label}
          </span>
          {showClose ? (
            <button
              type="button"
              onClick={handleBackdropClick}
              className="inline-flex h-7 w-7 items-center justify-center rounded-lg text-foreground-secondary hover:bg-surface-secondary hover:text-foreground transition-colors"
              aria-label="Close dialog"
            >
              <X className="h-4 w-4" />
            </button>
          ) : null}
        </div>

        {/* Content Section: Duotone Icon + Title & Message */}
        <div className="flex items-start gap-4 mb-6">
          <div
            className={`flex h-11 w-11 shrink-0 items-center justify-center rounded-xl border border-border-subtle ${config.iconBg}`}
            aria-hidden="true"
          >
            <IconComponent size={24} weight="duotone" className={config.iconColor} />
          </div>
          <div className="min-w-0 flex-1">
            <h3
              id="popup-dialog-title"
              className="text-base font-bold text-foreground leading-snug tracking-tight"
            >
              {title || config.defaultTitle}
            </h3>
            <div
              id="popup-dialog-desc"
              className="mt-2 text-sm text-foreground-secondary leading-relaxed break-words"
            >
              {message}
            </div>
          </div>
        </div>

        {/* Actions Row */}
        <div className="flex items-center justify-end gap-2.5 pt-2 border-t border-border-subtle">
          {isConfirm && (
            <button
              type="button"
              onClick={() => {
                onCancel?.();
              }}
              className="inline-flex items-center justify-center rounded-xl border border-border bg-surface px-4 py-2 text-sm font-medium text-foreground transition-all duration-150 hover:bg-surface-secondary active:scale-[0.985] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
            >
              {cancelText || config.defaultCancelText}
            </button>
          )}
          <button
            ref={confirmBtnRef}
            type="button"
            onClick={() => {
              onConfirm?.();
            }}
            className={`inline-flex items-center justify-center rounded-xl px-5 py-2 text-sm font-medium transition-all duration-150 active:scale-[0.985] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary ${config.confirmButtonClass}`}
          >
            {confirmText || (isConfirm ? config.defaultConfirmText : 'OK')}
          </button>
        </div>
      </motion.div>
    </ModalOverlay>
  );
}
