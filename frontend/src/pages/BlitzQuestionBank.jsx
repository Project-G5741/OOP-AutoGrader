import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Check,
  Download,
  Pencil,
  Plus,
  RefreshCw,
  Search,
  Trash2,
  Upload,
  X,
} from 'lucide-react';
import DashboardSection from '../components/lecturer/DashboardSection';
import ModalOverlay from '../components/ui/ModalOverlay';
import { useToast } from '../components/ui/Toast';
import { useDialog } from '../context/DialogContext';
import { apiFetch } from '../utils/apiFetch';
import { authHeaders } from '../utils/authHeaders';
import { friendlyLoadErrorFromResponse, readFriendlyApiError, toFriendlyError } from '../utils/apiError';
import { formatNumber } from '../utils/formatters';
import { tokenizeJavaLine } from '../utils/oopBlitzHelpers';
import {
  BLITZ_DIFFICULTIES,
  BLITZ_KINDS,
  buildBlitzSampleJson,
  buildBlitzUpsertPayload,
  createEmptyBlitzDraft,
  parseBlitzImportJson,
  splitCodeText,
  validateBlitzQuestionDraft,
} from '../utils/oopBlitzImport';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';
const PAGE_SIZE = 10;

const KIND_META = {
  BUG: { label: 'Bug Hunter', pill: 'bg-error-bg text-error-text' },
  OUTPUT: { label: 'Output', pill: 'bg-primary-light text-primary-text' },
  UML: { label: 'UML / MMD', pill: 'bg-success-bg text-success-text' },
  MODIFIER: { label: 'Modifier', pill: 'bg-warning-bg text-warning-text' },
};

const CODE_TOKEN_CLASSES = {
  kw: 'text-primary font-semibold',
  tp: 'text-secondary-text font-medium',
  str: 'text-success-text',
  num: 'text-warning-text',
  cmt: 'text-foreground-muted italic',
  plain: 'text-foreground',
};

function renderCodeLine(line) {
  return tokenizeJavaLine(line).map((tok, i) => (
    <span key={i} className={CODE_TOKEN_CLASSES[tok.type] || 'text-foreground'}>
      {tok.text}
    </span>
  ));
}

function difficultyLabel(raw) {
  if (!raw) return 'Easy';
  const s = String(raw).toUpperCase();
  return s.charAt(0) + s.slice(1).toLowerCase();
}

