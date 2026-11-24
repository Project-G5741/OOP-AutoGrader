import { describe, expect, it } from 'vitest';
import { existsSync, readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import {
  EGG_SMASH_BOARD_SIZE,
  EGG_SMASH_PICKS_REQUIRED,
  EGG_SMASH_TURN_COST,
} from '../../components/student/EggSmashPanel';

describe('EggSmashPanel invariants', () => {
  it('locks economy constants to 1000 pts per turn and 3 picks on a 9-egg board', () => {
    expect(EGG_SMASH_TURN_COST).toBe(1000);
    expect(EGG_SMASH_BOARD_SIZE).toBe(9);
    expect(EGG_SMASH_PICKS_REQUIRED).toBe(3);
  });

  it('forbids localStorage, sessionStorage, and CSS/SVG gradients in EggSmashPanel', () => {
    const jsxSrc = readFileSync(
      resolve(process.cwd(), 'src/components/student/EggSmashPanel.jsx'),
      'utf8',
    );
    const cssSrc = readFileSync(
      resolve(process.cwd(), 'src/components/student/EggSmashPanel.css'),
      'utf8',
    );

    expect(jsxSrc).not.toMatch(/\b(localStorage|sessionStorage)\b/);
    const forbiddenGradients = /(linear-gradient|radial-gradient|conic-gradient|bg-gradient-|linearGradient|radialGradient)/;
    expect(jsxSrc).not.toMatch(forbiddenGradients);
    expect(cssSrc).not.toMatch(forbiddenGradients);
  });

  it('ships every 2.5D WebP sprite referenced by EggSmashPanel under public/egg-smash', () => {
    const jsxSrc = readFileSync(
      resolve(process.cwd(), 'src/components/student/EggSmashPanel.jsx'),
      'utf8',
    );
    const matches = [...jsxSrc.matchAll(/['"](\/egg-smash\/[^'"]+\.webp)['"]/g)].map((m) => m[1]);
    expect(matches.length).toBeGreaterThanOrEqual(12);

    for (const rel of matches) {
      const fullPath = resolve(process.cwd(), 'public', rel.replace(/^\//, ''));
      expect(existsSync(fullPath), `Missing sprite: ${rel}`).toBe(true);
    }
  });
});
