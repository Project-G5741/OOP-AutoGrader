import React, { useState } from 'react';
import AppShell from '../components/layout/AppShell';
import Select from '../components/ui/Select';
import DropZone from '../components/ui/DropZone';
import ResultList from '../components/ResultList';
import Card from '../components/ui/Card';
import StudentHistoryPage from './StudentHistory';
import ProfileEditModal from '../components/student/ProfileEditModal';

export default function StudentDashboard({ user, onLogout }) {
  const [showHistory, setShowHistory] = useState(false);
  const [showProfile, setShowProfile] = useState(false);
  const labOptions = ['Lab 01: Abstraction', 'Lab 02: Polymorphism', 'Lab 03: Inheritance', 'Lab 04: Interface'];
  const problemOptions = ['Problem 01', 'Problem 02', 'Problem 03'];

  const classResults = [
    { name: 'Main', status: 'success' },
    { name: 'Helper', status: 'success' },
    { name: 'Utils', status: 'error' },
    { name: 'Config', status: 'success' },
  ];

  const testResults = [
    { name: 'Test 1: Basic Input', status: 'success' },
    { name: 'Test 2: Edge Cases', status: 'success' },
    { name: 'Test 3: Null Values', status: 'error' },
    { name: 'Test 4: Large Input', status: 'error' },
  ];

  // if (showHistory) {
  //   return(
  //   <StudentHistoryPage 
  //   user = {user}
  //   onLogout = {onLogout}
  //   onEditProfile={() => setShowHistory(false)} 
  //   onNavigate={() => setShowHistory(false)}
  //   />
  //   );
  // }

  const handleCommand = (cmd) => {
    if (cmd === 'home') {
      setShowHistory(false);
    } else if (cmd === 'history') {
      setShowHistory(true);
    } else if (cmd === 'editProfile') {
      setShowProfile(true);
    }
  };

  if (showHistory) {
    return (
      <AppShell user={user} onLogout={onLogout} onCommand={handleCommand}>
        <StudentHistoryPage 
          user={user}
          onLogout={onLogout}
          onEditProfile={() => setShowHistory(false)}
          // Không cần onNavigate nữa
        />
      </AppShell>
    );
  }

  return (
    <>
    <AppShell user={user} onLogout={onLogout} onCommand={handleCommand}>
      <div className="space-y-6">
        <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
          <Select label="Select Lab" options={labOptions} />
          <Select label="Select Problem" options={problemOptions} />
        </div>

        <div>
          <DropZone />
        </div>

        <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
          <ResultList title="Class Results" actionText="View All" items={classResults} />
          <ResultList title="Test Case Results" actionText="View Details" items={testResults} />
        </div>

        <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
          <Card className="flex flex-col justify-between">
            <div className="mb-3 flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-blue-500/20 text-blue-500">
                <svg className="h-6 w-6" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M19 3h-4.18C14.4 1.84 13.3 1 12 1c-1.3 0-2.4.84-2.82 2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-7 0c.55 0 1 .45 1 1s-.45 1-1 1-1-.45-1-1 .45-1 1-1zm2 14H7v-2h7v2zm3-4H7v-2h10v2zm0-4H7V7h10v2z" />
                </svg>
              </div>
              <span className="text-sm text-gray-500 dark:text-gray-400">Total Submissions</span>
            </div>
            <span className="text-3xl font-semibold text-gray-900 dark:text-white">12</span>
          </Card>

          <Card className="flex flex-col justify-between">
            <div className="mb-3 flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-purple-500/20 text-purple-500">
                <svg className="h-6 w-6" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z" />
                </svg>
              </div>
              <span className="text-sm text-gray-500 dark:text-gray-400">Latest submission</span>
            </div>
            <span className="text-sm text-gray-900 dark:text-white">2 days ago</span>
          </Card>

          <Card className="flex items-center justify-center">
            <div className="relative h-24 w-24">
              <svg className="h-24 w-24 -rotate-90">
                <circle cx="48" cy="48" r="40" stroke="currentColor" className="text-gray-200 dark:text-gray-800" strokeWidth="8" fill="none" />
                <circle cx="48" cy="48" r="40" stroke="#10B981" strokeWidth="8" fill="none" strokeDasharray="251.2" strokeDashoffset="62.8" strokeLinecap="round" />
              </svg>
              <div className="absolute inset-0 flex items-center justify-center">
                <span className="font-medium text-gray-900 dark:text-white">75%</span>
              </div>
            </div>
          </Card>

          <div className="flex flex-col justify-between rounded-xl bg-gradient-to-br from-green-600 to-green-700 p-6 shadow-lg">
            <div className="mb-3 flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-white/20 text-white">
                <svg className="h-6 w-6" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z" />
                </svg>
              </div>
              <span className="text-sm text-white/90">Current Grade</span>
            </div>
            <div className="flex items-baseline gap-1">
              <span className="text-4xl font-bold text-white">92</span>
              <span className="text-white/70">/100</span>
            </div>
          </div>
        </div>
      </div>
    </AppShell>
      {showProfile && <ProfileEditModal isOpen={showProfile} onClose={() => setShowProfile(false)} />}
    </>
  );
}