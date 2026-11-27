import { useEffect, useMemo, useRef, useState } from 'react';
import { motion, AnimatePresence, useReducedMotion } from 'framer-motion';
import {
  Award,
  ChevronRight,
  FastForward,
  HelpCircle,
  ShieldCheck,
  Ticket,
  Coins,
} from 'lucide-react';
import { Mascot } from './MascotSprite';
import { mascotArt } from './studentMascots';
import { formatNumber } from '../../utils/formatters';
import { GachaThreeMeteorScene, GachaThreeRevealStage } from './GachaThreeScene';

/**
 * Genshin-inspired 4-point concave Primogem Star SVG (flat vector planes, crisp at any scale).
 */
export function PrimogemStar({
  size = 16,
  color = '#F59E0B',
  coreColor = '#FFFBEB',
  className = '',
}) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      aria-hidden="true"
      className={`shrink-0 select-none ${className}`}
    >
      <path
        d="M12 1C12.7 7.9 16.1 11.3 23 12C16.1 12.7 12.7 16.1 12 23C11.3 16.1 7.9 12.7 1 12C7.9 11.3 11.3 7.9 12 1Z"
        fill={color}
      />
      <path
        d="M12 5.5C12.4 9.4 14.6 11.6 18.5 12C14.6 12.4 12.4 14.6 12 18.5C11.6 14.6 9.4 12.4 5.5 12C9.4 11.6 11.6 9.4 12 5.5Z"
        fill={coreColor}
        opacity="0.76"
      />
    </svg>
  );
}

/**
 * Handcrafted 3D-faceted Cosmic Crystal SVG matching the reference celestial gem.
 * Uses pure geometric light-plane facets and concentric aura rings.
 */
export function CosmicCrystalGem({ size = 80, className = '', glow = true }) {
  return (
    <div className={`relative inline-flex items-center justify-center select-none ${className}`}>
      <svg
        width={size}
        height={Math.round(size * 1.18)}
        viewBox="0 0 120 142"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        className="relative overflow-visible"
        aria-hidden="true"
      >
        {glow && (
          <g pointerEvents="none">
            <ellipse cx="60" cy="68" rx="54" ry="62" fill="#FDE68A" opacity="0.18" />
            <ellipse cx="60" cy="68" rx="42" ry="50" fill="#FBBF24" opacity="0.26" />
            <ellipse cx="60" cy="68" rx="30" ry="38" fill="#FEF3C7" opacity="0.36" />
          </g>
        )}

        {/* Crown (Upper Pyramid Facets) */}
        <polygon points="60,6 31,34 48,40" fill="#FFFBEB" />
        <polygon points="60,6 48,40 60,36" fill="#FEF3C7" />
        <polygon points="60,6 60,36 73,40" fill="#FEF08A" />
        <polygon points="60,6 73,40 89,34" fill="#FDE047" />

        {/* Upper Girdle Wing Facets */}
        <polygon points="31,34 17,56 38,56 48,40" fill="#FDE68A" />
        <polygon points="48,40 38,56 60,52 60,36" fill="#FFFBEB" />
        <polygon points="60,36 60,52 82,56 73,40" fill="#FCD34D" />
        <polygon points="73,40 82,56 103,56 89,34" fill="#FBBF24" />

        {/* Central Waist Facets */}
        <polygon points="17,56 26,82 43,78 38,56" fill="#FBBF24" />
        <polygon points="38,56 43,78 60,82 60,52" fill="#FCD34D" />
        <polygon points="60,52 60,82 77,78 82,56" fill="#F59E0B" />
        <polygon points="82,56 77,78 94,82 103,56" fill="#D97706" />

        {/* Lower Pavilion Point Facets */}
        <polygon points="26,82 60,134 43,78" fill="#D97706" />
        <polygon points="43,78 60,134 60,82" fill="#F59E0B" />
        <polygon points="60,82 60,134 77,78" fill="#D97706" />
        <polygon points="77,78 60,134 94,82" fill="#B45309" />

        {/* Crisp Facet Edge Highlights */}
        <g stroke="#FFFDF5" strokeWidth="1" strokeLinejoin="round" opacity="0.74">
          <polygon points="60,6 89,34 103,56 94,82 60,134 26,82 17,56 31,34" fill="none" strokeWidth="1.4" />
          <path d="M60,6 L48,40 L38,56 L43,78 L60,134" />
          <path d="M60,6 L73,40 L82,56 L77,78 L60,134" />
          <path d="M60,6 L60,134" opacity="0.55" />
          <path d="M31,34 L48,40 L60,36 L73,40 L89,34" />
          <path d="M17,56 L38,56 L60,52 L82,56 L103,56" />
          <path d="M26,82 L43,78 L60,82 L77,78 L94,82" />
        </g>

        {/* Signature Specular Star Glints */}
        <g pointerEvents="none">
          <circle cx="71" cy="39" r="5" fill="#FFFFFF" opacity="0.58" />
          <path
            d="M71,24 L72.5,37.5 L86,39 L72.5,40.5 L71,54 L69.5,40.5 L56,39 L69.5,37.5 Z"
            fill="#FFFFFF"
          />
          <path
            d="M42,55 L42.8,59.2 L47,60 L42.8,60.8 L42,65 L41.2,60.8 L37,60 L41.2,59.2 Z"
            fill="#FFFBEB"
            opacity="0.9"
          />
        </g>
      </svg>
    </div>
  );
}

/**
 * 3D Tilted Isometric Embossed Star-Medallion Coin (with extruded 3D cylinder rim,
 * fluted edge ribs, cast ground shadow, and 4-plane 3D faceted center jewel).
 */
export function AstralPointEmblem({ points = 20, size = 64 }) {
  const isGold = points >= 150;
  const isMid = points >= 60 && points < 150;

  const sideDark = isGold ? '#92400E' : isMid ? '#075985' : '#9A6B16';
  const sideMid = isGold ? '#B45309' : isMid ? '#0284C7' : '#C8922A';
  const rimTop = isGold ? '#FBBF24' : isMid ? '#7DD3FC' : '#FDE68A';
  const wellFill = isGold ? '#FEF3C7' : isMid ? '#E0F2FE' : '#FFF8E7';
  const jewelTop = isGold ? '#FFFBEB' : '#F0F9FF';
  const jewelLeft = isGold ? '#FDE047' : isMid ? '#38BDF8' : '#60A5FA';
  const jewelRight = isGold ? '#D97706' : isMid ? '#0284C7' : '#2563EB';
  const jewelBot = isGold ? '#B45309' : isMid ? '#0369A1' : '#1D4ED8';

  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 96 96"
      fill="none"
      aria-hidden="true"
      className="shrink-0 select-none overflow-visible"
    >
      {/* Soft Ground Cast Shadow */}
      <ellipse cx="48" cy="80" rx="28" ry="8" fill="#5C3D14" opacity="0.22" />
      {/* Warm Halo Behind Coin */}
      <circle cx="48" cy="44" r="36" fill={rimTop} opacity="0.24" />

      {/* Tilted 3D Coin Group */}
      <g transform="translate(48 45) rotate(-11)">
        {/* Extruded 3D Metallic Rim Wall (Thickness) */}
        <path
          d="M-31,0 L-31,9 A31,25 0 0,0 31,9 L31,0 Z"
          fill={sideDark}
        />
        <path
          d="M-23,6 L-23,14 A31,25 0 0,0 18,13 L18,5 Z"
          fill={sideMid}
        />
        {/* Fluted 3D Coin Edge Notches */}
        {[-22, -13, -4, 5, 14, 23].map((nx) => (
          <line
            key={nx}
            x1={nx}
            y1="14"
            x2={nx}
            y2="22"
            stroke="#FFFBEB"
            strokeWidth="1.1"
            opacity="0.4"
          />
        ))}

        {/* Top Bevelled Metallic Coin Face */}
        <ellipse cx="0" cy="0" rx="31" ry="25" fill={rimTop} stroke="#FFFDF7" strokeWidth="1.5" />
        {/* Recessed Inner Dish */}
        <ellipse cx="0" cy="1" rx="24" ry="19" fill={sideMid} opacity="0.35" />
        <ellipse cx="0" cy="-0.5" rx="23.5" ry="18.5" fill={wellFill} stroke="#FFFFFF" strokeWidth="1.2" />

        {/* 3D 4-Faceted Embossed Primogem Star Relief Popping Out */}
        <polygon points="0,-16 -6,-2 0,0" fill={jewelTop} />
        <polygon points="0,-16 6,-2 0,0" fill={jewelLeft} />
        <polygon points="20,0 5,-4 0,0" fill={jewelLeft} />
        <polygon points="20,0 5,4 0,0" fill={jewelRight} />
        <polygon points="-20,0 -5,-4 0,0" fill={jewelTop} />
        <polygon points="-20,0 -5,4 0,0" fill={jewelLeft} />
        <polygon points="0,16 -6,2 0,0" fill={jewelRight} />
        <polygon points="0,16 6,2 0,0" fill={jewelBot} />

        {/* Specular Ridge Lines & Glint */}
        <path d="M0,-16 L0,16 M-20,0 L20,0" stroke="#FFFFFF" strokeWidth="1" opacity="0.8" />
        <circle cx="-11" cy="-9" r="2.5" fill="#FFFFFF" opacity="0.92" />
      </g>

      {/* Floating 3D Sparkles */}
      <path d="M76,16 L77.2,21.8 L83,23 L77.2,24.2 L76,30 L74.8,24.2 L69,23 L74.8,21.8 Z" fill="#F59E0B" />
      <circle cx="20" cy="24" r="2.2" fill="#FFFBEB" />
    </svg>
  );
}

