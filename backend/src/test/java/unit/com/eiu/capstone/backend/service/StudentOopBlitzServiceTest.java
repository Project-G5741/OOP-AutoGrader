package unit.com.eiu.capstone.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.DTO.OopBlitzAnswerRequest;
import com.eiu.capstone.backend.DTO.OopBlitzAnswerResponse;
import com.eiu.capstone.backend.DTO.OopBlitzStartResponse;
import com.eiu.capstone.backend.DTO.OopBlitzStatusView;
import com.eiu.capstone.backend.model.OopBlitzQuestion;
import com.eiu.capstone.backend.model.StudentOopBlitzSession;
import com.eiu.capstone.backend.model.StudentStreak;
import com.eiu.capstone.backend.repository.OopBlitzQuestionRepository;
import com.eiu.capstone.backend.repository.StudentOopBlitzSessionRepository;
import com.eiu.capstone.backend.repository.StudentStreakRepository;
import com.eiu.capstone.backend.service.OopBlitzQuestionBank.StoredQuestion;
import com.eiu.capstone.backend.service.OopBlitzRules;
import com.eiu.capstone.backend.service.StudentMascotService;
import com.eiu.capstone.backend.service.StudentOopBlitzService;
import com.eiu.capstone.backend.service.StudentStreakService;
import com.eiu.capstone.backend.utility.TimeUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

@ExtendWith(MockitoExtension.class)
class StudentOopBlitzServiceTest {

    @Mock
    private StudentStreakRepository streakRepository;
    @Mock
    private EntityManager entityManager;
    @Mock
    private StudentMascotService mascotService;
    @Mock
    private StudentOopBlitzSessionRepository sessionRepository;
    @Mock
    private OopBlitzQuestionRepository customQuestionRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UUID userId = UUID.randomUUID();

    private MutableClock clock;
    private StudentStreak streakRow;
    private Map<UUID, StudentOopBlitzSession> sessionStore;
    private StudentOopBlitzService blitzService;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-11-25T03:00:00Z"), TimeUtil.VIETNAM_ZONE);
        LocalDate today = LocalDate.now(clock);

        streakRow = new StudentStreak();
        streakRow.setUserId(userId);
        streakRow.setMissionDate(today);
        streakRow.setPoints(200);
        streakRow.setSubmitCount(0);

        sessionStore = new ConcurrentHashMap<>();

