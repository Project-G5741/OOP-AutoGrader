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
  headerAddon = null,
}) {
  return (
    <div className="flex min-h-screen flex-col bg-background text-foreground transition-colors">
      <div className="flex w-full min-w-0 flex-1 flex-col px-4 py-0 sm:px-6 lg:px-8">
        <div className="relative z-30 w-full max-w-full min-w-0">
          <Header
            user={user}
            onLogout={onLogout}
            onNavigate={onNavigate}
            onCommand={onCommand}
            hideUserMenu={hideUserMenu}
            hideHome={hideHome}
            hideHistory={hideHistory}
            headerAddon={headerAddon}
          />
        </div>

        <div className="flex w-full max-w-full min-w-0 flex-1 flex-col overflow-x-hidden">
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
