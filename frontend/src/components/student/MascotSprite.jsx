import { useEffect, useLayoutEffect, useRef, useState } from 'react';

const DIRECTIONS = [
  'up-left',
  'up',
  'up-right',
  'left',
  'center',
  'right',
  'down-left',
  'down',
  'down-right',
];

const REACTIONS = [
  'blink',
  'heart',
  'sparkle',
  'surprised',
  'wink',
  'bashful',
  'sleepy',
  'dizzy',
  'delighted',
];

const REACTIONS_RARE = [
  'focus',
  'celebration',
  'trophy',
  'battle_cry',
  'pro_smirk',
  'fire_spirit',
  'coffee_break',
  'glitch_dizzy',
  'rare_burst',
];

const CLOCKWISE = [
  'right',
  'down-right',
  'down',
  'down-left',
  'left',
  'up-left',
  'up',
  'up-right',
];

const SECTOR = (Math.PI * 2) / CLOCKWISE.length;
const HYSTERESIS = 0.12;
const DEAD_ZONE = 70;
// Normal tier payoffs
const PAYOFFS = ['surprised', 'wink', 'sleepy', 'bashful', 'delighted', 'heart', 'sparkle'];
// Rare tier payoffs: distinctive Coder Adventure & Partner actions
const RARE_PAYOFFS = ['celebration', 'pro_smirk', 'coffee_break', 'battle_cry', 'trophy', 'fire_spirit', 'rare_burst'];

const BOOP_PAYOFF = 100;
const BOOP_END = 720;
const SQUASH_MS = 420;
const DIZZY_AFTER = 4;
const DIZZY_WINDOW = 420;
const DIZZY_END = 1100;
const SQUASH = [
  { transform: 'scale(1, 1)', easing: 'ease-in' },
  { transform: 'scale(1.10, 0.86)', offset: 0.18, easing: 'ease-out' },
  { transform: 'scale(0.95, 1.08)', offset: 0.45, easing: 'ease-in-out' },
  { transform: 'scale(1.03, 0.97)', offset: 0.72, easing: 'ease-in-out' },
  { transform: 'scale(1, 1)' },
];

// Double elastic spring physics with subtle rotation for Rare tier
const SQUASH_RARE = [
  { transform: 'scale(1, 1) rotate(0deg)', easing: 'ease-in' },
  { transform: 'scale(1.14, 0.88) rotate(-2deg)', offset: 0.16, easing: 'ease-out' },
  { transform: 'scale(0.92, 1.10) rotate(2deg)', offset: 0.42, easing: 'ease-in-out' },
  { transform: 'scale(1.05, 0.96) rotate(-1deg)', offset: 0.70, easing: 'ease-in-out' },
  { transform: 'scale(1, 1) rotate(0deg)' },
];

const spriteImages = new Map();

function loadSprite(src) {
  let pending = spriteImages.get(src);
  if (!pending) {
    pending = new Promise((resolve, reject) => {
      const image = new Image();
      image.onload = () => resolve(image);
      image.onerror = () => {
        spriteImages.delete(src);
        reject(new Error('mascot sheet failed'));
      };
      image.src = src;
    });
    spriteImages.set(src, pending);
  }
  return pending;
}

function useSprite(src, enabled = true) {
  const [image, setImage] = useState(null);
  useEffect(() => {
    if (!enabled || !src) return undefined;
    let cancelled = false;
    loadSprite(src).then((loaded) => {
      if (!cancelled) setImage(loaded);
    }).catch(() => {});
    return () => {
      cancelled = true;
    };
  }, [src, enabled]);
  return image;
}

function cellStyle(index) {
  return {
    backgroundPosition: `${(index % 3) * 50}% ${Math.floor(index / 3) * 50}%`,
  };
}

