import { useEffect, useMemo, useState } from 'react';
import { X } from 'lucide-react';
import ClassScoreBreakdown from './ClassScoreBreakdown';
import MmdScoreBreakdown from './MmdScoreBreakdown';
import OperationalTestcaseBreakdown from './OperationalTestcaseBreakdown';
import { formatNumber, formatPercent, formatText } from '../../utils/formatters';
import {
  LECTURER_DRAWER_BACKDROP_HIT,
  LECTURER_DRAWER_DIVIDER,
  LECTURER_DRAWER_OVERLAY,
  LECTURER_DRAWER_PANEL,
} from './lecturerDrawerChrome';

function tabClass(active) {
  return `px-4 py-2 text-sm font-medium border-b-2 transition-colors ${
    active
      ? 'border-primary text-primary'
      : 'border-transparent text-foreground-secondary hover:text-foreground'
  }`;
}

function indexLabResult(labResult) {
  const map = {};
  if (!labResult || typeof labResult !== 'object') return map;
  for (const [key, bundle] of Object.entries(labResult)) {
    map[key] = bundle;
    const m = String(key).match(/challenge[_-]?(\d+)/i);
    if (m) map[`n:${m[1]}`] = bundle;
  }
  return map;
}

function challengeScoreFromBundle(bundle) {
  const raw = bundle?.scores?.total;
  if (raw == null || raw === '') return null;
  const n = Number(raw);
  return Number.isNaN(n) ? null : n;
}

function challengeLabel(challenge, index = 0) {
  return challenge?.name || `Challenge ${challenge?.challengeNumber ?? index + 1}`;
}

/**
 * @param {boolean} lockToChallenge when true (Bulk results on a challenge tab), hide the
 *   multi-challenge switcher and show only that challenge — same as Dashboard View Submission.
 */
