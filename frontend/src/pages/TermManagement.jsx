import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { CalendarDays, FileSpreadsheet, Plus, Star, Trash2, UserPlus, Ban, UserCheck } from 'lucide-react';
import { authHeaders } from '../utils/authHeaders';
import { readFriendlyApiError, toFriendlyError } from '../utils/apiError';
import { isSpreadsheetFile, parseStudentImportFile } from '../utils/studentImport';
import DatePicker from '../components/ui/DatePicker';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';

const EMPTY_FORM = {
  yearLabel: '',
  termNumber: '1',
  startDate: '',
  endDate: '',
  setCurrent: false,
};

export default function TermManagement() {
  const [terms, setTerms] = useState([]);
  const [selectedTermId, setSelectedTermId] = useState(null);
  const [students, setStudents] = useState([]);
  const [available, setAvailable] = useState([]);
  const [selectedStudentIds, setSelectedStudentIds] = useState([]);
  const [form, setForm] = useState(EMPTY_FORM);
  const [showCreate, setShowCreate] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [isDragging, setIsDragging] = useState(false);
  const fileInputRef = useRef(null);

  const selectedTerm = useMemo(
    () => terms.find((term) => String(term.id) === String(selectedTermId)) ?? null,
    [terms, selectedTermId],
  );

  const loadTerms = useCallback(async () => {
    const response = await fetch(`${API_BASE}/api/lecturer/terms`, { headers: authHeaders() });
    if (!response.ok) {
      throw new Error(await readFriendlyApiError(response, 'read'));
    }
    const data = await response.json();
    setTerms(Array.isArray(data) ? data : []);
    return data;
  }, []);

  const loadTermStudents = useCallback(async (termId) => {
    if (!termId) {
      setStudents([]);
      setAvailable([]);
      return;
    }
    const response = await fetch(`${API_BASE}/api/lecturer/terms/${termId}/roster`, { headers: authHeaders() });
    if (!response.ok) {
      throw new Error(await readFriendlyApiError(response, 'read'));
    }
    const data = await response.json();
    setStudents(Array.isArray(data?.enrolled) ? data.enrolled : []);
    setAvailable(Array.isArray(data?.available) ? data.available : []);
    setSelectedStudentIds([]);
  }, []);

  const refreshSelectedTerm = useCallback(async (termId) => {
    await Promise.all([loadTerms(), loadTermStudents(termId)]);
  }, [loadTerms, loadTermStudents]);

  useEffect(() => {
    let cancelled = false;
    async function load() {
      setLoading(true);
      setError('');
      try {
        const data = await loadTerms();
        if (cancelled) return;
        const current = data.find((term) => term.current) ?? data[0];
        setSelectedTermId(current?.id ?? null);
        if (current?.id) {
          await loadTermStudents(current.id);
        }
      } catch (err) {
        if (!cancelled) setError(toFriendlyError(err, 'read'));
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    load();
    return () => {
      cancelled = true;
    };
  }, [loadTerms, loadTermStudents]);

  const handleSelectTerm = async (termId) => {
    setSelectedTermId(termId);
    setError('');
    setNotice('');
    try {
      await loadTermStudents(termId);
    } catch (err) {
      setError(toFriendlyError(err, 'read'));
    }
  };

  const handleCreate = async () => {
    if (!form.yearLabel.trim()) {
      setError('Year is required');
      return;
    }
    setSaving(true);
    setError('');
    try {
      const response = await fetch(`${API_BASE}/api/lecturer/terms`, {
        method: 'POST',
        headers: authHeaders({ 'Content-Type': 'application/json' }),
        body: JSON.stringify({
          yearLabel: form.yearLabel.trim(),
          termNumber: Number(form.termNumber),
          startDate: form.startDate || null,
          endDate: form.endDate || null,
          setCurrent: form.setCurrent,
        }),
      });
      if (!response.ok) {
        throw new Error(await readFriendlyApiError(response, 'save'));
      }
      const created = await response.json();
      setShowCreate(false);
      setForm(EMPTY_FORM);
      const nextId = created?.id ?? null;
      setSelectedTermId(nextId);
      await Promise.all([loadTerms(), loadTermStudents(nextId)]);
    } catch (err) {
      setError(toFriendlyError(err, 'save'));
    } finally {
      setSaving(false);
    }
  };

  const handleSetCurrent = async (termId) => {
    setSaving(true);
    setError('');
    try {
      const response = await fetch(`${API_BASE}/api/lecturer/terms/${termId}/current`, {
        method: 'POST',
        headers: authHeaders(),
      });
      if (!response.ok) {
        throw new Error(await readFriendlyApiError(response, 'save'));
      }
      await loadTerms();
    } catch (err) {
      setError(toFriendlyError(err, 'save'));
    } finally {
      setSaving(false);
    }
  };

  const handleEnroll = async () => {
    if (!selectedTermId || selectedStudentIds.length === 0) return;
    setSaving(true);
    setError('');
    setNotice('');
    try {
      const response = await fetch(`${API_BASE}/api/lecturer/terms/${selectedTermId}/students`, {
        method: 'POST',
        headers: authHeaders({ 'Content-Type': 'application/json' }),
        body: JSON.stringify({ studentIds: selectedStudentIds }),
      });
      if (!response.ok) {
        throw new Error(await readFriendlyApiError(response, 'save'));
      }
      await refreshSelectedTerm(selectedTermId);
    } catch (err) {
      setError(toFriendlyError(err, 'save'));
    } finally {
      setSaving(false);
    }
  };

  const formatImportNotice = (result) => {
    const parts = [`Added ${result.enrolled ?? 0} student${result.enrolled === 1 ? '' : 's'}`];
    if (result.alreadyInTerm) {
      parts.push(`${result.alreadyInTerm} already in this term`);
    }
    if (result.notFound) {
      parts.push(`${result.notFound} not found`);
    }
    if (result.skipped) {
      parts.push(`${result.skipped} skipped`);
    }
    return `${parts.join('. ')}.`;
  };

  const importExcelFile = async (file) => {
    if (!file || !selectedTermId) return;
    if (!isSpreadsheetFile(file)) {
      setError('Please drop an Excel (.xlsx, .xls) or CSV file.');
      return;
    }
    setSaving(true);
    setError('');
    setNotice('');
    try {
      const rows = await parseStudentImportFile(file);
      const response = await fetch(`${API_BASE}/api/lecturer/terms/${selectedTermId}/students/import`, {
        method: 'POST',
        headers: authHeaders({ 'Content-Type': 'application/json' }),
        body: JSON.stringify({ rows }),
      });
      if (!response.ok) {
        throw new Error(await readFriendlyApiError(response, 'save'));
      }
      const result = await response.json();
      await refreshSelectedTerm(selectedTermId);
      setNotice(formatImportNotice(result));
      if (Array.isArray(result.unmatched) && result.unmatched.length > 0) {
        setError(`Not matched: ${result.unmatched.join('; ')}`);
      }
    } catch (err) {
      setError(toFriendlyError(err, 'save'));
    } finally {
      setSaving(false);
    }
  };

  const handleImportFile = (event) => {
    const file = event.target.files?.[0];
    event.target.value = '';
    importExcelFile(file);
  };

  const handleDragOver = (event) => {
    event.preventDefault();
    event.stopPropagation();
    if (!saving) setIsDragging(true);
  };

  const handleDragLeave = (event) => {
    event.preventDefault();
    if (!event.currentTarget.contains(event.relatedTarget)) {
      setIsDragging(false);
    }
  };

  const handleDropExcel = (event) => {
    event.preventDefault();
    event.stopPropagation();
    setIsDragging(false);
    const file = event.dataTransfer?.files?.[0];
    importExcelFile(file);
  };

  const handleSuspendToggle = async (student) => {
    if (!student?.id) return;
    const suspending = student.isActive !== false;
    setSaving(true);
    setError('');
    setNotice('');
    try {
      const path = suspending ? 'suspend' : 'unsuspend';
      const response = await fetch(`${API_BASE}/api/users/${student.id}/${path}`, {
        method: 'POST',
        headers: authHeaders(),
      });
      if (!response.ok) {
        throw new Error(await readFriendlyApiError(response, 'save'));
      }
      await refreshSelectedTerm(selectedTermId);
      setNotice(suspending
        ? `${student.fullName} is suspended and cannot log in.`
        : `${student.fullName} can log in again.`);
    } catch (err) {
      setError(toFriendlyError(err, 'save'));
    } finally {
      setSaving(false);
    }
  };

  const handleRemove = async (studentId) => {
    if (!selectedTermId) return;
    setSaving(true);
    setError('');
    try {
      const response = await fetch(`${API_BASE}/api/lecturer/terms/${selectedTermId}/students/${studentId}`, {
        method: 'DELETE',
        headers: authHeaders(),
      });
      if (!response.ok) {
        throw new Error(await readFriendlyApiError(response, 'delete'));
      }
      await refreshSelectedTerm(selectedTermId);
    } catch (err) {
      setError(toFriendlyError(err, 'delete'));
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="space-y-6 px-4 sm:px-6 lg:px-8 max-w-full overflow-x-hidden">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-xl font-semibold text-foreground">Terms</h2>
          <p className="mt-1 text-sm text-foreground-secondary">
            Create a term for a year, mark which term is current, and add active students. Only students in the current term can submit labs.
          </p>
        </div>
        <button
          type="button"
          onClick={() => setShowCreate(true)}
          className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white hover:bg-primary-hover"
        >
          <Plus className="h-4 w-4" />
          Add term
        </button>
      </div>

      {error && (
        <p className="rounded-lg border border-warning/40 bg-warning-bg px-3 py-2 text-sm text-warning-text">{error}</p>
      )}
      {notice && (
        <p className="rounded-lg border border-success/40 bg-success-bg px-3 py-2 text-sm text-success-text">{notice}</p>
      )}

      {showCreate && (
        <div className="rounded-3xl border border-border bg-surface p-4 shadow-sm">
          <h3 className="mb-4 text-base font-semibold text-foreground">New term</h3>
          <div className="grid gap-4 sm:grid-cols-2">
            <label className="block text-sm">
              <span className="mb-1 block text-xs text-foreground-muted">Year</span>
              <input
                className="w-full rounded-lg border border-border bg-surface-secondary px-3 py-2 text-sm text-foreground"
                placeholder="2025-2026"
                value={form.yearLabel}
                onChange={(e) => setForm((prev) => ({ ...prev, yearLabel: e.target.value }))}
              />
            </label>
            <label className="block text-sm">
              <span className="mb-1 block text-xs text-foreground-muted">Term number</span>
              <select
                className="w-full rounded-lg border border-border bg-surface-secondary px-3 py-2 text-sm text-foreground"
                value={form.termNumber}
                onChange={(e) => setForm((prev) => ({ ...prev, termNumber: e.target.value }))}
              >
                <option value="1">Term 1</option>
                <option value="2">Term 2</option>
                <option value="3">Term 3</option>
              </select>
            </label>
            <label className="block text-sm">
              <span className="mb-1 block text-xs text-foreground-muted">Start date (optional)</span>
              <DatePicker
                className="w-full bg-surface-secondary"
                value={form.startDate}
                placeholder="Select Date..."
                onChange={(startDate) => setForm((prev) => ({ ...prev, startDate }))}
              />
            </label>
            <label className="block text-sm">
              <span className="mb-1 block text-xs text-foreground-muted">End date (optional)</span>
              <DatePicker
                className="w-full bg-surface-secondary"
                value={form.endDate}
                placeholder="Select Date..."
                onChange={(endDate) => setForm((prev) => ({ ...prev, endDate }))}
              />
            </label>
          </div>
          <label className="mt-4 flex items-center gap-2 text-sm text-foreground">
            <input
              type="checkbox"
              checked={form.setCurrent}
              onChange={(e) => setForm((prev) => ({ ...prev, setCurrent: e.target.checked }))}
            />
            Set as current term
          </label>
          <div className="mt-4 flex gap-2">
            <button
              type="button"
              disabled={saving}
              onClick={handleCreate}
              className="rounded-lg bg-primary px-4 py-2 text-sm text-white disabled:opacity-50"
            >
              Create
            </button>
            <button
              type="button"
              onClick={() => { setShowCreate(false); setForm(EMPTY_FORM); }}
              className="rounded-lg border border-border px-4 py-2 text-sm"
            >
              Cancel
            </button>
          </div>
        </div>
      )}

      <div className="grid gap-6 lg:grid-cols-[0.34fr_1fr]">
        <div className="rounded-3xl border border-border bg-surface p-4 shadow-sm">
          <h3 className="mb-3 text-xs font-semibold uppercase tracking-[0.15em] text-foreground-muted">
            Academic terms
          </h3>
          {loading ? (
            <p className="py-6 text-center text-sm text-foreground-muted">Loading terms...</p>
          ) : terms.length === 0 ? (
            <p className="py-6 text-center text-sm text-foreground-muted">No terms yet</p>
          ) : (
            <div className="space-y-2">
              {terms.map((term) => (
                <button
                  key={term.id}
                  type="button"
                  onClick={() => handleSelectTerm(term.id)}
                  className={`w-full rounded-xl border px-4 py-3 text-left text-sm transition ${
                    String(selectedTermId) === String(term.id)
                      ? 'border-primary bg-primary-light text-primary-text'
                      : 'border-border bg-surface text-foreground-secondary hover:border-primary hover:bg-primary-light'
                  }`}
                >
                  <div className="flex items-center justify-between gap-2">
                    <span className="font-medium">{term.label}</span>
                    {term.current && (
                      <span className="inline-flex items-center gap-1 rounded-full bg-success-bg px-2 py-0.5 text-[11px] font-semibold text-success-text">
                        <Star className="h-3 w-3" /> Current
                      </span>
                    )}
                  </div>
                  <p className="mt-1 text-xs text-foreground-muted">{term.studentCount ?? 0} students</p>
                </button>
              ))}
            </div>
          )}
        </div>

        <div className="rounded-3xl border border-border bg-surface p-4 shadow-sm">
          {selectedTerm ? (
            <>
              <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
                <div>
                  <h3 className="inline-flex items-center gap-2 text-base font-semibold text-foreground">
                    <CalendarDays className="h-4 w-4" />
                    {selectedTerm.label}
                  </h3>
                  {selectedTerm.endDate && (
                    <p className="mt-1 text-xs text-foreground-muted">Ends {selectedTerm.endDate}</p>
                  )}
                </div>
                {!selectedTerm.current && (
                  <button
                    type="button"
                    disabled={saving}
                    onClick={() => handleSetCurrent(selectedTerm.id)}
                    className="rounded-lg border border-border px-3 py-1.5 text-sm hover:bg-surface-secondary disabled:opacity-50"
                  >
                    Set as current
                  </button>
                )}
              </div>

              <div className="mb-4 rounded-xl border border-border p-3">
                <p className="mb-2 text-xs font-semibold uppercase tracking-[0.15em] text-foreground-muted">
                  Add students
                </p>
                <p className="mb-3 text-sm text-foreground-muted">
                  Drop an Excel file with Student ID (IRN) and Email, or click to browse. Extra columns are ignored. Only existing active students are added.
                </p>
                <input
                  ref={fileInputRef}
                  type="file"
                  accept=".xlsx,.xls,.csv,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,application/vnd.ms-excel,text/csv"
                  className="hidden"
                  onChange={handleImportFile}
                />
                <button
                  type="button"
                  disabled={saving}
                  onClick={() => fileInputRef.current?.click()}
                  onDragEnter={handleDragOver}
                  onDragOver={handleDragOver}
                  onDragLeave={handleDragLeave}
                  onDrop={handleDropExcel}
                  className={`mb-3 flex w-full flex-col items-center justify-center gap-2 rounded-xl border-2 border-dashed px-4 py-8 text-sm transition-colors disabled:opacity-50 ${
                    isDragging
                      ? 'border-primary bg-primary-light text-primary-text'
                      : 'border-border bg-surface-secondary text-foreground-secondary hover:border-primary hover:bg-primary-light'
                  }`}
                >
                  <FileSpreadsheet className="h-6 w-6" />
                  <span className="font-medium text-foreground">Drop Excel here or click to import</span>
                  <span className="text-xs text-foreground-muted">.xlsx, .xls, or .csv</span>
                </button>
                {available.length === 0 ? (
                  <p className="text-sm text-foreground-muted">All active students are already in this term.</p>
                ) : (
                  <div className="flex flex-col gap-2 sm:flex-row">
                    <select
                      multiple
                      className="min-h-[120px] w-full rounded-lg border border-border bg-surface-secondary px-3 py-2 text-sm text-foreground"
                      value={selectedStudentIds}
                      onChange={(e) =>
                        setSelectedStudentIds(Array.from(e.target.selectedOptions, (option) => option.value))
                      }
                    >
                      {available.map((student) => (
                        <option key={student.id} value={student.id}>
                          {student.fullName} ({student.studentCode || student.email})
                        </option>
                      ))}
                    </select>
                    <button
                      type="button"
                      disabled={saving || selectedStudentIds.length === 0}
                      onClick={handleEnroll}
                      className="inline-flex shrink-0 items-center justify-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm text-white disabled:opacity-50"
                    >
                      <UserPlus className="h-4 w-4" />
                      Add
                    </button>
                  </div>
                )}
              </div>

              <div className="overflow-x-auto rounded-xl border border-border">
                <table className="w-full">
                  <thead>
                    <tr className="border-b border-border text-left text-sm text-foreground-secondary">
                      <th className="px-4 py-3">Student</th>
                      <th className="px-4 py-3">IRN</th>
                      <th className="px-4 py-3">Email</th>
                      <th className="px-4 py-3">Status</th>
                      <th className="px-4 py-3">Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    {students.length === 0 ? (
                      <tr>
                        <td colSpan={5} className="px-4 py-8 text-center text-sm text-foreground-muted">
                          No students in this term yet
                        </td>
                      </tr>
                    ) : (
                      students.map((student) => (
                        <tr key={student.id} className="border-b border-border">
                          <td className="px-4 py-3 text-sm text-foreground">{student.fullName}</td>
                          <td className="px-4 py-3 text-sm text-foreground">{student.studentCode || '—'}</td>
                          <td className="px-4 py-3 text-sm text-foreground">{student.email}</td>
                          <td className="px-4 py-3">
                            <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-[11px] font-semibold ${
                              student.isActive === false
                                ? 'bg-warning-bg text-warning-text'
                                : 'bg-success-bg text-success-text'
                            }`}>
                              {student.isActive === false ? 'Suspended' : 'Active'}
                            </span>
                          </td>
                          <td className="px-4 py-3">
                            <div className="flex flex-wrap items-center gap-2">
                              <button
                                type="button"
                                disabled={saving}
                                onClick={() => handleSuspendToggle(student)}
                                className="inline-flex items-center gap-1 rounded-lg border border-border px-2 py-1 text-xs text-foreground-secondary hover:bg-surface-secondary disabled:opacity-50"
                              >
                                {student.isActive === false ? <UserCheck className="h-3 w-3" /> : <Ban className="h-3 w-3" />}
                                {student.isActive === false ? 'Restore' : 'Suspend'}
                              </button>
                              <button
                                type="button"
                                disabled={saving}
                                onClick={() => handleRemove(student.id)}
                                className="inline-flex items-center gap-1 rounded-lg border border-border px-2 py-1 text-xs text-error hover:bg-error-bg disabled:opacity-50"
                              >
                                <Trash2 className="h-3 w-3" />
                                Remove
                              </button>
                            </div>
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </>
          ) : (
            <p className="py-10 text-center text-sm text-foreground-muted">Select a term to manage students.</p>
          )}
        </div>
      </div>
    </div>
  );
}