/**
 * Shared Celestia Cloud & Golden Sunbeam Backdrop Layer used across ALL phases
 * (`idle`, `meteor`, `reveal`, `summary`) so the modal's warm cream-gold frame
 * and the celestial stage harmonize seamlessly with zero dark-black clash.
 */
function CelestiaStageBackdrop({ veilColor = 'rgba(72, 54, 110, 0.24)' }) {
  return (
    <div className="pointer-events-none absolute inset-0 overflow-hidden bg-[#6A5A8E]" aria-hidden="true">
      <svg
        viewBox="0 0 1000 540"
        preserveAspectRatio="xMidYMid slice"
        className="absolute inset-0 h-full w-full"
      >
        <rect width="1000" height="540" fill="#6D5D8C" />
        <polygon points="340,0 660,0 840,540 160,540" fill="#FDE68A" opacity="0.34" />
        <polygon points="415,0 585,0 710,540 290,540" fill="#FFFBEB" opacity="0.46" />
        <ellipse cx="500" cy="390" rx="280" ry="90" fill="#FDE68A" opacity="0.46" />
        <ellipse cx="130" cy="430" rx="240" ry="130" fill="#FCECD2" opacity="0.9" />
        <ellipse cx="870" cy="430" rx="240" ry="130" fill="#FCECD2" opacity="0.9" />
        <ellipse cx="500" cy="500" rx="520" ry="110" fill="#FDF4E3" />
      </svg>

      <img
        src="/gacha/celestial-stage-bg.webp"
        alt=""
        decoding="async"
        onError={(e) => {
          e.currentTarget.style.display = 'none';
        }}
        className="absolute inset-0 h-full w-full object-cover object-[center_64%]"
      />

      {veilColor && (
        <div className="absolute inset-0" style={{ backgroundColor: veilColor }} />
      )}

      {/* Subtle Golden Inner Sanctuary Frame Filigree */}
      <svg
        viewBox="0 0 1000 520"
        preserveAspectRatio="none"
        className="absolute inset-0 h-full w-full"
      >
        <path d="M12,12 L12,508 M988,12 L988,508" stroke="#E5B962" strokeWidth="1.8" opacity="0.65" />
        <path d="M12,472 L12,508 L54,508" fill="none" stroke="#DDA94B" strokeWidth="2.8" />
        <path d="M988,472 L988,508 L946,508" fill="none" stroke="#DDA94B" strokeWidth="2.8" />
      </svg>
    </div>
  );
}

const RARITY_THEME = {
  legendary: {
    rank: 5,
    stars: 5,
    label: 'Legendary Mascot',
    meteorCallout: 'LEGENDARY RADIANCE',
    veil: 'rgba(92, 56, 18, 0.28)',
    haloPrimary: '#F59E0B',
    haloSecondary: '#FEF08A',
    badgeBg: '#FEF3C7',
    badgeBorder: '#F59E0B',
    badgeText: '#92400E',
    starColor: '#F59E0B',
    stripBg: '#FFFBEB',
    stripAccent: '#FDE68A',
    stripBorder: '#F59E0B',
    stripLip: '#B45309',
    stripGlow: 'rgba(245, 158, 11, 0.52)',
    stripText: '#78350F',
    beamColor: '#FBBF24',
  },
  crystal: {
    rank: 6,
    stars: 5,
    label: 'Ultimate Treasure',
    meteorCallout: 'CELESTIAL RADIANCE',
    veil: 'rgba(92, 56, 18, 0.26)',
    haloPrimary: '#F59E0B',
    haloSecondary: '#FDE047',
    badgeBg: '#FEF3C7',
    badgeBorder: '#F59E0B',
    badgeText: '#92400E',
    starColor: '#F59E0B',
    stripBg: '#FFFBEB',
    stripAccent: '#FDE68A',
    stripBorder: '#F59E0B',
    stripLip: '#B45309',
    stripGlow: 'rgba(245, 158, 11, 0.58)',
    stripText: '#78350F',
    beamColor: '#F59E0B',
  },
  epic: {
    rank: 4,
    stars: 4,
    label: 'Epic Mascot',
    meteorCallout: 'ASTRAL ECLIPSE',
    veil: 'rgba(76, 46, 118, 0.34)',
    haloPrimary: '#A855F7',
    haloSecondary: '#E9D5FF',
    badgeBg: '#F3E8FF',
    badgeBorder: '#A855F7',
    badgeText: '#6B21A8',
    starColor: '#9333EA',
    stripBg: '#FAF5FF',
    stripAccent: '#EDE9FE',
    stripBorder: '#A855F7',
    stripLip: '#6B21A8',
    stripGlow: 'rgba(168, 85, 247, 0.44)',
    stripText: '#581C87',
    beamColor: '#A855F7',
  },
  rare: {
    rank: 3,
    stars: 4,
    label: 'Rare Mascot',
    meteorCallout: 'STARLIGHT RESONANCE',
    veil: 'rgba(36, 78, 86, 0.30)',
    haloPrimary: '#10B981',
    haloSecondary: '#A7F3D0',
    badgeBg: '#DCFCE7',
    badgeBorder: '#10B981',
    badgeText: '#065F46',
    starColor: '#10B981',
    stripBg: '#F0FDF4',
    stripAccent: '#DCFCE7',
    stripBorder: '#34D399',
    stripLip: '#047857',
    stripGlow: 'rgba(16, 185, 129, 0.38)',
    stripText: '#14532D',
    beamColor: '#10B981',
  },
  normal: {
    rank: 2,
    stars: 3,
    label: 'Normal Mascot',
    meteorCallout: 'STARFALL COMPANION',
    veil: 'rgba(66, 56, 102, 0.28)',
    haloPrimary: '#38BDF8',
    haloSecondary: '#BAE6FD',
    badgeBg: '#E0F2FE',
    badgeBorder: '#38BDF8',
    badgeText: '#075985',
    starColor: '#0284C7',
    stripBg: '#FFFDF9',
    stripAccent: '#E0F2FE',
    stripBorder: '#7DD3FC',
    stripLip: '#0284C7',
    stripGlow: 'rgba(56, 189, 248, 0.28)',
    stripText: '#1E293B',
    beamColor: '#38BDF8',
  },
  point: {
    rank: 1,
    stars: 3,
    label: 'Astral Bounty',
    meteorCallout: 'STARFALL WISH',
    veil: 'rgba(68, 54, 104, 0.25)',
    haloPrimary: '#F59E0B',
    haloSecondary: '#FDE68A',
    badgeBg: '#FFF8E8',
    badgeBorder: '#E4BA69',
    badgeText: '#784B12',
    starColor: '#D97706',
    stripBg: '#FFFDF7',
    stripAccent: '#F8EAD0',
    stripBorder: '#E2B86B',
    stripLip: '#B78332',
    stripGlow: 'rgba(180, 118, 28, 0.20)',
    stripText: '#4A2E14',
    beamColor: '#FBBF24',
  },
};

export function getItemRarityKey(item) {
  if (!item) return 'point';
  if (item.type === 'crystal' || item.rarity === 'crystal') return 'crystal';
  if (item.rarity === 'legendary') return 'legendary';
  if (item.rarity === 'epic') return 'epic';
  if (item.rarity === 'rare') return 'rare';
  if (item.type === 'mascot') return 'normal';
  return 'point';
}

export function getRarityTheme(itemOrKey) {
  const key = typeof itemOrKey === 'string' ? itemOrKey : getItemRarityKey(itemOrKey);
  return RARITY_THEME[key] || RARITY_THEME.point;
}

export const SUMMARY_AUTO_RETURN_SECONDS = 3;

export function resolvePostRollPhase(autoSkip) {
  return autoSkip ? 'summary' : 'meteor';
}

export function shouldRunSummaryAutoReturn({
  autoSkip = false,
  suppressed = false,
  blocked = false,
  cancelled = false,
} = {}) {
  return Boolean(autoSkip && !suppressed && !blocked && !cancelled);
}

