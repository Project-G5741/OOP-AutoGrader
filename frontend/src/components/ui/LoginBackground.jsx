import React, { useEffect, useRef } from 'react';
import { useTheme } from '../../context/ThemeContext';
import { useActiveHoliday } from '../../utils/holidayCalendar';
import { getHolidayBackgroundEntry } from '../auth/holidays/registry';

const POPUP_TOKEN_KEYS = [
  '--login-kicker-bg',
  '--login-kicker-fg',
  '--login-accent',
  '--login-accent-hover',
  '--login-logo-bg',
  '--login-hero-bg',
  '--login-hero-text',
  '--login-card-border',
  '--login-card-shadow',
];

export default function LoginBackground() {
  const containerRef = useRef(null);
  const { isDark } = useTheme();
  const activeHoliday = useActiveHoliday();
  const entry = getHolidayBackgroundEntry(activeHoliday?.id);
  const SceneComponent = entry.Scene;

  useEffect(() => {
    const host = containerRef.current?.closest('.login-bg');
    if (!host) return undefined;
    const tokens =
      (isDark ? entry.popupTokens?.dark : entry.popupTokens?.light) || {};

    for (const key of POPUP_TOKEN_KEYS) {
      if (tokens[key]) {
        host.style.setProperty(key, tokens[key]);
      } else {
        host.style.removeProperty(key);
      }
    }
    host.dataset.holiday = entry.id;

    return () => {
      for (const key of POPUP_TOKEN_KEYS) {
        host.style.removeProperty(key);
      }
      delete host.dataset.holiday;
    };
  }, [entry, isDark]);

  return (
    <div
      ref={containerRef}
      className="login-bg-ornaments"
      data-holiday-bg={entry.id}
      aria-hidden="true"
    >
      <SceneComponent />
    </div>
  );
}
