import { describe, expect, it } from 'vitest';
import { MAJOR_HOLIDAY_DEFINITIONS } from '../holidayCalendar';
import {
  DefaultBackgroundScene,
  getHolidayBackgroundEntry,
  HOLIDAY_BACKGROUND_REGISTRY,
} from '../../components/auth/holidays/registry';

describe('Holiday Background & Login Popup Scene Registry', () => {
  it('registers background scene, login popup tokens, and session effects for every major holiday', () => {
    for (const def of MAJOR_HOLIDAY_DEFINITIONS) {
      const entry = HOLIDAY_BACKGROUND_REGISTRY[def.id];
      expect(entry, `Missing registry entry for ${def.id}`).toBeDefined();
      expect(entry.id).toBe(def.id);
      expect(typeof entry.kicker).toBe('string');
      expect(entry.kicker.length).toBeGreaterThan(0);
      expect(entry.KickerIcon).toBeDefined();
      expect(typeof entry.Scene).toBe('function');
      expect(entry.sessionEffects).toBeDefined();
      expect(entry.popupTokens?.light).toBeDefined();
      expect(entry.popupTokens?.dark).toBeDefined();
    }
  });

  it('falls back to DefaultBackgroundScene when no holiday is active', () => {
    const fallback = getHolidayBackgroundEntry(null);
    expect(fallback.id).toBe('default');
    expect(fallback.Scene).toBe(DefaultBackgroundScene);
    expect(fallback.sessionEffects).toBeNull();
  });
});
