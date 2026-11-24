import { useEffect, useState } from 'react';
import { addDaysIso, formatIsoDate, lunarToSolarIso, parseIsoDate } from './vietnameseLunar';
import { isDesktopLocalMode } from './desktopMode';
import { apiFetch } from './apiFetch';
import { authHeaders } from './authHeaders';

/**
 * Holiday Calendar Engine for the 7 Major Vietnamese & Campus Holidays:
 * 1. Halloween (31/10)
 * 2. Noel / Giáng Sinh (24/12 - 25/12)
 * 3. Tết Dương Lịch (01/01)
 * 4. Tết Nguyên Đán (29/12 ÂL - 05/01 ÂL)
 * 5. Tết Lao Động (30/04 - 01/05)
 * 6. Tết Thiếu Nhi (01/06)
 * 7. Tết Trung Thu (14/08 ÂL - 15/08 ÂL)
 *
 * Default window rule: [startDate - 20 days, endDate].
 * Admin early activation rule: Admin can enable a holiday early from `earlyStartDate`
 * through `endDate`; once `todayIso > endDate`, the early activation turns off automatically.
 */

export const HOLIDAY_WINDOW_PRE_DAYS = 20;
export const HOLIDAY_ADMIN_CONFIG_KEY = 'oop-holiday-admin-config';
export const HOLIDAY_DEV_OVERRIDE_KEY = 'oop-dev-holiday-override';
export const HOLIDAY_PREVIEW_CHANGE_EVENT = 'oop-holiday-preview-change';

const API_BASE = import.meta.env?.VITE_API_URL || 'http://localhost:8002';

export const MAJOR_HOLIDAY_DEFINITIONS = [
  {
    id: 'new-year',
    name: 'Tết Dương Lịch',
    dateLabel: '01/01',
    presetId: 'light',
    priority: 90,
    resolveDates: (year) => ({
      startDate: formatIsoDate(year, 1, 1),
      endDate: formatIsoDate(year, 1, 1),
    }),
  },
  {
    id: 'tet-nguyen-dan',
    name: 'Tết Nguyên Đán',
    dateLabel: '29 Chạp – Mùng 5',
    presetId: 'scarlet',
    priority: 100,
    resolveDates: (year) => {
      const mung1 = lunarToSolarIso(1, 1, year);
      const mung5 = lunarToSolarIso(5, 1, year);
      const startDate = addDaysIso(mung1, -1);
      return { startDate, endDate: mung5 };
    },
  },
  {
    id: 'reunification-labor',
    name: 'Tết Lao Động',
    dateLabel: '30/04 – 01/05',
    presetId: 'scarlet',
    priority: 95,
    resolveDates: (year) => ({
      startDate: formatIsoDate(year, 4, 30),
      endDate: formatIsoDate(year, 5, 1),
    }),
  },
  {
    id: 'childrens-day',
    name: 'Tết Thiếu Nhi',
    dateLabel: '01/06',
    presetId: 'light',
    priority: 85,
    resolveDates: (year) => ({
      startDate: formatIsoDate(year, 6, 1),
      endDate: formatIsoDate(year, 6, 1),
    }),
  },
  {
    id: 'mid-autumn',
    name: 'Tết Trung Thu',
    dateLabel: '15/08 ÂL',
    presetId: 'sand',
    priority: 92,
    resolveDates: (year) => ({
      startDate: lunarToSolarIso(14, 8, year),
      endDate: lunarToSolarIso(15, 8, year),
    }),
  },
  {
    id: 'halloween',
    name: 'Halloween',
    dateLabel: '31/10',
    presetId: 'dark',
    priority: 88,
    resolveDates: (year) => ({
      startDate: formatIsoDate(year, 10, 31),
      endDate: formatIsoDate(year, 10, 31),
    }),
  },
  {
    id: 'christmas',
    name: 'Noel · Giáng Sinh',
    dateLabel: '24/12 – 25/12',
    presetId: 'evergreen',
    priority: 94,
    resolveDates: (year) => ({
      startDate: formatIsoDate(year, 12, 24),
      endDate: formatIsoDate(year, 12, 25),
    }),
  },
];

function isIsoDate(str) {
  return typeof str === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(str);
}

