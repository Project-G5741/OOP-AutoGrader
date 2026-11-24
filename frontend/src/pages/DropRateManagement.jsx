import { useCallback, useEffect, useMemo, useState } from 'react';
import { Award, Check, Coins, Crown, Hammer, RefreshCw, RotateCcw, Sparkles } from 'lucide-react';
import DashboardSection from '../components/lecturer/DashboardSection';
import { Badge } from '../components/ui/badge';
import { useToast } from '../components/ui/Toast';
import { useDialog } from '../context/DialogContext';
import { apiFetch } from '../utils/apiFetch';
import { authHeaders } from '../utils/authHeaders';
import {
  friendlyLoadErrorFromResponse,
  readFriendlyApiError,
  toFriendlyError,
} from '../utils/apiError';
import {
  buildEggSmashUpdatePayload,
  buildGachaUpdatePayload,
  DEFAULT_EGG_SMASH_CONFIG,
  DEFAULT_GACHA_CONFIG,
  eggSmashConfigToDraft,
  gachaConfigToDraft,
  validateEggSmashDraft,
  validateGachaDraft,
} from '../utils/dropRateConfig';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';

const GACHA_RATE_ROWS = [
  {
    key: 'directLegendaryRate',
    label: 'Direct Legendary Mascot',
    badge: 'Legendary',
    badgeVariant: 'gold',
    icon: Crown,
  },
  {
    key: 'crystalBaseRate',
    label: 'Crystal Stone · Ticket Roll',
    badge: 'Crystal',
    badgeVariant: 'warning',
    icon: Sparkles,
  },
  {
    key: 'pointCrystalRate',
    label: 'Crystal Stone · Point Roll',
    badge: 'Crystal',
    badgeVariant: 'warning',
    icon: Sparkles,
  },
  {
    key: 'randomMascotRate',
    label: 'Random Mascot (Normal / Rare / Epic)',
    badge: 'Mascot',
    badgeVariant: 'teal',
    icon: Award,
  },
  {
    key: 'cashbackRate300',
    label: '+300 Points',
    badge: 'Points',
    badgeVariant: 'secondary',
    icon: Coins,
  },
  {
    key: 'cashbackRate150',
    label: '+150 Points',
    badge: 'Points',
    badgeVariant: 'secondary',
    icon: Coins,
  },
  {
    key: 'cashbackRate60',
    label: '+60 Points',
    badge: 'Points',
    badgeVariant: 'secondary',
    icon: Coins,
  },
  {
    key: 'cashbackRate20',
    label: '+20 Points',
    badge: 'Points',
    badgeVariant: 'secondary',
    icon: Coins,
  },
];

const EGG_SMASH_ROWS = [
  {
    key: 'baseCrystalHitRate',
    label: 'Crystal Egg · Turns 1–5',
    badge: 'Crystal',
    badgeVariant: 'warning',
    icon: Sparkles,
  },
  {
    key: 'turn6CrystalHitRate',
    label: 'Crystal Egg · Turn 6',
    badge: 'Crystal',
    badgeVariant: 'warning',
    icon: Sparkles,
  },
  {
    key: 'turn7CrystalHitRate',
    label: 'Crystal Egg · Turn 7',
    badge: 'Crystal',
    badgeVariant: 'warning',
    icon: Sparkles,
  },
  {
    key: 'turn8CrystalHitRate',
    label: 'Crystal Egg · Turn 8 (Soft Pity)',
    badge: 'Crystal',
    badgeVariant: 'gold',
    icon: Sparkles,
  },
  {
    key: 'turn9CrystalHitRate',
    label: 'Crystal Egg · Turn 9 (Hard Pity)',
    badge: 'Crystal',
    badgeVariant: 'gold',
    icon: Sparkles,
  },
  {
    key: 'eggRate300',
    label: '+300 Points Egg',
    badge: 'Points',
    badgeVariant: 'secondary',
    icon: Hammer,
  },
  {
    key: 'eggRate200',
    label: '+200 Points Egg',
    badge: 'Points',
    badgeVariant: 'secondary',
    icon: Hammer,
  },
  {
    key: 'eggRate100',
    label: '+100 Points Egg',
    badge: 'Points',
    badgeVariant: 'secondary',
    icon: Hammer,
  },
];

