import { useCallback, useMemo, useRef, useState } from 'react';
import { SyncLoader } from 'react-spinners';
import { Eye, FlaskConical, Upload } from 'lucide-react';
import Combobox from '../ui/Combobox';
import Progress from '../ui/Progress';
import {
  challengeCountForLab,
  modeMatchesLab,
  parseBulkMainFolder,
} from '../../utils/bulkFolderParse';
import { useTheme } from '../../context/ThemeContext';
import { theme } from '../../theme/tokens';
import { apiFetch } from '../../utils/apiFetch';
import { authHeaders } from '../../utils/authHeaders';
import { friendlyLoadErrorFromResponse, toFriendlyError } from '../../utils/apiError';
import { formatNumber, formatText } from '../../utils/formatters';
import { computeWithinBatchPlagiarism, attachClientHashes } from '../../utils/withinBatchPlagiarism';
import BulkSubmissionDrawer from './BulkSubmissionDrawer';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';

function resultsTabClass(active) {
  return `px-4 py-2 text-sm font-medium border-b-2 transition-colors ${
    active
      ? 'border-primary text-primary'
      : 'border-transparent text-foreground-secondary hover:text-foreground'
  }`;
}

function challengeTabLabel(challenge, index) {
  return challenge?.name || `Challenge ${challenge?.challengeNumber ?? index + 1}`;
}

/**
 * Score for the active results tab from an ephemeral bulk row payload.
 * Overview → lab total; challenge → lab_result.challenge_N.scores.total or challengeResult[id].
 */
export function scoreForResultsTab(row, resultsTab, challenges = []) {
  if (!row || row.error) return null;
  if (resultsTab === 'overview') {
    return row.score ?? null;
  }
  const challenge = challenges.find((c) => String(c.id) === String(resultsTab));
  if (!challenge) return null;
  const num = challenge.challengeNumber ?? challenge.number;
  const bundle = row.payload?.labResult?.[`challenge_${num}`]
    || row.payload?.labResult?.[`challenge_${String(num)}`];
  const fromBundle = bundle?.scores?.total;
  if (fromBundle != null && fromBundle !== '') {
    const n = Number(fromBundle);
    return Number.isNaN(n) ? null : n;
  }
  const byId = row.payload?.challengeResult?.[challenge.id]
    ?? row.payload?.challengeResult?.[String(challenge.id)];
  if (byId != null && byId !== '') {
    const n = Number(byId);
    return Number.isNaN(n) ? null : n;
  }
  return null;
}

async function walkEntry(entry, pathPrefix, collected) {
  return new Promise((resolve, reject) => {
    if (entry.isFile) {
      entry.file((file) => {
        collected.push({ file, relativePath: pathPrefix + file.name });
        resolve();
      }, reject);
    } else if (entry.isDirectory) {
      const reader = entry.createReader();
      const readAllBatches = (accumulated = []) => {
        reader.readEntries(async (batch) => {
          if (batch.length === 0) {
            for (const child of accumulated) {
              await walkEntry(child, pathPrefix + entry.name + '/', collected);
            }
            resolve();
          } else {
            readAllBatches(accumulated.concat(batch));
          }
        }, reject);
      };
      readAllBatches();
    } else {
      resolve();
    }
  });
}

function buildStudentMultipart(student) {
  const form = new FormData();
  for (const item of student.files) {
    const rel = `${student.folderName}/${item.studentRelativePath}`;
    form.append('files', item.file, rel);
  }
  return form;
}

