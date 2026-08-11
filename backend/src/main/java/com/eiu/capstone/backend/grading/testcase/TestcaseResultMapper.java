package com.eiu.capstone.backend.grading.testcase;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.eiu.capstone.backend.DTO.TestcaseAssertionResultDTO;
import com.eiu.capstone.backend.DTO.TestcaseResultDTO;
import com.eiu.capstone.backend.grading.LabResultAssembler;
import com.eiu.capstone.backend.grading.rubric.AssertionRubric;
import com.eiu.capstone.backend.grading.rubric.TestcaseRubric;
import com.eiu.capstone.backend.model.SubmissionTestcaseAssertionResult;
import com.eiu.capstone.backend.model.SubmissionTestcaseResult;
import com.eiu.capstone.backend.model.TestcaseResultStatus;

@Component
public class TestcaseResultMapper {

    private final TestcaseDisplayFormatter displayFormatter;
    private final PrimaryAssertionSelector primaryAssertionSelector;

    public TestcaseResultMapper(TestcaseDisplayFormatter displayFormatter,
                                PrimaryAssertionSelector primaryAssertionSelector) {
        this.displayFormatter = displayFormatter;
        this.primaryAssertionSelector = primaryAssertionSelector;
    }

    public List<TestcaseResultDTO> mapChallengeTestcases(List<TestcaseRubric> testcases,
                                                         Map<UUID, SubmissionTestcaseResult> resultsById) {
        if (testcases == null || testcases.isEmpty()) {
            return List.of();
        }
        return testcases.stream()
                .sorted(Comparator.comparingInt(TestcaseRubric::orderIndex))
                .map(testcase -> mapOne(testcase, resultsById.get(testcase.id())))
                .toList();
    }

    private TestcaseResultDTO mapOne(TestcaseRubric testcase, SubmissionTestcaseResult submissionResult) {
        String frontendResult = submissionResult != null
                ? LabResultAssembler.toFrontendResult(submissionResult.getResult())
                : "SKIPPED";
        String feedback = submissionResult != null ? submissionResult.getFeedback() : null;

        if (testcase.hidden()) {
            return new TestcaseResultDTO(
                    testcase.name(),
                    frontendResult,
                    null,
                    true,
                    null,
                    null,
                    null,
                    null);
        }

        String input = submissionResult != null && submissionResult.getInputDisplay() != null
                ? submissionResult.getInputDisplay()
                : displayFormatter.formatInput(testcase);
        String expectedOutput = submissionResult != null ? submissionResult.getExpectedDisplay() : null;
        String actualOutput = submissionResult != null ? submissionResult.getActualDisplay() : null;

        List<TestcaseAssertionResultDTO> assertions = mapAssertions(testcase, submissionResult);

        return new TestcaseResultDTO(
                testcase.name(),
                frontendResult,
                feedback,
                false,
                input,
                expectedOutput,
                actualOutput,
                assertions.isEmpty() ? null : assertions);
    }

    private List<TestcaseAssertionResultDTO> mapAssertions(TestcaseRubric testcase,
                                                           SubmissionTestcaseResult submissionResult) {
        if (testcase.assertions() == null || testcase.assertions().isEmpty()) {
            return List.of();
        }

        Map<UUID, SubmissionTestcaseAssertionResult> assertionResultsById = submissionResult != null
                && submissionResult.getAssertionResults() != null
                ? submissionResult.getAssertionResults().stream()
                        .filter(row -> row.getTestcaseAssertion() != null)
                        .collect(Collectors.toMap(
                                row -> row.getTestcaseAssertion().getId(),
                                row -> row,
                                (left, right) -> left))
                : Map.of();

        AssertionRubric primary = primaryAssertionSelector.select(testcase.assertions());

        return testcase.assertions().stream()
                .sorted(Comparator.comparingInt(AssertionRubric::orderIndex))
                .map(assertion -> {
                    SubmissionTestcaseAssertionResult row = assertionResultsById.get(assertion.id());
                    String result = row != null
                            ? LabResultAssembler.toFrontendResult(row.getResult())
                            : TestcaseResultStatus.SKIPPED.name();
                    String expected = displayFormatter.formatExpected(assertion);
                    String actual;
                    if (primary != null && primary.id().equals(assertion.id())
                            && submissionResult != null
                            && submissionResult.getActualDisplay() != null) {
                        actual = submissionResult.getActualDisplay();
                    } else if (row != null) {
                        actual = displayFormatter.formatExpandedAssertion(assertion, row.getActualValue());
                    } else {
                        actual = "";
                    }
                    return new TestcaseAssertionResultDTO(
                            assertion.kind().name(),
                            result,
                            expected,
                            actual,
                            assertion.orderIndex());
                })
                .toList();
    }
}