export function AutoSkipToggleButton({ enabled = false, onToggle, className = '' }) {
  const prefersReducedMotion = useReducedMotion();

  return (
    <button
      type="button"
      role="switch"
      aria-checked={enabled}
      onClick={onToggle}
      className={`inline-flex cursor-pointer items-center gap-2 rounded-full border px-3 py-1.5 text-xs font-extrabold transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-[#D97706] ${
        enabled
          ? 'border-[#D97706] bg-[#2B180A] text-[#FEF3C7] shadow-[0_4px_14px_rgba(43,24,10,0.28)] hover:bg-[#3D2410]'
          : 'border-[#DFB66C] bg-[#FFFBF0]/92 text-[#5C3A16] shadow-sm hover:border-[#C9963B] hover:bg-white'
      } ${className}`}
    >
      <FastForward
        className={`h-3.5 w-3.5 shrink-0 ${enabled ? 'text-[#FBBF24]' : 'text-[#B4690E]'}`}
      />
      <span>Auto Skip</span>
      <span
        aria-hidden="true"
        className={`relative inline-flex h-4 w-7 shrink-0 items-center rounded-full p-0.5 transition-colors ${
          enabled ? 'bg-[#F59E0B]' : 'bg-[#E5D2B0]'
        }`}
      >
        <span
          className={`block h-3 w-3 rounded-full shadow-sm ${
            prefersReducedMotion ? '' : 'transition-transform duration-150'
          } ${enabled ? 'translate-x-3 bg-[#FFFBEB]' : 'translate-x-0 bg-[#FFFDF7]'}`}
        />
      </span>
    </button>
  );
}

export function sortPullsForGenshinSummary(pulls = []) {
  return pulls
    .map((pull, originalIndex) => ({
      pull,
      originalIndex,
      rank: getRarityTheme(pull?.item).rank,
      pts: pull?.item?.pointsRefund || 0,
    }))
    .sort((a, b) => {
      if (b.rank !== a.rank) return b.rank - a.rank;
      if (b.pts !== a.pts) return b.pts - a.pts;
      return a.originalIndex - b.originalIndex;
    });
}

const IDLE_SPARKLES = [
  { left: '10%', top: '28%', size: 18, delay: 0.1, dur: 3.1 },
  { left: '21%', top: '19%', size: 11, delay: 0.8, dur: 2.6 },
  { left: '29%', top: '38%', size: 13, delay: 1.5, dur: 3.4 },
  { left: '38%', top: '14%', size: 10, delay: 0.4, dur: 2.8 },
  { left: '63%', top: '15%', size: 11, delay: 1.1, dur: 3.0 },
  { left: '75%', top: '27%', size: 20, delay: 0.3, dur: 3.3 },
  { left: '87%', top: '32%', size: 13, delay: 1.7, dur: 2.7 },
  { left: '69%', top: '47%', size: 12, delay: 0.9, dur: 3.5 },
  { left: '24%', top: '57%', size: 12, delay: 1.3, dur: 2.9 },
  { left: '81%', top: '61%', size: 14, delay: 0.6, dur: 3.2 },
];

const ALTAR_RISING_MOTES = [
  { left: '43%', delay: 0, dur: 3.8, size: 5 },
  { left: '47%', delay: 0.9, dur: 4.2, size: 4 },
  { left: '50%', delay: 1.7, dur: 3.5, size: 6 },
  { left: '53%', delay: 0.5, dur: 4.0, size: 4 },
  { left: '57%', delay: 1.3, dur: 3.7, size: 5 },
];

/**
 * Interactive 3D Tilt Plaque Wrapper for Summary Cards (respects reduced motion).
 */
function Plaque3DTilt({ children, className = '', style, onClick, onPointerInteract, title }) {
  const prefersReducedMotion = useReducedMotion();
  const [tilt, setTilt] = useState({ rx: 0, ry: 0, hovered: false });

  const handleMove = (e) => {
    onPointerInteract?.();
    if (prefersReducedMotion) return;
    const box = e.currentTarget.getBoundingClientRect();
    const nx = (e.clientX - box.left) / Math.max(1, box.width) - 0.5;
    const ny = (e.clientY - box.top) / Math.max(1, box.height) - 0.5;
    setTilt({ rx: -ny * 18, ry: nx * 20, hovered: true });
  };

  const handleLeave = () => {
    setTilt({ rx: 0, ry: 0, hovered: false });
  };

  return (
    <div
      onClick={onClick}
      onPointerMove={handleMove}
      onPointerLeave={handleLeave}
      title={title}
      style={{
        ...style,
        transformStyle: 'preserve-3d',
        transform: prefersReducedMotion
          ? undefined
          : `perspective(900px) rotateX(${tilt.rx.toFixed(1)}deg) rotateY(${tilt.ry.toFixed(1)}deg) translateY(${tilt.hovered ? -7 : 0}px) scale(${tilt.hovered ? 1.035 : 1})`,
        transition: tilt.hovered
          ? 'transform 90ms ease-out, box-shadow 180ms ease'
          : 'transform 280ms ease-out, box-shadow 220ms ease',
      }}
      className={className}
    >
      {children}
    </div>
  );
}

/**
 * Ornate upward-curving Celestial Arch Overlay at the top of the stage.
 */
export function CelestialArchTopBar({
  pityCount,
  pityHard,
  softPityStart,
  onOpenOdds,
}) {
  const clampedPity = Math.min(pityCount, pityHard);
  const pct = Math.min(100, (clampedPity / Math.max(1, pityHard)) * 100);
  const isSoftPity = pityCount >= softPityStart;

  return (
    <div className="relative z-20 w-full select-none">
      <svg
        viewBox="0 0 1000 72"
        preserveAspectRatio="none"
        className="pointer-events-none block h-[54px] w-full sm:h-[62px]"
        aria-hidden="true"
      >
        <path
          d="M0,0 L1000,0 L1000,66 C875,62 765,44 650,20 C560,2 440,2 350,20 C235,44 125,62 0,66 Z"
          fill="#FDF6E7"
        />
        <path
          d="M0,66 C125,62 235,44 350,20 C440,2 560,2 650,20 C765,44 875,62 1000,66"
          fill="none"
          stroke="#DDA94B"
          strokeWidth="3"
        />
        <path
          d="M0,69 C125,65 235,47 350,23 C440,5 560,5 650,23 C765,47 875,65 1000,69"
          fill="none"
          stroke="#FFFDF7"
          strokeWidth="1.2"
          opacity="0.85"
        />
      </svg>

      <div className="absolute left-4 top-1.5 z-10 flex flex-col gap-1 sm:left-6 sm:top-2">
        <div className="flex flex-wrap items-center gap-1.5 text-[11px] sm:text-xs">
          <ShieldCheck className="h-4 w-4 shrink-0 text-[#B7791F]" />
          <span className="font-medium text-[#5C4028]">Crystal guarantee (Hard Pity):</span>
          <span className="font-extrabold tabular-nums text-[#2D1B0D]">
            {clampedPity} / {pityHard}
          </span>
          {isSoftPity && (
            <span className="rounded-full border border-[#E6B450] bg-[#FEF3C7] px-2 py-0.5 text-[10px] font-bold text-[#92400E]">
              Soft Pity
            </span>
          )}
        </div>
        <div className="h-1.5 w-40 overflow-hidden rounded-full bg-[#EFE2C6] sm:w-48">
          <motion.div
            className="h-full rounded-full"
            style={{ backgroundColor: isSoftPity ? '#D97706' : '#E5A93B' }}
            initial={false}
            animate={{ width: `${pct}%` }}
            transition={{ duration: 0.35 }}
          />
        </div>
      </div>

      <div className="absolute right-4 top-2 z-10 sm:right-6 sm:top-2.5">
        <button
          type="button"
          onClick={onOpenOdds}
          className="inline-flex cursor-pointer items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-bold text-[#B4690E] transition-colors hover:bg-[#F5E6C8]/70 hover:text-[#8C4D06] focus:outline-none focus-visible:ring-2 focus-visible:ring-[#D97706]"
        >
          <HelpCircle className="h-3.5 w-3.5" />
          <span>Rates &amp; Rules</span>
        </button>
      </div>
    </div>
  );
}

/**
 * IDLE STAGE: Matches the uploaded reference image with live floating crystals,
 * tilted orbital golden rings, rising altar stardust, ornate plaque, and summon pills.
 */
