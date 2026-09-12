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
    <div className="flex min-h-screen flex-col overflow-x-clip bg-background text-foreground transition-colors">
      <div className="flex w-full min-w-0 flex-1 flex-col px-4 py-0 sm:px-6 lg:px-8">
        <div className="flex w-full max-w-full min-w-0 flex-1 flex-col overflow-x-clip">
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
