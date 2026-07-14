import React from 'react';
import Header from '../components/Header';
import Footer from '../components/Footer';
import Select from '../components/ui/Select';
import DropZone from '../components/ui/DropZone';
import ResultList from '../components/ResultList';
import Card from '../components/ui/Card';

export default function StudentDashboard({ user, onLogout }) {
  const labOptions = ["Lab 01: Abstraction", "Lab 02: Polymophism", "Lab 03: Inheritance", "Lab 04: Interface"];
  const problemOptions = ["Problem 01", "Problem 02", "Problem 03"];

  const classResults = [
    { name: "Main", status: "success" },
    { name: "Helper", status: "success" },
    { name: "Utils", status: "error" },
    { name: "Config", status: "success" },
  ];

  const testResults = [
    { name: "Test 1: Basic Input", status: "success" },
    { name: "Test 2: Edge Cases", status: "success" },
    { name: "Test 3: Null Values", status: "error" },
    { name: "Test 4: Large Input", status: "error" },
  ];

  return (
    <div className="min-h-screen bg-[#F5F5F7] dark:bg-[#0A0A0F] p-6 transition-colors">
      <div className="max-w-[1400px] mx-auto">
        <Header user={user} onLogout={onLogout} />

        {/* Dropdowns */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
          <Select label="Select Lab" options={labOptions} />
          <Select label="Select Problem" options={problemOptions} />
        </div>

        {/* Upload Areas */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
          <DropZone fileType=".mmd" colorTheme="purple" />
          <DropZone fileType=".Java" colorTheme="green" />
        </div>

        {/* Lists Area */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
          <ResultList title="Class Results" actionText="View All" items={classResults} />
          <ResultList title="Test Case Results" actionText="View Details" items={testResults} />
        </div>

        {/* Metric Cards Area */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 mb-6">
          <Card className="flex flex-col justify-between">
            <div className="flex items-center gap-3 mb-3">
              <div className="w-10 h-10 bg-blue-500/20 rounded-lg flex items-center justify-center text-blue-500">
                <svg className="w-6 h-6" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M19 3h-4.18C14.4 1.84 13.3 1 12 1c-1.3 0-2.4.84-2.82 2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-7 0c.55 0 1 .45 1 1s-.45 1-1 1-1-.45-1-1 .45-1 1-1zm2 14H7v-2h7v2zm3-4H7v-2h10v2zm0-4H7V7h10v2z" />
                </svg>
              </div>
              <span className="text-gray-500 dark:text-gray-400 text-sm">Total Submissions</span>
            </div>
            <span className="text-gray-900 dark:text-white text-3xl font-semibold">12</span>
          </Card>

          <Card className="flex flex-col justify-between">
            <div className="flex items-center gap-3 mb-3">
              <div className="w-10 h-10 bg-purple-500/20 rounded-lg flex items-center justify-center text-purple-500">
                <svg className="w-6 h-6" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z" />
                </svg>
              </div>
              <span className="text-gray-500 dark:text-gray-400 text-sm">Latest submission</span>
            </div>
            <span className="text-gray-900 dark:text-white text-sm">2 days ago</span>
          </Card>

          <Card className="flex items-center justify-center">
            <div className="relative w-24 h-24">
              <svg className="w-24 h-24 transform -rotate-90">
                <circle cx="48" cy="48" r="40" stroke="currentColor" className="text-gray-200 dark:text-gray-800" strokeWidth="8" fill="none" />
                <circle cx="48" cy="48" r="40" stroke="#10B981" strokeWidth="8" fill="none" strokeDasharray="251.2" strokeDashoffset="62.8" strokeLinecap="round" />
              </svg>
              <div className="absolute inset-0 flex items-center justify-center">
                <span className="text-gray-900 dark:text-white font-medium">75%</span>
              </div>
            </div>
          </Card>

          <div className="bg-gradient-to-br from-green-600 to-green-700 rounded-xl p-6 shadow-lg flex flex-col justify-between">
            <div className="flex items-center gap-3 mb-3">
              <div className="w-10 h-10 bg-white/20 rounded-lg flex items-center justify-center text-white">
                <svg className="w-6 h-6" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z" />
                </svg>
              </div>
              <span className="text-white/90 text-sm">Current Grade</span>
            </div>
            <div className="flex items-baseline gap-1">
              <span className="text-4xl text-white font-bold">92</span>
              <span className="text-white/70">/100</span>
            </div>
          </div>
        </div>
        <Footer />
      </div>
    </div>
  );
}