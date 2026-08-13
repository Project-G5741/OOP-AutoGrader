package com.eiu.capstone.backend.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "testcase")
public class Testcase {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "challenge_id", nullable = false,
            foreignKey = @ForeignKey(name = "testcase_challenge_id_fkey"))
    private Challenge challenge;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "testcase_type", nullable = false, columnDefinition = "testcase_type")
    private TestcaseType testcaseType;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "comparison_method", columnDefinition = "testcase_comparison_method")
    private TestcaseComparisonMethod comparisonMethod;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "weight", nullable = false)
    private int weight = 1;

    @Column(name = "order_index", nullable = false)
    private int orderIndex = 0;

    @Column(name = "is_hidden", nullable = false)
    private boolean hidden = false;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public Testcase() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Challenge getChallenge() { return challenge; }
    public void setChallenge(Challenge challenge) { this.challenge = challenge; }

    public TestcaseType getTestcaseType() { return testcaseType; }
    public void setTestcaseType(TestcaseType testcaseType) { this.testcaseType = testcaseType; }

    public TestcaseComparisonMethod getComparisonMethod() { return comparisonMethod; }
    public void setComparisonMethod(TestcaseComparisonMethod comparisonMethod) {
        this.comparisonMethod = comparisonMethod;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getWeight() { return weight; }
    public void setWeight(int weight) { this.weight = weight; }

    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }

    public boolean isHidden() { return hidden; }
    public void setHidden(boolean hidden) { this.hidden = hidden; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
