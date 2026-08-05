// src/pages/LecturerDashboard.jsx
import { useMemo, useState, useEffect, useCallback } from 'react';
import { BarChart3, FileText, FolderKanban, Users, RefreshCw, AlertCircle } from 'lucide-react';
import { useTheme } from '../context/ThemeContext';
import AppShell from '../components/layout/AppShell';
import ChangePasswordModal from '../components/student/ChangePasswordModal';
import DashboardSection from '../components/lecturer/DashboardSection';
import LecturerOverviewCard from '../components/lecturer/LecturerOverviewCard';
import SubmissionTable from '../components/lecturer/SubmissionTable';
import UserManagement from './UserManagement';
import SolutionManagement from './SolutionManagement';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';

// ==================== SUB-COMPONENTS ====================

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

// ==================== MAIN COMPONENT ====================

export default function LecturerDashboard({ user, onLogout }) {
  const { isDark } = useTheme();
  const [activeNav, setActiveNav] = useState('dashboard');
  const [showProfile, setShowProfile] = useState(false);

  // ===== States =====
  const [labs, setLabs] = useState([]);
  const [selectedLabId, setSelectedLabId] = useState(null);
  const [overview, setOverview] = useState({
    totalSubmissions: 0,
    averageScore: null,
    activeLabs: 0,
    totalStudents: 0,
    totalLecturers: 0,
  });
  const [submissions, setSubmissions] = useState([]);
  const [labStatistics, setLabStatistics] = useState(null);
  const [pagination, setPagination] = useState({
    total: 0,
    page: 0,
    size: 20,
    totalPages: 0,
  });

  // ===== Loading/Error States =====
  const [loadingLabs, setLoadingLabs] = useState(false);
  const [loadingOverview, setLoadingOverview] = useState(false);
  const [loadingSubmissions, setLoadingSubmissions] = useState(false);
  const [loadingStatistics, setLoadingStatistics] = useState(false);
  const [hasData, setHasData] = useState(false);

  // ===== Fetch Functions =====

  const fetchLabs = useCallback(async () => {
    setLoadingLabs(true);
    try {
      const response = await fetch(`${API_BASE}/api/labs`, {
        headers: {
          Authorization: `Bearer ${sessionStorage.getItem('accessToken')}`,
        },
      });

      if (!response.ok) {
        console.info('Labs API not available yet');
        setLabs([]);
        return;
      }

      const data = await response.json();
      setLabs(data || []);
      if (data.length > 0 && !selectedLabId) {
        setSelectedLabId(data[0].id);
      }
    } catch (err) {
      console.info('Could not fetch labs:', err.message);
      setLabs([]);
    } finally {
      setLoadingLabs(false);
    }
  }, [selectedLabId]);

  const fetchOverview = useCallback(async () => {
    setLoadingOverview(true);
    try {
      const response = await fetch(`${API_BASE}/api/lecturer/overview`, {
        headers: {
          Authorization: `Bearer ${sessionStorage.getItem('accessToken')}`,
        },
      });

      if (!response.ok) {
        console.info('Overview API not available yet');
        setOverview({
          totalSubmissions: 0,
          averageScore: null,
          activeLabs: 0,
          totalStudents: 0,
          totalLecturers: 0,
        });
        return;
      }

      const data = await response.json();
      setOverview(data);
      setHasData(true);
    } catch (err) {
      console.info('Could not fetch overview:', err.message);
      setOverview({
        totalSubmissions: 0,
        averageScore: null,
        activeLabs: 0,
        totalStudents: 0,
        totalLecturers: 0,
      });
    } finally {
      setLoadingOverview(false);
    }
  }, []);

  const fetchSubmissions = useCallback(async (labId, page = 0) => {
    if (!labId) return;
    setLoadingSubmissions(true);
    try {
      const response = await fetch(
        `${API_BASE}/api/labs/${labId}/submissions?page=${page}&size=${pagination.size}`,
        {
          headers: {
            Authorization: `Bearer ${sessionStorage.getItem('accessToken')}`,
          },
        }
      );

      if (!response.ok) {
        console.info('Submissions API not available yet');
        setSubmissions([]);
        setPagination(prev => ({ ...prev, total: 0, totalPages: 0 }));
        return;
      }

      const data = await response.json();
      setSubmissions(data.submissions || []);
      setPagination(data.pagination || { total: 0, page: 0, size: 20, totalPages: 0 });
    } catch (err) {
      console.info('Could not fetch submissions:', err.message);
      setSubmissions([]);
      setPagination(prev => ({ ...prev, total: 0, totalPages: 0 }));
    } finally {
      setLoadingSubmissions(false);
    }
  }, [pagination.size]);

  const fetchLabStatistics = useCallback(async (labId) => {
    if (!labId) return;
    setLoadingStatistics(true);
    try {
      const response = await fetch(`${API_BASE}/api/labs/${labId}/statistics`, {
        headers: {
          Authorization: `Bearer ${sessionStorage.getItem('accessToken')}`,
        },
      });

      if (!response.ok) {
        console.info('Lab statistics API not available yet');
        setLabStatistics(null);
        return;
      }

      const data = await response.json();
      setLabStatistics(data);
    } catch (err) {
      console.info('Could not fetch lab statistics:', err.message);
      setLabStatistics(null);
    } finally {
      setLoadingStatistics(false);
    }
  }, []);

  // ===== Effects =====

  useEffect(() => {
    const loadData = async () => {
      await Promise.all([
        fetchLabs(),
        fetchOverview(),
      ]);
    };
    loadData();
  }, [fetchLabs, fetchOverview]);

  useEffect(() => {
    if (selectedLabId) {
      fetchSubmissions(selectedLabId);
      fetchLabStatistics(selectedLabId);
    }
  }, [selectedLabId, fetchSubmissions, fetchLabStatistics]);

  // ===== Handlers =====

  const handleLabChange = (labId) => {
    setSelectedLabId(labId);
    // Reset pagination khi đổi lab
    setPagination(prev => ({ ...prev, page: 0 }));
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
      fetchSubmissions(selectedLabId, 0);
      fetchLabStatistics(selectedLabId);
    }
  };

  // ===== Formatting Helpers =====

  const formatStatValue = (value) => {
    if (value === null || value === undefined) return '--';
    if (typeof value === 'number') {
      if (Number.isInteger(value)) return value;
      return Math.round(value);
    }
    return value;
  };

  const formatStatPercent = (value) => {
    if (value === null || value === undefined) return '--';
    return `${Math.round(value)}%`;
  };

  // ===== Computed Values =====

  const overviewCards = useMemo(() => [
    {
      title: 'Total Submissions',
      value: formatStatValue(overview.totalSubmissions),
      subtitle: 'All submissions across all labs',
      icon: <FileText className="h-5 w-5 text-blue-600" />,
      accent: 'bg-blue-100 text-blue-600 dark:bg-blue-900/30',
    },
    {
      title: 'Average Score',
      value: formatStatPercent(overview.averageScore),
      subtitle: 'Across all visible submissions',
      icon: <BarChart3 className="h-5 w-5 text-emerald-600" />,
      accent: 'bg-emerald-100 text-emerald-600 dark:bg-emerald-900/30',
    },
    {
      title: 'Active Labs',
      value: formatStatValue(overview.activeLabs),
      subtitle: 'Labs with submissions',
      icon: <FolderKanban className="h-5 w-5 text-purple-600" />,
      accent: 'bg-purple-100 text-purple-600 dark:bg-purple-900/30',
    },
    {
      title: 'Total Students',
      value: formatStatValue(overview.totalStudents),
      subtitle: 'Active students in the system',
      icon: <Users className="h-5 w-5 text-amber-600" />,
      accent: 'bg-amber-100 text-amber-600 dark:bg-amber-900/30',
    },
  ], [overview]);

  // ===== Render =====

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
            {/* ===== Overview Cards ===== */}
            <div className="grid grid-cols-1 gap-6 md:grid-cols-2 xl:grid-cols-4">
              {overviewCards.map((card) => (
                <LecturerOverviewCard key={card.title} {...card} />
              ))}
            </div>

            {/* ===== Lab Selection & Statistics ===== */}
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
                {/* Lab Selector */}
                <div className="rounded-3xl border border-gray-200 bg-white p-5 shadow-sm transition-colors dark:border-gray-700 dark:bg-[#1e2530]">
                  <div className="mb-5 flex items-center justify-between gap-3">
                    <div>
                      <h3 className="text-base font-semibold text-gray-900 dark:text-white">Select Lab</h3>
                      <p className="text-sm text-gray-500 dark:text-gray-400">Choose a lab to review submissions.</p>
                    </div>
                    {selectedLabId && (
                      <span className="rounded-full bg-purple-100 px-3 py-1 text-xs font-semibold text-purple-700 dark:bg-purple-900/30 dark:text-purple-300">
                        {submissions.length} submissions
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
                          <span>{lab.name}</span>
                        </button>
                      ))
                    )}
                  </div>
                </div>

                {/* Lab Statistics */}
                <div className="space-y-4">
                  <div className="rounded-3xl border border-gray-200 bg-white p-5 shadow-sm transition-colors dark:border-gray-700 dark:bg-[#1e2530]">
                    <div className="flex items-center justify-between">
                      <div>
                        <h3 className="text-base font-semibold text-gray-900 dark:text-white">
                          {labs.find(l => l.id === selectedLabId)?.name || 'Lab Overview'}
                        </h3>
                        <p className="text-sm text-gray-500 dark:text-gray-400">Performance summary</p>
                      </div>
                    </div>
                    
                    {loadingStatistics ? (
                      <div className="flex items-center justify-center py-8">
                        <div className="w-8 h-8 border-4 border-purple-500 border-t-transparent rounded-full animate-spin" />
                      </div>
                    ) : labStatistics ? (
                      <div className="mt-4 grid grid-cols-2 gap-4">
                        <div>
                          <p className="text-xs text-gray-400 dark:text-gray-500">Average Score</p>
                          <p className="text-xl font-semibold text-gray-900 dark:text-white">
                            {formatStatPercent(labStatistics.averageScore)}
                          </p>
                        </div>
                        <div>
                          <p className="text-xs text-gray-400 dark:text-gray-500">Pass Rate</p>
                          <p className="text-xl font-semibold text-gray-900 dark:text-white">
                            {formatStatPercent(labStatistics.passRate)}
                          </p>
                        </div>
                        <div>
                          <p className="text-xs text-gray-400 dark:text-gray-500">Highest Score</p>
                          <p className="text-xl font-semibold text-gray-900 dark:text-white">
                            {formatStatPercent(labStatistics.highestScore)}
                          </p>
                        </div>
                        <div>
                          <p className="text-xs text-gray-400 dark:text-gray-500">Lowest Score</p>
                          <p className="text-xl font-semibold text-gray-900 dark:text-white">
                            {formatStatPercent(labStatistics.lowestScore)}
                          </p>
                        </div>
                      </div>
                    ) : (
                      <p className="mt-4 text-sm text-gray-400 dark:text-gray-600 text-center py-4">
                        No statistics available
                      </p>
                    )}
                  </div>

                  {/* Most Failed Items */}
                  {labStatistics && (labStatistics.mostFailedClasses?.length > 0 || labStatistics.mostFailedTestcases?.length > 0) && (
                    <div className="grid grid-cols-2 gap-4">
                      {labStatistics.mostFailedClasses?.length > 0 && (
                        <div className="rounded-3xl border border-gray-200 bg-white p-4 shadow-sm transition-colors dark:border-gray-700 dark:bg-[#1e2530]">
                          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-gray-400 dark:text-gray-500">
                            Most Failed Classes
                          </p>
                          <div className="mt-2 space-y-1">
                            {labStatistics.mostFailedClasses.slice(0, 3).map((item, idx) => (
                              <div key={idx} className="flex justify-between text-sm">
                                <span className="text-gray-700 dark:text-gray-300">{item.name}</span>
                                <span className="text-red-500 dark:text-red-400">{item.count}</span>
                              </div>
                            ))}
                          </div>
                        </div>
                      )}
                      {labStatistics.mostFailedTestcases?.length > 0 && (
                        <div className="rounded-3xl border border-gray-200 bg-white p-4 shadow-sm transition-colors dark:border-gray-700 dark:bg-[#1e2530]">
                          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-gray-400 dark:text-gray-500">
                            Most Failed Testcases
                          </p>
                          <div className="mt-2 space-y-1">
                            {labStatistics.mostFailedTestcases.slice(0, 3).map((item, idx) => (
                              <div key={idx} className="flex justify-between text-sm">
                                <span className="text-gray-700 dark:text-gray-300">{item.name}</span>
                                <span className="text-purple-500 dark:text-purple-400">{item.count}</span>
                              </div>
                            ))}
                          </div>
                        </div>
                      )}
                    </div>
                  )}
                </div>
              </div>
            </DashboardSection>

            {/* ===== Submissions Table ===== */}
            <DashboardSection title="Submissions" subtitle="Review student submissions for the selected lab">
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
                    mostFailedClass: labStatistics?.mostFailedClasses?.[0]?.name ?? 'None',
                    mostFailedTest: labStatistics?.mostFailedTestcases?.[0]?.name ?? 'None',
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
        ) : (
          <div className="rounded-xl border border-gray-200 bg-white p-10 text-center text-gray-700 shadow-sm dark:border-gray-700 dark:bg-[#1e2530] dark:text-gray-300 mx-4 sm:mx-6 lg:mx-8 max-w-full overflow-x-hidden">
            <h2 className="mb-3 text-xl font-semibold">Reports</h2>
            <p>Report generation is coming soon.</p>
          </div>
        )}
      </AppShell>
    </div>
  );
}