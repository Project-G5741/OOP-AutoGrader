import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { FlaskConical, Loader2, Play, Plus, Save, Trash2 } from 'lucide-react';
import { authHeaders } from '../../../utils/authHeaders';
import { apiFetch } from '../../../utils/apiFetch';
import { readFriendlyApiError, toFriendlyError } from '../../../utils/apiError';
import ReferenceJavaFiles from './ReferenceJavaFiles';
import CompositionTestcaseScript from './CompositionTestcaseScript';
import UnitTestcaseWorksheet from './UnitTestcaseWorksheet';
import { DryRunResultCard } from './DryRunResultCard';
import {
  buildMemberCatalog,
  emptyTestcase,
  FIELD_CLASS,
  hydrateTestcase,
  isComposition,
  lecturerTestcaseShortLabels,
  normalizeTestcaseForApi,
  switchTestcaseType,
  validateTestcaseForDryRun,
} from './testcaseAuthoring';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';

const TESTCASE_LIST_WIDTH_DEFAULT = 260;
const TESTCASE_LIST_WIDTH_MIN = 180;
const TESTCASE_LIST_WIDTH_MAX = 520;
const TESTCASE_EDITOR_MIN = 280;

function testcaseListRowClass(isSelected, isRunning, tcResult) {
  const base = 'group flex w-full items-center gap-2 rounded-lg px-3 py-2 text-left text-sm transition-colors';
  if (isRunning) {
    return isSelected
      ? `${base} bg-primary-light text-foreground`
      : `${base} text-foreground-secondary hover:bg-surface-secondary hover:text-foreground`;
  }
  if (tcResult?.result === 'PASS') {
    return `${base} bg-success-bg text-success-text ${
      isSelected ? 'ring-2 ring-inset ring-success/50' : 'hover:brightness-[0.98]'
    }`;
  }
  if (tcResult) {
    return `${base} bg-error-bg text-error-text ${
      isSelected ? 'ring-2 ring-inset ring-error/50' : 'hover:brightness-[0.98]'
    }`;
  }
  if (isSelected) {
    return `${base} bg-primary-light text-foreground`;
  }
  return `${base} text-foreground-secondary hover:bg-surface-secondary hover:text-foreground`;
}

function refStorageKey(labId, challengeId) {
  return `ref-java:${labId}:${challengeId}`;
}

