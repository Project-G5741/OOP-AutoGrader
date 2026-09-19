import { useTheme } from '../context/ThemeContext';
import { Moon, Sun, LogOut, User, Home, Clock, Lock } from 'lucide-react';
import { useEffect, useRef, useState } from 'react';
import AppLogo from './ui/AppLogo';
import { brand } from '../theme/brand';

export default function Header({
  onLogout,
  user,
  onNavigate,
  onCommand,
  hideUserMenu = false,
  hideHome = false,
  hideHistory = false,
  headerAddon = null,
}) {
  const { isDark, toggleTheme } = useTheme();
  const [openMenu, setOpenMenu] = useState(false);
  const menuRef = useRef(null);

  useEffect(() => {
    if (!openMenu) return undefined;
    function handlePointerDown(event) {
      if (menuRef.current && !menuRef.current.contains(event.target)) {
        setOpenMenu(false);
      }
    }
    document.addEventListener('mousedown', handlePointerDown);
    return () => document.removeEventListener('mousedown', handlePointerDown);
  }, [openMenu]);

  const handleLogoClick = () => {
    if (hideHome) {
      if (onCommand) onCommand('history');
      return;
    }
    if (onCommand) onCommand('home');
    else if (onNavigate) onNavigate('dashboard');
  };

  const handleMenu = (key) => {
    setOpenMenu(false);
    onCommand?.(key);
  };

  return (
    <header className="w-full rounded-2xl border border-border bg-surface px-4 py-3 shadow-sm sm:px-6">
      <div className="relative flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <button onClick={handleLogoClick} className="flex items-center gap-3">
            <AppLogo variant="header" />
            <div className="flex min-w-0 flex-col justify-center">
              <span className="truncate text-sm font-semibold uppercase tracking-[0.15em] text-primary sm:max-w-none sm:tracking-[0.25em]">{brand.appName}</span>
            </div>
          </button>
        </div>

        <div className="flex items-center gap-2">
          {user?.fullName && (
            <span className="hidden rounded-full border border-border bg-surface-secondary px-3 py-1.5 text-sm text-foreground sm:inline-block">
              {user.fullName}
            </span>
          )}
          <button
            onClick={onLogout}
            className="hidden items-center gap-2 rounded-lg border border-border bg-surface px-3 py-2 text-foreground shadow-sm transition-colors hover:bg-surface-secondary sm:flex"
            type="button"
          >
            <LogOut className="h-4 w-4" />
            <span>Logout</span>
          </button>
          <button
            onClick={toggleTheme}
            className="flex min-h-11 min-w-0 items-center justify-between rounded-lg border border-border bg-surface px-3 py-2 text-foreground shadow-sm transition-colors hover:bg-surface-secondary sm:min-w-[120px]"
            type="button"
          >
            <span className="flex items-center gap-2">
              {isDark ? <Moon className="h-4 w-4" /> : <Sun className="h-4 w-4" />}
              <span className="hidden sm:inline">{isDark ? 'Dark' : 'Light'}</span>
            </span>
          </button>

          {!hideUserMenu && (
            <div ref={menuRef} className="relative">
              <button
                type="button"
                aria-label="Open account menu"
                aria-expanded={openMenu}
                aria-haspopup="menu"
                onClick={() => setOpenMenu((v) => !v)}
                className="ml-2 flex h-10 w-10 items-center justify-center rounded-full border border-border bg-surface text-foreground-secondary shadow-sm transition-colors hover:bg-surface-secondary hover:text-foreground focus:outline-none focus-visible:ring-2 focus-visible:ring-primary"
              >
                <User className="h-5 w-5" />
              </button>

              {openMenu && (
                <div
                  role="menu"
                  className="absolute right-0 z-50 mt-2 w-[min(20rem,calc(100vw-2rem))] overflow-hidden rounded-3xl border border-border bg-surface text-foreground shadow-lg"
                >
                  <div className="space-y-2 border-b border-border px-4 py-4">
                    <p className="text-sm font-semibold text-foreground">{user?.fullName || user?.username || 'User'}</p>
                    {user?.email && <p className="text-sm text-foreground-muted">{user.email}</p>}
                    <div className="grid gap-2 text-xs text-foreground-muted mt-3">
                      {(user?.irn || user?.studentCode || user?.lecturerCode || user?.id) && (
                        <div className="flex items-center justify-between gap-2 rounded-2xl bg-surface-secondary px-3 py-2">
                          <span>ID</span>
                          <span className="font-semibold text-foreground">
                            {user?.irn || user?.studentCode || user?.lecturerCode || user?.id}
                          </span>
                        </div>
                      )}
                      {user?.username && (
                        <div className="flex items-center justify-between gap-2 rounded-2xl bg-surface-secondary px-3 py-2">
                          <span>Username</span>
                          <span className="font-semibold text-foreground">{user.username}</span>
                        </div>
                      )}
                    </div>
                  </div>
                  <div className="space-y-2 p-3">
                    {!hideHome && (
                    <button onClick={() => handleMenu('home')} className="flex w-full items-center gap-3 rounded-2xl border border-border bg-surface-secondary px-3 py-2 text-sm text-foreground transition hover:bg-surface-tertiary">
                      <Home className="h-4 w-4" /> Home
                    </button>
                    )}
                    {!hideHistory && (
                    <button onClick={() => handleMenu('history')} className="flex w-full items-center gap-3 rounded-2xl border border-border bg-surface-secondary px-3 py-2 text-sm text-foreground transition hover:bg-surface-tertiary">
                      <Clock className="h-4 w-4" /> History
                    </button>
                    )}
                    <button onClick={() => handleMenu('changePassword')} className="flex w-full items-center gap-3 rounded-2xl border border-border bg-surface-secondary px-3 py-2 text-sm text-foreground transition hover:bg-surface-tertiary">
                      <Lock className="h-4 w-4" /> Change Password
                    </button>
                  </div>
                </div>
              )}
            </div>
          )}
          {headerAddon}
        </div>
      </div>
    </header>
  );
}