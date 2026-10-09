import { ROUTES } from './authRoutes';

const PRIOR_KEY = 'oop-rate-limit-prior';
const IS_DESKTOP_APP = import.meta.env.VITE_APP_MODE === 'desktop';

/** @returns {number} hold seconds from Retry-After or default 5 */
export function readRetryAfterSeconds(response, fallback = 5) {
  const raw = response?.headers?.get?.('Retry-After');
  const n = raw != null ? Number.parseInt(raw, 10) : NaN;
  if (Number.isFinite(n) && n > 0) {
    return n;
  }
  return fallback;
}

function isSafeRelativePath(path) {
  if (typeof path !== 'string' || !path.startsWith('/') || path.startsWith('//')) {
    return false;
  }
  if (path === ROUTES.rateLimited) {
    return false;
  }
  return true;
}

export function storePriorLocation(pathname, search = '') {
  const path = `${pathname || ''}${search || ''}`;
  if (!isSafeRelativePath(pathname || '')) {
    return;
  }
  try {
    sessionStorage.setItem(PRIOR_KEY, path);
  } catch {
    /* ignore quota */
  }
}

export function takePriorLocation(fallback = ROUTES.login) {
  let stored = null;
  try {
    stored = sessionStorage.getItem(PRIOR_KEY);
    sessionStorage.removeItem(PRIOR_KEY);
  } catch {
    stored = null;
  }
  if (stored && isSafeRelativePath(stored.split('?')[0])) {
    return stored;
  }
  return fallback;
}

/**
 * Navigate to the rate-limit jail once for a 429. Returns true if this call
 * consumed the response for UX (caller must not toast). Desktop skips.
 */
export function enterRateLimitJail(response) {
  if (IS_DESKTOP_APP || !response || response.status !== 429) {
    return false;
  }
  if (typeof window === 'undefined') {
    return false;
  }
  const { pathname, search } = window.location;
  if (pathname === ROUTES.rateLimited) {
    return true;
  }
  storePriorLocation(pathname, search);
  const seconds = readRetryAfterSeconds(response, 5);
  const qs = seconds !== 5 ? `?wait=${seconds}` : '';
  window.location.replace(`${ROUTES.rateLimited}${qs}`);
  return true;
}
