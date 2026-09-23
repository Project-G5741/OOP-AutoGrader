package unit.com.eiu.capstone.backend.grading.pipeline;

import com.eiu.capstone.backend.grading.pipeline.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.grading.rubric.AssertionRubric;
import com.eiu.capstone.backend.grading.rubric.ChallengeRubric;
import com.eiu.capstone.backend.grading.rubric.InvocationRubric;
import com.eiu.capstone.backend.grading.rubric.TestcaseRubric;
import com.eiu.capstone.backend.grading.testcase.AssertionEvaluator;
import com.eiu.capstone.backend.grading.testcase.InvocationOutcome;
import com.eiu.capstone.backend.grading.testcase.InvocationRunner;
import com.eiu.capstone.backend.grading.testcase.JsonValueCoercer;
import com.eiu.capstone.backend.grading.testcase.PrimaryAssertionSelector;
import com.eiu.capstone.backend.grading.testcase.TestcaseDisplayFormatter;
import com.eiu.capstone.backend.model.AssertionKind;
import com.eiu.capstone.backend.model.ComparisonMode;
import com.eiu.capstone.backend.model.InvocationKind;
import com.eiu.capstone.backend.model.OopPrincipleTag;
import com.eiu.capstone.backend.model.TestcaseResultStatus;
import com.eiu.capstone.backend.model.TestcaseType;

class TestcaseGraderTest {

    @Test
    void emptyTestcaseRowsScoreZeroPercent() {
        ChallengeRubric rubric = new ChallengeRubric(
                UUID.randomUUID(),
                1,
                "Challenge 1",
                List.of(),
                List.of(),
                List.of());

        ChallengeGradingContext context = ChallengeGradingContext.of(rubric, null, null, List.of());
        TestcaseGrader grader = new TestcaseGrader(null, null, null, null);

        TestcaseGrader.TestcasePillarResult result = grader.grade(context);

        assertEquals(0, result.pillarPercentage().compareTo(BigDecimal.ZERO));
        assertTrue(result.results().isEmpty());
    }

    @Test
    void mixedCompile_errorsWhenTargetClassFailed() {
        UUID invocationId = UUID.randomUUID();
        TestcaseRubric testcase = new TestcaseRubric(
                UUID.randomUUID(),
                "new student",
                TestcaseType.UNIT,
                null,
                1,
                0,
                false,
                new InvocationRubric(
                        invocationId,
                        InvocationKind.CONSTRUCTOR,
                        UUID.randomUUID(),
                        null,
                        "Student",
                        null,
                        List.of(),
                        "[]",
                        null,
                        null,
                        List.of(),
                        null),
                List.of(),
                List.of(new AssertionRubric(
                        UUID.randomUUID(),
                        AssertionKind.RETURN_VALUE,
                        invocationId,
                        null,
                        null,
                        null,
                        "null",
                        ComparisonMode.EXACT,
                        0)));
        ChallengeRubric rubric = new ChallengeRubric(
                UUID.randomUUID(),
                1,
                "Challenge 1",
                List.of(),
                List.of(),
                List.of(testcase));
        ChallengeGradingContext context = ChallengeGradingContext.of(
                rubric,
                null,
                null,
                List.of(),
                Set.of("Student"),
                Map.of("Student", "ERROR: line 1: ';' expected"));

        TestcaseGrader.PendingTestcaseResult result = new TestcaseGrader(null, null, null, null)
                .gradeSingle(testcase, context);

        assertEquals(TestcaseResultStatus.ERROR, result.status());
        assertTrue(result.feedback().contains("Missing ; on line 1"));
    }

