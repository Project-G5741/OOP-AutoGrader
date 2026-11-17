package unit.com.eiu.capstone.backend.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.ai.AiStudentCredential;
import com.eiu.capstone.backend.ai.AiTokenBudget;

class AiTokenBudgetTest {

    @Test
    void warnsAtFiftyAndEightyThenStopsAtNinety() {
        AiStudentCredential credential = newCredential(0, 1000);
        List<String> first = AiTokenBudget.applyUsageAndCollectAlerts(credential, 500);
        assertEquals(List.of("50%"), first);
        assertTrue(credential.isNotified50());
        assertFalse(AiTokenBudget.isExhausted(credential.getTokensUsed(), credential.getTokenBudget()));

        List<String> second = AiTokenBudget.applyUsageAndCollectAlerts(credential, 300);
        assertEquals(List.of("80%"), second);
        assertEquals(80, AiTokenBudget.usagePercent(credential.getTokensUsed(), credential.getTokenBudget()));

        List<String> third = AiTokenBudget.applyUsageAndCollectAlerts(credential, 100);
        assertEquals(List.of("90%"), third);
        assertTrue(AiTokenBudget.isExhausted(credential.getTokensUsed(), credential.getTokenBudget()));
    }

    @Test
    void doesNotRepeatAlerts() {
        AiStudentCredential credential = newCredential(900, 1000);
        credential.setNotified50(true);
        credential.setNotified80(true);
        credential.setNotified90(true);
        List<String> alerts = AiTokenBudget.applyUsageAndCollectAlerts(credential, 10);
        assertTrue(alerts.isEmpty());
    }

    private static AiStudentCredential newCredential(long used, long budget) {
        AiStudentCredential credential = new AiStudentCredential();
        credential.setId(UUID.randomUUID());
        credential.setStudentId(UUID.randomUUID());
        credential.setTokensUsed(used);
        credential.setTokenBudget(budget);
        return credential;
    }
}
