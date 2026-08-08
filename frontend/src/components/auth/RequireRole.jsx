import { Navigate, useLocation } from 'react-router-dom';
import { defaultDashboardPath, ROUTES } from '../../utils/authRoutes';

function readStoredUser() {
  try {
    const saved = sessionStorage.getItem('user');
    if (!saved) return null;
    const parsed = JSON.parse(saved);
    return Array.isArray(parsed?.roles) ? parsed : null;
  } catch {
    return null;
  }
}

function hasAnyRole(userRoles, requiredRoles) {
  if (!Array.isArray(userRoles) || !Array.isArray(requiredRoles)) return false;
  const normalizedUser = userRoles.map((role) => String(role).toUpperCase());
  return requiredRoles.some((role) => {
    const target = String(role).toUpperCase();
    if (target === 'LECTURER') {
      return normalizedUser.includes('LECTURER') || normalizedUser.includes('TEACHER');
    }
    return normalizedUser.includes(target);
  });
}

export default function RequireRole({ anyOf, children }) {
  const location = useLocation();
  const user = readStoredUser();

  if (!user) {
    return <Navigate to={ROUTES.login} replace state={{ from: location.pathname }} />;
  }

  if (!hasAnyRole(user.roles, anyOf)) {
    return <Navigate to={defaultDashboardPath(user.roles)} replace />;
  }

  return children;
}
