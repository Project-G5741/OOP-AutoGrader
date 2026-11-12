import { ArrowRight } from 'lucide-react';
import { AnimatedCounter, StageCard3D, StaggerGroup, StaggerItem } from '../ui/MotionPrimitives';

export default function OverviewPanel({ overviewCards, onSelect }) {
  return (
    <StaggerGroup className="grid grid-cols-1 gap-5 md:grid-cols-2 xl:grid-cols-4">
      {overviewCards.map((card) => (
        <StaggerItem key={card.id} className="h-full">
          <StageCard3D
            as="button"
            type="button"
            maxTilt={3.5}
            onClick={() => onSelect(card.id)}
            className="group h-full w-full cursor-pointer p-5 text-left focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2"
          >
            <div className="flex items-start justify-between gap-4">
              <div className="min-w-0">
                <p className="text-xs font-semibold uppercase tracking-wider text-foreground-secondary">
                  {card.title}
                </p>
                <p className="mt-2.5 text-2xl font-bold tracking-tight tabular-nums text-foreground sm:text-3xl">
                  {typeof card.rawValue === 'number' && Number.isFinite(card.rawValue) ? (
                    <AnimatedCounter value={card.rawValue} formatter={card.formatter} />
                  ) : (
                    card.value
                  )}
                </p>
                <span className="mt-3 inline-flex items-center gap-1 text-xs font-medium text-primary-text transition-transform group-hover:translate-x-0.5">
                  View list
                  <ArrowRight className="h-3.5 w-3.5" aria-hidden="true" />
                </span>
              </div>
              <div className={`shrink-0 rounded-xl p-3 ${card.iconSurface || card.accent || 'bg-surface-secondary text-foreground-secondary'}`}>
                {card.icon}
              </div>
            </div>
          </StageCard3D>
        </StaggerItem>
      ))}
    </StaggerGroup>
  );
}
