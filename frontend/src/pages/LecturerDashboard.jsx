import { useMemo, useState } from 'react';
import { BarChart3, FileText, FolderKanban, Users } from 'lucide-react';
import { useTheme } from '../context/ThemeContext';
import AppShell from '../components/layout/AppShell';
import ProfileEditModal from '../components/student/ProfileEditModal';
import DashboardSection from '../components/lecturer/DashboardSection';
import LecturerOverviewCard from '../components/lecturer/LecturerOverviewCard';
import SubmissionTable from '../components/lecturer/SubmissionTable';
import Select from '../components/ui/Select';
import UserManagement from './UserManagement';
import SubmissionManagement from './SubmissionManagement';

const ROOT_FOLDERS = [
  { name: 'Lab01_2190001_John_Doe', type: 'folder' },
  { name: 'Lab01_2190002_Jane_Smith', type: 'folder' },
  { name: 'Lab01_2190003_Mike_Johnson', type: 'folder' },
  { name: 'Lab01_2190004_Sarah_Williams', type: 'folder' },
];

const SUBMISSIONS = [
  {
    labName: 'Lab 01: Abstraction',
    student: 'John Doe',
    id: '2190001',
    score: 92,
    failedClasses: ['Car'],
    failedTests: ['Testcase 1', 'Testcase 3'],
  },
  {
    labName: 'Lab 01: Abstraction',
    student: 'Jane Smith',
    id: '2190002',
    score: 88,
    failedClasses: ['Book', 'Library'],
    failedTests: ['Testcase 2', 'Testcase 4', 'Testcase 5'],
  },
  {
    labName: 'Lab 02: Polymorphism',
    student: 'Mike Johnson',
    id: '2190003',
    score: 95,
    failedClasses: [],
    failedTests: ['Testcase 1'],
  },
  {
    labName: 'Lab 03: Inheritance',
    student: 'Sarah Williams',
    id: '2190004',
    score: 76,
    failedClasses: ['Car', 'Book', 'Person'],
    failedTests: ['Testcase 1', 'Testcase 2', 'Testcase 3', 'Testcase 5', 'Testcase 6'],
  },
];

const LAB_OPTIONS = ['Lab 01: Abstraction', 'Lab 02: Polymorphism', 'Lab 03: Inheritance', 'Lab 04: Interface'];

function computeSummary(submissions) {
  const averageScore = Math.round(submissions.reduce((total, item) => total + item.score, 0) / submissions.length);
  const lowestScore = Math.min(...submissions.map((item) => item.score));

  const countByValue = (values) => {
    const freq = {};
    values.forEach((value) => {
      freq[value] = (freq[value] || 0) + 1;
    });
    return Object.entries(freq).sort((left, right) => right[1] - left[1])[0]?.[0] ?? 'None';
  };

  return {
    averageScore,
    lowestScore,
    mostFailedClass: countByValue(submissions.flatMap((item) => item.failedClasses)),
    mostFailedTest: countByValue(submissions.flatMap((item) => item.failedTests)),
  };
}

