package com.eiu.capstone.backend.DTO;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ChallengeDetailBundleDTO {

    private final List<ClassDetailDTO> classData;
    private final MmdResponseDTO mmd;
    private final List<TestcaseResultDTO> testcases;
    private final Map<String, java.math.BigDecimal> scores;
    private final Map<String, Boolean> scoreApplicability;
    private final String normalizationNotice;

    public ChallengeDetailBundleDTO(List<ClassDetailDTO> classData,
                                    MmdResponseDTO mmd,
                                    List<TestcaseResultDTO> testcases,
                                    Map<String, java.math.BigDecimal> scores,
                                    Map<String, Boolean> scoreApplicability) {
        this(classData, mmd, testcases, scores, scoreApplicability, null);
    }

    public ChallengeDetailBundleDTO(List<ClassDetailDTO> classData,
                                    MmdResponseDTO mmd,
                                    List<TestcaseResultDTO> testcases,
                                    Map<String, java.math.BigDecimal> scores,
                                    Map<String, Boolean> scoreApplicability,
                                    String normalizationNotice) {
        this.classData = classData;
        this.mmd = mmd != null ? mmd : new MmdResponseDTO(List.of(), null);
        this.testcases = testcases;
        this.scores = scores;
        this.scoreApplicability = scoreApplicability;
        this.normalizationNotice = normalizationNotice;
    }

    /** @deprecated Use {@link #getMmd()} */
    public ChallengeDetailBundleDTO(List<ClassDetailDTO> classData,
                                    List<MmdClassDTO> mmdData,
                                    List<TestcaseResultDTO> testcases,
                                    Map<String, java.math.BigDecimal> scores,
                                    Map<String, Boolean> scoreApplicability) {
        this(classData, new MmdResponseDTO(mmdData, null), testcases, scores, scoreApplicability, null);
    }

    /** @deprecated Use {@link #getMmd()} */
    public ChallengeDetailBundleDTO(List<ClassDetailDTO> classData,
                                    List<MmdClassDTO> mmdData,
                                    List<TestcaseResultDTO> testcases,
                                    Map<String, java.math.BigDecimal> scores,
                                    Map<String, Boolean> scoreApplicability,
                                    String normalizationNotice) {
        this(classData, new MmdResponseDTO(mmdData, null), testcases, scores, scoreApplicability, normalizationNotice);
    }

    @JsonProperty("class")
    public List<ClassDetailDTO> getClassData() { return classData; }

    @JsonProperty("mmd")
    public MmdResponseDTO getMmd() { return mmd; }

    /** Convenience for callers that only need class rows. */
    public List<MmdClassDTO> getMmdClasses() { return mmd.classes(); }

    public String getMmdParseError() { return mmd.parseError(); }

    public List<TestcaseResultDTO> getTestcases() { return testcases; }
    public Map<String, java.math.BigDecimal> getScores() { return scores; }

    /**
     * Which pillars in {@link #getScores()} are applicable to this challenge (class is always
     * {@code true}; mmd/testcase are {@code false} when the challenge doesn't require an MMD
     * diagram or has no operational testcases). Frontend must check this before falling back to
     * a locally computed score — a pillar score of {@code null} or {@code 0} does not by itself
     * mean "not applicable."
     */
    public Map<String, Boolean> getScoreApplicability() { return scoreApplicability; }

    public String getNormalizationNotice() { return normalizationNotice; }
}