function paintCell(canvas, image, index, size) {
  if (index < 0 || !image?.naturalWidth) return;
  const dpr = Math.min(window.devicePixelRatio || 1, 3);
  const px = Math.max(1, Math.round(size * dpr));
  if (canvas.width !== px || canvas.height !== px) {
    canvas.width = px;
    canvas.height = px;
  }
  const ctx = canvas.getContext('2d');
  ctx.clearRect(0, 0, px, px);
  ctx.imageSmoothingEnabled = true;
  ctx.imageSmoothingQuality = 'high';
  const cell = image.naturalWidth / 3;
  const col = index % 3;
  const row = Math.floor(index / 3);
  ctx.drawImage(
    image,
    Math.round(col * cell),
    Math.round(row * cell),
    cell,
    cell,
    0,
    0,
    px,
    px,
  );
}

function SpriteCanvas({ image, index, size, visible }) {
  const ref = useRef(null);
  useLayoutEffect(() => {
    if (!visible || !image || !ref.current) return;
    paintCell(ref.current, image, index, size);
  }, [image, index, size, visible]);
  return (
    <canvas
      ref={ref}
      aria-hidden="true"
      style={{
        position: 'absolute',
        inset: 0,
        width: '100%',
        height: '100%',
        opacity: visible && image ? 1 : 0,
        pointerEvents: 'none',
      }}
    />
  );
}

function wrap(angle) {
  return Math.atan2(Math.sin(angle), Math.cos(angle));
}

/**
 * Same head-tracking and boop as page-mascot. Cells are painted on a canvas at
 * the screen's pixel ratio, because a CSS background scaled into the header
 * looks soft on every sheet.
 */
