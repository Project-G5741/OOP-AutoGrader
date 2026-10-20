import { authHeaders } from './authHeaders';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';
const IS_DESKTOP_APP = import.meta.env.VITE_APP_MODE === 'desktop';

/** @type {() => string[]} */
let getReplyIds = () => [];

/**
 * Bell registers the reply ids currently shown this visit.
 * @param {() => string[]} source
 */
export function setFeedbackReplyIdsSource(source) {
  getReplyIds = typeof source === 'function' ? source : () => [];
}

/**
 * Hard-delete author feedback rows that were shown as lecturer replies.
 * Fire-and-forget with keepalive so it can run on pagehide / logout.
 */
export function consumeFeedbackRepliesOnLeave() {
  if (IS_DESKTOP_APP) return;
  const ids = getReplyIds().filter((id) => typeof id === 'string' && id.length > 0);
  if (ids.length === 0) return;
  try {
    fetch(`${API_BASE}/api/feedback/replies`, {
      method: 'DELETE',
      headers: {
        ...authHeaders(),
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ ids }),
      keepalive: true,
    }).catch(() => {});
  } catch {
    // ignore — leave path must not block logout / unload
  }
}
