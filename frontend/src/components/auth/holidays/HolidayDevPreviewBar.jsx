import React from 'react';
import { ChevronLeft, ChevronRight, Sparkles } from 'lucide-react';
import {
  getDevHolidayOverride,
  MAJOR_HOLIDAY_DEFINITIONS,
  setDevHolidayOverride,
} from '../../../utils/holidayCalendar';
import { isDesktopLocalMode } from '../../../utils/desktopMode';

const PREVIEW_OPTIONS = [
  { id: 'auto', label: 'Auto (Hôm nay)' },
  { id: 'none', label: 'Mặc định (Không lễ)' },
  { id: 'halloween', label: '1. Halloween (31/10)' },
  { id: 'christmas', label: '2. Noel · Giáng Sinh (24–25/12)' },
  { id: 'new-year', label: '3. Tết Dương Lịch (01/01)' },
  { id: 'tet-nguyen-dan', label: '4. Tết Nguyên Đán (ÂL)' },
  { id: 'reunification-labor', label: '5. Tết Lao Động (30/04 – 01/05)' },
  { id: 'childrens-day', label: '6. Tết Thiếu Nhi (01/06)' },
  { id: 'mid-autumn', label: '7. Tết Trung Thu (15/08 ÂL)' },
];

export default function HolidayDevPreviewBar({ activeHoliday }) {
  if (!import.meta.env?.DEV || isDesktopLocalMode()) {
    return null;
  }

  const currentSelection = getDevHolidayOverride();

  const handleSelectChange = (e) => {
    setDevHolidayOverride(e.target.value);
  };

  const handleStep = (delta) => {
    const holidayIds = ['none', ...MAJOR_HOLIDAY_DEFINITIONS.map((h) => h.id)];
    const currentId =
      currentSelection === 'auto'
        ? activeHoliday?.id || 'none'
        : currentSelection;
    const idx = Math.max(0, holidayIds.indexOf(currentId));
    const nextIdx = (idx + delta + holidayIds.length) % holidayIds.length;
    setDevHolidayOverride(holidayIds[nextIdx]);
  };

  return (
    <div
      className="holiday-dev-preview-bar"
      role="region"
      aria-label="Holiday background preview bar"
    >
      <span className="holiday-dev-badge">
        <Sparkles className="h-3.5 w-3.5" />
        BG
      </span>
      <button
        type="button"
        onClick={() => handleStep(-1)}
        className="holiday-dev-step-btn"
        aria-label="Previous holiday background"
      >
        <ChevronLeft className="h-4 w-4" />
      </button>
      <select
        value={currentSelection}
        onChange={handleSelectChange}
        className="holiday-dev-select"
        aria-label="Select holiday background"
      >
        {PREVIEW_OPTIONS.map((opt) => (
          <option key={opt.id} value={opt.id}>
            {opt.label}
          </option>
        ))}
      </select>
      <button
        type="button"
        onClick={() => handleStep(1)}
        className="holiday-dev-step-btn"
        aria-label="Next holiday background"
      >
        <ChevronRight className="h-4 w-4" />
      </button>
    </div>
  );
}
