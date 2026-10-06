import { useEffect, useMemo, useState } from 'react';
import { X } from 'lucide-react';
import ClassScoreBreakdown from './ClassScoreBreakdown';
import MmdScoreBreakdown from './MmdScoreBreakdown';
import { formatPercent, formatText } from '../../utils/formatters';

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

export default function BulkSubmissionDrawer({ open, onClose, student, lab }) {
  const challenges = lab?.challenges || [];
  const [challengeId, setChallengeId] = useState(null);
  const [activeTab, setActiveTab] = useState('class');

  useEffect(() => {
    if (!open) return;
    setChallengeId(challenges[0]?.id || null);
    setActiveTab('class');
  }, [open, student?.studentId, challenges]);

  const indexed = useMemo(() => indexLabResult(student?.labResult), [student?.labResult]);

  const selectedChallenge = challenges.find((c) => String(c.id) === String(challengeId)) || challenges[0];
  const bundle = useMemo(() => {
    if (!selectedChallenge) return null;
    const num = selectedChallenge.challengeNumber ?? selectedChallenge.number;
    return indexed[`challenge_${num}`]
      || indexed[`n:${num}`]
      || indexed[String(selectedChallenge.id)]
      || null;
  }, [indexed, selectedChallenge]);

  const mmdApplicable = bundle?.scoreApplicability?.mmd !== false
    && selectedChallenge?.hasMmd !== false;
  const classData = bundle?.class || bundle?.classData || [];
  const mmdData = bundle?.mmd?.classes || bundle?.mmd || [];
  const mmdError = bundle?.mmd?.parseError || null;
  const overall = student?.score;

  if (!open || !student) return null;

  return (
    <div className="fixed inset-0 z-50 flex justify-end bg-background/50">
      <button type="button" className="flex-1 cursor-default" aria-label="Close drawer" onClick={onClose} />
      <aside className="flex h-full w-full max-w-xl flex-col border-l border-border bg-surface shadow-xl">
        <header className="flex items-start justify-between gap-3 border-b border-border px-5 py-4">
          <div>
            <h2 className="text-lg font-semibold text-foreground">{formatText(student.studentName)}</h2>
            <p className="text-sm text-foreground-secondary">{student.studentId}</p>
            {overall != null && (
              <p className="mt-1 text-sm text-foreground-secondary">
                Overall Score{' '}
                <span className="font-semibold text-foreground">{formatPercent(overall)}</span>
              </p>
            )}
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

        {challenges.length > 1 && (
          <div className="flex gap-1 overflow-x-auto border-b border-border px-3 pt-2">
            {challenges.map((ch) => (
              <button
                key={ch.id}
                type="button"
                onClick={() => setChallengeId(ch.id)}
                className={`whitespace-nowrap rounded-t-lg px-3 py-2 text-xs font-medium ${
                  String(challengeId) === String(ch.id)
                    ? 'bg-surface-secondary text-primary'
                    : 'text-foreground-secondary'
                }`}
              >
                {ch.name || `Challenge ${ch.challengeNumber}`}
              </button>
            ))}
          </div>
        )}

        <div className="flex gap-1 border-b border-border px-3">
          <button type="button" className={tabClass(activeTab === 'class')} onClick={() => setActiveTab('class')}>
            Class
          </button>
          {mmdApplicable && (
            <button type="button" className={tabClass(activeTab === 'mmd')} onClick={() => setActiveTab('mmd')}>
              MMD
            </button>
          )}
        </div>

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
        </div>
      </aside>
    </div>
  );
}
