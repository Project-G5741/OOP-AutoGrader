import React, { useEffect, useRef, useState } from 'react';
import {
  Compass,
  Navigation,
  Zap,
  Pin,
  Sword,
  Swords,
  Crosshair,
  Sparkles,
  Flower2,
  Clock,
  Orbit,
  Settings2,
} from 'lucide-react';
import { mascotArt } from './studentMascots';
import {
  AUTO_SKILL_INTERVALS,
  getMythicSkills,
  readMythicSkillPrefs,
  writeMythicSkillPrefs,
  sampleAutoSkillDelay,
  pickNextAutoSkill,
  createSkillInstance,
  stepSkillInstance,
  drawSkillInstance,
} from './mythicSkillEffects';

const ICON_BY_KEY = {
  sword: Sword,
  swords: Swords,
  crosshair: Crosshair,
  sparkles: Sparkles,
  flower: Flower2,
  clock: Clock,
  orbit: Orbit,
};

const SECTOR_TO_CELL = [5, 8, 7, 6, 3, 0, 1, 2];
const SECTOR_STEP = (Math.PI * 2) / 8;
const HYSTERESIS = 0.15;

const imageCache = new Map();

function getCachedImage(src) {
  if (!src) return null;
  let img = imageCache.get(src);
  if (!img) {
    img = new Image();
    img.decoding = 'async';
    img.src = src;
    imageCache.set(src, img);
  }
  return img;
}

function wrapAngle(angle) {
  return Math.atan2(Math.sin(angle), Math.cos(angle));
}

function drawFourPointStar(ctx, x, y, r, color, alpha) {
  if (alpha <= 0.01) return;
  ctx.save();
  ctx.globalAlpha = Math.max(0, Math.min(1, alpha));
  ctx.fillStyle = color;
  ctx.beginPath();
  for (let i = 0; i < 8; i += 1) {
    const rad = i % 2 === 0 ? r : r * 0.38;
    const ang = (i * Math.PI) / 4;
    if (i === 0) {
      ctx.moveTo(x + Math.cos(ang) * rad, y + Math.sin(ang) * rad);
    } else {
      ctx.lineTo(x + Math.cos(ang) * rad, y + Math.sin(ang) * rad);
    }
  }
  ctx.closePath();
  ctx.fill();
  ctx.restore();
}