export function CelestialIdleBannerStage({
  tickets,
  costSingle,
  costMulti,
  onRollOnce,
  onRollTen,
  rolling,
  autoSkip = false,
  onToggleAutoSkip,
}) {
  const prefersReducedMotion = useReducedMotion();
  const [pulseKey, setPulseKey] = useState(0);

  return (
    <div className="relative flex min-h-[430px] w-full flex-1 flex-col justify-between overflow-hidden px-4 pb-5 pt-2 sm:min-h-[450px] sm:px-8">
      <CelestiaStageBackdrop veilColor={null} />

      {/* Animated Celestial God-Rays, Rising Stardust, Twinkling Stars & Side Crystals */}
      <div className="pointer-events-none absolute inset-0 z-10 overflow-hidden" aria-hidden="true">
        <motion.svg
          viewBox="0 0 1000 520"
          preserveAspectRatio="xMidYMid slice"
          className="absolute inset-0 h-full w-full"
          animate={prefersReducedMotion ? undefined : { opacity: [0.5, 0.88, 0.5] }}
          transition={{ duration: 4.6, repeat: Infinity, ease: 'easeInOut' }}
        >
          <polygon points="445,0 485,0 410,410 335,410" fill="#FFFBEB" opacity="0.2" />
          <polygon points="488,0 518,0 535,410 465,410" fill="#FFFBEB" opacity="0.26" />
          <polygon points="522,0 562,0 665,410 590,410" fill="#FFFBEB" opacity="0.18" />
        </motion.svg>

        {!prefersReducedMotion &&
          ALTAR_RISING_MOTES.map((mote, idx) => (
            <motion.span
              key={idx}
              style={{
                left: mote.left,
                bottom: '26%',
                width: mote.size,
                height: mote.size,
              }}
              animate={{
                y: [0, -170],
                opacity: [0, 0.9, 0],
                scale: [0.6, 1.15, 0.4],
              }}
              transition={{
                duration: mote.dur,
                delay: mote.delay,
                repeat: Infinity,
                ease: 'easeOut',
              }}
              className="absolute rounded-full bg-[#FFFBEB] shadow-[0_0_8px_#FDE047]"
            />
          ))}

        {IDLE_SPARKLES.map((sp, i) => (
          <motion.div
            key={i}
            style={{ left: sp.left, top: sp.top }}
            className="absolute"
            animate={
              prefersReducedMotion
                ? undefined
                : {
                    opacity: [0.35, 1, 0.35],
                    scale: [0.82, 1.18, 0.82],
                  }
            }
            transition={{
              duration: sp.dur,
              delay: sp.delay,
              repeat: Infinity,
              ease: 'easeInOut',
            }}
          >
            <PrimogemStar size={sp.size} color="#FFFBEB" coreColor="#FDE047" />
          </motion.div>
        ))}

        <motion.div
          className="absolute left-[11%] top-[27%] hidden sm:block"
          animate={
            prefersReducedMotion
              ? undefined
              : { y: [-7, 7, -7], rotate: [-6, -2, -6] }
          }
          transition={{ duration: 4.4, repeat: Infinity, ease: 'easeInOut' }}
        >
          <CosmicCrystalGem size={44} glow />
        </motion.div>

        <motion.div
          className="absolute right-[11%] top-[38%] hidden sm:block"
          animate={
            prefersReducedMotion
              ? undefined
              : { y: [6, -7, 6], rotate: [7, 3, 7] }
          }
          transition={{ duration: 3.9, repeat: Infinity, ease: 'easeInOut' }}
        >
          <CosmicCrystalGem size={38} glow />
        </motion.div>
      </div>

      {/* Centerpiece Floating Cosmic Crystal, Tilted Orbital Rings & Ornate Plaque */}
      <div className="relative z-20 my-auto flex flex-col items-center justify-center pt-1">
        <div className="relative flex h-52 w-80 items-center justify-center sm:h-56 sm:w-96">
          <motion.svg
            viewBox="0 0 240 240"
            className="pointer-events-none absolute h-48 w-48"
            animate={prefersReducedMotion ? undefined : { rotate: 360 }}
            transition={{ duration: 38, repeat: Infinity, ease: 'linear' }}
            aria-hidden="true"
          >
            <circle
              cx="120"
              cy="120"
              r="92"
              fill="none"
              stroke="#FFFBEB"
              strokeWidth="1.1"
              strokeDasharray="5 7"
              opacity="0.52"
            />
            <polygon
              points="120,34 142,98 206,120 142,142 120,206 98,142 34,120 98,98"
              fill="none"
              stroke="#FEF08A"
              strokeWidth="0.9"
              opacity="0.38"
            />
          </motion.svg>

          <svg
            viewBox="0 0 420 240"
            className="pointer-events-none absolute inset-0 h-full w-full overflow-visible"
            aria-hidden="true"
          >
            <g transform="translate(210 118) rotate(-14)">
              <ellipse
                cx="0"
                cy="0"
                rx="168"
                ry="44"
                fill="none"
                stroke="#FDE68A"
                strokeWidth="2.4"
                opacity="0.82"
              />
              <motion.ellipse
                cx="0"
                cy="0"
                rx="168"
                ry="44"
                fill="none"
                stroke="#FFFFFF"
                strokeWidth="1.6"
                strokeDasharray="44 190"
                animate={prefersReducedMotion ? undefined : { strokeDashoffset: [0, -468] }}
                transition={{ duration: 6.5, repeat: Infinity, ease: 'linear' }}
              />
            </g>
            <g transform="translate(210 116) rotate(-7)">
              <ellipse
                cx="0"
                cy="0"
                rx="124"
                ry="30"
                fill="none"
                stroke="#FFFBEB"
                strokeWidth="2"
                opacity="0.85"
              />
              <motion.ellipse
                cx="0"
                cy="0"
                rx="124"
                ry="30"
                fill="none"
                stroke="#FDE047"
                strokeWidth="2"
                strokeDasharray="32 160"
                animate={prefersReducedMotion ? undefined : { strokeDashoffset: [0, 384] }}
                transition={{ duration: 5.2, repeat: Infinity, ease: 'linear' }}
              />
            </g>
            <ellipse cx="210" cy="196" rx="92" ry="15" fill="#F59E0B" opacity="0.28" />
            <ellipse cx="210" cy="196" rx="64" ry="9" fill="#FEF08A" opacity="0.54" />
          </svg>

          <AnimatePresence>
            {pulseKey > 0 && (
              <motion.div
                key={pulseKey}
                initial={{ scale: 0.55, opacity: 0.85 }}
                animate={{ scale: 2.1, opacity: 0 }}
                exit={{ opacity: 0 }}
                transition={{ duration: 0.75, ease: 'easeOut' }}
                className="pointer-events-none absolute h-28 w-28 rounded-full border-2 border-[#FFFBEB]"
              />
            )}
          </AnimatePresence>

          <motion.button
            type="button"
            onClick={() => setPulseKey((k) => k + 1)}
            aria-label="Resonate Cosmic Crystal"
            whileHover={prefersReducedMotion ? undefined : { scale: 1.07 }}
            whileTap={prefersReducedMotion ? undefined : { scale: 0.96 }}
            animate={
              prefersReducedMotion
                ? undefined
                : {
                    y: [-8, 5, -8],
                  }
            }
            transition={{ duration: 3.6, repeat: Infinity, ease: 'easeInOut' }}
            className="relative z-10 cursor-pointer rounded-full p-2 focus:outline-none focus-visible:ring-2 focus-visible:ring-[#F59E0B]"
          >
            <CosmicCrystalGem size={120} glow />
          </motion.button>
        </div>

        <div className="relative z-20 -mt-2 flex items-center justify-center">
          <div className="relative flex items-center gap-3 rounded-2xl border-2 border-[#E4BA69] bg-[#FFF8E8] px-6 py-2 shadow-[0_6px_20px_rgba(146,84,14,0.18)] sm:px-8">
            <span
              aria-hidden="true"
              className="pointer-events-none absolute inset-[3px] rounded-xl border border-[#F3DCAA]"
            />
            <PrimogemStar size={16} color="#E5A530" coreColor="#FFFBEB" />
            <h3 className="relative text-sm font-extrabold tracking-[0.09em] text-[#432812] sm:text-base">
              COSMIC CRYSTAL SUMMON
            </h3>
            <PrimogemStar size={16} color="#E5A530" coreColor="#FFFBEB" />
          </div>
        </div>
      </div>

      {/* Dual Genshin Wish Pill Buttons + Auto Skip Toggle */}
      <div className="relative z-20 mt-4 flex flex-col items-center gap-3">
        <div className="flex flex-wrap items-center justify-center gap-4 sm:gap-6">
          <motion.button
          type="button"
          disabled={rolling}
          onClick={onRollOnce}
          whileHover={prefersReducedMotion ? undefined : { y: -2, scale: 1.015 }}
          whileTap={prefersReducedMotion ? undefined : { scale: 0.98 }}
          className="group relative flex min-h-[60px] min-w-[215px] cursor-pointer flex-col items-center justify-center rounded-full border-2 border-[#DFB66C] bg-[#FFFBF0] px-7 py-2.5 shadow-[0_8px_20px_rgba(120,72,16,0.14)] transition-colors hover:border-[#C9963B] hover:bg-[#FFFDF7] focus:outline-none focus-visible:ring-2 focus-visible:ring-[#D97706] disabled:cursor-not-allowed disabled:opacity-60 sm:min-w-[235px]"
        >
          <span
            aria-hidden="true"
            className="pointer-events-none absolute inset-[3px] rounded-full border border-[#F3DFB4]"
          />
          <PrimogemStar
            size={12}
            color="#E5B962"
            className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 opacity-75 transition-transform group-hover:scale-125"
          />
          <PrimogemStar
            size={12}
            color="#E5B962"
            className="pointer-events-none absolute right-4 top-1/2 -translate-y-1/2 opacity-75 transition-transform group-hover:scale-125"
          />
          <span className="relative text-base font-extrabold text-[#3B2313]">Roll once</span>
          <span className="relative mt-0.5 flex items-center gap-1.5 text-xs font-bold">
            {tickets >= 1 ? (
              <>
                <Ticket className="h-3.5 w-3.5 text-[#D97706]" />
                <span className="tabular-nums text-[#8C5311]">1 Roll Ticket</span>
              </>
            ) : (
              <>
                <Coins className="h-3.5 w-3.5 text-[#D97706]" />
                <span className="tabular-nums text-[#8C5311]">{formatNumber(costSingle)} pts</span>
              </>
            )}
          </span>
        </motion.button>

        <motion.button
          type="button"
          disabled={rolling}
          onClick={onRollTen}
          whileHover={prefersReducedMotion ? undefined : { y: -2, scale: 1.02 }}
          whileTap={prefersReducedMotion ? undefined : { scale: 0.98 }}
          className="group relative flex min-h-[60px] min-w-[235px] cursor-pointer flex-col items-center justify-center rounded-full border-2 border-[#FDE68A] bg-[#F59E0B] px-8 py-2.5 text-white shadow-[0_10px_28px_rgba(245,158,11,0.44)] transition-colors hover:bg-[#D97706] focus:outline-none focus-visible:ring-2 focus-visible:ring-[#92400E] disabled:cursor-not-allowed disabled:opacity-60 sm:min-w-[255px]"
        >
          {!prefersReducedMotion && (
            <span
              aria-hidden="true"
              className="pointer-events-none absolute inset-0 overflow-hidden rounded-full"
            >
              <motion.span
                animate={{ x: ['-140%', '260%'] }}
                transition={{ duration: 3.4, repeat: Infinity, repeatDelay: 1.2, ease: 'easeInOut' }}
                className="block h-full w-16 -skew-x-12 bg-white/25"
              />
            </span>
          )}

          <svg
            viewBox="0 0 260 68"
            preserveAspectRatio="none"
            className="pointer-events-none absolute -inset-1.5 h-[calc(100%+12px)] w-[calc(100%+12px)] overflow-visible"
            aria-hidden="true"
          >
            <path
              d="M28,8 L232,8 C244,8 250,18 250,28 C190,22 70,22 10,28 C10,18 16,8 28,8 Z"
              fill="#FBBF24"
              opacity="0.5"
            />
            <ellipse
              cx="130"
              cy="34"
              rx="132"
              ry="31"
              fill="none"
              stroke="#FEF08A"
              strokeWidth="1.5"
              strokeDasharray="48 36"
              opacity="0.88"
            />
          </svg>

          <span
            aria-hidden="true"
            className="pointer-events-none absolute inset-[3px] rounded-full border border-[#FEF08A]/75"
          />

          <span className="absolute -top-3.5 left-1/2 z-10 flex -translate-x-1/2 items-center gap-1 whitespace-nowrap rounded-full border border-[#FDE68A] bg-[#854D0E] px-3 py-0.5 text-[10px] font-extrabold tracking-wide text-[#FEF9C3] shadow-sm">
            <PrimogemStar size={9} color="#FDE047" />
            <span>{tickets >= 10 ? 'Best Value' : 'Best Value · 10% Off'}</span>
            <PrimogemStar size={9} color="#FDE047" />
          </span>

          <PrimogemStar
            size={14}
            color="#FFFBEB"
            className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 transition-transform group-hover:rotate-45 group-hover:scale-125"
          />
          <PrimogemStar
            size={14}
            color="#FFFBEB"
            className="pointer-events-none absolute right-4 top-1/2 -translate-y-1/2 transition-transform group-hover:-rotate-45 group-hover:scale-125"
          />

          <span className="relative text-base font-extrabold tracking-wide text-white drop-shadow-[0_1px_2px_rgba(120,53,15,0.45)]">
            Roll 10 times
          </span>
          <span className="relative mt-0.5 flex items-center gap-1.5 text-xs font-bold text-[#FFFBEB]">
            {tickets >= 10 ? (
              <>
                <Ticket className="h-3.5 w-3.5 text-[#FEF08A]" />
                <span className="tabular-nums">10 Roll Tickets</span>
              </>
            ) : (
              <>
                <Coins className="h-3.5 w-3.5 text-[#FEF08A]" />
                <span className="tabular-nums">
                  {formatNumber(costMulti)} pts{' '}
                  <span className="line-through opacity-75">{formatNumber(costSingle * 10)}</span>
                </span>
              </>
            )}
          </span>
        </motion.button>
        </div>

        {onToggleAutoSkip && (
          <AutoSkipToggleButton enabled={autoSkip} onToggle={onToggleAutoSkip} />
        )}
      </div>
    </div>
  );
}

