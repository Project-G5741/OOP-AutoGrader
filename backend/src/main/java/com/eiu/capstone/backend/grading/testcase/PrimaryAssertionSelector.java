package com.eiu.capstone.backend.grading.testcase;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.eiu.capstone.backend.grading.rubric.AssertionRubric;
import com.eiu.capstone.backend.model.AssertionKind;

@Component
public class PrimaryAssertionSelector {

    private static final List<AssertionKind> PRIORITY = List.of(
            AssertionKind.STDOUT,
            AssertionKind.RETURN_VALUE,
            AssertionKind.FIELD_STATE,
            AssertionKind.EXCEPTION,
            AssertionKind.COMPARISON_RESULT);

    public AssertionRubric select(List<AssertionRubric> assertions) {
        if (assertions == null || assertions.isEmpty()) {
            return null;
        }
        for (AssertionKind kind : PRIORITY) {
            for (AssertionRubric assertion : assertions) {
                if (assertion.kind() == kind) {
                    return assertion;
                }
            }
        }
        return assertions.stream()
                .min(Comparator.comparingInt(AssertionRubric::orderIndex))
                .orElse(assertions.get(0));
    }
}
