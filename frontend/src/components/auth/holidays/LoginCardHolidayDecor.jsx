import React from 'react';
import {
  AtmosphericAura,
  HoiAnLantern2D,
  PaintedBlossomCluster,
  PaintedLotus2D,
} from './sharedPrimitives';

/**
 * 2D Holiday Decorative Ornaments for the Login Pop-up Card (`.login-unified-card`)
 * and the Login Logo Box (`.login-logo-holiday-wrap`).
 * Matched directly to the visual motifs of the 7 holiday background designs.
 * Strictly Login-only ("Theme chỉ có ngoài login thôi").
 */

export function LogoHolidayAccessory({ holidayId }) {
  if (!holidayId || holidayId === 'default') return null;

  if (holidayId === 'christmas') {
    // Red & White Santa Hat perched on the top-right corner of the logo box
    return (
      <svg
        className="login-logo-accessory"
        viewBox="0 0 64 64"
        aria-hidden="true"
      >
        <path
          d="M12,34 C18,12 42,8 54,24 C44,24 36,28 28,36 Z"
          fill="#DC2626"
        />
        <path
          d="M8,34 C20,28 34,30 46,38 C42,44 14,42 8,34 Z"
          fill="#FFFFFF"
        />
        <path
          d="M49,24 a6,6 0 1,0 12,0 a6,6 0 1,0 -12,0 Z"
          fill="#FFFFFF"
        />
      </svg>
    );
  }

  if (holidayId === 'new-year') {
    // Festive Black Top Hat with Red Ribbon & Holly Berry (matching the New Year 2026 Snowman)
    return (
      <svg
        className="login-logo-accessory"
        viewBox="0 0 64 64"
        aria-hidden="true"
      >
        <g transform="rotate(14 34 28)">
          <path d="M18,10 L46,10 L44,32 L20,32 Z" fill="#1E293B" />
          <path d="M19,25 L45,25 L44,31 L20,31 Z" fill="#DC2626" />
          <path
            d="M10,32 C24,29 40,29 54,32 C50,37 14,37 10,32 Z"
            fill="#0F172A"
          />
          {/* Holly leaf & red berry on the hat band */}
          <path d="M22,25 C16,19 18,14 24,20 Z M24,25 C26,18 31,17 28,24 Z" fill="#16A34A" />
          <path d="M21,25 a2.5,2.5 0 1,0 5,0 a2.5,2.5 0 1,0 -5,0 Z" fill="#EF4444" />
        </g>
      </svg>
    );
  }

  if (holidayId === 'tet-nguyen-dan') {
    // Blooming Golden Hoa Mai & Crimson Tassel on the logo corner
    return (
      <svg
        className="login-logo-accessory"
        viewBox="0 0 64 64"
        aria-hidden="true"
      >
        <path
          d="M44,34 C46,44 45,52 43,60 M48,34 C50,44 51,52 52,60"
          fill="none"
          stroke="#EF4444"
          strokeWidth="2.5"
          strokeLinecap="round"
        />
        <PaintedBlossomCluster x={44} y={24} scale={0.78} />
      </svg>
    );
  }

  if (holidayId === 'mid-autumn') {
    // Cute White Jade Rabbit (Thỏ Ngọc) Ears & Golden Star on the logo box
    return (
      <svg
        className="login-logo-accessory"
        viewBox="0 0 64 64"
        aria-hidden="true"
      >
        <path
          d="M22,36 C16,18 20,6 28,10 C32,14 30,26 26,36 Z"
          fill="#FFFFFF"
          stroke="#F59E0B"
          strokeWidth="1.5"
        />
        <path
          d="M23,32 C20,20 22,12 26,14 C28,17 27,25 25,32 Z"
          fill="#FDA4AF"
        />
        <path
          d="M34,36 C36,18 44,8 50,14 C52,20 44,30 38,36 Z"
          fill="#FFFFFF"
          stroke="#F59E0B"
          strokeWidth="1.5"
        />
        <path
          d="M36,32 C38,20 43,14 47,17 C48,21 42,28 38,32 Z"
          fill="#FDA4AF"
        />
      </svg>
    );
  }

  if (holidayId === 'reunification-labor') {
    // Radiant 5-Pointed Golden Star & Crimson Ribbon Rosette on the logo corner
    return (
      <svg
        className="login-logo-accessory"
        viewBox="0 0 64 64"
        aria-hidden="true"
      >
        <path
          d="M38,32 L33,52 L40,47 L45,54 L46,34 Z"
          fill="#DC2626"
        />
        <path
          d="M28,24 a14,14 0 1,0 28,0 a14,14 0 1,0 -28,0 Z"
          fill="#B91C1C"
          stroke="#FACC15"
          strokeWidth="2"
        />
        <path
          d="M42,13 L44.6,20.5 L52.5,20.5 L46,25 L48.5,32.5 L42,28 L35.5,32.5 L38,25 L31.5,20.5 L39.4,20.5 Z"
          fill="#FEF08A"
        />
      </svg>
    );
  }

  if (holidayId === 'halloween') {
    // Crooked Witch Hat with Orange Band & Golden Buckle (matching user's Halloween pumpkin)
    return (
      <svg
        className="login-logo-accessory"
        viewBox="0 0 64 64"
        aria-hidden="true"
      >
        <path
          d="M16,36 C24,18 34,8 54,8 C44,18 42,26 46,38 Z"
          fill="#1E1B4B"
        />
        <path
          d="M18,32 C28,30 38,31 46,35"
          fill="none"
          stroke="#F97316"
          strokeWidth="5"
          strokeLinecap="round"
        />
        <path
          d="M29,29 L36,30 L35,36 L28,35 Z"
          fill="none"
          stroke="#FACC15"
          strokeWidth="2"
        />
        <path
          d="M6,36 C22,30 42,32 58,40 C40,44 20,42 6,36 Z"
          fill="#311042"
        />
      </svg>
    );
  }

  if (holidayId === 'childrens-day') {
    // Colorful 4-Blade Pinwheel & Golden Star
    return (
      <svg
        className="login-logo-accessory"
        viewBox="0 0 64 64"
        aria-hidden="true"
      >
        <g transform="translate(42, 24)">
          <path d="M0,0 C6,-14 16,-12 14,0 Z" fill="#EF4444" />
          <path d="M0,0 C14,6 12,16 0,14 Z" fill="#FACC15" />
          <path d="M0,0 C-6,14 -16,12 -14,0 Z" fill="#38BDF8" />
          <path d="M0,0 C-14,-6 -12,-16 0,-14 Z" fill="#4ADE80" />
          <path d="M-3,0 a3,3 0 1,0 6,0 a3,3 0 1,0 -6,0 Z" fill="#FFFFFF" />
        </g>
      </svg>
    );
  }

  return null;
}

