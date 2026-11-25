import { describe, expect, it } from 'vitest';
import { getMascotProfile } from './mascotBehaviors';
import { listCatalogMascots, mascotArt, mascotOfferVisibleInShop } from './studentMascots';

describe('mascot catalog', () => {
  it('lists each mascot once and excludes retired goat', () => {
    const ids = listCatalogMascots().map((item) => item.id);
    expect(new Set(ids).size).toBe(ids.length);
    expect(ids).not.toContain('goat');
    expect(ids.filter((id) => id === 'ice_queen')).toEqual(['ice_queen']);
    expect(listCatalogMascots().filter((item) => item.name === 'Ice Queen')).toHaveLength(1);
  });

  it('resolves hyphenated ice queen onto the canonical sheet and falls back to fox for unknown ids', () => {
    expect(mascotArt('goat').id).toBe('fox');
    expect(mascotArt('ice-queen').id).toBe('ice_queen');
    expect(mascotArt('ice_queen').directions).toBe('/mascots/ice-queen-directions.webp');
  });

  it('keeps Ice Queen dialogue on the canonical profile and falls back to fox for retired ids', () => {
    expect(getMascotProfile('ice-queen')).toBe(getMascotProfile('ice_queen'));
    expect(getMascotProfile('goat')).toBe(getMascotProfile('fox'));
  });

  it('hides exclusive mascots in the shop unless the API offers them', () => {
    const iceQueen = listCatalogMascots().find((item) => item.id === 'ice_queen');
    const backend = new Set(['fox', 'bear']);
    expect(mascotOfferVisibleInShop(iceQueen, backend)).toBe(false);
    expect(mascotOfferVisibleInShop(iceQueen, new Set(['fox', 'ice_queen']))).toBe(true);
    expect(mascotOfferVisibleInShop({ id: 'bear', exclusive: false }, backend)).toBe(true);
  });
});
