import { Home, Users, FolderKanban, BarChart3, Moon, Sun } from 'lucide-react';

const navItems = [
  { id: 'dashboard', label: 'Dashboard', icon: Home },
  { id: 'users', label: 'Users', icon: Users },
  { id: 'projects', label: 'Submission', icon: FolderKanban },
  { id: 'reports', label: 'Reports', icon: BarChart3 },
];

export default function NavBar({ active, onNavigate }) {
  return (
    <div className="w-full rounded-2xl border border-gray-200/80 bg-white px-4 py-3 dark:border-gray-700 dark:bg-[#151b24] sm:px-6">
      <div className="flex flex-wrap items-center justify-end gap-2">
        <nav className="flex flex-wrap items-center gap-2">
          {navItems.map(({ id, label, icon: Icon }) => (
            <button
              key={id}
              type="button"
              onClick={() => onNavigate?.(id)}
              className={`flex items-center gap-2 rounded-lg px-3 py-2 text-sm transition-colors ${
                active === id
                  ? 'bg-purple-600 text-white shadow-sm'
                  : 'text-gray-600 hover:bg-gray-100 dark:text-gray-300 dark:hover:bg-[#1e2530]'
              }`}
            >
              <Icon className="w-4 h-4" />
              <span>{label}</span>
            </button>
          ))}
        </nav>
      </div>
    </div>
  );
}