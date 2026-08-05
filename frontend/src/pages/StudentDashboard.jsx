// StudentDashboard.jsx
import React, { useCallback, useEffect, useRef, useState } from 'react';
import AppShell from '../components/layout/AppShell';
import StudentHistoryPage from './StudentHistory';
import ChangePasswordModal from '../components/student/ChangePasswordModal';
import StudentUI from '../components/student/StudentUI';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';

export default function StudentDashboard({ user, onLogout }) {
  const [showHistory, setShowHistory] = useState(false);
  const [showChangePassword, setShowChangePassword] = useState(false);

  const [labs, setLabs] = useState([]);
  const [selectedLabId, setSelectedLabId] = useState(null);
  const [labsError, setLabsError] = useState(null);

  const [challenges, setChallenges] = useState([]);
  const [selectedChallengeId, setSelectedChallengeId] = useState(null);
  const [challengesError, setChallengesError] = useState(null);

  const [mmdData, setMmdData] = useState([]);
  const [classData, setClassData] = useState([]);
  const [testCases, setTestCases] = useState([]);

  const [stats, setStats] = useState({
    currentGrade: null,
    totalSubmissions: null,
    latestSubmission: null,
  });
  const [nextAttemptNumber, setNextAttemptNumber] = useState(1);

  const [isLoadingLabs, setIsLoadingLabs] = useState(false);
  const [isLoadingChallenges, setIsLoadingChallenges] = useState(false);
  const [isLoadingDetails, setIsLoadingDetails] = useState(false);
  const [isRefreshingResults, setIsRefreshingResults] = useState(false);

  const classDataCacheRef = useRef({});

  const studentId = user?.id;

  const fetchChallenges = useCallback(async (labId, { silent = false } = {}) => {
    if (!labId) return;
    if (!silent) {
      setIsLoadingChallenges(true);
    }
    setChallengesError(null);
    try {
      const query = studentId ? `?studentId=${studentId}` : '';
      const res = await fetch(`${API_BASE}/api/labs/${labId}/challenges${query}`);
      if (!res.ok) throw new Error(`Failed to load challenges (status ${res.status})`);
      const data = await res.json();
      setChallenges(data);
      setSelectedChallengeId((prev) => {
        if (prev && data.some((c) => c.id === prev)) return prev;
        return data.length > 0 ? data[0].id : null;
      });
    } catch (err) {
      console.error('Failed to fetch challenges:', err);
      if (!silent) {
        setChallengesError('Could not load challenges.');
        setChallenges([]);
        setSelectedChallengeId(null);
      }
    } finally {
      if (!silent) {
        setIsLoadingChallenges(false);
      }
    }
  }, [studentId]);

  useEffect(() => {
    if (stats.totalSubmissions != null) {
      setNextAttemptNumber((prev) =>
        Math.max(prev, Number(stats.totalSubmissions) + 1)
      );
    }
  }, [stats.totalSubmissions, selectedLabId]);

  const fetchStats = useCallback(async (labId, { uploadSnapshot } = {}) => {
    if (!labId || !studentId) {
      setStats({ currentGrade: null, totalSubmissions: null, latestSubmission: null });
      return;
    }
    try {
      const statsRes = await fetch(
        `${API_BASE}/api/labs/${labId}/stats?studentId=${studentId}`
      );
      if (statsRes.ok) {
        const fresh = await statsRes.json();
        setStats((prev) => ({
          ...fresh,
          totalSubmissions:
            uploadSnapshot?.totalSubmissions ?? fresh.totalSubmissions ?? prev.totalSubmissions,
          latestSubmission:
            uploadSnapshot?.latestSubmission ?? fresh.latestSubmission ?? prev.latestSubmission,
          currentGrade:
            uploadSnapshot?.currentGrade ?? fresh.currentGrade ?? prev.currentGrade,
        }));
      } else if (!uploadSnapshot) {
        setStats({ currentGrade: null, totalSubmissions: null, latestSubmission: null });
      }
    } catch (err) {
      console.error('Failed to fetch stats:', err);
      if (!uploadSnapshot) {
        setStats({ currentGrade: null, totalSubmissions: null, latestSubmission: null });
      }
    }
  }, [studentId]);

  const fetchClassForChallenge = useCallback(async (
    labId,
    challengeId,
    { force = false, score } = {},
  ) => {
    if (!labId || !challengeId || !studentId) {
      setClassData([]);
      return;
    }

    const hasSubmissionData = score !== undefined
      ? score !== null
      : (() => {
          const currentChallenge = challenges.find((c) => c.id === challengeId);
          return currentChallenge?.score !== null && currentChallenge?.score !== undefined;
        })();

    setMmdData([]);
    setTestCases([]);

    if (!hasSubmissionData) {
      setClassData([]);
      return;
    }

    if (!force && classDataCacheRef.current[challengeId]) {
      setClassData(classDataCacheRef.current[challengeId]);
      return;
    }

    try {
      const classRes = await fetch(
        `${API_BASE}/api/labs/${labId}/challenges/${challengeId}/class?studentId=${studentId}`
      );
      const data = classRes.ok ? await classRes.json() : [];
      classDataCacheRef.current[challengeId] = data;
      setClassData(data);
    } catch (err) {
      console.error('Failed to fetch class data:', err);
      setClassData([]);
    }
  }, [studentId, challenges]);

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

  useEffect(() => {
    if (!selectedLabId) return;
    Promise.all([
      fetchChallenges(selectedLabId),
      fetchStats(selectedLabId),
    ]);
  }, [selectedLabId, fetchChallenges, fetchStats]);

  useEffect(() => {
    if (!selectedChallengeId || !selectedLabId) return;

    const cached = classDataCacheRef.current[selectedChallengeId];
    if (cached) {
      setClassData(cached);
      setMmdData([]);
      setTestCases([]);
      return;
    }

    async function fetchDetails() {
      setIsLoadingDetails(true);
      try {
        await fetchClassForChallenge(selectedLabId, selectedChallengeId);
      } catch (err) {
        console.error('Failed to fetch details:', err);
      } finally {
        setIsLoadingDetails(false);
      }
    }
    fetchDetails();
  }, [selectedLabId, selectedChallengeId, fetchClassForChallenge]);

  const handleLabChange = (labId) => {
    setSelectedLabId(labId);
    setSelectedChallengeId(null);
    setNextAttemptNumber(1);
    classDataCacheRef.current = {};
    setMmdData([]);
    setClassData([]);
    setTestCases([]);
  };

  const handleChallengeChange = (challengeId) => {
    setSelectedChallengeId(challengeId);
    const cached = classDataCacheRef.current[challengeId];
    if (cached) {
      setClassData(cached);
      setMmdData([]);
      setTestCases([]);
    }
  };

  const handleUploadComplete = async (uploadResponse) => {
    const resultMap = uploadResponse?.challengeResult ?? {};
    const uploadSnapshot = {
      currentGrade: uploadResponse?.score != null
        ? Math.round(Number(uploadResponse.score))
        : undefined,
      totalSubmissions: uploadResponse?.totalSubmissions ?? undefined,
      latestSubmission: uploadResponse?.latestSubmission ?? undefined,
    };

    for (const challengeId of Object.keys(resultMap)) {
      delete classDataCacheRef.current[challengeId];
    }

    setChallenges((prev) =>
      prev.map((challenge) => {
        const score = resultMap[challenge.id];
        if (score === undefined) return challenge;
        return { ...challenge, score };
      })
    );

    setStats((prev) => ({
      currentGrade: uploadSnapshot.currentGrade ?? prev.currentGrade,
      totalSubmissions: uploadSnapshot.totalSubmissions ?? prev.totalSubmissions,
      latestSubmission: uploadSnapshot.latestSubmission ?? prev.latestSubmission,
    }));

    if (uploadResponse?.attemptNumber != null) {
      setNextAttemptNumber(Number(uploadResponse.attemptNumber) + 1);
    } else if (uploadSnapshot.totalSubmissions != null) {
      setNextAttemptNumber(Number(uploadSnapshot.totalSubmissions) + 1);
    }

    if (!selectedLabId) return;

    setIsRefreshingResults(true);
    try {
      await Promise.all([
        fetchChallenges(selectedLabId, { silent: true }),
        fetchStats(selectedLabId, { uploadSnapshot }),
        selectedChallengeId
          ? fetchClassForChallenge(selectedLabId, selectedChallengeId, {
              force: true,
              score: resultMap[selectedChallengeId],
            })
          : Promise.resolve(),
      ]);
    } finally {
      setIsRefreshingResults(false);
    }
  };

  const handleCommand = (cmd) => {
    if (cmd === 'home') {
      setShowHistory(false);
    } else if (cmd === 'history') {
      setShowHistory(true);
    } else if (cmd === 'changePassword') {
      setShowChangePassword(true);
    }
  };

  const isInitialLoading = isLoadingLabs || (isLoadingChallenges && challenges.length === 0);

  return (
    <>
      <AppShell user={user} onLogout={onLogout} onCommand={handleCommand}>
        <div className="w-full">
          {labsError && (
            <div className="mb-4 rounded-md border border-yellow-200 bg-yellow-50 p-3 text-sm text-yellow-800 dark:border-yellow-600 dark:bg-yellow-900/30 dark:text-yellow-200">
              {labsError}
            </div>
          )}

          {showHistory ? (
            <StudentHistoryPage
              user={user}
              onLogout={onLogout}
              onNavigate={() => setShowHistory(false)}
            />
          ) : (
            <StudentUI
              user={user}
              labs={labs}
              selectedLabId={selectedLabId}
              onLabChange={handleLabChange}
              challenges={challenges}
              selectedChallengeId={selectedChallengeId}
              onChallengeChange={handleChallengeChange}
              mmdData={mmdData}
              classData={classData}
              testCases={testCases}
              stats={stats}
              nextAttemptNumber={nextAttemptNumber}
              onUploadComplete={handleUploadComplete}
              isLoading={isInitialLoading}
              isLoadingDetails={isLoadingDetails}
              isRefreshingResults={isRefreshingResults}
              error={labsError || challengesError}
            />
          )}
        </div>
      </AppShell>

      {showChangePassword && (
        <ChangePasswordModal
          isOpen={showChangePassword}
          onClose={() => setShowChangePassword(false)}
          user={user}
        />
      )}
    </>
  );
}
