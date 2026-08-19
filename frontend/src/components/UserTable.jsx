import { Plus, Pencil, Search, Trash2, ArrowLeft, ArrowRight, Ban, UserCheck } from 'lucide-react';
import SortableTableHeader from './ui/SortableTableHeader';

const HEADER_CLASS = 'px-6 py-3 text-left text-xs font-semibold text-foreground-muted uppercase tracking-wider';

const USER_COLUMNS = [
  { key: 'irn', label: 'IRN' },
  { key: 'fullname', label: 'Full Name' },
  { key: 'dob', label: 'Date of Birth' },
  { key: 'email', label: 'Email' },
  { key: 'role', label: 'Role' },
  { key: 'status', label: 'Status' },
];

function isStudentOnly(user) {
  const roles = user?.roleNames || [];
  return roles.includes('STUDENT') && !roles.includes('LECTURER');
}

export default function UserTable({
  search,
  onSearchChange,
  onCreate,
  rows,
  users,
  totalItems,
  currentPage,
  pageSize,
  onPageChange,
  onSort,
  sortState,
  loading,
  onEdit,
  onDelete,
  onSuspend,
  roleColors,
}) {
  const pageCount = Math.max(1, Math.ceil(totalItems / pageSize));

  return (
    <div className="bg-surface rounded-2xl border border-border overflow-hidden">
      <div className="px-6 py-4 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between border-b border-border">
        <div className="flex items-center gap-3">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-foreground-muted" />
            <input
              type="text"
              placeholder="Search by IRN, name or email…"
              value={search}
              onChange={(e) => onSearchChange(e.target.value)}
              className="pl-9 pr-4 py-2 w-72 bg-surface-secondary bg-surface-secondary border border-border rounded-lg text-sm text-foreground placeholder-foreground-disabled focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </div>
        </div>
        <button
          onClick={onCreate}
          className="flex items-center gap-2 px-4 py-2 bg-primary hover:bg-primary-hover text-white rounded-lg text-sm font-medium transition-colors shadow-md shadow-primary/20"
        >
          <Plus className="w-4 h-4" />
          Add User
        </button>
      </div>

      <div className="overflow-x-auto">
        <table className="w-full table-auto text-sm min-w-full">
          <thead>
            <tr className="border-b border-border">
              {USER_COLUMNS.map((col) => (
                <SortableTableHeader
                  key={col.key}
                  label={col.label}
                  field={col.key}
                  activeField={sortState?.field}
                  direction={sortState?.direction}
                  onSort={onSort}
                  className={HEADER_CLASS}
                />
              ))}
              <SortableTableHeader label="Actions" sortable={false} className={HEADER_CLASS} />
            </tr>
          </thead>
          <tbody className="divide-y divide-border">
            {loading ? (
              <tr>
                <td colSpan={7} className="px-6 py-12 text-center text-foreground-muted">
                  Loading users...
                </td>
              </tr>
            ) : rows.length === 0 ? (
              <tr>
                <td colSpan={7} className="px-6 py-12 text-center text-foreground-disabled">No users found.</td>
              </tr>
            ) : rows.map((u) => (
              <tr key={u.id} className="hover:bg-surface-secondary hover:bg-surface-secondary transition-colors">
                <td className="px-6 py-4 font-mono text-foreground-secondary break-words">{u.irn}</td>
                <td className="px-6 py-4 text-foreground font-medium break-words">{u.fullname}</td>
                <td className="px-6 py-4 text-foreground-muted break-words">{u.dob}</td>
                <td className="px-6 py-4 text-foreground-muted break-words">{u.email}</td>
                <td className="px-6 py-4">
                  <div className="flex flex-wrap gap-1.5">
                    {(u.roleNames || (u.roles || []).map((role) => role?.name).filter(Boolean)).map((roleName) => {
                      const normalized = String(roleName).toUpperCase() === 'TEACHER' ? 'LECTURER' : String(roleName).toUpperCase();
                      return (
                        <span
                          key={`${u.id}-${normalized}`}
                          className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${roleColors[normalized] || roleColors.STUDENT}`}
                        >
                          {normalized}
                        </span>
                      );
                    })}
                  </div>
                </td>
                <td className="px-6 py-4">
                  <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                    u.isActive === false
                      ? 'bg-warning-bg text-warning-text'
                      : 'bg-success-bg text-success-text'
                  }`}>
                    {u.isActive === false ? 'Suspended' : 'Active'}
                  </span>
                </td>
                <td className="px-6 py-4">
                  <div className="flex items-center gap-2">
                    <button onClick={() => onEdit(u)} className="p-1.5 text-foreground-muted hover:text-primary hover:bg-primary-light rounded-lg transition-colors">
                      <Pencil className="w-4 h-4" />
                    </button>
                    {isStudentOnly(u) && onSuspend && (
                      <button
                        onClick={() => onSuspend(u)}
                        title={u.isActive === false ? 'Restore student' : 'Suspend student'}
                        className="p-1.5 text-foreground-muted hover:text-warning-text hover:bg-warning-bg rounded-lg transition-colors"
                      >
                        {u.isActive === false ? <UserCheck className="w-4 h-4" /> : <Ban className="w-4 h-4" />}
                      </button>
                    )}
                    <button onClick={() => onDelete(u)} className="p-1.5 text-foreground-muted hover:text-error hover:bg-error-bg rounded-lg transition-colors">
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="px-6 py-4 border-t border-border bg-surface-secondary bg-surface-secondary flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="text-xs text-foreground-muted">
          {loading ? 'Loading users...' : `Showing ${rows.length} of ${totalItems} matching users`}
        </div>
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => onPageChange(Math.max(1, currentPage - 1))}
            disabled={currentPage === 1}
            className="inline-flex items-center gap-2 rounded-lg border border-border bg-surface px-3 py-2 text-xs font-medium text-foreground-secondary disabled:cursor-not-allowed disabled:opacity-50 hover:bg-surface-tertiary"
          >
            <ArrowLeft className="w-4 h-4" />
            Previous
          </button>
          <span className="text-xs text-foreground-muted">Page {currentPage} of {pageCount}</span>
          <button
            type="button"
            onClick={() => onPageChange(Math.min(pageCount, currentPage + 1))}
            disabled={currentPage === pageCount}
            className="inline-flex items-center gap-2 rounded-lg border border-border bg-surface px-3 py-2 text-xs font-medium text-foreground-secondary disabled:cursor-not-allowed disabled:opacity-50 hover:bg-surface-tertiary"
          >
            Next
            <ArrowRight className="w-4 h-4" />
          </button>
        </div>
      </div>
    </div>
  );
}