export default function MythicScreenPetOverlay({
  mascotId,
  paused = false,
  skillTrigger = null,
  onBoop,
  onDockToHeader,
}) {
  const art = mascotArt(mascotId);
  const skills = getMythicSkills(mascotId);
  const canvasRef = useRef(null);
  const hitBoxRef = useRef(null);
  const [mode, setMode] = useState('roam');
  const modeRef = useRef('roam');
  const [skillPrefs, setSkillPrefs] = useState(() => readMythicSkillPrefs());
  const skillPrefsRef = useRef(skillPrefs);
  skillPrefsRef.current = skillPrefs;
  const [settingsOpen, setSettingsOpen] = useState(false);
  const settingsWrapRef = useRef(null);
  const petRef = useRef(null);
  const onBoopRef = useRef(onBoop);
  const activeSkillsRef = useRef([]);
  const skillsRef = useRef(skills);
  skillsRef.current = skills;
  const autoCastRef = useRef({
    countdown: 4.5 + Math.random() * 2.0,
    lastSkillId: null,
    sameCount: 0,
  });
  const castSkillRef = useRef(null);

  useEffect(() => {
    onBoopRef.current = onBoop;
  }, [onBoop]);

  const updatePrefs = (patch) => {
    setSkillPrefs((prev) => {
      const next = writeMythicSkillPrefs({ ...prev, ...patch });
      skillPrefsRef.current = next;
      if (Object.hasOwn(patch, 'autoCast') || Object.hasOwn(patch, 'intervalId')) {
        autoCastRef.current.countdown = patch.autoCast === true
          ? 3.5 + Math.random() * 2.0
          : sampleAutoSkillDelay(next.intervalId);
      }
      return next;
    });
  };

  const triggerSkill = (skillId) => {
    if (!skillId || !art?.canMove || paused) return;
    const pet = petRef.current;
    const vw = window.innerWidth;
    const vh = window.innerHeight;
    const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    const inst = createSkillInstance(skillId, {
      originX: pet ? pet.x : vw * 0.35,
      originY: pet ? pet.y : vh * 0.45,
      viewW: vw,
      viewH: vh,
      reducedMotion,
    });
    if (activeSkillsRef.current.length >= 2) {
      activeSkillsRef.current.shift();
    }
    activeSkillsRef.current.push(inst);
    autoCastRef.current.countdown = sampleAutoSkillDelay(skillPrefsRef.current.intervalId);
    if (pet) {
      pet.squashX = 1.12;
      pet.squashY = 0.89;
    }
  };
  castSkillRef.current = triggerSkill;

  useEffect(() => {
    if (paused || !art?.canMove) {
      activeSkillsRef.current = [];
      setSettingsOpen(false);
    }
  }, [paused, art]);

  useEffect(() => {
    autoCastRef.current = {
      countdown: 4.5 + Math.random() * 2.0,
      lastSkillId: null,
      sameCount: 0,
    };
  }, [mascotId]);

  useEffect(() => {
    if (!settingsOpen) return undefined;
    const onKeyDown = (e) => {
      if (e.key === 'Escape') setSettingsOpen(false);
    };
    const onDocPointerDown = (e) => {
      if (settingsWrapRef.current && !settingsWrapRef.current.contains(e.target)) {
        setSettingsOpen(false);
      }
    };
    window.addEventListener('keydown', onKeyDown);
    window.addEventListener('pointerdown', onDocPointerDown);
    return () => {
      window.removeEventListener('keydown', onKeyDown);
      window.removeEventListener('pointerdown', onDocPointerDown);
    };
  }, [settingsOpen]);

  useEffect(() => {
    if (skillTrigger?.skillId && !paused && art?.canMove) {
      castSkillRef.current?.(skillTrigger.skillId);
    }
  }, [skillTrigger, paused, art]);

  useEffect(() => {
    modeRef.current = mode;
    if (petRef.current) {
      petRef.current.waypointX = null;
      petRef.current.waypointY = null;
    }
  }, [mode]);

  useEffect(() => {
    if (!art?.canMove || paused) return undefined;

    getCachedImage(art.directions);
    getCachedImage(art.movement || art.directions);
    getCachedImage(art.reactions);

    const canvas = canvasRef.current;
    if (!canvas) return undefined;
    const ctx = canvas.getContext('2d', { alpha: true });
    const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    let viewW = window.innerWidth;
    let viewH = window.innerHeight;

    const syncCanvasSize = () => {
      const dpr = Math.min(window.devicePixelRatio || 1, 3);
      viewW = window.innerWidth;
      viewH = window.innerHeight;
      canvas.width = Math.max(1, Math.floor(viewW * dpr));
      canvas.height = Math.max(1, Math.floor(viewH * dpr));
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
      ctx.imageSmoothingEnabled = true;
      ctx.imageSmoothingQuality = 'high';
    };
    syncCanvasSize();
    window.addEventListener('resize', syncCanvasSize);

    if (!petRef.current) {
      petRef.current = {
        x: Math.min(viewW - 140, Math.max(180, viewW * 0.72)),
        y: Math.min(viewH - 160, Math.max(170, 220)),
        vx: -54,
        vy: 32,
        roamPhase: 0.45,
        waypointX: null,
        waypointY: null,
        squashX: 1.08,
        squashY: 0.93,
        turnScaleX: 1.0,
        turnScaleY: 1.0,
        facingRight: false,
        bankRad: 0,
        tiltX: 0,
        tiltY: 0,
        compassSector: -1,
        inCenterZone: true,
        currDirCell: 4,
        stepCooldown: 0,
        dragging: false,
        dragDX: 0,
        dragDY: 0,
        reactIdx: -1,
        reactTimer: 0,
        dashTimer: 0,
        parachuteTimer: 0.75,
        boops: 0,
        boopAt: 0,
        payoffStep: 0,
      };
    } else {
      petRef.current.squashX = 1.09;
      petRef.current.squashY = 0.92;
    }

    const pet = petRef.current;
    const mouse = { x: viewW * 0.5, y: viewH * 0.45, hasMoved: false };
    const trails = [];
    const bursts = [];
    const orbiters = [0, 1, 2, 3].map((i) => ({
      phase: (i * Math.PI) / 2,
      spd: 1.35 + (i % 2) * 0.18,
      rx: 60 + (i % 2) * 9,
      rz: 42,
      yo: i % 2 ? 10 : -9,
    }));

    const spawnBurst = (cx, cy, count) => {
      if (reducedMotion) return;
      for (let i = 0; i < count; i += 1) {
        const a = (Math.PI * 2 * i) / count + Math.random() * 0.25;
        const s = 60 + Math.random() * 110;
        bursts.push({
          x: cx,
          y: cy,
          vx: Math.cos(a) * s,
          vy: Math.sin(a) * s - 18,
          life: 1.0,
          sz: 3.8 + Math.random() * 3.8,
        });
      }
    };

    const computeTargetCell = (gx, gy) => {
      const dist = Math.hypot(gx, gy);
      if (pet.inCenterZone) {
        if (dist < 70) return 4;
        pet.inCenterZone = false;
      } else if (dist < 50) {
        pet.inCenterZone = true;
        pet.compassSector = -1;
        return 4;
      }
      const angle = Math.atan2(gy, gx);
      if (
        pet.compassSector !== -1 &&
        Math.abs(wrapAngle(angle - pet.compassSector * SECTOR_STEP)) < SECTOR_STEP * 0.5 + HYSTERESIS
      ) {
        return SECTOR_TO_CELL[pet.compassSector];
      }
      pet.compassSector = (Math.round(angle / SECTOR_STEP) + 8) % 8;
      return SECTOR_TO_CELL[pet.compassSector];
    };

    const advanceTurntableCell = (targetCell, dt) => {
      if (pet.stepCooldown > 0) {
        pet.stepCooldown -= dt;
        return pet.currDirCell;
      }
      if (pet.currDirCell === targetCell) return pet.currDirCell;
      const cCol = pet.currDirCell % 3;
      const cRow = Math.floor(pet.currDirCell / 3);
      const tCol = targetCell % 3;
      const tRow = Math.floor(targetCell / 3);
      const nCol = cCol + Math.sign(tCol - cCol);
      const nRow = cRow + Math.sign(tRow - cRow);
      if (nCol !== cCol) pet.turnScaleX = 0.962;
      if (nRow !== cRow) pet.turnScaleY = 0.972;
      pet.currDirCell = nRow * 3 + nCol;
      pet.stepCooldown = 0.048;
      return pet.currDirCell;
    };

    const onPointerMove = (e) => {
      mouse.x = e.clientX;
      mouse.y = e.clientY;
      mouse.hasMoved = true;
      if (pet.dragging) {
        const nx = Math.max(90, Math.min(viewW - 90, e.clientX - pet.dragDX));
        const ny = Math.max(115, Math.min(viewH - 110, e.clientY - pet.dragDY));
        pet.vx = (nx - pet.x) * 12;
        pet.vy = (ny - pet.y) * 12;
        pet.x = nx;
        pet.y = ny;
      }
    };

    let downPos = null;
    const hitEl = hitBoxRef.current;

    const onHitDown = (e) => {
      e.stopPropagation();
      downPos = { x: e.clientX, y: e.clientY };
      pet.dragging = true;
      pet.dragDX = e.clientX - pet.x;
      pet.dragDY = e.clientY - pet.y;
    };

    const onPointerUp = (e) => {
      if (pet.dragging) {
        pet.dragging = false;
        pet.squashX = 1.07;
        pet.squashY = 0.94;
      }
      if (downPos && Math.hypot(e.clientX - downPos.x, e.clientY - downPos.y) < 10) {
        const now = performance.now();
        pet.boops = now - pet.boopAt < 450 ? pet.boops + 1 : 1;
        pet.boopAt = now;
        const payoffs = [1, 2, 3, 4, 5, 6, 8, 0];
        let pick;
        if (pet.boops >= 4) {
          pick = 7;
          pet.boops = 0;
        } else {
          pick = payoffs[pet.payoffStep % payoffs.length];
          pet.payoffStep += 1;
        }
        pet.reactIdx = pick;
        pet.reactTimer = 2.4;
        pet.squashX = 1.11;
        pet.squashY = 0.90;
        spawnBurst(pet.x, pet.y, 14);
        onBoopRef.current?.();
      }
      downPos = null;
    };

    window.addEventListener('pointermove', onPointerMove, { passive: true });
    window.addEventListener('pointerup', onPointerUp, { passive: true });
    if (hitEl) {
      hitEl.addEventListener('pointerdown', onHitDown);
    }

    let rafId = 0;
    let lastTime = performance.now();

    const tick = (now) => {
      const rawDt = (now - lastTime) / 1000;
      lastTime = now;
      const dt = Math.min(0.033, Math.max(0.001, rawDt));
      const t = now / 1000;
      const accent = art.accentColor || '#22d3ee';
      const accent2 = art.secondaryColor || '#fbbf24';

      if (!pet.dragging && !reducedMotion) {
        const minX = 120;
        const maxX = Math.max(260, viewW - 130);
        const minY = 145;
        const maxY = Math.max(260, viewH - 135);
        const activeMode = modeRef.current;

        if (pet.waypointX !== null) {
          const dx = pet.waypointX - pet.x;
          const dy = pet.waypointY - pet.y;
          const d = Math.hypot(dx, dy);
          if (d < 12 && Math.hypot(pet.vx, pet.vy) < 42) {
            pet.waypointX = null;
            pet.waypointY = null;
          } else {
            const omega = pet.dashTimer > 0 ? 5.6 : 3.8;
            pet.vx += (omega * omega * dx - 2 * omega * pet.vx) * dt;
            pet.vy += (omega * omega * dy - 2 * omega * pet.vy) * dt;
            const maxSpd = pet.dashTimer > 0 ? 400 : 240;
            const sp = Math.hypot(pet.vx, pet.vy);
            if (sp > maxSpd) {
              pet.vx = (pet.vx / sp) * maxSpd;
              pet.vy = (pet.vy / sp) * maxSpd;
            }
          }
        } else if (activeMode === 'follow') {
          const dx = mouse.x - pet.x;
          const dy = mouse.y - pet.y;
          const d = Math.hypot(dx, dy);
          let tx = pet.x;
          let ty = pet.y;
          if (d > 62) {
            tx = Math.max(minX, Math.min(maxX, mouse.x - (dx / d) * 58));
            ty = Math.max(minY, Math.min(maxY, mouse.y - (dy / d) * 58));
          }
          const omega = 4.2;
          pet.vx += (omega * omega * (tx - pet.x) - 2 * omega * pet.vx) * dt;
          pet.vy += (omega * omega * (ty - pet.y) - 2 * omega * pet.vy) * dt;
          const sp = Math.hypot(pet.vx, pet.vy);
          if (sp > 255) {
            pet.vx = (pet.vx / sp) * 255;
            pet.vy = (pet.vy / sp) * 255;
          }
        } else {
          pet.roamPhase += dt * 0.36;
          const cx = (minX + maxX) * 0.5;
          const cy = (minY + maxY) * 0.5;
          const rx = Math.max(80, (maxX - minX) * 0.40);
          const ry = Math.max(60, (maxY - minY) * 0.36);

          const orbX = cx + Math.sin(pet.roamPhase) * rx;
          const orbY = cy + Math.sin(pet.roamPhase * 1.5 + 0.4) * ry;
          const orbVx = Math.cos(pet.roamPhase) * rx * 0.36;
          const orbVy = Math.cos(pet.roamPhase * 1.5 + 0.4) * ry * (0.36 * 1.5);

          pet.vx += (orbVx + (orbX - pet.x) * 1.05 - pet.vx) * (1 - Math.exp(-2.4 * dt));
          pet.vy += (orbVy + (orbY - pet.y) * 1.05 - pet.vy) * (1 - Math.exp(-2.4 * dt));
        }

        pet.x += pet.vx * dt;
        pet.y += pet.vy * dt;
      }

      const prefs = skillPrefsRef.current;
      const availSkills = skillsRef.current;
      const skillsList = activeSkillsRef.current;
      if (
        prefs.autoCast &&
        !reducedMotion &&
        !document.hidden &&
        !pet.dragging &&
        availSkills.length > 0 &&
        skillsList.length === 0
      ) {
        autoCastRef.current.countdown -= dt;
        if (autoCastRef.current.countdown <= 0) {
          const picked = pickNextAutoSkill(
            availSkills,
            autoCastRef.current.lastSkillId,
            autoCastRef.current.sameCount,
          );
          if (picked) {
            autoCastRef.current.sameCount =
              picked.id === autoCastRef.current.lastSkillId ? autoCastRef.current.sameCount + 1 : 1;
            autoCastRef.current.lastSkillId = picked.id;
            autoCastRef.current.countdown = sampleAutoSkillDelay(prefs.intervalId);
            castSkillRef.current?.(picked.id);
          }
        }
      }

      let shakeX = 0;
      let shakeY = 0;
      for (let i = skillsList.length - 1; i >= 0; i -= 1) {
        const inst = skillsList[i];
        if (!stepSkillInstance(inst, dt, pet, viewW, viewH)) {
          skillsList.splice(i, 1);
          continue;
        }
        shakeX += inst.shakeX || 0;
        shakeY += inst.shakeY || 0;
      }
      if (!prefs.screenShake) {
        shakeX = 0;
        shakeY = 0;
      } else {
        shakeX = Math.max(-7, Math.min(7, shakeX));
        shakeY = Math.max(-7, Math.min(7, shakeY));
      }

      const speed = Math.hypot(pet.vx, pet.vy);
      if (pet.dashTimer > 0) pet.dashTimer -= dt;
      if (pet.parachuteTimer > 0) pet.parachuteTimer -= dt;
      if (pet.vx > 20) pet.facingRight = true;
      else if (pet.vx < -20) pet.facingRight = false;

      let gx = 0;
      let gy = 0;
      if (pet.waypointX !== null) {
        gx = pet.waypointX - pet.x;
        gy = pet.waypointY - pet.y;
      } else if (mouse.hasMoved) {
        gx = mouse.x - pet.x;
        gy = mouse.y - (pet.y - 20);
      } else {
        gx = pet.vx * 1.6;
        gy = pet.vy * 1.6;
      }

      const desiredDir = computeTargetCell(gx, gy);
      const activeDir = advanceTurntableCell(desiredDir, dt);

      let activeSrc = art.directions;
      let activeCell = activeDir;
      let activeMir = false;

      if (pet.reactTimer > 0) {
        pet.reactTimer -= dt;
        activeSrc = art.reactions;
        activeCell = pet.reactIdx;
      } else if (pet.dragging || pet.parachuteTimer > 0) {
        activeSrc = art.movement || art.directions;
        activeCell = art.movement ? 7 : 4;
      } else if (pet.dashTimer > 0 && art.movement) {
        activeSrc = art.movement;
        activeCell = 5;
        activeMir = !pet.facingRight;
      }

      const gMag = Math.max(1, Math.hypot(gx, gy));
      pet.tiltX += ((gx / gMag) * Math.min(1, gMag / 80) - pet.tiltX) * (1 - Math.exp(-8.0 * dt));
      pet.tiltY += ((gy / gMag) * Math.min(1, gMag / 80) - pet.tiltY) * (1 - Math.exp(-8.0 * dt));

      pet.squashX += (1.0 - pet.squashX) * (1 - Math.exp(-10.0 * dt));
      pet.squashY += (1.0 - pet.squashY) * (1 - Math.exp(-10.0 * dt));
      pet.turnScaleX += (1.0 - pet.turnScaleX) * (1 - Math.exp(-22.0 * dt));
      pet.turnScaleY += (1.0 - pet.turnScaleY) * (1 - Math.exp(-22.0 * dt));

      let targetBank = reducedMotion ? 0 : Math.max(-0.11, Math.min(0.11, pet.vx * 0.00038));
      if (pet.dashTimer > 0 && speed > 40) {
        const climbTilt = Math.atan2(pet.vy, Math.abs(pet.vx)) * (pet.facingRight ? 0.40 : -0.40);
        targetBank = Math.max(-0.46, Math.min(0.46, climbTilt));
      }
      pet.bankRad += (targetBank - pet.bankRad) * (1 - Math.exp(-7.0 * dt));

      const floatY = reducedMotion ? 0 : Math.sin(t * 2.5) * 5.2;
      const breathe = reducedMotion ? 1.0 : 1.0 + Math.sin(t * 3.2) * 0.012;

      if (!reducedMotion && speed > 45 && Math.random() < 0.38) {
        trails.push({
          x: pet.x - (pet.vx / speed) * 20 + (Math.random() - 0.5) * 13,
          y: pet.y + floatY - (pet.vy / speed) * 12 + (Math.random() - 0.5) * 13,
          vx: -pet.vx * 0.06 + (Math.random() - 0.5) * 10,
          vy: -pet.vy * 0.06 + (Math.random() - 0.5) * 10,
          life: 1.0,
          sz: 3.4 + Math.random() * 3.2,
        });
      }

      if (hitBoxRef.current) {
        hitBoxRef.current.style.transform = `translate3d(${Math.round(pet.x - 58)}px, ${Math.round(pet.y + floatY - 58)}px, 0)`;
        hitBoxRef.current.style.cursor = pet.dragging ? 'grabbing' : 'grab';
      }

      ctx.clearRect(0, 0, viewW, viewH);

      ctx.save();
      if (shakeX || shakeY) {
        ctx.translate(shakeX, shakeY);
      }

      // Flat calibrated shadow discs (zero CSS/Canvas gradients)
      const shScale = 1.0 - (floatY + 5.2) * 0.012;
      ctx.save();
      ctx.translate(pet.x, pet.y + 68);
      ctx.scale(shScale * 1.15, shScale * 0.30);
      ctx.fillStyle = 'rgba(2, 6, 23, 0.14)';
      ctx.beginPath();
      ctx.arc(0, 0, 38, 0, Math.PI * 2);
      ctx.fill();
      ctx.fillStyle = 'rgba(2, 6, 23, 0.18)';
      ctx.beginPath();
      ctx.arc(0, 0, 22, 0, Math.PI * 2);
      ctx.fill();
      ctx.restore();

      for (let i = 0; i < skillsList.length; i += 1) {
        drawSkillInstance(ctx, skillsList[i], 'back', pet, viewW, viewH);
      }

      // 3D Parallax Celestial Ring
      ctx.save();
      ctx.translate(pet.x - pet.tiltX * 7, pet.y + floatY * 0.7 - pet.tiltY * 6);
      ctx.rotate(pet.bankRad + pet.tiltX * 0.12);
      ctx.scale(1.0, Math.max(0.22, 0.38 + pet.tiltY * 0.12));
      ctx.strokeStyle = accent;
      ctx.globalAlpha = 0.28;
      ctx.lineWidth = 1.9;
      ctx.beginPath();
      ctx.arc(0, 16, 62, 0, Math.PI * 2);
      ctx.stroke();
      ctx.restore();

      for (let i = trails.length - 1; i >= 0; i -= 1) {
        const p = trails[i];
        p.x += p.vx * dt;
        p.y += p.vy * dt;
        p.life -= dt * 1.45;
        if (p.life <= 0) {
          trails.splice(i, 1);
          continue;
        }
        drawFourPointStar(ctx, p.x, p.y, p.sz * 1.15, i % 2 ? accent : accent2, p.life * 0.75);
      }

      if (!reducedMotion) {
        orbiters.forEach((ob, oi) => {
          const ag = t * ob.spd + ob.phase;
          const oz = Math.sin(ag) * ob.rz;
          if (oz > 0) return;
          const ox = Math.cos(ag) * ob.rx;
          const oy = ob.yo - pet.tiltY * 7 + Math.sin(ag * 2) * 5;
          const pr = 320 / (320 - oz);
          drawFourPointStar(
            ctx,
            pet.x + ox * pr,
            pet.y + floatY + oy * pr,
            (4.6 + (oi % 2) * 1.6) * pr,
            oi % 2 ? accent2 : accent,
            0.44,
          );
        });
      }

      // Direct 1:1 native-DPR 360x360 sprite draw (zero alpha-crossfade ghosting)
      const sheetImg = getCachedImage(activeSrc);
      if (sheetImg && sheetImg.complete && sheetImg.naturalWidth) {
        const cellPx = sheetImg.naturalWidth / 3;
        const sx0 = Math.round((activeCell % 3) * cellPx);
        const sy0 = Math.round(Math.floor(activeCell / 3) * cellPx);
        const drawSize = 164;

        ctx.save();
        ctx.translate(pet.x, pet.y + floatY);
        ctx.rotate(pet.bankRad);
        ctx.scale(
          (activeMir ? -1 : 1) * pet.squashX * pet.turnScaleX * breathe,
          pet.squashY * pet.turnScaleY * (2.0 - breathe),
        );
        ctx.globalAlpha = 1.0;
        ctx.drawImage(sheetImg, sx0, sy0, cellPx, cellPx, -drawSize * 0.5, -drawSize * 0.5, drawSize, drawSize);
        ctx.restore();
      }

      if (!reducedMotion) {
        orbiters.forEach((ob, oi) => {
          const ag = t * ob.spd + ob.phase;
          const oz = Math.sin(ag) * ob.rz;
          if (oz <= 0) return;
          const ox = Math.cos(ag) * ob.rx;
          const oy = ob.yo + pet.tiltY * 7 + Math.sin(ag * 2) * 5;
          const pr = 320 / (320 - oz);
          drawFourPointStar(
            ctx,
            pet.x + ox * pr,
            pet.y + floatY + oy * pr,
            (5.0 + (oi % 2) * 1.8) * pr,
            oi % 2 ? accent2 : accent,
            0.90,
          );
        });
      }

      for (let i = bursts.length - 1; i >= 0; i -= 1) {
        const b = bursts[i];
        b.x += b.vx * dt;
        b.y += b.vy * dt;
        b.vy += 165 * dt;
        b.life -= dt * 1.5;
        if (b.life <= 0) {
          bursts.splice(i, 1);
          continue;
        }
        drawFourPointStar(ctx, b.x, b.y, b.sz, i % 2 ? accent : accent2, b.life);
      }

      for (let i = 0; i < skillsList.length; i += 1) {
        drawSkillInstance(ctx, skillsList[i], 'front', pet, viewW, viewH);
      }

      ctx.restore();

      rafId = window.requestAnimationFrame(tick);
    };

    rafId = window.requestAnimationFrame(tick);

    return () => {
      window.cancelAnimationFrame(rafId);
      window.removeEventListener('resize', syncCanvasSize);
      window.removeEventListener('pointermove', onPointerMove);
      window.removeEventListener('pointerup', onPointerUp);
      if (hitEl) {
        hitEl.removeEventListener('pointerdown', onHitDown);
      }
    };
  }, [art, paused]);

  if (!art?.canMove || paused) return null;

  const triggerDash = () => {
    const pet = petRef.current;
    if (!pet) return;
    const vw = window.innerWidth;
    const vh = window.innerHeight;
    const goRight = pet.x < vw * 0.5;
    pet.facingRight = goRight;
    pet.waypointX = goRight ? Math.max(220, vw - 180) : 180;
    pet.waypointY = 175 + Math.random() * Math.max(120, vh * 0.42);
    pet.dashTimer = 0.9;
  };

  const activeInterval =
    AUTO_SKILL_INTERVALS.find((o) => o.id === skillPrefs.intervalId) || AUTO_SKILL_INTERVALS[1];

  return (
    <>
      <canvas
        ref={canvasRef}
        aria-hidden="true"
        className="pointer-events-none fixed inset-0 z-[45] h-screen w-screen"
      />

      <button
        ref={hitBoxRef}
        type="button"
        aria-label={`Interact with ${art.name}`}
        className="fixed left-0 top-0 z-[46] h-[116px] w-[116px] rounded-full bg-transparent focus:outline-none focus-visible:ring-2 focus-visible:ring-primary"
      />

      <div className="fixed bottom-4 left-4 z-[46] flex max-w-[calc(100vw-6rem)] flex-wrap items-center gap-1 rounded-2xl border border-border bg-surface px-2 py-1 shadow-stage-2 sm:rounded-full">
        <span className="px-2 text-xs font-bold text-foreground">
          {art.name}
        </span>
        <button
          type="button"
          onClick={() => setMode('roam')}
          title="360° Free Roam"
          className={`flex h-7 cursor-pointer items-center gap-1 rounded-full px-2.5 text-xs font-semibold transition-colors ${
            mode === 'roam'
              ? 'bg-primary text-on-primary'
              : 'bg-surface-secondary text-foreground-secondary hover:bg-surface-tertiary'
          }`}
        >
          <Compass className="h-3.5 w-3.5" />
          <span className="hidden sm:inline">Roam</span>
        </button>
        <button
          type="button"
          onClick={() => setMode('follow')}
          title="Follow Pointer"
          className={`flex h-7 cursor-pointer items-center gap-1 rounded-full px-2.5 text-xs font-semibold transition-colors ${
            mode === 'follow'
              ? 'bg-primary text-on-primary'
              : 'bg-surface-secondary text-foreground-secondary hover:bg-surface-tertiary'
          }`}
        >
          <Navigation className="h-3.5 w-3.5" />
          <span className="hidden sm:inline">Follow</span>
        </button>
        <button
          type="button"
          onClick={triggerDash}
          title="Supersonic Dash"
          className="flex h-7 cursor-pointer items-center gap-1 rounded-full bg-surface-secondary px-2.5 text-xs font-semibold text-foreground-secondary transition-colors hover:bg-surface-tertiary hover:text-foreground"
        >
          <Zap className="h-3.5 w-3.5" />
          <span className="hidden sm:inline">Dash</span>
        </button>
        {skills.length > 0 && (
          <>
            <span className="mx-0.5 h-4 w-px bg-border" aria-hidden="true" />
            {skills.map((skill) => {
              const SkillIcon = ICON_BY_KEY[skill.iconKey] || Sparkles;
              return (
                <button
                  key={skill.id}
                  type="button"
                  onClick={() => triggerSkill(skill.id)}
                  title={skill.label}
                  className={`flex h-7 cursor-pointer items-center gap-1 rounded-full border px-2.5 text-xs font-semibold transition-opacity hover:opacity-90 focus:outline-none focus-visible:ring-2 focus-visible:ring-primary ${
                    skill.isUltimate
                      ? 'border-warning-border bg-warning-bg text-warning-text'
                      : 'border-info-border bg-info-bg text-info-text'
                  }`}
                >
                  <SkillIcon className="h-3.5 w-3.5 shrink-0" />
                  <span>{skill.shortLabel}</span>
                </button>
              );
            })}
            <div ref={settingsWrapRef} className="relative">
              <button
                type="button"
                onClick={() => setSettingsOpen((prev) => !prev)}
                aria-expanded={settingsOpen}
                aria-haspopup="dialog"
                title="Skill Settings"
                className={`flex h-7 cursor-pointer items-center gap-1 rounded-full px-2.5 text-xs font-semibold transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-primary ${
                  skillPrefs.autoCast
                    ? 'border border-primary/40 bg-primary/10 text-primary hover:bg-primary/20'
                    : 'bg-surface-secondary text-foreground-secondary hover:bg-surface-tertiary hover:text-foreground'
                }`}
              >
                <Settings2 className="h-3.5 w-3.5 shrink-0" />
                <span>{skillPrefs.autoCast ? `Auto · ${activeInterval.label}` : 'Auto · Off'}</span>
              </button>

              {settingsOpen && (
                <div
                  role="dialog"
                  aria-label="Skill Settings"
                  className="absolute bottom-full left-0 mb-2 w-56 rounded-2xl border border-border bg-surface p-3 shadow-stage-2"
                >
                  <div className="flex items-center justify-between gap-2">
                    <span className="text-xs font-semibold text-foreground">Auto Skill</span>
                    <button
                      type="button"
                      role="switch"
                      aria-checked={skillPrefs.autoCast}
                      onClick={() => updatePrefs({ autoCast: !skillPrefs.autoCast })}
                      className={`relative flex h-6 w-11 shrink-0 cursor-pointer items-center rounded-full px-1 transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-primary ${
                        skillPrefs.autoCast ? 'bg-primary' : 'bg-surface-tertiary'
                      }`}
                    >
                      <span
                        className={`h-4 w-4 rounded-full bg-white transition-transform ${
                          skillPrefs.autoCast ? 'translate-x-5' : 'translate-x-0'
                        }`}
                      />
                    </button>
                  </div>

                  <div className="mt-2.5 flex items-center justify-between gap-2">
                    <span className="text-xs font-semibold text-foreground">Interval</span>
                    <div className="flex rounded-full bg-surface-secondary p-0.5">
                      {AUTO_SKILL_INTERVALS.map((opt) => {
                        const selected = skillPrefs.intervalId === opt.id;
                        return (
                          <button
                            key={opt.id}
                            type="button"
                            disabled={!skillPrefs.autoCast}
                            onClick={() => updatePrefs({ intervalId: opt.id })}
                            className={`cursor-pointer rounded-full px-2 py-0.5 text-[11px] font-semibold transition-colors disabled:cursor-not-allowed disabled:opacity-45 ${
                              selected
                                ? 'bg-primary text-on-primary'
                                : 'text-foreground-secondary hover:text-foreground'
                            }`}
                          >
                            {opt.label}
                          </button>
                        );
                      })}
                    </div>
                  </div>

                  <div className="mt-2.5 flex items-center justify-between gap-2">
                    <span className="text-xs font-semibold text-foreground">Screen Shake</span>
                    <button
                      type="button"
                      role="switch"
                      aria-checked={skillPrefs.screenShake}
                      onClick={() => updatePrefs({ screenShake: !skillPrefs.screenShake })}
                      className={`relative flex h-6 w-11 shrink-0 cursor-pointer items-center rounded-full px-1 transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-primary ${
                        skillPrefs.screenShake ? 'bg-primary' : 'bg-surface-tertiary'
                      }`}
                    >
                      <span
                        className={`h-4 w-4 rounded-full bg-white transition-transform ${
                          skillPrefs.screenShake ? 'translate-x-5' : 'translate-x-0'
                        }`}
                      />
                    </button>
                  </div>
                </div>
              )}
            </div>
          </>
        )}
        {onDockToHeader && (
          <>
            <span className="mx-0.5 h-4 w-px bg-border" aria-hidden="true" />
            <button
              type="button"
              onClick={onDockToHeader}
              title="Pin to Header"
              className="flex h-7 cursor-pointer items-center gap-1 rounded-full bg-surface-secondary px-2.5 text-xs font-semibold text-foreground-secondary transition-colors hover:bg-surface-tertiary hover:text-foreground"
            >
              <Pin className="h-3.5 w-3.5" />
              <span className="hidden sm:inline">Pin</span>
            </button>
          </>
        )}
      </div>
    </>
  );
}
