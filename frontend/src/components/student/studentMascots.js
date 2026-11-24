export const MASCOT_TIERS = {
  normal: {
    id: 'normal',
    name: 'Normal',
    label: 'Normal (< 2,000 pts)',
    badgeClass: 'bg-surface-secondary text-foreground-secondary border-border',
    color: '#94a3b8',
  },
  rare: {
    id: 'rare',
    name: 'Rare',
    label: 'Rare (2,000 - 5,000 pts)',
    badgeClass: 'bg-info-bg text-info-text border-info-border',
    color: '#38bdf8',
  },
  epic: {
    id: 'epic',
    name: 'Epic',
    label: 'Epic (5,000 - 10,000 pts)',
    badgeClass: 'bg-purple-100 dark:bg-purple-950/40 text-purple-700 dark:text-purple-300 border-purple-300 dark:border-purple-800',
    color: '#a855f7',
  },
  legendary: {
    id: 'legendary',
    name: 'Legendary',
    label: 'Legendary (20,000+ pts)',
    badgeClass: 'bg-amber-100 dark:bg-amber-950/40 text-amber-800 dark:text-amber-300 border-amber-300 dark:border-amber-800',
    color: '#f59e0b',
  },
  mythic: {
    id: 'mythic',
    name: 'Mythic',
    label: 'Mythic (15 Crystal)',
    badgeClass: 'bg-error-bg text-error-text border-error-border',
    color: '#f43f5e',
  },
};

