import { useEffect, useRef } from 'react';
import { Construction } from 'lucide-react';
import AppLogo from '../components/ui/AppLogo';
import LoginBackground from '../components/ui/LoginBackground';
import { brand } from '../theme/brand';
import ThemeToggle from '../components/ThemeToggle';
import './LoginUI.css';

export default function MaintenancePage() {
  const headingRef = useRef(null);

  useEffect(() => {
    headingRef.current?.focus();
  }, []);

  return (
    <div className="login-root">
      <div className="login-bg">
        <LoginBackground />
        <ThemeToggle className="theme-toggle" />

        <div className="login-card-wrapper">
          <div className="logo-title">
            <AppLogo variant="login" />
            <h1 className="main-title">{brand.loginTitle}</h1>
          </div>

          <div className="card" role="status">
            <div className="setup-card" style={{ marginBottom: '1rem' }}>
              <span className="setup-icon">
                <Construction className="h-5 w-5" aria-hidden="true" />
              </span>
              <div>
                <p className="setup-title">Under maintenance</p>
                <p className="setup-copy">
                  The site is locked until this window ends. Sign-in and grading are paused.
                </p>
              </div>
            </div>

            <h2
              ref={headingRef}
              tabIndex={-1}
              className="main-title"
              style={{ fontSize: '1.35rem', textAlign: 'center', outline: 'none' }}
            >
              We&apos;ll be back soon
            </h2>
            <p className="info-text" style={{ marginTop: '0.75rem' }}>
              OOP AutoGrader is temporarily unavailable while we finish maintenance.
              Please try again later. You do not need to sign in during this time.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
