package com.eiu.capstone.backend.DTO;

import java.util.List;

import com.eiu.capstone.backend.model.OopPrincipleTag;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TestcaseResultDTO {

    @JsonProperty("testcase_name")
    private final String testcaseName;
    private final String result;
    private final String feedback;
    @JsonProperty("is_hidden")
    private final Boolean hidden;
    private final String input;
    @JsonProperty("expected_output")
    private final String expectedOutput;
    @JsonProperty("actual_output")
    private final String actualOutput;
    private final List<TestcaseAssertionResultDTO> assertions;
    @JsonProperty("oop_principle_tag")
    private final OopPrincipleTag oopPrincipleTag;

    public TestcaseResultDTO(String testcaseName, String result, String feedback) {
        this(testcaseName, result, feedback, false, null, null, null, null, null);
    }

    public TestcaseResultDTO(String testcaseName,
                             String result,
                             String feedback,
                             boolean hidden,
                             String input,
                             String expectedOutput,
                             String actualOutput,
                             List<TestcaseAssertionResultDTO> assertions) {
        this(testcaseName, result, feedback, hidden, input, expectedOutput, actualOutput, assertions, null);
    }

    public TestcaseResultDTO(String testcaseName,
                             String result,
                             String feedback,
                             boolean hidden,
                             String input,
                             String expectedOutput,
                             String actualOutput,
                             List<TestcaseAssertionResultDTO> assertions,
                             OopPrincipleTag oopPrincipleTag) {
        this.testcaseName = testcaseName;
        this.result = result;
        this.feedback = feedback;
        this.hidden = hidden;
        this.input = input;
        this.expectedOutput = expectedOutput;
        this.actualOutput = actualOutput;
        this.assertions = assertions;
        this.oopPrincipleTag = oopPrincipleTag;
    }

    public String getTestcaseName() { return testcaseName; }
    public String getResult() { return result; }
    public String getFeedback() { return feedback; }
    public Boolean getHidden() { return hidden; }
    public String getInput() { return input; }
    public String getExpectedOutput() { return expectedOutput; }
    public String getActualOutput() { return actualOutput; }
    public List<TestcaseAssertionResultDTO> getAssertions() { return assertions; }
    @JsonProperty("oop_principle_tag")
    public OopPrincipleTag getOopPrincipleTag() { return oopPrincipleTag; }
}
