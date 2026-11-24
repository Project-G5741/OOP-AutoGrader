import React from 'react';

/**
 * 1. HALLOWEEN (31/10) — 2K HD User Background Design
 * Renders `/holidays/halloween-bg.webp` with glowing pumpkin-lantern & string-light halos
 * and rolling ground mist. (Corner cobwebs, dangling spider & flying bats run session-wide
 * via SessionHolidayEffects).
 */
export default function HalloweenScene() {
  return (
    <div className="holiday-scene-root" aria-hidden="true">
      <img
        src="/holidays/halloween-bg.webp"
        alt=""
        className="holiday-scene-bg-img"
        decoding="async"
      />
      <div className="holiday-lights-layer">
        {[
          { left: '11%', top: '8%', delay: '0s' },
          { left: '7.5%', top: '24%', delay: '0.7s' },
          { left: '28%', top: '2%', delay: '1.2s' },
          { left: '65%', top: '3.5%', delay: '0.4s' },
          { left: '78.5%', top: '13%', delay: '0.9s' },
          { left: '96.5%', top: '30%', delay: '1.5s' },
          { left: '94.5%', top: '70%', delay: '0.3s' },
        ].map((orb, idx) => (
          <span
            key={idx}
            className="holiday-twinkle-orb"
            style={{
              left: orb.left,
              top: orb.top,
              animationDelay: orb.delay,
            }}
          />
        ))}
      </div>
      <div className="holiday-fog-layer">
        <div className="holiday-fog-band holiday-fog-band--a" />
        <div className="holiday-fog-band holiday-fog-band--b" />
      </div>
    </div>
  );
}