        Query nativeQuery = org.mockito.Mockito.mock(Query.class);
        lenient().when(entityManager.createNativeQuery(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(nativeQuery);
        lenient().when(nativeQuery.setParameter(org.mockito.ArgumentMatchers.anyInt(), any()))
                .thenReturn(nativeQuery);
        lenient().when(nativeQuery.executeUpdate()).thenReturn(1);

        lenient().when(streakRepository.lockByUserId(userId)).thenReturn(Optional.of(streakRow));
        lenient().when(streakRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        lenient().when(sessionRepository.save(any())).thenAnswer(inv -> {
            StudentOopBlitzSession s = inv.getArgument(0);
            sessionStore.put(s.getId(), s);
            return s;
        });
        lenient().when(sessionRepository.saveAndFlush(any())).thenAnswer(inv -> {
            StudentOopBlitzSession s = inv.getArgument(0);
            sessionStore.put(s.getId(), s);
            return s;
        });
        lenient().when(sessionRepository.findFirstByUserIdAndStatus(eq(userId), eq(OopBlitzRules.STATUS_ACTIVE)))
                .thenAnswer(inv -> sessionStore.values().stream()
                        .filter(s -> userId.equals(s.getUserId()) && OopBlitzRules.STATUS_ACTIVE.equals(s.getStatus()))
                        .findFirst());
        lenient().when(sessionRepository.lockByIdAndUserId(any(), eq(userId)))
                .thenAnswer(inv -> {
                    UUID id = inv.getArgument(0);
                    StudentOopBlitzSession s = sessionStore.get(id);
                    return s != null && userId.equals(s.getUserId()) ? Optional.of(s) : Optional.empty();
                });
        lenient().when(sessionRepository.findByUserIdAndPlayDate(eq(userId), any()))
                .thenAnswer(inv -> {
                    LocalDate date = inv.getArgument(1);
                    return sessionStore.values().stream()
                            .filter(s -> userId.equals(s.getUserId()) && date.equals(s.getPlayDate()))
                            .toList();
                });
        lenient().when(sessionRepository.findTop12ByUserIdOrderByStartedAtDesc(eq(userId)))
                .thenAnswer(inv -> sessionStore.values().stream()
                        .filter(s -> userId.equals(s.getUserId()))
                        .sorted((a, b) -> b.getStartedAt().compareTo(a.getStartedAt()))
                        .limit(12)
                        .toList());
        lenient().when(customQuestionRepository.findByActiveTrueOrderByUpdatedAtDesc())
                .thenReturn(List.of());

        StudentStreakService streakService = new StudentStreakService(streakRepository, entityManager, mascotService);
        blitzService = new StudentOopBlitzService(
                streakService,
                sessionRepository,
                customQuestionRepository,
                objectMapper,
                clock,
                () -> new Random(42L));
    }

    @Test
    void getStatus_learnToUnlockScalesWithTodaySubmits() {
        streakRow.setSubmitCount(0);
        assertEquals(1, blitzService.getStatus(userId).rewardedRunsAllowed());

        streakRow.setSubmitCount(1);
        assertEquals(2, blitzService.getStatus(userId).rewardedRunsAllowed());

        streakRow.setSubmitCount(2);
        assertEquals(3, blitzService.getStatus(userId).rewardedRunsAllowed());

        streakRow.setSubmitCount(5);
        assertEquals(3, blitzService.getStatus(userId).rewardedRunsAllowed());
    }

    @Test
    void fullPerfectFastRuns_credits100PerMatchAndEnforces250DailyCap() throws Exception {
        streakRow.setSubmitCount(2); // Unlock all 3 daily runs

        OopBlitzAnswerResponse run1End = playPerfectFastMatch();
        assertTrue(run1End.finished());
        assertEquals(100, run1End.summary().rawPoints());
        assertEquals(100, run1End.summary().creditedPoints());
        assertFalse(run1End.summary().cappedByDailyLimit());
        assertEquals(300, streakRow.getPoints());

        OopBlitzAnswerResponse run2End = playPerfectFastMatch();
        assertEquals(100, run2End.summary().creditedPoints());
        assertFalse(run2End.summary().cappedByDailyLimit());
        assertEquals(400, streakRow.getPoints());

        OopBlitzAnswerResponse run3End = playPerfectFastMatch();
        assertEquals(100, run3End.summary().rawPoints());
        assertEquals(50, run3End.summary().creditedPoints());
        assertTrue(run3End.summary().cappedByDailyLimit());
        assertEquals(450, streakRow.getPoints());
        assertEquals(250, run3End.status().pointsEarnedToday());
        assertEquals(3, run3End.status().rewardedRunsUsed());
    }

    @Test
    void start_rejectsWhenOutOfRewardedRunsOrDailyCapReached() throws Exception {
        streakRow.setSubmitCount(0); // Only 1 free run
        playPerfectFastMatch();

        ResponseStatusException locked = assertThrows(
                ResponseStatusException.class,
                () -> blitzService.start(userId, OopBlitzRules.MODE_REWARDED));
        assertEquals(HttpStatus.CONFLICT, locked.getStatusCode());

        // Unlock another submit, exhaust 250 daily cap manually via credited runs, then verify cap guard
        streakRow.setSubmitCount(2);
        StudentOopBlitzSession capFill = new StudentOopBlitzSession();
        capFill.setId(UUID.randomUUID());
        capFill.setUserId(userId);
        capFill.setPlayDate(LocalDate.now(clock));
        capFill.setMode(OopBlitzRules.MODE_REWARDED);
        capFill.setStatus(OopBlitzRules.STATUS_FINISHED);
        capFill.setQuestionsJson("[]");
        capFill.setAnswersJson("[]");
        capFill.setPointsEarned(150);
        capFill.setPointsCredited(150);
        capFill.setStartedAt(java.time.OffsetDateTime.now(clock));
        sessionStore.put(capFill.getId(), capFill);

        ResponseStatusException capped = assertThrows(
                ResponseStatusException.class,
                () -> blitzService.start(userId, OopBlitzRules.MODE_REWARDED));
        assertEquals(HttpStatus.CONFLICT, capped.getStatusCode());
    }

    @Test
    void answer_enforcesServerTimerAndResetsCombo() throws Exception {
        OopBlitzStartResponse started = blitzService.start(userId, OopBlitzRules.MODE_REWARDED);
        List<StoredQuestion> qs = readStoredQuestions(started.sessionId());

        // Q0 answered in 2s -> correct + speed bonus (+8)
        clock.advance(Duration.ofSeconds(2));
        OopBlitzAnswerResponse q0 = blitzService.answer(
                userId, new OopBlitzAnswerRequest(started.sessionId(), 0, qs.get(0).correctIndex()));
        assertTrue(q0.correct());
        assertFalse(q0.timedOut());
        assertEquals(8, q0.questionPts());
        assertEquals(1, q0.combo());

        // Q1 answered in 19.5s (> 15s + 1.8s hold + 2s grace = 18.8s) -> forced timeout even with correct index!
        clock.advance(Duration.ofMillis(19_500));
        OopBlitzAnswerResponse q1 = blitzService.answer(
                userId, new OopBlitzAnswerRequest(started.sessionId(), 1, qs.get(1).correctIndex()));
        assertFalse(q1.correct());
        assertTrue(q1.timedOut());
        assertEquals(0, q1.questionPts());
        assertEquals(0, q1.combo());
    }

    @Test
    void answer_rejectsIndexMismatchAndFinishedReplay() throws Exception {
        OopBlitzStartResponse started = blitzService.start(userId, OopBlitzRules.MODE_REWARDED);

        ResponseStatusException mismatch = assertThrows(
                ResponseStatusException.class,
                () -> blitzService.answer(userId, new OopBlitzAnswerRequest(started.sessionId(), 1, 0)));
        assertEquals(HttpStatus.CONFLICT, mismatch.getStatusCode());

        List<StoredQuestion> qs = readStoredQuestions(started.sessionId());
        for (int i = 0; i < qs.size(); i++) {
            clock.advance(Duration.ofSeconds(3));
            blitzService.answer(userId, new OopBlitzAnswerRequest(started.sessionId(), i, qs.get(i).correctIndex()));
        }

        ResponseStatusException replay = assertThrows(
                ResponseStatusException.class,
                () -> blitzService.answer(userId, new OopBlitzAnswerRequest(started.sessionId(), 7, 0)));
        assertEquals(HttpStatus.CONFLICT, replay.getStatusCode());
    }

    @Test
    void start_resumesActiveSessionAndAbandonMarksExpired() throws Exception {
        OopBlitzStartResponse first = blitzService.start(userId, OopBlitzRules.MODE_REWARDED);
        List<StoredQuestion> qs = readStoredQuestions(first.sessionId());
        clock.advance(Duration.ofSeconds(2));
        blitzService.answer(userId, new OopBlitzAnswerRequest(first.sessionId(), 0, qs.get(0).correctIndex()));

        // Resuming returns the exact same sessionId and advances to Q1 without burning another slot
        OopBlitzStartResponse resumed = blitzService.start(userId, OopBlitzRules.MODE_REWARDED);
        assertTrue(resumed.resumed());
        assertEquals(first.sessionId(), resumed.sessionId());
        assertEquals(1, resumed.currentIndex());
        assertEquals(1, resumed.status().rewardedRunsUsed());

        // Starting Practice while Rewarded is active is rejected
        ResponseStatusException practiceBlocked = assertThrows(
                ResponseStatusException.class,
                () -> blitzService.start(userId, OopBlitzRules.MODE_PRACTICE));
        assertEquals(HttpStatus.CONFLICT, practiceBlocked.getStatusCode());

        // Abandon expires the session while keeping the daily slot consumed
        OopBlitzStatusView afterAbandon = blitzService.abandon(userId, first.sessionId());
        assertNull(afterAbandon.activeSessionId());
        assertEquals(1, afterAbandon.rewardedRunsUsed());
        assertEquals(OopBlitzRules.STATUS_EXPIRED, sessionStore.get(first.sessionId()).getStatus());
    }

    @Test
    void practiceMode_worksWhenRewardedExhaustedAndCreditsZeroPoints() throws Exception {
        streakRow.setSubmitCount(0);
        playPerfectFastMatch();
        int walletBeforePractice = streakRow.getPoints();

        OopBlitzStartResponse practice = blitzService.start(userId, OopBlitzRules.MODE_PRACTICE);
        assertEquals(OopBlitzRules.MODE_PRACTICE, practice.mode());
        List<StoredQuestion> qs = readStoredQuestions(practice.sessionId());

        OopBlitzAnswerResponse last = null;
        for (int i = 0; i < qs.size(); i++) {
            clock.advance(Duration.ofSeconds(3));
            last = blitzService.answer(
                    userId, new OopBlitzAnswerRequest(practice.sessionId(), i, qs.get(i).correctIndex()));
        }
        assertNotNull(last);
        assertTrue(last.finished());
        assertEquals(0, last.summary().creditedPoints());
        assertEquals(walletBeforePractice, streakRow.getPoints());
    }

    @Test
    void getStatus_autoExpiresStaleSessionAfter15Minutes() {
        OopBlitzStartResponse started = blitzService.start(userId, OopBlitzRules.MODE_REWARDED);
        assertNotNull(blitzService.getStatus(userId).activeSessionId());

        clock.advance(Duration.ofMinutes(16));
        OopBlitzStatusView status = blitzService.getStatus(userId);
        assertNull(status.activeSessionId());
        assertEquals(OopBlitzRules.STATUS_EXPIRED, sessionStore.get(started.sessionId()).getStatus());
    }

    @Test
    void start_avoidsRecentPreviousDayKeysAndLoadsActiveCustomQuestions() throws Exception {
        OopBlitzStartResponse day1 = blitzService.start(userId, OopBlitzRules.MODE_REWARDED);
        List<StoredQuestion> day1Questions = readStoredQuestions(day1.sessionId());
        for (int i = 0; i < day1Questions.size(); i++) {
            clock.advance(Duration.ofSeconds(3));
            blitzService.answer(userId, new OopBlitzAnswerRequest(day1.sessionId(), i, day1Questions.get(i).correctIndex()));
        }

        clock.advance(Duration.ofHours(25));
        OopBlitzQuestion custom = new OopBlitzQuestion();
        custom.setId(UUID.randomUUID());
        custom.setKind("OUTPUT");
        custom.setDifficulty("MEDIUM");
        custom.setPrompt("Custom midterm check");
        custom.setCodeLines(objectMapper.writeValueAsString(List.of("System.out.println(99);")));
        custom.setOptionsJson(objectMapper.writeValueAsString(List.of("99", "98", "0", "Compile error")));
        custom.setCorrectIndex(0);
        custom.setExplanation("Prints 99.");
        custom.setActive(true);
        when(customQuestionRepository.findByActiveTrueOrderByUpdatedAtDesc()).thenReturn(List.of(custom));

        OopBlitzStartResponse day2 = blitzService.start(userId, OopBlitzRules.MODE_REWARDED);
        List<StoredQuestion> day2Questions = readStoredQuestions(day2.sessionId());
        List<String> day1Keys = day1Questions.stream().map(StoredQuestion::key).toList();
        long repeated = day2Questions.stream().filter(q -> day1Keys.contains(q.key())).count();
        assertEquals(0, repeated, "Day 2 run should avoid repeating keys from day 1 via recent-run window");
    }

    private OopBlitzAnswerResponse playPerfectFastMatch() throws Exception {
        OopBlitzStartResponse started = blitzService.start(userId, OopBlitzRules.MODE_REWARDED);
        List<StoredQuestion> qs = readStoredQuestions(started.sessionId());
        OopBlitzAnswerResponse last = null;
        for (int i = 0; i < qs.size(); i++) {
            // Q0: 1.5s, Q1..Q7: 3.0s (1.8s hold + 1.2s solve) -> always earns speed bonus
            clock.advance(Duration.ofMillis(i == 0 ? 1500 : 3000));
            last = blitzService.answer(
                    userId, new OopBlitzAnswerRequest(started.sessionId(), i, qs.get(i).correctIndex()));
        }
        return last;
    }

    private List<StoredQuestion> readStoredQuestions(UUID sessionId) throws Exception {
        StudentOopBlitzSession row = sessionStore.get(sessionId);
        return new ArrayList<>(objectMapper.readValue(row.getQuestionsJson(), new TypeReference<>() {
        }));
    }

    private static final class MutableClock extends Clock {
        private Instant instant;
        private final ZoneId zone;

        MutableClock(Instant instant, ZoneId zone) {
            this.instant = instant;
            this.zone = zone;
        }

        void advance(Duration delta) {
            instant = instant.plus(delta);
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
