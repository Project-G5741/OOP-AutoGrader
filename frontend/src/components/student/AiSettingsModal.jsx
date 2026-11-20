import { useEffect, useState } from 'react';
import { X, KeyRound, Loader2 } from 'lucide-react';
import { apiFetch } from '../../utils/apiFetch';
import { readFriendlyApiError } from '../../utils/apiError';
import { getAccessToken } from '../../utils/authRoutes';
import ModalOverlay from '../ui/ModalOverlay';
import { useToast } from '../ui/Toast';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';

function alertMessage(label) {
  if (label === '50%') return 'Token usage reached 50% of your budget.';
  if (label === '80%') return 'Token usage reached 80% of your budget.';
  if (label === '90%') return 'Token budget exhausted (90%). New hints are paused.';
  return `Token usage alert: ${label}`;
}

export default function AiSettingsModal({ isOpen, onClose, onStatusChange }) {
  const showToast = useToast();
  const [keyInput, setKeyInput] = useState('');
  const [token, setToken] = useState(null);
  const [loading, setLoading] = useState(false);
  const [loadingStatus, setLoadingStatus] = useState(false);
  const [error, setError] = useState('');

  const loadStatus = async () => {
    setLoadingStatus(true);
    try {
      const access = getAccessToken();
      const res = await apiFetch(`${API_BASE}/api/students/ai/token`, {
        authHandling: 'self',
        headers: { Authorization: `Bearer ${access}` },
      });
      if (!res.ok) {
        throw new Error(await readFriendlyApiError(res, 'read'));
      }
      const data = await res.json();
      setToken(data);
      onStatusChange?.(Boolean(data?.connected));
    } catch (err) {
      setError(err.message || 'Could not load token status.');
    } finally {
      setLoadingStatus(false);
    }
  };

  useEffect(() => {
    if (!isOpen) return;
    setKeyInput('');
    setError('');
    setLoading(false);
    loadStatus();
  }, [isOpen]);

  if (!isOpen) return null;

  const showAlerts = (alerts) => {
    (alerts || []).forEach((label) => {
      showToast({
        message: alertMessage(label),
        type: label === '90%' ? 'error' : 'warning',
      });
    });
  };

  const handleConnect = async () => {
    const next = keyInput.trim();
    if (!next) {
      setError('Paste your Gemini API key to import.');
      return;
    }
    setLoading(true);
    setError('');
    try {
      const access = getAccessToken();
      if (!access) {
        throw new Error('Please sign in again.');
      }
      const res = await apiFetch(`${API_BASE}/api/students/ai/connect`, {
        method: 'POST',
        authHandling: 'self',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${access}`,
        },
        body: JSON.stringify({ apiKey: next }),
      });
      if (!res.ok) {
        // Prefer backend reason (invalid key / model / timeout) over generic "Server Busy".
        let detail = '';
        try {
          const payload = await res.clone().json();
          if (typeof payload?.message === 'string' && payload.message.trim()) {
            detail = payload.message.trim();
          }
        } catch {
          /* fall through */
        }
        throw new Error(detail || (await readFriendlyApiError(res, 'save')));
      }
      const data = await res.json();
      setToken(data);
      setKeyInput('');
      onStatusChange?.(true);
      showAlerts(data?.alerts);
      showToast({ message: 'Gemini key saved on the server (encrypted).', type: 'success' });
    } catch (err) {
      setError(err.message || 'Could not import Gemini key.');
      showToast({ message: err.message || 'Could not import Gemini key.', type: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const handleDisconnect = async () => {
    setLoading(true);
    setError('');
    try {
      const access = getAccessToken();
      const res = await apiFetch(`${API_BASE}/api/students/ai/connect`, {
        method: 'DELETE',
        authHandling: 'self',
        headers: { Authorization: `Bearer ${access}` },
      });
      if (!res.ok) {
        throw new Error(await readFriendlyApiError(res, 'save'));
      }
      const data = await res.json();
      setToken(data);
      setKeyInput('');
      onStatusChange?.(false);
      showToast({ message: 'Gemini key removed from the server.', type: 'success' });
    } catch (err) {
      setError(err.message || 'Could not remove Gemini key.');
      showToast({ message: err.message || 'Could not remove Gemini key.', type: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const usagePercent = token?.usagePercent ?? 0;
  const exhausted = Boolean(token?.exhausted);

  return (
    <ModalOverlay onBackdropClick={onClose}>
      <div
        className="relative w-full max-w-lg rounded-2xl border border-border bg-surface p-6 shadow-stage-2"
        onClick={(event) => event.stopPropagation()}
      >
        <button
          type="button"
          onClick={onClose}
          className="absolute right-4 top-4 rounded-lg p-1 text-foreground-muted hover:bg-surface-secondary"
          aria-label="Close"
        >
          <X className="h-5 w-5" />
        </button>
        <div className="mb-4 flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-lg border border-border bg-surface-secondary">
            <KeyRound className="h-5 w-5 text-primary" />
          </div>
          <div>
            <h2 className="text-lg font-semibold text-foreground">Import API Key</h2>
          </div>
        </div>

        <div className="text-sm text-foreground-muted">
          <p>
            Status:{' '}
            <span className="font-medium text-foreground">
              {loadingStatus
                ? 'Checking…'
                : token?.connected
                  ? `Imported (${token.keyHint})`
                  : 'Not configured'}
            </span>
          </p>
        </div>

        <div className="mt-5 rounded-xl border border-border bg-surface-secondary/40 p-4">
          <div className="flex items-center justify-between gap-3 text-sm">
            <p className="font-medium text-foreground">Token budget</p>
            <p className="text-foreground-muted tabular-nums">
              {token ? `${token.tokensUsed} / ${token.tokenBudget}` : '—'} ({usagePercent}%)
            </p>
          </div>
          <div className="mt-3 h-2 overflow-hidden rounded-full bg-surface-tertiary">
            <div
              className={`h-full rounded-full transition-all ${
                exhausted ? 'bg-error' : usagePercent >= 80 ? 'bg-warning' : 'bg-primary'
              }`}
              style={{ width: `${Math.min(100, usagePercent)}%` }}
            />
          </div>
          {exhausted && (
            <p className="mt-2 text-sm font-medium text-error-text">
              Token budget exhausted (90%). New Conceptual hints are paused.
            </p>
          )}
        </div>

        <label className="mt-5 block text-sm font-medium text-foreground" htmlFor="gemini-key">
          Gemini API key
        </label>
        <input
          id="gemini-key"
          type="password"
          autoComplete="off"
          value={keyInput}
          onChange={(e) => setKeyInput(e.target.value)}
          placeholder={token?.connected ? 'Paste a new key to replace' : 'Paste key to import'}
          className="mt-2 w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-foreground"
        />
        {error && <p className="mt-2 text-sm text-error-text">{error}</p>}

        <div className="mt-6 flex flex-wrap justify-end gap-2">
          {token?.connected && (
            <button
              type="button"
              disabled={loading}
              onClick={handleDisconnect}
              className="rounded-lg border border-border px-4 py-2 text-sm font-medium text-foreground disabled:opacity-60"
            >
              Remove key
            </button>
          )}
          <button
            type="button"
            disabled={loading}
            onClick={handleConnect}
            className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground disabled:opacity-60"
          >
            {loading && <Loader2 className="h-4 w-4 animate-spin" />}
            Import / Replace
          </button>
        </div>
      </div>
    </ModalOverlay>
  );
}
