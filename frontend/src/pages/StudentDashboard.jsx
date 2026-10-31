// StudentDashboard.jsx
import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { Clock, Home, Store, Trophy } from 'lucide-react';
import AppShell from '../components/layout/AppShell';
import StudentHistoryPage from './StudentHistory';
import StudentRankingPage from './StudentRankingPage';
import ChangePasswordModal from '../components/student/ChangePasswordModal';
import StudentUI from '../components/student/StudentUI';
import StudentFoxMascot from '../components/student/StudentFoxMascot';
import StudentStreakFlame from '../components/student/StudentStreakFlame';
import MascotShop from '../components/student/MascotShop';
import { mascotArt } from '../components/student/studentMascots';
import { formatNumber } from '../utils/formatters';
import StudentOfflinePracticeDownload from '../components/student/StudentOfflinePracticeDownload';
import DesktopRubricPackImport from '../components/student/DesktopRubricPackImport';
import { useToast } from '../components/ui/Toast';
import { getAccessToken, hasRole, isInCurrentTerm, patchStoredUser, ROUTES } from '../utils/authRoutes';
import { authHeaders } from '../utils/authHeaders';
import { apiFetch } from '../utils/apiFetch';
import { friendlyLoadErrorFromResponse, toFriendlyError } from '../utils/apiError';
import { parseMmdResponse, mmdFromChallengeBundle } from '../utils/mmdResponse';
import { waitForDesktopBootstrap } from '../utils/desktopBootstrap';
import { labResultHasCompileError } from '../utils/compileErrors';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';
const IS_DESKTOP_APP = import.meta.env.VITE_APP_MODE === 'desktop';

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
    oopPrincipleTag: testcase.oop_principle_tag ?? testcase.oopPrincipleTag ?? null,
    feedback: testcase.feedback,
  }));
}

