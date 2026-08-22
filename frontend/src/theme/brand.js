/**
 * ═══════════════════════════════════════════════════════════════════════════
 * BRAND CONFIG — edit this file ONLY to change logo and app naming.
 *
 * Browser tab icon (favicon):
 *   Drop any image into frontend/public/brand/
 *   Supported: .png .svg .webp .ico .jpg .jpeg .gif .avif
 *   Any filename works. Optional: name it logo.<ext> to take priority.
 *   Run dev/build (or npm run theme:sync) to pick up new files.
 *
 * In-app logo (login, header): always the graduation cap icon below.
 * ═══════════════════════════════════════════════════════════════════════════
 */

import { GraduationCap } from 'lucide-react';
import { brandAssets } from './brand.assets.generated.js';

export const brand = {
  appName: 'OOP AutoGrader',
  loginTitle: 'Lab Management System',

  favicon: {
    url: brandAssets.favicon?.url ?? null,
    type: brandAssets.favicon?.type ?? null,
  },

  logo: {
    icon: GraduationCap,
    alt: 'OOP AutoGrader',
  },
};
