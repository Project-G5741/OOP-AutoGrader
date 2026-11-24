import React from 'react';

/**
 * 2D Vietnamese Landscape Illustration Primitives.
 * Crafted exclusively with organic cubic/quadratic Bezier paths (<path d="M... C... S... Q..." />),
 * variable-weight 2D ink/brush linework, layered atmospheric perspective, and traditional
 * Vietnamese architectural & botanical motifs.
 * Zero blocky primitive shapes.
 */

/**
 * Soft atmospheric light aura rendered with organic layered path contours.
 */
export function AtmosphericAura({ cx, cy, r, color = '#FBBF24', intensity = 1 }) {
  const mkCirclePath = (rad) =>
    `M ${cx - rad},${cy} a ${rad},${rad} 0 1,0 ${rad * 2},0 a ${rad},${rad} 0 1,0 ${-rad * 2},0 Z`;
  return (
    <g pointerEvents="none">
      <path d={mkCirclePath(r * 2.1)} fill={color} opacity={0.045 * intensity} />
      <path d={mkCirclePath(r * 1.6)} fill={color} opacity={0.08 * intensity} />
      <path d={mkCirclePath(r * 1.22)} fill={color} opacity={0.14 * intensity} />
      <path d={mkCirclePath(r)} fill={color} opacity={0.24 * intensity} />
    </g>
  );
}

/**
 * Traditional Vietnamese Oriental Cloud Scroll (Vân Mây 2D) drawn with flowing brush strokes.
 */
export function OrientalCloud({
  x,
  y,
  scale = 1,
  fill = '#FFFBEB',
  stroke = '#D97706',
  opacity = 0.82,
  flip = false,
}) {
  return (
    <g
      transform={`translate(${x}, ${y}) scale(${flip ? -scale : scale}, ${scale})`}
      opacity={opacity}
    >
      <path
        d="M-110,14 C-92,14 -82,2 -68,4 C-58,-14 -32,-18 -18,-6 C-6,-26 28,-26 42,-6 C58,-14 82,-6 86,10 C102,10 118,18 126,24 C92,28 45,26 -12,26 C-56,26 -88,22 -110,14 Z"
        fill={fill}
      />
      <path
        d="M-96,15 C-76,14 -68,2 -54,6 C-44,-10 -22,-12 -12,0 C0,-18 26,-18 36,0 C50,-8 72,-2 76,12 M-64,22 C-24,20 34,20 98,22"
        fill="none"
        stroke={stroke}
        strokeWidth="1.6"
        strokeLinecap="round"
        opacity="0.55"
      />
    </g>
  );
}

/**
 * Flowing Water Ripples & Reflections (Nét vẽ sóng nước 2D).
 */
export function WaterRipples({ x, y, width = 360, color = '#FDE68A', opacity = 0.45 }) {
  const w = width;
  return (
    <g
      transform={`translate(${x}, ${y})`}
      fill="none"
      stroke={color}
      strokeLinecap="round"
      opacity={opacity}
    >
      <path d={`M 0,0 Q ${w * 0.18},-5 ${w * 0.36},0 T ${w * 0.72},0 T ${w},0`} strokeWidth="2.2" />
      <path d={`M ${w * 0.08},12 Q ${w * 0.28},7 ${w * 0.48},12 T ${w * 0.88},12`} strokeWidth="1.7" />
      <path d={`M ${w * 0.16},24 Q ${w * 0.36},20 ${w * 0.56},24 T ${w * 0.82},24`} strokeWidth="1.4" />
      <path d={`M ${w * 0.26},35 Q ${w * 0.46},32 ${w * 0.68},35`} strokeWidth="1.1" />
    </g>
  );
}

/**
 * Traditional Vietnamese Curved Communal/Pagoda Roof Pavilion (Đình Chùa Mái Đao 2D)
 * drawn with sweeping upturned dragon-tail eaves, yin-yang ridge tiles, and wooden columns.
 */
