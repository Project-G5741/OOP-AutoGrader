import { useMemo } from 'react';
import { formatNumber } from '../../utils/formatters';

const BUCKET_ORDER = ['0-49', '50-69', '70-84', '85-100'];

const BUCKET_BAR_CLASS = {
  '0-49': 'bg-chart-red',
  '50-69': 'bg-chart-amber',
  '70-84': 'bg-chart-blue',
  '85-100': 'bg-chart-green',
};

export default function GradeDistributionChart({ distribution = [] }) {
  const buckets = useMemo(() => {
    const countsByRange = Object.fromEntries(
      distribution.map((bucket) => [bucket.range, bucket.count ?? 0]),
    );
    return BUCKET_ORDER.map((range) => ({
      range,
      count: countsByRange[range] ?? 0,
    }));
  }, [distribution]);

  const maxCount = Math.max(...buckets.map((bucket) => bucket.count), 1);
  const hasData = buckets.some((bucket) => bucket.count > 0);

  if (!hasData) {
    return <p className="mt-3 text-sm text-foreground-secondary">No data available</p>;
  }

  return (
    <div className="mt-3 space-y-2.5" role="img" aria-label="Grade distribution bar chart">
      {buckets.map(({ range, count }) => {
        const widthPercent = count > 0 ? Math.max((count / maxCount) * 100, 6) : 0;

        return (
          <div key={range} className="grid grid-cols-[4.5rem_minmax(0,1fr)_1.75rem] items-center gap-x-2">
            <span className="text-right text-xs tabular-nums text-foreground-secondary">{range}</span>
            <div className="h-5 overflow-hidden rounded-md bg-surface-secondary">
              <div
                className={`h-full rounded-md transition-[width] duration-300 ${BUCKET_BAR_CLASS[range]}`}
                style={{ width: `${widthPercent}%` }}
                title={`${range}: ${count}`}
              />
            </div>
            <span className="text-right text-sm font-medium tabular-nums text-foreground">{formatNumber(count)}</span>
          </div>
        );
      })}
    </div>
  );
}
