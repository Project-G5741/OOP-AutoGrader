import { cn } from './cn';

export default function Progress({
  value = 0,
  max = 100,
  className = '',
  barClassName = '',
  'aria-label': ariaLabel = 'Progress',
}) {
  const safeMax = max > 0 ? max : 100;
  const percent = Math.min(100, Math.max(0, (value / safeMax) * 100));

  return (
    <div
      role="progressbar"
      aria-label={ariaLabel}
      aria-valuemin={0}
      aria-valuemax={safeMax}
      aria-valuenow={Math.min(value, safeMax)}
      className={cn('h-2.5 w-full overflow-hidden rounded-full bg-surface-tertiary', className)}
    >
      <div
        className={cn(
          'h-full rounded-full transition-[width] duration-300 ease-out',
          barClassName,
        )}
        style={{ width: `${percent}%` }}
      />
    </div>
  );
}
