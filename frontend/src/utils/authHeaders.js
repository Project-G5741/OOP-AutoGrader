import { ROUTES, clearAuthSession, getAccessToken } from './authRoutes';

export function authHeaders(extra = {}) {
  const token = getAccessToken();
  return {
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...extra,
  };
}

let sessionRedirectPending = false;

/** Clear stored session and return to login (e.g. expired or revoked JWT). */
export function clearSessionAndRedirectToLogin() {
  if (sessionRedirectPending) {
    return;
  }
  sessionRedirectPending = true;
  clearAuthSession();
  window.location.assign(ROUTES.login);
}
