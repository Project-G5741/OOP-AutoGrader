import { Home, Users, FolderKanban, BarChart3, ClipboardList, CalendarDays } from 'lucide-react';

const navItems = [
  { id: 'dashboard', label: 'Dashboard', icon: Home },
  { id: 'grading', label: 'Grading', icon: ClipboardList },
  { id: 'users', label: 'Users', icon: Users },
  { id: 'terms', label: 'Quarters', icon: CalendarDays },
  { id: 'projects', label: 'Solution', icon: FolderKanban },
  { id: 'reports', label: 'Reports', icon: BarChart3 },
];

export default function NavBar({ active, onNavigate }) {
  return (
    <div className="w-full rounded-2xl border border-border bg-surface px-3 py-3 sm:px-6">
      <div className="flex items-center justify-end">
        <nav className="flex max-w-full items-center gap-2 overflow-x-auto pb-1 [-ms-overflow-style:none] [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
          {navItems.map(({ id, label, icon: Icon }) => (
            <button
              key={id}
              type="button"
              onClick={() => onNavigate?.(id)}
              className={`flex shrink-0 items-center gap-2 rounded-lg px-3 py-2.5 text-sm transition-colors min-h-11 ${
                active === id
                  ? 'bg-primary text-white shadow-sm'
                  : 'text-foreground-secondary hover:bg-surface-secondary'
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