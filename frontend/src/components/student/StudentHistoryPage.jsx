import { useState } from 'react';
import Header from '../Header';
import ProfileEditModal from './ProfileEditModal';
import { History, TrendingUp, Award, Clock, ChevronDown, ChevronUp, UserCircle } from 'lucide-react';

const HISTORY = [
  { id: 1, labName: 'Lab 01: Abstraction', attempt: 1, score: 62, submittedAt: '2026-06-01 09:14', failedClasses: ['Car', 'Person'], failedTests: ['TC-1', 'TC-2', 'TC-4'], plagiarism: 5 },
  { id: 2, labName: 'Lab 01: Abstraction', attempt: 2, score: 78, submittedAt: '2026-06-01 14:30', failedClasses: ['Car'], failedTests: ['TC-1'], plagiarism: 5 },
  { id: 3, labName: 'Lab 01: Abstraction', attempt: 3, score: 90, submittedAt: '2026-06-02 08:55', failedClasses: [], failedTests: [], plagiarism: 5 },
  { id: 4, labName: 'Lab 02: Polymorphism', attempt: 1, score: 75, submittedAt: '2026-06-10 10:00', failedClasses: ['Shape'], failedTests: ['TC-3'], plagiarism: 8 },
  { id: 5, labName: 'Lab 02: Polymorphism', attempt: 2, score: 88, submittedAt: '2026-06-10 16:20', failedClasses: [], failedTests: [], plagiarism: 8 },
  { id: 6, labName: 'Lab 03: Inheritance', attempt: 1, score: 95, submittedAt: '2026-06-18 11:45', failedClasses: [], failedTests: ['TC-2'], plagiarism: 3 },
  { id: 7, labName: 'Lab 04: Interface', attempt: 1, score: 55, submittedAt: '2026-07-01 09:30', failedClasses: ['Printable', 'Saveable'], failedTests: ['TC-1', 'TC-2', 'TC-5', 'TC-6'], plagiarism: 72 },
  { id: 8, labName: 'Lab 04: Interface', attempt: 2, score: 80, submittedAt: '2026-07-01 15:00', failedClasses: ['Printable'], failedTests: ['TC-1'], plagiarism: 15 },
];

const LAB_OPTIONS = ['All Labs', ...new Set(HISTORY.map((item) => item.labName))];

function scoreColor(score) {
  if (score >= 90) return 'text-green-400';
  if (score >= 75) return 'text-blue-400';
  if (score >= 60) return 'text-yellow-400';
  return 'text-red-400';
}

function scoreBadge(score) {
  if (score >= 90) return 'bg-green-900/30 border-green-500/30';
  if (score >= 75) return 'bg-blue-900/30 border-blue-500/30';
  if (score >= 60) return 'bg-yellow-900/30 border-yellow-500/30';
  return 'bg-red-900/30 border-red-500/30';
}