export default function TestcasesPanel({
  labId,
  challenge,
  relationTypeOptions = [],
  declaringTypeOptions = [],
  structureDirty,
  suppressServerLoad = false,
  onToast,
}) {
  const [testcases, setTestcases] = useState([]);
  const [snapshot, setSnapshot] = useState('[]');
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [runningId, setRunningId] = useState(null);
  const [selectedId, setSelectedId] = useState(null);
  const [referenceSources, setReferenceSources] = useState([]);
  const [dryRunResults, setDryRunResults] = useState({});
  const [warnStructure, setWarnStructure] = useState(false);
  const [listWidth, setListWidth] = useState(TESTCASE_LIST_WIDTH_DEFAULT);
  const splitContainerRef = useRef(null);
  const listResizeRef = useRef(null);

  const isDirty = useMemo(
    () => JSON.stringify(testcases) !== snapshot,
    [testcases, snapshot],
  );

  const dryRunSummary = useMemo(() => {
    let pass = 0;
    let fail = 0;
    let notRun = 0;
    testcases.forEach((tc) => {
      const result = dryRunResults[tc.id];
      if (!result) notRun += 1;
      else if (result.result === 'PASS') pass += 1;
      else fail += 1;
    });
    return { pass, fail, notRun };
  }, [testcases, dryRunResults]);

  const runningAll = Boolean(runningId);

  const selectedTestcase = useMemo(
    () => testcases.find((tc) => tc.id === selectedId) ?? null,
    [testcases, selectedId],
  );

  const selectedDryRunResult = selectedId ? dryRunResults[selectedId] ?? null : null;

  const catalog = useMemo(
    () => buildMemberCatalog(challenge, relationTypeOptions, declaringTypeOptions),
    [challenge, relationTypeOptions, declaringTypeOptions],
  );

  const testcaseShortLabels = useMemo(
    () => lecturerTestcaseShortLabels(testcases),
    [testcases],
  );

  const dryRunPayloadSources = useMemo(
    () => referenceSources
      .filter((s) => s.source?.trim())
      .map(({ className, source }) => ({ className, source })),
    [referenceSources],
  );

  const loadTestcases = useCallback(async () => {
    if (!labId || !challenge?.id) return;
    setLoading(true);
    try {
      const res = await apiFetch(
        `${API_BASE}/api/lecturer/labs/${labId}/challenges/${challenge.id}/testcases`,
        { headers: authHeaders() },
      );
      if (!res.ok) throw new Error(await readFriendlyApiError(res, 'read'));
      const data = await res.json();
      const rows = (data.testcases || []).map(hydrateTestcase);
      setTestcases(rows);
      setSnapshot(JSON.stringify(rows));
      setSelectedId((prev) => {
        if (prev && rows.some((r) => r.id === prev)) return prev;
        return rows[0]?.id ?? null;
      });
    } catch (e) {
      onToast?.({ type: 'error', message: toFriendlyError(e, 'read') });
    } finally {
      setLoading(false);
    }
  }, [labId, challenge?.id, onToast]);

  useEffect(() => {
    if (suppressServerLoad) {
      setTestcases([]);
      setSnapshot(JSON.stringify([]));
      setSelectedId(null);
      setDryRunResults({});
      setLoading(false);
    } else {
      loadTestcases();
    }
    const stored = sessionStorage.getItem(refStorageKey(labId, challenge?.id));
    if (stored) {
      try {
        const parsed = JSON.parse(stored);
        setReferenceSources(Array.isArray(parsed) ? parsed : []);
      } catch {
        setReferenceSources([]);
      }
    } else {
      setReferenceSources([]);
    }
    setDryRunResults({});
  }, [labId, challenge?.id, loadTestcases, suppressServerLoad]);

  useEffect(() => {
    if (structureDirty) setWarnStructure(true);
  }, [structureDirty]);

  useEffect(() => {
    sessionStorage.setItem(
      refStorageKey(labId, challenge?.id),
      JSON.stringify(referenceSources),
    );
  }, [referenceSources, labId, challenge?.id]);

  const updateTestcase = (id, patch) => {
    setTestcases((prev) => prev.map((tc) => (tc.id === id ? { ...tc, ...patch } : tc)));
    setDryRunResults((prev) => {
      if (!prev[id]) return prev;
      const next = { ...prev };
      delete next[id];
      return next;
    });
  };

  const selectTestcase = (id) => {
    if (id === selectedId) return;
    setSelectedId(id);
  };

  const handleSave = async () => {
    if (!labId || !challenge?.id) return;
    if (structureDirty) setWarnStructure(true);
    setSaving(true);
    try {
      const payload = testcases.map(normalizeTestcaseForApi);
      const res = await apiFetch(
        `${API_BASE}/api/lecturer/labs/${labId}/challenges/${challenge.id}/testcases`,
        {
          method: 'PUT',
          headers: { ...authHeaders(), 'Content-Type': 'application/json' },
          body: JSON.stringify(payload),
        },
      );
      if (!res.ok) throw new Error(await readFriendlyApiError(res, 'testcase-save'));
      const data = await res.json();
      const rows = (data.testcases || []).map(hydrateTestcase);
      setTestcases(rows);
      setSnapshot(JSON.stringify(rows));
      onToast?.({ type: 'success', message: 'Testcases saved' });
    } catch (e) {
      onToast?.({ type: 'error', message: toFriendlyError(e, 'testcase-save') });
    } finally {
      setSaving(false);
    }
  };

  const runDryRunForTestcase = async (tc) => {
    const res = await apiFetch(
      `${API_BASE}/api/lecturer/labs/${labId}/challenges/${challenge.id}/testcases/dry-run`,
      {
        method: 'POST',
        headers: { ...authHeaders(), 'Content-Type': 'application/json' },
        body: JSON.stringify({
          referenceSources: dryRunPayloadSources,
          testcase: normalizeTestcaseForApi(tc),
        }),
      },
    );
    if (!res.ok) throw new Error(await readFriendlyApiError(res, 'testcase-dry-run'));
    return res.json();
  };

  const handleDryRun = async (tc) => {
    if (!labId || !challenge?.id) return;
    if (dryRunPayloadSources.length === 0) {
      onToast?.({ type: 'error', message: 'Add at least one reference Java file before running.' });
      return;
    }
    const configError = validateTestcaseForDryRun(tc, catalog);
    if (configError) {
      onToast?.({ type: 'error', message: configError });
      return;
    }
    if (structureDirty) setWarnStructure(true);
    setRunningId(tc.id);
    try {
      const data = await runDryRunForTestcase(tc);
      setDryRunResults((prev) => ({ ...prev, [tc.id]: data }));
    } catch (e) {
      onToast?.({ type: 'error', message: toFriendlyError(e, 'testcase-dry-run') });
    } finally {
      setRunningId(null);
    }
  };

  const handleRunAll = async () => {
    if (!labId || !challenge?.id || testcases.length === 0) return;
    if (dryRunPayloadSources.length === 0) {
      onToast?.({ type: 'error', message: 'Add at least one reference Java file before running.' });
      return;
    }
    if (structureDirty) setWarnStructure(true);
    setRunningId('__batch__');
    const nextResults = { ...dryRunResults };
    let failedCount = 0;
    try {
      for (const tc of testcases) {
        setRunningId(tc.id);
        const configError = validateTestcaseForDryRun(tc, catalog);
        if (configError) {
          failedCount += 1;
          onToast?.({ type: 'error', message: configError });
          continue;
        }
        try {
          nextResults[tc.id] = await runDryRunForTestcase(tc);
        } catch (e) {
          failedCount += 1;
          onToast?.({ type: 'error', message: toFriendlyError(e, 'testcase-dry-run') });
        }
      }
      setDryRunResults(nextResults);
      if (failedCount === 0) {
        onToast?.({ type: 'success', message: `Ran ${testcases.length} testcase${testcases.length === 1 ? '' : 's'}` });
      }
    } finally {
      setRunningId(null);
    }
  };

  const handleDelete = (id) => {
    const idx = testcases.findIndex((tc) => tc.id === id);
    const next = testcases.filter((tc) => tc.id !== id);
    setTestcases(next);
    if (selectedId === id) {
      const fallback = next[Math.min(idx, next.length - 1)];
      setSelectedId(fallback?.id ?? null);
    }
    setDryRunResults((prev) => {
      if (!prev[id]) return prev;
      const nextResults = { ...prev };
      delete nextResults[id];
      return nextResults;
    });
  };

  const handleAddTestcase = (type = 'UNIT') => {
    const tc = emptyTestcase(testcases.length, type);
    setTestcases([...testcases, tc]);
    setSelectedId(tc.id);
  };

  const handleListResizePointerDown = (event) => {
    event.preventDefault();
    const containerWidth = splitContainerRef.current?.offsetWidth ?? 0;
    listResizeRef.current = {
      startX: event.clientX,
      startWidth: listWidth,
      containerWidth,
    };
    const onPointerMove = (moveEvent) => {
      const drag = listResizeRef.current;
      if (!drag) return;
      const delta = moveEvent.clientX - drag.startX;
      const maxWidth = drag.containerWidth > 0
        ? Math.min(TESTCASE_LIST_WIDTH_MAX, drag.containerWidth - TESTCASE_EDITOR_MIN)
        : TESTCASE_LIST_WIDTH_MAX;
      const next = Math.min(maxWidth, Math.max(TESTCASE_LIST_WIDTH_MIN, drag.startWidth + delta));
      setListWidth(next);
    };
    const onPointerUp = () => {
      listResizeRef.current = null;
      document.body.style.cursor = '';
      document.body.style.userSelect = '';
      window.removeEventListener('pointermove', onPointerMove);
      window.removeEventListener('pointerup', onPointerUp);
    };
    document.body.style.cursor = 'col-resize';
    document.body.style.userSelect = 'none';
    window.addEventListener('pointermove', onPointerMove);
    window.addEventListener('pointerup', onPointerUp);
  };

  if (!challenge) {
    return (
      <div className="flex h-full min-h-[24rem] items-center justify-center rounded-xl border border-dashed border-border text-foreground-secondary">
        Select a problem from the structure sidebar.
      </div>
    );
  }

  return (
    <div className="min-w-0 space-y-4 pb-4">
      {warnStructure && (
        <div className="rounded-lg border border-warning/40 bg-warning-bg px-3 py-2 text-sm text-warning-text">
          Lab structure has unsaved changes. Save structure first so new methods and fields can be referenced.
        </div>
      )}

      <div className="rounded-xl border border-border bg-surface p-4 dark:border-border">
        <div className="mb-3 text-sm font-medium text-foreground-secondary">
          Reference Java (dry-run)
        </div>
        <ReferenceJavaFiles
          sources={referenceSources}
          onChange={setReferenceSources}
          onError={(message) => onToast?.({ type: 'error', message })}
        />
      </div>

      <div className="min-w-0 rounded-xl bg-surface">
        <div className="flex flex-wrap items-center justify-between gap-2 px-4 py-3">
          <div className="flex items-center gap-2">
            <FlaskConical className="h-4 w-4 text-chart-green" />
            <span className="font-medium text-foreground">Operational Testcases</span>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            {testcases.length > 0 && (
              <button
                type="button"
                onClick={handleRunAll}
                disabled={!!runningId}
                className="inline-flex items-center gap-1 rounded-lg bg-surface-secondary px-2.5 py-1.5 text-xs font-medium text-foreground-secondary transition-colors hover:bg-surface-tertiary hover:text-foreground disabled:opacity-50"
              >
                {runningAll ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : <Play className="h-3.5 w-3.5" />}
                Run all
              </button>
            )}
            <button
              type="button"
              onClick={() => handleAddTestcase()}
              className="inline-flex items-center gap-1 text-sm text-primary-text transition-colors hover:text-foreground"
            >
              <Plus className="h-4 w-4" /> Add new testcase
            </button>
          </div>
        </div>

        {loading ? (
          <div className="flex justify-center py-12">
            <Loader2 className="h-6 w-6 animate-spin text-primary" />
          </div>
        ) : testcases.length === 0 ? (
          <p className="px-4 py-8 text-center text-sm text-foreground-secondary">No testcases yet.</p>
        ) : (
          <div
            ref={splitContainerRef}
            className="flex min-h-[22rem] min-w-0 flex-col lg:flex-row"
          >
            <aside
              className="w-full shrink-0 border-b border-border-subtle p-2 lg:border-b-0 lg:[width:var(--tc-list-w)]"
              style={{ '--tc-list-w': `${listWidth}px` }}
            >
              {(dryRunSummary.pass > 0 || dryRunSummary.fail > 0 || dryRunSummary.notRun > 0) && (
                <div className="mb-3 flex flex-wrap gap-2 px-2">
                  {dryRunSummary.pass > 0 && (
                    <span className="inline-flex min-w-[4.5rem] flex-col items-center rounded-lg border border-success/30 bg-success-bg px-2.5 py-1.5 text-center">
                      <span className="text-lg font-bold tabular-nums leading-none text-success-text">
                        {dryRunSummary.pass}
                      </span>
                      <span className="mt-0.5 text-[10px] font-semibold uppercase tracking-wide text-success-text">
                        Passed
                      </span>
                    </span>
                  )}
                  {dryRunSummary.fail > 0 && (
                    <span className="inline-flex min-w-[4.5rem] flex-col items-center rounded-lg border border-error/30 bg-error-bg px-2.5 py-1.5 text-center">
                      <span className="text-lg font-bold tabular-nums leading-none text-error-text">
                        {dryRunSummary.fail}
                      </span>
                      <span className="mt-0.5 text-[10px] font-semibold uppercase tracking-wide text-error-text">
                        Failed
                      </span>
                    </span>
                  )}
                  {dryRunSummary.notRun > 0 && (
                    <span className="inline-flex min-w-[4.5rem] flex-col items-center rounded-lg border border-border bg-surface-secondary px-2.5 py-1.5 text-center">
                      <span className="text-lg font-bold tabular-nums leading-none text-foreground-secondary">
                        {dryRunSummary.notRun}
                      </span>
                      <span className="mt-0.5 text-[10px] font-semibold uppercase tracking-wide text-foreground-muted">
                        Not run
                      </span>
                    </span>
                  )}
                </div>
              )}
              <ul className="scrollbar-themed max-h-[min(70vh,42rem)] space-y-1 overflow-y-auto">
                {testcases.map((tc) => {
                  const isSelected = selectedId === tc.id;
                  const tcResult = dryRunResults[tc.id];
                  const isRunning = runningId === tc.id;
                  const onStatusRow = !isRunning && Boolean(tcResult);
                  const rowAriaLabel = tcResult
                    ? `${tc.name} — ${tcResult.result === 'PASS' ? 'passed' : 'failed'}`
                    : tc.name;
                  return (
                    <li key={tc.id}>
                      <button
                        type="button"
                        onClick={() => selectTestcase(tc.id)}
                        className={testcaseListRowClass(isSelected, isRunning, tcResult)}
                        aria-label={rowAriaLabel}
                      >
                        <span
                          className={`shrink-0 rounded px-1.5 py-0.5 font-mono text-[10px] font-bold tabular-nums tracking-tight ${
                            onStatusRow
                              ? 'bg-white/50 text-inherit dark:bg-black/20'
                              : 'bg-surface-tertiary text-foreground-secondary'
                          }`}
                          title={tc.hidden ? 'Other testcase (hidden from students)' : 'Example testcase'}
                        >
                          {testcaseShortLabels.get(tc.id)}
                        </span>
                        <span className="min-w-0 flex-1 truncate font-medium">{tc.name}</span>
                        {isRunning && (
                          <Loader2 className="h-3.5 w-3.5 shrink-0 animate-spin text-primary" aria-label="Running" />
                        )}
                        <span
                          className={`shrink-0 rounded px-1.5 text-[10px] uppercase tracking-wide ${
                            onStatusRow
                              ? 'bg-white/50 text-inherit dark:bg-black/20'
                              : 'bg-surface-secondary text-foreground-muted'
                          }`}
                        >
                          {isComposition(tc) ? 'Composition' : 'Unit'}
                        </span>
                        {tc.hidden && (
                          <span
                            className={`shrink-0 rounded px-1.5 text-[10px] uppercase tracking-wide ${
                              onStatusRow
                                ? 'bg-white/60 text-inherit dark:bg-black/25'
                                : 'bg-foreground-muted/80 text-foreground'
                            }`}
                          >
                            hidden
                          </span>
                        )}
                      </button>
                    </li>
                  );
                })}
              </ul>
            </aside>

            <div
              role="separator"
              aria-orientation="vertical"
              aria-label="Resize testcase list"
              onPointerDown={handleListResizePointerDown}
              className="relative hidden w-0 shrink-0 lg:block"
            >
              <div
                className="absolute inset-y-0 -left-1.5 z-10 w-3 cursor-col-resize touch-none"
                title="Drag to resize"
              />
              <div className="absolute inset-y-0 -left-px w-px bg-border-subtle" />
            </div>

            <div className="flex min-h-0 min-w-0 max-h-[min(70vh,42rem)] flex-1 flex-col p-4">
              {selectedTestcase ? (
                <div key={selectedTestcase.id} className="flex min-h-0 min-w-0 flex-1 flex-col animate-panel-in">
                  <div className="min-w-0 shrink-0 space-y-3 border-b border-border pb-3 dark:border-border">
                    <div className="flex items-start justify-between gap-3">
                      <div className="min-w-0 flex-1 space-y-3">
                        <label className="block text-[10px] font-semibold uppercase tracking-wide text-foreground-secondary">
                          <span className="flex items-center gap-2">
                            Name
                            <span
                              className="rounded bg-surface-tertiary px-1.5 py-0.5 font-mono text-[10px] font-bold normal-case tracking-tight text-foreground"
                              title={selectedTestcase.hidden ? 'Other testcase (hidden from students)' : 'Example testcase'}
                            >
                              {testcaseShortLabels.get(selectedTestcase.id)}
                            </span>
                          </span>
                          <input
                            type="text"
                            value={selectedTestcase.name ?? ''}
                            onChange={(e) => updateTestcase(selectedTestcase.id, { name: e.target.value })}
                            className="mt-1 w-full rounded-lg border border-border bg-surface-secondary px-3 py-2 text-sm font-medium text-foreground outline-none ring-primary/0 transition-shadow focus:border-primary/50 focus:ring-2 focus:ring-primary/20"
                            placeholder="Testcase name"
                          />
                        </label>
                        <label className="block text-xs text-foreground-muted">
                          Type
                          <select
                            className={FIELD_CLASS}
                            value={selectedTestcase.testcaseType}
                            onChange={(e) => updateTestcase(
                              selectedTestcase.id,
                              switchTestcaseType(selectedTestcase, e.target.value),
                            )}
                          >
                            <option value="UNIT">Unit</option>
                            <option value="COMPOSITION">Composition</option>
                          </select>
                        </label>
                      </div>
                      <div className="flex shrink-0 items-center gap-1 pt-4">
                        <button
                          type="button"
                          disabled={runningId === selectedTestcase.id}
                          onClick={() => handleDryRun(selectedTestcase)}
                          className="inline-flex items-center gap-1.5 rounded-lg bg-success px-3 py-2 text-xs font-semibold text-white transition-colors hover:bg-success-hover disabled:opacity-50"
                          title="Run dry-run"
                        >
                          {runningId === selectedTestcase.id
                            ? <Loader2 className="h-3.5 w-3.5 animate-spin" />
                            : <Play className="h-3.5 w-3.5" />}
                          Run
                        </button>
                        <button
                          type="button"
                          onClick={() => handleDelete(selectedTestcase.id)}
                          className="rounded-lg p-2 text-foreground-muted transition-colors hover:bg-error-bg hover:text-error"
                          title="Delete testcase"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </div>
                    </div>

                    {selectedDryRunResult && (
                      <DryRunResultCard result={selectedDryRunResult} />
                    )}
                  </div>

                  <div className="scrollbar-themed min-h-0 flex-1 overflow-y-auto pt-3">
                    {isComposition(selectedTestcase) ? (
                      <CompositionTestcaseScript
                        tc={selectedTestcase}
                        catalog={catalog}
                        onUpdate={(patch) => updateTestcase(selectedTestcase.id, patch)}
                      />
                    ) : (
                      <UnitTestcaseWorksheet
                        tc={selectedTestcase}
                        catalog={catalog}
                        onUpdate={(patch) => updateTestcase(selectedTestcase.id, patch)}
                      />
                    )}
                    <label className="mt-3 flex items-center gap-2 text-sm text-foreground-muted">
                      <input
                        type="checkbox"
                        checked={!!selectedTestcase.hidden}
                        onChange={(e) => updateTestcase(selectedTestcase.id, { hidden: e.target.checked })}
                      />
                      Hidden from students (pass/fail only)
                    </label>
                  </div>
                </div>
              ) : (
                <div className="flex h-full min-h-[16rem] items-center justify-center text-sm text-foreground-secondary">
                  Select a testcase to edit
                </div>
              )}
            </div>
          </div>
        )}
      </div>

      <div className="flex justify-end">
        <button
          type="button"
          disabled={!isDirty || saving}
          onClick={handleSave}
          className="inline-flex items-center gap-2 rounded-full bg-success px-5 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-success-hover disabled:opacity-50"
        >
          {saving ? <Loader2 className="h-4 w-4 animate-spin" /> : <Save className="h-4 w-4" />}
          Save Testcases
        </button>
      </div>
    </div>
  );
}

export { normalizeTestcaseForApi, emptyTestcase };
