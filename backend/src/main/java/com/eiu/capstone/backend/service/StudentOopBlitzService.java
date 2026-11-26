package com.eiu.capstone.backend.service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.DTO.OopBlitzAnswerRequest;
import com.eiu.capstone.backend.DTO.OopBlitzAnswerResponse;
import com.eiu.capstone.backend.DTO.OopBlitzQuestionView;
import com.eiu.capstone.backend.DTO.OopBlitzReviewItemView;
import com.eiu.capstone.backend.DTO.OopBlitzStartResponse;
import com.eiu.capstone.backend.DTO.OopBlitzStatusView;
import com.eiu.capstone.backend.DTO.OopBlitzSummaryView;
import com.eiu.capstone.backend.model.OopBlitzQuestion;
import com.eiu.capstone.backend.model.StudentOopBlitzSession;
import com.eiu.capstone.backend.model.StudentStreak;
import com.eiu.capstone.backend.repository.OopBlitzQuestionRepository;
import com.eiu.capstone.backend.repository.StudentOopBlitzSessionRepository;
import com.eiu.capstone.backend.service.OopBlitzQuestionBank.StoredAnswer;
import com.eiu.capstone.backend.service.OopBlitzQuestionBank.StoredQuestion;
import com.eiu.capstone.backend.utility.TimeUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Server-authoritative state machine and points engine for OOP Blitz.
 */
@Service
@Profile("!desktop & !desktop-lecturer")
public class StudentOopBlitzService {

    private static final TypeReference<List<StoredQuestion>> QUESTION_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<StoredAnswer>> ANSWER_LIST_TYPE = new TypeReference<>() {
    };

    private final StudentStreakService streakService;
    private final StudentOopBlitzSessionRepository sessions;
    private final OopBlitzQuestionRepository customQuestions;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final Supplier<RandomGenerator> rngSupplier;

    @Autowired
    public StudentOopBlitzService(
            StudentStreakService streakService,
            StudentOopBlitzSessionRepository sessions,
            OopBlitzQuestionRepository customQuestions,
            ObjectMapper objectMapper) {
        this(
                streakService,
                sessions,
                customQuestions,
                objectMapper,
                Clock.system(TimeUtil.VIETNAM_ZONE),
                ThreadLocalRandom::current);
    }

    public StudentOopBlitzService(
            StudentStreakService streakService,
            StudentOopBlitzSessionRepository sessions,
            ObjectMapper objectMapper,
            Clock clock,
            Supplier<RandomGenerator> rngSupplier) {
        this(streakService, sessions, null, objectMapper, clock, rngSupplier);
    }

    public StudentOopBlitzService(
            StudentStreakService streakService,
            StudentOopBlitzSessionRepository sessions,
            OopBlitzQuestionRepository customQuestions,
            ObjectMapper objectMapper,
            Clock clock,
            Supplier<RandomGenerator> rngSupplier) {
        this.streakService = streakService;
        this.sessions = sessions;
        this.customQuestions = customQuestions;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.rngSupplier = rngSupplier;
    }

    @Transactional
    public OopBlitzStatusView getStatus(UUID userId) {
        LocalDate today = today();
        OffsetDateTime now = now();
        StudentStreak streak = loadAndRollStreak(userId, today);
        StudentOopBlitzSession active = findValidActiveSession(userId, today, now);
        List<StudentOopBlitzSession> todayRuns = sessions.findByUserIdAndPlayDate(userId, today);
        return buildStatus(streak, todayRuns, active);
    }

    @Transactional
    public OopBlitzStartResponse start(UUID userId, String rawMode) {
        String mode = OopBlitzRules.normalizeMode(rawMode);
        LocalDate today = today();
        OffsetDateTime now = now();

        StudentStreak streak = loadAndRollStreak(userId, today);
        StudentOopBlitzSession active = findValidActiveSession(userId, today, now);

        if (active != null) {
            if (mode.equals(active.getMode())) {
                List<StoredQuestion> questions = readQuestions(active);
                int idx = active.getCurrentIndex();
                if (idx >= 0 && idx < questions.size()) {
                    List<StudentOopBlitzSession> todayRuns = sessions.findByUserIdAndPlayDate(userId, today);
                    OopBlitzQuestionView questionView = toQuestionView(
                            idx, questions.get(idx), remainingOnResumeMs(active, questions.get(idx), idx, now));
                    return new OopBlitzStartResponse(
                            active.getId(),
                            active.getMode(),
                            true,
                            questions.size(),
                            idx,
                            active.getPointsEarned(),
                            active.getCorrectCount(),
                            active.getCurrentCombo(),
                            questionView,
                            buildStatus(streak, todayRuns, active));
                }
                expireSession(active, now);
                active = null;
            } else if (OopBlitzRules.MODE_PRACTICE.equals(active.getMode())) {
                expireSession(active, now);
                active = null;
            } else {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "Finish or abandon your active rewarded run first.");
            }
        }

