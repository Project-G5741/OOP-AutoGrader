import { useMemo, useState } from 'react';
import { BarChart3, FileText, FolderKanban, Users } from 'lucide-react';
import { useTheme } from '../context/ThemeContext';
import AppShell from '../components/layout/AppShell';
import DashboardSection from '../components/lecturer/DashboardSection';
import LecturerOverviewCard from '../components/lecturer/LecturerOverviewCard';
import SubmissionTable from '../components/lecturer/SubmissionTable';
import UploadPanel from '../components/lecturer/UploadPanel';
import Select from '../components/ui/Select';
import UserManagement from './UserManagement';

const ROOT_FOLDERS = [
  { name: 'Lab01_2190001_John_Doe', type: 'folder' },
  { name: 'Lab01_2190002_Jane_Smith', type: 'folder' },
  { name: 'Lab01_2190003_Mike_Johnson', type: 'folder' },
  { name: 'Lab01_2190004_Sarah_Williams', type: 'folder' },
];

const SUBMISSIONS = [
  {
    student: 'John Doe',
    id: '2190001',
    score: 92,
    failedClasses: ['Car'],
    failedTests: ['Testcase 1', 'Testcase 3'],
  },
  {
    student: 'Jane Smith',
    id: '2190002',
    score: 88,
    failedClasses: ['Book', 'Library'],
    failedTests: ['Testcase 2', 'Testcase 4', 'Testcase 5'],
  },
  {
    student: 'Mike Johnson',
    id: '2190003',
    score: 95,
    failedClasses: [],
    failedTests: ['Testcase 1'],
  },
  {
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

  const summary = useMemo(() => computeSummary(SUBMISSIONS), []);

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
      <AppShell user={user} onLogout={onLogout} showNav activeNav={activeNav} onNavigate={setActiveNav}>
        {activeNav === 'dashboard' ? (
          <div className="space-y-6">
            <div className="grid grid-cols-1 gap-6 md:grid-cols-2 xl:grid-cols-4">
              {overviewCards.map((card) => (
                <LecturerOverviewCard key={card.title} {...card} />
              ))}
            </div>

            <div className="grid grid-cols-1 gap-6 xl:grid-cols-[1.2fr_0.8fr]">
              <DashboardSection
                title="Submission intake"
                subtitle="Upload compressed files and select the active lab"
              >
                <div className="space-y-4">
                  <Select label="Select Lab" options={LAB_OPTIONS} />
                  <UploadPanel title="Drop .zip or .rar files here" description="Upload compressed student submissions to begin review" />
                </div>
              </DashboardSection>

              <DashboardSection title="Current review queue" subtitle="A compact view of the latest student work">
                <div className="rounded-lg border border-dashed border-gray-300 p-5 text-sm text-gray-600 dark:border-gray-700 dark:text-gray-300">
                  <p className="font-medium text-gray-900 dark:text-white">{ROOT_FOLDERS.length} student folders are ready</p>
                  <p className="mt-2">Each folder represents a submission batch that can be inspected and graded.</p>
                </div>
              </DashboardSection>
            </div>

            <DashboardSection title="Grading overview" subtitle="Recent submissions and evaluation summary">
              <SubmissionTable submissions={SUBMISSIONS} summary={summary} />
            </DashboardSection>
          </div>
        ) : activeNav === 'users' ? (
          <UserManagement hideNav noShell user={user} onLogout={onLogout} />
        ) : activeNav === 'projects' ? (
          <div className="rounded-xl border border-gray-200 bg-white p-10 text-center text-gray-700 shadow-sm dark:border-gray-700 dark:bg-[#1e2530] dark:text-gray-300">
            <h2 className="mb-3 text-xl font-semibold">Projects</h2>
            <p>Project tracking is coming soon.</p>
          </div>
        ) : (
          <div className="rounded-xl border border-gray-200 bg-white p-10 text-center text-gray-700 shadow-sm dark:border-gray-700 dark:bg-[#1e2530] dark:text-gray-300">
            <h2 className="mb-3 text-xl font-semibold">Reports</h2>
            <p>Report generation is coming soon.</p>
          </div>
        )}
      </AppShell>
    </div>
  );
}
