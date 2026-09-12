import { authHeaders } from './authHeaders';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';
export const PRESENCE_POLL_MS = 10_000;

export async function fetchPresenceCount() {
  const response = await fetch(`${API_BASE}/api/presence`, { headers: authHeaders() });
  if (!response.ok) {
    return null;
  }
  const body = await response.json();
  return typeof body.count === 'number' ? body.count : null;
}

/** Call while the JWT is still in sessionStorage. */
export function leavePresence() {
  const headers = authHeaders();
  if (!headers.Authorization) {
    return;
  }
  fetch(`${API_BASE}/api/presence`, {
    method: 'DELETE',
    headers,
    keepalive: true,
  }).catch(() => {});
}
