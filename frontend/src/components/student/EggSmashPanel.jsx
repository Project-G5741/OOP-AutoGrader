import { useState, useEffect, useRef, useCallback } from 'react';
import { useReducedMotion } from 'framer-motion';
import { Sparkles, Hammer, X } from 'lucide-react';
import { useToast } from '../ui/Toast';
import { formatNumber } from '../../utils/formatters';
import { apiFetch } from '../../utils/apiFetch';
import { authHeaders } from '../../utils/authHeaders';
import { friendlyLoadErrorFromResponse, toFriendlyError } from '../../utils/apiError';
import './EggSmashPanel.css';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';

export const EGG_SMASH_TURN_COST = 1000;
export const EGG_SMASH_BOARD_SIZE = 9;
export const EGG_SMASH_PICKS_REQUIRED = 3;

const BASE_STAGE_WIDTH = 892;
const BASE_STAGE_HEIGHT = 528;
const HOME_X = 20;
const HOME_Y = 54;
const ROOSTER_W = 172;
const ROOSTER_H = 176;
const HAMMER_HIT_X_PCT = 0.819;
const HAMMER_HIT_Y_PCT = 0.833;
const ROW_DEPTH_SCALE = [0.93, 0.98, 1.04];

const SPRITES = {
  bg: '/egg-smash/meadow-bg.webp',
  banner: '/egg-smash/banner.webp',
  hammer: '/egg-smash/hammer.webp',
  intact: '/egg-smash/egg-intact.webp',
  chick: '/egg-smash/egg-chick.webp',
  crystal: '/egg-smash/egg-crystal.webp',
  perch: '/egg-smash/perch.webp',
  rIdle: '/egg-smash/rooster-idle.webp',
  rWin: '/egg-smash/rooster-win.webp',
  rLeap: '/egg-smash/rooster-leap.webp',
  rHit: '/egg-smash/rooster-hit.webp',
  rVault: '/egg-smash/rooster-vault.webp',
};

function pointBadgePalette(pts) {
  if (pts >= 300) {
    return { border: '#F59E0B', bg: '#2B1806', text: '#FDE68A' };
  }
  if (pts >= 200) {
    return { border: '#60A5FA', bg: '#0F1E36', text: '#DBEAFE' };
  }
  if (pts >= 100) {
    return { border: '#34D399', bg: '#0B291E', text: '#D1FAE5' };
  }
  return { border: '#D6B88A', bg: '#26180B', text: '#FDE8C4' };
}

function createInitialTiles() {
  return Array.from({ length: EGG_SMASH_BOARD_SIZE }, (_, index) => ({
    index,
    status: 'intact',
    outcome: null,
    impactBurst: false,
  }));
}

function sleep(ms) {
  return new Promise((resolve) => {
    setTimeout(resolve, ms);
  });
}

