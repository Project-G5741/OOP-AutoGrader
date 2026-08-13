import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { CheckCircle2, Circle, ChevronDown, ChevronUp, FlaskConical, Loader2, Play, Plus, Save, Trash2, XCircle } from 'lucide-react';
import { authHeaders } from '../../../utils/authHeaders';
import { readApiErrorMessage } from '../../../utils/apiError';
import ReferenceJavaFiles from './ReferenceJavaFiles';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';

const ASSERTION_KINDS = [
  'RETURN_VALUE',
  'FIELD_STATE',
  'STDOUT',
  'EXCEPTION',
  'COMPARISON_RESULT',
];

const COMPARISON_MODES = ['EXACT', 'TRIMMED', 'NORMALIZED_WHITESPACE'];

const COMPARISON_RESULT_EQUALS_OPTIONS = ['true', 'false'];
const COMPARISON_RESULT_COMPARE_TO_OPTIONS = ['-1', '0', '1'];

function comparisonResultSelectValue(expectedValue, comparisonMethod) {
  if (comparisonMethod === 'COMPARE_TO') {
    const value = String(expectedValue ?? '0');
    return COMPARISON_RESULT_COMPARE_TO_OPTIONS.includes(value) ? value : '0';
  }
  const value = String(expectedValue ?? 'true').toLowerCase();
  if (COMPARISON_RESULT_EQUALS_OPTIONS.includes(value)) return value;
  if (value === '0') return 'true';
  if (value === '1') return 'false';
  return 'true';
}

function emptyTestcase(orderIndex = 0) {
  return {
    id: crypto.randomUUID(),
    name: 'New testcase',
    testcaseType: 'SINGLE_INVOCATION',
    comparisonMethod: null,
    weight: 1,
    orderIndex,
    hidden: false,
    invocation: {
      id: crypto.randomUUID(),
      invocationKind: 'CONSTRUCTOR',
      constructorId: null,
      methodId: null,
      params: '[]',
      receiverConstructorId: null,
      receiverParams: '[]',
    },
    instances: [],
    assertions: [{
      id: crypto.randomUUID(),
      invocationId: null,
      assertionKind: 'FIELD_STATE',
      fieldId: null,
      expectedValue: '0',
      comparisonMode: 'EXACT',
      orderIndex: 0,
    }],
  };
}

function refStorageKey(labId, challengeId) {
  return `ref-java:${labId}:${challengeId}`;
}

function normalizeTestcaseForApi(tc) {
  return {
    ...tc,
    assertions: (tc.assertions || []).map((a, idx) => ({
      ...a,
      assertionKind: a.assertionKind,
      fieldId: a.assertionKind === 'FIELD_STATE' ? (a.fieldId || null) : null,
      expectedValue: a.assertionKind === 'COMPARISON_RESULT' && tc.testcaseType === 'COMPARISON'
        ? comparisonResultSelectValue(a.expectedValue, tc.comparisonMethod)
        : (a.expectedValue?.trim() ? a.expectedValue.trim() : 'null'),
      comparisonMode: a.comparisonMode || 'EXACT',
      orderIndex: a.orderIndex ?? idx,
    })),
  };
}

function comparisonTestcaseDefaults() {
  return {
    comparisonMethod: 'EQUALS',
    invocation: null,
    instances: [
      { id: crypto.randomUUID(), label: 'A', constructorId: null, params: '[]' },
      { id: crypto.randomUUID(), label: 'B', constructorId: null, params: '[]' },
    ],
    assertions: [{
      id: crypto.randomUUID(),
      invocationId: null,
      assertionKind: 'COMPARISON_RESULT',
      fieldId: null,
      expectedValue: 'true',
      comparisonMode: 'EXACT',
      orderIndex: 0,
    }],
  };
}

function invocationForKindChange(invocation, kind) {
  const next = { ...invocation, invocationKind: kind };
  if (kind === 'CONSTRUCTOR') {
    next.methodId = null;
    next.receiverConstructorId = null;
    next.receiverParams = '[]';
  } else {
    next.constructorId = null;
  }
  return next;
}

