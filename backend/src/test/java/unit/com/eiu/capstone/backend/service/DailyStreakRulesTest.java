package unit.com.eiu.capstone.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.model.StudentStreak;
import com.eiu.capstone.backend.service.DailyStreakRules;

class DailyStreakRulesTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 20);

    @Test
    void fourthPetFinishesThatMissionOnly() {
        StudentStreak row = new StudentStreak();
        row.setPetCount(DailyStreakRules.PET_GOAL);
        assertTrue(DailyStreakRules.missionJustFinished(false, 0, DailyStreakRules.PET_GOAL - 1, row));
        assertFalse(DailyStreakRules.missionJustFinished(true, 0, DailyStreakRules.PET_GOAL, row));
    }

    @Test
    void rewardGrowsByFiveOnEachFifthDay() {
        assertEquals(50, DailyStreakRules.rewardFor(1));
        assertEquals(50, DailyStreakRules.rewardFor(4));
        assertEquals(55, DailyStreakRules.rewardFor(5));
        assertEquals(55, DailyStreakRules.rewardFor(6));
        assertEquals(60, DailyStreakRules.rewardFor(10));
        assertEquals(65, DailyStreakRules.rewardFor(15));
        assertEquals(150, DailyStreakRules.rewardFor(101));
    }

    @Test
    void completingMissionsAwardsOnce() {
        StudentStreak row = ready(TODAY);
        assertEquals(50, DailyStreakRules.awardIfReady(row, TODAY));
        assertEquals(1, row.getStreakCount());
        assertEquals(50, row.getPoints());
        assertEquals(0, DailyStreakRules.awardIfReady(row, TODAY));
        assertEquals(50, row.getPoints());
    }

    @Test
    void fifthDayAddsTheMilestoneBonus() {
        StudentStreak row = ready(TODAY);
        row.setStreakCount(4);
        row.setLastCompletedOn(TODAY.minusDays(1));
        assertEquals(55, DailyStreakRules.awardIfReady(row, TODAY));
        assertEquals(5, row.getStreakCount());
    }

    @Test
    void missedDayBreaksTheStreakUntilRestore() {
        StudentStreak row = new StudentStreak();
        row.setStreakCount(7);
        row.setLastCompletedOn(TODAY.minusDays(2));
        row.setMissionDate(TODAY.minusDays(1));
        DailyStreakRules.roll(row, TODAY);
        assertTrue(row.isStreakBroken());
        assertEquals(7, row.getSavedStreak());
        assertEquals(0, row.getStreakCount());
        assertTrue(DailyStreakRules.canRestore(row, TODAY));

        fillMissions(row);
        assertEquals(50, DailyStreakRules.awardIfReady(row, TODAY));
        assertEquals(1, row.getStreakCount());
        assertFalse(row.isStreakBroken());
        assertFalse(DailyStreakRules.canRestore(row, TODAY));
    }

    @Test
    void restoreContinuesTheSavedStreakOncePerMonth() {
        StudentStreak row = new StudentStreak();
        row.setStreakCount(7);
        row.setPoints(100);
        row.setLastCompletedOn(TODAY.minusDays(2));
        row.setMissionDate(TODAY.minusDays(1));
        DailyStreakRules.roll(row, TODAY);
        fillMissions(row);
        DailyStreakRules.restore(row, TODAY);
        assertEquals(55, DailyStreakRules.awardIfReady(row, TODAY));
        assertEquals(8, row.getStreakCount());
        assertEquals(155, row.getPoints());
        assertEquals("2026-10", row.getRestoreYearMonth());

        row.setStreakBroken(true);
        row.setSavedStreak(8);
        assertFalse(DailyStreakRules.canRestore(row, TODAY));
        assertThrows(IllegalStateException.class, () -> DailyStreakRules.restore(row, TODAY));
    }

    @Test
    void yesterdayKeepsTheStreak() {
        StudentStreak row = new StudentStreak();
        row.setStreakCount(3);
        row.setLastCompletedOn(TODAY.minusDays(1));
        row.setMissionDate(TODAY.minusDays(1));
        row.setPetCount(4);
        DailyStreakRules.roll(row, TODAY);
        assertFalse(row.isStreakBroken());
        assertEquals(3, row.getStreakCount());
        assertEquals(0, row.getPetCount());
    }

    private static StudentStreak ready(LocalDate today) {
        StudentStreak row = new StudentStreak();
        row.setMissionDate(today);
        fillMissions(row);
        return row;
    }

    private static void fillMissions(StudentStreak row) {
        row.setLoggedIn(true);
        row.setSubmitCount(DailyStreakRules.SUBMIT_GOAL);
        row.setPetCount(DailyStreakRules.PET_GOAL);
    }
}
