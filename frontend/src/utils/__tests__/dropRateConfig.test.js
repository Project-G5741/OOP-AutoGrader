import { describe, expect, it } from 'vitest';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import {
  buildEggSmashUpdatePayload,
  buildGachaUpdatePayload,
  DEFAULT_EGG_SMASH_CONFIG,
  DEFAULT_GACHA_CONFIG,
  eggSmashConfigToDraft,
  gachaConfigToDraft,
  rateToPercentString,
  validateEggSmashDraft,
  validateGachaDraft,
} from '../dropRateConfig';

describe('dropRateConfig helpers & Admin page invariants', () => {
  it('formats default Gacha and Egg Smash rates cleanly as percentages', () => {
    expect(rateToPercentString(0.0015)).toBe('0.15');
    expect(rateToPercentString(0.025)).toBe('2.5');
    expect(rateToPercentString(1 / 6000)).toBe('0.0167');
    expect(rateToPercentString(1 / 3)).toBe('33.3333');
  });

  it('preserves repeating-fraction server rates when untouched and converts edited fields', () => {
    const gachaDraft = gachaConfigToDraft(DEFAULT_GACHA_CONFIG);
    expect(validateGachaDraft(gachaDraft)).toMatchObject({
      valid: true,
      remainder5Pct: 31.5,
    });

    const unchangedPayload = buildGachaUpdatePayload(gachaDraft, DEFAULT_GACHA_CONFIG);
    expect(unchangedPayload.directLegendaryRate).toBe(DEFAULT_GACHA_CONFIG.directLegendaryRate);

    const editedPayload = buildGachaUpdatePayload(
      { ...gachaDraft, crystalBaseRate: '1.25' },
      DEFAULT_GACHA_CONFIG,
    );
    expect(editedPayload.crystalBaseRate).toBeCloseTo(0.0125, 8);
    expect(editedPayload.directLegendaryRate).toBe(DEFAULT_GACHA_CONFIG.directLegendaryRate);

    const eggDraft = eggSmashConfigToDraft(DEFAULT_EGG_SMASH_CONFIG);
    expect(eggDraft.baseCrystalHitRate).toBe('0');
    expect(eggDraft.turn6CrystalHitRate).toBe('0');
    expect(eggDraft.turn7CrystalHitRate).toBe('0');
    expect(validateEggSmashDraft(eggDraft)).toMatchObject({
      valid: true,
      remainder50Pct: 50,
    });
    const unchangedEgg = buildEggSmashUpdatePayload(eggDraft, DEFAULT_EGG_SMASH_CONFIG);
    expect(unchangedEgg.turn8CrystalHitRate).toBe(DEFAULT_EGG_SMASH_CONFIG.turn8CrystalHitRate);
  });

  it('rejects pool sums above 100% and invalid pity ordering', () => {
    const badGacha = {
      ...gachaConfigToDraft(DEFAULT_GACHA_CONFIG),
      softPityStart: '110',
      hardPityLimit: '100',
      cashbackRate300: '50',
      cashbackRate150: '40',
      cashbackRate60: '20',
    };
    const gachaRes = validateGachaDraft(badGacha);
    expect(gachaRes.valid).toBe(false);
    expect(gachaRes.errors.softPityStart).toBeDefined();
    expect(gachaRes.errors.cashbackPool).toBeDefined();

    const badEgg = {
      ...eggSmashConfigToDraft(DEFAULT_EGG_SMASH_CONFIG),
      eggRate300: '50',
      eggRate200: '40',
      eggRate100: '20',
    };
    const eggRes = validateEggSmashDraft(badEgg);
    expect(eggRes.valid).toBe(false);
    expect(eggRes.errors.eggPool).toBeDefined();
  });

  it('enforces zero CSS/SVG gradients in DropRateManagement and GachaManagement', () => {
    const dropRateSrc = readFileSync(
      resolve(process.cwd(), 'src/pages/DropRateManagement.jsx'),
      'utf8',
    );
    const gachaPageSrc = readFileSync(
      resolve(process.cwd(), 'src/pages/GachaManagement.jsx'),
      'utf8',
    );
    const forbidden = /(linear-gradient|radial-gradient|conic-gradient|bg-gradient-|linearGradient|radialGradient)/;
    expect(dropRateSrc).not.toMatch(forbidden);
    expect(gachaPageSrc).not.toMatch(forbidden);
  });
});
