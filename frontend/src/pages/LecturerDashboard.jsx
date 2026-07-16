import { useState, useEffect, useRef } from "react";
import {
  ArrowLeft,
  ChevronDown,
  ChevronUp,
  Folder,
  FileText,
  Download,
  Upload,
  FileSpreadsheet,
  FileImage,
  Trash2,
  Eye,
  X,
} from "lucide-react";
import React from "react";
import { useTheme } from "../context/ThemeContext";
import Header from "../components/Header";
import Footer from "../components/Footer";
import NavBar from "../components/NavBar";
import UserManagement from "./UserManagement";
import Select from "../components/ui/Select";

// ─── Data ────────────────────────────────────────────────────────────────────

const ROOT_FOLDERS = [
  { name: "Lab01_2190001_John_Doe", type: "folder" },
  { name: "Lab01_2190002_Jane_Smith", type: "folder" },
  { name: "Lab01_2190003_Mike_Johnson", type: "folder" },
  { name: "Lab01_2190004_Sarah_Williams", type: "folder" },
];

const FILES_IN_FOLDER = [
  { name: "Main.java", type: "file" },
  { name: "Helper.java", type: "file" },
  { name: "diagram.mmd", type: "file" },
  { name: "Utils.java", type: "file" },
];

const SUBMISSIONS = [
  {
    student: "John Doe",
    id: "2190001",
    score: 92,
    failedClasses: ["Car"],
    failedTests: ["Testcase 1", "Testcase 3"],
  },
  {
    student: "Jane Smith",
    id: "2190002",
    score: 88,
    failedClasses: ["Book", "Library"],
    failedTests: ["Testcase 2", "Testcase 4", "Testcase 5"],
  },
  {
    student: "Mike Johnson",
    id: "2190003",
    score: 95,
    failedClasses: [],
    failedTests: ["Testcase 1"],
  },
  {
    student: "Sarah Williams",
    id: "2190004",
    score: 76,
    failedClasses: ["Car", "Book", "Person"],
    failedTests: [
      "Testcase 1",
      "Testcase 2",
      "Testcase 3",
      "Testcase 5",
      "Testcase 6",
    ],
  },
];

const EXPORT_OPTIONS = [
  { label: "Excel", icon: <FileSpreadsheet className="w-4 h-4" /> },
  { label: "PDF", icon: <FileText className="w-4 h-4" /> },
  { label: "SVG", icon: <FileImage className="w-4 h-4" /> },
];

const labOptions = [
  "Lab 01: Abstraction",
  "Lab 02: Polymorphism",
  "Lab 03: Inheritance",
  "Lab 04: Interface",
];

// ─── Helpers ─────────────────────────────────────────────────────────────────

function computeSummary(submissions) {
  const avg = Math.round(
    submissions.reduce((a, s) => a + s.score, 0) / submissions.length,
  );
  const low = Math.min(...submissions.map((s) => s.score));

  const top = (arr) => {
    const freq = {};
    arr.forEach((v) => (freq[v] = (freq[v] || 0) + 1));
    return Object.entries(freq).sort((a, b) => b[1] - a[1])[0]?.[0] ?? "None";
  };

  return {
    averageScore: avg,
    lowestScore: low,
    mostFailedClass: top(submissions.flatMap((s) => s.failedClasses)),
    mostFailedTest: top(submissions.flatMap((s) => s.failedTests)),
  };
}

// ─── Main component ───────────────────────────────────────────────────────────