function dryRunSummaryText(result) {
  if (!result) return '';
  if (result.feedback) return result.feedback;
  const assertions = result.assertions ?? [];
  const failed = assertions.filter((a) => a.result !== 'PASS').length;
  if (failed > 0) return `${failed} assertion${failed === 1 ? '' : 's'} failed`;
  if (assertions.length > 0) return 'All assertions passed';
  return result.result === 'PASS' ? 'Passed' : 'Failed';
}

function DryRunStatusIcon({ result, running }) {
  if (running) {
    return <Loader2 className="h-3.5 w-3.5 shrink-0 animate-spin text-primary" aria-hidden />;
  }
  if (!result) {
    return <Circle className="h-3.5 w-3.5 shrink-0 text-foreground-disabled" aria-hidden />;
  }
  if (result.result === 'PASS') {
    return <CheckCircle2 className="h-3.5 w-3.5 shrink-0 text-success" aria-hidden />;
  }
  return <XCircle className="h-3.5 w-3.5 shrink-0 text-error" aria-hidden />;
}

function DryRunResultCard({ result }) {
  if (!result) return null;
  const passed = result.result === 'PASS';
  const [expanded, setExpanded] = useState(!passed);
  const assertions = result.assertions ?? [];
  const hasAssertionRows = assertions.length > 0
    && assertions.some((a) => a.expected_output ?? a.expectedOutput);

  const headerBarClass = passed
    ? 'bg-[var(--success-panel)] text-[var(--success-panel-text)]'
    : 'bg-[var(--error-bg)] text-[var(--error-text)]';
  const bodyCardClass = 'overflow-hidden rounded-lg border border-border-subtle bg-surface-secondary';
  const sectionDividerClass = 'border-t border-border-subtle';
  const sectionLabelClass = 'mb-1 text-[10px] font-semibold uppercase tracking-wide text-foreground-muted';
  const codeBlockClass = 'whitespace-pre-wrap rounded-md bg-surface px-2 py-1.5 font-mono text-xs text-foreground-secondary';

  const summary = dryRunSummaryText(result);

  if (!expanded) {
    return (
      <div className={`animate-panel-in overflow-hidden rounded-lg ${headerBarClass}`}>
        <div className="flex items-center justify-between gap-2 px-3 py-2 text-sm">
          <div className="flex min-w-0 items-center gap-2">
            <span className="shrink-0 text-xs font-semibold">{result.result}</span>
            <span className="truncate text-xs">{summary}</span>
          </div>
          <button
            type="button"
            onClick={() => setExpanded(true)}
            className="inline-flex shrink-0 items-center gap-1 text-xs opacity-80 hover:opacity-100"
          >
            Details <ChevronDown className="h-3.5 w-3.5" />
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className={`animate-panel-in text-sm ${bodyCardClass}`}>
      <div className={`flex items-center justify-between gap-3 px-3 py-2 ${headerBarClass}`}>
        <div className="flex min-w-0 items-center gap-2">
          <span className="shrink-0 text-xs font-semibold">{result.result}</span>
          <span className="truncate text-xs">{summary}</span>
        </div>
        <button
          type="button"
          onClick={() => setExpanded(false)}
          className="inline-flex shrink-0 items-center gap-1 text-xs opacity-80 hover:opacity-100"
        >
          Hide <ChevronUp className="h-3.5 w-3.5" />
        </button>
      </div>

      <div className="px-3 py-3">
      {result.input != null && (
        <div className="pb-3">
          <div className={sectionLabelClass}>Input</div>
          <pre className={codeBlockClass}>{result.input}</pre>
        </div>
      )}

      {hasAssertionRows ? (
        <div className={`scrollbar-themed max-h-36 space-y-0 overflow-y-auto pr-1 ${result.input != null ? sectionDividerClass : ''}`}>
          {assertions.map((assertion, index) => {
            const aPassed = assertion.result === 'PASS';
            const expected = assertion.expected_output ?? assertion.expectedOutput ?? '—';
            const actual = assertion.actual_output ?? assertion.actualOutput ?? '—';
            const fieldLabel = assertion.kind === 'FIELD_STATE' && expected.includes('=')
              ? expected.split('=')[0].trim()
              : null;

            return (
              <div
                key={`${assertion.kind}-${assertion.order_index ?? assertion.orderIndex ?? index}`}
                className={`py-3 ${index > 0 ? sectionDividerClass : ''}`}
              >
                <div className="mb-1.5 flex items-center justify-between gap-2">
                  <span className="text-[10px] font-semibold uppercase tracking-wide text-foreground-secondary">
                    {fieldLabel ?? assertion.kind}
                  </span>
                  <span className={`rounded-full px-2 py-0.5 text-[10px] font-semibold ${
                    aPassed ? 'bg-success-bg text-success-text' : 'bg-error-bg text-error-text'
                  }`}
                  >
                    {assertion.result}
                  </span>
                </div>
                <div className="grid gap-2 sm:grid-cols-2">
                  <div>
                    <div className="mb-0.5 text-[10px] font-semibold uppercase tracking-wide text-foreground-muted">Expected</div>
                    <pre className={codeBlockClass}>{expected}</pre>
                  </div>
                  <div>
                    <div className="mb-0.5 text-[10px] font-semibold uppercase tracking-wide text-foreground-muted">Actual</div>
                    <pre className={`whitespace-pre-wrap rounded-md px-2 py-1.5 font-mono text-xs ${
                      aPassed
                        ? 'bg-surface text-foreground-secondary'
                        : 'bg-error-bg text-error-text'
                    }`}
                    >
                      {actual}
                    </pre>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      ) : (
        <div className={`grid gap-3 sm:grid-cols-2 ${result.input != null ? `${sectionDividerClass} pt-3` : ''}`}>
          {result.expected_output != null && (
            <div>
              <div className={sectionLabelClass}>Expected</div>
              <pre className={codeBlockClass}>{result.expected_output}</pre>
            </div>
          )}
          {result.actual_output != null && (
            <div>
              <div className={sectionLabelClass}>Actual</div>
              <pre className={codeBlockClass}>{result.actual_output}</pre>
            </div>
          )}
        </div>
      )}
      </div>
    </div>
  );
}

function TestcaseEditor({
  tc,
  memberOptions,
  onUpdate,
}) {
  return (
    <div className="space-y-3">
      <div className="grid gap-3 sm:grid-cols-2">
        <label className="block text-xs text-foreground-muted sm:col-span-2">
          Type
          <select
            className="mt-1 w-full rounded border border-border bg-surface-secondary px-2 py-1.5 text-sm dark:text-white"
            value={tc.testcaseType}
            onChange={(e) => {
              const type = e.target.value;
              if (type === 'SINGLE_INVOCATION') {
                onUpdate({
                  testcaseType: type,
                  comparisonMethod: null,
                  invocation: tc.invocation || emptyTestcase().invocation,
                  instances: [],
                  assertions: (tc.assertions?.length && tc.testcaseType === 'SINGLE_INVOCATION')
                    ? tc.assertions
                    : emptyTestcase().assertions,
                });
              } else {
                onUpdate({
                  testcaseType: type,
                  ...comparisonTestcaseDefaults(),
                });
              }
            }}
          >
            <option value="SINGLE_INVOCATION">SINGLE_INVOCATION</option>
            <option value="COMPARISON">COMPARISON</option>
          </select>
        </label>
      </div>

      {tc.testcaseType === 'SINGLE_INVOCATION' && tc.invocation && (
        <div className="grid gap-3 sm:grid-cols-2">
          <label className="block text-xs text-foreground-muted">
            Invocation kind
            <select
              className="mt-1 w-full rounded border border-border bg-surface-secondary px-2 py-1.5 text-sm dark:text-white"
              value={tc.invocation.invocationKind}
              onChange={(e) => onUpdate({
                invocation: invocationForKindChange(tc.invocation, e.target.value),
              })}
            >
              <option value="CONSTRUCTOR">CONSTRUCTOR</option>
              <option value="METHOD">METHOD</option>
            </select>
          </label>
          {tc.invocation.invocationKind === 'CONSTRUCTOR' ? (
            <label className="block text-xs text-foreground-muted">
              Constructor
              <select
                className="mt-1 w-full rounded border border-border bg-surface-secondary px-2 py-1.5 text-sm dark:text-white"
                value={tc.invocation.constructorId || ''}
                onChange={(e) => onUpdate({
                  invocation: { ...tc.invocation, constructorId: e.target.value || null },
                })}
              >
                <option value="">Select constructor</option>
                {memberOptions.constructors.map((opt) => (
                  <option key={opt.id} value={opt.id}>{opt.label}</option>
                ))}
              </select>
            </label>
          ) : (
            <>
              <label className="block text-xs text-foreground-muted">
                Method
                <select
                  className="mt-1 w-full rounded border border-border bg-surface-secondary px-2 py-1.5 text-sm dark:text-white"
                  value={tc.invocation.methodId || ''}
                  onChange={(e) => onUpdate({
                    invocation: { ...tc.invocation, methodId: e.target.value || null },
                  })}
                >
                  <option value="">Select method</option>
                  {memberOptions.methods.map((opt) => (
                    <option key={opt.id} value={opt.id}>{opt.label}</option>
                  ))}
                </select>
              </label>
              <label className="block text-xs text-foreground-muted">
                Receiver constructor (optional)
                <select
                  className="mt-1 w-full rounded border border-border bg-surface-secondary px-2 py-1.5 text-sm dark:text-white"
                  value={tc.invocation.receiverConstructorId || ''}
                  onChange={(e) => onUpdate({
                    invocation: { ...tc.invocation, receiverConstructorId: e.target.value || null },
                  })}
                >
                  <option value="">No-arg ctor on class</option>
                  {memberOptions.constructors.map((opt) => (
                    <option key={opt.id} value={opt.id}>{opt.label}</option>
                  ))}
                </select>
              </label>
              <label className="block text-xs text-foreground-muted sm:col-span-2">
                Receiver params (JSON array)
                <input
                  className="mt-1 w-full rounded border border-border bg-surface-secondary px-2 py-1.5 font-mono text-sm dark:text-white"
                  value={tc.invocation.receiverParams || '[]'}
                  onChange={(e) => onUpdate({
                    invocation: { ...tc.invocation, receiverParams: e.target.value },
                  })}
                />
              </label>
            </>
          )}
          <label className="block text-xs text-foreground-muted sm:col-span-2">
            Params (JSON array)
            <input
              className="mt-1 w-full rounded border border-border bg-surface-secondary px-2 py-1.5 font-mono text-sm dark:text-white"
              value={tc.invocation.params || '[]'}
              onChange={(e) => onUpdate({
                invocation: { ...tc.invocation, params: e.target.value },
              })}
            />
          </label>
        </div>
      )}

      {tc.testcaseType === 'COMPARISON' && (
        <div className="space-y-2">
          <select
            className="w-full rounded border border-border bg-surface-secondary px-2 py-1.5 text-sm dark:text-white"
            value={tc.comparisonMethod || 'EQUALS'}
            onChange={(e) => {
              const method = e.target.value;
              const assertions = (tc.assertions || []).map((a) => {
                if (a.assertionKind !== 'COMPARISON_RESULT') return a;
                return {
                  ...a,
                  expectedValue: method === 'COMPARE_TO' ? '0' : 'true',
                };
              });
              onUpdate({ comparisonMethod: method, assertions });
            }}
          >
            <option value="EQUALS">EQUALS</option>
            <option value="COMPARE_TO">COMPARE_TO</option>
          </select>
          {(tc.instances || []).map((inst, idx) => (
            <div key={inst.id || idx} className="grid gap-2 sm:grid-cols-2">
              <span className="text-xs text-foreground-muted">Instance {inst.label}</span>
              <select
                className="rounded border border-border bg-surface-secondary px-2 py-1.5 text-sm dark:text-white"
                value={inst.constructorId || ''}
                onChange={(e) => {
                  const instances = [...(tc.instances || [])];
                  instances[idx] = { ...inst, constructorId: e.target.value || null };
                  onUpdate({ instances });
                }}
              >
                <option value="">Constructor</option>
                {memberOptions.constructors.map((opt) => (
                  <option key={opt.id} value={opt.id}>{opt.label}</option>
                ))}
              </select>
              <input
                className="sm:col-span-2 rounded border border-border bg-surface-secondary px-2 py-1.5 font-mono text-sm dark:text-white"
                value={inst.params || '[]'}
                onChange={(e) => {
                  const instances = [...(tc.instances || [])];
                  instances[idx] = { ...inst, params: e.target.value };
                  onUpdate({ instances });
                }}
                placeholder="Params JSON"
              />
            </div>
          ))}
        </div>
      )}

      <div className="space-y-2">
        <div className="text-xs font-semibold text-foreground-secondary">Assertions</div>
        {(tc.assertions || []).map((a, idx) => (
          <div key={a.id || idx} className="grid gap-2 rounded border border-border p-2 sm:grid-cols-3">
            <select
              className="rounded border border-border bg-surface-secondary px-2 py-1 text-sm dark:text-white"
              value={a.assertionKind}
              onChange={(e) => {
                const assertions = [...(tc.assertions || [])];
                const nextKind = e.target.value;
                assertions[idx] = {
                  ...a,
                  assertionKind: nextKind,
                  fieldId: nextKind === 'FIELD_STATE' ? (a.fieldId || null) : null,
                  expectedValue: nextKind === 'COMPARISON_RESULT' && tc.testcaseType === 'COMPARISON'
                    ? (tc.comparisonMethod === 'COMPARE_TO' ? '0' : 'true')
                    : a.expectedValue,
                };
                onUpdate({ assertions });
              }}
            >
              {ASSERTION_KINDS.map((k) => <option key={k} value={k}>{k}</option>)}
            </select>
            {a.assertionKind === 'FIELD_STATE' && (
              <select
                className="rounded border border-border bg-surface-secondary px-2 py-1 text-sm dark:text-white"
                value={a.fieldId || ''}
                onChange={(e) => {
                  const assertions = [...(tc.assertions || [])];
                  assertions[idx] = { ...a, fieldId: e.target.value || null };
                  onUpdate({ assertions });
                }}
              >
                <option value="">Field</option>
                {memberOptions.fields.map((opt) => (
                  <option key={opt.id} value={opt.id}>{opt.label}</option>
                ))}
              </select>
            )}
            {tc.testcaseType === 'COMPARISON' && a.assertionKind === 'COMPARISON_RESULT' ? (
              <select
                className="rounded border border-border bg-surface-secondary px-2 py-1 text-sm dark:text-white"
                value={comparisonResultSelectValue(a.expectedValue, tc.comparisonMethod)}
                onChange={(e) => {
                  const assertions = [...(tc.assertions || [])];
                  assertions[idx] = { ...a, expectedValue: e.target.value };
                  onUpdate({ assertions });
                }}
              >
                {(tc.comparisonMethod === 'COMPARE_TO'
                  ? COMPARISON_RESULT_COMPARE_TO_OPTIONS
                  : COMPARISON_RESULT_EQUALS_OPTIONS
                ).map((option) => (
                  <option key={option} value={option}>{option}</option>
                ))}
              </select>
            ) : (
              <input
                className="rounded border border-border bg-surface-secondary px-2 py-1 font-mono text-sm dark:text-white"
                value={a.expectedValue || ''}
                onChange={(e) => {
                  const assertions = [...(tc.assertions || [])];
                  assertions[idx] = { ...a, expectedValue: e.target.value };
                  onUpdate({ assertions });
                }}
                placeholder="Expected value JSON"
              />
            )}
            <select
              className="rounded border border-border bg-surface-secondary px-2 py-1 text-sm dark:text-white"
              value={a.comparisonMode || 'EXACT'}
              onChange={(e) => {
                const assertions = [...(tc.assertions || [])];
                assertions[idx] = { ...a, comparisonMode: e.target.value };
                onUpdate({ assertions });
              }}
            >
              {COMPARISON_MODES.map((m) => <option key={m} value={m}>{m}</option>)}
            </select>
          </div>
        ))}
        <button
          type="button"
          className="text-xs text-primary"
          onClick={() => onUpdate({
            assertions: [
              ...(tc.assertions || []),
              {
                id: crypto.randomUUID(),
                assertionKind: 'RETURN_VALUE',
                expectedValue: 'null',
                comparisonMode: 'EXACT',
                orderIndex: (tc.assertions || []).length,
              },
            ],
          })}
        >
          + Add assertion
        </button>
      </div>

      <label className="flex items-center gap-2 text-sm text-foreground-muted">
        <input
          type="checkbox"
          checked={!!tc.hidden}
          onChange={(e) => onUpdate({ hidden: e.target.checked })}
        />
        Hidden from students (pass/fail only)
      </label>
    </div>
  );
}

export default function TestcasesPanel({
  labId,
  challenge,
  structureDirty,
  onToast,
}) {
  const [testcases, setTestcases] = useState([]);
  const [snapshot, setSnapshot] = useState('[]');
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [runningId, setRunningId] = useState(null);
  const [selectedId, setSelectedId] = useState(null);
  const [referenceSources, setReferenceSources] = useState([]);
  const [dryRunResults, setDryRunResults] = useState({}); // { [testcaseId]: resultData }
  const [warnStructure, setWarnStructure] = useState(false);

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
      const res = await fetch(
        `${API_BASE}/api/lecturer/labs/${labId}/challenges/${challenge.id}/testcases`,
        { headers: authHeaders() },
      );
      if (!res.ok) throw new Error(await readApiErrorMessage(res));
      const data = await res.json();
      const rows = data.testcases || [];
      setTestcases(rows);
      setSnapshot(JSON.stringify(rows));
      setSelectedId((prev) => {
        if (prev && rows.some((r) => r.id === prev)) return prev;
        return rows[0]?.id ?? null;
      });
    } catch (e) {
      onToast?.({ type: 'error', message: e.message || 'Failed to load testcases' });
    } finally {
      setLoading(false);
    }
  }, [labId, challenge?.id, onToast]);

  useEffect(() => {
    loadTestcases();
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
  }, [labId, challenge?.id, loadTestcases]);

  useEffect(() => {
    if (structureDirty) setWarnStructure(true);
  }, [structureDirty]);

  useEffect(() => {
    sessionStorage.setItem(
      refStorageKey(labId, challenge?.id),
      JSON.stringify(referenceSources),
    );
  }, [referenceSources, labId, challenge?.id]);

  const memberOptions = useMemo(() => {
    const classes = challenge?.classes || [];
    const constructors = [];
    const methods = [];
    const fields = [];
    classes.forEach((cls) => {
      (cls.constructors || []).forEach((c) => {
        constructors.push({ id: c.id, label: `${cls.name}.<init>(...)` });
      });
      (cls.methods || []).forEach((m) => {
        methods.push({ id: m.id, label: `${cls.name}.${m.name}(...)` });
      });
      (cls.fields || []).forEach((f) => {
        fields.push({ id: f.id, label: `${cls.name}.${f.name}` });
      });
    });
    return { constructors, methods, fields };
  }, [challenge]);

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
      const res = await fetch(
        `${API_BASE}/api/lecturer/labs/${labId}/challenges/${challenge.id}/testcases`,
        {
          method: 'PUT',
          headers: { ...authHeaders(), 'Content-Type': 'application/json' },
          body: JSON.stringify(payload),
        },
      );
      if (!res.ok) throw new Error(await readApiErrorMessage(res));
      const data = await res.json();
      const rows = data.testcases || [];
      setTestcases(rows);
      setSnapshot(JSON.stringify(rows));
      onToast?.({ type: 'success', message: 'Testcases saved' });
    } catch (e) {
      onToast?.({ type: 'error', message: e.message || 'Save failed' });
    } finally {
      setSaving(false);
    }
  };

  const runDryRunForTestcase = async (tc) => {
    const res = await fetch(
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
    if (!res.ok) throw new Error(await readApiErrorMessage(res));
    return res.json();
  };

  const handleDryRun = async (tc) => {
    if (!labId || !challenge?.id) return;
    if (dryRunPayloadSources.length === 0) {
      onToast?.({ type: 'error', message: 'Add at least one reference Java file before running.' });
      return;
    }
    if (structureDirty) setWarnStructure(true);
    setRunningId(tc.id);
    try {
      const data = await runDryRunForTestcase(tc);
      setDryRunResults((prev) => ({ ...prev, [tc.id]: data }));
    } catch (e) {
      onToast?.({ type: 'error', message: e.message || 'Dry-run failed' });
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
        try {
          nextResults[tc.id] = await runDryRunForTestcase(tc);
        } catch (e) {
          failedCount += 1;
          onToast?.({ type: 'error', message: `${tc.name}: ${e.message || 'Dry-run failed'}` });
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

  const handleAddTestcase = () => {
    const tc = emptyTestcase(testcases.length);
    setTestcases([...testcases, tc]);
    setSelectedId(tc.id);
  };

  if (!challenge) {
    return (
      <div className="flex h-full min-h-[24rem] items-center justify-center rounded-xl border border-dashed border-border text-foreground-secondary">
        Select a problem from the structure sidebar.
      </div>
    );
  }

  return (
    <div className="space-y-4 pb-4">
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

      <div className="rounded-xl bg-surface">
        <div className="flex items-center justify-between px-4 py-3">
          <div className="flex items-center gap-2">
            <FlaskConical className="h-4 w-4 text-chart-green" />
            <span className="font-medium text-foreground">Operational Testcases</span>
          </div>
          <div className="flex items-center gap-2">
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
              onClick={handleAddTestcase}
              className="inline-flex items-center gap-1 text-sm text-primary-text transition-colors hover:text-foreground"
            >
              <Plus className="h-4 w-4" /> Add testcase
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
          <div className="grid min-h-[22rem] lg:grid-cols-[minmax(200px,260px)_1fr]">
            <aside className="border-b border-border-subtle p-2 lg:border-b-0 lg:border-r lg:border-border-subtle">
              {(dryRunSummary.pass > 0 || dryRunSummary.fail > 0) && (
                <div className="mb-2 flex flex-wrap gap-x-3 gap-y-1 px-2 text-[11px] text-foreground-muted">
                  {dryRunSummary.pass > 0 && (
                    <span className="text-success-text">{dryRunSummary.pass} passed</span>
                  )}
                  {dryRunSummary.fail > 0 && (
                    <span className="text-error-text">{dryRunSummary.fail} failed</span>
                  )}
                  {dryRunSummary.notRun > 0 && (
                    <span>{dryRunSummary.notRun} not run</span>
                  )}
                </div>
              )}
              <ul className="scrollbar-themed max-h-[min(70vh,42rem)] space-y-1 overflow-y-auto">
                {testcases.map((tc) => {
                  const isSelected = selectedId === tc.id;
                  const tcResult = dryRunResults[tc.id];
                  const isRunning = runningId === tc.id;
                  return (
                    <li key={tc.id}>
                      <button
                        type="button"
                        onClick={() => selectTestcase(tc.id)}
                        className={`group flex w-full items-center gap-2 rounded-lg px-3 py-2 text-left text-sm transition-colors ${
                          isSelected
                            ? 'bg-primary-light text-foreground'
                            : 'text-foreground-secondary hover:bg-surface-secondary hover:text-foreground'
                        }`}
                      >
                        <DryRunStatusIcon result={tcResult} running={isRunning} />
                        <span className="min-w-0 flex-1 truncate font-medium">{tc.name}</span>
                        {tc.hidden && (
                          <span className="shrink-0 rounded bg-foreground-muted/80 px-1.5 text-[10px] uppercase tracking-wide text-foreground">
                            hidden
                          </span>
                        )}
                      </button>
                    </li>
                  );
                })}
              </ul>
            </aside>

            <div className="flex max-h-[min(70vh,42rem)] flex-col p-4">
              {selectedTestcase ? (
                <div key={selectedTestcase.id} className="flex min-h-0 flex-1 flex-col animate-panel-in">
                  <div className="shrink-0 space-y-3 border-b border-border pb-3 dark:border-border">
                    <div className="flex items-start justify-between gap-3">
                      <div className="min-w-0 flex-1">
                        <label className="block text-[10px] font-semibold uppercase tracking-wide text-foreground-secondary">
                          Name
                        </label>
                        <input
                          type="text"
                          value={selectedTestcase.name ?? ''}
                          onChange={(e) => updateTestcase(selectedTestcase.id, { name: e.target.value })}
                          className="mt-1 w-full rounded-lg border border-border bg-surface-secondary px-3 py-2 text-sm font-medium text-foreground outline-none ring-primary/0 transition-shadow focus:border-primary/50 focus:ring-2 focus:ring-primary/20"
                          placeholder="Testcase name"
                        />
                        <p className="mt-1 text-xs text-foreground-muted">{selectedTestcase.testcaseType}</p>
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
                    <TestcaseEditor
                      tc={selectedTestcase}
                      memberOptions={memberOptions}
                      onUpdate={(patch) => updateTestcase(selectedTestcase.id, patch)}
                    />
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
