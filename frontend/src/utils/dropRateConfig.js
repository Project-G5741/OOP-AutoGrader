const PERCENT_EPSILON = 1e-4;

export const DEFAULT_GACHA_CONFIG = {
  costSinglePoints: 100,
  costMultiPoints: 900,
  hardPityLimit: 100,
  softPityStart: 90,
  directLegendaryRate: 1 / 6000,
  crystalBaseRate: 0.0015,
  pointCrystalRate: 0.0001,
  randomMascotRate: 0.003,
  cashbackRate300: 0.025,
  cashbackRate150: 0.06,
  cashbackRate60: 0.18,
  cashbackRate20: 0.42,
};

export const DEFAULT_EGG_SMASH_CONFIG = {
  baseCrystalHitRate: 0,
  turn6CrystalHitRate: 0,
  turn7CrystalHitRate: 0,
  turn8CrystalHitRate: 1 / 3,
  turn9CrystalHitRate: 1,
  eggRate300: 0.05,
  eggRate200: 0.15,
  eggRate100: 0.3,
};

export function rateToPercentString(rate) {
  if (!Number.isFinite(rate)) return '0';
  const pct = rate * 100;
  return Number(pct.toFixed(4)).toString();
}

export function parsePercentInput(raw) {
  const trimmed = String(raw ?? '').trim();
  if (trimmed === '') return NaN;
  const num = Number(trimmed);
  return Number.isFinite(num) ? num : NaN;
}

export function parsePositiveIntInput(raw) {
  const trimmed = String(raw ?? '').trim();
  if (!/^\d+$/.test(trimmed)) return NaN;
  return Number.parseInt(trimmed, 10);
}

export function resolveSubmittedRate(draftStr, savedRate) {
  if (Number.isFinite(savedRate) && String(draftStr ?? '').trim() === rateToPercentString(savedRate)) {
    return savedRate;
  }
  const pct = parsePercentInput(draftStr);
  if (!Number.isFinite(pct)) return NaN;
  return Math.min(1, Math.max(0, pct / 100));
}

export function gachaConfigToDraft(cfg = DEFAULT_GACHA_CONFIG) {
  return {
    softPityStart: String(cfg.softPityStart ?? DEFAULT_GACHA_CONFIG.softPityStart),
    hardPityLimit: String(cfg.hardPityLimit ?? DEFAULT_GACHA_CONFIG.hardPityLimit),
    directLegendaryRate: rateToPercentString(cfg.directLegendaryRate ?? DEFAULT_GACHA_CONFIG.directLegendaryRate),
    crystalBaseRate: rateToPercentString(cfg.crystalBaseRate ?? DEFAULT_GACHA_CONFIG.crystalBaseRate),
    pointCrystalRate: rateToPercentString(cfg.pointCrystalRate ?? DEFAULT_GACHA_CONFIG.pointCrystalRate),
    randomMascotRate: rateToPercentString(cfg.randomMascotRate ?? DEFAULT_GACHA_CONFIG.randomMascotRate),
    cashbackRate300: rateToPercentString(cfg.cashbackRate300 ?? DEFAULT_GACHA_CONFIG.cashbackRate300),
    cashbackRate150: rateToPercentString(cfg.cashbackRate150 ?? DEFAULT_GACHA_CONFIG.cashbackRate150),
    cashbackRate60: rateToPercentString(cfg.cashbackRate60 ?? DEFAULT_GACHA_CONFIG.cashbackRate60),
    cashbackRate20: rateToPercentString(cfg.cashbackRate20 ?? DEFAULT_GACHA_CONFIG.cashbackRate20),
  };
}

export function eggSmashConfigToDraft(cfg = DEFAULT_EGG_SMASH_CONFIG) {
  return {
    baseCrystalHitRate: rateToPercentString(cfg.baseCrystalHitRate ?? DEFAULT_EGG_SMASH_CONFIG.baseCrystalHitRate),
    turn6CrystalHitRate: rateToPercentString(cfg.turn6CrystalHitRate ?? DEFAULT_EGG_SMASH_CONFIG.turn6CrystalHitRate),
    turn7CrystalHitRate: rateToPercentString(cfg.turn7CrystalHitRate ?? DEFAULT_EGG_SMASH_CONFIG.turn7CrystalHitRate),
    turn8CrystalHitRate: rateToPercentString(cfg.turn8CrystalHitRate ?? DEFAULT_EGG_SMASH_CONFIG.turn8CrystalHitRate),
    turn9CrystalHitRate: rateToPercentString(cfg.turn9CrystalHitRate ?? DEFAULT_EGG_SMASH_CONFIG.turn9CrystalHitRate),
    eggRate300: rateToPercentString(cfg.eggRate300 ?? DEFAULT_EGG_SMASH_CONFIG.eggRate300),
    eggRate200: rateToPercentString(cfg.eggRate200 ?? DEFAULT_EGG_SMASH_CONFIG.eggRate200),
    eggRate100: rateToPercentString(cfg.eggRate100 ?? DEFAULT_EGG_SMASH_CONFIG.eggRate100),
  };
}

