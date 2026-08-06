import { useState, useCallback, useEffect } from 'react';
import ReportsPanel from '../components/lecturer/ReportsPanel';
import { formatText } from '../utils/formatters';

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
      const response = await fetch(`${API_BASE}/api/analytics/dashboard`, {
        headers: {
          Authorization: `Bearer ${sessionStorage.getItem('accessToken')}`,
        },
      });

      if (!response.ok) {
        setReportData(EMPTY_REPORT);
        setReportError('Unable to load report data');
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
      setReportError(formatText(err.message));
    } finally {
      setLoadingReports(false);
    }
  }, []);

  useEffect(() => {
    fetchReportData();
  }, [fetchReportData]);

  return (
    <div className="px-4 sm:px-6 lg:px-8 max-w-full overflow-x-hidden">
      <div className="space-y-6">
        <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm transition-colors dark:border-gray-700 dark:bg-[#1e2530]">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white">Reports & Analytics</h2>
              <p className="text-sm text-gray-500 dark:text-gray-400">Detailed analytics and reports for your courses.</p>
            </div>
            <div>
              <button
                onClick={fetchReportData}
                className="p-2 rounded-lg border border-gray-200 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-[#151b24] transition-colors"
                title="Refresh reports"
              >
                Refresh
              </button>
            </div>
          </div>

          {reportError && (
            <div className="mt-4 rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800 dark:border-amber-800 dark:bg-[#2a2414] dark:text-amber-200">
              {reportError}
            </div>
          )}

          <div className="mt-6">
            {loadingReports ? (
              <div className="flex items-center justify-center py-12">
                <div className="w-8 h-8 border-4 border-purple-500 border-t-transparent rounded-full animate-spin" />
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