export function VietnamesePavilion2D({
  x,
  y,
  scale = 1,
  roofColor = '#451A03',
  roofHighlight = '#78350F',
  trimColor = '#D97706',
  pillarColor = '#7F1D1D',
  wallColor = '#F5E6C8',
  lanternGlow = '#FEF08A',
}) {
  return (
    <g transform={`translate(${x}, ${y}) scale(${scale})`}>
      {/* Stone terrace base drawn with organic stepped curves */}
      <path
        d="M-165,168 C-110,165 110,165 165,168 C172,175 174,182 170,186 C65,188 -65,188 -170,186 C-174,182 -172,175 -165,168 Z"
        fill="#57534E"
      />
      <path
        d="M-148,154 C-90,152 90,152 148,154 C154,160 154,166 150,169 C50,170 -50,170 -150,169 C-154,166 -154,160 -148,154 Z"
        fill="#78716C"
      />

      {/* Warm inner sanctuary glow & arched doorway */}
      <path
        d="M-108,68 C-40,66 40,66 108,68 L110,155 L-110,155 Z"
        fill={wallColor}
      />
      <path
        d="M-38,155 L-38,104 C-38,80 38,80 38,104 L38,155 Z"
        fill={roofColor}
      />
      <path
        d="M-30,155 L-30,108 C-30,88 30,88 30,108 L30,155 Z"
        fill={lanternGlow}
        opacity="0.72"
      />
      {/* Traditional wooden lattice screen lines */}
      <path
        d="M-88,82 L-54,82 L-54,142 L-88,142 Z M54,82 L88,82 L88,142 L54,142 Z"
        fill="none"
        stroke={pillarColor}
        strokeWidth="2.2"
      />

      {/* Wooden pillars (Cột đình gỗ lim) */}
      {[-112, -46, 46, 112].map((px) => (
        <path
          key={px}
          d={`M ${px - 6},64 C ${px - 5},95 ${px - 5},125 ${px - 6},155 L ${px + 6},155 C ${px + 5},125 ${px + 5},95 ${px + 6},64 Z`}
          fill={pillarColor}
        />
      ))}

      {/* Lower Sweeping Curved Roof (Mái Đao Tầng Dưới) */}
      <path
        d="M-192,48 C-165,64 -128,42 -96,24 L96,24 C128,42 165,64 192,48 C184,66 164,74 138,72 C55,68 -55,68 -138,72 C-164,74 -184,66 -192,48 Z"
        fill={roofColor}
      />
      {/* Upturned dragon-tail eave tips (Đầu đao cong vút) */}
      <path
        d="M-192,48 C-204,34 -202,16 -188,10 C-188,24 -182,36 -166,46 M192,48 C204,34 202,16 188,10 C188,24 182,36 166,46"
        fill="none"
        stroke={trimColor}
        strokeWidth="3.2"
        strokeLinecap="round"
      />
      {/* Hand-drawn roof tile ribs (Sóng ngói âm dương) */}
      {[-120, -96, -72, -48, -24, 0, 24, 48, 72, 96, 120].map((rx) => (
        <path
          key={rx}
          d={`M ${rx * 0.78},26 Q ${rx * 0.92},48 ${rx * 1.12},68`}
          fill="none"
          stroke={roofHighlight}
          strokeWidth="1.8"
          strokeLinecap="round"
        />
      ))}
      <path
        d="M-182,54 C-110,70 110,70 182,54"
        fill="none"
        stroke={trimColor}
        strokeWidth="2.4"
        strokeLinecap="round"
      />

      {/* Upper Sweeping Curved Roof (Mái Đao Tầng Trên) */}
      <path
        d="M-142,8 C-115,20 -86,-2 -58,-22 L58,-22 C86,-2 115,20 142,8 C134,24 114,30 92,28 C35,25 -35,25 -92,28 C-114,30 -134,24 -142,8 Z"
        fill={roofColor}
      />
      <path
        d="M-142,8 C-154,-4 -152,-18 -138,-24 C-138,-12 -132,-2 -118,6 M142,8 C154,-4 152,-18 138,-24 C138,-12 132,-2 118,6"
        fill="none"
        stroke={trimColor}
        strokeWidth="2.8"
        strokeLinecap="round"
      />
      {[-75, -50, -25, 0, 25, 50, 75].map((rx) => (
        <path
          key={rx}
          d={`M ${rx * 0.7},-20 Q ${rx * 0.88},4 ${rx * 1.08},26`}
          fill="none"
          stroke={roofHighlight}
          strokeWidth="1.6"
          strokeLinecap="round"
        />
      ))}
      {/* Roof ridge crest (Bờ nóc lưỡng long chầu nguyệt) */}
      <path
        d="M-64,-22 C-20,-26 20,-26 64,-22"
        fill="none"
        stroke={trimColor}
        strokeWidth="3.5"
        strokeLinecap="round"
      />
      <path
        d="M0,-36 C6,-36 10,-30 10,-24 C10,-18 6,-14 0,-14 C-6,-14 -10,-18 -10,-24 C-10,-30 -6,-36 0,-36 Z"
        fill="none"
        stroke={trimColor}
        strokeWidth="2.2"
      />
    </g>
  );
}

