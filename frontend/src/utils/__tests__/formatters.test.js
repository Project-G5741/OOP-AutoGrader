import { describe, expect, it } from 'vitest';
import { formatNumber, formatPercent } from '../formatters.js';

describe('formatNumber', () => {
  it('floors fractional scores by default', () => {
    expect(formatNumber(85.9)).toBe('85');
    expect(formatNumber(99.99)).toBe('99');
  });

  it('returns -- for nullish or NaN', () => {
    expect(formatNumber(null)).toBe('--');
    expect(formatNumber(undefined)).toBe('--');
    expect(formatNumber(Number.NaN)).toBe('--');
  });

  it('appends suffix for percent helper', () => {
    expect(formatPercent(72.8)).toBe('72%');
  });
});
