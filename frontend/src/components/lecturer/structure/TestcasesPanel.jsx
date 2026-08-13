import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { FlaskConical, Loader2, Play, Plus, Save, Trash2 } from 'lucide-react';
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
      expectedValue: a.expectedValue?.trim() ? a.expectedValue.trim() : 'null',
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
      expectedValue: '0',
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

function DryRunResultCard({ result }) {
  if (!result) return null;
  const passed = result.result === 'PASS';
  const assertions = result.assertions ?? [];
  const hasAssertionRows = assertions.length > 0
    && assertions.some((a) => a.expected_output ?? a.expectedOutput);

  return (
    <div className={`animate-panel-in rounded-lg border p-3 text-sm ${
      passed
        ? 'border-emerald-500/40 bg-emerald-500/10'
        : 'border-red-500/40 bg-red-500/10'
    }`}
    >
      <div className={`mb-3 font-semibold ${passed ? 'text-emerald-300' : 'text-red-300'}`}>
        Dry-run: {result.result}
      </div>

      {result.input != null && (
        <div className="mb-3 rounded-md bg-black/20 p-2">
          <div className="mb-1 text-[10px] font-semibold uppercase tracking-wide text-gray-500">Input</div>
          <pre className="whitespace-pre-wrap font-mono text-xs text-gray-200">{result.input}</pre>
        </div>
      )}

      {hasAssertionRows ? (
        <div className="scrollbar-themed max-h-48 space-y-2 overflow-y-auto pr-1">
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
                className="rounded-md border border-gray-700/60 bg-black/15 p-2"
              >
                <div className="mb-2 flex items-center justify-between gap-2">
                  <span className="text-[10px] font-semibold uppercase tracking-wide text-gray-400">
                    {fieldLabel ?? assertion.kind}
                  </span>
                  <span className={`text-[10px] font-bold ${aPassed ? 'text-emerald-400' : 'text-red-400'}`}>
                    {assertion.result}
                  </span>
                </div>
                <div className="grid gap-2 sm:grid-cols-2">
                  <div>
                    <div className="mb-1 text-[10px] font-semibold uppercase tracking-wide text-gray-500">Expected</div>
                    <pre className="whitespace-pre-wrap font-mono text-xs text-emerald-300/90">{expected}</pre>
                  </div>
                  <div>
                    <div className="mb-1 text-[10px] font-semibold uppercase tracking-wide text-gray-500">Actual</div>
                    <pre className={`whitespace-pre-wrap font-mono text-xs ${aPassed ? 'text-emerald-300/90' : 'text-red-300'}`}>
                      {actual}
                    </pre>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      ) : (
        <div className="grid gap-2 sm:grid-cols-2">
          {result.expected_output != null && (
            <div className="rounded-md bg-black/20 p-2">
              <div className="mb-1 text-[10px] font-semibold uppercase tracking-wide text-gray-500">Expected</div>
              <pre className="whitespace-pre-wrap font-mono text-xs text-gray-200">{result.expected_output}</pre>
            </div>
          )}
          {result.actual_output != null && (
            <div className="rounded-md bg-black/20 p-2">
              <div className="mb-1 text-[10px] font-semibold uppercase tracking-wide text-gray-500">Actual</div>
              <pre className="whitespace-pre-wrap font-mono text-xs text-gray-200">{result.actual_output}</pre>
            </div>
          )}
        </div>
      )}

      {result.feedback && (
        <p className="mt-2 text-xs text-gray-400">{result.feedback}</p>
      )}
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
        <label className="block text-xs text-gray-500 sm:col-span-2">
          Type
          <select
            className="mt-1 w-full rounded border border-gray-300 bg-white px-2 py-1.5 text-sm dark:border-gray-700 dark:bg-[#0d1117] dark:text-white"
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
          <label className="block text-xs text-gray-500">
            Invocation kind
            <select
              className="mt-1 w-full rounded border border-gray-300 bg-white px-2 py-1.5 text-sm dark:border-gray-700 dark:bg-[#0d1117] dark:text-white"
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
            <label className="block text-xs text-gray-500">
              Constructor
              <select
                className="mt-1 w-full rounded border border-gray-300 bg-white px-2 py-1.5 text-sm dark:border-gray-700 dark:bg-[#0d1117] dark:text-white"
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
              <label className="block text-xs text-gray-500">
                Method
                <select
                  className="mt-1 w-full rounded border border-gray-300 bg-white px-2 py-1.5 text-sm dark:border-gray-700 dark:bg-[#0d1117] dark:text-white"
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
              <label className="block text-xs text-gray-500">
                Receiver constructor (optional)
                <select
                  className="mt-1 w-full rounded border border-gray-300 bg-white px-2 py-1.5 text-sm dark:border-gray-700 dark:bg-[#0d1117] dark:text-white"
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
              <label className="block text-xs text-gray-500 sm:col-span-2">
                Receiver params (JSON array)
                <input
                  className="mt-1 w-full rounded border border-gray-300 bg-white px-2 py-1.5 font-mono text-sm dark:border-gray-700 dark:bg-[#0d1117] dark:text-white"
                  value={tc.invocation.receiverParams || '[]'}
                  onChange={(e) => onUpdate({
                    invocation: { ...tc.invocation, receiverParams: e.target.value },
                  })}
                />
              </label>
            </>
          )}
          <label className="block text-xs text-gray-500 sm:col-span-2">
            Params (JSON array)
            <input
              className="mt-1 w-full rounded border border-gray-300 bg-white px-2 py-1.5 font-mono text-sm dark:border-gray-700 dark:bg-[#0d1117] dark:text-white"
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
            className="w-full rounded border border-gray-300 bg-white px-2 py-1.5 text-sm dark:border-gray-700 dark:bg-[#0d1117] dark:text-white"
            value={tc.comparisonMethod || 'EQUALS'}
            onChange={(e) => onUpdate({ comparisonMethod: e.target.value })}
          >
            <option value="EQUALS">EQUALS</option>
            <option value="COMPARE_TO">COMPARE_TO</option>
          </select>
          {(tc.instances || []).map((inst, idx) => (
            <div key={inst.id || idx} className="grid gap-2 sm:grid-cols-2">
              <span className="text-xs text-gray-500">Instance {inst.label}</span>
              <select
                className="rounded border border-gray-300 bg-white px-2 py-1.5 text-sm dark:border-gray-700 dark:bg-[#0d1117] dark:text-white"
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
                className="sm:col-span-2 rounded border border-gray-300 bg-white px-2 py-1.5 font-mono text-sm dark:border-gray-700 dark:bg-[#0d1117] dark:text-white"
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
        <div className="text-xs font-semibold text-gray-500">Assertions</div>
        {(tc.assertions || []).map((a, idx) => (
          <div key={a.id || idx} className="grid gap-2 rounded border border-gray-200 p-2 dark:border-gray-700 sm:grid-cols-3">
            <select
              className="rounded border border-gray-300 bg-white px-2 py-1 text-sm dark:border-gray-700 dark:bg-[#0d1117] dark:text-white"
              value={a.assertionKind}
              onChange={(e) => {
                const assertions = [...(tc.assertions || [])];
                const nextKind = e.target.value;
                assertions[idx] = {
                  ...a,
                  assertionKind: nextKind,
                  fieldId: nextKind === 'FIELD_STATE' ? (a.fieldId || null) : null,
                };
                onUpdate({ assertions });
              }}
            >
              {ASSERTION_KINDS.map((k) => <option key={k} value={k}>{k}</option>)}
            </select>
            {a.assertionKind === 'FIELD_STATE' && (
              <select
                className="rounded border border-gray-300 bg-white px-2 py-1 text-sm dark:border-gray-700 dark:bg-[#0d1117] dark:text-white"
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
            <input
              className="rounded border border-gray-300 bg-white px-2 py-1 font-mono text-sm dark:border-gray-700 dark:bg-[#0d1117] dark:text-white"
              value={a.expectedValue || ''}
              onChange={(e) => {
                const assertions = [...(tc.assertions || [])];
                assertions[idx] = { ...a, expectedValue: e.target.value };
                onUpdate({ assertions });
              }}
              placeholder="Expected value JSON"
            />
            <select
              className="rounded border border-gray-300 bg-white px-2 py-1 text-sm dark:border-gray-700 dark:bg-[#0d1117] dark:text-white"
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
          className="text-xs text-purple-400"
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

      <label className="flex items-center gap-2 text-sm text-gray-400">
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
  const [dryRunResult, setDryRunResult] = useState(null); // { testcaseId, data }
  const [warnStructure, setWarnStructure] = useState(false);

  const isDirty = useMemo(
    () => JSON.stringify(testcases) !== snapshot,
    [testcases, snapshot],
  );

  const selectedTestcase = useMemo(
    () => testcases.find((tc) => tc.id === selectedId) ?? null,
    [testcases, selectedId],
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
    setDryRunResult(null);
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
  };

  const selectTestcase = (id) => {
    if (id === selectedId) return;
    setSelectedId(id);
    setDryRunResult(null);
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

  const handleDryRun = async (tc) => {
    if (!labId || !challenge?.id) return;
    if (dryRunPayloadSources.length === 0) {
      onToast?.({ type: 'error', message: 'Add at least one reference Java file before running.' });
      return;
    }
    if (structureDirty) setWarnStructure(true);
    setRunningId(tc.id);
    setDryRunResult(null);
    try {
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
      const data = await res.json();
      setDryRunResult({ testcaseId: tc.id, data });
    } catch (e) {
      onToast?.({ type: 'error', message: e.message || 'Dry-run failed' });
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
      setDryRunResult(null);
    }
  };

  const handleAddTestcase = () => {
    const tc = emptyTestcase(testcases.length);
    setTestcases([...testcases, tc]);
    setSelectedId(tc.id);
    setDryRunResult(null);
  };

  if (!challenge) {
    return (
      <div className="flex h-full min-h-[24rem] items-center justify-center rounded-xl border border-dashed border-gray-300 text-gray-500 dark:border-gray-700 dark:text-gray-400">
        Select a problem from the structure sidebar.
      </div>
    );
  }

  return (
    <div className="space-y-4 pb-4">
      {warnStructure && (
        <div className="rounded-lg border border-amber-500/40 bg-amber-500/10 px-3 py-2 text-sm text-amber-200">
          Lab structure has unsaved changes. Save structure first so new methods and fields can be referenced.
        </div>
      )}

      <div className="rounded-xl border border-gray-200 bg-white p-4 dark:border-gray-800 dark:bg-[#0f1419]">
        <div className="mb-3 text-sm font-medium text-gray-700 dark:text-gray-200">
          Reference Java (dry-run)
        </div>
        <ReferenceJavaFiles
          sources={referenceSources}
          onChange={setReferenceSources}
          onError={(message) => onToast?.({ type: 'error', message })}
        />
      </div>

      <div className="rounded-xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-[#0f1419]">
        <div className="flex items-center justify-between border-b border-gray-200 px-4 py-3 dark:border-gray-800">
          <div className="flex items-center gap-2">
            <FlaskConical className="h-4 w-4 text-emerald-400" />
            <span className="font-medium text-gray-800 dark:text-gray-100">Operational Testcases</span>
          </div>
          <button
            type="button"
            onClick={handleAddTestcase}
            className="inline-flex items-center gap-1 text-sm text-purple-400 transition-colors hover:text-purple-300"
          >
            <Plus className="h-4 w-4" /> Add testcase
          </button>
        </div>

        {loading ? (
          <div className="flex justify-center py-12">
            <Loader2 className="h-6 w-6 animate-spin text-purple-400" />
          </div>
        ) : testcases.length === 0 ? (
          <p className="px-4 py-8 text-center text-sm text-gray-500">No testcases yet.</p>
        ) : (
          <div className="grid min-h-[22rem] lg:grid-cols-[minmax(200px,260px)_1fr]">
            <aside className="border-b border-gray-200 p-2 dark:border-gray-800 lg:border-b-0 lg:border-r">
              <ul className="space-y-1">
                {testcases.map((tc) => {
                  const isSelected = selectedId === tc.id;
                  return (
                    <li key={tc.id}>
                      <button
                        type="button"
                        onClick={() => selectTestcase(tc.id)}
                        className={`group flex w-full items-center gap-2 rounded-lg px-3 py-2.5 text-left text-sm transition-all duration-200 ease-out ${
                          isSelected
                            ? 'border border-purple-500/40 bg-purple-500/15 text-gray-100 shadow-sm'
                            : 'border border-transparent text-gray-400 hover:bg-gray-100 hover:text-gray-800 dark:hover:bg-gray-800/60 dark:hover:text-gray-200'
                        }`}
                      >
                        <span className="min-w-0 flex-1 truncate font-medium">{tc.name}</span>
                        {tc.hidden && (
                          <span className="shrink-0 rounded bg-gray-600/80 px-1.5 text-[10px] uppercase tracking-wide">
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
                  <div className="shrink-0 space-y-3 border-b border-gray-200 pb-3 dark:border-gray-800">
                    <div className="flex items-start justify-between gap-3">
                      <div className="min-w-0 flex-1">
                        <label className="block text-[10px] font-semibold uppercase tracking-wide text-gray-500">
                          Name
                        </label>
                        <input
                          type="text"
                          value={selectedTestcase.name ?? ''}
                          onChange={(e) => updateTestcase(selectedTestcase.id, { name: e.target.value })}
                          className="mt-1 w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm font-medium text-gray-800 outline-none ring-purple-500/0 transition-shadow focus:border-purple-500/50 focus:ring-2 focus:ring-purple-500/20 dark:border-gray-700 dark:bg-[#0d1117] dark:text-gray-100"
                          placeholder="Testcase name"
                        />
                        <p className="mt-1 text-xs text-gray-500">{selectedTestcase.testcaseType}</p>
                      </div>
                      <div className="flex shrink-0 items-center gap-1 pt-4">
                        <button
                          type="button"
                          disabled={runningId === selectedTestcase.id}
                          onClick={() => handleDryRun(selectedTestcase)}
                          className="inline-flex items-center gap-1.5 rounded-lg bg-emerald-600 px-3 py-2 text-xs font-semibold text-white transition-colors hover:bg-emerald-500 disabled:opacity-50"
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
                          className="rounded-lg p-2 text-gray-400 transition-colors hover:bg-red-500/10 hover:text-red-400"
                          title="Delete testcase"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </div>
                    </div>

                    {dryRunResult?.testcaseId === selectedTestcase.id && (
                      <DryRunResultCard result={dryRunResult.data} />
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
                <div className="flex h-full min-h-[16rem] items-center justify-center text-sm text-gray-500">
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
          className="inline-flex items-center gap-2 rounded-full bg-emerald-600 px-5 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-emerald-500 disabled:opacity-50"
        >
          {saving ? <Loader2 className="h-4 w-4 animate-spin" /> : <Save className="h-4 w-4" />}
          Save Testcases
        </button>
      </div>
    </div>
  );
}
