import React, { useState } from 'react';
import { GoogleLogin } from '@react-oauth/google';
import './LoginUI.css';
import { Moon, Sun, BarChart3 } from 'lucide-react';

export default function LoginUI({ onLoginSuccess }) {
  const [isDark, setIsDark] = useState(true);

  const handleGoogleSuccess = async (credentialResponse) => {
    const idToken = credentialResponse?.credential;

    if (!idToken) {
      console.error('Google login response missing credential', credentialResponse);
      alert('Google login không nhận được token. Vui lòng thử lại.');
      return;
    }

    try {
      const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';
      const response = await fetch(`${API_BASE}/api/auth/google`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ token: idToken }),
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || `HTTP ${response.status}`);
      }

      const data = await response.json();
      onLoginSuccess?.(data.accessToken);
    } catch (error) {
      console.error('Backend authentication failed', error);
      alert('Đăng nhập thất bại. Vui lòng thử lại.');
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
            <h1 className="main-title">Lab Management System</h1>
            <p className="subtitle">Sign in to your account</p>
          </div>

          <div className="card">
            <div>
              <p className="card-subtitle">Sign in with your university Google account</p>

              <div className="google-login-wrapper">
                <GoogleLogin
                  onSuccess={handleGoogleSuccess}
                  onError={() => {
                    alert('Google login bị lỗi. Vui lòng thử lại.');
                  }}
                />
              </div>

              <div className="info-text">
                <p>Use your university email (@eiu.edu.vn) to access the system</p>
              </div>
            </div>
          </div>

          <div className="footer-text">Make by Pham Quan Kha & Doan Tuan Kiet</div>
        </div>
      </div>
    </div>
  );
}
