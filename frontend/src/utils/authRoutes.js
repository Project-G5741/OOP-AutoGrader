export const ROUTES = {
  login: '/',
  lecturerDashboard: '/lecturer-dashboard',
  lecturerGrading: '/lecturer-grading',
  lecturerBulkGrading: '/lecturer-bulk-grading',
  lecturerUsers: '/lecturer-users',
  lecturerSolution: '/lecturer-solution',
  lecturerReport: '/lecturer-report',
  lecturerTerms: '/lecturer-terms',
  studentDashboard: '/student-dashboard',
  studentHistory: '/student-history',
  noAccess: '/no-access',
  rateLimited: '/rate-limited',
};

const ACCESS_TOKEN_KEY = 'accessToken';
const USER_KEY = 'user';
const LEGACY_TOKEN_KEY = 'token';

function readAuthValue(key) {
  const fromLocal = localStorage.getItem(key);
  if (fromLocal != null) return fromLocal;
  const fromSession = sessionStorage.getItem(key);
  if (fromSession == null) return null;
  localStorage.setItem(key, fromSession);
  sessionStorage.removeItem(key);
  return fromSession;
}

function writeAuthValue(key, value) {
  localStorage.setItem(key, value);
  sessionStorage.removeItem(key);
}

function removeAuthValue(key) {
  localStorage.removeItem(key);
  sessionStorage.removeItem(key);
}

/** Bearer JWT (or desktop synthetic token) for signed-in API calls. */
export function getAccessToken() {
  return readAuthValue(ACCESS_TOKEN_KEY);
}

export function persistAuthSession(accessToken, userPayload) {
  writeAuthValue(ACCESS_TOKEN_KEY, accessToken);
  writeAuthValue(USER_KEY, JSON.stringify(userPayload));
}

export function clearAuthSession() {
  removeAuthValue(ACCESS_TOKEN_KEY);
  removeAuthValue(USER_KEY);
  localStorage.removeItem(LEGACY_TOKEN_KEY);
}

/** False when the JWT `exp` claim is in the past. Non-JWT desktop token is allowed only in desktop mode. */
export function isAccessTokenCurrentlyValid(token) {
  if (!token) return false;
  if (token === 'desktop-local') {
    return import.meta.env.VITE_APP_MODE === 'desktop';
  }
  const parts = String(token).split('.');
  if (parts.length !== 3) return false;
  try {
    const b64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const padded = b64 + '='.repeat((4 - (b64.length % 4)) % 4);
    const json = atob(padded);
    const payload = JSON.parse(json);
    if (typeof payload.exp !== 'number') return true;
    return payload.exp * 1000 > Date.now();
  } catch {
    return false;
  }
}

function normalizeRoleName(role) {
  const name = typeof role === 'string'
    ? role
    : (role && typeof role.name === 'string' ? role.name : '');
  const upper = String(name).trim().toUpperCase();
  if (!upper) return null;
  if (upper === 'TEACHER') return 'LECTURER';
  return upper;
}

export function normalizeRoleList(roles) {
  if (!Array.isArray(roles)) return [];
  const normalized = roles
    .map((role) => normalizeRoleName(role))
    .filter(Boolean);
  return [...new Set(normalized)];
}

export function hasRole(roles, roleName) {
  const normalizedRoles = normalizeRoleList(roles);
  const target = normalizeRoleName(roleName);
  if (!target) return false;
  return normalizedRoles.includes(target);
}

export function hasAnyRole(userRoles, requiredRoles) {
  if (!Array.isArray(requiredRoles)) return false;
  return requiredRoles.some((role) => hasRole(userRoles, role));
}

export function isInCurrentTerm(value) {
  return value !== false;
}

export function defaultDashboardPath(roles = [], inCurrentTerm = true) {
  if (hasRole(roles, 'LECTURER')) return ROUTES.lecturerDashboard;
  if (hasRole(roles, 'STUDENT')) {
    return isInCurrentTerm(inCurrentTerm) ? ROUTES.studentDashboard : ROUTES.studentHistory;
  }
  return ROUTES.login;
}

export function readStoredUser() {
  const token = getAccessToken();
  if (!token) return null;
  if (!isAccessTokenCurrentlyValid(token)) {
    clearAuthSession();
    return null;
  }
  try {
    const saved = readAuthValue(USER_KEY);
    if (!saved) return null;
    const parsed = JSON.parse(saved);
    if (!parsed || typeof parsed !== 'object') return null;
    const roles = normalizeRoleList(parsed.roles);
    if (!roles.length) return null;
    return { ...parsed, roles };
  } catch {
    return null;
  }
}

export function patchStoredUser(partial) {
  try {
    const stored = JSON.parse(readAuthValue(USER_KEY) || 'null');
    if (!stored || typeof stored !== 'object') return;
    const next = { ...stored, ...partial };
    if (JSON.stringify(stored) === JSON.stringify(next)) return;
    writeAuthValue(USER_KEY, JSON.stringify(next));
  } catch {
    // keep in-memory session
  }
}

export const LECTURER_NAV_TO_ROUTE = {
  dashboard: ROUTES.lecturerDashboard,
  score: ROUTES.lecturerGrading,
  grading: ROUTES.lecturerBulkGrading,
  users: ROUTES.lecturerUsers,
  projects: ROUTES.lecturerSolution,
  reports: ROUTES.lecturerReport,
  terms: ROUTES.lecturerTerms,
};

export const LECTURER_ROUTE_TO_NAV = Object.fromEntries(
  Object.entries(LECTURER_NAV_TO_ROUTE).map(([nav, route]) => [route, nav]),
);
