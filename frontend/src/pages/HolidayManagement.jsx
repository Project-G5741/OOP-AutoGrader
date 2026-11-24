import { useCallback, useEffect, useMemo, useState } from 'react';
import { RotateCcw, Sparkles, Check, Power } from 'lucide-react';
import DashboardSection from '../components/lecturer/DashboardSection';
import DatePicker from '../components/ui/DatePicker';
import { useToast } from '../components/ui/Toast';
import {
  fetchAdminHolidayConfigFromServer,
  getHolidayScheduleRowsForAdmin,
  getLocalVietnamDateIso,
  HOLIDAY_PREVIEW_CHANGE_EVENT,
  HOLIDAY_WINDOW_PRE_DAYS,
  readAdminHolidayConfig,
  resolveActiveHolidayForDate,
  saveAdminHolidayConfigToServer,
} from '../utils/holidayCalendar';

const HOLIDAY_THUMBNAILS = {
  halloween: '/holidays/halloween-bg.webp',
  christmas: '/holidays/christmas-bg.webp',
  'new-year': '/holidays/new-year-bg.webp',
  'tet-nguyen-dan': '/holidays/tet-nguyen-dan-bg.webp',
  'reunification-labor': '/holidays/reunification-labor-bg.webp',
  'childrens-day': '/holidays/childrens-day-bg.webp',
  'mid-autumn': '/holidays/mid-autumn-bg.webp',
};

function formatDisplayDate(isoDate) {
  if (!isoDate || !/^\d{4}-\d{2}-\d{2}$/.test(isoDate)) return '—';
  const [y, m, d] = isoDate.split('-');
  return `${d}.${m}.${y}`;
}

