import { useMemo, useState, useEffect, useCallback } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { BarChart3, FileText, FolderKanban, Users, RefreshCw, ChevronRight, ChevronUp, ChevronDown } from 'lucide-react';
import { useTheme } from '../context/ThemeContext';
import AppShell from '../components/layout/AppShell';
import ChangePasswordModal from '../components/student/ChangePasswordModal';
import DashboardSection from '../components/lecturer/DashboardSection';
import OverviewPanel from '../components/lecturer/OverviewPanel';
import ReportsPage from './Reports';
import SubmissionTable from '../components/lecturer/SubmissionTable';
import GradeOverviewTable from '../components/lecturer/GradeOverviewTable';
import GradeOverviewSubmissionHistory from '../components/lecturer/GradeOverviewSubmissionHistory';
import ExportMenu from '../components/lecturer/ExportMenu';
import LecturerSubmissionDrawer from '../components/lecturer/LecturerSubmissionDrawer';
import LabAttemptHistoryDrawer from '../components/lecturer/LabAttemptHistoryDrawer';
import { exportGradeOverview, exportRosterRows } from '../components/lecturer/exportRoster';
import UserManagement from './UserManagement';
import SolutionManagement from './SolutionManagement';
import { formatNumber, formatText, hasItems } from '../utils/formatters';
import { LECTURER_NAV_TO_ROUTE, LECTURER_ROUTE_TO_NAV, ROUTES } from '../utils/authRoutes';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';
const ROSTER_PAGE_SIZE = 5;
const GRADE_OVERVIEW_EXPORT_PAGE_SIZE = 100;
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

async function fetchAllLabSubmissions(labId, sort = 'studentName,asc') {
  const sortQuery = sort ? `?sort=${encodeURIComponent(sort)}` : '';
  const response = await fetch(`${API_BASE}/api/labs/${labId}/submissions/export${sortQuery}`, {
    headers: authHeaders(),
  });
  if (!response.ok) return [];
  return response.json();
}

async function fetchAllGradeOverview() {
  let page = 0;
  let totalPages = 1;
  let labs = [];
  const students = [];

  while (page < totalPages) {
    const response = await fetch(
      `${API_BASE}/api/lecturer/grade-overview?page=${page}&size=${GRADE_OVERVIEW_EXPORT_PAGE_SIZE}`,
      { headers: authHeaders() },
    );
    if (!response.ok) {
      return { labs: [], students: [] };
    }
    const data = await response.json();
    if (page === 0) {
      labs = data.labs ?? [];
    }
    students.push(...(data.content ?? []));
    totalPages = Math.max(data.totalPages ?? 1, 1);
    page += 1;
  }

  return { labs, students };
}