export default function LecturerDashboard({ user, onLogout }) {
  const { isDark } = useTheme();
  const [activeNav, setActiveNav] = useState('dashboard');
  const [selectedLab, setSelectedLab] = useState(LAB_OPTIONS[0]);
  const [showProfile, setShowProfile] = useState(false);

  const summary = useMemo(() => computeSummary(SUBMISSIONS), []);

  const filteredSubmissions = selectedLab
    ? SUBMISSIONS.filter((item) => item.labName === selectedLab)
    : SUBMISSIONS;

  const overviewSummary = useMemo(() => computeSummary(filteredSubmissions), [filteredSubmissions]);

  const overviewCards = [
    {
      title: 'Total submissions',
      value: SUBMISSIONS.length,
      subtitle: 'Students uploaded files for this lab',
      icon: <FileText className="h-5 w-5 text-blue-600" />,
      accent: 'bg-blue-100 text-blue-600 dark:bg-blue-900/30',
    },
    {
      title: 'Average score',
      value: `${summary.averageScore}%`,
      subtitle: 'Across all visible submissions',
      icon: <BarChart3 className="h-5 w-5 text-emerald-600" />,
      accent: 'bg-emerald-100 text-emerald-600 dark:bg-emerald-900/30',
    },
    {
      title: 'Active labs',
      value: LAB_OPTIONS.length,
      subtitle: 'Ready for grading and review',
      icon: <FolderKanban className="h-5 w-5 text-purple-600" />,
      accent: 'bg-purple-100 text-purple-600 dark:bg-purple-900/30',
    },
    {
      title: 'Managed users',
      value: '24',
      subtitle: 'Students and lecturers in the system',
      icon: <Users className="h-5 w-5 text-amber-600" />,
      accent: 'bg-amber-100 text-amber-600 dark:bg-amber-900/30',
    },
  ];

  return (
    <div className={isDark ? 'dark' : ''}>
      <AppShell
          user={user}
          onLogout={onLogout}
          showNav
          activeNav={activeNav}
          onNavigate={setActiveNav}
          onCommand={(cmd) => {
            if (cmd === 'home') setActiveNav('dashboard');
            else if (cmd === 'history') setActiveNav('projects');
            else if (cmd === 'editProfile') setShowProfile(true);
          }}
      >
        {activeNav === 'dashboard' ? (
          <div className="space-y-6">
            <div className="grid grid-cols-1 gap-6 md:grid-cols-2 xl:grid-cols-4">
              {overviewCards.map((card) => (
                <LecturerOverviewCard key={card.title} {...card} />
              ))}
            </div>

            <DashboardSection title="Grading overview" subtitle="Recent submissions and evaluation summary">
              <div className="grid gap-6 xl:grid-cols-[0.4fr_0.6fr]">
                <div className="rounded-3xl border border-gray-200 bg-white p-5 shadow-sm transition-colors dark:border-gray-700 dark:bg-[#1e2530]">
                  <div className="mb-5 flex items-center justify-between gap-3">
                    <div>
                      <h3 className="text-base font-semibold text-gray-900 dark:text-white">Select Lab</h3>
                      <p className="text-sm text-gray-500 dark:text-gray-400">Choose the current grading lab.</p>
                    </div>
                    <span className="rounded-full bg-purple-100 px-3 py-1 text-xs font-semibold text-purple-700 dark:bg-purple-900/30 dark:text-purple-300">
                      {filteredSubmissions.length} submissions
                    </span>
                  </div>
                  <div className="space-y-3">
                    {LAB_OPTIONS.map((lab) => (
                      <button
                        key={lab}
                        type="button"
                        onClick={() => setSelectedLab(lab)}
                        className={`w-full rounded-2xl border px-4 py-3 text-left text-sm transition ${
                          selectedLab === lab
                            ? 'border-purple-500 bg-purple-950/30 text-white'
                            : 'border-gray-200 bg-white text-gray-700 hover:border-purple-400 hover:bg-purple-50 dark:border-gray-700 dark:bg-[#141a23] dark:text-gray-300'
                        }`}
                      >
                        <div className="flex items-center justify-between gap-3">
                          <span>{lab}</span>
                          <span className="text-xs text-gray-400 dark:text-gray-500">{SUBMISSIONS.filter((item) => item.labName === lab).length} submissions</span>
                        </div>
                      </button>
                    ))}
                  </div>
                </div>

                <div className="space-y-4">
                  <div className="flex items-center justify-between rounded-3xl border border-gray-200 bg-white p-5 shadow-sm transition-colors dark:border-gray-700 dark:bg-[#1e2530]">
                    <div>
                      <h3 className="text-base font-semibold text-gray-900 dark:text-white">{selectedLab} overview</h3>
                      <p className="text-sm text-gray-500 dark:text-gray-400">Submission performance and failure summary.</p>
                    </div>
                    <button className="rounded-2xl bg-emerald-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-emerald-500" type="button">
                      Export
                    </button>
                  </div>
                  <SubmissionTable submissions={filteredSubmissions} summary={overviewSummary} />
                </div>
              </div>
            </DashboardSection>

            <DashboardSection title="Grading overview" subtitle="Recent submissions and evaluation summary">
              <SubmissionTable submissions={SUBMISSIONS} summary={summary} />
            </DashboardSection>
          </div>
        ) : activeNav === 'users' ? (
          <UserManagement hideNav noShell user={user} onLogout={onLogout} />
        ) : activeNav === 'projects' ? (
          <SubmissionManagement />
        ) : (
          <div className="rounded-xl border border-gray-200 bg-white p-10 text-center text-gray-700 shadow-sm dark:border-gray-700 dark:bg-[#1e2530] dark:text-gray-300">
            <h2 className="mb-3 text-xl font-semibold">Reports</h2>
            <p>Report generation is coming soon.</p>
          </div>
        )}
      </AppShell>
      {showProfile && <ProfileEditModal isOpen={showProfile} onClose={() => setShowProfile(false)} />}
    </div>
  );
}
