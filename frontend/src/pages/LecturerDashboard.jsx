// src/pages/LecturerDashboard.jsx
import { useMemo, useState, useEffect, useCallback } from 'react';
import { BarChart3, FileText, FolderKanban, Users, RefreshCw } from 'lucide-react';
import { useTheme } from '../context/ThemeContext';
import AppShell from '../components/layout/AppShell';
import ChangePasswordModal from '../components/student/ChangePasswordModal';
import DashboardSection from '../components/lecturer/DashboardSection';
import OverviewPanel from '../components/lecturer/OverviewPanel';
import ReportsPage from './Reports';
import SubmissionTable from '../components/lecturer/SubmissionTable';
import UserManagement from './UserManagement';
import SolutionManagement from './SolutionManagement';
import { formatNumber, formatPercent, formatText, hasItems } from '../utils/formatters';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';

const EMPTY_OVERVIEW = {
  totalStudents: 0,
  totalLabs: 0,
  averageScore: null,
  atRiskStudents: 0,
  recentSubmissions: [],
  activeStudents: 0,
};

function LoadingSpinner() {
  return (
    <div className="flex items-center justify-center h-64">
      <div className="flex flex-col items-center gap-3">
        <div className="w-10 h-10 border-4 border-purple-500 border-t-transparent rounded-full animate-spin" />
        <p className="text-gray-500 dark:text-gray-400">Loading data...</p>
      </div>
    </div>
  );
}

function authHeaders() {
  return {
    Authorization: `Bearer ${sessionStorage.getItem('accessToken')}`,
  };
}

