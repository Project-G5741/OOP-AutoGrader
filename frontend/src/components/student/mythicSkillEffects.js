/**
 * 60 FPS native-DPR 2D Canvas active skill VFX engine for Mythic 3D screen-pets.
 * Strict design compliance: zero CSS/Canvas gradients — luminous glow and depth
 * are built strictly with translucent solid polygons, ribbons, and crisp cores.
 */

export const MYTHIC_SKILLS = {
  kiem_de: [
    {
      id: 'van_kiem',
      label: 'Vạn Kiếm Quy Tông',
      shortLabel: 'Vạn Kiếm',
      iconKey: 'sword',
      duration: 3.2,
      reactCell: 8,
      isUltimate: false,
    },
    {
      id: 'nhat_kiem',
      label: 'Nhất Kiếm Bình Thiên Hạ',
      shortLabel: 'Nhất Kiếm',
      iconKey: 'swords',
      duration: 2.8,
      reactCell: 4,
      isUltimate: true,
    },
  ],
  cyber_striker: [
    {
      id: 'mua_ten_lua',
      label: 'Mưa Tên Lửa Từ Trường',
      shortLabel: 'Mưa Tên Lửa',
      iconKey: 'crosshair',
      duration: 3.0,
      reactCell: 1,
      isUltimate: false,
    },
    {
      id: 'phao_quy_dao',
      label: 'Pháo Quỹ Đạo Tối Thượng',
      shortLabel: 'Pháo Quỹ Đạo',
      iconKey: 'sparkles',
      duration: 2.8,
      reactCell: 8,
      isUltimate: true,
    },
  ],
  floral_fairy: [
    {
      id: 'bach_hoa',
      label: 'Bách Hoa Vũ Bão',
      shortLabel: 'Bách Hoa',
      iconKey: 'flower',
      duration: 3.0,
      reactCell: 2,
      isUltimate: false,
    },
    {
      id: 'linh_lan_thanh_vuc',
      label: 'Thánh Vực Linh Lan',
      shortLabel: 'Thánh Vực',
      iconKey: 'sparkles',
      duration: 3.0,
      reactCell: 8,
      isUltimate: true,
    },
  ],
  chrono_celestia: [
    {
      id: 'thoi_khong_ngung_dong',
      label: 'Thời Không Ngưng Đọng',
      shortLabel: 'Ngưng Đọng',
      iconKey: 'clock',
      duration: 3.0,
      reactCell: 1,
      isUltimate: false,
    },
    {
      id: 'thien_ha_tinh_vuc',
      label: 'Tinh Hà Sụp Đổ',
      shortLabel: 'Tinh Hà',
      iconKey: 'orbit',
      duration: 3.0,
      reactCell: 8,
      isUltimate: true,
    },
  ],
};

const SKILL_LOOKUP = Object.values(MYTHIC_SKILLS)
  .flat()
  .reduce((acc, skill) => {
    acc[skill.id] = skill;
    return acc;
  }, {});

export const MYTHIC_SKILL_PREF_KEY = 'oop_mythic_skill_prefs';

export const AUTO_SKILL_INTERVALS = [
  { id: 'fast', label: '8s', minSec: 6, maxSec: 10 },
  { id: 'normal', label: '15s', minSec: 12, maxSec: 18 },
  { id: 'relaxed', label: '30s', minSec: 24, maxSec: 36 },
];

export const DEFAULT_MYTHIC_SKILL_PREFS = {
  autoCast: true,
  intervalId: 'normal',
  screenShake: true,
};

export function normalizeMythicSkillPrefs(raw) {
  const intervalId = AUTO_SKILL_INTERVALS.some((o) => o.id === raw?.intervalId)
    ? raw.intervalId
    : DEFAULT_MYTHIC_SKILL_PREFS.intervalId;
  return {
    autoCast: typeof raw?.autoCast === 'boolean' ? raw.autoCast : DEFAULT_MYTHIC_SKILL_PREFS.autoCast,
    intervalId,
    screenShake: typeof raw?.screenShake === 'boolean' ? raw.screenShake : DEFAULT_MYTHIC_SKILL_PREFS.screenShake,
  };
}

export function readMythicSkillPrefs(storage = globalThis?.localStorage) {
  try {
    if (!storage || typeof storage.getItem !== 'function') {
      return { ...DEFAULT_MYTHIC_SKILL_PREFS };
    }
    const raw = storage.getItem(MYTHIC_SKILL_PREF_KEY);
    if (!raw) return { ...DEFAULT_MYTHIC_SKILL_PREFS };
    return normalizeMythicSkillPrefs(JSON.parse(raw));
  } catch {
    return { ...DEFAULT_MYTHIC_SKILL_PREFS };
  }
}

export function writeMythicSkillPrefs(prefs, storage = globalThis?.localStorage) {
  const normalized = normalizeMythicSkillPrefs(prefs);
  try {
    if (storage && typeof storage.setItem === 'function') {
      storage.setItem(MYTHIC_SKILL_PREF_KEY, JSON.stringify(normalized));
    }
  } catch {
    /* ignore quota or private mode storage failures */
  }
  return normalized;
}

export function sampleAutoSkillDelay(intervalId, rng = Math.random) {
  const entry = AUTO_SKILL_INTERVALS.find((o) => o.id === intervalId) || AUTO_SKILL_INTERVALS[1];
  const r = clamp01(typeof rng === 'function' ? rng() : Math.random());
  return entry.minSec + r * (entry.maxSec - entry.minSec);
}

export function pickNextAutoSkill(skills, lastSkillId = null, sameCount = 0, rng = Math.random) {
  if (!Array.isArray(skills) || skills.length === 0) return null;
  if (skills.length === 1) return skills[0];
  if (lastSkillId && sameCount >= 2) {
    return skills.find((s) => s.id !== lastSkillId) || skills[0];
  }
  const r = clamp01(typeof rng === 'function' ? rng() : Math.random());
  return r < 0.58 ? skills[0] : skills[1];
}

function clamp(v, lo, hi) {
  if (!Number.isFinite(v)) return lo;
  if (v < lo) return lo;
  if (v > hi) return hi;
  return v;
}

function clamp01(v) {
  return clamp(v, 0, 1);
}

function easeOutCubic(t) {
  const u = 1 - clamp01(t);
  return 1 - u * u * u;
}

function easeInOutQuad(t) {
  const x = clamp01(t);
  return x < 0.5 ? 2 * x * x : 1 - ((-2 * x + 2) ** 2) / 2;
}

function cubicBezier(p0, p1, p2, p3, t) {
  const u = 1 - t;
  return u * u * u * p0 + 3 * u * u * t * p1 + 3 * u * t * t * p2 + t * t * t * p3;
}

function hash01(seed) {
  const s = Math.sin(seed * 127.1 + 311.7) * 43758.5453123;
  return s - Math.floor(s);
}

