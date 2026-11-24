import React from 'react';

/**
 * 6. TẾT THIẾU NHI (01/06) — 2K HD User Background Design
 * Renders `/holidays/childrens-day-bg.webp` with warm sunbeam & star highlights.
 * (Floating bubbles, paper airplanes & pastel confetti run session-wide via SessionHolidayEffects).
 */
export default function ChildrensDayScene() {
  return (
    <div className="holiday-scene-root" aria-hidden="true">
      <img
        src="/holidays/childrens-day-bg.webp"
        alt=""
        className="holiday-scene-bg-img"
        decoding="async"
      />
      <div className="holiday-lights-layer">
        {[
          { left: '88%', top: '11%', delay: '0s' },
          { left: '28%', top: '61.5%', delay: '0.8s' },
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
