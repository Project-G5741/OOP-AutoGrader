package com.eiu.capstone.backend.DTO.rubric.testcase;

import java.util.List;

import com.eiu.capstone.backend.DTO.TestcaseResultDTO;

public record TestcaseDryRunRequest(
        List<ReferenceSourceDTO> referenceSources,
        TestcaseStructureDTO testcase) {}
