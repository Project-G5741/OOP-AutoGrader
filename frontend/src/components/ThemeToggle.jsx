import { useCallback, useEffect, useId, useMemo, useRef, useState } from 'react';
import { AnimatePresence, motion, useReducedMotion } from 'framer-motion';
import {
  Check,
  ChevronRight,
  Flame,
  Leaf,
  MoonStar,
  Sun,
  Terminal,
  Waves,
} from 'lucide-react';
import { useTheme } from '../context/ThemeContext';
import './ThemeToggle.css';

const ORBS_PER_PAGE = 3;
const WHEEL_COOLDOWN_MS = 180;

const THEME_ICONS = {
  sun: Sun,
  moonStar: MoonStar,
  moon: MoonStar,
  sand: Waves,
  leaf: Leaf,
  terminal: Terminal,
  flame: Flame,
};

function renderThemeIcon(iconName, className = 'h-5 w-5') {
  const Icon = THEME_ICONS[iconName] ?? Sun;
  return <Icon className={className} strokeWidth={2.1} aria-hidden="true" />;
}

/**
 * Tight quarter-circle offsets (174deg -> 92deg, r=86) for up to 3 enlarged orbs
 * so they hug the trigger closely with zero empty gap.
 */
function computeArcOffsets(count) {
  if (count <= 1) return [{ x: -48, y: 48 }];
  if (count === 2) {
    return [
      { x: -82, y: 18 },
      { x: -24, y: 78 },
    ];
  }
  const startDeg = 174;
  const endDeg = 92;
  const radius = 86;
  return Array.from({ length: count }, (_, idx) => {
    const ratio = idx / (count - 1);
    const rad = ((startDeg + (endDeg - startDeg) * ratio) * Math.PI) / 180;
    return {
      x: Math.round(Math.cos(rad) * radius),
      y: Math.round(Math.sin(rad) * radius),
    };
  });
}