        List<StudentOopBlitzSession> todayRuns = sessions.findByUserIdAndPlayDate(userId, today);
        if (OopBlitzRules.MODE_REWARDED.equals(mode)) {
            int used = countRewardedRuns(todayRuns);
            int allowed = OopBlitzRules.allowedRewardedRuns(streak.getSubmitCount());
            int earnedToday = sumCreditedToday(todayRuns);
            if (earnedToday >= OopBlitzRules.DAILY_POINTS_CAP) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Daily OOP Blitz points cap reached.");
            }
            if (used >= allowed) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "No rewarded runs left. Submit a Lab or play Practice.");
            }
        }

        List<StudentOopBlitzSession> recentRuns = sessions.findTop12ByUserIdOrderByStartedAtDesc(userId);
        Set<String> avoidKeys = collectUsedQuestionKeys(recentRuns.isEmpty() ? todayRuns : recentRuns);
        List<StoredQuestion> customPool = loadCustomPool();
        List<StoredQuestion> generated = OopBlitzQuestionBank.generateMatch(
                customPool, avoidKeys, rngSupplier.get());

        StudentOopBlitzSession created = new StudentOopBlitzSession();
        created.setId(UUID.randomUUID());
        created.setUserId(userId);
        created.setPlayDate(today);
        created.setMode(mode);
        created.setStatus(OopBlitzRules.STATUS_ACTIVE);
        created.setQuestionsJson(writeJson(generated));
        created.setAnswersJson("[]");
        created.setCurrentIndex(0);
        created.setCorrectCount(0);
        created.setCurrentCombo(0);
        created.setMaxCombo(0);
        created.setSpeedBonusTotal(0);
        created.setPointsEarned(0);
        created.setPointsCredited(0);
        created.setQuestionIssuedAt(now);
        created.setStartedAt(now);
        sessions.save(created);

        List<StudentOopBlitzSession> updatedRuns = new ArrayList<>(todayRuns);
        updatedRuns.add(created);

        StoredQuestion firstQ = generated.get(0);
        OopBlitzQuestionView firstView = toQuestionView(0, firstQ, firstQ.timeLimitSec() * 1000L);
        return new OopBlitzStartResponse(
                created.getId(),
                mode,
                false,
                generated.size(),
                0,
                0,
                0,
                0,
                firstView,
                buildStatus(streak, updatedRuns, created));
    }

    @Transactional
    public OopBlitzAnswerResponse answer(UUID userId, OopBlitzAnswerRequest request) {
        if (request == null || request.sessionId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing OOP Blitz session");
        }
        LocalDate today = today();
        OffsetDateTime now = now();

        StudentOopBlitzSession session = sessions.lockByIdAndUserId(request.sessionId(), userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "OOP Blitz session not found"));
        if (!OopBlitzRules.STATUS_ACTIVE.equals(session.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Session is no longer active.");
        }

        List<StoredQuestion> questions = readQuestions(session);
        int idx = session.getCurrentIndex();
        if (idx < 0 || idx >= questions.size()) {
            expireSession(session, now);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Session is no longer active.");
        }
        if (request.questionIndex() != idx) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Question index mismatch.");
        }

        StoredQuestion question = questions.get(idx);
        Integer selectedIndex = request.selectedIndex();
        int maxChoice = OopBlitzQuestionBank.KIND_BUG.equals(question.kind())
                ? question.codeLines().size()
                : question.options().size();
        if (selectedIndex != null && (selectedIndex < 0 || selectedIndex >= maxChoice)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected answer out of range");
        }

        long rawElapsedMs = session.getQuestionIssuedAt() == null
                ? Long.MAX_VALUE
                : Math.max(0L, Duration.between(session.getQuestionIssuedAt(), now).toMillis());

        boolean timedOut = selectedIndex == null
                || OopBlitzRules.isTimedOut(rawElapsedMs, question.timeLimitSec(), idx);
        boolean correct = !timedOut && selectedIndex == question.correctIndex();

        int basePts = 0;
        int comboBonus = 0;
        int speedBonus = 0;
        int questionPts = 0;

        if (correct) {
            session.setCorrectCount(session.getCorrectCount() + 1);
            int nextCombo = session.getCurrentCombo() + 1;
            session.setCurrentCombo(nextCombo);
            session.setMaxCombo(Math.max(session.getMaxCombo(), nextCombo));

            basePts = OopBlitzRules.basePoints(question.difficulty());
            comboBonus = OopBlitzRules.comboBonus(nextCombo);
            speedBonus = OopBlitzRules.speedBonus(rawElapsedMs, question.timeLimitSec(), idx);
            questionPts = basePts + comboBonus + speedBonus;

            session.setSpeedBonusTotal(session.getSpeedBonusTotal() + speedBonus);
            session.setPointsEarned(Math.min(
                    OopBlitzRules.MAX_POINTS_PER_MATCH,
                    session.getPointsEarned() + questionPts));
        } else {
            session.setCurrentCombo(0);
        }

        List<StoredAnswer> answers = new ArrayList<>(readAnswers(session));
        String chosenLabel = timedOut
                ? "Timed out"
                : choiceLabel(question, selectedIndex);
        String correctLabel = choiceLabel(question, question.correctIndex());
        answers.add(new StoredAnswer(
                idx,
                question.kind(),
                question.difficulty(),
                question.prompt(),
                selectedIndex,
                correct,
                timedOut,
                rawElapsedMs,
                basePts,
                comboBonus,
                speedBonus,
                questionPts,
                chosenLabel,
                correctLabel,
                question.explanation()));

        session.setAnswersJson(writeJson(answers));
        int nextIdx = idx + 1;
        session.setCurrentIndex(nextIdx);

        boolean finished = nextIdx >= questions.size();
        OopBlitzQuestionView nextQuestionView = null;
        OopBlitzSummaryView summary = null;
        OopBlitzStatusView statusView = null;

        if (finished) {
            session.setStatus(OopBlitzRules.STATUS_FINISHED);
            session.setFinishedAt(now);

            StudentStreak streak = loadAndRollStreak(userId, today);
            List<StudentOopBlitzSession> todayRuns = sessions.findByUserIdAndPlayDate(userId, today);

            int credited = 0;
            boolean capped = false;
            if (OopBlitzRules.MODE_REWARDED.equals(session.getMode())) {
                int earnedBefore = 0;
                for (StudentOopBlitzSession run : todayRuns) {
                    if (!session.getId().equals(run.getId()) && OopBlitzRules.MODE_REWARDED.equals(run.getMode())) {
                        earnedBefore += Math.max(0, run.getPointsCredited());
                    }
                }
                int room = Math.max(0, OopBlitzRules.DAILY_POINTS_CAP - earnedBefore);
                credited = Math.min(session.getPointsEarned(), room);
                capped = credited < session.getPointsEarned();
                session.setPointsCredited(credited);
                if (credited > 0) {
                    streak.setPoints(safeAdd(streak.getPoints(), credited));
                    streakService.save(streak);
                }
            }
            sessions.save(session);

            List<StudentOopBlitzSession> statusRuns = mergeRun(todayRuns, session);
            statusView = buildStatus(streak, statusRuns, null);
            summary = new OopBlitzSummaryView(
                    session.getMode(),
                    questions.size(),
                    session.getCorrectCount(),
                    session.getMaxCombo(),
                    session.getSpeedBonusTotal(),
                    session.getPointsEarned(),
                    credited,
                    capped,
                    streak.getPoints(),
                    answers.stream()
                            .map(a -> new OopBlitzReviewItemView(
                                    a.index(),
                                    a.kind(),
                                    a.difficulty(),
                                    a.prompt(),
                                    a.correct(),
                                    a.timedOut(),
                                    a.pointsEarned(),
                                    a.chosenLabel(),
                                    a.correctLabel(),
                                    a.explanation()))
                            .toList());
        } else {
            session.setQuestionIssuedAt(now);
            sessions.save(session);
            StoredQuestion nextQ = questions.get(nextIdx);
            nextQuestionView = toQuestionView(nextIdx, nextQ, nextQ.timeLimitSec() * 1000L);
        }

        return new OopBlitzAnswerResponse(
                idx,
                correct,
                timedOut,
                question.correctIndex(),
                question.explanation(),
                basePts,
                comboBonus,
                speedBonus,
                questionPts,
                session.getCurrentCombo(),
                session.getMaxCombo(),
                session.getPointsEarned(),
                session.getCorrectCount(),
                OopBlitzRules.FEEDBACK_HOLD_MS,
                finished,
                nextQuestionView,
                summary,
                statusView);
    }

    @Transactional
    public OopBlitzStatusView abandon(UUID userId, UUID sessionId) {
        if (sessionId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing OOP Blitz session");
        }
        LocalDate today = today();
        OffsetDateTime now = now();
        StudentOopBlitzSession session = sessions.lockByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "OOP Blitz session not found"));
        if (OopBlitzRules.STATUS_ACTIVE.equals(session.getStatus())) {
            expireSession(session, now);
        }
        StudentStreak streak = loadAndRollStreak(userId, today);
        StudentOopBlitzSession active = findValidActiveSession(userId, today, now);
        List<StudentOopBlitzSession> todayRuns = sessions.findByUserIdAndPlayDate(userId, today);
        return buildStatus(streak, todayRuns, active);
    }

    private StudentStreak loadAndRollStreak(UUID userId, LocalDate today) {
        StudentStreak row = streakService.lock(userId);
        LocalDate beforeDate = row.getMissionDate();
        DailyStreakRules.roll(row, today);
        if (!today.equals(beforeDate)) {
            streakService.save(row);
        }
        return row;
    }

    private StudentOopBlitzSession findValidActiveSession(UUID userId, LocalDate today, OffsetDateTime now) {
        StudentOopBlitzSession active = sessions.findFirstByUserIdAndStatus(userId, OopBlitzRules.STATUS_ACTIVE)
                .orElse(null);
        if (active == null) {
            return null;
        }
        if (isStale(active, today, now)) {
            expireSession(active, now);
            return null;
        }
        return active;
    }

    private boolean isStale(StudentOopBlitzSession active, LocalDate today, OffsetDateTime now) {
        if (active.getPlayDate() == null || active.getPlayDate().isBefore(today)) {
            return true;
        }
        if (active.getQuestionIssuedAt() == null) {
            return true;
        }
        return Duration.between(active.getQuestionIssuedAt(), now).toMinutes() >= OopBlitzRules.SESSION_STALE_MINUTES;
    }

    private void expireSession(StudentOopBlitzSession session, OffsetDateTime now) {
        session.setStatus(OopBlitzRules.STATUS_EXPIRED);
        session.setFinishedAt(now);
        sessions.saveAndFlush(session);
    }

    private OopBlitzStatusView buildStatus(
            StudentStreak streak,
            List<StudentOopBlitzSession> todayRuns,
            StudentOopBlitzSession active) {
        int used = countRewardedRuns(todayRuns);
        int allowed = OopBlitzRules.allowedRewardedRuns(streak.getSubmitCount());
        int earnedToday = sumCreditedToday(todayRuns);
        Integer bestToday = findTodayBestScore(todayRuns);
        return new OopBlitzStatusView(
                used,
                allowed,
                OopBlitzRules.MAX_DAILY_REWARDED_RUNS,
                earnedToday,
                OopBlitzRules.DAILY_POINTS_CAP,
                streak.getSubmitCount(),
                bestToday,
                active == null ? null : active.getId(),
                active == null ? null : active.getMode(),
                active == null ? null : active.getCurrentIndex(),
                OopBlitzRules.QUESTIONS_PER_MATCH,
                streak.getPoints());
    }

    private static int countRewardedRuns(List<StudentOopBlitzSession> runs) {
        int count = 0;
        for (StudentOopBlitzSession run : runs) {
            if (OopBlitzRules.MODE_REWARDED.equals(run.getMode())) {
                count++;
            }
        }
        return count;
    }

    private static int sumCreditedToday(List<StudentOopBlitzSession> runs) {
        int total = 0;
        for (StudentOopBlitzSession run : runs) {
            if (OopBlitzRules.MODE_REWARDED.equals(run.getMode())) {
                total += Math.max(0, run.getPointsCredited());
            }
        }
        return total;
    }

    private static Integer findTodayBestScore(List<StudentOopBlitzSession> runs) {
        Integer best = null;
        for (StudentOopBlitzSession run : runs) {
            if (OopBlitzRules.MODE_REWARDED.equals(run.getMode())
                    && OopBlitzRules.STATUS_FINISHED.equals(run.getStatus())) {
                best = best == null ? run.getPointsEarned() : Math.max(best, run.getPointsEarned());
            }
        }
        return best;
    }

    private List<StoredQuestion> loadCustomPool() {
        if (customQuestions == null) {
            return List.of();
        }
        List<OopBlitzQuestion> rows = customQuestions.findByActiveTrueOrderByUpdatedAtDesc();
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<StoredQuestion> pool = new ArrayList<>(rows.size());
        for (OopBlitzQuestion row : rows) {
            pool.add(LecturerOopBlitzQuestionService.toStoredQuestion(row, objectMapper));
        }
        return pool;
    }

    private Set<String> collectUsedQuestionKeys(List<StudentOopBlitzSession> runs) {
        Set<String> keys = new HashSet<>();
        for (StudentOopBlitzSession run : runs) {
            for (StoredQuestion q : readQuestions(run)) {
                if (q.key() != null) {
                    keys.add(q.key());
                }
            }
        }
        return keys;
    }

    private static long remainingOnResumeMs(
            StudentOopBlitzSession active,
            StoredQuestion question,
            int idx,
            OffsetDateTime now) {
        long limitMs = question.timeLimitSec() * 1000L;
        if (active.getQuestionIssuedAt() == null) {
            return limitMs;
        }
        long elapsedMs = Math.max(0L, Duration.between(active.getQuestionIssuedAt(), now).toMillis());
        long remaining = limitMs + OopBlitzRules.holdOffsetMs(idx) - elapsedMs;
        return Math.max(0L, Math.min(limitMs, remaining));
    }

    private static OopBlitzQuestionView toQuestionView(int index, StoredQuestion q, long remainingMs) {
        return new OopBlitzQuestionView(
                index,
                q.kind(),
                q.difficulty(),
                q.timeLimitSec(),
                remainingMs,
                q.prompt(),
                q.codeLines(),
                q.options());
    }

    private static String choiceLabel(StoredQuestion q, int idx) {
        if (OopBlitzQuestionBank.KIND_BUG.equals(q.kind())) {
            return "Line " + (idx + 1);
        }
        if (idx >= 0 && idx < q.options().size()) {
            return q.options().get(idx);
        }
        return "?";
    }

    private static List<StudentOopBlitzSession> mergeRun(
            List<StudentOopBlitzSession> todayRuns,
            StudentOopBlitzSession updated) {
        List<StudentOopBlitzSession> merged = new ArrayList<>(todayRuns.size() + 1);
        boolean replaced = false;
        for (StudentOopBlitzSession run : todayRuns) {
            if (updated.getId().equals(run.getId())) {
                merged.add(updated);
                replaced = true;
            } else {
                merged.add(run);
            }
        }
        if (!replaced) {
            merged.add(updated);
        }
        return merged;
    }

    private List<StoredQuestion> readQuestions(StudentOopBlitzSession session) {
        try {
            return objectMapper.readValue(session.getQuestionsJson(), QUESTION_LIST_TYPE);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Corrupt OOP Blitz question payload", ex);
        }
    }

    private List<StoredAnswer> readAnswers(StudentOopBlitzSession session) {
        String raw = session.getAnswersJson();
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(raw, ANSWER_LIST_TYPE);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Corrupt OOP Blitz answer payload", ex);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize OOP Blitz payload", ex);
        }
    }

    private static int safeAdd(int current, int delta) {
        long next = (long) current + delta;
        return next > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) next;
    }

    private LocalDate today() {
        return LocalDate.now(clock.withZone(TimeUtil.VIETNAM_ZONE));
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock.withZone(TimeUtil.VIETNAM_ZONE));
    }
}
