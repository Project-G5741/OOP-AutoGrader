import { Plus, Pencil, Search, Trash2, ArrowLeft, ArrowRight, ChevronUp, ChevronDown } from 'lucide-react';

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
  sortDirection,
  loading,
  onEdit,
  onDelete,
  roleColors,
}) {
  const pageCount = Math.max(1, Math.ceil(totalItems / pageSize));

  return (
    <div className="bg-white dark:bg-[#1e2530] rounded-2xl border border-gray-100 dark:border-gray-800 overflow-hidden">
      <div className="px-6 py-4 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between border-b border-gray-100 dark:border-gray-800">
        <div className="flex items-center gap-3">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
            <input
              type="text"
              placeholder="Search by IRN, name or email…"
              value={search}
              onChange={(e) => onSearchChange(e.target.value)}
              className="pl-9 pr-4 py-2 w-72 bg-gray-50 dark:bg-[#151b24] border border-gray-200 dark:border-gray-700 rounded-lg text-sm text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-600 focus:outline-none focus:ring-2 focus:ring-purple-500"
            />
          </div>
          <button
            onClick={onSort}
            type="button"
            className="inline-flex items-center gap-2 rounded-lg border border-gray-200 bg-gray-50 px-3 py-2 text-sm font-medium text-gray-700 hover:bg-gray-100 dark:border-gray-700 dark:bg-[#151b24] dark:text-gray-200 dark:hover:bg-[#1a1a2c] transition-colors"
          >
            {sortDirection === 'asc' ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
            Sort by name
          </button>
        </div>
        <button
          onClick={onCreate}
          className="flex items-center gap-2 px-4 py-2 bg-purple-600 hover:bg-purple-700 text-white rounded-lg text-sm font-medium transition-colors shadow-md shadow-purple-500/20"
        >
          <Plus className="w-4 h-4" />
          Add User
        </button>
      </div>

      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-gray-100 dark:border-gray-800">
              {['IRN', 'Full Name', 'Date of Birth', 'Email', 'Role', 'Actions'].map((h) => (
                <th key={h} className="px-6 py-3 text-left text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                  {h}
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-50 dark:divide-gray-800">
            {loading ? (
              <tr>
                <td colSpan={6} className="px-6 py-12 text-center text-gray-500 dark:text-gray-400">
                  Loading users...
                </td>
              </tr>
            ) : rows.length === 0 ? (
              <tr>
                <td colSpan={6} className="px-6 py-12 text-center text-gray-400 dark:text-gray-600">No users found.</td>
              </tr>
            ) : rows.map((u) => (
              <tr key={u.id} className="hover:bg-gray-50 dark:hover:bg-[#151b24] transition-colors">
                <td className="px-6 py-4 font-mono text-gray-700 dark:text-gray-300">{u.irn}</td>
                <td className="px-6 py-4 text-gray-900 dark:text-white font-medium">{u.fullname}</td>
                <td className="px-6 py-4 text-gray-600 dark:text-gray-400">{u.dob}</td>
                <td className="px-6 py-4 text-gray-600 dark:text-gray-400">{u.email}</td>
                <td className="px-6 py-4">
                  <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${roleColors[u.role]}`}>
                    {u.role}
                  </span>
                </td>
                <td className="px-6 py-4">
                  <div className="flex items-center gap-2">
                    <button onClick={() => onEdit(u)} className="p-1.5 text-gray-400 hover:text-purple-600 dark:hover:text-purple-400 hover:bg-purple-50 dark:hover:bg-purple-900/20 rounded-lg transition-colors">
                      <Pencil className="w-4 h-4" />
                    </button>
                    <button onClick={() => onDelete(u)} className="p-1.5 text-gray-400 hover:text-red-600 dark:hover:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/20 rounded-lg transition-colors">
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="px-6 py-4 border-t border-gray-100 dark:border-gray-800 bg-gray-50 dark:bg-[#151b24] flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="text-xs text-gray-500 dark:text-gray-400">
          {loading ? 'Loading users...' : `Showing ${rows.length} of ${totalItems} matching users`}
        </div>
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => onPageChange(Math.max(1, currentPage - 1))}
            disabled={currentPage === 1}
            className="inline-flex items-center gap-2 rounded-lg border border-gray-200 bg-white px-3 py-2 text-xs font-medium text-gray-700 disabled:cursor-not-allowed disabled:opacity-50 hover:bg-gray-100 dark:border-gray-700 dark:bg-[#1a1a2c] dark:text-gray-200"
          >
            <ArrowLeft className="w-4 h-4" />
            Previous
          </button>
          <span className="text-xs text-gray-600 dark:text-gray-400">Page {currentPage} of {pageCount}</span>
          <button
            type="button"
            onClick={() => onPageChange(Math.min(pageCount, currentPage + 1))}
            disabled={currentPage === pageCount}
            className="inline-flex items-center gap-2 rounded-lg border border-gray-200 bg-white px-3 py-2 text-xs font-medium text-gray-700 disabled:cursor-not-allowed disabled:opacity-50 hover:bg-gray-100 dark:border-gray-700 dark:bg-[#1a1a2c] dark:text-gray-200"
          >
            Next
            <ArrowRight className="w-4 h-4" />
          </button>
        </div>
      </div>
    </div>
  );
}
