import { useState, useRef } from 'react';
import { X, Eye, EyeOff, Lock, Camera, Calendar, Send, Paperclip, Video, Image as ImageIcon, CheckCircle2 } from 'lucide-react';

const tabs = [
  { id: 'profile', label: 'Profile' },
  { id: 'password', label: 'Password' },
  { id: 'contact', label: 'Contact Lecturer' },
];

export default function ProfileEditModal({ isOpen, onClose }) {
  const [activeTab, setActiveTab] = useState('profile');
  const [avatar, setAvatar] = useState(null);
  const [dob, setDob] = useState('2002-03-15');
  const [oldPw, setOldPw] = useState('');
  const [newPw, setNewPw] = useState('');
  const [confirmPw, setConfirmPw] = useState('');
  const [showOld, setShowOld] = useState(false);
  const [showNew, setShowNew] = useState(false);
  const [message, setMessage] = useState('');
  const [attachment, setAttachment] = useState(null);
  const [saved, setSaved] = useState(false);
  const fileRef = useRef(null);
  const avatarRef = useRef(null);

  if (!isOpen) return null;

  const pwMatch = newPw && confirmPw && newPw === confirmPw;
  const pwMismatch = newPw && confirmPw && newPw !== confirmPw;

  const handleAvatarChange = (event) => {
    const file = event.target.files?.[0];
    if (!file) return;
    setAvatar(URL.createObjectURL(file));
  };

  const handleAttach = (event) => {
    const file = event.target.files?.[0];
    if (!file) return;
    const mb = file.size / (1024 * 1024);
    if (mb > 25) {
      alert('File must be under 25 MB');
      return;
    }
    setAttachment({ name: file.name, size: `${mb.toFixed(1)} MB` });
  };

  const handleSave = () => {
    setSaved(true);
    setTimeout(() => {
      setSaved(false);
      onClose();
    }, 1200);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4">
      <div className="w-full max-w-lg overflow-hidden rounded-3xl border border-gray-700 bg-[#161b22] shadow-2xl">
        <div className="flex items-center justify-between border-b border-gray-800 px-6 py-4">
          <div>
            <h2 className="text-lg font-semibold text-white">Student Profile</h2>
            <p className="text-sm text-gray-400">Update your details and contact your lecturer.</p>
          </div>
          <button onClick={onClose} className="text-gray-400 transition hover:text-white">
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="flex gap-1 border-b border-gray-800 bg-[#12181f] px-4">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              type="button"
              onClick={() => setActiveTab(tab.id)}
              className={`flex-1 py-3 text-sm font-medium transition ${
                activeTab === tab.id
                  ? 'border-b-2 border-purple-500 text-purple-300'
                  : 'text-gray-500 hover:text-gray-300'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>

        <div className="space-y-5 px-6 py-6">
          {activeTab === 'profile' && (
            <div className="space-y-5">
              <div className="flex flex-col items-center gap-3">
                <div className="relative">
                  <div className="flex h-20 w-20 items-center justify-center overflow-hidden rounded-full bg-purple-700 text-3xl font-bold text-white">
                    {avatar ? <img src={avatar} alt="avatar" className="h-full w-full object-cover" /> : 'S'}
                  </div>
                  <button
                    type="button"
                    onClick={() => avatarRef.current?.click()}
                    className="absolute -bottom-1 -right-1 flex h-8 w-8 items-center justify-center rounded-full bg-purple-600 text-white transition hover:bg-purple-500"
                  >
                    <Camera className="h-4 w-4" />
                  </button>
                  <input ref={avatarRef} type="file" accept="image/*" className="hidden" onChange={handleAvatarChange} />
                </div>
                <p className="text-xs text-gray-500">Upload a profile avatar.</p>
              </div>

              <div>
                <label className="mb-2 block text-xs font-medium text-gray-400">Date of Birth</label>
                <div className="relative">
                  <Calendar className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-500" />
                  <input
                    type="date"
                    value={dob}
                    onChange={(event) => setDob(event.target.value)}
                    className="w-full rounded-2xl border border-gray-700 bg-[#0d1117] px-10 py-3 text-sm text-white outline-none focus:border-purple-500"
                  />
                </div>
              </div>

              <button
                type="button"
                onClick={handleSave}
                className="flex w-full items-center justify-center gap-2 rounded-2xl bg-purple-600 px-4 py-3 text-sm font-semibold text-white transition hover:bg-purple-500"
              >
                {saved ? <><CheckCircle2 className="h-4 w-4" /> Saved!</> : 'Save Profile'}
              </button>
            </div>
          )}

          {activeTab === 'password' && (
            <div className="space-y-4">
              {[
                { label: 'Current Password', value: oldPw, setter: setOldPw, visible: showOld, toggle: () => setShowOld((value) => !value) },
                { label: 'New Password', value: newPw, setter: setNewPw, visible: showNew, toggle: () => setShowNew((value) => !value) },
              ].map((field) => (
                <div key={field.label}>
                  <label className="mb-2 block text-xs font-medium text-gray-400">{field.label}</label>
                  <div className="relative">
                    <Lock className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-500" />
                    <input
                      type={field.visible ? 'text' : 'password'}
                      value={field.value}
                      onChange={(event) => field.setter(event.target.value)}
                      placeholder={`Enter ${field.label.toLowerCase()}`}
                      className="w-full rounded-2xl border border-gray-700 bg-[#0d1117] px-10 py-3 text-sm text-white outline-none focus:border-purple-500"
                    />
                    <button
                      type="button"
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-200"
                      onClick={field.toggle}
                    >
                      {field.visible ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                    </button>
                  </div>
                </div>
              ))}

              <div>
                <label className="mb-2 block text-xs font-medium text-gray-400">Confirm New Password</label>
                <input
                  type="password"
                  value={confirmPw}
                  onChange={(event) => setConfirmPw(event.target.value)}
                  placeholder="Repeat new password"
                  className={`w-full rounded-2xl border px-4 py-3 text-sm text-white outline-none bg-[#0d1117] focus:border-purple-500 ${
                    pwMismatch ? 'border-red-500' : pwMatch ? 'border-green-500' : 'border-gray-700'
                  }`}
                />
                {pwMismatch && <p className="mt-2 text-xs text-red-400">Passwords do not match.</p>}
                {pwMatch && <p className="mt-2 text-xs text-green-400">Passwords match.</p>}
              </div>

              <button
                type="button"
                disabled={!oldPw || !pwMatch}
                onClick={handleSave}
                className="w-full rounded-2xl bg-purple-600 px-4 py-3 text-sm font-semibold text-white transition hover:bg-purple-500 disabled:cursor-not-allowed disabled:opacity-50"
              >
                {saved ? 'Password Changed!' : 'Change Password'}
              </button>
            </div>
          )}

          {activeTab === 'contact' && (
            <div className="space-y-4">
              <p className="text-xs text-gray-500">Write a message or attach a file to contact your lecturer.</p>
              <textarea
                rows={4}
                value={message}
                onChange={(event) => setMessage(event.target.value)}
                placeholder="Type your message here..."
                className="w-full rounded-2xl border border-gray-700 bg-[#0d1117] px-4 py-3 text-sm text-white outline-none focus:border-purple-500"
              />

              <div>
                <div className="mb-2 flex items-center justify-between text-xs text-gray-400">
                  <span>Attachment</span>
                  <span className="text-gray-500">Max 25 MB</span>
                </div>
                <div className="flex flex-wrap gap-2">
                  {[
                    { icon: Paperclip, label: 'File' },
                    { icon: ImageIcon, label: 'Image' },
                    { icon: Video, label: 'Video' },
                  ].map((item) => (
                    <button
                      key={item.label}
                      type="button"
                      onClick={() => fileRef.current?.click()}
                      className="inline-flex items-center gap-2 rounded-2xl border border-gray-700 bg-[#12181f] px-3 py-2 text-xs text-gray-300 transition hover:border-purple-500 hover:text-white"
                    >
                      <item.icon className="h-4 w-4" />
                      {item.label}
                    </button>
                  ))}
                </div>
                <input
                  ref={fileRef}
                  type="file"
                  accept="image/*,video/*,.pdf,.zip,.rar"
                  className="hidden"
                  onChange={handleAttach}
                />
                {attachment && (
                  <div className="mt-3 flex items-center justify-between rounded-2xl border border-gray-700 bg-[#12181f] px-3 py-2 text-sm text-gray-300">
                    <span className="truncate">{attachment.name}</span>
                    <span className="text-xs text-gray-500">{attachment.size}</span>
                  </div>
                )}
              </div>

              <button
                type="button"
                disabled={!message && !attachment}
                onClick={handleSave}
                className="flex w-full items-center justify-center gap-2 rounded-2xl bg-purple-600 px-4 py-3 text-sm font-semibold text-white transition hover:bg-purple-500 disabled:cursor-not-allowed disabled:opacity-50"
              >
                <Send className="h-4 w-4" />
                {saved ? 'Sent!' : 'Send Message'}
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
