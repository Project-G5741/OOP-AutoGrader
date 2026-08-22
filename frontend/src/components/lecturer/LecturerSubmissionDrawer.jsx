import { useEffect, useState } from 'react';
import { X } from 'lucide-react';
import ClassScoreBreakdown from './ClassScoreBreakdown';
import MmdScoreBreakdown from './MmdScoreBreakdown';
import ExportMenu from './ExportMenu';
import { exportChallengeBreakdown } from './exportRoster';
import { formatNumber, formatPercent, formatText } from '../../utils/formatters';
import { friendlyLoadErrorFromResponse, toFriendlyError } from '../../utils/apiError';
import { parseMmdResponse } from '../../utils/mmdResponse';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';

function authHeaders() {
  return {
    Authorization: `Bearer ${sessionStorage.getItem('accessToken')}`,
  };
}

function tabClass(active) {
  return `px-4 py-2 text-sm font-medium border-b-2 transition-colors ${
    active
      ? 'border-primary text-primary dark:text-primary'
      : 'border-transparent text-foreground-secondary hover:text-foreground'
  }`;
}

export default function LecturerSubmissionDrawer({
  open,
  onClose,
  labId,
  challengeId,
  challengeLabel,
  hasMmd = true,
  student,
}) {
  const mmdApplicable = hasMmd !== false;
  const [activeTab, setActiveTab] = useState('class');
  const [classData, setClassData] = useState([]);
  const [mmdData, setMmdData] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [mmdError, setMmdError] = useState(null);

  useEffect(() => {
    if (!open) {
      setActiveTab('class');
    }
  }, [open]);

  useEffect(() => {
    if (!mmdApplicable && activeTab === 'mmd') {
      setActiveTab('class');
    }
  }, [mmdApplicable, activeTab]);

  useEffect(() => {
    if (!open || !labId || !challengeId || !student?.studentId) {
      setClassData([]);
      setMmdData([]);
      setError(null);
      setMmdError(null);
      return;
    }

    let cancelled = false;
    const load = async () => {
      setLoading(true);
      setError(null);
      setMmdError(null);
      setClassData([]);
      setMmdData([]);

      const params = new URLSearchParams({ studentId: student.studentId });
      if (student.submissionId) {
        params.set('submissionId', student.submissionId);
      }
      const query = params.toString();
      const classUrl = `${API_BASE}/api/labs/${labId}/challenges/${challengeId}/class?${query}`;
      const mmdUrl = `${API_BASE}/api/labs/${labId}/challenges/${challengeId}/mmd?${query}`;

      try {
        const classResponse = await fetch(classUrl, { headers: authHeaders() });
        if (!classResponse.ok) {
          throw new Error(await friendlyLoadErrorFromResponse(classResponse));
        }
        const classJson = await classResponse.json();
        if (!cancelled) {
          setClassData(Array.isArray(classJson) ? classJson : (classJson?.classes ?? []));
        }
      } catch (err) {
        if (!cancelled) {
          setClassData([]);
          setError(toFriendlyError(err, 'read'));
        }
      }

      if (mmdApplicable) {
        try {
          const mmdResponse = await fetch(mmdUrl, { headers: authHeaders() });
          if (!mmdResponse.ok) {
            throw new Error(await friendlyLoadErrorFromResponse(mmdResponse));
          }
          const mmdJson = await mmdResponse.json();
          if (!cancelled) {
            const parsedMmd = parseMmdResponse(mmdJson);
            setMmdData(parsedMmd.classes);
            setMmdError(parsedMmd.parseError);
          }
        } catch (err) {
          if (!cancelled) {
            setMmdData([]);
            setMmdError(toFriendlyError(err, 'read'));
          }
        }
      }

      if (!cancelled) {
        setLoading(false);
      }
    };

    void load();
    return () => {
      cancelled = true;
    };
  }, [open, labId, challengeId, mmdApplicable, student?.studentId, student?.submissionId]);

  if (!open || !student) {
    return null;
  }

  const initials = (student.studentName || '?')
    .split(' ')
    .map((part) => part[0])
    .join('')
    .slice(0, 2)
    .toUpperCase();

  const handleExport = async (format) => {
    const fileBase = `${String(student.studentCode || 'student').replace(/\s+/g, '_')}_${String(challengeLabel || 'challenge').replace(/\s+/g, '_')}`;
    await exportChallengeBreakdown(format, {
      studentName: student.studentName,
      classData,
      mmdData,
      fileBase,
    });
  };

  return (
    <div className="fixed inset-0 z-40 flex justify-end bg-black/40">
      <button type="button" className="flex-1" aria-label="Close drawer" onClick={onClose} />
      <aside className="flex h-full w-full max-w-2xl flex-col border-l border-border-subtle bg-surface shadow-2xl dark:shadow-none">
        <div className="flex items-start justify-between gap-3 border-b border-border px-5 py-4">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-full bg-primary-light text-sm font-bold text-primary-text">
              {initials}
            </div>
            <div>
              <p className="font-semibold text-foreground">{formatText(student.studentName)}</p>
              <p className="text-xs text-foreground-secondary">ID: {formatText(student.studentCode)}</p>
            </div>
          </div>
          <button type="button" onClick={onClose} className="rounded-lg p-2 text-foreground-secondary hover:bg-surface-secondary">
            <X className="h-4 w-4" />
          </button>
        </div>

        <div className="space-y-4 border-b border-border px-5 py-4 text-sm">
          <div className="flex items-center justify-between gap-3">
            <span className="text-foreground-secondary">Challenge</span>
            <span className="font-medium text-foreground">{formatText(challengeLabel)}</span>
          </div>
          <div className="flex items-center justify-between gap-3">
            <span className="text-foreground-secondary">Overall Score</span>
            <span className="font-semibold text-foreground">{formatPercent(student.score)}</span>
          </div>
          <div className="flex items-center justify-between gap-3">
            <span className="text-foreground-secondary">Attempts</span>
            <span className="font-medium text-foreground">{formatNumber(student.attempt ?? student.attempts)}</span>
          </div>
          <div className="flex items-center justify-between gap-3">
            <span className="text-foreground-secondary">Last Submitted</span>
            <span className="font-medium text-foreground">{student.submittedAt || '—'}</span>
          </div>
        </div>

        {mmdApplicable ? (
          <div className="flex border-b border-border px-5">
            <button type="button" onClick={() => setActiveTab('class')} className={tabClass(activeTab === 'class')}>
              Declaration Test
            </button>
            <button type="button" onClick={() => setActiveTab('mmd')} className={tabClass(activeTab === 'mmd')}>
              MMD
            </button>
          </div>
        ) : null}

        <div className="flex-1 overflow-y-auto px-5 py-4">
          {loading ? (
            <div className="flex items-center justify-center py-12">
              <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
            </div>
          ) : activeTab === 'class' ? (
            error ? (
              <p className="text-sm text-warning-text">{error}</p>
            ) : (
              <ClassScoreBreakdown classData={classData} overallScore={student.score} />
            )
          ) : (
            <MmdScoreBreakdown mmdData={mmdData} mmdError={mmdError} />
          )}
        </div>

        <div className="border-t border-border px-5 py-4">
          <ExportMenu onExport={handleExport} disabled={loading} dropUp />
        </div>
      </aside>
    </div>
  );
}
