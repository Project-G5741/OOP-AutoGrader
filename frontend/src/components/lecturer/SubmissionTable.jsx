import React from 'react';
import { Eye } from 'lucide-react';

export default function SubmissionTable({ submissions, summary }) {
  return (
    <div className="overflow-x-auto rounded-xl border border-gray-200 bg-white shadow-sm transition-colors dark:border-gray-700 dark:bg-[#1e2530]">
      <table className="min-w-full">
        <thead>
          <tr className="border-b border-gray-200 dark:border-gray-700">
            {['Student', 'ID', 'Score', 'Failed Classes', 'Failed Testcases', 'Action'].map((col) => (
              <th key={col} className="px-4 py-3 text-left text-sm font-medium text-gray-700 dark:text-gray-300">
                {col}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {submissions.map((submission, index) => (
            <tr key={`${submission.id}-${index}`} className="border-b border-gray-200 dark:border-gray-700">
              <td className="px-4 py-3 text-sm text-gray-800 dark:text-gray-200">{submission.student}</td>
              <td className="px-4 py-3 text-sm text-gray-800 dark:text-gray-200">{submission.id}</td>
              <td className="px-4 py-3 text-sm font-semibold text-gray-900 dark:text-white">{submission.score}</td>
              <td className="px-4 py-3 text-sm font-medium text-green-600 dark:text-green-500">
                {submission.failedClasses.length > 0 ? submission.failedClasses.join(', ') : 'None'}
              </td>
              <td className="px-4 py-3 text-sm font-medium text-purple-600 dark:text-purple-500">
                {submission.failedTests.length > 0 ? submission.failedTests.join(', ') : 'None'}
              </td>
              <td className="px-4 py-3">
                <button className="flex items-center gap-1 rounded-lg bg-green-600 px-3 py-1.5 text-xs text-white transition-colors hover:bg-green-700" type="button">
                  <Eye className="h-3 w-3" />
                  View Details
                </button>
              </td>
            </tr>
          ))}

          <tr className="border-t border-blue-400 bg-blue-50 dark:border-blue-600 dark:bg-blue-900/20">
            <td className="px-4 py-4 text-sm font-bold text-blue-900 dark:text-blue-100">SUMMARY</td>
            <td className="px-4 py-4 text-sm font-semibold text-blue-800 dark:text-blue-200">
              Average: <span className="text-blue-900 dark:text-blue-100">{summary.averageScore}</span>
            </td>
            <td className="px-4 py-4 text-sm font-semibold text-blue-800 dark:text-blue-200">
              Lowest: <span className="text-blue-900 dark:text-blue-100">{summary.lowestScore}</span>
            </td>
            <td className="px-4 py-4 text-sm font-bold text-green-700 dark:text-green-400">Most failed: {summary.mostFailedClass}</td>
            <td className="px-4 py-4 text-sm font-bold text-purple-700 dark:text-purple-400">Most failed: {summary.mostFailedTest}</td>
            <td className="px-4 py-4" />
          </tr>
        </tbody>
      </table>
    </div>
  );
}
