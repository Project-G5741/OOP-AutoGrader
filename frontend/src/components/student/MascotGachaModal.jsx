import { useState, useMemo, useEffect, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { 
  Sparkles, 
  RotateCw, 
  X, 
  Info, 
  Coins, 
  Ticket,
  ShieldCheck,
  Zap,
  HelpCircle,
  Gem,
  Award,
  Crown,
  FastForward,
  ChevronRight
} from 'lucide-react';
import ModalOverlay from '../ui/ModalOverlay';
import { useToast } from '../ui/Toast';
import { formatNumber } from '../../utils/formatters';
import { Mascot } from './MascotSprite';
import { MASCOT_TIERS, STUDENT_MASCOTS, mascotArt } from './studentMascots';

// Economics & Pity constants
export const GACHA_COST_SINGLE_POINTS = 100;
export const GACHA_COST_MULTI_POINTS = 900; // 10% discount
export const HARD_PITY_LIMIT = 80;
export const SOFT_PITY_START = 72;

// Ultra-rare direct legendary mascot odds: 1 in 6,000 rolls
export const DIRECT_LEGENDARY_RATE = 1 / 6000; // ~0.0001667 (0.0167%)
export const CRYSTAL_BASE_RATE = 0.0015; // 0.15% (nerfed from 1% so it doesn't pop in 30 pulls)
export const RANDOM_MASCOT_RATE = 0.003; // 0.3% (nerfed from 1.5% so mascots don't drop too easily, no duplicates)

// Handcrafted Prismatic Cosmic Crystal SVG Component
export function CosmicCrystalGem({ size = 80, className = '', glow = true }) {
  return (
    <div className={`relative inline-flex items-center justify-center ${className}`}>
      {glow && (
        <div 
          className="absolute inset-0 rounded-full blur-xl opacity-70 bg-amber-400/40 animate-pulse pointer-events-none"
        />
      )}
      <svg 
        width={size} 
        height={size * 1.15} 
        viewBox="0 0 100 115" 
        fill="none" 
        xmlns="http://www.w3.org/2000/svg"
        className="relative drop-shadow-[0_8px_16px_rgba(245,158,11,0.4)] transition-transform hover:scale-105 duration-300 select-none"
      >
        <defs>
          <linearGradient id="facetTop" x1="50" y1="5" x2="50" y2="40" gradientUnits="userSpaceOnUse">
            <stop offset="0%" stopColor="#FFFBEB" />
            <stop offset="60%" stopColor="#FDE68A" />
            <stop offset="100%" stopColor="#F59E0B" />
          </linearGradient>
          <linearGradient id="facetLeft" x1="15" y1="40" x2="50" y2="105" gradientUnits="userSpaceOnUse">
            <stop offset="0%" stopColor="#FBBF24" />
            <stop offset="50%" stopColor="#D97706" />
            <stop offset="100%" stopColor="#78350F" />
          </linearGradient>
          <linearGradient id="facetRight" x1="85" y1="40" x2="50" y2="105" gradientUnits="userSpaceOnUse">
            <stop offset="0%" stopColor="#FDE68A" />
            <stop offset="50%" stopColor="#F59E0B" />
            <stop offset="100%" stopColor="#B45309" />
          </linearGradient>
          <linearGradient id="facetCenter" x1="50" y1="35" x2="50" y2="105" gradientUnits="userSpaceOnUse">
            <stop offset="0%" stopColor="#FEF3C7" />
            <stop offset="40%" stopColor="#FBBF24" />
            <stop offset="100%" stopColor="#D97706" />
          </linearGradient>
          <linearGradient id="facetUpperLeft" x1="15" y1="40" x2="50" y2="35" gradientUnits="userSpaceOnUse">
            <stop offset="0%" stopColor="#FDE68A" />
            <stop offset="100%" stopColor="#F59E0B" />
          </linearGradient>
          <linearGradient id="facetUpperRight" x1="85" y1="40" x2="50" y2="35" gradientUnits="userSpaceOnUse">
            <stop offset="0%" stopColor="#FEF9C3" />
            <stop offset="100%" stopColor="#FBBF24" />
          </linearGradient>
        </defs>

        <polygon points="50,6 74,24 50,34 26,24" fill="url(#facetTop)" stroke="#FFF" strokeWidth="0.8" />
        <polygon points="26,24 50,34 50,6" fill="url(#facetUpperLeft)" stroke="#FFF" strokeWidth="0.6" opacity="0.9" />
        <polygon points="74,24 50,34 50,6" fill="url(#facetUpperRight)" stroke="#FFF" strokeWidth="0.6" opacity="0.9" />
        <polygon points="26,24 50,34 16,46" fill="#F59E0B" stroke="#FFF" strokeWidth="0.6" />
        <polygon points="74,24 50,34 84,46" fill="#FEF08A" stroke="#FFF" strokeWidth="0.6" />
        <polygon points="16,46 50,34 84,46" fill="#FDE68A" stroke="#FFF" strokeWidth="0.8" />
        <polygon points="16,46 50,34 32,74" fill="url(#facetLeft)" stroke="#FFF" strokeWidth="0.6" />
        <polygon points="84,46 50,34 68,74" fill="url(#facetRight)" stroke="#FFF" strokeWidth="0.6" />
        <polygon points="32,74 50,34 68,74" fill="url(#facetCenter)" stroke="#FFF" strokeWidth="0.7" />
        <polygon points="32,74 68,74 50,108" fill="url(#facetCenter)" stroke="#FFF" strokeWidth="0.8" />
        <polygon points="16,46 32,74 50,108" fill="url(#facetLeft)" stroke="#FFF" strokeWidth="0.6" />
        <polygon points="84,46 68,74 50,108" fill="url(#facetRight)" stroke="#FFF" strokeWidth="0.6" />

        <circle cx="50" cy="35" r="2.5" fill="#FFFFFF" />
        <path d="M43 35 L57 35 M50 28 L50 42" stroke="#FFFFFF" strokeWidth="1.5" strokeLinecap="round" opacity="0.8" />
        <circle cx="72" cy="26" r="1.5" fill="#FFFFFF" opacity="0.9" />
        <circle cx="34" cy="74" r="1.5" fill="#FFFFFF" opacity="0.7" />
      </svg>
    </div>
  );
}

const RANDOM_MASCOT_POOL = [
  'bunny', 
  'cat', 
  'bear', 
  'panda', 
  'penguin',
  'vit',
  'hamster',
  'chuot_tui',
  'shiba',
  'dai_bang',
  'cho_soi',
  'thanh_long',
  'bach_ho',
  'chu_tuoc',
  'huyen_vu',
];
const LEGENDARY_MASCOT_POOL = ['zerotwo', 'con_bang', 'cung_ky', 'linh_loc', 'ki_lan'];
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
  isAdmin = false,
}) {
  const [points, setPoints] = useState(() => {
    if (!isAdmin) return initialPoints;
    const saved = localStorage.getItem('gacha_demo_points');
    return saved !== null ? parseInt(saved, 10) : initialPoints;
  });

  const [tickets, setTickets] = useState(() => {
    if (controlledTickets !== undefined) return controlledTickets;
    const saved = localStorage.getItem('gacha_demo_tickets');
    return saved !== null ? parseInt(saved, 10) : (isAdmin ? 15 : 0);
  });

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

  const [pityCount, setPityCount] = useState(() => {
    const saved = localStorage.getItem('gacha_demo_pity');
    return saved !== null ? parseInt(saved, 10) : 12;
  });

  // Phases: 'idle' | 'charging' | 'burst' | 'showcase' | 'summary'
  const [phase, setPhase] = useState('idle');
  const [currentPulls, setCurrentPulls] = useState([]);
  const [highestRarity, setHighestRarity] = useState('normal');
  const [showcaseItem, setShowcaseItem] = useState(null);
  const [showcaseCountdown, setShowcaseCountdown] = useState(5);
  const [showOddsModal, setShowOddsModal] = useState(false);
  const [demoBannerOpen, setDemoBannerOpen] = useState(true);
  const [pointsSpendPrompt, setPointsSpendPrompt] = useState(null);
  const [rememberPointsChoice, setRememberPointsChoice] = useState(false);
  const showToast = useToast();

  const countdownTimerRef = useRef(null);

  // Persist the demo counters. Do not call the parent from here: this effect also
  // runs when the parent pushes a new ticket count, and echoing that value back
  // fights the header badge (it steps by 2 and never settles).
  useEffect(() => {
    localStorage.setItem('gacha_demo_points', String(points));
    localStorage.setItem('gacha_demo_tickets', String(tickets));
    localStorage.setItem('gacha_demo_pity', String(pityCount));
  }, [points, tickets, pityCount]);

  // Handle 5-second showcase countdown
  useEffect(() => {
    if (phase === 'showcase') {
      setShowcaseCountdown(5);
      countdownTimerRef.current = setInterval(() => {
        setShowcaseCountdown((prev) => {
          if (prev <= 1) {
            clearInterval(countdownTimerRef.current);
            setPhase('summary');
            return 0;
          }
          return prev - 1;
        });
      }, 1000);
    } else {
      if (countdownTimerRef.current) {
        clearInterval(countdownTimerRef.current);
      }
    }
    return () => {
      if (countdownTimerRef.current) {
        clearInterval(countdownTimerRef.current);
      }
    };
  }, [phase]);

  useEffect(() => {
    if (!open) setPointsSpendPrompt(null);
  }, [open]);

  if (!open) return null;

  // Single roll logic with duplicate protection
  const rollSingle = (currentPity, sessionOwnedMascots = []) => {
    const rng = Math.random();

    // 1. Ultra-Rare Direct Legendary Mascot (1 in 6,000) - No Duplicates
    const availableLegendaryPool = LEGENDARY_MASCOT_POOL.filter(id => !sessionOwnedMascots.includes(id));
    if (availableLegendaryPool.length > 0 && rng < DIRECT_LEGENDARY_RATE) {
      const pickedId = availableLegendaryPool[Math.floor(Math.random() * availableLegendaryPool.length)];
      sessionOwnedMascots.push(pickedId);
      const mascotInfo = STUDENT_MASCOTS[pickedId];
      return {
        item: {
          id: `legendary_${pickedId}`,
          name: `${mascotInfo.name}`,
          rarity: 'legendary',
          tierName: 'Legendary Mascot',
          type: 'mascot',
          mascotId: pickedId,
          image: mascotInfo.image,
          iconEmoji: '✨',
          pointsRefund: 0,
        },
        nextPity: currentPity + 1,
        wasPity: false,
      };
    }

    // 2. Crystal with Hard/Soft Pity & nerfed base rate
    const isHardPity = currentPity + 1 >= HARD_PITY_LIMIT;
    let crystalRate = CRYSTAL_BASE_RATE; // 0.15%

    if (currentPity + 1 >= SOFT_PITY_START) {
      crystalRate = CRYSTAL_BASE_RATE + (currentPity + 1 - SOFT_PITY_START) * 0.12;
    }

    if (isHardPity || Math.random() < crystalRate) {
      return {
        item: {
          id: 'crystal',
          name: 'Đá Crystal (Tinh Thể)',
          rarity: 'crystal',
          tierName: 'Đá Tinh Thể',
          type: 'crystal',
          crystalAmount: 1,
          pointsRefund: 0,
          description: 'Bảo vật tối thượng. Đổi mascot Legendary trong shop, hoặc 4.000 điểm mỗi viên.',
        },
        nextPity: 0, // Reset pity
        wasPity: isHardPity,
      };
    }

    // 3. Random Mascot (0.3% rate + Strict Duplicate Protection)
    const availableMascotPool = RANDOM_MASCOT_POOL.filter(id => !sessionOwnedMascots.includes(id));
    const secondaryRng = Math.random();
    if (availableMascotPool.length > 0 && secondaryRng < RANDOM_MASCOT_RATE) {
      const pickedId = availableMascotPool[Math.floor(Math.random() * availableMascotPool.length)];
      sessionOwnedMascots.push(pickedId);
      const mascotInfo = STUDENT_MASCOTS[pickedId];
      const tier = mascotInfo?.tier || 'rare';
      return {
        item: {
          id: `mascot_${pickedId}`,
          name: `${mascotInfo?.name || pickedId}`,
          rarity: tier, // 'normal' | 'rare'
          tierName: `${tier.toUpperCase()} Mascot`,
          type: 'mascot',
          mascotId: pickedId,
          iconEmoji: '🐾',
          pointsRefund: 0,
        },
        nextPity: currentPity + 1,
        wasPity: false,
      };
    }

    // 4. Points Cashback distribution (no normal/rare tiering for points)
    const pointRng = Math.random();
    let pts = 5;
    if (pointRng < 0.025) pts = 300;
    else if (pointRng < 0.025 + 0.06) pts = 150;
    else if (pointRng < 0.025 + 0.06 + 0.18) pts = 60;
    else if (pointRng < 0.025 + 0.06 + 0.18 + 0.42) pts = 20;

    return {
      item: {
        id: `points_${pts}`,
        name: `+${pts} Points`,
        rarity: 'point',
        tierName: 'Điểm thưởng',
        type: 'points',
        pointsRefund: pts,
      },
      nextPity: currentPity + 1,
      wasPity: false,
    };
  };

  const executeGacha = (count) => {
    const useTickets = tickets >= count;
    const requiredPoints = count === 1 ? GACHA_COST_SINGLE_POINTS : GACHA_COST_MULTI_POINTS;

    if (!useTickets && points < requiredPoints) {
      alert(`Bạn không đủ Vé Roll hoặc Points! Cần ${requiredPoints} Points hoặc ${count} Vé.`);
      return;
    }

    let newTickets = tickets;
    let newPoints = points;
    if (useTickets) {
      newTickets -= count;
    } else {
      newPoints -= requiredPoints;
    }

    const results = [];
    let rollingPity = pityCount;
    let earnedCrystals = 0;
    let totalCashback = 0;
    let maxRarity = 'normal';
    const newUnlockedMascots = [];
    let bestShowcaseItem = null;
    const sessionOwnedMascots = [...ownedMascots];

    for (let i = 0; i < count; i++) {
      const rollOutcome = rollSingle(rollingPity, sessionOwnedMascots);
      results.push(rollOutcome);
      rollingPity = rollOutcome.nextPity;

      const it = rollOutcome.item;

      if (it.rarity === 'crystal' || it.rarity === 'legendary') {
        if (it.type === 'crystal') {
          earnedCrystals += it.crystalAmount || 1;
        }
        maxRarity = 'legendary';
        if (!bestShowcaseItem || bestShowcaseItem.rarity !== 'legendary') {
          bestShowcaseItem = it;
        }
      } else if (it.rarity === 'epic' && maxRarity !== 'legendary') {
        maxRarity = 'epic';
        if (!bestShowcaseItem || (bestShowcaseItem.rarity !== 'legendary' && bestShowcaseItem.rarity !== 'crystal')) {
          bestShowcaseItem = it;
        }
      } else if (it.rarity === 'rare' && maxRarity === 'normal') {
        maxRarity = 'rare';
        if (!bestShowcaseItem && it.type === 'mascot') {
          bestShowcaseItem = it;
        }
      }

      // Check any mascot
      if (it.type === 'mascot' && it.mascotId) {
        newUnlockedMascots.push(it.mascotId);
        if (!bestShowcaseItem) {
          bestShowcaseItem = it;
        }
      }

      totalCashback += it.pointsRefund || 0;
    }

    // Apply currency returns. Notify the parent here, not from an effect,
    // so a streak grant on the dashboard is not written back as the old count.
    const nextPoints = newPoints + totalCashback;
    setTickets(newTickets);
    setPoints(nextPoints);
    onTicketsChange?.(newTickets);
    onPointsChange?.(nextPoints);
    if (earnedCrystals > 0) {
      onCrystalsChange?.(crystals + earnedCrystals);
    }
    if (newUnlockedMascots.length > 0) {
      const updatedOwned = Array.from(new Set([...ownedMascots, ...newUnlockedMascots]));
      onOwnedMascotsChange?.(updatedOwned);
    }

    setPityCount(rollingPity);
    setCurrentPulls(results);
    setHighestRarity(maxRarity);
    setShowcaseItem(bestShowcaseItem);

    // Direct transition to charging sequence
    setPhase('charging');
    setTimeout(() => {
      setPhase('burst');
      setTimeout(() => {
        // If an epic/legendary mascot or crystal appeared, enter 5s showcase!
        const hasShowcaseTrigger = bestShowcaseItem && (
          bestShowcaseItem.type === 'crystal' || 
          bestShowcaseItem.type === 'mascot'
        );

        if (hasShowcaseTrigger) {
          setPhase('showcase');
        } else {
          setPhase('summary');
        }
      }, 900);
    }, 1300);
  };

  const triggerGacha = (count) => {
    const useTickets = tickets >= count;
    const requiredPoints = count === 1 ? GACHA_COST_SINGLE_POINTS : GACHA_COST_MULTI_POINTS;

    if (useTickets) {
      executeGacha(count);
      return;
    }

    if (points < requiredPoints) {
      alert(`Bạn không đủ Vé Roll hoặc Points! Cần ${requiredPoints} Points hoặc ${count} Vé.`);
      return;
    }

    const pref = readGachaPointsSpendPref();
    if (pref === true) {
      executeGacha(count);
      return;
    }
    if (pref === false) {
      showToast('Hôm nay bạn đã chọn không dùng điểm để quay.', 'warning');
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

  const skipShowcase = () => {
    if (countdownTimerRef.current) {
      clearInterval(countdownTimerRef.current);
    }
    setPhase('summary');
  };

  const totalRefundedInPulls = currentPulls.reduce((sum, p) => sum + (p.item.pointsRefund || 0), 0);
  const crystalsWonInPulls = currentPulls.filter((p) => p.item.type === 'crystal').length;
  const mascotsWonInPulls = currentPulls.filter((p) => p.item.type === 'mascot');

  return (
    <ModalOverlay onBackdropClick={phase === 'charging' || phase === 'burst' ? undefined : onClose}>
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="gacha-title"
        className="relative flex max-h-[94vh] w-full max-w-4xl flex-col overflow-hidden rounded-3xl border border-border bg-surface shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header Bar */}
        <div className="flex items-center justify-between border-b border-border px-6 py-4">
          <div className="flex items-center gap-3">
            <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-amber-500/10 text-amber-500">
              <CosmicCrystalGem size={32} glow={false} />
            </div>
            <div>
              <h2 id="gacha-title" className="text-lg font-bold text-foreground">
                Vòng Quay Tinh Thể (Lucky Summon)
              </h2>
            </div>
          </div>

          <div className="flex items-center gap-2">
            {/* Currency Badges */}
            <div className="flex items-center gap-1.5 rounded-full border border-border bg-surface-secondary px-3 py-1.5 text-xs font-semibold">
              <Ticket className="h-3.5 w-3.5 text-primary" />
              <span className="tabular-nums text-foreground">{tickets}</span>
              <span className="text-foreground-muted">Vé</span>
            </div>

            <div className="flex items-center gap-1.5 rounded-full border border-border bg-surface-secondary px-3 py-1.5 text-xs font-semibold">
              <Coins className="h-3.5 w-3.5 text-warning-text" />
              <span className="tabular-nums text-foreground">{formatNumber(points)}</span>
              <span className="text-foreground-muted">pts</span>
            </div>

            <div className="flex items-center gap-1.5 rounded-full border border-amber-300 bg-amber-50 px-3 py-1.5 text-xs font-bold text-amber-800 shadow-sm">
              <CosmicCrystalGem size={18} glow={false} />
              <span className="tabular-nums">{crystals}</span>
              <span>Crystal</span>
            </div>

            {/* Close button: Enabled whenever not actively charging or bursting */}
            <button
              type="button"
              onClick={onClose}
              disabled={phase === 'charging' || phase === 'burst'}
              className="cursor-pointer rounded-lg p-1.5 text-foreground-secondary hover:text-foreground focus:outline-none focus-visible:ring-2 focus-visible:ring-primary disabled:opacity-20"
              aria-label="Đóng"
            >
              <X className="h-5 w-5" />
            </button>
          </div>
        </div>

        {/* Pity Progress Tracker */}
        <div className="border-b border-border bg-surface-secondary/40 px-6 py-2.5">
          <div className="flex items-center justify-between text-xs">
            <div className="flex items-center gap-2 font-medium text-foreground-secondary">
              <ShieldCheck className="h-4 w-4 text-primary" />
              <span>Bảo hiểm Đá Crystal (Hard Pity):</span>
              <span className="font-bold tabular-nums text-foreground">
                {pityCount} / {HARD_PITY_LIMIT}
              </span>
              {pityCount >= SOFT_PITY_START && (
                <span className="animate-pulse rounded-full bg-amber-100 px-2 py-0.5 text-[10px] font-bold text-amber-800 border border-amber-300">
                  Tỷ lệ tăng vọt (Soft Pity)
                </span>
              )}
            </div>
            <button
              type="button"
              onClick={() => setShowOddsModal(true)}
              className="flex cursor-pointer items-center gap-1 text-xs text-primary hover:underline font-medium"
            >
              <HelpCircle className="h-3.5 w-3.5" />
              <span>Xem Tỷ Lệ & Quy Tắc</span>
            </button>
          </div>

          <div className="mt-1.5 h-2 w-full overflow-hidden rounded-full bg-surface-secondary">
            <motion.div
              className={`h-full rounded-full ${
                pityCount >= SOFT_PITY_START ? 'bg-amber-500' : 'bg-primary'
              }`}
              initial={{ width: 0 }}
              animate={{ width: `${Math.min(100, (pityCount / HARD_PITY_LIMIT) * 100)}%` }}
              transition={{ duration: 0.4 }}
            />
          </div>
        </div>

        {/* Main Interaction Area */}
        <div className="relative flex min-h-[460px] flex-1 flex-col items-center justify-center p-6">
          <AnimatePresence mode="wait">
            {/* PHASE 1: IDLE / PORTAL SELECTION */}
            {phase === 'idle' && (
              <motion.div
                key="idle"
                initial={{ opacity: 0, scale: 0.96 }}
                animate={{ opacity: 1, scale: 1 }}
                exit={{ opacity: 0, scale: 0.9 }}
                className="flex w-full flex-1 flex-col items-center justify-between"
              >
                {/* Visual Sacred Crystal Summoning Altar */}
                <div className="relative my-auto flex flex-col items-center justify-center py-6 select-none">
                  <div className="absolute h-72 w-72 rounded-full border border-dashed border-amber-400/25 animate-[spin_40s_linear_infinite]" />
                  <div className="absolute h-56 w-56 rounded-full border-2 border-primary/20 animate-[spin_25s_linear_infinite_reverse]" />
                  <div className="absolute h-44 w-44 rounded-full border border-purple/20 animate-[spin_15s_linear_infinite]" />

                  <motion.div
                    animate={{
                      scale: [1, 1.06, 1],
                      rotate: [0, 2, -2, 0],
                    }}
                    transition={{ repeat: Infinity, duration: 4, ease: 'easeInOut' }}
                    className="relative z-10 flex h-40 w-40 items-center justify-center rounded-full border-2 border-amber-400/50 bg-surface shadow-2xl backdrop-blur-sm"
                  >
                    <CosmicCrystalGem size={105} glow={true} />
                    <Sparkles className="absolute -right-2 -top-2 h-7 w-7 text-amber-400 animate-bounce" />
                    <Sparkles className="absolute -left-2 -bottom-2 h-5 w-5 text-purple animate-pulse" />
                  </motion.div>

                  <div className="mt-6 text-center z-10">
                    <h3 className="text-xl font-extrabold tracking-wide text-foreground">
                      ĐÀI TRIỆU HỒI TINH THỂ VŨ TRỤ
                    </h3>
                    <p className="mt-1.5 max-w-lg text-xs text-foreground-secondary leading-relaxed">
                      Quay nhận Mascot ngẫu nhiên, Đá Crystal và hoàn lại điểm thưởng. Tích lũy 3 - 4 Crystal để đổi Mascot Legendary (chu kỳ 2.5 tháng cày nhiệm vụ sở hữu đúng 2 Legendary).
                    </p>
                  </div>
                </div>

                {/* Summon Buttons */}
                <div className="flex w-full flex-wrap items-center justify-center gap-4 pt-2">
                  <button
                    type="button"
                    onClick={() => triggerGacha(1)}
                    className="group flex min-h-[56px] min-w-[200px] cursor-pointer flex-col items-center justify-center rounded-2xl border border-border bg-surface-secondary px-6 py-2.5 transition-all hover:border-primary hover:bg-surface-tertiary focus:outline-none focus-visible:ring-2 focus-visible:ring-primary active:scale-[0.98]"
                  >
                    <span className="text-sm font-bold text-foreground">Quay 1 Lần</span>
                    <span className="mt-0.5 flex items-center gap-1 text-xs text-foreground-muted">
                      {tickets >= 1 ? (
                        <span className="font-semibold text-primary">1 Vé Roll</span>
                      ) : (
                        <span className="tabular-nums text-warning-text">{GACHA_COST_SINGLE_POINTS} pts</span>
                      )}
                    </span>
                  </button>

                  <button
                    type="button"
                    onClick={() => triggerGacha(10)}
                    className="group relative flex min-h-[56px] min-w-[220px] cursor-pointer flex-col items-center justify-center rounded-2xl border-2 border-primary bg-primary px-8 py-2.5 text-white shadow-lg transition-all hover:bg-primary-hover focus:outline-none focus-visible:ring-2 focus-visible:ring-primary active:scale-[0.98]"
                  >
                    {tickets < 10 && (
                      <span className="absolute -top-2.5 right-4 rounded-full bg-warning-bg px-2 py-0.5 text-[10px] font-bold uppercase tracking-wider text-warning-text shadow-sm border border-warning-border">
                        Giảm 10%
                      </span>
                    )}
                    <span className="text-sm font-bold text-white">Quay 10 Lần</span>
                    <span className="mt-0.5 flex items-center gap-1 text-xs text-white/90">
                      {tickets >= 10 ? (
                        <span className="font-semibold text-white">10 Vé Roll</span>
                      ) : (
                        <span className="tabular-nums font-semibold text-warning-bg">
                          {GACHA_COST_MULTI_POINTS} pts <span className="line-through text-white/60">1000</span>
                        </span>
                      )}
                    </span>
                  </button>
                </div>
              </motion.div>
            )}

            {/* PHASE 2: CHARGING ANIMATION */}
            {phase === 'charging' && (
              <motion.div
                key="charging"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                className="flex flex-col items-center justify-center py-10"
              >
                <div className="relative flex h-52 w-52 items-center justify-center">
                  <motion.div
                    animate={{ rotate: 360 }}
                    transition={{ repeat: Infinity, duration: 1, ease: 'linear' }}
                    className="absolute h-full w-full rounded-full border-4 border-dashed border-amber-400"
                  />
                  <motion.div
                    animate={{ rotate: -360 }}
                    transition={{ repeat: Infinity, duration: 0.7, ease: 'linear' }}
                    className="absolute h-40 w-40 rounded-full border-2 border-primary"
                  />
                  <motion.div
                    animate={{ scale: [0.85, 1.25, 0.95, 1.3] }}
                    transition={{ duration: 1.3, ease: 'easeInOut' }}
                    className="flex items-center justify-center"
                  >
                    <CosmicCrystalGem size={96} glow={true} />
                  </motion.div>
                </div>
                <p className="mt-6 font-bold tracking-widest text-foreground animate-pulse uppercase text-sm">
                  Đang hội tụ năng lượng vũ trụ...
                </p>
              </motion.div>
            )}

            {/* PHASE 3: RARITY BURST BEAM */}
            {phase === 'burst' && (
              <motion.div
                key="burst"
                initial={{ opacity: 0, scale: 0.8 }}
                animate={{ opacity: 1, scale: 1.15 }}
                exit={{ opacity: 0 }}
                className="flex flex-col items-center justify-center py-10"
              >
                <div
                  className={`flex h-44 w-44 items-center justify-center rounded-full shadow-2xl ${
                    highestRarity === 'legendary'
                      ? 'bg-amber-50 border-4 border-amber-400 ring-8 ring-amber-400/40'
                      : highestRarity === 'epic'
                      ? 'bg-purple-50 border-4 border-purple ring-8 ring-purple/40'
                      : 'bg-emerald-50 border-4 border-emerald-500 ring-8 ring-emerald-500/40'
                  }`}
                >
                  {highestRarity === 'legendary' ? (
                    <CosmicCrystalGem size={90} glow={false} />
                  ) : (
                    <Sparkles
                      className={`h-20 w-20 animate-spin ${
                        highestRarity === 'epic' ? 'text-purple' : 'text-emerald-500'
                      }`}
                    />
                  )}
                </div>
                <p className="mt-6 text-base font-extrabold tracking-widest text-foreground">
                  {highestRarity === 'legendary' && '✨ HÀO QUANG TINH THỂ / HUYỀN THOẠI! ✨'}
                  {highestRarity === 'epic' && '🔮 ÁNH SÁNG EPIC XUẤT HIỆN! 🔮'}
                  {highestRarity === 'rare' && '🌿 PHẦN THƯỞNG RARE! 🌿'}
                  {highestRarity === 'normal' && 'TIẾP TỤC QUAY...'}
                </p>
              </motion.div>
            )}

            {/* PHASE 3.5: 5-SECOND EPIC SHOWCASE CUTSCENE (for Crystal & Mascot) */}
            {phase === 'showcase' && showcaseItem && (
              <motion.div
                key="showcase"
                initial={{ opacity: 0, scale: 0.85 }}
                animate={{ opacity: 1, scale: 1 }}
                exit={{ opacity: 0, scale: 0.9 }}
                className="relative flex w-full flex-1 flex-col items-center justify-center py-6 select-none"
              >
                {/* Skip Button Top Right */}
                <button
                  type="button"
                  onClick={skipShowcase}
                  className="absolute right-2 top-0 flex cursor-pointer items-center gap-1.5 rounded-full border border-border bg-surface px-3 py-1.5 text-xs font-bold text-foreground shadow-sm hover:bg-surface-secondary transition-all"
                >
                  <span>Bỏ qua (Skip)</span>
                  <FastForward className="h-3.5 w-3.5 text-primary" />
                </button>

                {/* Rotating Radiance Halo */}
                <div className="absolute h-80 w-80 rounded-full border-2 border-dashed border-amber-400/30 animate-[spin_20s_linear_infinite]" />
                <div className="absolute h-64 w-64 rounded-full border border-purple/20 animate-[spin_15s_linear_infinite_reverse]" />

                {/* Main Visual Display */}
                <motion.div
                  animate={{
                    scale: [0.95, 1.08, 1],
                    y: [-4, 4, -4],
                  }}
                  transition={{ repeat: Infinity, duration: 3, ease: 'easeInOut' }}
                  className="relative z-10 flex h-48 w-48 items-center justify-center rounded-3xl border-2 border-amber-400/60 bg-amber-50/80 dark:bg-amber-950/40 shadow-2xl"
                  style={{
                    backgroundColor: showcaseItem.type === 'crystal' ? '#FEF3C7' : '#EDE9FE',
                    borderColor: showcaseItem.type === 'crystal' ? '#FCD34D' : '#C4B5FD',
                  }}
                >
                  {showcaseItem.type === 'crystal' ? (
                    <CosmicCrystalGem size={110} glow={true} />
                  ) : (
                    <Mascot 
                      size={110}
                      label={`${showcaseItem.name} mascot`}
                      directions={mascotArt(showcaseItem.mascotId).directions}
                      reactions={mascotArt(showcaseItem.mascotId).reactions}
                      tier={mascotArt(showcaseItem.mascotId).tier}
                      trackPointer
                    />
                  )}
                  <Sparkles className="absolute -top-3 -right-3 h-8 w-8 text-amber-500 animate-spin" />
                </motion.div>

                {/* Showcase Titles */}
                <div className="mt-6 text-center z-10">
                  <span className="inline-block rounded-full bg-amber-100 border border-amber-300 px-3 py-1 text-xs font-extrabold uppercase tracking-widest text-amber-800 shadow-sm">
                    {showcaseItem.type === 'crystal' ? 'BẢO VẬT TỐI THƯỢNG' : `${showcaseItem.tierName}`}
                  </span>
                  <h3 className="mt-2 text-2xl font-black tracking-wide text-foreground">
                    {showcaseItem.type === 'crystal' 
                      ? '✨ THỨC TỈNH TINH THỂ VŨ TRỤ! ✨'
                      : `🎉 THỨC TỈNH LINH VẬT ${showcaseItem.name.toUpperCase()}!`
                    }
                  </h3>
                  {showcaseItem.type === 'crystal' && (
                    <p className="mt-1.5 max-w-md text-xs text-foreground-secondary">
                      Bảo vật tối thượng dùng để đổi các Mascot Legendary trong shop!
                    </p>
                  )}
                </div>

                {/* 5-second countdown progress bar */}
                <div className="mt-8 flex flex-col items-center gap-1.5 z-10">
                  <div className="h-1.5 w-48 overflow-hidden rounded-full bg-surface-secondary">
                    <motion.div 
                      className="h-full bg-primary rounded-full"
                      initial={{ width: '100%' }}
                      animate={{ width: '0%' }}
                      transition={{ duration: 5, ease: 'linear' }}
                    />
                  </div>
                  <span className="text-[11px] font-semibold text-foreground-muted">
                    Tự động tiếp tục sau {showcaseCountdown}s (bấm Bỏ qua để xem ngay)
                  </span>
                </div>
              </motion.div>
            )}

            {/* PHASE 4: SUMMARY & RESULTS GRID */}
            {phase === 'summary' && (
              <motion.div
                key="summary"
                initial={{ opacity: 0, y: 16 }}
                animate={{ opacity: 1, y: 0 }}
                className="flex w-full flex-1 flex-col justify-between"
              >
                <div>
                  {/* Pulls Header Summary */}
                  <div className="flex flex-wrap items-center justify-between gap-3 border-b border-border pb-3">
                    <div>
                      <h4 className="text-base font-bold text-foreground">
                        Kết Quả Lượt Quay ({currentPulls.length} phần thưởng)
                      </h4>
                      <p className="text-xs text-foreground-muted">
                        Bảo hiểm Crystal hiện tại: <span className="font-bold text-foreground">{pityCount} / {HARD_PITY_LIMIT}</span>
                      </p>
                    </div>

                    <div className="flex items-center gap-2">
                      {crystalsWonInPulls > 0 && (
                        <div className="flex items-center gap-1 rounded-full border border-amber-300 bg-amber-50 px-3 py-1 text-xs font-bold text-amber-800">
                          <CosmicCrystalGem size={16} glow={false} />
                          <span>+{crystalsWonInPulls} Crystal Đã Nhận!</span>
                        </div>
                      )}
                      {mascotsWonInPulls.length > 0 && (
                        <div className="flex items-center gap-1 rounded-full border border-purple/40 bg-purple-50 px-3 py-1 text-xs font-bold text-purple">
                          <Award className="h-3.5 w-3.5" />
                          <span>+{mascotsWonInPulls.length} Mascot Mới!</span>
                        </div>
                      )}
                      <div className="flex items-center gap-1 rounded-full bg-surface-secondary px-3 py-1 text-xs font-bold text-foreground border border-border">
                        <Coins className="h-3.5 w-3.5 text-warning-text" />
                        <span>+{totalRefundedInPulls} pts Hoàn Lại</span>
                      </div>
                    </div>
                  </div>

                  {/* Rewards Grid with Explicit Colors:
                      - Normal Mascot: bg grey (#F1F5F9)
                      - Rare Mascot: bg green light (#DCFCE7)
                      - Epic Mascot: bg purple light (#EDE9FE)
                      - Legendary Mascot & Crystal: bg light yellow (#FEF3C7)
                      - Points: Clean neutral card (#F8FAFC) without normal/rare division!
                  */}
                  <div className={`mt-4 grid gap-3 overflow-y-auto max-h-[320px] p-1 ${
                    currentPulls.length === 1 
                      ? 'grid-cols-1 max-w-xs mx-auto' 
                      : 'grid-cols-2 sm:grid-cols-3 md:grid-cols-5'
                  }`}>
                    {currentPulls.map((pull, idx) => {
                      const item = pull.item;
                      const isPoints = item.type === 'points';
                      const isCrystal = item.type === 'crystal';
                      const isMascot = item.type === 'mascot';

                      // Explicit visual backgrounds requested:
                      // Normal: bg grey (#F1F5F9)
                      // Rare: bg green light (#DCFCE7)
                      // Epic: bg purple light (#EDE9FE)
                      // Legendary/Crystal: bg light yellow (#FEF3C7)
                      let cardStyle = {
                        backgroundColor: '#F8FAFC',
                        borderColor: '#E2E8F0',
                        color: '#0F172A',
                      };
                      let badgeStyle = {
                        backgroundColor: '#E2E8F0',
                        color: '#475569',
                      };

                      if (isCrystal || item.rarity === 'legendary') {
                        cardStyle = {
                          backgroundColor: '#FEF3C7',
                          borderColor: '#FCD34D',
                          color: '#78350F',
                        };
                        badgeStyle = {
                          backgroundColor: '#FDE68A',
                          color: '#92400E',
                        };
                      } else if (item.rarity === 'epic') {
                        cardStyle = {
                          backgroundColor: '#EDE9FE',
                          borderColor: '#C4B5FD',
                          color: '#4C1D95',
                        };
                        badgeStyle = {
                          backgroundColor: '#DDD6FE',
                          color: '#5B21B6',
                        };
                      } else if (item.rarity === 'rare') {
                        cardStyle = {
                          backgroundColor: '#DCFCE7',
                          borderColor: '#86EFAC',
                          color: '#14532D',
                        };
                        badgeStyle = {
                          backgroundColor: '#BBF7D0',
                          color: '#166534',
                        };
                      } else if (isMascot && item.rarity === 'normal') {
                        cardStyle = {
                          backgroundColor: '#F1F5F9',
                          borderColor: '#CBD5E1',
                          color: '#334155',
                        };
                        badgeStyle = {
                          backgroundColor: '#E2E8F0',
                          color: '#475569',
                        };
                      }

                      return (
                        <motion.div
                          key={idx}
                          initial={{ opacity: 0, scale: 0.8, y: 12 }}
                          animate={{ opacity: 1, scale: 1, y: 0 }}
                          transition={{ delay: idx * 0.04 }}
                          style={cardStyle}
                          className={`relative flex flex-col items-center rounded-2xl border-2 p-3 text-center transition-all shadow-sm ${
                            isCrystal || item.rarity === 'legendary' ? 'ring-2 ring-amber-400/40' : ''
                          }`}
                        >
                          {/* Item Icon */}
                          <div className="flex h-14 w-14 items-center justify-center">
                            {isCrystal ? (
                              <CosmicCrystalGem size={44} glow={false} />
                            ) : isMascot ? (
                              <Mascot
                                size={46}
                                label={`${item.name} mascot`}
                                directions={mascotArt(item.mascotId).directions}
                                reactions={mascotArt(item.mascotId).reactions}
                              />
                            ) : (
                              <Coins className="h-8 w-8 text-warning-text drop-shadow-sm" />
                            )}
                          </div>

                          {/* Item Title */}
                          <div className="mt-1.5 flex-1">
                            <span className="block text-xs font-bold line-clamp-1">
                              {item.name}
                            </span>
                            <span 
                              style={badgeStyle}
                              className="mt-1 inline-block rounded-md px-1.5 py-0.5 text-[10px] font-extrabold uppercase tracking-wider"
                            >
                              {item.tierName}
                            </span>
                          </div>

                          {/* Rewards Value Bottom Strip */}
                          <div className="mt-2 w-full border-t border-black/10 pt-1.5 text-[11px] font-bold">
                            {isCrystal ? (
                              <span className="text-amber-800">+1 Crystal</span>
                            ) : isMascot ? (
                              <span className="text-emerald-800">ĐÃ MỞ KHÓA!</span>
                            ) : (
                              <span className="text-foreground-secondary">+{item.pointsRefund} pts</span>
                            )}
                          </div>
                        </motion.div>
                      );
                    })}
                  </div>
                </div>

                {/* Bottom Actions: Return to idle screen, Close, or Roll again with clear currency indicator */}
                <div className="mt-4 flex items-center justify-end gap-3 border-t border-border pt-4">
                  <button
                    type="button"
                    onClick={() => setPhase('idle')}
                    className="min-h-11 cursor-pointer rounded-xl border border-border bg-surface-secondary px-5 text-xs font-semibold text-foreground transition-colors hover:bg-surface-tertiary focus:outline-none focus-visible:ring-2 focus-visible:ring-primary"
                  >
                    Màn Hình Chờ
                  </button>

                  <button
                    type="button"
                    onClick={onClose}
                    className="min-h-11 cursor-pointer rounded-xl border border-border bg-surface-secondary px-5 text-xs font-semibold text-foreground transition-colors hover:bg-surface-tertiary focus:outline-none focus-visible:ring-2 focus-visible:ring-primary"
                  >
                    Xong
                  </button>

                  <button
                    type="button"
                    onClick={() => triggerGacha(currentPulls.length)}
                    className="min-h-11 cursor-pointer rounded-xl bg-primary px-6 text-xs font-bold text-white shadow-md transition-all hover:bg-primary-hover focus:outline-none focus-visible:ring-2 focus-visible:ring-primary active:scale-[0.98] flex items-center gap-2"
                  >
                    <span>Quay Tiếp ({currentPulls.length}x)</span>
                    <span className="rounded-full bg-white/20 px-2 py-0.5 text-[11px] font-semibold text-white">
                      {tickets >= currentPulls.length
                        ? `${currentPulls.length} Vé`
                        : `${currentPulls.length === 1 ? 100 : 900} pts`}
                    </span>
                  </button>
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        </div>

        {/* Interactive Demo Sandbox Tools (Admin Only) */}
        {isAdmin && demoBannerOpen && (
          <div className="border-t border-border bg-surface-secondary px-6 py-3">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <div className="flex items-center gap-1.5 text-xs font-semibold text-foreground">
                <Zap className="h-4 w-4 text-warning-text" />
                <span>Thanh Thử Nghiệm Gacha (Demo Sandbox Controls):</span>
              </div>
              <div className="flex flex-wrap items-center gap-2">
                <button
                  type="button"
                  onClick={() => {
                    const next = tickets + 10;
                    setTickets(next);
                    localStorage.setItem('gacha_demo_tickets', next);
                    onTicketsChange?.(next);
                  }}
                  className="cursor-pointer rounded-lg border border-border bg-surface px-2.5 py-1 text-xs font-medium text-foreground hover:bg-surface-tertiary"
                >
                  +10 Vé Roll
                </button>
                <button
                  type="button"
                  onClick={() => {
                    const todayStr = new Date().toLocaleDateString('en-CA');
                    localStorage.removeItem(`oop_streak_tickets_claimed_${todayStr}`);
                    alert('Đã xóa tiến độ nhận vé Streak hôm nay! Hãy thử làm nhiệm vụ (Pet/Nộp bài) để nhận lại vé.');
                  }}
                  className="cursor-pointer rounded-lg border border-primary/40 bg-primary/10 px-2.5 py-1 text-xs font-bold text-primary hover:bg-primary/20"
                  title="Xóa cache vé Streak hôm nay để bạn có thể test lại việc nhận vé khi làm nhiệm vụ"
                >
                  Reset Claim Vé Hôm Nay
                </button>
                <button
                  type="button"
                  onClick={() => {
                    const next = points + 1000;
                    setPoints(next);
                    onPointsChange?.(next);
                  }}
                  className="cursor-pointer rounded-lg border border-border bg-surface px-2.5 py-1 text-xs font-medium text-foreground hover:bg-surface-tertiary"
                >
                  +1,000 Points
                </button>
                <button
                  type="button"
                  onClick={() => onCrystalsChange?.(crystals + 1)}
                  className="cursor-pointer rounded-lg border border-amber-300 bg-amber-50 text-amber-800 px-2.5 py-1 text-xs font-bold hover:opacity-90"
                >
                  +1 Crystal
                </button>
                <button
                  type="button"
                  onClick={() => setPityCount(79)}
                  className="cursor-pointer rounded-lg border border-warning-border bg-warning-bg px-2.5 py-1 text-xs font-bold text-warning-text hover:opacity-90"
                  title="Đặt bảo hiểm lên 79 để lần quay tiếp theo kích hoạt chắc chắn ra Đá Crystal"
                >
                  Đặt Pity = 79 (Test Nổ Crystal)
                </button>
                <button
                  type="button"
                  onClick={() => setPityCount(0)}
                  className="cursor-pointer rounded-lg border border-border bg-surface px-2 py-1 text-xs text-foreground-muted hover:bg-surface-tertiary"
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
        <ModalOverlay onBackdropClick={() => setShowOddsModal(false)}>
          <div
            role="dialog"
            aria-modal="true"
            aria-labelledby="odds-title"
            className="w-full max-w-lg rounded-3xl border border-border bg-surface p-6 shadow-2xl"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center justify-between border-b border-border pb-3">
              <h3 id="odds-title" className="text-base font-bold text-foreground">
                Tỷ Lệ Phần Thưởng & Cơ Chế Hoàn Điểm
              </h3>
              <button
                type="button"
                onClick={() => setShowOddsModal(false)}
                className="cursor-pointer rounded-lg p-1 text-foreground-secondary hover:text-foreground"
              >
                <X className="h-4 w-4" />
              </button>
            </div>

            <div className="mt-4 space-y-3 text-xs text-foreground-secondary">
              <p>
                <strong>Quy tắc Bảo hiểm (Hard Pity):</strong> Nếu sau 79 lần quay chưa xuất hiện Đá Crystal, lần thứ 80 chắc chắn 100% sẽ nhận được 1 Viên Đá Crystal. Tỷ lệ cơ sở được giới hạn ở mức 0.15% để tránh việc mới quay ~30 lần đã ra Crystal.
              </p>
              <p>
                <strong>Hiệu ứng Thức Tỉnh (Showcase 5s):</strong> Khi quay ra Đá Crystal hoặc Mascot từ Epic trở lên, màn hình vinh danh đặc biệt sẽ xuất hiện trong 5 giây (có thể bấm "Bỏ qua" để vào thẳng kết quả).
              </p>
              <p>
                <strong>Mascot Legendary Trực Tiếp (1/6000):</strong> Tỷ lệ cực hiếm 0.0167% để trực tiếp mở khóa 1 Mascot Legendary.
              </p>
              <p>
                <strong>Mascot Ngẫu Nhiên (0.3% - Chống Trùng Lặp):</strong> Tỷ lệ 0.30% mở khóa ngay 1 linh vật mới chưa sở hữu (không bao giờ ra lại mascot đã có).
              </p>
              <p>
                <strong>Đổi Mascot Legendary:</strong> Cần 3 - 4 Đá Crystal để đổi 1 Mascot Legendary trong shop. Ở điều kiện thông thường (hoàn thành đầy đủ nhiệm vụ Streak ~50 vé/tuần), sinh viên sở hữu đúng 2 Mascot Legendary trong 2.5 tháng.
              </p>

              {/* Odds Table */}
              <div className="mt-3 overflow-hidden rounded-xl border border-border">
                <table className="w-full text-left text-[11px]">
                  <thead className="bg-surface-secondary text-foreground-muted">
                    <tr>
                      <th className="px-3 py-2">Phần Thưởng</th>
                      <th className="px-3 py-2">Tỷ Lệ</th>
                      <th className="px-3 py-2">Mô Tả / Hoàn Lại</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-border">
                    <tr style={{ backgroundColor: '#FEF3C7', color: '#78350F' }} className="font-bold">
                      <td className="px-3 py-2">👑 Mascot Legendary Trực Tiếp</td>
                      <td className="px-3 py-2 tabular-nums">0.0167% (1/6000)</td>
                      <td className="px-3 py-2">Mở khóa ngay Legendary</td>
                    </tr>
                    <tr style={{ backgroundColor: '#FEF3C7', color: '#92400E' }} className="font-bold">
                      <td className="px-3 py-2">💎 Đá Crystal (Tinh Thể)</td>
                      <td className="px-3 py-2 tabular-nums">0.15% (Pity 80)</td>
                      <td className="px-3 py-2">+1 Đá Crystal</td>
                    </tr>
                    <tr style={{ backgroundColor: '#EDE9FE', color: '#4C1D95' }} className="font-medium">
                      <td className="px-3 py-2">🐾 Mascot Ngẫu Nhiên (Không trùng)</td>
                      <td className="px-3 py-2 tabular-nums">0.30%</td>
                      <td className="px-3 py-2">Mở khóa linh vật mới chưa sở hữu</td>
                    </tr>
                    <tr>
                      <td className="px-3 py-2 font-medium">+300 Points Thưởng</td>
                      <td className="px-3 py-2 tabular-nums">2.50%</td>
                      <td className="px-3 py-2 text-primary font-semibold">+300 pts</td>
                    </tr>
                    <tr>
                      <td className="px-3 py-2 font-medium">+150 Points Thưởng</td>
                      <td className="px-3 py-2 tabular-nums">6.00%</td>
                      <td className="px-3 py-2 text-primary font-semibold">+150 pts</td>
                    </tr>
                    <tr>
                      <td className="px-3 py-2 font-medium">+60 Points Thưởng</td>
                      <td className="px-3 py-2 tabular-nums">18.00%</td>
                      <td className="px-3 py-2 text-primary font-semibold">+60 pts</td>
                    </tr>
                    <tr>
                      <td className="px-3 py-2 font-medium">+20 Points Thưởng</td>
                      <td className="px-3 py-2 tabular-nums">42.00%</td>
                      <td className="px-3 py-2 text-primary font-semibold">+20 pts</td>
                    </tr>
                    <tr>
                      <td className="px-3 py-2 text-slate-500">+5 Points Thưởng</td>
                      <td className="px-3 py-2 tabular-nums">29.82%</td>
                      <td className="px-3 py-2 text-slate-500">+5 pts</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>

            <div className="mt-5 flex justify-end">
              <button
                type="button"
                onClick={() => setShowOddsModal(false)}
                className="min-h-9 cursor-pointer rounded-xl bg-primary px-4 text-xs font-semibold text-white hover:bg-primary-hover"
              >
                Đã hiểu
              </button>
            </div>
          </div>
        </ModalOverlay>
      )}

      {pointsSpendPrompt && (
        <ModalOverlay onBackdropClick={declinePointsSpend}>
          <div
            role="alertdialog"
            aria-modal="true"
            aria-labelledby="gacha-points-title"
            aria-describedby="gacha-points-body"
            className="w-full max-w-md rounded-3xl border border-border bg-surface p-6 shadow-xl"
            onClick={(event) => event.stopPropagation()}
          >
            <h3 id="gacha-points-title" className="text-lg font-bold text-foreground">
              Dùng điểm để quay?
            </h3>
            <p id="gacha-points-body" className="mt-1.5 text-xs text-foreground-secondary">
              Không đủ vé. Quay {pointsSpendPrompt.count} lần sẽ trừ{' '}
              <span className="font-semibold text-foreground">
                {formatNumber(pointsSpendPrompt.cost)} điểm
              </span>
              . Số dư hiện có: {formatNumber(points)} pts.
            </p>
            <label className="mt-4 flex cursor-pointer items-start gap-2.5 text-xs text-foreground">
              <input
                type="checkbox"
                checked={rememberPointsChoice}
                onChange={(event) => setRememberPointsChoice(event.target.checked)}
                className="mt-0.5 h-4 w-4 shrink-0 rounded border-border text-primary focus:ring-primary"
              />
              <span>
                Remember my choice.
              </span>
            </label>
            <div className="mt-6 flex justify-end gap-2">
              <button
                type="button"
                onClick={declinePointsSpend}
                className="min-h-11 cursor-pointer rounded-xl border border-border bg-surface-secondary px-4 text-xs font-semibold text-foreground hover:bg-surface-tertiary focus:outline-none focus-visible:ring-2 focus-visible:ring-primary"
              >
                Hủy
              </button>
              <button
                type="button"
                onClick={confirmPointsSpend}
                className="min-h-11 cursor-pointer rounded-xl bg-primary px-5 text-xs font-bold text-white hover:bg-primary-hover focus:outline-none focus-visible:ring-2 focus-visible:ring-primary"
              >
                Dùng điểm
              </button>
            </div>
          </div>
        </ModalOverlay>
      )}
    </ModalOverlay>
  );
}
