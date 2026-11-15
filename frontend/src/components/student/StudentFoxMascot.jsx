import React, { useEffect, useRef, useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Mascot, MascotFrame } from './MascotSprite';
import { X } from 'lucide-react';
import { mascotArt, MASCOT_TIERS } from './studentMascots';
import { getRandomDialogue } from './mascotBehaviors';

const MASCOT_EMOJIS = {
  fox: '🦊',
  bear: '🐻',
  panda: '🐼',
  penguin: '🐧',
  bunny: '🐰',
  cat: '🐱',
  vit: '🦆',
  hamster: '🐹',
  chuot_tui: '🦘',
  shiba: '🐕',
  dai_bang: '🦅',
  cho_soi: '🐺',
  thanh_long: '🐉',
  bach_ho: '🐅',
  chu_tuoc: '🪶',
  huyen_vu: '🐢',
  linh_loc: '🦌',
  ki_lan: '🦄',
  zerotwo: '🌸',
  con_bang: '🐋',
  cung_ky: '🦁',
};

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

function FoxCoveringEyes({ size, className, art }) {
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
      aria-label={`${art.name} mascot covering its eyes`}
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
        <MascotFrame src={art.reactions} index={bashfulIndex} size={size} />
      </span>
    </div>
  );
}

export default function StudentFoxMascot({
  size = 56,
  className = '',
  coverEyes = false,
  mascotId = 'fox',
  onPet,
  speech = null,
  onClearSpeech,
}) {
  const art = mascotArt(mascotId);
  const [localSpeech, setLocalSpeech] = useState(null);
  const speechTimerRef = useRef(null);

  const matchingSpeech = speech?.mascotId === mascotId ? speech : null;
  const currentSpeech = matchingSpeech || localSpeech;

  useEffect(() => {
    setLocalSpeech(null);
  }, [mascotId]);

  // Auto clear speech after 5 seconds
  useEffect(() => {
    if (!currentSpeech) return undefined;
    clearTimeout(speechTimerRef.current);
    speechTimerRef.current = setTimeout(() => {
      onClearSpeech?.();
      setLocalSpeech(null);
    }, 5000);
    return () => clearTimeout(speechTimerRef.current);
  }, [currentSpeech, onClearSpeech]);

  if (coverEyes) {
    return <FoxCoveringEyes size={size} className={className} art={art} />;
  }

  const handleContainerClick = () => {
    onClearSpeech?.();
    onPet?.();
    const quote = getRandomDialogue(mascotId, 'pet');
    setLocalSpeech({ text: quote, type: 'pet', mascotId });
  };

  return (
    <div 
      onClick={handleContainerClick}
      className={`relative inline-flex items-center justify-center select-none ${className}`}
    >
      {/* Authentic page-mascot with cursor head tracking & boop reactions */}
      <Mascot
        size={size}
        label={`${art.name} mascot`}
        directions={art.directions}
        reactions={art.reactions}
        trackPointer
        tier={art.tier}
      />

      {/* Interactive Speech Bubble: Floats cleanly BELOW the mascot icon */}
      <AnimatePresence>
        {currentSpeech && (
          <motion.div
            initial={{ opacity: 0, scale: 0.9, y: -6 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.9, y: -6 }}
            transition={{ type: 'spring', damping: 22, stiffness: 320 }}
            className="absolute top-full right-0 mt-3 z-[100] w-72 max-w-[85vw] rounded-2xl border border-border bg-surface p-3 shadow-xl text-left"
          >
            {/* Pointer notch pointing UP towards the mascot */}
            <div className="absolute -top-1.5 right-6 h-3 w-3 rotate-45 border-t border-l border-border bg-surface" />

            <div className="relative flex items-start gap-2.5">
              <span className="text-xl select-none shrink-0" aria-hidden="true">
                {MASCOT_EMOJIS[mascotId] || '🐾'}
              </span>
              <div className="flex-1 min-w-0">
                <div className="flex items-center justify-between gap-1 mb-0.5">
                  <div className="flex items-center gap-1.5">
                    <span className="text-xs font-bold text-foreground">
                      {art.name}
                    </span>
                    {art.tier && (
                      <span className={`text-[9px] font-bold uppercase px-1.5 py-0.2 rounded-full border ${MASCOT_TIERS[art.tier]?.badgeClass || ''}`}>
                        {art.tier}
                      </span>
                    )}
                  </div>
                  <button
                    type="button"
                    onClick={(e) => {
                      e.stopPropagation();
                      onClearSpeech?.();
                      setLocalSpeech(null);
                    }}
                    className="cursor-pointer text-foreground-muted hover:text-foreground p-0.5"
                    aria-label="Close"
                  >
                    <X className="h-3 w-3" />
                  </button>
                </div>
                <p className="text-xs font-semibold text-foreground leading-relaxed">
                  {currentSpeech.text}
                </p>
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
