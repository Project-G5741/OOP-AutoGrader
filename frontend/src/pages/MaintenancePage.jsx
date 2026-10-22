import { useEffect, useRef, useState } from 'react';
import AppLogo from '../components/ui/AppLogo';
import { brand } from '../theme/brand';
import ThemeToggle from '../components/ThemeToggle';
import Login from './Login';
import './MaintenancePage.css';

export default function MaintenancePage({
  onLoginSuccess,
  loginMessage,
  onDismissLoginMessage,
}) {
  const headingRef = useRef(null);
  const [showLecturerLogin, setShowLecturerLogin] = useState(false);

  useEffect(() => {
    headingRef.current?.focus();
  }, [showLecturerLogin]);

  if (showLecturerLogin && onLoginSuccess) {
    return (
      <Login
        onLoginSuccess={onLoginSuccess}
        loginMessage={loginMessage}
        onDismissLoginMessage={onDismissLoginMessage}
        onBack={() => setShowLecturerLogin(false)}
      />
    );
  }

  return (
    <div className="mnt">
      <ThemeToggle className="mnt-toggle" />

      <main className="mnt-shell">
        <div className="mnt-copy">
          <div className="mnt-brand">
            <AppLogo variant="header" />
            <p className="mnt-brand-name">{brand.appName}</p>
          </div>

          <p className="mnt-kicker">Site under maintenance</p>
          <h1 ref={headingRef} tabIndex={-1} className="mnt-title">
            We&apos;ll be back soon
          </h1>
          <p className="mnt-body" role="status">
            Student access is paused while we finish scheduled work. Lecturers can
            still sign in to manage the course.
          </p>
          {onLoginSuccess && (
            <button
              type="button"
              className="mnt-lecturer-btn"
              onClick={() => setShowLecturerLogin(true)}
            >
              Lecturer sign in
            </button>
          )}
        </div>

        <div className="mnt-art" aria-hidden="true">
          <span className="mnt-art-glow" />
          <img
            src="/maintenance/maintenance-hero.jpg"
            alt=""
            width={1024}
            height={1024}
          />
        </div>
      </main>
    </div>
  );
}
