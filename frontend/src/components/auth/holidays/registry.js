import {
  Balloon,
  FlagBanner,
  FlowerLotus,
  Ghost,
  GraduationCap,
  MoonStars,
  Sparkle,
  TreeEvergreen,
} from '@phosphor-icons/react';
import DefaultBackgroundScene from './scenes/DefaultBackgroundScene';
import HalloweenScene from './scenes/HalloweenScene';
import ChristmasScene from './scenes/ChristmasScene';
import NewYearScene from './scenes/NewYearScene';
import TetNguyenDanScene from './scenes/TetNguyenDanScene';
import LaborDayScene from './scenes/LaborDayScene';
import ChildrensDayScene from './scenes/ChildrensDayScene';
import MidAutumnScene from './scenes/MidAutumnScene';

export { DefaultBackgroundScene };

/**
 * Registry of the 7 Major Holiday Login Designs:
 * - Background Scene component (`Scene`) rendered in `LoginBackground.jsx`
 * - Login Pop-up metadata (`kicker`, `KickerIcon`, `popupTokens`) applied strictly
 *   on the Login / Auth screens ("Theme chỉ có ngoài login thôi")
 * - Session-wide particle & corner animation configuration (`sessionEffects`)
 *   rendered across the entire web session via `SessionHolidayEffects.jsx`.
 */
