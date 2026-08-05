import React, { useState } from 'react';
import { Plus, Trash2 } from 'lucide-react';
import DropZone from '../components/ui/DropZone';
import Modal from '../components/ui/Modal';

const INITIAL_SOLUTIONS = [
  { lab: 'Lab 01: Abstraction', uploaded: '2026-07-10', files: 4, status: 'Ready', description: 'Abstraction exercises for OOP basics' },
  { lab: 'Lab 02: Polymorphism', uploaded: '2026-07-12', files: 3, status: 'Ready', description: 'Polymorphism examples and tests' },
  { lab: 'Lab 03: Inheritance', uploaded: '-', files: 0, status: 'Missing', description: '' },
  { lab: 'Lab 04: Interface', uploaded: '2026-07-15', files: 5, status: 'Ready', description: 'Interface-based design' },
];

// Reusable Modal is provided by components/ui/Modal

export default function SubmissionManagement() {
  const [solutions, setSolutions] = useState(INITIAL_SOLUTIONS);
  const [showAdd, setShowAdd] = useState(false);
  const [replaceFor, setReplaceFor] = useState(null);
  const [addStep, setAddStep] = useState('form');
  const [addLab, setAddLab] = useState('');
  const [addDesc, setAddDesc] = useState('');
  const [addTestLab, setAddTestLab] = useState('');
  const [addTestChallenge, setAddTestChallenge] = useState('');
  const [newChallengeName, setNewChallengeName] = useState('');
  const [challengesByLab, setChallengesByLab] = useState({});
  const [showAddTestcase, setShowAddTestcase] = useState(false);
  const [testcaseInput, setTestcaseInput] = useState('');
  const [testcaseOutput, setTestcaseOutput] = useState('');
  const [testcases, setTestcases] = useState([]);
  const [editingLab, setEditingLab] = useState(null);
  const [editingName, setEditingName] = useState('');
  const [confirmDelete, setConfirmDelete] = useState(null);

  const handleAddFiles = (files) => {
    // Use addLab/addDesc from form step to create or update solution.
    const now = new Date().toISOString().slice(0, 10);
    const filesCount = files.length;
    if (!addLab) {
      // fallback: use Imported date
      const updated = [{ lab: `Imported ${now}`, uploaded: now, files: filesCount, status: 'Ready', description: addDesc }, ...solutions];
      setSolutions(updated);
      setShowAdd(false);
      setAddStep('form');
      setAddLab('');
      setAddDesc('');
      return;
    }

    const existingIndex = solutions.findIndex((s) => s.lab === addLab);
    const newEntry = { lab: addLab, uploaded: now, files: filesCount, status: 'Ready', description: addDesc };
    if (existingIndex >= 0) {
      const updated = [...solutions];
      updated[existingIndex] = { ...updated[existingIndex], ...newEntry };
      setSolutions(updated);
    } else {
      setSolutions([newEntry, ...solutions]);
    }

    setShowAdd(false);
    setAddStep('form');
    setAddLab('');
    setAddDesc('');
  };

  const handleReplaceFiles = (files) => {
    if (!replaceFor) return;
    const now = new Date().toISOString().slice(0, 10);
    const updated = solutions.map((s) => (s.lab === replaceFor ? { ...s, uploaded: now, files: files.length, status: 'Ready' } : s));
    setSolutions(updated);
    setReplaceFor(null);
  };

  const handleDelete = (lab) => {
    setSolutions((prev) => prev.filter((s) => s.lab !== lab));
  };

  return (
    <div className="space-y-6 px-4 sm:px-6 lg:px-8 max-w-full overflow-x-hidden">
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div>
          <h2 className="text-xl font-semibold">Solution Management</h2>
          <p className="text-sm text-gray-500">Upload grading solutions per lab</p>
        </div>
        <div className="inline-flex items-center gap-2">
          <button onClick={() => setShowAdd(true)} className="inline-flex items-center gap-2 rounded-full bg-purple-600 px-4 py-2 text-white hover:bg-purple-500">
            <Plus className="h-4 w-4" />
            Add Solution
          </button>
          <button onClick={() => setShowAddTestcase(true)} className="inline-flex items-center gap-2 rounded-full bg-blue-600 px-4 py-2 text-white hover:bg-blue-500">
            <Plus className="h-4 w-4" />
            Add Testcase
          </button>
        </div>
      </div>

        <div className="overflow-x-auto rounded-2xl border border-gray-200 dark:border-gray-700">
          <table className="w-full table-auto text-sm min-w-full bg-white dark:bg-transparent">
            <thead className="bg-white dark:bg-[#0f1720] text-gray-600 dark:text-gray-400">
              <tr>
                  <th className="w-1/4 px-4 py-4 text-left">Lab</th>
                  <th className="w-1/6 px-4 py-4 text-left">Description</th>
                <th className="w-1/8 px-4 py-4 text-left">Uploaded</th>
                <th className="w-1/8 px-4 py-4 text-left">Files</th>
                <th className="w-1/8 px-4 py-4 text-left">Status</th>
                <th className="w-32 px-6 py-4"></th>
              </tr>
            </thead>
            <tbody>
              {solutions.map((s) => (
                <tr key={s.lab} className="border-t border-gray-200 dark:border-gray-800 hover:bg-gray-50 dark:hover:bg-[#0b0f14]">
                  <td className="px-6 py-4 align-middle break-words">
                    <div className="flex items-center gap-3">
                      {editingLab === s.lab ? (
                        <input
                          className="w-52 rounded-md border border-gray-600 bg-[#0d1117] px-3 py-1 text-sm text-white"
                          value={editingName}
                          onChange={(e) => setEditingName(e.target.value)}
                        />
                      ) : (
                        <div className="max-w-[18rem] truncate text-gray-900 dark:text-gray-100 font-medium">{s.lab}</div>
                      )}
                      {editingLab === s.lab ? (
                        <div className="flex gap-2">
                          <button
                            onClick={() => {
                              if (editingName.trim()) {
                                setSolutions((prev) => prev.map((x) => (x.lab === s.lab ? { ...x, lab: editingName.trim() } : x)));
                              }
                              setEditingLab(null);
                              setEditingName('');
                            }}
                            className="rounded-md bg-green-600 px-3 py-1 text-xs text-white"
                          >Save</button>
                          <button onClick={() => { setEditingLab(null); setEditingName(''); }} className="rounded-md border border-gray-700 px-3 py-1 text-xs text-gray-200">Cancel</button>
                        </div>
                      ) : (
                        <button onClick={() => { setEditingLab(s.lab); setEditingName(s.lab); }} className="text-xs text-gray-400 hover:text-gray-200">Edit</button>
                      )}
                    </div>
                  </td>
                  <td className="px-4 py-4 text-gray-700 dark:text-gray-300 break-words">
                    <div className="max-w-full break-words text-sm" title={s.description || ''}>{s.description || '-'}</div>
                  </td>
                  <td className="px-4 py-4 text-gray-700 dark:text-gray-400">{s.uploaded}</td>
                  <td className="px-4 py-4 text-gray-700 dark:text-gray-400">{s.files} files</td>
                  <td className="px-6 py-4">
                    {s.status === 'Ready' ? (
                      <span className="inline-flex rounded-full px-3 py-1 text-xs font-semibold bg-emerald-100 text-emerald-800 dark:bg-emerald-800/40 dark:text-emerald-300">✓ Ready</span>
                    ) : (
                      <span className="inline-flex rounded-full px-3 py-1 text-xs font-semibold bg-red-100 text-red-800 dark:bg-red-800/40 dark:text-red-300">⚠ Missing</span>
                    )}
                  </td>
                  <td className="px-3 py-4 text-right">
                    <div className="inline-flex items-center gap-2">
                      <button onClick={() => setReplaceFor(s.lab)} className="inline-flex items-center gap-1 rounded-md bg-blue-600 px-2 py-1 text-white hover:bg-blue-500 text-sm">
                        Replace
                      </button>
                      <button onClick={() => setConfirmDelete(s.lab)} className="inline-flex items-center justify-center h-8 w-8 rounded-full border border-gray-300 text-gray-700 hover:bg-gray-100 dark:border-gray-700 dark:text-gray-300 dark:hover:bg-gray-800">
                        <Trash2 className="h-4 w-4" />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {confirmDelete && (
            <Modal onClose={() => setConfirmDelete(null)}>
              <div>
                <h3 className="mb-3 text-lg font-semibold text-gray-900 dark:text-white">Confirm delete</h3>
                <p className="text-sm text-gray-600 dark:text-gray-400 mb-4">Are you sure you want to delete <span className="font-semibold">{confirmDelete}</span>? This action cannot be undone.</p>
                <div className="flex gap-3">
                  <button onClick={() => { handleDelete(confirmDelete); setConfirmDelete(null); }} className="rounded-2xl bg-red-600 px-4 py-2 text-sm font-semibold text-white">Delete</button>
                  <button onClick={() => setConfirmDelete(null)} className="rounded-2xl border border-gray-700 px-4 py-2 text-sm text-gray-700 dark:text-gray-200">Cancel</button>
                </div>
              </div>
            </Modal>
        )}

      {showAdd && (
        <Modal onClose={() => { setShowAdd(false); setAddStep('form'); setAddLab(''); setAddDesc(''); }}>
          {addStep === 'form' ? (
            <div>
              <h3 className="mb-4 text-lg font-semibold text-gray-900 dark:text-white">Add Solution — Details</h3>
              <div className="space-y-4">
                <div>
                  <label className="block text-xs font-medium text-gray-400 mb-2">Select Lab</label>
                  <select value={addLab} onChange={(e) => setAddLab(e.target.value)} className="w-full rounded-2xl border border-gray-300 bg-white px-4 py-3 text-sm text-gray-900 outline-none dark:border-gray-700 dark:bg-[#0d1117] dark:text-white">
                    <option value="">-- Select a lab --</option>
                    {solutions.map((s) => <option key={s.lab} value={s.lab}>{s.lab}</option>)}
                    <option value="__NEW__">Create new lab...</option>
                  </select>
                  {addLab === '__NEW__' && (
                    <input
                      placeholder="Enter new lab name"
                      value={editingName || ''}
                      onChange={(e) => { setEditingName(e.target.value); }}
                      className="mt-3 w-full rounded-2xl border border-gray-300 bg-white px-4 py-2 text-sm text-gray-900 outline-none dark:border-gray-700 dark:bg-[#0d1117] dark:text-white"
                    />
                  )}
                </div>

                <div>
                  <label className="block text-xs font-medium text-gray-400 mb-2">Description</label>
                  <textarea value={addDesc} onChange={(e) => setAddDesc(e.target.value)} rows={3} className="w-full rounded-2xl border border-gray-300 bg-white px-4 py-3 text-sm text-gray-900 outline-none dark:border-gray-700 dark:bg-[#0d1117] dark:text-white" placeholder="Optional description for this solution" />
                </div>

                <div className="flex items-center gap-3">
                  <button
                    disabled={!addLab && !(addLab === '__NEW__' && editingName && editingName.trim())}
                    onClick={() => {
                      if (addLab === '__NEW__') {
                        setAddLab(editingName.trim());
                      }
                      setAddStep('upload');
                    }}
                    className="rounded-2xl bg-purple-600 px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
                  >Continue to Upload</button>
                  <button onClick={() => { setShowAdd(false); setAddLab(''); setAddDesc(''); }} className="rounded-2xl border border-gray-700 px-4 py-2 text-sm text-gray-200">Cancel</button>
                </div>
              </div>
            </div>
          ) : (
            <div>
              <h3 className="mb-4 text-lg font-semibold text-gray-900 dark:text-white">Upload files for {addLab}</h3>
              <p className="text-sm text-gray-400 mb-4">{addDesc}</p>
              <DropZone title={`Drop .zip or .rar files for ${addLab}`} buttonText="Select files" onFilesSelected={handleAddFiles} />
              <div className="mt-3 flex justify-between">
                <button onClick={() => setAddStep('form')} className="rounded-2xl border border-gray-700 px-4 py-2 text-sm text-gray-200">Back</button>
                <button onClick={() => { setShowAdd(false); setAddStep('form'); setAddLab(''); setAddDesc(''); }} className="rounded-2xl bg-gray-700 px-4 py-2 text-sm text-white">Close</button>
              </div>
            </div>
          )}
        </Modal>
      )}

      {showAddTestcase && (
        <Modal onClose={() => { setShowAddTestcase(false); setTestcaseInput(''); setTestcaseOutput(''); }}>
          <div>
            <h3 className="mb-4 text-lg font-semibold text-gray-900 dark:text-white">Add Testcase</h3>
            <div className="space-y-4">
              <div>
                <label className="block text-xs font-medium text-gray-500 dark:text-gray-400 mb-2">Lab</label>
                <select value={addTestLab} onChange={(e) => { setAddTestLab(e.target.value); setAddTestChallenge(''); }} className="w-full rounded-2xl border border-gray-300 bg-white px-4 py-3 text-sm text-gray-900 outline-none dark:border-gray-700 dark:bg-[#0d1117] dark:text-white">
                  <option value="">-- Select lab --</option>
                  {solutions.map((s) => <option key={s.lab} value={s.lab}>{s.lab}</option>)}
                </select>
              </div>
              <div>
                <label className="block text-xs font-medium text-gray-500 dark:text-gray-400 mb-2">Challenge</label>
                <div className="flex gap-2">
                  <select value={addTestChallenge} onChange={(e) => setAddTestChallenge(e.target.value)} className="flex-1 rounded-2xl border border-gray-300 bg-white px-4 py-3 text-sm text-gray-900 outline-none dark:border-gray-700 dark:bg-[#0d1117] dark:text-white">
                    <option value="">-- Select challenge --</option>
                    {(challengesByLab[addTestLab] || []).map((c) => <option key={c} value={c}>{c}</option>)}
                    <option value="__NEW__">Create new challenge...</option>
                  </select>
                  {addTestChallenge === '__NEW__' && (
                    <input value={newChallengeName} onChange={(e) => setNewChallengeName(e.target.value)} placeholder="New challenge name" className="rounded-2xl border border-gray-300 bg-white px-4 py-2 text-sm text-gray-900 outline-none dark:border-gray-700 dark:bg-[#0d1117] dark:text-white" />
                  )}
                </div>
              </div>
              <div>
                <label className="block text-xs font-medium text-gray-500 dark:text-gray-400 mb-2">Input</label>
                <textarea value={testcaseInput} onChange={(e) => setTestcaseInput(e.target.value)} rows={4} className="w-full rounded-2xl border border-gray-300 bg-white px-4 py-3 text-sm text-gray-900 outline-none dark:border-gray-700 dark:bg-[#0d1117] dark:text-white" />
              </div>
              <div>
                <label className="block text-xs font-medium text-gray-500 dark:text-gray-400 mb-2">Expected Output</label>
                <textarea value={testcaseOutput} onChange={(e) => setTestcaseOutput(e.target.value)} rows={4} className="w-full rounded-2xl border border-gray-300 bg-white px-4 py-3 text-sm text-gray-900 outline-none dark:border-gray-700 dark:bg-[#0d1117] dark:text-white" />
              </div>
              <div className="flex gap-2">
                <button onClick={() => {
                  // Determine final lab and challenge
                  const lab = addTestLab || (solutions[0] && solutions[0].lab) || '';
                  const challenge = addTestChallenge === '__NEW__' ? newChallengeName.trim() : addTestChallenge;
                  if (!lab || !challenge) {
                    // simple validation: require both
                    return alert('Please select a lab and challenge name for the testcase');
                  }
                  // persist challenge under lab
                  setChallengesByLab((prev) => {
                    const existing = prev[lab] || [];
                    const updated = existing.includes(challenge) ? existing : [challenge, ...existing];
                    return { ...prev, [lab]: updated };
                  });
                  setTestcases((prev) => [{ lab, challenge, input: testcaseInput, output: testcaseOutput }, ...prev]);
                  setShowAddTestcase(false);
                  setTestcaseInput('');
                  setTestcaseOutput('');
                  setAddTestLab('');
                  setAddTestChallenge('');
                  setNewChallengeName('');
                }} className="rounded-2xl bg-purple-600 px-4 py-2 text-sm font-semibold text-white">Save Testcase</button>
                <button onClick={() => { setShowAddTestcase(false); }} className="rounded-2xl border border-gray-700 px-4 py-2 text-sm text-gray-700 dark:text-gray-200">Cancel</button>
              </div>
            </div>
          </div>
        </Modal>
      )}

      {replaceFor && (
        <Modal onClose={() => setReplaceFor(null)}>
          <h3 className="mb-4 text-lg font-semibold text-white">Replace Solution for {replaceFor} — Drag & Drop or select files</h3>
          <DropZone title={`Drop files for ${replaceFor}`} buttonText="Select files" onFilesSelected={handleReplaceFiles} />
        </Modal>
      )}
    </div>
  );
}
