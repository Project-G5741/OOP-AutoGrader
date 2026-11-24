import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Sparkles,
  X,
  Coins,
  Ticket,
  Zap,
  Award,
  Crown,
  ArrowRight,
  Check,
  Hammer,
} from 'lucide-react';
import ModalOverlay from '../ui/ModalOverlay';
import EggSmashPanel from './EggSmashPanel';
import { useToast } from '../ui/Toast';
import { useDialog } from '../../context/DialogContext';
import { formatNumber } from '../../utils/formatters';
import { apiFetch } from '../../utils/apiFetch';
import { authHeaders } from '../../utils/authHeaders';
import { friendlyLoadErrorFromResponse, toFriendlyError } from '../../utils/apiError';
import { mapServerPullToOutcome, formatPercent } from '../../utils/gachaServer';
import { STUDENT_MASCOTS } from './studentMascots';
import {
  CosmicCrystalGem,
  PrimogemStar,
  CelestialArchTopBar,
  CelestialIdleBannerStage,
  GenshinMeteorCutscene,
  GenshinSingleRevealStage,
  GenshinWishSummaryStage,
  getItemRarityKey,
  getRarityTheme,
} from './GachaCelestialStage';

export { CosmicCrystalGem };

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';

// Economics & Pity constants
export const GACHA_COST_SINGLE_POINTS = 100;
export const GACHA_COST_MULTI_POINTS = 900; // 10% discount
export const HARD_PITY_LIMIT = 100;
export const SOFT_PITY_START = 90;

// Base odds constants (fallback when /gacha-config has not loaded yet or in Admin sandbox)
export const DIRECT_LEGENDARY_RATE = 1 / 6000; // ~0.0001667 (0.0167%)
export const CRYSTAL_BASE_RATE = 0.0015; // 0.15%
/** Ticket soft-pity ramp from roll 90 toward guaranteed crystal at roll 100 (tickets only). */
export const SOFT_PITY_RATE_STEP =
  (1 - CRYSTAL_BASE_RATE) / (HARD_PITY_LIMIT - SOFT_PITY_START);
export const RANDOM_MASCOT_RATE = 0.003; // 0.3%

const SANDBOX_MASCOT_POOL = [
  'bunny',
  'cat',
  'bear',
  'capybara',
  'panda',
  'penguin',
  'alpaca',
  'vit',
  'hai_cau',
  'hamster',
  'chuot_tui',
  'red_panda',
  'shiba',
  'dai_bang',
  'pomeranian',
  'cho_soi',
  'ho_diep',
  'huyen_vu',
  'thien_nga',
  'chu_tuoc',
  'bach_ho',
  'cuu_vi',
  'thanh_long',
];
const SANDBOX_LEGENDARY_POOL = [
  'zerotwo',
  'con_bang',
  'cung_ky',
  'stella_witch',
  'linh_loc',
  'ki_lan',
  'sakura_princess',
];

const GACHA_POINTS_SPEND_PREF_KEY = 'gacha_points_spend_pref';
const VIETNAM_TZ = 'Asia/Ho_Chi_Minh';

