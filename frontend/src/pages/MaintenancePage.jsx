import { useEffect, useRef } from 'react';
import AppLogo from '../components/ui/AppLogo';
import { brand } from '../theme/brand';
import ThemeToggle from '../components/ThemeToggle';
import './MaintenancePage.css';

export default function MaintenancePage() {
  const headingRef = useRef(null);

  useEffect(() => {
    headingRef.current?.focus();
  }, []);

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
            OOP AutoGrader is paused while we finish scheduled work. Please try
            again later. You do not need to stay on this page.
          </p>
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