function RateRow({ id, label, badge, badgeVariant, icon: Icon, value, error, onChange, disabled }) {
  return (
    <div className="flex flex-col gap-2.5 px-5 py-3.5 sm:flex-row sm:items-center sm:justify-between">
      <div className="flex min-w-0 items-center gap-2.5">
        <Icon className="h-4 w-4 shrink-0 text-primary" aria-hidden="true" />
        <label htmlFor={id} className="truncate text-sm font-semibold text-foreground">
          {label}
        </label>
        <Badge variant={badgeVariant}>{badge}</Badge>
      </div>

      <div className="flex items-center gap-2.5 sm:justify-end">
        {error && (
          <span className="font-mono text-xs font-semibold text-error">{error}</span>
        )}
        <div className="relative flex items-center">
          <input
            id={id}
            type="number"
            inputMode="decimal"
            min="0"
            max="100"
            step="any"
            disabled={disabled}
            value={value}
            onChange={(e) => onChange(e.target.value)}
            className={`w-32 rounded-lg border bg-surface-secondary py-1.5 pl-3 pr-7 text-right font-mono text-sm font-semibold tabular-nums text-foreground focus:outline-none focus:ring-2 disabled:opacity-50 ${
              error
                ? 'border-error focus:ring-error'
                : 'border-border focus:ring-primary'
            }`}
          />
          <span className="pointer-events-none absolute right-2.5 font-mono text-xs font-semibold text-foreground-muted">
            %
          </span>
        </div>
      </div>
    </div>
  );
}