export default function LecturerDashboard({ user, onLogout }) {
  const { isDark } = useTheme();
  const [activeNav, setActiveNav] = useState('dashboard');
  const [showProfile, setShowProfile] = useState(false);

  const [labs, setLabs] = useState([]);
  const [selectedLabId, setSelectedLabId] = useState(null);
  const [overview, setOverview] = useState(EMPTY_OVERVIEW);
  const [submissions, setSubmissions] = useState([]);
  const [labStatistics, setLabStatistics] = useState(null);
  const [pagination, setPagination] = useState({
    total: 0,
    page: 0,
    size: 20,
    totalPages: 0,
  });

  const [loadingLabs, setLoadingLabs] = useState(false);
  const [loadingOverview, setLoadingOverview] = useState(false);
  const [loadingSubmissions, setLoadingSubmissions] = useState(false);
  const [loadingStatistics, setLoadingStatistics] = useState(false);
  const [labsError, setLabsError] = useState(null);
  const [overviewError, setOverviewError] = useState(null);
  const [submissionsError, setSubmissionsError] = useState(null);
  const [statisticsError, setStatisticsError] = useState(null);

  const fetchLabs = useCallback(async () => {
    setLoadingLabs(true);
    setLabsError(null);
    try {
      const response = await fetch(`${API_BASE}/api/labs`, { headers: authHeaders() });
      if (!response.ok) {
        setLabs([]);
        setLabsError('Unable to load labs');
        return;
      }
      const data = await response.json();
      const nextLabs = Array.isArray(data) ? data : [];
      setLabs(nextLabs);
      if (nextLabs.length > 0) {
        setSelectedLabId((current) => current ?? nextLabs[0].id);
      }
    } catch {
      setLabs([]);
      setLabsError('Unable to load labs');
    } finally {
      setLoadingLabs(false);
    }
  }, []);

  const fetchOverview = useCallback(async () => {
    setLoadingOverview(true);
    setOverviewError(null);
    try {
      const response = await fetch(`${API_BASE}/api/lecturer/overview`, { headers: authHeaders() });
      if (!response.ok) {
        setOverview(EMPTY_OVERVIEW);
        setOverviewError('Unable to load overview');
        return;
      }
      const data = await response.json();
      setOverview({
        ...EMPTY_OVERVIEW,
        ...data,
        recentSubmissions: data.recentSubmissions ?? [],
      });
    } catch {
      setOverview(EMPTY_OVERVIEW);
      setOverviewError('Unable to load overview');
    } finally {
      setLoadingOverview(false);
    }
  }, []);

  const fetchSubmissions = useCallback(async (labId, page = 0) => {
    if (!labId) return;
    setLoadingSubmissions(true);
    setSubmissionsError(null);
    try {
      const response = await fetch(
        `${API_BASE}/api/labs/${labId}/submissions?page=${page}&size=${pagination.size}&sort=submittedAt,desc`,
        { headers: authHeaders() }
      );

      if (!response.ok) {
        setSubmissions([]);
        setPagination((prev) => ({ ...prev, total: 0, totalPages: 0, page: 0 }));
        setSubmissionsError('Unable to load submissions');
        return;
      }

      const data = await response.json();
      setSubmissions(data.content ?? []);
      setPagination({
        total: data.totalElements ?? 0,
        page: data.number ?? 0,
        size: data.size ?? pagination.size,
        totalPages: data.totalPages ?? 0,
      });
    } catch {
      setSubmissions([]);
      setPagination((prev) => ({ ...prev, total: 0, totalPages: 0, page: 0 }));
      setSubmissionsError('Unable to load submissions');
    } finally {
      setLoadingSubmissions(false);
    }
  }, [pagination.size]);

  const fetchLabStatistics = useCallback(async (labId) => {
    if (!labId) return;
    setLoadingStatistics(true);
    setStatisticsError(null);
    try {
      const response = await fetch(`${API_BASE}/api/labs/${labId}/statistics`, { headers: authHeaders() });
      if (!response.ok) {
        setLabStatistics(null);
        setStatisticsError('Unable to load lab statistics');
        return;
      }
      const data = await response.json();
      setLabStatistics({
        ...data,
        gradeDistribution: data.gradeDistribution ?? [],
      });
    } catch {
      setLabStatistics(null);
      setStatisticsError('Unable to load lab statistics');
    } finally {
      setLoadingStatistics(false);
    }
  }, []);

  useEffect(() => {
    fetchLabs();
    fetchOverview();
  }, [fetchLabs, fetchOverview]);

  useEffect(() => {
    if (selectedLabId) {
      fetchSubmissions(selectedLabId);
      fetchLabStatistics(selectedLabId);
    }
  }, [selectedLabId, fetchSubmissions, fetchLabStatistics]);

  const handleLabChange = (labId) => {
    setSelectedLabId(labId);
    setPagination((prev) => ({ ...prev, page: 0 }));
  };

  const handlePageChange = (newPage) => {
    if (selectedLabId) {
      fetchSubmissions(selectedLabId, newPage);
    }
  };

  const handleRefresh = () => {
    fetchOverview();
    fetchLabs();
    if (selectedLabId) {
      fetchSubmissions(selectedLabId, pagination.page);
      fetchLabStatistics(selectedLabId);
    }
  };

  const overviewCards = useMemo(() => [
    {
      title: 'Total Students',
      value: formatNumber(overview.totalStudents),
      subtitle: 'Active students in the system',
      icon: <Users className="h-5 w-5 text-amber-600" />,
      accent: 'bg-amber-100 text-amber-600 dark:bg-amber-900/30',
    },
    {
      title: 'Average Score',
      value: formatPercent(overview.averageScore),
      subtitle: 'Across all student lab progress',
      icon: <BarChart3 className="h-5 w-5 text-emerald-600" />,
      accent: 'bg-emerald-100 text-emerald-600 dark:bg-emerald-900/30',
    },
    {
      title: 'Total Labs',
      value: formatNumber(overview.totalLabs),
      subtitle: 'Labs available for grading',
      icon: <FolderKanban className="h-5 w-5 text-purple-600" />,
      accent: 'bg-purple-100 text-purple-600 dark:bg-purple-900/30',
    },
    {
      title: 'At-Risk Students',
      value: formatNumber(overview.atRiskStudents),
      subtitle: `${formatNumber(overview.activeStudents)} active students`,
      icon: <FileText className="h-5 w-5 text-blue-600" />,
      accent: 'bg-blue-100 text-blue-600 dark:bg-blue-900/30',
    },
  ], [overview]);

  const isInitialLoading = loadingLabs || loadingOverview;

  if (isInitialLoading) {
    return (
      <div className={isDark ? 'dark' : ''}>
        <AppShell
          user={user}
          onLogout={onLogout}
          showNav
          hideUserMenu
          activeNav={activeNav}
          onNavigate={setActiveNav}
          onCommand={(cmd) => {
            if (cmd === 'home') setActiveNav('dashboard');
            else if (cmd === 'history') setActiveNav('projects');
            else if (cmd === 'editProfile') setShowProfile(true);
          }}
        >
          <LoadingSpinner />
        </AppShell>
      </div>
    );
  }

  return (
    <div className={isDark ? 'dark' : ''}>
      <AppShell
        user={user}
        onLogout={onLogout}
        showNav
        hideUserMenu
        activeNav={activeNav}
        onNavigate={setActiveNav}
        onCommand={(cmd) => {
          if (cmd === 'home') setActiveNav('dashboard');
          else if (cmd === 'history') setActiveNav('projects');
          else if (cmd === 'editProfile') setShowProfile(true);
        }}
      >
        {activeNav === 'dashboard' ? (
          <div className="space-y-6 px-4 sm:px-6 lg:px-8 max-w-full overflow-x-hidden">
            {(overviewError || labsError) && (
              <div className="rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800 dark:border-amber-800 dark:bg-[#2a2414] dark:text-amber-200">
                {[overviewError, labsError].filter(Boolean).join(' · ')}
              </div>
            )}

            <OverviewPanel overviewCards={overviewCards} />

            <DashboardSection
              title="Reports & analytics"
              subtitle="Open the dedicated reports page for detailed analytics"
              actions={
                <button
                  onClick={() => setActiveNav('reports')}
                  className="p-2 rounded-lg border border-gray-200 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-[#151b24] transition-colors"
                  title="Open reports page"
                >
                  <RefreshCw className="w-4 h-4 text-gray-500 dark:text-gray-400" />
                </button>
              }
            >
              <div className="rounded-xl border border-gray-100 bg-gray-50 p-6 dark:border-gray-700 dark:bg-[#141820]">
                <p className="text-sm text-gray-600 dark:text-gray-400">
                  Recent submissions: {hasItems(overview.recentSubmissions)
                    ? `${overview.recentSubmissions.length} loaded`
                    : 'Data not found'}
                </p>
              </div>
            </DashboardSection>

            <DashboardSection
              title="Grading overview"
              subtitle="Select a lab to view submissions and statistics"
              actions={
                <button
                  onClick={handleRefresh}
                  className="p-2 rounded-lg border border-gray-200 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-[#151b24] transition-colors"
                  title="Refresh"
                >
                  <RefreshCw className={`w-4 h-4 text-gray-500 dark:text-gray-400 ${loadingOverview || loadingSubmissions ? 'animate-spin' : ''}`} />
                </button>
              }
            >
              <div className="grid gap-6 lg:grid-cols-[0.4fr_0.6fr]">
                <div className="rounded-3xl border border-gray-200 bg-white p-5 shadow-sm transition-colors dark:border-gray-700 dark:bg-[#1e2530]">
                  <div className="mb-5 flex items-center justify-between gap-3">
                    <div>
                      <h3 className="text-base font-semibold text-gray-900 dark:text-white">Select Lab</h3>
                      <p className="text-sm text-gray-500 dark:text-gray-400">Choose a lab to review submissions.</p>
                    </div>
                    {selectedLabId && (
                      <span className="rounded-full bg-purple-100 px-3 py-1 text-xs font-semibold text-purple-700 dark:bg-purple-900/30 dark:text-purple-300">
                        {formatNumber(pagination.total)} submissions
                      </span>
                    )}
                  </div>
                  <div className="space-y-3">
                    {labs.length === 0 ? (
                      <p className="text-sm text-gray-400 dark:text-gray-600 text-center py-4">No labs available</p>
                    ) : (
                      labs.map((lab) => (
                        <button
                          key={lab.id}
                          type="button"
                          onClick={() => handleLabChange(lab.id)}
                          className={`w-full rounded-2xl border px-4 py-3 text-left text-sm transition ${
                            selectedLabId === lab.id
                              ? 'border-purple-500 bg-purple-50 dark:bg-purple-900/20 text-purple-700 dark:text-purple-300'
                              : 'border-gray-200 bg-white text-gray-700 hover:border-purple-400 hover:bg-purple-50 dark:border-gray-700 dark:bg-[#141a23] dark:text-gray-300 dark:hover:bg-purple-900/10'
                          }`}
                        >
                          <span>{formatText(lab.name)}</span>
                        </button>
                      ))
                    )}
                  </div>
                </div>

                <div className="space-y-4">
                  <div className="rounded-3xl border border-gray-200 bg-white p-5 shadow-sm transition-colors dark:border-gray-700 dark:bg-[#1e2530]">
                    <div className="flex items-center justify-between">
                      <div>
                        <h3 className="text-base font-semibold text-gray-900 dark:text-white">
                          {formatText(labs.find((l) => l.id === selectedLabId)?.name)}
                        </h3>
                        <p className="text-sm text-gray-500 dark:text-gray-400">Performance summary</p>
                      </div>
                    </div>

                    {statisticsError && (
                      <p className="mt-3 text-sm text-amber-700 dark:text-amber-300">{statisticsError}</p>
                    )}

                    {loadingStatistics ? (
                      <div className="flex items-center justify-center py-8">
                        <div className="w-8 h-8 border-4 border-purple-500 border-t-transparent rounded-full animate-spin" />
                      </div>
                    ) : (
                      <div className="mt-4 grid grid-cols-2 gap-4">
                        <div>
                          <p className="text-xs text-gray-400 dark:text-gray-500">Average Score</p>
                          <p className="text-xl font-semibold text-gray-900 dark:text-white">
                            {formatPercent(labStatistics?.averageScore)}
                          </p>
                        </div>
                        <div>
                          <p className="text-xs text-gray-400 dark:text-gray-500">Completion Rate</p>
                          <p className="text-xl font-semibold text-gray-900 dark:text-white">
                            {formatPercent(labStatistics?.completionRate)}
                          </p>
                        </div>
                        <div>
                          <p className="text-xs text-gray-400 dark:text-gray-500">Highest Score</p>
                          <p className="text-xl font-semibold text-gray-900 dark:text-white">
                            {formatPercent(labStatistics?.highestScore)}
                          </p>
                        </div>
                        <div>
                          <p className="text-xs text-gray-400 dark:text-gray-500">Lowest Score</p>
                          <p className="text-xl font-semibold text-gray-900 dark:text-white">
                            {formatPercent(labStatistics?.lowestScore)}
                          </p>
                        </div>
                        <div>
                          <p className="text-xs text-gray-400 dark:text-gray-500">Submission Count</p>
                          <p className="text-xl font-semibold text-gray-900 dark:text-white">
                            {formatNumber(labStatistics?.submissionCount)}
                          </p>
                        </div>
                        <div>
                          <p className="text-xs text-gray-400 dark:text-gray-500">Students Count</p>
                          <p className="text-xl font-semibold text-gray-900 dark:text-white">
                            {formatNumber(labStatistics?.studentCount)}
                          </p>
                        </div>
                      </div>
                    )}

                    {!loadingStatistics && (
                      <div className="mt-6">
                        <p className="text-xs font-semibold uppercase tracking-[0.2em] text-gray-400 dark:text-gray-500">
                          Grade distribution
                        </p>
                        {hasItems(labStatistics?.gradeDistribution) ? (
                          <div className="mt-3 space-y-2">
                            {labStatistics.gradeDistribution.map((bucket) => (
                              <div key={bucket.range} className="flex items-center justify-between text-sm">
                                <span className="text-gray-700 dark:text-gray-300">{bucket.range}</span>
                                <span className="font-medium text-purple-600 dark:text-purple-300">{formatNumber(bucket.count)}</span>
                              </div>
                            ))}
                          </div>
                        ) : (
                          <p className="mt-3 text-sm text-gray-500 dark:text-gray-400">No data available</p>
                        )}
                      </div>
                    )}
                  </div>
                </div>
              </div>
            </DashboardSection>

            <DashboardSection title="Submissions" subtitle="Review student submissions for the selected lab">
              {submissionsError && (
                <p className="mb-4 text-sm text-amber-700 dark:text-amber-300">{submissionsError}</p>
              )}
              {loadingSubmissions ? (
                <div className="flex items-center justify-center py-12">
                  <div className="w-8 h-8 border-4 border-purple-500 border-t-transparent rounded-full animate-spin" />
                </div>
              ) : (
                <SubmissionTable
                  submissions={submissions}
                  summary={{
                    averageScore: labStatistics?.averageScore ?? null,
                    lowestScore: labStatistics?.lowestScore ?? null,
                    submissionCount: labStatistics?.submissionCount ?? null,
                    studentCount: labStatistics?.studentCount ?? null,
                    completionRate: labStatistics?.completionRate ?? null,
                  }}
                  pagination={pagination}
                  onPageChange={handlePageChange}
                />
              )}
            </DashboardSection>
          </div>
        ) : activeNav === 'users' ? (
          <div className="px-4 sm:px-6 lg:px-8 max-w-full overflow-x-hidden">
            <UserManagement hideNav noShell user={user} onLogout={onLogout} />
          </div>
        ) : activeNav === 'projects' ? (
          <div className="px-4 sm:px-6 lg:px-8 max-w-full overflow-x-hidden">
            <SolutionManagement />
          </div>
        ) : activeNav === 'reports' ? (
          <ReportsPage />
        ) : (
          <div className="rounded-xl border border-gray-200 bg-white p-10 text-center text-gray-700 shadow-sm dark:border-gray-700 dark:bg-[#1e2530] dark:text-gray-300 mx-4 sm:mx-6 lg:mx-8 max-w-full overflow-x-hidden">
            <h2 className="mb-3 text-xl font-semibold">Reports</h2>
            <p>Report generation is coming soon.</p>
          </div>
        )}
      </AppShell>

      {showProfile && (
        <ChangePasswordModal isOpen={showProfile} user={user} onClose={() => setShowProfile(false)} />
      )}
    </div>
  );
}
