import { describe, expect, it } from 'vitest';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import {
  MYTHIC_SKILLS,
  AUTO_SKILL_INTERVALS,
  DEFAULT_MYTHIC_SKILL_PREFS,
  MYTHIC_SKILL_PREF_KEY,
  getMythicSkills,
  pickRandomViewportTarget,
  normalizeMythicSkillPrefs,
  readMythicSkillPrefs,
  writeMythicSkillPrefs,
  sampleAutoSkillDelay,
  pickNextAutoSkill,
  createSkillInstance,
  stepSkillInstance,
  drawSkillInstance,
} from './mythicSkillEffects';

function createMockCanvasCtx() {
  const noop = () => {};
  return {
    globalAlpha: 1,
    fillStyle: '#000000',
    strokeStyle: '#000000',
    lineWidth: 1,
    lineCap: 'butt',
    lineJoin: 'miter',
    save: noop,
    restore: noop,
    translate: noop,
    rotate: noop,
    scale: noop,
    beginPath: noop,
    closePath: noop,
    moveTo: noop,
    lineTo: noop,
    arc: noop,
    bezierCurveTo: noop,
    quadraticCurveTo: noop,
    fillRect: noop,
    strokeRect: noop,
    fill: noop,
    stroke: noop,
    setLineDash: noop,
  };
}

