export const ROUTES = {
  login: '/',
  lecturerDashboard: '/lecturer-dashboard',
  lecturerGrading: '/lecturer-grading',
  lecturerUsers: '/lecturer-users',
  lecturerSolution: '/lecturer-solution',
  lecturerReport: '/lecturer-report',
  studentDashboard: '/student-dashboard',
  studentHistory: '/student-history',
};

export function hasRole(roles, roleName) {
  if (!Array.isArray(roles)) return false;
  const normalized = roleName === 'LECTURER'
    ? ['LECTURER', 'TEACHER']
    : [roleName];
  return roles.some((role) => normalized.includes(String(role).toUpperCase()));
}

export function defaultDashboardPath(roles = []) {
  if (hasRole(roles, 'LECTURER')) return ROUTES.lecturerDashboard;
  if (hasRole(roles, 'STUDENT')) return ROUTES.studentDashboard;
  return ROUTES.login;
}

export const LECTURER_NAV_TO_ROUTE = {
  dashboard: ROUTES.lecturerDashboard,
  grading: ROUTES.lecturerGrading,
  users: ROUTES.lecturerUsers,
  projects: ROUTES.lecturerSolution,
  reports: ROUTES.lecturerReport,
};

export const LECTURER_ROUTE_TO_NAV = Object.fromEntries(
  Object.entries(LECTURER_NAV_TO_ROUTE).map(([nav, route]) => [route, nav]),
);
