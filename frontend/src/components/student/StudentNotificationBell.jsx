import React, { useEffect, useMemo, useRef, useState } from 'react';
import { Bell, Trash2 } from 'lucide-react';
import { BellRinging } from '@phosphor-icons/react';
import { AnimatePresence, motion, useReducedMotion } from 'framer-motion';
import {
  buildFeedbackReplyNotifications,
  buildStudentNotifications,
  notificationSeverityClasses,
} from '../../utils/studentNotifications';
import { apiFetch } from '../../utils/apiFetch';
import { authHeaders } from '../../utils/authHeaders';
import { readFriendlyApiError, toFriendlyError } from '../../utils/apiError';
import { useToast } from '../ui/Toast';
const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';
const IS_DESKTOP_APP = import.meta.env.VITE_APP_MODE === 'desktop';
const READS_BASE = `${API_BASE}/api/students/notification-reads`;

export default function StudentNotificationBell({
  labs = [],
  labSummariesById = {},
  now = new Date(),
  className = '',
}) {
  const [open, setOpen] = useState(false);
  const reducedMotion = useReducedMotion();
  const showToast = useToast();
  const [readIds, setReadIds] = useState(() => new Set());
  const [dismissedIds, setDismissedIds] = useState(() => new Set());
  const [dismissedReady, setDismissedReady] = useState(IS_DESKTOP_APP);
  const [durableReadIds, setDurableReadIds] = useState(() => new Set());
  const [feedbackReplies, setFeedbackReplies] = useState([]);
  const rootRef = useRef(null);

  const notifications = useMemo(() => {
    if (!dismissedReady) return [];
    const labNotifications = buildStudentNotifications(labs, labSummariesById, now);
    const replyNotifications = buildFeedbackReplyNotifications(feedbackReplies);
    return [...replyNotifications, ...labNotifications].filter(
      (item) => !dismissedIds.has(item.id),
    );
  }, [dismissedReady, labs, labSummariesById, now, feedbackReplies, dismissedIds]);

  useEffect(() => {
    if (IS_DESKTOP_APP) return undefined;
    let cancelled = false;
    (async () => {
      try {
        const response = await apiFetch(`${READS_BASE}/list`, { headers: authHeaders() });
        if (!response.ok) return;
        const data = await response.json();
        const keys = Array.isArray(data?.keys) ? data.keys : [];
        if (!cancelled) {
          const next = new Set(keys.filter((id) => typeof id === 'string'));
          setReadIds(next);
          setDurableReadIds(next);
        }
      } catch {
        // Silent degrade: leave empty Set (same as failed feedback replies fetch).
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (IS_DESKTOP_APP) return undefined;
    let cancelled = false;
    (async () => {
      try {
        const response = await apiFetch(`${READS_BASE}/dismissed/list`, { headers: authHeaders() });
        if (!response.ok) {
          if (!cancelled) setDismissedReady(true);
          return;
        }
        const data = await response.json();
        const keys = Array.isArray(data?.keys) ? data.keys : [];
        if (!cancelled) {
          setDismissedIds(new Set(keys.filter((id) => typeof id === 'string')));
          setDismissedReady(true);
        }
      } catch {
        if (!cancelled) setDismissedReady(true);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (IS_DESKTOP_APP) return undefined;
    let cancelled = false;
    (async () => {
      try {
        const response = await apiFetch(`${API_BASE}/api/feedback/replies`, { headers: authHeaders() });
        if (!response.ok) return;
        const data = await response.json();
        if (!cancelled && Array.isArray(data)) {
          setFeedbackReplies(data);
        }
      } catch {
        if (!cancelled) setFeedbackReplies([]);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  const unreadCount = useMemo(
    () => notifications.filter((item) => !readIds.has(item.id)).length,
    [notifications, readIds],
  );
  const hasUnread = unreadCount > 0;
  const listedReadKeys = useMemo(
    () => notifications.filter((item) => durableReadIds.has(item.id)).map((item) => item.id),
    [notifications, durableReadIds],
  );
  const hasListedRead = listedReadKeys.length > 0;

  useEffect(() => {
    if (!open) return undefined;
    function handlePointerDown(event) {
      if (rootRef.current && !rootRef.current.contains(event.target)) {
        setOpen(false);
      }
    }
    document.addEventListener('mousedown', handlePointerDown);
    return () => document.removeEventListener('mousedown', handlePointerDown);
  }, [open]);

  async function markOneRead(id) {
    if (typeof id !== 'string' || id.length === 0) return;
    if (readIds.has(id)) return;
    setReadIds((prev) => {
      const next = new Set(prev);
      next.add(id);
      return next;
    });
    if (IS_DESKTOP_APP) {
      setDurableReadIds((prev) => {
        const next = new Set(prev);
        next.add(id);
        return next;
      });
      return;
    }
    try {
      const response = await apiFetch(`${READS_BASE}/mark`, {
        method: 'POST',
        headers: { ...authHeaders(), 'Content-Type': 'application/json' },
        body: JSON.stringify({ key: id }),
      });
      if (!response.ok) {
        setReadIds((prev) => {
          const next = new Set(prev);
          next.delete(id);
          return next;
        });
        showToast(await readFriendlyApiError(response, 'default'), 'error');
        return;
      }
      setDurableReadIds((prev) => {
        const next = new Set(prev);
        next.add(id);
        return next;
      });
    } catch (error) {
      setReadIds((prev) => {
        const next = new Set(prev);
        next.delete(id);
        return next;
      });
      showToast(toFriendlyError(error), 'error');
    }
  }

  async function dismissKeys(keys) {
    const unique = [...new Set(keys.filter((id) => typeof id === 'string' && id.length > 0))];
    if (unique.length === 0) return;
    setDismissedIds((prev) => {
      const next = new Set(prev);
      unique.forEach((id) => next.add(id));
      return next;
    });
    if (IS_DESKTOP_APP) return;
    try {
      const response =
        unique.length === 1
          ? await apiFetch(`${READS_BASE}/dismiss`, {
              method: 'POST',
              headers: { ...authHeaders(), 'Content-Type': 'application/json' },
              body: JSON.stringify({ key: unique[0] }),
            })
          : await apiFetch(`${READS_BASE}/dismiss/batch`, {
              method: 'POST',
              headers: { ...authHeaders(), 'Content-Type': 'application/json' },
              body: JSON.stringify({ keys: unique }),
            });
      if (!response.ok) {
        setDismissedIds((prev) => {
          const next = new Set(prev);
          unique.forEach((id) => next.delete(id));
          return next;
        });
        showToast(await readFriendlyApiError(response, 'default'), 'error');
      }
    } catch (error) {
      setDismissedIds((prev) => {
        const next = new Set(prev);
        unique.forEach((id) => next.delete(id));
        return next;
      });
      showToast(toFriendlyError(error), 'error');
    }
  }

  function handleBellClick() {
    setOpen((prev) => !prev);
  }

  return (
    <div ref={rootRef} className={`relative shrink-0 ${className}`}>
      <button
        type="button"
        aria-label={hasUnread ? 'Notifications — new items' : 'Notifications'}
        aria-expanded={open}
        onClick={handleBellClick}
        className="relative flex h-11 w-11 items-center justify-center rounded-xl bg-surface-secondary text-foreground-secondary transition-colors hover:bg-surface-tertiary hover:text-foreground focus:outline-none focus-visible:ring-2 focus-visible:ring-primary"
      >
        {hasUnread ? (
          <BellRinging size={20} weight="duotone" className="text-gold" aria-hidden="true" />
        ) : (
          <Bell className="h-5 w-5" strokeWidth={2} aria-hidden="true" />
        )}
        {hasUnread && (
          <span className="absolute right-2 top-2 flex h-2.5 w-2.5" aria-hidden="true">
            {!reducedMotion && (
              <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-error opacity-75" />
            )}
            <span className="relative inline-flex h-2.5 w-2.5 rounded-full bg-error ring-2 ring-surface-secondary" />
          </span>
        )}
      </button>

      <AnimatePresence>
      {open && (
        <motion.div
          initial={reducedMotion ? false : { opacity: 0, y: 6, scale: 0.97 }}
          animate={{ opacity: 1, y: 0, scale: 1 }}
          exit={reducedMotion ? undefined : { opacity: 0, y: 4, scale: 0.98 }}
          transition={{ duration: 0.15 }}
          className="absolute right-0 top-full z-30 mt-2 w-[min(100vw-2rem,22rem)] overflow-hidden rounded-2xl border border-border bg-surface shadow-stage-3"
        >
          <div className="flex items-start justify-between gap-3 border-b border-border px-4 py-3">
            <div className="min-w-0">
              <p className="text-sm font-semibold text-foreground">Notifications</p>
              <p className="text-xs text-foreground-muted">
                {!dismissedReady
                  ? 'Loading…'
                  : hasUnread
                    ? `${unreadCount} unread item${unreadCount === 1 ? '' : 's'}`
                    : 'All caught up'}
              </p>
            </div>
            {dismissedReady && hasListedRead && (
              <button
                type="button"
                aria-label="Clear all read"
                onClick={() => dismissKeys(listedReadKeys)}
                className="shrink-0 text-xs font-medium text-foreground-secondary transition-colors hover:text-foreground focus:outline-none focus-visible:ring-2 focus-visible:ring-primary rounded-md px-1 py-0.5"
              >
                Clear all read
              </button>
            )}
          </div>
          <ul className="max-h-72 overflow-y-auto p-2 space-y-1">
            {!dismissedReady ? (
              <li className="px-3 py-6 text-center text-sm text-foreground-muted">
                Loading notifications…
              </li>
            ) : notifications.length === 0 ? (
              <li className="px-3 py-6 text-center text-sm text-foreground-muted">
                No notifications right now.
              </li>
            ) : (
              notifications.map((item) => {
                const read = readIds.has(item.id);
                const canDelete = durableReadIds.has(item.id);
                return (
                  <li key={item.id}>
                    <div
                      className={`flex w-full items-stretch gap-1 rounded-r-xl border-l-4 ${notificationSeverityClasses(item.severity)} ${read ? 'opacity-60' : ''}`}
                    >
                      <button
                        type="button"
                        onClick={() => markOneRead(item.id)}
                        aria-pressed={read}
                        aria-label={read ? `${item.title} (read)` : `Mark ${item.title} as read`}
                        className="min-w-0 flex-1 px-3 py-2.5 text-left transition-opacity focus:outline-none focus-visible:ring-2 focus-visible:ring-primary hover:opacity-90"
                      >
                        <div className="flex items-start gap-2">
                          <div className="min-w-0 flex-1">
                            <p className="text-sm font-medium text-foreground">{item.title}</p>
                            <p className="mt-1 text-xs leading-relaxed text-foreground-secondary">{item.message}</p>
                          </div>
                          {!read && (
                            <span
                              className="mt-1.5 h-2 w-2 shrink-0 rounded-full bg-error"
                              aria-hidden
                            />
                          )}
                        </div>
                      </button>
                      {canDelete && (
                        <button
                          type="button"
                          aria-label="Delete notification"
                          onClick={() => dismissKeys([item.id])}
                          className="shrink-0 self-center mr-2 rounded-lg p-2 text-foreground-muted transition-colors hover:bg-surface-secondary hover:text-foreground focus:outline-none focus-visible:ring-2 focus-visible:ring-primary"
                        >
                          <Trash2 className="h-4 w-4" strokeWidth={2} aria-hidden="true" />
                        </button>
                      )}
                    </div>
                  </li>
                );
              })
            )}
          </ul>
        </motion.div>
      )}
      </AnimatePresence>
    </div>
  );
}
