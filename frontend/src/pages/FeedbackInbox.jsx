import { useCallback, useEffect, useState } from 'react';
import { Trash2 } from 'lucide-react';
import { apiFetch } from '../utils/apiFetch';
import { authHeaders } from '../utils/authHeaders';
import { friendlyLoadErrorFromResponse, readFriendlyApiError, toFriendlyError } from '../utils/apiError';
import { useToast } from '../components/ui/Toast';
import Button from '../components/ui/Button';
import ModalOverlay from '../components/ui/ModalOverlay';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';

function typeLabel(type) {
  return type === 'CONTRIBUTION' ? 'Contribution' : 'Bug report';
}

function statusLabel(status) {
  return status === 'RESOLVED' ? 'Resolved' : 'Open';
}

function formatWhen(value) {
  if (!value) return '';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString();
}

export default function FeedbackInbox() {
  const showToast = useToast();
  const [statusFilter, setStatusFilter] = useState('OPEN');
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedId, setSelectedId] = useState(null);
  const [detail, setDetail] = useState(null);
  const [detailError, setDetailError] = useState(null);
  const [updating, setUpdating] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [replyOpen, setReplyOpen] = useState(false);
  const [replyDraft, setReplyDraft] = useState('');

  const loadList = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await apiFetch(
        `${API_BASE}/api/lecturer/feedback/list?status=${encodeURIComponent(statusFilter)}`,
        { headers: authHeaders() },
      );
      if (!response.ok) {
        throw new Error(await friendlyLoadErrorFromResponse(response));
      }
      setRows(await response.json());
    } catch (err) {
      setError(toFriendlyError(err, 'read'));
      setRows([]);
    } finally {
      setLoading(false);
    }
  }, [statusFilter]);

  useEffect(() => {
    loadList();
  }, [loadList]);

  const openItem = async (id) => {
    setSelectedId(id);
    setDetail(null);
    setDetailError(null);
    try {
      const response = await apiFetch(`${API_BASE}/api/lecturer/feedback/${id}`, {
        headers: authHeaders(),
      });
      if (!response.ok) {
        throw new Error(await friendlyLoadErrorFromResponse(response));
      }
      setDetail(await response.json());
    } catch (err) {
      setDetailError(toFriendlyError(err, 'read'));
    }
  };

  const patchStatus = async (nextStatus) => {
    if (!selectedId) return;
    setUpdating(true);
    try {
      const response = await apiFetch(`${API_BASE}/api/lecturer/feedback/${selectedId}/status`, {
        method: 'PATCH',
        headers: authHeaders({ 'Content-Type': 'application/json' }),
        body: JSON.stringify({ status: nextStatus }),
      });
      if (!response.ok) {
        throw new Error(await readFriendlyApiError(response, 'feedback'));
      }
      const updated = await response.json();
      setDetail(updated);
      showToast({ type: 'success', message: nextStatus === 'RESOLVED' ? 'Marked resolved.' : 'Reopened.' });
      await loadList();
    } catch (err) {
      showToast({ type: 'error', message: toFriendlyError(err, 'feedback') });
    } finally {
      setUpdating(false);
    }
  };

  const saveReply = async (event) => {
    event.preventDefault();
    if (!selectedId || !replyDraft.trim()) return;
    setUpdating(true);
    try {
      const response = await apiFetch(`${API_BASE}/api/lecturer/feedback/${selectedId}/response`, {
        method: 'PATCH',
        headers: authHeaders({ 'Content-Type': 'application/json' }),
        body: JSON.stringify({ response: replyDraft.trim() }),
      });
      if (!response.ok) {
        throw new Error(await readFriendlyApiError(response, 'feedback'));
      }
      const updated = await response.json();
      setDetail(updated);
      setReplyOpen(false);
      showToast({ type: 'success', message: 'Reply sent to the student notification.' });
    } catch (err) {
      showToast({ type: 'error', message: toFriendlyError(err, 'feedback') });
    } finally {
      setUpdating(false);
    }
  };

  const deleteResolved = async () => {
    if (!selectedId) return;
    setUpdating(true);
    try {
      const response = await apiFetch(`${API_BASE}/api/lecturer/feedback/${selectedId}`, {
        method: 'DELETE',
        headers: authHeaders(),
      });
      if (!response.ok) {
        throw new Error(await readFriendlyApiError(response, 'feedback'));
      }
      setConfirmDelete(false);
      setSelectedId(null);
      setDetail(null);
      showToast({ type: 'success', message: 'Feedback deleted.' });
      await loadList();
    } catch (err) {
      showToast({ type: 'error', message: toFriendlyError(err, 'feedback') });
    } finally {
      setUpdating(false);
    }
  };

  const openAttachment = async (attachment) => {
    try {
      const response = await apiFetch(
        `${API_BASE}/api/lecturer/feedback/${selectedId}/attachments/${attachment.id}`,
        { headers: authHeaders() },
      );
      if (!response.ok) {
        throw new Error(await readFriendlyApiError(response, 'download'));
      }
      const blob = await response.blob();
      const url = URL.createObjectURL(blob);
      window.open(url, '_blank', 'noopener,noreferrer');
      setTimeout(() => URL.revokeObjectURL(url), 60_000);
    } catch (err) {
      showToast({ type: 'error', message: toFriendlyError(err, 'download') });
    }
  };

  return (
    <div className="grid gap-6 lg:grid-cols-[minmax(0,1.2fr)_minmax(0,1fr)]">
      <section className="rounded-2xl border border-border bg-surface p-4 sm:p-6">
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-lg font-semibold text-foreground">Feedback</h2>
          <div className="flex rounded-lg border border-border p-1">
            {['OPEN', 'RESOLVED', 'ALL'].map((value) => (
              <button
                key={value}
                type="button"
                onClick={() => setStatusFilter(value)}
                className={`rounded-md px-3 py-1.5 text-sm ${
                  statusFilter === value
                    ? 'bg-primary text-white'
                    : 'text-foreground-secondary hover:bg-surface-secondary'
                }`}
              >
                {value === 'OPEN' ? 'Open' : value === 'RESOLVED' ? 'Resolved' : 'All'}
              </button>
            ))}
          </div>
        </div>
        {loading ? (
          <p className="text-sm text-foreground-secondary">Loading data...</p>
        ) : error ? (
          <p className="rounded-lg border border-warning bg-warning-bg p-3 text-sm text-warning-text">{error}</p>
        ) : rows.length === 0 ? (
          <p className="text-sm text-foreground-secondary">No feedback yet.</p>
        ) : (
          <ul className="divide-y divide-border">
            {rows.map((row) => (
              <li key={row.id}>
                <button
                  type="button"
                  onClick={() => openItem(row.id)}
                  className={`flex w-full flex-col gap-1 px-2 py-3 text-left hover:bg-surface-secondary ${
                    selectedId === row.id ? 'bg-primary-light' : ''
                  }`}
                >
                  <div className="flex flex-wrap items-center gap-2 text-sm">
                    <span className="font-semibold text-foreground">{typeLabel(row.itemType)}</span>
                    <span className={`rounded-full px-2 py-0.5 text-xs ${
                      row.status === 'RESOLVED'
                        ? 'bg-success-bg text-success-text'
                        : 'bg-primary-light text-primary-text'
                    }`}>
                      {statusLabel(row.status)}
                    </span>
                  </div>
                  <p className="line-clamp-2 text-sm text-foreground">{row.preview}</p>
                  <p className="text-xs text-foreground-muted">
                    {row.authorName} · {row.authorIrn} · {formatWhen(row.createdAt)}
                    {row.attachmentCount > 0 ? ` · ${row.attachmentCount} file${row.attachmentCount === 1 ? '' : 's'}` : ''}
                  </p>
                </button>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="rounded-2xl border border-border bg-surface p-4 sm:p-6">
        {!selectedId ? (
          <p className="text-sm text-foreground-secondary">Select an item to read the full message and files.</p>
        ) : detailError ? (
          <p className="rounded-lg border border-warning bg-warning-bg p-3 text-sm text-warning-text">{detailError}</p>
        ) : !detail ? (
          <p className="text-sm text-foreground-secondary">Loading data...</p>
        ) : (
          <div className="space-y-4">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <h3 className="text-base font-semibold text-foreground">{typeLabel(detail.itemType)}</h3>
                <p className="text-sm text-foreground-secondary">
                  {detail.authorName} · {detail.authorIrn}
                </p>
                <p className="text-xs text-foreground-muted">{formatWhen(detail.createdAt)}</p>
              </div>
              <div className="flex flex-wrap gap-2">
                <Button
                  disabled={updating}
                  onClick={() => patchStatus(detail.status === 'RESOLVED' ? 'OPEN' : 'RESOLVED')}
                  className="bg-primary text-white hover:bg-primary-hover"
                >
                  {detail.status === 'RESOLVED' ? 'Reopen' : 'Mark resolved'}
                </Button>
                {detail.status === 'RESOLVED' && (
                  <>
                    <Button
                      disabled={updating}
                      onClick={() => {
                        setReplyDraft(detail.lecturerResponse || '');
                        setReplyOpen(true);
                      }}
                      className="bg-secondary text-white hover:bg-secondary-hover"
                    >
                      {detail.lecturerResponse ? 'Edit reply' : 'Reply'}
                    </Button>
                    <Button
                      disabled={updating}
                      onClick={() => setConfirmDelete(true)}
                      className="bg-error text-white hover:bg-error-hover"
                    >
                      Delete
                    </Button>
                  </>
                )}
              </div>
            </div>
            <p className="whitespace-pre-wrap text-sm text-foreground">{detail.message}</p>
            {detail.lecturerResponse && (
              <div className="rounded-lg border border-border bg-surface-secondary px-3 py-2">
                <p className="text-xs font-medium text-foreground-muted">Reply to student</p>
                <p className="mt-1 whitespace-pre-wrap text-sm text-foreground">{detail.lecturerResponse}</p>
              </div>
            )}
            {detail.attachments?.length > 0 && (
              <div>
                <p className="mb-2 text-sm font-medium text-foreground">Attachments</p>
                <ul className="space-y-2">
                  {detail.attachments.map((attachment) => (
                    <li key={attachment.id}>
                      <button
                        type="button"
                        onClick={() => openAttachment(attachment)}
                        className="text-sm text-primary hover:underline"
                      >
                        {attachment.originalFilename}
                      </button>
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        )}
      </section>

      {replyOpen && (
        <ModalOverlay onBackdropClick={() => !updating && setReplyOpen(false)}>
          <form
            role="dialog"
            aria-modal="true"
            aria-labelledby="reply-feedback-title"
            className="w-full max-w-md rounded-2xl border border-border bg-surface p-6 shadow-2xl"
            onClick={(event) => event.stopPropagation()}
            onSubmit={saveReply}
          >
            <h3 id="reply-feedback-title" className="mb-1 font-semibold text-foreground">
              Reply to student
            </h3>
            <p className="mb-4 text-sm text-foreground-muted">
              This short note appears in the student’s notification bell.
            </p>
            <textarea
              required
              maxLength={500}
              rows={4}
              value={replyDraft}
              onChange={(event) => setReplyDraft(event.target.value)}
              className="w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-foreground"
              placeholder="Write a short response"
            />
            <p className="mt-1 text-right text-xs text-foreground-muted">{replyDraft.length}/500</p>
            <div className="mt-4 flex justify-end gap-2">
              <Button
                type="button"
                disabled={updating}
                onClick={() => setReplyOpen(false)}
                className="bg-surface-secondary text-foreground hover:bg-surface-tertiary"
              >
                Cancel
              </Button>
              <Button type="submit" disabled={updating} className="bg-primary text-white hover:bg-primary-hover">
                {updating ? 'Sending…' : 'Send reply'}
              </Button>
            </div>
          </form>
        </ModalOverlay>
      )}

      {confirmDelete && (
        <ModalOverlay onBackdropClick={() => !updating && setConfirmDelete(false)}>
          <div
            role="dialog"
            aria-modal="true"
            aria-labelledby="delete-feedback-title"
            className="w-full max-w-sm rounded-2xl border border-border bg-surface p-6 shadow-2xl"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-xl bg-error-bg">
              <Trash2 className="h-6 w-6 text-error" aria-hidden="true" />
            </div>
            <h3 id="delete-feedback-title" className="mb-1 text-center font-semibold text-foreground">
              Delete feedback
            </h3>
            <p className="mb-6 text-center text-sm text-foreground-muted">
              Delete this resolved item? Attachments are removed too. This cannot be undone.
            </p>
            <div className="flex gap-3">
              <button
                type="button"
                disabled={updating}
                onClick={() => setConfirmDelete(false)}
                className="flex-1 rounded-lg border border-border py-2.5 text-sm text-foreground-secondary transition-colors hover:bg-surface-secondary focus:outline-none focus:ring-2 focus:ring-primary disabled:opacity-50"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={updating}
                onClick={deleteResolved}
                className="flex-1 rounded-lg bg-error py-2.5 text-sm font-medium text-white transition-colors hover:bg-error-hover focus:outline-none focus:ring-2 focus:ring-error disabled:opacity-50"
              >
                {updating ? 'Deleting…' : 'Delete'}
              </button>
            </div>
          </div>
        </ModalOverlay>
      )}
    </div>
  );
}
