import { Navigate, useLocation } from 'react-router-dom';
import { defaultDashboardPath, hasAnyRole, readStoredUser, ROUTES } from '../../utils/authRoutes';

export default function RequireRole({ anyOf, children }) {
  const location = useLocation();
  const user = readStoredUser();

  if (!user) {
    return <Navigate to={ROUTES.login} replace state={{ from: location.pathname }} />;
  }

  if (!hasAnyRole(user.roles, anyOf)) {
    return <Navigate to={defaultDashboardPath(user.roles, user.inCurrentTerm)} replace />;
  }

  return children;
}
