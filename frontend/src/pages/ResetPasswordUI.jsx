import React, { useState } from 'react';
import { Moon, Sun, BarChart3, Lock, Eye, EyeOff, CheckCircle2 } from 'lucide-react';
import './LoginUI.css';

export default function ResetPasswordUI({ token, onComplete }) {
  const [isDark, setIsDark] = useState(true);
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showNew, setShowNew] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);

  const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';

  const passwordsMatch = newPassword && confirmPassword && newPassword === confirmPassword;
  const passwordsMismatch = newPassword && confirmPassword && newPassword !== confirmPassword;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!newPassword || newPassword.length < 6) {
      setError('Password must be at least 6 characters.');
      return;
    }
    if (newPassword.length > 100) {
      setError('Password must be less than 100 characters.');
      return;
    }
    if (!passwordsMatch) {
      setError('Passwords do not match.');
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
        const text = await response.text();
        throw new Error(text || 'Unable to reset password. The link may be invalid or expired.');
      }

      setSuccess(true);
      setTimeout(() => {
        onComplete?.();
      }, 2000);
    } catch (err) {
      setError(err.message || 'Unable to reset password. Please request a new link.');
    } finally {
      setIsLoading(false);
    }
  };

  if (!token) {
    return (
      <div className={isDark ? 'login-root dark' : 'login-root'}>
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
    <div className={isDark ? 'login-root dark' : 'login-root'}>
      <div className="login-bg">
        <button
          onClick={() => setIsDark(!isDark)}
          className="theme-toggle"
          type="button"
        >
          <div className="theme-left">
            {isDark ? <Moon className="icon" /> : <Sun className="icon" />}
            <span>{isDark ? 'Dark Mode' : 'Light Mode'}</span>
          </div>
        </button>

        <div className="login-card-wrapper">
          <div className="logo-title">
            <div className="logo-box">
              <BarChart3 className="logo-icon" />
            </div>
            <h1 className="main-title">Set new password</h1>
            <p className="subtitle">
              {success ? 'Password updated successfully' : 'Choose a new password for your account'}
            </p>
          </div>

          <div className="card">
            {success ? (
              <div className="login-form" style={{ textAlign: 'center' }}>
                <CheckCircle2 className="logo-icon" style={{ color: '#22c55e', margin: '0 auto 1rem' }} />
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
                        setError('');
                      }}
                      placeholder="At least 6 characters"
                      className="input-field"
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
                        setError('');
                      }}
                      placeholder="Re-enter your password"
                      className="input-field"
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
                  {passwordsMismatch && (
                    <p className="info-text" style={{ color: '#f87171', marginTop: '0.35rem' }}>
                      Passwords do not match
                    </p>
                  )}
                </div>

                {error && (
                  <p className="info-text" style={{ color: '#f87171', marginBottom: '0.75rem' }}>
                    {error}
                  </p>
                )}

                <button type="submit" className="primary-btn" disabled={isLoading || passwordsMismatch}>
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
