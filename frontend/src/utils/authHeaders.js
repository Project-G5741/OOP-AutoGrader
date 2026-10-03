export function authHeaders(extra = {}) {
  const token = sessionStorage.getItem('accessToken');
  return {
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...extra,
  };
}

import { ROUTES } from './authRoutes';

let sessionRedirectPending = false;

/** Clear stored session and return to login (e.g. expired JWT after backend restart). */
export function clearSessionAndRedirectToLogin() {
  if (sessionRedirectPending) {
    return;
  }
  sessionRedirectPending = true;
  sessionStorage.removeItem('accessToken');
  sessionStorage.removeItem('user');
  localStorage.removeItem('token');
  window.location.assign(ROUTES.login);
}
