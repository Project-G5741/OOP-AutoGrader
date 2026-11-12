import React from 'react';
import { StageCard3D } from '../ui/MotionPrimitives';

export default function LecturerOverviewCard({ title, value, icon, accent }) {
  return (
    <StageCard3D maxTilt={3} className="p-5">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-xs font-semibold uppercase tracking-wider text-foreground-secondary">{title}</p>
          <p className="mt-2 text-2xl font-bold tracking-tight tabular-nums text-foreground">{value}</p>
        </div>
        <div className={`rounded-xl p-3 ${accent}`}>{icon}</div>
      </div>
    </StageCard3D>
  );
}
