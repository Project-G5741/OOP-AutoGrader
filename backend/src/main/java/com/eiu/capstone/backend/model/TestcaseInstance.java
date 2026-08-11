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
@Table(name = "testcase_instance")
public class TestcaseInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "testcase_id", nullable = false,
            foreignKey = @ForeignKey(name = "testcase_instance_testcase_id_fkey"))
    private Testcase testcase;

    @Column(name = "label", nullable = false)
    private String label;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "constructor_id", nullable = false,
            foreignKey = @ForeignKey(name = "testcase_instance_constructor_id_fkey"))
    private Constructor constructor;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "params", nullable = false, columnDefinition = "jsonb")
    private String params = "[]";

    public TestcaseInstance() {}

    public UUID getId() { return id; }

    public Testcase getTestcase() { return testcase; }
    public void setTestcase(Testcase testcase) { this.testcase = testcase; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public Constructor getConstructor() { return constructor; }
    public void setConstructor(Constructor constructor) { this.constructor = constructor; }

    public String getParams() { return params; }
    public void setParams(String params) { this.params = params; }
}
