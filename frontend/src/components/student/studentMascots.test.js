import { describe, expect, it } from 'vitest';
import { getMascotProfile } from './mascotBehaviors';
import { listCatalogMascots, mascotArt, mascotOfferVisibleInShop } from './studentMascots';

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

  it('hides exclusive mascots in the shop unless the API offers them', () => {
    const iceQueen = listCatalogMascots().find((item) => item.id === 'ice_queen');
    const goat = listCatalogMascots().find((item) => item.id === 'goat');
    const backend = new Set(['fox', 'bear']);
    expect(mascotOfferVisibleInShop(iceQueen, backend)).toBe(false);
    expect(mascotOfferVisibleInShop(goat, backend)).toBe(false);
    expect(mascotOfferVisibleInShop(iceQueen, new Set(['fox', 'ice_queen']))).toBe(true);
    expect(mascotOfferVisibleInShop(goat, new Set(['fox', 'goat']))).toBe(true);
    expect(mascotOfferVisibleInShop({ id: 'bear', exclusive: false }, backend)).toBe(true);
  });
});
