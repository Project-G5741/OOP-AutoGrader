package com.eiu.capstone.backend.grading.testcase;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.eiu.capstone.backend.grading.rubric.AssertionRubric;
import com.eiu.capstone.backend.grading.rubric.InvocationRubric;
import com.eiu.capstone.backend.model.AssertionKind;
import com.eiu.capstone.backend.model.TestcaseResultStatus;

@Component
public class PrimaryAssertionSelector {

    private static final List<AssertionKind> PRIORITY = List.of(
            AssertionKind.STDOUT,
            AssertionKind.RETURN_VALUE,
            AssertionKind.FIELD_STATE,
            AssertionKind.EXCEPTION);

    public AssertionRubric select(List<AssertionRubric> assertions) {
        if (assertions == null || assertions.isEmpty()) {
            return null;
        }
        for (AssertionKind kind : PRIORITY) {
            AssertionRubric match = assertions.stream()
                    .filter(assertion -> assertion.kind() == kind)
                    .min(Comparator.comparingInt(AssertionRubric::orderIndex))
                    .orElse(null);
            if (match != null) {
                return match;
            }
        }
        return assertions.stream()
                .min(Comparator.comparingInt(AssertionRubric::orderIndex))
                .orElse(assertions.get(0));
    }

    public AssertionRubric select(List<AssertionRubric> assertions, UUID invocationId) {
        if (invocationId == null) {
            return select(assertions);
        }
        List<AssertionRubric> scoped = boundTo(assertions, invocationId, false);
        return select(scoped.isEmpty() ? assertions : scoped);
    }

    public AssertionRubric selectScenarioPrimary(List<InvocationRubric> steps,
                                                 List<AssertionRubric> assertions,
                                                 Map<UUID, TestcaseResultStatus> statusById) {
        if (steps == null || steps.isEmpty()) {
            return select(assertions);
        }
        InvocationRubric lastRun = null;
        boolean singleStep = steps.size() == 1;
        for (InvocationRubric step : steps) {
            List<AssertionRubric> onStep = boundTo(assertions, step.id(), singleStep);
            List<AssertionRubric> failed = new ArrayList<>();
            boolean ran = false;
            for (AssertionRubric assertion : onStep) {
                TestcaseResultStatus status = statusById == null ? null : statusById.get(assertion.id());
                if (status == null || status == TestcaseResultStatus.SKIPPED) {
                    continue;
                }
                ran = true;
                if (status == TestcaseResultStatus.FAILED || status == TestcaseResultStatus.ERROR) {
                    failed.add(assertion);
                }
            }
            if (ran) {
                lastRun = step;
            }
            if (!failed.isEmpty()) {
                return select(failed);
            }
        }
        if (lastRun != null) {
            return select(boundTo(assertions, lastRun.id(), singleStep));
        }
        return select(assertions);
    }

    public static List<AssertionRubric> boundTo(List<AssertionRubric> assertions, UUID stepId, boolean singleStep) {
        if (assertions == null || assertions.isEmpty() || stepId == null) {
            return List.of();
        }
        List<AssertionRubric> bound = new ArrayList<>();
        for (AssertionRubric assertion : assertions) {
            if (stepId.equals(assertion.invocationId())
                    || (assertion.invocationId() == null && singleStep)) {
                bound.add(assertion);
            }
        }
        return bound;
    }
}
