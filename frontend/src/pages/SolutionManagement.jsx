import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Check, Loader2 } from 'lucide-react';
import Modal from '../components/ui/Modal';
import Toast from '../components/ui/Toast';
import ClassDetailPanel from '../components/lecturer/structure/ClassDetailPanel';
import ChallengeDetailPanel from '../components/lecturer/structure/ChallengeDetailPanel';
import StructureSidebar from '../components/lecturer/structure/StructureSidebar';
import DatePicker from '../components/ui/DatePicker';
import { authHeaders } from '../utils/authHeaders';
import { readFriendlyApiError, toFriendlyError } from '../utils/apiError';
import { formatQualifiedClassName } from '../utils/classNaming';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';

const emptyDraft = (lab) => ({
  id: lab.id,
  name: lab.name,
  termId: lab.termId || null,
  deadlineDate: lab.deadlineDate ?? null,
  challenges: [],
});

function cloneDraft(data) {
  return JSON.parse(JSON.stringify(data));
}

function toDateInputValue(value) {
  if (value == null || value === '') return '';
  if (typeof value === 'string') return value.slice(0, 10);
  if (Array.isArray(value) && value.length >= 3) {
    const [year, month, day] = value;
    return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
  }
  return '';
}

function isValidCalendarDate(value) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return false;
  const [year, month, day] = value.split('-').map(Number);
  const utc = new Date(Date.UTC(year, month - 1, day));
  return utc.getUTCFullYear() === year && utc.getUTCMonth() === month - 1 && utc.getUTCDate() === day;
}

function collectNestedDependents(classes, outerClassId, acc = new Set()) {
  (classes || []).forEach((cls) => {
    if (cls.outerClassId === outerClassId && !acc.has(cls.id)) {
      acc.add(cls.id);
      collectNestedDependents(classes, cls.id, acc);
    }
  });
  return acc;
}

