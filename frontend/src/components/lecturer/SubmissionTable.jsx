import React from 'react';
import { Eye } from 'lucide-react';
import { formatNumber, formatPercent, formatText } from '../../utils/formatters';

export default function SubmissionTable({ submissions, summary, pagination, onPageChange }) {
  const rows = Array.isArray(submissions) ? submissions : [];

  return (
    <div className="overflow-x-auto rounded-xl border border-gray-200 bg-white shadow-sm transition-colors dark:border-gray-700 dark:bg-[#1e2530]">
      <table className="w-full table-auto">
        <thead>
          <tr className="border-b border-gray-200 dark:border-gray-700">
            {['Student', 'ID', 'Score', 'Attempt', 'Submitted At', 'Best', 'Action'].map((col) => (
              <th key={col} className="px-4 py-3 text-left text-sm font-medium text-gray-700 dark:text-gray-300">
                {col}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.length === 0 ? (
            <tr>
              <td colSpan={7} className="px-4 py-10 text-center text-sm text-gray-500 dark:text-gray-400">
                No student data found
              </td>
            </tr>
          ) : (
            rows.map((submission, index) => (
              <tr key={`${submission.studentCode ?? 'row'}-${index}`} className="border-b border-gray-200 dark:border-gray-700">
                <td className="px-4 py-3 text-sm text-gray-800 dark:text-gray-200">{formatText(submission.studentName)}</td>
                <td className="px-4 py-3 text-sm text-gray-800 dark:text-gray-200">{formatText(submission.studentCode)}</td>
                <td className="px-4 py-3 text-sm font-semibold text-gray-900 dark:text-white">{formatPercent(submission.score)}</td>
                <td className="px-4 py-3 text-sm text-gray-800 dark:text-gray-200">{formatNumber(submission.attempt)}</td>
                <td className="px-4 py-3 text-sm text-gray-800 dark:text-gray-200">{formatText(submission.submittedAt)}</td>
                <td className="px-4 py-3 text-sm text-gray-800 dark:text-gray-200">
                  {submission.bestSubmission ? 'Yes' : 'No'}
                </td>
                <td className="px-4 py-3">
                  <button className="flex items-center gap-1 rounded-lg bg-green-600 px-3 py-1.5 text-xs text-white transition-colors hover:bg-green-700" type="button">
                    <Eye className="h-3 w-3" />
                    View Details
                  </button>
                </td>
              </tr>
            ))
          )}

          <tr className="border-t border-blue-400 bg-blue-50 dark:border-blue-600 dark:bg-blue-900/20">
            <td className="px-4 py-4 text-sm font-bold text-blue-900 dark:text-blue-100">SUMMARY</td>
            <td className="px-4 py-4 text-sm font-semibold text-blue-800 dark:text-blue-200">
              Submissions: <span className="text-blue-900 dark:text-blue-100">{formatNumber(summary?.submissionCount)}</span>
            </td>
            <td className="px-4 py-4 text-sm font-semibold text-blue-800 dark:text-blue-200">
              Average: <span className="text-blue-900 dark:text-blue-100">{formatPercent(summary?.averageScore)}</span>
            </td>
            <td className="px-4 py-4 text-sm font-semibold text-blue-800 dark:text-blue-200">
              Lowest: <span className="text-blue-900 dark:text-blue-100">{formatPercent(summary?.lowestScore)}</span>
            </td>
            <td className="px-4 py-4 text-sm font-semibold text-blue-800 dark:text-blue-200">
              Students: <span className="text-blue-900 dark:text-blue-100">{formatNumber(summary?.studentCount)}</span>
            </td>
            <td className="px-4 py-4 text-sm font-semibold text-blue-800 dark:text-blue-200">
              Completion: <span className="text-blue-900 dark:text-blue-100">{formatPercent(summary?.completionRate)}</span>
            </td>
            <td className="px-4 py-4" />
          </tr>
        </tbody>
      </table>

      {pagination && pagination.totalPages > 1 && (
        <div className="flex items-center justify-between border-t border-gray-200 px-4 py-3 dark:border-gray-700">
          <p className="text-sm text-gray-500 dark:text-gray-400">
            Page {pagination.page + 1} of {pagination.totalPages}
          </p>
          <div className="flex gap-2">
            <button
              type="button"
              disabled={pagination.page <= 0}
              onClick={() => onPageChange?.(pagination.page - 1)}
              className="rounded-lg border border-gray-200 px-3 py-1.5 text-sm disabled:opacity-50 dark:border-gray-700"
            >
              Previous
            </button>
            <button
              type="button"
              disabled={pagination.page >= pagination.totalPages - 1}
              onClick={() => onPageChange?.(pagination.page + 1)}
              className="rounded-lg border border-gray-200 px-3 py-1.5 text-sm disabled:opacity-50 dark:border-gray-700"
            >
              Next
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
