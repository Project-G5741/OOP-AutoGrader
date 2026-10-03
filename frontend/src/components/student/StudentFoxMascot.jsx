import { useEffect, useRef } from 'react';
import { Mascot } from 'page-mascot';

const FOX_DIRECTIONS = '/mascots/fox-directions.webp';
const FOX_REACTIONS = '/mascots/fox-reactions.webp';

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

function spritePosition(index) {
  return `${(index % 3) * 50}% ${Math.floor(index / 3) * 50}%`;
}

function FoxCoveringEyes({ size, className }) {
  const squashRef = useRef(null);
  const bashfulIndex = REACTIONS.indexOf('bashful');

  useEffect(() => {
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      return;
    }
    squashRef.current?.animate(
      [
        { transform: 'scale(1, 1)', easing: 'ease-in' },
        { transform: 'scale(1.08, 0.9)', offset: 0.2, easing: 'ease-out' },
        { transform: 'scale(1, 1)' },
      ],
      { duration: 360, easing: 'linear' },
    );
  }, []);

  return (
    <div
      className={className}
      role="img"
      aria-label="Fox mascot covering its eyes"
      style={{
        position: 'relative',
        display: 'block',
        flexShrink: 0,
        width: size,
        height: size,
      }}
    >
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
        <span
          style={{
            position: 'absolute',
            inset: 0,
            backgroundImage: `url(${FOX_REACTIONS})`,
            backgroundSize: '300% 300%',
            backgroundRepeat: 'no-repeat',
            backgroundPosition: spritePosition(bashfulIndex),
          }}
        />
      </span>
    </div>
  );
}

export default function StudentFoxMascot({ size = 88, className = '', coverEyes = false }) {
  if (coverEyes) {
    return <FoxCoveringEyes size={size} className={className} />;
  }

  return (
    <Mascot
      className={className}
      size={size}
      label="Fox mascot"
      directions={FOX_DIRECTIONS}
      reactions={FOX_REACTIONS}
    />
  );
}
