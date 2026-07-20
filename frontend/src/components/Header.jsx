import { useTheme } from '../context/ThemeContext';
import { Moon, Sun, LogOut, GraduationCap } from 'lucide-react';

export default function Header({ onLogout, user }) {
  const { isDark, toggleTheme } = useTheme();

  return (
    <header className="w-full rounded-2xl border border-gray-200/80 bg-white px-4 py-3 shadow-sm dark:border-gray-700 dark:bg-[#151b24] sm:px-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br from-purple-600 to-indigo-600">
            <GraduationCap className="h-5 w-5 text-white" />
          </div>
          <div>
            <p className="text-[11px] font-semibold uppercase tracking-[0.3em] text-purple-600">OOP AutoGrader</p>
            <h1 className="text-lg font-semibold text-gray-900 dark:text-white">Teaching & Review Workspace</h1>
          </div>
        </div>

        <div className="flex items-center gap-2">
          {user?.fullName && (
            <span className="hidden rounded-full border border-gray-200 bg-gray-50 px-3 py-1.5 text-sm text-gray-700 dark:border-gray-700 dark:bg-[#1A1A24] dark:text-gray-200 sm:inline-block">
              {user.fullName}
            </span>
          )}
          <button
            onClick={onLogout}
            className="flex items-center gap-2 rounded-lg border border-gray-200 bg-white px-3 py-2 text-gray-900 shadow-sm transition-colors hover:bg-gray-50 dark:border-gray-700 dark:bg-[#1A1A24] dark:text-white dark:hover:bg-[#222230]"
            type="button"
          >
            <LogOut className="h-4 w-4" />
            <span>Logout</span>
          </button>
          <button
            onClick={toggleTheme}
            className="flex min-w-[150px] items-center justify-between rounded-lg border border-gray-200 bg-white px-3 py-2 text-gray-900 shadow-sm transition-colors hover:bg-gray-50 dark:border-gray-700 dark:bg-[#1A1A24] dark:text-white dark:hover:bg-[#222230]"
            type="button"
          >
            <span className="flex items-center gap-2">
              {isDark ? <Moon className="h-4 w-4" /> : <Sun className="h-4 w-4" />}
              <span>{isDark ? 'Dark Mode' : 'Light Mode'}</span>
            </span>
          </button>
        </div>
      </div>
    </header>
  );
}