import React from 'react';

/**
 * 4. TẾT NGUYÊN ĐÁN (29/12 ÂL – 05/01 ÂL) — User Reference Background Design
 * Renders the user's custom Tết Nguyên Đán spring courtyard illustration (`/holidays/tet-nguyen-dan-bg.webp`)
 * with warm glowing lantern halos.
 * (Drifting Hoa Mai & Hoa Đào petals run session-wide via SessionHolidayEffects).
 */
export default function TetNguyenDanScene() {
  return (
    <div className="holiday-scene-root" aria-hidden="true">
      <img
        src="/holidays/tet-nguyen-dan-bg.webp"
        alt=""
        className="holiday-scene-bg-img"
        decoding="async"
      />
      <div className="holiday-lights-layer">
        {[
          { left: '22%', top: '16%', delay: '0s' },
          { left: '19%', top: '28%', delay: '0.9s' },
          { left: '89%', top: '18%', delay: '0.4s' },
          { left: '96%', top: '24%', delay: '1.3s' },
          { left: '93%', top: '65%', delay: '0.6s' },
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
