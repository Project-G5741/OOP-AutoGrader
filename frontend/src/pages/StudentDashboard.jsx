// StudentDashboard.jsx
import React, { useCallback, useEffect, useRef, useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import AppShell from '../components/layout/AppShell';
import StudentHistoryPage from './StudentHistory';
import ChangePasswordModal from '../components/student/ChangePasswordModal';
import StudentUI from '../components/student/StudentUI';
import Toast from '../components/ui/Toast';
import { isInCurrentTerm, patchStoredUser, ROUTES } from '../utils/authRoutes';
import { authHeaders } from '../utils/authHeaders';
import { friendlyLoadErrorFromResponse, toFriendlyError } from '../utils/apiError';
import { parseMmdResponse, mmdFromChallengeBundle } from '../utils/mmdResponse';

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

function mapOperationalTestcases(testcases = []) {
  return testcases.map((testcase, index) => ({
    id: `tc-${index}-${testcase.testcase_name || testcase.name || index}`,
    name: testcase.testcase_name || testcase.name || `Testcase ${index + 1}`,
    isHidden: testcase.is_hidden ?? testcase.isHidden ?? false,
    passed: testcase.result === 'PASS',
    result: testcase.result,
    input: testcase.input ?? '',
    expectedOutput: testcase.expected_output ?? testcase.expectedOutput ?? '',
    actualOutput: testcase.actual_output ?? testcase.actualOutput ?? '',
    assertions: testcase.assertions ?? [],
    feedback: testcase.feedback,
  }));
}

function normalizeClassMember(item) {
  return {
    ...item,
    ok: item.ok ?? item.isCorrect ?? false,
    partial: item.partial === true,
  };
}

function normalizeClassData(classes = []) {
  return classes.map((cls) => ({
    ...cls,
    fields: (cls.fields ?? []).map(normalizeClassMember),
    constructors: (cls.constructors ?? []).map(normalizeClassMember),
    methods: (cls.methods ?? []).map(normalizeClassMember),
  }));
}

function applyChallengeBundle(bundle) {
  if (!bundle) {
    return { classData: [], mmdData: [], mmdParseError: null, testCases: [], normalizationNotice: null };
  }
  const mmd = mmdFromChallengeBundle(bundle);
  return {
    classData: normalizeClassData(bundle.class ?? []),
    mmdData: mmd.classes,
    mmdParseError: mmd.parseError,
    testCases: mapOperationalTestcases(bundle.testcases),
    normalizationNotice: bundle.normalizationNotice ?? bundle.normalization_notice ?? null,
  };
}

function parseClassTabResponse(json) {
  if (Array.isArray(json)) {
    return { classData: normalizeClassData(json), normalizationNotice: null };
  }
  return {
    classData: normalizeClassData(json?.classes ?? []),
    normalizationNotice: json?.normalizationNotice ?? json?.normalization_notice ?? null,
  };
}

function applyCachedBundleToState(
    cachedBundle,
    setClassData,
    setMmdData,
    setMmdParseError,
    setTestCases,
    setClassNormalizationNotice,
) {
  const bundle = applyChallengeBundle(cachedBundle);
  setClassData(bundle.classData);
  setMmdData(bundle.mmdData);
  if (setMmdParseError) {
    setMmdParseError(bundle.mmdParseError);
  }
  setTestCases(bundle.testCases);
  if (setClassNormalizationNotice) {
    setClassNormalizationNotice(bundle.normalizationNotice);
  }
  return bundle;
}

function indexLabResultByChallengeId(labResult, challenges) {
  if (!labResult) return {};
  const indexed = {};
  for (const challenge of challenges) {
    const challengeNumber = challenge.challengeNumber ?? challenge.challenge_number;
    if (challengeNumber == null) continue;
    const bundle = labResult[`challenge_${challengeNumber}`];
    if (bundle) {
      indexed[challenge.id] = bundle;
    }
  }
  return indexed;
}

function challengeScoresFromBundles(indexedLabResult) {
  const scores = {};
  for (const [challengeId, bundle] of Object.entries(indexedLabResult)) {
    const total = bundle?.scores?.total;
    if (total != null) {
      scores[challengeId] = Math.round(Number(total));
    }
  }
  return scores;
}

export default function StudentDashboard({ user, onLogout, view = 'dashboard' }) {
  const navigate = useNavigate();
  const showHistory = view === 'history';
  const [inCurrentTerm, setInCurrentTerm] = useState(isInCurrentTerm(user?.inCurrentTerm));
  const [showChangePassword, setShowChangePassword] = useState(false);

  const [labs, setLabs] = useState([]);
  const [labSummariesById, setLabSummariesById] = useState({});
  const [selectedLabId, setSelectedLabId] = useState(null);
  const [labsError, setLabsError] = useState(null);

  const [challenges, setChallenges] = useState([]);
  const [selectedChallengeId, setSelectedChallengeId] = useState(null);
  const [challengesError, setChallengesError] = useState(null);

  const [mmdData, setMmdData] = useState([]);
  const [mmdParseError, setMmdParseError] = useState(null);
  const [classData, setClassData] = useState([]);
  const [classNormalizationNotice, setClassNormalizationNotice] = useState(null);
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
  const [toast, setToast] = useState(null);

  const classDataCacheRef = useRef({});
  const classNoticeCacheRef = useRef({});
  const mmdDataCacheRef = useRef({});
  const testcaseDataCacheRef = useRef({});
  const labResultCacheRef = useRef({});
  const statsFetchGenRef = useRef(0);

  const studentId = user?.id;

  const resultsRevealed = selectedLabId != null && revealedLabIds.includes(selectedLabId);
  const sessionResults = selectedLabId ? sessionResultsByLab[selectedLabId] : null;
  const sessionChallengeScores = sessionResults?.challengeScores ?? {};
  const sessionChallengeBundles = sessionResults?.challengeBundles ?? {};
  const sessionOverallScore = sessionResults?.overallScore ?? null;

  const fetchChallenges = useCallback(async (labId, { silent = false } = {}) => {
    if (!labId) return;
    if (!silent) {
      setIsLoadingChallenges(true);
    }
    setChallengesError(null);
    try {
      const res = await fetch(`${API_BASE}/api/labs/${labId}/challenges`);
      if (!res.ok) {
        throw new Error(await friendlyLoadErrorFromResponse(res));
      }
      const data = await res.json();
      setChallenges(data);
      setSelectedChallengeId((prev) => {
        if (prev && data.some((c) => c.id === prev)) return prev;
        return data.length > 0 ? data[0].id : null;
      });
    } catch (err) {
      console.error('Failed to fetch challenges:', err);
      if (!silent) {
        setChallengesError(toFriendlyError(err, 'read'));
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
      setNextAttemptNumber(Number(stats.totalSubmissions) + 1);
    } else {
      setNextAttemptNumber(1);
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
          currentGrade: null,
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
      setClassNormalizationNotice(null);
      setMmdData([]);
      setMmdParseError(null);
      setTestCases([]);
      return;
    }

    if (!hasSessionChallengeScore(challengeScores, challengeId)) {
      setClassData([]);
      setClassNormalizationNotice(null);
      setMmdData([]);
      setMmdParseError(null);
      setTestCases([]);
      return;
    }

    const cachedBundle = !force && labResultCacheRef.current[challengeId];
    if (cachedBundle) {
      const bundle = applyCachedBundleToState(
          cachedBundle,
          setClassData,
          setMmdData,
          setMmdParseError,
          setTestCases,
          setClassNormalizationNotice);
      classDataCacheRef.current[challengeId] = bundle.classData;
      classNoticeCacheRef.current[challengeId] = bundle.normalizationNotice;
      mmdDataCacheRef.current[challengeId] = {
        classes: bundle.mmdData,
        parseError: bundle.mmdParseError,
      };
      testcaseDataCacheRef.current[challengeId] = bundle.testCases;
      return;
    }

    const cachedClass = !force && classDataCacheRef.current[challengeId];
    const cachedNotice = !force && Object.hasOwn(classNoticeCacheRef.current, challengeId)
      ? classNoticeCacheRef.current[challengeId]
      : null;
    const cachedMmd = !force && mmdDataCacheRef.current[challengeId];
    const cachedTestcases = !force && testcaseDataCacheRef.current[challengeId];
    if (cachedClass && cachedMmd && cachedTestcases) {
      setClassData(cachedClass);
      setClassNormalizationNotice(cachedNotice);
      const parsedMmd = parseMmdResponse(cachedMmd);
      setMmdData(parsedMmd.classes);
      setMmdParseError(parsedMmd.parseError);
      setTestCases(cachedTestcases);
      return;
    }

    try {
      const query = new URLSearchParams({ studentId });
      if (submissionId) {
        query.set('submissionId', submissionId);
      }
      const qs = `?${query.toString()}`;
      const [classRes, mmdRes, testcaseRes] = await Promise.all([
        cachedClass
          ? Promise.resolve({
              ok: true,
              json: async () => ({
                classes: cachedClass,
                normalizationNotice: classNoticeCacheRef.current[challengeId] ?? null,
              }),
            })
          : fetch(`${API_BASE}/api/labs/${labId}/challenges/${challengeId}/class${qs}`),
        cachedMmd
          ? Promise.resolve({ ok: true, json: async () => cachedMmd })
          : fetch(`${API_BASE}/api/labs/${labId}/challenges/${challengeId}/mmd${qs}`),
        cachedTestcases
          ? Promise.resolve({ ok: true, json: async () => cachedTestcases })
          : fetch(`${API_BASE}/api/labs/${labId}/challenges/${challengeId}/testcases${qs}`),
      ]);

      const classJson = classRes.ok ? await classRes.json() : [];
      const parsedClass = parseClassTabResponse(classJson);
      const mmdJson = mmdRes.ok ? await mmdRes.json() : { classes: [], parseError: null };
      const parsedMmd = parseMmdResponse(mmdJson);
      const testcaseJson = testcaseRes.ok ? await testcaseRes.json() : [];

      if (!cachedClass) {
        classDataCacheRef.current[challengeId] = parsedClass.classData;
        classNoticeCacheRef.current[challengeId] = parsedClass.normalizationNotice;
      }
      if (!cachedMmd) {
        mmdDataCacheRef.current[challengeId] = parsedMmd;
      }
      if (!cachedTestcases) {
        testcaseDataCacheRef.current[challengeId] = mapOperationalTestcases(testcaseJson);
      }
      setClassData(parsedClass.classData);
      setClassNormalizationNotice(parsedClass.normalizationNotice);
      setMmdData(parsedMmd.classes);
      setMmdParseError(parsedMmd.parseError);
      setTestCases(mapOperationalTestcases(testcaseJson));
    } catch (err) {
      console.error('Failed to fetch challenge details:', err);
      setClassData([]);
      setClassNormalizationNotice(null);
      setMmdData([]);
      setMmdParseError(null);
      setTestCases([]);
    }
  }, [studentId]);

  const fetchLabSummaries = useCallback(async () => {
    const token = sessionStorage.getItem('accessToken');
    if (!token) {
      setLabSummariesById({});
      return;
    }
    try {
      const res = await fetch(`${API_BASE}/api/submissions/my-labs`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (!res.ok) return;
      const data = await res.json();
      const byId = {};
      for (const summary of data) {
        if (summary?.id != null) {
          byId[summary.id] = summary;
        }
      }
      setLabSummariesById(byId);
    } catch (err) {
      console.info('Failed to fetch lab summaries:', err.message);
    }
  }, []);

  useEffect(() => {
    async function fetchLabs() {
      if (showHistory || !inCurrentTerm) {
        setLabs([]);
        setIsLoadingLabs(false);
        return;
      }
      setIsLoadingLabs(true);
      try {
        const res = await fetch(`${API_BASE}/api/labs`, { headers: authHeaders() });
        if (!res.ok) {
          throw new Error(await friendlyLoadErrorFromResponse(res));
        }
        const data = await res.json();
        setLabs(data);
        if (data.length > 0) {
          setSelectedLabId(data[0].id);
        }
      } catch (err) {
        console.info('Failed to fetch labs:', err.message);
        setLabsError(toFriendlyError(err, 'read'));
      } finally {
        setIsLoadingLabs(false);
      }
    }
    fetchLabs();
    if (!showHistory && inCurrentTerm) {
      fetchLabSummaries();
    }
  }, [fetchLabSummaries, inCurrentTerm, showHistory]);

  useEffect(() => {
    let cancelled = false;
    async function refreshAccess() {
      try {
        const res = await fetch(`${API_BASE}/api/students/term-access`, { headers: authHeaders() });
        if (!res.ok) return;
        const data = await res.json();
        if (!cancelled) {
          const next = Boolean(data.inCurrentTerm);
          setInCurrentTerm(next);
          patchStoredUser({ inCurrentTerm: next });
        }
      } catch {
        // keep login-time value
      }
    }
    refreshAccess();
    return () => {
      cancelled = true;
    };
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
      setClassNormalizationNotice(null);
      setMmdData([]);
      setMmdParseError(null);
      setTestCases([]);
    }
  }, [selectedLabId, revealedLabIds]);

  useEffect(() => {
    if (!selectedChallengeId || !selectedLabId || !resultsRevealed) return;

    const labSession = sessionResultsByLab[selectedLabId];
    const challengeScores = labSession?.challengeScores ?? {};
    if (!hasSessionChallengeScore(challengeScores, selectedChallengeId)) {
      setClassData([]);
      setClassNormalizationNotice(null);
      setMmdData([]);
      setMmdParseError(null);
      setTestCases([]);
      return;
    }

    const cachedBundle = labResultCacheRef.current[selectedChallengeId];
    if (cachedBundle) {
      applyCachedBundleToState(
          cachedBundle,
          setClassData,
          setMmdData,
          setMmdParseError,
          setTestCases,
          setClassNormalizationNotice);
      return;
    }

    const cachedClass = classDataCacheRef.current[selectedChallengeId];
    const cachedMmd = mmdDataCacheRef.current[selectedChallengeId];
    const cachedTestcases = testcaseDataCacheRef.current[selectedChallengeId];
    if (cachedClass && cachedMmd && cachedTestcases) {
      setClassData(cachedClass);
      setClassNormalizationNotice(classNoticeCacheRef.current[selectedChallengeId] ?? null);
      const parsedMmd = parseMmdResponse(cachedMmd);
      setMmdData(parsedMmd.classes);
      setMmdParseError(parsedMmd.parseError);
      setTestCases(cachedTestcases);
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
    if (labId == null || String(labId) === String(selectedLabId)) return;
    statsFetchGenRef.current += 1;
    setStats({ currentGrade: null, totalSubmissions: null, latestSubmission: null });
    setSelectedLabId(labId);
    setSelectedChallengeId(null);
    classDataCacheRef.current = {};
    classNoticeCacheRef.current = {};
    mmdDataCacheRef.current = {};
    testcaseDataCacheRef.current = {};
    labResultCacheRef.current = {};
    setMmdData([]);
    setMmdParseError(null);
    setClassData([]);
    setClassNormalizationNotice(null);
    setTestCases([]);
  };

  const handleChallengeChange = (challengeId) => {
    setSelectedChallengeId(challengeId);
    if (!resultsRevealed) {
      setClassData([]);
      setClassNormalizationNotice(null);
      setMmdData([]);
      setMmdParseError(null);
      setTestCases([]);
      return;
    }
    const challengeScores = sessionResultsByLab[selectedLabId]?.challengeScores ?? {};
    if (!hasSessionChallengeScore(challengeScores, challengeId)) {
      setClassData([]);
      setClassNormalizationNotice(null);
      setMmdData([]);
      setMmdParseError(null);
      setTestCases([]);
      return;
    }
    const cachedBundle = labResultCacheRef.current[challengeId];
    if (cachedBundle) {
      applyCachedBundleToState(
          cachedBundle,
          setClassData,
          setMmdData,
          setMmdParseError,
          setTestCases,
          setClassNormalizationNotice);
      return;
    }
    const cachedClass = classDataCacheRef.current[challengeId];
    const cachedMmd = mmdDataCacheRef.current[challengeId];
    const cachedTestcases = testcaseDataCacheRef.current[challengeId];
    setClassData(cachedClass ?? []);
    setClassNormalizationNotice(classNoticeCacheRef.current[challengeId] ?? null);
    const parsedMmd = parseMmdResponse(cachedMmd ?? { classes: [], parseError: null });
    setMmdData(parsedMmd.classes);
    setMmdParseError(parsedMmd.parseError);
    setTestCases(cachedTestcases ?? []);
  };

  const handleUploadComplete = async (uploadResponse) => {
    if (!selectedLabId) return;

    const score = uploadResponse?.score != null
      ? Math.round(Number(uploadResponse.score))
      : null;
    setToast({
      message: score != null
        ? `Grading complete. Your score: ${score}/100`
        : 'Grading completed successfully.',
      type: 'success',
    });

    const resultMap = uploadResponse?.challengeResult ?? {};
    const submissionId = uploadResponse?.submissionId ?? null;

    const labResult = uploadResponse?.lab_result ?? uploadResponse?.labResult ?? null;
    const indexedLabResult = indexLabResultByChallengeId(labResult, challenges);
    const challengeScores = {
      ...normalizeChallengeScores(resultMap),
      ...challengeScoresFromBundles(indexedLabResult),
    };

    statsFetchGenRef.current += 1;

    classDataCacheRef.current = {};
    mmdDataCacheRef.current = {};
    testcaseDataCacheRef.current = {};
    labResultCacheRef.current = indexedLabResult;

    setSessionResultsByLab((prev) => ({
      ...prev,
      [selectedLabId]: {
        submissionId,
        overallScore: uploadResponse?.score != null
          ? Math.round(Number(uploadResponse.score))
          : null,
        challengeScores,
        challengeBundles: indexedLabResult,
      },
    }));

    setStats({
      currentGrade: null,
      totalSubmissions: uploadResponse?.totalSubmissions ?? null,
      latestSubmission: uploadResponse?.latestSubmission ?? null,
    });

    if (uploadResponse?.totalSubmissions != null) {
      setNextAttemptNumber(Number(uploadResponse.totalSubmissions) + 1);
    }

    fetchLabSummaries();

    setRevealedLabIds((prev) =>
      prev.includes(selectedLabId) ? prev : [...prev, selectedLabId]
    );

    setIsRefreshingResults(true);
    try {
      if (
        selectedChallengeId
        && hasSessionChallengeScore(challengeScores, selectedChallengeId)
      ) {
        const cachedBundle = indexedLabResult[selectedChallengeId];
        if (cachedBundle) {
          const bundle = applyCachedBundleToState(
              cachedBundle,
              setClassData,
              setMmdData,
              setMmdParseError,
              setTestCases,
              setClassNormalizationNotice);
          classDataCacheRef.current[selectedChallengeId] = bundle.classData;
          classNoticeCacheRef.current[selectedChallengeId] = bundle.normalizationNotice;
          mmdDataCacheRef.current[selectedChallengeId] = {
            classes: bundle.mmdData,
            parseError: bundle.mmdParseError,
          };
          testcaseDataCacheRef.current[selectedChallengeId] = bundle.testCases;
        } else {
          await fetchChallengeDetails(selectedLabId, selectedChallengeId, {
            force: true,
            submissionId,
            challengeScores,
          });
        }
      } else {
        setClassData([]);
        setClassNormalizationNotice(null);
        setMmdData([]);
        setMmdParseError(null);
        setTestCases([]);
      }
    } finally {
      setIsRefreshingResults(false);
    }
  };

  const handleCommand = (cmd) => {
    if (cmd === 'home') {
      navigate(inCurrentTerm ? ROUTES.studentDashboard : ROUTES.studentHistory);
    } else if (cmd === 'history') {
      navigate(ROUTES.studentHistory);
    } else if (cmd === 'changePassword') {
      setShowChangePassword(true);
    }
  };

  if (view === 'dashboard' && !inCurrentTerm) {
    return <Navigate to={ROUTES.studentHistory} replace />;
  }

  const isInitialLoading = isLoadingLabs || (isLoadingChallenges && challenges.length === 0);

  return (
    <>
      <AppShell user={user} onLogout={onLogout} onCommand={handleCommand} hideHome={!inCurrentTerm} className="!mt-0">
        <div className="w-full">
          {labsError && (
            <div className="mb-4 rounded-md border border-warning/40 bg-warning-bg p-3 text-sm text-warning-text">
              {labsError}
            </div>
          )}

          {showHistory ? (
            <StudentHistoryPage
              user={user}
              onLogout={onLogout}
              onNavigate={() => navigate(ROUTES.studentDashboard)}
              inCurrentTerm={inCurrentTerm}
            />
          ) : (
            <StudentUI
              user={user}
              labs={labs}
              labSummariesById={labSummariesById}
              selectedLabId={selectedLabId}
              onLabChange={handleLabChange}
              challenges={challenges}
              selectedChallengeId={selectedChallengeId}
              onChallengeChange={handleChallengeChange}
              mmdData={mmdData}
              mmdParseError={mmdParseError}
              classData={classData}
              classNormalizationNotice={classNormalizationNotice}
              testCases={testCases}
              stats={stats}
              nextAttemptNumber={nextAttemptNumber}
              onUploadComplete={handleUploadComplete}
              isLoading={isInitialLoading}
              isLoadingDetails={isLoadingDetails}
              isRefreshingResults={isRefreshingResults}
              resultsRevealed={resultsRevealed}
              sessionChallengeScores={sessionChallengeScores}
              sessionChallengeBundles={sessionChallengeBundles}
              sessionOverallScore={sessionOverallScore}
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

      {toast && (
        <Toast
          message={toast.message}
          type={toast.type}
          onDismiss={() => setToast(null)}
        />
      )}
    </>
  );
}