    @Test
    void mixedCompile_errorsWhenParamTypeFailed() {
        UUID invocationId = UUID.randomUUID();
        TestcaseRubric testcase = new TestcaseRubric(
                UUID.randomUUID(),
                "deposit",
                TestcaseType.UNIT,
                null,
                1,
                0,
                false,
                new InvocationRubric(
                        invocationId,
                        InvocationKind.METHOD,
                        null,
                        UUID.randomUUID(),
                        "BankAccount",
                        "setOwner",
                        List.of("Student"),
                        "[null]",
                        null,
                        null,
                        List.of(),
                        null),
                List.of(),
                List.of(new AssertionRubric(
                        UUID.randomUUID(),
                        AssertionKind.RETURN_VALUE,
                        invocationId,
                        null,
                        null,
                        null,
                        "null",
                        ComparisonMode.EXACT,
                        0)));
        ChallengeRubric rubric = new ChallengeRubric(
                UUID.randomUUID(),
                1,
                "Challenge 1",
                List.of(),
                List.of(),
                List.of(testcase));
        ChallengeGradingContext context = ChallengeGradingContext.of(
                rubric,
                null,
                null,
                List.of(),
                Set.of("Student"),
                Map.of("Student", "ERROR: line 1: ';' expected"));

        TestcaseGrader.PendingTestcaseResult result = new TestcaseGrader(null, null, null, null)
                .gradeSingle(testcase, context);

        assertEquals(TestcaseResultStatus.ERROR, result.status());
        assertTrue(result.feedback().contains("Missing ; on line 1"));
    }