export function getLocalVietnamDateIso(now = new Date()) {
  try {
    const fmt = new Intl.DateTimeFormat('en-CA', {
      timeZone: 'Asia/Ho_Chi_Minh',
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
    });
    const formatted = fmt.format(now);
    if (isIsoDate(formatted)) {
      return formatted;
    }
  } catch {
    // Fallback to UTC+7 math
  }
  const utcMs = now.getTime() + now.getTimezoneOffset() * 60000;
  const vnDate = new Date(utcMs + 7 * 3600000);
  return formatIsoDate(vnDate.getFullYear(), vnDate.getMonth() + 1, vnDate.getDate());
}

export function normalizeAdminHolidayConfig(raw) {
  if (!raw || typeof raw !== 'object') {
    return { schedules: {} };
  }
  const rawSchedules =
    raw.schedules && typeof raw.schedules === 'object' ? raw.schedules : {};
  const schedules = {};
  for (const def of MAJOR_HOLIDAY_DEFINITIONS) {
    const item = rawSchedules[def.id];
    if (!item || typeof item !== 'object') continue;
    const preDaysNum = Number(item.preDays);
    const preDays =
      Number.isFinite(preDaysNum) && preDaysNum >= 1 && preDaysNum <= 90
        ? Math.round(preDaysNum)
        : HOLIDAY_WINDOW_PRE_DAYS;
    schedules[def.id] = {
      enabledEarly: Boolean(item.enabledEarly),
      earlyStartDate: isIsoDate(item.earlyStartDate) ? item.earlyStartDate : null,
      endDate: isIsoDate(item.endDate) ? item.endDate : null,
      preDays,
      activatedAt: Number.isFinite(Number(item.activatedAt))
        ? Number(item.activatedAt)
        : 0,
    };
  }
  return { schedules };
}

export function readAdminHolidayConfig() {
  if (typeof window === 'undefined') {
    return { schedules: {} };
  }
  try {
    const raw = localStorage.getItem(HOLIDAY_ADMIN_CONFIG_KEY);
    if (!raw) return { schedules: {} };
    return normalizeAdminHolidayConfig(JSON.parse(raw));
  } catch {
    return { schedules: {} };
  }
}

export function writeAdminHolidayConfig(nextConfig) {
  const normalized = normalizeAdminHolidayConfig(nextConfig);
  if (typeof window !== 'undefined') {
    try {
      localStorage.setItem(HOLIDAY_ADMIN_CONFIG_KEY, JSON.stringify(normalized));
      sessionStorage.removeItem(HOLIDAY_DEV_OVERRIDE_KEY);
      window.dispatchEvent(new CustomEvent(HOLIDAY_PREVIEW_CHANGE_EVENT));
    } catch {
      // ignore storage quota errors
    }
  }
  return normalized;
}

export async function fetchAdminHolidayConfigFromServer() {
  if (isDesktopLocalMode() || typeof window === 'undefined') {
    return readAdminHolidayConfig();
  }
  try {
    const response = await fetch(`${API_BASE}/api/auth/holiday-config`, {
      method: 'GET',
      headers: { Accept: 'application/json' },
    });
    if (!response.ok) {
      return readAdminHolidayConfig();
    }
    const data = await response.json();
    if (data && typeof data === 'object' && data.schedules) {
      const normalized = normalizeAdminHolidayConfig(data);
      const prevRaw = localStorage.getItem(HOLIDAY_ADMIN_CONFIG_KEY);
      const nextRaw = JSON.stringify(normalized);
      if (prevRaw !== nextRaw) {
        localStorage.setItem(HOLIDAY_ADMIN_CONFIG_KEY, nextRaw);
        window.dispatchEvent(new CustomEvent(HOLIDAY_PREVIEW_CHANGE_EVENT));
      }
      return normalized;
    }
  } catch {
    // Fallback to localStorage config when offline or server not restarted
  }
  return readAdminHolidayConfig();
}

