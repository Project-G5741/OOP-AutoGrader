package unit.com.eiu.capstone.backend.grading.testcase;

import com.eiu.capstone.backend.grading.testcase.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.grading.rubric.AssertionRubric;
import com.eiu.capstone.backend.model.AssertionKind;
import com.eiu.capstone.backend.model.ComparisonMode;

class PrimaryAssertionSelectorTest {

    private final PrimaryAssertionSelector selector = new PrimaryAssertionSelector();

    @Test
    void selectsLowestOrderIndexWithinPriorityKind() {
        List<AssertionRubric> assertions = List.of(
                assertion(AssertionKind.FIELD_STATE, 2),
                assertion(AssertionKind.STDOUT, 5),
                assertion(AssertionKind.STDOUT, 1));

        AssertionRubric primary = selector.select(assertions);

        assertEquals(AssertionKind.STDOUT, primary.kind());
        assertEquals(1, primary.orderIndex());
    }

    @Test
    void selectByInvocationIdIgnoresLaterKindPriority() {
        UUID firstStep = UUID.randomUUID();
        UUID secondStep = UUID.randomUUID();
        AssertionRubric returnValue = assertion(AssertionKind.RETURN_VALUE, 0, firstStep);
        AssertionRubric stdout = assertion(AssertionKind.STDOUT, 1, secondStep);

        AssertionRubric primary = selector.select(List.of(returnValue, stdout), firstStep);

        assertEquals(AssertionKind.RETURN_VALUE, primary.kind());
    }

    private AssertionRubric assertion(AssertionKind kind, int orderIndex) {
        return assertion(kind, orderIndex, null);
    }

    private AssertionRubric assertion(AssertionKind kind, int orderIndex, UUID invocationId) {
        return new AssertionRubric(
                UUID.randomUUID(),
                kind,
                invocationId,
                null,
                null,
                null,
                "\"x\"",
                ComparisonMode.EXACT,
                orderIndex);
    }
}