/**
 * STAGE 1 OF WISH: Real-Time 3D WebGL Meteor & 3D Armillary Core Shatter!
 * Blended over the warm Celestia twilight-cloud backdrop so colors stay 100% harmonious.
 */
export function GenshinMeteorCutscene({
  highestRarityKey = 'point',
  pullCount = 1,
  onComplete,
  onSkip,
  autoSkip = false,
  onToggleAutoSkip,
}) {
  const prefersReducedMotion = useReducedMotion();
  const rarityTheme = getRarityTheme(highestRarityKey);
  const [ignitedUI, setIgnitedUI] = useState(false);
  const onCompleteRef = useRef(onComplete);
  const completedRef = useRef(false);

  useEffect(() => {
    onCompleteRef.current = onComplete;
  }, [onComplete]);

  const finishOnce = () => {
    if (completedRef.current) return;
    completedRef.current = true;
    onCompleteRef.current?.();
  };

  useEffect(() => {
    completedRef.current = false;
    if (prefersReducedMotion) {
      const quickTimer = window.setTimeout(finishOnce, 420);
      return () => window.clearTimeout(quickTimer);
    }
    setIgnitedUI(false);
    const igniteTimer = window.setTimeout(() => setIgnitedUI(true), 820);
    // Wall-clock failsafe so backgrounded tabs or throttled rAF can never stall the cutscene
    const safetyTimer = window.setTimeout(finishOnce, 2750);
    return () => {
      window.clearTimeout(igniteTimer);
      window.clearTimeout(safetyTimer);
    };
  }, [highestRarityKey, pullCount, prefersReducedMotion]);

  return (
    <div className="relative mx-3 my-2.5 flex min-h-[430px] flex-1 items-center justify-center overflow-hidden rounded-[24px] border-2 border-[#E2B45B] shadow-[0_12px_32px_rgba(120,72,16,0.18)] sm:mx-5 sm:my-3 sm:min-h-[445px]">
      {/* Harmonious Celestia Twilight-Gold Cloud Backdrop */}
      <CelestiaStageBackdrop veilColor="rgba(52, 38, 86, 0.48)" />

      {/* Real-Time Three.js 3D Meteor, 3D Gyroscopic Gate & 3D Crystal Shatter */}
      {!prefersReducedMotion && (
        <GachaThreeMeteorScene
          highestRarityKey={highestRarityKey}
          pullCount={pullCount}
          onComplete={finishOnce}
        />
      )}

      {/* Top-Right Auto Skip & Skip Pills */}
      <div className="absolute right-4 top-4 z-20 flex items-center gap-2">
        {onToggleAutoSkip && (
          <AutoSkipToggleButton
            enabled={autoSkip}
            onToggle={onToggleAutoSkip}
            className="shadow-md backdrop-blur-sm"
          />
        )}
        <button
          type="button"
          onClick={onSkip}
          className="inline-flex cursor-pointer items-center gap-1.5 rounded-full border border-[#E5C17B] bg-[#FFFBF0]/90 px-3.5 py-1.5 text-xs font-extrabold text-[#3B2313] shadow-md backdrop-blur-sm transition-colors hover:bg-white focus:outline-none focus-visible:ring-2 focus-visible:ring-[#D97706]"
        >
          <span>Skip</span>
          <FastForward className="h-3.5 w-3.5 text-[#D97706]" />
        </button>
      </div>

      {/* Bottom Harmonious Parchment Callout Pill */}
      <div className="pointer-events-none absolute bottom-4 left-1/2 z-20 -translate-x-1/2">
        <motion.div
          animate={ignitedUI ? { scale: [0.92, 1.07, 1] } : { scale: 1 }}
          transition={{ duration: 0.35 }}
          className="flex items-center gap-2 rounded-full border-2 border-[#E4BA69] bg-[#FFF8E8]/92 px-5 py-1.5 text-[11px] font-extrabold uppercase tracking-[0.15em] text-[#432812] shadow-lg backdrop-blur-sm"
        >
          <PrimogemStar size={13} color={rarityTheme.starColor} />
          <span>
            {ignitedUI
              ? rarityTheme.meteorCallout
              : pullCount > 1
                ? '10x Celestial Summon'
                : 'Celestial Summon'}
          </span>
          <PrimogemStar size={13} color={rarityTheme.starColor} />
        </motion.div>
      </div>
    </div>
  );
}

/**
 * STAGE 2 OF WISH: 3D WebGL Pedestal + 3D Gyroscopic Rings + 3D Spinning Gem/Coin/Mascot!
 */
