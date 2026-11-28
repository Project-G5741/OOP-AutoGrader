import { describe, expect, it } from 'vitest';
import { getMascotProfile } from './mascotBehaviors';
import { listCatalogMascots, mascotArt, mascotOfferVisibleInShop } from './studentMascots';
import { filterShopOffers } from './MascotShop';

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

  it('filters MascotShop offers by Owned, search query, and sort order', () => {
    const offers = [
      { id: 'fox', name: 'Fox', tier: 'normal', price: 0, owned: true },
      { id: 'bear', name: 'Bear', tier: 'normal', price: 333, owned: false },
      { id: 'shiba', name: 'Shiba', tier: 'rare', price: 3888, owned: true },
      { id: 'kiem_de', name: 'Kiếm Đế Hư Không', tier: 'mythic', price: 75000, owned: true },
      { id: 'zerotwo', name: 'Zero2', tier: 'legendary', price: 35000, owned: false },
    ];

    expect(
      filterShopOffers(offers, { selectedTier: 'owned', sortBy: 'price-asc' }).map((o) => o.id),
    ).toEqual(['fox', 'shiba', 'kiem_de']);

    expect(
      filterShopOffers(offers, { selectedTier: 'owned', sortBy: 'price-desc' }).map((o) => o.id),
    ).toEqual(['kiem_de', 'shiba', 'fox']);

    expect(
      filterShopOffers(offers, { selectedTier: 'owned', searchQuery: 'shi' }).map((o) => o.id),
    ).toEqual(['shiba']);

    expect(
      filterShopOffers(offers, { selectedTier: 'owned', searchQuery: 'bear' }),
    ).toEqual([]);
  });
});