export default function LecturerDashBoard({ user, onLogout }) {

  const { isDark, toggleTheme } = useTheme();
  const [activeNav, setActiveNav] = useState('dashboard');

  // ── File Explorer state ──
  const [isCollapsed, setIsCollapsed] = useState(false);
  const [currentPath, setCurrentPath] = useState([]);
  const [isDeleteMode, setIsDeleteMode] = useState(false);
  const [selectedFolders, setSelectedFolders] = useState([]);

  // ── Grading Overview state ──
  const [showAllStudents, setShowAllStudents] = useState(false);
  const [showExportMenu, setShowExportMenu] = useState(false);
  const exportMenuRef = useRef(null);

  useEffect(() => {
    if (!showExportMenu) return;
    const onOutside = (e) => {
      if (exportMenuRef.current && !exportMenuRef.current.contains(e.target))
        setShowExportMenu(false);
    };
    document.addEventListener("mousedown", onOutside);
    return () => document.removeEventListener("mousedown", onOutside);
  }, [showExportMenu]);

  // ── File Explorer helpers ──
  const currentItems =
    currentPath.length === 0 ? ROOT_FOLDERS : FILES_IN_FOLDER;

  const handleQuickDelete = () => {
    if (isDeleteMode && selectedFolders.length > 0) {
      setSelectedFolders([]);
      setIsDeleteMode(false);
    } else {
      setIsDeleteMode((v) => !v);
      setSelectedFolders([]);
    }
  };

  const toggleFolder = (name) =>
    setSelectedFolders((prev) =>
      prev.includes(name) ? prev.filter((f) => f !== name) : [...prev, name],
    );

  // ── Summary ──
  const { averageScore, lowestScore, mostFailedClass, mostFailedTest } =
    computeSummary(SUBMISSIONS);
  const visibleSubmissions = showAllStudents
    ? SUBMISSIONS
    : SUBMISSIONS.slice(0, 4);

  return (
    <div className={isDark ? 'dark' : ''}>
      <div className="min-h-screen bg-[#F5F5F7] dark:bg-[#0A0A0F] p-6 transition-colors">
        <NavBar
          active={activeNav}
          onNavigate={setActiveNav}
          isDark={isDark}
          onToggleTheme={toggleTheme}
        />

        {activeNav === 'dashboard' ? (
          <>
            <div className="max-w-[1400px] mx-auto">
              <Header user={user} onLogout={onLogout} />
            </div>

            {/* ── Bulk Upload ── */}
            <div className="mb-6">
              <div className="bg-white dark:bg-[#1e2530] rounded-xl p-6 shadow-sm dark:shadow-none">
                <div className="border-2 border-dashed border-blue-500/30 rounded-lg p-8">
                  <div className="flex flex-col items-center justify-center">
                    <div className="w-16 h-16 bg-blue-500/10 rounded-full flex items-center justify-center mb-4">
                      <Upload className="w-8 h-8 text-blue-500" />
                    </div>
                    <h3 className="text-gray-900 dark:text-white mb-2">
                      Drop .zip or .rar files here
                    </h3>
                    <p className="text-gray-500 dark:text-gray-400 text-sm mb-4">
                      Upload compressed student submissions
                    </p>
                    <button className="px-6 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition-colors">
                      + Select files
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-1 gap-6 mb-6">
              <Select label="Select Lab" options={labOptions} />
            </div>

            {/* ── File Explorer ── */}
            <div className="mb-6">
              <div className="bg-white dark:bg-[#1e2530] rounded-xl shadow-sm dark:shadow-none overflow-hidden">
                {/* Top bar */}
                <div className="flex items-center justify-between p-4 border-b border-gray-200 dark:border-gray-700">
                  <div className="flex items-center gap-3">
                    {currentPath.length > 0 && (
                      <button
                        onClick={() => setCurrentPath([])}
                        className="p-1.5 hover:bg-gray-100 dark:hover:bg-[#1A1A24] rounded-lg transition-colors"
                      >
                        <ArrowLeft className="w-5 h-5 text-gray-600 dark:text-gray-400" />
                      </button>
                    )}
                    <h2 className="text-gray-900 dark:text-white">
                      {currentPath.length === 0
                        ? "Student Submissions"
                        : currentPath[0]}
                    </h2>
                    <span className="text-sm text-gray-500 dark:text-gray-400">
                      ({currentItems.length}{" "}
                      {currentItems.length === 1 ? "item" : "items"})
                    </span>
                  </div>

                  <div className="flex items-center gap-2">
                    <button
                      onClick={handleQuickDelete}
                      className={`px-3 py-1.5 ${isDeleteMode ? "bg-red-700" : "bg-red-600"} hover:bg-red-700 text-white rounded-lg text-sm transition-colors flex items-center gap-2`}
                    >
                      <Trash2 className="w-4 h-4" />
                      {isDeleteMode
                        ? selectedFolders.length > 0
                          ? `Delete (${selectedFolders.length})`
                          : "Cancel"
                        : "Quick Delete"}
                    </button>
                    <button
                      onClick={() => setIsCollapsed((v) => !v)}
                      className="p-1.5 hover:bg-gray-100 dark:hover:bg-[#1A1A24] rounded-lg transition-colors"
                    >
                      {isCollapsed ? (
                        <ChevronDown className="w-5 h-5 text-gray-600 dark:text-gray-400" />
                      ) : (
                        <ChevronUp className="w-5 h-5 text-gray-600 dark:text-gray-400" />
                      )}
                    </button>
                  </div>
                </div>

          {/* Delete hint */}
          {isDeleteMode && !isCollapsed && (
            <div className="px-4 py-2 bg-red-50 dark:bg-red-900/20 border-b border-red-200 dark:border-red-800">
              <p className="text-red-800 dark:text-red-200 text-sm">
                Click on folders to select them for deletion
              </p>
            </div>
          )}

          {/* File grid */}
          {!isCollapsed && (
            <div className="p-4">
              <div className="grid grid-cols-2 gap-3">
                {currentItems.map((item, i) => {
                  const isSelected = selectedFolders.includes(item.name);
                  return (
                    <div
                      key={i}
                      className={`flex items-center gap-3 p-3 rounded-lg border transition-colors group
                        ${isSelected ? "border-red-500 bg-red-50 dark:bg-red-900/20" : "border-gray-200 dark:border-gray-700"}
                        ${item.type === "folder" ? "hover:bg-gray-50 dark:hover:bg-[#1A1A24]" : "bg-gray-50 dark:bg-[#151b24]"}`}
                    >
                      <div
                        className="flex items-center gap-3 flex-1 cursor-pointer"
                        onClick={() => {
                          if (item.type !== "folder") return;
                          if (isDeleteMode && currentPath.length === 0)
                            toggleFolder(item.name);
                          else if (!isDeleteMode) setCurrentPath([item.name]);
                        }}
                      >
                        {item.type === "folder" ? (
                          <Folder
                            className={`w-5 h-5 ${isSelected ? "text-red-500" : "text-yellow-500"}`}
                          />
                        ) : (
                          <FileText className="w-5 h-5 text-gray-500 dark:text-gray-400" />
                        )}
                        <span
                          className={`text-sm flex-1 ${isSelected ? "text-red-700 dark:text-red-300 font-medium" : "text-gray-900 dark:text-white"}`}
                        >
                          {item.name}
                        </span>
                      </div>

                      {isSelected ? (
                        <div className="w-5 h-5 bg-red-500 rounded-full flex items-center justify-center">
                          <svg
                            className="w-3 h-3"
                            viewBox="0 0 12 12"
                            fill="none"
                          >
                            <path
                              d="M10 3L4.5 8.5L2 6"
                              stroke="white"
                              strokeWidth="2"
                              strokeLinecap="round"
                              strokeLinejoin="round"
                            />
                          </svg>
                        </div>
                      ) : (
                        item.type === "folder" &&
                        currentPath.length === 0 &&
                        !isDeleteMode && (
                          <button
                            onClick={(e) => e.stopPropagation()}
                            className="opacity-0 group-hover:opacity-100 p-1 hover:bg-red-100 dark:hover:bg-red-900/30 rounded transition-opacity"
                          >
                            <X className="w-4 h-4 text-red-600 dark:text-red-400" />
                          </button>
                        )
                      )}
                    </div>
                  );
                })}
              </div>
            </div>
          )}
        </div>
      </div>

      {/* ── Solution Upload & Grade ── */}
      <div className="mb-6">
        <div className="bg-white dark:bg-[#1e2530] rounded-xl p-4 shadow-sm dark:shadow-none">
          <div className="flex items-center justify-between gap-4">
            <div className="flex-1 border-2 border-dashed border-purple-500/30 rounded-lg p-4">
              <div className="flex items-center gap-3">
                <Folder className="w-8 h-8 text-purple-500" />
                <div className="flex-1">
                  <p className="text-gray-900 dark:text-white text-sm font-medium">
                    Solution Folder (UML & Java)
                  </p>
                  <p className="text-gray-500 dark:text-gray-400 text-xs">
                    Drop solution folder here or browse
                  </p>
                </div>
                <button className="px-4 py-2 bg-purple-600 hover:bg-purple-700 text-white rounded-lg text-sm transition-colors">
                  Browse
                </button>
              </div>
            </div>
            <button className="px-6 py-3 bg-purple-600 hover:bg-purple-700 text-white rounded-lg font-medium transition-colors">
              Grade
            </button>
          </div>
        </div>
      </div>

      {/* ── Grading Overview ── */}
      <div className="bg-white dark:bg-[#1e2530] rounded-xl p-6 shadow-sm dark:shadow-none mb-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-gray-900 dark:text-white">Grading Overview</h2>
          <div className="flex items-center gap-2">
            <button
              onClick={() => setShowAllStudents((v) => !v)}
              className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-sm transition-colors"
            >
              {showAllStudents ? "Show Less" : "View More"}
            </button>

            <div className="relative" ref={exportMenuRef}>
              <button
                onClick={() => setShowExportMenu((v) => !v)}
                className="px-4 py-2 bg-green-600 hover:bg-green-700 text-white rounded-lg text-sm transition-colors flex items-center gap-2"
              >
                <Download className="w-4 h-4" />
                Export
              </button>
              {showExportMenu && (
                <div className="absolute right-0 mt-2 w-40 bg-white dark:bg-[#151b24] rounded-lg border border-gray-200 dark:border-gray-700 shadow-lg overflow-hidden z-10">
                  {EXPORT_OPTIONS.map(({ label, icon }) => (
                    <button
                      key={label}
                      onClick={() => setShowExportMenu(false)}
                      className="w-full px-4 py-2 text-left hover:bg-gray-50 dark:hover:bg-[#222230] text-gray-900 dark:text-gray-300 text-sm flex items-center gap-2"
                    >
                      {icon}
                      {label}
                    </button>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-gray-200 dark:border-gray-700">
                {[
                  "Student",
                  "ID",
                  "Score",
                  "Failed Classes",
                  "Failed Testcases",
                  "Action",
                ].map((col) => (
                  <th
                    key={col}
                    className="text-left py-3 px-4 text-gray-700 dark:text-gray-300 font-medium"
                  >
                    {col}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {visibleSubmissions.map((s, i) => (
                <tr
                  key={i}
                  className="border-b border-gray-200 dark:border-gray-700"
                >
                  <td className="py-3 px-4 text-gray-800 dark:text-gray-200 text-xs">
                    {s.student}
                  </td>
                  <td className="py-3 px-4 text-gray-800 dark:text-gray-200 text-xs">
                    {s.id}
                  </td>
                  <td className="py-3 px-4 text-gray-900 dark:text-white font-medium text-xs">
                    {s.score}
                  </td>
                  <td className="py-3 px-4 text-green-600 dark:text-green-500 font-medium text-xs">
                    {s.failedClasses.length > 0
                      ? s.failedClasses.join(", ")
                      : "None"}
                  </td>
                  <td className="py-3 px-4 text-purple-600 dark:text-purple-500 font-medium text-xs">
                    {s.failedTests.length > 0
                      ? s.failedTests.join(", ")
                      : "None"}
                  </td>
                  <td className="py-3 px-4">
                    <button className="px-3 py-1.5 bg-green-600 hover:bg-green-700 text-white rounded-lg text-xs transition-colors flex items-center gap-1">
                      <Eye className="w-3 h-3" />
                      View Details
                    </button>
                  </td>
                </tr>
              ))}

              {/* Summary row */}
              <tr className="bg-blue-50 dark:bg-blue-900/20 border-t-2 border-blue-400 dark:border-blue-600">
                <td className="py-4 px-4 text-blue-900 dark:text-blue-100 font-bold text-sm">
                  SUMMARY
                </td>
                <td className="py-4 px-4 text-blue-800 dark:text-blue-200 font-semibold text-sm">
                  Average:{" "}
                  <span className="text-blue-900 dark:text-blue-100">
                    {averageScore}
                  </span>
                </td>
                <td className="py-4 px-4 text-blue-800 dark:text-blue-200 font-semibold text-sm">
                  Lowest:{" "}
                  <span className="text-blue-900 dark:text-blue-100">
                    {lowestScore}
                  </span>
                </td>
                <td className="py-4 px-4 text-green-700 dark:text-green-400 font-bold text-sm">
                  Most failed: {mostFailedClass}
                </td>
                <td className="py-4 px-4 text-purple-700 dark:text-purple-400 font-bold text-sm">
                  Most failed: {mostFailedTest}
                </td>
                <td className="py-4 px-4" />
              </tr>
            </tbody>
          </table>
        </div>
      </div>
        </>
      ) : activeNav === 'users' ? (
        <UserManagement hideNav />
      ) : activeNav === 'projects' ? (
        <div className="max-w-[1400px] mx-auto py-10 text-center text-gray-700 dark:text-gray-300">
          <h2 className="text-xl font-semibold mb-3">Projects</h2>
          <p>Project tracking is coming soon.</p>
        </div>
      ) : activeNav === 'reports' ? (
        <div className="max-w-[1400px] mx-auto py-10 text-center text-gray-700 dark:text-gray-300">
          <h2 className="text-xl font-semibold mb-3">Reports</h2>
          <p>Report generation is coming soon.</p>
        </div>
      ) : (
        <div className="max-w-[1400px] mx-auto py-10 text-center text-gray-700 dark:text-gray-300">
          <h2 className="text-xl font-semibold mb-3">Page not found</h2>
        </div>
      )}
      <Footer />
    </div>
  </div>
  );
}
