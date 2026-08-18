import { useState } from 'react';
import { X, Eye, EyeOff, Lock, CheckCircle2, AlertCircle } from 'lucide-react';
import { getChangePasswordErrors, isFormValid, validatePassword } from '../../utils/validation';
import { readFriendlyAuthError, toFriendlyError } from '../../utils/apiError';

export default function ChangePasswordModal({ isOpen, onClose, user, token: propToken }) {
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showCurrent, setShowCurrent] = useState(false);
  const [showNew, setShowNew] = useState(false);
  const [saved, setSaved] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  if (!isOpen) return null;

  const fieldErrors = getChangePasswordErrors(currentPassword, newPassword, confirmPassword);
  const canSave = isFormValid(fieldErrors);
  const passwordsMatch = confirmPassword && !fieldErrors.confirmPassword;

  const handleSave = async () => {
    if (!canSave) {
      return;
    }

    setError('');
    setLoading(true);
    try {
      const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';

      const token =
        propToken ||
        localStorage.getItem('token') ||
        sessionStorage.getItem('token') ||
        user?.accessToken;

      if (!token) {
        setError('No authentication token found. Please login again.');
        setLoading(false);
        return;
      }

      const response = await fetch(`${API_BASE}/api/users/change-password`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          currentPassword,
          newPassword,
        }),
      });

      if (!response.ok) {
        throw new Error(await readFriendlyAuthError(response, 'change-password'));
      }

      await response.json().catch(() => ({}));

      setSaved(true);
      setTimeout(() => {
        setSaved(false);
        setCurrentPassword('');
        setNewPassword('');
        setConfirmPassword('');
        setError('');
        onClose();
      }, 1500);
    } catch (saveError) {
      console.error('Change password error:', saveError);
      setError(toFriendlyError(saveError, 'change-password'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4">
      <div className="w-full max-w-md overflow-hidden rounded-3xl bg-surface shadow-2xl">
        <div className="flex items-center justify-between px-6 py-4">
          <div>
            <h2 className="text-lg font-semibold text-foreground">Change Password</h2>
          </div>
          <button
            onClick={onClose}
            className="text-foreground-muted transition hover:text-foreground-secondary"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="space-y-5 px-6 py-6">
          {error && (
            <div className="flex items-start gap-2 rounded-lg bg-error-bg p-3 text-sm text-error">
              <AlertCircle className="h-4 w-4 flex-shrink-0 mt-0.5" />
              <span>{error}</span>
            </div>
          )}

          <div className="space-y-4">
            <div>
              <label className="mb-2 block text-xs font-medium text-foreground-muted">
                Current Password
              </label>
              <div className="relative">
                <Lock className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-foreground-muted" />
                <input
                  type={showCurrent ? 'text' : 'password'}
                  value={currentPassword}
                  onChange={(e) => {
                    setCurrentPassword(e.target.value);
                    setError('');
                  }}
                  placeholder="Enter current password"
                  className={`w-full rounded-2xl bg-surface-secondary px-10 py-3 text-sm text-foreground outline-none transition focus:ring-2 focus:ring-primary/30 ${
                    fieldErrors.currentPassword ? 'ring-1 ring-error/30' : ''
                  }`}
                />
                <button
                  type="button"
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-foreground-muted hover:text-foreground-secondary"
                  onClick={() => setShowCurrent(!showCurrent)}
                >
                  {showCurrent ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </button>
              </div>
              {fieldErrors.currentPassword && (
                <p className="mt-1 text-xs text-error">{fieldErrors.currentPassword}</p>
              )}
            </div>

            <div>
              <label className="mb-2 block text-xs font-medium text-foreground-muted">
                New Password
              </label>
              <div className="relative">
                <Lock className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-foreground-muted" />
                <input
                  type={showNew ? 'text' : 'password'}
                  value={newPassword}
                  onChange={(e) => {
                    setNewPassword(e.target.value);
                    setError('');
                  }}
                  placeholder="Enter new password"
                  className={`w-full rounded-2xl bg-surface-secondary px-10 py-3 text-sm text-foreground outline-none transition focus:ring-2 focus:ring-primary/30 ${
                    fieldErrors.newPassword ? 'ring-1 ring-error/30' : ''
                  }`}
                />
                <button
                  type="button"
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-foreground-muted hover:text-foreground-secondary"
                  onClick={() => setShowNew(!showNew)}
                >
                  {showNew ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </button>
              </div>
              {fieldErrors.newPassword && (
                <p className="mt-1 text-xs text-error">{fieldErrors.newPassword}</p>
              )}
              {!fieldErrors.newPassword && newPassword && !validatePassword(newPassword) && (
                <p className="mt-1 text-xs text-success">✓ Password is valid</p>
              )}
            </div>

            <div>
              <label className="mb-2 block text-xs font-medium text-foreground-muted">
                Confirm New Password
              </label>
              <input
                type="password"
                value={confirmPassword}
                onChange={(e) => {
                  setConfirmPassword(e.target.value);
                  setError('');
                }}
                placeholder="Repeat new password"
                className={`w-full rounded-2xl bg-surface-secondary px-4 py-3 text-sm text-foreground outline-none transition focus:ring-2 focus:ring-primary/30 ${
                  fieldErrors.confirmPassword
                    ? 'ring-1 ring-error/30'
                    : passwordsMatch && confirmPassword
                    ? 'ring-1 ring-success/20'
                    : ''
                }`}
              />
              {fieldErrors.confirmPassword && (
                <p className="mt-1 text-xs text-error">{fieldErrors.confirmPassword}</p>
              )}
              {passwordsMatch && confirmPassword && !fieldErrors.confirmPassword && (
                <p className="mt-1 text-xs text-success">✓ Passwords match</p>
              )}
            </div>
          </div>

          <button
            type="button"
            disabled={loading || saved || !canSave}
            onClick={handleSave}
            className="flex w-full items-center justify-center gap-2 rounded-2xl bg-primary px-4 py-3 text-sm font-semibold text-white transition hover:bg-primary disabled:cursor-not-allowed disabled:opacity-50"
          >
            {saved ? (
              <>
                <CheckCircle2 className="h-4 w-4" /> Password Updated
              </>
            ) : loading ? (
              'Updating...'
            ) : (
              'Update Password'
            )}
          </button>
        </div>
      </div>
    </div>
  );
}