export default function LecturerDashboard({ user, onLogout }) {
  const { isDark } = useTheme();
  const location = useLocation();
  const navigate = useNavigate();
  const activeNav = LECTURER_ROUTE_TO_NAV[location.pathname] || 'dashboard';
  const [showProfile, setShowProfile] = useState(false);

  const [labs, setLabs] = useState([]);
  const [selectedLabId, setSelectedLabId] = useState(null);
  const [overview, setOverview] = useState(EMPTY_OVERVIEW);
  const [submissions, setSubmissions] = useState([]);
  const [labStatistics, setLabStatistics] = useState(null);
  const [pagination, setPagination] = useState({
    total: 0,
    page: 0,
    size: ROSTER_PAGE_SIZE,
    totalPages: 0,
  });
  const [challengeSubmissions, setChallengeSubmissions] = useState([]);
  const [challengePagination, setChallengePagination] = useState({
    total: 0,
    page: 0,
    size: ROSTER_PAGE_SIZE,
    totalPages: 0,
  });
  const [selectedRosterStudent, setSelectedRosterStudent] = useState(null);
  const [rosterSort, setRosterSort] = useState({ field: 'studentName', direction: 'asc' });
  const [selectedChallengeStudent, setSelectedChallengeStudent] = useState(null);
  const [showAttemptHistory, setShowAttemptHistory] = useState(false);
  const [showChallengeDrawer, setShowChallengeDrawer] = useState(false);

  const [loadingLabs, setLoadingLabs] = useState(false);
  const [loadingOverview, setLoadingOverview] = useState(false);
  const [loadingSubmissions, setLoadingSubmissions] = useState(false);
  const [loadingChallengeSubmissions, setLoadingChallengeSubmissions] = useState(false);
  const [loadingStatistics, setLoadingStatistics] = useState(false);
  const [labsError, setLabsError] = useState(null);
  const [overviewError, setOverviewError] = useState(null);
  const [submissionsError, setSubmissionsError] = useState(null);
  const [challengeSubmissionsError, setChallengeSubmissionsError] = useState(null);
  const [statisticsError, setStatisticsError] = useState(null);
  const [gradeOverview, setGradeOverview] = useState({ labs: [], content: [] });
  const [gradeOverviewPagination, setGradeOverviewPagination] = useState({
    total: 0,
    page: 0,
    size: ROSTER_PAGE_SIZE,
    totalPages: 0,
  });
  const [loadingGradeOverview, setLoadingGradeOverview] = useState(false);
  const [gradeOverviewError, setGradeOverviewError] = useState(null);
  const [selectedGradeStudent, setSelectedGradeStudent] = useState(null);
  const [gradeStudentHistory, setGradeStudentHistory] = useState([]);
  const [loadingGradeStudentHistory, setLoadingGradeStudentHistory] = useState(false);
  const [gradeStudentHistoryError, setGradeStudentHistoryError] = useState(null);
  const [historyLabFilter, setHistoryLabFilter] = useState('All Labs');
  const [historySortDirection, setHistorySortDirection] = useState('desc');
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

  const fetchSubmissions = useCallback(async (labId, page = 0, sort = 'studentName,asc') => {
    if (!labId) return;
    setLoadingSubmissions(true);
    setSubmissionsError(null);
    try {
      const response = await fetch(
        `${API_BASE}/api/labs/${labId}/submissions?page=${page}&size=${ROSTER_PAGE_SIZE}&sort=${encodeURIComponent(sort)}`,
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
        size: data.size ?? ROSTER_PAGE_SIZE,
        totalPages: data.totalPages ?? 0,
      });
    } catch {
      setSubmissions([]);
      setPagination((prev) => ({ ...prev, total: 0, totalPages: 0, page: 0 }));
      setSubmissionsError('Unable to load student roster');
    } finally {
      setLoadingSubmissions(false);
    }
  }, []);

  const fetchChallengeSubmissions = useCallback(async (labId, challengeId, page = 0) => {
    if (!labId || !challengeId) return;
    setLoadingChallengeSubmissions(true);
    setChallengeSubmissionsError(null);
    try {
      const response = await fetch(
        `${API_BASE}/api/labs/${labId}/challenges/${challengeId}/students?page=${page}&size=${ROSTER_PAGE_SIZE}&sort=submittedAt,desc`,
        { headers: authHeaders() },
      );

      if (!response.ok) {
        setChallengeSubmissions([]);
        setChallengePagination((prev) => ({ ...prev, total: 0, totalPages: 0, page: 0 }));
        setChallengeSubmissionsError('Unable to load challenge submissions');
        return;
      }

      const data = await response.json();
      setChallengeSubmissions(data.content ?? []);
      setChallengePagination({
        total: data.totalElements ?? 0,
        page: data.number ?? 0,
        size: data.size ?? ROSTER_PAGE_SIZE,
        totalPages: data.totalPages ?? 0,
      });
    } catch {
      setChallengeSubmissions([]);
      setChallengePagination((prev) => ({ ...prev, total: 0, totalPages: 0, page: 0 }));
      setChallengeSubmissionsError('Unable to load challenge submissions');
    } finally {
      setLoadingChallengeSubmissions(false);
    }
  }, []);

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

  const fetchGradeOverview = useCallback(async (page = 0) => {
    setLoadingGradeOverview(true);
    setGradeOverviewError(null);
    try {
      const response = await fetch(
        `${API_BASE}/api/lecturer/grade-overview?page=${page}&size=${ROSTER_PAGE_SIZE}`,
        { headers: authHeaders() },
      );
      if (!response.ok) {
        setGradeOverview({ labs: [], content: [] });
        setGradeOverviewPagination((prev) => ({ ...prev, total: 0, totalPages: 0, page: 0 }));
        setGradeOverviewError('Unable to load grade overview');
        return;
      }
      const data = await response.json();
      setGradeOverview({
        labs: data.labs ?? [],
        content: data.content ?? [],
      });
      setGradeOverviewPagination({
        total: data.totalElements ?? 0,
        page: data.page ?? 0,
        size: data.size ?? ROSTER_PAGE_SIZE,
        totalPages: data.totalPages ?? 0,
      });
    } catch {
      setGradeOverview({ labs: [], content: [] });
      setGradeOverviewPagination((prev) => ({ ...prev, total: 0, totalPages: 0, page: 0 }));
      setGradeOverviewError('Unable to load grade overview');
    } finally {
      setLoadingGradeOverview(false);
    }
  }, []);

  useEffect(() => {
    fetchLabs();
    fetchOverview();
  }, [fetchLabs, fetchOverview]);

  useEffect(() => {
    if (activeNav === 'grading') {
      fetchGradeOverview(0);
    }
  }, [activeNav, fetchGradeOverview]);

  useEffect(() => {
    if (selectedLabId) {
      setActiveTab('overview');
      setChallengePagination((prev) => ({ ...prev, page: 0 }));
      setRosterSort({ field: 'studentName', direction: 'asc' });
      void Promise.all([
        fetchSubmissions(selectedLabId, 0, 'studentName,asc'),
        fetchLabStatistics(selectedLabId),
        fetchChallengesForLab(selectedLabId),
      ]);
    }
  }, [selectedLabId, fetchSubmissions, fetchLabStatistics, fetchChallengesForLab]);

  useEffect(() => {
    if (!selectedLabId || activeTab === 'overview') {
      return;
    }
    fetchChallengeSubmissions(selectedLabId, activeTab, challengePagination.page);
  }, [selectedLabId, activeTab, challengePagination.page, fetchChallengeSubmissions]);

  const handleLabChange = (labId) => {
    if (labId === selectedLabId) return;
    setSelectedLabId(labId);
    setPagination((prev) => ({ ...prev, page: 0 }));
  };
  const handlePageChange = (newPage) => {
    if (selectedLabId) {
      fetchSubmissions(selectedLabId, newPage, `${rosterSort.field},${rosterSort.direction}`);
    }
  };

  const handleRosterSort = (field) => {
    const next =
      rosterSort.field === field
        ? { field, direction: rosterSort.direction === 'asc' ? 'desc' : 'asc' }
        : { field, direction: 'asc' };
    setRosterSort(next);
    setPagination((prev) => ({ ...prev, page: 0 }));
    if (selectedLabId) {
      fetchSubmissions(selectedLabId, 0, `${next.field},${next.direction}`);
    }
  };

  const handleChallengePageChange = (newPage) => {
    setChallengePagination((prev) => ({ ...prev, page: newPage }));
  };

  const handleChallengeTabSelect = (challengeId) => {
    setActiveTab(challengeId);
    setChallengePagination((prev) => ({ ...prev, page: 0 }));
  };

  const handleRefresh = () => {
    if (activeNav === 'grading') {
      fetchGradeOverview(gradeOverviewPagination.page);
      return;
    }
    fetchOverview();
    fetchLabs();
    if (selectedLabId) {
      void Promise.all([
        fetchSubmissions(selectedLabId, pagination.page, `${rosterSort.field},${rosterSort.direction}`),
        fetchLabStatistics(selectedLabId),
        fetchChallengesForLab(selectedLabId),
      ]);
      if (activeTab !== 'overview') {
        fetchChallengeSubmissions(selectedLabId, activeTab, challengePagination.page);
      }
    }
  };
  const handleGradeOverviewPageChange = (newPage) => {
    fetchGradeOverview(newPage);
  };

  const fetchStudentSubmissionHistory = useCallback(async (studentId) => {
    setLoadingGradeStudentHistory(true);
    setGradeStudentHistoryError(null);
    try {
      const response = await fetch(`${API_BASE}/api/analytics/student/${studentId}`, {
        headers: authHeaders(),
      });
      if (!response.ok) {
        throw new Error('Unable to load submission history');
      }
      const data = await response.json();
      setGradeStudentHistory(Array.isArray(data.submissionHistory) ? data.submissionHistory : []);
    } catch (err) {
      setGradeStudentHistory([]);
      setGradeStudentHistoryError(err.message || 'Unable to load submission history');
    } finally {
      setLoadingGradeStudentHistory(false);
    }
  }, []);

  const handleGradeStudentSelect = (student) => {
    setSelectedGradeStudent(student);
    setHistoryLabFilter('All Labs');
    setHistorySortDirection('desc');
    if (student?.studentId) {
      void fetchStudentSubmissionHistory(student.studentId);
    }
  };

  const gradeStudentHistoryLabOptions = useMemo(() => {
    const labNames = gradeStudentHistory
      .map((item) => item.labName)
      .filter((name) => name != null && String(name).trim().length > 0);
    return ['All Labs', ...Array.from(new Set(labNames))];
  }, [gradeStudentHistory]);

  const filteredGradeStudentHistoryRows = useMemo(() => {
    if (!selectedGradeStudent) return [];
    const filtered = gradeStudentHistory.filter((item) => {
      if (historyLabFilter === 'All Labs') return true;
      return item.labName === historyLabFilter;
    });
    const sorted = [...filtered].sort((a, b) => {
      const aTime = Date.parse(a.submittedAt ?? '') || 0;
      const bTime = Date.parse(b.submittedAt ?? '') || 0;
      return historySortDirection === 'desc' ? bTime - aTime : aTime - bTime;
    });
    return sorted.map((item) => ({
      studentName: selectedGradeStudent.studentName,
      irn: selectedGradeStudent.irn,
      labName: item.labName,
      submittedAt: item.submittedAt,
      score: item.score,
    }));
  }, [gradeStudentHistory, historyLabFilter, historySortDirection, selectedGradeStudent]);

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
    { label: 'Average Score', value: formatNumber(labStatistics?.averageScore) },
    { label: 'Completion Rate', value: formatNumber(labStatistics?.completionRate) },
    { label: 'Highest Score', value: formatNumber(labStatistics?.highestScore) },
    { label: 'Lowest Score', value: formatNumber(labStatistics?.lowestScore) },
    { label: 'Total Submissions', value: formatNumber(labStatistics?.submissionCount) },
    { label: 'Enrolled Students', value: formatNumber(labStatistics?.studentCount) },
    { label: 'Students Submitted', value: formatNumber(labStatistics?.studentsSubmitted) },
  ], [labStatistics]);

  const exportOverview = async (format) => {
    if (!selectedLabId) return;
    const all = await fetchAllLabSubmissions(selectedLabId, `${rosterSort.field},${rosterSort.direction}`);
    const rows = all.map((r) => ({
      'Student Name': r.studentName ?? '',
      'Student ID': r.studentCode ?? '',
      Attempts: r.attempt ?? 0,
      'Lab Score': r.score != null ? formatNumber(r.score) : 'Not Submitted',
      'Last Submitted': r.submittedAt ?? '—',
    }));
    const labName = selectedLab?.name ?? selectedLabId;
    await exportRosterRows(format, {
      rows,
      labName,
      fileBase: `lab_${String(labName).replace(/\s+/g, '_')}_roster`,
    });
  };

  const handleExportGradeOverview = async (format) => {
    const { labs, students } = await fetchAllGradeOverview();
    if (!students.length) return;
    await exportGradeOverview(format, { labs, students });
  };

  const handleRosterView = (student) => {
    setSelectedRosterStudent(student);
    setShowAttemptHistory(true);
  };

  const handleChallengeView = (student) => {
    setSelectedChallengeStudent(student);
    setShowChallengeDrawer(true);
  };

  const overviewCards = useMemo(() => [
    {
      title: 'Total Students',
      value: formatNumber(overview.totalStudents),
      icon: <Users className="h-5 w-5 text-amber-600" />,
      accent: 'bg-amber-100 text-amber-600 dark:bg-amber-900/30',
    },
    {
      title: 'Average Score',
      value: formatNumber(overview.averageScore),
      icon: <BarChart3 className="h-5 w-5 text-emerald-600" />,
      accent: 'bg-emerald-100 text-emerald-600 dark:bg-emerald-900/30',
    },
    {
      title: 'Total Labs',
      value: formatNumber(overview.totalLabs),
      icon: <FolderKanban className="h-5 w-5 text-purple-600" />,
      accent: 'bg-purple-100 text-purple-600 dark:bg-purple-900/30',
    },
    {
      title: 'At-Risk Students',
      value: formatNumber(overview.atRiskStudents),
      icon: <FileText className="h-5 w-5 text-blue-600" />,
      accent: 'bg-blue-100 text-blue-600 dark:bg-blue-900/30',
    },
  ], [overview]);

  const handleShellCommand = useCallback((cmd) => {
    if (cmd === 'home') navigate(ROUTES.lecturerDashboard);
    else if (cmd === 'history') navigate(ROUTES.lecturerSolution);
    else if (cmd === 'editProfile') setShowProfile(true);
  }, [navigate]);

  const handleNavChange = useCallback((navId) => {
    const target = LECTURER_NAV_TO_ROUTE[navId];
    if (target) navigate(target);
  }, [navigate]);

  const isInitialLoading = loadingLabs || loadingOverview;

  return (
    <div className={isDark ? 'dark' : ''}>
      <AppShell
        user={user}
        onLogout={onLogout}
        showNav
        hideUserMenu
        activeNav={activeNav}
        onNavigate={handleNavChange}
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
              title="Grading overview"
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
                          onClick={() => handleChallengeTabSelect(challenge.id)}
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
                                <ExportMenu onExport={exportOverview} />
                              </div>
                            </>
                          )}

                          <div>
                            <div className="mb-3 flex flex-wrap items-center justify-between gap-3">
                              <h4 className="text-sm font-semibold text-gray-900 dark:text-white">Student roster</h4>
                              <div className="flex flex-wrap items-center gap-2">
                                <button
                                  type="button"
                                  onClick={() => handleRosterSort('studentName')}
                                  className={`inline-flex items-center gap-2 rounded-lg border px-3 py-2 text-sm font-medium transition-colors ${
                                    rosterSort.field === 'studentName'
                                      ? 'border-purple-300 bg-purple-50 text-purple-800 dark:border-purple-600 dark:bg-purple-900/30 dark:text-purple-200'
                                      : 'border-gray-200 bg-gray-50 text-gray-700 hover:bg-gray-100 dark:border-gray-700 dark:bg-[#151b24] dark:text-gray-200 dark:hover:bg-[#1a1a2c]'
                                  }`}
                                >
                                  {rosterSort.field === 'studentName' && rosterSort.direction === 'asc' ? (
                                    <ChevronUp className="h-4 w-4" />
                                  ) : rosterSort.field === 'studentName' ? (
                                    <ChevronDown className="h-4 w-4" />
                                  ) : null}
                                  Sort by name
                                </button>
                                <button
                                  type="button"
                                  onClick={() => handleRosterSort('score')}
                                  className={`inline-flex items-center gap-2 rounded-lg border px-3 py-2 text-sm font-medium transition-colors ${
                                    rosterSort.field === 'score'
                                      ? 'border-purple-300 bg-purple-50 text-purple-800 dark:border-purple-600 dark:bg-purple-900/30 dark:text-purple-200'
                                      : 'border-gray-200 bg-gray-50 text-gray-700 hover:bg-gray-100 dark:border-gray-700 dark:bg-[#151b24] dark:text-gray-200 dark:hover:bg-[#1a1a2c]'
                                  }`}
                                >
                                  {rosterSort.field === 'score' && rosterSort.direction === 'asc' ? (
                                    <ChevronUp className="h-4 w-4" />
                                  ) : rosterSort.field === 'score' ? (
                                    <ChevronDown className="h-4 w-4" />
                                  ) : null}
                                  Sort by score
                                </button>
                              </div>
                            </div>
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
                                onView={handleRosterView}
                                requireSubmissionForView={false}
                              />
                            )}
                          </div>
                        </div>
                      ) : (
                        <div>
                          <div className="mb-4 flex items-center justify-between gap-3">
                            <h4 className="text-base font-semibold text-gray-900 dark:text-white">
                              {activeChallengeTab
                                ? `${challengeTabLabel(activeChallengeTab.challenge, activeChallengeTab.index)} Submissions`
                                : 'Challenge Submissions'}
                            </h4>
                          </div>
                          {challengeSubmissionsError && (
                            <p className="mb-4 text-sm text-amber-700 dark:text-amber-300">{challengeSubmissionsError}</p>
                          )}
                          {loadingChallengeSubmissions ? (
                            <div className="flex items-center justify-center py-12">
                              <div className="w-8 h-8 border-4 border-purple-500 border-t-transparent rounded-full animate-spin" />
                            </div>
                          ) : (
                            <SubmissionTable
                              submissions={challengeSubmissions}
                              pagination={challengePagination}
                              onPageChange={handleChallengePageChange}
                              onView={handleChallengeView}
                              attemptLabel="Attempts"
                              viewLabel="View"
                            />
                          )}
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              </div>
            </DashboardSection>
          </div>
        ) : activeNav === 'grading' ? (
          <div className="space-y-6 px-4 sm:px-6 lg:px-8 max-w-full overflow-x-hidden">
            <DashboardSection
              title="Grading"
              actions={
                <div className="flex items-center gap-2">
                  <ExportMenu
                    onExport={handleExportGradeOverview}
                    disabled={loadingGradeOverview || gradeOverviewPagination.total === 0}
                  />
                  <button
                    onClick={() => fetchGradeOverview(gradeOverviewPagination.page)}
                    className="p-2 rounded-lg border border-gray-200 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-[#151b24] transition-colors"
                    title="Refresh"
                  >
                    <RefreshCw className={`w-4 h-4 text-gray-500 dark:text-gray-400 ${loadingGradeOverview ? 'animate-spin' : ''}`} />
                  </button>
                </div>
              }
            >
              {gradeOverviewError && (
                <p className="mb-4 text-sm text-amber-700 dark:text-amber-300">{gradeOverviewError}</p>
              )}
              <GradeOverviewTable
                labs={gradeOverview.labs}
                students={gradeOverview.content}
                loading={loadingGradeOverview}
                pagination={gradeOverviewPagination}
                onPageChange={handleGradeOverviewPageChange}
                selectedStudentId={selectedGradeStudent?.studentId}
                onStudentSelect={handleGradeStudentSelect}
              />
              {selectedGradeStudent && (
                <GradeOverviewSubmissionHistory
                  student={selectedGradeStudent}
                  rows={filteredGradeStudentHistoryRows}
                  loading={loadingGradeStudentHistory}
                  error={gradeStudentHistoryError}
                  labFilter={historyLabFilter}
                  onLabFilterChange={setHistoryLabFilter}
                  sortDirection={historySortDirection}
                  onSortDirectionChange={setHistorySortDirection}
                  labOptions={gradeStudentHistoryLabOptions}
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
        ) : null}
      </AppShell>

      {showProfile && (
        <ChangePasswordModal isOpen={showProfile} user={user} onClose={() => setShowProfile(false)} />
      )}

      <LabAttemptHistoryDrawer
        open={showAttemptHistory}
        onClose={() => {
          setShowAttemptHistory(false);
          setSelectedRosterStudent(null);
        }}
        labId={selectedLabId}
        student={selectedRosterStudent}
        labName={selectedLab?.name}
      />

      <LecturerSubmissionDrawer
        open={showChallengeDrawer}
        onClose={() => {
          setShowChallengeDrawer(false);
          setSelectedChallengeStudent(null);
        }}
        labId={selectedLabId}
        challengeId={activeTab !== 'overview' ? activeTab : null}
        challengeLabel={activeChallengeTab ? challengeTabLabel(activeChallengeTab.challenge, activeChallengeTab.index) : 'Challenge'}
        student={selectedChallengeStudent}
      />
    </div>
  );
}
