import React from 'react';

/**
 * 2. NOEL / GIÁNG SINH (24/12 – 25/12) — User Reference Background Design
 * Renders the user's custom Noel winter wonderland illustration (`/holidays/christmas-bg.webp`)
 * with subtle twinkling fairy-light halos along the top garlands and Christmas tree.
 * (Falling snow animation runs session-wide via SessionHolidayEffects).
 */
export default function ChristmasScene() {
  return (
    <div className="holiday-scene-root" aria-hidden="true">
      <img
        src="/holidays/christmas-bg.webp"
        alt=""
        className="holiday-scene-bg-img"
        decoding="async"
      />
      <div className="holiday-lights-layer">
        {[
          { left: '14%', top: '5%', delay: '0s' },
          { left: '22%', top: '4%', delay: '0.7s' },
          { left: '77%', top: '4%', delay: '1.2s' },
          { left: '84%', top: '6%', delay: '0.4s' },
          { left: '88%', top: '52%', delay: '1.5s' },
          { left: '92%', top: '64%', delay: '0.9s' },
          { left: '93%', top: '34%', delay: '0.2s' },
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
