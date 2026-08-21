export const ROUTES = {
  login: '/',
  lecturerDashboard: '/lecturer-dashboard',
  lecturerGrading: '/lecturer-grading',
  lecturerUsers: '/lecturer-users',
  lecturerSolution: '/lecturer-solution',
  lecturerReport: '/lecturer-report',
  lecturerTerms: '/lecturer-terms',
  studentDashboard: '/student-dashboard',
  studentHistory: '/student-history',
};

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
  try {
    const saved = sessionStorage.getItem('user');
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
    const stored = JSON.parse(sessionStorage.getItem('user') || 'null');
    if (!stored || typeof stored !== 'object') return;
    const next = { ...stored, ...partial };
    if (JSON.stringify(stored) === JSON.stringify(next)) return;
    sessionStorage.setItem('user', JSON.stringify(next));
  } catch {
    // keep in-memory session
  }
}

export const LECTURER_NAV_TO_ROUTE = {
  dashboard: ROUTES.lecturerDashboard,
  grading: ROUTES.lecturerGrading,
  users: ROUTES.lecturerUsers,
  projects: ROUTES.lecturerSolution,
  reports: ROUTES.lecturerReport,
  terms: ROUTES.lecturerTerms,
};

export const LECTURER_ROUTE_TO_NAV = Object.fromEntries(
  Object.entries(LECTURER_NAV_TO_ROUTE).map(([nav, route]) => [route, nav]),
);
