/**
 * ═══════════════════════════════════════════════════════════════════════════
 * BRAND CONFIG — edit this file ONLY to change logo and app naming.
 *
 * Logo icon: change `icon` to any lucide-react component.
 * Logo image: import a file and set `image` — it overrides the icon everywhere.
 *   Example:
 *     import logoImage from '../assets/brand/logo.png';
 *     logo: { image: logoImage, icon: GraduationCap, alt: 'OOP AutoGrader' }
 * ═══════════════════════════════════════════════════════════════════════════
 */

import { GraduationCap } from 'lucide-react';

// import logoImage from '../assets/brand/logo.png';

export const brand = {
  appName: 'OOP AutoGrader',
  loginTitle: 'Lab Management System',

  logo: {
    image: null, // logoImage — set to a Vite import to use a custom picture
    icon: GraduationCap,
    alt: 'OOP AutoGrader',
  },
};