export default function BulkGradingPanel({ labs = [] }) {
  const { isDark } = useTheme();
  const inputRef = useRef(null);
  const cancelGradingRef = useRef(false);
  const [labId, setLabId] = useState('');
  const [mode, setMode] = useState('EXAM');
  const [isDragging, setIsDragging] = useState(false);
  const [parseResult, setParseResult] = useState(null);
  const [folderLabel, setFolderLabel] = useState('');
  const [grading, setGrading] = useState(false);
  const [gradedCount, setGradedCount] = useState(0);
  const [rows, setRows] = useState([]);
  const [gradeError, setGradeError] = useState(null);
  const [drawerStudent, setDrawerStudent] = useState(null);
  const [resultsTab, setResultsTab] = useState('overview');
  const [gradingStoppedEarly, setGradingStoppedEarly] = useState(false);

  const selectedLab = useMemo(
    () => labs.find((lab) => String(lab.id) === String(labId)) || null,
    [labs, labId],
  );
  const labChallenges = useMemo(
    () => (Array.isArray(selectedLab?.challenges) ? selectedLab.challenges : []),
    [selectedLab],
  );
  const challengeCount = challengeCountForLab(selectedLab);
  const modeOk = modeMatchesLab(mode, challengeCount);
  const modeBlockMessage = !selectedLab
    ? null
    : mode === 'EXAM' && challengeCount !== 1
      ? `Exam mode requires a lab with exactly 1 challenge (this lab has ${challengeCount}).`
      : mode === 'LAB' && challengeCount < 1
        ? 'Lab mode requires a lab with at least one challenge.'
        : null;

  const drawerInitialChallengeId = useMemo(() => {
    if (resultsTab === 'overview') return labChallenges[0]?.id || null;
    return labChallenges.some((c) => String(c.id) === String(resultsTab))
      ? resultsTab
      : (labChallenges[0]?.id || null);
  }, [resultsTab, labChallenges]);
  const lockDrawerToChallenge = resultsTab !== 'overview';

  const gradingTotal = parseResult?.accepted?.length || rows.length || 0;
  const gradingPercent = gradingTotal > 0
    ? Math.min(100, Math.round((gradedCount / gradingTotal) * 100))
    : 0;
  const gradingProgressLabel = grading
    ? 'Grading in progress'
    : gradingStoppedEarly
      ? 'Grading stopped'
      : 'Grading complete';
  const gradingBarClass = grading && gradedCount < gradingTotal
    ? 'bg-primary'
    : gradingStoppedEarly && !grading
      ? 'bg-warning'
      : 'bg-success';
  const syncLoaderColor = useMemo(
    () => (isDark ? theme.dark.secondary : theme.light.secondary),
    [isDark],
  );

  const applyEntries = useCallback((entries, label) => {
    const result = parseBulkMainFolder(entries, mode);
    setParseResult(result);
    setFolderLabel(label || 'dropped folder');
    setRows([]);
    setGradedCount(0);
    setGradeError(null);
    setGradingStoppedEarly(false);
    setDrawerStudent(null);
    setResultsTab('overview');
  }, [mode]);

  const handleInputFiles = (fileList) => {
    const collected = Array.from(fileList).map((file) => ({
      file,
      relativePath: file.webkitRelativePath || file.name,
    }));
    const top = collected[0]?.relativePath?.split('/')[0] || 'folder';
    applyEntries(collected, top);
    if (inputRef.current) inputRef.current.value = '';
  };

  const handleDrop = async (event) => {
    event.preventDefault();
    setIsDragging(false);
    const items = event.dataTransfer?.items;
    if (!items?.length) return;
    const topLevel = Array.from(items).map((item) => item.webkitGetAsEntry()).filter(Boolean);
    const collected = [];
    for (const entry of topLevel) {
      await walkEntry(entry, '', collected);
    }
    applyEntries(collected, topLevel[0]?.name || 'folder');
  };

  const canStart =
    modeOk
    && !grading
    && parseResult
    && parseResult.accepted.length > 0;

  const requestCancelGrading = () => {
    cancelGradingRef.current = true;
  };

  const startGrading = async () => {
    if (!canStart || !selectedLab) return;
    cancelGradingRef.current = false;
    setGradingStoppedEarly(false);
    setGrading(true);
    setGradeError(null);
    setRows([]);
    setGradedCount(0);
    setDrawerStudent(null);
    setResultsTab('overview');

    const accepted = parseResult.accepted;
    const nextRows = [];
    const payloadsByIrn = {};
    let stoppedEarly = false;

    for (let i = 0; i < accepted.length; i += 1) {
      if (cancelGradingRef.current) {
        stoppedEarly = true;
        break;
      }
      const student = accepted[i];
      try {
        const form = buildStudentMultipart(student);
        const response = await apiFetch(
          `${API_BASE}/api/lecturer/labs/${selectedLab.id}/bulk-grade?mode=${encodeURIComponent(mode)}`,
          {
            method: 'POST',
            headers: authHeaders(),
            body: form,
          },
        );
        if (!response.ok) {
          throw new Error(await friendlyLoadErrorFromResponse(response));
        }
        const data = await response.json();
        const score = data.score ?? data.totalScore ?? null;
        const labResult = data.labResult || data.lab_result || {};
        const challengeResult = data.challengeResult || {};
        const fileHashes = data.fileHashes || data.contentHashes || null;
        payloadsByIrn[student.studentId] = {
          ...student,
          score,
          labResult,
          challengeResult,
          fileHashes,
          challenges: labChallenges,
        };
        nextRows.push({
          studentId: student.studentId,
          studentName: student.studentName,
          score,
          plagiarismLabel: '—',
          error: null,
          payload: payloadsByIrn[student.studentId],
        });
      } catch (err) {
        nextRows.push({
          studentId: student.studentId,
          studentName: student.studentName,
          score: null,
          plagiarismLabel: '—',
          error: toFriendlyError(err, 'upload'),
          payload: null,
        });
      }
      setGradedCount(i + 1);
    }

    if (stoppedEarly) {
      setGradingStoppedEarly(true);
    }

    const withHashes = await attachClientHashes(Object.values(payloadsByIrn));
    const plag = computeWithinBatchPlagiarism(withHashes);
    setRows(nextRows.map((row) => ({
      ...row,
      plagiarismLabel: plag[row.studentId]?.label || '—',
      plagiarismOverlap: plag[row.studentId]?.overlap ?? null,
      payload: payloadsByIrn[row.studentId]
        ? {
            ...payloadsByIrn[row.studentId],
            fileHashes: withHashes.find((h) => h.studentId === row.studentId)?.fileHashes
              || payloadsByIrn[row.studentId].fileHashes,
          }
        : null,
    })));

    setGrading(false);
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-end gap-4">
        <div className="flex min-w-[14rem] flex-col gap-1.5 text-sm">
          <span className="font-semibold tracking-tight text-foreground">Lab</span>
          <Combobox
            aria-label="Lab"
            value={labId}
            disabled={grading}
            placeholder="Select lab…"
            icon={FlaskConical}
            options={labs.map((lab) => ({
              value: String(lab.id),
              label: lab.name || lab.labName || String(lab.id),
            }))}
            onChange={(next) => {
              setLabId(next);
              setRows([]);
              setGradedCount(0);
              setResultsTab('overview');
            }}
          />
        </div>

        <div className="flex flex-col gap-1 text-sm">
          <span className="font-medium text-foreground">Mode</span>
          <div className="inline-flex overflow-hidden rounded-lg border border-border">
            {['LAB', 'EXAM'].map((m) => (
              <button
                key={m}
                type="button"
                disabled={grading}
                onClick={() => {
                  setMode(m);
                  setParseResult(null);
                  setRows([]);
                  setResultsTab('overview');
                }}
                className={`px-4 py-2 text-sm font-medium ${
                  mode === m
                    ? 'bg-primary text-white'
                    : 'bg-surface text-foreground-secondary hover:bg-surface-secondary'
                }`}
              >
                {m === 'LAB' ? 'Lab' : 'Exam'}
              </button>
            ))}
          </div>
        </div>

        {selectedLab && (
          <p className="text-xs text-foreground-muted pb-2">
            Challenges: {challengeCount}
          </p>
        )}
      </div>

      {modeBlockMessage && (
        <p className="rounded-lg border border-warning/40 bg-warning/10 px-3 py-2 text-sm text-warning-text">
          {modeBlockMessage}
        </p>
      )}

      <div
        onDragOver={(e) => { e.preventDefault(); setIsDragging(true); }}
        onDragLeave={() => setIsDragging(false)}
        onDrop={handleDrop}
        className={`rounded-xl border-2 border-dashed px-6 py-10 text-center transition-colors ${
          isDragging ? 'border-primary bg-primary/5' : 'border-border bg-surface-secondary'
        }`}
      >
        <Upload className="mx-auto mb-3 h-8 w-8 text-primary" />
        <p className="text-sm font-medium text-foreground">Drop Main folder here</p>
        <p className="mt-1 text-xs text-foreground-muted">
          {mode === 'EXAM'
            ? 'Main → IRN_Name → .java / .mmd'
            : 'Main → IRN_Name → challenge_n → .java / .mmd'}
        </p>
        <button
          type="button"
          disabled={grading || !modeOk}
          onClick={() => inputRef.current?.click()}
          className="mt-4 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-white disabled:opacity-50"
        >
          Select folder
        </button>
        <input
          ref={inputRef}
          type="file"
          className="hidden"
          // @ts-expect-error webkitdirectory
          webkitdirectory=""
          directory=""
          multiple
          onChange={(e) => handleInputFiles(e.target.files || [])}
        />
      </div>

      {parseResult && (
        <div className="space-y-3 rounded-xl border border-border bg-surface p-4">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div>
              <p className="text-sm font-semibold text-foreground">Parsed: {folderLabel}</p>
              <p className="text-xs text-foreground-muted">
                {parseResult.accepted.length} accepted
                {parseResult.skipped.length ? ` · ${parseResult.skipped.length} skipped` : ''}
              </p>
            </div>
            <div className="flex gap-2">
              <button
                type="button"
                disabled={grading}
                onClick={() => {
                  cancelGradingRef.current = false;
                  setParseResult(null);
                  setFolderLabel('');
                  setRows([]);
                  setGradedCount(0);
                  setGradingStoppedEarly(false);
                  setResultsTab('overview');
                }}
                className="rounded-lg border border-border px-3 py-2 text-sm text-foreground-secondary hover:bg-surface-secondary"
              >
                Replace folder
              </button>
              <button
                type="button"
                disabled={!canStart}
                onClick={startGrading}
                className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
              >
                Start Grading ({parseResult.accepted.length})
              </button>
              {grading && (
                <button
                  type="button"
                  onClick={requestCancelGrading}
                  className="rounded-lg border border-error/40 px-4 py-2 text-sm font-semibold text-error-text hover:bg-error-bg"
                >
                  Cancel
                </button>
              )}
            </div>
          </div>

          {parseResult.skipped.length > 0 && (
            <ul className="max-h-32 overflow-auto text-xs text-warning-text">
              {parseResult.skipped.map((s) => (
                <li key={`${s.path}-${s.reason}`}>⚠ {s.path}: {s.reason}</li>
              ))}
            </ul>
          )}
          {parseResult.accepted.length > 0 && (
            <ul
              className="grid max-h-40 grid-cols-[repeat(auto-fill,minmax(12rem,1fr))] gap-x-3 gap-y-1 overflow-auto text-xs text-foreground-secondary"
              aria-label="Accepted student folders"
            >
              {parseResult.accepted.map((s) => (
                <li key={s.folderName} className="min-w-0 truncate" title={`${s.studentId}_${s.studentName}`}>
                  ✓ {s.studentId}_{s.studentName}
                </li>
              ))}
            </ul>
          )}
        </div>
      )}

      {grading && (
        <div
          className="space-y-4 rounded-xl border border-border bg-surface p-6"
          aria-busy="true"
          aria-live="polite"
        >
          <div className="space-y-2">
            <div className="flex flex-wrap items-center justify-between gap-2 text-sm">
              <span className="font-medium text-foreground">{gradingProgressLabel}</span>
              <span className="tabular-nums text-foreground-muted">{gradingPercent}%</span>
            </div>
            <Progress
              value={gradedCount}
              max={gradingTotal}
              aria-label="Bulk grading progress"
              barClassName={gradingBarClass}
            />
          </div>
          <div className="flex flex-col items-center justify-center gap-3 py-8">
            <SyncLoader color={syncLoaderColor} size={22} margin={3} speedMultiplier={0.5} />
            <p className="text-sm text-foreground-muted">Grading submissions…</p>
          </div>
        </div>
      )}

      {!grading && rows.length > 0 && (
        <div className="space-y-3">
          {gradeError && <p className="text-sm text-error">{gradeError}</p>}

          {gradingStoppedEarly && (
            <p className="text-sm text-foreground-muted">
              Grading stopped — showing {rows.length} of {gradingTotal} students.
            </p>
          )}

          {labChallenges.length > 0 && (
            <div className="flex min-w-0 flex-wrap gap-2 border-b border-border pb-2 sm:gap-3">
              <button
                type="button"
                onClick={() => setResultsTab('overview')}
                className={resultsTabClass(resultsTab === 'overview')}
              >
                Overview
              </button>
              {labChallenges.map((challenge, index) => (
                <button
                  key={challenge.id}
                  type="button"
                  onClick={() => setResultsTab(challenge.id)}
                  className={resultsTabClass(String(resultsTab) === String(challenge.id))}
                >
                  {challengeTabLabel(challenge, index)}
                </button>
              ))}
            </div>
          )}

          <div className="overflow-x-auto rounded-xl border border-border bg-surface">
            <table className="min-w-full text-sm">
              <thead>
                <tr className="border-b border-border text-left text-foreground-secondary">
                  <th className="px-4 py-3 font-semibold">Student</th>
                  <th className="px-4 py-3 font-semibold">ID</th>
                  <th className="px-4 py-3 font-semibold">Score</th>
                  <th className="px-4 py-3 font-semibold">Plagiarism</th>
                  <th className="px-4 py-3 font-semibold">Action</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((row) => {
                  const displayScore = scoreForResultsTab(row, resultsTab, labChallenges);
                  return (
                    <tr key={row.studentId} className="border-b border-border last:border-0">
                      <td className="px-4 py-3 text-foreground">{formatText(row.studentName)}</td>
                      <td className="px-4 py-3 text-foreground">{row.studentId}</td>
                      <td className="px-4 py-3 font-semibold text-foreground">
                        {row.error ? '—' : formatNumber(displayScore)}
                      </td>
                      <td className="px-4 py-3 text-foreground-secondary">
                        {row.error ? '—' : (row.plagiarismLabel || '—')}
                      </td>
                      <td className="px-4 py-3">
                        {row.error ? (
                          <span className="text-xs text-error">{row.error}</span>
                        ) : (
                          <button
                            type="button"
                            disabled={!row.payload}
                            onClick={() => setDrawerStudent(row.payload)}
                            className="inline-flex items-center gap-1.5 rounded-md bg-success px-3 py-1.5 text-xs font-medium text-white disabled:opacity-40"
                          >
                            <Eye className="h-3.5 w-3.5" />
                            View Submission
                          </button>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

      <BulkSubmissionDrawer
        open={Boolean(drawerStudent)}
        onClose={() => setDrawerStudent(null)}
        student={drawerStudent}
        lab={selectedLab}
        initialChallengeId={drawerInitialChallengeId}
        lockToChallenge={lockDrawerToChallenge}
      />
    </div>
  );
}