export default function ThemeToggle({ className = '' }) {
  const { theme, themeMeta, themes, setTheme } = useTheme();
  const [open, setOpen] = useState(false);
  const [page, setPage] = useState(0);
  const rootRef = useRef(null);
  const lastWheelRef = useRef(0);
  const menuId = useId();
  const reducedMotion = useReducedMotion();

  const totalPages = Math.max(1, Math.ceil(themes.length / ORBS_PER_PAGE));

  const visibleThemes = useMemo(() => {
    const start = page * ORBS_PER_PAGE;
    return themes.slice(start, start + ORBS_PER_PAGE);
  }, [themes, page]);

  const offsets = useMemo(
    () => computeArcOffsets(visibleThemes.length),
    [visibleThemes.length],
  );

  const cyclePage = useCallback(
    (step = 1) => {
      if (totalPages <= 1) return;
      setPage((prev) => (prev + step + totalPages) % totalPages);
    },
    [totalPages],
  );

  const handleToggleOpen = () => {
    if (!open) {
      const activeIdx = themes.findIndex((item) => item.id === theme);
      setPage(activeIdx >= 0 ? Math.floor(activeIdx / ORBS_PER_PAGE) : 0);
    }
    setOpen((prev) => !prev);
  };

  useEffect(() => {
    if (!open) return undefined;

    function handlePointerDown(event) {
      if (rootRef.current && !rootRef.current.contains(event.target)) {
        setOpen(false);
      }
    }

    function handleKeyDown(event) {
      if (event.key === 'Escape') {
        setOpen(false);
      } else if (event.key === 'ArrowRight' || event.key === 'PageDown') {
        event.preventDefault();
        cyclePage(1);
      } else if (event.key === 'ArrowLeft' || event.key === 'PageUp') {
        event.preventDefault();
        cyclePage(-1);
      }
    }

    const node = rootRef.current;
    function handleWheel(event) {
      if (totalPages <= 1) return;
      const dominantDelta =
        Math.abs(event.deltaY) >= Math.abs(event.deltaX) ? event.deltaY : event.deltaX;
      if (Math.abs(dominantDelta) < 4) return;
      event.preventDefault();
      const now = Date.now();
      if (now - lastWheelRef.current < WHEEL_COOLDOWN_MS) return;
      lastWheelRef.current = now;
      cyclePage(dominantDelta > 0 ? 1 : -1);
    }

    document.addEventListener('mousedown', handlePointerDown);
    document.addEventListener('keydown', handleKeyDown);
    node?.addEventListener('wheel', handleWheel, { passive: false });
    return () => {
      document.removeEventListener('mousedown', handlePointerDown);
      document.removeEventListener('keydown', handleKeyDown);
      node?.removeEventListener('wheel', handleWheel);
    };
  }, [open, totalPages, cyclePage]);

  const handleSelect = (themeId) => {
    setTheme(themeId);
    setOpen(false);
  };

  const pageSchemes = [...new Set(visibleThemes.map((p) => (p.colorScheme === 'dark' ? 'Dark' : 'Light')))];
  const groupHint = pageSchemes.length === 1 ? pageSchemes[0] : 'Themes';

  return (
    <div ref={rootRef} className={`u038 relative shrink-0 ${className}`.trim()}>
      <button
        type="button"
        className="theme-trigger"
        onClick={handleToggleOpen}
        aria-expanded={open}
        aria-controls={menuId}
        aria-haspopup="menu"
        aria-label={`Change theme (current: ${themeMeta.label})`}
        title={`Theme: ${themeMeta.label}`}
      >
        <span className="theme-trigger-icon">
          {renderThemeIcon(themeMeta.icon, 'h-5 w-5')}
        </span>
        <span className="theme-trigger-dot" aria-hidden="true" />
      </button>

      <AnimatePresence>
        {open && (
          <div id={menuId} role="menu" aria-label="Color themes" className="theme-arc-anchor">
            {visibleThemes.map((preset, index) => {
              const isSelected = preset.id === theme;
              const { x, y } = offsets[index];
              const schemeTag = preset.colorScheme === 'dark' ? 'Dark' : 'Light';
              const iconColor = preset.orbColor || preset.primary;

              return (
                <motion.button
                  key={`${page}-${preset.id}`}
                  type="button"
                  role="menuitemradio"
                  aria-checked={isSelected}
                  aria-label={`${preset.label} (${schemeTag})`}
                  onClick={() => handleSelect(preset.id)}
                  initial={reducedMotion ? false : { opacity: 0, x: 0, y: -4, scale: 0.6 }}
                  animate={{ opacity: 1, x, y, scale: 1 }}
                  exit={reducedMotion ? { opacity: 0 } : { opacity: 0, x: 0, y: -4, scale: 0.6 }}
                  transition={
                    reducedMotion
                      ? { duration: 0.01 }
                      : {
                          type: 'spring',
                          stiffness: 440,
                          damping: 26,
                          delay: index * 0.03,
                        }
                  }
                  whileHover={reducedMotion ? undefined : { scale: 1.1 }}
                  whileTap={reducedMotion ? undefined : { scale: 0.96 }}
                  className={`theme-orb group ${isSelected ? 'is-selected' : ''}`}
                  style={{
                    backgroundColor: preset.surface,
                    borderColor: isSelected ? iconColor : preset.border,
                    color: iconColor,
                    boxShadow: isSelected
                      ? `0 0 0 2px ${iconColor}40, 0 10px 22px rgb(var(--shadow-rgb) / 0.28)`
                      : `0 8px 18px rgb(var(--shadow-rgb) / 0.22)`,
                  }}
                >
                  <span
                    className="theme-orb-core"
                    style={{
                      backgroundColor: preset.background,
                      borderColor: preset.colorScheme === 'dark' ? iconColor : preset.navy,
                      color: iconColor,
                    }}
                  >
                    {renderThemeIcon(preset.icon, 'h-5 w-5')}
                  </span>

                  {isSelected && (
                    <span
                      className="theme-orb-check"
                      style={{
                        backgroundColor: preset.primary,
                        borderColor: preset.surface,
                      }}
                      aria-hidden="true"
                    >
                      <Check className="h-3 w-3 text-white" strokeWidth={3} />
                    </span>
                  )}

                  <span
                    className="theme-orb-label"
                    style={{
                      backgroundColor: preset.surface,
                      borderColor: preset.border,
                      color: preset.textPrimary,
                      boxShadow: `0 4px 12px rgb(var(--shadow-rgb) / 0.18)`,
                    }}
                  >
                    <span>{preset.label}</span>
                    <span style={{ opacity: 0.65 }}>{schemeTag}</span>
                  </span>
                </motion.button>
              );
            })}

            {totalPages > 1 && (
              <motion.button
                type="button"
                onClick={() => cyclePage(1)}
                initial={reducedMotion ? false : { opacity: 0, y: -4, scale: 0.9 }}
                animate={{ opacity: 1, y: 0, scale: 1 }}
                exit={reducedMotion ? { opacity: 0 } : { opacity: 0, y: -4, scale: 0.9 }}
                transition={reducedMotion ? { duration: 0.01 } : { duration: 0.16, delay: 0.06 }}
                className="theme-page-pill"
                aria-label={`Next theme group (page ${page + 1} of ${totalPages})`}
                title="Cuộn chuột hoặc bấm để xem 3 theme tiếp theo"
              >
                <span>{groupHint}</span>
                <span className="theme-page-count">
                  {page + 1}/{totalPages}
                </span>
                <ChevronRight className="h-3.5 w-3.5" aria-hidden="true" />
              </motion.button>
            )}
          </div>
        )}
      </AnimatePresence>
    </div>
  );
}
