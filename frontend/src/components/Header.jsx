import { useTheme } from '../context/ThemeContext';
import { Moon, Sun, LogOut, User, Home, Clock, Lock } from 'lucide-react';
import { useState } from 'react';
import AppLogo from './ui/AppLogo';
import { brand } from '../theme/brand';

export default function Header({ onLogout, user, onNavigate, onCommand, hideUserMenu = false }) {
  const { isDark, toggleTheme } = useTheme();
  const [openMenu, setOpenMenu] = useState(false);

  const handleLogoClick = () => {
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
            <div className="flex flex-col justify-center">
              <span className="text-sm font-semibold uppercase tracking-[0.25em] text-primary">{brand.appName}</span>
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
            className="flex min-w-[120px] items-center justify-between rounded-lg border border-border bg-surface px-3 py-2 text-foreground shadow-sm transition-colors hover:bg-surface-secondary"
            type="button"
          >
            <span className="flex items-center gap-2">
              {isDark ? <Moon className="h-4 w-4" /> : <Sun className="h-4 w-4" />}
              <span className="hidden sm:inline">{isDark ? 'Dark' : 'Light'}</span>
            </span>
          </button>

          {!hideUserMenu && (
            <div className="relative">
              <button onClick={() => setOpenMenu((v) => !v)} className="ml-2 flex h-10 w-10 items-center justify-center rounded-full border border-border bg-surface text-foreground-secondary shadow-sm">
                <User className="h-5 w-5" />
              </button>

              {openMenu && (
                <div className="absolute right-0 mt-2 w-80 overflow-hidden rounded-3xl border border-border bg-surface text-foreground shadow-lg">
                  <div className="space-y-2 border-b border-border px-4 py-4">
                    <p className="text-sm font-semibold text-foreground">{user?.fullName || user?.username || 'Student'}</p>
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
                    <button onClick={() => handleMenu('home')} className="flex w-full items-center gap-3 rounded-2xl border border-border bg-surface-secondary px-3 py-2 text-sm text-foreground transition hover:bg-surface-tertiary">
                      <Home className="h-4 w-4" /> Home
                    </button>
                    <button onClick={() => handleMenu('history')} className="flex w-full items-center gap-3 rounded-2xl border border-border bg-surface-secondary px-3 py-2 text-sm text-foreground transition hover:bg-surface-tertiary">
                      <Clock className="h-4 w-4" /> History
                    </button>
                    <button onClick={() => handleMenu('changePassword')} className="flex w-full items-center gap-3 rounded-2xl border border-border bg-surface-secondary px-3 py-2 text-sm text-foreground transition hover:bg-surface-tertiary">
                      <Lock className="h-4 w-4" /> Change Password
                    </button>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </header>
  );
}