export default function BlitzQuestionBank() {
  const showToast = useToast();
  const dialog = useDialog();

  const [stats, setStats] = useState(null);
  const [questions, setQuestions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [busyId, setBusyId] = useState(null);

  const [searchInput, setSearchInput] = useState('');
  const [kindFilter, setKindFilter] = useState('ALL');
  const [diffFilter, setDiffFilter] = useState('ALL');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [page, setPage] = useState(0);

  // Add / Edit modal state
  const [editorOpen, setEditorOpen] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [draft, setDraft] = useState(createEmptyBlitzDraft);
  const [fieldErrors, setFieldErrors] = useState({});
  const [saving, setSaving] = useState(false);

  // Import JSON modal state
  const [importOpen, setImportOpen] = useState(false);
  const [importText, setImportText] = useState('');
  const [importError, setImportError] = useState('');
  const [importResult, setImportResult] = useState(null);
  const [importing, setImporting] = useState(false);

  const loadBank = useCallback(async () => {
    setLoading(true);
    setLoadError('');
    try {
      const response = await apiFetch(`${API_BASE}/api/lecturer/oop-blitz/questions`, {
        headers: authHeaders(),
      });
      if (!response.ok) {
        throw new Error(await friendlyLoadErrorFromResponse(response));
      }
      const data = await response.json();
      setStats(data.stats || null);
      setQuestions(Array.isArray(data.questions) ? data.questions : []);
    } catch (err) {
      setLoadError(toFriendlyError(err, 'read'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadBank();
  }, [loadBank]);

  const filteredQuestions = useMemo(() => {
    const query = searchInput.trim().toLowerCase();
    return questions.filter((q) => {
      if (kindFilter !== 'ALL' && q.kind !== kindFilter) return false;
      if (diffFilter !== 'ALL' && q.difficulty !== diffFilter) return false;
      if (statusFilter === 'ACTIVE' && !q.active) return false;
      if (statusFilter === 'DISABLED' && q.active) return false;
      if (!query) return true;
      const hay = [
        q.prompt,
        q.topic,
        q.explanation,
        ...(q.codeLines || []),
        ...(q.options || []),
      ]
        .filter(Boolean)
        .join('\n')
        .toLowerCase();
      return hay.includes(query);
    });
  }, [questions, searchInput, kindFilter, diffFilter, statusFilter]);

  const totalPages = Math.max(1, Math.ceil(filteredQuestions.length / PAGE_SIZE));
  const safePage = Math.min(page, totalPages - 1);
  const pageItems = useMemo(() => {
    const start = safePage * PAGE_SIZE;
    return filteredQuestions.slice(start, start + PAGE_SIZE);
  }, [filteredQuestions, safePage]);

  useEffect(() => {
    setPage(0);
  }, [searchInput, kindFilter, diffFilter, statusFilter]);

  const openAddModal = () => {
    setEditingId(null);
    setDraft(createEmptyBlitzDraft());
    setFieldErrors({});
    setEditorOpen(true);
  };

  const openEditModal = (q) => {
    setEditingId(q.id);
    setDraft({
      kind: q.kind,
      difficulty: q.difficulty,
      topic: q.topic || '',
      prompt: q.prompt || '',
      codeText: (q.codeLines || []).join('\n'),
      options: q.kind === 'BUG'
        ? ['', '', '', '']
        : [0, 1, 2, 3].map((i) => (q.options?.[i] ?? '')),
      correctIndex: Number(q.correctIndex ?? 0),
      explanation: q.explanation || '',
      active: q.active !== false,
    });
    setFieldErrors({});
    setEditorOpen(true);
  };

  const handleSaveQuestion = async (event) => {
    event.preventDefault();
    const errs = validateBlitzQuestionDraft(draft);
    setFieldErrors(errs);
    if (Object.keys(errs).length > 0 || saving) return;

    setSaving(true);
    try {
      const payload = buildBlitzUpsertPayload(draft);
      const url = editingId
        ? `${API_BASE}/api/lecturer/oop-blitz/questions/${editingId}`
        : `${API_BASE}/api/lecturer/oop-blitz/questions`;
      const response = await apiFetch(url, {
        method: editingId ? 'PUT' : 'POST',
        headers: { ...authHeaders(), 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });
      if (!response.ok) {
        throw new Error(await readFriendlyApiError(response, 'oop-blitz-bank'));
      }
      showToast(editingId ? 'Question updated.' : 'Question added.', 'success');
      setEditorOpen(false);
      await loadBank();
    } catch (err) {
      showToast(toFriendlyError(err, 'oop-blitz-bank'), 'error');
    } finally {
      setSaving(false);
    }
  };

  const handleToggleActive = async (q) => {
    if (busyId) return;
    setBusyId(q.id);
    try {
      const response = await apiFetch(`${API_BASE}/api/lecturer/oop-blitz/questions/${q.id}/active`, {
        method: 'PATCH',
        headers: { ...authHeaders(), 'Content-Type': 'application/json' },
        body: JSON.stringify({ active: !q.active }),
      });
      if (!response.ok) {
        throw new Error(await readFriendlyApiError(response, 'oop-blitz-bank'));
      }
      const updated = await response.json();
      setQuestions((prev) => prev.map((item) => (item.id === q.id ? updated : item)));
      setStats((prev) => {
        if (!prev) return prev;
        const delta = updated.active ? 1 : -1;
        return { ...prev, customActive: Math.max(0, (prev.customActive ?? 0) + delta) };
      });
      showToast(updated.active ? 'Question enabled.' : 'Question disabled.', 'success');
    } catch (err) {
      showToast(toFriendlyError(err, 'oop-blitz-bank'), 'error');
    } finally {
      setBusyId(null);
    }
  };

  const handleDeleteQuestion = async (q) => {
    if (busyId) return;
    const confirmed = await dialog.confirm({
      title: 'Delete question?',
      message: 'This removes the custom question from OOP Blitz.',
      confirmText: 'Delete',
      cancelText: 'Cancel',
      level: 'dangerous',
    });
    if (!confirmed) return;

    setBusyId(q.id);
    try {
      const response = await apiFetch(`${API_BASE}/api/lecturer/oop-blitz/questions/${q.id}`, {
        method: 'DELETE',
        headers: authHeaders(),
      });
      if (!response.ok) {
        throw new Error(await readFriendlyApiError(response, 'oop-blitz-bank'));
      }
      showToast('Question deleted.', 'success');
      await loadBank();
    } catch (err) {
      showToast(toFriendlyError(err, 'oop-blitz-bank'), 'error');
    } finally {
      setBusyId(null);
    }
  };

  const handleDownloadSample = () => {
    const blob = new Blob([buildBlitzSampleJson()], { type: 'application/json;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'oop-blitz-sample.json';
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
  };

  const handleFilePick = async (event) => {
    const file = event.target.files?.[0];
    if (!file) return;
    try {
      const content = await file.text();
      setImportText(content);
      setImportError('');
      setImportResult(null);
    } catch {
      setImportError('Could not read selected file.');
    } finally {
      event.target.value = '';
    }
  };

  const handleRunImport = async () => {
    if (importing) return;
    const parsed = parseBlitzImportJson(importText);
    if (parsed.error) {
      setImportError(parsed.error);
      return;
    }
    setImportError('');
    setImportResult(null);
    setImporting(true);
    try {
      const response = await apiFetch(`${API_BASE}/api/lecturer/oop-blitz/questions/import`, {
        method: 'POST',
        headers: { ...authHeaders(), 'Content-Type': 'application/json' },
        body: JSON.stringify({ questions: parsed.questions }),
      });
      if (!response.ok) {
        throw new Error(await readFriendlyApiError(response, 'oop-blitz-bank'));
      }
      const result = await response.json();
      setImportResult(result);
      showToast(
        `Imported ${result.importedCount} question${result.importedCount === 1 ? '' : 's'}.`,
        result.importedCount > 0 ? 'success' : 'warning',
      );
      if (result.importedCount > 0) {
        await loadBank();
      }
    } catch (err) {
      showToast(toFriendlyError(err, 'oop-blitz-bank'), 'error');
    } finally {
      setImporting(false);
    }
  };

  const draftLines = useMemo(() => splitCodeText(draft.codeText), [draft.codeText]);
  const procByKind = stats?.proceduralByKind || {};
  const customByDiff = stats?.customByDifficulty || {};

  return (
    <div className="max-w-full space-y-5">
      <DashboardSection
        title="Blitz Bank"
        actions={
          <div className="flex flex-wrap items-center gap-2">
            <button
              type="button"
              onClick={openAddModal}
              className="inline-flex h-10 cursor-pointer items-center gap-1.5 rounded-lg bg-primary px-3.5 text-sm font-semibold text-primary-foreground transition-colors hover:bg-primary-hover"
            >
              <Plus className="h-4 w-4" aria-hidden="true" />
              <span>Add Question</span>
            </button>
            <button
              type="button"
              onClick={() => {
                setImportError('');
                setImportResult(null);
                setImportOpen(true);
              }}
              className="inline-flex h-10 cursor-pointer items-center gap-1.5 rounded-lg border border-border bg-surface px-3.5 text-sm font-semibold text-foreground transition-colors hover:border-primary"
            >
              <Upload className="h-4 w-4" aria-hidden="true" />
              <span>Import JSON</span>
            </button>
            <button
              type="button"
              onClick={() => void loadBank()}
              className="inline-flex h-10 w-10 cursor-pointer items-center justify-center rounded-lg border border-border bg-surface text-foreground-secondary transition-colors hover:bg-surface-secondary"
              title="Refresh"
            >
              <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
            </button>
          </div>
        }
      >
        {loadError && (
          <div className="mb-4 rounded-lg border border-error/40 bg-error-bg px-4 py-3 text-sm text-error-text">
            {loadError}
          </div>
        )}

        {/* 4 Flat KPI Cells */}
        <div className="mb-5 grid grid-cols-2 divide-y divide-border-subtle rounded-xl border border-border bg-surface lg:grid-cols-4 lg:divide-x lg:divide-y-0">
          <div className="px-4 py-3">
            <div className="text-xs text-foreground-muted">Procedural Pool</div>
            <div className="mt-1 flex items-baseline gap-1.5">
              <span className="text-xl font-bold tabular-nums text-primary">
                {formatNumber(stats?.estimatedProceduralVariants ?? 15000)}+
              </span>
              <span className="text-xs text-foreground-muted tabular-nums">
                {stats?.proceduralTemplateCount ?? 62} templates
              </span>
            </div>
          </div>

          <div className="px-4 py-3">
            <div className="text-xs text-foreground-muted">Custom Questions</div>
            <div className="mt-1 flex items-baseline gap-1.5">
              <span className="text-xl font-bold tabular-nums">
                {formatNumber(stats?.customActive ?? 0)}
              </span>
              <span className="text-xs text-foreground-muted tabular-nums">
                active / {formatNumber(stats?.customTotal ?? 0)} total
              </span>
            </div>
          </div>

          <div className="px-4 py-3">
            <div className="text-xs text-foreground-muted">Procedural Kinds</div>
            <div className="mt-1.5 flex flex-wrap items-center gap-1.5 text-xs font-semibold tabular-nums">
              <span className="rounded px-1.5 py-0.5 bg-error-bg text-error-text">
                Bug {procByKind.BUG ?? 24}
              </span>
              <span className="rounded px-1.5 py-0.5 bg-primary-light text-primary-text">
                Out {procByKind.OUTPUT ?? 22}
              </span>
              <span className="rounded px-1.5 py-0.5 bg-success-bg text-success-text">
                UML {procByKind.UML ?? 9}
              </span>
              <span className="rounded px-1.5 py-0.5 bg-warning-bg text-warning-text">
                Mod {procByKind.MODIFIER ?? 7}
              </span>
            </div>
          </div>

          <div className="px-4 py-3">
            <div className="text-xs text-foreground-muted">Custom Tiers</div>
            <div className="mt-1.5 flex flex-wrap items-center gap-2 text-xs font-semibold tabular-nums text-foreground-secondary">
              <span>Easy {customByDiff.EASY ?? 0}</span>
              <span>·</span>
              <span>Medium {customByDiff.MEDIUM ?? 0}</span>
              <span>·</span>
              <span>Hard {customByDiff.HARD ?? 0}</span>
            </div>
          </div>
        </div>

        {/* Filter Bar */}
        <div className="mb-4 flex flex-wrap items-center gap-2.5">
          <div className="relative min-w-[220px] flex-1">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-foreground-muted" />
            <input
              type="text"
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              placeholder="Search custom questions, code, topic..."
              className="h-10 w-full rounded-lg border border-border bg-surface pl-9 pr-3 text-sm text-foreground placeholder-foreground-disabled focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </div>

          <select
            aria-label="Filter by kind"
            value={kindFilter}
            onChange={(e) => setKindFilter(e.target.value)}
            className="h-10 rounded-lg border border-border bg-surface px-3 text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
          >
            <option value="ALL">All Kinds</option>
            {BLITZ_KINDS.map((k) => (
              <option key={k} value={k}>
                {KIND_META[k].label}
              </option>
            ))}
          </select>

          <select
            aria-label="Filter by difficulty"
            value={diffFilter}
            onChange={(e) => setDiffFilter(e.target.value)}
            className="h-10 rounded-lg border border-border bg-surface px-3 text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
          >
            <option value="ALL">All Tiers</option>
            {BLITZ_DIFFICULTIES.map((d) => (
              <option key={d} value={d}>
                {difficultyLabel(d)}
              </option>
            ))}
          </select>

          <select
            aria-label="Filter by status"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="h-10 rounded-lg border border-border bg-surface px-3 text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
          >
            <option value="ALL">All Status</option>
            <option value="ACTIVE">Active</option>
            <option value="DISABLED">Disabled</option>
          </select>
        </div>

        {/* Custom Question List */}
        <div className="overflow-hidden rounded-xl border border-border bg-surface">
          {loading ? (
            <div className="flex items-center justify-center py-14">
              <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
            </div>
          ) : pageItems.length === 0 ? (
            <div className="px-6 py-12 text-center text-sm text-foreground-muted">
              {questions.length === 0
                ? 'No custom questions yet. Procedural Pool is live — add or import questions to blend into OOP Blitz.'
                : 'No custom questions match your filters.'}
            </div>
          ) : (
            <div className="divide-y divide-border-subtle">
              {pageItems.map((q) => {
                const km = KIND_META[q.kind] || KIND_META.OUTPUT;
                const previewLines = (q.codeLines || []).slice(0, 3);
                const extraLines = Math.max(0, (q.codeLines?.length ?? 0) - 3);
                const answerLabel = q.kind === 'BUG'
                  ? `Line ${(q.correctIndex ?? 0) + 1}`
                  : (q.options?.[q.correctIndex] ?? '');

                return (
                  <div
                    key={q.id}
                    className={`flex flex-col justify-between gap-3 px-4 py-3.5 sm:flex-row sm:items-start ${
                      q.active ? '' : 'opacity-60'
                    }`}
                  >
                    <div className="min-w-0 flex-1 space-y-2">
                      <div className="flex flex-wrap items-center gap-1.5 text-xs">
                        <span className={`rounded px-2 py-0.5 font-semibold ${km.pill}`}>
                          {km.label}
                        </span>
                        <span className="rounded border border-border-subtle px-2 py-0.5 font-medium text-foreground-secondary">
                          {difficultyLabel(q.difficulty)}
                        </span>
                        {q.topic && (
                          <span className="rounded bg-surface-secondary px-2 py-0.5 font-medium text-foreground-muted">
                            {q.topic}
                          </span>
                        )}
                        {!q.active && (
                          <span className="rounded bg-surface-tertiary px-2 py-0.5 font-semibold text-foreground-muted">
                            Disabled
                          </span>
                        )}
                      </div>

                      <div className="text-sm font-semibold text-foreground">{q.prompt}</div>

                      <div className="rounded-lg border border-border-subtle bg-surface-secondary px-3 py-1.5 font-mono text-xs leading-relaxed">
                        {previewLines.map((line, idx) => (
                          <div key={idx} className="truncate">
                            <span className="mr-2 select-none text-foreground-muted">{idx + 1}</span>
                            {renderCodeLine(line)}
                          </div>
                        ))}
                        {extraLines > 0 && (
                          <div className="text-foreground-muted">+{extraLines} more lines</div>
                        )}
                      </div>

                      <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs">
                        <span className="font-semibold text-success-text">
                          Answer: <span className="font-mono">{answerLabel}</span>
                        </span>
                        <span className="text-foreground-muted">{q.explanation}</span>
                      </div>
                    </div>

                    <div className="flex shrink-0 items-center gap-1.5">
                      <button
                        type="button"
                        disabled={busyId === q.id}
                        onClick={() => void handleToggleActive(q)}
                        className={`h-8 cursor-pointer rounded-lg border px-2.5 text-xs font-semibold transition-colors ${
                          q.active
                            ? 'border-success/40 bg-success-bg text-success-text'
                            : 'border-border bg-surface text-foreground-muted hover:text-foreground'
                        }`}
                      >
                        {q.active ? 'Active' : 'Disabled'}
                      </button>
                      <button
                        type="button"
                        disabled={busyId === q.id}
                        onClick={() => openEditModal(q)}
                        aria-label="Edit question"
                        className="inline-flex h-8 w-8 cursor-pointer items-center justify-center rounded-lg border border-border text-foreground-secondary transition-colors hover:border-primary hover:text-primary"
                      >
                        <Pencil className="h-3.5 w-3.5" />
                      </button>
                      <button
                        type="button"
                        disabled={busyId === q.id}
                        onClick={() => void handleDeleteQuestion(q)}
                        aria-label="Delete question"
                        className="inline-flex h-8 w-8 cursor-pointer items-center justify-center rounded-lg border border-border text-foreground-secondary transition-colors hover:border-error hover:text-error-text"
                      >
                        <Trash2 className="h-3.5 w-3.5" />
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          )}

          {totalPages > 1 && (
            <div className="flex items-center justify-between border-t border-border-subtle px-4 py-2.5 text-xs text-foreground-muted">
              <span>
                Page {safePage + 1} of {totalPages} ({filteredQuestions.length} items)
              </span>
              <div className="flex items-center gap-1.5">
                <button
                  type="button"
                  disabled={safePage === 0}
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  className="h-7 cursor-pointer rounded border border-border px-2.5 font-medium text-foreground disabled:cursor-not-allowed disabled:opacity-40"
                >
                  Prev
                </button>
                <button
                  type="button"
                  disabled={safePage >= totalPages - 1}
                  onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                  className="h-7 cursor-pointer rounded border border-border px-2.5 font-medium text-foreground disabled:cursor-not-allowed disabled:opacity-40"
                >
                  Next
                </button>
              </div>
            </div>
          )}
        </div>
      </DashboardSection>

      {/* ==================== ADD / EDIT MODAL ==================== */}
      {editorOpen && (
        <ModalOverlay onBackdropClick={() => !saving && setEditorOpen(false)}>
          <form
            onSubmit={handleSaveQuestion}
            onClick={(e) => e.stopPropagation()}
            className="max-h-[90vh] w-full max-w-2xl overflow-y-auto rounded-xl border border-border bg-surface text-foreground shadow-stage-3"
          >
            <div className="flex items-center justify-between border-b border-border-subtle px-5 py-3.5">
              <h3 className="text-base font-bold">
                {editingId ? 'Edit Question' : 'Add Question'}
              </h3>
              <button
                type="button"
                disabled={saving}
                onClick={() => setEditorOpen(false)}
                className="inline-flex h-8 w-8 cursor-pointer items-center justify-center rounded-lg border border-border-subtle text-foreground-muted hover:bg-surface-secondary"
              >
                <X className="h-4 w-4" />
              </button>
            </div>

            <div className="space-y-4 p-5 text-sm">
              <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
                <label className="space-y-1">
                  <span className="text-xs font-semibold text-foreground-secondary">Kind</span>
                  <select
                    value={draft.kind}
                    onChange={(e) => {
                      const nextKind = e.target.value;
                      setDraft((d) => ({ ...d, kind: nextKind, correctIndex: 0 }));
                      setFieldErrors({});
                    }}
                    className="h-9 w-full rounded-lg border border-border bg-surface px-2.5 text-sm"
                  >
                    {BLITZ_KINDS.map((k) => (
                      <option key={k} value={k}>
                        {KIND_META[k].label}
                      </option>
                    ))}
                  </select>
                </label>

                <label className="space-y-1">
                  <span className="text-xs font-semibold text-foreground-secondary">Difficulty</span>
                  <select
                    value={draft.difficulty}
                    onChange={(e) => setDraft((d) => ({ ...d, difficulty: e.target.value }))}
                    className="h-9 w-full rounded-lg border border-border bg-surface px-2.5 text-sm"
                  >
                    {BLITZ_DIFFICULTIES.map((d) => (
                      <option key={d} value={d}>
                        {difficultyLabel(d)}
                      </option>
                    ))}
                  </select>
                </label>

                <label className="space-y-1">
                  <span className="text-xs font-semibold text-foreground-secondary">Topic</span>
                  <input
                    type="text"
                    maxLength={64}
                    value={draft.topic}
                    onChange={(e) => setDraft((d) => ({ ...d, topic: e.target.value }))}
                    placeholder="e.g. Polymorphism"
                    className="h-9 w-full rounded-lg border border-border bg-surface px-2.5 text-sm"
                  />
                </label>
              </div>

              <label className="block space-y-1">
                <span className="text-xs font-semibold text-foreground-secondary">Question</span>
                <input
                  type="text"
                  maxLength={400}
                  value={draft.prompt}
                  onChange={(e) => setDraft((d) => ({ ...d, prompt: e.target.value }))}
                  placeholder="What does this snippet print?"
                  className="h-9 w-full rounded-lg border border-border bg-surface px-3 text-sm"
                />
                {fieldErrors.prompt && (
                  <p className="text-xs text-error-text">{fieldErrors.prompt}</p>
                )}
              </label>

              <label className="block space-y-1">
                <span className="text-xs font-semibold text-foreground-secondary">Code Snippet</span>
                <textarea
                  rows={5}
                  value={draft.codeText}
                  onChange={(e) => setDraft((d) => ({ ...d, codeText: e.target.value }))}
                  placeholder="class Base { ... }"
                  className="w-full rounded-lg border border-border bg-surface-secondary p-3 font-mono text-xs leading-relaxed"
                />
                {fieldErrors.codeLines && (
                  <p className="text-xs text-error-text">{fieldErrors.codeLines}</p>
                )}
              </label>

              {draft.kind === 'BUG' ? (
                <div className="space-y-1.5">
                  <span className="text-xs font-semibold text-foreground-secondary">
                    Buggy Line
                  </span>
                  <div className="max-h-40 overflow-y-auto rounded-lg border border-border bg-surface-secondary py-1 font-mono text-xs">
                    {draftLines.map((line, idx) => {
                      const selected = draft.correctIndex === idx;
                      return (
                        <button
                          key={idx}
                          type="button"
                          onClick={() => setDraft((d) => ({ ...d, correctIndex: idx }))}
                          className={`flex w-full cursor-pointer items-center gap-2.5 px-3 py-1 text-left ${
                            selected
                              ? 'bg-error-bg font-semibold text-error-text'
                              : 'hover:bg-primary-light'
                          }`}
                        >
                          <span className="w-4 shrink-0 text-right tabular-nums text-foreground-muted">
                            {idx + 1}
                          </span>
                          <span className="min-w-0 flex-1 truncate">{line || ' '}</span>
                          {selected && <Check className="h-3.5 w-3.5 shrink-0" />}
                        </button>
                      );
                    })}
                  </div>
                  {fieldErrors.correctIndex && (
                    <p className="text-xs text-error-text">{fieldErrors.correctIndex}</p>
                  )}
                </div>
              ) : (
                <div className="space-y-2">
                  <span className="text-xs font-semibold text-foreground-secondary">
                    Choices (select correct)
                  </span>
                  <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
                    {[0, 1, 2, 3].map((idx) => (
                      <div key={idx} className="flex items-center gap-2">
                        <input
                          type="radio"
                          name="blitz-correct-opt"
                          checked={draft.correctIndex === idx}
                          onChange={() => setDraft((d) => ({ ...d, correctIndex: idx }))}
                          aria-label={`Mark choice ${idx + 1} correct`}
                          className="h-4 w-4 accent-primary"
                        />
                        <input
                          type="text"
                          maxLength={180}
                          value={draft.options[idx] ?? ''}
                          onChange={(e) => {
                            const val = e.target.value;
                            setDraft((d) => {
                              const next = [...d.options];
                              next[idx] = val;
                              return { ...d, options: next };
                            });
                          }}
                          placeholder={`Choice ${idx + 1}`}
                          className="h-9 min-w-0 flex-1 rounded-lg border border-border bg-surface px-2.5 font-mono text-xs"
                        />
                      </div>
                    ))}
                  </div>
                  {(fieldErrors.options || fieldErrors.correctIndex) && (
                    <p className="text-xs text-error-text">
                      {fieldErrors.options || fieldErrors.correctIndex}
                    </p>
                  )}
                </div>
              )}

              <label className="block space-y-1">
                <span className="text-xs font-semibold text-foreground-secondary">Explanation</span>
                <input
                  type="text"
                  maxLength={400}
                  value={draft.explanation}
                  onChange={(e) => setDraft((d) => ({ ...d, explanation: e.target.value }))}
                  placeholder="Short 1-line Java explanation"
                  className="h-9 w-full rounded-lg border border-border bg-surface px-3 text-sm"
                />
                {fieldErrors.explanation && (
                  <p className="text-xs text-error-text">{fieldErrors.explanation}</p>
                )}
              </label>

              <label className="inline-flex cursor-pointer items-center gap-2 text-xs font-semibold text-foreground-secondary">
                <input
                  type="checkbox"
                  checked={draft.active}
                  onChange={(e) => setDraft((d) => ({ ...d, active: e.target.checked }))}
                  className="h-4 w-4 accent-primary"
                />
                <span>Active in student Blitz pool</span>
              </label>
            </div>

            <div className="flex items-center justify-end gap-2 border-t border-border-subtle px-5 py-3">
              <button
                type="button"
                disabled={saving}
                onClick={() => setEditorOpen(false)}
                className="h-9 cursor-pointer rounded-lg border border-border px-3.5 text-sm font-semibold text-foreground"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={saving}
                className="h-9 cursor-pointer rounded-lg bg-primary px-4 text-sm font-bold text-primary-foreground hover:bg-primary-hover disabled:opacity-50"
              >
                {saving ? 'Saving...' : 'Save'}
              </button>
            </div>
          </form>
        </ModalOverlay>
      )}

      {/* ==================== IMPORT JSON MODAL ==================== */}
      {importOpen && (
        <ModalOverlay onBackdropClick={() => !importing && setImportOpen(false)}>
          <div
            onClick={(e) => e.stopPropagation()}
            className="w-full max-w-2xl overflow-hidden rounded-xl border border-border bg-surface text-foreground shadow-stage-3"
          >
            <div className="flex items-center justify-between border-b border-border-subtle px-5 py-3.5">
              <h3 className="text-base font-bold">Import JSON</h3>
              <button
                type="button"
                disabled={importing}
                onClick={() => setImportOpen(false)}
                className="inline-flex h-8 w-8 cursor-pointer items-center justify-center rounded-lg border border-border-subtle text-foreground-muted hover:bg-surface-secondary"
              >
                <X className="h-4 w-4" />
              </button>
            </div>

            <div className="space-y-3 p-5 text-sm">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <label className="inline-flex h-9 cursor-pointer items-center gap-1.5 rounded-lg border border-border bg-surface-secondary px-3 text-xs font-semibold text-foreground hover:border-primary">
                  <Upload className="h-3.5 w-3.5" aria-hidden="true" />
                  <span>Choose .json File</span>
                  <input
                    type="file"
                    accept=".json,application/json"
                    onChange={handleFilePick}
                    className="hidden"
                  />
                </label>

                <button
                  type="button"
                  onClick={handleDownloadSample}
                  className="inline-flex h-9 cursor-pointer items-center gap-1.5 rounded-lg border border-border px-3 text-xs font-semibold text-foreground-secondary hover:border-primary hover:text-primary"
                >
                  <Download className="h-3.5 w-3.5" aria-hidden="true" />
                  <span>Download Sample JSON</span>
                </button>
              </div>

              <textarea
                rows={9}
                value={importText}
                onChange={(e) => setImportText(e.target.value)}
                placeholder='{ "questions": [ ... ] }'
                className="w-full rounded-lg border border-border bg-surface-secondary p-3 font-mono text-xs leading-relaxed"
              />

              {importError && (
                <div className="rounded-lg bg-error-bg px-3 py-2 text-xs font-medium text-error-text">
                  {importError}
                </div>
              )}

              {importResult && (
                <div className="space-y-1.5 rounded-lg border border-border-subtle bg-surface-secondary p-3 text-xs">
                  <div className="font-semibold text-foreground">
                    Imported {importResult.importedCount} · Skipped {importResult.skippedCount}
                  </div>
                  {(importResult.errors || []).slice(0, 10).map((errLine, idx) => (
                    <div key={idx} className="text-error-text">
                      {errLine}
                    </div>
                  ))}
                </div>
              )}
            </div>

            <div className="flex items-center justify-end gap-2 border-t border-border-subtle px-5 py-3">
              <button
                type="button"
                disabled={importing}
                onClick={() => setImportOpen(false)}
                className="h-9 cursor-pointer rounded-lg border border-border px-3.5 text-sm font-semibold text-foreground"
              >
                Close
              </button>
              <button
                type="button"
                disabled={importing}
                onClick={() => void handleRunImport()}
                className="h-9 cursor-pointer rounded-lg bg-primary px-4 text-sm font-bold text-primary-foreground hover:bg-primary-hover disabled:opacity-50"
              >
                {importing ? 'Importing...' : 'Import'}
              </button>
            </div>
          </div>
        </ModalOverlay>
      )}
    </div>
  );
}
