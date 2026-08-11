package com.eiu.capstone.backend.model;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "submission_testcase_result",
        uniqueConstraints = @UniqueConstraint(
                name = "submission_testcase_result_key",
                columnNames = {"submission_id", "testcase_id"}
        )
)
public class SubmissionTestcaseResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false,
            foreignKey = @ForeignKey(name = "str_submission_id_fkey"))
    private LabSubmission submission;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "testcase_id", nullable = false,
            foreignKey = @ForeignKey(name = "str_testcase_id_fkey"))
    private Testcase testcase;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "result", nullable = false, columnDefinition = "testcase_result_status")
    private TestcaseResultStatus result;

    @Column(name = "feedback")
    private String feedback;

    @Column(name = "input_display")
    private String inputDisplay;

    @Column(name = "expected_display")
    private String expectedDisplay;

    @Column(name = "actual_display")
    private String actualDisplay;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @OneToMany(mappedBy = "submissionTestcaseResult", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SubmissionTestcaseAssertionResult> assertionResults = new ArrayList<>();

    public SubmissionTestcaseResult() {}

    public UUID getId() { return id; }

    public LabSubmission getSubmission() { return submission; }
    public void setSubmission(LabSubmission submission) { this.submission = submission; }

    public Testcase getTestcase() { return testcase; }
    public void setTestcase(Testcase testcase) { this.testcase = testcase; }

    public TestcaseResultStatus getResult() { return result; }
    public void setResult(TestcaseResultStatus result) { this.result = result; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public String getInputDisplay() { return inputDisplay; }
    public void setInputDisplay(String inputDisplay) { this.inputDisplay = inputDisplay; }

    public String getExpectedDisplay() { return expectedDisplay; }
    public void setExpectedDisplay(String expectedDisplay) { this.expectedDisplay = expectedDisplay; }

    public String getActualDisplay() { return actualDisplay; }
    public void setActualDisplay(String actualDisplay) { this.actualDisplay = actualDisplay; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public List<SubmissionTestcaseAssertionResult> getAssertionResults() { return assertionResults; }
    public void setAssertionResults(List<SubmissionTestcaseAssertionResult> assertionResults) {
        this.assertionResults = assertionResults;
    }
}
