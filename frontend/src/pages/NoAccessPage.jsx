import { Link } from 'react-router-dom';
import { defaultDashboardPath } from '../utils/authRoutes';

export default function NoAccessPage({ user }) {
  const dashboardPath = defaultDashboardPath(user?.roles, user?.inCurrentTerm);

  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-4 bg-surface p-8 text-center text-foreground">
      <h1 className="text-2xl font-semibold">No access</h1>
      <p className="max-w-md text-foreground-secondary">
        You do not have permission to use that resource. Your session is still active.
      </p>
      <Link
        to={dashboardPath}
        className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-white"
      >
        Back to dashboard
      </Link>
    </div>
  );
}
