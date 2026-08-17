import React, { useState } from 'react';
import { Lock, Eye, EyeOff, CheckCircle2 } from 'lucide-react';
import AppLogo from '../components/ui/AppLogo';
import './LoginUI.css';
import ThemeToggle from '../components/ThemeToggle';
import { getResetPasswordErrors, isFormValid } from '../utils/validation';
import { readFriendlyAuthError, toFriendlyError } from '../utils/apiError';

export default function ResetPasswordUI({ token, onComplete }) {
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showNew, setShowNew] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [formError, setFormError] = useState('');
  const [success, setSuccess] = useState(false);
  const [hasAttemptedSubmit, setHasAttemptedSubmit] = useState(false);
  const [touchedFields, setTouchedFields] = useState({ newPassword: false, confirmPassword: false });

  const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';

  const rawFieldErrors = getResetPasswordErrors(newPassword, confirmPassword);
  const fieldErrors = {
    newPassword: hasAttemptedSubmit || touchedFields.newPassword ? rawFieldErrors.newPassword : '',
    confirmPassword: hasAttemptedSubmit || touchedFields.confirmPassword ? rawFieldErrors.confirmPassword : '',
  };
  const canSubmit = isFormValid(rawFieldErrors);

  const handleFieldBlur = (field) => {
    setTouchedFields((prev) => ({ ...prev, [field]: true }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setHasAttemptedSubmit(true);
    setTouchedFields({ newPassword: true, confirmPassword: true });
    setFormError('');

    if (!canSubmit) {
      return;
    }

    setIsLoading(true);
    try {
      const response = await fetch(`${API_BASE}/api/auth/reset-password`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          token,
          newPassword,
          confirmPassword,
        }),
      });

      if (!response.ok) {
        throw new Error(await readFriendlyAuthError(response, 'reset-password'));
      }

      setSuccess(true);
      setTimeout(() => {
        onComplete?.();
      }, 2000);
    } catch (err) {
      setFormError(toFriendlyError(err, 'reset-password'));
    } finally {
      setIsLoading(false);
    }
  };

  if (!token) {
    return (
      <div className="login-root">
        <div className="login-bg">
          <div className="login-card-wrapper">
            <div className="card">
              <p className="info-text">Invalid reset link. Please request a new password reset from the login page.</p>
              <button type="button" className="primary-btn" onClick={onComplete}>
                Back to sign in
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="login-root">
      <div className="login-bg">
        <ThemeToggle className="theme-toggle" />

        <div className="login-card-wrapper">
          <div className="logo-title">
            <AppLogo variant="login" />
            <h1 className="main-title">Set new password</h1>
            <p className="subtitle">
              {success ? 'Password updated successfully' : 'Choose a new password for your account'}
            </p>
          </div>

          <div className="card">
            {success ? (
              <div className="login-form" style={{ textAlign: 'center' }}>
                <CheckCircle2 className="logo-icon success-icon" />
                <p className="info-text">Redirecting you to sign in...</p>
              </div>
            ) : (
              <form onSubmit={handleSubmit} className="login-form">
                <div className="form-group">
                  <label className="field-label">New password</label>
                  <div className="input-wrapper">
                    <Lock className="input-icon" />
                    <input
                      type={showNew ? 'text' : 'password'}
                      value={newPassword}
                      onChange={(e) => {
                        setNewPassword(e.target.value);
                        setFormError('');
                      }}
                      onBlur={() => handleFieldBlur('newPassword')}
                      placeholder="At least 6 characters"
                      className={`input-field${fieldErrors.newPassword ? ' input-error' : ''}`}
                      autoComplete="new-password"
                    />
                    <button
                      type="button"
                      onClick={() => setShowNew(!showNew)}
                      className="password-toggle"
                      aria-label="Toggle password visibility"
                    >
                      {showNew ? <EyeOff className="toggle-icon" /> : <Eye className="toggle-icon" />}
                    </button>
                  </div>
                  {fieldErrors.newPassword && (
                    <p className="error-text">{fieldErrors.newPassword}</p>
                  )}
                </div>

                <div className="form-group">
                  <label className="field-label">Confirm password</label>
                  <div className="input-wrapper">
                    <Lock className="input-icon" />
                    <input
                      type={showConfirm ? 'text' : 'password'}
                      value={confirmPassword}
                      onChange={(e) => {
                        setConfirmPassword(e.target.value);
                        setFormError('');
                      }}
                      onBlur={() => handleFieldBlur('confirmPassword')}
                      placeholder="Re-enter your password"
                      className={`input-field${fieldErrors.confirmPassword ? ' input-error' : ''}`}
                      autoComplete="new-password"
                    />
                    <button
                      type="button"
                      onClick={() => setShowConfirm(!showConfirm)}
                      className="password-toggle"
                      aria-label="Toggle confirm password visibility"
                    >
                      {showConfirm ? <EyeOff className="toggle-icon" /> : <Eye className="toggle-icon" />}
                    </button>
                  </div>
                  {fieldErrors.confirmPassword && (
                    <p className="error-text">{fieldErrors.confirmPassword}</p>
                  )}
                </div>

                {formError && (
                  <p className="error-text" style={{ marginBottom: '0.75rem' }}>
                    {formError}
                  </p>
                )}

                <button type="submit" className="primary-btn" disabled={isLoading || !canSubmit}>
                  {isLoading ? 'Saving...' : 'Reset password'}
                </button>
              </form>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
