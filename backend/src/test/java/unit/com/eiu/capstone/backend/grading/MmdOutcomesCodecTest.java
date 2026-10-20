package unit.com.eiu.capstone.backend.grading;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.grading.MmdGradingOutcome;
import com.eiu.capstone.backend.grading.MmdOutcomesCodec;

class MmdOutcomesCodecTest {

    @Test
    void roundTrip_preservesPresentTypeOkAndMemberFlags() {
        UUID classId = UUID.randomUUID();
        UUID fieldId = UUID.randomUUID();
        UUID methodId = UUID.randomUUID();
        UUID ctorId = UUID.randomUUID();
        UUID relationId = UUID.randomUUID();

        MmdGradingOutcome original = new MmdGradingOutcome();
        original.setClassPresent(classId, true);
        original.setClass(classId, false);
        original.setField(fieldId, true);
        original.setMethod(methodId, false);
        original.setConstructor(ctorId, true);
        original.setRelation(relationId, false);

        String json = MmdOutcomesCodec.toJson(original);
        assertNotNull(json);
        assertTrue(json.contains("\"v\":1") || json.contains("\"v\": 1"));

        MmdGradingOutcome restored = MmdOutcomesCodec.fromJson(json);
        assertNotNull(restored);
        assertTrue(restored.isClassPresent(classId));
        assertFalse(restored.isClassCorrect(classId));
        assertTrue(restored.isFieldCorrect(fieldId));
        assertFalse(restored.isMethodCorrect(methodId));
        assertTrue(restored.isConstructorCorrect(ctorId));
        assertFalse(restored.isRelationCorrect(relationId));
    }

    @Test
    void fromJson_nullOrBlank_returnsNull() {
        assertNull(MmdOutcomesCodec.fromJson(null));
        assertNull(MmdOutcomesCodec.fromJson(""));
        assertNull(MmdOutcomesCodec.fromJson("   "));
    }

    @Test
    void toJson_nullOutcome_returnsNull() {
        assertNull(MmdOutcomesCodec.toJson(null));
    }

    @Test
    void fromJson_invalidJson_returnsNull() {
        assertNull(MmdOutcomesCodec.fromJson("{not-json"));
    }
}
