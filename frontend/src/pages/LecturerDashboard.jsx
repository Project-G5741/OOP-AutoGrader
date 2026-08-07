import { useMemo, useState, useEffect, useCallback } from 'react';
import { BarChart3, FileText, FolderKanban, Users, RefreshCw, ChevronRight } from 'lucide-react';
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

function tabClass(active) {
  return `px-3 py-2 text-sm rounded-t-md whitespace-nowrap ${
    active
      ? 'border-b-2 border-purple-600 font-semibold text-purple-600'
      : 'text-gray-600 hover:bg-purple-50 dark:text-gray-400 dark:hover:bg-purple-900/10'
  }`;
}

async function fetchAllLabSubmissions(labId, pageSize = 50) {
  const all = [];
  let page = 0;
  while (true) {
    const response = await fetch(
      `${API_BASE}/api/labs/${labId}/submissions?page=${page}&size=${pageSize}`,
      { headers: authHeaders() }
    );
    if (!response.ok) break;
    const data = await response.json();
    all.push(...(data.content ?? []));
    if (data.totalPages != null && page < data.totalPages - 1) {
      page += 1;
    } else {
      break;
    }
  }
  return all;
}
function ChallengeSubmissionsPlaceholder({ challengeLabel }) {
  return (
    <div>
      <div className="mb-4 flex items-center justify-between gap-3">
        <h4 className="text-base font-semibold text-gray-900 dark:text-white">
          {challengeLabel} Submissions
        </h4>
        <span className="inline-flex items-center gap-2 rounded-full bg-purple-50 px-3 py-1 text-xs text-purple-600 dark:bg-purple-900/20 dark:text-purple-300">
          <span className="h-2 w-2 rounded-full bg-purple-400 animate-pulse" />
          Loading submissions...
        </span>
      </div>
      <div className="overflow-x-auto rounded-xl border border-gray-200 dark:border-gray-700">
        <table className="w-full table-auto">
          <thead>
            <tr className="border-b border-gray-200 dark:border-gray-700">
              {['Student', 'ID', 'Score', 'Attempts', 'Last Submission', 'Action'].map((col) => (
                <th key={col} className="px-4 py-3 text-left text-sm font-medium text-gray-700 dark:text-gray-300">
                  {col}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {[...Array(5)].map((_, index) => (
              <tr key={index} className="border-b border-gray-100 dark:border-gray-800">
                <td className="px-4 py-3">
                  <div className="flex items-center gap-3">
                    <div className="h-8 w-8 rounded-full bg-gray-200 animate-pulse dark:bg-gray-700" />
                    <div className="h-4 w-28 rounded bg-gray-200 animate-pulse dark:bg-gray-700" />
                  </div>
                </td>
                <td className="px-4 py-3"><div className="h-4 w-20 rounded bg-gray-200 animate-pulse dark:bg-gray-700" /></td>
                <td className="px-4 py-3"><div className="h-4 w-12 rounded bg-gray-200 animate-pulse dark:bg-gray-700" /></td>
                <td className="px-4 py-3"><div className="h-4 w-10 rounded bg-gray-200 animate-pulse dark:bg-gray-700" /></td>
                <td className="px-4 py-3"><div className="h-4 w-24 rounded bg-gray-200 animate-pulse dark:bg-gray-700" /></td>
                <td className="px-4 py-3"><div className="h-8 w-20 rounded-lg bg-gray-200 animate-pulse dark:bg-gray-700" /></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
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
  const [challenges, setChallenges] = useState([]);
  const [activeTab, setActiveTab] = useState('overview');

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

  const fetchChallengesForLab = useCallback(async (labId) => {
    if (!labId) {
      setChallenges([]);
      return;
    }
    try {
      const response = await fetch(`${API_BASE}/api/labs/${labId}/challenges`, { headers: authHeaders() });
      if (!response.ok) {
        setChallenges([]);
        return;
      }
      const data = await response.json();
      setChallenges(Array.isArray(data) ? data : []);
    } catch {
      setChallenges([]);
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
        setSubmissionsError('Unable to load student roster');
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
      setSubmissionsError('Unable to load student roster');
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
      setActiveTab('overview');
      void Promise.all([
        fetchSubmissions(selectedLabId),
        fetchLabStatistics(selectedLabId),
        fetchChallengesForLab(selectedLabId),
      ]);
    }
  }, [selectedLabId, fetchSubmissions, fetchLabStatistics, fetchChallengesForLab]);

  const handleLabChange = (labId) => {
    if (labId === selectedLabId) return;
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
      void Promise.all([
        fetchSubmissions(selectedLabId, pagination.page),
        fetchLabStatistics(selectedLabId),
        fetchChallengesForLab(selectedLabId),
      ]);
    }
  };
  const selectedLab = useMemo(
    () => labs.find((lab) => lab.id === selectedLabId),
    [labs, selectedLabId]
  );

  const activeChallengeTab = useMemo(() => {
    const index = challenges.findIndex((challenge) => challenge.id === activeTab);
    if (index < 0) return null;
    return { challenge: challenges[index], index };
  }, [challenges, activeTab]);

  const challengeTabLabel = (challenge, index) => challenge.name ?? `Challenge ${index + 1}`;

  const labStatFields = useMemo(() => [
    { label: 'Average Score', value: formatPercent(labStatistics?.averageScore) },
    { label: 'Completion Rate', value: formatPercent(labStatistics?.completionRate) },
    { label: 'Highest Score', value: formatPercent(labStatistics?.highestScore) },
    { label: 'Lowest Score', value: formatPercent(labStatistics?.lowestScore) },
    { label: 'Total Submissions', value: formatNumber(labStatistics?.submissionCount) },
    { label: 'Enrolled Students', value: formatNumber(labStatistics?.studentCount) },
    { label: 'Students Submitted', value: formatNumber(labStatistics?.studentsSubmitted) },
  ], [labStatistics]);

  const exportOverview = async (format) => {
    if (!selectedLabId) return;
    const all = await fetchAllLabSubmissions(selectedLabId);

    const rows = all.map((r) => ({
      'Student Name': r.studentName ?? '',
      'Student ID': r.studentCode ?? '',
      Attempts: r.attempt ?? 0,
      'Lab Score': r.score != null ? `${r.score}%` : 'Not Submitted',
      'Last Submitted': r.submittedAt ?? '—',
    }));

    const labName = selectedLab?.name ?? selectedLabId;
    const fileBase = `lab_${String(labName).replace(/\s+/g, '_')}_roster`;

    if (format === 'excel') {
      const XLSX = await import('xlsx');
      const ws = XLSX.utils.json_to_sheet(rows);
      const wb = XLSX.utils.book_new();
      XLSX.utils.book_append_sheet(wb, ws, 'Roster');
      XLSX.writeFile(wb, `${fileBase}.xlsx`);
    } else if (format === 'pdf') {
      const { jsPDF } = await import('jspdf');
      const doc = new jsPDF();
      doc.setFontSize(12);
      let y = 20;
      doc.text('Lab Student Roster', 14, y);
      y += 10;
      rows.forEach((row) => {
        const line = `${row['Student Name']} | ${row['Student ID']} | ${row.Attempts} | ${row['Lab Score']}`;
        doc.text(line, 14, y);
        y += 8;
        if (y > 270) {
          doc.addPage();
          y = 20;
        }
      });
      doc.save(`${fileBase}.pdf`);
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

  const handleShellCommand = useCallback((cmd) => {
    if (cmd === 'home') setActiveNav('dashboard');
    else if (cmd === 'history') setActiveNav('projects');
    else if (cmd === 'editProfile') setShowProfile(true);
  }, []);

  const isInitialLoading = loadingLabs || loadingOverview;

  return (
    <div className={isDark ? 'dark' : ''}>
      <AppShell
        user={user}
        onLogout={onLogout}
        showNav
        hideUserMenu
        activeNav={activeNav}
        onNavigate={setActiveNav}
        onCommand={handleShellCommand}
      >
        {isInitialLoading ? (
          <LoadingSpinner />
        ) : activeNav === 'dashboard' ? (
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
              <div className="grid gap-6 lg:grid-cols-[0.28fr_1fr]">
                <div className="rounded-3xl border border-gray-200 bg-white p-4 shadow-sm transition-colors dark:border-gray-700 dark:bg-[#1e2530]">
                  <div className="mb-3">
                    <h3 className="text-xs font-semibold uppercase tracking-[0.15em] text-gray-400 dark:text-gray-500">
                      Select Lab Assignment
                    </h3>
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
                          className={`w-full rounded-xl border px-4 py-3 text-left text-sm transition flex items-center justify-between gap-3 ${
                            selectedLabId === lab.id
                              ? 'border-purple-500 bg-purple-50 text-purple-700 dark:bg-purple-900/20 dark:text-purple-300'
                              : 'border-gray-200 bg-white text-gray-700 hover:border-purple-300 hover:bg-purple-50 dark:border-gray-700 dark:bg-[#141a23] dark:text-gray-300 dark:hover:bg-purple-900/10'
                          }`}
                        >
                          <div className="min-w-0">
                            <div className="font-medium truncate">{formatText(lab.name)}</div>
                            {lab.description && (
                              <div className="mt-1 text-xs text-gray-500 dark:text-gray-400 truncate">{formatText(lab.description)}</div>
                            )}
                          </div>
                          {selectedLabId === lab.id && (
                            <ChevronRight className="h-4 w-4 shrink-0 text-purple-500" />
                          )}
                        </button>
                      ))
                    )}
                  </div>
                </div>

                <div className="rounded-3xl border border-gray-200 bg-white p-4 shadow-sm transition-colors dark:border-gray-700 dark:bg-[#1e2530]">
                  <div className="flex items-center justify-between">
                    <div>
                      <h3 className="text-base font-semibold text-gray-900 dark:text-white">
                        {formatText(selectedLab?.name)}
                      </h3>
                      <p className="text-sm text-gray-500 dark:text-gray-400">Performance summary</p>
                    </div>
                  </div>

                  <div className="mt-4">
                    <div className="flex gap-3 overflow-x-auto border-b border-gray-200 pb-2 dark:border-gray-700">
                      <button
                        type="button"
                        onClick={() => setActiveTab('overview')}
                        className={tabClass(activeTab === 'overview')}
                      >
                        Overview
                      </button>
                      {challenges.map((challenge, index) => (
                        <button
                          key={challenge.id}
                          type="button"
                          onClick={() => setActiveTab(challenge.id)}
                          className={tabClass(activeTab === challenge.id)}
                        >
                          {challengeTabLabel(challenge, index)}
                        </button>
                      ))}
                    </div>

                    <div className="mt-6">
                      {activeTab === 'overview' ? (
                        <div className="space-y-6">
                          {statisticsError && (
                            <p className="text-sm text-amber-700 dark:text-amber-300">{statisticsError}</p>
                          )}

                          {loadingStatistics ? (
                            <div className="flex items-center justify-center py-8">
                              <div className="w-8 h-8 border-4 border-purple-500 border-t-transparent rounded-full animate-spin" />
                            </div>
                          ) : (
                            <>
                              <div className="grid grid-cols-2 gap-4 lg:grid-cols-3">
                                {labStatFields.map((stat) => (
                                  <div key={stat.label}>
                                    <p className="text-xs text-gray-400 dark:text-gray-500">{stat.label}</p>
                                    <p className="text-xl font-semibold text-gray-900 dark:text-white">{stat.value}</p>
                                  </div>
                                ))}
                              </div>

                              <div>
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

                              <div className="flex flex-wrap items-center gap-2">
                                <button
                                  type="button"
                                  onClick={() => exportOverview('excel')}
                                  className="inline-flex items-center gap-2 rounded-md bg-purple-600 px-3 py-2 text-sm text-white"
                                >
                                  Export Excel
                                </button>
                                <button
                                  type="button"
                                  onClick={() => exportOverview('pdf')}
                                  className="inline-flex items-center gap-2 rounded-md bg-gray-200 px-3 py-2 text-sm dark:bg-gray-700 dark:text-gray-200"
                                >
                                  Export PDF
                                </button>
                              </div>
                            </>
                          )}

                          <div>
                            <h4 className="mb-3 text-sm font-semibold text-gray-900 dark:text-white">Student roster</h4>
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
                                  submissionCount: labStatistics?.submissionCount ?? null,
                                  studentCount: labStatistics?.studentCount ?? null,
                                  completionRate: labStatistics?.completionRate ?? null,
                                }}
                                pagination={pagination}
                                onPageChange={handlePageChange}
                              />
                            )}
                          </div>
                        </div>
                      ) : (
                        <ChallengeSubmissionsPlaceholder
                          challengeLabel={
                            activeChallengeTab
                              ? challengeTabLabel(activeChallengeTab.challenge, activeChallengeTab.index)
                              : 'Challenge'
                          }
                        />
                      )}
                    </div>
                  </div>
                </div>
              </div>
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
        ) : null}
      </AppShell>

      {showProfile && (
        <ChangePasswordModal isOpen={showProfile} user={user} onClose={() => setShowProfile(false)} />
      )}
    </div>
  );
}
