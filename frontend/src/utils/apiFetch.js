import { clearSessionAndRedirectToLogin } from './authHeaders';
import { ROUTES } from './authRoutes';

const IS_DESKTOP_APP = import.meta.env.VITE_APP_MODE === 'desktop';

/**
 * Authenticated API fetch. 401 clears the session and returns to login.
 * 403 keeps the session and opens /no-access.
 * Pass authHandling: 'self' for change-password so 401/403 stay on the form.
 */
export async function apiFetch(input, init = {}) {
  const { authHandling = 'gated', ...fetchInit } = init;
  const response = await fetch(input, fetchInit);
  if (authHandling === 'gated' && !IS_DESKTOP_APP) {
    if (response.status === 401) {
      clearSessionAndRedirectToLogin();
    } else if (response.status === 403 && window.location.pathname !== ROUTES.noAccess) {
      window.location.assign(ROUTES.noAccess);
    }
  }
  return response;
}
