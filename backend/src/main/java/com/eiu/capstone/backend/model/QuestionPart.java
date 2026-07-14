package com.eiu.capstone.backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "question_parts", uniqueConstraints = @UniqueConstraint(columnNames = {"question_id", "part_number"}))
public class QuestionPart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "part_id")
    private Integer partId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "part_number", nullable = false)
    private Short partNumber;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "weight", nullable = false, precision = 6, scale = 2)
    private BigDecimal weight;

    public QuestionPart() {}

    public Integer getPartId() { return partId; }
    public void setPartId(Integer partId) { this.partId = partId; }

    public Question getQuestion() { return question; }
    public void setQuestion(Question question) { this.question = question; }

    public Short getPartNumber() { return partNumber; }
    public void setPartNumber(Short partNumber) { this.partNumber = partNumber; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }
}