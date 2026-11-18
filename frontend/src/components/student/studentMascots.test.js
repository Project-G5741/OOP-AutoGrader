import { describe, expect, it } from 'vitest';
import { getMascotProfile } from './mascotBehaviors';
import { listCatalogMascots, mascotArt } from './studentMascots';

describe('mascot catalog', () => {
  it('lists each mascot once', () => {
    const ids = listCatalogMascots().map((item) => item.id);
    expect(new Set(ids).size).toBe(ids.length);
    expect(ids.filter((id) => id === 'goat')).toEqual(['goat']);
    expect(ids.filter((id) => id === 'ice_queen')).toEqual(['ice_queen']);
    expect(listCatalogMascots().filter((item) => item.name === 'The GOAT')).toHaveLength(1);
    expect(listCatalogMascots().filter((item) => item.name === 'Ice Queen')).toHaveLength(1);
  });

  it('resolves hyphenated ice queen onto the canonical sheet', () => {
    expect(mascotArt('goat').name).toBe('The GOAT');
    expect(mascotArt('ice-queen').id).toBe('ice_queen');
    expect(mascotArt('ice_queen').directions).toBe('/mascots/ice-queen-directions.webp');
  });

  it('keeps The GOAT and Ice Queen dialogue on the canonical profile', () => {
    expect(getMascotProfile('ice-queen')).toBe(getMascotProfile('ice_queen'));
    expect(getMascotProfile('goat').dialogues.idle.join(' ')).toContain('The GOAT');
  });
});
