import { useRef, useState } from 'react';
import { Upload, X } from 'lucide-react';
import ModalOverlay from '../ui/ModalOverlay';
import Button from '../ui/Button';
import { apiFetch } from '../../utils/apiFetch';
import { authHeaders } from '../../utils/authHeaders';
import { readFriendlyApiError, toFriendlyError } from '../../utils/apiError';
import { useToast } from '../ui/Toast';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';
const ALLOWED_SUFFIXES = ['.png', '.jpg', '.jpeg', '.java', '.mmd'];
const MAX_FILES = 5;
const MAX_UPLOAD_BYTES = 25 * 1024 * 1024;

function identityFromUser(user) {
  const irn = user?.irn || user?.studentCode || user?.lecturerCode || '';
  const name = user?.name || user?.fullName || '';
  return { irn, name };
}

function suffixAllowed(filename) {
  const lower = String(filename || '').toLowerCase();
  return ALLOWED_SUFFIXES.some((suffix) => lower.endsWith(suffix));
}

export default function FeedbackForm({ user, onClose }) {
  const showToast = useToast();
  const { irn, name } = identityFromUser(user);
  const inputRef = useRef(null);
  const [itemType, setItemType] = useState('BUG');
  const [message, setMessage] = useState('');
  const [files, setFiles] = useState([]);
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [dragging, setDragging] = useState(false);

  const addFiles = (incoming) => {
    const next = [...files];
    for (const file of incoming) {
      if (!suffixAllowed(file.name)) {
        setError('Only .png, .jpg, .java, and .mmd files are allowed.');
        return;
      }
      if (next.length >= MAX_FILES) {
        setError('You can attach at most 5 files.');
        return;
      }
      const nextTotal = next.reduce((sum, existing) => sum + existing.size, 0) + file.size;
      if (nextTotal > MAX_UPLOAD_BYTES) {
        setError('Attachments must be at most 25 MB for one upload.');
        return;
      }
      next.push(file);
    }
    setError(null);
    setFiles(next);
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (!message.trim()) {
      setError('Message is required.');
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      const body = new FormData();
      body.append('type', itemType);
      body.append('message', message.trim());
      files.forEach((file) => body.append('files', file));
      const response = await apiFetch(`${API_BASE}/api/feedback/create`, {
        method: 'POST',
        headers: authHeaders(),
        body,
      });
      if (!response.ok) {
        throw new Error(await readFriendlyApiError(response, 'feedback'));
      }
      showToast({ type: 'success', message: 'Feedback sent.' });
      onClose();
    } catch (err) {
      setError(toFriendlyError(err, 'feedback'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <ModalOverlay onBackdropClick={submitting ? undefined : onClose}>
      <form
        onClick={(event) => event.stopPropagation()}
        onSubmit={handleSubmit}
        className="flex max-h-[90vh] w-full max-w-lg flex-col overflow-hidden rounded-2xl border border-border bg-surface shadow-lg"
      >
        <div className="flex items-start justify-between gap-3 border-b border-border px-5 py-4">
          <div>
            <h2 className="text-lg font-semibold text-foreground">Send feedback</h2>
            <p className="mt-1 text-sm text-foreground-secondary">Bug report or contribution. Your IRN and name are saved from your account.</p>
          </div>
          <button
            type="button"
            aria-label="Close feedback form"
            onClick={onClose}
            disabled={submitting}
            className="inline-flex h-9 w-9 items-center justify-center rounded-lg text-foreground-secondary hover:bg-surface-secondary"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        <div className="min-h-0 flex-1 space-y-4 overflow-y-auto px-5 py-4">
          <fieldset className="space-y-2">
            <legend className="text-sm font-medium text-foreground">Type</legend>
            <div className="flex gap-2">
              {[
                { id: 'BUG', label: 'Bug report' },
                { id: 'CONTRIBUTION', label: 'Contribution' },
              ].map((option) => (
                <label
                  key={option.id}
                  className={`flex flex-1 cursor-pointer items-center justify-center rounded-lg border px-3 py-2 text-sm font-medium ${
                    itemType === option.id
                      ? 'border-primary bg-primary-light text-primary-text'
                      : 'border-border bg-surface text-foreground-secondary hover:bg-surface-secondary'
                  }`}
                >
                  <input
                    type="radio"
                    name="feedback-type"
                    value={option.id}
                    checked={itemType === option.id}
                    onChange={() => setItemType(option.id)}
                    className="sr-only"
                  />
                  {option.label}
                </label>
              ))}
            </div>
          </fieldset>

          <div className="grid grid-cols-2 gap-3">
            <label className="text-sm">
              <span className="mb-1 block font-medium text-foreground-muted">IRN</span>
              <input
                readOnly
                disabled
                tabIndex={-1}
                value={irn}
                className="w-full cursor-not-allowed rounded-lg border border-border bg-surface-tertiary px-3 py-2 text-sm text-foreground-muted"
              />
            </label>
            <label className="text-sm">
              <span className="mb-1 block font-medium text-foreground-muted">Name</span>
              <input
                readOnly
                disabled
                tabIndex={-1}
                value={name}
                className="w-full cursor-not-allowed rounded-lg border border-border bg-surface-tertiary px-3 py-2 text-sm text-foreground-muted"
              />
            </label>
          </div>

          <label className="block text-sm">
            <span className="mb-1 block font-medium text-foreground">Message</span>
            <textarea
              required
              maxLength={4000}
              rows={5}
              value={message}
              onChange={(event) => setMessage(event.target.value)}
              className="w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-foreground"
              placeholder="Describe the bug or the idea"
            />
          </label>

          <div>
            <p className="mb-1 text-sm font-medium text-foreground">Attachments (optional)</p>
            <div
              onDragOver={(event) => {
                event.preventDefault();
                setDragging(true);
              }}
              onDragLeave={() => setDragging(false)}
              onDrop={(event) => {
                event.preventDefault();
                setDragging(false);
                addFiles(Array.from(event.dataTransfer.files || []));
              }}
              className={`flex cursor-pointer flex-col items-center justify-center rounded-xl border border-dashed px-4 py-6 text-center ${
                dragging ? 'border-primary bg-primary-light' : 'border-border bg-surface-secondary'
              }`}
              onClick={() => inputRef.current?.click()}
            >
              <Upload className="mb-2 h-5 w-5 text-primary" />
              <p className="text-sm text-foreground">Drop .png, .jpg, .java, or .mmd</p>
              <p className="mt-1 text-xs text-foreground-muted">Up to 5 files, 25 MB total</p>
              <input
                ref={inputRef}
                type="file"
                multiple
                accept=".png,.jpg,.jpeg,.java,.mmd"
                className="hidden"
                onChange={(event) => {
                  addFiles(Array.from(event.target.files || []));
                  event.target.value = '';
                }}
              />
            </div>
            {files.length > 0 && (
              <ul className="mt-2 space-y-1">
                {files.map((file, index) => (
                  <li key={`${file.name}-${index}`} className="flex items-center justify-between rounded-lg bg-surface-secondary px-3 py-2 text-sm">
                    <span className="truncate text-foreground">{file.name}</span>
                    <button
                      type="button"
                      aria-label={`Remove ${file.name}`}
                      onClick={() => setFiles(files.filter((_, i) => i !== index))}
                      className="ml-2 text-foreground-secondary hover:text-error"
                    >
                      <X className="h-4 w-4" />
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </div>

          {error && (
            <p className="rounded-lg border border-error bg-error-bg px-3 py-2 text-sm text-error-text">{error}</p>
          )}
        </div>

        <div className="flex justify-end gap-2 border-t border-border px-5 py-4">
          <Button
            type="button"
            onClick={onClose}
            disabled={submitting}
            className="bg-surface-secondary text-foreground hover:bg-surface-tertiary"
          >
            Cancel
          </Button>
          <Button type="submit" disabled={submitting} className="bg-primary text-white hover:bg-primary-hover">
            {submitting ? 'Sending…' : 'Send'}
          </Button>
        </div>
      </form>
    </ModalOverlay>
  );
}
