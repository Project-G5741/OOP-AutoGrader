import React, { useState } from 'react';
import { Eye, EyeOff, Lock, CreditCard, CheckCircle2 } from 'lucide-react';
import AppLogo from '../components/ui/AppLogo';
import { brand } from '../theme/brand';
import ThemeToggle from '../components/ThemeToggle';
import { getFirstTimeSetupErrors, isFormValid } from '../utils/validation';
import { readFriendlyAuthError, toFriendlyError } from '../utils/apiError';

export default function FirstTimeSetupUI({ token, profile = {}, onClose, onComplete }) {
  const [irn, setIrn] = useState('');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [done, setDone] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [formError, setFormError] = useState('');

  const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';

  const fieldErrors = getFirstTimeSetupErrors(irn, password, confirm);
  const canSubmit = isFormValid(fieldErrors);
  const passwordMatch = password && confirm && !fieldErrors.password && !fieldErrors.confirm;

  async function handleSubmit(e) {
    e.preventDefault();
    if (!canSubmit) {
      return;
    }

    setFormError('');
    setIsSubmitting(true);
    try {
      const resp = await fetch(`${API_BASE}/api/auth/google/upsert`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token, irn: irn.trim(), password, role: 'STUDENT' }),
      });
      if (!resp.ok) {
        throw new Error(await readFriendlyAuthError(resp, 'setup'));
      }
      const data = await resp.json();
      setDone(true);
      onComplete?.(data);
    } catch (err) {
      console.error('Upsert failed', err);
      setFormError(toFriendlyError(err, 'setup'));
    } finally {
      setIsSubmitting(false);
    }
  }

  const borderClass = (hasError, isMatch = false) => {
    if (hasError) {
      return 'border-error focus:ring-error';
    }
    if (isMatch) {
      return 'border-success focus:ring-success';
    }
    return 'border-border focus:ring-primary';
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-background to-surface-secondary flex items-center justify-center p-6 transition-colors relative">
      <ThemeToggle className="absolute top-6 right-6 z-10" />

      <div className="absolute inset-0 bg-black/40 dark:bg-black/60" />

      <div className="relative w-full max-w-md z-10">
        <div className="text-center mb-4 opacity-60">
          <div className="inline-flex items-center gap-2 text-white">
            <AppLogo variant="inline" />
            <span className="text-sm font-medium">{brand.loginTitle}</span>
          </div>
        </div>

        <div className="bg-surface rounded-2xl shadow-2xl border border-border overflow-hidden">
          <div className="bg-gradient-to-r from-primary to-primary-hover px-6 py-5">
            <div className="flex items-center gap-3">
              <div className="flex items-center justify-center w-10 h-10 bg-white/20 rounded-xl">
                <CheckCircle2 className="w-5 h-5 text-white" />
              </div>
              <div>
                <h2 className="text-white font-semibold text-lg leading-tight">Complete Your Profile</h2>
                <p className="text-white/70 text-xs mt-0.5">First-time setup — takes less than a minute</p>
              </div>
            </div>
          </div>

          {!done ? (
            <form onSubmit={handleSubmit} className="px-6 py-6 space-y-4">
              <div className="flex items-center gap-3 px-3 py-2.5 bg-surface-secondary rounded-lg border border-border">
                <svg className="w-4 h-4 flex-shrink-0" viewBox="0 0 24 24">
                  <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
                  <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                  <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
                  <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
                </svg>
                <div className="min-w-0">
                  <p className="text-xs text-foreground-muted leading-none mb-0.5">Signed in as</p>
                  <p className="text-sm text-foreground font-medium truncate">{profile?.email || '—'}</p>
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-foreground-secondary mb-1.5">IRN <span className="text-primary">*</span></label>
                <div className="relative">
                  <CreditCard className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-foreground-disabled" />
                  <input
                    type="text"
                    value={irn}
                    onChange={(e) => {
                      setIrn(e.target.value);
                      setFormError('');
                    }}
                    placeholder="e.g. 2052123456"
                    className={`w-full pl-10 pr-4 py-2.5 bg-surface-secondary border rounded-lg text-foreground placeholder-foreground-disabled focus:outline-none focus:ring-2 focus:border-transparent transition-all text-sm ${borderClass(fieldErrors.irn)}`}
                  />
                </div>
                {fieldErrors.irn ? (
                  <p className="mt-1 text-xs text-error">{fieldErrors.irn}</p>
                ) : (
                  <p className="mt-1 text-xs text-foreground-disabled">10-digit student identification number</p>
                )}
              </div>

              <div>
                <label className="block text-sm font-medium text-foreground-secondary mb-1.5">Set Password <span className="text-primary">*</span></label>
                <div className="relative">
                  <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-foreground-disabled" />
                  <input
                    type={showPassword ? 'text' : 'password'}
                    value={password}
                    onChange={(e) => {
                      setPassword(e.target.value);
                      setFormError('');
                    }}
                    placeholder="Create a password"
                    className={`w-full pl-10 pr-10 py-2.5 bg-surface-secondary border rounded-lg text-foreground placeholder-foreground-disabled focus:outline-none focus:ring-2 focus:border-transparent transition-all text-sm ${borderClass(fieldErrors.password)}`}
                  />
                  <button type="button" onClick={() => setShowPassword(!showPassword)} className="absolute right-3 top-1/2 -translate-y-1/2 text-foreground-disabled">
                    {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
                {fieldErrors.password && <p className="mt-1 text-xs text-error">{fieldErrors.password}</p>}
              </div>

              <div>
                <label className="block text-sm font-medium text-foreground-secondary mb-1.5">Confirm Password <span className="text-primary">*</span></label>
                <div className="relative">
                  <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-foreground-disabled" />
                  <input
                    type={showConfirm ? 'text' : 'password'}
                    value={confirm}
                    onChange={(e) => {
                      setConfirm(e.target.value);
                      setFormError('');
                    }}
                    placeholder="Re-enter your password"
                    className={`w-full pl-10 pr-10 py-2.5 bg-surface-secondary border rounded-lg text-foreground placeholder-foreground-disabled focus:outline-none focus:ring-2 focus:border-transparent transition-all text-sm ${borderClass(fieldErrors.confirm, passwordMatch)}`}
                  />
                  <button type="button" onClick={() => setShowConfirm(!showConfirm)} className="absolute right-3 top-1/2 -translate-y-1/2 text-foreground-disabled">
                    {showConfirm ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
                {fieldErrors.confirm && <p className="mt-1 text-xs text-error">{fieldErrors.confirm}</p>}
                {passwordMatch && (
                  <p className="mt-1 text-xs text-success flex items-center gap-1"><CheckCircle2 className="w-3 h-3" /> Passwords match</p>
                )}
              </div>

              {formError && <p className="text-xs text-error">{formError}</p>}

              <button type="submit" disabled={!canSubmit || isSubmitting} className="w-full py-3 bg-gradient-to-r from-primary to-primary-hover text-white rounded-lg font-medium hover:from-primary-hover hover:to-primary-active transition-all duration-200 shadow-md disabled:opacity-40 disabled:cursor-not-allowed mt-2">
                {isSubmitting ? 'Processing...' : 'Complete Setup'}
              </button>
            </form>
          ) : (
            <div className="px-6 py-10 text-center">
              <div className="inline-flex items-center justify-center w-16 h-16 bg-success-bg rounded-2xl mb-4">
                <CheckCircle2 className="w-8 h-8 text-success" />
              </div>
              <h3 className="text-foreground font-semibold text-xl mb-2">All set!</h3>
              <p className="text-foreground-muted text-sm mb-6">Your profile is complete. You can now access the Lab Management System.</p>
              <div className="flex justify-center gap-2">
                <button onClick={() => onClose?.()} className="px-6 py-2.5 bg-surface-secondary rounded-lg text-sm text-foreground">Close</button>
              </div>
            </div>
          )}

          {!done && (
            <div className="px-6 pb-4 text-center">
              <p className="text-xs text-foreground-disabled">Make by Pham Quan Kha & Doan Tuan Kiet</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
