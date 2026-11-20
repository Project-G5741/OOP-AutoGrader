import { describe, it, expect, beforeEach } from 'vitest';
import {
  readCrystalSourcedPoints,
  addCrystalSourcedPoints,
  consumeCrystalSourcedPointsForSpend,
  tagGachaCashbackAsCrystalSourced,
} from '../gachaCrystalSourcedPoints';

const KEY = 'gacha_crystal_sourced_points';

describe('gachaCrystalSourcedPoints', () => {
  beforeEach(() => {
    localStorage.removeItem(KEY);
  });

  it('tags exchange credits and consumes FIFO on spend', () => {
    addCrystalSourcedPoints(4000);
    expect(readCrystalSourcedPoints()).toBe(4000);

    const first = consumeCrystalSourcedPointsForSpend(100);
    expect(first).toEqual({ consumed: 100, balance: 3900 });
    expect(readCrystalSourcedPoints()).toBe(3900);

    const second = consumeCrystalSourcedPointsForSpend(5000);
    expect(second).toEqual({ consumed: 3900, balance: 0 });
    expect(readCrystalSourcedPoints()).toBe(0);
  });

  it('tags gacha cashback from crystal-blocked rolls', () => {
    tagGachaCashbackAsCrystalSourced(60);
    expect(readCrystalSourcedPoints()).toBe(60);
  });
});