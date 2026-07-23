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
}) {
  return (
    <div className="min-h-screen bg-[#F5F5F7] text-slate-900 transition-colors dark:bg-[#0A0A0F] dark:text-slate-100">
      <div className="w-full px-4 py-4 sm:px-6 lg:px-8">
        <div className="w-full">
          <Header user={user} onLogout={onLogout} onNavigate={onNavigate} onCommand={onCommand} />

          {showNav && (
            <div className="mt-4 w-full">
              <NavBar active={activeNav} onNavigate={onNavigate} />
            </div>
          )}

          <main className={`mt-6 flex-1 ${className}`}>{children}</main>

          <Footer />
        </div>
      </div>
    </div>
  );
}
