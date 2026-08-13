import { Moon, Sun } from 'lucide-react';
import { useTheme } from '../context/ThemeContext';

export default function ThemeToggle({ className = '' }) {
  const { isDark, toggleTheme } = useTheme();

  return (
    <button
      type="button"
      onClick={toggleTheme}
      className={`flex items-center gap-2 rounded-lg border border-border bg-surface px-4 py-2 text-foreground shadow-sm transition-colors hover:bg-surface-secondary min-w-[160px] justify-between ${className}`}
    >
      <div className="flex items-center gap-2">
        {isDark ? <Moon className="h-4 w-4" /> : <Sun className="h-4 w-4" />}
        <span>{isDark ? 'Dark Mode' : 'Light Mode'}</span>
      </div>
    </button>
  );
}
