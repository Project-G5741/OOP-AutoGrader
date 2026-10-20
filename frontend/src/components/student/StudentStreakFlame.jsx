/* Lucian UX/UI Prompt #379 · MIT License (c) 2026 Seokwoooo · https://lucianlabs.dev/uxui-prompt/#379 */
import { useEffect, useId, useRef, useState } from 'react';
import './StudentStreakFlame.css';

const PRO = [[0.05, 0.6], [0.19, 0.96], [0.37, 1], [0.55, 0.8], [0.71, 0.52], [0.85, 0.26]];
const CX = 60;
const BASE = 138;
const LAYERS = [
  { key: 'o', w: 40, h: 126, ph: 0, sw: 1 },
  { key: 'm', w: 28, h: 88, ph: 1.7, sw: 0.8 },
  { key: 'c', w: 15, h: 48, ph: 3.1, sw: 0.55 },
];

function prefersReducedMotion() {
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches;
}

function outline(layer, t, flare) {
  const w = layer.w * (1 + flare * 0.08);
  const h = layer.h * (1 + flare * 0.2);
  const sway = (f) => (Math.sin(t * 1.7 + layer.ph) * 0.6 + Math.sin(t * 2.9 + layer.ph * 2) * 0.4)
    * h * 0.13 * layer.sw * Math.pow(f, 1.5);
  const right = [];
  const left = [];
  PRO.forEach((q) => {
    const f = q[0];
    const y = BASE - f * h;
    const x = q[1] * w;
    const s0 = sway(f);
    right.push([CX + x + s0 + Math.sin(t * 6.1 + f * 7 + layer.ph) * w * 0.07 * f, y]);
    left.unshift([CX - x + s0 - Math.sin(t * 5.3 + f * 6 + layer.ph + 2) * w * 0.07 * f, y]);
  });
  const tip = [
    CX + sway(1) + Math.sin(t * 4.3 + layer.ph) * w * 0.12,
    BASE - h * (1 + Math.sin(t * 5.2 + layer.ph) * 0.035),
  ];
  const points = [[CX, BASE]].concat(right, [tip, tip], left);
  const n = points.length;
  let d = `M${points[0][0].toFixed(1)} ${points[0][1].toFixed(1)}`;
  for (let i = 0; i < n; i += 1) {
    const a = points[(i - 1 + n) % n];
    const b = points[i];
    const c = points[(i + 1) % n];
    const e = points[(i + 2) % n];
    d += ` C${(b[0] + (c[0] - a[0]) / 6).toFixed(1)} ${(b[1] + (c[1] - a[1]) / 6).toFixed(1)} ${(c[0] - (e[0] - b[0]) / 6).toFixed(1)} ${(c[1] - (e[1] - b[1]) / 6).toFixed(1)} ${c[0].toFixed(1)} ${c[1].toFixed(1)}`;
  }
  return `${d}Z`;
}

function MissionDot({ done, today }) {
  const className = `u379-dt${done ? ' is-on' : ''}${!done && today ? ' is-today' : ''}`;
  return <span className={className} aria-hidden="true" />;
}