/**
 * Hand-drawn 2D Vietnamese Bamboo Grove (Lũy Tre Làng Việt Nam)
 * with curved culms, nodes, and sweeping lance-shaped leaves.
 */
export function BambooGrove2D({
  x,
  y,
  scale = 1,
  flip = false,
  stalkColor = '#15803D',
  leafColor = '#166534',
  highlightColor = '#4ADE80',
}) {
  return (
    <g transform={`translate(${x}, ${y}) scale(${flip ? -scale : scale}, ${scale})`}>
      {/* Curved bamboo stalks with organic nodes */}
      <path
        d="M0,320 C4,230 14,130 34,20 M34,20 C44,-35 62,-85 88,-130"
        fill="none"
        stroke={stalkColor}
        strokeWidth="8"
        strokeLinecap="round"
      />
      <path
        d="M2,320 C6,230 16,130 36,20"
        fill="none"
        stroke={highlightColor}
        strokeWidth="2.2"
        strokeLinecap="round"
        opacity="0.55"
      />
      <path
        d="M-24,320 C-18,240 -2,145 24,55 C44,-15 72,-75 112,-115"
        fill="none"
        stroke={leafColor}
        strokeWidth="6.5"
        strokeLinecap="round"
      />
      <path
        d="M18,320 C26,245 48,165 78,90 C102,32 132,-18 168,-55"
        fill="none"
        stroke={stalkColor}
        strokeWidth="5.5"
        strokeLinecap="round"
      />
      {/* Bamboo joint nodes (Đốt tre) */}
      {[
        [6, 250], [14, 175], [24, 98], [35, 22], [54, -48],
        [-16, 235], [-2, 155], [16, 78], [42, 6],
        [32, 240], [52, 168], [76, 95],
      ].map(([nx, ny], i) => (
        <path
          key={i}
          d={`M ${nx - 6},${ny} Q ${nx},${ny - 3} ${nx + 6},${ny}`}
          fill="none"
          stroke="#DCFCE7"
          strokeWidth="2"
          strokeLinecap="round"
          opacity="0.7"
        />
      ))}

      {/* Sweeping 2D brush-stroke bamboo leaf clusters (Lá tre) */}
      {[
        [35, 22, -15], [54, -48, -25], [86, -120, -35],
        [16, 78, 12], [42, 6, -8], [108, -108, -20],
        [76, 95, 18], [118, 18, 5], [162, -48, -12],
      ].map(([lx, ly, rot], idx) => (
        <g key={idx} transform={`translate(${lx}, ${ly}) rotate(${rot})`}>
          <path
            d="M0,0 C18,-12 42,-14 64,-4 C42,2 18,2 0,0 Z"
            fill={leafColor}
          />
          <path
            d="M0,0 C16,4 38,14 56,28 C32,22 14,12 0,0 Z"
            fill={stalkColor}
          />
          <path
            d="M0,0 C-14,-10 -34,-18 -54,-12 C-34,-2 -16,0 0,0 Z"
            fill={highlightColor}
            opacity="0.75"
          />
          <path
            d="M0,0 C10,12 22,28 28,46 C14,32 4,18 0,0 Z"
            fill={leafColor}
          />
        </g>
      ))}
    </g>
  );
}

/**
 * Hand-drawn 2D Lotus Bloom, Bud & Swaying Pad (Hoa Sen & Lá Sen 2D).
 */