export default function BulkSubmissionDrawer({
  open,
  onClose,
  student,
  lab,
  initialChallengeId = null,
  lockToChallenge = false,
}) {
  const challenges = lab?.challenges || [];
  const [challengeId, setChallengeId] = useState(null);
  const [activeTab, setActiveTab] = useState('class');

  useEffect(() => {
    if (!open) return;
    const preferred = initialChallengeId
      && challenges.some((c) => String(c.id) === String(initialChallengeId))
      ? initialChallengeId
      : (challenges[0]?.id || null);
    setChallengeId(preferred);
    setActiveTab('class');
  }, [open, student?.studentId, challenges, initialChallengeId]);

  const indexed = useMemo(() => indexLabResult(student?.labResult), [student?.labResult]);

  const selectedChallenge = challenges.find((c) => String(c.id) === String(challengeId)) || challenges[0];
  const selectedIndex = Math.max(0, challenges.findIndex((c) => String(c.id) === String(challengeId)));
  const bundle = useMemo(() => {
    if (!selectedChallenge) return null;
    const num = selectedChallenge.challengeNumber ?? selectedChallenge.number;
    return indexed[`challenge_${num}`]
      || indexed[`n:${num}`]
      || indexed[String(selectedChallenge.id)]
      || null;
  }, [indexed, selectedChallenge]);

  // Challenge has_mmd=false wins over a stale/missing scoreApplicability.mmd flag
  // (e.g. ungraded challenge used to default mmdApplicable=true in lab_result).
  const mmdApplicable = selectedChallenge?.hasMmd !== false
    && bundle?.scoreApplicability?.mmd !== false
    && (bundle?.scoreApplicability?.mmd === true || selectedChallenge?.hasMmd === true);
  const testcaseApplicable = bundle?.scoreApplicability?.testcase === true
    || (Array.isArray(bundle?.testcases) && bundle.testcases.length > 0);

  const classData = bundle?.class || bundle?.classData || [];
  const mmdData = bundle?.mmd?.classes || bundle?.mmd || [];
  const mmdError = bundle?.mmd?.parseError || null;
  const labScore = student?.score;
  const challengeScore = challengeScoreFromBundle(bundle);

  useEffect(() => {
    if (activeTab === 'mmd' && !mmdApplicable) setActiveTab('class');
    if (activeTab === 'testcase' && !testcaseApplicable) setActiveTab('class');
  }, [activeTab, mmdApplicable, testcaseApplicable]);

  if (!open || !student) return null;

  const showPillarTabs = mmdApplicable || testcaseApplicable;
  const showChallengeSwitcher = !lockToChallenge && challenges.length > 1;

  return (
    <div className={`${LECTURER_DRAWER_OVERLAY} z-50`}>
      <button
        type="button"
        className={LECTURER_DRAWER_BACKDROP_HIT}
        aria-label="Close drawer"
        onClick={onClose}
      />
      <aside className={`${LECTURER_DRAWER_PANEL} max-w-xl`}>
        <header className={`flex items-start justify-between gap-3 border-b px-5 py-4 ${LECTURER_DRAWER_DIVIDER}`}>
          <div>
            <h2 className="text-lg font-semibold text-foreground">{formatText(student.studentName)}</h2>
            <p className="text-sm text-foreground-secondary">{student.studentId}</p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-lg p-2 text-foreground-secondary hover:bg-surface-secondary"
            aria-label="Close"
          >
            <X className="h-5 w-5" />
          </button>
        </header>

        <div className={`space-y-3 border-b px-5 py-4 text-sm ${LECTURER_DRAWER_DIVIDER}`}>
          {selectedChallenge && (
            <div className="flex items-center justify-between gap-3">
              <span className="text-foreground-secondary">Challenge</span>
              <span className="font-medium text-foreground text-right">
                {formatText(challengeLabel(selectedChallenge, selectedIndex))}
              </span>
            </div>
          )}
          <div className="flex items-center justify-between gap-3">
            <span className="text-foreground-secondary">
              {lockToChallenge ? 'Overall Score' : 'Challenge Score'}
            </span>
            <span className="font-semibold text-foreground">
              {challengeScore != null ? formatNumber(challengeScore) : '—'}
            </span>
          </div>
          {!lockToChallenge && labScore != null && (
            <div className="flex items-center justify-between gap-3">
              <span className="text-foreground-secondary">Lab Score</span>
              <span className="font-semibold text-foreground">{formatPercent(labScore)}</span>
            </div>
          )}
        </div>

        {showChallengeSwitcher && (
          <div className={`flex gap-1 overflow-x-auto border-b px-3 pt-2 ${LECTURER_DRAWER_DIVIDER}`}>
            {challenges.map((ch, index) => (
              <button
                key={ch.id}
                type="button"
                onClick={() => {
                  setChallengeId(ch.id);
                  setActiveTab('class');
                }}
                className={`whitespace-nowrap rounded-t-lg px-3 py-2 text-xs font-medium ${
                  String(challengeId) === String(ch.id)
                    ? 'bg-surface text-primary shadow-sm dark:bg-background dark:text-primary-text'
                    : 'text-foreground-secondary hover:text-foreground'
                }`}
              >
                {challengeLabel(ch, index)}
              </button>
            ))}
          </div>
        )}

        {showPillarTabs && (
          <div className={`flex gap-1 border-b px-3 ${LECTURER_DRAWER_DIVIDER}`}>
            <button type="button" className={tabClass(activeTab === 'class')} onClick={() => setActiveTab('class')}>
              Declaration Test
            </button>
            {mmdApplicable && (
              <button type="button" className={tabClass(activeTab === 'mmd')} onClick={() => setActiveTab('mmd')}>
                MMD
              </button>
            )}
            {testcaseApplicable && (
              <button
                type="button"
                className={tabClass(activeTab === 'testcase')}
                onClick={() => setActiveTab('testcase')}
              >
                Operation Test
              </button>
            )}
          </div>
        )}

        <div className="flex-1 overflow-y-auto px-5 py-4">
          {!bundle && (
            <p className="text-sm text-foreground-secondary">No breakdown available for this challenge.</p>
          )}
          {bundle && activeTab === 'class' && (
            <ClassScoreBreakdown classData={classData} overallScore={bundle?.scores?.class ?? null} />
          )}
          {bundle && activeTab === 'mmd' && mmdApplicable && (
            <MmdScoreBreakdown mmdData={Array.isArray(mmdData) ? mmdData : []} mmdError={mmdError} />
          )}
          {bundle && activeTab === 'testcase' && testcaseApplicable && (
            <OperationalTestcaseBreakdown bundle={bundle} />
          )}
        </div>
      </aside>
    </div>
  );
}