export default function DropRateManagement() {
  const showToast = useToast();
  const dialog = useDialog();

  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);

  const [savedGacha, setSavedGacha] = useState(DEFAULT_GACHA_CONFIG);
  const [gachaDraft, setGachaDraft] = useState(() => gachaConfigToDraft(DEFAULT_GACHA_CONFIG));
  const [savingGacha, setSavingGacha] = useState(false);

  const [savedEgg, setSavedEgg] = useState(DEFAULT_EGG_SMASH_CONFIG);
  const [eggDraft, setEggDraft] = useState(() => eggSmashConfigToDraft(DEFAULT_EGG_SMASH_CONFIG));
  const [savingEgg, setSavingEgg] = useState(false);

  const loadConfig = useCallback(async () => {
    setLoading(true);
    setLoadError(null);
    try {
      const res = await apiFetch(`${API_BASE}/api/lecturer/drop-rates/config`, {
        headers: authHeaders(),
      });
      if (!res.ok) {
        setLoadError(await friendlyLoadErrorFromResponse(res));
        return;
      }
      const data = await res.json();
      const nextGacha = data?.gacha || DEFAULT_GACHA_CONFIG;
      const nextEgg = data?.eggSmash || DEFAULT_EGG_SMASH_CONFIG;
      setSavedGacha(nextGacha);
      setGachaDraft(gachaConfigToDraft(nextGacha));
      setSavedEgg(nextEgg);
      setEggDraft(eggSmashConfigToDraft(nextEgg));
    } catch (err) {
      setLoadError(toFriendlyError(err, 'read'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadConfig();
  }, [loadConfig]);

  const gachaValidation = useMemo(() => validateGachaDraft(gachaDraft), [gachaDraft]);
  const eggValidation = useMemo(() => validateEggSmashDraft(eggDraft), [eggDraft]);

  const gachaDirty = useMemo(() => {
    const clean = gachaConfigToDraft(savedGacha);
    return Object.keys(clean).some((k) => String(gachaDraft[k] ?? '').trim() !== clean[k]);
  }, [gachaDraft, savedGacha]);

  const eggDirty = useMemo(() => {
    const clean = eggSmashConfigToDraft(savedEgg);
    return Object.keys(clean).some((k) => String(eggDraft[k] ?? '').trim() !== clean[k]);
  }, [eggDraft, savedEgg]);

  const handleSaveGacha = async () => {
    if (!gachaValidation.valid || savingGacha) return;
    setSavingGacha(true);
    try {
      const payload = buildGachaUpdatePayload(gachaDraft, savedGacha);
      const res = await apiFetch(`${API_BASE}/api/lecturer/drop-rates/gacha`, {
        method: 'PUT',
        headers: { ...authHeaders(), 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });
      if (!res.ok) {
        showToast(await readFriendlyApiError(res, 'save'), 'error');
        return;
      }
      const updated = await res.json();
      setSavedGacha(updated);
      setGachaDraft(gachaConfigToDraft(updated));
      showToast('Saved Gacha drop rates.', 'success');
    } catch (err) {
      showToast(toFriendlyError(err, 'save'), 'error');
    } finally {
      setSavingGacha(false);
    }
  };

  const handleResetGacha = async () => {
    if (savingGacha) return;
    const confirmed = await dialog.confirm({
      level: 'warning',
      title: 'Reset Gacha Rates',
      message: 'Reset all Crystal Wheel drop rates and pity thresholds to default values?',
      confirmLabel: 'Reset',
    });
    if (!confirmed) return;

    setSavingGacha(true);
    try {
      const res = await apiFetch(`${API_BASE}/api/lecturer/drop-rates/gacha/reset`, {
        method: 'POST',
        headers: authHeaders(),
      });
      if (!res.ok) {
        showToast(await readFriendlyApiError(res, 'save'), 'error');
        return;
      }
      const updated = await res.json();
      setSavedGacha(updated);
      setGachaDraft(gachaConfigToDraft(updated));
      showToast('Reset Gacha drop rates to defaults.', 'success');
    } catch (err) {
      showToast(toFriendlyError(err, 'save'), 'error');
    } finally {
      setSavingGacha(false);
    }
  };

  const handleSaveEgg = async () => {
    if (!eggValidation.valid || savingEgg) return;
    setSavingEgg(true);
    try {
      const payload = buildEggSmashUpdatePayload(eggDraft, savedEgg);
      const res = await apiFetch(`${API_BASE}/api/lecturer/drop-rates/egg-smash`, {
        method: 'PUT',
        headers: { ...authHeaders(), 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });
      if (!res.ok) {
        showToast(await readFriendlyApiError(res, 'save'), 'error');
        return;
      }
      const updated = await res.json();
      setSavedEgg(updated);
      setEggDraft(eggSmashConfigToDraft(updated));
      showToast('Saved Egg Smash drop rates.', 'success');
    } catch (err) {
      showToast(toFriendlyError(err, 'save'), 'error');
    } finally {
      setSavingEgg(false);
    }
  };

  const handleResetEgg = async () => {
    if (savingEgg) return;
    const confirmed = await dialog.confirm({
      level: 'warning',
      title: 'Reset Egg Smash Rates',
      message: 'Reset all Egg Smash Crystal and Point egg drop rates to default values?',
      confirmLabel: 'Reset',
    });
    if (!confirmed) return;

    setSavingEgg(true);
    try {
      const res = await apiFetch(`${API_BASE}/api/lecturer/drop-rates/egg-smash/reset`, {
        method: 'POST',
        headers: authHeaders(),
      });
      if (!res.ok) {
        showToast(await readFriendlyApiError(res, 'save'), 'error');
        return;
      }
      const updated = await res.json();
      setSavedEgg(updated);
      setEggDraft(eggSmashConfigToDraft(updated));
      showToast('Reset Egg Smash drop rates to defaults.', 'success');
    } catch (err) {
      showToast(toFriendlyError(err, 'save'), 'error');
    } finally {
      setSavingEgg(false);
    }
  };

  if (loading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <div className="flex flex-col items-center gap-3">
          <div className="h-10 w-10 animate-spin rounded-full border-4 border-primary border-t-transparent" />
          <p className="text-foreground-secondary">Loading data...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-full space-y-6">
      {loadError && (
        <div className="flex items-center justify-between gap-3 rounded-xl bg-warning-bg px-4 py-3 text-sm text-warning-text">
          <span>{loadError}</span>
          <button
            type="button"
            onClick={loadConfig}
            className="inline-flex min-h-9 items-center gap-1.5 rounded-lg bg-surface px-3 py-1.5 text-xs font-semibold text-foreground transition-colors hover:bg-surface-secondary cursor-pointer"
          >
            <RefreshCw className="h-3.5 w-3.5" />
            Retry
          </button>
        </div>
      )}

      {/* 1. Crystal Wheel (Gacha) */}
      <DashboardSection
        title="Crystal Wheel (Gacha)"
        actions={
          <div className="flex flex-wrap items-center gap-2">
            <button
              type="button"
              onClick={handleResetGacha}
              disabled={savingGacha}
              className="inline-flex min-h-10 items-center gap-2 rounded-xl border border-border bg-surface px-3.5 py-2 text-xs font-semibold text-foreground-secondary transition-colors hover:bg-surface-secondary hover:text-foreground disabled:opacity-50 cursor-pointer"
            >
              <RotateCcw className="h-3.5 w-3.5" />
              Reset defaults
            </button>
            <button
              type="button"
              onClick={handleSaveGacha}
              disabled={savingGacha || !gachaDirty || !gachaValidation.valid}
              className="inline-flex min-h-10 items-center gap-2 rounded-xl bg-primary px-4 py-2 text-xs font-semibold text-on-primary transition-colors hover:bg-primary-hover disabled:opacity-50 cursor-pointer"
            >
              <Check className="h-3.5 w-3.5" />
              Save
            </button>
          </div>
        }
      >
        <div className="overflow-hidden rounded-2xl border border-border bg-surface shadow-stage-1">
          <div className="flex flex-wrap items-center justify-between gap-4 bg-surface-secondary px-5 py-3">
            <div className="flex flex-wrap items-center gap-4">
              <div className="flex items-center gap-2">
                <label
                  htmlFor="gacha-soft-pity"
                  className="text-xs font-semibold text-foreground-secondary"
                >
                  Soft Pity Roll
                </label>
                <input
                  id="gacha-soft-pity"
                  type="number"
                  inputMode="numeric"
                  min="1"
                  max="1000"
                  disabled={savingGacha}
                  value={gachaDraft.softPityStart}
                  onChange={(e) =>
                    setGachaDraft((prev) => ({ ...prev, softPityStart: e.target.value }))
                  }
                  className={`w-20 rounded-lg border bg-surface px-2.5 py-1 text-right font-mono text-xs font-semibold tabular-nums text-foreground focus:outline-none focus:ring-2 ${
                    gachaValidation.errors.softPityStart
                      ? 'border-error focus:ring-error'
                      : 'border-border focus:ring-primary'
                  }`}
                />
              </div>

              <div className="flex items-center gap-2">
                <label
                  htmlFor="gacha-hard-pity"
                  className="text-xs font-semibold text-foreground-secondary"
                >
                  Hard Pity Roll
                </label>
                <input
                  id="gacha-hard-pity"
                  type="number"
                  inputMode="numeric"
                  min="1"
                  max="1000"
                  disabled={savingGacha}
                  value={gachaDraft.hardPityLimit}
                  onChange={(e) =>
                    setGachaDraft((prev) => ({ ...prev, hardPityLimit: e.target.value }))
                  }
                  className={`w-20 rounded-lg border bg-surface px-2.5 py-1 text-right font-mono text-xs font-semibold tabular-nums text-foreground focus:outline-none focus:ring-2 ${
                    gachaValidation.errors.hardPityLimit
                      ? 'border-error focus:ring-error'
                      : 'border-border focus:ring-primary'
                  }`}
                />
              </div>
            </div>

            {gachaValidation.errors.cashbackPool && (
              <span className="font-mono text-xs font-semibold text-error">
                {gachaValidation.errors.cashbackPool}
              </span>
            )}
          </div>

          <div className="divide-y divide-border">
            {GACHA_RATE_ROWS.map((row) => (
              <RateRow
                key={row.key}
                id={`gacha-${row.key}`}
                label={row.label}
                badge={row.badge}
                badgeVariant={row.badgeVariant}
                icon={row.icon}
                value={gachaDraft[row.key]}
                error={gachaValidation.errors[row.key]}
                disabled={savingGacha}
                onChange={(val) =>
                  setGachaDraft((prev) => ({ ...prev, [row.key]: val }))
                }
              />
            ))}

            <div className="flex flex-col gap-2.5 bg-surface-secondary/60 px-5 py-3.5 sm:flex-row sm:items-center sm:justify-between">
              <div className="flex min-w-0 items-center gap-2.5">
                <Coins className="h-4 w-4 shrink-0 text-foreground-muted" aria-hidden="true" />
                <span className="text-sm font-semibold text-foreground-secondary">
                  +5 Points
                </span>
                <Badge variant="outline">Auto Remainder</Badge>
              </div>
              <span
                className={`font-mono text-sm font-semibold tabular-nums ${
                  gachaValidation.remainder5Pct < 0 ? 'text-error' : 'text-foreground-secondary'
                }`}
              >
                {gachaValidation.remainder5Pct}%
              </span>
            </div>
          </div>
        </div>
      </DashboardSection>

      {/* 2. Egg Smash */}
      <DashboardSection
        title="Egg Smash"
        actions={
          <div className="flex flex-wrap items-center gap-2">
            <button
              type="button"
              onClick={handleResetEgg}
              disabled={savingEgg}
              className="inline-flex min-h-10 items-center gap-2 rounded-xl border border-border bg-surface px-3.5 py-2 text-xs font-semibold text-foreground-secondary transition-colors hover:bg-surface-secondary hover:text-foreground disabled:opacity-50 cursor-pointer"
            >
              <RotateCcw className="h-3.5 w-3.5" />
              Reset defaults
            </button>
            <button
              type="button"
              onClick={handleSaveEgg}
              disabled={savingEgg || !eggDirty || !eggValidation.valid}
              className="inline-flex min-h-10 items-center gap-2 rounded-xl bg-primary px-4 py-2 text-xs font-semibold text-on-primary transition-colors hover:bg-primary-hover disabled:opacity-50 cursor-pointer"
            >
              <Check className="h-3.5 w-3.5" />
              Save
            </button>
          </div>
        }
      >
        <div className="overflow-hidden rounded-2xl border border-border bg-surface shadow-stage-1">
          {eggValidation.errors.eggPool && (
            <div className="bg-surface-secondary px-5 py-3 text-right">
              <span className="font-mono text-xs font-semibold text-error">
                {eggValidation.errors.eggPool}
              </span>
            </div>
          )}

          <div className="divide-y divide-border">
            {EGG_SMASH_ROWS.map((row) => (
              <RateRow
                key={row.key}
                id={`egg-${row.key}`}
                label={row.label}
                badge={row.badge}
                badgeVariant={row.badgeVariant}
                icon={row.icon}
                value={eggDraft[row.key]}
                error={eggValidation.errors[row.key]}
                disabled={savingEgg}
                onChange={(val) =>
                  setEggDraft((prev) => ({ ...prev, [row.key]: val }))
                }
              />
            ))}

            <div className="flex flex-col gap-2.5 bg-surface-secondary/60 px-5 py-3.5 sm:flex-row sm:items-center sm:justify-between">
              <div className="flex min-w-0 items-center gap-2.5">
                <Hammer className="h-4 w-4 shrink-0 text-foreground-muted" aria-hidden="true" />
                <span className="text-sm font-semibold text-foreground-secondary">
                  +50 Points Egg
                </span>
                <Badge variant="outline">Auto Remainder</Badge>
              </div>
              <span
                className={`font-mono text-sm font-semibold tabular-nums ${
                  eggValidation.remainder50Pct < 0 ? 'text-error' : 'text-foreground-secondary'
                }`}
              >
                {eggValidation.remainder50Pct}%
              </span>
            </div>
          </div>
        </div>
      </DashboardSection>
    </div>
  );
}