export function PaintedLotus2D({ x, y, scale = 1 }) {
  return (
    <g transform={`translate(${x}, ${y}) scale(${scale})`}>
      {/* Swaying undulating lotus leaf pad (Lá sen uốn lượn) */}
      <path
        d="M-62,22 C-54,10 -28,6 0,8 C28,6 54,10 62,22 C52,34 26,38 0,36 C-26,38 -52,34 -62,22 Z"
        fill="#15803D"
      />
      <path
        d="M-54,20 C-36,12 -14,14 0,22 C14,14 36,12 54,20"
        fill="none"
        stroke="#4ADE80"
        strokeWidth="1.5"
        opacity="0.6"
      />
      {/* Graceful curved stems */}
      <path
        d="M0,26 C-4,4 -4,-16 0,-36 M-28,28 C-36,6 -44,-14 -48,-32"
        fill="none"
        stroke="#166534"
        strokeWidth="3"
        strokeLinecap="round"
      />
      {/* Lotus bud on left stem */}
      <g transform="translate(-48, -32) rotate(-14)">
        <path
          d="M0,-22 C10,-12 12,4 0,12 C-12,4 -10,-12 0,-22 Z"
          fill="#F472B6"
        />
        <path
          d="M0,-22 C5,-12 6,2 0,10"
          fill="none"
          stroke="#FCE7F3"
          strokeWidth="1.4"
        />
      </g>
      {/* Full blooming 2D Lotus with layered organic petals & vein lines */}
      <g transform="translate(0, -36)">
        <path d="M-36,12 C-46,-6 -26,-22 -12,-26 C-16,-6 -20,8 -36,12 Z" fill="#EC4899" />
        <path d="M36,12 C46,-6 26,-22 12,-26 C16,-6 20,8 36,12 Z" fill="#EC4899" />
        <path d="M-26,14 C-32,-12 -14,-32 0,-36 C-6,-12 -10,6 -26,14 Z" fill="#F472B6" />
        <path d="M26,14 C32,-12 14,-32 0,-36 C6,-12 10,6 26,14 Z" fill="#F472B6" />
        <path d="M0,-42 C14,-26 16,2 0,16 C-16,2 -14,-26 0,-42 Z" fill="#F9A8D4" />
        {/* Delicate petal brush-stroke outlines & veins */}
        <path
          d="M0,-40 C6,-24 6,0 0,14 M-18,-22 C-14,-8 -12,4 -6,12 M18,-22 C14,-8 12,4 6,12"
          fill="none"
          stroke="#FDF2F8"
          strokeWidth="1.3"
          strokeLinecap="round"
          opacity="0.8"
        />
        {/* Golden stamen crown */}
        <path d="M-10,6 Q0,1 10,6 Q0,11 -10,6 Z" fill="#FACC15" />
      </g>
    </g>
  );
}

/**
 * Hand-drawn 2D Blossom Branch (Cành Mai / Cành Đào / Phượng Vĩ vẽ tay 2D)
 */
export function PaintedBlossomCluster({
  x,
  y,
  scale = 1,
  petalColor = '#FACC15',
  innerColor = '#D97706',
  stamenColor = '#FEF08A',
}) {
  return (
    <g transform={`translate(${x}, ${y}) scale(${scale})`}>
      {[0, 72, 144, 216, 288].map((deg) => (
        <g key={deg} transform={`rotate(${deg})`}>
          <path
            d="M0,-2 C-9,-8 -11,-22 0,-26 C11,-22 9,-8 0,-2 Z"
            fill={petalColor}
          />
          <path
            d="M0,-4 C-3,-12 -3,-20 0,-24"
            fill="none"
            stroke={stamenColor}
            strokeWidth="1.1"
            opacity="0.65"
          />
        </g>
      ))}
      {/* Delicate hand-drawn stamens */}
      {[18, 90, 162, 234, 306].map((deg) => (
        <g key={deg} transform={`rotate(${deg})`}>
          <path d="M0,0 L0,-11" stroke={innerColor} strokeWidth="1.2" />
          <path d="M-1.5,-11 a1.5,1.5 0 1,0 3,0 a1.5,1.5 0 1,0 -3,0" fill={stamenColor} />
        </g>
      ))}
      <path d="M-3.5,0 a3.5,3.5 0 1,0 7,0 a3.5,3.5 0 1,0 -7,0" fill={innerColor} />
    </g>
  );
}

/**
 * Traditional Vietnamese Sampan Boat (Thuyền Nan / Thuyền Lá 2D trên sông).
 */
