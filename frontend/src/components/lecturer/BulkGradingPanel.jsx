import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Eye, FlaskConical } from 'lucide-react';
import Combobox from '../ui/Combobox';
import Progress from '../ui/Progress';
import LottieAnimation from '../ui/LottieAnimation';
import { GradingActivityVisual } from '../ui/MotionPrimitives';
import uploadCloudData from '../../assets/lottie/upload-cloud.json';
import {
  challengeCountForLab,
  modeMatchesLab,
  parseBulkMainFolder,
} from '../../utils/bulkFolderParse';
import { apiFetch } from '../../utils/apiFetch';
import { authHeaders } from '../../utils/authHeaders';
import { readFriendlyApiError, toFriendlyError } from '../../utils/apiError';
import { formatNumber, formatText } from '../../utils/formatters';
import { computeWithinBatchPlagiarism, attachClientHashes } from '../../utils/withinBatchPlagiarism';
import { SUCCESS_ACTION_BUTTON } from '../../theme/statusClasses';
import ExportMenu from './ExportMenu';
import { exportDataset, exportRowsAsCsv, exportRowsAsExcel } from './exportRoster';
import { PILLAR_EXPORT_COLUMNS, formatPillarScore, pillarExportFields, pillarsForResultsTab } from './pillarScoreColumns';
import BulkSubmissionDrawer from './BulkSubmissionDrawer';
import { desktopApiBase } from '../../utils/desktopMode';

