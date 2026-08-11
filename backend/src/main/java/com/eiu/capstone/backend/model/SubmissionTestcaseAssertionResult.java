package com.eiu.capstone.backend.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "submission_testcase_assertion_result",
        uniqueConstraints = @UniqueConstraint(
                name = "submission_testcase_assertion_result_key",
                columnNames = {"submission_testcase_result_id", "testcase_assertion_id"}
        )
)
public class SubmissionTestcaseAssertionResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_testcase_result_id", nullable = false,
            foreignKey = @ForeignKey(name = "staresult_str_id_fkey"))
    private SubmissionTestcaseResult submissionTestcaseResult;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "testcase_assertion_id", nullable = false,
            foreignKey = @ForeignKey(name = "staresult_assertion_id_fkey"))
    private TestcaseAssertion testcaseAssertion;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "result", nullable = false, columnDefinition = "testcase_result_status")
    private TestcaseResultStatus result;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "actual_value", columnDefinition = "jsonb")
    private String actualValue;

    @Column(name = "feedback")
    private String feedback;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public SubmissionTestcaseAssertionResult() {}

    public UUID getId() { return id; }

    public SubmissionTestcaseResult getSubmissionTestcaseResult() { return submissionTestcaseResult; }
    public void setSubmissionTestcaseResult(SubmissionTestcaseResult submissionTestcaseResult) {
        this.submissionTestcaseResult = submissionTestcaseResult;
    }

    public TestcaseAssertion getTestcaseAssertion() { return testcaseAssertion; }
    public void setTestcaseAssertion(TestcaseAssertion testcaseAssertion) {
        this.testcaseAssertion = testcaseAssertion;
    }

    public TestcaseResultStatus getResult() { return result; }
    public void setResult(TestcaseResultStatus result) { this.result = result; }

    public String getActualValue() { return actualValue; }
    public void setActualValue(String actualValue) { this.actualValue = actualValue; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
