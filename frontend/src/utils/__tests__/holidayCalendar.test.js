import { describe, expect, it } from 'vitest';
import { lunarToSolarIso } from '../vietnameseLunar';
import {
  getHolidayScheduleRowsForAdmin,
  MAJOR_HOLIDAY_DEFINITIONS,
  resolveActiveHolidayForDate,
} from '../holidayCalendar';

describe('vietnameseLunar & 7-major-holiday calendar engine', () => {
  it('converts Vietnamese Lunar dates (Tết Nguyên Đán & Trung Thu) accurately in UTC+7', () => {
    // 2026: Mùng 1 Tết Bính Ngọ = 2026-02-17; Rằm Trung Thu = 2026-09-25
    expect(lunarToSolarIso(1, 1, 2026)).toBe('2026-02-17');
    expect(lunarToSolarIso(15, 8, 2026)).toBe('2026-09-25');

    // 2027: Mùng 1 Tết Đinh Mùi = 2027-02-06; Rằm Trung Thu = 2027-09-15
    expect(lunarToSolarIso(1, 1, 2027)).toBe('2027-02-06');
    expect(lunarToSolarIso(15, 8, 2027)).toBe('2027-09-15');
  });

  it('defines the 7 major holidays', () => {
    const ids = MAJOR_HOLIDAY_DEFINITIONS.map((d) => d.id);
    expect(ids).toEqual([
      'new-year',
      'tet-nguyen-dan',
      'reunification-labor',
      'childrens-day',
      'mid-autumn',
      'halloween',
      'christmas',
    ]);
  });

  it('activates Halloween (31/10) across its 20-day pre-holiday window (11/10 -> 31/10)', () => {
    expect(resolveActiveHolidayForDate('2026-10-10')).toBeNull();
    expect(resolveActiveHolidayForDate('2026-10-11')?.id).toBe('halloween');
    expect(resolveActiveHolidayForDate('2026-10-31')?.id).toBe('halloween');
    expect(resolveActiveHolidayForDate('2026-11-01')).toBeNull();
  });

  it('activates Tết Lao Động (10/04 -> 01/05) and Tết Thiếu Nhi (12/05 -> 01/06)', () => {
    expect(resolveActiveHolidayForDate('2026-04-09')).toBeNull();
    expect(resolveActiveHolidayForDate('2026-04-10')?.id).toBe('reunification-labor');
    expect(resolveActiveHolidayForDate('2026-05-01')?.id).toBe('reunification-labor');
    expect(resolveActiveHolidayForDate('2026-05-02')).toBeNull();

    expect(resolveActiveHolidayForDate('2026-05-11')).toBeNull();
    expect(resolveActiveHolidayForDate('2026-05-12')?.id).toBe('childrens-day');
    expect(resolveActiveHolidayForDate('2026-06-01')?.id).toBe('childrens-day');
    expect(resolveActiveHolidayForDate('2026-06-02')).toBeNull();
  });

  it('hands off sequentially from Noel (04/12 -> 25/12) to Tết Dương Lịch (26/12 -> 01/01)', () => {
    expect(resolveActiveHolidayForDate('2026-12-03')).toBeNull();
    expect(resolveActiveHolidayForDate('2026-12-04')?.id).toBe('christmas');
    expect(resolveActiveHolidayForDate('2026-12-25')?.id).toBe('christmas');
    expect(resolveActiveHolidayForDate('2026-12-26')?.id).toBe('new-year');
    expect(resolveActiveHolidayForDate('2027-01-01')?.id).toBe('new-year');
    expect(resolveActiveHolidayForDate('2027-01-02')).toBeNull();
  });

  it('activates Tết Nguyên Đán and Tết Trung Thu 20 days prior by Lunar calendar dates', () => {
    // 2026 Tết: startDate = 2026-02-16 (29 Chạp), windowStart = 2026-01-27 (startDate - 20d), endDate = 2026-02-21 (Mùng 5)
    expect(resolveActiveHolidayForDate('2026-01-26')).toBeNull();
    expect(resolveActiveHolidayForDate('2026-01-27')?.id).toBe('tet-nguyen-dan');
    expect(resolveActiveHolidayForDate('2026-02-17')?.id).toBe('tet-nguyen-dan');
    expect(resolveActiveHolidayForDate('2026-02-21')?.id).toBe('tet-nguyen-dan');
    expect(resolveActiveHolidayForDate('2026-02-22')).toBeNull();

    // 2026 Trung Thu: 14/08 ÂL = 2026-09-24 (windowStart = 2026-09-04), 15/08 ÂL = 2026-09-25
    expect(resolveActiveHolidayForDate('2026-09-03')).toBeNull();
    expect(resolveActiveHolidayForDate('2026-09-04')?.id).toBe('mid-autumn');
    expect(resolveActiveHolidayForDate('2026-09-25')?.id).toBe('mid-autumn');
    expect(resolveActiveHolidayForDate('2026-09-26')).toBeNull();
  });

  it('supports Admin early holiday activation and turns off automatically once endDate passes', () => {
    const adminConfig = {
      schedules: {
        christmas: {
          enabledEarly: true,
          earlyStartDate: '2026-11-24',
          endDate: '2026-12-25',
          activatedAt: 1000,
        },
      },
    };

    // Before earlyStartDate -> null
    expect(resolveActiveHolidayForDate('2026-11-23', adminConfig)).toBeNull();

    // From earlyStartDate through endDate -> active with isEarlyOverride = true
    const activeEarly = resolveActiveHolidayForDate('2026-11-24', adminConfig);
    expect(activeEarly?.id).toBe('christmas');
    expect(activeEarly?.isEarlyOverride).toBe(true);
    expect(resolveActiveHolidayForDate('2026-12-25', adminConfig)?.id).toBe('christmas');

    // Once endDate passes (2026-12-26 > 2026-12-25) -> Christmas turns off automatically and hands off to New Year
    const afterChristmas = resolveActiveHolidayForDate('2026-12-26', adminConfig);
    expect(afterChristmas?.id).toBe('new-year');
    expect(afterChristmas?.isEarlyOverride).toBeUndefined();

    // And after New Year (2027-01-02), no holiday is active
    expect(resolveActiveHolidayForDate('2027-01-02', adminConfig)).toBeNull();
  });

  it('marks expired Admin early schedules as inactive in getHolidayScheduleRowsForAdmin', () => {
    const adminConfig = {
      schedules: {
        halloween: {
          enabledEarly: true,
          earlyStartDate: '2026-10-01',
          endDate: '2026-10-31',
          activatedAt: 500,
        },
      },
    };

    const rowsDuring = getHolidayScheduleRowsForAdmin('2026-10-05', adminConfig);
    const hwDuring = rowsDuring.find((r) => r.id === 'halloween');
    expect(hwDuring?.enabledEarly).toBe(true);
    expect(hwDuring?.isActiveEarly).toBe(true);

    const rowsAfter = getHolidayScheduleRowsForAdmin('2026-11-01', adminConfig);
    const hwAfter = rowsAfter.find((r) => r.id === 'halloween');
    expect(hwAfter?.isExpiredEarly).toBe(true);
    expect(hwAfter?.enabledEarly).toBe(false);
    expect(hwAfter?.isActiveEarly).toBe(false);
  });
});