export const STUDENT_MASCOTS = {
  fox: {
    id: 'fox',
    name: 'Fox',
    tier: 'normal',
    price: 0,
    starter: true,
    directions: '/mascots/fox-directions.webp',
    reactions: '/mascots/fox-reactions.webp',
  },
  bear: {
    id: 'bear',
    name: 'Bear',
    tier: 'normal',
    price: 333,
    directions: '/mascots/bear-directions.webp',
    reactions: '/mascots/bear-reactions.webp',
  },
  panda: {
    id: 'panda',
    name: 'Panda',
    tier: 'normal',
    price: 999,
    directions: '/mascots/panda-directions.webp',
    reactions: '/mascots/panda-reactions.webp',
  },
  penguin: {
    id: 'penguin',
    name: 'Penguin',
    tier: 'normal',
    price: 1111,
    directions: '/mascots/penguin-directions.webp',
    reactions: '/mascots/penguin-reactions.webp',
  },
  bunny: {
    id: 'bunny',
    name: 'Bunny',
    tier: 'rare',
    price: 2222,
    directions: '/mascots/bunny-directions.webp',
    reactions: '/mascots/bunny-reactions.webp',
  },
  cat: {
    id: 'cat',
    name: 'Cat',
    tier: 'rare',
    price: 3333,
    directions: '/mascots/cat-directions.webp',
    reactions: '/mascots/cat-reactions.webp',
  },
  vit: {
    id: 'vit',
    name: 'Duck',
    tier: 'rare',
    price: 2444,
    directions: '/mascots/vit-directions.webp',
    reactions: '/mascots/vit-reactions.webp',
  },
  hamster: {
    id: 'hamster',
    name: 'Hamster',
    tier: 'rare',
    price: 2888,
    directions: '/mascots/hamster-directions.webp',
    reactions: '/mascots/hamster-reactions.webp',
  },
  chuot_tui: {
    id: 'chuot_tui',
    name: 'Kangaroo',
    tier: 'rare',
    price: 3555,
    directions: '/mascots/chuot-tui-directions.webp',
    reactions: '/mascots/chuot-tui-reactions.webp',
  },
  shiba: {
    id: 'shiba',
    name: 'Shiba',
    tier: 'rare',
    price: 3888,
    directions: '/mascots/shiba-directions.webp',
    reactions: '/mascots/shiba-reactions.webp',
  },
  dai_bang: {
    id: 'dai_bang',
    name: 'Eagle',
    tier: 'rare',
    price: 4222,
    directions: '/mascots/dai-bang-directions.webp',
    reactions: '/mascots/dai-bang-reactions.webp',
  },
  cho_soi: {
    id: 'cho_soi',
    name: 'Wolf',
    tier: 'rare',
    price: 4888,
    directions: '/mascots/cho-soi-directions.webp',
    reactions: '/mascots/cho-soi-reactions.webp',
  },
  thanh_long: {
    id: 'thanh_long',
    name: 'Thanh Long',
    tier: 'epic',
    price: 9999,
    directions: '/mascots/thanh-long-directions.webp',
    reactions: '/mascots/thanh-long-reactions.webp',
  },
  bach_ho: {
    id: 'bach_ho',
    name: 'Bạch Hổ',
    tier: 'epic',
    price: 8888,
    directions: '/mascots/bach-ho-directions.webp',
    reactions: '/mascots/bach-ho-reactions.webp',
  },
  chu_tuoc: {
    id: 'chu_tuoc',
    name: 'Chu Tước',
    tier: 'epic',
    price: 7777,
    directions: '/mascots/chu-tuoc-directions.webp',
    reactions: '/mascots/chu-tuoc-reactions.webp',
  },
  huyen_vu: {
    id: 'huyen_vu',
    name: 'Huyền Vũ',
    tier: 'epic',
    price: 6666,
    directions: '/mascots/huyen-vu-directions.webp',
    reactions: '/mascots/huyen-vu-reactions.webp',
  },
  linh_loc: {
    id: 'linh_loc',
    name: 'Linh Lộc',
    tier: 'legendary',
    price: 40000,
    crystalCost: 8,
    directions: '/mascots/linh-loc-directions.webp',
    reactions: '/mascots/linh-loc-reactions.webp',
  },
  ki_lan: {
    id: 'ki_lan',
    name: 'Kì Lân',
    tier: 'legendary',
    price: 40000,
    crystalCost: 8,
    directions: '/mascots/ki-lan-directions.webp',
    reactions: '/mascots/ki-lan-reactions.webp',
  },
  zerotwo: {
    id: 'zerotwo',
    name: 'Zero2',
    tier: 'legendary',
    price: 35000,
    crystalCost: 7,
    directions: '/mascots/zerotwo-directions.webp',
    reactions: '/mascots/zerotwo-reactions.webp',
  },
  con_bang: {
    id: 'con_bang',
    name: 'Côn Bằng',
    tier: 'legendary',
    price: 20000,
    crystalCost: 4,
    directions: '/mascots/con-bang-directions.webp',
    reactions: '/mascots/con-bang-reactions.webp',
  },
  cung_ky: {
    id: 'cung_ky',
    name: 'Cùng Kỳ',
    tier: 'legendary',
    price: 20000,
    crystalCost: 4,
    directions: '/mascots/cung-ky-directions.webp',
    reactions: '/mascots/cung-ky-reactions.webp',
  },
  ice_queen: {
    id: 'ice_queen',
    name: 'Ice Queen',
    tier: 'legendary',
    price: 0,
    exclusive: true,
    directions: '/mascots/ice-queen-directions.webp',
    reactions: '/mascots/ice-queen-reactions.webp',
  },
  capybara: {
    id: 'capybara',
    name: 'Capybara',
    tier: 'normal',
    price: 666,
    directions: '/mascots/capybara-directions.webp',
    reactions: '/mascots/capybara-reactions.webp',
  },
  alpaca: {
    id: 'alpaca',
    name: 'Alpaca',
    tier: 'normal',
    price: 1555,
    directions: '/mascots/alpaca-directions.webp',
    reactions: '/mascots/alpaca-reactions.webp',
  },
  hai_cau: {
    id: 'hai_cau',
    name: 'Hải Cẩu',
    tier: 'rare',
    price: 2666,
    directions: '/mascots/hai-cau-directions.webp',
    reactions: '/mascots/hai-cau-reactions.webp',
  },
  red_panda: {
    id: 'red_panda',
    name: 'Red Panda',
    tier: 'rare',
    price: 3666,
    directions: '/mascots/red-panda-directions.webp',
    reactions: '/mascots/red-panda-reactions.webp',
  },
  pomeranian: {
    id: 'pomeranian',
    name: 'Pomeranian',
    tier: 'rare',
    price: 4555,
    directions: '/mascots/pomeranian-directions.webp',
    reactions: '/mascots/pomeranian-reactions.webp',
  },
  ho_diep: {
    id: 'ho_diep',
    name: 'Tinh Linh Hồ Điệp',
    tier: 'epic',
    price: 5888,
    directions: '/mascots/ho-diep-directions.webp',
    reactions: '/mascots/ho-diep-reactions.webp',
  },
  thien_nga: {
    id: 'thien_nga',
    name: 'Thiên Nga Ánh Trăng',
    tier: 'epic',
    price: 7222,
    directions: '/mascots/thien-nga-directions.webp',
    reactions: '/mascots/thien-nga-reactions.webp',
  },
  cuu_vi: {
    id: 'cuu_vi',
    name: 'Cửu Vĩ Hồ',
    tier: 'epic',
    price: 9555,
    directions: '/mascots/cuu-vi-directions.webp',
    reactions: '/mascots/cuu-vi-reactions.webp',
  },
  stella_witch: {
    id: 'stella_witch',
    name: 'Pháp Sư Tinh Tú',
    tier: 'legendary',
    price: 30000,
    crystalCost: 6,
    directions: '/mascots/stella-witch-directions.webp',
    reactions: '/mascots/stella-witch-reactions.webp',
  },
  sakura_princess: {
    id: 'sakura_princess',
    name: 'Công Chúa Nguyệt Hoa',
    tier: 'legendary',
    price: 40000,
    crystalCost: 8,
    directions: '/mascots/sakura-princess-directions.webp',
    reactions: '/mascots/sakura-princess-reactions.webp',
  },
  floral_fairy: {
    id: 'floral_fairy',
    name: 'Hoa Tiên Linh Lan',
    tier: 'mythic',
    price: 75000,
    crystalCost: 15,
    canMove: true,
    accentColor: '#34d399',
    secondaryColor: '#f472b6',
    directions: '/mascots/floral-fairy-directions.webp',
    movement: '/mascots/floral-fairy-movement.webp',
    reactions: '/mascots/floral-fairy-reactions.webp',
  },
  chrono_celestia: {
    id: 'chrono_celestia',
    name: 'Nữ Thần Thời Không',
    tier: 'mythic',
    price: 75000,
    crystalCost: 15,
    canMove: true,
    accentColor: '#c084fc',
    secondaryColor: '#fbbf24',
    directions: '/mascots/chrono-celestia-directions.webp',
    movement: '/mascots/chrono-celestia-movement.webp',
    reactions: '/mascots/chrono-celestia-reactions.webp',
  },
  cyber_striker: {
    id: 'cyber_striker',
    name: 'Chiến Thần Cơ Giáp',
    tier: 'mythic',
    price: 75000,
    crystalCost: 15,
    canMove: true,
    accentColor: '#22d3ee',
    secondaryColor: '#fb923c',
    directions: '/mascots/cyber-striker-directions.webp',
    movement: '/mascots/cyber-striker-movement.webp',
    reactions: '/mascots/cyber-striker-reactions.webp',
  },
  kiem_de: {
    id: 'kiem_de',
    name: 'Kiếm Đế Hư Không',
    tier: 'mythic',
    price: 75000,
    crystalCost: 15,
    canMove: true,
    accentColor: '#fbbf24',
    secondaryColor: '#a855f7',
    directions: '/mascots/kiem-de-directions.webp',
    movement: '/mascots/kiem-de-movement.webp',
    reactions: '/mascots/kiem-de-reactions.webp',
  },
};

/** Exclusive mascots appear in the shop only when the API includes them for the signed-in student. */
export function mascotOfferVisibleInShop(catalogItem, backendOfferIds) {
  if (catalogItem?.exclusive && !backendOfferIds?.has(catalogItem.id)) {
    return false;
  }
  return true;
}

export function listCatalogMascots() {
  const seen = new Set();
  const list = [];
  for (const item of Object.values(STUDENT_MASCOTS)) {
    if (!item?.id || seen.has(item.id)) continue;
    seen.add(item.id);
    list.push(item);
  }
  return list;
}

export function mascotArt(id) {
  if (!id) return STUDENT_MASCOTS.fox;
  const raw = String(id).trim();
  const lower = raw.toLowerCase();
  const normalized = lower.replace(/-/g, '_');
  const hyphenated = lower.replace(/_/g, '-');
  return (
    STUDENT_MASCOTS[raw] ||
    STUDENT_MASCOTS[lower] ||
    STUDENT_MASCOTS[normalized] ||
    STUDENT_MASCOTS[hyphenated] ||
    STUDENT_MASCOTS.fox
  );
}
