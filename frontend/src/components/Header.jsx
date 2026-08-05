import { useTheme } from '../context/ThemeContext';
import { Moon, Sun, LogOut, GraduationCap, User, Home, Clock, Lock } from 'lucide-react';
import { useState } from 'react';

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
    <header className="w-full rounded-2xl border border-gray-200/80 bg-white px-4 py-3 shadow-sm dark:border-gray-700 dark:bg-[#151b24] sm:px-6">
      <div className="relative flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <button onClick={handleLogoClick} className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br from-purple-600 to-indigo-600 shadow-sm shadow-purple-500/20">
              <GraduationCap className="h-5 w-5 text-white" />
            </div>
            <div className="flex flex-col justify-center">
              <span className="text-sm font-semibold uppercase tracking-[0.25em] text-purple-600 dark:text-purple-300">OOP AutoGrader</span>
            </div>
          </button>
        </div>

        <div className="flex items-center gap-2">
          {user?.fullName && (
            <span className="hidden rounded-full border border-gray-200 bg-gray-50 px-3 py-1.5 text-sm text-gray-700 dark:border-gray-700 dark:bg-[#1A1A24] dark:text-gray-200 sm:inline-block">
              {user.fullName}
            </span>
          )}
          <button
            onClick={onLogout}
            className="hidden items-center gap-2 rounded-lg border border-gray-200 bg-white px-3 py-2 text-gray-900 shadow-sm transition-colors hover:bg-gray-50 dark:border-gray-700 dark:bg-[#1A1A24] dark:text-white dark:hover:bg-[#222230] sm:flex"
            type="button"
          >
            <LogOut className="h-4 w-4" />
            <span>Logout</span>
          </button>
          <button
            onClick={toggleTheme}
            className="flex min-w-[120px] items-center justify-between rounded-lg border border-gray-200 bg-white px-3 py-2 text-gray-900 shadow-sm transition-colors hover:bg-gray-50 dark:border-gray-700 dark:bg-[#1A1A24] dark:text-white dark:hover:bg-[#222230]"
            type="button"
          >
            <span className="flex items-center gap-2">
              {isDark ? <Moon className="h-4 w-4" /> : <Sun className="h-4 w-4" />}
              <span className="hidden sm:inline">{isDark ? 'Dark' : 'Light'}</span>
            </span>
          </button>

          {!hideUserMenu && (
            <div className="relative">
              <button onClick={() => setOpenMenu((v) => !v)} className="ml-2 flex h-10 w-10 items-center justify-center rounded-full border border-gray-200 bg-white text-gray-700 shadow-sm dark:border-gray-700 dark:bg-[#1A1A24]">
                <User className="h-5 w-5" />
              </button>

              {openMenu && (
                <div className="absolute right-0 mt-2 w-80 overflow-hidden rounded-3xl border border-gray-200 bg-white text-gray-900 shadow-lg dark:border-gray-700 dark:bg-[#0d1117] dark:text-gray-100">
                  <div className="space-y-2 border-b border-gray-100 px-4 py-4 dark:border-gray-800">
                    <p className="text-sm font-semibold text-gray-900 dark:text-gray-100">{user?.fullName || user?.username || 'Student'}</p>
                    {user?.email && <p className="text-sm text-gray-500 dark:text-gray-400">{user.email}</p>}
                    <div className="grid gap-2 text-xs text-gray-500 dark:text-gray-400 mt-3">
                      {user?.id && (
                        <div className="flex items-center justify-between gap-2 rounded-2xl bg-gray-50 px-3 py-2 dark:bg-white/5">
                          <span>ID</span>
                          <span className="font-semibold text-gray-900 dark:text-gray-100">{user.id}</span>
                        </div>
                      )}
                      {user?.username && (
                        <div className="flex items-center justify-between gap-2 rounded-2xl bg-gray-50 px-3 py-2 dark:bg-white/5">
                          <span>Username</span>
                          <span className="font-semibold text-gray-900 dark:text-gray-100">{user.username}</span>
                        </div>
                      )}
                      {(user?.irn || user?.studentCode) && (
                        <div className="flex items-center justify-between gap-2 rounded-2xl bg-gray-50 px-3 py-2 dark:bg-white/5">
                          <span>IRN</span>
                          <span className="font-semibold text-gray-900 dark:text-gray-100">{user.irn || user.studentCode}</span>
                        </div>
                      )}
                    </div>
                  </div>
                  <div className="space-y-2 p-3">
                    <button onClick={() => handleMenu('home')} className="flex w-full items-center gap-3 rounded-2xl border border-gray-200 bg-gray-50 px-3 py-2 text-sm text-gray-800 transition hover:bg-gray-100 dark:border-gray-700 dark:bg-[#11171f] dark:text-gray-100 dark:hover:bg-[#1b2230]">
                      <Home className="h-4 w-4" /> Home
                    </button>
                    <button onClick={() => handleMenu('history')} className="flex w-full items-center gap-3 rounded-2xl border border-gray-200 bg-gray-50 px-3 py-2 text-sm text-gray-800 transition hover:bg-gray-100 dark:border-gray-700 dark:bg-[#11171f] dark:text-gray-100 dark:hover:bg-[#1b2230]">
                      <Clock className="h-4 w-4" /> History
                    </button>
                    <button onClick={() => handleMenu('changePassword')} className="flex w-full items-center gap-3 rounded-2xl border border-gray-200 bg-gray-50 px-3 py-2 text-sm text-gray-800 transition hover:bg-gray-100 dark:border-gray-700 dark:bg-[#11171f] dark:text-gray-100 dark:hover:bg-[#1b2230]">
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