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
  hideHistory = false,
}) {
  return (
    <div className="min-h-screen bg-background text-foreground transition-colors">
      <div className="w-full min-w-0 px-4 py-0 sm:px-6 lg:px-8">
        <div className="relative z-30 w-full max-w-full min-w-0">
          <Header
            user={user}
            onLogout={onLogout}
            onNavigate={onNavigate}
            onCommand={onCommand}
            hideUserMenu={hideUserMenu}
            hideHome={hideHome}
            hideHistory={hideHistory}
          />
        </div>

        <div className="w-full max-w-full min-w-0 overflow-x-hidden">
          {showNav && (
            <div className="relative mt-4 w-full min-w-0 overflow-visible">
              <NavBar active={activeNav} onNavigate={onNavigate} />
            </div>
          )}

          <main className={`relative z-0 mt-6 flex-1 min-w-0 ${className}`}>{children}</main>

          <Footer />
        </div>
      </div>
    </div>
  );
}
