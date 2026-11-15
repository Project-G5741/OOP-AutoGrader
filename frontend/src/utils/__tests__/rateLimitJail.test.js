import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ROUTES } from '../authRoutes.js';
import {
  readRetryAfterSeconds,
  storePriorLocation,
  takePriorLocation,
} from '../rateLimitJail.js';

describe('rateLimitJail', () => {
  beforeEach(() => {
    sessionStorage.clear();
  });

  afterEach(() => {
    sessionStorage.clear();
  });

  it('reads Retry-After seconds with fallback', () => {
    expect(readRetryAfterSeconds({ headers: { get: () => '12' } })).toBe(12);
    expect(readRetryAfterSeconds({ headers: { get: () => null } }, 5)).toBe(5);
    expect(readRetryAfterSeconds({ headers: { get: () => '0' } }, 5)).toBe(5);
  });

  it('stores and takes a safe prior path', () => {
    storePriorLocation('/student-dashboard', '?lab=1');
    expect(takePriorLocation()).toBe('/student-dashboard?lab=1');
    expect(takePriorLocation(ROUTES.login)).toBe(ROUTES.login);
  });

  it('rejects absolute or jail paths', () => {
    storePriorLocation('//evil.example', '');
    expect(takePriorLocation(ROUTES.login)).toBe(ROUTES.login);
    storePriorLocation(ROUTES.rateLimited, '');
    expect(takePriorLocation(ROUTES.login)).toBe(ROUTES.login);
  });
});
