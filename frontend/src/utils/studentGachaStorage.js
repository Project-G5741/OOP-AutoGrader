/** Legacy gacha keys must not leak economy state across accounts on one browser. */
const LEGACY_GACHA_KEYS = [
  'gacha_demo_crystals',
  'gacha_demo_tickets',
  'gacha_demo_points',
  'gacha_demo_pity',
  'oop_student_owned_mascots',
  'oop_student_equipped_mascot',
];

export function purgeLegacyGachaLocalStorage() {
  if (typeof localStorage === 'undefined') return;
  for (const key of LEGACY_GACHA_KEYS) {
    localStorage.removeItem(key);
  }
}