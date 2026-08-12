import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Check, Loader2 } from 'lucide-react';
import Modal from '../components/ui/Modal';
import Toast from '../components/ui/Toast';
import ClassDetailPanel from '../components/lecturer/structure/ClassDetailPanel';
import MmdRelationsPanel from '../components/lecturer/structure/MmdRelationsPanel';
import StructureSidebar from '../components/lecturer/structure/StructureSidebar';
import { authHeaders } from '../utils/authHeaders';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';

const emptyDraft = (lab) => ({
  id: lab.id,
  name: lab.name,
  termId: lab.termId || null,
  challenges: [],
});

function cloneDraft(data) {
  return JSON.parse(JSON.stringify(data));
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
  const [confirmDelete, setConfirmDelete] = useState(null);

  const isDirty = useMemo(() => {
    if (!draft || !savedSnapshot) return false;
    return JSON.stringify(draft) !== JSON.stringify(savedSnapshot);
  }, [draft, savedSnapshot]);

  const isDirtyRef = useRef(isDirty);
  const structureCacheRef = useRef({});
  useEffect(() => {
    isDirtyRef.current = isDirty;
  }, [isDirty]);

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
    if (!res.ok) throw new Error('Failed to load labs');
    return res.json();
  }, []);

  const loadStructure = useCallback(async (labId) => {
    const res = await fetch(`${API_BASE}/api/lecturer/labs/${labId}/structure`, { headers: authHeaders() });
    if (!res.ok) throw new Error('Failed to load lab structure');
    return res.json();
  }, []);

  const applyStructure = useCallback((labId, structure) => {
    const nextDraft = cloneDraft(structure);
    nextDraft.challenges = (nextDraft.challenges || []).map((challenge) => ({
      ...challenge,
      relations: challenge.relations || [],
      hasMmd: challenge.hasMmd !== false,
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
      setError(e.message || 'Failed to load lab structure');
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
        if (active) setError(e.message || 'Failed to initialize editor');
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
    if (!draft || !selectedLabId) return;
    setSaving(true);
    setToast(null);
    try {
      const res = await fetch(`${API_BASE}/api/lecturer/labs/${selectedLabId}/structure`, {
        method: 'PUT',
        headers: authHeaders({ 'Content-Type': 'application/json' }),
        body: JSON.stringify(draft),
      });
      if (!res.ok) {
        const body = await res.json().catch(() => ({}));
        throw new Error(body.message || 'Save failed');
      }
      const saved = await res.json();
      const nextDraft = cloneDraft(saved);
      nextDraft.challenges = (nextDraft.challenges || []).map((challenge) => ({
        ...challenge,
        relations: challenge.relations || [],
        hasMmd: challenge.hasMmd !== false,
      }));
      const snapshot = cloneDraft(saved);
      structureCacheRef.current[selectedLabId] = { draft: cloneDraft(nextDraft), snapshot };
      setDraft(nextDraft);
      setSavedSnapshot(snapshot);
      if (selectedClassRef) {
        const challenge = nextDraft.challenges.find((c) => c.id === selectedClassRef.challengeId);
        const cls = challenge?.classes?.find((c) => c.id === selectedClassRef.classId);
        if (!cls) setSelectedClassRef(null);
      }
      if (selectedChallengeId) {
        const challenge = nextDraft.challenges.find((c) => c.id === selectedChallengeId);
        if (!challenge) setSelectedChallengeId(null);
      }
      setLabs((prev) => prev.map((lab) => (lab.id === saved.id ? { ...lab, name: saved.name } : lab)));
      setToast({ message: 'Lab structure saved.', type: 'success' });
    } catch (e) {
      setToast({ message: e.message || 'Save failed', type: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const handleCreateLab = async () => {
    if (!newLabName.trim() || !newLabTermId) return;
    const res = await fetch(`${API_BASE}/api/lecturer/labs`, {
      method: 'POST',
      headers: authHeaders({ 'Content-Type': 'application/json' }),
      body: JSON.stringify({ name: newLabName.trim(), termId: newLabTermId }),
    });
    if (!res.ok) throw new Error('Failed to create lab');
    const created = await res.json();
    setLabs((prev) => [...prev, { id: created.id, name: created.name }]);
    setShowCreateLab(false);
    setNewLabName('');
    setNewLabTermId('');
    await selectLab(created.id, true);
  };

  const runDelete = async () => {
    if (!confirmDelete) return;
    const { type, labId, challengeId, classId } = confirmDelete;
    if (type === 'lab') {
      const res = await fetch(`${API_BASE}/api/lecturer/labs/${labId}`, { method: 'DELETE', headers: authHeaders() });
      if (!res.ok) throw new Error('Failed to delete lab');
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
      setDraft({
        ...draft,
        challenges: draft.challenges.map((c) => (
          c.id === challengeId
            ? {
                ...c,
                classes: c.classes.filter((cls) => cls.id !== classId),
                relations: (c.relations || []).filter(
                  (rel) => rel.sourceClassId !== classId && rel.targetClassId !== classId,
                ),
              }
            : c
        )),
      });
      if (selectedClassRef?.classId === classId) setSelectedClassRef(null);
    }
    setConfirmDelete(null);
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20 text-gray-500">
        <Loader2 className="mr-2 h-5 w-5 animate-spin" /> Loading structure editor...
      </div>
    );
  }

  return (
    <div className="flex min-h-[calc(100dvh-16rem)] flex-col">
      <div className="space-y-4">
        <div>
          <h2 className="text-xl font-semibold text-gray-900 dark:text-white">Solution Management</h2>
          <p className="text-sm text-gray-500 dark:text-gray-400">Define lab rubric structure for grading.</p>
        </div>

        {error && <div className="rounded-lg border border-red-300 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-800 dark:bg-red-950 dark:text-red-200">{error}</div>}

        <div className="relative flex flex-col gap-4 lg:flex-row">
        {structureLoading && (
          <div className="absolute inset-0 z-10 flex items-center justify-center rounded-xl bg-black/20">
            <Loader2 className="h-6 w-6 animate-spin text-purple-400" />
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
              fields: [],
              methods: [],
              constructors: [],
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
          onDeleteClass={(challengeId, classId) => setConfirmDelete({ type: 'class', challengeId, classId })}
        />

        <div className="min-w-0 flex-1">
          {selectedClass ? (
            <ClassDetailPanel
              classData={selectedClass}
              scopeOptions={scopeOptions}
              declaringTypeOptions={declaringTypeOptions}
              onChange={updateSelectedClass}
            />
          ) : (
            <MmdRelationsPanel
              challenge={selectedChallenge}
              relationTypeOptions={relationTypeOptions}
              onChange={updateSelectedChallenge}
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
          className="inline-flex items-center gap-2 rounded-full bg-purple-600 px-6 py-3 text-sm font-semibold text-white shadow-lg hover:bg-purple-500 disabled:cursor-not-allowed disabled:opacity-50"
        >
          {saving ? <Loader2 className="h-4 w-4 animate-spin" /> : <Check className="h-4 w-4" />}
          Save Lab Structure
        </button>
      </div>

      {showCreateLab && (
        <Modal onClose={() => setShowCreateLab(false)}>
          <h3 className="mb-4 text-lg font-semibold text-gray-900 dark:text-white">Create Lab</h3>
          <div className="space-y-4">
            <div>
              <label className="mb-1 block text-xs text-gray-500">Lab name</label>
              <input
                className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm dark:border-gray-700 dark:bg-[#0d1117] dark:text-white"
                value={newLabName}
                onChange={(e) => setNewLabName(e.target.value)}
              />
            </div>
            <div>
              <label className="mb-1 block text-xs text-gray-500">Term</label>
              <select
                className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm dark:border-gray-700 dark:bg-[#0d1117] dark:text-white"
                value={newLabTermId}
                onChange={(e) => setNewLabTermId(e.target.value)}
              >
                <option value="">Select term</option>
                {terms.map((term) => (
                  <option key={term.id} value={term.id}>{term.label}</option>
                ))}
              </select>
            </div>
            <div className="flex gap-2">
              <button type="button" onClick={handleCreateLab} className="rounded-lg bg-purple-600 px-4 py-2 text-sm text-white">Create</button>
              <button type="button" onClick={() => setShowCreateLab(false)} className="rounded-lg border border-gray-600 px-4 py-2 text-sm">Cancel</button>
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
          <h3 className="mb-3 text-lg font-semibold text-gray-900 dark:text-white">Confirm delete</h3>
          <p className="mb-4 text-sm text-gray-600 dark:text-gray-400">
            This will remove the selected item from the lab structure
            {confirmDelete.type === 'lab' ? ' and delete all problems, classes, and related grading references after save.' : '.'}
            {' '}Student submission data may be orphaned. Continue?
          </p>
          <div className="flex gap-2">
            <button type="button" onClick={runDelete} className="rounded-lg bg-red-600 px-4 py-2 text-sm text-white">Delete</button>
            <button type="button" onClick={() => setConfirmDelete(null)} className="rounded-lg border border-gray-600 px-4 py-2 text-sm">Cancel</button>
          </div>
        </Modal>
      )}
    </div>
  );
}
