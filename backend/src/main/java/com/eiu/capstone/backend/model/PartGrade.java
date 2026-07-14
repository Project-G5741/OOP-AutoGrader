package com.eiu.capstone.backend.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "part_grades")
public class PartGrade {

    @EmbeddedId
    private PartGradeId id = new PartGradeId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("submissionId")
    @JoinColumn(name = "submission_id")
    private Submission submission;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("partId")
    @JoinColumn(name = "part_id")
    private QuestionPart questionPart;

    @Column(name = "auto_score", precision = 6, scale = 2)
    private BigDecimal autoScore;

    @Column(name = "final_score", precision = 6, scale = 2)
    private BigDecimal finalScore;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "source", nullable = false, columnDefinition = "grade_source")
    private GradeSource source = GradeSource.auto;

    @Column(name = "feedback", columnDefinition = "TEXT")
    private String feedback;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "graded_by")
    private Teacher gradedBy;

    @Column(name = "graded_at")
    private LocalDateTime gradedAt;

    public PartGrade() {}

    public PartGradeId getId() { return id; }
    public void setId(PartGradeId id) { this.id = id; }

    public Submission getSubmission() { return submission; }
    public void setSubmission(Submission submission) { this.submission = submission; }

    public QuestionPart getQuestionPart() { return questionPart; }
    public void setQuestionPart(QuestionPart questionPart) { this.questionPart = questionPart; }

    public BigDecimal getAutoScore() { return autoScore; }
    public void setAutoScore(BigDecimal autoScore) { this.autoScore = autoScore; }

    public BigDecimal getFinalScore() { return finalScore; }
    public void setFinalScore(BigDecimal finalScore) { this.finalScore = finalScore; }

    public GradeSource getSource() { return source; }
    public void setSource(GradeSource source) { this.source = source; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public Teacher getGradedBy() { return gradedBy; }
    public void setGradedBy(Teacher gradedBy) { this.gradedBy = gradedBy; }

    public LocalDateTime getGradedAt() { return gradedAt; }
    public void setGradedAt(LocalDateTime gradedAt) { this.gradedAt = gradedAt; }
}