export function validateGachaDraft(draft) {
  const errors = {};
  const hardPity = parsePositiveIntInput(draft.hardPityLimit);
  const softPity = parsePositiveIntInput(draft.softPityStart);

  if (!Number.isInteger(hardPity) || hardPity < 1 || hardPity > 1000) {
    errors.hardPityLimit = '1–1000';
  }
  if (!Number.isInteger(softPity) || softPity < 1 || (Number.isInteger(hardPity) && softPity > hardPity)) {
    errors.softPityStart = Number.isInteger(hardPity) ? `1–${hardPity}` : 'Invalid';
  }

  const rateKeys = [
    'directLegendaryRate',
    'crystalBaseRate',
    'pointCrystalRate',
    'randomMascotRate',
    'cashbackRate300',
    'cashbackRate150',
    'cashbackRate60',
    'cashbackRate20',
  ];

  const parsedPct = {};
  for (const key of rateKeys) {
    const val = parsePercentInput(draft[key]);
    parsedPct[key] = val;
    if (!Number.isFinite(val) || val < 0 || val > 100) {
      errors[key] = '0%–100%';
    }
  }

  const cashbackSumPct =
    (Number.isFinite(parsedPct.cashbackRate300) ? parsedPct.cashbackRate300 : 0) +
    (Number.isFinite(parsedPct.cashbackRate150) ? parsedPct.cashbackRate150 : 0) +
    (Number.isFinite(parsedPct.cashbackRate60) ? parsedPct.cashbackRate60 : 0) +
    (Number.isFinite(parsedPct.cashbackRate20) ? parsedPct.cashbackRate20 : 0);

  const remainder5Pct = Number((100 - cashbackSumPct).toFixed(4));
  if (cashbackSumPct > 100 + PERCENT_EPSILON) {
    errors.cashbackPool = 'Points rates exceed 100%';
  }

  return {
    valid: Object.keys(errors).length === 0,
    errors,
    cashbackSumPct: Number(cashbackSumPct.toFixed(4)),
    remainder5Pct,
  };
}

export function validateEggSmashDraft(draft) {
  const errors = {};
  const rateKeys = [
    'baseCrystalHitRate',
    'turn6CrystalHitRate',
    'turn7CrystalHitRate',
    'turn8CrystalHitRate',
    'turn9CrystalHitRate',
    'eggRate300',
    'eggRate200',
    'eggRate100',
  ];

  const parsedPct = {};
  for (const key of rateKeys) {
    const val = parsePercentInput(draft[key]);
    parsedPct[key] = val;
    if (!Number.isFinite(val) || val < 0 || val > 100) {
      errors[key] = '0%–100%';
    }
  }

  const eggSumPct =
    (Number.isFinite(parsedPct.eggRate300) ? parsedPct.eggRate300 : 0) +
    (Number.isFinite(parsedPct.eggRate200) ? parsedPct.eggRate200 : 0) +
    (Number.isFinite(parsedPct.eggRate100) ? parsedPct.eggRate100 : 0);

  const remainder50Pct = Number((100 - eggSumPct).toFixed(4));
  if (eggSumPct > 100 + PERCENT_EPSILON) {
    errors.eggPool = 'Point egg rates exceed 100%';
  }

  return {
    valid: Object.keys(errors).length === 0,
    errors,
    eggSumPct: Number(eggSumPct.toFixed(4)),
    remainder50Pct,
  };
}

export function buildGachaUpdatePayload(draft, saved = DEFAULT_GACHA_CONFIG) {
  return {
    costSinglePoints: saved?.costSinglePoints ?? DEFAULT_GACHA_CONFIG.costSinglePoints,
    costMultiPoints: saved?.costMultiPoints ?? DEFAULT_GACHA_CONFIG.costMultiPoints,
    hardPityLimit: parsePositiveIntInput(draft.hardPityLimit),
    softPityStart: parsePositiveIntInput(draft.softPityStart),
    directLegendaryRate: resolveSubmittedRate(draft.directLegendaryRate, saved?.directLegendaryRate),
    crystalBaseRate: resolveSubmittedRate(draft.crystalBaseRate, saved?.crystalBaseRate),
    pointCrystalRate: resolveSubmittedRate(draft.pointCrystalRate, saved?.pointCrystalRate),
    randomMascotRate: resolveSubmittedRate(draft.randomMascotRate, saved?.randomMascotRate),
    cashbackRate300: resolveSubmittedRate(draft.cashbackRate300, saved?.cashbackRate300),
    cashbackRate150: resolveSubmittedRate(draft.cashbackRate150, saved?.cashbackRate150),
    cashbackRate60: resolveSubmittedRate(draft.cashbackRate60, saved?.cashbackRate60),
    cashbackRate20: resolveSubmittedRate(draft.cashbackRate20, saved?.cashbackRate20),
  };
}

export function buildEggSmashUpdatePayload(draft, saved = DEFAULT_EGG_SMASH_CONFIG) {
  return {
    baseCrystalHitRate: resolveSubmittedRate(draft.baseCrystalHitRate, saved?.baseCrystalHitRate),
    turn6CrystalHitRate: resolveSubmittedRate(draft.turn6CrystalHitRate, saved?.turn6CrystalHitRate),
    turn7CrystalHitRate: resolveSubmittedRate(draft.turn7CrystalHitRate, saved?.turn7CrystalHitRate),
    turn8CrystalHitRate: resolveSubmittedRate(draft.turn8CrystalHitRate, saved?.turn8CrystalHitRate),
    turn9CrystalHitRate: resolveSubmittedRate(draft.turn9CrystalHitRate, saved?.turn9CrystalHitRate),
    eggRate300: resolveSubmittedRate(draft.eggRate300, saved?.eggRate300),
    eggRate200: resolveSubmittedRate(draft.eggRate200, saved?.eggRate200),
    eggRate100: resolveSubmittedRate(draft.eggRate100, saved?.eggRate100),
  };
}