describe('mythicSkillEffects', () => {
  it('returns 2 signature skills for each Mythic mascot (including hyphenated slugs) and empty for non-Mythic', () => {
    expect(getMythicSkills('kiem_de').map((s) => s.id)).toEqual(['van_kiem', 'nhat_kiem']);
    expect(getMythicSkills('kiem-de').map((s) => s.id)).toEqual(['van_kiem', 'nhat_kiem']);
    expect(getMythicSkills('cyber_striker').map((s) => s.id)).toEqual(['mua_ten_lua', 'phao_quy_dao']);
    expect(getMythicSkills('floral_fairy').map((s) => s.id)).toEqual(['bach_hoa', 'linh_lan_thanh_vuc']);
    expect(getMythicSkills('chrono_celestia').map((s) => s.id)).toEqual([
      'thoi_khong_ngung_dong',
      'thien_ha_tinh_vuc',
    ]);

    expect(getMythicSkills('fox')).toEqual([]);
    expect(getMythicSkills('zerotwo')).toEqual([]);
    expect(getMythicSkills('')).toEqual([]);
  });

  it('simulates and draws every Mythic skill cleanly across its full lifecycle', () => {
    const ctx = createMockCanvasCtx();
    const allSkills = Object.values(MYTHIC_SKILLS).flat();
    expect(allSkills).toHaveLength(8);

    for (const skill of allSkills) {
      const pet = {
        x: 360,
        y: 260,
        reactIdx: 4,
        reactTimer: 0,
        dashTimer: 0,
      };
      const inst = createSkillInstance(skill.id, {
        originX: pet.x,
        originY: pet.y,
        targetX: 880,
        targetY: 420,
        viewW: 1280,
        viewH: 720,
        reducedMotion: false,
      });

      let alive = true;
      let steps = 0;
      const dt = 0.05;
      while (steps * dt < 3.5) {
        alive = stepSkillInstance(inst, dt, pet, 1280, 720);
        expect(Number.isFinite(inst.shakeX)).toBe(true);
        expect(Number.isFinite(inst.shakeY)).toBe(true);
        expect(Number.isFinite(pet.x)).toBe(true);
        expect(Number.isFinite(pet.y)).toBe(true);
        expect(() => drawSkillInstance(ctx, inst, 'back', pet, 1280, 720)).not.toThrow();
        expect(() => drawSkillInstance(ctx, inst, 'front', pet, 1280, 720)).not.toThrow();
        steps += 1;
      }

      expect(alive, `${skill.id} should complete before 3.5s`).toBe(false);
    }
  });

  it('samples varied random viewport targets inside bounds without fixed coordinates', () => {
    const viewW = 1280;
    const viewH = 720;
    const targets = Array.from({ length: 30 }, () =>
      pickRandomViewportTarget(360, 260, viewW, viewH),
    );

    const uniqueX = new Set(targets.map((pt) => Math.round(pt.x)));
    const uniqueY = new Set(targets.map((pt) => Math.round(pt.y)));
    expect(uniqueX.size).toBeGreaterThan(10);
    expect(uniqueY.size).toBeGreaterThan(10);
    for (const pt of targets) {
      expect(pt.x).toBeGreaterThanOrEqual(120);
      expect(pt.x).toBeLessThanOrEqual(viewW - 120);
      expect(pt.y).toBeGreaterThanOrEqual(110);
      expect(pt.y).toBeLessThanOrEqual(viewH - 110);
    }

    const vanCasts = Array.from({ length: 12 }, () =>
      createSkillInstance('van_kiem', { originX: 360, originY: 260, viewW, viewH }),
    );
    expect(new Set(vanCasts.map((c) => Math.round(c.tx))).size).toBeGreaterThan(4);

    const cleaveCasts = Array.from({ length: 12 }, () =>
      createSkillInstance('nhat_kiem', { originX: 360, originY: 260, viewW, viewH }),
    );
    expect(new Set(cleaveCasts.map((c) => Math.round(c.slashCx))).size).toBeGreaterThan(4);
    expect(new Set(cleaveCasts.map((c) => Math.round(c.slashAngle * 100))).size).toBeGreaterThan(4);
  });

  it('normalizes, persists, and schedules auto-skill preferences safely', () => {
    expect(normalizeMythicSkillPrefs(null)).toEqual(DEFAULT_MYTHIC_SKILL_PREFS);
    expect(
      normalizeMythicSkillPrefs({ autoCast: false, intervalId: 'invalid', screenShake: false }),
    ).toEqual({
      autoCast: false,
      intervalId: 'normal',
      screenShake: false,
    });

    const store = new Map();
    const mockStorage = {
      getItem: (k) => (store.has(k) ? store.get(k) : null),
      setItem: (k, v) => store.set(k, String(v)),
    };

    expect(readMythicSkillPrefs(mockStorage)).toEqual(DEFAULT_MYTHIC_SKILL_PREFS);
    writeMythicSkillPrefs({ autoCast: false, intervalId: 'fast', screenShake: false }, mockStorage);
    expect(JSON.parse(store.get(MYTHIC_SKILL_PREF_KEY))).toEqual({
      autoCast: false,
      intervalId: 'fast',
      screenShake: false,
    });
    expect(readMythicSkillPrefs(mockStorage)).toEqual({
      autoCast: false,
      intervalId: 'fast',
      screenShake: false,
    });

    for (const cfg of AUTO_SKILL_INTERVALS) {
      expect(sampleAutoSkillDelay(cfg.id, () => 0)).toBe(cfg.minSec);
      expect(sampleAutoSkillDelay(cfg.id, () => 1)).toBe(cfg.maxSec);
    }

    const kiemSkills = getMythicSkills('kiem_de');
    expect(pickNextAutoSkill(kiemSkills, 'van_kiem', 2, () => 0.1).id).toBe('nhat_kiem');
    expect(pickNextAutoSkill(kiemSkills, 'nhat_kiem', 2, () => 0.99).id).toBe('van_kiem');
    expect(pickNextAutoSkill([], null, 0)).toBeNull();
  });

  it('enforces zero CSS and Canvas gradients across Mythic overlay and skill VFX files', () => {
    const files = [
      'src/components/student/mythicSkillEffects.js',
      'src/components/student/MythicScreenPetOverlay.jsx',
    ];
    const forbidden = /(linear-gradient|radial-gradient|conic-gradient|bg-gradient-|createLinearGradient|createRadialGradient|createConicGradient)/;

    for (const rel of files) {
      const src = readFileSync(resolve(process.cwd(), rel), 'utf8');
      expect(src, `${rel} contains forbidden gradient API`).not.toMatch(forbidden);
    }
  });
});
