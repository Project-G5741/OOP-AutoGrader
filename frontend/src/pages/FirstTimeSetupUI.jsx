import React, { useState } from 'react';
import { Moon, Sun, BarChart3, Eye, EyeOff, Lock, CreditCard, Calendar, CheckCircle2 } from 'lucide-react';

export default function FirstTimeSetupUI({ token, profile = {}, onClose, onComplete }) {
  const [isDark, setIsDark] = useState(true);
  const [irn, setIrn] = useState('');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [done, setDone] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const passwordMatch = password && confirm && password === confirm;
  const passwordMismatch = password && confirm && password !== confirm;

  const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';

  async function handleSubmit(e) {
    e.preventDefault();
    if (!irn || !passwordMatch) return;
    setIsSubmitting(true);
    try {
      const resp = await fetch(`${API_BASE}/api/auth/google/upsert`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token, irn, password, role: 'STUDENT'}),
      });
      if (!resp.ok) {
        const text = await resp.text();
        throw new Error(text || `HTTP ${resp.status}`);
      }
      const data = await resp.json();
      setDone(true);
      onComplete?.(data);
    } catch (err) {
      console.error('Upsert failed', err);
      alert(err.message || 'Setup failed');
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className={isDark ? 'dark' : ''}>
      <div className="min-h-screen bg-gradient-to-br from-gray-50 to-gray-100 dark:from-[#0f1419] dark:to-[#1a1f2e] flex items-center justify-center p-6 transition-colors relative">
        <button
          onClick={() => setIsDark(!isDark)}
          className="absolute top-6 right-6 px-4 py-2 bg-white dark:bg-[#1e2530] text-gray-900 dark:text-white rounded-lg border border-gray-200 dark:border-gray-700 flex items-center gap-2 shadow-sm transition-colors hover:bg-gray-50 dark:hover:bg-[#252d3a] z-10"
        >
          {isDark ? <Moon className="w-4 h-4" /> : <Sun className="w-4 h-4" />}
          <span className="text-sm">{isDark ? 'Dark Mode' : 'Light Mode'}</span>
        </button>

        <div className="absolute inset-0 bg-black/40 dark:bg-black/60" />

        <div className="relative w-full max-w-md z-10">
          <div className="text-center mb-4 opacity-60">
            <div className="inline-flex items-center gap-2 text-white">
              <BarChart3 className="w-5 h-5" />
              <span className="text-sm font-medium">Lab Management System</span>
            </div>
          </div>

          <div className="bg-white dark:bg-[#1e2530] rounded-2xl shadow-2xl border border-gray-100 dark:border-gray-800 overflow-hidden">
            <div className="bg-gradient-to-r from-purple-600 to-purple-500 px-6 py-5">
              <div className="flex items-center gap-3">
                <div className="flex items-center justify-center w-10 h-10 bg-white/20 rounded-xl">
                  <CheckCircle2 className="w-5 h-5 text-white" />
                </div>
                <div>
                  <h2 className="text-white font-semibold text-lg leading-tight">Complete Your Profile</h2>
                  <p className="text-purple-200 text-xs mt-0.5">First-time setup — takes less than a minute</p>
                </div>
              </div>
            </div>

            {!done ? (
              <form onSubmit={handleSubmit} className="px-6 py-6 space-y-4">
                <div className="flex items-center gap-3 px-3 py-2.5 bg-gray-50 dark:bg-[#151b24] rounded-lg border border-gray-200 dark:border-gray-700">
                  <svg className="w-4 h-4 flex-shrink-0" viewBox="0 0 24 24">
                    <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
                    <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                    <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
                    <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
                  </svg>
                  <div className="min-w-0">
                    <p className="text-xs text-gray-500 dark:text-gray-500 leading-none mb-0.5">Signed in as</p>
                    <p className="text-sm text-gray-800 dark:text-gray-200 font-medium truncate">{profile?.email || '—'}</p>
                  </div>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">IRN <span className="text-purple-500">*</span></label>
                  <div className="relative">
                    <CreditCard className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400 dark:text-gray-500" />
                    <input
                      type="text"
                      value={irn}
                      onChange={(e) => setIrn(e.target.value)}
                      placeholder="e.g. 20521234"
                      className="w-full pl-10 pr-4 py-2.5 bg-gray-50 dark:bg-[#151b24] border border-gray-200 dark:border-gray-700 rounded-lg text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-600 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent transition-all text-sm"
                    />
                  </div>
                  <p className="mt-1 text-xs text-gray-400 dark:text-gray-600">Your student identification number</p>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">Set Password <span className="text-purple-500">*</span></label>
                  <div className="relative">
                    <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400 dark:text-gray-500" />
                    <input
                      type={showPassword ? 'text' : 'password'}
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      placeholder="Create a password"
                      className="w-full pl-10 pr-10 py-2.5 bg-gray-50 dark:bg-[#151b24] border border-gray-200 dark:border-gray-700 rounded-lg text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-600 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent transition-all text-sm"
                    />
                    <button type="button" onClick={() => setShowPassword(!showPassword)} className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 dark:text-gray-500">
                      {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                    </button>
                  </div>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">Confirm Password <span className="text-purple-500">*</span></label>
                  <div className="relative">
                    <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400 dark:text-gray-500" />
                    <input
                      type={showConfirm ? 'text' : 'password'}
                      value={confirm}
                      onChange={(e) => setConfirm(e.target.value)}
                      placeholder="Re-enter your password"
                      className={`w-full pl-10 pr-10 py-2.5 bg-gray-50 dark:bg-[#151b24] border rounded-lg text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-600 focus:outline-none focus:ring-2 focus:border-transparent transition-all text-sm ${
                        passwordMismatch
                          ? 'border-red-400 dark:border-red-500 focus:ring-red-400'
                          : passwordMatch
                          ? 'border-green-400 dark:border-green-500 focus:ring-green-400'
                          : 'border-gray-200 dark:border-gray-700 focus:ring-purple-500'
                      }`}
                    />
                    <button type="button" onClick={() => setShowConfirm(!showConfirm)} className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 dark:text-gray-500">
                      {showConfirm ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                    </button>
                  </div>
                  {passwordMismatch && <p className="mt-1 text-xs text-red-500">Passwords do not match</p>}
                  {passwordMatch && (
                    <p className="mt-1 text-xs text-green-500 flex items-center gap-1"><CheckCircle2 className="w-3 h-3" /> Passwords match</p>
                  )}
                </div>

                <button type="submit" disabled={!irn || !passwordMatch || isSubmitting} className="w-full py-3 bg-gradient-to-r from-purple-600 to-purple-500 text-white rounded-lg font-medium hover:from-purple-700 hover:to-purple-600 transition-all duration-200 shadow-md shadow-purple-500/20 disabled:opacity-40 disabled:cursor-not-allowed mt-2">
                  {isSubmitting ? 'Processing...' : 'Complete Setup'}
                </button>
              </form>
            ) : (
              <div className="px-6 py-10 text-center">
                <div className="inline-flex items-center justify-center w-16 h-16 bg-green-100 dark:bg-green-900/30 rounded-2xl mb-4">
                  <CheckCircle2 className="w-8 h-8 text-green-500" />
                </div>
                <h3 className="text-gray-900 dark:text-white font-semibold text-xl mb-2">All set!</h3>
                <p className="text-gray-500 dark:text-gray-400 text-sm mb-6">Your profile is complete. You can now access the Lab Management System.</p>
                <div className="flex justify-center gap-2">
                  <button onClick={() => onClose?.()} className="px-6 py-2.5 bg-gray-200 dark:bg-[#111217] rounded-lg text-sm">Close</button>
                </div>
              </div>
            )}

            {!done && (
              <div className="px-6 pb-4 text-center">
                <p className="text-xs text-gray-400 dark:text-gray-600">Make by Pham Quan Kha & Doan Tuan Kiet</p>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
