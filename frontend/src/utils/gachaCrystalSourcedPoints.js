/**
 * Points credited from Crystal → reward-point exchange cannot yield another
 * Crystal on gacha rolls (prevents crystal ↔ points infinite loops at 4,000 pts each).
 */
const STORAGE_KEY = 'gacha_crystal_sourced_points';

function readRaw() {
  if (typeof localStorage === 'undefined') return 0;
  const saved = localStorage.getItem(STORAGE_KEY);
  if (saved == null || saved === '') return 0;
  const n = Number.parseInt(saved, 10);
  return Number.isFinite(n) && n > 0 ? n : 0;
}

function writeRaw(value) {
  if (typeof localStorage === 'undefined') return;
  const safe = Math.max(0, Math.floor(value));
  if (safe <= 0) {
    localStorage.removeItem(STORAGE_KEY);
  } else {
    localStorage.setItem(STORAGE_KEY, String(safe));
  }
}

export function readCrystalSourcedPoints() {
  return readRaw();
}

/** Call after a successful crystals/exchange API credit. */
export function addCrystalSourcedPoints(amount) {
  const delta = Math.max(0, Math.floor(amount));
  if (delta <= 0) return readRaw();
  const next = readRaw() + delta;
  writeRaw(next);
  return next;
}

/**
 * FIFO: gacha point spend draws from crystal-sourced balance first.
 * @returns {{ consumed: number, balance: number }}
 */
export function consumeCrystalSourcedPointsForSpend(spendAmount) {
  const spend = Math.max(0, Math.floor(spendAmount));
  const balance = readRaw();
  const consumed = Math.min(spend, balance);
  writeRaw(balance - consumed);
  return { consumed, balance: balance - consumed };
}

/** Cashback from rolls that could not drop Crystals stays non-crystal-eligible. */
export function tagGachaCashbackAsCrystalSourced(amount) {
  return addCrystalSourcedPoints(amount);
}