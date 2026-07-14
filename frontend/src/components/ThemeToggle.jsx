import { Moon, Sun } from 'lucide-react';
import { useTheme } from '../context/ThemeContext';

export default function ThemeToggle() {
  const { isDark, toggleTheme } = useTheme();

  return (
    <button
      onClick={toggleTheme}
      className="absolute top-6 right-6 px-4 py-2 bg-white dark:bg-[#1e2530] text-gray-900 dark:text-white rounded-lg border border-gray-200 dark:border-gray-700 flex items-center gap-2 min-w-[160px] justify-between shadow-sm dark:shadow-none transition-colors hover:bg-gray-50 dark:hover:bg-[#252d3a]"
    >
      <div className="flex items-center gap-2">
        {isDark ? <Moon className="w-4 h-4" /> : <Sun className="w-4 h-4" />}
        <span>{isDark ? 'Dark Mode' : 'Light Mode'}</span>
      </div>
    </button>
  );
}