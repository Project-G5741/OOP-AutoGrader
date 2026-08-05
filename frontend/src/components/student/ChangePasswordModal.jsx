import { useState } from 'react';
import { X, Eye, EyeOff, Lock, CheckCircle2 } from 'lucide-react';

export default function ChangePasswordModal({ isOpen, onClose, user }) {
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showCurrent, setShowCurrent] = useState(false);
  const [showNew, setShowNew] = useState(false);
  const [saved, setSaved] = useState(false);

  if (!isOpen) return null;

  const passwordsMatch = newPassword && confirmPassword && newPassword === confirmPassword;
  const passwordsMismatch = newPassword && confirmPassword && newPassword !== confirmPassword;
  const canSave = currentPassword && passwordsMatch;

  const handleSave = () => {
    setSaved(true);
    setTimeout(() => {
      setSaved(false);
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
      onClose();
    }, 1200);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4">
      <div className="w-full max-w-md overflow-hidden rounded-3xl border border-gray-200/80 bg-white shadow-2xl dark:border-gray-700 dark:bg-[#161b22]">
        <div className="flex items-center justify-between border-b border-gray-200/80 px-6 py-4 dark:border-gray-800">
          <div>
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white">Change Password</h2>
            <p className="text-sm text-gray-500 dark:text-gray-400">Update your student account password securely.</p>
          </div>
          <button onClick={onClose} className="text-gray-500 transition hover:text-gray-700 dark:text-gray-400 dark:hover:text-white">
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="space-y-5 px-6 py-6">
          <div className="space-y-4">
            {[
              { label: 'Current Password', value: currentPassword, setter: setCurrentPassword, visible: showCurrent, toggle: () => setShowCurrent((value) => !value) },
              { label: 'New Password', value: newPassword, setter: setNewPassword, visible: showNew, toggle: () => setShowNew((value) => !value) },
            ].map((field) => (
              <div key={field.label}>
                <label className="mb-2 block text-xs font-medium text-gray-500 dark:text-gray-400">{field.label}</label>
                <div className="relative">
                  <Lock className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
                  <input
                    type={field.visible ? 'text' : 'password'}
                    value={field.value}
                    onChange={(event) => field.setter(event.target.value)}
                    placeholder={field.label}
                    className="w-full rounded-2xl border border-gray-200 bg-white px-10 py-3 text-sm text-gray-900 outline-none transition focus:border-purple-500 dark:border-gray-700 dark:bg-[#0d1117] dark:text-white"
                  />
                  <button
                    type="button"
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-500"
                    onClick={field.toggle}
                  >
                    {field.visible ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                  </button>
                </div>
              </div>
            ))}

            <div>
              <label className="mb-2 block text-xs font-medium text-gray-500 dark:text-gray-400">Confirm New Password</label>
              <input
                type="password"
                value={confirmPassword}
                onChange={(event) => setConfirmPassword(event.target.value)}
                placeholder="Repeat new password"
                className={`w-full rounded-2xl border px-4 py-3 text-sm text-gray-900 outline-none bg-white transition focus:border-purple-500 dark:bg-[#0d1117] dark:text-white ${
                  passwordsMismatch ? 'border-red-500' : passwordsMatch ? 'border-green-500' : 'border-gray-200 dark:border-gray-700'
                }`}
              />
              {passwordsMismatch && <p className="mt-2 text-xs text-red-400">Passwords do not match.</p>}
              {passwordsMatch && <p className="mt-2 text-xs text-green-400">Passwords match.</p>}
            </div>
          </div>

          <button
            type="button"
            disabled={!canSave}
            onClick={handleSave}
            className="flex w-full items-center justify-center gap-2 rounded-2xl bg-purple-600 px-4 py-3 text-sm font-semibold text-white transition hover:bg-purple-500 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {saved ? <><CheckCircle2 className="h-4 w-4" /> Password Updated</> : 'Update Password'}
          </button>
        </div>
      </div>
    </div>
  );
}