export function Mascot({
  directions,
  reactions,
  size = 140,
  className,
  label = 'mascot',
  trackPointer = false,
  interactive = true,
  tier = 'normal',
}) {
  const buttonRef = useRef(null);
  const squashRef = useRef(null);
  const timersRef = useRef([]);
  const boopsRef = useRef({ rapid: 0, at: 0, step: 0 });
  const [direction, setDirection] = useState('center');
  const [reaction, setReaction] = useState(null);
  const followPointer = interactive && trackPointer;
  const directionImage = useSprite(directions);
  const reactionImage = useSprite(reactions, interactive && (followPointer || reaction != null));

  useEffect(() => {
    if (!followPointer) return undefined;
    if (!window.matchMedia('(hover: hover) and (pointer: fine)').matches) {
      return undefined;
    }
    let sector = -1;
    let pointer = null;
    const aim = () => {
      const button = buttonRef.current;
      if (!button || !pointer) return;
      const box = button.getBoundingClientRect();
      const dx = pointer.x - (box.left + box.width / 2);
      const dy = pointer.y - (box.top + box.height / 2);
      if (Math.hypot(dx, dy) < DEAD_ZONE) {
        if (sector !== -1) {
          sector = -1;
          setDirection('center');
        }
        return;
      }
      const angle = Math.atan2(dy, dx);
      if (sector !== -1 && Math.abs(wrap(angle - sector * SECTOR)) < SECTOR / 2 + HYSTERESIS) {
        return;
      }
      sector = (Math.round(angle / SECTOR) + CLOCKWISE.length) % CLOCKWISE.length;
      setDirection(CLOCKWISE[sector]);
    };
    const onPointerMove = (event) => {
      pointer = { x: event.clientX, y: event.clientY };
      aim();
    };
    window.addEventListener('pointermove', onPointerMove, { passive: true });
    window.addEventListener('scroll', aim, { passive: true });
    return () => {
      window.removeEventListener('pointermove', onPointerMove);
      window.removeEventListener('scroll', aim);
    };
  }, [followPointer]);

  useEffect(() => () => {
    timersRef.current.forEach(window.clearTimeout);
  }, []);

  const boop = () => {
    if (!interactive) return;
    timersRef.current.forEach(window.clearTimeout);
    timersRef.current = [];
    const later = (ms, next) => {
      timersRef.current.push(window.setTimeout(() => setReaction(next), ms));
    };
    const now = Date.now();
    const boops = boopsRef.current;
    boops.rapid = now - boops.at < DIZZY_WINDOW ? boops.rapid + 1 : 1;
    boops.at = now;

    const isRare = tier === 'rare';
    const activePayoffs = isRare ? RARE_PAYOFFS : PAYOFFS;
    const dizzyReaction = isRare ? 'glitch_dizzy' : 'dizzy';
    const blinkReaction = isRare ? 'focus' : 'blink';
    const squashAnim = isRare ? SQUASH_RARE : SQUASH;
    const squashDuration = isRare ? 400 : SQUASH_MS;

    if (boops.rapid >= DIZZY_AFTER) {
      boops.rapid = 0;
      setReaction(dizzyReaction);
      later(DIZZY_END, null);
    } else {
      const payoff = activePayoffs[boops.step % activePayoffs.length];
      boops.step += 1;
      setReaction(blinkReaction);
      later(BOOP_PAYOFF, payoff);
      later(BOOP_END, null);
    }
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;
    squashRef.current?.animate(squashAnim, { duration: squashDuration, easing: 'linear' });
  };

  const isRare = tier === 'rare';
  const activeReactions = isRare ? REACTIONS_RARE : REACTIONS;
  const directionIndex = interactive ? DIRECTIONS.indexOf(direction) : DIRECTIONS.indexOf('center');
  const reactionIndex = reaction != null
    ? (activeReactions.indexOf(reaction) !== -1
        ? activeReactions.indexOf(reaction)
        : (REACTIONS.indexOf(reaction) !== -1
            ? REACTIONS.indexOf(reaction)
            : (REACTIONS_RARE.indexOf(reaction) !== -1 ? REACTIONS_RARE.indexOf(reaction) : 0)))
    : 0;
  const boxStyle = {
    position: 'relative',
    display: 'block',
    flexShrink: 0,
    width: size,
    height: size,
    padding: 0,
    border: 0,
    background: 'transparent',
    appearance: 'none',
    userSelect: 'none',
  };
  const portrait = (
    <span
      ref={squashRef}
      style={{
        position: 'relative',
        display: 'block',
        width: '100%',
        height: '100%',
        transformOrigin: '50% 78%',
      }}
    >
      <SpriteCanvas image={directionImage} index={directionIndex} size={size} visible={!interactive || !reaction || !reactionImage} />
      {interactive && (
        <SpriteCanvas image={reactionImage} index={reactionIndex} size={size} visible={Boolean(reaction) && Boolean(reactionImage)} />
      )}
      {!directionImage && (
        <span
          aria-hidden="true"
          style={{
            position: 'absolute',
            inset: 0,
            backgroundImage: `url(${directions})`,
            backgroundSize: '300% 300%',
            backgroundRepeat: 'no-repeat',
            ...cellStyle(directionIndex),
          }}
        />
      )}
    </span>
  );

  if (!interactive) {
    return (
      <div
        role="img"
        aria-label={label}
        className={className}
        style={boxStyle}
      >
        {portrait}
      </div>
    );
  }

  return (
    <button
      ref={buttonRef}
      type="button"
      onClick={boop}
      aria-label={`Boop the ${label}`}
      className={className}
      style={{ ...boxStyle, cursor: 'pointer' }}
    >
      {portrait}
    </button>
  );
}

export function MascotFrame({ src, index, size }) {
  const image = useSprite(src);
  return (
    <span style={{ position: 'absolute', inset: 0 }}>
      {!image && (
        <span
          aria-hidden="true"
          style={{
            position: 'absolute',
            inset: 0,
            backgroundImage: `url(${src})`,
            backgroundSize: '300% 300%',
            backgroundRepeat: 'no-repeat',
            ...cellStyle(index),
          }}
        />
      )}
      <SpriteCanvas image={image} index={index} size={size} visible />
    </span>
  );
}
