import React from 'react';
import { isDesktopLocalMode } from '../../../utils/desktopMode';
import { useActiveHoliday } from '../../../utils/holidayCalendar';
import { getHolidayBackgroundEntry } from './registry';
import { HolidayParticleLayer, PaintedBlossomCluster } from './sharedPrimitives';

/**
 * Session-Wide Holiday Ambient Animation Layer (`SessionHolidayEffects`).
 * Mounted at the root of `App.jsx` so ambient seasonal animations (falling snow,
 * corner cobwebs with dangling spider & flying bats, drifting Hoa Mai / Hoa Đào petals,
 * rising sky lanterns, paper airplanes, and star confetti) persist throughout the
 * entire web session, while the full holiday background and popup styling remain
 * strictly on the Login / Auth screens.
 */
export default function SessionHolidayEffects() {
  const activeHoliday = useActiveHoliday();

  if (isDesktopLocalMode() || !activeHoliday) {
    return null;
  }

  const entry = getHolidayBackgroundEntry(activeHoliday.id);
  const effects = entry.sessionEffects;
  if (!effects) {
    return null;
  }

  return (
    <div
      className="session-holiday-effects"
      data-session-holiday={entry.id}
      aria-hidden="true"
    >
      {/* 1. Viewport Corner Ambient Ornaments (Cobwebs & Dangling Spider for Halloween, etc.) */}
      {effects.cornerEffect === 'cobwebs' && (
        <svg
          className="session-holiday-corner-svg"
          viewBox="0 0 1600 900"
          preserveAspectRatio="none"
        >
          {/* Top-left cobweb */}
          <g stroke="#CBD5E1" strokeWidth="1.4" fill="none" opacity="0.45">
            <path d="M0,0 L215,30 M0,0 L185,92 M0,0 L135,148 M0,0 L72,185 M0,0 L20,205" />
            <path d="M56,9 Q48,26 40,30 Q30,42 20,50 Q10,54 4,58" />
            <path d="M112,18 Q96,46 80,54 Q62,74 45,88 Q26,98 8,105" />
            <path d="M168,25 Q145,68 122,80 Q96,112 70,132 Q40,148 14,158" />
          </g>
          {/* Top-right cobweb + Cute animated dangling spider on silk thread */}
          <g
            transform="translate(1600, 0) scale(-1, 1)"
            stroke="#CBD5E1"
            strokeWidth="1.4"
            fill="none"
            opacity="0.45"
          >
            <path d="M0,0 L215,30 M0,0 L185,92 M0,0 L135,148 M0,0 L72,185 M0,0 L20,205" />
            <path d="M56,9 Q48,26 40,30 Q30,42 20,50 Q10,54 4,58" />
            <path d="M112,18 Q96,46 80,54 Q62,74 45,88 Q26,98 8,105" />
            <path d="M168,25 Q145,68 122,80 Q96,112 70,132 Q40,148 14,158" />
          </g>
          {/* Cute dangling spider on top-right matching user's Halloween artwork */}
          <g className="holiday-spider-dangle" transform="translate(1475, 0)">
            <path d="M0,0 L0,115" stroke="#E2E8F0" strokeWidth="1.4" opacity="0.65" />
            <g transform="translate(0, 118)">
              {/* Spider legs */}
              <path
                d="M-6,-2 C-16,-10 -24,-6 -26,2 M-8,2 C-18,-2 -26,4 -27,12 M-6,6 C-16,6 -22,12 -22,20 M6,-2 C16,-10 24,-6 26,2 M8,2 C18,-2 26,4 27,12 M6,6 C16,6 22,12 22,20"
                fill="none"
                stroke="#0F172A"
                strokeWidth="2.4"
                strokeLinecap="round"
              />
              {/* Spider body & big cute eyes */}
              <path d="M-11,2 a11,10 0 1,0 22,0 a11,10 0 1,0 -22,0 Z" fill="#0F172A" />
              <path d="M-6,2 a2.8,2.8 0 1,0 5.6,0 a2.8,2.8 0 1,0 -5.6,0 M0.4,2 a2.8,2.8 0 1,0 5.6,0 a2.8,2.8 0 1,0 -5.6,0" fill="#FFFFFF" />
              <path d="M-4,2 a1.2,1.2 0 1,0 2.4,0 a1.2,1.2 0 1,0 -2.4,0 M2,2 a1.2,1.2 0 1,0 2.4,0 a1.2,1.2 0 1,0 -2.4,0" fill="#0F172A" />
            </g>
          </g>
        </svg>
      )}

      {effects.cornerEffect === 'tet-blossom' && (
        <svg
          className="session-holiday-corner-svg"
          viewBox="0 0 1600 900"
          preserveAspectRatio="none"
        >
          <g opacity="0.6">
            <PaintedBlossomCluster x={28} y={24} scale={0.75} />
            <PaintedBlossomCluster
              x={1572}
              y={24}
              scale={0.75}
              petalColor="#F472B6"
              innerColor="#BE185D"
              stamenColor="#FCE7F3"
            />
          </g>
        </svg>
      )}

      {/* 2. Continuous Session-Wide Particle Layer (Snow, Petals, Sky Lanterns, Bats, Paper Airplanes, Stars) */}
      <HolidayParticleLayer
        type={effects.particleType}
        colors={effects.particleColors}
        rising={effects.rising}
      />
    </div>
  );
}
