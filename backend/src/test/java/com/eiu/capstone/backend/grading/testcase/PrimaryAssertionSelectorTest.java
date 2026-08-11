package com.eiu.capstone.backend.grading.testcase;

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

    private AssertionRubric assertion(AssertionKind kind, int orderIndex) {
        return new AssertionRubric(
                UUID.randomUUID(),
                kind,
                null,
                null,
                null,
                null,
                "\"x\"",
                ComparisonMode.EXACT,
                orderIndex);
    }
}
