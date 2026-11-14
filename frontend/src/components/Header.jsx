import { LogOut, User, Home, Clock, Lock, Trophy, GraduationCap, Presentation, KeyRound } from 'lucide-react';
import { AnimatePresence, motion, useReducedMotion } from 'framer-motion';
import ThemeToggle from './ThemeToggle';
import { useEffect, useRef, useState } from 'react';
import AppLogo from './ui/AppLogo';
import { brand } from '../theme/brand';
import { studentAreaWaitMs } from '../utils/studentAreaGate';

const IS_DESKTOP_APP = import.meta.env.VITE_APP_MODE === 'desktop';

export default function Header({
  onLogout,
  user,
  onNavigate,
  onCommand,
  hideUserMenu = false,
  hideHome = false,
  hideHistory = false,
  hideRanking = false,
  hideSectionLinks = false,
  hideChangePassword = false,
  showMascotPracticeToken = false,
  showStudentArea = false,
  showLecturerArea = false,
  headerAddon = null,
  headerBeforeUserMenu = null,
  headerUserSlot = null,
}) {
  const reducedMotion = useReducedMotion();
  const [openMenu, setOpenMenu] = useState(false);
  const [studentAreaLocked, setStudentAreaLocked] = useState(
    () => showStudentArea && studentAreaWaitMs() > 0,
  );
  const menuRef = useRef(null);
  const showChangePassword = !IS_DESKTOP_APP && !hideChangePassword;

  useEffect(() => {
    if (!showStudentArea) {
      setStudentAreaLocked(false);
      return undefined;
    }
    const wait = studentAreaWaitMs();
    if (wait <= 0) {
      setStudentAreaLocked(false);
      return undefined;
    }
    setStudentAreaLocked(true);
    const timer = setTimeout(() => setStudentAreaLocked(false), wait);
    return () => clearTimeout(timer);
  }, [showStudentArea]);

  useEffect(() => {
    if (IS_DESKTOP_APP || !openMenu) return undefined;
    function handlePointerDown(event) {
      if (menuRef.current && !menuRef.current.contains(event.target)) {
        setOpenMenu(false);
      }
    }
    document.addEventListener('mousedown', handlePointerDown);
    return () => document.removeEventListener('mousedown', handlePointerDown);
  }, [openMenu]);

  const handleLogoClick = () => {
    if (hideHome) {
      if (onCommand) onCommand('history');
      return;
    }
    if (onCommand) onCommand('home');
    else if (onNavigate) onNavigate('dashboard');
  };

  const handleMenu = (key) => {
    setOpenMenu(false);
    onCommand?.(key);
  };

  return (
    <header className="w-full rounded-2xl border border-border bg-surface px-3 py-2.5 shadow-stage-1 sm:px-6 sm:py-3">
      <div className="relative flex min-w-0 items-center gap-2 sm:gap-3">
        <div className="flex min-w-0 flex-1 items-center gap-2 sm:gap-3">
          <button onClick={handleLogoClick} className="group flex min-w-0 items-center gap-2.5 sm:gap-3">
            <AppLogo variant="header" />
            <div className="flex min-w-0 flex-col justify-center text-left">
              <span className="truncate text-xs font-bold uppercase tracking-[0.12em] text-navy dark:text-primary-text sm:text-sm sm:tracking-[0.22em]">
                {brand.appName}
              </span>
            </div>
          </button>
        </div>

        <div className="ml-auto flex shrink-0 flex-nowrap items-center gap-1.5 sm:gap-2">
          {headerUserSlot ?? (!IS_DESKTOP_APP && user?.fullName && (
            <span className="hidden rounded-full bg-surface-secondary px-3 py-1.5 text-sm font-medium text-foreground sm:inline-block">
              {user.fullName}
            </span>
          ))}
          <ThemeToggle />

          {headerBeforeUserMenu}
          {!hideUserMenu && (
            IS_DESKTOP_APP ? (
              <button
                type="button"
                onClick={() => onLogout?.()}
                aria-label="Exit"
                className="inline-flex h-10 shrink-0 items-center gap-2 rounded-full border border-error/30 bg-error-bg/50 px-3.5 text-sm font-medium text-error-text transition-colors hover:bg-error-bg focus:outline-none focus-visible:ring-2 focus-visible:ring-primary"
              >
                <LogOut className="h-4 w-4" aria-hidden="true" />
                <span>Exit</span>
              </button>
            ) : (
            <div ref={menuRef} className="relative">
              <button
                type="button"
                aria-label="Open account menu"
                aria-expanded={openMenu}
                aria-haspopup="menu"
                onClick={() => setOpenMenu((v) => !v)}
                className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-navy text-white transition-all hover:bg-navy-hover focus:outline-none focus-visible:ring-2 focus-visible:ring-primary dark:bg-surface-secondary dark:text-foreground dark:hover:bg-surface-tertiary"
              >
                <User className="h-5 w-5" />
              </button>

              <AnimatePresence>
              {openMenu && (
                <motion.div
                  role="menu"
                  initial={reducedMotion ? false : { opacity: 0, y: 6, scale: 0.97 }}
                  animate={{ opacity: 1, y: 0, scale: 1 }}
                  exit={reducedMotion ? undefined : { opacity: 0, y: 4, scale: 0.98 }}
                  transition={{ duration: 0.15, ease: [0.22, 1, 0.36, 1] }}
                  className="absolute right-0 z-50 mt-2 w-[min(20rem,calc(100vw-2rem))] overflow-hidden rounded-2xl border border-border bg-surface text-foreground shadow-stage-3"
                >
                  <div className="space-y-1.5 border-b border-border px-4 py-4">
                    <p className="text-sm font-semibold text-foreground">{user?.fullName || user?.username || 'User'}</p>
                    {user?.email && <p className="text-xs text-foreground-muted">{user.email}</p>}
                    {(user?.irn || user?.studentCode || user?.lecturerCode || user?.id || user?.username) && (
                      <div className="mt-2.5 space-y-1 rounded-xl bg-surface-secondary px-3 py-2 text-xs text-foreground-muted">
                        {(user?.irn || user?.studentCode || user?.lecturerCode || user?.id) && (
                          <div className="flex items-center justify-between gap-2">
                            <span>ID</span>
                            <span className="font-mono font-semibold text-foreground">
                              {user?.irn || user?.studentCode || user?.lecturerCode || user?.id}
                            </span>
                          </div>
                        )}
                        {user?.username && (
                          <div className="flex items-center justify-between gap-2">
                            <span>Username</span>
                            <span className="font-semibold text-foreground">{user.username}</span>
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                  <div className="space-y-0.5 p-2">
                    {!hideSectionLinks && !hideHome && (
                    <button onClick={() => handleMenu('home')} className="flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium text-foreground transition-colors hover:bg-surface-secondary">
                      <Home className="h-4 w-4 text-foreground-secondary" /> Home
                    </button>
                    )}
                    {!hideSectionLinks && !hideHistory && (
                    <button onClick={() => handleMenu('history')} className="flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium text-foreground transition-colors hover:bg-surface-secondary">
                      <Clock className="h-4 w-4 text-foreground-secondary" /> History
                    </button>
                    )}
                    {!hideSectionLinks && !hideRanking && (
                    <button onClick={() => handleMenu('ranking')} className="flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium text-foreground transition-colors hover:bg-surface-secondary">
                      <Trophy className="h-4 w-4 text-gold" /> Ranking
                    </button>
                    )}
                    {showStudentArea && (
                    <button
                      type="button"
                      disabled={studentAreaLocked}
                      title={studentAreaLocked ? 'Available 5 seconds after sign-in' : undefined}
                      onClick={() => {
                        if (studentAreaLocked) return;
                        handleMenu('studentArea');
                      }}
                      className="flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium text-foreground transition-colors hover:bg-surface-secondary disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      <GraduationCap className="h-4 w-4 text-primary" /> Student area
                    </button>
                    )}
                    {showLecturerArea && (
                    <button onClick={() => handleMenu('lecturerArea')} className="flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium text-foreground transition-colors hover:bg-surface-secondary">
                      <Presentation className="h-4 w-4 text-secondary" /> Lecturer area
                    </button>
                    )}
                    {showChangePassword && (
                    <button onClick={() => handleMenu('changePassword')} className="flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium text-foreground transition-colors hover:bg-surface-secondary">
                      <Lock className="h-4 w-4 text-foreground-secondary" /> Change Password
                    </button>
                    )}
                    {showMascotPracticeToken && (
                    <>
                    <button onClick={() => handleMenu('copyMascotPracticeToken')} className="flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium text-foreground transition-colors hover:bg-surface-secondary">
                      <KeyRound className="h-4 w-4 text-foreground-secondary" /> Copy practice code
                    </button>
                    <button
                      type="button"
                      onClick={() => {
                        if (!window.confirm('Replace the current practice code? The old code stops working on desktop.')) {
                          return;
                        }
                        handleMenu('regenerateMascotPracticeToken');
                      }}
                      className="flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium text-foreground transition-colors hover:bg-surface-secondary"
                    >
                      <KeyRound className="h-4 w-4 text-foreground-secondary" /> New practice code
                    </button>
                    </>
                    )}
                    <div className="my-1 border-t border-border-subtle" />
                    <button
                      type="button"
                      onClick={() => {
                        setOpenMenu(false);
                        onLogout?.();
                      }}
                      className="flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium text-error-text transition-colors hover:bg-error-bg/50"
                    >
                      <LogOut className="h-4 w-4" /> Logout
                    </button>
                  </div>
                </motion.div>
              )}
              </AnimatePresence>
            </div>
            )
          )}
          {headerAddon}
        </div>
      </div>
    </header>
  );
}