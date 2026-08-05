import { ArrowUpRight, BarChart3, TrendingUp, AlertTriangle } from 'lucide-react';
import { formatNumber, formatPercent, formatText, hasItems } from '../../utils/formatters';

function EmptyState({ message = 'Data not found' }) {
  return (
    <div className="flex min-h-[120px] items-center justify-center rounded-2xl border border-dashed border-gray-200 bg-gray-50 p-6 text-sm text-gray-500 dark:border-gray-700 dark:bg-[#151b24] dark:text-gray-400">
      {message}
    </div>
  );
}

export default function ReportsPanel({ reportData }) {
  const data = reportData ?? {};
  const aiSummary = data.aiSummary ?? {};
  const recommendedResources = aiSummary.recommendedResources ?? [];

  return (
    <div className="grid gap-6 xl:grid-cols-[1.2fr_0.8fr]">
      <div className="space-y-6">
        <div className="rounded-3xl border border-gray-200 bg-white p-6 shadow-sm transition-colors dark:border-gray-700 dark:bg-[#1e2530]">
          <div className="flex items-center justify-between gap-4">
            <div>
              <p className="text-sm font-semibold uppercase tracking-[0.2em] text-gray-500 dark:text-gray-400">
                Analytics summary
              </p>
              <h3 className="mt-2 text-xl font-semibold text-gray-900 dark:text-white">Lecturer reports</h3>
            </div>
            <BarChart3 className="h-6 w-6 text-purple-600" />
          </div>

          <div className="mt-6 grid gap-4 sm:grid-cols-2">
            <div className="rounded-3xl border border-gray-200 bg-gray-50 p-4 dark:border-gray-700 dark:bg-[#151b24]">
              <p className="text-xs text-gray-400 dark:text-gray-500">Overall average</p>
              <p className="mt-2 text-2xl font-semibold text-gray-900 dark:text-white">
                {formatPercent(data.overallAverage)}
              </p>
            </div>
            <div className="rounded-3xl border border-gray-200 bg-gray-50 p-4 dark:border-gray-700 dark:bg-[#151b24]">
              <p className="text-xs text-gray-400 dark:text-gray-500">Lowest average lab</p>
              <p className="mt-2 text-2xl font-semibold text-gray-900 dark:text-white">
                {formatText(data.lowestAverageLab)}
              </p>
            </div>
            <div className="rounded-3xl border border-gray-200 bg-gray-50 p-4 dark:border-gray-700 dark:bg-[#151b24]">
              <p className="text-xs text-gray-400 dark:text-gray-500">Lowest average score</p>
              <p className="mt-2 text-2xl font-semibold text-gray-900 dark:text-white">
                {formatPercent(data.lowestAverageScore)}
              </p>
            </div>
            <div className="rounded-3xl border border-gray-200 bg-gray-50 p-4 dark:border-gray-700 dark:bg-[#151b24]">
              <p className="text-xs text-gray-400 dark:text-gray-500">Most difficult topic</p>
              <p className="mt-2 text-2xl font-semibold text-gray-900 dark:text-white">
                {formatText(data.mostDifficultTopic)}
              </p>
            </div>
          </div>
        </div>

        <div className="rounded-3xl border border-gray-200 bg-white p-6 shadow-sm transition-colors dark:border-gray-700 dark:bg-[#1e2530]">
          <div className="flex items-center justify-between gap-4">
            <div>
              <p className="text-sm font-semibold uppercase tracking-[0.2em] text-gray-500 dark:text-gray-400">
                Lab performance trend
              </p>
              <h3 className="mt-2 text-xl font-semibold text-gray-900 dark:text-white">Average scores by lab</h3>
            </div>
            <TrendingUp className="h-6 w-6 text-emerald-600" />
          </div>

          <div className="mt-6 space-y-3">
            {hasItems(data.labTrend) ? (
              data.labTrend.map((item) => (
                <div key={item.labId ?? item.labName} className="flex items-center justify-between gap-4 rounded-3xl border border-gray-100 bg-gray-50 p-4 dark:border-gray-700 dark:bg-[#151b24]">
                  <div className="min-w-0">
                    <p className="text-sm font-semibold text-gray-900 dark:text-white">{formatText(item.labName)}</p>
                    <p className="text-sm text-gray-500 dark:text-gray-400">
                      {formatNumber(item.submissionCount)} submissions
                    </p>
                  </div>
                  <div className="text-right">
                    <p className="text-lg font-semibold text-gray-900 dark:text-white">{formatPercent(item.averageScore)}</p>
                    <p className="text-xs text-gray-500 dark:text-gray-400">Average score</p>
                  </div>
                </div>
              ))
            ) : (
              <EmptyState message="No data available" />
            )}
          </div>
        </div>
      </div>

      <div className="space-y-6">
        <div className="rounded-3xl border border-gray-200 bg-white p-6 shadow-sm transition-colors dark:border-gray-700 dark:bg-[#1e2530]">
          <div className="flex items-center justify-between gap-4">
            <div>
              <p className="text-sm font-semibold uppercase tracking-[0.2em] text-gray-500 dark:text-gray-400">
                At-risk measures
              </p>
              <h3 className="mt-2 text-xl font-semibold text-gray-900 dark:text-white">Labs & students</h3>
            </div>
            <AlertTriangle className="h-6 w-6 text-red-600" />
          </div>

          <div className="mt-6 space-y-4">
            <div>
              <p className="text-xs text-gray-400 dark:text-gray-500">At-risk labs</p>
              {hasItems(data.atRiskLabs) ? (
                data.atRiskLabs.map((lab) => (
                  <div key={lab.labId ?? lab.labName} className="mt-3 rounded-3xl border border-gray-100 bg-gray-50 p-4 dark:border-gray-700 dark:bg-[#151b24]">
                    <p className="font-semibold text-gray-900 dark:text-white">{formatText(lab.labName)}</p>
                    <p className="text-sm text-gray-500 dark:text-gray-400">{formatText(lab.reason)}</p>
                  </div>
                ))
              ) : (
                <EmptyState message="Data not found" />
              )}
            </div>

            <div>
              <p className="text-xs text-gray-400 dark:text-gray-500">At-risk students</p>
              {hasItems(data.atRiskStudents) ? (
                data.atRiskStudents.map((student) => (
                  <div key={student.studentId ?? student.studentName} className="mt-3 rounded-3xl border border-gray-100 bg-gray-50 p-4 dark:border-gray-700 dark:bg-[#151b24]">
                    <p className="font-semibold text-gray-900 dark:text-white">{formatText(student.studentName)}</p>
                    <p className="text-sm text-gray-500 dark:text-gray-400">
                      Current average: {formatPercent(student.currentAverage)}
                    </p>
                  </div>
                ))
              ) : (
                <EmptyState message="Data not found" />
              )}
            </div>
          </div>
        </div>

        <div className="rounded-3xl border border-gray-200 bg-white p-6 shadow-sm transition-colors dark:border-gray-700 dark:bg-[#1e2530]">
          <div className="flex items-center justify-between gap-4">
            <div>
              <p className="text-sm font-semibold uppercase tracking-[0.2em] text-gray-500 dark:text-gray-400">
                AI assistance
              </p>
              <h3 className="mt-2 text-xl font-semibold text-gray-900 dark:text-white">Recommendations</h3>
            </div>
            <ArrowUpRight className="h-6 w-6 text-sky-600" />
          </div>

          <div className="mt-6 space-y-3">
            <p className="text-sm text-gray-500 dark:text-gray-400">{formatText(aiSummary.details)}</p>
            {hasItems(recommendedResources) ? (
              <div className="space-y-2">
                {recommendedResources.map((resource) => (
                  <a
                    key={resource.url ?? resource.title}
                    href={resource.url || '#'}
                    target="_blank"
                    rel="noreferrer"
                    className="block rounded-2xl bg-gray-50 px-4 py-3 text-sm text-purple-700 transition hover:bg-gray-100 dark:bg-[#151b24] dark:text-purple-300 dark:hover:bg-[#2d3750]"
                  >
                    {formatText(resource.title)}
                  </a>
                ))}
              </div>
            ) : (
              <EmptyState message="No data available" />
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
