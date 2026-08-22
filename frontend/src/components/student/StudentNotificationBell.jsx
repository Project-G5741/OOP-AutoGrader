import React, { useEffect, useMemo, useRef, useState } from 'react';
import { Bell } from 'lucide-react';
import { buildStudentNotifications, notificationSeverityClasses } from '../../utils/studentNotifications';

const READ_STORAGE_KEY = 'oop-student-notif-read';

function loadReadIds() {
  try {
    const parsed = JSON.parse(sessionStorage.getItem(READ_STORAGE_KEY) || '[]');
    return Array.isArray(parsed) ? parsed.filter((id) => typeof id === 'string') : [];
  } catch {
    return [];
  }
}

function persistReadIds(ids) {
  sessionStorage.setItem(READ_STORAGE_KEY, JSON.stringify([...ids]));
}

export default function StudentNotificationBell({
  labs = [],
  labSummariesById = {},
  className = '',
}) {
  const [open, setOpen] = useState(false);
  const [readIds, setReadIds] = useState(() => new Set(loadReadIds()));
  const rootRef = useRef(null);
  const notifications = buildStudentNotifications(labs, labSummariesById);
  const unreadCount = useMemo(
    () => notifications.filter((item) => !readIds.has(item.id)).length,
    [notifications, readIds],
  );
  const hasUnread = unreadCount > 0;

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

  function handleNotificationClick(notification) {
    setReadIds((prev) => {
      if (prev.has(notification.id)) return prev;
      const next = new Set(prev);
      next.add(notification.id);
      persistReadIds(next);
      return next;
    });
  }

  return (
    <div ref={rootRef} className={`relative shrink-0 ${className}`}>
      <button
        type="button"
        aria-label={hasUnread ? 'Notifications — new items' : 'Notifications'}
        aria-expanded={open}
        onClick={() => setOpen((prev) => !prev)}
        className="relative flex h-11 w-11 items-center justify-center rounded-lg border border-border bg-surface-secondary text-foreground-secondary transition-colors hover:bg-surface-tertiary hover:text-foreground focus:outline-none focus-visible:ring-2 focus-visible:ring-primary"
      >
        <Bell className="h-5 w-5" strokeWidth={2} />
        {hasUnread && (
          <span
            className="absolute right-2 top-2 h-2 w-2 rounded-full bg-error ring-2 ring-surface-secondary"
            aria-hidden
          />
        )}
      </button>

      {open && (
        <div className="absolute right-0 top-full z-30 mt-2 w-[min(100vw-2rem,22rem)] overflow-hidden rounded-xl border border-border bg-surface shadow-lg dark:shadow-none">
          <div className="border-b border-border px-4 py-3">
            <p className="text-sm font-semibold text-foreground">Notifications</p>
            <p className="text-xs text-foreground-muted">
              {hasUnread
                ? `${unreadCount} unread item${unreadCount === 1 ? '' : 's'}`
                : 'All caught up'}
            </p>
          </div>
          <ul className="max-h-72 overflow-y-auto p-2">
            {notifications.length === 0 ? (
              <li className="px-3 py-6 text-center text-sm text-foreground-muted">
                No notifications right now.
              </li>
            ) : (
              notifications.map((item) => {
                const read = readIds.has(item.id);
                return (
                  <li key={item.id}>
                    <button
                      type="button"
                      onClick={() => handleNotificationClick(item)}
                      className={`mb-1 w-full rounded-lg border px-3 py-2.5 text-left transition-colors hover:opacity-90 ${notificationSeverityClasses(item.severity)} ${read ? 'opacity-60' : ''}`}
                    >
                      <p className="text-sm font-medium text-foreground">{item.title}</p>
                      <p className="mt-1 text-xs leading-relaxed text-foreground-secondary">{item.message}</p>
                    </button>
                  </li>
                );
              })
            )}
          </ul>
        </div>
      )}
    </div>
  );
}