function vietnamTodayKey() {
  return new Intl.DateTimeFormat('en-CA', {
    timeZone: VIETNAM_TZ,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(new Date());
}

function readGachaPointsSpendPref() {
  try {
    const raw = JSON.parse(localStorage.getItem(GACHA_POINTS_SPEND_PREF_KEY) || 'null');
    if (!raw || raw.date !== vietnamTodayKey() || typeof raw.allow !== 'boolean') {
      return null;
    }
    return raw.allow;
  } catch {
    return null;
  }
}

function writeGachaPointsSpendPref(allow) {
  localStorage.setItem(
    GACHA_POINTS_SPEND_PREF_KEY,
    JSON.stringify({ date: vietnamTodayKey(), allow }),
  );
}

/**
 * In-modal demo roll simulation strictly for Admin sandbox testing (`isAdmin === true`).
 * Student rolls always execute authoritatively on the server via `POST /api/students/streak/gacha-roll`.
 */
function rollAdminSandboxOnce(currentPity, usedTickets, sessionOwnedMascots, rateConfig) {
  const nextAttempt = currentPity + 1;
  const directLegendaryRate = rateConfig?.directLegendaryRate ?? DIRECT_LEGENDARY_RATE;
  const ticketCrystalBaseRate = rateConfig?.crystalBaseRate ?? CRYSTAL_BASE_RATE;
  const pointCrystalBaseRate = rateConfig?.pointCrystalRate ?? 0.0001;
  const randomMascotRate = rateConfig?.randomMascotRate ?? RANDOM_MASCOT_RATE;
  const hardPity = rateConfig?.hardPityLimit ?? HARD_PITY_LIMIT;
  const softPity = rateConfig?.softPityStart ?? SOFT_PITY_START;

  const availableLegendaries = SANDBOX_LEGENDARY_POOL.filter(
    (id) => !sessionOwnedMascots.includes(id),
  );

  if (availableLegendaries.length > 0 && Math.random() < directLegendaryRate) {
    const pickedId =
      availableLegendaries[Math.floor(Math.random() * availableLegendaries.length)];
    sessionOwnedMascots.push(pickedId);
    const info = STUDENT_MASCOTS[pickedId];
    return {
      outcome: {
        item: {
          id: `legendary_${pickedId}`,
          name: info?.name || pickedId,
          rarity: 'legendary',
          tierName: 'Legendary Mascot',
          type: 'mascot',
          mascotId: pickedId,
          pointsRefund: 0,
        },
        wasPity: false,
      },
      nextPity: nextAttempt,
    };
  }

  const isTicketHardPity = usedTickets && nextAttempt >= hardPity;
  const span = Math.max(1, hardPity - softPity);
  let crystalRate = usedTickets ? ticketCrystalBaseRate : pointCrystalBaseRate;
  if (nextAttempt >= softPity) {
    if (usedTickets) {
      const step = Math.max(0, (1 - ticketCrystalBaseRate) / span);
      crystalRate = Math.min(1, ticketCrystalBaseRate + (nextAttempt - softPity) * step);
    } else {
      const cap = Math.max(pointCrystalBaseRate, 0.99);
      const step = Math.max(0, (cap - pointCrystalBaseRate) / span);
      crystalRate = Math.min(cap, pointCrystalBaseRate + (nextAttempt - softPity) * step);
    }
  }

  if (isTicketHardPity || Math.random() < crystalRate) {
    return {
      outcome: {
        item: {
          id: 'crystal',
          name: 'Crystal Stone',
          rarity: 'crystal',
          tierName: 'Crystal',
          type: 'crystal',
          crystalAmount: 1,
          pointsRefund: 0,
        },
        wasPity: isTicketHardPity || nextAttempt >= softPity,
      },
      nextPity: 0,
    };
  }

  const availableMascots = SANDBOX_MASCOT_POOL.filter(
    (id) => !sessionOwnedMascots.includes(id),
  );
  if (availableMascots.length > 0 && Math.random() < randomMascotRate) {
    const pickedId = availableMascots[Math.floor(Math.random() * availableMascots.length)];
    sessionOwnedMascots.push(pickedId);
    const info = STUDENT_MASCOTS[pickedId];
    const tier = info?.tier || 'rare';
    return {
      outcome: {
        item: {
          id: `mascot_${pickedId}`,
          name: info?.name || pickedId,
          rarity: tier,
          tierName: `${tier.toUpperCase()} Mascot`,
          type: 'mascot',
          mascotId: pickedId,
          pointsRefund: 0,
        },
        wasPity: false,
      },
      nextPity: nextAttempt,
    };
  }

  const cb300 = rateConfig?.cashbackRate300 ?? 0.025;
  const cb150 = rateConfig?.cashbackRate150 ?? 0.06;
  const cb60 = rateConfig?.cashbackRate60 ?? 0.18;
  const cb20 = rateConfig?.cashbackRate20 ?? 0.42;

  const r = Math.random();
  let pts = 5;
  if (r < cb300) pts = 300;
  else if (r < cb300 + cb150) pts = 150;
  else if (r < cb300 + cb150 + cb60) pts = 60;
  else if (r < cb300 + cb150 + cb60 + cb20) pts = 20;

  return {
    outcome: {
      item: {
        id: `points_${pts}`,
        name: `+${pts} Points`,
        rarity: 'point',
        tierName: 'Reward points',
        type: 'points',
        pointsRefund: pts,
      },
      wasPity: false,
    },
    nextPity: nextAttempt,
  };
}

export default function MascotGachaModal({
  open,
  onClose,
  initialPoints = 1250,
  onPointsChange,
  crystals = 0,
  onCrystalsChange,
  tickets: controlledTickets,
  onTicketsChange,
  ownedMascots = ['fox'],
  onOwnedMascotsChange,
  initialGachaPity = 0,
  onWalletSync,
  isAdmin = false,
  initialTab = 'wheel',
}) {
  const [activeTab, setActiveTab] = useState(initialTab);
  const [eggSmashBusy, setEggSmashBusy] = useState(false);
  const [points, setPoints] = useState(() => {
    if (!isAdmin) return initialPoints;
    const saved = localStorage.getItem('gacha_demo_points');
    return saved !== null ? parseInt(saved, 10) : initialPoints;
  });

  const [tickets, setTickets] = useState(() => {
    if (controlledTickets !== undefined) return controlledTickets;
    const saved = localStorage.getItem('gacha_demo_tickets');
    return saved !== null ? parseInt(saved, 10) : isAdmin ? 15 : 0;
  });

  const [demoCrystals, setDemoCrystals] = useState(() => crystals);

  useEffect(() => {
    setDemoCrystals(crystals);
  }, [crystals]);

  useEffect(() => {
    if (controlledTickets !== undefined) {
      setTickets(controlledTickets);
    }
  }, [controlledTickets]);

  useEffect(() => {
    if (!isAdmin && initialPoints !== undefined) {
      setPoints(initialPoints);
    }
  }, [isAdmin, initialPoints]);

  const [pityCount, setPityCount] = useState(() =>
    isAdmin
      ? (() => {
          const saved = localStorage.getItem('gacha_demo_pity');
          return saved !== null ? parseInt(saved, 10) : 0;
        })()
      : initialGachaPity,
  );
  const [rateConfig, setRateConfig] = useState(null);
  const [rolling, setRolling] = useState(false);

  // Genshin Wish Phases: 'idle' | 'meteor' | 'reveal' | 'summary'
  const [phase, setPhase] = useState('idle');
  const [currentPulls, setCurrentPulls] = useState([]);
  const [highestRarityKey, setHighestRarityKey] = useState('point');
  const [revealIndex, setRevealIndex] = useState(0);
  const [inspectingSingleFromSummary, setInspectingSingleFromSummary] = useState(false);

  const [showOddsModal, setShowOddsModal] = useState(false);
  const [pointsSpendPrompt, setPointsSpendPrompt] = useState(null);
  const [rememberPointsChoice, setRememberPointsChoice] = useState(false);
  const showToast = useToast();
  const dialog = useDialog();

  // Persist demo counters for Admin sandbox only
  useEffect(() => {
    if (!isAdmin) return;
    localStorage.setItem('gacha_demo_points', String(points));
    localStorage.setItem('gacha_demo_tickets', String(tickets));
    localStorage.setItem('gacha_demo_pity', String(pityCount));
  }, [isAdmin, points, tickets, pityCount]);

  useEffect(() => {
    if (!isAdmin) {
      setPityCount(initialGachaPity);
    }
  }, [isAdmin, initialGachaPity]);

  useEffect(() => {
    if (!open) return undefined;
    let cancelled = false;
    (async () => {
      try {
        const response = await apiFetch(`${API_BASE}/api/students/streak/gacha-config`, {
          headers: authHeaders(),
        });
        if (!response.ok || cancelled) return;
        const body = await response.json();
        if (!cancelled) setRateConfig(body);
      } catch {
        /* fall back to exported defaults */
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [open]);

  useEffect(() => {
    if (!open) {
      setPointsSpendPrompt(null);
      setPhase('idle');
      setInspectingSingleFromSummary(false);
      setEggSmashBusy(false);
    } else {
      setActiveTab(initialTab || 'wheel');
    }
  }, [open, initialTab]);

  const costSingle = rateConfig?.costSinglePoints ?? GACHA_COST_SINGLE_POINTS;
  const costMulti = rateConfig?.costMultiPoints ?? GACHA_COST_MULTI_POINTS;
  const pityHard = rateConfig?.hardPityLimit ?? HARD_PITY_LIMIT;
  const displayedCrystals = activeTab === 'egg-smash' ? crystals : (isAdmin ? demoCrystals : crystals);
  const displayedPoints = activeTab === 'egg-smash' ? initialPoints : points;

  if (!open) return null;

  const presentRollResults = (results) => {
    let bestKey = 'point';
    let bestRank = 0;

    for (const rollOutcome of results) {
      const key = getItemRarityKey(rollOutcome.item);
      const rank = getRarityTheme(key).rank;
      if (rank > bestRank) {
        bestRank = rank;
        bestKey = key;
      }
    }

    setCurrentPulls(results);
    setHighestRarityKey(bestKey);
    setRevealIndex(0);
    setInspectingSingleFromSummary(false);
    setPhase('meteor');
  };

  const executeAdminSandboxGacha = (count) => {
    const useTickets = tickets >= count;
    const requiredPoints = count === 1 ? costSingle : costMulti;

    if (!useTickets && points < requiredPoints) {
      dialog.alert({
        level: 'warning',
        title: 'Insufficient Balance',
        message: `Not enough Roll Tickets or Points! Need ${formatNumber(requiredPoints)} Points or ${count} Ticket${count > 1 ? 's' : ''}.`,
      });
      return;
    }

    let nextTickets = tickets;
    let nextPoints = points;
    if (useTickets) {
      nextTickets -= count;
    } else {
      nextPoints -= requiredPoints;
    }

    let rollingPity = pityCount;
    let crystalsGained = 0;
    let cashbackPts = 0;
    const sessionOwned = [...ownedMascots];
    const results = [];

    for (let i = 0; i < count; i += 1) {
      const { outcome, nextPity } = rollAdminSandboxOnce(
        rollingPity,
        useTickets,
        sessionOwned,
        rateConfig,
      );
      rollingPity = nextPity;
      results.push(outcome);
      if (outcome.item.type === 'crystal') {
        crystalsGained += outcome.item.crystalAmount || 1;
      }
      if (outcome.item.pointsRefund) {
        cashbackPts += outcome.item.pointsRefund;
      }
    }

    nextPoints += cashbackPts;
    setTickets(nextTickets);
    setPoints(nextPoints);
    setPityCount(rollingPity);
    onTicketsChange?.(nextTickets);
    onPointsChange?.(nextPoints);

    if (crystalsGained > 0) {
      const updatedCrystals = demoCrystals + crystalsGained;
      setDemoCrystals(updatedCrystals);
      onCrystalsChange?.(updatedCrystals);
    }
    if (sessionOwned.length !== ownedMascots.length) {
      onOwnedMascotsChange?.(sessionOwned);
    }

    presentRollResults(results);
  };

  const executeGacha = async (count) => {
    if (rolling) return;

    if (isAdmin) {
      executeAdminSandboxGacha(count);
      return;
    }

    const useTickets = tickets >= count;
    const requiredPoints = count === 1 ? costSingle : costMulti;

    if (!useTickets && points < requiredPoints) {
      dialog.alert({
        level: 'warning',
        title: 'Insufficient Balance',
        message: `Not enough Roll Tickets or Points! Need ${formatNumber(requiredPoints)} Points or ${count} Ticket${count > 1 ? 's' : ''}.`,
      });
      return;
    }

    setRolling(true);
    try {
      const response = await apiFetch(`${API_BASE}/api/students/streak/gacha-roll`, {
        method: 'POST',
        headers: { ...authHeaders(), 'Content-Type': 'application/json' },
        body: JSON.stringify({ count }),
      });
      if (!response.ok) {
        const message =
          response.status === 409
            ? 'Not enough Roll Tickets or Points.'
            : await friendlyLoadErrorFromResponse(response);
        dialog.alert({ level: 'warning', title: 'Summon failed', message });
        return;
      }
      const body = await response.json();
      const results = (body.pulls || []).map(mapServerPullToOutcome);

      setPityCount(body.gachaPity ?? pityCount);
      setPoints(body.points ?? points);
      setTickets(body.gachaTickets ?? tickets);
      onWalletSync?.({
        points: body.points,
        gachaTickets: body.gachaTickets,
        crystals: body.crystals,
        gachaPity: body.gachaPity,
      });
      onPointsChange?.(body.points);
      onTicketsChange?.(body.gachaTickets);
      if (Array.isArray(body.ownedMascotIds)) {
        onOwnedMascotsChange?.(body.ownedMascotIds);
      }
      presentRollResults(results);
    } catch (err) {
      showToast(toFriendlyError(err), 'error');
    } finally {
      setRolling(false);
    }
  };

  const triggerGacha = (count) => {
    const useTickets = tickets >= count;
    const requiredPoints = count === 1 ? costSingle : costMulti;

    if (useTickets) {
      executeGacha(count);
      return;
    }

    if (points < requiredPoints) {
      dialog.alert({
        level: 'warning',
        title: 'Insufficient Balance',
        message: `Not enough Roll Tickets or Points! Need ${formatNumber(requiredPoints)} Points or ${count} Ticket${count > 1 ? 's' : ''}.`,
      });
      return;
    }

    const pref = readGachaPointsSpendPref();
    if (pref === true) {
      executeGacha(count);
      return;
    }
    if (pref === false) {
      showToast('You chose not to spend points on rolls for today.', 'warning');
      return;
    }

    setRememberPointsChoice(false);
    setPointsSpendPrompt({ count, cost: requiredPoints });
  };

  const confirmPointsSpend = () => {
    if (!pointsSpendPrompt) return;
    const { count } = pointsSpendPrompt;
    if (rememberPointsChoice) {
      writeGachaPointsSpendPref(true);
    }
    setPointsSpendPrompt(null);
    executeGacha(count);
  };

  const declinePointsSpend = () => {
    if (rememberPointsChoice) {
      writeGachaPointsSpendPref(false);
    }
    setPointsSpendPrompt(null);
  };

  const handleAdvanceReveal = () => {
    if (inspectingSingleFromSummary) {
      setInspectingSingleFromSummary(false);
      setPhase('summary');
      return;
    }
    if (revealIndex + 1 < currentPulls.length) {
      setRevealIndex((idx) => idx + 1);
    } else {
      setPhase('summary');
    }
  };

  const handleInspectFromSummary = (idx) => {
    setRevealIndex(idx);
    setInspectingSingleFromSummary(true);
    setPhase('reveal');
  };

  const modalLocked = phase === 'meteor' || rolling || eggSmashBusy;

  return (
    <ModalOverlay onBackdropClick={modalLocked ? undefined : onClose}>
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="gacha-title"
        className={`relative flex max-h-[95vh] w-full max-w-4xl flex-col overflow-hidden rounded-[28px] border-2 shadow-[0_24px_64px_rgba(0,0,0,0.48)] ${
          activeTab === 'egg-smash'
            ? 'border-[#5C3A1B] bg-[#2A4814] text-[#2D1B0D]'
            : 'border-[#E6C37A] bg-[#FDF6E7] text-[#2D1B0D]'
        }`}
        onClick={(e) => e.stopPropagation()}
      >
        <h2 id="gacha-title" className="sr-only">
          {activeTab === 'egg-smash' ? 'Egg Smash' : 'Crystal Wheel'}
        </h2>

        {activeTab === 'egg-smash' ? (
          <EggSmashPanel
            points={initialPoints}
            crystals={crystals}
            onBusyChange={setEggSmashBusy}
            onSwitchToWheel={() => setActiveTab('wheel')}
            onClose={onClose}
            onWalletSync={(wallet) => {
              if (!wallet) return;
              if (!isAdmin && typeof wallet.points === 'number') {
                setPoints(wallet.points);
                onPointsChange?.(wallet.points);
              }
              if (typeof wallet.crystals === 'number') {
                onCrystalsChange?.(wallet.crystals);
              }
              onWalletSync?.(wallet);
            }}
          />
        ) : (
        <>
        {/* Top Celestial Header Bar (Crystal Wheel mode) */}
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-[#EFE0C2] bg-[#FFFBF2] px-5 py-3 sm:px-6">
          {/* Left Emblem + Mode Switcher */}
          <div className="flex flex-wrap items-center gap-3">
            <div className="relative flex h-10 w-10 shrink-0 items-center justify-center rounded-full border border-[#ECCD8E] bg-[#FBEBD0] shadow-sm">
              <CosmicCrystalGem size={26} glow={false} />
            </div>
            <div
              role="tablist"
              aria-label="Game mode"
              className="inline-flex items-center rounded-full border border-[#E5CCA1] bg-[#F6E8CC] p-1"
            >
              <button
                type="button"
                role="tab"
                aria-selected={true}
                disabled={modalLocked}
                className="flex cursor-default items-center gap-1.5 rounded-full bg-[#2B180A] px-3.5 py-1.5 text-xs font-extrabold text-[#FEF3C7] shadow-sm"
              >
                <Sparkles className="h-3.5 w-3.5" />
                <span>Crystal Wheel</span>
              </button>
              <button
                type="button"
                role="tab"
                aria-selected={false}
                disabled={modalLocked}
                onClick={() => setActiveTab('egg-smash')}
                className="flex cursor-pointer items-center gap-1.5 rounded-full px-3.5 py-1.5 text-xs font-extrabold text-[#6E4720] transition-colors hover:text-[#2B180A] disabled:cursor-not-allowed"
              >
                <Hammer className="h-3.5 w-3.5" />
                <span>Egg Smash</span>
              </button>
            </div>
            <div className="hidden items-center gap-0.5 sm:flex" aria-hidden="true">
              <PrimogemStar size={15} color="#EAB308" />
              <PrimogemStar size={9} color="#F59E0B" className="-mt-2" />
            </div>
          </div>

          {/* Right Currency Pills & Close */}
          <div className="flex flex-wrap items-center gap-2">
            {/* Tickets Pill */}
            <div className="flex items-center gap-1.5 rounded-full border border-[#E6CBA0] bg-[#F7E7CA] px-3 py-1.5 text-xs font-bold text-[#3B2313]">
              <Ticket className="h-3.5 w-3.5 text-[#C97A16]" />
              <span className="tabular-nums font-extrabold">{tickets}</span>
              <span className="text-[#6E4720]">Tickets</span>
            </div>

            {/* Points Pill */}
            <div className="flex items-center gap-1.5 rounded-full border border-[#E6CBA0] bg-[#F7E7CA] px-3 py-1.5 text-xs font-bold text-[#3B2313]">
              <Coins className="h-3.5 w-3.5 text-[#C97A16]" />
              <span className="tabular-nums font-extrabold">{formatNumber(displayedPoints)}</span>
              <span className="text-[#6E4720]">pts</span>
            </div>

            {/* Highlighted Crystal Pill */}
            <div className="flex items-center gap-1.5 rounded-full border-2 border-[#EAB308] bg-[#FEF08A] px-3.5 py-1.5 text-xs font-extrabold text-[#713F12] shadow-[0_2px_10px_rgba(234,179,8,0.28)]">
              <CosmicCrystalGem size={16} glow={false} />
              <span className="tabular-nums">{displayedCrystals}</span>
              <span>Crystal</span>
            </div>

            {/* Close Button */}
            <button
              type="button"
              onClick={onClose}
              disabled={modalLocked}
              className="ml-0.5 cursor-pointer rounded-full p-1.5 text-[#6E4720] transition-colors hover:bg-[#F3E2C0] hover:text-[#2B180A] focus:outline-none focus-visible:ring-2 focus-visible:ring-[#D97706] disabled:opacity-25"
              aria-label="Close"
            >
              <X className="h-5 w-5" />
            </button>
          </div>
        </div>

        {/* Main Stage Body */}
        <div className="relative flex min-h-0 flex-1 flex-col overflow-y-auto overflow-x-hidden">
          {phase === 'idle' && (
            <CelestialArchTopBar
              pityCount={pityCount}
              pityHard={pityHard}
              softPityStart={rateConfig?.softPityStart ?? SOFT_PITY_START}
              onOpenOdds={() => setShowOddsModal(true)}
            />
          )}

          <AnimatePresence mode="wait">
            {phase === 'idle' && (
              <motion.div
                key="idle"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                transition={{ duration: 0.22 }}
                className=" -mt-[54px] flex flex-1 flex-col pt-[42px] sm:-mt-[62px] sm:pt-[48px]"
              >
                <CelestialIdleBannerStage
                  tickets={tickets}
                  costSingle={costSingle}
                  costMulti={costMulti}
                  onRollOnce={() => triggerGacha(1)}
                  onRollTen={() => triggerGacha(10)}
                  rolling={rolling}
                />
              </motion.div>
            )}

            {phase === 'meteor' && (
              <motion.div
                key="meteor"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                transition={{ duration: 0.18 }}
                className="flex flex-1 flex-col"
              >
                <GenshinMeteorCutscene
                  highestRarityKey={highestRarityKey}
                  pullCount={currentPulls.length}
                  onComplete={() => setPhase('reveal')}
                  onSkip={() => setPhase('summary')}
                />
              </motion.div>
            )}

            {phase === 'reveal' && currentPulls[revealIndex] && (
              <motion.div
                key="reveal"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                transition={{ duration: 0.2 }}
                className="flex flex-1 flex-col"
              >
                <GenshinSingleRevealStage
                  pull={currentPulls[revealIndex]}
                  index={revealIndex}
                  total={inspectingSingleFromSummary ? 1 : currentPulls.length}
                  onAdvance={handleAdvanceReveal}
                  onSkipAll={() => {
                    setInspectingSingleFromSummary(false);
                    setPhase('summary');
                  }}
                />
              </motion.div>
            )}

            {phase === 'summary' && (
              <motion.div
                key="summary"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                transition={{ duration: 0.22 }}
                className="flex flex-1 flex-col"
              >
                <GenshinWishSummaryStage
                  pulls={currentPulls}
                  pityCount={pityCount}
                  pityHard={pityHard}
                  tickets={tickets}
                  costSingle={costSingle}
                  costMulti={costMulti}
                  onInspectPull={handleInspectFromSummary}
                  onReturnIdle={() => setPhase('idle')}
                  onClose={onClose}
                  onRollAgain={(count) => triggerGacha(count)}
                  rolling={rolling}
                />
              </motion.div>
            )}
          </AnimatePresence>
        </div>
        </>
        )}

        {/* Admin Demo Sandbox Controls Footer (Crystal Wheel only) */}
        {isAdmin && activeTab === 'wheel' && (
          <div className="border-t border-[#ECDCBC] bg-[#FCF4E4] px-5 py-3 sm:px-6">
            <div className="flex flex-col gap-2">
              <div className="flex items-center gap-1.5 text-xs font-bold text-[#4A2E14]">
                <Zap className="h-3.5 w-3.5 text-[#D97706]" />
                <span>Gacha demo sandbox controls:</span>
              </div>
              <div className="flex flex-wrap items-center gap-2">
                <button
                  type="button"
                  onClick={() => {
                    const next = tickets + 10;
                    setTickets(next);
                    localStorage.setItem('gacha_demo_tickets', String(next));
                    onTicketsChange?.(next);
                  }}
                  className="cursor-pointer rounded-full border border-[#E5CCA1] bg-[#FFFDF7] px-3 py-1 text-xs font-semibold text-[#3B2313] shadow-sm transition-colors hover:bg-[#F7E7CA]"
                >
                  +10 Roll Tickets
                </button>

                <button
                  type="button"
                  onClick={() => {
                    const todayStr = new Date().toLocaleDateString('en-CA');
                    localStorage.removeItem(`oop_streak_tickets_claimed_${todayStr}`);
                    dialog.alert({
                      level: 'safety',
                      title: 'Ticket Progress Reset',
                      message:
                        "Cleared today's streak ticket claim progress! Try missions (Pet / Submit) to earn tickets again.",
                    });
                  }}
                  className="cursor-pointer rounded-full border border-[#EAB676] bg-[#FADFC0] px-3 py-1 text-xs font-bold text-[#8C450A] transition-colors hover:bg-[#F6D2A7]"
                  title="Clear today's streak ticket claim cache so you can retest mission ticket rewards"
                >
                  Reset today&apos;s ticket claims
                </button>

                <button
                  type="button"
                  onClick={() => {
                    const next = points + 1000;
                    setPoints(next);
                    onPointsChange?.(next);
                  }}
                  className="cursor-pointer rounded-full border border-[#E5CCA1] bg-[#FFFDF7] px-3 py-1 text-xs font-semibold text-[#3B2313] shadow-sm transition-colors hover:bg-[#F7E7CA]"
                >
                  +1,000 Points
                </button>

                <button
                  type="button"
                  onClick={() => {
                    const next = demoCrystals + 1;
                    setDemoCrystals(next);
                    onCrystalsChange?.(next);
                  }}
                  className="cursor-pointer rounded-full border border-[#EAB308] bg-[#FEF08A] px-3 py-1 text-xs font-extrabold text-[#78350F] transition-opacity hover:opacity-90"
                >
                  +1 Crystal
                </button>

                <button
                  type="button"
                  onClick={() => setPityCount(Math.max(0, pityHard - 1))}
                  className="cursor-pointer rounded-full border border-[#EAB676] bg-[#FADFC0] px-3 py-1 text-xs font-bold text-[#8C450A] transition-colors hover:bg-[#F6D2A7]"
                  title="Set pity right before hard pity so the next ticket roll is a guaranteed Crystal"
                >
                  Set Pity = {Math.max(0, pityHard - 1)} (test Crystal)
                </button>

                <button
                  type="button"
                  onClick={() => setPityCount(0)}
                  className="cursor-pointer rounded-full border border-[#E5CCA1] bg-[#FFFDF7] px-3 py-1 text-xs font-medium text-[#6E4720] shadow-sm transition-colors hover:bg-[#F7E7CA]"
                >
                  Reset Pity (0)
                </button>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Probability & Rules Modal */}
      {showOddsModal && (
        <ModalOverlay className="z-[110]" onBackdropClick={() => setShowOddsModal(false)}>
          <div
            role="dialog"
            aria-modal="true"
            aria-labelledby="odds-title"
            className="w-full max-w-lg rounded-3xl border border-border bg-surface p-6 shadow-stage-3"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center justify-between border-b border-border pb-3">
              <div className="flex items-center gap-2">
                <PrimogemStar size={18} color="#F59E0B" />
                <h3 id="odds-title" className="text-base font-bold text-foreground">
                  Reward rates &amp; cashback rules
                </h3>
              </div>
              <button
                type="button"
                onClick={() => setShowOddsModal(false)}
                className="cursor-pointer rounded-lg p-1 text-foreground-secondary hover:text-foreground focus:outline-none focus-visible:ring-2 focus-visible:ring-primary"
                aria-label="Close"
              >
                <X className="h-4 w-4" />
              </button>
            </div>

            <div className="mt-4 space-y-2.5 text-xs text-foreground-secondary">
              <p>
                <strong className="text-foreground">Crystal pity:</strong> Roll tickets guarantee a Crystal on roll{' '}
                <span className="tabular-nums font-semibold">{pityHard}</span> if none dropped earlier; from roll{' '}
                <span className="tabular-nums font-semibold">{rateConfig?.softPityStart ?? SOFT_PITY_START}</span> onward the rate ramps up.
              </p>
              <p>
                <strong className="text-foreground">Random mascot (no duplicates):</strong> Mascot drops always unlock a mascot you do not already own.
              </p>

              {/* Odds Table */}
              <div className="mt-3 overflow-hidden rounded-xl border border-border">
                <table className="w-full text-left text-[11px]">
                  <thead className="bg-surface-secondary text-foreground-muted">
                    <tr>
                      <th className="px-3 py-2">Reward</th>
                      <th className="px-3 py-2">Rate</th>
                      <th className="px-3 py-2">Effect</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-border">
                    <tr className="bg-warning-bg/75 font-bold text-warning-text">
                      <td className="px-3 py-2">
                        <span className="inline-flex items-center gap-1.5">
                          <Crown className="h-3.5 w-3.5 shrink-0" />
                          <span>Direct Legendary mascot</span>
                        </span>
                      </td>
                      <td className="px-3 py-2 tabular-nums">
                        {formatPercent(rateConfig?.directLegendaryRate ?? DIRECT_LEGENDARY_RATE)}
                      </td>
                      <td className="px-3 py-2">Unlock Legendary immediately</td>
                    </tr>
                    <tr className="bg-warning-bg/55 font-bold text-warning-text">
                      <td className="px-3 py-2">
                        <span className="inline-flex items-center gap-1.5">
                          <CosmicCrystalGem size={14} glow={false} />
                          <span>Crystal Stone</span>
                        </span>
                      </td>
                      <td className="px-3 py-2 tabular-nums">
                        {formatPercent(rateConfig?.crystalBaseRate ?? CRYSTAL_BASE_RATE)} (Pity {pityHard})
                      </td>
                      <td className="px-3 py-2">+1 Crystal</td>
                    </tr>
                    <tr className="bg-surface-secondary/60 font-semibold text-foreground">
                      <td className="px-3 py-2">
                        <span className="inline-flex items-center gap-1.5">
                          <Award className="h-3.5 w-3.5 shrink-0 text-primary" />
                          <span>Random mascot (no duplicates)</span>
                        </span>
                      </td>
                      <td className="px-3 py-2 tabular-nums">
                        {formatPercent(rateConfig?.randomMascotRate ?? RANDOM_MASCOT_RATE)}
                      </td>
                      <td className="px-3 py-2">Unlock unowned mascot</td>
                    </tr>
                    <tr>
                      <td className="px-3 py-2 font-medium text-foreground">+300 reward points</td>
                      <td className="px-3 py-2 tabular-nums">
                        {formatPercent(rateConfig?.cashbackRate300 ?? 0.025)}
                      </td>
                      <td className="px-3 py-2 font-semibold tabular-nums text-primary">+300 pts</td>
                    </tr>
                    <tr>
                      <td className="px-3 py-2 font-medium text-foreground">+150 reward points</td>
                      <td className="px-3 py-2 tabular-nums">
                        {formatPercent(rateConfig?.cashbackRate150 ?? 0.06)}
                      </td>
                      <td className="px-3 py-2 font-semibold tabular-nums text-primary">+150 pts</td>
                    </tr>
                    <tr>
                      <td className="px-3 py-2 font-medium text-foreground">+60 reward points</td>
                      <td className="px-3 py-2 tabular-nums">
                        {formatPercent(rateConfig?.cashbackRate60 ?? 0.18)}
                      </td>
                      <td className="px-3 py-2 font-semibold tabular-nums text-primary">+60 pts</td>
                    </tr>
                    <tr>
                      <td className="px-3 py-2 font-medium text-foreground">+20 reward points</td>
                      <td className="px-3 py-2 tabular-nums">
                        {formatPercent(rateConfig?.cashbackRate20 ?? 0.42)}
                      </td>
                      <td className="px-3 py-2 font-semibold tabular-nums text-primary">+20 pts</td>
                    </tr>
                    <tr>
                      <td className="px-3 py-2 text-foreground-muted">+5 reward points</td>
                      <td className="px-3 py-2 tabular-nums">
                        {formatPercent(
                          Math.max(
                            0,
                            1 -
                              ((rateConfig?.cashbackRate300 ?? 0.025) +
                                (rateConfig?.cashbackRate150 ?? 0.06) +
                                (rateConfig?.cashbackRate60 ?? 0.18) +
                                (rateConfig?.cashbackRate20 ?? 0.42)),
                          ),
                        )}
                      </td>
                      <td className="px-3 py-2 tabular-nums text-foreground-muted">+5 pts</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>

            <div className="mt-5 flex justify-end">
              <button
                type="button"
                onClick={() => setShowOddsModal(false)}
                className="min-h-9 cursor-pointer rounded-xl bg-primary px-4 text-xs font-semibold text-white hover:bg-primary-hover focus:outline-none focus-visible:ring-2 focus-visible:ring-primary"
              >
                Got it
              </button>
            </div>
          </div>
        </ModalOverlay>
      )}

      {/* Points Spend Confirmation Modal */}
      {pointsSpendPrompt && (
        <ModalOverlay className="z-[110]" onBackdropClick={declinePointsSpend}>
          <motion.div
            role="alertdialog"
            aria-modal="true"
            aria-labelledby="gacha-points-title"
            initial={{ opacity: 0, scale: 0.94, y: 10 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.94, y: 10 }}
            transition={{ type: 'spring', damping: 25, stiffness: 350 }}
            className="relative w-full max-w-md overflow-hidden rounded-3xl border border-border bg-surface p-6 shadow-stage-3"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="flex items-center justify-between gap-3">
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl border border-warning/30 bg-warning-bg text-warning-text">
                  <Coins className="h-5 w-5" />
                </div>
                <h3 id="gacha-points-title" className="text-base font-bold text-foreground">
                  Spend points to roll?
                </h3>
              </div>
              <button
                type="button"
                onClick={declinePointsSpend}
                className="cursor-pointer rounded-lg p-1.5 text-foreground-secondary transition-colors hover:bg-surface-secondary hover:text-foreground focus:outline-none focus-visible:ring-2 focus-visible:ring-primary"
                aria-label="Close"
              >
                <X className="h-4 w-4" />
              </button>
            </div>

            {/* Borderless surface-secondary tint well (no card-in-card) */}
            <div className="mt-4 space-y-2.5 rounded-2xl bg-surface-secondary/75 p-4 text-xs">
              <div className="flex items-center justify-between">
                <span className="flex items-center gap-1.5 text-foreground-secondary">
                  <Sparkles className="h-3.5 w-3.5 text-primary" />
                  Rolls
                </span>
                <span className="font-bold tabular-nums text-foreground">
                  {pointsSpendPrompt.count}x
                </span>
              </div>

              <div className="flex items-center justify-between">
                <span className="flex items-center gap-1.5 text-foreground-secondary">
                  <Ticket className="h-3.5 w-3.5 text-foreground-muted" />
                  Tickets
                </span>
                <span className="font-semibold tabular-nums text-foreground">
                  {tickets} / {pointsSpendPrompt.count}
                </span>
              </div>

              <div className="flex items-center justify-between">
                <span className="flex items-center gap-1.5 text-foreground-secondary">
                  <Coins className="h-3.5 w-3.5 text-warning-text" />
                  Cost
                </span>
                <span className="font-bold tabular-nums text-warning-text">
                  -{formatNumber(pointsSpendPrompt.cost)} pts
                </span>
              </div>

              <div className="border-t border-border/70 pt-2.5">
                <div className="flex items-center justify-between">
                  <span className="text-foreground-secondary">Balance after roll</span>
                  <div className="flex items-center gap-1.5 font-mono tabular-nums">
                    <span className="text-foreground-muted line-through">
                      {formatNumber(points)}
                    </span>
                    <ArrowRight className="h-3 w-3 text-foreground-muted" />
                    <span className="font-bold text-foreground">
                      {formatNumber(Math.max(0, points - pointsSpendPrompt.cost))} pts
                    </span>
                  </div>
                </div>
              </div>
            </div>

            {/* Remember Choice Toggle */}
            <label className="mt-3.5 flex cursor-pointer items-center gap-2.5 rounded-xl bg-surface-secondary/50 px-3.5 py-2.5 transition-colors hover:bg-surface-secondary">
              <div
                className={`flex h-4 w-4 shrink-0 items-center justify-center rounded border transition-colors ${
                  rememberPointsChoice
                    ? 'border-primary bg-primary text-white'
                    : 'border-border bg-surface'
                }`}
              >
                {rememberPointsChoice && <Check className="h-3 w-3 stroke-[3]" />}
              </div>
              <input
                type="checkbox"
                checked={rememberPointsChoice}
                onChange={(event) => setRememberPointsChoice(event.target.checked)}
                className="sr-only"
              />
              <span className="text-xs font-medium text-foreground">Remember my choice</span>
            </label>

            <div className="mt-5 flex items-center gap-2.5">
              <button
                type="button"
                onClick={declinePointsSpend}
                className="min-h-11 flex-1 cursor-pointer rounded-xl border border-border bg-surface px-4 text-xs font-semibold text-foreground transition-all duration-150 hover:bg-surface-secondary active:scale-[0.985] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={confirmPointsSpend}
                className="flex min-h-11 flex-1 cursor-pointer items-center justify-center gap-1.5 rounded-xl bg-primary px-5 text-xs font-bold text-white shadow-stage-1 transition-all duration-150 hover:bg-primary-hover active:scale-[0.985] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
              >
                <Coins className="h-4 w-4" />
                <span className="tabular-nums">Spend {formatNumber(pointsSpendPrompt.cost)} pts</span>
              </button>
            </div>
          </motion.div>
        </ModalOverlay>
      )}
    </ModalOverlay>
  );
}
