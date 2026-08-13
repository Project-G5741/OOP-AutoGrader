export function ScorePill({ ok, total, pct, size = 'sm' }) {
  const color = pct >= 80
    ? 'bg-success-bg text-success-text'
    : pct >= 60
      ? 'bg-warning-bg text-warning-text'
      : 'bg-error-bg text-error-text';

  const sizeClass = size === 'md'
    ? 'px-3 py-1 text-sm'
    : 'px-2.5 py-0.5 text-xs';

  return (
    <span className={`inline-flex shrink-0 items-center gap-1 rounded-full font-semibold ${sizeClass} ${color}`}>
      {ok}/{total} · {pct}%
    </span>
  );
}

export function ScoreSectionHeader({ title, score, showPill = false }) {
  return (
    <div className="mb-4 flex items-center justify-between gap-3">
      <p className="text-sm font-semibold text-foreground">{title}</p>
      {showPill && score && (
        <ScorePill ok={score.ok} total={score.total} pct={score.pct} size="md" />
      )}
    </div>
  );
}

export function hasScoreToShow(score, bundle, pillarKey) {
  if (score?.total > 0) return true;
  const raw = bundle?.scores?.[pillarKey];
  return raw != null && !Number.isNaN(Number(raw));
}

/**
 * A pillar is not applicable when the backend explicitly says so via
 * `bundle.scoreApplicability[pillarKey] === false` (e.g. challenge has has_mmd=false,
 * or no operational testcases exist). Absent signal (older cached bundle, or the
 * "class" pillar which is always applicable) defaults to applicable.
 */
export function isPillarNotApplicable(bundle, pillarKey) {
  return bundle?.scoreApplicability?.[pillarKey] === false;
}