export default function StudentHistoryPage({ user, onLogout, onNavigate, onEditProfile }) {
  const [showProfile, setShowProfile] = useState(false);
  const [filterLab, setFilterLab] = useState('All Labs');
  const [expandedRow, setExpandedRow] = useState(null);

  const handleCommand = (cmd) => {
    if (cmd === 'home') {
      if (onNavigate) {
        onNavigate();
      }
    } else if (cmd === 'history') {
      setFilterLab('All Labs');
      setExpandedRow(null);
    } else if (cmd === 'editProfile') {
      setShowProfile(true);
    }
  };

  const filteredHistory = HISTORY.filter((item) => filterLab === 'All Labs' || item.labName === filterLab);
  const labs = Array.from(new Set(HISTORY.map((item) => item.labName)));
  const bestPerLab = labs.map((labName) => {
    const labAttempts = HISTORY.filter((item) => item.labName === labName);
    const bestScore = Math.max(...labAttempts.map((attempt) => attempt.score));
    return { labName, bestScore, attempts: labAttempts.length };
  });
  const averageBestScore = Math.round(bestPerLab.reduce((sum, item) => sum + item.bestScore, 0) / bestPerLab.length);

  return (
    <div className="min-h-screen bg-[#0d1117] text-white">   
    {/* <Header user={user} onLogout={onLogout} onNavigate={onNavigate} onCommand={handleCommand} />        */}
      {/* <div className="border-b border-gray-800 bg-[#161b22] px-6 py-4">
        <div className="flex flex-col gap-4 xl:flex-row xl:items-center xl:justify-between">
          <div className="flex items-center gap-3">
            <div className="grid h-11 w-11 place-items-center rounded-2xl bg-purple-600 text-white">
              <History className="h-5 w-5" />
            </div>
            <div>
              <h1 className="text-xl font-semibold">Student — Submission History</h1>
              <p className="text-sm text-gray-400">Review your lab attempts and update student profile details.</p>
            </div>
          </div>
        </div>
      </div> */}

      <div className="mx-auto flex max-w-7xl flex-col gap-6 px-6 py-8">
        <div className="grid gap-4 md:grid-cols-4">
          {[
            { label: 'Labs Attempted', value: labs.length, tone: 'text-purple-400', bg: 'bg-purple-900/30' },
            { label: 'Total Submissions', value: HISTORY.length, tone: 'text-blue-400', bg: 'bg-blue-900/30' },
            { label: 'Average Best Score', value: `${averageBestScore}%`, tone: 'text-green-400', bg: 'bg-green-900/30' },
            { label: 'Latest Submission', value: 'Jul 1', tone: 'text-orange-400', bg: 'bg-orange-900/30' },
          ].map((card) => (
            <div key={card.label} className="rounded-3xl border border-gray-800 bg-[#151b24] p-5">
              <div className="flex items-center justify-between gap-4">
                <p className="text-xs uppercase tracking-[0.2em] text-gray-500">{card.label}</p>
                <div className={`rounded-2xl p-3 ${card.bg}`}>
                  <TrendingUp className={`h-4 w-4 ${card.tone}`} />
                </div>
              </div>
              <p className={`mt-4 text-3xl font-semibold ${card.tone}`}>{card.value}</p>
            </div>
          ))}
        </div>

        <div className="grid gap-6 xl:grid-cols-[0.6fr_1fr]">
          <section className="rounded-3xl border border-gray-800 bg-[#151b24] p-6">
            <h2 className="text-sm font-semibold uppercase tracking-[0.2em] text-gray-400">Best Score per Lab</h2>
            <div className="mt-6 space-y-4">
              {bestPerLab.map((item) => (
                <div key={item.labName} className="space-y-2">
                  <div className="flex items-center justify-between text-sm text-gray-300">
                    <span>{item.labName}</span>
                    <span className={`font-semibold ${scoreColor(item.bestScore)}`}>{item.bestScore}%</span>
                  </div>
                  <div className="h-2 overflow-hidden rounded-full bg-gray-900">
                    <div className="h-full rounded-full bg-gradient-to-r from-purple-600 to-sky-500" style={{ width: `${item.bestScore}%` }} />
                  </div>
                  <p className="text-xs text-gray-500">{item.attempts} attempt{item.attempts > 1 ? 's' : ''}</p>
                </div>
              ))}
            </div>
          </section>

          <section className="rounded-3xl border border-gray-800 bg-[#151b24] p-6">
            <div className="flex items-center justify-between">
              <div>
                <h2 className="text-sm font-semibold uppercase tracking-[0.2em] text-gray-400">All Submissions</h2>
                <p className="mt-1 text-xs text-gray-500">Tap a row to view details.</p>
              </div>
              <select
                value={filterLab}
                onChange={(event) => setFilterLab(event.target.value)}
                className="rounded-2xl border border-gray-700 bg-[#12181f] px-3 py-2 text-xs text-gray-200 outline-none"
              >
                {LAB_OPTIONS.map((labName) => (
                  <option key={labName} value={labName}>{labName}</option>
                ))}
              </select>
            </div>

            <div className="mt-5 overflow-hidden rounded-3xl border border-gray-800 bg-[#0f151b]">
              <table className="w-full border-collapse text-left text-sm">
                <thead className="bg-[#12181f] text-gray-500">
                  <tr>
                    <th className="px-4 py-3">Lab</th>
                    <th className="px-4 py-3">Attempt</th>
                    <th className="px-4 py-3">Score</th>
                    <th className="px-4 py-3">Submitted</th>
                    <th className="px-4 py-3">Status</th>
                    <th className="px-4 py-3" />
                  </tr>
                </thead>
                <tbody>
                  {filteredHistory.map((item, index) => (
                    <>
                      <tr
                        key={item.id}
                        className={`cursor-pointer border-b border-gray-800 transition hover:bg-[#11171f] ${index % 2 === 0 ? 'bg-[#0f151b]' : 'bg-[#12181f]'}`}
                        onClick={() => setExpandedRow(expandedRow === item.id ? null : item.id)}
                      >
                        <td className="px-4 py-4 text-gray-200">{item.labName}</td>
                        <td className="px-4 py-4 text-gray-400">#{item.attempt}</td>
                        <td className="px-4 py-4">
                          <span className={`inline-flex rounded-full border px-3 py-1 text-xs font-semibold ${scoreBadge(item.score)} ${scoreColor(item.score)}`}>
                            {item.score}
                          </span>
                        </td>
                        <td className="px-4 py-4 text-gray-400">{item.submittedAt}</td>
                        <td className={`px-4 py-4 text-xs font-semibold uppercase ${item.failedTests.length === 0 ? 'text-emerald-400' : 'text-red-400'}`}>
                          {item.failedTests.length === 0 ? 'Pass' : 'Partial'}
                        </td>
                        <td className="px-4 py-4 text-gray-400">
                          {expandedRow === item.id ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
                        </td>
                      </tr>
                      {expandedRow === item.id && (
                        <tr className="bg-[#10151b]">
                          <td colSpan={6} className="px-4 py-4">
                            <div className="grid gap-4 md:grid-cols-2">
                              <div>
                                <p className="text-xs uppercase tracking-[0.2em] text-gray-500">Failed Classes</p>
                                <p className="mt-2 text-sm text-green-300">{item.failedClasses.length ? item.failedClasses.join(', ') : 'None'}</p>
                              </div>
                              <div>
                                <p className="text-xs uppercase tracking-[0.2em] text-gray-500">Failed Testcases</p>
                                <p className="mt-2 text-sm text-purple-300">{item.failedTests.length ? item.failedTests.join(', ') : 'None'}</p>
                              </div>
                            </div>
                          </td>
                        </tr>
                      )}
                    </>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        </div>
      </div>

      <footer className="border-t border-gray-800 px-6 py-4 text-center text-xs text-gray-500">
        Made by Pham Quan Kha &amp; Doan Tuan Kiet
      </footer>
      {showProfile && <ProfileEditModal isOpen={showProfile} onClose={() => setShowProfile(false)} />}
    </div>
  );
}