function createRng(seedInput) {
  let state = Number.isFinite(seedInput)
    ? (Math.floor(seedInput) >>> 0) || 1
    : (((Math.random() * 0xffffffff) ^ ((Math.random() * 0xffffffff) << 1)) >>> 0) || 1;
  return () => {
    state = (state + 0x6d2b79f5) >>> 0;
    let t = Math.imul(state ^ (state >>> 15), 1 | state);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

function randRange(rng, lo, hi) {
  return lo + rng() * (hi - lo);
}

export function pickRandomViewportTarget(ox, oy, viewW, viewH, rng = Math.random) {
  const vw = Math.max(320, Number(viewW) || 1280);
  const vh = Math.max(320, Number(viewH) || 720);
  const padX = Math.min(150, vw * 0.14);
  const padTop = Math.min(125, vh * 0.16);
  const padBot = Math.min(135, vh * 0.18);
  const minX = padX;
  const maxX = Math.max(minX + 40, vw - padX);
  const minY = padTop;
  const maxY = Math.max(minY + 40, vh - padBot);
  const minDist = Math.min(200, vw * 0.16);
  const dockGuardX = Math.min(540, vw * 0.56);
  const dockGuardY = vh - 115;

  let best = null;
  let bestDist = -1;

  for (let i = 0; i < 8; i += 1) {
    const cx = clamp(minX + rng() * (maxX - minX), minX, maxX);
    const cy = clamp(minY + rng() * (maxY - minY), minY, maxY);
    const inDockZone = cx < dockGuardX && cy > dockGuardY;
    const dist = Math.hypot(cx - ox, cy - oy);
    if (!inDockZone && dist >= minDist) {
      return { x: cx, y: cy };
    }
    const score = inDockZone ? dist * 0.35 : dist;
    if (score > bestDist) {
      bestDist = score;
      best = { x: cx, y: cy };
    }
  }

  return best || {
    x: clamp(vw * 0.5, minX, maxX),
    y: clamp(vh * 0.45, minY, maxY),
  };
}

export function getMythicSkills(mascotId) {
  if (!mascotId) return [];
  const key = String(mascotId).trim().toLowerCase().replace(/-/g, '_');
  return MYTHIC_SKILLS[key] || [];
}

/* ============================================================================
 * Crisp Gradient-Free Vector Primitives
 * ========================================================================== */

function drawFourPointStar(ctx, x, y, r, color, alpha) {
  const a = clamp01(alpha);
  if (a <= 0.01 || r <= 0.2) return;
  ctx.save();
  ctx.globalAlpha = a;
  ctx.fillStyle = color;
  ctx.beginPath();
  for (let i = 0; i < 8; i += 1) {
    const rad = i % 2 === 0 ? r : r * 0.34;
    const ang = (i * Math.PI) / 4;
    const px = x + Math.cos(ang) * rad;
    const py = y + Math.sin(ang) * rad;
    if (i === 0) ctx.moveTo(px, py);
    else ctx.lineTo(px, py);
  }
  ctx.closePath();
  ctx.fill();
  ctx.restore();
}

function drawPolygonRing(ctx, cx, cy, radius, sides, rotRad, color, lineWidth, alpha, fillAlpha = 0) {
  const a = clamp01(alpha);
  if (a <= 0.01 || radius <= 0.5) return;
  ctx.save();
  ctx.translate(cx, cy);
  ctx.rotate(rotRad);
  ctx.beginPath();
  for (let i = 0; i < sides; i += 1) {
    const ang = (Math.PI * 2 * i) / sides;
    const px = Math.cos(ang) * radius;
    const py = Math.sin(ang) * radius;
    if (i === 0) ctx.moveTo(px, py);
    else ctx.lineTo(px, py);
  }
  ctx.closePath();
  if (fillAlpha > 0) {
    ctx.globalAlpha = clamp01(fillAlpha);
    ctx.fillStyle = color;
    ctx.fill();
  }
  ctx.globalAlpha = a;
  ctx.strokeStyle = color;
  ctx.lineWidth = lineWidth;
  ctx.stroke();
  ctx.restore();
}

function drawSpiritSword(ctx, x, y, angle, scale, bladeColor, auraColor, guardColor, alpha) {
  const a = clamp01(alpha);
  if (a <= 0.01 || scale <= 0.05) return;
  ctx.save();
  ctx.translate(x, y);
  ctx.rotate(angle);
  ctx.scale(scale, scale);

  // Outer qi aura diamond
  ctx.globalAlpha = a * 0.34;
  ctx.fillStyle = auraColor;
  ctx.beginPath();
  ctx.moveTo(40, 0);
  ctx.lineTo(-4, -9.5);
  ctx.lineTo(-22, 0);
  ctx.lineTo(-4, 9.5);
  ctx.closePath();
  ctx.fill();

  // Blade body
  ctx.globalAlpha = a;
  ctx.fillStyle = bladeColor;
  ctx.beginPath();
  ctx.moveTo(35, 0);
  ctx.lineTo(-6, -4.8);
  ctx.lineTo(-10, 0);
  ctx.lineTo(-6, 4.8);
  ctx.closePath();
  ctx.fill();

  // Bright central spine fuller
  ctx.globalAlpha = a * 0.96;
  ctx.strokeStyle = '#ffffff';
  ctx.lineWidth = 1.6;
  ctx.beginPath();
  ctx.moveTo(31, 0);
  ctx.lineTo(-7, 0);
  ctx.stroke();

  // Winged crossguard
  ctx.fillStyle = guardColor;
  ctx.beginPath();
  ctx.moveTo(-5, -10.5);
  ctx.lineTo(-2, 0);
  ctx.lineTo(-5, 10.5);
  ctx.lineTo(-9.5, 4);
  ctx.lineTo(-9.5, -4);
  ctx.closePath();
  ctx.fill();

  // Hilt & pommel gem
  ctx.fillStyle = auraColor;
  ctx.fillRect(-19, -1.7, 10.5, 3.4);
  ctx.fillStyle = bladeColor;
  ctx.beginPath();
  ctx.moveTo(-19, 0);
  ctx.lineTo(-22, -3.2);
  ctx.lineTo(-25, 0);
  ctx.lineTo(-22, 3.2);
  ctx.closePath();
  ctx.fill();

  ctx.restore();
}

function drawFormationRing3D(ctx, cx, cy, rx, ry, tiltRad, spinRad, points, primaryColor, secondaryColor, alpha) {
  const a = clamp01(alpha);
  if (a <= 0.01 || rx <= 1 || ry <= 1) return;
  const squashY = ry / rx;

  ctx.save();
  ctx.translate(cx, cy);
  ctx.rotate(tiltRad);
  ctx.scale(1, squashY);

  // Outer aura ring
  ctx.globalAlpha = a * 0.35;
  ctx.strokeStyle = secondaryColor;
  ctx.lineWidth = 5;
  ctx.beginPath();
  ctx.arc(0, 0, rx, 0, Math.PI * 2);
  ctx.stroke();

  // Crisp double rims
  ctx.globalAlpha = a;
  ctx.strokeStyle = primaryColor;
  ctx.lineWidth = 2.2;
  ctx.beginPath();
  ctx.arc(0, 0, rx, 0, Math.PI * 2);
  ctx.stroke();

  ctx.globalAlpha = a * 0.72;
  ctx.strokeStyle = secondaryColor;
  ctx.lineWidth = 1.4;
  ctx.beginPath();
  ctx.arc(0, 0, rx * 0.84, 0, Math.PI * 2);
  ctx.stroke();

  // Inscribed star polygon
  ctx.save();
  ctx.rotate(spinRad);
  const step = Math.max(2, Math.floor(points / 2) - 1);
  ctx.globalAlpha = a * 0.85;
  ctx.strokeStyle = primaryColor;
  ctx.lineWidth = 1.6;
  ctx.beginPath();
  for (let i = 0; i < points; i += 1) {
    const a1 = (Math.PI * 2 * i) / points;
    const a2 = (Math.PI * 2 * ((i + step) % points)) / points;
    ctx.moveTo(Math.cos(a1) * rx * 0.84, Math.sin(a1) * rx * 0.84);
    ctx.lineTo(Math.cos(a2) * rx * 0.84, Math.sin(a2) * rx * 0.84);
  }
  ctx.stroke();

  // Perimeter rune diamonds
  for (let i = 0; i < points; i += 1) {
    const ang = (Math.PI * 2 * i) / points;
    const px = Math.cos(ang) * rx * 0.92;
    const py = Math.sin(ang) * rx * 0.92;
    ctx.fillStyle = i % 2 === 0 ? primaryColor : '#ffffff';
    ctx.globalAlpha = a * 0.9;
    ctx.beginPath();
    ctx.moveTo(px + Math.cos(ang) * 4, py + Math.sin(ang) * 4);
    ctx.lineTo(px + Math.cos(ang + Math.PI / 2) * 3, py + Math.sin(ang + Math.PI / 2) * 3);
    ctx.lineTo(px - Math.cos(ang) * 4, py - Math.sin(ang) * 4);
    ctx.lineTo(px - Math.cos(ang + Math.PI / 2) * 3, py - Math.sin(ang + Math.PI / 2) * 3);
    ctx.closePath();
    ctx.fill();
  }
  ctx.restore();

  ctx.restore();
}

function drawTaperedTrail(ctx, pts, maxWidth, outerColor, coreColor, alpha) {
  if (!pts || pts.length < 2) return;
  const a = clamp01(alpha);
  if (a <= 0.01) return;

  const left = [];
  const right = [];
  for (let i = 0; i < pts.length; i += 1) {
    const p = pts[i];
    const prev = pts[Math.max(0, i - 1)];
    const next = pts[Math.min(pts.length - 1, i + 1)];
    const tx = next.x - prev.x;
    const ty = next.y - prev.y;
    const len = Math.max(0.001, Math.hypot(tx, ty));
    const nx = -ty / len;
    const ny = tx / len;
    const w = maxWidth * 0.5 * ((i + 1) / pts.length);
    left.push({ x: p.x + nx * w, y: p.y + ny * w });
    right.push({ x: p.x - nx * w, y: p.y - ny * w });
  }

  ctx.save();
  ctx.globalAlpha = a * 0.55;
  ctx.fillStyle = outerColor;
  ctx.beginPath();
  ctx.moveTo(left[0].x, left[0].y);
  for (let i = 1; i < left.length; i += 1) ctx.lineTo(left[i].x, left[i].y);
  for (let i = right.length - 1; i >= 0; i -= 1) ctx.lineTo(right[i].x, right[i].y);
  ctx.closePath();
  ctx.fill();

  ctx.globalAlpha = a * 0.92;
  ctx.strokeStyle = coreColor;
  ctx.lineWidth = Math.max(1.2, maxWidth * 0.28);
  ctx.lineCap = 'round';
  ctx.lineJoin = 'round';
  ctx.beginPath();
  ctx.moveTo(pts[0].x, pts[0].y);
  for (let i = 1; i < pts.length; i += 1) ctx.lineTo(pts[i].x, pts[i].y);
  ctx.stroke();
  ctx.restore();
}

function drawBlossomPetal(ctx, x, y, rot, size, petalColor, coreColor, alpha) {
  const a = clamp01(alpha);
  if (a <= 0.01 || size <= 0.4) return;
  ctx.save();
  ctx.translate(x, y);
  ctx.rotate(rot);
  ctx.globalAlpha = a;
  ctx.fillStyle = petalColor;
  ctx.beginPath();
  ctx.moveTo(0, -size * 0.2);
  ctx.bezierCurveTo(size * 0.85, -size * 0.9, size * 0.9, size * 0.35, size * 0.18, size * 0.85);
  ctx.lineTo(0, size * 0.58);
  ctx.lineTo(-size * 0.18, size * 0.85);
  ctx.bezierCurveTo(-size * 0.9, size * 0.35, -size * 0.85, -size * 0.9, 0, -size * 0.2);
  ctx.closePath();
  ctx.fill();

  ctx.globalAlpha = a * 0.72;
  ctx.strokeStyle = coreColor;
  ctx.lineWidth = Math.max(0.8, size * 0.12);
  ctx.beginPath();
  ctx.moveTo(0, 0);
  ctx.lineTo(0, size * 0.48);
  ctx.stroke();
  ctx.restore();
}

function drawGearWheel(ctx, cx, cy, radius, teeth, rotRad, color, alpha) {
  const a = clamp01(alpha);
  if (a <= 0.01) return;
  const inner = radius * 0.82;
  const hub = radius * 0.44;
  ctx.save();
  ctx.translate(cx, cy);
  ctx.rotate(rotRad);
  ctx.globalAlpha = a;
  ctx.strokeStyle = color;
  ctx.lineWidth = 2.2;
  ctx.beginPath();
  for (let i = 0; i < teeth; i += 1) {
    const a0 = (Math.PI * 2 * i) / teeth;
    const a1 = a0 + (Math.PI * 2 * 0.24) / teeth;
    const a2 = a0 + (Math.PI * 2 * 0.52) / teeth;
    const a3 = a0 + (Math.PI * 2 * 0.76) / teeth;
    if (i === 0) ctx.moveTo(Math.cos(a0) * inner, Math.sin(a0) * inner);
    else ctx.lineTo(Math.cos(a0) * inner, Math.sin(a0) * inner);
    ctx.lineTo(Math.cos(a1) * radius, Math.sin(a1) * radius);
    ctx.lineTo(Math.cos(a2) * radius, Math.sin(a2) * radius);
    ctx.lineTo(Math.cos(a3) * inner, Math.sin(a3) * inner);
  }
  ctx.closePath();
  ctx.stroke();

  ctx.beginPath();
  ctx.arc(0, 0, hub, 0, Math.PI * 2);
  ctx.stroke();

  for (let s = 0; s < 4; s += 1) {
    const sa = (Math.PI * s) / 2;
    ctx.beginPath();
    ctx.moveTo(Math.cos(sa) * hub, Math.sin(sa) * hub);
    ctx.lineTo(Math.cos(sa) * inner, Math.sin(sa) * inner);
    ctx.stroke();
  }
  ctx.restore();
}

/* ============================================================================
 * Instance Creation
 * ========================================================================== */

export function createSkillInstance(skillId, opts = {}) {
  const meta = SKILL_LOOKUP[skillId] || SKILL_LOOKUP.van_kiem;
  const vw = Math.max(320, Number(opts.viewW) || 1280);
  const vh = Math.max(320, Number(opts.viewH) || 720);
  const ox = clamp(Number(opts.originX) || vw * 0.35, 90, vw - 90);
  const oy = clamp(Number(opts.originY) || vh * 0.45, 110, vh - 110);
  const reducedMotion = Boolean(opts.reducedMotion);
  const rng = createRng(opts.seed);

  const hasExplicitTarget = Number.isFinite(opts.targetX) && Number.isFinite(opts.targetY);
  const picked = hasExplicitTarget
    ? { x: Number(opts.targetX), y: Number(opts.targetY) }
    : pickRandomViewportTarget(ox, oy, vw, vh, rng);
  const tx = clamp(picked.x, 120, Math.max(160, vw - 120));
  const ty = clamp(picked.y, 120, Math.max(150, vh - 130));

  const inst = {
    skillId: meta.id,
    elapsed: 0,
    duration: meta.duration,
    ox,
    oy,
    tx,
    ty,
    viewW: vw,
    viewH: vh,
    shakeX: 0,
    shakeY: 0,
    reducedMotion,
    choresTriggered: false,
    cleaveDashStarted: false,
  };

  if (meta.id === 'van_kiem') {
    const clusters = [{ x: tx, y: ty }];
    if (rng() < 0.5) {
      clusters.push(pickRandomViewportTarget(tx, ty, vw, vh, rng));
    }
    inst.clusters = clusters;
    inst.haloTilt = randRange(rng, -0.4, 0.4);
    inst.haloScale = randRange(rng, 0.86, 1.16);
    inst.finisherTilt = randRange(rng, -0.22, 0.22);
    inst.swords = Array.from({ length: 36 }, (_, i) => {
      const ring = i < 16 ? 0 : 1;
      const idxInRing = ring === 0 ? i : i - 16;
      const ringTotal = ring === 0 ? 16 : 20;
      const haloAng =
        -Math.PI * 0.92 +
        (idxInRing / (ringTotal - 1)) * Math.PI * 1.84 +
        randRange(rng, -0.09, 0.09);
      const haloR = (ring === 0 ? 78 : 112) * inst.haloScale + randRange(rng, -8, 10);
      const targetCluster = clusters[i % clusters.length];
      const spreadX = randRange(rng, -115, 115);
      const spreadY = randRange(rng, -65, 65);
      const arcBias = randRange(rng, -160, 160);
      return {
        i,
        haloAng,
        haloR,
        launchAt: 0.62 + i * 0.042,
        flightDur: randRange(rng, 0.32, 0.5),
        endX: clamp(targetCluster.x + spreadX, 90, vw - 90),
        endY: clamp(targetCluster.y + spreadY, 120, vh - 100),
        arcBias,
        x: ox,
        y: oy,
        ang: 0,
        trail: [],
        impactTriggered: false,
      };
    });
    inst.impacts = [];
  } else if (meta.id === 'nhat_kiem') {
    inst.slashCx = hasExplicitTarget ? tx : randRange(rng, vw * 0.22, vw * 0.78);
    inst.slashCy = hasExplicitTarget ? ty : randRange(rng, vh * 0.22, vh * 0.72);
    inst.slashAngle = randRange(rng, -Math.PI * 0.85, Math.PI * 0.85);
    inst.curveDrop = (rng() < 0.5 ? -1 : 1) * randRange(rng, 45, 92);
    inst.qiLines = Array.from({ length: 28 }, (_, i) => ({
      ang: (Math.PI * 2 * i) / 28 + randRange(rng, -0.14, 0.14),
      dist: randRange(rng, 165, 360),
      len: randRange(rng, 26, 62),
      gold: i % 2 === 0,
    }));
    inst.cracks = Array.from({ length: 14 }, (_, i) => {
      const frac = 0.08 + (i / 13) * 0.84;
      const side = rng() < 0.5 ? 1 : -1;
      const segs = Array.from({ length: 4 }, (__, k) => ({
        along: (k + 1) * randRange(rng, 22, 52),
        perp: side * ((k + 1) * 16 + randRange(rng, -10, 20)),
      }));
      return { frac, side, segs };
    });
    inst.shards = Array.from({ length: 48 }, (_, i) => ({
      frac: rng(),
      vx: randRange(rng, -125, 125),
      vy: randRange(rng, -125, 125),
      rot: rng() * Math.PI * 2,
      vRot: randRange(rng, -4.5, 4.5),
      sz: randRange(rng, 4, 13),
      gold: i % 3 !== 0,
    }));
  } else if (meta.id === 'mua_ten_lua') {
    inst.lockTargets = [
      { x: tx, y: ty },
      pickRandomViewportTarget(tx, ty, vw, vh, rng),
      pickRandomViewportTarget(ox, oy, vw, vh, rng),
    ];
    inst.missiles = Array.from({ length: 28 }, (_, i) => {
      const tgt = inst.lockTargets[i % inst.lockTargets.length];
      const side = i % 2 === 0 ? -1 : 1;
      const arcH = randRange(rng, 110, 290);
      const sideSpread = side * randRange(rng, 85, 260);
      return {
        launchAt: 0.22 + i * 0.055,
        flightDur: randRange(rng, 0.5, 0.72),
        c1x: ox + sideSpread * 0.55,
        c1y: oy - arcH,
        c2x: tgt.x + sideSpread,
        c2y: tgt.y - arcH * 0.45,
        endX: clamp(tgt.x + randRange(rng, -85, 85), 90, vw - 90),
        endY: clamp(tgt.y + randRange(rng, -65, 65), 120, vh - 100),
        x: ox,
        y: oy,
        ang: 0,
        trail: [],
        detonated: false,
      };
    });
    inst.detonations = [];
  } else if (meta.id === 'phao_quy_dao') {
    inst.satX = clamp(tx + randRange(rng, -240, 240), 120, Math.max(120, vw - 120));
    inst.satY = randRange(rng, 52, 125);
    inst.helicalRings = Array.from({ length: 8 }, (_, i) => ({
      offset: i / 8,
      orange: i % 2 === 0,
    }));
    inst.arcs = Array.from({ length: 12 }, (_, i) => ({
      ang: (Math.PI * 2 * i) / 12 + randRange(rng, -0.15, 0.15),
      seed: i * 17.3 + rng() * 100,
    }));
  } else if (meta.id === 'bach_hoa') {
    inst.swirlSign = rng() < 0.5 ? -1 : 1;
    inst.windRot = randRange(rng, -0.8, 0.8);
    inst.petals = Array.from({ length: 54 }, (_, i) => {
      const palette = ['#f472b6', '#fbcfe8', '#fef08a', '#34d399'];
      return {
        spawnAt: rng() * 0.65,
        orbAng: rng() * Math.PI * 2,
        orbSpeed: inst.swirlSign * randRange(rng, 3.2, 5.8),
        radMin: randRange(rng, 40, 105),
        radMax: randRange(rng, 160, 480),
        driftX: (tx - ox) * randRange(rng, 0.4, 1.3),
        driftY: (ty - oy) * randRange(rng, 0.35, 1.15) + randRange(rng, -110, 110),
        rot: rng() * Math.PI * 2,
        spin: randRange(rng, -4.5, 4.5),
        size: randRange(rng, 6.5, 14),
        color: palette[i % palette.length],
        isStar: i % 5 === 0,
      };
    });
  } else if (meta.id === 'linh_lan_thanh_vuc') {
    inst.baseRot = rng() * Math.PI * 2;
    inst.tiltRad = randRange(rng, -0.22, 0.22);
    inst.lanterns = Array.from({ length: 8 }, (_, i) => ({
      ang: inst.baseRot + (Math.PI * 2 * i) / 8,
      radius: randRange(rng, 126, 152),
      delay: i * 0.06,
    }));
    inst.motes = Array.from({ length: 40 }, (_, i) => ({
      xOff: randRange(rng, -165, 165),
      yOff: randRange(rng, -45, 45),
      speed: randRange(rng, 55, 140),
      size: randRange(rng, 2.8, 7.0),
      gold: i % 3 === 0,
      emerald: i % 3 === 1,
    }));
  } else if (meta.id === 'thoi_khong_ngung_dong') {
    inst.handOffset = rng() * Math.PI * 2;
    inst.gearOrbit = rng() * Math.PI * 2;
    inst.crystals = Array.from({ length: 32 }, (_, i) => ({
      ang: (Math.PI * 2 * i) / 32 + randRange(rng, -0.08, 0.08),
      dist: randRange(rng, 80, 340),
      size: randRange(rng, 5, 12),
      rot: rng() * Math.PI,
      purple: i % 2 === 0,
    }));
  } else if (meta.id === 'thien_ha_tinh_vuc') {
    inst.ringTilts = [
      randRange(rng, -0.85, 0.85),
      randRange(rng, -0.85, 0.85),
      randRange(rng, -0.85, 0.85),
    ];
    inst.stars = Array.from({ length: 44 }, (_, i) => ({
      ang: (Math.PI * 2 * i) / 44 + randRange(rng, -0.18, 0.18),
      startDist: randRange(rng, 160, 400),
      speed: randRange(rng, 2.2, 4.0),
      size: randRange(rng, 3.4, 8.0),
      burstAng: rng() * Math.PI * 2,
      burstSpd: randRange(rng, 140, 360),
      tint: i % 3 === 0 ? '#c084fc' : i % 3 === 1 ? '#fbbf24' : '#38bdf8',
    }));
  }

  return inst;
}

/* ============================================================================
 * Per-Frame Simulation Step
 * ========================================================================== */

export function stepSkillInstance(inst, dt, pet, viewW, viewH) {
  if (!inst) return false;
  const safeDt = Math.min(0.05, Math.max(0.001, Number(dt) || 0.016));
  inst.elapsed += safeDt;
  const vw = Math.max(320, Number(viewW) || inst.viewW || 1280);
  const vh = Math.max(320, Number(viewH) || inst.viewH || 720);
  inst.viewW = vw;
  inst.viewH = vh;

  const t = inst.elapsed;
  const meta = SKILL_LOOKUP[inst.skillId];

  // Trigger mascot cast expression once at launch
  if (pet && !inst.choresTriggered) {
    inst.choresTriggered = true;
    pet.reactIdx = meta?.reactCell ?? 8;
    pet.reactTimer = Math.min(1.4, inst.duration * 0.5);
  }

  let shakeAmp = 0;

  if (inst.skillId === 'van_kiem') {
    const px = pet ? pet.x : inst.ox;
    const py = pet ? pet.y : inst.oy;

    for (const sw of inst.swords) {
      if (t < sw.launchAt) {
        const appear = clamp01((t - sw.i * 0.016) / 0.24);
        const hx = px + Math.cos(sw.haloAng) * sw.haloR * easeOutCubic(appear);
        const hy = py - 14 + Math.sin(sw.haloAng) * (sw.haloR * 0.65) * easeOutCubic(appear);
        sw.x = hx;
        sw.y = hy;
        sw.ang = Math.atan2(sw.endY - hy, sw.endX - hx);
      } else {
        const u = clamp01((t - sw.launchAt) / sw.flightDur);
        const sx = px + Math.cos(sw.haloAng) * sw.haloR;
        const sy = py - 14 + Math.sin(sw.haloAng) * (sw.haloR * 0.65);
        const mx = (sx + sw.endX) * 0.5;
        const my = (sy + sw.endY) * 0.5 + sw.arcBias;
        const inv = 1 - u;
        const nx = inv * inv * sx + 2 * inv * u * mx + u * u * sw.endX;
        const ny = inv * inv * sy + 2 * inv * u * my + u * u * sw.endY;
        if (u < 1) {
          sw.ang = Math.atan2(ny - sw.y, nx - sw.x) || sw.ang;
          sw.x = nx;
          sw.y = ny;
          sw.trail.push({ x: nx, y: ny });
          if (sw.trail.length > 10) sw.trail.shift();
        } else {
          sw.x = sw.endX;
          sw.y = sw.endY;
          if (!sw.impactTriggered) {
            sw.impactTriggered = true;
            inst.impacts.push({ x: sw.endX, y: sw.endY, at: t });
          }
        }
      }
    }

    if (t >= 2.25 && t <= 2.65) {
      shakeAmp = (1 - (t - 2.25) / 0.4) * 5.2;
    } else if (t >= 0.7 && t <= 2.15) {
      shakeAmp = 1.6;
    }
  } else if (inst.skillId === 'nhat_kiem') {
    // Gentle dash glide along slash axis during 0.72..1.15s
    if (pet && t >= 0.72 && t <= 1.15) {
      if (!inst.cleaveDashStarted) {
        inst.cleaveDashStarted = true;
        pet.dashTimer = 0.45;
      }
      const dashTargetX = clamp(inst.slashCx + Math.cos(inst.slashAngle) * 110, 120, vw - 120);
      const dashTargetY = clamp(inst.slashCy + Math.sin(inst.slashAngle) * 85, 135, vh - 135);
      const lerpFactor = 1 - Math.exp(-8 * safeDt);
      pet.x += (dashTargetX - pet.x) * lerpFactor;
      pet.y += (dashTargetY - pet.y) * lerpFactor;
    }
    if (t >= 0.72 && t <= 1.25) {
      shakeAmp = (1 - (t - 0.72) / 0.53) * 6.5;
    }
  } else if (inst.skillId === 'mua_ten_lua') {
    const px = pet ? pet.x : inst.ox;
    const py = pet ? pet.y - 10 : inst.oy - 10;

    for (const ms of inst.missiles) {
      if (t < ms.launchAt) {
        ms.x = px;
        ms.y = py;
      } else {
        const u = clamp01((t - ms.launchAt) / ms.flightDur);
        const nx = cubicBezier(px, ms.c1x, ms.c2x, ms.endX, u);
        const ny = cubicBezier(py, ms.c1y, ms.c2y, ms.endY, u);
        if (u < 1) {
          ms.ang = Math.atan2(ny - ms.y, nx - ms.x) || ms.ang;
          ms.x = nx;
          ms.y = ny;
          ms.trail.push({ x: nx, y: ny });
          if (ms.trail.length > 12) ms.trail.shift();
        } else if (!ms.detonated) {
          ms.detonated = true;
          inst.detonations.push({ x: ms.endX, y: ms.endY, at: t });
        }
      }
    }

    if (t >= 2.05 && t <= 2.55) {
      shakeAmp = (1 - (t - 2.05) / 0.5) * 4.8;
    } else if (t >= 0.85 && t <= 2.0) {
      shakeAmp = 1.8;
    }
  } else if (inst.skillId === 'phao_quy_dao') {
    if (t >= 0.68 && t <= 1.85) {
      shakeAmp = 4.2 * clamp01((1.85 - t) / 0.5 + 0.35);
    }
  } else if (inst.skillId === 'linh_lan_thanh_vuc') {
    if (t >= 1.55 && t <= 1.95) {
      shakeAmp = (1 - (t - 1.55) / 0.4) * 2.2;
    }
  } else if (inst.skillId === 'thoi_khong_ngung_dong') {
    if (t >= 1.05 && t <= 1.35) {
      shakeAmp = (1 - (t - 1.05) / 0.3) * 3.5;
    } else if (t >= 2.15 && t <= 2.45) {
      shakeAmp = (1 - (t - 2.15) / 0.3) * 4.5;
    }
  } else if (inst.skillId === 'thien_ha_tinh_vuc') {
    if (t >= 1.65 && t <= 2.25) {
      shakeAmp = (1 - (t - 1.65) / 0.6) * 6.0;
    }
  }

  if (inst.reducedMotion || shakeAmp <= 0.05) {
    inst.shakeX = 0;
    inst.shakeY = 0;
  } else {
    inst.shakeX = clamp(Math.sin(t * 68) * shakeAmp, -7, 7);
    inst.shakeY = clamp(Math.cos(t * 83) * shakeAmp, -7, 7);
  }

  return inst.elapsed < inst.duration;
}

/* ============================================================================
 * Per-Skill Back / Front Renderers
 * ========================================================================== */

function drawVanKiem(ctx, inst, pass, pet) {
  const t = inst.elapsed;
  const px = pet ? pet.x : inst.ox;
  const py = pet ? pet.y : inst.oy;
  const { tx, ty, duration } = inst;
  const fadeOut = clamp01((duration - t) / 0.45);
  const haloTilt = inst.haloTilt || 0;
  const finisherTilt = inst.finisherTilt || 0;

  if (pass === 'back') {
    const formOpen = easeOutCubic(clamp01(t / 0.45));
    const formAlpha = formOpen * clamp01((2.5 - t) / 0.5);
    if (formAlpha > 0.01) {
      drawFormationRing3D(ctx, px, py - 12, 138 * formOpen, 138 * formOpen, haloTilt, t * 1.4, 8, '#fbbf24', '#a855f7', formAlpha * 0.85);
      drawFormationRing3D(ctx, px, py - 12, 92 * formOpen, 92 * formOpen, haloTilt, -t * 2.1, 6, '#f59e0b', '#c084fc', formAlpha * 0.65);
    }

    const sealAlpha = clamp01(t / 0.4) * fadeOut;
    if (sealAlpha > 0.01) {
      const seals = inst.clusters || [{ x: tx, y: ty }];
      for (const cl of seals) {
        drawFormationRing3D(ctx, cl.x, cl.y + 28, 128, 42, 0, -t * 1.2, 8, '#a855f7', '#fbbf24', sealAlpha * 0.78);
      }
    }
    return;
  }

  // Front pass: flying spirit swords + trails + impacts
  for (const sw of inst.swords) {
    if (t < sw.i * 0.016) continue;
    const appear = clamp01((t - sw.i * 0.016) / 0.2);
    const flightDone = sw.impactTriggered ? clamp01((t - (sw.launchAt + sw.flightDur)) / 0.85) : 0;
    const swAlpha = appear * (1 - flightDone) * fadeOut;
    if (swAlpha <= 0.01) continue;

    if (t >= sw.launchAt && !sw.impactTriggered) {
      drawTaperedTrail(ctx, sw.trail, 8, sw.i % 2 ? '#a855f7' : '#fbbf24', '#ffffff', swAlpha * 0.85);
    }
    drawSpiritSword(
      ctx,
      sw.x,
      sw.y,
      sw.ang,
      0.88 + (sw.i % 3) * 0.1,
      sw.i % 2 === 0 ? '#fbbf24' : '#f59e0b',
      '#a855f7',
      '#fef08a',
      swAlpha,
    );
  }

  for (const imp of inst.impacts) {
    const age = t - imp.at;
    if (age < 0 || age > 0.55) continue;
    const u = age / 0.55;
    const rad = 14 + easeOutCubic(u) * 44;
    const a = (1 - u) * fadeOut;
    ctx.save();
    ctx.translate(imp.x, imp.y + 8);
    ctx.scale(1, 0.36);
    ctx.globalAlpha = a * 0.82;
    ctx.strokeStyle = '#fbbf24';
    ctx.lineWidth = 2.4;
    ctx.beginPath();
    ctx.arc(0, 0, rad, 0, Math.PI * 2);
    ctx.stroke();
    ctx.restore();
    drawFourPointStar(ctx, imp.x, imp.y, (1 - u) * 16 + 4, '#ffffff', a);
  }

  // Sovereign Heaven Sword finisher (2.15..3.20s)
  if (t >= 2.1) {
    const dropU = clamp01((t - 2.1) / 0.26);
    const localY = -340 + easeOutCubic(dropU) * 340;
    const finAlpha = clamp01((t - 2.1) / 0.12) * fadeOut;

    ctx.save();
    ctx.translate(tx, ty);
    ctx.rotate(finisherTilt);
    if (dropU < 1) {
      ctx.globalAlpha = finAlpha * 0.45;
      ctx.fillStyle = '#fbbf24';
      ctx.fillRect(-10, localY - 180, 20, 180);
      ctx.globalAlpha = finAlpha * 0.85;
      ctx.fillStyle = '#ffffff';
      ctx.fillRect(-3, localY - 160, 6, 160);
    }
    drawSpiritSword(ctx, 0, localY - 24, Math.PI * 0.5, 3.2, '#fde047', '#a855f7', '#fbbf24', finAlpha);
    ctx.restore();

    if (t >= 2.34) {
      const waveAge = t - 2.34;
      for (let w = 0; w < 3; w += 1) {
        const wu = clamp01((waveAge - w * 0.1) / 0.65);
        if (wu <= 0 || wu >= 1) continue;
        const r = 34 + easeOutCubic(wu) * (175 + w * 45);
        drawFormationRing3D(
          ctx,
          tx,
          ty + 26,
          r,
          r * 0.34,
          0,
          t * (w % 2 === 0 ? 1.5 : -1.5),
          8,
          w === 0 ? '#ffffff' : '#fbbf24',
          '#a855f7',
          (1 - wu) * fadeOut,
        );
      }
      if (waveAge < 0.7) {
        const blastU = waveAge / 0.7;
        for (let k = 0; k < 20; k += 1) {
          const ka = (Math.PI * 2 * k) / 20;
          const dist = 25 + easeOutCubic(blastU) * (90 + (k % 3) * 45);
          drawFourPointStar(
            ctx,
            tx + Math.cos(ka) * dist,
            ty + Math.sin(ka) * dist * 0.55,
            (1 - blastU) * 14 + 3,
            k % 2 === 0 ? '#fbbf24' : '#c084fc',
            (1 - blastU) * fadeOut,
          );
        }
      }
    }
  }
}

function drawNhatKiem(ctx, inst, pass, pet, viewW, viewH) {
  const t = inst.elapsed;
  const px = pet ? pet.x : inst.ox;
  const py = pet ? pet.y : inst.oy;
  const cx = inst.slashCx;
  const cy = inst.slashCy;
  const span = Math.hypot(viewW, viewH) * 0.95;
  const fadeOut = clamp01((inst.duration - t) / 0.45);

  if (pass === 'back') {
    if (!inst.reducedMotion) {
      const dim = t < 0.72
        ? clamp01(t / 0.4) * 0.24
        : t < 1.55
        ? 0.24
        : 0.24 * clamp01((2.3 - t) / 0.75);
      if (dim > 0.01) {
        ctx.save();
        ctx.fillStyle = `rgba(2, 6, 23, ${dim.toFixed(3)})`;
        ctx.fillRect(-12, -12, viewW + 24, viewH + 24);
        ctx.restore();
      }
    }

    // Charge phase (0.0..0.74s): inward converging qi threads
    if (t < 0.78) {
      const u = clamp01(t / 0.72);
      const easeU = easeInOutQuad(u);
      const qiAlpha = clamp01((0.78 - t) / 0.14);
      ctx.save();
      for (const q of inst.qiLines) {
        const dNow = q.dist * (1 - easeU);
        const x1 = px + Math.cos(q.ang) * (dNow + q.len);
        const y1 = py + Math.sin(q.ang) * (dNow + q.len);
        const x2 = px + Math.cos(q.ang) * dNow;
        const y2 = py + Math.sin(q.ang) * dNow;
        ctx.globalAlpha = qiAlpha * 0.75;
        ctx.strokeStyle = q.gold ? '#fbbf24' : '#a855f7';
        ctx.lineWidth = 2.4;
        ctx.beginPath();
        ctx.moveTo(x1, y1);
        ctx.lineTo(x2, y2);
        ctx.stroke();
      }
      ctx.restore();
      drawFormationRing3D(ctx, px, py, 95 * (1 - easeU * 0.65), 95 * (1 - easeU * 0.65), 0, t * 4, 8, '#fbbf24', '#a855f7', qiAlpha * 0.85);
    }
    return;
  }

  // Front pass: glint + corner-to-corner dimension slash + fracture shards
  if (t >= 0.45 && t < 0.76) {
    const glU = clamp01((t - 0.45) / 0.25);
    drawFourPointStar(ctx, px, py - 8, 22 + glU * 26, '#ffffff', 0.95);
    drawFourPointStar(ctx, px, py - 8, 36 + glU * 34, '#fbbf24', 0.55);
  }

  if (t >= 0.72 && t <= 1.65) {
    const slashU = clamp01((t - 0.72) / 0.18);
    const slashOut = t > 1.05 ? clamp01((1.65 - t) / 0.6) : 1;
    const curSpan = span * easeOutCubic(slashU);
    const curveDrop = inst.curveDrop || 75;

    ctx.save();
    ctx.translate(cx, cy);
    ctx.rotate(inst.slashAngle);

    // Outer violet slash crescent polygon
    ctx.globalAlpha = slashOut * 0.72;
    ctx.fillStyle = '#a855f7';
    ctx.beginPath();
    ctx.moveTo(-curSpan, 0);
    ctx.quadraticCurveTo(0, curveDrop - 42 * slashOut, curSpan, 0);
    ctx.quadraticCurveTo(0, curveDrop + 42 * slashOut, -curSpan, 0);
    ctx.closePath();
    ctx.fill();

    // Mid gold crescent
    ctx.globalAlpha = slashOut * 0.9;
    ctx.fillStyle = '#fbbf24';
    ctx.beginPath();
    ctx.moveTo(-curSpan * 0.96, 0);
    ctx.quadraticCurveTo(0, curveDrop - 22 * slashOut, curSpan * 0.96, 0);
    ctx.quadraticCurveTo(0, curveDrop + 22 * slashOut, -curSpan * 0.96, 0);
    ctx.closePath();
    ctx.fill();

    // Pure white razor core
    ctx.globalAlpha = slashOut;
    ctx.fillStyle = '#ffffff';
    ctx.beginPath();
    ctx.moveTo(-curSpan * 0.92, 0);
    ctx.quadraticCurveTo(0, curveDrop - 8 * slashOut, curSpan * 0.92, 0);
    ctx.quadraticCurveTo(0, curveDrop + 8 * slashOut, -curSpan * 0.92, 0);
    ctx.closePath();
    ctx.fill();

    // Spatial fracture void-lightning cracks
    ctx.globalAlpha = slashOut * 0.85;
    ctx.strokeStyle = '#c084fc';
    ctx.lineWidth = 2.0;
    for (const c of inst.cracks) {
      if (c.frac > slashU) continue;
      const sx = -span + c.frac * span * 2;
      const sy = curveDrop * 0.5 * (1 - (c.frac * 2 - 1) ** 2);
      ctx.beginPath();
      ctx.moveTo(sx, sy);
      for (const seg of c.segs) {
        ctx.lineTo(sx + seg.along, sy + seg.perp);
      }
      ctx.stroke();
    }
    ctx.restore();
  }

  // Floating dimension shards after cleave (1.15..2.80s)
  if (t >= 1.15) {
    const shAge = t - 1.15;
    const cosA = Math.cos(inst.slashAngle);
    const sinA = Math.sin(inst.slashAngle);
    for (const sh of inst.shards) {
      const localX = -span * 0.85 + sh.frac * span * 1.7;
      const sx = cx + localX * cosA + sh.vx * shAge * 0.65;
      const sy = cy + localX * sinA + sh.vy * shAge * 0.65;
      ctx.save();
      ctx.translate(sx, sy);
      ctx.rotate(sh.rot + sh.vRot * shAge);
      ctx.globalAlpha = fadeOut * 0.85;
      ctx.fillStyle = sh.gold ? '#fbbf24' : '#c084fc';
      ctx.beginPath();
      ctx.moveTo(0, -sh.sz);
      ctx.lineTo(sh.sz * 0.42, 0);
      ctx.lineTo(0, sh.sz);
      ctx.lineTo(-sh.sz * 0.42, 0);
      ctx.closePath();
      ctx.fill();
      ctx.restore();
    }
  }
}

function drawMuaTenLua(ctx, inst, pass) {
  const t = inst.elapsed;
  const { tx, ty, duration } = inst;
  const targets = inst.lockTargets || [{ x: tx, y: ty }];
  const fadeOut = clamp01((duration - t) / 0.45);

  if (pass === 'back') {
    const lockAlpha = clamp01(t / 0.25) * fadeOut;
    if (lockAlpha <= 0.01) return;

    const corners = [
      [-1, -1],
      [1, -1],
      [1, 1],
      [-1, 1],
    ];
    for (let k = 0; k < targets.length; k += 1) {
      const tgt = targets[k];
      const scale = k === 0 ? 1 : 0.78;
      drawPolygonRing(ctx, tgt.x, tgt.y, 94 * scale, 6, t * 1.1 + k, '#22d3ee', 2.2, lockAlpha * 0.78);
      drawPolygonRing(ctx, tgt.x, tgt.y, 64 * scale, 6, -t * 1.7 + k, '#fb923c', 2.0, lockAlpha * 0.85);
      drawPolygonRing(ctx, tgt.x, tgt.y, 32 * scale, 6, t * 2.5 + k, '#ffffff', 1.6, lockAlpha * 0.7);

      ctx.save();
      ctx.globalAlpha = lockAlpha * 0.9;
      ctx.strokeStyle = '#22d3ee';
      ctx.lineWidth = 2.2;
      const b = 108 * scale;
      const leg = 18 * scale;
      for (const [cxS, cyS] of corners) {
        const bx = tgt.x + cxS * b;
        const by = tgt.y + cyS * b;
        ctx.beginPath();
        ctx.moveTo(bx - cxS * leg, by);
        ctx.lineTo(bx, by);
        ctx.lineTo(bx, by - cyS * leg);
        ctx.stroke();
      }
      ctx.restore();
    }
    return;
  }

  for (const ms of inst.missiles) {
    if (t < ms.launchAt || ms.detonated) continue;
    drawTaperedTrail(ctx, ms.trail, 7, '#22d3ee', '#ffffff', 0.9 * fadeOut);

    ctx.save();
    ctx.translate(ms.x, ms.y);
    ctx.rotate(ms.ang);
    ctx.globalAlpha = fadeOut;
    ctx.fillStyle = '#ffffff';
    ctx.beginPath();
    ctx.moveTo(11, 0);
    ctx.lineTo(-4, -3.6);
    ctx.lineTo(-7, 0);
    ctx.lineTo(-4, 3.6);
    ctx.closePath();
    ctx.fill();
    ctx.fillStyle = '#fb923c';
    ctx.beginPath();
    ctx.moveTo(-5, 0);
    ctx.lineTo(-13, -3);
    ctx.lineTo(-10, 0);
    ctx.lineTo(-13, 3);
    ctx.closePath();
    ctx.fill();
    ctx.restore();
  }

  for (const det of inst.detonations) {
    const age = t - det.at;
    if (age < 0 || age > 0.55) continue;
    const u = age / 0.55;
    const rad = 14 + easeOutCubic(u) * 56;
    const a = (1 - u) * fadeOut;
    drawPolygonRing(ctx, det.x, det.y, rad, 6, det.at * 2 + u, '#fb923c', 2.6, a * 0.9, a * 0.16);
    drawPolygonRing(ctx, det.x, det.y, rad * 0.65, 6, -u, '#22d3ee', 2.0, a * 0.85);
    drawFourPointStar(ctx, det.x, det.y, (1 - u) * 18 + 3, '#ffffff', a);
  }

  // Final EMP Hex Pulse across all locked zones
  if (t >= 2.05) {
    for (let k = 0; k < targets.length; k += 1) {
      const tgt = targets[k];
      const u = clamp01((t - (2.05 + k * 0.12)) / 0.7);
      if (u <= 0 || u >= 1) continue;
      const r = 36 + easeOutCubic(u) * (k === 0 ? 210 : 155);
      drawPolygonRing(ctx, tgt.x, tgt.y, r, 6, t * 0.8 + k, '#22d3ee', 3.4, (1 - u) * fadeOut, (1 - u) * 0.08);
      drawPolygonRing(ctx, tgt.x, tgt.y, r * 0.75, 6, -t * 0.8 + k, '#fb923c', 2.4, (1 - u) * fadeOut);
    }
  }
}

function drawPhaoQuyDao(ctx, inst, pass, pet) {
  const t = inst.elapsed;
  const px = pet ? pet.x : inst.ox;
  const py = pet ? pet.y : inst.oy;
  const { tx, ty, duration } = inst;
  const satX = Number.isFinite(inst.satX) ? inst.satX : tx;
  const satY = Number.isFinite(inst.satY) ? inst.satY : 74;
  const dx = tx - satX;
  const dy = ty - satY;
  const beamLen = Math.max(40, Math.hypot(dx, dy));
  const beamAng = Math.atan2(dy, dx);
  const satTilt = clamp(Math.atan2(dx, Math.max(40, dy)) * 0.28, -0.35, 0.35);
  const fadeOut = clamp01((duration - t) / 0.45);

  if (pass === 'back') {
    const satAlpha = clamp01(t / 0.35) * fadeOut;
    if (satAlpha > 0.01) {
      drawFormationRing3D(ctx, satX, satY, 108, 34, satTilt, t * 1.8, 6, '#22d3ee', '#fb923c', satAlpha * 0.88);
      drawFormationRing3D(ctx, satX, satY, 64, 20, satTilt, -t * 2.5, 6, '#ffffff', '#22d3ee', satAlpha * 0.75);
      // Chest reactor charge ring around mascot
      if (t < 1.9) {
        drawPolygonRing(ctx, px, py - 8, 52 + Math.sin(t * 14) * 6, 6, t * 2.2, '#22d3ee', 2.4, satAlpha * 0.82);
      }
    }
    return;
  }

  // Telegraph laser sight (0.0..0.68s)
  if (t < 0.72) {
    const sigA = clamp01(t / 0.2) * (1 - clamp01((t - 0.65) / 0.07));
    ctx.save();
    ctx.globalAlpha = sigA * 0.78;
    ctx.strokeStyle = '#fb923c';
    ctx.lineWidth = 2.0;
    ctx.setLineDash?.([8, 6]);
    ctx.beginPath();
    ctx.moveTo(satX, satY);
    ctx.lineTo(tx, ty);
    ctx.moveTo(px, py - 10);
    ctx.lineTo(tx, ty);
    ctx.stroke();
    ctx.setLineDash?.([]);
    ctx.restore();
  }

  // Angled Orbital Ion Pillar + Railgun Lance (0.68..2.05s)
  if (t >= 0.65 && t <= 2.15) {
    const onU = clamp01((t - 0.65) / 0.14);
    const offU = t > 1.75 ? clamp01((2.15 - t) / 0.4) : 1;
    const beamS = easeOutCubic(onU) * offU;

    ctx.save();
    ctx.translate(satX, satY);
    ctx.rotate(beamAng);
    // Outer pillar
    ctx.globalAlpha = beamS * 0.36;
    ctx.fillStyle = '#0891b2';
    ctx.fillRect(0, -46 * beamS, beamLen, 92 * beamS);
    // Mid pillar
    ctx.globalAlpha = beamS * 0.78;
    ctx.fillStyle = '#22d3ee';
    ctx.fillRect(0, -26 * beamS, beamLen, 52 * beamS);
    // White core
    ctx.globalAlpha = beamS * 0.96;
    ctx.fillStyle = '#ffffff';
    ctx.fillRect(0, -10 * beamS, beamLen, 20 * beamS);
    ctx.restore();

    ctx.save();
    // Angled lance from Chien Than chest to target
    ctx.lineCap = 'round';
    ctx.globalAlpha = beamS * 0.45;
    ctx.strokeStyle = '#fb923c';
    ctx.lineWidth = 24 * beamS;
    ctx.beginPath();
    ctx.moveTo(px, py - 10);
    ctx.lineTo(tx, ty);
    ctx.stroke();

    ctx.globalAlpha = beamS * 0.92;
    ctx.strokeStyle = '#ffffff';
    ctx.lineWidth = 7 * beamS;
    ctx.beginPath();
    ctx.moveTo(px, py - 10);
    ctx.lineTo(tx, ty);
    ctx.stroke();
    ctx.restore();

    // 8 descending helical plasma rings along the angled orbital beam
    for (const hr of inst.helicalRings) {
      const p = (t * 1.45 + hr.offset) % 1;
      const rx = satX + dx * p;
      const ry = satY + dy * p;
      drawFormationRing3D(
        ctx,
        rx,
        ry,
        62 * beamS,
        18 * beamS,
        beamAng - Math.PI * 0.5,
        t * 3,
        6,
        hr.orange ? '#fb923c' : '#22d3ee',
        '#ffffff',
        beamS * 0.85,
      );
    }

    drawFormationRing3D(ctx, tx, ty + 16, 130 * beamS, 42 * beamS, 0, -t * 3, 6, '#fb923c', '#22d3ee', beamS * 0.92);
  }

  // Discharge electric arcs (1.75..2.80s)
  if (t >= 1.75) {
    const disU = clamp01((t - 1.75) / 1.0);
    const disA = (1 - disU) * fadeOut;
    ctx.save();
    ctx.strokeStyle = '#22d3ee';
    ctx.lineWidth = 2.4;
    ctx.globalAlpha = disA * 0.9;
    for (const arc of inst.arcs) {
      ctx.beginPath();
      ctx.moveTo(tx, ty);
      for (let s = 1; s <= 4; s += 1) {
        const len = s * (32 + disU * 34);
        const jitter = (hash01(arc.seed + s + Math.floor(t * 18)) - 0.5) * 28;
        ctx.lineTo(tx + Math.cos(arc.ang) * len + jitter, ty + Math.sin(arc.ang) * len + jitter);
      }
      ctx.stroke();
    }
    ctx.restore();
    drawPolygonRing(ctx, tx, ty, 36 + easeOutCubic(disU) * 160, 6, t * 2, '#fb923c', 2.8, disA);
  }
}

function drawBachHoa(ctx, inst, pass, pet) {
  const t = inst.elapsed;
  const px = pet ? pet.x : inst.ox;
  const py = pet ? pet.y : inst.oy;
  const { duration } = inst;
  const fadeOut = clamp01((duration - t) / 0.5);
  const fadeIn = clamp01(t / 0.35);

  if (pass === 'back') {
    const ribAlpha = fadeIn * fadeOut * 0.72;
    if (ribAlpha <= 0.01) return;

    const swirlSign = inst.swirlSign || 1;
    const windRot = inst.windRot || 0;
    for (let r = 0; r < 2; r += 1) {
      const pts = [];
      const phase = r * Math.PI + t * 2.8 * swirlSign;
      for (let s = 0; s <= 22; s += 1) {
        const u = s / 22;
        const rad = 42 + u * 210;
        const ang = windRot + phase + u * Math.PI * 2.2 * swirlSign;
        pts.push({
          x: px + (inst.tx - px) * u * 0.65 + Math.cos(ang) * rad,
          y: py + (inst.ty - py) * u * 0.45 + Math.sin(ang) * (rad * 0.46),
        });
      }
      drawTaperedTrail(ctx, pts, 14, r === 0 ? '#34d399' : '#f472b6', '#ffffff', ribAlpha);
    }
    return;
  }

  for (const pt of inst.petals) {
    if (t < pt.spawnAt) continue;
    const localT = t - pt.spawnAt;
    const lifeU = clamp01(localT / (duration - pt.spawnAt));
    const r = pt.radMin + (pt.radMax - pt.radMin) * easeOutCubic(lifeU);
    const ang = pt.orbAng + localT * pt.orbSpeed;
    const x = px + pt.driftX * lifeU + Math.cos(ang) * r;
    const y = py + pt.driftY * lifeU + Math.sin(ang) * (r * 0.52);
    const alpha = clamp01(localT / 0.25) * fadeOut;

    if (pt.isStar) {
      drawFourPointStar(ctx, x, y, pt.size * 0.8, '#fef08a', alpha * 0.9);
    } else {
      drawBlossomPetal(ctx, x, y, pt.rot + localT * pt.spin, pt.size, pt.color, '#ffffff', alpha * 0.94);
    }
  }
}

function drawLinhLanThanhVuc(ctx, inst, pass) {
  const t = inst.elapsed;
  const { tx, ty, duration } = inst;
  const fadeOut = clamp01((duration - t) / 0.5);
  const openU = easeOutCubic(clamp01(t / 0.55));

  if (pass === 'back') {
    const a = openU * fadeOut;
    if (a <= 0.01) return;

    const tiltRad = inst.tiltRad || 0;
    const baseRot = inst.baseRot || 0;
    drawFormationRing3D(ctx, tx, ty + 24, 156 * openU, 54 * openU, tiltRad, baseRot + t * 0.9, 12, '#34d399', '#f472b6', a * 0.88);
    drawFormationRing3D(ctx, tx, ty + 24, 108 * openU, 38 * openU, tiltRad, baseRot - t * 1.4, 8, '#fbbf24', '#34d399', a * 0.78);

    // Blooming 12-petal sacred lotus geometry at ground center
    ctx.save();
    ctx.translate(tx, ty + 24);
    ctx.rotate(tiltRad);
    ctx.scale(1, 0.36);
    ctx.rotate(baseRot + t * 0.55);
    for (let p = 0; p < 12; p += 1) {
      const ang = (Math.PI * 2 * p) / 12;
      ctx.save();
      ctx.rotate(ang);
      ctx.globalAlpha = a * 0.42;
      ctx.fillStyle = p % 2 === 0 ? '#34d399' : '#f472b6';
      ctx.beginPath();
      ctx.moveTo(0, 0);
      ctx.quadraticCurveTo(32 * openU, -28 * openU, 118 * openU, 0);
      ctx.quadraticCurveTo(32 * openU, 28 * openU, 0, 0);
      ctx.closePath();
      ctx.fill();
      ctx.restore();
    }
    ctx.restore();
    return;
  }

  // Front pass: 8 descending luminous lily-bell lanterns + 40 rising starlight motes
  for (const lt of inst.lanterns) {
    const dropU = easeOutCubic(clamp01((t - lt.delay) / 0.65));
    const lx = tx + Math.cos(lt.ang + t * 0.35) * lt.radius;
    const ly = ty - 220 + dropU * 205 + Math.sin(t * 3 + lt.ang) * 7;
    const la = clamp01((t - lt.delay) / 0.25) * fadeOut;
    if (la <= 0.01) continue;

    ctx.save();
    ctx.translate(lx, ly);
    // Bell-flower aura
    ctx.globalAlpha = la * 0.32;
    ctx.fillStyle = '#34d399';
    ctx.beginPath();
    ctx.arc(0, 0, 20, 0, Math.PI * 2);
    ctx.fill();
    // Bell cap
    ctx.globalAlpha = la * 0.95;
    ctx.fillStyle = '#fbcfe8';
    ctx.beginPath();
    ctx.moveTo(0, -13);
    ctx.bezierCurveTo(13, -10, 14, 4, 11, 10);
    ctx.lineTo(4, 7);
    ctx.lineTo(0, 12);
    ctx.lineTo(-4, 7);
    ctx.lineTo(-11, 10);
    ctx.bezierCurveTo(-14, 4, -13, -10, 0, -13);
    ctx.closePath();
    ctx.fill();
    // Golden stamen core
    drawFourPointStar(ctx, 0, 11, 6.5, '#fbbf24', la);
    ctx.restore();
  }

  // Rising starlight motes
  for (const m of inst.motes) {
    const my = ty + 35 - ((t * m.speed) % 210);
    const mx = tx + m.xOff;
    const color = m.gold ? '#fbbf24' : m.emerald ? '#34d399' : '#f472b6';
    drawFourPointStar(ctx, mx, my, m.size, color, fadeOut * 0.82);
  }
}

function drawThoiKhongNgungDong(ctx, inst, pass) {
  const t = inst.elapsed;
  const { tx, ty, duration } = inst;
  const fadeOut = clamp01((duration - t) / 0.45);
  const frozen = t >= 1.05 && t < 2.15;

  // Spin clock quickly before 1.05s, snap-freeze during 1.05..2.15s, resume gently on shatter
  const baseOffset = inst.handOffset || 0;
  const gearOrbit = inst.gearOrbit || 0;
  const clockTime = baseOffset + (t < 1.05 ? t * 4.5 : t < 2.15 ? 1.05 * 4.5 : 1.05 * 4.5 + (t - 2.15) * 1.4);

  if (pass === 'back') {
    const clockAlpha = clamp01(t / 0.35) * fadeOut;
    if (clockAlpha <= 0.01) return;

    const scale = easeOutCubic(clamp01(t / 0.45));
    const r = 112 * scale;
    drawFormationRing3D(ctx, tx, ty, r, r, 0, clockTime * 0.25, 12, '#c084fc', '#fbbf24', clockAlpha * 0.85);
    drawGearWheel(ctx, tx + Math.cos(gearOrbit + 2.4) * r * 0.54, ty + Math.sin(gearOrbit + 2.4) * r * 0.54, 34 * scale, 10, clockTime, '#fbbf24', clockAlpha * 0.65);
    drawGearWheel(ctx, tx + Math.cos(gearOrbit - 0.6) * r * 0.54, ty + Math.sin(gearOrbit - 0.6) * r * 0.54, 28 * scale, 8, -clockTime * 1.3, '#c084fc', clockAlpha * 0.65);
    drawGearWheel(ctx, tx, ty, 44 * scale, 12, clockTime * 0.7, '#e9d5ff', clockAlpha * 0.55);

    // 12 Hour tick marks & frozen/spinning hands
    ctx.save();
    ctx.translate(tx, ty);
    ctx.globalAlpha = clockAlpha * 0.9;
    ctx.strokeStyle = '#fbbf24';
    ctx.lineWidth = 2.4;
    for (let h = 0; h < 12; h += 1) {
      const ha = (Math.PI * 2 * h) / 12;
      ctx.beginPath();
      ctx.moveTo(Math.cos(ha) * (r * 0.74), Math.sin(ha) * (r * 0.74));
      ctx.lineTo(Math.cos(ha) * (r * 0.84), Math.sin(ha) * (r * 0.84));
      ctx.stroke();
    }
    // Minute & Hour hands
    ctx.strokeStyle = '#ffffff';
    ctx.lineWidth = 3.2;
    ctx.beginPath();
    ctx.moveTo(0, 0);
    ctx.lineTo(Math.cos(clockTime * 2.4) * (r * 0.68), Math.sin(clockTime * 2.4) * (r * 0.68));
    ctx.stroke();

    ctx.strokeStyle = '#fbbf24';
    ctx.lineWidth = 4.2;
    ctx.beginPath();
    ctx.moveTo(0, 0);
    ctx.lineTo(Math.cos(clockTime * 0.35) * (r * 0.45), Math.sin(clockTime * 0.35) * (r * 0.45));
    ctx.stroke();
    ctx.restore();
    return;
  }

  // Time-stop shockwave pulse at 1.05s
  if (t >= 1.02 && t <= 1.65) {
    const u = (t - 1.02) / 0.63;
    drawPolygonRing(ctx, tx, ty, 32 + easeOutCubic(u) * 280, 12, t * 0.4, '#c084fc', 3.4, (1 - u) * fadeOut);
  }

  // Suspended chrono-crystals hanging motionless during freeze then shattering outward
  const shatterT = t > 2.15 ? t - 2.15 : 0;
  for (const cr of inst.crystals) {
    const d = cr.dist + (shatterT > 0 ? easeOutCubic(clamp01(shatterT / 0.75)) * 210 : 0);
    const aNow = frozen ? cr.ang : cr.ang + (t < 1.05 ? t * 0.75 : 1.05 * 0.75);
    const x = tx + Math.cos(aNow) * d;
    const y = ty + Math.sin(aNow) * d * 0.72;
    const alpha = clamp01(t / 0.4) * fadeOut;

    ctx.save();
    ctx.translate(x, y);
    ctx.rotate(frozen ? cr.rot : cr.rot + t * 1.8);
    ctx.globalAlpha = alpha * 0.92;
    ctx.fillStyle = cr.purple ? '#c084fc' : '#fbbf24';
    ctx.beginPath();
    ctx.moveTo(0, -cr.size);
    ctx.lineTo(cr.size * 0.55, 0);
    ctx.lineTo(0, cr.size);
    ctx.lineTo(-cr.size * 0.55, 0);
    ctx.closePath();
    ctx.fill();
    ctx.restore();
  }
}

function drawThienHaTinhVuc(ctx, inst, pass) {
  const t = inst.elapsed;
  const { tx, ty, duration } = inst;
  const fadeOut = clamp01((duration - t) / 0.5);
  const collapseEnd = 1.65;

  if (pass === 'back') {
    const openU = easeOutCubic(clamp01(t / 0.45));
    const a = openU * fadeOut;
    if (a <= 0.01) return;

    // Central celestial singularity core disc (solid concentric circles, zero gradients)
    const coreScale = t < collapseEnd
      ? 0.65 + (t / collapseEnd) * 0.55
      : Math.max(0.15, 1.2 - ((t - collapseEnd) / 0.35) * 0.9);
    ctx.save();
    ctx.globalAlpha = a * 0.65;
    ctx.fillStyle = '#3b0764';
    ctx.beginPath();
    ctx.arc(tx, ty, 38 * coreScale, 0, Math.PI * 2);
    ctx.fill();
    ctx.globalAlpha = a * 0.9;
    ctx.fillStyle = '#581c87';
    ctx.beginPath();
    ctx.arc(tx, ty, 24 * coreScale, 0, Math.PI * 2);
    ctx.fill();
    ctx.globalAlpha = a;
    ctx.strokeStyle = '#fbbf24';
    ctx.lineWidth = 2.4;
    ctx.beginPath();
    ctx.arc(tx, ty, 28 * coreScale, 0, Math.PI * 2);
    ctx.stroke();
    ctx.restore();

    // 3 Gyroscopic 3D Armillary Rings
    const tilts = inst.ringTilts || [0.28, -0.42, 0.95];
    drawFormationRing3D(ctx, tx, ty, 138 * openU, 46 * openU, tilts[0], t * 1.4, 8, '#c084fc', '#fbbf24', a * 0.86);
    drawFormationRing3D(ctx, tx, ty, 118 * openU, 40 * openU, tilts[1], -t * 1.9, 6, '#fbbf24', '#38bdf8', a * 0.86);
    drawFormationRing3D(ctx, tx, ty, 94 * openU, 32 * openU, tilts[2], t * 2.4, 6, '#38bdf8', '#c084fc', a * 0.78);
    return;
  }

  // Front pass: constellation spiral collapsing into supernova
  if (t < collapseEnd) {
    const u = clamp01(t / collapseEnd);
    const collapseU = easeInOutQuad(u);
    const pts = [];
    for (const st of inst.stars) {
      const dist = Math.max(12, st.startDist * (1 - collapseU * 0.88));
      const ang = st.ang + t * st.speed;
      const sx = tx + Math.cos(ang) * dist;
      const sy = ty + Math.sin(ang) * dist * 0.68;
      pts.push({ x: sx, y: sy, size: st.size, tint: st.tint });
    }

    // Constellation web lines
    ctx.save();
    ctx.globalAlpha = 0.45 * fadeOut;
    ctx.strokeStyle = '#c084fc';
    ctx.lineWidth = 1.2;
    ctx.beginPath();
    for (let i = 0; i < pts.length; i += 2) {
      const p1 = pts[i];
      const p2 = pts[(i + 3) % pts.length];
      ctx.moveTo(p1.x, p1.y);
      ctx.lineTo(p2.x, p2.y);
    }
    ctx.stroke();
    ctx.restore();

    for (const p of pts) {
      drawFourPointStar(ctx, p.x, p.y, p.size, p.tint, fadeOut * 0.95);
    }
  } else {
    const blastT = t - collapseEnd;
    const blastU = clamp01(blastT / 0.9);
    const burstR = easeOutCubic(blastU) * 230;
    drawPolygonRing(ctx, tx, ty, burstR, 12, t * 1.2, '#fbbf24', 3.4, (1 - blastU) * fadeOut, (1 - blastU) * 0.14);
    drawPolygonRing(ctx, tx, ty, burstR * 0.72, 12, -t * 1.5, '#c084fc', 2.6, (1 - blastU) * fadeOut);
    drawFourPointStar(ctx, tx, ty, (1 - blastU) * 62 + 10, '#ffffff', (1 - blastU) * fadeOut);

    for (const st of inst.stars) {
      const d = 14 + easeOutCubic(blastU) * st.burstSpd;
      const sx = tx + Math.cos(st.burstAng) * d;
      const sy = ty + Math.sin(st.burstAng) * d * 0.72;
      drawFourPointStar(ctx, sx, sy, st.size * (1 - blastU * 0.4), st.tint, (1 - blastU) * fadeOut);
    }
  }
}

export function drawSkillInstance(ctx, inst, pass, pet, viewW, viewH) {
  if (!ctx || !inst) return;
  const vw = Math.max(320, Number(viewW) || inst.viewW || 1280);
  const vh = Math.max(320, Number(viewH) || inst.viewH || 720);

  switch (inst.skillId) {
    case 'van_kiem':
      drawVanKiem(ctx, inst, pass, pet);
      break;
    case 'nhat_kiem':
      drawNhatKiem(ctx, inst, pass, pet, vw, vh);
      break;
    case 'mua_ten_lua':
      drawMuaTenLua(ctx, inst, pass);
      break;
    case 'phao_quy_dao':
      drawPhaoQuyDao(ctx, inst, pass, pet, vw, vh);
      break;
    case 'bach_hoa':
      drawBachHoa(ctx, inst, pass, pet);
      break;
    case 'linh_lan_thanh_vuc':
      drawLinhLanThanhVuc(ctx, inst, pass);
      break;
    case 'thoi_khong_ngung_dong':
      drawThoiKhongNgungDong(ctx, inst, pass);
      break;
    case 'thien_ha_tinh_vuc':
      drawThienHaTinhVuc(ctx, inst, pass);
      break;
    default:
      break;
  }
}
