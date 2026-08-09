package com.eiu.capstone.backend.DTO;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TestcaseResultDTO {

    @JsonProperty("testcase_name")
    private final String testcaseName;
    private final String result;
    private final String feedback;

    public TestcaseResultDTO(String testcaseName, String result, String feedback) {
        this.testcaseName = testcaseName;
        this.result = result;
        this.feedback = feedback;
    }

    public String getTestcaseName() { return testcaseName; }
    public String getResult() { return result; }
    public String getFeedback() { return feedback; }
}
