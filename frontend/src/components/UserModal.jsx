import { Check, Trash2, X } from 'lucide-react';

const ROLE_OPTIONS = [
  { value: 'STUDENT', label: 'STUDENT' },
  { value: 'LECTURER', label: 'LECTURER' },
];

const inputClass = (hasError) =>
  `w-full px-3 py-2.5 bg-gray-50 dark:bg-[#151b24] border rounded-lg text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-600 focus:outline-none focus:ring-2 text-sm transition-all ${
    hasError
      ? 'border-red-500 focus:ring-red-400'
      : 'border-gray-200 dark:border-gray-700 focus:ring-purple-500'
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
          <div className="bg-white dark:bg-[#1e2530] rounded-2xl shadow-2xl p-6 w-full max-w-sm border border-gray-100 dark:border-gray-800">
            <div className="flex items-center justify-center w-12 h-12 bg-red-100 dark:bg-red-900/30 rounded-xl mb-4 mx-auto">
              <Trash2 className="w-6 h-6 text-red-500" />
            </div>
            <h3 className="text-center text-gray-900 dark:text-white font-semibold mb-1">Delete User</h3>
            <p className="text-center text-sm text-gray-500 dark:text-gray-400 mb-6">
              Are you sure you want to delete <strong className="text-gray-700 dark:text-gray-200">{selected.fullname}</strong>? This cannot be undone.
            </p>
            <div className="flex gap-3">
              <button onClick={onClose} className="flex-1 py-2.5 border border-gray-200 dark:border-gray-700 text-gray-700 dark:text-gray-300 rounded-lg text-sm hover:bg-gray-50 dark:hover:bg-[#151b24] transition-colors">Cancel</button>
              <button onClick={onDelete} className="flex-1 py-2.5 bg-red-600 hover:bg-red-700 text-white rounded-lg text-sm font-medium transition-colors">Delete</button>
            </div>
          </div>
        )}

        {(modal === 'create' || modal === 'edit') && (
          <div className="bg-white dark:bg-[#1e2530] rounded-2xl shadow-2xl w-full max-w-lg border border-gray-100 dark:border-gray-800">
            <div className="flex items-center justify-between px-6 py-4 border-b border-gray-100 dark:border-gray-800">
              <h3 className="text-gray-900 dark:text-white font-semibold">{modal === 'create' ? 'Add New User' : 'Edit User'}</h3>
              <button onClick={onClose} className="p-1.5 text-gray-400 hover:text-gray-600 dark:hover:text-gray-200 hover:bg-gray-100 dark:hover:bg-[#151b24] rounded-lg transition-colors">
                <X className="w-4 h-4" />
              </button>
            </div>
            <div className="px-6 py-5 space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">Roles</label>
                <div className="flex flex-wrap gap-4">
                  {ROLE_OPTIONS.map(({ value, label }) => (
                    <label key={value} className="inline-flex items-center gap-2 text-sm text-gray-700 dark:text-gray-300">
                      <input
                        type="checkbox"
                        checked={roles.includes(value)}
                        onChange={() => onRoleToggle(value)}
                        className="rounded border-gray-300 text-purple-600 focus:ring-purple-500"
                      />
                      {label}
                    </label>
                  ))}
                </div>
                {fieldErrors.roles ? (
                  <p className="mt-1.5 text-xs text-red-500">{fieldErrors.roles}</p>
                ) : (
                  <p className="mt-1.5 text-xs text-gray-500 dark:text-gray-400">
                    Select roles first — IRN fields appear based on your selection.
                  </p>
                )}
              </div>

              {hasStudent && (
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">Student IRN</label>
                  <input
                    type="text"
                    value={form.studentIrn || ''}
                    onChange={(e) => onFieldChange('studentIrn', e.target.value)}
                    placeholder="e.g. 2052123456"
                    className={inputClass(fieldErrors.studentIrn)}
                  />
                  {fieldErrors.studentIrn && (
                    <p className="mt-1 text-xs text-red-500">{fieldErrors.studentIrn}</p>
                  )}
                </div>
              )}

              {hasLecturer && (
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">Lecturer IRN</label>
                  <input
                    type="text"
                    value={form.lecturerIrn || ''}
                    onChange={(e) => onFieldChange('lecturerIrn', e.target.value)}
                    placeholder="e.g. lan.cao"
                    className={inputClass(fieldErrors.lecturerIrn)}
                  />
                  {fieldErrors.lecturerIrn && (
                    <p className="mt-1 text-xs text-red-500">{fieldErrors.lecturerIrn}</p>
                  )}
                </div>
              )}

              {[
                { label: 'Full Name', key: 'fullname', type: 'text', placeholder: 'Enter full name' },
                { label: 'Email', key: 'email', type: 'email', placeholder: 'user@eiu.edu.vn' },
                { label: 'Password', key: 'password', type: 'password', placeholder: modal === 'edit' ? 'Leave blank to keep current password' : 'Enter password' },
              ].map(({ label, key, type, placeholder }) => (
                <div key={key}>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">{label}</label>
                  <input
                    type={type}
                    value={form[key] || ''}
                    onChange={(e) => onFieldChange(key, e.target.value)}
                    placeholder={placeholder}
                    className={inputClass(fieldErrors[key])}
                  />
                  {fieldErrors[key] && (
                    <p className="mt-1 text-xs text-red-500">{fieldErrors[key]}</p>
                  )}
                </div>
              ))}

              {formError && (
                <p className="text-sm text-red-500">{formError}</p>
              )}
            </div>
            <div className="flex gap-3 px-6 py-4 border-t border-gray-100 dark:border-gray-800">
              <button onClick={onClose} className="flex-1 py-2.5 border border-gray-200 dark:border-gray-700 text-gray-700 dark:text-gray-300 rounded-lg text-sm hover:bg-gray-50 dark:hover:bg-[#151b24] transition-colors">Cancel</button>
              <button
                onClick={onSave}
                disabled={!canSave}
                className="flex-1 py-2.5 bg-purple-600 hover:bg-purple-700 disabled:opacity-50 disabled:cursor-not-allowed text-white rounded-lg text-sm font-medium transition-colors flex items-center justify-center gap-2"
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
