import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { CalendarDays, FileSpreadsheet, Plus, Search, Star, Trash2, UserPlus, Ban, UserCheck } from 'lucide-react';
import { authHeaders } from '../utils/authHeaders';
import { apiFetch } from '../utils/apiFetch';
import { readFriendlyApiError, toFriendlyError } from '../utils/apiError';
import { isSpreadsheetFile, parseStudentImportFile } from '../utils/studentImport';
import DatePicker from '../components/ui/DatePicker';
import Modal from '../components/ui/Modal';
import { useToast } from '../components/ui/Toast';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';

const EMPTY_FORM = {
  yearLabel: '',
  termNumber: '1',
  startDate: '',
  endDate: '',
  setCurrent: false,
};

function matchesStudentSearch(student, query) {
  const haystack = `${student.fullName || ''} ${student.studentCode || ''} ${student.email || ''}`.toLowerCase();
  return haystack.includes(query);
}

function studentIdentity(item) {
  if (typeof item === 'string') {
    return { name: item, meta: '' };
  }
  const name = (item?.fullName || '').trim()
    || (item?.studentCode || '').trim()
    || (item?.email || '').trim()
    || 'Unknown name';
  const meta = [item?.studentCode, item?.email].filter(Boolean).join(' · ');
  return { name, meta };
}

function formatImportNotice(result) {
  const parts = [`Added ${result.enrolled ?? 0} student${result.enrolled === 1 ? '' : 's'}`];
  if (result.alreadyInTerm) {
    parts.push(`${result.alreadyInTerm} already in this quarter`);
  }
  if (result.notFound) {
    parts.push(`${result.notFound} not found in the system`);
  }
  return `${parts.join('. ')}.`;
}

function asStudentList(value) {
  return Array.isArray(value) ? value : [];
}

function compareTerms(a, b, order = 'desc') {
  const yearA = a.yearLabel || '';
  const yearB = b.yearLabel || '';
  const yearCmp = yearA.localeCompare(yearB);
  const termCmp = (a.termNumber ?? 0) - (b.termNumber ?? 0);
  const combined = yearCmp !== 0 ? yearCmp : termCmp;
  return order === 'asc' ? combined : -combined;
}

function matchesTermSearch(term, query) {
  const haystack = `${term.label || ''} ${term.yearLabel || ''} quarter ${term.termNumber ?? ''}`.toLowerCase();
  return haystack.includes(query);
}

