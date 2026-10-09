import { clearSessionAndRedirectToLogin } from './authHeaders';
import { ROUTES } from './authRoutes';
import { enterRateLimitJail } from './rateLimitJail';

const IS_DESKTOP_APP = import.meta.env.VITE_APP_MODE === 'desktop';

/**
 * Authenticated API fetch. 401 clears the session and returns to login.
 * 403 keeps the session and opens /no-access.
 * 429 opens the rate-limit jail (session kept) even when authHandling is 'self'.
 * Pass authHandling: 'self' for change-password so 401/403 stay on the form.
 */
export async function apiFetch(input, init = {}) {
  const { authHandling = 'gated', ...fetchInit } = init;
  const response = await fetch(input, fetchInit);
  if (!IS_DESKTOP_APP && enterRateLimitJail(response)) {
    // Navigation owns UX; do not let callers toast on the 429 body.
    return new Promise(() => {});
  }
  if (authHandling === 'gated' && !IS_DESKTOP_APP) {
    if (response.status === 401) {
      clearSessionAndRedirectToLogin();
    } else if (response.status === 403 && window.location.pathname !== ROUTES.noAccess) {
      window.location.assign(ROUTES.noAccess);
    }
  }
  return response;
}