export async function saveAdminHolidayConfigToServer(nextConfig) {
  const normalized = writeAdminHolidayConfig(nextConfig);
  if (isDesktopLocalMode()) {
    return { config: normalized, persistedRemote: false };
  }
  try {
    const response = await apiFetch(`${API_BASE}/api/lecturer/holiday-config`, {
      method: 'PUT',
      headers: {
        ...authHeaders(),
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(normalized),
    });
    if (response.ok) {
      const saved = await response.json();
      const synced = writeAdminHolidayConfig(saved);
      return { config: synced, persistedRemote: true };
    }
  } catch {
    // Local storage already updated
  }
  return { config: normalized, persistedRemote: false };
}

export function expandHolidayOccurrencesForYear(year, adminConfig = null) {
  const schedules = adminConfig?.schedules || {};
  return MAJOR_HOLIDAY_DEFINITIONS.map((def) => {
    const { startDate, endDate } = def.resolveDates(year);
    const customPreDays = schedules[def.id]?.preDays;
    const preDays =
      Number.isFinite(customPreDays) && customPreDays >= 1
        ? customPreDays
        : HOLIDAY_WINDOW_PRE_DAYS;
    const windowStart = addDaysIso(startDate, -preDays);
    return {
      id: def.id,
      name: def.name,
      dateLabel: def.dateLabel,
      presetId: def.presetId,
      priority: def.priority,
      holidayYear: year,
      occurrenceKey: `${year}:${def.id}`,
      preDays,
      windowStart,
      startDate,
      endDate,
    };
  });
}

/**
 * Find the current or next upcoming occurrence of a holiday relative to `referenceIsoDate`
 * (first occurrence whose official `endDate >= referenceIsoDate`).
 */
export function resolveUpcomingHolidayOccurrence(
  holidayId,
  referenceIsoDate = getLocalVietnamDateIso(),
  adminConfig = null
) {
  const { year } = parseIsoDate(referenceIsoDate);
  const all = [
    ...expandHolidayOccurrencesForYear(year - 1, adminConfig),
    ...expandHolidayOccurrencesForYear(year, adminConfig),
    ...expandHolidayOccurrencesForYear(year + 1, adminConfig),
  ]
    .filter((item) => item.id === holidayId && item.endDate >= referenceIsoDate)
    .sort((a, b) => (a.endDate < b.endDate ? -1 : 1));
  return all[0] || getHolidayById(holidayId, year, adminConfig);
}

/**
 * Resolve which major holiday is active on a given ISO date ('YYYY-MM-DD').
 * - First checks Admin early-activated holidays (`enabledEarly: true`):
 *   active when `earlyStartDate <= isoDate <= endDate`.
 *   Once `isoDate > endDate`, the early activation turns off automatically!
 * - Otherwise falls back to the automatic calendar window `[startDate - preDays, endDate]`.
 */
export function resolveActiveHolidayForDate(isoDate, adminConfig = null) {
  if (!isIsoDate(isoDate)) {
    return null;
  }
  const normalizedAdmin = adminConfig
    ? normalizeAdminHolidayConfig(adminConfig)
    : null;
  const schedules = normalizedAdmin?.schedules || {};

  // 1. Check Admin early-set holidays (auto-expires when isoDate > endDate)
  const earlyCandidates = [];
  for (const def of MAJOR_HOLIDAY_DEFINITIONS) {
    const sched = schedules[def.id];
    if (!sched || !sched.enabledEarly) continue;

    const upcoming = resolveUpcomingHolidayOccurrence(
      def.id,
      sched.earlyStartDate || isoDate,
      normalizedAdmin
    );
    const effectiveEndDate = isIsoDate(sched.endDate)
      ? sched.endDate
      : upcoming.endDate;
    const effectiveEarlyStart = isIsoDate(sched.earlyStartDate)
      ? sched.earlyStartDate
      : upcoming.windowStart;

    // Auto-off rule: once isoDate passes effectiveEndDate, this early setting is automatically off
    if (isoDate > effectiveEndDate) {
      continue;
    }
    if (effectiveEarlyStart <= isoDate && isoDate <= effectiveEndDate) {
      earlyCandidates.push({
        ...upcoming,
        windowStart: effectiveEarlyStart,
        endDate: effectiveEndDate,
        isEarlyOverride: true,
        activatedAt: sched.activatedAt || 0,
      });
    }
  }

  if (earlyCandidates.length > 0) {
    earlyCandidates.sort((a, b) => {
      if (a.activatedAt !== b.activatedAt) {
        return b.activatedAt - a.activatedAt;
      }
      if (a.endDate !== b.endDate) {
        return a.endDate < b.endDate ? -1 : 1;
      }
      return b.priority - a.priority;
    });
    return earlyCandidates[0];
  }

  // 2. Standard automatic schedule [startDate - preDays, endDate]
  const { year } = parseIsoDate(isoDate);
  const candidates = [
    ...expandHolidayOccurrencesForYear(year - 1, normalizedAdmin),
    ...expandHolidayOccurrencesForYear(year, normalizedAdmin),
    ...expandHolidayOccurrencesForYear(year + 1, normalizedAdmin),
  ].filter((item) => item.windowStart <= isoDate && isoDate <= item.endDate);

  if (candidates.length === 0) {
    return null;
  }

  candidates.sort((a, b) => {
    if (a.startDate !== b.startDate) {
      return a.startDate < b.startDate ? -1 : 1;
    }
    return b.priority - a.priority;
  });

  return candidates[0];
}

export function getHolidayById(holidayId, referenceYear = 2026, adminConfig = null) {
  const def = MAJOR_HOLIDAY_DEFINITIONS.find((h) => h.id === holidayId);
  if (!def) return null;
  const { startDate, endDate } = def.resolveDates(referenceYear);
  const customPreDays = adminConfig?.schedules?.[def.id]?.preDays;
  const preDays =
    Number.isFinite(customPreDays) && customPreDays >= 1
      ? customPreDays
      : HOLIDAY_WINDOW_PRE_DAYS;
  return {
    id: def.id,
    name: def.name,
    dateLabel: def.dateLabel,
    presetId: def.presetId,
    priority: def.priority,
    holidayYear: referenceYear,
    occurrenceKey: `${referenceYear}:${def.id}`,
    preDays,
    windowStart: addDaysIso(startDate, -preDays),
    startDate,
    endDate,
  };
}

/**
 * Build full status rows for all 7 major holidays for the Admin Holidays management tab.
 */
export function getHolidayScheduleRowsForAdmin(
  referenceIsoDate = getLocalVietnamDateIso(),
  adminConfig = readAdminHolidayConfig()
) {
  const normalized = normalizeAdminHolidayConfig(adminConfig);
  const activeToday = resolveActiveHolidayForDate(referenceIsoDate, normalized);

  return MAJOR_HOLIDAY_DEFINITIONS.map((def) => {
    const upcoming = resolveUpcomingHolidayOccurrence(
      def.id,
      referenceIsoDate,
      normalized
    );
    const sched = normalized.schedules[def.id] || null;
    const preDays = sched?.preDays || HOLIDAY_WINDOW_PRE_DAYS;

    // Check if an existing early setting has already expired (referenceIsoDate > sched.endDate)
    const isExpiredEarly = Boolean(
      sched?.enabledEarly && isIsoDate(sched.endDate) && referenceIsoDate > sched.endDate
    );
    const enabledEarly = Boolean(sched?.enabledEarly && !isExpiredEarly);

    const earlyStartDate =
      enabledEarly && isIsoDate(sched?.earlyStartDate)
        ? sched.earlyStartDate
        : referenceIsoDate;
    const effectiveEndDate =
      enabledEarly && isIsoDate(sched?.endDate)
        ? sched.endDate
        : upcoming.endDate;

    const isActiveEarly =
      enabledEarly &&
      earlyStartDate <= referenceIsoDate &&
      referenceIsoDate <= effectiveEndDate;
    const isScheduledEarly =
      enabledEarly &&
      earlyStartDate > referenceIsoDate &&
      referenceIsoDate <= effectiveEndDate;
    const isActiveAuto =
      !enabledEarly &&
      upcoming.windowStart <= referenceIsoDate &&
      referenceIsoDate <= upcoming.endDate;
    const isCurrentlyDisplaying = activeToday?.id === def.id;

    return {
      id: def.id,
      name: def.name,
      dateLabel: def.dateLabel,
      presetId: def.presetId,
      officialStartDate: upcoming.startDate,
      officialEndDate: upcoming.endDate,
      autoWindowStart: upcoming.windowStart,
      preDays,
      enabledEarly,
      isExpiredEarly,
      earlyStartDate,
      endDate: effectiveEndDate,
      isActiveEarly,
      isScheduledEarly,
      isActiveAuto,
      isCurrentlyDisplaying,
    };
  });
}

/**
 * Resolve the currently active holiday on Web (returns null on Offline Desktop).
 * Uses Admin early schedule config + automatic 20-day calendar window.
 */
export function resolveCurrentActiveHoliday() {
  if (isDesktopLocalMode()) {
    return null;
  }

  const todayIso = getLocalVietnamDateIso();
  const { year } = parseIsoDate(todayIso);
  const adminConfig = readAdminHolidayConfig();

  if (typeof window !== 'undefined' && import.meta.env?.DEV) {
    try {
      const params = new URLSearchParams(window.location.search);
      const qHoliday = params.get('holiday');
      const qDate = params.get('holidayDate');
      if (qHoliday === 'none' || qHoliday === 'default') {
        return null;
      }
      if (qHoliday) {
        const matched = getHolidayById(qHoliday, year, adminConfig);
        if (matched) return matched;
      }
      if (qDate && isIsoDate(qDate)) {
        return resolveActiveHolidayForDate(qDate, adminConfig);
      }

      const storedOverride = sessionStorage.getItem(HOLIDAY_DEV_OVERRIDE_KEY);
      if (storedOverride === 'none' || storedOverride === 'default') {
        return null;
      }
      if (storedOverride && storedOverride.startsWith('date:')) {
        const d = storedOverride.slice(5);
        return resolveActiveHolidayForDate(d, adminConfig);
      }
      if (storedOverride) {
        const matched = getHolidayById(storedOverride, year, adminConfig);
        if (matched) return matched;
      }
    } catch {
      // ignore storage errors
    }
  }

  return resolveActiveHolidayForDate(todayIso, adminConfig);
}

export function setDevHolidayOverride(value) {
  if (typeof window === 'undefined') return;
  try {
    if (!value || value === 'auto') {
      sessionStorage.removeItem(HOLIDAY_DEV_OVERRIDE_KEY);
    } else {
      sessionStorage.setItem(HOLIDAY_DEV_OVERRIDE_KEY, value);
    }
    window.dispatchEvent(new CustomEvent(HOLIDAY_PREVIEW_CHANGE_EVENT));
  } catch {
    // ignore
  }
}

export function getDevHolidayOverride() {
  if (typeof window === 'undefined') return 'auto';
  try {
    const params = new URLSearchParams(window.location.search);
    if (params.get('holiday')) return params.get('holiday');
    return sessionStorage.getItem(HOLIDAY_DEV_OVERRIDE_KEY) || 'auto';
  } catch {
    return 'auto';
  }
}

let hasFetchedRemoteConfigThisSession = false;

export function useActiveHoliday() {
  const [activeHoliday, setActiveHoliday] = useState(() =>
    resolveCurrentActiveHoliday()
  );

  useEffect(() => {
    const syncHoliday = () => {
      setActiveHoliday(resolveCurrentActiveHoliday());
    };
    const handleVisibilityChange = () => {
      if (document.visibilityState === 'visible') {
        syncHoliday();
      }
    };
    const handleStorageChange = (e) => {
      if (!e.key || e.key === HOLIDAY_ADMIN_CONFIG_KEY) {
        syncHoliday();
      }
    };

    if (!hasFetchedRemoteConfigThisSession && !isDesktopLocalMode()) {
      hasFetchedRemoteConfigThisSession = true;
      void fetchAdminHolidayConfigFromServer();
    }

    window.addEventListener(HOLIDAY_PREVIEW_CHANGE_EVENT, syncHoliday);
    window.addEventListener('popstate', syncHoliday);
    window.addEventListener('focus', syncHoliday);
    window.addEventListener('storage', handleStorageChange);
    document.addEventListener('visibilitychange', handleVisibilityChange);
    return () => {
      window.removeEventListener(HOLIDAY_PREVIEW_CHANGE_EVENT, syncHoliday);
      window.removeEventListener('popstate', syncHoliday);
      window.removeEventListener('focus', syncHoliday);
      window.removeEventListener('storage', handleStorageChange);
      document.removeEventListener('visibilitychange', handleVisibilityChange);
    };
  }, []);

  return activeHoliday;
}