export default function TermManagement() {
  const showToast = useToast();
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
  const [isDragging, setIsDragging] = useState(false);
  const [availableSearch, setAvailableSearch] = useState('');
  const [rosterSearch, setRosterSearch] = useState('');
  const [termSearch, setTermSearch] = useState('');
  const [termFilter, setTermFilter] = useState('all');
  const [termSortOrder, setTermSortOrder] = useState('desc');
  const [importResult, setImportResult] = useState(null);
  const [importDialog, setImportDialog] = useState(null);
  const fileInputRef = useRef(null);

  const selectedTerm = useMemo(
    () => terms.find((term) => String(term.id) === String(selectedTermId)) ?? null,
    [terms, selectedTermId],
  );

  const filteredAvailable = useMemo(() => {
    const query = availableSearch.trim().toLowerCase();
    if (!query) return available;
    return available.filter((student) => matchesStudentSearch(student, query));
  }, [available, availableSearch]);

  const filteredStudents = useMemo(() => {
    const query = rosterSearch.trim().toLowerCase();
    if (!query) return students;
    return students.filter((student) => matchesStudentSearch(student, query));
  }, [students, rosterSearch]);

  const termYearOptions = useMemo(() => {
    const years = [...new Set(terms.map((term) => term.yearLabel).filter(Boolean))];
    years.sort((a, b) => b.localeCompare(a));
    return years;
  }, [terms]);

  const filteredTerms = useMemo(() => {
    let list = [...terms];
    if (termFilter !== 'all') {
      list = list.filter((term) => term.yearLabel === termFilter);
    }
    const query = termSearch.trim().toLowerCase();
    if (query) {
      list = list.filter((term) => matchesTermSearch(term, query));
    }
    list.sort((a, b) => compareTerms(a, b, termSortOrder));
    return list;
  }, [terms, termFilter, termSearch, termSortOrder]);

  const loadTerms = useCallback(async () => {
    const response = await apiFetch(`${API_BASE}/api/lecturer/terms`, { headers: authHeaders() });
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
    const response = await apiFetch(`${API_BASE}/api/lecturer/terms/${termId}/roster`, { headers: authHeaders() });
    if (!response.ok) {
      throw new Error(await readFriendlyApiError(response, 'read'));
    }
    const data = await response.json();
    setStudents(Array.isArray(data?.enrolled) ? data.enrolled : []);
    setAvailable(Array.isArray(data?.available) ? data.available : []);
    setSelectedStudentIds([]);
    setAvailableSearch('');
    setRosterSearch('');
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

  const toggleStudentSelection = (studentId) => {
    const id = String(studentId);
    setSelectedStudentIds((prev) =>
      prev.includes(id) ? prev.filter((value) => value !== id) : [...prev, id],
    );
  };

  const handleSelectTerm = async (termId) => {
    setSelectedTermId(termId);
    setError('');
    setAvailableSearch('');
    setRosterSearch('');
    setImportResult(null);
    setImportDialog(null);
    try {
      await loadTermStudents(termId);
    } catch (err) {
      setError(toFriendlyError(err, 'read'));
    }
  };

  const handleCreate = async () => {
    if (!form.yearLabel.trim()) {
      showToast({ message: 'Year is required', type: 'error' });
      return;
    }
    setSaving(true);
    setError('');
    try {
      const response = await apiFetch(`${API_BASE}/api/lecturer/terms`, {
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
      showToast({ message: 'Saved successfully.', type: 'success' });
    } catch (err) {
      const message = toFriendlyError(err, 'save');
      setError(message);
      showToast({ message, type: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const handleDeleteTerm = async (termId) => {
    const term = terms.find((item) => String(item.id) === String(termId));
    const label = term?.label ?? 'this quarter';
    if (!window.confirm(`Delete ${label}? Enrolled students are removed from this quarter only. This cannot be undone.`)) {
      return;
    }
    setSaving(true);
    setError('');
    try {
      const response = await apiFetch(`${API_BASE}/api/lecturer/terms/${termId}`, {
        method: 'DELETE',
        headers: authHeaders(),
      });
      if (!response.ok) {
        throw new Error(await readFriendlyApiError(response, 'delete'));
      }
      const data = await loadTerms();
      const next = data.find((item) => item.current) ?? data[0];
      const nextId = next?.id ?? null;
      setSelectedTermId(nextId);
      if (nextId) {
        await loadTermStudents(nextId);
      } else {
        setStudents([]);
        setAvailable([]);
      }
      showToast({ message: 'Deleted successfully.', type: 'success' });
    } catch (err) {
      const message = toFriendlyError(err, 'delete');
      setError(message);
      showToast({ message, type: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const handleSetCurrent = async (termId) => {
    setSaving(true);
    setError('');
    try {
      const response = await apiFetch(`${API_BASE}/api/lecturer/terms/${termId}/current`, {
        method: 'POST',
        headers: authHeaders(),
      });
      if (!response.ok) {
        throw new Error(await readFriendlyApiError(response, 'save'));
      }
      await loadTerms();
      showToast({ message: 'Saved successfully.', type: 'success' });
    } catch (err) {
      const message = toFriendlyError(err, 'save');
      setError(message);
      showToast({ message, type: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const handleEnroll = async () => {
    if (!selectedTermId || selectedStudentIds.length === 0) return;
    setSaving(true);
    setError('');
    try {
      const response = await apiFetch(`${API_BASE}/api/lecturer/terms/${selectedTermId}/students`, {
        method: 'POST',
        headers: authHeaders({ 'Content-Type': 'application/json' }),
        body: JSON.stringify({ studentIds: selectedStudentIds }),
      });
      if (!response.ok) {
        throw new Error(await readFriendlyApiError(response, 'save'));
      }
      await refreshSelectedTerm(selectedTermId);
      showToast({ message: 'Saved successfully.', type: 'success' });
    } catch (err) {
      const message = toFriendlyError(err, 'save');
      setError(message);
      showToast({ message, type: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const closeImportDialog = () => {
    setImportDialog(null);
    setImportResult(null);
  };

  const importExcelFile = async (file) => {
    if (!file || !selectedTermId) return;
    if (!isSpreadsheetFile(file)) {
      const message = 'Please drop an Excel (.xlsx, .xls) or CSV file.';
      setError(message);
      showToast({ message, type: 'error' });
      return;
    }
    setSaving(true);
    setError('');
    try {
      const rows = await parseStudentImportFile(file);
      const response = await apiFetch(`${API_BASE}/api/lecturer/terms/${selectedTermId}/students/import`, {
        method: 'POST',
        headers: authHeaders({ 'Content-Type': 'application/json' }),
        body: JSON.stringify({ rows }),
      });
      if (!response.ok) {
        throw new Error(await readFriendlyApiError(response, 'save'));
      }
      const result = await response.json();
      await refreshSelectedTerm(selectedTermId);
      const notFoundStudents = asStudentList(result.notFoundStudents).length > 0
        ? asStudentList(result.notFoundStudents)
        : asStudentList(result.unmatched);
      const alreadyInTermStudents = asStudentList(result.alreadyInTermStudents);
      const report = {
        enrolled: result.enrolled ?? 0,
        alreadyInTerm: result.alreadyInTerm ?? alreadyInTermStudents.length,
        notFound: result.notFound ?? notFoundStudents.length,
        notFoundStudents,
        alreadyInTermStudents,
      };
      setImportResult(report);
      if (report.notFound > 0 || report.alreadyInTerm > 0) {
        setImportDialog(null);
        showToast({
          message: formatImportNotice(report),
          type: 'warning',
          persist: true,
          actionLabel: 'Show details',
          onAction: () => setImportDialog('details'),
        });
      } else {
        setImportDialog(null);
        showToast({
          message: formatImportNotice(report),
          type: 'success',
        });
      }
    } catch (err) {
      const message = toFriendlyError(err, 'save');
      setError(message);
      showToast({ message, type: 'error' });
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
    try {
      const path = suspending ? 'suspend' : 'unsuspend';
      const response = await apiFetch(`${API_BASE}/api/users/${student.id}/${path}`, {
        method: 'POST',
        headers: authHeaders(),
      });
      if (!response.ok) {
        throw new Error(await readFriendlyApiError(response, 'save'));
      }
      await refreshSelectedTerm(selectedTermId);
      showToast({
        message: suspending
          ? `${student.fullName} is suspended and cannot log in.`
          : `${student.fullName} can log in again.`,
        type: 'success',
      });
    } catch (err) {
      const message = toFriendlyError(err, 'save');
      setError(message);
      showToast({ message, type: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const handleRemove = async (studentId) => {
    if (!selectedTermId) return;
    setSaving(true);
    setError('');
    try {
      const response = await apiFetch(`${API_BASE}/api/lecturer/terms/${selectedTermId}/students/${studentId}`, {
        method: 'DELETE',
        headers: authHeaders(),
      });
      if (!response.ok) {
        throw new Error(await readFriendlyApiError(response, 'delete'));
      }
      await refreshSelectedTerm(selectedTermId);
      showToast({ message: 'Deleted successfully.', type: 'success' });
    } catch (err) {
      const message = toFriendlyError(err, 'delete');
      setError(message);
      showToast({ message, type: 'error' });
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="max-w-full space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-xl font-semibold text-foreground">Quarters</h2>
          <p className="mt-1 text-sm text-foreground-secondary">
            Create a quarter for a year, mark which quarter is current, and add active students. Only students in the current quarter can submit labs.
          </p>
        </div>
        <button
          type="button"
          onClick={() => setShowCreate(true)}
          className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white hover:bg-primary-hover"
        >
          <Plus className="h-4 w-4" />
          Add quarter
        </button>
      </div>

      {error && (
        <p className="rounded-lg border border-warning/40 bg-warning-bg px-3 py-2 text-sm text-warning-text">{error}</p>
      )}

      {showCreate && (
        <div className="rounded-3xl border border-border bg-surface p-4 shadow-sm">
          <h3 className="mb-4 text-base font-semibold text-foreground">New quarter</h3>
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
              <span className="mb-1 block text-xs text-foreground-muted">Quarter</span>
              <select
                className="w-full rounded-lg border border-border bg-surface-secondary px-3 py-2 text-sm text-foreground"
                value={form.termNumber}
                onChange={(e) => setForm((prev) => ({ ...prev, termNumber: e.target.value }))}
              >
                <option value="1">Quarter 1</option>
                <option value="2">Quarter 2</option>
                <option value="3">Quarter 3</option>
                <option value="4">Quarter 4 (Summer Quarter)</option>
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
            Set as current quarter
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
            Academic quarters
          </h3>
          {!loading && terms.length > 0 && (
            <div className="mb-3 space-y-2">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-foreground-muted" />
                <input
                  type="text"
                  placeholder="Search quarters…"
                  value={termSearch}
                  onChange={(e) => setTermSearch(e.target.value)}
                  className="w-full rounded-lg border border-border bg-surface-secondary py-2 pl-9 pr-3 text-sm text-foreground placeholder-foreground-disabled focus:outline-none focus:ring-2 focus:ring-primary"
                />
              </div>
              <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
                <select
                  value={termFilter}
                  onChange={(e) => setTermFilter(e.target.value)}
                  className="w-full rounded-lg border border-border bg-surface-secondary px-3 py-2 text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
                  aria-label="Filter quarters by year"
                >
                  <option value="all">All quarters</option>
                  {termYearOptions.map((year) => (
                    <option key={year} value={year}>{year}</option>
                  ))}
                </select>
                <select
                  value={termSortOrder}
                  onChange={(e) => setTermSortOrder(e.target.value)}
                  className="w-full rounded-lg border border-border bg-surface-secondary px-3 py-2 text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
                  aria-label="Sort quarters"
                >
                  <option value="desc">Descending</option>
                  <option value="asc">Ascending</option>
                </select>
              </div>
            </div>
          )}
          {loading ? (
            <p className="py-6 text-center text-sm text-foreground-muted">Loading quarters...</p>
          ) : terms.length === 0 ? (
            <p className="py-6 text-center text-sm text-foreground-muted">No quarters yet</p>
          ) : filteredTerms.length === 0 ? (
            <p className="py-6 text-center text-sm text-foreground-muted">No quarters match your search</p>
          ) : (
            <div className="space-y-2">
              {filteredTerms.map((term) => (
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
                <div className="flex flex-wrap items-center gap-2">
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
                  <button
                    type="button"
                    disabled={saving || selectedTerm.current}
                    onClick={() => handleDeleteTerm(selectedTerm.id)}
                    title={selectedTerm.current ? 'Set another quarter as current before deleting' : 'Delete quarter'}
                    className="inline-flex items-center gap-1.5 rounded-lg border border-error/40 px-3 py-1.5 text-sm text-error-text hover:bg-error-bg disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    <Trash2 className="h-4 w-4" />
                    Delete quarter
                  </button>
                </div>
              </div>

              <div className="mb-4 rounded-xl border border-border p-3">
                <p className="mb-2 text-xs font-semibold uppercase tracking-[0.15em] text-foreground-muted">
                  Add students
                </p>
                <p className="mb-3 text-sm text-foreground-muted">
                  Import from Excel (IRN + Email) or search and select students to add manually. Only existing active students can be enrolled.
                </p>
                <div className="grid grid-cols-1 gap-3 lg:grid-cols-[1fr_2fr]">
                  <div>
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
                      className={`flex h-full min-h-[160px] w-full flex-col items-center justify-center gap-2 rounded-xl border-2 border-dashed px-3 py-6 text-sm transition-colors disabled:opacity-50 ${
                        isDragging
                          ? 'border-primary bg-primary-light text-primary-text'
                          : 'border-border bg-surface-secondary text-foreground-secondary hover:border-primary hover:bg-primary-light'
                      }`}
                    >
                      <FileSpreadsheet className="h-6 w-6" />
                      <span className="text-center font-medium text-foreground">Drop Excel or click to import</span>
                      <span className="text-center text-xs text-foreground-muted">.xlsx, .xls, or .csv</span>
                    </button>
                  </div>

                  <div className="flex min-h-[160px] flex-col gap-2">
                    {available.length === 0 ? (
                      <p className="flex flex-1 items-center text-sm text-foreground-muted">
                        All active students are already in this quarter.
                      </p>
                    ) : (
                      <>
                        <div className="relative">
                          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-foreground-muted" />
                          <input
                            type="text"
                            placeholder="Search by name, IRN, or email…"
                            value={availableSearch}
                            onChange={(e) => setAvailableSearch(e.target.value)}
                            className="w-full rounded-lg border border-border bg-surface-secondary py-2 pl-9 pr-3 text-sm text-foreground placeholder-foreground-disabled focus:outline-none focus:ring-2 focus:ring-primary"
                          />
                        </div>
                        <div className="max-h-40 flex-1 overflow-y-auto rounded-lg border border-border bg-surface-secondary">
                          {filteredAvailable.length === 0 ? (
                            <p className="px-3 py-4 text-sm text-foreground-muted">No students match your search.</p>
                          ) : (
                            <ul className="divide-y divide-border">
                              {filteredAvailable.map((student) => {
                                const studentId = String(student.id);
                                const checked = selectedStudentIds.includes(studentId);
                                return (
                                  <li key={student.id}>
                                    <label className="flex cursor-pointer items-start gap-2 px-3 py-2 text-sm hover:bg-surface">
                                      <input
                                        type="checkbox"
                                        className="mt-0.5"
                                        checked={checked}
                                        onChange={() => toggleStudentSelection(studentId)}
                                      />
                                      <span className="text-foreground">
                                        {student.fullName}
                                        <span className="text-foreground-muted">
                                          {' '}
                                          ({student.studentCode || student.email})
                                        </span>
                                      </span>
                                    </label>
                                  </li>
                                );
                              })}
                            </ul>
                          )}
                        </div>
                        <button
                          type="button"
                          disabled={saving || selectedStudentIds.length === 0}
                          onClick={handleEnroll}
                          className="inline-flex shrink-0 items-center justify-center gap-2 self-start rounded-lg bg-primary px-4 py-2 text-sm text-white disabled:opacity-50"
                        >
                          <UserPlus className="h-4 w-4" />
                          {selectedStudentIds.length > 0
                            ? `Add selected (${selectedStudentIds.length})`
                            : 'Add selected'}
                        </button>
                      </>
                    )}
                  </div>
                </div>
              </div>

              <div className="overflow-x-auto rounded-xl border border-border">
                <div className="border-b border-border px-4 py-3">
                  <div className="relative max-w-xs">
                    <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-foreground-muted" />
                    <input
                      type="text"
                      placeholder="Search enrolled students…"
                      value={rosterSearch}
                      onChange={(e) => setRosterSearch(e.target.value)}
                      className="w-full rounded-lg border border-border bg-surface-secondary py-2 pl-9 pr-3 text-sm text-foreground placeholder-foreground-disabled focus:outline-none focus:ring-2 focus:ring-primary"
                    />
                  </div>
                </div>
                <table className="w-full min-w-[600px]">
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
                          No students in this quarter yet
                        </td>
                      </tr>
                    ) : filteredStudents.length === 0 ? (
                      <tr>
                        <td colSpan={5} className="px-4 py-8 text-center text-sm text-foreground-muted">
                          No students match your search
                        </td>
                      </tr>
                    ) : (
                      filteredStudents.map((student) => (
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
                                className="inline-flex min-h-10 items-center gap-1 rounded-lg border border-border px-3 py-2 text-xs text-foreground-secondary hover:bg-surface-secondary disabled:opacity-50 sm:text-sm"
                              >
                                {student.isActive === false ? <UserCheck className="h-3 w-3" /> : <Ban className="h-3 w-3" />}
                                {student.isActive === false ? 'Restore' : 'Suspend'}
                              </button>
                              <button
                                type="button"
                                disabled={saving}
                                onClick={() => handleRemove(student.id)}
                                className="inline-flex min-h-10 items-center gap-1 rounded-lg border border-border px-3 py-2 text-xs text-error hover:bg-error-bg disabled:opacity-50 sm:text-sm"
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
            <p className="py-10 text-center text-sm text-foreground-muted">Select a quarter to manage students.</p>
          )}
        </div>
      </div>
      {importDialog === 'details' && importResult && (
        <Modal onClose={closeImportDialog} className="max-w-lg">
          <h3 className="mb-1 text-lg font-semibold text-foreground">Import details</h3>
          <p className="mb-4 text-sm text-foreground-muted">
            Students not in the system must be created in Users first. Students already in this quarter were skipped.
          </p>
          <div className="max-h-80 space-y-4 overflow-y-auto">
            <section>
              <h4 className="mb-2 text-xs font-semibold uppercase tracking-[0.15em] text-error">
                Not in the system ({importResult.notFoundStudents.length})
              </h4>
              {importResult.notFoundStudents.length === 0 ? (
                <p className="text-sm text-foreground-muted">Every imported name matched an account.</p>
              ) : (
                <ul className="space-y-2">
                  {importResult.notFoundStudents.map((item, index) => {
                    const identity = studentIdentity(item);
                    return (
                      <li key={`missing-${identity.name}-${index}`} className="rounded-lg border border-error/30 bg-error-bg/40 px-3 py-2">
                        <p className="text-sm font-medium text-foreground">{identity.name}</p>
                        {identity.meta ? (
                          <p className="mt-0.5 text-xs text-foreground-muted">{identity.meta}</p>
                        ) : null}
                        <p className="mt-1 text-xs text-error-text">
                          {typeof item === 'string' ? item : (item.reason || 'No matching student account was found.')}
                        </p>
                      </li>
                    );
                  })}
                </ul>
              )}
            </section>
            <section>
              <h4 className="mb-2 text-xs font-semibold uppercase tracking-[0.15em] text-warning-text">
                Already in this quarter ({importResult.alreadyInTermStudents.length})
              </h4>
              {importResult.alreadyInTermStudents.length === 0 ? (
                <p className="text-sm text-foreground-muted">No imported students were already enrolled.</p>
              ) : (
                <ul className="space-y-2">
                  {importResult.alreadyInTermStudents.map((item, index) => {
                    const identity = studentIdentity(item);
                    return (
                      <li key={`enrolled-${identity.name}-${index}`} className="rounded-lg border border-warning/30 bg-warning-bg/50 px-3 py-2">
                        <p className="text-sm font-medium text-foreground">{identity.name}</p>
                        {identity.meta ? (
                          <p className="mt-0.5 text-xs text-foreground-muted">{identity.meta}</p>
                        ) : null}
                        <p className="mt-1 text-xs text-warning-text">
                          {item.reason || 'Already enrolled in this quarter.'}
                        </p>
                      </li>
                    );
                  })}
                </ul>
              )}
            </section>
          </div>
        </Modal>
      )}
    </div>
  );
}