    @Test
    void mixedCompile_independentTargetStillInvokes() {
        UUID invocationId = UUID.randomUUID();
        TestcaseRubric testcase = new TestcaseRubric(
                UUID.randomUUID(),
                "new good",
                TestcaseType.UNIT,
                null,
                1,
                0,
                false,
                new InvocationRubric(
                        invocationId,
                        InvocationKind.CONSTRUCTOR,
                        UUID.randomUUID(),
                        null,
                        "Good",
                        null,
                        List.of(),
                        "[]",
                        null,
                        null,
                        List.of(),
                        null),
                List.of(),
                List.of());
        ChallengeRubric rubric = new ChallengeRubric(
                UUID.randomUUID(),
                1,
                "Challenge 1",
                List.of(),
                List.of(),
                List.of(testcase));
        ChallengeGradingContext context = ChallengeGradingContext.of(
                rubric,
                null,
                null,
                List.of(),
                Set.of("Bad"),
                Map.of("Bad", "ERROR: line 1: ';' expected"));

        com.eiu.capstone.backend.grading.testcase.InvocationRunner runner =
                org.mockito.Mockito.mock(com.eiu.capstone.backend.grading.testcase.InvocationRunner.class);
        org.mockito.Mockito.when(runner.invokeScenario(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(com.eiu.capstone.backend.grading.testcase.InvocationOutcome.normal(null, "", false, Map.of())));
        com.eiu.capstone.backend.grading.testcase.TestcaseDisplayFormatter formatter =
                org.mockito.Mockito.mock(com.eiu.capstone.backend.grading.testcase.TestcaseDisplayFormatter.class);
        org.mockito.Mockito.when(formatter.formatInput(org.mockito.ArgumentMatchers.any())).thenReturn("in");
        org.mockito.Mockito.when(formatter.formatInput(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn("in");
        com.eiu.capstone.backend.grading.testcase.PrimaryAssertionSelector selector =
                org.mockito.Mockito.mock(com.eiu.capstone.backend.grading.testcase.PrimaryAssertionSelector.class);
        org.mockito.Mockito.when(selector.select(org.mockito.ArgumentMatchers.any())).thenReturn(null);

        TestcaseGrader.PendingTestcaseResult result =
                new TestcaseGrader(runner, null, selector, formatter).gradeSingle(testcase, context);

        org.mockito.Mockito.verify(runner).invokeScenario(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        assertTrue(result.feedback() == null || !result.feedback().startsWith("Compilation error:"));
    }

    @Test
    void mixedCompile_errorsWhenLaterStepTypeFailed() {
        UUID constructId = UUID.randomUUID();
        UUID helperId = UUID.randomUUID();
        InvocationRubric construct = constructorStep(constructId, "Student", "[]", "student");
        InvocationRubric helperCall = methodStep(helperId, "Helper", "ping", "[]", "student", null);
        TestcaseRubric testcase = scenario(
                "later helper",
                List.of(construct, helperCall),
                List.of(returnValue(helperId, "\"ok\"", 0)));
        ChallengeGradingContext context = contextWithFailed(testcase, Set.of("Helper"),
                Map.of("Helper", "ERROR: line 1: ';' expected"));

        TestcaseGrader.PendingTestcaseResult result = new TestcaseGrader(null, null, null, null)
                .gradeSingle(testcase, context);

        assertEquals(TestcaseResultStatus.ERROR, result.status());
        assertTrue(result.feedback().contains("Missing ; on line 1"));
    }

    @Test
    void mixedCompile_errorsWhenDispatchTypeFailed() {
        UUID constructId = UUID.randomUUID();
        UUID callId = UUID.randomUUID();
        InvocationRubric construct = constructorStep(constructId, "Circle", "[]", "circle");
        InvocationRubric dispatchCall = methodStep(callId, "Circle", "getArea", "[]", "circle", "Shape");
        TestcaseRubric testcase = scenario(
                "dispatch",
                List.of(construct, dispatchCall),
                List.of(returnValue(callId, "12.5", 0)));
        ChallengeGradingContext context = contextWithFailed(testcase, Set.of("Shape"),
                Map.of("Shape", "ERROR: line 2: ';' expected"));

        TestcaseGrader.PendingTestcaseResult result = new TestcaseGrader(null, null, null, null)
                .gradeSingle(testcase, context);

        assertEquals(TestcaseResultStatus.ERROR, result.status());
        assertTrue(result.feedback().contains("Missing ; on line 2"));
    }

    @Test
    void ae2EmptyOverrideFailsWhenReturnIsGeneric() {
        UUID constructId = UUID.randomUUID();
        UUID soundId = UUID.randomUUID();
        InvocationRubric construct = constructorStep(constructId, "Dog", "[]", "dog");
        InvocationRubric sound = methodStep(soundId, "Dog", "makeSound", "[]", "dog", null);
        TestcaseRubric testcase = scenario(
                "dog sound",
                List.of(construct, sound),
                List.of(returnValue(soundId, "\"woof\"", 0)));
        InvocationRunner runner = scenarioRunner(List.of(
                InvocationOutcome.normal(null, "", false, Map.of()),
                InvocationOutcome.normal("generic", "", false, Map.of())));

        TestcaseGrader.PendingTestcaseResult result = realGrader(runner)
                .gradeSingle(testcase, context(testcase));

        assertEquals(TestcaseResultStatus.FAILED, result.status());
        assertTrue(result.inputDisplay() != null && result.inputDisplay().contains("makeSound"));
    }

    @Test
    void ae2RealOverridePassesWhenReturnIsDogSpecific() {
        UUID constructId = UUID.randomUUID();
        UUID soundId = UUID.randomUUID();
        InvocationRubric construct = constructorStep(constructId, "Dog", "[]", "dog");
        InvocationRubric sound = methodStep(soundId, "Dog", "makeSound", "[]", "dog", null);
        TestcaseRubric testcase = scenario(
                "dog sound",
                List.of(construct, sound),
                List.of(returnValue(soundId, "\"woof\"", 0)));
        InvocationRunner runner = scenarioRunner(List.of(
                InvocationOutcome.normal(null, "", false, Map.of()),
                InvocationOutcome.normal("woof", "", false, Map.of())));

        TestcaseGrader.PendingTestcaseResult result = realGrader(runner)
                .gradeSingle(testcase, context(testcase));

        assertEquals(TestcaseResultStatus.PASSED, result.status());
        assertEquals(1, result.assertions().size());
        assertEquals(TestcaseResultStatus.PASSED, result.assertions().get(0).status());
    }

    @Test
    void acceptedThrowStillStopsLaterAssertions() {
        UUID constructId = UUID.randomUUID();
        UUID setAgeId = UUID.randomUUID();
        UUID getAgeId = UUID.randomUUID();
        TestcaseRubric testcase = personAgeScenario(constructId, setAgeId, getAgeId);
        InvocationRunner runner = scenarioRunner(List.of(
                InvocationOutcome.normal(null, "", false, Map.of()),
                InvocationOutcome.threw("", false, "IllegalArgumentException", List.of("RuntimeException")),
                InvocationOutcome.normal(30, "", false, Map.of())));

        TestcaseGrader.PendingTestcaseResult result = realGrader(runner)
                .gradeSingle(testcase, context(testcase));

        assertEquals(TestcaseResultStatus.FAILED, result.status());
        assertEquals(TestcaseResultStatus.PASSED, result.assertions().get(0).status());
        assertEquals(TestcaseResultStatus.FAILED, result.assertions().get(1).status());
        assertTrue(result.assertions().get(1).feedback().toLowerCase().contains("not executed"),
                result.assertions().get(1).feedback());
    }

    @Test
    void constructFailMarksLaterAssertionsNotExecuted() {
        UUID constructId = UUID.randomUUID();
        UUID getAgeId = UUID.randomUUID();
        InvocationRubric construct = constructorStep(constructId, "Person", "[30]", "person");
        InvocationRubric getAge = methodStep(getAgeId, "Person", "getAge", "[]", "person", null);
        TestcaseRubric testcase = composition(
                "boom construct",
                List.of(construct, getAge),
                List.of(returnValue(getAgeId, "30", 0)));
        InvocationRunner runner = scenarioRunner(List.of(
                InvocationOutcome.threw("", false, "IllegalStateException", List.of("RuntimeException"))));

        TestcaseGrader.PendingTestcaseResult result = realGrader(runner)
                .gradeSingle(testcase, context(testcase));

        assertEquals(TestcaseResultStatus.FAILED, result.status());
        assertEquals(1, result.assertions().size());
        assertEquals(TestcaseResultStatus.FAILED, result.assertions().get(0).status());
        assertTrue(result.assertions().get(0).feedback().toLowerCase().contains("not executed"),
                result.assertions().get(0).feedback());
        assertTrue(result.inputDisplay() != null && result.inputDisplay().contains("Person"));
    }

    @Test
    void firstFailingStepDrivesInputDisplayOverLaterKindPriority() {
        UUID constructId = UUID.randomUUID();
        UUID barkId = UUID.randomUUID();
        UUID printId = UUID.randomUUID();
        InvocationRubric construct = constructorStep(constructId, "Dog", "[]", "dog");
        InvocationRubric bark = methodStep(barkId, "Dog", "bark", "[]", "dog", null);
        InvocationRubric print = methodStep(printId, "Dog", "print", "[]", "dog", null);
        AssertionRubric returnAssert = returnValue(barkId, "1", 0);
        AssertionRubric stdoutAssert = new AssertionRubric(
                UUID.randomUUID(),
                AssertionKind.STDOUT,
                printId,
                null,
                null,
                null,
                "\"hello\"",
                ComparisonMode.EXACT,
                1);
        TestcaseRubric testcase = scenario(
                "priority trap",
                List.of(construct, bark, print),
                List.of(returnAssert, stdoutAssert));
        InvocationRunner runner = scenarioRunner(List.of(
                InvocationOutcome.normal(null, "", false, Map.of()),
                InvocationOutcome.normal(2, "", false, Map.of()),
                InvocationOutcome.normal(null, "hello", false, Map.of())));

        TestcaseGrader.PendingTestcaseResult result = realGrader(runner)
                .gradeSingle(testcase, context(testcase));

        assertEquals(TestcaseResultStatus.FAILED, result.status());
        assertTrue(result.inputDisplay().contains("bark"), result.inputDisplay());
        assertFalse(result.inputDisplay().contains("print"), result.inputDisplay());
        assertEquals("1", result.expectedDisplay());
    }

    @Test
    void allPassUsesKindPriorityOnLastRunStep() {
        UUID constructId = UUID.randomUUID();
        UUID barkId = UUID.randomUUID();
        InvocationRubric construct = constructorStep(constructId, "Dog", "[]", "dog");
        InvocationRubric bark = methodStep(barkId, "Dog", "bark", "[]", "dog", null);
        AssertionRubric returnAssert = returnValue(barkId, "\"woof\"", 1);
        AssertionRubric stdoutAssert = new AssertionRubric(
                UUID.randomUUID(),
                AssertionKind.STDOUT,
                barkId,
                null,
                null,
                null,
                "\"hi\"",
                ComparisonMode.EXACT,
                0);
        TestcaseRubric testcase = scenario(
                "all pass last step",
                List.of(construct, bark),
                List.of(returnAssert, stdoutAssert));
        InvocationRunner runner = scenarioRunner(List.of(
                InvocationOutcome.normal(null, "", false, Map.of()),
                InvocationOutcome.normal("woof", "hi", false, Map.of())));

        TestcaseGrader.PendingTestcaseResult result = realGrader(runner)
                .gradeSingle(testcase, context(testcase));

        assertEquals(TestcaseResultStatus.PASSED, result.status());
        assertTrue(result.inputDisplay().contains("bark"), result.inputDisplay());
        assertEquals("hi", result.expectedDisplay());
    }

    @Test
    void ae8LegacyOneStepStillPasses() {
        UUID invocationId = UUID.randomUUID();
        InvocationRubric invocation = constructorStep(invocationId, "Car", "[]", null);
        TestcaseRubric testcase = new TestcaseRubric(
                UUID.randomUUID(),
                "legacy",
                TestcaseType.UNIT,
                null,
                1,
                0,
                false,
                invocation,
                List.of(),
                List.of(returnValue(invocationId, "null", 0)));
        InvocationRunner runner = scenarioRunner(List.of(
                InvocationOutcome.normal(null, "", false, Map.of())));

        TestcaseGrader.PendingTestcaseResult result = realGrader(runner)
                .gradeSingle(testcase, context(testcase));

        assertEquals(TestcaseResultStatus.PASSED, result.status());
        org.mockito.Mockito.verify(runner).invokeScenario(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void ae1UnitDepositInjectsHiddenNoArgReceiverAndPassesFieldState() {
        UUID invocationId = UUID.randomUUID();
        InvocationRubric deposit = new InvocationRubric(
                invocationId,
                InvocationKind.METHOD,
                null,
                UUID.randomUUID(),
                "BankAccount",
                "deposit",
                List.of("int"),
                "[100]",
                null,
                null,
                List.of(),
                null);
        AssertionRubric fieldState = new AssertionRubric(
                UUID.randomUUID(),
                AssertionKind.FIELD_STATE,
                invocationId,
                null,
                "balance",
                "int",
                "100",
                ComparisonMode.EXACT,
                0);
        TestcaseRubric testcase = new TestcaseRubric(
                UUID.randomUUID(),
                "deposit",
                TestcaseType.UNIT,
                null,
                1,
                0,
                false,
                deposit,
                List.of(),
                List.of(fieldState));
        InvocationRunner runner = scenarioRunner(List.of(
                InvocationOutcome.normal(null, "", false, Map.of("balance", 100))));

        TestcaseGrader.PendingTestcaseResult result = realGrader(runner)
                .gradeSingle(testcase, context(testcase));

        assertEquals(TestcaseResultStatus.PASSED, result.status());
        org.mockito.ArgumentCaptor<List<InvocationRubric>> stepsCaptor =
                org.mockito.ArgumentCaptor.forClass(List.class);
        org.mockito.Mockito.verify(runner).invokeScenario(
                org.mockito.ArgumentMatchers.any(),
                stepsCaptor.capture(),
                org.mockito.ArgumentMatchers.any());
        InvocationRubric sent = stepsCaptor.getValue().get(0);
        assertEquals("BankAccount", sent.receiverClassName());
        assertEquals(List.of(), sent.receiverParameterTypes());
        assertEquals("[]", sent.receiverParamsJson());
    }

    @Test
    void ae4CompositionPassesNamedEngineIntoCarConstructor() {
        UUID engineId = UUID.randomUUID();
        UUID carId = UUID.randomUUID();
        InvocationRubric engine = constructorStep(engineId, "Engine", "[200]", "eng");
        InvocationRubric car = constructorStep(carId, "Car", "[{\"$instance\":\"eng\"}]", "car");
        TestcaseRubric testcase = composition(
                "car engine",
                List.of(engine, car),
                List.of(returnValue(carId, "{\"$objectCheck\":\"TYPE\"}", 0)));
        InvocationRunner runner = scenarioRunner(List.of(
                InvocationOutcome.normal(null, "", false, Map.of(), "Engine", Map.of(), Map.of()),
                InvocationOutcome.normal(null, "", false, Map.of(), "Car", Map.of(), Map.of())));

        TestcaseGrader.PendingTestcaseResult result = realGrader(runner)
                .gradeSingle(testcase, context(testcase));

        assertEquals(TestcaseResultStatus.PASSED, result.status());
        org.mockito.ArgumentCaptor<List<InvocationRubric>> stepsCaptor =
                org.mockito.ArgumentCaptor.forClass(List.class);
        org.mockito.Mockito.verify(runner).invokeScenario(
                org.mockito.ArgumentMatchers.any(),
                stepsCaptor.capture(),
                org.mockito.ArgumentMatchers.any());
        assertTrue(stepsCaptor.getValue().get(1).paramsJson().contains("$instance"));
        assertTrue(stepsCaptor.getValue().get(1).paramsJson().contains("eng"));
    }

    @Test
    void ae6UnacceptedThrowFailsLaterAssertionsAsNotExecuted() {
        UUID firstId = UUID.randomUUID();
        UUID boomId = UUID.randomUUID();
        UUID laterId = UUID.randomUUID();
        TestcaseRubric testcase = composition(
                "mid throw",
                List.of(
                        constructorStep(firstId, "Actor", "[]", "actor"),
                        methodStep(boomId, "Actor", "boom", "[]", "actor", null),
                        methodStep(laterId, "Actor", "ok", "[]", "actor", null)),
                List.of(returnValue(laterId, "7", 0)));
        InvocationRunner runner = scenarioRunner(List.of(
                InvocationOutcome.normal(null, "", false, Map.of()),
                InvocationOutcome.threw("", false, "IllegalStateException", List.of("RuntimeException"))));

        TestcaseGrader.PendingTestcaseResult result = realGrader(runner)
                .gradeSingle(testcase, context(testcase));

        assertEquals(TestcaseResultStatus.FAILED, result.status());
        assertEquals(TestcaseResultStatus.FAILED, result.assertions().get(0).status());
        assertTrue(result.assertions().get(0).feedback().toLowerCase().contains("not executed"),
                result.assertions().get(0).feedback());
    }

    @Test
    void ae7ExceptionStdoutAndFieldStateOnSameInvocation() {
        UUID constructId = UUID.randomUUID();
        UUID callId = UUID.randomUUID();
        InvocationRubric construct = constructorStep(constructId, "Probe", "[]", "probe");
        InvocationRubric call = methodStep(callId, "Probe", "shout", "[]", "probe", null);
        AssertionRubric exception = new AssertionRubric(
                UUID.randomUUID(), AssertionKind.EXCEPTION, callId, null, null, null,
                "\"IllegalArgumentException\"", ComparisonMode.EXACT, 0);
        AssertionRubric stdout = new AssertionRubric(
                UUID.randomUUID(), AssertionKind.STDOUT, callId, null, null, null,
                "\"hi\"", ComparisonMode.EXACT, 1);
        AssertionRubric field = new AssertionRubric(
                UUID.randomUUID(), AssertionKind.FIELD_STATE, callId, null, "flag", "int",
                "1", ComparisonMode.EXACT, 2);
        TestcaseRubric testcase = composition(
                "mixed",
                List.of(construct, call),
                List.of(exception, stdout, field));
        InvocationRunner runner = scenarioRunner(List.of(
                InvocationOutcome.normal(null, "", false, Map.of()),
                InvocationOutcome.threw("hi", false, "IllegalArgumentException",
                        List.of("RuntimeException"), Map.of("flag", 1))));

        TestcaseGrader.PendingTestcaseResult result = realGrader(runner)
                .gradeSingle(testcase, context(testcase));

        assertEquals(TestcaseResultStatus.PASSED, result.status());
        assertEquals(3, result.assertions().size());
        assertTrue(result.assertions().stream()
                .allMatch(row -> row.status() == TestcaseResultStatus.PASSED));
    }

    @Test
    void ae10ConstructorStdoutIsNotEvaluated() {
        UUID invocationId = UUID.randomUUID();
        InvocationRubric construct = constructorStep(invocationId, "Printer", "[]", null);
        AssertionRubric stdout = new AssertionRubric(
                UUID.randomUUID(), AssertionKind.STDOUT, invocationId, null, null, null,
                "\"hello\"", ComparisonMode.EXACT, 0);
        AssertionRubric typeCheck = returnValue(invocationId, "{\"$objectCheck\":\"TYPE\"}", 1);
        TestcaseRubric testcase = new TestcaseRubric(
                UUID.randomUUID(),
                "ctor stdout",
                TestcaseType.UNIT,
                null,
                1,
                0,
                false,
                construct,
                List.of(),
                List.of(stdout, typeCheck));
        InvocationRunner runner = scenarioRunner(List.of(
                InvocationOutcome.normal(null, "noise", false, Map.of(), "Printer", Map.of(), Map.of())));

        TestcaseGrader.PendingTestcaseResult result = realGrader(runner)
                .gradeSingle(testcase, context(testcase));

        assertEquals(TestcaseResultStatus.PASSED, result.status());
        assertEquals(TestcaseResultStatus.PASSED, result.assertions().get(1).status());
    }

    @Test
    void compositionEqualsUsesWorkerNamedFacts() {
        UUID leftId = UUID.randomUUID();
        UUID rightId = UUID.randomUUID();
        TestcaseRubric testcase = composition(
                "equals coins",
                List.of(
                        constructorStep(leftId, "Coin", "[5]", "left"),
                        constructorStep(rightId, "Coin", "[5]", "right")),
                List.of(returnValue(rightId, "{\"$objectCheck\":\"EQUALS\",\"$instance\":\"left\"}", 0)));
        InvocationRunner runner = scenarioRunner(List.of(
                InvocationOutcome.normal(null, "", false, Map.of(), "Coin", Map.of(), Map.of()),
                InvocationOutcome.normal(null, "", false, Map.of(), "Coin", Map.of(), Map.of("left", true))));

        TestcaseGrader.PendingTestcaseResult result = realGrader(runner)
                .gradeSingle(testcase, context(testcase));

        assertEquals(TestcaseResultStatus.PASSED, result.status());
        assertEquals(TestcaseResultStatus.PASSED, result.assertions().get(0).status());
    }

    private static TestcaseGrader realGrader(InvocationRunner runner) {
        JsonValueCoercer coercer = new JsonValueCoercer();
        return new TestcaseGrader(
                runner,
                new AssertionEvaluator(coercer),
                new PrimaryAssertionSelector(),
                new TestcaseDisplayFormatter(coercer));
    }

    private static InvocationRunner scenarioRunner(List<InvocationOutcome> outcomes) {
        InvocationRunner runner = org.mockito.Mockito.mock(InvocationRunner.class);
        org.mockito.Mockito.when(runner.invokeScenario(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()))
                .thenReturn(outcomes);
        if (!outcomes.isEmpty()) {
            org.mockito.Mockito.when(runner.invokeSingle(
                    org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any()))
                    .thenReturn(outcomes.get(0));
        }
        return runner;
    }

    private static ChallengeGradingContext context(TestcaseRubric testcase) {
        ChallengeRubric rubric = new ChallengeRubric(
                UUID.randomUUID(), 1, "Challenge 1", List.of(), List.of(), List.of(testcase));
        return ChallengeGradingContext.of(rubric, null, null, List.of());
    }

    private static ChallengeGradingContext contextWithFailed(TestcaseRubric testcase,
                                                             Set<String> failed,
                                                             Map<String, String> errors) {
        ChallengeRubric rubric = new ChallengeRubric(
                UUID.randomUUID(), 1, "Challenge 1", List.of(), List.of(), List.of(testcase));
        return ChallengeGradingContext.of(rubric, null, null, List.of(), failed, errors);
    }

    private static TestcaseRubric personAgeScenario(UUID constructId, UUID setAgeId, UUID getAgeId) {
        InvocationRubric construct = constructorStep(constructId, "Person", "[30]", "person");
        InvocationRubric setAge = methodStep(setAgeId, "Person", "setAge", "[-5]", "person", null);
        InvocationRubric getAge = methodStep(getAgeId, "Person", "getAge", "[]", "person", null);
        AssertionRubric exception = new AssertionRubric(
                UUID.randomUUID(),
                AssertionKind.EXCEPTION,
                setAgeId,
                null,
                null,
                null,
                "\"IllegalArgumentException\"",
                ComparisonMode.EXACT,
                0);
        return composition(
                "rejected age",
                List.of(construct, setAge, getAge),
                List.of(exception, returnValue(getAgeId, "30", 1)));
    }

    private static TestcaseRubric composition(String name,
                                              List<InvocationRubric> steps,
                                              List<AssertionRubric> assertions) {
        return new TestcaseRubric(
                UUID.randomUUID(),
                name,
                TestcaseType.COMPOSITION,
                null,
                1,
                0,
                false,
                steps.get(0),
                List.of(),
                assertions,
                steps,
                OopPrincipleTag.Composition);
    }

    private static TestcaseRubric scenario(String name,
                                           List<InvocationRubric> steps,
                                           List<AssertionRubric> assertions) {
        return new TestcaseRubric(
                UUID.randomUUID(),
                name,
                TestcaseType.UNIT,
                null,
                1,
                0,
                false,
                steps.get(0),
                List.of(),
                assertions,
                steps,
                OopPrincipleTag.Inheritance);
    }

    private static InvocationRubric constructorStep(UUID id, String className, String paramsJson, String instanceName) {
        return new InvocationRubric(
                id,
                InvocationKind.CONSTRUCTOR,
                UUID.randomUUID(),
                null,
                className,
                null,
                List.of(),
                paramsJson,
                null,
                null,
                List.of(),
                null,
                instanceName,
                null,
                null);
    }

    private static InvocationRubric methodStep(UUID id,
                                               String className,
                                               String methodName,
                                               String paramsJson,
                                               String instanceName,
                                               String dispatchClassName) {
        return new InvocationRubric(
                id,
                InvocationKind.METHOD,
                null,
                UUID.randomUUID(),
                className,
                methodName,
                List.of(),
                paramsJson,
                null,
                null,
                List.of(),
                null,
                instanceName,
                null,
                dispatchClassName);
    }

    private static AssertionRubric returnValue(UUID invocationId, String expectedJson, int orderIndex) {
        return new AssertionRubric(
                UUID.randomUUID(),
                AssertionKind.RETURN_VALUE,
                invocationId,
                null,
                null,
                null,
                expectedJson,
                ComparisonMode.EXACT,
                orderIndex);
    }
}