export default function EggSmashPanel({
  points = 0,
  crystals = 0,
  onWalletSync,
  onBusyChange,
  onSwitchToWheel,
  onClose,
}) {
  const showToast = useToast();
  const reducedMotion = useReducedMotion();

  const [displayPoints, setDisplayPoints] = useState(points);
  const [displayCrystals, setDisplayCrystals] = useState(crystals);
  const [phase, setPhase] = useState('picking'); // 'picking' | 'smashing' | 'revealed'
  const [selected, setSelected] = useState([]);
  const [tiles, setTiles] = useState(createInitialTiles);
  const [bannerText, setBannerText] = useState('Pick 3 eggs to smash!');
  const [stageScale, setStageScale] = useState(1);

  const outerWrapRef = useRef(null);
  const stageRef = useRef(null);
  const roosterActorRef = useRef(null);
  const roosterInnerRef = useRef(null);
  const roosterImgRef = useRef(null);
  const roosterShadowRef = useRef(null);
  const emptyPerchRef = useRef(null);
  const smashBtnRef = useRef(null);

  const mountedRef = useRef(true);
  const pendingWalletRef = useRef(null);
  const scaleRef = useRef(1);
  const onWalletSyncRef = useRef(onWalletSync);
  const onBusyChangeRef = useRef(onBusyChange);

  useEffect(() => {
    onWalletSyncRef.current = onWalletSync;
  }, [onWalletSync]);

  useEffect(() => {
    onBusyChangeRef.current = onBusyChange;
  }, [onBusyChange]);

  // Keep local readout synced with parent DB wallet when idle
  useEffect(() => {
    if (phase !== 'smashing') {
      setDisplayPoints(points);
    }
  }, [points, phase]);

  useEffect(() => {
    if (phase !== 'smashing') {
      setDisplayCrystals(crystals);
    }
  }, [crystals, phase]);

  // Preload sprites & load authoritative balance straight from PostgreSQL on mount
  useEffect(() => {
    mountedRef.current = true;
    Object.values(SPRITES).forEach((src) => {
      const img = new Image();
      img.src = src;
    });

    let cancelled = false;
    (async () => {
      try {
        const res = await apiFetch(`${API_BASE}/api/students/streak`, {
          headers: authHeaders(),
        });
        if (!res.ok || cancelled) return;
        const body = await res.json();
        if (cancelled) return;
        if (typeof body?.points === 'number' && typeof body?.crystals === 'number') {
          setDisplayPoints(body.points);
          setDisplayCrystals(body.crystals);
          onWalletSyncRef.current?.({
            points: body.points,
            crystals: body.crystals,
            gachaTickets: body.gachaTickets,
            gachaPity: body.gachaPityCount,
          });
        }
      } catch {
        // Keep current props if offline
      }
    })();

    return () => {
      cancelled = true;
      mountedRef.current = false;
      onBusyChangeRef.current?.(false);
      if (pendingWalletRef.current) {
        onWalletSyncRef.current?.(pendingWalletRef.current);
        pendingWalletRef.current = null;
      }
    };
  }, []);

  // Responsive scale observer so mobile viewports scale the 800x472 diorama cleanly
  useEffect(() => {
    const el = outerWrapRef.current;
    if (!el || typeof ResizeObserver === 'undefined') return undefined;

    const updateScale = () => {
      const w = el.clientWidth || BASE_STAGE_WIDTH;
      const next = w < BASE_STAGE_WIDTH ? Math.max(0.42, w / BASE_STAGE_WIDTH) : 1;
      scaleRef.current = next;
      setStageScale(next);
    };

    updateScale();
    const observer = new ResizeObserver(updateScale);
    observer.observe(el);
    return () => observer.disconnect();
  }, []);

  const setRoosterTransform = useCallback((x, y, dir = 1, tiltDeg = 0, scale = 1) => {
    if (roosterActorRef.current) {
      roosterActorRef.current.style.transform = `translate3d(${x.toFixed(1)}px, ${y.toFixed(1)}px, 0)`;
    }
    if (roosterInnerRef.current) {
      roosterInnerRef.current.style.transform = `scaleX(${dir}) rotate(${tiltDeg.toFixed(1)}deg) scale(${scale.toFixed(3)})`;
    }
  }, []);

  const setShadow = useCallback((x, groundY, airRatio, visible) => {
    const sh = roosterShadowRef.current;
    if (!sh) return;
    if (!visible) {
      sh.style.opacity = '0';
      return;
    }
    const sc = 1 - airRatio * 0.42;
    const op = 0.52 * (1 - airRatio * 0.58);
    sh.style.opacity = op.toFixed(2);
    sh.style.transform = `translate3d(${(x + ROOSTER_W * 0.27).toFixed(1)}px, ${(groundY - 12).toFixed(1)}px, 0) scale(${sc.toFixed(2)})`;
  }, []);

  const parkRoosterHome = useCallback((poseSrc = SPRITES.rIdle, idleBob = true) => {
    if (emptyPerchRef.current) {
      emptyPerchRef.current.style.opacity = '0';
    }
    setShadow(0, 0, 0, false);
    if (roosterImgRef.current) {
      roosterImgRef.current.src = poseSrc;
    }
    if (roosterInnerRef.current) {
      roosterInnerRef.current.className = idleBob ? 'w-full h-full egg-smash-rooster-idle' : 'w-full h-full';
      roosterInnerRef.current.style.transform = '';
    }
    if (roosterActorRef.current) {
      roosterActorRef.current.style.transform = `translate3d(${HOME_X}px, ${HOME_Y}px, 0)`;
    }
  }, [setShadow]);

  const animateJump = useCallback((fromX, fromY, toX, toY, arcHeight, durationMs, dir = 1) => {
    if (reducedMotion) {
      setRoosterTransform(toX, toY, dir, 0, 1);
      return sleep(50);
    }
    return new Promise((resolve) => {
      const start = performance.now();
      const fromGroundY = fromY + ROOSTER_H * 0.9;
      const toGroundY = toY + ROOSTER_H * 0.9;

      function tick(now) {
        if (!mountedRef.current) {
          resolve();
          return;
        }
        const raw = Math.min(1, (now - start) / durationMs);
        const curX = fromX + (toX - fromX) * raw;
        const linearY = fromY + (toY - fromY) * raw;
        const parabola = 4 * arcHeight * raw * (1 - raw);
        const curY = linearY - parabola;

        const tilt = -7 + 14 * raw;
        setRoosterTransform(curX, curY, dir, tilt, 1.02);

        const groundY = fromGroundY + (toGroundY - fromGroundY) * raw;
        const airRatio = 4 * raw * (1 - raw);
        setShadow(curX, groundY, airRatio, true);

        if (raw < 1) {
          requestAnimationFrame(tick);
        } else {
          setRoosterTransform(toX, toY, dir, 0, 1);
          setShadow(toX, toGroundY, 0, true);
          resolve();
        }
      }
      requestAnimationFrame(tick);
    });
  }, [reducedMotion, setRoosterTransform, setShadow]);

  const computeEggStrikeTarget = useCallback((eggIndex, prevCenterX) => {
    const stageEl = stageRef.current;
    if (!stageEl) {
      return { landX: 220, landY: 160, dir: 1, centerX: 300 };
    }
    const slotEl = stageEl.querySelector(`[data-nest-slot="${eggIndex}"]`);
    if (!slotEl) {
      return { landX: 220, landY: 160, dir: 1, centerX: 300 };
    }
    const sc = scaleRef.current || 1;
    const stageRect = stageEl.getBoundingClientRect();
    const r = slotEl.getBoundingClientRect();

    const eggX = ((r.left - stageRect.left) + r.width * 0.5) / sc;
    const eggY = ((r.top - stageRect.top) + r.height * 0.48) / sc;
    const col = eggIndex % 3;

    const movingLeft = eggX < prevCenterX - 35 && col < 2;
    const dir = movingLeft ? -1 : 1;

    const landX = dir === 1
      ? eggX - ROOSTER_W * HAMMER_HIT_X_PCT
      : eggX - ROOSTER_W * (1 - HAMMER_HIT_X_PCT);
    const landY = eggY - ROOSTER_H * HAMMER_HIT_Y_PCT;

    return { landX, landY, dir, centerX: eggX };
  }, []);

  const triggerStageThud = useCallback(() => {
    if (reducedMotion || !stageRef.current) return;
    const st = stageRef.current;
    st.classList.remove('egg-smash-thud');
    void st.offsetWidth;
    st.classList.add('egg-smash-thud');
  }, [reducedMotion]);

  const resetBoardForNextTurn = useCallback(() => {
    setPhase('picking');
    setSelected([]);
    setTiles(createInitialTiles());
    setBannerText('Pick 3 eggs to smash!');
    parkRoosterHome(SPRITES.rIdle, true);
  }, [parkRoosterHome]);

  const toggleEggSelect = (index) => {
    if (phase === 'smashing') return;

    let baseSelected = selected;
    if (phase === 'revealed') {
      setPhase('picking');
      setTiles(createInitialTiles());
      parkRoosterHome(SPRITES.rIdle, true);
      baseSelected = [];
    }

    const next = [...baseSelected];
    const pos = next.indexOf(index);
    if (pos !== -1) {
      next.splice(pos, 1);
    } else {
      if (next.length >= EGG_SMASH_PICKS_REQUIRED) {
        next.shift();
      }
      next.push(index);
    }

    setSelected(next);
    const left = EGG_SMASH_PICKS_REQUIRED - next.length;
    if (left === 3) {
      setBannerText('Pick 3 eggs to smash!');
    } else if (left > 0) {
      setBannerText(`${next.length}/3 selected · Pick ${left}`);
    } else {
      setBannerText('3/3 ready · Smash now!');
    }
  };

  const pickRandomThree = () => {
    if (phase === 'smashing') return;
    if (phase === 'revealed') {
      setPhase('picking');
      setTiles(createInitialTiles());
      parkRoosterHome(SPRITES.rIdle, true);
    }
    const indices = [0, 1, 2, 3, 4, 5, 6, 7, 8];
    for (let i = indices.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [indices[i], indices[j]] = [indices[j], indices[i]];
    }
    setSelected(indices.slice(0, EGG_SMASH_PICKS_REQUIRED));
    setBannerText('3/3 ready · Smash now!');
  };

  const executeSmash = async () => {
    if (phase === 'smashing') return;
    if (phase === 'revealed') {
      resetBoardForNextTurn();
      return;
    }

    if (selected.length < EGG_SMASH_PICKS_REQUIRED) {
      setBannerText('Select 3 eggs first!');
      if (!reducedMotion && smashBtnRef.current) {
        const btn = smashBtnRef.current;
        btn.classList.remove('egg-smash-nudge');
        void btn.offsetWidth;
        btn.classList.add('egg-smash-nudge');
      }
      return;
    }

    if (displayPoints < EGG_SMASH_TURN_COST) {
      setBannerText('Not enough points!');
      showToast('You need at least 1,000 pts to smash eggs.', 'error');
      return;
    }

    const picks = [...selected];
    setPhase('smashing');
    setBannerText('Smashing eggs...');
    onBusyChangeRef.current?.(true);

    let payload;
    try {
      const response = await apiFetch(`${API_BASE}/api/students/streak/egg-smash`, {
        method: 'POST',
        headers: { ...authHeaders(), 'Content-Type': 'application/json' },
        body: JSON.stringify({ picks }),
      });

      if (!response.ok) {
        const msg = response.status === 409
          ? 'Not enough points to smash eggs.'
          : await friendlyLoadErrorFromResponse(response);
        if (mountedRef.current) {
          setPhase('picking');
          setBannerText(response.status === 409 ? 'Not enough points!' : 'Pick 3 eggs to smash!');
          onBusyChangeRef.current?.(false);
          showToast(msg, 'error');
        }
        return;
      }

      payload = await response.json();
    } catch (error) {
      if (mountedRef.current) {
        setPhase('picking');
        setBannerText('Pick 3 eggs to smash!');
        onBusyChangeRef.current?.(false);
        showToast(toFriendlyError(error), 'error');
      }
      return;
    }

    if (!mountedRef.current) return;

    // DB transaction is now durably committed; store authoritative end-wallet for unmount safety
    const finalWallet = {
      points: payload.points,
      crystals: payload.crystals,
    };
    pendingWalletRef.current = finalWallet;

    const cellsByIndex = new Map((payload.cells || []).map((c) => [c.index, c]));

    // Step visual wallet down by the 1,000 PTS cost as Rooster launches
    let runningPoints = Math.max(0, payload.points - (payload.pointsEarned || 0));
    let runningCrystals = Math.max(0, payload.crystals - (payload.crystalsEarned || 0));
    setDisplayPoints(runningPoints);
    setDisplayCrystals(runningCrystals);

    if (emptyPerchRef.current) {
      emptyPerchRef.current.style.opacity = '1';
    }
    if (roosterInnerRef.current) {
      roosterInnerRef.current.className = 'w-full h-full';
    }

    let curX = HOME_X;
    let curY = HOME_Y;
    let prevFocusX = HOME_X + ROOSTER_W * 0.5;

    for (let step = 0; step < picks.length; step++) {
      if (!mountedRef.current) return;
      const eggIdx = picks[step];
      const cell = cellsByIndex.get(eggIdx) || { type: 'points', points: 50, crystals: 0 };
      const target = computeEggStrikeTarget(eggIdx, prevFocusX);

      if (roosterImgRef.current) {
        roosterImgRef.current.src = SPRITES.rLeap;
      }
      const arcH = step === 0 ? 60 : 50;
      const flyMs = step === 0 ? 250 : 210;
      await animateJump(curX, curY, target.landX, target.landY, arcH, flyMs, target.dir);
      if (!mountedRef.current) return;

      if (roosterImgRef.current) {
        roosterImgRef.current.src = SPRITES.rHit;
      }
      setRoosterTransform(target.landX, target.landY, target.dir, 0, 1.03);
      triggerStageThud();

      const isCrystal = cell.type === 'crystal';
      const palette = pointBadgePalette(cell.points || 0);
      const outcome = isCrystal
        ? { type: 'crystal', pts: 0 }
        : { type: 'points', pts: cell.points || 0, ...palette };

      if (isCrystal) {
        runningCrystals += cell.crystals || 1;
        setDisplayCrystals(runningCrystals);
      } else {
        runningPoints += cell.points || 0;
        setDisplayPoints(runningPoints);
      }

      setTiles((prev) => prev.map((t, idx) => (
        idx === eggIdx
          ? { ...t, status: 'cracked', outcome, impactBurst: true }
          : t
      )));

      curX = target.landX;
      curY = target.landY;
      prevFocusX = target.centerX;

      await sleep(170);
      if (!mountedRef.current) return;

      setTiles((prev) => prev.map((t, idx) => (
        idx === eggIdx ? { ...t, impactBurst: false } : t
      )));
    }

    if (roosterImgRef.current) {
      roosterImgRef.current.src = SPRITES.rVault;
    }
    await animateJump(curX, curY, HOME_X, HOME_Y, 64, 265, -1);
    if (!mountedRef.current) return;

    // Reveal the 6 unpicked nests from the server board & sync exact DB wallet
    setTiles((prev) => prev.map((t, idx) => {
      if (t.status !== 'intact') return t;
      const cell = cellsByIndex.get(idx) || { type: 'points', points: 50 };
      const isCrystal = cell.type === 'crystal';
      const outcome = isCrystal
        ? { type: 'crystal', pts: 0 }
        : { type: 'points', pts: cell.points || 0, ...pointBadgePalette(cell.points || 0) };
      return { ...t, status: 'peek', outcome };
    }));

    setDisplayPoints(payload.points);
    setDisplayCrystals(payload.crystals);
    pendingWalletRef.current = null;
    onWalletSyncRef.current?.(finalWallet);

    const gotCrystal = (payload.crystalsEarned || 0) > 0;

    if (gotCrystal) {
      parkRoosterHome(SPRITES.rWin, false);
      setBannerText(`+1 CRYSTAL & +${formatNumber(payload.pointsEarned || 0)} PTS!`);
    } else {
      parkRoosterHome(SPRITES.rIdle, true);
      setBannerText(`Won +${formatNumber(payload.pointsEarned || 0)} PTS!`);
    }

    setSelected([]);
    setPhase('revealed');
    onBusyChangeRef.current?.(false);
  };

  const smashButtonStyle = (() => {
    if (phase === 'revealed') {
      return {
        backgroundColor: '#B45309',
        borderColor: '#FDE68A',
        boxShadow: '0 4px 0 #451A03, 0 8px 16px rgba(0,0,0,0.45)',
      };
    }
    if (selected.length === EGG_SMASH_PICKS_REQUIRED && displayPoints >= EGG_SMASH_TURN_COST && phase === 'picking') {
      return {
        backgroundColor: '#15803D',
        borderColor: '#86EFAC',
        boxShadow: '0 4px 0 #052E16, 0 0 14px rgba(34,197,94,0.55)',
      };
    }
    return {
      backgroundColor: '#525B69',
      borderColor: '#272C35',
      boxShadow: '0 4px 0 #1E2229, inset 0 1px 0 rgba(255,255,255,0.22)',
    };
  })();

  return (
    <div ref={outerWrapRef} className="w-full select-none">
      <div
        style={{
          height: `${Math.round(BASE_STAGE_HEIGHT * stageScale)}px`,
        }}
        className="relative w-full overflow-hidden"
      >
        <div
          ref={stageRef}
          style={{
            width: `${BASE_STAGE_WIDTH}px`,
            height: `${BASE_STAGE_HEIGHT}px`,
            transform: stageScale < 1 ? `scale(${stageScale})` : undefined,
            transformOrigin: 'top left',
          }}
          className="relative overflow-hidden bg-[#2A4814]"
        >
          {/* 1. Full-Bleed Sunlit Meadow + Upper-Left Knoll + Bottom-Left Fence & Boulder */}
          <img
            src={SPRITES.bg}
            alt="Sunlit meadow clearing"
            className="absolute inset-0 w-full h-full object-cover pointer-events-none"
          />
          <div
            className="absolute inset-0 pointer-events-none"
            style={{
              boxShadow: 'inset 0 -28px 42px rgba(14, 30, 5, 0.34), inset 0 16px 28px rgba(15, 23, 42, 0.18)',
            }}
          />

          {/* 2. Integrated Top-Left Mode Switcher Pill (Harmonized Parchment & Oak) */}
          {onSwitchToWheel && (
            <div
              role="tablist"
              aria-label="Game mode"
              className="absolute left-4 top-3 z-40 inline-flex items-center rounded-full border-[1.5px] border-[#5C3A1B] bg-[#F4E5C8]/95 p-1 shadow-[0_6px_14px_rgba(0,0,0,0.38)] backdrop-blur-sm"
            >
              <button
                type="button"
                role="tab"
                aria-selected={false}
                disabled={phase === 'smashing'}
                onClick={onSwitchToWheel}
                className="flex cursor-pointer items-center gap-1.5 rounded-full px-3 py-1 text-xs font-extrabold text-[#5C3A1B] transition-colors hover:bg-[#E8D2AA] hover:text-[#2C1606] disabled:cursor-not-allowed disabled:opacity-50"
              >
                <Sparkles className="h-3.5 w-3.5" />
                <span>Crystal Wheel</span>
              </button>
              <button
                type="button"
                role="tab"
                aria-selected={true}
                disabled={phase === 'smashing'}
                className="flex cursor-default items-center gap-1.5 rounded-full bg-[#4A2810] px-3 py-1 text-xs font-extrabold text-[#FEF3C7] shadow-sm"
              >
                <Hammer className="h-3.5 w-3.5 text-[#FBBF24]" />
                <span>Egg Smash</span>
              </button>
            </div>
          )}

          {/* 3. Integrated Top-Right Close Button */}
          {onClose && (
            <button
              type="button"
              onClick={onClose}
              disabled={phase === 'smashing'}
              aria-label="Close"
              className="absolute right-4 top-3 z-40 flex h-9 w-9 cursor-pointer items-center justify-center rounded-full border-[1.5px] border-[#5C3A1B] bg-[#F4E5C8]/95 text-[#3E2009] shadow-[0_6px_14px_rgba(0,0,0,0.38)] backdrop-blur-sm transition-colors hover:bg-[#FFFDF7] focus:outline-none focus-visible:ring-2 focus-visible:ring-[#FDE68A] disabled:cursor-not-allowed disabled:opacity-40"
            >
              <X className="h-4 w-4 stroke-[2.5]" />
            </button>
          )}

          {/* 4. Anchored Mossy Stone Perch on Upper-Left Knoll */}
          <img
            ref={emptyPerchRef}
            src={SPRITES.perch}
            alt=""
            className="absolute left-[20px] top-[54px] z-10 w-[172px] h-[176px] object-contain pointer-events-none egg-smash-rooster-grounded opacity-0"
          />

          {/* 5. Dynamic Meadow Ground Shadow Following Leaping Rooster */}
          <div
            ref={roosterShadowRef}
            className="absolute left-0 top-0 z-[24] w-[80px] h-[22px] rounded-full bg-[#0F2404] pointer-events-none opacity-0 blur-[3px]"
            style={{ willChange: 'transform, opacity' }}
          />

          {/* 6. Leaping & Smashing Blacksmith Rooster */}
          <div
            ref={roosterActorRef}
            className="absolute left-0 top-0 z-30 w-[172px] h-[176px] pointer-events-none"
            style={{
              transform: `translate3d(${HOME_X}px, ${HOME_Y}px, 0)`,
              willChange: 'transform',
            }}
          >
            <div ref={roosterInnerRef} className="w-full h-full egg-smash-rooster-idle">
              <img
                ref={roosterImgRef}
                src={SPRITES.rIdle}
                alt="Blacksmith Rooster"
                className="w-full h-full object-contain block egg-smash-rooster-grounded"
              />
            </div>
          </div>

          {/* 7. Locked-Aspect Carved Oak & Forged Iron Banner */}
          <div className="absolute top-2 left-[51%] -translate-x-1/2 z-20 w-[332px] aspect-[430/171] pointer-events-none">
            <img
              src={SPRITES.banner}
              alt=""
              className="w-full h-full block drop-shadow-[0_6px_12px_rgba(0,0,0,0.48)]"
            />
            <div className="absolute top-[11%] h-[38%] left-[20%] right-[20%] flex items-center justify-center">
              <span className="egg-smash-banner-title font-black text-[22px] leading-none tracking-widest uppercase whitespace-nowrap">
                EGG SMASH
              </span>
            </div>
            <div className="absolute top-[65%] h-[25%] left-[25%] right-[25%] flex items-center justify-center overflow-hidden">
              <span className="text-xs font-extrabold text-[#3E2009] leading-none tracking-tight whitespace-nowrap tabular-nums">
                {bannerText}
              </span>
            </div>
          </div>

          {/* 8. 3x3 Woven Straw Egg Nests Arena */}
          <div className="absolute z-20 left-[23.5%] right-[19.5%] top-[138px] bottom-[10px] grid grid-cols-3 grid-rows-3 items-end justify-items-center">
            {tiles.map((tile, i) => {
              const row = Math.floor(i / 3);
              const depthScale = ROW_DEPTH_SCALE[row];
              const selOrder = selected.indexOf(i) + 1;
              const isSelected = selOrder > 0;

              if (tile.status === 'intact') {
                return (
                  <button
                    key={i}
                    type="button"
                    data-nest-slot={i}
                    disabled={phase === 'smashing'}
                    onClick={() => toggleEggSelect(i)}
                    style={{ transform: `scale(${depthScale})` }}
                    aria-label={`Egg nest ${i + 1}${isSelected ? `, selected #${selOrder}` : ''}`}
                    className="relative flex flex-col items-center justify-end w-[124px] h-[120px] cursor-pointer focus:outline-none focus-visible:ring-2 focus-visible:ring-[#FDE68A] rounded-xl group disabled:cursor-default"
                  >
                    {isSelected && phase === 'picking' && (
                      <span className="absolute top-1 right-2 z-20 rounded-full bg-[#7C2D12] border-[1.5px] border-[#FDE68A] px-2 py-0.5 text-xs font-black text-[#FEF3C7] shadow-md tabular-nums leading-none">
                        #{selOrder}
                      </span>
                    )}
                    <img
                      src={SPRITES.intact}
                      alt=""
                      className={`w-[120px] h-[120px] object-contain pointer-events-none transition-transform duration-150 ${
                        isSelected
                          ? 'egg-smash-sel egg-smash-nest-selected'
                          : 'egg-smash-nest-grounded group-hover:-translate-y-1.5'
                      }`}
                    />
                  </button>
                );
              }

              if (tile.status === 'cracked') {
                const isCrystal = tile.outcome?.type === 'crystal';
                const sprite = isCrystal ? SPRITES.crystal : SPRITES.chick;
                return (
                  <div
                    key={i}
                    data-nest-slot={i}
                    style={{ transform: `scale(${depthScale})` }}
                    className="relative flex flex-col items-center justify-end w-[124px] h-[120px]"
                  >
                    {tile.impactBurst && (
                      <div className="absolute inset-2 rounded-full border-[3px] border-[#FDE68A] z-20 pointer-events-none egg-smash-impact" />
                    )}
                    {isCrystal ? (
                      <div className="absolute -top-1 z-20 rounded-md bg-[#78350F] border border-[#FDE68A] px-2 py-0.5 text-xs font-black text-[#FEF08A] shadow-lg egg-smash-reward leading-none whitespace-nowrap">
                        +1 CRYSTAL
                      </div>
                    ) : (
                      <div
                        className="absolute -top-0.5 right-0.5 z-20 rounded-md border px-1.5 py-0.5 font-mono text-xs font-black tabular-nums shadow-md egg-smash-reward leading-none"
                        style={{
                          backgroundColor: tile.outcome?.bg,
                          borderColor: tile.outcome?.border,
                          color: tile.outcome?.text,
                        }}
                      >
                        +{tile.outcome?.pts}
                      </div>
                    )}
                    <img
                      src={sprite}
                      alt=""
                      className={`w-[120px] h-[120px] object-contain pointer-events-none ${
                        isCrystal
                          ? 'egg-smash-nest-crystal'
                          : 'egg-smash-nest-grounded egg-smash-chick'
                      }`}
                    />
                  </div>
                );
              }

              // 'peek'
              const isMissedCrystal = tile.outcome?.type === 'crystal';
              const peekSprite = isMissedCrystal ? SPRITES.crystal : SPRITES.chick;
              return (
                <button
                  key={i}
                  type="button"
                  data-nest-slot={i}
                  onClick={() => toggleEggSelect(i)}
                  style={{ transform: `scale(${depthScale})` }}
                  aria-label={`Unpicked nest ${i + 1}`}
                  className={`relative flex flex-col items-center justify-end w-[124px] h-[120px] cursor-pointer focus:outline-none transition-opacity ${
                    isMissedCrystal ? 'opacity-85 hover:opacity-100' : 'opacity-55 hover:opacity-85'
                  }`}
                >
                  {isMissedCrystal ? (
                    <span className="absolute top-0 z-20 rounded bg-[#451A03]/90 border border-[#F59E0B] px-1.5 py-0.5 text-xs font-bold text-[#FDE68A] leading-none">
                      MISSED
                    </span>
                  ) : (
                    <span className="absolute top-0 right-1 z-20 rounded bg-[#1C1917]/85 px-1.5 py-0.5 font-mono text-xs font-bold text-[#E7E5E4] tabular-nums leading-none">
                      +{tile.outcome?.pts}
                    </span>
                  )}
                  <img
                    src={peekSprite}
                    alt=""
                    className="w-[114px] h-[114px] object-contain pointer-events-none egg-smash-nest-grounded"
                  />
                </button>
              );
            })}
          </div>

          {/* 9. Right Aged Parchment Ledger + Forged Plate CTA */}
          <div className="absolute z-20 right-4 top-[140px] bottom-3.5 w-[156px] flex flex-col justify-between">
            <div className="egg-smash-parchment relative rounded-xl pt-4 pb-3 px-3">
              <div className="egg-smash-header-tab absolute -top-3 left-1/2 -translate-x-1/2 rounded-md px-3.5 py-0.5 flex items-center gap-1">
                <span className="w-1.5 h-1.5 rounded-full bg-[#DDA23B]" />
                <span className="text-xs font-extrabold text-[#FDF3DC] tracking-wider uppercase leading-none">
                  Info
                </span>
                <span className="w-1.5 h-1.5 rounded-full bg-[#DDA23B]" />
              </div>

              <div className="divide-y divide-[#C9AE86]">
                <div className="py-2 flex flex-col items-center">
                  <span className="text-xs font-bold text-[#6B4421] leading-tight">Balance</span>
                  <span className="text-sm font-black text-[#2C1606] tabular-nums tracking-tight">
                    {formatNumber(displayPoints)} PTS
                  </span>
                </div>

                <div className="py-2 flex flex-col items-center">
                  <span className="text-xs font-bold text-[#6B4421] leading-tight">Per Smash</span>
                  <span className="text-sm font-black text-[#B91C1C] tabular-nums">
                    1,000 PTS
                  </span>
                </div>

                <div className="py-2 flex flex-col items-center">
                  <span className="text-xs font-bold text-[#6B4421] leading-tight">Selected</span>
                  <span
                    className={`text-sm font-black tabular-nums ${
                      selected.length === EGG_SMASH_PICKS_REQUIRED ? 'text-[#15803D]' : 'text-[#2C1606]'
                    }`}
                  >
                    {phase === 'revealed' ? '3 / 3' : `${selected.length} / 3`}
                  </span>
                </div>

                <div className="pt-2 flex flex-col items-center">
                  <span className="text-xs font-bold text-[#6B4421] leading-tight">Crystals</span>
                  <span className="text-sm font-black text-[#B45309] tabular-nums">
                    {formatNumber(displayCrystals)}
                  </span>
                </div>
              </div>
            </div>

            <div className="flex flex-col items-center gap-1.5">
              <button
                ref={smashBtnRef}
                type="button"
                disabled={phase === 'smashing'}
                onClick={executeSmash}
                style={smashButtonStyle}
                className="relative w-full h-[48px] rounded-xl border-2 flex items-center justify-center gap-1.5 font-black text-xs tracking-wider text-white uppercase transition-all active:translate-y-0.5 cursor-pointer disabled:opacity-70 disabled:cursor-not-allowed"
              >
                <img
                  src={SPRITES.hammer}
                  alt=""
                  className="w-5 h-5 object-contain drop-shadow shrink-0"
                />
                <span>{phase === 'revealed' ? 'NEW ROUND' : 'SMASH NOW!'}</span>
              </button>

              <button
                type="button"
                disabled={phase === 'smashing'}
                onClick={pickRandomThree}
                className="rounded-full bg-[#1E130A]/80 hover:bg-[#1E130A]/95 border border-[#A67B4B]/60 px-3 py-0.5 text-xs font-bold text-[#FDE8C4] cursor-pointer transition-colors backdrop-blur-sm disabled:opacity-50 disabled:cursor-not-allowed"
              >
                Auto-Pick 3
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
