import React from 'react';

/**
 * 3. TẾT DƯƠNG LỊCH (01/01) — 2K HD User Background Design
 * Renders `/holidays/new-year-bg.webp` with twinkling string-light, firework, and
 * streetlamp halos. (Falling snow & golden firework stars run session-wide via SessionHolidayEffects).
 */
export default function NewYearScene() {
  return (
    <div className="holiday-scene-root" aria-hidden="true">
      <img
        src="/holidays/new-year-bg.webp"
        alt=""
        className="holiday-scene-bg-img"
        decoding="async"
      />
      <div className="holiday-lights-layer">
        {[
          { left: '16.5%', top: '5.5%', delay: '0s' },
          { left: '25.5%', top: '2%', delay: '0.6s' },
          { left: '41%', top: '13%', delay: '1.1s' },
          { left: '71%', top: '4%', delay: '0.3s' },
          { left: '76%', top: '20%', delay: '0.9s' },
          { left: '89.5%', top: '1.5%', delay: '1.4s' },
          { left: '94.5%', top: '29%', delay: '0.5s' },
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
    </div>
  );
}
