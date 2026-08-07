import React, { useState } from 'react';
import { Moon, Sun, BarChart3, Mail, ArrowLeft } from 'lucide-react';
import './LoginUI.css';

export default function ForgotPasswordUI({ onBack, onSuccess }) {
  const [isDark, setIsDark] = useState(true);
  const [email, setEmail] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const [sent, setSent] = useState(false);

  const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';

  const parseErrorMessage = async (response) => {
    const text = await response.text();
    if (!text) {
      return 'Unable to send reset email. Please try again.';
    }
    try {
      const data = JSON.parse(text);
      return data.message || data.error || text;
    } catch {
      return text;
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!email.trim()) {
      setError('Please enter your email address.');
      return;
    }

    setIsLoading(true);
    try {
      const response = await fetch(`${API_BASE}/api/auth/forgot-password`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: email.trim() }),
      });

      if (response.status === 404) {
        throw new Error(await parseErrorMessage(response));
      }

      if (!response.ok) {
        throw new Error(await parseErrorMessage(response));
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
                      onChange={(e) => {
                        setEmail(e.target.value);
                        setError('');
                      }}
                      placeholder="you@eiu.edu.vn"
                      className="input-field"
                      autoComplete="email"
                    />
                  </div>
                </div>

                {error && (
                  <p className="info-text" style={{ color: '#f87171', marginBottom: '0.75rem' }}>
                    {error}
                  </p>
                )}

                <button type="submit" className="primary-btn" disabled={isLoading}>
                  {isLoading ? 'Sending...' : 'Send reset link'}
                </button>

                <button
                  type="button"
                  className="forgot-link"
                  style={{ marginTop: '1rem', display: 'inline-flex', alignItems: 'center', gap: '0.35rem' }}
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
