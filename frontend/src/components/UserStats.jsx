import { ChalkboardTeacher, GraduationCap, UsersThree } from '@phosphor-icons/react';
import { AnimatedCounter, StageCard3D, StaggerGroup, StaggerItem } from './ui/MotionPrimitives';

const STAT_META = [
  { icon: UsersThree, pillClass: 'bg-navy-light text-navy dark:text-foreground' },
  { icon: GraduationCap, pillClass: 'bg-secondary-light text-secondary-text' },
  { icon: ChalkboardTeacher, pillClass: 'bg-gold-bg text-gold-text' },
];

export default function UserStats({ stats }) {
  return (
    <StaggerGroup className="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-3">
      {stats.map(({ label, value, color }, index) => {
        const meta = STAT_META[index % STAT_META.length];
        const Icon = meta.icon;
        const numeric = typeof value === 'number' ? value : null;
        return (
          <StaggerItem key={label} className="h-full">
            <StageCard3D maxTilt={3} className="h-full p-4 sm:p-5">
              <div className="flex items-start justify-between gap-3">
                <div>
                  <span className="text-xs font-semibold uppercase tracking-wider text-foreground-muted">
                    {label}
                  </span>
                  <p className={`mt-2 text-2xl font-bold tabular-nums ${color}`}>
                    {numeric !== null ? <AnimatedCounter value={numeric} /> : value}
                  </p>
                </div>
                <div className={`rounded-xl p-2.5 ${meta.pillClass}`}>
                  <Icon weight="duotone" className="h-5 w-5" aria-hidden="true" />
                </div>
              </div>
            </StageCard3D>
          </StaggerItem>
        );
      })}
    </StaggerGroup>
  );
}
