import { ArrowUpRight } from 'lucide-react';
import { ChartBar, TrendUp, WarningDiamond, Sparkle } from '@phosphor-icons/react';
import { formatNumber, formatPercent, formatText, hasItems } from '../../utils/formatters';
import { EmptyState, StageCard3D, StaggerGroup, StaggerItem } from '../ui/MotionPrimitives';

export default function ReportsPanel({ reportData }) {
  const data = reportData ?? {};
  const aiSummary = data.aiSummary ?? {};
  const recommendedResources = aiSummary.recommendedResources ?? [];

  return (
    <StaggerGroup className="grid gap-6 xl:grid-cols-[1.2fr_0.8fr]">
      <div className="space-y-6">
        <StaggerItem>
          <StageCard3D maxTilt={2.5} hoverLift={-2} className="p-6">
            <div className="flex items-center justify-between gap-4">
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.2em] text-foreground-secondary">
                  Analytics summary
                </p>
                <h3 className="mt-1.5 text-xl font-bold tracking-tight text-foreground">Lecturer reports</h3>
              </div>
              <div className="rounded-xl bg-navy-light p-2.5 text-navy dark:text-foreground">
                <ChartBar weight="duotone" className="h-6 w-6" aria-hidden="true" />
              </div>
            </div>

            <div className="mt-6 grid gap-3 sm:grid-cols-2">
              <div className="rounded-xl bg-surface-secondary/80 p-3.5">
                <p className="text-xs font-medium text-foreground-muted">Overall average</p>
                <p className="mt-1.5 text-xl font-bold tabular-nums text-foreground sm:text-2xl">
                  {formatPercent(data.overallAverage)}
                </p>
              </div>
              <div className="rounded-xl bg-surface-secondary/80 p-3.5">
                <p className="text-xs font-medium text-foreground-muted">Lowest average lab</p>
                <p className="mt-1.5 text-xl font-bold text-foreground sm:text-2xl">
                  {formatText(data.lowestAverageLab)}
                </p>
              </div>
              <div className="rounded-xl bg-surface-secondary/80 p-3.5">
                <p className="text-xs font-medium text-foreground-muted">Lowest average score</p>
                <p className="mt-1.5 text-xl font-bold tabular-nums text-foreground sm:text-2xl">
                  {formatPercent(data.lowestAverageScore)}
                </p>
              </div>
              <div className="rounded-xl bg-surface-secondary/80 p-3.5">
                <p className="text-xs font-medium text-foreground-muted">Most difficult topic</p>
                <p className="mt-1.5 text-xl font-bold text-foreground sm:text-2xl">
                  {formatText(data.mostDifficultTopic)}
                </p>
              </div>
            </div>
          </StageCard3D>
        </StaggerItem>

        <StaggerItem>
          <StageCard3D maxTilt={2.5} hoverLift={-2} className="p-6">
            <div className="flex items-center justify-between gap-4">
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.2em] text-foreground-secondary">
                  Lab performance trend
                </p>
                <h3 className="mt-1.5 text-xl font-bold tracking-tight text-foreground">Average scores by lab</h3>
              </div>
              <div className="rounded-xl bg-success-bg p-2.5 text-success-text">
                <TrendUp weight="duotone" className="h-6 w-6" aria-hidden="true" />
              </div>
            </div>

            <div className="mt-6 space-y-1">
              {hasItems(data.labTrend) ? (
                data.labTrend.map((item) => {
                  const pct = Math.max(0, Math.min(100, Number(item.averageScore) || 0));
                  return (
                    <div key={item.labId ?? item.labName} className="border-t border-border-subtle py-3 first:border-t-0">
                      <div className="flex items-center justify-between gap-4">
                        <div className="min-w-0">
                          <p className="text-sm font-semibold text-foreground">{formatText(item.labName)}</p>
                          <p className="text-xs text-foreground-secondary">
                            {formatNumber(item.submissionCount)} submissions
                          </p>
                        </div>
                        <div className="text-right">
                          <p className="text-lg font-bold tabular-nums text-foreground">{formatPercent(item.averageScore)}</p>
                          <p className="text-xs text-foreground-secondary">Average score</p>
                        </div>
                      </div>
                      <div className="mt-2 h-1.5 w-full overflow-hidden rounded-full bg-surface-secondary">
                        <div
                          className="h-full rounded-full bg-secondary transition-[width] duration-300"
                          style={{ width: `${pct}%` }}
                        />
                      </div>
                    </div>
                  );
                })
              ) : (
                <EmptyState compact title="No trend data available" description="Lab average scores will appear once students submit solutions." />
              )}
            </div>
          </StageCard3D>
        </StaggerItem>
      </div>

      <div className="space-y-6">
        <StaggerItem>
          <StageCard3D maxTilt={2.5} hoverLift={-2} className="p-6">
            <div className="flex items-center justify-between gap-4">
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.2em] text-foreground-secondary">
                  At-risk measures
                </p>
                <h3 className="mt-1.5 text-xl font-bold tracking-tight text-foreground">Labs & students</h3>
              </div>
              <div className="rounded-xl bg-error-bg p-2.5 text-error-text">
                <WarningDiamond weight="duotone" className="h-6 w-6" aria-hidden="true" />
              </div>
            </div>

            <div className="mt-6 space-y-5">
              <div>
                <p className="text-xs font-semibold uppercase tracking-wider text-foreground-muted">At-risk labs</p>
                {hasItems(data.atRiskLabs) ? (
                  <div className="mt-2 space-y-2.5">
                    {data.atRiskLabs.map((lab) => (
                      <div key={lab.labId ?? lab.labName} className="border-l-2 border-error pl-3 py-0.5">
                        <p className="text-sm font-semibold text-foreground">{formatText(lab.labName)}</p>
                        <p className="text-xs text-foreground-secondary">{formatText(lab.reason)}</p>
                      </div>
                    ))}
                  </div>
                ) : (
                  <EmptyState compact title="No at-risk labs" description="All labs are currently performing within target range." />
                )}
              </div>

              <div className="border-t border-border-subtle pt-4">
                <p className="text-xs font-semibold uppercase tracking-wider text-foreground-muted">At-risk students</p>
                {hasItems(data.atRiskStudents) ? (
                  <div className="mt-2 space-y-2.5">
                    {data.atRiskStudents.map((student) => (
                      <div key={student.studentId ?? student.studentName} className="border-l-2 border-gold pl-3 py-0.5">
                        <p className="text-sm font-semibold text-foreground">{formatText(student.studentName)}</p>
                        <p className="text-xs tabular-nums text-foreground-secondary">
                          Current average: {formatPercent(student.currentAverage)}
                        </p>
                      </div>
                    ))}
                  </div>
                ) : (
                  <EmptyState compact title="No at-risk students" description="No students currently flagged below threshold." />
                )}
              </div>
            </div>
          </StageCard3D>
        </StaggerItem>

        <StaggerItem>
          <StageCard3D maxTilt={2.5} hoverLift={-2} className="p-6">
            <div className="flex items-center justify-between gap-4">
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.2em] text-foreground-secondary">
                  AI assistance
                </p>
                <h3 className="mt-1.5 text-xl font-bold tracking-tight text-foreground">Recommendations</h3>
              </div>
              <div className="rounded-xl bg-accent-purple-light p-2.5 text-accent-purple-text">
                <Sparkle weight="duotone" className="h-6 w-6" aria-hidden="true" />
              </div>
            </div>

            <div className="mt-5 space-y-3">
              <p className="text-sm leading-relaxed text-foreground-secondary">{formatText(aiSummary.details)}</p>
              {hasItems(recommendedResources) ? (
                <div className="divide-y divide-border-subtle border-t border-border-subtle">
                  {recommendedResources.map((resource) => (
                    <a
                      key={resource.url ?? resource.title}
                      href={resource.url || '#'}
                      target="_blank"
                      rel="noreferrer"
                      className="flex items-center justify-between gap-2 py-2.5 text-sm font-medium text-primary-text transition-colors hover:text-primary"
                    >
                      <span>{formatText(resource.title)}</span>
                      <ArrowUpRight className="h-4 w-4 shrink-0" aria-hidden="true" />
                    </a>
                  ))}
                </div>
              ) : (
                <EmptyState compact title="No recommendations yet" description="Teaching suggestions will generate after grading data accumulates." />
              )}
            </div>
          </StageCard3D>
        </StaggerItem>
      </div>
    </StaggerGroup>
  );
}
