import { ArrowUpRight, BarChart3, TrendingUp, AlertTriangle } from 'lucide-react';
import { formatNumber, formatPercent, formatText, hasItems } from '../../utils/formatters';

function EmptyState({ message = 'Data not found' }) {
  return (
    <div className="flex min-h-[120px] items-center justify-center rounded-2xl border border-dashed border-border bg-surface-secondary p-6 text-sm text-foreground-secondary dark:text-foreground-muted">
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
        <div className="rounded-3xl border border-border bg-surface p-6 shadow-sm transition-colors">
          <div className="flex items-center justify-between gap-4">
            <div>
              <p className="text-sm font-semibold uppercase tracking-[0.2em] text-foreground-secondary">
                Analytics summary
              </p>
              <h3 className="mt-2 text-xl font-semibold text-foreground">Lecturer reports</h3>
            </div>
            <BarChart3 className="h-6 w-6 text-primary" />
          </div>

          <div className="mt-6 grid gap-4 sm:grid-cols-2">
            <div className="rounded-3xl border border-border bg-surface-secondary p-4">
              <p className="text-xs text-foreground-muted">Overall average</p>
              <p className="mt-2 text-2xl font-semibold text-foreground">
                {formatPercent(data.overallAverage)}
              </p>
            </div>
            <div className="rounded-3xl border border-border bg-surface-secondary p-4">
              <p className="text-xs text-foreground-muted">Lowest average lab</p>
              <p className="mt-2 text-2xl font-semibold text-foreground">
                {formatText(data.lowestAverageLab)}
              </p>
            </div>
            <div className="rounded-3xl border border-border bg-surface-secondary p-4">
              <p className="text-xs text-foreground-muted">Lowest average score</p>
              <p className="mt-2 text-2xl font-semibold text-foreground">
                {formatPercent(data.lowestAverageScore)}
              </p>
            </div>
            <div className="rounded-3xl border border-border bg-surface-secondary p-4">
              <p className="text-xs text-foreground-muted">Most difficult topic</p>
              <p className="mt-2 text-2xl font-semibold text-foreground">
                {formatText(data.mostDifficultTopic)}
              </p>
            </div>
          </div>
        </div>

        <div className="rounded-3xl border border-border bg-surface p-6 shadow-sm transition-colors">
          <div className="flex items-center justify-between gap-4">
            <div>
              <p className="text-sm font-semibold uppercase tracking-[0.2em] text-foreground-secondary">
                Lab performance trend
              </p>
              <h3 className="mt-2 text-xl font-semibold text-foreground">Average scores by lab</h3>
            </div>
            <TrendingUp className="h-6 w-6 text-chart-green" />
          </div>

          <div className="mt-6 space-y-3">
            {hasItems(data.labTrend) ? (
              data.labTrend.map((item) => (
                <div key={item.labId ?? item.labName} className="flex items-center justify-between gap-4 rounded-3xl border border-border bg-surface-secondary p-4">
                  <div className="min-w-0">
                    <p className="text-sm font-semibold text-foreground">{formatText(item.labName)}</p>
                    <p className="text-sm text-foreground-secondary">
                      {formatNumber(item.submissionCount)} submissions
                    </p>
                  </div>
                  <div className="text-right">
                    <p className="text-lg font-semibold text-foreground">{formatPercent(item.averageScore)}</p>
                    <p className="text-xs text-foreground-secondary">Average score</p>
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
        <div className="rounded-3xl border border-border bg-surface p-6 shadow-sm transition-colors">
          <div className="flex items-center justify-between gap-4">
            <div>
              <p className="text-sm font-semibold uppercase tracking-[0.2em] text-foreground-secondary">
                At-risk measures
              </p>
              <h3 className="mt-2 text-xl font-semibold text-foreground">Labs & students</h3>
            </div>
            <AlertTriangle className="h-6 w-6 text-error" />
          </div>

          <div className="mt-6 space-y-4">
            <div>
              <p className="text-xs text-foreground-muted">At-risk labs</p>
              {hasItems(data.atRiskLabs) ? (
                data.atRiskLabs.map((lab) => (
                  <div key={lab.labId ?? lab.labName} className="mt-3 rounded-3xl border border-border bg-surface-secondary p-4">
                    <p className="font-semibold text-foreground">{formatText(lab.labName)}</p>
                    <p className="text-sm text-foreground-secondary">{formatText(lab.reason)}</p>
                  </div>
                ))
              ) : (
                <EmptyState message="Data not found" />
              )}
            </div>

            <div>
              <p className="text-xs text-foreground-muted">At-risk students</p>
              {hasItems(data.atRiskStudents) ? (
                data.atRiskStudents.map((student) => (
                  <div key={student.studentId ?? student.studentName} className="mt-3 rounded-3xl border border-border bg-surface-secondary p-4">
                    <p className="font-semibold text-foreground">{formatText(student.studentName)}</p>
                    <p className="text-sm text-foreground-secondary">
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

        <div className="rounded-3xl border border-border bg-surface p-6 shadow-sm transition-colors">
          <div className="flex items-center justify-between gap-4">
            <div>
              <p className="text-sm font-semibold uppercase tracking-[0.2em] text-foreground-secondary">
                AI assistance
              </p>
              <h3 className="mt-2 text-xl font-semibold text-foreground">Recommendations</h3>
            </div>
            <ArrowUpRight className="h-6 w-6 text-chart-cyan" />
          </div>

          <div className="mt-6 space-y-3">
            <p className="text-sm text-foreground-secondary">{formatText(aiSummary.details)}</p>
            {hasItems(recommendedResources) ? (
              <div className="space-y-2">
                {recommendedResources.map((resource) => (
                  <a
                    key={resource.url ?? resource.title}
                    href={resource.url || '#'}
                    target="_blank"
                    rel="noreferrer"
                    className="block rounded-2xl bg-surface-secondary px-4 py-3 text-sm text-primary-text transition hover:bg-surface-secondary"
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
