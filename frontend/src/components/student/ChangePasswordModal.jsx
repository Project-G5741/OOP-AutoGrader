import { useState } from 'react';
import { X, Eye, EyeOff, Lock, CheckCircle2, AlertCircle } from 'lucide-react';
import { getChangePasswordErrors, isFormValid, validatePassword } from '../../utils/validation';
import { readApiErrorMessage } from '../../utils/apiError';

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
        throw new Error(await readApiErrorMessage(response, 'Failed to change password'));
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
      setError(saveError.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4">
      <div className="w-full max-w-md overflow-hidden rounded-3xl border border-gray-200/80 bg-white shadow-2xl dark:border-gray-700 dark:bg-[#161b22]">
        <div className="flex items-center justify-between border-b border-gray-200/80 px-6 py-4 dark:border-gray-800">
          <div>
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white">Change Password</h2>
            <p className="text-sm text-gray-500 dark:text-gray-400">Update your account password securely.</p>
          </div>
          <button
            onClick={onClose}
            className="text-gray-500 transition hover:text-gray-700 dark:text-gray-400 dark:hover:text-white"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="space-y-5 px-6 py-6">
          {error && (
            <div className="flex items-start gap-2 rounded-lg bg-red-50 p-3 text-sm text-red-600 dark:bg-red-900/20 dark:text-red-400">
              <AlertCircle className="h-4 w-4 flex-shrink-0 mt-0.5" />
              <span>{error}</span>
            </div>
          )}

          <div className="space-y-4">
            <div>
              <label className="mb-2 block text-xs font-medium text-gray-500 dark:text-gray-400">
                Current Password
              </label>
              <div className="relative">
                <Lock className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
                <input
                  type={showCurrent ? 'text' : 'password'}
                  value={currentPassword}
                  onChange={(e) => {
                    setCurrentPassword(e.target.value);
                    setError('');
                  }}
                  placeholder="Enter current password"
                  className={`w-full rounded-2xl border bg-white px-10 py-3 text-sm text-gray-900 outline-none transition focus:border-purple-500 dark:bg-[#0d1117] dark:text-white ${
                    fieldErrors.currentPassword ? 'border-red-500' : 'border-gray-200 dark:border-gray-700'
                  }`}
                />
                <button
                  type="button"
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-500"
                  onClick={() => setShowCurrent(!showCurrent)}
                >
                  {showCurrent ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </button>
              </div>
              {fieldErrors.currentPassword && (
                <p className="mt-1 text-xs text-red-500">{fieldErrors.currentPassword}</p>
              )}
            </div>

            <div>
              <label className="mb-2 block text-xs font-medium text-gray-500 dark:text-gray-400">
                New Password
              </label>
              <div className="relative">
                <Lock className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
                <input
                  type={showNew ? 'text' : 'password'}
                  value={newPassword}
                  onChange={(e) => {
                    setNewPassword(e.target.value);
                    setError('');
                  }}
                  placeholder="Enter new password"
                  className={`w-full rounded-2xl border bg-white px-10 py-3 text-sm text-gray-900 outline-none transition focus:border-purple-500 dark:bg-[#0d1117] dark:text-white ${
                    fieldErrors.newPassword ? 'border-red-500' : 'border-gray-200 dark:border-gray-700'
                  }`}
                />
                <button
                  type="button"
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-500"
                  onClick={() => setShowNew(!showNew)}
                >
                  {showNew ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </button>
              </div>
              {fieldErrors.newPassword && (
                <p className="mt-1 text-xs text-red-500">{fieldErrors.newPassword}</p>
              )}
              {!fieldErrors.newPassword && newPassword && !validatePassword(newPassword) && (
                <p className="mt-1 text-xs text-green-500">✓ Password is valid</p>
              )}
            </div>

            <div>
              <label className="mb-2 block text-xs font-medium text-gray-500 dark:text-gray-400">
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
                className={`w-full rounded-2xl border bg-white px-4 py-3 text-sm text-gray-900 outline-none transition focus:border-purple-500 dark:bg-[#0d1117] dark:text-white ${
                  fieldErrors.confirmPassword
                    ? 'border-red-500'
                    : passwordsMatch && confirmPassword
                    ? 'border-green-500'
                    : 'border-gray-200 dark:border-gray-700'
                }`}
              />
              {fieldErrors.confirmPassword && (
                <p className="mt-1 text-xs text-red-500">{fieldErrors.confirmPassword}</p>
              )}
              {passwordsMatch && confirmPassword && !fieldErrors.confirmPassword && (
                <p className="mt-1 text-xs text-green-500">✓ Passwords match</p>
              )}
            </div>
          </div>

          <button
            type="button"
            disabled={loading || saved || !canSave}
            onClick={handleSave}
            className="flex w-full items-center justify-center gap-2 rounded-2xl bg-purple-600 px-4 py-3 text-sm font-semibold text-white transition hover:bg-purple-500 disabled:cursor-not-allowed disabled:opacity-50"
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
