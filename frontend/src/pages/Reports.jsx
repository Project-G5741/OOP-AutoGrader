import { useState, useCallback, useEffect } from 'react';
import ReportsPanel from '../components/lecturer/ReportsPanel';
import { apiFetch } from '../utils/apiFetch';
import { authHeaders } from '../utils/authHeaders';
import { friendlyLoadErrorFromResponse, toFriendlyError } from '../utils/apiError';

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

  const fetchReportData = useCallback(async () => {
    setLoadingReports(true);
    setReportError(null);
    try {
      const response = await apiFetch(`${API_BASE}/api/analytics/dashboard`, {
        headers: authHeaders(),
      });

      if (!response.ok) {
        setReportData(EMPTY_REPORT);
        setReportError(await friendlyLoadErrorFromResponse(response));
        return;
      }

      const data = await response.json();
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
    fetchReportData();
  }, [fetchReportData]);

  return (
    <div className="max-w-full">
      <div className="space-y-6">
        <div className="rounded-xl border border-border bg-surface p-4 shadow-sm transition-colors sm:p-6">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <h2 className="text-lg font-semibold text-foreground">Reports & Analytics</h2>
            </div>
            <div>
              <button
                onClick={fetchReportData}
                className="inline-flex min-h-11 items-center rounded-lg border border-border px-4 py-2 transition-colors hover:bg-surface-secondary"
                title="Refresh reports"
              >
                Refresh
              </button>
            </div>
          </div>

          {reportError && (
            <div className="mt-4 rounded-xl border border-warning bg-warning-bg p-4 text-sm text-warning-text">
              {reportError}
            </div>
          )}

          <div className="mt-6">
            {loadingReports ? (
              <div className="flex items-center justify-center py-12">
                <div className="w-8 h-8 border-4 border-primary border-t-transparent rounded-full animate-spin" />
              </div>
            ) : (
              <ReportsPanel reportData={reportData} />
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
