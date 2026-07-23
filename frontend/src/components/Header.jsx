import { useTheme } from '../context/ThemeContext';
import { Moon, Sun, LogOut, GraduationCap, User, Home, Clock, Edit3 } from 'lucide-react';
import { useState } from 'react';

export default function Header({ onLogout, user, onNavigate, onCommand }) {
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
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br from-purple-600 to-indigo-600">
              <GraduationCap className="h-5 w-5 text-white" />
            </div>
            <div>
              <p className="text-[11px] font-semibold uppercase tracking-[0.3em] text-purple-600">OOP AutoGrader</p>
              <h1 className="text-lg font-semibold text-gray-900 dark:text-white">Teaching & Review Workspace</h1>
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

          <div className="relative">
            <button onClick={() => setOpenMenu((v) => !v)} className="ml-2 flex h-10 w-10 items-center justify-center rounded-full border border-gray-200 bg-white text-gray-700 shadow-sm dark:border-gray-700 dark:bg-[#1A1A24]">
              <User className="h-5 w-5" />
            </button>

            {openMenu && (
              <div className="absolute right-0 mt-2 w-48 rounded-xl border border-gray-800 bg-[#0d1117] p-2 shadow-lg">
                <button onClick={() => handleMenu('home')} className="flex w-full items-center gap-3 rounded-lg px-3 py-2 text-sm text-gray-200 hover:bg-gray-800">
                  <Home className="h-4 w-4 text-gray-300" /> Home
                </button>
                <button onClick={() => handleMenu('history')} className="flex w-full items-center gap-3 rounded-lg px-3 py-2 text-sm text-gray-200 hover:bg-gray-800">
                  <Clock className="h-4 w-4 text-gray-300" /> History
                </button>
                <button onClick={() => handleMenu('editProfile')} className="flex w-full items-center gap-3 rounded-lg px-3 py-2 text-sm text-gray-200 hover:bg-gray-800">
                  <Edit3 className="h-4 w-4 text-gray-300" /> Edit Profile
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </header>
  );
}