export default function SolutionManagement() {
  const [labs, setLabs] = useState([]);
  const [scopeOptions, setScopeOptions] = useState([]);
  const [declaringTypeOptions, setDeclaringTypeOptions] = useState([]);
  const [relationTypeOptions, setRelationTypeOptions] = useState([]);
  const [terms, setTerms] = useState([]);
  const [selectedLabId, setSelectedLabId] = useState(null);
  const [draft, setDraft] = useState(null);
  const [savedSnapshot, setSavedSnapshot] = useState(null);
  const [expandedLabs, setExpandedLabs] = useState({});
  const [expandedChallenges, setExpandedChallenges] = useState({});
  const [selectedClassRef, setSelectedClassRef] = useState(null);
  const [selectedChallengeId, setSelectedChallengeId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [structureLoading, setStructureLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [toast, setToast] = useState(null);
  const [showCreateLab, setShowCreateLab] = useState(false);
  const [newLabName, setNewLabName] = useState('');
  const [newLabTermId, setNewLabTermId] = useState('');
  const [newLabDeadline, setNewLabDeadline] = useState('');
  const [confirmDelete, setConfirmDelete] = useState(null);
  const [challengeTabById, setChallengeTabById] = useState({});
  const [deadlineInput, setDeadlineInput] = useState('');
  const [deadlineSaving, setDeadlineSaving] = useState(false);

  const isDirty = useMemo(() => {
    if (!draft || !savedSnapshot) return false;
    return JSON.stringify(draft) !== JSON.stringify(savedSnapshot);
  }, [draft, savedSnapshot]);

  const isDirtyRef = useRef(isDirty);
  const savingRef = useRef(false);
  const structureCacheRef = useRef({});
  useEffect(() => {
    isDirtyRef.current = isDirty;
  }, [isDirty]);

  useEffect(() => {
    setDeadlineInput(toDateInputValue(savedSnapshot?.deadlineDate));
  }, [selectedLabId, savedSnapshot?.deadlineDate]);

  const loadLookups = useCallback(async () => {
    const [scopeRes, declaringRes, relationRes, termsRes] = await Promise.all([
      fetch(`${API_BASE}/api/master-data?category=SCOPE`),
      fetch(`${API_BASE}/api/master-data?category=DECLARING_TYPE`),
      fetch(`${API_BASE}/api/master-data?category=RELATION_TYPE`),
      fetch(`${API_BASE}/api/terms`),
    ]);
    if (scopeRes.ok) setScopeOptions(await scopeRes.json());
    if (declaringRes.ok) setDeclaringTypeOptions(await declaringRes.json());
    if (relationRes.ok) setRelationTypeOptions(await relationRes.json());
    if (termsRes.ok) setTerms(await termsRes.json());
  }, []);

  const loadLabs = useCallback(async () => {
    const res = await fetch(`${API_BASE}/api/labs`, { headers: authHeaders() });
    if (!res.ok) throw new Error(await readFriendlyApiError(res, 'read'));
    return res.json();
  }, []);

  const loadStructure = useCallback(async (labId) => {
    const res = await fetch(`${API_BASE}/api/lecturer/labs/${labId}/structure`, { headers: authHeaders() });
    if (!res.ok) throw new Error(await readFriendlyApiError(res, 'read'));
    return res.json();
  }, []);

  const applyStructure = useCallback((labId, structure) => {
    const nextDraft = cloneDraft(structure);
    nextDraft.challenges = (nextDraft.challenges || []).map((challenge) => ({
      ...challenge,
      relations: challenge.relations || [],
      hasMmd: challenge.hasMmd !== false,
      weight: challenge.weight > 0 ? challenge.weight : 1,
      classWeight: challenge.classWeight > 0 ? challenge.classWeight : 1,
      mmdWeight: challenge.mmdWeight > 0 ? challenge.mmdWeight : 1,
      testcaseWeight: challenge.testcaseWeight > 0 ? challenge.testcaseWeight : 1,
      classes: (challenge.classes || []).map((cls) => ({
        ...cls,
        weight: cls.weight > 0 ? cls.weight : 1,
      })),
    }));
    const snapshot = cloneDraft(structure);
    structureCacheRef.current[labId] = { draft: cloneDraft(nextDraft), snapshot };
    setSelectedLabId(labId);
    setDraft(nextDraft);
    setSavedSnapshot(snapshot);
    setExpandedLabs((prev) => ({ ...prev, [labId]: true }));
  }, []);

  const selectLab = useCallback(async (labId, force = false) => {
    if (!force && isDirtyRef.current) {
      const proceed = window.confirm('You have unsaved changes. Discard them and switch labs?');
      if (!proceed) return;
    }
    setError('');
    const cached = structureCacheRef.current[labId];
    if (cached) {
      setSelectedLabId(labId);
      setDraft(cloneDraft(cached.draft));
      setSavedSnapshot(cloneDraft(cached.snapshot));
      setExpandedLabs((prev) => ({ ...prev, [labId]: true }));
      setSelectedClassRef(null);
      setSelectedChallengeId(null);
      return;
    }
    setStructureLoading(true);
    try {
      const structure = await loadStructure(labId);
      applyStructure(labId, structure);
      setSelectedClassRef(null);
      setSelectedChallengeId(null);
    } catch (e) {
      setError(toFriendlyError(e, 'read'));
    } finally {
      setStructureLoading(false);
    }
  }, [loadStructure, applyStructure]);

  useEffect(() => {
    let active = true;
    (async () => {
      try {
        setLoading(true);
        await loadLookups();
        const labList = await loadLabs();
        if (!active) return;
        setLabs(labList);
        if (labList.length > 0) {
          await selectLab(labList[0].id, true);
        }
      } catch (e) {
        if (active) setError(toFriendlyError(e, 'read'));
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => {
      active = false;
    };
  }, [loadLookups, loadLabs, selectLab]);

  const selectedClass = useMemo(() => {
    if (!draft || !selectedClassRef) return null;
    const challenge = draft.challenges.find((c) => c.id === selectedClassRef.challengeId);
    return challenge?.classes?.find((cls) => cls.id === selectedClassRef.classId) || null;
  }, [draft, selectedClassRef]);

  const selectedChallengeClasses = useMemo(() => {
    if (!draft || !selectedClassRef) return [];
    const challenge = draft.challenges.find((c) => c.id === selectedClassRef.challengeId);
    return challenge?.classes || [];
  }, [draft, selectedClassRef]);

  const selectedChallenge = useMemo(() => {
    if (!draft || !selectedChallengeId || selectedClassRef) return null;
    return draft.challenges.find((c) => c.id === selectedChallengeId) || null;
  }, [draft, selectedChallengeId, selectedClassRef]);

  const updateSelectedClass = (updatedClass) => {
    if (!draft || !selectedClassRef) return;
    const challenges = draft.challenges.map((challenge) => {
      if (challenge.id !== selectedClassRef.challengeId) return challenge;
      return {
        ...challenge,
        classes: challenge.classes.map((cls) => (cls.id === updatedClass.id ? updatedClass : cls)),
      };
    });
    setDraft({ ...draft, challenges });
  };

  const updateSelectedChallenge = (updatedChallenge) => {
    if (!draft || !selectedChallengeId) return;
    const challenges = draft.challenges.map((challenge) => (
      challenge.id === updatedChallenge.id ? updatedChallenge : challenge
    ));
    setDraft({ ...draft, challenges });
  };

  const handleSave = async () => {
    if (!draft || !selectedLabId || savingRef.current) return;
    savingRef.current = true;
    setSaving(true);
    setToast(null);
    try {
      const res = await fetch(`${API_BASE}/api/lecturer/labs/${selectedLabId}/structure`, {
        method: 'PUT',
        headers: authHeaders({ 'Content-Type': 'application/json' }),
        body: JSON.stringify(draft),
      });
      if (!res.ok) {
        throw new Error(await readFriendlyApiError(res, 'save'));
      }
      const saved = await res.json();
      applyStructure(selectedLabId, saved);
      const savedChallenges = saved.challenges || [];
      if (selectedClassRef) {
        const challenge = savedChallenges.find((c) => c.id === selectedClassRef.challengeId);
        const cls = challenge?.classes?.find((c) => c.id === selectedClassRef.classId);
        if (!cls) setSelectedClassRef(null);
      }
      if (selectedChallengeId) {
        const challenge = savedChallenges.find((c) => c.id === selectedChallengeId);
        if (!challenge) setSelectedChallengeId(null);
      }
      setLabs((prev) => prev.map((lab) => (
        lab.id === saved.id ? { ...lab, name: saved.name ?? lab.name } : lab
      )));
      setToast({ message: 'Lab structure saved.', type: 'success' });
    } catch (e) {
      setToast({ message: toFriendlyError(e, 'save'), type: 'error' });
    } finally {
      savingRef.current = false;
      setSaving(false);
    }
  };

  const handleCreateLab = async () => {
    if (!newLabName.trim() || !newLabTermId) return;
    const body = { name: newLabName.trim(), termId: newLabTermId };
    if (newLabDeadline) body.deadlineDate = newLabDeadline;
    const res = await fetch(`${API_BASE}/api/lecturer/labs`, {
      method: 'POST',
      headers: authHeaders({ 'Content-Type': 'application/json' }),
      body: JSON.stringify(body),
    });
    if (!res.ok) throw new Error(await readFriendlyApiError(res, 'save'));
    const created = await res.json();
    setLabs((prev) => [...prev, { id: created.id, name: created.name }]);
    setShowCreateLab(false);
    setNewLabName('');
    setNewLabTermId('');
    setNewLabDeadline('');
    await selectLab(created.id, true);
  };

  const applyDeadlineToSelectedLab = (labId, deadlineDate) => {
    const normalized = toDateInputValue(deadlineDate) || null;
    setDraft((prev) => (prev ? { ...prev, deadlineDate: normalized } : prev));
    setSavedSnapshot((prev) => (prev ? { ...prev, deadlineDate: normalized } : prev));
    setLabs((prev) => prev.map((lab) => (
      lab.id === labId ? { ...lab, deadlineDate: normalized } : lab
    )));
    const cached = structureCacheRef.current[labId];
    if (cached) {
      cached.draft = { ...cached.draft, deadlineDate: normalized };
      cached.snapshot = { ...cached.snapshot, deadlineDate: normalized };
    }
  };

  const handleDeadlineChange = async (deadlineDate) => {
    if (!selectedLabId || deadlineSaving) return;
    const nextDeadline = toDateInputValue(deadlineDate) || null;
    if (nextDeadline && !isValidCalendarDate(nextDeadline)) {
      setToast({
        message: 'That day does not exist. Pick a valid date from the calendar, then save.',
        type: 'error',
      });
      return;
    }
    if (deadlineDate !== null && !nextDeadline) {
      setToast({
        message: 'Pick a valid date from the calendar, then click Save deadline.',
        type: 'error',
      });
      return;
    }
    const previousDeadline = toDateInputValue(savedSnapshot?.deadlineDate) || null;
    setDeadlineSaving(true);
    applyDeadlineToSelectedLab(selectedLabId, nextDeadline);
    setDeadlineInput(nextDeadline ?? '');
    try {
      const res = await fetch(`${API_BASE}/api/lecturer/labs/${selectedLabId}/deadline`, {
        method: 'PATCH',
        headers: authHeaders({ 'Content-Type': 'application/json' }),
        body: JSON.stringify({ deadlineDate: nextDeadline }),
      });
      if (!res.ok) throw new Error(await readFriendlyApiError(res, 'save'));
      const updated = await res.json();
      const savedDeadline = updated.deadlineDate ?? null;
      applyDeadlineToSelectedLab(selectedLabId, savedDeadline);
      setDeadlineInput(savedDeadline ?? '');
      setToast({
        message: savedDeadline
          ? `${draft?.name || 'Lab'} deadline saved.`
          : `${draft?.name || 'Lab'} deadline cleared.`,
        type: 'success',
      });
    } catch (e) {
      applyDeadlineToSelectedLab(selectedLabId, previousDeadline);
      setDeadlineInput(previousDeadline ?? '');
      setToast({ message: toFriendlyError(e, 'save'), type: 'error' });
    } finally {
      setDeadlineSaving(false);
    }
  };

  const runDelete = async () => {
    if (!confirmDelete) return;
    const { type, labId, challengeId, classId } = confirmDelete;
    if (type === 'lab') {
      const res = await fetch(`${API_BASE}/api/lecturer/labs/${labId}`, { method: 'DELETE', headers: authHeaders() });
      if (!res.ok) throw new Error(await readFriendlyApiError(res, 'delete'));
      const remaining = labs.filter((l) => l.id !== labId);
      setLabs(remaining);
      if (selectedLabId === labId) {
        setDraft(null);
        setSavedSnapshot(null);
        setSelectedLabId(null);
        if (remaining[0]) await selectLab(remaining[0].id, true);
      }
    } else if (type === 'challenge') {
      setDraft({
        ...draft,
        challenges: draft.challenges.filter((c) => c.id !== challengeId),
      });
      if (selectedClassRef?.challengeId === challengeId) setSelectedClassRef(null);
      if (selectedChallengeId === challengeId) setSelectedChallengeId(null);
    } else if (type === 'class') {
      const challenge = draft.challenges.find((c) => c.id === challengeId);
      const nestedIds = collectNestedDependents(challenge?.classes || [], classId);
      nestedIds.add(classId);
      setDraft({
        ...draft,
        challenges: draft.challenges.map((c) => (
          c.id === challengeId
            ? {
                ...c,
                classes: c.classes.filter((cls) => !nestedIds.has(cls.id)),
                relations: (c.relations || []).filter(
                  (rel) => !nestedIds.has(rel.sourceClassId) && !nestedIds.has(rel.targetClassId),
                ),
              }
            : c
        )),
      });
      if (selectedClassRef?.classId && nestedIds.has(selectedClassRef.classId)) setSelectedClassRef(null);
    }
    setConfirmDelete(null);
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20 text-foreground-secondary">
        <Loader2 className="mr-2 h-5 w-5 animate-spin" /> Loading structure editor...
      </div>
    );
  }

  return (
    <div className="flex min-h-[calc(100dvh-16rem)] flex-col">
      <div className="space-y-4">
        <div>
          <h2 className="text-xl font-semibold text-foreground">Solution Management</h2>
          <p className="text-sm text-foreground-secondary">Define lab rubric structure for grading.</p>
        </div>

        {draft && (
          <div className="flex flex-wrap items-end gap-3 rounded-lg border border-border bg-surface-secondary px-4 py-3">
            <div>
              <label className="mb-1 block text-xs font-semibold uppercase tracking-wider text-foreground-muted">
                Deadline for {draft.name}
              </label>
              <DatePicker
                value={deadlineInput}
                disabled={deadlineSaving}
                placeholder="Select Date..."
                onChange={setDeadlineInput}
              />
            </div>
            <button
              type="button"
              className="rounded-lg bg-primary px-3 py-2 text-sm font-semibold text-white hover:bg-primary-hover disabled:cursor-not-allowed disabled:opacity-50"
              disabled={deadlineSaving}
              onClick={() => handleDeadlineChange(deadlineInput)}
            >
              {deadlineSaving ? 'Saving…' : 'Save deadline'}
            </button>
            <button
              type="button"
              className="rounded-lg border border-border px-3 py-2 text-sm text-foreground-secondary hover:bg-surface disabled:cursor-not-allowed disabled:opacity-50"
              disabled={deadlineSaving}
              onClick={() => handleDeadlineChange(null)}
            >
              Clear deadline
            </button>
            <p className="text-xs text-foreground-muted pb-2">
              Use the calendar to pick a real date, then Save. End of day 23:59 Vietnam time.
            </p>
          </div>
        )}

        {error && <div className="rounded-lg border border-error bg-error-bg px-4 py-3 text-sm text-error-text">{error}</div>}

        <div className="relative flex flex-col gap-4 lg:flex-row">
        {structureLoading && (
          <div className="absolute inset-0 z-10 flex items-center justify-center rounded-xl bg-black/20">
            <Loader2 className="h-6 w-6 animate-spin text-primary" />
          </div>
        )}
        <StructureSidebar
          labs={labs}
          draft={draft}
          selectedLabId={selectedLabId}
          expandedLabs={expandedLabs}
          expandedChallenges={expandedChallenges}
          selectedChallengeId={selectedChallengeId}
          selectedClassId={selectedClassRef?.classId}
          onSelectLab={(labId) => selectLab(labId)}
          onToggleLab={(labId) => setExpandedLabs((prev) => ({ ...prev, [labId]: !prev[labId] }))}
          onToggleChallenge={(challengeId) => setExpandedChallenges((prev) => ({ ...prev, [challengeId]: !prev[challengeId] }))}
          onSelectChallenge={(challengeId) => {
            setSelectedChallengeId(challengeId);
            setSelectedClassRef(null);
            setExpandedChallenges((prev) => ({ ...prev, [challengeId]: true }));
          }}
          onSelectClass={(challengeId, classId) => {
            setSelectedClassRef({ challengeId, classId });
            setSelectedChallengeId(challengeId);
          }}
          onRenameChallenge={(challengeId, name) => {
            if (!draft) return;
            setDraft({
              ...draft,
              challenges: draft.challenges.map((c) => (
                c.id === challengeId ? { ...c, name } : c
              )),
            });
          }}
          onAddLab={() => setShowCreateLab(true)}
          onAddChallenge={() => {
            if (!draft) return;
            const nextNumber = (draft.challenges || []).reduce(
              (max, c) => Math.max(max, c.challengeNumber ?? 0),
              0,
            ) + 1;
            const challenge = {
              id: crypto.randomUUID(),
              name: `Problem ${nextNumber}`,
              challengeNumber: nextNumber,
              classes: [],
              relations: [],
              hasMmd: true,
              weight: 1,
              classWeight: 1,
              mmdWeight: 1,
              testcaseWeight: 1,
            };
            setDraft({ ...draft, challenges: [...(draft.challenges || []), challenge] });
            setExpandedChallenges((prev) => ({ ...prev, [challenge.id]: true }));
          }}
          onAddClass={(challengeId) => {
            const cls = {
              id: crypto.randomUUID(),
              name: 'NewClass',
              scopeId: scopeOptions[0]?.id,
              declaringTypeId: declaringTypeOptions[0]?.id,
              isAbstract: false,
              isStatic: false,
              fields: [],
              methods: [],
              constructors: [],
              weight: 1,
            };
            setDraft({
              ...draft,
              challenges: draft.challenges.map((c) => (
                c.id === challengeId ? { ...c, classes: [...(c.classes || []), cls] } : c
              )),
            });
            setSelectedClassRef({ challengeId, classId: cls.id });
            setExpandedChallenges((prev) => ({ ...prev, [challengeId]: true }));
          }}
          onDeleteLab={(labId) => setConfirmDelete({ type: 'lab', labId })}
          onDeleteChallenge={(challengeId) => setConfirmDelete({ type: 'challenge', challengeId })}
          formatClassLabel={formatQualifiedClassName}
          onDeleteClass={(challengeId, classId) => {
            const challenge = draft?.challenges?.find((c) => c.id === challengeId);
            const nestedIds = collectNestedDependents(challenge?.classes || [], classId);
            const nestedNames = (challenge?.classes || [])
              .filter((cls) => nestedIds.has(cls.id))
              .map((cls) => formatQualifiedClassName(cls, challenge?.classes));
            setConfirmDelete({ type: 'class', challengeId, classId, nestedNames });
          }}
        />

        <div className="min-w-0 flex-1">
          {selectedClass ? (
            <ClassDetailPanel
              classData={selectedClass}
              challengeClasses={selectedChallengeClasses}
              scopeOptions={scopeOptions}
              declaringTypeOptions={declaringTypeOptions}
              onChange={updateSelectedClass}
            />
          ) : (
            <ChallengeDetailPanel
              challenge={selectedChallenge}
              relationTypeOptions={relationTypeOptions}
              onMmdChange={updateSelectedChallenge}
              activeTab={challengeTabById[selectedChallengeId] || 'mmd'}
              onTabChange={(tab) => {
                if (selectedChallengeId) {
                  setChallengeTabById((prev) => ({ ...prev, [selectedChallengeId]: tab }));
                }
              }}
              labId={selectedLabId}
              structureDirty={isDirty}
              onToast={setToast}
            />
          )}
        </div>
      </div>
      </div>

      <div className="mt-6 flex flex-1 items-center justify-end py-8">
        <button
          type="button"
          disabled={!isDirty || saving || !draft}
          onClick={handleSave}
          className="inline-flex items-center gap-2 rounded-full bg-primary px-6 py-3 text-sm font-semibold text-white shadow-lg hover:bg-primary-hover disabled:cursor-not-allowed disabled:opacity-50"
        >
          {saving ? <Loader2 className="h-4 w-4 animate-spin" /> : <Check className="h-4 w-4" />}
          Save Lab Structure
        </button>
      </div>

      {showCreateLab && (
        <Modal onClose={() => setShowCreateLab(false)}>
          <h3 className="mb-4 text-lg font-semibold text-foreground">Create Lab</h3>
          <div className="space-y-4">
            <div>
              <label className="mb-1 block text-xs text-foreground-muted">Lab name</label>
              <input
                className="w-full rounded-lg border border-border bg-surface-secondary px-3 py-2 text-sm dark:text-white"
                value={newLabName}
                onChange={(e) => setNewLabName(e.target.value)}
              />
            </div>
            <div>
              <label className="mb-1 block text-xs text-foreground-muted">Term</label>
              <select
                className="w-full rounded-lg border border-border bg-surface-secondary px-3 py-2 text-sm dark:text-white"
                value={newLabTermId}
                onChange={(e) => {
                  const termId = e.target.value;
                  setNewLabTermId(termId);
                  const term = terms.find((t) => String(t.id) === String(termId));
                  setNewLabDeadline(term?.endDate ?? '');
                }}
              >
                <option value="">Select term</option>
                {terms.map((term) => (
                  <option key={term.id} value={term.id}>{term.label}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="mb-1 block text-xs text-foreground-muted">Deadline (optional)</label>
              <DatePicker
                className="w-full bg-surface-secondary"
                value={newLabDeadline}
                placeholder="Select Date..."
                onChange={setNewLabDeadline}
              />
            </div>
            <div className="flex gap-2">
              <button type="button" onClick={handleCreateLab} className="rounded-lg bg-primary px-4 py-2 text-sm text-white">Create</button>
              <button type="button" onClick={() => setShowCreateLab(false)} className="rounded-lg border border-border px-4 py-2 text-sm">Cancel</button>
            </div>
          </div>
        </Modal>
      )}

      {toast && (
        <Toast
          message={toast.message}
          type={toast.type}
          onDismiss={() => setToast(null)}
        />
      )}

      {confirmDelete && (
        <Modal onClose={() => setConfirmDelete(null)}>
          <h3 className="mb-3 text-lg font-semibold text-foreground">Confirm delete</h3>
          <p className="mb-4 text-sm text-foreground-secondary">
            This will remove the selected item from the lab structure
            {confirmDelete.type === 'lab' ? ' and delete all problems, classes, and related grading references after save.' : '.'}
            {confirmDelete.type === 'class' && confirmDelete.nestedNames?.length > 0 && (
              <>
                {' '}Nested classes that will also be removed:
                {' '}
                <span className="font-medium text-foreground">{confirmDelete.nestedNames.join(', ')}</span>.
              </>
            )}
            {' '}Student submission data may be orphaned. Continue?
          </p>
          <div className="flex gap-2">
            <button type="button" onClick={runDelete} className="rounded-lg bg-error px-4 py-2 text-sm text-white hover:bg-error-hover">Delete</button>
            <button type="button" onClick={() => setConfirmDelete(null)} className="rounded-lg border border-border px-4 py-2 text-sm">Cancel</button>
          </div>
        </Modal>
      )}
    </div>
  );
}
