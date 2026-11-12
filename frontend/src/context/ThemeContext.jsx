import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { getThemePreset, isValidThemeId, themeMetaList } from '../theme/tokens';

const STORAGE_KEY = 'oop-theme';
const THEME_TRANSITION_MS = 36;
const THEME_TRANSITION_CLASS = 'theme-transition';

const ThemeContext = createContext();

let themeTransitionTimeoutId;

function enableThemeTransition() {
  if (typeof document === 'undefined') return;
  const root = document.documentElement;
  root.classList.add(THEME_TRANSITION_CLASS);
  if (themeTransitionTimeoutId != null) {
    window.clearTimeout(themeTransitionTimeoutId);
  }
  themeTransitionTimeoutId = window.setTimeout(() => {
    root.classList.remove(THEME_TRANSITION_CLASS);
    themeTransitionTimeoutId = undefined;
  }, THEME_TRANSITION_MS);
}

function readSystemDark() {
  if (typeof window === 'undefined') return false;
  return window.matchMedia('(prefers-color-scheme: dark)').matches;
}

function readStoredTheme() {
  if (typeof window === 'undefined') return null;
  const stored = localStorage.getItem(STORAGE_KEY);
  return isValidThemeId(stored) ? stored : null;
}

function resolveInitialTheme() {
  return readStoredTheme() ?? (readSystemDark() ? 'dark' : 'light');
}

function applyThemeAttributes(themeId) {
  if (typeof document === 'undefined') return;
  const preset = getThemePreset(themeId);
  const root = document.documentElement;
  root.dataset.theme = preset.id;
  root.classList.toggle('dark', preset.colorScheme === 'dark');
}

// Apply synchronously before React mounts to prevent a flash of wrong theme.
applyThemeAttributes(resolveInitialTheme());

export function ThemeProvider({ children }) {
  const [theme, setThemeState] = useState(resolveInitialTheme);

  const preset = getThemePreset(theme);
  const isDark = preset.colorScheme === 'dark';

  useEffect(() => {
    applyThemeAttributes(preset.id);
  }, [preset.id]);

  useEffect(() => {
    const mq = window.matchMedia('(prefers-color-scheme: dark)');
    const onChange = () => {
      if (!readStoredTheme()) {
        setThemeState(mq.matches ? 'dark' : 'light');
      }
    };
    mq.addEventListener('change', onChange);
    return () => mq.removeEventListener('change', onChange);
  }, []);

  const setTheme = useCallback((nextId) => {
    const value = isValidThemeId(nextId) ? nextId : 'light';
    localStorage.setItem(STORAGE_KEY, value);
    enableThemeTransition();
    setThemeState(value);
  }, []);

  const toggleTheme = useCallback(() => {
    setTheme(isDark ? 'light' : 'dark');
  }, [isDark, setTheme]);

  const themeMeta = themeMetaList.find((item) => item.id === preset.id) ?? themeMetaList[0];

  return (
    <ThemeContext.Provider
      value={{
        theme: preset.id,
        themeMeta,
        themes: themeMetaList,
        isDark,
        setTheme,
        toggleTheme,
      }}
    >
      {children}
    </ThemeContext.Provider>
  );
}

export const useTheme = () => useContext(ThemeContext);