export function VietnameseSampan2D({
  x,
  y,
  scale = 1,
  hullColor = '#451A03',
  rimColor = '#92400E',
  hasLantern = true,
  flip = false,
}) {
  return (
    <g transform={`translate(${x}, ${y}) scale(${flip ? -scale : scale}, ${scale})`}>
      {/* Water reflection beneath hull */}
      <path
        d="M-68,14 Q0,20 68,14 Q36,24 -36,24 Z"
        fill={hullColor}
        opacity="0.25"
      />
      {/* Curved woven bamboo sampan hull */}
      <path
        d="M-82,-4 C-52,18 52,18 84,-6 C66,2 42,6 0,6 C-42,6 -66,2 -82,-4 Z"
        fill={hullColor}
      />
      <path
        d="M-82,-4 C-42,6 42,6 84,-6"
        fill="none"
        stroke={rimColor}
        strokeWidth="2.4"
        strokeLinecap="round"
      />
      {/* Arched bamboo canopy (Mui thuyền đan tre) */}
      <path
        d="M-28,3 C-24,-18 18,-18 24,3 C8,5 -10,5 -28,3 Z"
        fill={rimColor}
      />
      <path
        d="M-18,3 C-15,-14 10,-14 14,3"
        fill="none"
        stroke={hullColor}
        strokeWidth="1.6"
      />
      {/* Glowing bow lantern */}
      {hasLantern && (
        <g transform="translate(64, -12)">
          <AtmosphericAura cx={0} cy={4} r={16} color="#F59E0B" intensity={0.9} />
          <path d="M-8,8 L0,-6 L4,-6" fill="none" stroke={hullColor} strokeWidth="1.8" />
          <path d="M4,-6 C9,-6 9,4 4,4 C-1,4 -1,-6 4,-6 Z" fill="#FEF08A" />
        </g>
      )}
    </g>
  );
}

/**
 * Traditional Hội An Diamond / Garlic Silk Lantern (Đèn Lồng Hội An 2D).
 */
export function HoiAnLantern2D({
  x,
  y,
  scale = 1,
  color = '#DC2626',
  ribColor = '#7F1D1D',
  glowColor = '#FDE047',
}) {
  return (
    <g transform={`translate(${x}, ${y}) scale(${scale})`}>
      <AtmosphericAura cx={0} cy={38} r={34} color="#F59E0B" intensity={0.95} />
      <path d="M0,0 L0,16" stroke="#78350F" strokeWidth="2.2" />
      {/* Diamond curved silk body */}
      <path
        d="M0,16 C22,18 32,28 32,38 C32,48 22,58 0,60 C-22,58 -32,48 -32,38 C-32,28 -22,18 0,16 Z"
        fill={color}
      />
      {/* Inner warm candle core */}
      <path
        d="M0,24 C12,25 16,32 16,38 C16,44 12,51 0,52 C-12,51 -16,44 -16,38 C-16,32 -12,25 0,24 Z"
        fill={glowColor}
        opacity="0.48"
      />
      {/* Curved bamboo ribs */}
      <path
        d="M0,16 C14,22 20,30 20,38 C20,46 14,54 0,60 M0,16 C-14,22 -20,30 -20,38 C-20,46 -14,54 0,60 M0,16 L0,60"
        fill="none"
        stroke={ribColor}
        strokeWidth="1.6"
        opacity="0.75"
      />
      {/* Top & bottom wooden collars */}
      <path d="M-10,14 C-4,13 4,13 10,14 L11,18 C4,19 -4,19 -11,18 Z" fill="#F59E0B" />
      <path d="M-9,58 C-4,57 4,57 9,58 L10,62 C4,63 -4,63 -10,62 Z" fill="#F59E0B" />
      {/* Flowing silk tassel */}
      <path
        d="M0,62 C-1,72 -3,82 -5,92 M0,62 C0,73 0,84 0,94 M0,62 C1,72 3,82 5,92"
        fill="none"
        stroke="#F59E0B"
        strokeWidth="1.8"
        strokeLinecap="round"
      />
    </g>
  );
}

/**
 * Graceful 2D Swallow / Bird / Dove in Flight (Chim Én / Bồ Câu vẽ nét cọ 2D).
 */
export function PaintedBird2D({
  x,
  y,
  scale = 1,
  color = '#1E293B',
  accent = '#DC2626',
  flip = false,
  isSwallow = true,
}) {
  return (
    <g transform={`translate(${x}, ${y}) scale(${flip ? -scale : scale}, ${scale})`}>
      {/* Upper sweeping wing */}
      <path
        d="M-4,-2 C-14,-22 -32,-32 -48,-26 C-32,-14 -18,-2 -8,4 Z"
        fill={color}
      />
      {/* Lower sweeping wing */}
      <path
        d="M2,0 C12,-18 28,-24 44,-16 C28,-8 14,0 4,5 Z"
        fill={color}
      />
      {/* Streamlined body & forked swallow tail */}
      <path
        d={
          isSwallow
            ? 'M-18,6 C-6,0 8,0 18,4 C6,10 -8,12 -28,22 L-16,10 L-32,14 Z'
            : 'M-18,6 C-6,-2 10,-2 20,4 C8,10 -6,12 -24,14 Z'
        }
        fill={color}
      />
      {isSwallow && (
        <path d="M12,2 C16,2 19,4 16,6 C13,6 10,4 12,2 Z" fill={accent} />
      )}
    </g>
  );
}

