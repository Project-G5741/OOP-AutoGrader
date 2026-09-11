import { useEffect, useState } from 'react';
import { Home, Users, FolderKanban, BarChart3, ClipboardList, CalendarDays, Menu, X } from 'lucide-react';

const navItems = [
  { id: 'dashboard', label: 'Dashboard', icon: Home },
  { id: 'grading', label: 'Grading', icon: ClipboardList },
  { id: 'users', label: 'Users', icon: Users },
  { id: 'terms', label: 'Quarters', icon: CalendarDays },
  { id: 'projects', label: 'Solution', icon: FolderKanban },
  { id: 'reports', label: 'Reports', icon: BarChart3 },
];

function NavButton({ id, label, icon: Icon, active, onClick, className = '' }) {
  const isActive = active === id;
  return (
    <button
      type="button"
      onClick={() => onClick(id)}
      className={`flex w-full items-center gap-3 rounded-xl px-4 py-3 text-sm transition-colors min-h-11 cursor-pointer ${
        isActive
          ? 'bg-primary text-white shadow-sm'
          : 'text-foreground-secondary hover:bg-surface-secondary'
      } ${className}`}
    >
      <Icon className="h-4 w-4 shrink-0" />
      <span className="font-medium">{label}</span>
    </button>
  );
}

export default function NavBar({ active, onNavigate }) {
  const [menuOpen, setMenuOpen] = useState(false);
  const activeItem = navItems.find((item) => item.id === active) ?? navItems[0];

  useEffect(() => {
    if (!menuOpen) return undefined;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = previousOverflow;
    };
  }, [menuOpen]);

  const handleNavigate = (id) => {
    setMenuOpen(false);
    onNavigate?.(id);
  };

  return (
    <div className="relative w-full min-w-0 rounded-2xl border border-border bg-surface px-3 py-3 sm:px-6">
      {/* Mobile / tablet: hamburger menu */}
      <div className="flex items-center justify-between gap-3 lg:hidden">
        <button
          type="button"
          aria-expanded={menuOpen}
          aria-controls="dashboard-nav-menu"
          aria-label={menuOpen ? 'Close navigation menu' : 'Open navigation menu'}
          onClick={() => setMenuOpen((open) => !open)}
          className="inline-flex h-11 w-11 shrink-0 items-center justify-center rounded-xl border border-border bg-surface-secondary text-foreground transition-colors hover:bg-surface-tertiary cursor-pointer"
        >
          {menuOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
        </button>

        <div className="flex min-w-0 flex-1 items-center gap-2">
          <activeItem.icon className="h-4 w-4 shrink-0 text-primary" />
          <span className="truncate text-sm font-semibold text-foreground">{activeItem.label}</span>
        </div>
      </div>

      {menuOpen && (
        <>
          <button
            type="button"
            aria-label="Close navigation menu"
            className="fixed inset-0 z-40 bg-background/60 lg:hidden"
            onClick={() => setMenuOpen(false)}
          />
          <nav
            id="dashboard-nav-menu"
            className="absolute left-0 right-0 top-[calc(100%+0.5rem)] z-50 rounded-2xl border border-border bg-surface p-2 shadow-lg lg:hidden"
          >
            <div className="grid grid-cols-1 gap-1 sm:grid-cols-2">
              {navItems.map(({ id, label, icon }) => (
                <NavButton
                  key={id}
                  id={id}
                  label={label}
                  icon={icon}
                  active={active}
                  onClick={handleNavigate}
                />
              ))}
            </div>
          </nav>
        </>
      )}

      {/* Desktop: inline navigation */}
      <nav className="hidden items-center justify-end gap-2 lg:flex">
        {navItems.map(({ id, label, icon }) => (
          <NavButton
            key={id}
            id={id}
            label={label}
            icon={icon}
            active={active}
            onClick={handleNavigate}
            className="w-auto shrink-0 rounded-lg px-3 py-2.5"
          />
        ))}
      </nav>
    </div>
  );
}
