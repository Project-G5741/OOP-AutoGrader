package com.eiu.capstone.backend.grading.scoring;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class PartialCreditEvaluatorTest {

    @Test
    void accuracy_threeOfFourAttributesMatch() {
        double result = PartialCreditEvaluator.accuracy(List.of(true, true, true, false));
        assertEquals(0.75, result, 0.0001);
    }

    @Test
    void binaryAccuracy_allOrNothing() {
        assertEquals(1.0, PartialCreditEvaluator.binaryAccuracy(true));
        assertEquals(0.0, PartialCreditEvaluator.binaryAccuracy(false));
    }

    @Test
    void matchesList_comparesParameterTypesInOrder() {
        double result = PartialCreditEvaluator.accuracy(
                PartialCreditEvaluator.matchesList(List.of("String", "int"), List.of("String", "long")));
        assertEquals(0.5, result, 0.0001);
    }
}