export const HOLIDAY_BACKGROUND_REGISTRY = {
  halloween: {
    id: 'halloween',
    name: 'Halloween (31/10)',
    kicker: 'ĐÊM HỘI HALLOWEEN · 31/10',
    KickerIcon: Ghost,
    Scene: HalloweenScene,
    sessionEffects: {
      particleType: 'spooky',
      particleColors: ['#F97316', '#A855F7', '#FACC15'],
      rising: false,
      cornerEffect: 'cobwebs',
    },
    popupTokens: {
      light: {
        '--login-kicker-bg': '#4C1D95',
        '--login-kicker-fg': '#FDBA74',
        '--login-accent': '#EA580C',
        '--login-accent-hover': '#C2410C',
        '--login-logo-bg': '#4C1D95',
        '--login-hero-bg': 'rgba(250, 240, 255, 0.90)',
        '--login-card-border': 'rgba(249, 115, 22, 0.45)',
        '--login-card-shadow': '0 28px 60px -14px rgba(76, 29, 149, 0.35)',
      },
      dark: {
        '--login-kicker-bg': '#581C87',
        '--login-kicker-fg': '#FDBA74',
        '--login-accent': '#F97316',
        '--login-accent-hover': '#EA580C',
        '--login-logo-bg': '#4C1D95',
        '--login-hero-bg': 'rgba(24, 18, 43, 0.90)',
        '--login-card-border': 'rgba(249, 115, 22, 0.48)',
        '--login-card-shadow': '0 28px 64px -12px rgba(2, 6, 23, 0.85)',
      },
    },
  },
  christmas: {
    id: 'christmas',
    name: 'Noel · Giáng Sinh (24–25/12)',
    kicker: 'GIÁNG SINH AN LÀNH · 24 - 25/12',
    KickerIcon: TreeEvergreen,
    Scene: ChristmasScene,
    sessionEffects: {
      particleType: 'snow',
      particleColors: ['#FFFFFF', '#BAE6FD', '#FDE68A'],
      rising: false,
      cornerEffect: 'snow-pine',
    },
    popupTokens: {
      light: {
        '--login-kicker-bg': '#166534',
        '--login-kicker-fg': '#FEF08A',
        '--login-accent': '#15803D',
        '--login-accent-hover': '#166534',
        '--login-logo-bg': '#DC2626',
        '--login-hero-bg': 'rgba(253, 248, 238, 0.90)',
        '--login-card-border': 'rgba(21, 128, 61, 0.35)',
        '--login-card-shadow': '0 26px 56px -14px rgba(20, 83, 45, 0.24)',
      },
      dark: {
        '--login-kicker-bg': '#14532D',
        '--login-kicker-fg': '#FEF08A',
        '--login-accent': '#16A34A',
        '--login-accent-hover': '#15803D',
        '--login-logo-bg': '#B91C1C',
        '--login-hero-bg': 'rgba(20, 40, 30, 0.90)',
        '--login-card-border': 'rgba(34, 197, 94, 0.38)',
        '--login-card-shadow': '0 28px 60px -14px rgba(2, 6, 23, 0.82)',
      },
    },
  },
  'new-year': {
    id: 'new-year',
    name: 'Tết Dương Lịch (01/01)',
    kicker: 'HAPPY NEW YEAR · 01/01',
    KickerIcon: Sparkle,
    Scene: NewYearScene,
    sessionEffects: {
      particleType: 'snow',
      particleColors: ['#FFFFFF', '#FDE047', '#93C5FD'],
      rising: false,
      cornerEffect: 'none',
    },
    popupTokens: {
      light: {
        '--login-kicker-bg': '#1E3A8A',
        '--login-kicker-fg': '#FEF08A',
        '--login-accent': '#1D4ED8',
        '--login-accent-hover': '#1E3A8A',
        '--login-logo-bg': '#1E3A8A',
        '--login-hero-bg': 'rgba(239, 246, 255, 0.90)',
        '--login-card-border': 'rgba(245, 158, 11, 0.42)',
        '--login-card-shadow': '0 26px 56px -14px rgba(15, 23, 42, 0.42)',
      },
      dark: {
        '--login-kicker-bg': '#1E40AF',
        '--login-kicker-fg': '#FEF08A',
        '--login-accent': '#2563EB',
        '--login-accent-hover': '#1D4ED8',
        '--login-logo-bg': '#1E3A8A',
        '--login-hero-bg': 'rgba(15, 23, 42, 0.90)',
        '--login-card-border': 'rgba(245, 158, 11, 0.45)',
        '--login-card-shadow': '0 28px 60px -14px rgba(2, 6, 23, 0.85)',
      },
    },
  },
  'tet-nguyen-dan': {
    id: 'tet-nguyen-dan',
    name: 'Tết Nguyên Đán (Âm Lịch)',
    kicker: 'CHÚC MỪNG NĂM MỚI · TẾT NGUYÊN ĐÁN',
    KickerIcon: FlowerLotus,
    Scene: TetNguyenDanScene,
    sessionEffects: {
      particleType: 'blossom',
      particleColors: ['#FACC15', '#F472B6', '#EF4444'],
      rising: false,
      cornerEffect: 'tet-blossom',
    },
    popupTokens: {
      light: {
        '--login-kicker-bg': '#B91C1C',
        '--login-kicker-fg': '#FEF08A',
        '--login-accent': '#DC2626',
        '--login-accent-hover': '#B91C1C',
        '--login-logo-bg': '#B91C1C',
        '--login-hero-bg': 'rgba(254, 245, 228, 0.90)',
        '--login-card-border': 'rgba(220, 38, 38, 0.36)',
        '--login-card-shadow': '0 26px 56px -14px rgba(153, 27, 27, 0.25)',
      },
      dark: {
        '--login-kicker-bg': '#991B1B',
        '--login-kicker-fg': '#FEF08A',
        '--login-accent': '#EF4444',
        '--login-accent-hover': '#DC2626',
        '--login-logo-bg': '#991B1B',
        '--login-hero-bg': 'rgba(40, 18, 18, 0.90)',
        '--login-card-border': 'rgba(245, 158, 11, 0.42)',
        '--login-card-shadow': '0 28px 60px -14px rgba(2, 6, 23, 0.85)',
      },
    },
  },
  'reunification-labor': {
    id: 'reunification-labor',
    name: 'Tết Lao Động (30/04 – 01/05)',
    kicker: 'QUỐC TẾ LAO ĐỘNG · 30/04 - 01/05',
    KickerIcon: FlagBanner,
    Scene: LaborDayScene,
    sessionEffects: {
      particleType: 'stars',
      particleColors: ['#DC2626', '#FACC15', '#F472B6'],
      rising: false,
      cornerEffect: 'none',
    },
    popupTokens: {
      light: {
        '--login-kicker-bg': '#DC2626',
        '--login-kicker-fg': '#FEF08A',
        '--login-accent': '#DC2626',
        '--login-accent-hover': '#B91C1C',
        '--login-logo-bg': '#DC2626',
        '--login-hero-bg': 'rgba(254, 249, 235, 0.90)',
        '--login-card-border': 'rgba(220, 38, 38, 0.36)',
        '--login-card-shadow': '0 26px 56px -14px rgba(180, 83, 9, 0.25)',
      },
      dark: {
        '--login-kicker-bg': '#991B1B',
        '--login-kicker-fg': '#FEF08A',
        '--login-accent': '#EF4444',
        '--login-accent-hover': '#DC2626',
        '--login-logo-bg': '#991B1B',
        '--login-hero-bg': 'rgba(38, 20, 20, 0.90)',
        '--login-card-border': 'rgba(250, 204, 21, 0.40)',
        '--login-card-shadow': '0 28px 60px -14px rgba(2, 6, 23, 0.85)',
      },
    },
  },
  'childrens-day': {
    id: 'childrens-day',
    name: 'Tết Thiếu Nhi (01/06)',
    kicker: 'TẾT THIẾU NHI · 01/06',
    KickerIcon: Balloon,
    Scene: ChildrensDayScene,
    sessionEffects: {
      particleType: 'paper-planes',
      particleColors: ['#38BDF8', '#FACC15', '#FB7185'],
      rising: false,
      cornerEffect: 'none',
    },
    popupTokens: {
      light: {
        '--login-kicker-bg': '#0284C7',
        '--login-kicker-fg': '#FEF9C3',
        '--login-accent': '#0284C7',
        '--login-accent-hover': '#0369A1',
        '--login-logo-bg': '#EF4444',
        '--login-hero-bg': 'rgba(240, 249, 255, 0.90)',
        '--login-card-border': 'rgba(56, 189, 248, 0.42)',
        '--login-card-shadow': '0 26px 56px -14px rgba(2, 132, 199, 0.24)',
      },
      dark: {
        '--login-kicker-bg': '#0369A1',
        '--login-kicker-fg': '#FEF9C3',
        '--login-accent': '#38BDF8',
        '--login-accent-hover': '#0284C7',
        '--login-logo-bg': '#DC2626',
        '--login-hero-bg': 'rgba(15, 23, 42, 0.90)',
        '--login-card-border': 'rgba(56, 189, 248, 0.42)',
        '--login-card-shadow': '0 28px 60px -14px rgba(2, 6, 23, 0.85)',
      },
    },
  },
  'mid-autumn': {
    id: 'mid-autumn',
    name: 'Tết Trung Thu (15/08 ÂL)',
    kicker: 'ĐÊM HỘI TRĂNG RẰM · 15/08 ÂL',
    KickerIcon: MoonStars,
    Scene: MidAutumnScene,
    sessionEffects: {
      particleType: 'lanterns',
      particleColors: ['#F97316', '#F59E0B', '#EF4444'],
      rising: true,
      cornerEffect: 'none',
    },
    popupTokens: {
      light: {
        '--login-kicker-bg': '#1E3A8A',
        '--login-kicker-fg': '#FEF08A',
        '--login-accent': '#D97706',
        '--login-accent-hover': '#B45309',
        '--login-logo-bg': '#DC2626',
        '--login-hero-bg': 'rgba(254, 246, 226, 0.90)',
        '--login-card-border': 'rgba(245, 158, 11, 0.45)',
        '--login-card-shadow': '0 26px 56px -14px rgba(15, 23, 42, 0.42)',
      },
      dark: {
        '--login-kicker-bg': '#1E40AF',
        '--login-kicker-fg': '#FEF08A',
        '--login-accent': '#F59E0B',
        '--login-accent-hover': '#D97706',
        '--login-logo-bg': '#B91C1C',
        '--login-hero-bg': 'rgba(23, 28, 48, 0.90)',
        '--login-card-border': 'rgba(245, 158, 11, 0.48)',
        '--login-card-shadow': '0 28px 60px -14px rgba(2, 6, 23, 0.85)',
      },
    },
  },
};

export const DEFAULT_HOLIDAY_ENTRY = {
  id: 'default',
  name: 'Mặc định (Không lễ)',
  kicker: 'EIU · SCIT',
  KickerIcon: GraduationCap,
  Scene: DefaultBackgroundScene,
  sessionEffects: null,
  popupTokens: {
    light: {},
    dark: {},
  },
};

export function getHolidayBackgroundEntry(holidayId) {
  if (holidayId && HOLIDAY_BACKGROUND_REGISTRY[holidayId]) {
    return HOLIDAY_BACKGROUND_REGISTRY[holidayId];
  }
  return DEFAULT_HOLIDAY_ENTRY;
}
