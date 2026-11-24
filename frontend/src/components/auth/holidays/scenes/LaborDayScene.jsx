import React from 'react';

/**
 * 5. TẾT LAO ĐỘNG & THỐNG NHẤT (30/04 – 01/05) — User Reference Background Design
 * Renders the user's custom Quốc Tế Lao Động 1/5 illustration (`/holidays/reunification-labor-bg.webp`)
 * with warm sunlit highlights.
 * (Fluttering crimson-gold petals & stars run session-wide via SessionHolidayEffects).
 */
export default function LaborDayScene() {
  return (
    <div className="holiday-scene-root" aria-hidden="true">
      <img
        src="/holidays/reunification-labor-bg.webp"
        alt=""
        className="holiday-scene-bg-img"
        decoding="async"
      />
    </div>
  );
}
