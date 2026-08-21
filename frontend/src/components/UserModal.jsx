import { Check, Trash2, X, Ban, UserCheck } from 'lucide-react';

const ROLE_OPTIONS = [
  { value: 'STUDENT', label: 'STUDENT' },
  { value: 'LECTURER', label: 'LECTURER' },
];

const inputClass = (hasError) =>
  `w-full px-3 py-2.5 bg-surface-secondary bg-surface-secondary border rounded-lg text-foreground placeholder-foreground-disabled focus:outline-none focus:ring-2 text-sm transition-all ${
    hasError
      ? 'border-error focus:ring-error'
      : 'border-border focus:ring-primary'
  }`;

export default function UserModal({
  modal,
  selected,
  form,
  fieldErrors = {},
  formError = '',
  canSave = false,
  isDark,
  onClose,
  onSave,
  onDelete,
  onSuspendToggle,
  onFieldChange,
  onRoleToggle,
}) {
  if (!modal) return null;

  const roles = form.roles || [];
  const hasStudent = roles.includes('STUDENT');
  const hasLecturer = roles.includes('LECTURER');

  return (
    <div className={isDark ? 'dark' : ''}>
      <div className="fixed inset-0 bg-black/50 dark:bg-black/70 flex items-center justify-center z-50 p-4">
        {modal === 'delete' && selected && (
          <div className="bg-surface rounded-2xl shadow-2xl p-6 w-full max-w-sm border border-border">
            <div className="flex items-center justify-center w-12 h-12 bg-error-bg rounded-xl mb-4 mx-auto">
              <Trash2 className="w-6 h-6 text-error" />
            </div>
            <h3 className="text-center text-foreground font-semibold mb-1">Delete User</h3>
            <p className="text-center text-sm text-foreground-muted mb-6">
              Are you sure you want to delete <strong className="text-foreground-secondary">{selected.fullname}</strong>? This cannot be undone.
            </p>
            <div className="flex gap-3">
              <button onClick={onClose} className="flex-1 py-2.5 border border-border text-foreground-secondary rounded-lg text-sm hover:bg-surface-secondary hover:bg-surface-secondary transition-colors">Cancel</button>
              <button onClick={onDelete} className="flex-1 py-2.5 bg-error hover:bg-error-hover text-white rounded-lg text-sm font-medium transition-colors">Delete</button>
            </div>
          </div>
        )}

        {(modal === 'suspend' || modal === 'restore') && selected && (
          <div className="bg-surface rounded-2xl shadow-2xl p-6 w-full max-w-sm border border-border">
            <div className={`flex items-center justify-center w-12 h-12 rounded-xl mb-4 mx-auto ${
              modal === 'suspend' ? 'bg-warning-bg' : 'bg-success-bg'
            }`}>
              {modal === 'suspend'
                ? <Ban className="w-6 h-6 text-warning-text" />
                : <UserCheck className="w-6 h-6 text-success-text" />}
            </div>
            <h3 className="text-center text-foreground font-semibold mb-1">
              {modal === 'suspend' ? 'Suspend student' : 'Restore student'}
            </h3>
            <p className="text-center text-sm text-foreground-muted mb-6">
              {modal === 'suspend'
                ? <>Suspend <strong className="text-foreground-secondary">{selected.fullname}</strong>? They will not be able to log in until restored.</>
                : <>Restore <strong className="text-foreground-secondary">{selected.fullname}</strong>? They will be able to log in again.</>}
            </p>
            <div className="flex gap-3">
              <button onClick={onClose} className="flex-1 py-2.5 border border-border text-foreground-secondary rounded-lg text-sm hover:bg-surface-secondary transition-colors">Cancel</button>
              <button
                onClick={onSuspendToggle}
                className={`flex-1 py-2.5 text-white rounded-lg text-sm font-medium transition-colors ${
                  modal === 'suspend' ? 'bg-warning hover:bg-warning-hover' : 'bg-success hover:bg-success-hover'
                }`}
              >
                {modal === 'suspend' ? 'Suspend' : 'Restore'}
              </button>
            </div>
          </div>
        )}

        {(modal === 'create' || modal === 'edit') && (
          <div className="bg-surface rounded-2xl shadow-2xl w-full max-w-lg border border-border">
            <div className="flex items-center justify-between px-6 py-4 border-b border-border">
              <h3 className="text-foreground font-semibold">{modal === 'create' ? 'Add New User' : 'Edit User'}</h3>
              <button onClick={onClose} className="p-1.5 text-foreground-muted hover:text-foreground-secondary hover:bg-surface-secondary hover:bg-surface-secondary rounded-lg transition-colors">
                <X className="w-4 h-4" />
              </button>
            </div>
            <div className="px-6 py-5 space-y-4">
              <div>
                <label className="block text-sm font-medium text-foreground-secondary mb-2">Roles</label>
                <div className="flex flex-wrap gap-4">
                  {ROLE_OPTIONS.map(({ value, label }) => (
                    <label key={value} className="inline-flex items-center gap-2 text-sm text-foreground-secondary">
                      <input
                        type="checkbox"
                        checked={roles.includes(value)}
                        onChange={() => onRoleToggle(value)}
                        className="rounded border-border text-primary focus:ring-primary"
                      />
                      {label}
                    </label>
                  ))}
                </div>
                {fieldErrors.roles ? (
                  <p className="mt-1.5 text-xs text-error">{fieldErrors.roles}</p>
                ) : (
                  <p className="mt-1.5 text-xs text-foreground-muted">
                    Select roles first — IRN fields appear based on your selection.
                  </p>
                )}
              </div>

              {hasStudent && (
                <div>
                  <label className="block text-sm font-medium text-foreground-secondary mb-1.5">Student IRN</label>
                  <input
                    type="text"
                    value={form.studentIrn || ''}
                    onChange={(e) => onFieldChange('studentIrn', e.target.value)}
                    placeholder="e.g. 2052123456"
                    className={inputClass(fieldErrors.studentIrn)}
                  />
                  {fieldErrors.studentIrn && (
                    <p className="mt-1 text-xs text-error">{fieldErrors.studentIrn}</p>
                  )}
                </div>
              )}

              {hasLecturer && (
                <div>
                  <label className="block text-sm font-medium text-foreground-secondary mb-1.5">Lecturer IRN</label>
                  <input
                    type="text"
                    value={form.lecturerIrn || ''}
                    onChange={(e) => onFieldChange('lecturerIrn', e.target.value)}
                    placeholder="e.g. lan.cao"
                    className={inputClass(fieldErrors.lecturerIrn)}
                  />
                  {fieldErrors.lecturerIrn && (
                    <p className="mt-1 text-xs text-error">{fieldErrors.lecturerIrn}</p>
                  )}
                </div>
              )}

              {[
                { label: 'Full Name', key: 'fullname', type: 'text', placeholder: 'Enter full name' },
                { label: 'Email', key: 'email', type: 'email', placeholder: 'user@eiu.edu.vn' },
                { label: 'Password', key: 'password', type: 'password', placeholder: modal === 'edit' ? 'Leave blank to keep current password' : 'Enter password' },
              ].map(({ label, key, type, placeholder }) => (
                <div key={key}>
                  <label className="block text-sm font-medium text-foreground-secondary mb-1.5">{label}</label>
                  <input
                    type={type}
                    value={form[key] || ''}
                    onChange={(e) => onFieldChange(key, e.target.value)}
                    placeholder={placeholder}
                    className={inputClass(fieldErrors[key])}
                  />
                  {fieldErrors[key] && (
                    <p className="mt-1 text-xs text-error">{fieldErrors[key]}</p>
                  )}
                </div>
              ))}

              {formError && (
                <p className="text-sm text-error">{formError}</p>
              )}
            </div>
            <div className="flex gap-3 px-6 py-4 border-t border-border">
              <button onClick={onClose} className="flex-1 py-2.5 border border-border text-foreground-secondary rounded-lg text-sm hover:bg-surface-secondary hover:bg-surface-secondary transition-colors">Cancel</button>
              <button
                onClick={onSave}
                disabled={!canSave}
                className="flex-1 py-2.5 bg-primary hover:bg-primary-hover disabled:opacity-50 disabled:cursor-not-allowed text-white rounded-lg text-sm font-medium transition-colors flex items-center justify-center gap-2"
              >
                <Check className="w-4 h-4" />
                {modal === 'create' ? 'Create User' : 'Save Changes'}
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
