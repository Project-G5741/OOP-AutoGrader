package com.eiu.capstone.backend.model;

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

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "testcase_assertion")
public class TestcaseAssertion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "testcase_id", nullable = false,
            foreignKey = @ForeignKey(name = "testcase_assertion_testcase_id_fkey"))
    private Testcase testcase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invocation_id",
            foreignKey = @ForeignKey(name = "testcase_assertion_invocation_id_fkey"))
    private TestcaseInvocation invocation;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "assertion_kind", nullable = false, columnDefinition = "assertion_kind")
    private AssertionKind assertionKind;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "field_id",
            foreignKey = @ForeignKey(name = "testcase_assertion_field_id_fkey"))
    private Field field;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "expected_value", nullable = false, columnDefinition = "jsonb")
    private String expectedValue;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "comparison_mode", nullable = false, columnDefinition = "comparison_mode")
    private ComparisonMode comparisonMode = ComparisonMode.EXACT;

    @Column(name = "order_index", nullable = false)
    private int orderIndex = 0;

    public TestcaseAssertion() {}

    public UUID getId() { return id; }

    public Testcase getTestcase() { return testcase; }
    public void setTestcase(Testcase testcase) { this.testcase = testcase; }

    public TestcaseInvocation getInvocation() { return invocation; }
    public void setInvocation(TestcaseInvocation invocation) { this.invocation = invocation; }

    public AssertionKind getAssertionKind() { return assertionKind; }
    public void setAssertionKind(AssertionKind assertionKind) { this.assertionKind = assertionKind; }

    public Field getField() { return field; }
    public void setField(Field field) { this.field = field; }

    public String getExpectedValue() { return expectedValue; }
    public void setExpectedValue(String expectedValue) { this.expectedValue = expectedValue; }

    public ComparisonMode getComparisonMode() { return comparisonMode; }
    public void setComparisonMode(ComparisonMode comparisonMode) { this.comparisonMode = comparisonMode; }

    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }
}