export function GenshinSingleRevealStage({
  pull,
  index,
  total,
  onAdvance,
  onSkipAll,
  autoSkip = false,
  onToggleAutoSkip,
}) {
  const prefersReducedMotion = useReducedMotion();
  const item = pull?.item;
  const rarityKey = getItemRarityKey(item);
  const theme = getRarityTheme(rarityKey);
  const isCrystal = item?.type === 'crystal';
  const isMascot = item?.type === 'mascot' && Boolean(item?.mascotId);
  const itemMode = isCrystal ? 'crystal' : isMascot ? 'mascot' : 'points';

  const [awakened, setAwakened] = useState(() => !isMascot || Boolean(prefersReducedMotion));

  useEffect(() => {
    if (!isMascot || prefersReducedMotion) {
      setAwakened(true);
      return undefined;
    }
    setAwakened(false);
    const timer = window.setTimeout(() => {
      setAwakened(true);
    }, 340);
    return () => window.clearTimeout(timer);
  }, [index, item?.id, isMascot, prefersReducedMotion]);

  const handleStageClick = () => {
    if (!awakened) {
      setAwakened(true);
      return;
    }
    onAdvance?.();
  };

  useEffect(() => {
    const onKeyDown = (event) => {
      if (event.key === ' ' || event.key === 'Enter' || event.key === 'ArrowRight') {
        event.preventDefault();
        if (!awakened) setAwakened(true);
        else onAdvance?.();
      }
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [awakened, onAdvance]);

  const art = isMascot ? mascotArt(item.mascotId) : null;

  return (
    <div
      role="button"
      tabIndex={0}
      onClick={handleStageClick}
      aria-label="Reveal next reward"
      style={{ perspective: '1200px' }}
      className="relative mx-3 my-2.5 flex min-h-[430px] flex-1 cursor-pointer flex-col items-center justify-between overflow-hidden rounded-[24px] border-2 border-[#E2B45B] px-5 py-4 select-none shadow-[0_12px_32px_rgba(120,72,16,0.18)] focus:outline-none sm:mx-5 sm:my-3 sm:min-h-[445px]"
    >
      {/* Unified Celestia Clouds & Warm Sunbeam Backdrop */}
      <CelestiaStageBackdrop veilColor={theme.veil} />

      {/* Live Three.js 3D Pedestal, 3D Gyroscopic Rings & Real-Time 3D Crystal / 3D Star-Coin! */}
      {!prefersReducedMotion && (
        <GachaThreeRevealStage
          key={`three-${index}-${item?.id}`}
          rarityKey={rarityKey}
          itemMode={itemMode}
          pointsAmount={item?.pointsRefund || 20}
        />
      )}

      {/* Top Counter & Skip Pill */}
      <div
        className="relative z-30 flex w-full items-center justify-between"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center gap-2">
          {total > 1 && (
            <span className="rounded-full border border-[#E5C17B] bg-[#FFFBF0]/90 px-3 py-1 text-xs font-extrabold tabular-nums text-[#3B2313] shadow-sm">
              {index + 1} / {total}
            </span>
          )}
          {pull?.wasPity && (
            <span className="rounded-full border border-[#FDE047] bg-[#854D0E] px-2.5 py-1 text-[10px] font-extrabold uppercase tracking-wider text-[#FEF9C3]">
              Guaranteed Pity
            </span>
          )}
        </div>

        <div className="flex items-center gap-2">
          {onToggleAutoSkip && (
            <AutoSkipToggleButton enabled={autoSkip} onToggle={onToggleAutoSkip} />
          )}
          <button
            type="button"
            onClick={onSkipAll}
            className="inline-flex cursor-pointer items-center gap-1.5 rounded-full border border-[#E5C17B] bg-[#FFFBF0]/90 px-3.5 py-1.5 text-xs font-extrabold text-[#3B2313] shadow-sm transition-colors hover:bg-white focus:outline-none focus-visible:ring-2 focus-visible:ring-[#D97706]"
          >
            <span>{total > 1 ? 'Skip' : 'Summary'}</span>
            <FastForward className="h-3.5 w-3.5 text-[#D97706]" />
          </button>
        </div>
      </div>

      {/* Center 3D Flip-Out Hero Display & Ornate Celestia Callout Plaque */}
      <AnimatePresence mode="wait">
        <motion.div
          key={`reveal-card-${index}-${item?.id}`}
          initial={
            prefersReducedMotion
              ? { opacity: 0 }
              : { opacity: 0, rotateY: -155, rotateX: 22, scale: 0.35, y: 18 }
          }
          animate={{ opacity: 1, rotateY: 0, rotateX: 0, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.9, y: -10 }}
          transition={{ type: 'spring', stiffness: 230, damping: 20 }}
          style={{ transformStyle: 'preserve-3d' }}
          className="relative z-20 mt-auto flex flex-col items-center"
        >
          {/* Mascot Sprite (or Reduced-Motion Fallback for Crystal / Points) */}
          <div
            onClick={(e) => {
              if (isMascot && awakened) e.stopPropagation();
            }}
            className="flex min-h-[185px] items-center justify-center"
          >
            {isMascot ? (
              <div
                className={`rounded-full p-2 transition-all duration-300 ${
                  !awakened
                    ? 'brightness-0 contrast-200 drop-shadow-[0_0_24px_rgba(253,224,71,0.95)]'
                    : 'drop-shadow-[0_14px_24px_rgba(67,40,18,0.38)]'
                }`}
              >
                <Mascot
                  size={162}
                  label={`${item.name} mascot`}
                  directions={art.directions}
                  reactions={art.reactions}
                  tier={art.tier}
                  trackPointer
                />
              </div>
            ) : prefersReducedMotion ? (
              isCrystal ? (
                <CosmicCrystalGem size={136} glow />
              ) : (
                <AstralPointEmblem points={item?.pointsRefund || 20} size={124} />
              )
            ) : (
              /* Spacer letting the live 3D spinning Three.js Crystal / 3D Coin take center stage */
              <div className="h-44 w-44" aria-hidden="true" />
            )}
          </div>

          {/* Harmonious Celestia Parchment & Gold 3D Callout Plaque */}
          <div
            style={{
              borderColor: theme.stripBorder,
              boxShadow: `0 6px 0 ${theme.stripLip}, 0 14px 28px rgba(67,40,18,0.28)`,
            }}
            className="mt-1 flex flex-col items-center rounded-2xl border-2 bg-[#FFFBF2]/95 px-7 py-2.5 text-center backdrop-blur-sm"
          >
            <div className="flex flex-wrap items-center justify-center gap-1.5">
              <span
                style={{
                  backgroundColor: theme.badgeBg,
                  borderColor: theme.badgeBorder,
                  color: theme.badgeText,
                }}
                className="rounded-full border px-2.5 py-0.5 text-[10px] font-extrabold uppercase tracking-widest"
              >
                {item?.tierName || theme.label}
              </span>
              {isMascot && (
                <span className="rounded-full border border-[#FDE047] bg-[#F59E0B] px-2 py-0.5 text-[10px] font-black uppercase tracking-wider text-white">
                  NEW UNLOCK
                </span>
              )}
            </div>

            <h3 className="mt-1 text-xl font-black tracking-wide text-[#2D1B0D] sm:text-2xl">
              {item?.name}
            </h3>

            <div className="mt-1 flex items-center justify-center gap-1">
              {Array.from({ length: theme.stars }).map((_, starIdx) => (
                <motion.span
                  key={starIdx}
                  initial={prefersReducedMotion ? false : { scale: 0, rotate: -40, opacity: 0 }}
                  animate={{ scale: 1, rotate: 0, opacity: 1 }}
                  transition={{
                    type: 'spring',
                    stiffness: 420,
                    damping: 15,
                    delay: starIdx * 0.065,
                  }}
                >
                  <PrimogemStar size={17} color={theme.starColor} coreColor="#FFFBEB" />
                </motion.span>
              ))}
            </div>
          </div>
        </motion.div>
      </AnimatePresence>

      {/* Bottom Advance Prompt Pill */}
      <div className="relative z-20 mt-3 flex items-center gap-1 rounded-full border border-[#E5CCA1] bg-[#FFFBF0]/88 px-4 py-1 text-xs font-bold text-[#5C3A16] shadow-sm">
        <span>
          {index + 1 < total ? 'Click anywhere for next drop' : 'Click anywhere to view summary'}
        </span>
        <ChevronRight className="h-3.5 w-3.5 text-[#D97706]" />
      </div>
    </div>
  );
}

/**
 * STAGE 3 OF WISH: Harmonious Celestia Stage + 3D Flipping Bevelled Wish Plaques!
 * Eliminates the clashing dark-navy box and upgrades every reward card with 3D flip
 * entrance, 3D bevel lip depth, foil specular sweep, and pointer-driven 3D tilt.
 */
export function GenshinWishSummaryStage({
  pulls,
  rollSeq = 0,
  pityCount,
  pityHard,
  tickets,
  costSingle,
  costMulti,
  onInspectPull,
  onReturnIdle,
  onClose,
  onRollAgain,
  rolling,
  autoSkip = false,
  onToggleAutoSkip,
  autoReturnSuppressed = false,
  autoReturnBlocked = false,
  autoReturnSeconds = SUMMARY_AUTO_RETURN_SECONDS,
}) {
  const prefersReducedMotion = useReducedMotion();
  const sortedPulls = useMemo(() => sortPullsForGenshinSummary(pulls), [pulls]);

  const onReturnIdleRef = useRef(onReturnIdle);
  const countdownArmedAtRef = useRef(Date.now());
  const prevRollSeqRef = useRef(rollSeq);
  const prevAutoSkipRef = useRef(autoSkip);
  const [countdownCancelled, setCountdownCancelled] = useState(false);
  const [countdownRunId, setCountdownRunId] = useState(0);
  const [countdownRemaining, setCountdownRemaining] = useState(null);

  useEffect(() => {
    onReturnIdleRef.current = onReturnIdle;
  }, [onReturnIdle]);

  useEffect(() => {
    if (rollSeq !== prevRollSeqRef.current) {
      prevRollSeqRef.current = rollSeq;
      setCountdownCancelled(false);
      setCountdownRunId((n) => n + 1);
      countdownArmedAtRef.current = Date.now();
    }
  }, [rollSeq]);

  useEffect(() => {
    if (autoSkip && !prevAutoSkipRef.current) {
      setCountdownCancelled(false);
      setCountdownRunId((n) => n + 1);
      countdownArmedAtRef.current = Date.now();
    }
    prevAutoSkipRef.current = autoSkip;
  }, [autoSkip]);

  const countdownActive = shouldRunSummaryAutoReturn({
    autoSkip,
    suppressed: autoReturnSuppressed,
    blocked: autoReturnBlocked || rolling,
    cancelled: countdownCancelled,
  });

  const cancelCountdown = () => {
    setCountdownCancelled(true);
  };

  const handlePlaquePointerInteract = () => {
    if (!countdownActive) return;
    if (Date.now() - countdownArmedAtRef.current < 250) return;
    setCountdownCancelled(true);
  };

  useEffect(() => {
    if (!countdownActive) {
      setCountdownRemaining(null);
      return undefined;
    }

    let left = autoReturnSeconds;
    setCountdownRemaining(left);
    countdownArmedAtRef.current = Date.now();

    const timerId = window.setInterval(() => {
      left -= 1;
      if (left <= 0) {
        window.clearInterval(timerId);
        setCountdownRemaining(0);
        onReturnIdleRef.current?.();
        return;
      }
      setCountdownRemaining(left);
    }, 1000);

    return () => {
      window.clearInterval(timerId);
    };
  }, [countdownActive, rollSeq, countdownRunId, autoReturnSeconds]);

  useEffect(() => {
    if (!countdownActive) return undefined;
    const onKeyDown = () => {
      setCountdownCancelled(true);
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [countdownActive]);

  const totalRefunded = pulls.reduce((sum, p) => sum + (p.item?.pointsRefund || 0), 0);
  const crystalsWon = pulls.filter((p) => p.item?.type === 'crystal').length;
  const mascotsWon = pulls.filter((p) => p.item?.type === 'mascot');
  const rollCount = pulls.length || 1;
  const rollAgainCost = rollCount === 1 ? costSingle : costMulti;

  return (
    <div
      onPointerDown={cancelCountdown}
      style={{ perspective: '1200px' }}
      className="relative mx-3 my-2.5 flex min-h-[430px] flex-1 flex-col justify-between overflow-hidden rounded-[24px] border-2 border-[#E2B45B] px-4 py-3.5 shadow-[0_12px_32px_rgba(120,72,16,0.16)] sm:mx-5 sm:my-3 sm:min-h-[445px] sm:px-6"
    >
      {/* Unified Celestia Clouds & Warm Lavender-Gold Sky (Matches Header & Footer!) */}
      <CelestiaStageBackdrop veilColor="rgba(68, 52, 102, 0.18)" />

      {/* Top Harmonious Champagne-Ivory Summary Bar */}
      <div className="relative z-20 flex flex-wrap items-center justify-between gap-2 rounded-2xl border border-[#E6C785] bg-[#FFFBF2]/92 px-4 py-2 shadow-sm backdrop-blur-sm">
        <div className="flex items-center gap-2">
          <PrimogemStar size={18} color="#D97706" />
          <h4 className="text-sm font-extrabold tracking-tight text-[#2B180A] sm:text-base">
            Wish Summary ({rollCount} {rollCount === 1 ? 'Summon' : 'Summons'})
          </h4>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          {crystalsWon > 0 && (
            <span className="inline-flex items-center gap-1 rounded-full border border-[#F59E0B] bg-[#FEF08A] px-2.5 py-0.5 text-xs font-extrabold tabular-nums text-[#78350F]">
              <CosmicCrystalGem size={14} glow={false} />
              <span>+{crystalsWon} Crystal</span>
            </span>
          )}
          {mascotsWon.length > 0 && (
            <span className="inline-flex items-center gap-1 rounded-full border border-[#C084FC] bg-[#F3E8FF] px-2.5 py-0.5 text-xs font-extrabold tabular-nums text-[#6B21A8]">
              <Award className="h-3.5 w-3.5" />
              <span>+{mascotsWon.length} Mascot</span>
            </span>
          )}
          {totalRefunded > 0 && (
            <span className="inline-flex items-center gap-1 rounded-full border border-[#E5C585] bg-[#F8EAD0] px-2.5 py-0.5 text-xs font-extrabold tabular-nums text-[#5C3A16]">
              <Coins className="h-3.5 w-3.5 text-[#C97A16]" />
              <span>+{formatNumber(totalRefunded)} pts</span>
            </span>
          )}
          <span className="rounded-full border border-[#E5CCA1] bg-[#FFFDF7] px-2.5 py-0.5 text-[11px] font-bold tabular-nums text-[#5C4028]">
            Pity: {Math.min(pityCount, pityHard)} / {pityHard}
          </span>
        </div>
      </div>

      {/* Center 3D Flipping Bevelled Wish Plaques */}
      <div className="relative z-20 my-auto py-4">
        {rollCount === 1 ? (
          (() => {
            const single = sortedPulls[0];
            const item = single.pull.item;
            const theme = getRarityTheme(item);
            const isCrystal = item.type === 'crystal';
            const isMascot = item.type === 'mascot' && Boolean(item.mascotId);
            const isSpecial = getItemRarityKey(item) !== 'point';
            const art = isMascot ? mascotArt(item.mascotId) : null;

            return (
              <motion.div
                key={`single-${rollSeq}-${item.id}`}
                initial={
                  prefersReducedMotion
                    ? false
                    : { opacity: 0, rotateY: -95, rotateX: 16, scale: 0.85 }
                }
                animate={{ opacity: 1, rotateY: 0, rotateX: 0, scale: 1 }}
                transition={{ type: 'spring', stiffness: 240, damping: 20 }}
                style={{ transformStyle: 'preserve-3d' }}
              >
                <Plaque3DTilt
                  onClick={() => {
                    cancelCountdown();
                    if (isSpecial) onInspectPull?.(single.originalIndex);
                  }}
                  onPointerInteract={handlePlaquePointerInteract}
                  style={{
                    backgroundColor: theme.stripBg,
                    borderColor: theme.stripBorder,
                    color: theme.stripText,
                    boxShadow: `0 8px 0 ${theme.stripLip}, 0 18px 34px ${theme.stripGlow}`,
                  }}
                  className={`mx-auto flex w-full max-w-[250px] flex-col items-center rounded-3xl border-2 p-5 text-center ${
                    isSpecial ? 'cursor-pointer' : ''
                  }`}
                >
                  <div className="flex items-center gap-1">
                    {Array.from({ length: theme.stars }).map((_, sIdx) => (
                      <PrimogemStar key={sIdx} size={15} color={theme.starColor} />
                    ))}
                  </div>
                  <div
                    style={{ transform: 'translateZ(20px)' }}
                    className="my-2.5 flex h-28 w-28 items-center justify-center"
                  >
                    {isCrystal ? (
                      <CosmicCrystalGem size={96} glow />
                    ) : isMascot ? (
                      <Mascot
                        size={104}
                        label={`${item.name} mascot`}
                        directions={art.directions}
                        reactions={art.reactions}
                        tier={art.tier}
                      />
                    ) : (
                      <AstralPointEmblem points={item.pointsRefund || 20} size={96} />
                    )}
                  </div>
                  <span className="text-base font-extrabold">{item.name}</span>
                  <span
                    style={{
                      backgroundColor: theme.stripAccent,
                      color: theme.stripText,
                    }}
                    className="mt-1 rounded-full px-3 py-0.5 text-[10px] font-extrabold uppercase tracking-wider"
                  >
                    {item.tierName}
                  </span>
                </Plaque3DTilt>
              </motion.div>
            );
          })()
        ) : (
          /* 10-Pull 3D Flipping Bevelled Celestial Plaques */
          <div className="grid grid-cols-2 gap-2.5 sm:grid-cols-5 lg:grid-cols-10">
            {sortedPulls.map(({ pull, originalIndex }, idx) => {
              const item = pull.item;
              const rarityKey = getItemRarityKey(item);
              const theme = getRarityTheme(rarityKey);
              const isCrystal = item.type === 'crystal';
              const isMascot = item.type === 'mascot' && Boolean(item.mascotId);
              const isSpecial = rarityKey !== 'point';
              const art = isMascot ? mascotArt(item.mascotId) : null;

              return (
                <motion.div
                  key={`${rollSeq}-${originalIndex}-${item.id}`}
                  initial={
                    prefersReducedMotion
                      ? { opacity: 0 }
                      : { opacity: 0, rotateY: -96, rotateX: 20, y: 28, scale: 0.84 }
                  }
                  animate={{ opacity: 1, rotateY: 0, rotateX: 0, y: 0, scale: 1 }}
                  transition={{
                    type: 'spring',
                    stiffness: 260,
                    damping: 20,
                    delay: prefersReducedMotion ? 0 : idx * 0.06,
                  }}
                  style={{ transformStyle: 'preserve-3d' }}
                >
                  <Plaque3DTilt
                    onClick={() => {
                      cancelCountdown();
                      if (isSpecial) onInspectPull?.(originalIndex);
                    }}
                    onPointerInteract={handlePlaquePointerInteract}
                    title={isSpecial ? `Inspect ${item.name} in 3D` : item.name}
                    style={{
                      backgroundColor: theme.stripBg,
                      borderColor: theme.stripBorder,
                      color: theme.stripText,
                      boxShadow: `0 6px 0 ${theme.stripLip}, 0 12px 24px ${theme.stripGlow}`,
                    }}
                    className={`group relative flex min-h-[222px] flex-col items-center justify-between overflow-visible rounded-2xl border-2 px-1.5 py-2.5 text-center ${
                      isSpecial ? 'cursor-pointer' : ''
                    }`}
                  >
                    {/* Inner Hairline Golden Arch Inset */}
                    <span
                      aria-hidden="true"
                      className="pointer-events-none absolute inset-[3px] rounded-xl border border-[#F3DFB4]/80"
                    />

                    {/* Specular Foil Sweep Across Plaque On Deal */}
                    {!prefersReducedMotion && (
                      <span
                        aria-hidden="true"
                        className="pointer-events-none absolute inset-0 overflow-hidden rounded-2xl"
                      >
                        <motion.span
                          initial={{ x: '-140%' }}
                          animate={{ x: '240%' }}
                          transition={{
                            duration: 0.85,
                            delay: idx * 0.06 + 0.18,
                            ease: 'easeInOut',
                          }}
                          className="block h-full w-10 -skew-x-12 bg-white/55"
                        />
                      </span>
                    )}

                    {/* Tall Genshin Rarity Light Spire Above & Below High-Rarity Plaques */}
                    {isSpecial && (
                      <>
                        <svg
                          viewBox="0 0 24 28"
                          className="pointer-events-none absolute -top-6 left-1/2 h-6 w-5 -translate-x-1/2 overflow-visible"
                          aria-hidden="true"
                        >
                          <polygon points="12,0 18,28 6,28" fill={theme.beamColor} opacity="0.82" />
                          <polygon points="12,4 14.5,28 9.5,28" fill="#FFFFFF" opacity="0.9" />
                        </svg>
                        <svg
                          viewBox="0 0 24 28"
                          className="pointer-events-none absolute -bottom-6 left-1/2 h-6 w-5 -translate-x-1/2 overflow-visible"
                          aria-hidden="true"
                        >
                          <polygon points="6,0 18,0 12,28" fill={theme.beamColor} opacity="0.82" />
                          <polygon points="9.5,0 14.5,0 12,24" fill="#FFFFFF" opacity="0.9" />
                        </svg>
                      </>
                    )}

                    {/* Top Crest / NEW Tag */}
                    <div className="relative z-10 flex min-h-[18px] items-center justify-center">
                      {isCrystal ? (
                        <span className="inline-flex items-center gap-0.5 rounded-full bg-[#F59E0B] px-1.5 py-0.5 text-[9px] font-black uppercase tracking-wider text-white shadow-sm">
                          <PrimogemStar size={8} color="#FFFBEB" />
                          <span>GEM</span>
                        </span>
                      ) : isMascot ? (
                        <span className="rounded-full bg-[#10B981] px-1.5 py-0.5 text-[9px] font-black uppercase tracking-wider text-white shadow-sm">
                          NEW
                        </span>
                      ) : (
                        <PrimogemStar size={11} color="#D9A94E" />
                      )}
                    </div>

                    {/* Center 3D Raised Item Artwork */}
                    <div
                      style={{ transform: 'translateZ(16px)' }}
                      className="relative z-10 my-1 flex h-16 w-16 items-center justify-center"
                    >
                      {isCrystal ? (
                        <CosmicCrystalGem size={54} glow />
                      ) : isMascot ? (
                        <Mascot
                          size={60}
                          label={`${item.name} mascot`}
                          directions={art.directions}
                          reactions={art.reactions}
                          interactive={false}
                        />
                      ) : (
                        <AstralPointEmblem points={item.pointsRefund || 20} size={58} />
                      )}
                    </div>

                    {/* Rarity Stars */}
                    <div className="relative z-10 flex items-center justify-center gap-0.5">
                      {Array.from({ length: theme.stars }).map((_, sIdx) => (
                        <PrimogemStar key={sIdx} size={9} color={theme.starColor} />
                      ))}
                    </div>

                    {/* Bottom Name & Value Plate */}
                    <div
                      style={{ backgroundColor: theme.stripAccent }}
                      className="relative z-10 mt-1.5 w-full rounded-xl border border-black/5 px-1 py-1.5"
                    >
                      <span className="block truncate text-[11px] font-extrabold tabular-nums leading-tight">
                        {isCrystal
                          ? 'Crystal'
                          : isMascot
                            ? item.name
                            : `+${item.pointsRefund}`}
                      </span>
                      <span className="block text-[9px] font-bold uppercase tracking-wider opacity-75">
                        {isCrystal ? '+1 Gem' : isMascot ? item.rarity : 'Points'}
                      </span>
                    </div>
                  </Plaque3DTilt>
                </motion.div>
              );
            })}
          </div>
        )}
      </div>

      {/* Bottom Harmonious Celestia Action Bar */}
      <div
        onPointerDown={(e) => e.stopPropagation()}
        className="relative z-20 flex flex-wrap items-center justify-between gap-2.5 pt-2"
      >
        <div>
          {onToggleAutoSkip && (
            <AutoSkipToggleButton enabled={autoSkip} onToggle={onToggleAutoSkip} />
          )}
        </div>

        <div className="flex flex-wrap items-center justify-end gap-2.5">
          <button
            type="button"
            onClick={onReturnIdle}
            aria-label={
              countdownRemaining > 0
                ? `Banner (returning in ${countdownRemaining}s)`
                : 'Banner'
            }
            className="relative inline-flex min-h-10 cursor-pointer items-center justify-center gap-1.5 overflow-hidden rounded-full border-2 border-[#DFB66C] bg-[#FFFBF0] px-5 text-xs font-extrabold text-[#3B2313] shadow-sm transition-colors hover:bg-white focus:outline-none focus-visible:ring-2 focus-visible:ring-[#D97706]"
          >
            {countdownRemaining > 0 && (
              <motion.span
                key={`auto-return-bar-${rollSeq}-${countdownRunId}`}
                aria-hidden="true"
                initial={{ scaleX: 1 }}
                animate={prefersReducedMotion ? undefined : { scaleX: 0 }}
                transition={{ duration: autoReturnSeconds, ease: 'linear' }}
                style={{ transformOrigin: 'left center' }}
                className="pointer-events-none absolute inset-x-0 bottom-0 h-[3px] bg-[#F59E0B]"
              />
            )}
            <span className="relative">Banner</span>
            {countdownRemaining > 0 && (
              <span className="relative rounded-full bg-[#FEF08A] px-1.5 py-0.5 text-[10px] font-black tabular-nums text-[#78350F]">
                {countdownRemaining}s
              </span>
            )}
          </button>

          <button
            type="button"
            onClick={onClose}
            className="min-h-10 cursor-pointer rounded-full border border-[#DFB66C] bg-[#F7E7CA]/95 px-5 text-xs font-extrabold text-[#4A2E14] shadow-sm transition-colors hover:bg-[#FFFBF0] focus:outline-none focus-visible:ring-2 focus-visible:ring-[#D97706]"
          >
            Done
          </button>

          <button
            type="button"
            disabled={rolling}
            onClick={() => {
              cancelCountdown();
              onRollAgain(rollCount);
            }}
            className="inline-flex min-h-10 cursor-pointer items-center gap-2 rounded-full border-2 border-[#FDE68A] bg-[#F59E0B] px-6 text-xs font-extrabold text-white shadow-[0_6px_18px_rgba(245,158,11,0.38)] transition-colors hover:bg-[#D97706] focus:outline-none focus-visible:ring-2 focus-visible:ring-[#92400E] disabled:opacity-60"
          >
            <span>Roll again ({rollCount}x)</span>
            <span className="rounded-full bg-[#78350F]/45 px-2 py-0.5 text-[11px] font-bold tabular-nums text-[#FEF9C3]">
              {tickets >= rollCount
                ? `${rollCount} Ticket${rollCount > 1 ? 's' : ''}`
                : `${formatNumber(rollAgainCost)} pts`}
            </span>
          </button>
        </div>
      </div>
    </div>
  );
}
