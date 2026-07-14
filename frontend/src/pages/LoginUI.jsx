import React, { useState } from 'react';
import { GoogleLogin } from '@react-oauth/google';
import './LoginUI.css';
import { Moon, Sun, BarChart3, Eye, EyeOff, User, Lock } from 'lucide-react';
import FirstTimeSetupUI from './FirstTimeSetupUI';

function decodeJwtPayload(token) {
  try {
    const parts = token.split('.');
    if (parts.length < 2) return null;
    const payload = parts[1];
    const json = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
    return JSON.parse(decodeURIComponent(escape(json)));
  } catch (e) {
    return null;
  }
}

export default function LoginUI({ onLoginSuccess }) {
  const [isDark, setIsDark] = useState(true);
  const [irn, setIrn] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [remember, setRemember] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  const [showFirstTimeSetup, setShowFirstTimeSetup] = useState(false);
  const [googleToken, setGoogleToken] = useState(null);
  const [googleProfile, setGoogleProfile] = useState(null);

  const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';

  const handleLocalLogin = async (e) => {
    e.preventDefault();
    if (!irn || !password) {
      alert('Please enter your IRN and password.');
      return;
    }

    setIsLoading(true);
    try {
      const response = await fetch(`${API_BASE}/api/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ irn, password }),
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || 'Invalid credentials');
      }

      const data = await response.json();
      if (remember) localStorage.setItem('rememberedIrn', irn);
      onLoginSuccess?.(data);
    } catch (error) {
      console.error('Local authentication failed', error);
      alert(error.message || 'Login failed. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleGoogleSuccess = async (credentialResponse) => {
    const idToken = credentialResponse?.credential;
    if (!idToken) {
      console.error('Google login response missing credential', credentialResponse);
      alert('Google login did not receive the token. Please try again.');
      return;
    }

    setIsLoading(true);
    try {
      // Try normal authenticate first
      const resp = await fetch(`${API_BASE}/api/auth/google`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token: idToken }),
      });

      if (resp.ok) {
        const data = await resp.json();
        onLoginSuccess?.(data);
        return;
      }

      if (resp.status === 403) {
        // account not registered yet — open first-time setup modal
        const payload = decodeJwtPayload(idToken) || {};
        setGoogleToken(idToken);
        setGoogleProfile({ email: payload.email, name: payload.name });
        setShowFirstTimeSetup(true);
        return;
      }

      const errorText = await resp.text();
      throw new Error(errorText || `HTTP ${resp.status}`);
    } catch (error) {
      console.error('Backend authentication failed', error);
      alert(error.message || 'Login failed. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleCompleteFirstTime = async (createdData) => {
    setShowFirstTimeSetup(false);
    onLoginSuccess?.(createdData);
  };

  if (showFirstTimeSetup) {
    return (
      <FirstTimeSetupUI
        token={googleToken}
        profile={googleProfile}
        onClose={() => setShowFirstTimeSetup(false)}
        onComplete={handleCompleteFirstTime}
      />
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
            <h1 className="main-title">Lab Management System</h1>
            <p className="subtitle">Sign in to your account</p>
          </div>

          <div className="card">
            <form onSubmit={handleLocalLogin} className="login-form">
              <div className="form-group">
                <label className="field-label">IRN (Student ID)</label>
                <div className="input-wrapper">
                  <User className="input-icon" />
                  <input
                    type="text"
                    value={irn}
                    onChange={(e) => setIrn(e.target.value)}
                    placeholder="e.g. 20521234"
                    className="input-field"
                  />
                </div>
              </div>

              <div className="form-group">
                <label className="field-label">Password</label>
                <div className="input-wrapper">
                  <Lock className="input-icon" />
                  <input
                    type={showPassword ? 'text' : 'password'}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="Enter your password"
                    className="input-field"
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="password-toggle"
                    aria-label="Toggle password visibility"
                  >
                    {showPassword ? <EyeOff className="toggle-icon" /> : <Eye className="toggle-icon" />}
                  </button>
                </div>
              </div>

              <div className="options-row">
                <label className="remember-option">
                  <input
                    type="checkbox"
                    checked={remember}
                    onChange={(e) => setRemember(e.target.checked)}
                  />
                  <span>Remember me</span>
                </label>
                <button type="button" className="forgot-link">
                  Forgot password?
                </button>
              </div>

              <button type="submit" className="primary-btn" disabled={isLoading}>
                {isLoading ? 'Signing in...' : 'Sign In'}
              </button>

              <div className="divider">
                <div className="divider-line" />
                <span>or continue with</span>
                <div className="divider-line" />
              </div>

              <div className="google-login-wrapper">
                <GoogleLogin
                  onSuccess={handleGoogleSuccess}
                  onError={() => {
                    alert('Google login failed. Please try again.');
                  }}
                  render={(renderProps) => (
                    <button
                      type="button"
                      onClick={renderProps.onClick}
                      disabled={renderProps.disabled || isLoading}
                      className="google-btn"
                    >
                      <svg className="g-icon" viewBox="0 0 24 24" aria-hidden="true">
                        <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" />
                        <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" />
                        <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" />
                        <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" />
                      </svg>
                      <span>{isLoading ? 'Processing...' : 'Sign in with Google'}</span>
                    </button>
                  )}
                />
              </div>

              <div className="info-text">
                <p>Use your university email (@eiu.edu.vn) to access the system.</p>
              </div>
            </form>
          </div>

          <div className="footer-text">Made by Pham Quan Kha & Doan Tuan Kiet</div>
        </div>
      </div>

    </div>
  );
}
