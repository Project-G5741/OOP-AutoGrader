import { describe, expect, it } from 'vitest';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import {
  getItemRarityKey,
  getRarityTheme,
  sortPullsForGenshinSummary,
} from '../../components/student/GachaCelestialStage';
import { clamp01 } from '../../components/student/GachaThreeScene';
import * as THREE from 'three';

describe('GachaCelestialStage rarity & Genshin summary helpers', () => {
  it('maps crystal, mascots, and point outcomes to distinct rarity keys and star ratings', () => {
    expect(getItemRarityKey({ type: 'crystal', rarity: 'crystal' })).toBe('crystal');
    expect(getRarityTheme({ type: 'crystal', rarity: 'crystal' }).stars).toBe(5);

    expect(getItemRarityKey({ type: 'mascot', rarity: 'legendary', mascotId: 'zerotwo' })).toBe('legendary');
    expect(getRarityTheme('legendary').stars).toBe(5);

    expect(getItemRarityKey({ type: 'mascot', rarity: 'epic', mascotId: 'thanh_long' })).toBe('epic');
    expect(getRarityTheme('epic').stars).toBe(4);

    expect(getItemRarityKey({ type: 'mascot', rarity: 'rare', mascotId: 'shiba' })).toBe('rare');
    expect(getItemRarityKey({ type: 'points', rarity: 'point', pointsRefund: 20 })).toBe('point');
    expect(getRarityTheme('point').stars).toBe(3);
  });

  it('sorts 10-pull summary with Crystal and Mascots ahead of points, preserving higher points first', () => {
    const pulls = [
      { item: { id: 'points_5', type: 'points', rarity: 'point', pointsRefund: 5 } },
      { item: { id: 'mascot_cat', type: 'mascot', rarity: 'rare', mascotId: 'cat' } },
      { item: { id: 'points_300', type: 'points', rarity: 'point', pointsRefund: 300 } },
      { item: { id: 'crystal', type: 'crystal', rarity: 'crystal', crystalAmount: 1 } },
      { item: { id: 'mascot_thanh_long', type: 'mascot', rarity: 'epic', mascotId: 'thanh_long' } },
    ];

    const sorted = sortPullsForGenshinSummary(pulls);
    expect(sorted.map((entry) => entry.pull.item.id)).toEqual([
      'crystal',
      'mascot_thanh_long',
      'mascot_cat',
      'points_300',
      'points_5',
    ]);
    expect(sorted[0].originalIndex).toBe(3);
  });

  it('clamps out-of-range rAF timestamps so CatmullRomCurve3 never throws on negative elapsed time', () => {
    expect(clamp01(-0.008)).toBe(0);
    expect(clamp01(NaN)).toBe(0);
    expect(clamp01(1.25)).toBe(1);

    const curve = new THREE.CatmullRomCurve3([
      new THREE.Vector3(19, 10.5, -34),
      new THREE.Vector3(9.5, 4.8, -16),
      new THREE.Vector3(3.2, 1.4, -5.5),
      new THREE.Vector3(0, 0, 0),
    ]);

    for (const raw of [-0.25, -0.002, 0, 0.34, 0.999, 1, 1.12]) {
      const safeU = clamp01(raw);
      expect(() => curve.getPointAt(safeU)).not.toThrow();
      expect(() => curve.getTangentAt(safeU)).not.toThrow();
    }
  });

  it('enforces zero CSS and SVG color-ramps inside Gacha components', () => {
    const files = [
      'src/components/student/MascotGachaModal.jsx',
      'src/components/student/GachaCelestialStage.jsx',
      'src/components/student/GachaThreeScene.jsx',
    ];
    const forbidden = /(linear-gradient|radial-gradient|conic-gradient|bg-gradient-|linearGradient|radialGradient)/;

    for (const rel of files) {
      const src = readFileSync(resolve(process.cwd(), rel), 'utf8');
      expect(src, `${rel} contains forbidden CSS/SVG ramp`).not.toMatch(forbidden);
    }
  });
});