export default function StudentStreakFlame({
  streak = 0,
  loggedIn = false,
  submitCount = 0,
  submitGoal = 3,
  petCount = 0,
  petGoal = 4,
  completedToday = false,
  broken = false,
  savedStreak = 0,
  canRestore = false,
  awardPulse = 0,
  onRestore,
  restoring = false,
}) {
  const uid = useId().replace(/:/g, '');
  const rootRef = useRef(null);
  const dialogRef = useRef(null);
  const flRef = useRef(null);
  const pathRefs = useRef([]);
  const sparkRefs = useRef([]);
  const heatRef = useRef(0);
  const litRef = useRef(false);
  const flareRef = useRef(0);
  const paintRef = useRef(() => {});
  const [open, setOpen] = useState(false);
  const [reduced, setReduced] = useState(false);
  const [roll, setRoll] = useState(false);
  const [fromCount, setFromCount] = useState(streak);
  const prevStreak = useRef(streak);

  const submitShown = Math.min(submitCount, submitGoal);
  const petShown = Math.min(petCount, petGoal);
  const submitDone = submitCount >= submitGoal;
  const petDone = petCount >= petGoal;
  const heat = (loggedIn ? 1 : 0) + (submitDone ? 1 : 0) + (petDone ? 1 : 0);
  heatRef.current = heat;
  litRef.current = completedToday;
  const nextMission = !loggedIn ? 'login' : !submitDone ? 'submit' : !petDone ? 'pet' : null;

  useEffect(() => {
    const media = window.matchMedia('(prefers-reduced-motion: reduce)');
    const apply = () => setReduced(media.matches);
    apply();
    media.addEventListener('change', apply);
    return () => media.removeEventListener('change', apply);
  }, []);

  useEffect(() => {
    const prev = prevStreak.current;
    if (!reduced && streak === prev + 1) {
      setFromCount(prev);
      setRoll(false);
      const frame = requestAnimationFrame(() => setRoll(true));
      prevStreak.current = streak;
      return () => cancelAnimationFrame(frame);
    }
    setFromCount(streak);
    setRoll(false);
    prevStreak.current = streak;
    return undefined;
  }, [streak, awardPulse, reduced]);

  useEffect(() => {
    if (!awardPulse) return;
    flareRef.current = prefersReducedMotion() ? 0 : 1;
    if (prefersReducedMotion()) paintRef.current(0);
  }, [awardPulse]);

  useEffect(() => {
    const root = rootRef.current;
    if (!root) return undefined;
    const sparks = Array.from({ length: 7 }, () => ({ t0: -1, life: 0, x: 0, dx: 0, r: 0 }));
    let visible = true;
    let frameId = 0;
    const reducedNow = prefersReducedMotion();

    function paint(now) {
      const flare = flareRef.current;
      const done = heatRef.current;
      const t = reducedNow ? 0 : now / 1000;
      LAYERS.forEach((layer, index) => {
        pathRefs.current[index]?.setAttribute('d', outline(layer, t, flare));
      });
      if (flRef.current) {
        const scale = 0.8 + done * 0.05;
        flRef.current.style.transform = `scale(${scale.toFixed(2)})`;
      }
      const lit = litRef.current;
      sparks.forEach((spark, k) => {
        const el = sparkRefs.current[k];
        if (!el) return;
        if (reducedNow || !lit) {
          el.setAttribute('r', '0');
          return;
        }
        const on = k < 2 + Math.round(done * 0.7 + flare * 3);
        if (spark.t0 < 0 || now - spark.t0 > spark.life) {
          if (!on) {
            el.setAttribute('r', '0');
            spark.t0 = -1;
            return;
          }
          spark.t0 = now + Math.random() * 600;
          spark.life = 900 + Math.random() * 700;
          spark.x = CX + (Math.random() - 0.5) * 30;
          spark.dx = (Math.random() - 0.5) * 30;
          spark.r = 1 + Math.random() * 1.6;
        }
        const q = (now - spark.t0) / spark.life;
        if (q < 0) {
          el.setAttribute('r', '0');
          return;
        }
        el.setAttribute('cx', (spark.x + spark.dx * q + Math.sin(q * 9 + k) * 3).toFixed(1));
        el.setAttribute('cy', (BASE - 70 - q * 80).toFixed(1));
        el.setAttribute('r', (spark.r * (1 - q)).toFixed(2));
        el.setAttribute('opacity', (1 - q).toFixed(2));
      });
      flareRef.current = Math.max(0, flare - 0.02);
    }

    paintRef.current = paint;

    function frame(now) {
      if (!root.isConnected) return;
      if (visible) paint(now);
      if (!reducedNow) frameId = requestAnimationFrame(frame);
    }

    paint(0);
    if (!reducedNow) frameId = requestAnimationFrame(frame);
    const observer = 'IntersectionObserver' in window
      ? new IntersectionObserver((entries) => {
        visible = entries[0].isIntersecting;
      })
      : null;
    observer?.observe(root);
    return () => {
      cancelAnimationFrame(frameId);
      observer?.disconnect();
    };
  }, [reduced]);

  useEffect(() => {
    if (!reduced) return;
    paintRef.current(0);
  }, [reduced, heat]);

  useEffect(() => {
    if (!open) return undefined;
    dialogRef.current?.focus();
    function onPointer(event) {
      if (rootRef.current && !rootRef.current.contains(event.target)) setOpen(false);
    }
    function onKey(event) {
      if (event.key === 'Escape') {
        event.stopPropagation();
        setOpen(false);
        rootRef.current?.querySelector('button')?.focus();
      }
    }
    document.addEventListener('mousedown', onPointer);
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('mousedown', onPointer);
      document.removeEventListener('keydown', onKey);
    };
  }, [open]);

  const label = `Daily streak, ${streak} day${streak === 1 ? '' : 's'}. Show missions`;

  return (
    <div ref={rootRef} className={completedToday ? 'u379 is-lit' : 'u379'}>
      <button
        type="button"
        className="u379-hit"
        aria-label={label}
        aria-expanded={open}
        aria-haspopup="dialog"
        onClick={() => setOpen((value) => !value)}
      >
        <span className="u379-clip">
          <span ref={flRef} className="u379-fl" aria-hidden="true">
            <svg viewBox="0 0 120 144">
              <defs>
                <linearGradient id={`${uid}o`} x1="0" y1="1" x2="0" y2="0">
                  <stop offset="0" stopColor="#FF7A1A" />
                  <stop offset=".55" stopColor="#F5462A" />
                  <stop offset="1" stopColor="#D9262B" />
                </linearGradient>
                <linearGradient id={`${uid}m`} x1="0" y1="1" x2="0" y2="0">
                  <stop offset="0" stopColor="#FFC24A" />
                  <stop offset="1" stopColor="#FF8A1F" />
                </linearGradient>
                <linearGradient id={`${uid}c`} x1="0" y1="1" x2="0" y2="0">
                  <stop offset="0" stopColor="#FFFBEA" />
                  <stop offset="1" stopColor="#FFE27A" />
                </linearGradient>
              </defs>
              <g className="u379-sp">
                {Array.from({ length: 7 }, (_, index) => (
                  <circle key={index} ref={(node) => { sparkRefs.current[index] = node; }} r="0" />
                ))}
              </g>
              {LAYERS.map((layer, index) => (
                <path
                  key={layer.key}
                  ref={(node) => { pathRefs.current[index] = node; }}
                  className={layer.key === 'o' ? 'u379-fo' : undefined}
                  fill={`url(#${uid}${layer.key})`}
                />
              ))}
            </svg>
          </span>
        </span>
      </button>
      {open && (
        <div
          ref={dialogRef}
          className="u379-panel"
          role="dialog"
          aria-label="Daily missions"
          tabIndex={-1}
          onClick={(event) => event.stopPropagation()}
        >
          <div className="u379-ct" aria-live="polite">
            <span className="u379-nw">
              <span className="u379-ns" style={{ transform: roll ? 'translateY(-44px)' : 'translateY(0)' }}>
                <i>{fromCount}</i>
                <i>{streak}</i>
              </span>
            </span>
            <b>day streak</b>
          </div>
          <div className="u379-missions">
            <div className="u379-mission">
              <MissionDot done={loggedIn} today={nextMission === 'login'} />
              <span>Log in to the page</span>
            </div>
            <div className="u379-mission">
              <MissionDot done={submitDone} today={nextMission === 'submit'} />
              <span>{`Submit ${submitShown}/${submitGoal} times`}</span>
            </div>
            <div className="u379-mission">
              <MissionDot done={petDone} today={nextMission === 'pet'} />
              <span>{`Pet the fox ${petShown}/${petGoal} times`}</span>
            </div>
          </div>
          <p className="u379-note">
            {completedToday
              ? 'Today is complete.'
              : 'Complete all three to earn 50 points. Every 5th day adds 5 more.'}
          </p>
          {broken && (
            <p className="u379-note">
              {canRestore
                ? `Your streak stopped at ${savedStreak}. Restore it once this month, or finish today to start again at day 1.`
                : 'A restore was already used this month. Finish today to start again at day 1.'}
            </p>
          )}
          {broken && canRestore && (
            <button
              type="button"
              className="u379-go"
              disabled={restoring}
              onClick={onRestore}
            >
              Restore streak
            </button>
          )}
        </div>
      )}
    </div>
  );
}
