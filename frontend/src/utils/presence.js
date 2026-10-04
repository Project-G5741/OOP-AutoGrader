import { authHeaders, clearSessionAndRedirectToLogin } from './authHeaders';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';
export const PRESENCE_POLL_MS = 10_000;

const IS_DESKTOP_APP = import.meta.env.VITE_APP_MODE === 'desktop';

/**
 * @returns {Promise<number|null>} active count, or null on soft failure.
 * When a Bearer token was sent and the API returns 401, clears session and
 * redirects to login (forced logout after access revoke).
 */
export async function fetchPresenceCount() {
  if (IS_DESKTOP_APP) {
    return null;
  }
  const headers = authHeaders();
  const response = await fetch(`${API_BASE}/api/presence/count`, { headers });
  if (response.status === 401 && headers.Authorization) {
    clearSessionAndRedirectToLogin();
    return null;
  }
  if (!response.ok) {
    return null;
  }
  const body = await response.json();
  return typeof body.count === 'number' ? body.count : null;
}

/** Call while the JWT is still in auth storage. */
export function leavePresence() {
  if (IS_DESKTOP_APP) {
    return;
  }
  const headers = authHeaders();
  if (!headers.Authorization) {
    return;
  }
  fetch(`${API_BASE}/api/presence/leave`, {
    method: 'DELETE',
    headers,
    keepalive: true,
  }).catch(() => {});
}
