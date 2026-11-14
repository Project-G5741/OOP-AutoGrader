import { formatNumber } from '../../utils/formatters';

export const PILLAR_EXPORT_COLUMNS = [
  { key: 'mmdScore', label: 'MMD' },
  { key: 'declarationScore', label: 'Declaration test' },
  { key: 'operationalScore', label: 'Operational Testcase' },
];

export function formatPillarScore(value) {
  return formatNumber(value);
}

function challengeBundle(row, challenge) {
  const num = challenge?.challengeNumber ?? challenge?.number;
  if (num == null) return null;
  return row?.payload?.labResult?.[`challenge_${num}`]
    || row?.payload?.labResult?.[`challenge_${String(num)}`]
    || null;
}

function weightedPillar(row, challenges, scoreKey, applicabilityKey) {
  let weightSum = 0;
  let earned = 0;
  let any = false;
  for (const challenge of challenges) {
    const bundle = challengeBundle(row, challenge);
    if (bundle?.scoreApplicability?.[applicabilityKey] !== true) continue;
    any = true;
    const weight = Math.max(1, Number(challenge.weight) || 1);
    const raw = Number(bundle?.scores?.[scoreKey] ?? 0);
    const pct = Number.isFinite(raw) ? raw : 0;
    weightSum += weight;
    earned += weight * pct;
  }
  if (!any || weightSum <= 0) return null;
  return Math.floor((earned / weightSum) * 100) / 100;
}

/**
 * Pillar scores for the active bulk results tab.
 * Overview is the weighted lab rollup of the graded attempt.
 * A challenge tab uses that challenge's scores from the same attempt.
 */
export function pillarsForResultsTab(row, resultsTab, challenges = []) {
  const empty = { mmdScore: null, declarationScore: null, operationalScore: null };
  if (!row || row.error) return empty;
  if (resultsTab === 'overview') {
    return {
      mmdScore: weightedPillar(row, challenges, 'mmd', 'mmd'),
      declarationScore: weightedPillar(row, challenges, 'class', 'class'),
      operationalScore: weightedPillar(row, challenges, 'testcase', 'testcase'),
    };
  }
  const challenge = challenges.find((item) => String(item.id) === String(resultsTab));
  const bundle = challenge ? challengeBundle(row, challenge) : null;
  const applicability = bundle?.scoreApplicability || {};
  const scores = bundle?.scores || {};
  const pick = (scoreKey, applicabilityKey) => (
    applicability[applicabilityKey] === false ? null : (scores[scoreKey] ?? null)
  );
  return {
    mmdScore: pick('mmd', 'mmd'),
    declarationScore: pick('class', 'class'),
    operationalScore: pick('testcase', 'testcase'),
  };
}

export function pillarExportFields(scores, { blank = '—' } = {}) {
  const fields = {};
  for (const column of PILLAR_EXPORT_COLUMNS) {
    const value = scores?.[column.key];
    fields[column.label] = value == null || value === '' ? blank : formatPillarScore(value);
  }
  return fields;
}
