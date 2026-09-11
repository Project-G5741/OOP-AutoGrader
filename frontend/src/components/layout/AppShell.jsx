import Header from '../Header';
import Footer from '../Footer';
import NavBar from '../NavBar';

export default function AppShell({
  user,
  onLogout,
  children,
  activeNav,
  onNavigate,
  showNav = false,
  className = '',
  onCommand,
  hideUserMenu = false,
  hideHome = false,
}) {
  return (
    <div className="min-h-screen overflow-x-hidden bg-background text-foreground transition-colors">
      <div className="w-full min-w-0 px-4 py-0 sm:px-6 lg:px-8">
        <div className="w-full max-w-full min-w-0 overflow-x-hidden">
          <Header
            user={user}
            onLogout={onLogout}
            onNavigate={onNavigate}
            onCommand={onCommand}
            hideUserMenu={hideUserMenu}
            hideHome={hideHome}
          />

          {showNav && (
            <div className="relative mt-4 w-full min-w-0">
              <NavBar active={activeNav} onNavigate={onNavigate} />
            </div>
          )}

          <main className={`mt-6 flex-1 min-w-0 ${className}`}>{children}</main>

          <Footer />
        </div>
      </div>
    </div>
  );
}