export default function HolidayManagement() {
  const showToast = useToast();
  const [todayIso, setTodayIso] = useState(() => getLocalVietnamDateIso());
  const [adminConfig, setAdminConfig] = useState(() => readAdminHolidayConfig());
  const [draftDates, setDraftDates] = useState({});
  const [savingId, setSavingId] = useState(null);

  const syncFromStorage = useCallback(() => {
    setTodayIso(getLocalVietnamDateIso());
    setAdminConfig(readAdminHolidayConfig());
  }, []);

  useEffect(() => {
    void fetchAdminHolidayConfigFromServer().then((cfg) => {
      setAdminConfig(cfg);
    });
    window.addEventListener(HOLIDAY_PREVIEW_CHANGE_EVENT, syncFromStorage);
    window.addEventListener('focus', syncFromStorage);
    return () => {
      window.removeEventListener(HOLIDAY_PREVIEW_CHANGE_EVENT, syncFromStorage);
      window.removeEventListener('focus', syncFromStorage);
    };
  }, [syncFromStorage]);

  const rows = useMemo(
    () => getHolidayScheduleRowsForAdmin(todayIso, adminConfig),
    [todayIso, adminConfig]
  );

  const activeHoliday = useMemo(
    () => resolveActiveHolidayForDate(todayIso, adminConfig),
    [todayIso, adminConfig]
  );

  const hasAnyEarlySchedule = useMemo(
    () => rows.some((r) => r.enabledEarly),
    [rows]
  );

  const getRowDraft = (row) => {
    const draft = draftDates[row.id];
    return {
      earlyStartDate: draft?.earlyStartDate ?? row.earlyStartDate ?? todayIso,
      endDate: draft?.endDate ?? row.endDate ?? row.officialEndDate,
    };
  };

  const handleDateChange = (holidayId, field, value, row) => {
    setDraftDates((prev) => {
      const current = prev[holidayId] || {
        earlyStartDate: row.earlyStartDate || todayIso,
        endDate: row.endDate || row.officialEndDate,
      };
      return {
        ...prev,
        [holidayId]: {
          ...current,
          [field]: value,
        },
      };
    });
  };

  const handleActivateOrSaveEarly = async (row, forceImmediateToday = false) => {
    const draft = getRowDraft(row);
    const earlyStartDate = forceImmediateToday
      ? todayIso
      : draft.earlyStartDate || todayIso;
    const endDate = draft.endDate || row.officialEndDate;

    if (earlyStartDate > endDate) {
      showToast('Ngày bắt đầu sớm phải trước hoặc bằng ngày tự tắt.', 'error');
      return;
    }
    if (endDate < todayIso) {
      showToast('Ngày tự tắt đã qua so với hôm nay.', 'error');
      return;
    }

    setSavingId(row.id);
    try {
      const nextConfig = {
        ...adminConfig,
        schedules: {
          ...(adminConfig?.schedules || {}),
          [row.id]: {
            enabledEarly: true,
            earlyStartDate,
            endDate,
            preDays: row.preDays || HOLIDAY_WINDOW_PRE_DAYS,
            activatedAt: Date.now(),
          },
        },
      };
      const { config } = await saveAdminHolidayConfigToServer(nextConfig);
      setAdminConfig(config);
      setDraftDates((prev) => ({
        ...prev,
        [row.id]: { earlyStartDate, endDate },
      }));
      showToast(`Đã bật sớm ${row.name} (tự tắt sau ${formatDisplayDate(endDate)}).`, 'success');
    } finally {
      setSavingId(null);
    }
  };

  const handleDisableEarly = async (row) => {
    setSavingId(row.id);
    try {
      const nextSchedules = { ...(adminConfig?.schedules || {}) };
      delete nextSchedules[row.id];
      const nextConfig = {
        ...adminConfig,
        schedules: nextSchedules,
      };
      const { config } = await saveAdminHolidayConfigToServer(nextConfig);
      setAdminConfig(config);
      setDraftDates((prev) => {
        const copy = { ...prev };
        delete copy[row.id];
        return copy;
      });
      showToast(`Đã chuyển ${row.name} về lịch tự động.`, 'success');
    } finally {
      setSavingId(null);
    }
  };

  const handleResetAllToAuto = async () => {
    setSavingId('all');
    try {
      const { config } = await saveAdminHolidayConfigToServer({ schedules: {} });
      setAdminConfig(config);
      setDraftDates({});
      showToast('Đã đặt lại tất cả ngày lễ về lịch tự động.', 'success');
    } finally {
      setSavingId(null);
    }
  };

  return (
    <div className="max-w-full space-y-6">
      <DashboardSection
        title="Holidays"
        actions={
          hasAnyEarlySchedule ? (
            <button
              type="button"
              onClick={handleResetAllToAuto}
              disabled={savingId === 'all'}
              className="inline-flex min-h-10 items-center gap-2 rounded-xl border border-border bg-surface px-3.5 py-2 text-xs font-semibold text-foreground-secondary transition-colors hover:bg-surface-secondary hover:text-foreground disabled:opacity-50 cursor-pointer"
            >
              <RotateCcw className="h-3.5 w-3.5" />
              Về tự động tất cả
            </button>
          ) : null
        }
      >
        <div className="overflow-hidden rounded-2xl border border-border bg-surface shadow-stage-1">
          <div className="flex flex-wrap items-center justify-between gap-4 bg-surface-secondary px-5 py-3.5">
            <div className="flex flex-wrap items-center gap-3 text-sm">
              <span className="font-mono text-xs font-semibold text-foreground-muted">
                Hôm nay: {formatDisplayDate(todayIso)}
              </span>
              <span className="text-foreground-disabled">·</span>
              <span className="inline-flex items-center gap-2 font-semibold text-foreground">
                <Sparkles className="h-4 w-4 text-primary" />
                Đang hiển thị:{' '}
                {activeHoliday ? (
                  <span className="inline-flex items-center gap-2">
                    <span className="text-primary">{activeHoliday.name}</span>
                    <span
                      className={`rounded-md px-2 py-0.5 text-xs font-semibold ${
                        activeHoliday.isEarlyOverride
                          ? 'bg-success-bg text-success-text'
                          : 'bg-secondary-light text-secondary-text'
                      }`}
                    >
                      {activeHoliday.isEarlyOverride ? 'Bật sớm' : 'Tự động'}
                    </span>
                    <span className="font-mono text-xs font-normal text-foreground-secondary">
                      (Tự tắt sau {formatDisplayDate(activeHoliday.endDate)})
                    </span>
                  </span>
                ) : (
                  <span className="text-foreground-secondary">Mặc định (Không lễ)</span>
                )}
              </span>
            </div>
          </div>

          <div className="divide-y divide-border">
            {rows.map((row) => {
              const draft = getRowDraft(row);
              const isDirty =
                row.enabledEarly &&
                (draft.earlyStartDate !== row.earlyStartDate ||
                  draft.endDate !== row.endDate);
              const isBusy = savingId === row.id;

              return (
                <div
                  key={row.id}
                  className={`flex flex-col gap-4 px-5 py-4 transition-colors lg:flex-row lg:items-center lg:justify-between ${
                    row.isCurrentlyDisplaying ? 'bg-primary-light/30' : ''
                  }`}
                >
                  <div className="flex min-w-0 items-center gap-3.5">
                    <img
                      src={HOLIDAY_THUMBNAILS[row.id]}
                      alt={row.name}
                      className="h-12 w-20 shrink-0 rounded-lg border border-border object-cover"
                      loading="lazy"
                    />
                    <div className="min-w-0">
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="text-sm font-semibold text-foreground">
                          {row.name}
                        </span>
                        <span className="rounded-md bg-surface-secondary px-2 py-0.5 font-mono text-xs text-foreground-secondary">
                          {row.dateLabel}
                        </span>
                        {row.isActiveEarly ? (
                          <span className="rounded-md bg-success-bg px-2 py-0.5 text-xs font-semibold text-success-text">
                            Đang bật sớm
                          </span>
                        ) : row.isScheduledEarly ? (
                          <span className="rounded-md bg-gold-bg px-2 py-0.5 text-xs font-semibold text-gold-text">
                            Hẹn bật từ {formatDisplayDate(row.earlyStartDate)}
                          </span>
                        ) : row.isActiveAuto ? (
                          <span className="rounded-md bg-secondary-light px-2 py-0.5 text-xs font-semibold text-secondary-text">
                            Đang bật tự động
                          </span>
                        ) : (
                          <span className="rounded-md bg-surface-secondary px-2 py-0.5 text-xs text-foreground-muted">
                            Tự động ({formatDisplayDate(row.autoWindowStart)} → {formatDisplayDate(row.officialEndDate)})
                          </span>
                        )}
                      </div>
                      <p className="mt-1 font-mono text-xs text-foreground-muted">
                        Kỳ lễ tới: {formatDisplayDate(row.officialStartDate)}
                        {row.officialEndDate !== row.officialStartDate
                          ? ` – ${formatDisplayDate(row.officialEndDate)}`
                          : ''}
                      </p>
                    </div>
                  </div>

                  <div className="flex flex-wrap items-center gap-3">
                    <div className="flex items-center gap-2">
                      <label
                        htmlFor={`holiday-start-${row.id}`}
                        className="text-xs font-medium text-foreground-secondary whitespace-nowrap"
                      >
                        Bật từ
                      </label>
                      <DatePicker
                        id={`holiday-start-${row.id}`}
                        value={draft.earlyStartDate}
                        onChange={(val) =>
                          handleDateChange(row.id, 'earlyStartDate', val, row)
                        }
                        className="w-32 font-mono text-xs"
                      />
                    </div>

                    <div className="flex items-center gap-2">
                      <label
                        htmlFor={`holiday-end-${row.id}`}
                        className="text-xs font-medium text-foreground-secondary whitespace-nowrap"
                      >
                        Tự tắt sau
                      </label>
                      <DatePicker
                        id={`holiday-end-${row.id}`}
                        value={draft.endDate}
                        onChange={(val) =>
                          handleDateChange(row.id, 'endDate', val, row)
                        }
                        className="w-32 font-mono text-xs"
                      />
                    </div>

                    <div className="flex items-center gap-2">
                      {!row.isActiveEarly && (
                        <button
                          type="button"
                          disabled={isBusy}
                          onClick={() => handleActivateOrSaveEarly(row, true)}
                          className="inline-flex min-h-9 items-center gap-1.5 rounded-lg bg-primary px-3 py-1.5 text-xs font-semibold text-on-primary transition-colors hover:bg-primary-hover disabled:opacity-50 cursor-pointer"
                        >
                          <Sparkles className="h-3.5 w-3.5" />
                          Bật sớm ngay
                        </button>
                      )}

                      {(isDirty || (!row.enabledEarly && draft.earlyStartDate !== todayIso)) && (
                        <button
                          type="button"
                          disabled={isBusy}
                          onClick={() => handleActivateOrSaveEarly(row, false)}
                          className="inline-flex min-h-9 items-center gap-1.5 rounded-lg bg-navy px-3 py-1.5 text-xs font-semibold text-on-navy transition-colors hover:bg-navy-hover dark:bg-primary dark:text-on-primary disabled:opacity-50 cursor-pointer"
                        >
                          <Check className="h-3.5 w-3.5" />
                          Lưu lịch
                        </button>
                      )}

                      {row.enabledEarly && (
                        <button
                          type="button"
                          disabled={isBusy}
                          onClick={() => handleDisableEarly(row)}
                          className="inline-flex min-h-9 items-center gap-1.5 rounded-lg border border-border bg-surface-secondary px-3 py-1.5 text-xs font-semibold text-foreground-secondary transition-colors hover:bg-surface-tertiary hover:text-foreground disabled:opacity-50 cursor-pointer"
                        >
                          <Power className="h-3.5 w-3.5" />
                          Tắt sớm
                        </button>
                      )}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </DashboardSection>
    </div>
  );
}
