import { useState } from 'react';
import { X, Eye, EyeOff, Lock, CheckCircle2, AlertCircle } from 'lucide-react';

export default function ChangePasswordModal({ isOpen, onClose, user, token: propToken }) {
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showCurrent, setShowCurrent] = useState(false);
  const [showNew, setShowNew] = useState(false);
  const [saved, setSaved] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [fieldErrors, setFieldErrors] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  });

  if (!isOpen) return null;

  const passwordsMatch = newPassword && confirmPassword && newPassword === confirmPassword;
  const passwordsMismatch = newPassword && confirmPassword && newPassword !== confirmPassword;
  
  // Validate on change
  const validateField = (field, value) => {
    let errorMsg = '';
    if (field === 'newPassword') {
      if (value && value.length < 6) {
        errorMsg = 'Password must be at least 6 characters';
      } else if (value && value.length > 100) {
        errorMsg = 'Password must be less than 100 characters';
      }
    }
    if (field === 'confirmPassword') {
      if (value && value !== newPassword) {
        errorMsg = 'Passwords do not match';
      }
    }
    setFieldErrors(prev => ({ ...prev, [field]: errorMsg }));
  };

  const handleFieldChange = (field, value) => {
    if (field === 'currentPassword') {
      setCurrentPassword(value);
      setFieldErrors(prev => ({ ...prev, currentPassword: '' }));
    }
    if (field === 'newPassword') {
      setNewPassword(value);
      validateField('newPassword', value);
      // Re-validate confirm password when new password changes
      if (confirmPassword) {
        validateField('confirmPassword', confirmPassword);
      }
    }
    if (field === 'confirmPassword') {
      setConfirmPassword(value);
      validateField('confirmPassword', value);
    }
    // Clear general error on any change
    setError('');
  };

  const handleSave = async () => {
    // Clear previous errors
    setError('');
    setFieldErrors({ currentPassword: '', newPassword: '', confirmPassword: '' });

    // Validate all fields
    let hasError = false;
    
    if (!currentPassword) {
      setFieldErrors(prev => ({ ...prev, currentPassword: 'Current password is required' }));
      hasError = true;
    }
    
    if (!newPassword) {
      setFieldErrors(prev => ({ ...prev, newPassword: 'New password is required' }));
      hasError = true;
    } else if (newPassword.length < 6) {
      setFieldErrors(prev => ({ ...prev, newPassword: 'Password must be at least 6 characters' }));
      hasError = true;
    }
    
    if (!confirmPassword) {
      setFieldErrors(prev => ({ ...prev, confirmPassword: 'Please confirm your password' }));
      hasError = true;
    } else if (newPassword !== confirmPassword) {
      setFieldErrors(prev => ({ ...prev, confirmPassword: 'Passwords do not match' }));
      hasError = true;
    }

    if (hasError) return;

    setLoading(true);
    try {
      const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';
      
      const token = propToken || 
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
          'Authorization': `Bearer ${token}`,
        },
        body: JSON.stringify({
          currentPassword: currentPassword,
          newPassword: newPassword,
        }),
      });

      const data = await response.json();

      if (!response.ok) {
        // Hiển thị lỗi từ backend
        throw new Error(data.message || data.detail || 'Failed to change password');
      }

      // Success
      setSaved(true);
      setTimeout(() => {
        setSaved(false);
        setCurrentPassword('');
        setNewPassword('');
        setConfirmPassword('');
        setError('');
        setFieldErrors({ currentPassword: '', newPassword: '', confirmPassword: '' });
        onClose();
      }, 1500);
      
    } catch (error) {
      console.error('Change password error:', error);
      setError(error.message);
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
          {/* Error message */}
          {error && (
            <div className="flex items-start gap-2 rounded-lg bg-red-50 p-3 text-sm text-red-600 dark:bg-red-900/20 dark:text-red-400">
              <AlertCircle className="h-4 w-4 flex-shrink-0 mt-0.5" />
              <span>{error}</span>
            </div>
          )}

          <div className="space-y-4">
            {/* Current Password */}
            <div>
              <label className="mb-2 block text-xs font-medium text-gray-500 dark:text-gray-400">
                Current Password
              </label>
              <div className="relative">
                <Lock className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
                <input
                  type={showCurrent ? 'text' : 'password'}
                  value={currentPassword}
                  onChange={(e) => handleFieldChange('currentPassword', e.target.value)}
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

            {/* New Password */}
            <div>
              <label className="mb-2 block text-xs font-medium text-gray-500 dark:text-gray-400">
                New Password
              </label>
              <div className="relative">
                <Lock className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
                <input
                  type={showNew ? 'text' : 'password'}
                  value={newPassword}
                  onChange={(e) => handleFieldChange('newPassword', e.target.value)}
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
              {!fieldErrors.newPassword && newPassword && newPassword.length >= 6 && (
                <p className="mt-1 text-xs text-green-500">✓ Password is valid</p>
              )}
            </div>

            {/* Confirm Password */}
            <div>
              <label className="mb-2 block text-xs font-medium text-gray-500 dark:text-gray-400">
                Confirm New Password
              </label>
              <input
                type="password"
                value={confirmPassword}
                onChange={(e) => handleFieldChange('confirmPassword', e.target.value)}
                placeholder="Repeat new password"
                className={`w-full rounded-2xl border bg-white px-4 py-3 text-sm text-gray-900 outline-none transition focus:border-purple-500 dark:bg-[#0d1117] dark:text-white ${
                  fieldErrors.confirmPassword ? 'border-red-500' : 
                  passwordsMatch && confirmPassword ? 'border-green-500' : 
                  'border-gray-200 dark:border-gray-700'
                }`}
              />
              {fieldErrors.confirmPassword && (
                <p className="mt-1 text-xs text-red-500">{fieldErrors.confirmPassword}</p>
              )}
              {passwordsMatch && confirmPassword && (
                <p className="mt-1 text-xs text-green-500">✓ Passwords match</p>
              )}
            </div>
          </div>

          <button
            type="button"
            disabled={loading || saved}
            onClick={handleSave}
            className="flex w-full items-center justify-center gap-2 rounded-2xl bg-purple-600 px-4 py-3 text-sm font-semibold text-white transition hover:bg-purple-500 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {saved ? (
              <><CheckCircle2 className="h-4 w-4" /> Password Updated</>
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