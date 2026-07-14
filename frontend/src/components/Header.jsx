import { useTheme } from '../context/ThemeContext';
import { Moon, Sun } from 'lucide-react';

export default function Header() {
  const { isDark, toggleTheme } = useTheme();

  return (
    <div className="flex items-center justify-between mb-8">
      <div className="flex items-center gap-3">
        <div className="w-8 h-8 bg-gradient-to-br from-purple-500 to-purple-600 rounded-lg flex items-center justify-center">
          <svg className="w-5 h-5 text-white" viewBox="0 0 24 24" fill="currentColor">
            <path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zM9 17H7v-7h2v7zm4 0h-2V7h2v10zm4 0h-2v-4h2v4z" />
          </svg>
        </div>
        <h1 className="text-xl text-gray-900 dark:text-white font-medium">Lab Submission</h1>
      </div>

      <button 
        onClick={toggleTheme}
        className="px-4 py-2 bg-white dark:bg-[#1A1A24] text-gray-900 dark:text-white rounded-lg border border-gray-200 dark:border-gray-700 flex items-center gap-2 min-w-[160px] justify-between shadow-sm transition-colors"
      >
        <div className="flex items-center gap-2">
          {isDark ? <Moon className="w-4 h-4" /> : <Sun className="w-4 h-4" />}
          <span>{isDark ? 'Dark Mode' : 'Light Mode'}</span>
        </div>
      </button>
    </div>
  );
}