/**
 * Corner & Rim Ornaments rendered directly on `.login-unified-card` so the
 * login pop-up feels custom-built for the active holiday.
 */
export default function LoginCardHolidayDecor({ holidayId }) {
  if (!holidayId || holidayId === 'default') return null;

  return (
    <div className="login-card-holiday-decor" aria-hidden="true">
      {holidayId === 'christmas' && (
        <svg className="login-card-decor-svg" viewBox="0 0 960 540" preserveAspectRatio="none">
          {/* Soft snow cushions along the top edge of the popup card */}
          <path
            d="M0,26 C0,8 22,0 55,0 L245,0 C225,14 185,18 145,14 C95,22 45,16 0,26 Z"
            fill="#FFFFFF"
            opacity="0.95"
          />
          <path
            d="M960,26 C960,8 938,0 905,0 L715,0 C735,14 775,18 815,14 C865,22 915,16 960,26 Z"
            fill="#FFFFFF"
            opacity="0.95"
          />
          {/* Top-left Pine & Holly Berry sprig */}
          <path
            d="M0,0 L125,0 C98,18 68,26 38,34 C22,52 10,74 0,98 Z"
            fill="#15803D"
          />
          <path
            d="M0,0 L88,0 C68,14 46,22 24,28 C14,42 6,56 0,72 Z"
            fill="#166534"
          />
          {/* Red Holly Berries & Ribbon Bow at top-left corner */}
          <path d="M36,16 C22,4 16,22 34,22 Z M38,16 C52,4 58,22 40,22 Z" fill="#DC2626" />
          <path
            d="M32,20 a5,5 0 1,0 10,0 a5,5 0 1,0 -10,0 M26,26 a4.5,4.5 0 1,0 9,0 a4.5,4.5 0 1,0 -9,0 M38,26 a4.5,4.5 0 1,0 9,0 a4.5,4.5 0 1,0 -9,0"
            fill="#EF4444"
          />
          {/* Top-right Pine sprig & hanging golden star + bauble */}
          <path
            d="M960,0 L835,0 C862,18 892,26 922,34 C938,52 950,74 960,98 Z"
            fill="#15803D"
          />
          <path d="M912,24 L912,62" stroke="#D97706" strokeWidth="1.8" />
          <path
            d="M912,48 L916,59 L927,63 L916,67 L912,78 L908,67 L897,63 L908,59 Z"
            fill="#FACC15"
          />
          <path d="M936,18 L936,44" stroke="#D97706" strokeWidth="1.8" />
          <path d="M927,52 a9,9 0 1,0 18,0 a9,9 0 1,0 -18,0 Z" fill="#DC2626" />
        </svg>
      )}

      {holidayId === 'new-year' && (
        <svg className="login-card-decor-svg" viewBox="0 0 960 540" preserveAspectRatio="none">
          {/* Soft winter snow caps along top rim + red velvet bow & golden firework stars */}
          <path
            d="M0,24 C0,8 22,0 55,0 L215,0 C195,12 155,16 115,12 C75,20 35,14 0,24 Z"
            fill="#FFFFFF"
            opacity="0.95"
          />
          <path
            d="M960,24 C960,8 938,0 905,0 L745,0 C765,12 805,16 845,12 C885,20 925,14 960,24 Z"
            fill="#FFFFFF"
            opacity="0.95"
          />
          {/* Top-left snow-dusted pine & glowing bulb string */}
          <path d="M0,0 L110,0 C82,16 54,24 28,32 C16,46 6,62 0,80 Z" fill="#1E3A8A" />
          <path d="M0,38 Q65,42 125,0" fill="none" stroke="#F59E0B" strokeWidth="1.8" />
          <path d="M44,26 a5,5 0 1,0 10,0 a5,5 0 1,0 -10,0 M84,14 a5,5 0 1,0 10,0 a5,5 0 1,0 -10,0" fill="#FEF08A" />
          {/* Top-right Red Velvet Ribbon Bow & Golden Firework Sparkles */}
          <g transform="translate(918, 26)">
            <path d="M0,0 L-14,26 L-4,22 L2,28 Z M0,0 L14,26 L4,22 L-2,28 Z" fill="#B91C1C" />
            <path d="M0,0 C-22,-14 -24,12 -2,6 Z M0,0 C22,-14 24,12 2,6 Z" fill="#DC2626" />
            <path d="M-4,0 a4,4 0 1,0 8,0 a4,4 0 1,0 -8,0 Z" fill="#991B1B" />
          </g>
          <path
            d="M865,24 L868,34 L878,37 L868,40 L865,50 L862,40 L852,37 L862,34 Z"
            fill="#FACC15"
          />
        </svg>
      )}

      {holidayId === 'tet-nguyen-dan' && (
        <svg className="login-card-decor-svg" viewBox="0 0 960 540" preserveAspectRatio="none">
          {/* Top-left Hoa Mai branch & hanging red lantern */}
          <path
            d="M0,78 C28,56 58,34 112,18 C82,36 52,54 24,84"
            fill="none"
            stroke="#5C3A21"
            strokeWidth="5"
            strokeLinecap="round"
          />
          <PaintedBlossomCluster x={34} y={42} scale={0.78} />
          <PaintedBlossomCluster x={72} y={24} scale={0.88} />
          <PaintedBlossomCluster x={108} y={18} scale={0.65} />
          <HoiAnLantern2D x={54} y={36} scale={0.56} color="#DC2626" />

          {/* Top-right Pink Hoa Đào branch & lantern */}
          <path
            d="M960,78 C932,56 902,34 848,18 C878,36 908,54 936,84"
            fill="none"
            stroke="#5C3A21"
            strokeWidth="5"
            strokeLinecap="round"
          />
          <PaintedBlossomCluster
            x={926}
            y={42}
            scale={0.78}
            petalColor="#F472B6"
            innerColor="#BE185D"
            stamenColor="#FCE7F3"
          />
          <PaintedBlossomCluster
            x={888}
            y={24}
            scale={0.88}
            petalColor="#F472B6"
            innerColor="#BE185D"
            stamenColor="#FCE7F3"
          />
          <HoiAnLantern2D x={906} y={36} scale={0.56} color="#EA580C" />

          {/* Bottom-left & bottom-right golden cloud corner filigree */}
          <path
            d="M14,502 L14,526 L38,526 M22,510 L22,518 L30,518 M946,502 L946,526 L922,526 M938,510 L938,518 L930,518"
            fill="none"
            stroke="#D97706"
            strokeWidth="2"
            strokeLinecap="round"
            opacity="0.65"
          />
        </svg>
      )}

      {holidayId === 'mid-autumn' && (
        <svg className="login-card-decor-svg" viewBox="0 0 960 540" preserveAspectRatio="none">
          {/* Top-left Banyan branch & glowing mini 5-pointed Star Lantern (Đèn Ông Sao) */}
          <path
            d="M0,48 C35,34 74,22 122,12"
            fill="none"
            stroke="#5C3A21"
            strokeWidth="5"
            strokeLinecap="round"
          />
          <PaintedBlossomCluster x={38} y={32} scale={0.62} />
          <PaintedBlossomCluster x={86} y={18} scale={0.58} />
          <path d="M58,26 L58,54" stroke="#B45309" strokeWidth="1.8" />
          <g transform="translate(58, 72)">
            <AtmosphericAura cx={0} cy={0} r={24} color="#F59E0B" intensity={0.95} />
            <path
              d="M-19,0 a19,19 0 1,0 38,0 a19,19 0 1,0 -38,0 Z"
              fill="none"
              stroke="#D97706"
              strokeWidth="2"
            />
            <path
              d="M0,-18 L4.5,-5 L18,-5 L7,3 L11,16 L0,8 L-11,16 L-7,3 L-18,-5 L-4.5,-5 Z"
              fill="#DC2626"
              stroke="#FEF08A"
              strokeWidth="1.4"
            />
            <path d="M-4,0 a4,4 0 1,0 8,0 a4,4 0 1,0 -8,0 Z" fill="#FEF08A" />
          </g>

          {/* Top-right hanging glowing Mid-Autumn silk lantern */}
          <path
            d="M960,48 C925,34 886,22 838,12"
            fill="none"
            stroke="#5C3A21"
            strokeWidth="5"
            strokeLinecap="round"
          />
          <PaintedBlossomCluster x={922} y={32} scale={0.62} />
          <HoiAnLantern2D x={896} y={24} scale={0.58} color="#EA580C" />

          {/* Bottom-right blooming Pink Lotus accent */}
          <PaintedLotus2D x={918} y={522} scale={0.52} />
        </svg>
      )}

      {holidayId === 'reunification-labor' && (
        <svg className="login-card-decor-svg" viewBox="0 0 960 540" preserveAspectRatio="none">
          {/* Top-left Crimson & Gold Flag Pennant Garland */}
          <path
            d="M0,58 C45,42 92,22 142,0"
            fill="none"
            stroke="#B91C1C"
            strokeWidth="2.5"
          />
          {[
            { x: 18, y: 40, rot: -22 },
            { x: 58, y: 24, rot: -22 },
            { x: 98, y: 8, rot: -22 },
          ].map((f, idx) => (
            <g key={idx} transform={`translate(${f.x}, ${f.y}) rotate(${f.rot})`}>
              <path d="M0,0 L28,0 L28,22 L14,17 L0,22 Z" fill="#DC2626" />
              <path
                d="M14,4 L15.6,9 L21,9 L16.6,12 L18.2,17 L14,14 L9.8,17 L11.4,12 L7,9 L12.4,9 Z"
                fill="#FACC15"
              />
            </g>
          ))}

          {/* Top-right Crimson & Gold Pennant + Bottom-left/right Sunflower & Lotus accents */}
          <g transform="translate(960, 0) scale(-1, 1)">
            <path
              d="M0,58 C45,42 92,22 142,0"
              fill="none"
              stroke="#B91C1C"
              strokeWidth="2.5"
            />
            {[
              { x: 18, y: 40, rot: -22 },
              { x: 58, y: 24, rot: -22 },
            ].map((f, idx) => (
              <g key={idx} transform={`translate(${f.x}, ${f.y}) rotate(${f.rot})`}>
                <path d="M0,0 L28,0 L28,22 L14,17 L0,22 Z" fill="#DC2626" />
                <path
                  d="M14,4 L15.6,9 L21,9 L16.6,12 L18.2,17 L14,14 L9.8,17 L11.4,12 L7,9 L12.4,9 Z"
                  fill="#FACC15"
                />
              </g>
            ))}
          </g>
          <PaintedLotus2D x={44} y={524} scale={0.52} />
          <PaintedBlossomCluster
            x={924}
            y={512}
            scale={0.85}
            petalColor="#FACC15"
            innerColor="#78350F"
            stamenColor="#FDE047"
          />
        </svg>
      )}

      {holidayId === 'halloween' && (
        <svg className="login-card-decor-svg" viewBox="0 0 960 540" preserveAspectRatio="none">
          {/* Corner silver cobwebs on the Login Popup Card */}
          <g stroke="#94A3B8" strokeWidth="1.3" fill="none" opacity="0.52">
            <path d="M0,0 L115,22 M0,0 L95,58 M0,0 L62,88 M0,0 L24,108" />
            <path d="M36,7 Q30,20 22,24 Q14,32 7,38 M72,14 Q62,36 48,44 Q32,58 14,68 M104,20 Q88,52 68,64 Q46,84 20,96" />
          </g>
          <g transform="translate(960, 0) scale(-1, 1)" stroke="#94A3B8" strokeWidth="1.3" fill="none" opacity="0.52">
            <path d="M0,0 L115,22 M0,0 L95,58 M0,0 L62,88 M0,0 L24,108" />
            <path d="M36,7 Q30,20 22,24 Q14,32 7,38 M72,14 Q62,36 48,44 Q32,58 14,68 M104,20 Q88,52 68,64 Q46,84 20,96" />
          </g>
          {/* Hanging glowing mini pumpkin lantern on top-right card corner */}
          <path d="M902,0 L902,42" stroke="#64748B" strokeWidth="1.6" />
          <g transform="translate(902, 54)">
            <AtmosphericAura cx={0} cy={0} r={20} color="#F97316" intensity={0.95} />
            <path
              d="M0,-12 C-14,-13 -20,-3 -18,7 C-16,15 -8,16 0,15 C8,16 16,15 18,7 C20,-3 14,-13 0,-12 Z"
              fill="#EA580C"
            />
            <path
              d="M-9,-2 L-4,-7 L-2,-1 Z M9,-2 L4,-7 L2,-1 Z M-10,5 Q0,12 10,5 Q0,15 -10,5 Z"
              fill="#FEF08A"
            />
          </g>
        </svg>
      )}

      {holidayId === 'childrens-day' && (
        <svg className="login-card-decor-svg" viewBox="0 0 960 540" preserveAspectRatio="none">
          {/* Colorful carnival bunting flags at top-left & pastel rainbow + paper plane at top-right */}
          <path d="M0,52 Q75,28 148,0" fill="none" stroke="#92400E" strokeWidth="2" />
          <path d="M16,44 L44,34 L34,64 Z" fill="#FB7185" />
          <path d="M52,31 L80,21 L70,51 Z" fill="#FACC15" />
          <path d="M88,18 L116,8 L106,38 Z" fill="#38BDF8" />
          {/* Top-right paper airplane with dashed flight loop */}
          <path
            d="M845,52 Q885,68 912,32"
            fill="none"
            stroke="#38BDF8"
            strokeWidth="1.6"
            strokeDasharray="4 4"
          />
          <g transform="translate(918, 26) rotate(-14)">
            <path d="M0,0 L-26,8 L-10,12 Z" fill="#FFFFFF" stroke="#38BDF8" strokeWidth="1" />
            <path d="M0,0 L-10,12 L-14,20 L-5,14 Z" fill="#BAE6FD" />
          </g>
        </svg>
      )}
    </div>
  );
}
