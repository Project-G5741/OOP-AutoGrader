package unit.com.eiu.capstone.backend.grading.testcase.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.eiu.capstone.backend.grading.testcase.SerializedInvocationOutcome;
import com.eiu.capstone.backend.grading.testcase.worker.WorkerIpc;
import com.eiu.capstone.backend.grading.testcase.worker.WorkerInvokeEngine;
import com.fasterxml.jackson.databind.JsonNode;

class WorkerInvokeEngineTest {

    @TempDir
    Path tempDir;

    private Path classesDir;
    private final WorkerInvokeEngine engine = new WorkerInvokeEngine();

    @BeforeEach
    void compileFixtures() throws Exception {
        classesDir = tempDir.resolve("classes");
        Path sourceDir = tempDir.resolve("src");
        Files.createDirectories(classesDir);
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("Car.java"), """
                public class Car {
                    private int speed;

                    public Car(int yearModel, String make) {
                        this.speed = 0;
                    }

                    public void accelerate() {
                        speed += 5;
                    }

                    public int getSpeed() {
                        return speed;
                    }

                    public void flood() {
                        for (int i = 0; i < 70000; i++) {
                            System.out.print('x');
                        }
                    }
                }
                """);
        Files.writeString(sourceDir.resolve("Probe.java"), """
                public class Probe {
                    public String harness() {
                        try {
                            Class.forName("com.eiu.capstone.backend.grading.pipeline.TestcaseGrader");
                            return "harness";
                        } catch (ClassNotFoundException e) {
                            return "missing-harness";
                        }
                    }

                    public String kernel() {
                        try {
                            Class.forName("com.eiu.capstone.backend.grading.testcase.kernel.JsonValueCoercer");
                            return "kernel";
                        } catch (ClassNotFoundException e) {
                            return "missing-kernel";
                        }
                    }

                    public String workerIpc() {
                        try {
                            Class.forName("com.eiu.capstone.backend.grading.testcase.worker.WorkerIpc");
                            return "ipc";
                        } catch (ClassNotFoundException e) {
                            return "missing-ipc";
                        }
                    }

                    public String contextKernel() throws Exception {
                        return String.valueOf(Thread.currentThread().getContextClassLoader()
                                .loadClass("com.eiu.capstone.backend.grading.testcase.kernel.JsonValueCoercer"));
                    }
                }
                """);
        Process compile = new ProcessBuilder(
                "javac",
                "-d", classesDir.toString(),
                sourceDir.resolve("Car.java").toString(),
                sourceDir.resolve("Probe.java").toString())
                .redirectErrorStream(true)
                .start();
        int exit = compile.waitFor();
        String out = new String(compile.getInputStream().readAllBytes());
        assertEquals(0, exit, () -> "javac failed: " + out);
    }

    @Test
    void methodWithReceiverReturnsValueAndFieldSnapshot() {
        SerializedInvocationOutcome outcome = engine.invoke(
                classesDir,
                accelerateSpec(),
                List.of("speed"),
                WorkerIpc.DEFAULT_STDOUT_CAP);
        assertEquals("NORMAL", outcome.kind());
        assertEquals("null", outcome.returnValueJson());
        assertEquals("5", outcome.fieldSnapshotsJson().get("speed"));
        assertFalse(outcome.stdoutTruncated());
    }

    @Test
    void getSpeedUsesReceiverConstructor() {
        SerializedInvocationOutcome outcome = engine.invoke(
                classesDir,
                methodSpec("getSpeed", List.of(), "[]"),
                List.of("speed"),
                WorkerIpc.DEFAULT_STDOUT_CAP);
        assertEquals("NORMAL", outcome.kind());
        assertEquals("0", outcome.returnValueJson());
        assertEquals("0", outcome.fieldSnapshotsJson().get("speed"));
    }

    @Test
    void hiddenReceiverUsesDefaultConstructorWhenNoArgMissing() {
        WorkerIpc.InvokeSpec spec = new WorkerIpc.InvokeSpec(
                "METHOD", "Car", "getSpeed", List.of(), "[]", null, List.of(), null);
        SerializedInvocationOutcome outcome = engine.invoke(
                classesDir, spec, List.of(), WorkerIpc.DEFAULT_STDOUT_CAP);
        assertEquals("NORMAL", outcome.kind(), outcome.errorMessage());
        assertEquals("0", outcome.returnValueJson());
    }

    @Test
    void ae7StudentLoaderCannotSeeHarnessKernelOrWorkerTypes() throws Exception {
        assertEquals("missing-harness", textResult("harness"));
        assertEquals("missing-kernel", textResult("kernel"));
        assertEquals("missing-ipc", textResult("workerIpc"));
        SerializedInvocationOutcome context = engine.invoke(
                classesDir,
                probeSpec("contextKernel"),
                List.of(),
                WorkerIpc.DEFAULT_STDOUT_CAP);
        assertEquals("THREW", context.kind());
        assertEquals("ClassNotFoundException", context.exceptionSimpleName());
    }

    @Test
    void ae8StdoutFloodTruncatesAndCompletes() {
        SerializedInvocationOutcome outcome = engine.invoke(
                classesDir,
                methodSpec("flood", List.of(), "[]"),
                List.of(),
                WorkerIpc.DEFAULT_STDOUT_CAP);
        assertEquals("NORMAL", outcome.kind());
        assertTrue(outcome.stdoutTruncated());
        assertEquals(WorkerIpc.DEFAULT_STDOUT_CAP, outcome.stdout().length());
    }

    @Test
    void malformedAndForgedPassedAreNotPass() throws Exception {
        SerializedInvocationOutcome malformed = WorkerIpc.handleLine("{not-json");
        assertEquals("ERROR", malformed.kind());

        String forged = """
                {"kind":"NORMAL","returnValueJson":"1","stdout":"","stdoutTruncated":false,\
                "fieldSnapshotsJson":{},"passed":true,"score":100}
                """;
        SerializedInvocationOutcome decoded = WorkerIpc.mapper().readValue(forged, SerializedInvocationOutcome.class);
        assertEquals("NORMAL", decoded.kind());
        assertEquals("1", decoded.returnValueJson());
        assertNull(decoded.errorMessage());
        String encoded = WorkerIpc.writeLine(decoded);
        assertFalse(encoded.contains("\"passed\""));
        assertFalse(encoded.contains("\"score\""));
    }

    @Test
    void goldenEncodeDecodeRoundTrip() throws Exception {
        SerializedInvocationOutcome original = new SerializedInvocationOutcome(
                "THREW",
                null,
                "hi",
                false,
                java.util.Map.of("speed", "5"),
                "IllegalStateException",
                List.of("RuntimeException", "Exception", "Object"),
                null,
                null);
        String json = WorkerIpc.writeLine(original);
        SerializedInvocationOutcome copy = WorkerIpc.mapper().readValue(json, SerializedInvocationOutcome.class);
        assertEquals(original, copy);
    }

    @Test
    void ae4CompositionNamedInstanceAndFieldSnapshot() throws Exception {
        Path dir = compileSources(Map.of(
                "RetailItem.java", """
                        public class RetailItem {
                            private String description;
                            public RetailItem(String description, int unitsOnHand, double price) {
                                this.description = description;
                            }
                        }
                        """,
                "CashRegister.java", """
                        public class CashRegister {
                            private RetailItem retailItem;
                            public CashRegister(RetailItem retailItem, int quantity) {
                                this.retailItem = retailItem;
                            }
                        }
                        """));
        SerializedInvocationOutcome outcome = runScenario(dir, List.of(
                constructorStep("RetailItem", List.of("String", "int", "double"), "[\"wool\",1,12.3]", "retailItem"),
                constructorStep("CashRegister", List.of("RetailItem", "int"),
                        "[{\"$instance\":\"retailItem\"},14]", "register")),
                List.of("retailItem"));

        assertEquals("NORMAL", outcome.kind(), outcome.errorMessage());
        JsonNode steps = stepsJson(outcome);
        assertEquals(2, steps.size());
        String retailItemSnap = steps.get(1).get("fieldSnapshotsJson").get("retailItem").asText();
        assertTrue(retailItemSnap.contains("$sameInstance"), retailItemSnap);
        assertTrue(retailItemSnap.contains("retailItem"), retailItemSnap);
        String encoded = WorkerIpc.writeLine(outcome);
        assertFalse(encoded.contains("\"passed\""));
    }

    @Test
    void ae1GetAreaThroughShapeUsesCircleOverride() throws Exception {
        Path dir = compileSources(Map.of(
                "Shape.java", """
                        public interface Shape {
                            double getArea();
                        }
                        """,
                "Circle.java", """
                        public class Circle implements Shape {
                            public double getArea() { return 12.5; }
                            public double getAreaCheat() { return 99.0; }
                        }
                        """));
        SerializedInvocationOutcome outcome = runScenario(dir, List.of(
                constructorStep("Circle", List.of(), "[]", "circle"),
                methodStep("Circle", "getArea", List.of(), "[]", "circle", "Shape")), List.of());

        assertEquals("NORMAL", outcome.kind(), outcome.errorMessage());
        JsonNode steps = stepsJson(outcome);
        assertEquals(2, steps.size());
        assertEquals("NORMAL", steps.get(1).get("kind").asText());
        assertTrue(steps.get(1).get("kind").isTextual());
        assertEquals("12.5", steps.get(1).get("returnValueJson").asText());
    }

    @Test
    void scenarioWithoutDispatchUsesConcreteClassLookup() throws Exception {
        Map<String, Object> accelerate = methodStep("Car", "accelerate", List.of(), "[]", null, null);
        accelerate.put("receiverClassName", "Car");
        accelerate.put("receiverParameterTypes", List.of("int", "String"));
        accelerate.put("receiverParamsJson", "[2020, \"Toyota\"]");
        SerializedInvocationOutcome outcome = runScenario(classesDir, List.of(accelerate), List.of("speed"));

        assertEquals("NORMAL", outcome.kind(), outcome.errorMessage());
        JsonNode steps = stepsJson(outcome);
        assertEquals(1, steps.size());
        assertEquals("NORMAL", steps.get(0).get("kind").asText());
        assertEquals("null", steps.get(0).get("returnValueJson").asText());
        assertEquals("5", steps.get(0).get("fieldSnapshotsJson").get("speed").asText());
    }

    @Test
    void scenarioUnsupportedNonInstanceObjectTypeStillThrows() throws Exception {
        SerializedInvocationOutcome outcome = runScenario(classesDir, List.of(
                constructorStep("Probe", List.of("Object"), "[{}]", null)), List.of());

        assertEquals("NORMAL", outcome.kind(), outcome.errorMessage());
        JsonNode steps = stepsJson(outcome);
        assertEquals(1, steps.size());
        assertEquals("ERROR", steps.get(0).get("kind").asText());
        assertTrue(steps.get(0).get("errorMessage").asText().contains("Unsupported type in v1"));
    }

    @Test
    void scenarioStopsLaterStepsAfterFirstThrew() throws Exception {
        Path dir = compileSources(Map.of(
                "Boom.java", """
                        public class Boom {
                            public Boom() { throw new IllegalStateException("boom"); }
                            public int ok() { return 7; }
                        }
                        """));
        SerializedInvocationOutcome outcome = runScenario(dir, List.of(
                constructorStep("Boom", List.of(), "[]", "boom"),
                methodStep("Boom", "ok", List.of(), "[]", "boom", null)), List.of());

        assertEquals("NORMAL", outcome.kind(), outcome.errorMessage());
        JsonNode steps = stepsJson(outcome);
        assertEquals(1, steps.size());
        assertEquals("THREW", steps.get(0).get("kind").asText());
        assertEquals("IllegalStateException", steps.get(0).get("exceptionSimpleName").asText());
        for (JsonNode step : steps) {
            assertFalse("NORMAL".equals(step.get("kind").asText())
                    && "7".equals(step.get("returnValueJson").asText()));
        }
    }

    @Test
    void scenarioStopsLaterStepsAfterMethodThrew() throws Exception {
        Path dir = compileSources(Map.of(
                "Person.java", """
                        public class Person {
                            private int age;
                            public Person(int age) { this.age = age; }
                            public void setAge(int age) {
                                if (age < 0) throw new IllegalArgumentException("negative");
                                this.age = age;
                            }
                            public int getAge() { return age; }
                        }
                        """));
        SerializedInvocationOutcome outcome = runScenario(dir, List.of(
                constructorStep("Person", List.of("int"), "[30]", "person"),
                methodStep("Person", "setAge", List.of("int"), "[-5]", "person", null),
                methodStep("Person", "getAge", List.of(), "[]", "person", null)), List.of());

        assertEquals("NORMAL", outcome.kind(), outcome.errorMessage());
        JsonNode steps = stepsJson(outcome);
        assertEquals(2, steps.size());
        assertEquals("NORMAL", steps.get(0).get("kind").asText());
        assertEquals("THREW", steps.get(1).get("kind").asText());
        assertEquals("IllegalArgumentException", steps.get(1).get("exceptionSimpleName").asText());
    }

    @Test
    void staticFactoryReturnRegistersNamedInstanceForLaterArgument() throws Exception {
        Path dir = compileSources(Map.of(
                "Money.java", """
                        public class Money {
                            private final int amount;
                            private Money(int amount) { this.amount = amount; }
                            public static Money of(int amount) { return new Money(amount); }
                            public int getAmount() { return amount; }
                        }
                        """,
                "Wallet.java", """
                        public class Wallet {
                            private final Money money;
                            public Wallet(Money money) { this.money = money; }
                            public int read() { return money.getAmount(); }
                        }
                        """));
        SerializedInvocationOutcome outcome = runScenario(dir, List.of(
                methodStep("Money", "of", List.of("int"), "[50]", "cash", null),
                constructorStep("Wallet", List.of("Money"), "[{\"$instance\":\"cash\"}]", "wallet"),
                methodStep("Wallet", "read", List.of(), "[]", "wallet", null)), List.of());

        assertEquals("NORMAL", outcome.kind(), outcome.errorMessage());
        JsonNode steps = stepsJson(outcome);
        assertEquals(3, steps.size());
        assertEquals("NORMAL", steps.get(0).get("kind").asText());
        assertEquals("NORMAL", steps.get(1).get("kind").asText());
        assertEquals("NORMAL", steps.get(2).get("kind").asText());
        assertEquals("50", steps.get(2).get("returnValueJson").asText());
    }

    @Test
    void compositionEqualsNamedFactBetweenTwoInstances() throws Exception {
        Path dir = compileSources(Map.of(
                "Coin.java", """
                        public class Coin {
                            private final int value;
                            public Coin(int value) { this.value = value; }
                            @Override public boolean equals(Object other) {
                                return other instanceof Coin coin && coin.value == value;
                            }
                        }
                        """));
        SerializedInvocationOutcome outcome = runScenario(dir, List.of(
                constructorStep("Coin", List.of("int"), "[5]", "left"),
                constructorStep("Coin", List.of("int"), "[5]", "right")), List.of());

        assertEquals("NORMAL", outcome.kind(), outcome.errorMessage());
        JsonNode steps = stepsJson(outcome);
        assertEquals(2, steps.size());
        assertEquals("Coin", steps.get(1).get("objectTypeSimpleName").asText());
        assertTrue(steps.get(1).get("equalsNamed").get("left").asBoolean());
    }

    @Test
    void unitHiddenReceiverObjectReturnSnapshotsReturnTypeFields() throws Exception {
        Path dir = compileSources(Map.of(
                "Circle.java", """
                        public class Circle {
                            private double diameter;
                            public Circle(double diameter) { this.diameter = diameter; }
                        }
                        """,
                "CircleFactory.java", """
                        public class CircleFactory {
                            public Circle getShape() { return new Circle(10.0); }
                        }
                        """));
        Map<String, Object> step = methodStep("CircleFactory", "getShape", List.of(), "[]", null, null);
        SerializedInvocationOutcome outcome = runScenario(dir, List.of(step), List.of("diameter"));

        assertEquals("NORMAL", outcome.kind(), outcome.errorMessage());
        JsonNode steps = stepsJson(outcome);
        assertEquals(1, steps.size());
        assertEquals("10.0", steps.get(0).get("fieldSnapshotsJson").get("diameter").asText());
    }

    @Test
    void unitHiddenNoArgReceiverSnapshotsFieldState() throws Exception {
        Path dir = compileSources(Map.of(
                "BankAccount.java", """
                        public class BankAccount {
                            private int balance;
                            public BankAccount() { this.balance = 0; }
                            public void deposit(int amount) { this.balance += amount; }
                        }
                        """));
        Map<String, Object> deposit = methodStep("BankAccount", "deposit", List.of("int"), "[100]", null, null);
        SerializedInvocationOutcome outcome = runScenario(dir, List.of(deposit), List.of("balance"));

        assertEquals("NORMAL", outcome.kind(), outcome.errorMessage());
        JsonNode steps = stepsJson(outcome);
        assertEquals(1, steps.size());
        assertEquals("NORMAL", steps.get(0).get("kind").asText());
        assertEquals("100", steps.get(0).get("fieldSnapshotsJson").get("balance").asText());
    }

    @Test
    void compareIpcOpIsUnknown() throws Exception {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("op", "compare");
        request.put("classesDir", classesDir.toAbsolutePath().toString());
        SerializedInvocationOutcome outcome = WorkerIpc.handleLine(WorkerIpc.mapper().writeValueAsString(request));
        assertEquals("ERROR", outcome.kind());
        assertTrue(outcome.errorMessage() != null && outcome.errorMessage().toLowerCase().contains("unknown"),
                outcome.errorMessage());
    }

    @Test
    void scenarioWireKindStaysAString() throws Exception {
        Map<String, Object> accelerate = methodStep("Car", "accelerate", List.of(), "[]", null, null);
        accelerate.put("receiverClassName", "Car");
        accelerate.put("receiverParameterTypes", List.of("int", "String"));
        accelerate.put("receiverParamsJson", "[2020, \"Toyota\"]");
        SerializedInvocationOutcome outcome = runScenario(classesDir, List.of(accelerate), List.of());
        String encoded = WorkerIpc.writeLine(outcome);
        JsonNode node = WorkerIpc.mapper().readTree(encoded);
        assertTrue(node.get("kind").isTextual());
        assertEquals("NORMAL", node.get("kind").asText());
        assertFalse(encoded.contains("\"passed\""));
        assertEquals(String.class, outcome.kind().getClass());
    }

    private String textResult(String method) throws Exception {
        SerializedInvocationOutcome outcome = engine.invoke(
                classesDir, probeSpec(method), List.of(), WorkerIpc.DEFAULT_STDOUT_CAP);
        assertEquals("NORMAL", outcome.kind(), outcome.errorMessage());
        return WorkerIpc.mapper().readTree(outcome.returnValueJson()).asText();
    }

    private WorkerIpc.InvokeSpec accelerateSpec() {
        return methodSpec("accelerate", List.of(), "[]");
    }

    private WorkerIpc.InvokeSpec methodSpec(String method, List<String> types, String params) {
        return new WorkerIpc.InvokeSpec(
                "METHOD",
                "Car",
                method,
                types,
                params,
                "Car",
                List.of("int", "String"),
                "[2020, \"Toyota\"]");
    }

    private WorkerIpc.InvokeSpec probeSpec(String method) {
        return new WorkerIpc.InvokeSpec(
                "METHOD", "Probe", method, List.of(), "[]", null, List.of(), null);
    }

    private SerializedInvocationOutcome runScenario(Path dir,
                                                    List<Map<String, Object>> steps,
                                                    List<String> snapshotFields) throws Exception {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("op", "scenario");
        request.put("classesDir", dir.toAbsolutePath().toString());
        request.put("snapshotFieldNames", snapshotFields);
        request.put("stdoutCap", WorkerIpc.DEFAULT_STDOUT_CAP);
        request.put("steps", steps);
        return WorkerIpc.handleLine(WorkerIpc.mapper().writeValueAsString(request));
    }

    private JsonNode stepsJson(SerializedInvocationOutcome outcome) throws Exception {
        JsonNode node = WorkerIpc.mapper().readTree(WorkerIpc.writeLine(outcome));
        JsonNode steps = node.get("steps");
        assertTrue(steps != null && steps.isArray(), () -> "missing steps array: " + node);
        return steps;
    }

    private Map<String, Object> constructorStep(String className,
                                                List<String> parameterTypes,
                                                String paramsJson,
                                                String instanceName) {
        Map<String, Object> spec = new LinkedHashMap<>();
        spec.put("kind", "CONSTRUCTOR");
        spec.put("className", className);
        spec.put("parameterTypes", parameterTypes);
        spec.put("paramsJson", paramsJson);
        spec.put("instanceName", instanceName);
        return spec;
    }

    private Map<String, Object> methodStep(String className,
                                           String methodName,
                                           List<String> parameterTypes,
                                           String paramsJson,
                                           String instanceName,
                                           String dispatchClassName) {
        Map<String, Object> spec = new LinkedHashMap<>();
        spec.put("kind", "METHOD");
        spec.put("className", className);
        spec.put("methodName", methodName);
        spec.put("parameterTypes", parameterTypes);
        spec.put("paramsJson", paramsJson);
        spec.put("instanceName", instanceName);
        spec.put("dispatchClassName", dispatchClassName);
        return spec;
    }

    @Test
    void batch_runsMultipleScenarios() {
        WorkerIpc.BatchItemSpec speed = new WorkerIpc.BatchItemSpec(List.of(
                new WorkerIpc.ScenarioStepSpec(
                        "CONSTRUCTOR", "Car", null, List.of("int", "String"), "[2020,\"X\"]",
                        null, null, null, "car", null),
                new WorkerIpc.ScenarioStepSpec(
                        "METHOD", "Car", "getSpeed", List.of(), "[]",
                        null, null, null, "car", null)), List.of());
        WorkerIpc.BatchItemSpec accel = new WorkerIpc.BatchItemSpec(List.of(
                new WorkerIpc.ScenarioStepSpec(
                        "CONSTRUCTOR", "Car", null, List.of("int", "String"), "[2020,\"X\"]",
                        null, null, null, "car", null),
                new WorkerIpc.ScenarioStepSpec(
                        "METHOD", "Car", "accelerate", List.of(), "[]",
                        null, null, null, "car", null),
                new WorkerIpc.ScenarioStepSpec(
                        "METHOD", "Car", "getSpeed", List.of(), "[]",
                        null, null, null, "car", null)), List.of());

        SerializedInvocationOutcome result = engine.batch(classesDir, List.of(speed, accel), 5, 65536);
        assertEquals(SerializedInvocationOutcome.KIND_NORMAL, result.kind());
        assertEquals(2, result.batch().size());
        assertEquals(SerializedInvocationOutcome.KIND_NORMAL, result.batch().get(0).kind());
        assertEquals(SerializedInvocationOutcome.KIND_NORMAL, result.batch().get(1).kind());
        assertEquals("0", result.batch().get(0).steps().get(1).returnValueJson());
        assertEquals("5", result.batch().get(1).steps().get(2).returnValueJson());
    }

    @Test
    void batch_warmSingleUnitUnder300ms() {
        WorkerIpc.BatchItemSpec unit = new WorkerIpc.BatchItemSpec(List.of(
                new WorkerIpc.ScenarioStepSpec(
                        "METHOD", "Car", "getSpeed", List.of(), "[]",
                        "Car", List.of("int", "String"), "[2020,\"X\"]", null, null)), List.of("speed"));
        // Warm classloader / executor path once so the measured call excludes cold JIT.
        engine.batch(classesDir, List.of(unit), 5, WorkerIpc.DEFAULT_STDOUT_CAP);

        long started = System.nanoTime();
        SerializedInvocationOutcome result = engine.batch(
                classesDir, List.of(unit), 5, WorkerIpc.DEFAULT_STDOUT_CAP);
        long elapsedMs = (System.nanoTime() - started) / 1_000_000L;

        assertEquals(SerializedInvocationOutcome.KIND_NORMAL, result.kind());
        assertEquals(1, result.batch().size());
        assertEquals("0", result.batch().get(0).steps().get(0).returnValueJson());
        assertTrue(elapsedMs < 300,
                () -> "warm one-item UNIT batch took " + elapsedMs + "ms (budget 300ms)");
    }

    @Test
    void batch_timesOutStopsWithoutRunningLaterItemsInSameJvm() throws Exception {
        Path hangDir = compileSources(Map.of(
                "Hang.java", """
                        public class Hang {
                            public int spin() {
                                while (true) {
                                    Thread.onSpinWait();
                                }
                            }
                        }
                        """,
                "Ok.java", """
                        public class Ok {
                            public int value() { return 7; }
                        }
                        """));
        WorkerIpc.BatchItemSpec hang = new WorkerIpc.BatchItemSpec(List.of(
                new WorkerIpc.ScenarioStepSpec(
                        "METHOD", "Hang", "spin", List.of(), "[]",
                        "Hang", List.of(), "[]", null, null)), List.of());
        WorkerIpc.BatchItemSpec ok = new WorkerIpc.BatchItemSpec(List.of(
                new WorkerIpc.ScenarioStepSpec(
                        "METHOD", "Ok", "value", List.of(), "[]",
                        "Ok", List.of(), "[]", null, null)), List.of());

        SerializedInvocationOutcome result = engine.batch(hangDir, List.of(hang, ok), 1, 65536);
        assertEquals(1, result.batch().size());
        assertEquals(SerializedInvocationOutcome.KIND_TIMED_OUT, result.batch().get(0).kind());
    }

    private Path compileSources(Map<String, String> sources) throws Exception {
        Path dir = tempDir.resolve("classes-" + UUID.randomUUID());
        Path sourceDir = tempDir.resolve("src-" + UUID.randomUUID());
        Files.createDirectories(dir);
        Files.createDirectories(sourceDir);
        List<String> command = new ArrayList<>();
        command.add("javac");
        command.add("-d");
        command.add(dir.toString());
        for (Map.Entry<String, String> source : sources.entrySet()) {
            Path file = sourceDir.resolve(source.getKey());
            Files.writeString(file, source.getValue());
            command.add(file.toString());
        }
        Process compile = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();
        int exit = compile.waitFor();
        String out = new String(compile.getInputStream().readAllBytes());
        assertEquals(0, exit, () -> "javac failed: " + out);
        return dir;
    }
}
