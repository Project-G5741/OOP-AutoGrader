package com.eiu.capstone.backend.grading;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.eiu.capstone.backend.model.SubmissionConstructorResult;
import com.eiu.capstone.backend.model.SubmissionFieldResult;
import com.eiu.capstone.backend.model.SubmissionMethodResult;
import com.eiu.capstone.backend.model.SubmissionRelationResult;
import com.eiu.capstone.backend.model.SubmissionTestcaseAssertionResult;
import com.eiu.capstone.backend.model.SubmissionTestcaseResult;
import com.eiu.capstone.backend.model.TestcaseResultStatus;

public record GradingDetailPersistPayload(
        UUID submissionId,
        List<MemberFlag> fields,
        List<MemberFlag> methods,
        List<MemberFlag> constructors,
        List<MemberFlag> relations,
        List<TestcaseRow> testcases) {

    public record MemberFlag(UUID elementId, boolean correct) {}

    public record AssertionRow(UUID assertionId, TestcaseResultStatus status, String actualValueJson, String feedback) {}

    public record TestcaseRow(
            UUID testcaseId,
            TestcaseResultStatus status,
            String feedback,
            String inputDisplay,
            String expectedDisplay,
            String actualDisplay,
            List<AssertionRow> assertions) {}

    public static GradingDetailPersistPayload from(GradingService.GradingComputationResult computed) {
        UUID submissionId = submissionIdFrom(computed);
        List<MemberFlag> fields = new ArrayList<>();
        if (computed.fieldResults != null) {
            for (SubmissionFieldResult row : computed.fieldResults) {
                fields.add(new MemberFlag(row.getField().getId(), row.isCorrect()));
            }
        }
        List<MemberFlag> methods = new ArrayList<>();
        if (computed.methodResults != null) {
            for (SubmissionMethodResult row : computed.methodResults) {
                methods.add(new MemberFlag(row.getMethod().getId(), row.isCorrect()));
            }
        }
        List<MemberFlag> constructors = new ArrayList<>();
        if (computed.constructorResults != null) {
            for (SubmissionConstructorResult row : computed.constructorResults) {
                constructors.add(new MemberFlag(row.getConstructor().getId(), row.isCorrect()));
            }
        }
        List<MemberFlag> relations = new ArrayList<>();
        if (computed.relationResults != null) {
            for (SubmissionRelationResult row : computed.relationResults) {
                relations.add(new MemberFlag(row.getClassRelation().getId(), row.isCorrect()));
            }
        }
        List<TestcaseRow> testcases = new ArrayList<>();
        if (computed.testcaseResults != null) {
            for (SubmissionTestcaseResult row : computed.testcaseResults) {
                List<AssertionRow> assertions = new ArrayList<>();
                if (row.getAssertionResults() != null) {
                    for (SubmissionTestcaseAssertionResult assertion : row.getAssertionResults()) {
                        assertions.add(new AssertionRow(
                                assertion.getTestcaseAssertion().getId(),
                                assertion.getResult(),
                                assertion.getActualValue(),
                                assertion.getFeedback()));
                    }
                }
                testcases.add(new TestcaseRow(
                        row.getTestcase().getId(),
                        row.getResult(),
                        row.getFeedback(),
                        row.getInputDisplay(),
                        row.getExpectedDisplay(),
                        row.getActualDisplay(),
                        assertions));
            }
        }
        return new GradingDetailPersistPayload(submissionId, fields, methods, constructors, relations, testcases);
    }

    private static UUID submissionIdFrom(GradingService.GradingComputationResult computed) {
        if (computed.challengeResults != null && !computed.challengeResults.isEmpty()
                && computed.challengeResults.get(0).getSubmission() != null) {
            return computed.challengeResults.get(0).getSubmission().getId();
        }
        if (computed.fieldResults != null && !computed.fieldResults.isEmpty()
                && computed.fieldResults.get(0).getSubmission() != null) {
            return computed.fieldResults.get(0).getSubmission().getId();
        }
        if (computed.testcaseResults != null && !computed.testcaseResults.isEmpty()
                && computed.testcaseResults.get(0).getSubmission() != null) {
            return computed.testcaseResults.get(0).getSubmission().getId();
        }
        return null;
    }
}
