package com.eiu.capstone.backend.DTO;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ChallengeDetailBundleDTO {

    private final List<ClassDetailDTO> classData;
    private final List<MmdClassDTO> mmdData;
    private final List<TestcaseResultDTO> testcases;
    private final Map<String, java.math.BigDecimal> scores;

    public ChallengeDetailBundleDTO(List<ClassDetailDTO> classData,
                                    List<MmdClassDTO> mmdData,
                                    List<TestcaseResultDTO> testcases,
                                    Map<String, java.math.BigDecimal> scores) {
        this.classData = classData;
        this.mmdData = mmdData;
        this.testcases = testcases;
        this.scores = scores;
    }

    @JsonProperty("class")
    public List<ClassDetailDTO> getClassData() { return classData; }

    @JsonProperty("mmd")
    public List<MmdClassDTO> getMmdData() { return mmdData; }

    public List<TestcaseResultDTO> getTestcases() { return testcases; }
    public Map<String, java.math.BigDecimal> getScores() { return scores; }
}
