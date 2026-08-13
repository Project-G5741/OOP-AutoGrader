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
}) {
  return (
    <div className="min-h-screen bg-background text-foreground transition-colors overflow-x-hidden">
      <div className="w-full px-0 py-0">
        <div className="w-full max-w-full overflow-x-hidden">
          <Header
            user={user}
            onLogout={onLogout}
            onNavigate={onNavigate}
            onCommand={onCommand}
            hideUserMenu={hideUserMenu}
          />

          {showNav && (
            <div className="mt-4 w-full">
              <NavBar active={activeNav} onNavigate={onNavigate} />
            </div>
          )}

          <main className={`mt-6 flex-1 min-w-0 overflow-x-hidden ${className}`}>{children}</main>

          <Footer />
        </div>
      </div>
    </div>
  );
}