function normalizeClassMember(item) {
  const leakedPartial = item.partial === true;
  return {
    ...item,
    ok: (item.ok ?? item.isCorrect ?? false) && !leakedPartial,
    partial: false,
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

function mergeTestcaseBundle(challengeId, testcaseJson, cacheRef) {
  const mapped = mapOperationalTestcases(testcaseJson);
  if (mapped.length === 0) {
    return mapped;
  }
  const existing = cacheRef.current[challengeId] ?? {};
  cacheRef.current[challengeId] = {
    ...existing,
    testcases: testcaseJson,
    scoreApplicability: {
      ...(existing.scoreApplicability ?? existing.score_applicability ?? {}),
      testcase: true,
    },
  };
  return mapped;
}

function applyChallengeBundle(bundle) {
  if (!bundle) {
    return { classData: [], mmdData: [], mmdParseError: null, testCases: [], normalizationNotice: null };
  }
  const mmd = mmdFromChallengeBundle(bundle);
  const testcaseApplicable = bundle.scoreApplicability?.testcase === true
    || bundle.score_applicability?.testcase === true
    || (Array.isArray(bundle.testcases) && bundle.testcases.length > 0);
  return {
    classData: normalizeClassData(bundle.class ?? []),
    mmdData: mmd.classes,
    mmdParseError: mmd.parseError,
    testCases: testcaseApplicable ? mapOperationalTestcases(bundle.testcases) : [],
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
      indexed[challengeCacheKey(challenge.id)] = bundle;
    }
  }
  return indexed;
}

function challengeCacheKey(challengeId) {
  return challengeId == null ? '' : String(challengeId);
}

/**
 * Student upload returns scores-only (or Class compile cards). Detail trees load via GET.
 * OT rows may later be merged into this same cache object without Class/MMD — that must
 * still count as thin, or the UI reapplies empty Class/MMD and wipes a successful GET.
 */
function isThinChallengeBundle(bundle) {
  if (!bundle) {
    return true;
  }
  const classes = bundle.class ?? [];
  const mmdClasses = bundle.mmd?.classes ?? (Array.isArray(bundle.mmd) ? bundle.mmd : []);
  const testcases = bundle.testcases ?? [];
  const applicability = bundle.scoreApplicability ?? bundle.score_applicability ?? {};

  const classNeeded = applicability.class !== false;
  const mmdNeeded = applicability.mmd === true;
  const testcaseNeeded = applicability.testcase === true;

  const classReady = !classNeeded || classes.length > 0;
  const mmdReady = !mmdNeeded || mmdClasses.length > 0;
  const testcaseReady = !testcaseNeeded || testcases.length > 0;

  return !(classReady && mmdReady && testcaseReady);
}

/**
 * Tab caches are ready only when every applicable pillar has data. Empty [] from a racy
 * or failed GET must not count as a hit (Declaration score can show while classData is []).
 */
function detailCachesReady(challengeId, bundle, classCache, mmdCache, testcaseCache) {
  const key = challengeCacheKey(challengeId);
  if (!key) return false;
  if (!Object.hasOwn(classCache, key)
      || !Object.hasOwn(mmdCache, key)
      || !Object.hasOwn(testcaseCache, key)) {
    return false;
  }
  const applicability = bundle?.scoreApplicability ?? bundle?.score_applicability ?? {};
  const classes = classCache[key] ?? [];
  const mmdClasses = mmdCache[key]?.classes ?? [];
  if (applicability.class !== false && classes.length === 0) {
    return false;
  }
  if (applicability.mmd === true && mmdClasses.length === 0) {
    return false;
  }
  return true;
}

const LAB_RESULT_CACHE_TTL_MS = 60_000;

function pruneExpiredLabResultCache(cacheRef, atRef) {
  const now = Date.now();
  for (const challengeId of Object.keys(atRef.current)) {
    if (now - atRef.current[challengeId] > LAB_RESULT_CACHE_TTL_MS) {
      delete cacheRef.current[challengeId];
      delete atRef.current[challengeId];
    }
  }
}

function challengeScoresFromBundles(indexedLabResult) {
  const scores = {};
  for (const [challengeId, bundle] of Object.entries(indexedLabResult)) {
    const total = bundle?.scores?.total;
    if (total != null) {
      scores[challengeCacheKey(challengeId)] = Math.floor(Number(total));
    }
  }
  return scores;
}

function emptyStats() {
  return { currentGrade: null, totalSubmissions: null, latestSubmission: null };
}

function statsFromLab(lab) {
  if (!lab) return emptyStats();
  return {
    currentGrade: null,
    totalSubmissions: lab.totalSubmissions ?? null,
    latestSubmission: lab.latestSubmission ?? null,
  };
}

function cacheEmbeddedChallenges(labs, cache) {
  for (const lab of labs) {
    if (Array.isArray(lab?.challenges)) {
      cache[lab.id] = lab.challenges;
    }
  }
}

function firstChallengeId(challenges) {
  return challenges.length > 0 ? challenges[0].id : null;
}

export default function StudentDashboard({ user, onLogout, view = 'dashboard' }) {
  const showToast = useToast();
  const navigate = useNavigate();
  const showHistory = view === 'history';
  const showRanking = view === 'ranking';
  const [inCurrentTerm, setInCurrentTerm] = useState(isInCurrentTerm(user?.inCurrentTerm));
  const [quarterName, setQuarterName] = useState('');
  const [showChangePassword, setShowChangePassword] = useState(false);
  const [showMascotShop, setShowMascotShop] = useState(false);
  const [foxCoverEyes, setFoxCoverEyes] = useState(false);
  const [streak, setStreak] = useState(null);
  const [awardPulse, setAwardPulse] = useState(0);
  const [restoringStreak, setRestoringStreak] = useState(false);
  const streakSnapshot = useRef(null);

  const applyStreak = useCallback((next) => {
    const prev = streakSnapshot.current;
    const awarded = prev && next && next.points > prev.points;
    const missionJustDone = Boolean(next?.missionCompletedNow) || (prev && next && (
      (!prev.loggedIn && next.loggedIn)
      || (prev.submitCount < (prev.submitGoal ?? 3) && next.submitCount >= (next.submitGoal ?? 3))
      || (prev.petCount < (prev.petGoal ?? 4) && next.petCount >= (next.petGoal ?? 4))
    ));
    if (missionJustDone && !awarded) {
      showToast('Mission complete', 'success');
    }
    if (awarded) {
      const gained = next.points - prev.points;
      showToast(`Day ${next.streak} streak. +${gained} points.`, 'success');
      setAwardPulse((value) => value + 1);
    }
    streakSnapshot.current = next;
    setStreak(next);
  }, [showToast]);

  const refreshStreak = useCallback(async () => {
    try {
      const response = await apiFetch(`${API_BASE}/api/students/streak`, {
        headers: authHeaders(),
      });
      if (!response.ok) return;
      applyStreak(await response.json());
    } catch {
      /* the dashboard still works when the streak request fails */
    }
  }, [applyStreak]);

  useEffect(() => {
    refreshStreak();
  }, [refreshStreak]);

  const petFox = useCallback(async () => {
    const goal = streakSnapshot.current?.petGoal ?? 4;
    if ((streakSnapshot.current?.petCount ?? 0) >= goal) return;
    try {
      const response = await apiFetch(`${API_BASE}/api/students/streak/pet`, {
        method: 'POST',
        headers: authHeaders(),
      });
      if (!response.ok) {
        showToast(await friendlyLoadErrorFromResponse(response), 'error');
        return;
      }
      applyStreak(await response.json());
    } catch (error) {
      showToast(toFriendlyError(error), 'error');
    }
  }, [applyStreak, showToast]);

  const restoreStreak = useCallback(async () => {
    setRestoringStreak(true);
    try {
      const response = await apiFetch(`${API_BASE}/api/students/streak/restore`, {
        method: 'POST',
        headers: authHeaders(),
      });
      if (!response.ok) {
        showToast(await friendlyLoadErrorFromResponse(response), 'error');
        return;
      }
      applyStreak(await response.json());
    } catch (error) {
      showToast(toFriendlyError(error), 'error');
    } finally {
      setRestoringStreak(false);
    }
  }, [applyStreak, showToast]);

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
  const [dailySubmissionQuota, setDailySubmissionQuota] = useState(null);

  const refreshDailySubmissionQuota = useCallback(async () => {
    if (IS_DESKTOP_APP) return;
    try {
      const res = await apiFetch(`${API_BASE}/api/students/daily-submission-quota`, {
        headers: authHeaders(),
      });
      if (!res.ok) return;
      const data = await res.json();
      setDailySubmissionQuota({
        usedToday: Number(data.usedToday) || 0,
        dailyLimit: Number(data.dailyLimit) || 30,
        remaining: Number(data.remaining) || 0,
        canSubmit: data.canSubmit !== false,
      });
    } catch {
      /* submit still works; server enforces quota */
    }
  }, []);

  const [isLoadingLabs, setIsLoadingLabs] = useState(
    () => view !== 'history' && view !== 'ranking' && isInCurrentTerm(user?.inCurrentTerm),
  );
  const [isLoadingChallenges, setIsLoadingChallenges] = useState(false);
  const [isLoadingDetails, setIsLoadingDetails] = useState(false);
  const [isRefreshingResults, setIsRefreshingResults] = useState(false);
  const [revealedLabIds, setRevealedLabIds] = useState([]);
  const [sessionResultsByLab, setSessionResultsByLab] = useState({});

  const classDataCacheRef = useRef({});
  const classNoticeCacheRef = useRef({});
  const mmdDataCacheRef = useRef({});
  const testcaseDataCacheRef = useRef({});
  const labResultCacheRef = useRef({});
  const labResultCacheAtRef = useRef({});
  const statsFetchGenRef = useRef(0);
  const skipLabReloadRef = useRef(false);
  const challengesByLabRef = useRef({});
  /** True while post-upload prefetch runs — blocks the challenge-switch effect from a second spinner. */
  const prefetchingDetailsRef = useRef(false);

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
      setChallenges([]);
      setSelectedChallengeId(null);
    }
    setChallengesError(null);
    try {
      const res = await apiFetch(`${API_BASE}/api/labs/${labId}/challenges/list`, { headers: authHeaders() });
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
      const statsRes = await apiFetch(
        `${API_BASE}/api/labs/${labId}/stats?studentId=${studentId}`,
        { headers: authHeaders() },
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

  const applyDetailCachesToState = useCallback((challengeId) => {
    const key = challengeCacheKey(challengeId);
    setClassData(classDataCacheRef.current[key] ?? []);
    setClassNormalizationNotice(classNoticeCacheRef.current[key] ?? null);
    const parsedMmd = parseMmdResponse(
        mmdDataCacheRef.current[key] ?? { classes: [], parseError: null });
    setMmdData(parsedMmd.classes);
    setMmdParseError(parsedMmd.parseError);
    setTestCases(testcaseDataCacheRef.current[key] ?? []);
  }, []);

  const fetchChallengeDetails = useCallback(async (
    labId,
    challengeId,
    { force = false, submissionId, challengeScores, applyToUi = true } = {},
  ) => {
    const key = challengeCacheKey(challengeId);
    if (!labId || !key || !studentId) {
      if (applyToUi) {
        setClassData([]);
        setClassNormalizationNotice(null);
        setMmdData([]);
        setMmdParseError(null);
        setTestCases([]);
      }
      return;
    }

    if (!hasSessionChallengeScore(challengeScores, challengeId)) {
      if (applyToUi) {
        setClassData([]);
        setClassNormalizationNotice(null);
        setMmdData([]);
        setMmdParseError(null);
        setTestCases([]);
      }
      return;
    }

    pruneExpiredLabResultCache(labResultCacheRef, labResultCacheAtRef);
    const thinBundle = labResultCacheRef.current[key];
    const cachedBundle = !force && thinBundle;
    if (cachedBundle && !isThinChallengeBundle(cachedBundle)) {
      const bundle = applyChallengeBundle(cachedBundle);
      classDataCacheRef.current[key] = bundle.classData;
      classNoticeCacheRef.current[key] = bundle.normalizationNotice;
      mmdDataCacheRef.current[key] = {
        classes: bundle.mmdData,
        parseError: bundle.mmdParseError,
      };
      testcaseDataCacheRef.current[key] = bundle.testCases;
      if (applyToUi) {
        applyCachedBundleToState(
            cachedBundle,
            setClassData,
            setMmdData,
            setMmdParseError,
            setTestCases,
            setClassNormalizationNotice);
      }
      return;
    }

    const cacheBundle = thinBundle ?? labResultCacheRef.current[key];
    if (!force && detailCachesReady(
        key,
        cacheBundle,
        classDataCacheRef.current,
        mmdDataCacheRef.current,
        testcaseDataCacheRef.current,
    )) {
      if (applyToUi) {
        applyDetailCachesToState(key);
      }
      return;
    }

    try {
      const query = new URLSearchParams({ studentId });
      if (submissionId) {
        query.set('submissionId', submissionId);
      }
      const qs = `?${query.toString()}`;
      const [classRes, mmdRes, testcaseRes] = await Promise.all([
        apiFetch(`${API_BASE}/api/labs/${labId}/challenges/${challengeId}/class${qs}`, { headers: authHeaders() }),
        apiFetch(`${API_BASE}/api/labs/${labId}/challenges/${challengeId}/mmd${qs}`, { headers: authHeaders() }),
        apiFetch(`${API_BASE}/api/labs/${labId}/challenges/${challengeId}/testcases${qs}`, { headers: authHeaders() }),
      ]);

      const classJson = classRes.ok ? await classRes.json() : null;
      const parsedClass = classJson != null
        ? parseClassTabResponse(classJson)
        : { classData: [], normalizationNotice: null };
      const mmdJson = mmdRes.ok ? await mmdRes.json() : null;
      const parsedMmd = mmdJson != null
        ? parseMmdResponse(mmdJson)
        : { classes: [], parseError: null };
      const testcaseRaw = testcaseRes.ok ? await testcaseRes.json() : null;
      const testcaseJson = Array.isArray(testcaseRaw) ? testcaseRaw : [];
      const mappedTestcases = testcaseRes.ok
        ? mergeTestcaseBundle(key, testcaseJson, labResultCacheRef)
        : [];

      // Only cache successful responses — never poison the cache with !ok empties.
      if (classRes.ok) {
        classDataCacheRef.current[key] = parsedClass.classData;
        classNoticeCacheRef.current[key] = parsedClass.normalizationNotice;
      } else {
        delete classDataCacheRef.current[key];
        delete classNoticeCacheRef.current[key];
      }
      if (mmdRes.ok) {
        mmdDataCacheRef.current[key] = parsedMmd;
      } else {
        delete mmdDataCacheRef.current[key];
      }
      if (testcaseRes.ok) {
        testcaseDataCacheRef.current[key] = mappedTestcases;
      } else {
        delete testcaseDataCacheRef.current[key];
      }
      if (applyToUi) {
        setClassData(classRes.ok ? parsedClass.classData : []);
        setClassNormalizationNotice(classRes.ok ? parsedClass.normalizationNotice : null);
        setMmdData(mmdRes.ok ? parsedMmd.classes : []);
        setMmdParseError(mmdRes.ok ? parsedMmd.parseError : null);
        setTestCases(testcaseRes.ok ? mappedTestcases : []);
      }
    } catch (err) {
      console.error('Failed to fetch challenge details:', err);
      if (applyToUi) {
        setClassData([]);
        setClassNormalizationNotice(null);
        setMmdData([]);
        setMmdParseError(null);
        setTestCases([]);
      }
    }
  }, [studentId, applyDetailCachesToState]);

  const prefetchAllChallengeDetails = useCallback(async (
    labId,
    challengeList,
    { submissionId, challengeScores } = {},
  ) => {
    const ids = (challengeList ?? [])
        .map((ch) => ch?.id)
        .filter((id) => id != null && hasSessionChallengeScore(challengeScores, id));
    if (ids.length === 0) return;
    await Promise.all(ids.map((challengeId) => fetchChallengeDetails(labId, challengeId, {
      force: true,
      submissionId,
      challengeScores,
      applyToUi: false,
    })));
  }, [fetchChallengeDetails]);

  const fetchLabSummaries = useCallback(async () => {
    const token = getAccessToken();
    if (!token) {
      setLabSummariesById({});
      return;
    }
    try {
      const res = await apiFetch(`${API_BASE}/api/submissions/my-labs?scope=current`, {
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
      if (showHistory || showRanking || !inCurrentTerm) {
        setLabs([]);
        setIsLoadingLabs(false);
        return;
      }
      setIsLoadingLabs(true);
      try {
        if (IS_DESKTOP_APP) {
          await waitForDesktopBootstrap();
        }
        const res = await apiFetch(`${API_BASE}/api/labs/list?scope=student`, { headers: authHeaders() });
        if (!res.ok) {
          throw new Error(await friendlyLoadErrorFromResponse(res));
        }
        const data = await res.json();
        cacheEmbeddedChallenges(data, challengesByLabRef.current);
        setLabs(data);
        if (data.length > 0) {
          setSelectedLabId((prev) => prev ?? data[0].id);
        }
      } catch (err) {
        console.info('Failed to fetch labs:', err.message);
        setLabsError(toFriendlyError(err, 'read'));
      } finally {
        setIsLoadingLabs(false);
      }
    }
    fetchLabs();
    if (!showHistory && !showRanking && inCurrentTerm) {
      fetchLabSummaries();
    }
  }, [fetchLabSummaries, inCurrentTerm, showHistory, showRanking]);

  useEffect(() => {
    let cancelled = false;
    async function refreshAccess() {
      try {
        const res = await apiFetch(`${API_BASE}/api/students/term-access`, { headers: authHeaders() });
        if (!res.ok) return;
        const data = await res.json();
        if (!cancelled) {
          const next = Boolean(data.inCurrentTerm);
          const name = typeof data.quarterName === 'string' ? data.quarterName.trim() : '';
          setInCurrentTerm((prev) => (prev === next ? prev : next));
          setQuarterName(next ? name : '');
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
    if (inCurrentTerm && !showHistory && !showRanking) {
      refreshDailySubmissionQuota();
    }
  }, [inCurrentTerm, showHistory, showRanking, refreshDailySubmissionQuota]);

  useEffect(() => {
    if (!selectedLabId) return;
    if (skipLabReloadRef.current) return;
    const cached = challengesByLabRef.current[selectedLabId];
    if (cached) {
      setChallenges(cached);
      setSelectedChallengeId((prev) => {
        if (prev && cached.some((challenge) => challenge.id === prev)) return prev;
        return firstChallengeId(cached);
      });
      setIsLoadingChallenges(false);
    } else {
      fetchChallenges(selectedLabId);
    }
    const lab = labs.find((item) => String(item.id) === String(selectedLabId));
    if (lab && Array.isArray(lab.challenges)) {
      setStats(statsFromLab(lab));
    } else {
      fetchStats(selectedLabId);
    }
  }, [selectedLabId, fetchChallenges, fetchStats, labs]);

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
    // Post-upload prefetch owns loading; avoid a second spinner / duplicate GETs.
    if (prefetchingDetailsRef.current || isRefreshingResults) return;

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

    const selectedKey = challengeCacheKey(selectedChallengeId);
    const cachedBundle = labResultCacheRef.current[selectedKey];
    if (cachedBundle && !isThinChallengeBundle(cachedBundle)) {
      applyCachedBundleToState(
          cachedBundle,
          setClassData,
          setMmdData,
          setMmdParseError,
          setTestCases,
          setClassNormalizationNotice);
      return;
    }

    if (detailCachesReady(
        selectedKey,
        cachedBundle,
        classDataCacheRef.current,
        mmdDataCacheRef.current,
        testcaseDataCacheRef.current,
    )) {
      applyDetailCachesToState(selectedKey);
      return;
    }

    async function fetchDetails() {
      setIsLoadingDetails(true);
      try {
        await fetchChallengeDetails(selectedLabId, selectedChallengeId, {
          force: true,
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
    applyDetailCachesToState,
    resultsRevealed,
    sessionResultsByLab,
    isRefreshingResults,
  ]);

  const handleLabChange = (labId) => {
    if (labId == null || String(labId) === String(selectedLabId)) return;
    skipLabReloadRef.current = false;
    statsFetchGenRef.current += 1;
    const lab = labs.find((item) => String(item.id) === String(labId));
    const cached = challengesByLabRef.current[labId];
    if (lab && Array.isArray(lab.challenges)) {
      setStats(statsFromLab(lab));
    } else {
      setStats(emptyStats());
    }
    if (cached) {
      setChallenges(cached);
      setSelectedChallengeId(firstChallengeId(cached));
    } else {
      setChallenges([]);
      setSelectedChallengeId(null);
    }
    setSelectedLabId(labId);
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
    const key = challengeCacheKey(challengeId);
    const cachedBundle = labResultCacheRef.current[key];
    if (cachedBundle && !isThinChallengeBundle(cachedBundle)) {
      applyCachedBundleToState(
          cachedBundle,
          setClassData,
          setMmdData,
          setMmdParseError,
          setTestCases,
          setClassNormalizationNotice);
      return;
    }
    if (detailCachesReady(
        key,
        cachedBundle,
        classDataCacheRef.current,
        mmdDataCacheRef.current,
        testcaseDataCacheRef.current,
    )) {
      applyDetailCachesToState(key);
      return;
    }
    if (selectedLabId) {
      setClassData([]);
      setClassNormalizationNotice(null);
      setMmdData([]);
      setMmdParseError(null);
      setTestCases([]);
      const labSession = sessionResultsByLab[selectedLabId];
      setIsLoadingDetails(true);
      void fetchChallengeDetails(selectedLabId, challengeId, {
        force: true,
        submissionId: labSession?.submissionId,
        challengeScores,
      }).finally(() => setIsLoadingDetails(false));
    }
  };

  const handleUploadComplete = async (uploadResponse) => {
    if (!selectedLabId) return;

    refreshDailySubmissionQuota();
    skipLabReloadRef.current = true;
    statsFetchGenRef.current += 1;
    const reportedTotal = uploadResponse?.totalSubmissions;
    if (reportedTotal != null) {
      setStats({
        currentGrade: null,
        totalSubmissions: reportedTotal,
        latestSubmission: uploadResponse?.latestSubmission ?? null,
      });
      setNextAttemptNumber(Number(reportedTotal) + 1);
    } else {
      setStats((prev) => {
        const nextTotal = (Number(prev.totalSubmissions) || 0) + 1;
        return {
          currentGrade: null,
          totalSubmissions: nextTotal,
          latestSubmission: uploadResponse?.latestSubmission ?? prev.latestSubmission,
        };
      });
      setNextAttemptNumber((n) => n + 1);
    }
    setLabs((prev) => prev.map((lab) => {
      if (String(lab.id) !== String(selectedLabId)) return lab;
      return {
        ...lab,
        totalSubmissions: reportedTotal ?? (Number(lab.totalSubmissions) || 0) + 1,
        latestSubmission: uploadResponse?.latestSubmission ?? lab.latestSubmission,
      };
    }));

    const score = uploadResponse?.score != null
      ? Math.floor(Number(uploadResponse.score))
      : null;

    const resultMap = uploadResponse?.challengeResult ?? {};
    const submissionId = uploadResponse?.submissionId ?? null;

    const labResult = uploadResponse?.lab_result ?? uploadResponse?.labResult ?? null;

    const scoreToast = {
      message: score != null
        ? `Grading complete. Your score: ${score}/100`
        : 'Grading completed successfully.',
      type: 'success',
    };
    void refreshStreak();
    const indexedLabResult = indexLabResultByChallengeId(labResult, challenges);
    const challengeScores = {
      ...normalizeChallengeScores(resultMap),
      ...challengeScoresFromBundles(indexedLabResult),
    };

    setChallenges((prev) => {
      const next = prev.map((challenge) => {
        const nextScore = challengeScores[challenge.id] ?? challengeScores[String(challenge.id)];
        if (nextScore == null || nextScore === challenge.score) {
          return challenge;
        }
        return { ...challenge, score: nextScore };
      });
      challengesByLabRef.current[selectedLabId] = next;
      return next;
    });

    classDataCacheRef.current = {};
    mmdDataCacheRef.current = {};
    testcaseDataCacheRef.current = {};
    labResultCacheRef.current = indexedLabResult;
    const stampedAt = Date.now();
    labResultCacheAtRef.current = Object.fromEntries(
      Object.keys(indexedLabResult).map((id) => [id, stampedAt]),
    );

    setSessionResultsByLab((prev) => ({
      ...prev,
      [selectedLabId]: {
        submissionId,
        overallScore: uploadResponse?.score != null
          ? Math.floor(Number(uploadResponse.score))
          : null,
        challengeScores,
        challengeBundles: indexedLabResult,
      },
    }));

    setLabSummariesById((prev) => {
      const existing = prev[selectedLabId];
      const nextAttempts = reportedTotal != null
        ? Number(reportedTotal)
        : (Number(existing?.attempts) || 0) + 1;
      return {
        ...prev,
        [selectedLabId]: {
          ...(existing ?? { id: selectedLabId }),
          attempts: nextAttempts,
          lastSubmittedAt: uploadResponse?.latestSubmission ?? existing?.lastSubmittedAt ?? null,
        },
      };
    });

    setRevealedLabIds((prev) =>
      prev.includes(selectedLabId) ? prev : [...prev, selectedLabId]
    );

    prefetchingDetailsRef.current = true;
    setIsRefreshingResults(true);
    setIsLoadingDetails(false);
    try {
      const needsSidecar = Object.values(indexedLabResult).some((bundle) => isThinChallengeBundle(bundle));
      if (needsSidecar) {
        await prefetchAllChallengeDetails(selectedLabId, challenges, {
          submissionId,
          challengeScores,
        });
      } else {
        for (const [challengeId, bundle] of Object.entries(indexedLabResult)) {
          if (!bundle || isThinChallengeBundle(bundle)) continue;
          const key = challengeCacheKey(challengeId);
          const applied = applyChallengeBundle(bundle);
          classDataCacheRef.current[key] = applied.classData;
          classNoticeCacheRef.current[key] = applied.normalizationNotice;
          mmdDataCacheRef.current[key] = {
            classes: applied.mmdData,
            parseError: applied.mmdParseError,
          };
          testcaseDataCacheRef.current[key] = applied.testCases;
        }
      }
      if (
        selectedChallengeId
        && hasSessionChallengeScore(challengeScores, selectedChallengeId)
      ) {
        applyDetailCachesToState(challengeCacheKey(selectedChallengeId));
      } else {
        setClassData([]);
        setClassNormalizationNotice(null);
        setMmdData([]);
        setMmdParseError(null);
        setTestCases([]);
      }
    } finally {
      prefetchingDetailsRef.current = false;
      setIsRefreshingResults(false);
    }
    if (labResultHasCompileError(labResult)) {
      showToast([scoreToast, { message: 'Compilation Error', type: 'error' }]);
    } else {
      showToast(scoreToast);
    }
  };

  const studentNavItems = useMemo(() => {
    const items = [];
    if (inCurrentTerm) {
      items.push({ id: 'home', label: 'Home', icon: Home });
    }
    items.push({ id: 'history', label: 'History', icon: Clock });
    if (!IS_DESKTOP_APP && inCurrentTerm) {
      items.push({ id: 'ranking', label: 'Ranking', icon: Trophy });
    }
    return items;
  }, [inCurrentTerm]);

  const activeNav = showRanking ? 'ranking' : showHistory ? 'history' : 'home';

  const handleNavChange = useCallback((navId) => {
    if (navId === 'home' && inCurrentTerm) {
      navigate(ROUTES.studentDashboard);
    } else if (navId === 'history') {
      navigate(ROUTES.studentHistory);
    } else if (navId === 'ranking' && !IS_DESKTOP_APP && inCurrentTerm) {
      navigate(ROUTES.studentRanking);
    }
  }, [inCurrentTerm, navigate]);

  const handleCommand = (cmd) => {
    if (cmd === 'home') {
      navigate(inCurrentTerm ? ROUTES.studentDashboard : ROUTES.studentHistory);
    } else if (cmd === 'history') {
      navigate(ROUTES.studentHistory);
    } else if (cmd === 'ranking' && !IS_DESKTOP_APP) {
      navigate(ROUTES.studentRanking);
    } else if (cmd === 'lecturerArea') {
      navigate(ROUTES.lecturerDashboard);
    } else if (cmd === 'changePassword' && !IS_DESKTOP_APP) {
      setShowChangePassword(true);
    } else if (cmd === 'mascotShop') {
      setShowMascotShop(true);
    }
  };

  const applyShop = useCallback((shop) => {
    if (!shop) return;
    const prev = streakSnapshot.current;
    const next = {
      ...(prev || {}),
      points: shop.points,
      equipped: shop.equipped || 'fox',
    };
    streakSnapshot.current = next;
    setStreak(next);
  }, []);

  if (view === 'dashboard' && !inCurrentTerm) {
    return <Navigate to={ROUTES.studentHistory} replace />;
  }

  const isInitialLoading = isLoadingLabs;

  return (
    <>
      <AppShell
        user={user}
        onLogout={onLogout}
        onCommand={handleCommand}
        showNav
        navItems={studentNavItems}
        navAlign="start"
        navCollapse={false}
        navTrailing={!IS_DESKTOP_APP ? <StudentOfflinePracticeDownload onToast={showToast} /> : null}
        activeNav={activeNav}
        onNavigate={handleNavChange}
        hideSectionLinks
        hideHome={!inCurrentTerm}
        hideChangePassword={IS_DESKTOP_APP}
        showLecturerArea={!IS_DESKTOP_APP && hasRole(user?.roles, 'ADMIN')}
        className={showRanking ? 'flex min-h-0 flex-1 flex-col' : ''}
        headerUserSlot={IS_DESKTOP_APP && !showHistory && !showRanking ? (
          <DesktopRubricPackImport onToast={showToast} />
        ) : null}
        headerBeforeUserMenu={
          <div className="flex shrink-0 items-center gap-1">
            <StudentStreakFlame
              streak={streak?.streak ?? 0}
              loggedIn={Boolean(streak?.loggedIn)}
              submitCount={streak?.submitCount ?? 0}
              submitGoal={streak?.submitGoal ?? 3}
              petCount={streak?.petCount ?? 0}
              petGoal={streak?.petGoal ?? 4}
              completedToday={Boolean(streak?.completedToday)}
              broken={Boolean(streak?.broken)}
              savedStreak={streak?.savedStreak ?? 0}
              canRestore={Boolean(streak?.canRestore)}
              awardPulse={awardPulse}
              restoring={restoringStreak}
              onRestore={restoreStreak}
            />
            <button
              type="button"
              onClick={() => setShowMascotShop(true)}
              aria-label={`Open mascot shop. ${formatNumber(streak?.points ?? 0)} points`}
              className="flex h-10 shrink-0 cursor-pointer items-center gap-1.5 rounded-full border border-border bg-surface px-2.5 text-sm font-semibold text-foreground shadow-sm transition-colors hover:bg-surface-secondary focus:outline-none focus-visible:ring-2 focus-visible:ring-primary"
            >
              <Store className="h-4 w-4 text-warning-text" aria-hidden="true" />
              <span className="tabular-nums">{formatNumber(streak?.points ?? 0)}</span>
            </button>
          </div>
        }
        headerAddon={
          <button
            type="button"
            className="-my-1 shrink-0 rounded-full focus:outline-none focus-visible:ring-2 focus-visible:ring-primary"
            aria-label={`Pet the ${mascotArt(streak?.equipped || 'fox').name}. ${Math.min(streak?.petCount ?? 0, streak?.petGoal ?? 4)} of ${streak?.petGoal ?? 4} pets today.`}
            onClick={petFox}
          >
            <StudentFoxMascot size={56} coverEyes={foxCoverEyes} mascotId={streak?.equipped || 'fox'} />
          </button>
        }
      >
        <div className={showRanking ? 'flex min-h-0 w-full flex-1 flex-col' : 'w-full'}>
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
          ) : showRanking ? (
            <StudentRankingPage user={user} inCurrentTerm={inCurrentTerm} />
          ) : (
            <StudentUI
              user={user}
              quarterName={quarterName}
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
              isLoadingChallenges={isLoadingChallenges}
              isLoadingDetails={isLoadingDetails}
              isRefreshingResults={isRefreshingResults}
              resultsRevealed={resultsRevealed}
              sessionChallengeScores={sessionChallengeScores}
              sessionChallengeBundles={sessionChallengeBundles}
              sessionOverallScore={sessionOverallScore}
              error={labsError || challengesError}
              dailySubmissionQuota={dailySubmissionQuota}
            />
          )}
        </div>
      </AppShell>

      <MascotShop
        open={showMascotShop}
        onClose={() => setShowMascotShop(false)}
        onUpdated={applyShop}
      />

      {!IS_DESKTOP_APP && showChangePassword && (
        <ChangePasswordModal
          isOpen={showChangePassword}
          onClose={() => {
            setFoxCoverEyes(false);
            setShowChangePassword(false);
          }}
          onPasswordFocusChange={setFoxCoverEyes}
          user={user}
        />
      )}
    </>
  );
}