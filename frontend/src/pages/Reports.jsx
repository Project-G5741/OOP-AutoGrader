import { useState, useCallback, useEffect } from 'react';
import { RefreshCw } from 'lucide-react';
import ReportsPanel from '../components/lecturer/ReportsPanel';
import { apiFetch } from '../utils/apiFetch';
import { authHeaders } from '../utils/authHeaders';
import { friendlyLoadErrorFromResponse, toFriendlyError } from '../utils/apiError';
import { getOrFetchLecturerBootstrap, invalidateLecturerBootstrap } from '../utils/lecturerBootstrapStore';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';

const EMPTY_REPORT = {
  overallAverage: null,
  lowestAverageLab: null,
  lowestAverageScore: null,
  mostDifficultTopic: null,
  labTrend: [],
  atRiskLabs: [],
  atRiskStudents: [],
  aiSummary: {
    title: null,
    details: null,
    recommendedResources: [],
  },
};

export default function ReportsPage() {
  const [reportData, setReportData] = useState(EMPTY_REPORT);
  const [loadingReports, setLoadingReports] = useState(false);
  const [reportError, setReportError] = useState(null);

  const fetchReportData = useCallback(async ({ fresh = false } = {}) => {
    setLoadingReports(true);
    setReportError(null);
    try {
      if (fresh) {
        invalidateLecturerBootstrap('reports');
      }
      const data = await getOrFetchLecturerBootstrap(
        'reports',
        async (path) => {
          const response = await apiFetch(`${API_BASE}${path}`, { headers: authHeaders() });
          if (!response.ok) {
            throw new Error(await friendlyLoadErrorFromResponse(response));
          }
          return response.json();
        },
        { fresh },
      );
      setReportData({
        ...EMPTY_REPORT,
        ...data,
        labTrend: data.labTrend ?? [],
        atRiskLabs: data.atRiskLabs ?? [],
        atRiskStudents: data.atRiskStudents ?? [],
        aiSummary: {
          ...EMPTY_REPORT.aiSummary,
          ...(data.aiSummary ?? {}),
          recommendedResources: data.aiSummary?.recommendedResources ?? [],
        },
      });
    } catch (err) {
      setReportData(EMPTY_REPORT);
      setReportError(toFriendlyError(err, 'read'));
    } finally {
      setLoadingReports(false);
    }
  }, []);

  useEffect(() => {
    fetchReportData({ fresh: true });
  }, [fetchReportData]);

  return (
    <div className="max-w-full space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-2.5">
          <span className="h-5 w-1 rounded-full bg-navy dark:bg-primary" aria-hidden="true" />
          <h2 className="text-lg font-bold tracking-tight text-foreground">Reports & Analytics</h2>
        </div>
        <button
          type="button"
          onClick={() => fetchReportData({ fresh: true })}
          className="inline-flex min-h-10 items-center gap-2 rounded-xl border border-border bg-surface px-4 py-2 text-sm font-medium text-foreground shadow-stage-1 transition-colors hover:bg-surface-secondary active:scale-[0.98]"
          title="Refresh reports"
        >
          <RefreshCw className={`h-4 w-4 text-foreground-secondary ${loadingReports ? 'animate-spin' : ''}`} aria-hidden="true" />
          Refresh
        </button>
      </div>

      {reportError && (
        <div className="rounded-xl border border-warning bg-warning-bg p-4 text-sm text-warning-text">
          {reportError}
        </div>
      )}

      {loadingReports ? (
        <div className="flex items-center justify-center rounded-2xl border border-border bg-surface py-14 shadow-stage-1">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
        </div>
      ) : (
        <ReportsPanel reportData={reportData} />
      )}
    </div>
  );
}
