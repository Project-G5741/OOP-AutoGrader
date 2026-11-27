import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  LECTURER_NAV_TO_ROUTE,
  LECTURER_ROUTE_TO_NAV,
  ROUTES,
  clearAuthSession,
  defaultDashboardPath,
  hasRole,
  isAccessTokenCurrentlyValid,
  isInCurrentTerm,
  normalizeRoleList,
  persistAuthSession,
  readStoredUser,
} from '../authRoutes.js';

function jwtWithExp(expSeconds) {
  const header = btoa(JSON.stringify({ alg: 'none', typ: 'JWT' }));
  const payload = btoa(JSON.stringify({ exp: expSeconds }));
  return `${header}.${payload}.sig`;
}

describe('authRoutes', () => {
  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
  });

  afterEach(() => {
    vi.unstubAllEnvs();
    localStorage.clear();
    sessionStorage.clear();
  });

  it('rejects expired JWT', () => {
    const expired = jwtWithExp(Math.floor(Date.now() / 1000) - 60);
    expect(isAccessTokenCurrentlyValid(expired)).toBe(false);
  });

  it('accepts future JWT', () => {
    const live = jwtWithExp(Math.floor(Date.now() / 1000) + 3600);
    expect(isAccessTokenCurrentlyValid(live)).toBe(true);
  });

  it('clears session when readStoredUser sees expired token', () => {
    const expired = jwtWithExp(Math.floor(Date.now() / 1000) - 10);
    persistAuthSession(expired, { roles: ['STUDENT'], name: 'A' });
    expect(readStoredUser()).toBeNull();
    expect(localStorage.getItem('accessToken')).toBeNull();
  });

  it('treats missing inCurrentTerm as enrolled', () => {
    expect(isInCurrentTerm(undefined)).toBe(true);
    expect(isInCurrentTerm(null)).toBe(true);
    expect(isInCurrentTerm(false)).toBe(false);
  });

  it('normalizes TEACHER to LECTURER and maps default dashboards', () => {
    expect(normalizeRoleList(['TEACHER'])).toEqual(['LECTURER']);
    expect(hasRole(['TEACHER'], 'LECTURER')).toBe(true);
    expect(defaultDashboardPath(['LECTURER'])).toBe(ROUTES.lecturerDashboard);
    expect(defaultDashboardPath(['STUDENT'], true)).toBe(ROUTES.studentDashboard);
    expect(defaultDashboardPath(['STUDENT'], false)).toBe(ROUTES.studentHistory);
  });

  it('allows desktop-local token only in desktop mode', () => {
    vi.stubEnv('VITE_APP_MODE', 'web');
    expect(isAccessTokenCurrentlyValid('desktop-local')).toBe(false);
    vi.stubEnv('VITE_APP_MODE', 'desktop');
    expect(isAccessTokenCurrentlyValid('desktop-local')).toBe(true);
  });

  it('clearAuthSession removes durable keys', () => {
    persistAuthSession(jwtWithExp(Math.floor(Date.now() / 1000) + 60), {
      roles: ['STUDENT'],
    });
    clearAuthSession();
    expect(localStorage.getItem('accessToken')).toBeNull();
    expect(localStorage.getItem('user')).toBeNull();
  });

  it('maps unified Admin gacha nav item to /lecturer-gacha', () => {
    expect(ROUTES.lecturerGacha).toBe('/lecturer-gacha');
    expect(LECTURER_NAV_TO_ROUTE.gacha).toBe('/lecturer-gacha');
    expect(LECTURER_ROUTE_TO_NAV['/lecturer-gacha']).toBe('gacha');
  });
});
