package com.eiu.capstone.backend.model;

import java.util.UUID;

/**
 * In-memory comparison instance leftover. The {@code testcase_instance} table is dropped;
 * this type is not a JPA entity.
 */
public class TestcaseInstance {

    private UUID id;
    private Testcase testcase;
    private String label;
    private Constructor constructor;
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
