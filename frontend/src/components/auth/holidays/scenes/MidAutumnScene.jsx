import React from 'react';

/**
 * 7. TẾT TRUNG THU (15/08 ÂL) — User Reference Background Design
 * Renders the user's custom Tết Trung Thu moonlit riverside illustration (`/holidays/mid-autumn-bg.webp`)
 * with warm glowing star-lantern and sky-lantern halos.
 * (Rising sky lanterns & twinkling stars run session-wide via SessionHolidayEffects).
 */
export default function MidAutumnScene() {
  return (
    <div className="holiday-scene-root" aria-hidden="true">
      <img
        src="/holidays/mid-autumn-bg.webp"
        alt=""
        className="holiday-scene-bg-img"
        decoding="async"
      />
      <div className="holiday-lights-layer">
        {[
          { left: '9%', top: '18%', delay: '0s' },
          { left: '19%', top: '22%', delay: '0.6s' },
          { left: '31%', top: '19%', delay: '1.1s' },
          { left: '29%', top: '59%', delay: '0.3s' },
          { left: '67%', top: '9%', delay: '1.4s' },
          { left: '92%', top: '29%', delay: '0.8s' },
          { left: '84%', top: '63%', delay: '0.5s' },
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
