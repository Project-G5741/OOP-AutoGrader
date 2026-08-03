// StudentDashboard.jsx
import React, { useEffect, useState } from 'react';
import AppShell from '../components/layout/AppShell';
import StudentHistoryPage from './StudentHistory';
import ProfileEditModal from '../components/student/ProfileEditModal';
import StudentUI from '../components/student/StudentUI';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';

export default function StudentDashboard({ user, onLogout }) {
  const [showHistory, setShowHistory] = useState(false);
  const [showProfile, setShowProfile] = useState(false);

  // Labs
  const [labs, setLabs] = useState([]);
  const [selectedLabId, setSelectedLabId] = useState(null);
  const [labsError, setLabsError] = useState(null);

  // Challenges
  const [challenges, setChallenges] = useState([]);
  const [selectedChallengeId, setSelectedChallengeId] = useState(null);
  const [challengesError, setChallengesError] = useState(null);

  // Dữ liệu chi tiết
  const [mmdData, setMmdData] = useState([]);
  const [classData, setClassData] = useState([]);
  const [testCases, setTestCases] = useState([]);

  // Stats - null means "no data yet", not zero
  const [stats, setStats] = useState({
    currentGrade: null,
    totalSubmissions: null,
    latestSubmission: null,
  });

  // Loading states
  const [isLoadingLabs, setIsLoadingLabs] = useState(false);
  const [isLoadingChallenges, setIsLoadingChallenges] = useState(false);
  const [isLoadingDetails, setIsLoadingDetails] = useState(false);

  // 1. Fetch labs
  useEffect(() => {
    async function fetchLabs() {
      setIsLoadingLabs(true);
      try {
        const res = await fetch(`${API_BASE}/api/labs`);
        if (!res.ok) throw new Error(`Failed to load labs (status ${res.status})`);
        const data = await res.json();
        setLabs(data);
        if (data.length > 0) {
          setSelectedLabId(data[0].id);
        }
      } catch (err) {
        console.info('Failed to fetch labs:', err.message);
        setLabsError('Could not load labs. The backend may be offline.');
      } finally {
        setIsLoadingLabs(false);
      }
    }
    fetchLabs();
  }, []);

  // 2. Fetch challenges khi lab thay đổi
  useEffect(() => {
    if (!selectedLabId) return;

    async function fetchChallenges() {
      setIsLoadingChallenges(true);
      setChallengesError(null);
      try {
        const res = await fetch(`${API_BASE}/api/labs/${selectedLabId}/challenges`);
        if (!res.ok) throw new Error(`Failed to load challenges (status ${res.status})`);
        const data = await res.json();
        setChallenges(data);
        if (data.length > 0) {
          setSelectedChallengeId(data[0].id);
        } else {
          setSelectedChallengeId(null);
        }
      } catch (err) {
        console.error('Failed to fetch challenges:', err);
        setChallengesError('Could not load challenges.');
        setChallenges([]);
        setSelectedChallengeId(null);
      } finally {
        setIsLoadingChallenges(false);
      }
    }
    fetchChallenges();
  }, [selectedLabId]);

  // 3. Fetch chi tiết (MMD, Class, Testcases, Stats) khi challenge thay đổi
  //
  // Gating rule: the challenges list (fetched above) already tells us, per
  // challenge, whether the student has a submission for it (`ch.score` is
  // null/undefined when there's none — this is the front-end equivalent of
  // the backend's `challenge_result["challenge_N"]` being an empty array).
  // When a challenge has no submission, there is nothing to grade-detail,
  // so we skip the mmd/class/testcase calls entirely instead of hitting the
  // backend for empty results.
  useEffect(() => {
    if (!selectedChallengeId || !selectedLabId) return;

    async function fetchDetails() {
      setIsLoadingDetails(true);
      try {
        const currentChallenge = challenges.find((c) => c.id === selectedChallengeId);
        const hasSubmissionData =
          currentChallenge?.score !== null && currentChallenge?.score !== undefined;

        if (hasSubmissionData) {
          // Fetch MMD data
          const mmdRes = await fetch(
            `${API_BASE}/api/labs/${selectedLabId}/challenges/${selectedChallengeId}/mmd`
          );
          setMmdData(mmdRes.ok ? await mmdRes.json() : []);

          // Fetch Class data
          const classRes = await fetch(
            `${API_BASE}/api/labs/${selectedLabId}/challenges/${selectedChallengeId}/class`
          );
          setClassData(classRes.ok ? await classRes.json() : []);

          // Fetch Testcases
          const testRes = await fetch(
            `${API_BASE}/api/labs/${selectedLabId}/challenges/${selectedChallengeId}/testcases`
          );
          setTestCases(testRes.ok ? await testRes.json() : []);
        } else {
          // No submission for this challenge yet — nothing to fetch, tabs render empty.
          setMmdData([]);
          setClassData([]);
          setTestCases([]);
        }

        // Stats are always fetched; the backend returns null fields when
        // there's no data and the UI falls back to "--/--".
        const statsRes = await fetch(
          `${API_BASE}/api/labs/${selectedLabId}/challenges/${selectedChallengeId}/stats?studentId=${user?.id}`
        );
        if (statsRes.ok) {
          setStats(await statsRes.json());
        } else {
          setStats({ currentGrade: null, totalSubmissions: null, latestSubmission: null });
        }
      } catch (err) {
        console.error('Failed to fetch details:', err);
        setStats({ currentGrade: null, totalSubmissions: null, latestSubmission: null });
      } finally {
        setIsLoadingDetails(false);
      }
    }
    fetchDetails();
  }, [selectedLabId, selectedChallengeId, user?.id, challenges]);

  const handleLabChange = (labId) => {
    setSelectedLabId(labId);
    // Reset challenge selection
    setSelectedChallengeId(null);
  };

  const handleChallengeChange = (challengeId) => {
    setSelectedChallengeId(challengeId);
  };

  const handleFileUpload = async (files, labId, challengeId) => {
    // TODO: Implement file upload to backend
    console.log('Uploading files:', files, 'to lab:', labId, 'challenge:', challengeId);
  };

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
        />
      </AppShell>
    );
  }

  const isLoading = isLoadingLabs || isLoadingChallenges;

  return (
    <>
      <AppShell user={user} onLogout={onLogout} onCommand={handleCommand}>
        <div className="w-full">
          {/* Hiển thị lỗi labs nếu có */}
          {labsError && (
            <div className="mb-4 rounded-md border border-yellow-200 bg-yellow-50 p-3 text-sm text-yellow-800 dark:border-yellow-600 dark:bg-yellow-900/30 dark:text-yellow-200">
              {labsError}
            </div>
          )}

          <StudentUI
            user={user}
            // Labs
            labs={labs}
            selectedLabId={selectedLabId}
            onLabChange={handleLabChange}
            // Challenges
            challenges={challenges}
            selectedChallengeId={selectedChallengeId}
            onChallengeChange={handleChallengeChange}
            // Details
            mmdData={mmdData}
            classData={classData}
            testCases={testCases}
            // Stats
            stats={stats}
            // Upload
            onFileUpload={handleFileUpload}
            // States
            isLoading={isLoading}
            isLoadingDetails={isLoadingDetails}
            error={labsError || challengesError}
          />
        </div>
      </AppShell>

      {showProfile && (
        <ProfileEditModal
          isOpen={showProfile}
          onClose={() => setShowProfile(false)}
          user={user}
        />
      )}
    </>
  );
}