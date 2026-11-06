package com.eiu.capstone.backend.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TestcaseAssertionResultDTO {

    private final String kind;
    private final String result;
    @JsonProperty("expected_output")
    private final String expectedOutput;
    @JsonProperty("actual_output")
    private final String actualOutput;
    @JsonProperty("order_index")
    private final int orderIndex;

    public TestcaseAssertionResultDTO(String kind,
                                      String result,
                                      String expectedOutput,
                                      String actualOutput,
                                      int orderIndex) {
        this.kind = kind;
        this.result = result;
        this.expectedOutput = expectedOutput;
        this.actualOutput = actualOutput;
        this.orderIndex = orderIndex;
    }

    public String getKind() { return kind; }
    public String getResult() { return result; }
    public String getExpectedOutput() { return expectedOutput; }
    public String getActualOutput() { return actualOutput; }
    public int getOrderIndex() { return orderIndex; }
}
