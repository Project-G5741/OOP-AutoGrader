// StudentDashboard.jsx
import React, { useCallback, useEffect, useRef, useState } from 'react';
import AppShell from '../components/layout/AppShell';
import StudentHistoryPage from './StudentHistory';
import ChangePasswordModal from '../components/student/ChangePasswordModal';
import StudentUI from '../components/student/StudentUI';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';

function normalizeChallengeScores(resultMap = {}) {
  const scores = {};
  for (const [id, score] of Object.entries(resultMap)) {
    scores[id] = score;
  }
  return scores;
}

function hasSessionChallengeScore(challengeScores, challengeId) {
  if (!challengeScores || challengeId == null) return false;
  return Object.hasOwn(challengeScores, challengeId)
    || Object.hasOwn(challengeScores, String(challengeId));
}

function sessionChallengeScore(challengeScores, challengeId) {
  if (!challengeScores || challengeId == null) return undefined;
  if (Object.hasOwn(challengeScores, challengeId)) return challengeScores[challengeId];
  return challengeScores[String(challengeId)];
}

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
  const [revealedLabIds, setRevealedLabIds] = useState([]);
  const [sessionResultsByLab, setSessionResultsByLab] = useState({});

  const classDataCacheRef = useRef({});
  const mmdDataCacheRef = useRef({});
  const statsFetchGenRef = useRef(0);

  const studentId = user?.id;

  const resultsRevealed = selectedLabId != null && revealedLabIds.includes(selectedLabId);
  const sessionResults = selectedLabId ? sessionResultsByLab[selectedLabId] : null;
  const sessionChallengeScores = sessionResults?.challengeScores ?? {};

  const fetchChallenges = useCallback(async (labId, { silent = false } = {}) => {
    if (!labId) return;
    if (!silent) {
      setIsLoadingChallenges(true);
    }
    setChallengesError(null);
    try {
      const res = await fetch(`${API_BASE}/api/labs/${labId}/challenges`);
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
  }, []);

  useEffect(() => {
    if (stats.totalSubmissions != null) {
      setNextAttemptNumber((prev) =>
        Math.max(prev, Number(stats.totalSubmissions) + 1)
      );
    }
  }, [stats.totalSubmissions, selectedLabId]);

  const fetchStats = useCallback(async (labId) => {
    if (!labId || !studentId) {
      setStats({ currentGrade: null, totalSubmissions: null, latestSubmission: null });
      return;
    }
    const generation = statsFetchGenRef.current;
    try {
      const statsRes = await fetch(
        `${API_BASE}/api/labs/${labId}/stats?studentId=${studentId}`
      );
      if (generation !== statsFetchGenRef.current) return;
      if (statsRes.ok) {
        const fresh = await statsRes.json();
        setStats({
          currentGrade: fresh.currentGrade ?? null,
          totalSubmissions: fresh.totalSubmissions ?? null,
          latestSubmission: fresh.latestSubmission ?? null,
        });
      } else {
        setStats({ currentGrade: null, totalSubmissions: null, latestSubmission: null });
      }
    } catch (err) {
      console.error('Failed to fetch stats:', err);
      if (generation === statsFetchGenRef.current) {
        setStats({ currentGrade: null, totalSubmissions: null, latestSubmission: null });
      }
    }
  }, [studentId]);

  const fetchChallengeDetails = useCallback(async (
    labId,
    challengeId,
    { force = false, submissionId, challengeScores } = {},
  ) => {
    if (!labId || !challengeId || !studentId) {
      setClassData([]);
      setMmdData([]);
      return;
    }

    setTestCases([]);

    if (!hasSessionChallengeScore(challengeScores, challengeId)) {
      setClassData([]);
      setMmdData([]);
      return;
    }

    const cachedClass = !force && classDataCacheRef.current[challengeId];
    const cachedMmd = !force && mmdDataCacheRef.current[challengeId];
    if (cachedClass && cachedMmd) {
      setClassData(cachedClass);
      setMmdData(cachedMmd);
      return;
    }

    try {
      const query = new URLSearchParams({ studentId });
      if (submissionId) {
        query.set('submissionId', submissionId);
      }
      const qs = `?${query.toString()}`;
      const [classRes, mmdRes] = await Promise.all([
        cachedClass
          ? Promise.resolve({ ok: true, json: async () => cachedClass })
          : fetch(`${API_BASE}/api/labs/${labId}/challenges/${challengeId}/class${qs}`),
        cachedMmd
          ? Promise.resolve({ ok: true, json: async () => cachedMmd })
          : fetch(`${API_BASE}/api/labs/${labId}/challenges/${challengeId}/mmd${qs}`),
      ]);

      const classJson = classRes.ok ? await classRes.json() : [];
      const mmdJson = mmdRes.ok ? await mmdRes.json() : [];

      if (!cachedClass) {
        classDataCacheRef.current[challengeId] = classJson;
      }
      if (!cachedMmd) {
        mmdDataCacheRef.current[challengeId] = mmdJson;
      }
      setClassData(classJson);
      setMmdData(mmdJson);
    } catch (err) {
      console.error('Failed to fetch challenge details:', err);
      setClassData([]);
      setMmdData([]);
    }
  }, [studentId]);

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
    fetchChallenges(selectedLabId);
    fetchStats(selectedLabId);
  }, [selectedLabId, fetchChallenges, fetchStats]);

  useEffect(() => {
    if (!selectedLabId) return;
    const labRevealed = revealedLabIds.includes(selectedLabId);
    if (!labRevealed) {
      setClassData([]);
      setMmdData([]);
      setTestCases([]);
    }
  }, [selectedLabId, revealedLabIds]);

  useEffect(() => {
    if (!selectedChallengeId || !selectedLabId || !resultsRevealed) return;

    const labSession = sessionResultsByLab[selectedLabId];
    const challengeScores = labSession?.challengeScores ?? {};
    if (!hasSessionChallengeScore(challengeScores, selectedChallengeId)) {
      setClassData([]);
      setMmdData([]);
      setTestCases([]);
      return;
    }

    const cachedClass = classDataCacheRef.current[selectedChallengeId];
    const cachedMmd = mmdDataCacheRef.current[selectedChallengeId];
    if (cachedClass && cachedMmd) {
      setClassData(cachedClass);
      setMmdData(cachedMmd);
      setTestCases([]);
      return;
    }

    async function fetchDetails() {
      setIsLoadingDetails(true);
      try {
        await fetchChallengeDetails(selectedLabId, selectedChallengeId, {
          submissionId: labSession?.submissionId,
          challengeScores,
        });
      } catch (err) {
        console.error('Failed to fetch details:', err);
      } finally {
        setIsLoadingDetails(false);
      }
    }
    fetchDetails();
  }, [
    selectedLabId,
    selectedChallengeId,
    fetchChallengeDetails,
    resultsRevealed,
    sessionResultsByLab,
  ]);

  const handleLabChange = (labId) => {
    setSelectedLabId(labId);
    setSelectedChallengeId(null);
    classDataCacheRef.current = {};
    mmdDataCacheRef.current = {};
    setMmdData([]);
    setClassData([]);
    setTestCases([]);
  };

  const handleChallengeChange = (challengeId) => {
    setSelectedChallengeId(challengeId);
    if (!resultsRevealed) {
      setClassData([]);
      setMmdData([]);
      setTestCases([]);
      return;
    }
    const challengeScores = sessionResultsByLab[selectedLabId]?.challengeScores ?? {};
    if (!hasSessionChallengeScore(challengeScores, challengeId)) {
      setClassData([]);
      setMmdData([]);
      setTestCases([]);
      return;
    }
    const cachedClass = classDataCacheRef.current[challengeId];
    const cachedMmd = mmdDataCacheRef.current[challengeId];
    if (cachedClass && cachedMmd) {
      setClassData(cachedClass);
      setMmdData(cachedMmd);
    }
  };

  const handleUploadComplete = async (uploadResponse) => {
    if (!selectedLabId) return;

    const resultMap = uploadResponse?.challengeResult ?? {};
    const challengeScores = normalizeChallengeScores(resultMap);
    const submissionId = uploadResponse?.submissionId ?? null;

    statsFetchGenRef.current += 1;

    classDataCacheRef.current = {};
    mmdDataCacheRef.current = {};

    setSessionResultsByLab((prev) => ({
      ...prev,
      [selectedLabId]: {
        submissionId,
        challengeScores,
      },
    }));

    setStats({
      currentGrade: uploadResponse?.score != null
        ? Math.round(Number(uploadResponse.score))
        : null,
      totalSubmissions: uploadResponse?.totalSubmissions ?? null,
      latestSubmission: uploadResponse?.latestSubmission ?? null,
    });

    if (uploadResponse?.attemptNumber != null) {
      setNextAttemptNumber(Number(uploadResponse.attemptNumber) + 1);
    } else if (uploadResponse?.totalSubmissions != null) {
      setNextAttemptNumber(Number(uploadResponse.totalSubmissions) + 1);
    }

    setRevealedLabIds((prev) =>
      prev.includes(selectedLabId) ? prev : [...prev, selectedLabId]
    );

    setIsRefreshingResults(true);
    try {
      if (
        selectedChallengeId
        && hasSessionChallengeScore(challengeScores, selectedChallengeId)
      ) {
        await fetchChallengeDetails(selectedLabId, selectedChallengeId, {
          force: true,
          submissionId,
          challengeScores,
        });
      } else {
        setClassData([]);
        setMmdData([]);
        setTestCases([]);
      }
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
              resultsRevealed={resultsRevealed}
              sessionChallengeScores={sessionChallengeScores}
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