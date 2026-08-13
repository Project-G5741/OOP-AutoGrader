import React, { useState } from 'react';
import { Mail, ArrowLeft } from 'lucide-react';
import AppLogo from '../components/ui/AppLogo';
import './LoginUI.css';
import ThemeToggle from '../components/ThemeToggle';
import { validateEmail } from '../utils/validation';
import { readFriendlyAuthError } from '../utils/apiError';

export default function ForgotPasswordUI({ onBack, onSuccess }) {
  const [email, setEmail] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const [sent, setSent] = useState(false);
  const [hasAttemptedSubmit, setHasAttemptedSubmit] = useState(false);
  const [emailTouched, setEmailTouched] = useState(false);

  const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';
  const rawEmailError = validateEmail(email);
  const emailError = hasAttemptedSubmit || emailTouched ? rawEmailError : '';
  const canSubmit = !rawEmailError;

  const handleEmailChange = (value) => {
    setEmail(value);
    setError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setHasAttemptedSubmit(true);
    setEmailTouched(true);
    setError('');

    if (rawEmailError) {
      return;
    }

    setIsLoading(true);
    try {
      const response = await fetch(`${API_BASE}/api/auth/forgot-password`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: email.trim() }),
      });

      if (!response.ok) {
        throw new Error(await readFriendlyAuthError(response, 'forgot-password'));
      }

      setSent(true);
      onSuccess?.();
    } catch (err) {
      setError(err.message || 'Unable to send reset email. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="login-root">
      <div className="login-bg">
        <ThemeToggle className="theme-toggle" />

        <div className="login-card-wrapper">
          <div className="logo-title">
            <AppLogo variant="login" />
            <h1 className="main-title">Forgot password</h1>
            <p className="subtitle">
              {sent
                ? 'Check your inbox for a reset link'
                : 'Enter your school email to receive a reset link'}
            </p>
          </div>

          <div className="card">
            {sent ? (
              <div className="login-form">
                <p className="info-text" style={{ marginBottom: '1rem' }}>
                  If an account exists for <strong>{email}</strong>, we sent a password reset link.
                  The link expires in 15 minutes.
                </p>
                <button type="button" className="primary-btn" onClick={onBack}>
                  Back to sign in
                </button>
              </div>
            ) : (
              <form onSubmit={handleSubmit} className="login-form">
                <div className="form-group">
                  <label className="field-label">School email</label>
                  <div className="input-wrapper">
                    <Mail className="input-icon" />
                    <input
                      type="email"
                      value={email}
                      onChange={(e) => handleEmailChange(e.target.value)}
                      onBlur={() => setEmailTouched(true)}
                      placeholder="you@eiu.edu.vn"
                      className={`input-field${emailError ? ' input-error' : ''}`}
                      autoComplete="email"
                    />
                  </div>
                  {emailError && (
                    <p className="error-text">{emailError}</p>
                  )}
                </div>

                {error && (
                  <p className="error-text" style={{ marginBottom: '0.75rem' }}>
                    {error}
                  </p>
                )}

                <button type="submit" className="primary-btn" disabled={isLoading || !canSubmit}>
                  {isLoading ? 'Sending...' : 'Send reset link'}
                </button>

                <button
                  type="button"
                  className="forgot-link back-link"
                  onClick={onBack}
                >
                  <ArrowLeft className="toggle-icon" style={{ width: 16, height: 16 }} />
                  Back to sign in
                </button>
              </form>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
