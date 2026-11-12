import { Link } from 'react-router-dom';
import { ShieldWarning } from '@phosphor-icons/react';
import { ArrowLeft } from 'lucide-react';
import { defaultDashboardPath } from '../utils/authRoutes';
import { StageCard3D } from '../components/ui/MotionPrimitives';
import LottieAnimation from '../components/ui/LottieAnimation';
import emptyOrbitData from '../assets/lottie/empty-orbit.json';

export default function NoAccessPage({ user }) {
  const dashboardPath = defaultDashboardPath(user?.roles, user?.inCurrentTerm);

  return (
    <div className="flex min-h-screen items-center justify-center bg-background p-6 text-foreground">
      <StageCard3D className="w-full max-w-md p-8 text-center">
        <div className="mx-auto mb-2 h-24 w-24">
          <LottieAnimation animationData={emptyOrbitData} className="h-full w-full" />
        </div>
        <div className="mx-auto mb-3 inline-flex h-11 w-11 items-center justify-center rounded-xl bg-error-bg text-error-text">
          <ShieldWarning size={24} weight="duotone" aria-hidden="true" />
        </div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">No access</h1>
        <p className="mt-2 text-sm leading-relaxed text-foreground-secondary">
          You do not have permission to use that resource. Your session is still active.
        </p>
        <Link
          to={dashboardPath}
          className="mt-6 inline-flex items-center justify-center gap-2 rounded-xl bg-primary px-5 py-2.5 text-sm font-semibold text-white shadow-stage-1 transition-all hover:bg-primary-hover"
        >
          <ArrowLeft className="h-4 w-4" aria-hidden />
          Back to dashboard
        </Link>
      </StageCard3D>
    </div>
  );
}