const API_BASE = desktopApiBase();
const BULK_RESULTS_PAGE_SIZE = 12;
const IS_DESKTOP_LECTURER = import.meta.env.VITE_APP_MODE === 'desktop-lecturer';
const BULK_EXPORT_FORMATS = IS_DESKTOP_LECTURER
  ? ['excel', 'csv']
  : ['excel', 'pdf', 'csv'];

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
  const [resultsPage, setResultsPage] = useState(0);
  /** @type {'loading'|'ready'|'empty-roster'|'no-quarter'|'error'} */
  const [membershipStatus, setMembershipStatus] = useState('loading');
  const [enrolledIrns, setEnrolledIrns] = useState(() => new Set());
  const [membershipError, setMembershipError] = useState(null);
  const [retainedNotice, setRetainedNotice] = useState(false);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      setMembershipStatus('loading');
      setMembershipError(null);
      try {
        const response = await apiFetch(
          `${API_BASE}/api/lecturer/bulk-grading/current-term-student-irns`,
          { headers: authHeaders() },
        );
        if (cancelled) return;
        if (response.status === 409) {
          setEnrolledIrns(new Set());
          setMembershipStatus('no-quarter');
          return;
        }
        if (!response.ok) {
          throw new Error(await readFriendlyApiError(response, 'bulk-grade'));
        }
        const data = await response.json();
        const list = Array.isArray(data?.irns) ? data.irns : [];
        setEnrolledIrns(new Set(list.map((code) => String(code))));
        setMembershipStatus(list.length > 0 ? 'ready' : 'empty-roster');
      } catch (err) {
        if (cancelled) return;
        setEnrolledIrns(new Set());
        setMembershipStatus('error');
        setMembershipError(toFriendlyError(err, 'bulk-grade'));
      }
    })();
    return () => { cancelled = true; };
  }, []);

  useEffect(() => {
    if (!IS_DESKTOP_LECTURER) return undefined;
    let cancelled = false;
    (async () => {
      try {
        const response = await fetch(`${API_BASE}/api/desktop/bulk-batch`);
        if (!response.ok || cancelled) return;
        const data = await response.json();
        if (!data?.present || !data.batch || cancelled) return;
        const batch = data.batch;
        if (batch.labId != null) setLabId(String(batch.labId));
        if (batch.mode === 'LAB' || batch.mode === 'EXAM') setMode(batch.mode);
        if (Array.isArray(batch.rows) && batch.rows.length > 0) {
          setRows(batch.rows);
          setGradedCount(batch.rows.length);
          setRetainedNotice(true);
        }
        if (batch.folderLabel) setFolderLabel(String(batch.folderLabel));
        if (typeof batch.gradingStoppedEarly === 'boolean') {
          setGradingStoppedEarly(batch.gradingStoppedEarly);
        }
        if (batch.resultsTab != null) setResultsTab(batch.resultsTab);
      } catch {
        // Retained-batch hydrate is best-effort.
      }
    })();
    return () => { cancelled = true; };
  }, []);

  const handleDownloadEnrollmentIrns = useCallback(async (format) => {
    if (enrolledIrns.size === 0) return;
    const rows = [...enrolledIrns]
      .sort()
      .map((student_code) => ({ student_code }));
    const fileBase = 'current_term_enrollment_irns';
    if (format === 'csv') {
      exportRowsAsCsv(rows, fileBase);
      return;
    }
    await exportRowsAsExcel(rows, fileBase);
  }, [enrolledIrns]);

  const selectedLab = useMemo(
    () => labs.find((lab) => String(lab.id) === String(labId)) || null,
    [labs, labId],
  );

  const persistFinishedBatch = useCallback(async (finalRows, stoppedEarly) => {
    if (!IS_DESKTOP_LECTURER || !selectedLab || !finalRows.length) return;
    try {
      await fetch(`${API_BASE}/api/desktop/bulk-batch`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          labId: String(selectedLab.id),
          labName: selectedLab.name || selectedLab.labName || '',
          mode,
          folderLabel,
          gradingStoppedEarly: stoppedEarly,
          resultsTab: 'overview',
          savedAt: new Date().toISOString(),
          rows: finalRows,
        }),
      });
      setRetainedNotice(true);
    } catch {
      // Persistence failure should not undo on-screen results.
    }
  }, [selectedLab, mode, folderLabel]);

  const clearRetainedBatch = useCallback(async () => {
    if (!IS_DESKTOP_LECTURER) return;
    try {
      await fetch(`${API_BASE}/api/desktop/bulk-batch`, { method: 'DELETE' });
    } catch {
      // Still clear the local table so the lecturer can continue.
    }
    setRows([]);
    setGradedCount(0);
    setParseResult(null);
    setGradeError(null);
    setGradingStoppedEarly(false);
    setDrawerStudent(null);
    setResultsTab('overview');
    setResultsPage(0);
    setRetainedNotice(false);
  }, []);
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
  const resultsPagination = useMemo(() => {
    const total = rows.length;
    const totalPages = Math.max(Math.ceil(total / BULK_RESULTS_PAGE_SIZE), 1);
    const page = Math.min(Math.max(resultsPage, 0), totalPages - 1);
    return { page, size: BULK_RESULTS_PAGE_SIZE, total, totalPages };
  }, [rows.length, resultsPage]);

  const pagedRows = useMemo(() => {
    const start = resultsPagination.page * BULK_RESULTS_PAGE_SIZE;
    return rows.slice(start, start + BULK_RESULTS_PAGE_SIZE);
  }, [rows, resultsPagination.page]);

  const showResultsPagination =
    rows.length > 0
    && (resultsPagination.totalPages > 1 || resultsPagination.total > resultsPagination.size);

  const bulkResultsTabLabel = useMemo(() => {
    if (resultsTab === 'overview') return 'Overview';
    const index = labChallenges.findIndex((c) => String(c.id) === String(resultsTab));
    const challenge = index >= 0 ? labChallenges[index] : null;
    return challenge ? challengeTabLabel(challenge, index) : 'Challenge';
  }, [resultsTab, labChallenges]);

  const handleExportBulkResults = useCallback(async (format) => {
    if (!rows.length) return;
    const exportRows = rows.map((row) => ({
      Student: formatText(row.studentName),
      ID: row.studentId,
      Score: row.error
        ? '—'
        : formatNumber(scoreForResultsTab(row, resultsTab, labChallenges)),
      ...(row.error
        ? pillarExportFields(null)
        : pillarExportFields(pillarsForResultsTab(row, resultsTab, labChallenges))),
      Plagiarism: row.error ? '—' : (row.plagiarismLabel || '—'),
      Error: row.error || '',
    }));
    const labSlug = String(selectedLab?.name || selectedLab?.id || 'lab').replace(/\s+/g, '_');
    const tabSlug = bulkResultsTabLabel.replace(/\s+/g, '_');
    await exportDataset(format, {
      rows: exportRows,
      title: `Bulk Grading — ${formatText(selectedLab?.name)} — ${bulkResultsTabLabel}`,
      fileBase: `bulk_grading_${labSlug}_${tabSlug}`,
    });
  }, [rows, resultsTab, labChallenges, selectedLab, bulkResultsTabLabel]);

  const membershipReady = membershipStatus === 'ready';
  const dropBlocked = !membershipReady || grading || !modeOk;

  const applyEntries = useCallback((entries, label) => {
    if (membershipStatus !== 'ready') return;
    const result = parseBulkMainFolder(entries, mode, { enrolledIrns });
    setParseResult(result);
    setFolderLabel(label || 'dropped folder');
    setRows([]);
    setGradedCount(0);
    setGradeError(null);
    setGradingStoppedEarly(false);
    setDrawerStudent(null);
    setResultsTab('overview');
    setResultsPage(0);
  }, [mode, enrolledIrns, membershipStatus]);

  const handleInputFiles = (fileList) => {
    if (dropBlocked) return;
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
    if (dropBlocked) return;
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
    membershipReady
    && modeOk
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
    setResultsPage(0);

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
          throw new Error(await readFriendlyApiError(response, 'bulk-grade'));
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
          error: toFriendlyError(err, 'bulk-grade'),
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
    const finalRows = nextRows.map((row) => ({
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
    }));
    setRows(finalRows);
    setGrading(false);
    await persistFinishedBatch(finalRows, stoppedEarly);
  };

  return (
    <div className="space-y-6">
      <div className="space-y-5 rounded-2xl border border-border bg-surface p-5 shadow-stage-1">
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

          <div className="flex flex-col gap-1.5 text-sm">
            <span className="font-semibold tracking-tight text-foreground">Mode</span>
            <div className="inline-flex overflow-hidden rounded-xl bg-surface-secondary p-1">
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
                  className={`rounded-lg px-4 py-1.5 text-sm font-semibold transition-colors ${
                    mode === m
                      ? 'bg-primary text-white shadow-sm'
                      : 'text-foreground-secondary hover:text-foreground'
                  }`}
                >
                  {m === 'LAB' ? 'Lab' : 'Exam'}
                </button>
              ))}
            </div>
          </div>

          {selectedLab && (
            <p className="pb-2 text-xs font-medium text-foreground-muted">
              Challenges: {challengeCount}
            </p>
          )}
        </div>

        {modeBlockMessage && (
          <p className="rounded-r-xl border-l-4 border-warning bg-warning-bg px-3.5 py-2.5 text-sm text-warning-text">
            {modeBlockMessage}
          </p>
        )}

        {membershipStatus === 'no-quarter' && (
          <p className="rounded-r-xl border-l-4 border-warning bg-warning-bg px-3.5 py-2.5 text-sm text-warning-text">
            No current quarter is set. Set a current quarter before using Bulk Grading.
          </p>
        )}
        {membershipStatus === 'empty-roster' && (
          <p className="rounded-r-xl border-l-4 border-warning bg-warning-bg px-3.5 py-2.5 text-sm text-warning-text">
            {IS_DESKTOP_LECTURER
              ? 'Import a current-term enrollment roster before using Bulk Grading.'
              : 'No enrolled students in the current quarter. Enroll students before using Bulk Grading.'}
          </p>
        )}
        {membershipStatus === 'error' && membershipError && (
          <p className="rounded-r-xl border-l-4 border-error bg-error-bg px-3.5 py-2.5 text-sm text-error-text">
            {membershipError}
          </p>
        )}
        {membershipStatus === 'loading' && (
          <p className="text-sm text-foreground-muted">Loading current-quarter student IRNs…</p>
        )}
        {membershipStatus === 'ready' && enrolledIrns.size > 0 && !IS_DESKTOP_LECTURER && (
          <div className="flex flex-wrap items-center gap-2 text-sm">
            <span className="font-medium text-foreground-secondary">
              Offline roster ({enrolledIrns.size} IRNs)
            </span>
            <button
              type="button"
              onClick={() => handleDownloadEnrollmentIrns('csv')}
              className="rounded-lg border border-border px-3 py-1.5 font-semibold text-foreground transition-colors hover:bg-surface-secondary"
            >
              Download CSV
            </button>
            <button
              type="button"
              onClick={() => handleDownloadEnrollmentIrns('excel')}
              className="rounded-lg border border-border px-3 py-1.5 font-semibold text-foreground transition-colors hover:bg-surface-secondary"
            >
              Download Excel
            </button>
          </div>
        )}

        <div
          onDragOver={(e) => {
            e.preventDefault();
            if (!dropBlocked) setIsDragging(true);
          }}
          onDragLeave={() => setIsDragging(false)}
          onDrop={handleDrop}
          className={`rounded-xl border-2 border-dashed px-6 py-8 text-center transition-colors ${
            dropBlocked
              ? 'border-border bg-surface-secondary/40 opacity-60'
              : isDragging
                ? 'border-primary bg-primary-light'
                : 'border-border bg-surface-secondary/70'
          }`}
        >
          <div className="mx-auto mb-2 h-16 w-16">
            <LottieAnimation animationData={uploadCloudData} className="h-full w-full" />
          </div>
          <p className="text-sm font-semibold text-foreground">Drop Main folder here</p>
          <p className="mt-1 font-mono text-xs text-foreground-muted">
            {mode === 'EXAM'
              ? 'Main → IRN_Name → .java / .mmd'
              : 'Main → IRN_Name → challenge_n → .java / .mmd'}
          </p>
          <button
            type="button"
            disabled={dropBlocked}
            onClick={() => inputRef.current?.click()}
            className="mt-4 rounded-xl bg-primary px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-primary-hover disabled:opacity-50"
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
            disabled={dropBlocked}
            onChange={(e) => handleInputFiles(e.target.files || [])}
          />
        </div>
      </div>

      {parseResult && (
        <div className="space-y-3 rounded-2xl border border-border bg-surface p-5 shadow-stage-1">
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
                  setResultsPage(0);
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
          className="space-y-4 rounded-2xl border border-border bg-surface p-6 shadow-stage-1"
          aria-busy="true"
          aria-live="polite"
        >
          <div className="space-y-2">
            <div className="flex flex-wrap items-center justify-between gap-2 text-sm">
              <span className="font-semibold text-foreground">{gradingProgressLabel}</span>
              <span className="font-mono tabular-nums text-foreground-muted">{gradingPercent}%</span>
            </div>
            <Progress
              value={gradedCount}
              max={gradingTotal}
              aria-label="Bulk grading progress"
              barClassName={gradingBarClass}
            />
          </div>
          <GradingActivityVisual
            title="Grading submissions…"
            subtitle="Compiling student folders, verifying MMD diagrams, and running isolated testcases"
          />
        </div>
      )}

      {!grading && rows.length > 0 && (
        <div className="overflow-hidden rounded-2xl border border-border bg-surface shadow-stage-1">
          <div className="space-y-3 px-5 pt-5">
            {gradeError && <p className="text-sm text-error">{gradeError}</p>}

            {gradingStoppedEarly && (
              <p className="text-sm text-foreground-muted">
                Grading stopped — showing {rows.length} of {gradingTotal} students.
              </p>
            )}
            {IS_DESKTOP_LECTURER && retainedNotice && (
              <p className="text-sm text-foreground-muted">
                Showing the last finished batch retained on this machine.
              </p>
            )}

            <div className="flex flex-wrap items-center justify-between gap-3 border-b border-border pb-2">
              {labChallenges.length > 0 ? (
                <div className="flex min-w-0 flex-wrap gap-2 sm:gap-3">
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
              ) : (
                <span className="text-sm font-semibold text-foreground">Results</span>
              )}
              <div className="flex flex-wrap items-center gap-2">
                {IS_DESKTOP_LECTURER && (
                  <button
                    type="button"
                    onClick={clearRetainedBatch}
                    className="rounded-lg border border-border px-3 py-1.5 text-sm font-semibold text-foreground transition-colors hover:bg-surface-secondary"
                  >
                    Clear batch
                  </button>
                )}
                <ExportMenu formats={BULK_EXPORT_FORMATS} onExport={handleExportBulkResults} />
              </div>
            </div>
          </div>

          <div className="mt-2 overflow-x-auto">
            <table className="min-w-full text-sm">
              <thead>
                <tr className="border-b border-border text-left text-foreground-secondary">
                  <th className="px-4 py-3 font-semibold">Student</th>
                  <th className="px-4 py-3 font-semibold">ID</th>
                  <th className="px-4 py-3 font-semibold">Score</th>
                  {PILLAR_EXPORT_COLUMNS.map((column) => (
                    <th key={column.key} className="px-4 py-3 text-center font-semibold whitespace-nowrap">{column.label}</th>
                  ))}
                  <th className="px-4 py-3 font-semibold">Plagiarism</th>
                  <th className="px-4 py-3 font-semibold">Action</th>
                </tr>
              </thead>
              <tbody>
                {pagedRows.map((row) => {
                  const displayScore = scoreForResultsTab(row, resultsTab, labChallenges);
                  const pillarScores = pillarsForResultsTab(row, resultsTab, labChallenges);
                  const canOpenSubmission = !row.error && row.payload;
                  const openSubmission = () => {
                    if (canOpenSubmission) setDrawerStudent(row.payload);
                  };
                  return (
                    <tr
                      key={row.studentId}
                      onDoubleClick={openSubmission}
                      className={`border-b border-border last:border-0 ${
                        canOpenSubmission ? 'cursor-pointer hover:bg-surface-secondary' : ''
                      }`}
                    >
                      <td className="px-4 py-3 text-foreground">{formatText(row.studentName)}</td>
                      <td className="px-4 py-3 text-foreground">{row.studentId}</td>
                      <td className="px-4 py-3 font-semibold text-foreground">
                        {row.error ? '—' : formatNumber(displayScore)}
                      </td>
                      {PILLAR_EXPORT_COLUMNS.map((column) => (
                        <td key={column.key} className="px-4 py-3 text-center font-medium tabular-nums text-foreground">
                          {row.error ? '—' : formatPillarScore(pillarScores[column.key])}
                        </td>
                      ))}
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
                            onClick={openSubmission}
                            className={`inline-flex items-center gap-1.5 rounded-md px-3 py-1.5 text-xs font-medium transition-colors disabled:opacity-40 ${SUCCESS_ACTION_BUTTON}`}
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
          {showResultsPagination && (
            <div className="flex flex-col gap-3 border-t border-border px-5 py-3 sm:flex-row sm:items-center sm:justify-between">
              <p className="text-sm text-foreground-secondary">
                Page {resultsPagination.page + 1} of {Math.max(resultsPagination.totalPages, 1)}
              </p>
              <div className="flex gap-2">
                <button
                  type="button"
                  disabled={resultsPagination.page <= 0}
                  onClick={() => setResultsPage((p) => Math.max(0, p - 1))}
                  className="rounded-lg border border-border px-3 py-1.5 text-sm disabled:opacity-50"
                >
                  Previous
                </button>
                <button
                  type="button"
                  disabled={resultsPagination.page >= resultsPagination.totalPages - 1}
                  onClick={() => setResultsPage((p) => p + 1)}
                  className="rounded-lg border border-border px-3 py-1.5 text-sm disabled:opacity-50"
                >
                  Next
                </button>
              </div>
            </div>
          )}
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