/**
 * Deterministic atmospheric particle engine using 2D organic brush-stroke glyphs.
 */
function buildParticles(count = 28) {
  const items = [];
  for (let i = 0; i < count; i += 1) {
    const seed = (i * 9301 + 49297) % 233280;
    const r1 = ((i * 37 + 11) % 100) / 100;
    const r2 = ((i * 73 + 19) % 100) / 100;
    const r3 = (seed % 100) / 100;
    items.push({
      id: i,
      left: `${(r1 * 96 + 2).toFixed(1)}%`,
      delay: `${(-r2 * 15).toFixed(2)}s`,
      duration: `${(8.5 + r3 * 8).toFixed(2)}s`,
      size: Math.round(12 + (i % 4) * 4),
      swayClass: i % 2 === 0 ? 'holiday-sway-left' : 'holiday-sway-right',
      slot: i % 3,
    });
  }
  return items;
}

const PARTICLES = buildParticles(28);

function render2DParticleGlyph(type, size, color) {
  if (type === 'snow') {
    return (
      <svg width={size} height={size} viewBox="0 0 24 24" aria-hidden="true">
        <path
          d="M12,2 L12,22 M3.5,7 L20.5,17 M3.5,17 L20.5,7 M12,5 L9,8 M12,5 L15,8 M12,19 L9,16 M12,19 L15,16"
          fill="none"
          stroke={color}
          strokeWidth="1.8"
          strokeLinecap="round"
        />
      </svg>
    );
  }
  if (type === 'spooky') {
    return (
      <svg width={size + 8} height={size} viewBox="0 0 32 20" aria-hidden="true">
        <path
          d="M16,8 C12,1 5,1 1,6 C6,7 8,10 9,14 C12,10 14,11 16,15 C18,11 20,10 23,14 C24,10 26,7 31,6 C27,1 20,1 16,8 Z"
          fill={color}
        />
      </svg>
    );
  }
  if (type === 'lanterns') {
    return (
      <svg width={size + 4} height={size + 6} viewBox="0 0 24 28" aria-hidden="true">
        <path
          d="M6,5 C9,3 15,3 18,5 L16,21 C14,23 10,23 8,21 Z"
          fill={color}
        />
        <path d="M9,9 C11,8 13,8 15,9 L14,19 C13,20 11,20 10,19 Z" fill="#FEF08A" opacity="0.82" />
      </svg>
    );
  }
  if (type === 'paper-planes') {
    return (
      <svg width={size + 6} height={size + 4} viewBox="0 0 28 24" aria-hidden="true">
        <path d="M2,11 L26,3 L17,21 L12,14 Z" fill="#FFFFFF" stroke={color} strokeWidth="1.2" />
        <path d="M26,3 L12,14 L12,19 L15,16" fill={color} opacity="0.55" />
      </svg>
    );
  }
  if (type === 'stars') {
    return (
      <svg width={size} height={size} viewBox="0 0 24 24" aria-hidden="true">
        <path
          d="M12,2 L14.8,8.8 L22,9.3 L16.5,14 L18.2,21 L12,17.2 L5.8,21 L7.5,14 L2,9.3 L9.2,8.8 Z"
          fill={color}
        />
      </svg>
    );
  }
  // Organic 2D flower petal / autumn leaf stroke
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" aria-hidden="true">
      <path
        d="M12,2 C18,6 20,14 12,22 C4,14 6,6 12,2 Z"
        fill={color}
      />
      <path d="M12,5 C13,10 13,15 12,20" fill="none" stroke="#FFFFFF" strokeWidth="1" opacity="0.5" />
    </svg>
  );
}

export function HolidayParticleLayer({
  type = 'blossom',
  colors = ['#F59E0B', '#FB7185', '#FDE68A'],
  rising = false,
}) {
  return (
    <div className="holiday-particles-layer" aria-hidden="true">
      {PARTICLES.map((p) => (
        <div
          key={p.id}
          className={`holiday-particle ${rising ? 'holiday-particle--rise' : 'holiday-particle--fall'}`}
          style={{
            left: p.left,
            animationDelay: p.delay,
            animationDuration: p.duration,
          }}
        >
          <div className={p.swayClass}>
            {render2DParticleGlyph(type, p.size, colors[p.slot % colors.length])}
          </div>
        </div>
      ))}
    </div>
  );
}
