package com.eiu.capstone.backend.grading.testcase.worker;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import com.eiu.capstone.backend.grading.testcase.AssertionEvaluator;
import com.eiu.capstone.backend.grading.testcase.SerializedInvocationOutcome;
import com.eiu.capstone.backend.grading.testcase.kernel.JavaTypeResolver;
import com.eiu.capstone.backend.grading.testcase.kernel.JsonValueCoercer;
import com.eiu.capstone.backend.model.InvocationKind;

public final class WorkerInvokeEngine {

    private final JsonValueCoercer coercer = new JsonValueCoercer();

    public SerializedInvocationOutcome invoke(Path classesDir,
                                              WorkerIpc.InvokeSpec spec,
                                              List<String> snapshotFieldNames,
                                              int stdoutCap) {
        return invoke(classesDir, spec, snapshotFieldNames, null, stdoutCap);
    }

    public SerializedInvocationOutcome invoke(Path classesDir,
                                              WorkerIpc.InvokeSpec spec,
                                              List<String> snapshotFieldNames,
                                              Map<String, List<String>> snapshotFieldAliases,
                                              int stdoutCap) {
        if (!Files.isDirectory(classesDir)) {
            return SerializedInvocationOutcome.error("Missing compiled classes directory");
        }
        if (spec == null || spec.className() == null || spec.className().isBlank()) {
            return SerializedInvocationOutcome.error("Missing invocation rubric");
        }
        try (URLClassLoader loader = studentLoader(classesDir)) {
            return executeStep(
                    loader, toStep(spec), snapshotFieldNames, snapshotFieldAliases,
                    stdoutCap, new LinkedHashMap<>());
        } catch (Exception e) {
            return SerializedInvocationOutcome.error(messageOrSimpleName(e));
        }
    }

    public SerializedInvocationOutcome scenario(Path classesDir,
                                                List<WorkerIpc.ScenarioStepSpec> steps,
                                                List<String> snapshotFieldNames,
                                                int stdoutCap) {
        return scenario(classesDir, steps, snapshotFieldNames, null, stdoutCap);
    }

    public SerializedInvocationOutcome scenario(Path classesDir,
                                                List<WorkerIpc.ScenarioStepSpec> steps,
                                                List<String> snapshotFieldNames,
                                                Map<String, List<String>> snapshotFieldAliases,
                                                int stdoutCap) {
        if (!Files.isDirectory(classesDir)) {
            return SerializedInvocationOutcome.error("Missing compiled classes directory");
        }
        if (steps == null || steps.isEmpty()) {
            return SerializedInvocationOutcome.error("Missing scenario steps");
        }
        try (URLClassLoader loader = studentLoader(classesDir)) {
            return scenario(loader, steps, snapshotFieldNames, snapshotFieldAliases, stdoutCap);
        } catch (Exception e) {
            return SerializedInvocationOutcome.error(messageOrSimpleName(e));
        }
    }

    /**
     * Run each batch item as a scenario under {@code timeoutSeconds} of execution wall-clock.
     * On timeout, emit {@code TIMED_OUT} and omit later items from this response so the API can
     * respawn and continue (interrupt cannot stop tight loops; the process must be killed).
     * Each item uses its own student {@link URLClassLoader}. Concurrent items share a pool sized
     * {@code max(1, min(itemCount, parallelismOrCpus))}.
     */
    public SerializedInvocationOutcome batch(Path classesDir,
                                             List<WorkerIpc.BatchItemSpec> items,
                                             int timeoutSeconds,
                                             int stdoutCap) {
        return batch(classesDir, items, timeoutSeconds, stdoutCap, 0);
    }

    public SerializedInvocationOutcome batch(Path classesDir,
                                             List<WorkerIpc.BatchItemSpec> items,
                                             int timeoutSeconds,
                                             int stdoutCap,
                                             int batchParallelism) {
        if (items == null || items.isEmpty()) {
            return SerializedInvocationOutcome.error("Missing batch items");
        }
        int budget = Math.max(1, timeoutSeconds);
        int poolSize = resolveBatchPoolSize(items.size(), batchParallelism);
        ExecutorService executor = Executors.newFixedThreadPool(poolSize, r -> {
            Thread thread = new Thread(r, "worker-batch-item");
            thread.setDaemon(true);
            return thread;
        });
        List<Future<SerializedInvocationOutcome>> futures = new ArrayList<>(items.size());
        List<Long> deadlineNanos = new ArrayList<>(items.size());
        try {
            for (WorkerIpc.BatchItemSpec item : items) {
                Path itemDir = resolveItemClassesDir(classesDir, item);
                futures.add(executor.submit(() -> runScenarioItem(itemDir, item, stdoutCap)));
                deadlineNanos.add(System.nanoTime() + TimeUnit.SECONDS.toNanos(budget));
            }
            List<SerializedInvocationOutcome> results = new ArrayList<>();
            for (int i = 0; i < futures.size(); i++) {
                Future<SerializedInvocationOutcome> future = futures.get(i);
                long remainingNs = deadlineNanos.get(i) - System.nanoTime();
                SerializedInvocationOutcome outcome;
                if (remainingNs <= 0) {
                    future.cancel(true);
                    outcome = SerializedInvocationOutcome.timedOut("", false);
                } else {
                    try {
                        outcome = future.get(remainingNs, TimeUnit.NANOSECONDS);
                    } catch (TimeoutException e) {
                        future.cancel(true);
                        outcome = SerializedInvocationOutcome.timedOut("", false);
                    } catch (ExecutionException e) {
                        outcome = SerializedInvocationOutcome.error(messageOrSimpleName(
                                e.getCause() != null ? e.getCause() : e));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        future.cancel(true);
                        outcome = SerializedInvocationOutcome.timedOut("", false);
                    }
                }
                if (outcome == null) {
                    outcome = SerializedInvocationOutcome.error("Empty batch item outcome");
                }
                results.add(outcome);
                if (SerializedInvocationOutcome.KIND_TIMED_OUT.equals(outcome.kind())) {
                    for (int j = i + 1; j < futures.size(); j++) {
                        futures.get(j).cancel(true);
                    }
                    break;
                }
            }
            return SerializedInvocationOutcome.batchOf(results);
        } catch (Exception e) {
            return SerializedInvocationOutcome.error(messageOrSimpleName(e));
        } finally {
            executor.shutdownNow();
        }
    }

    public static int resolveBatchPoolSize(int itemCount, int batchParallelism) {
        int cpuCap = Math.max(1, Runtime.getRuntime().availableProcessors());
        int requested = batchParallelism > 0 ? batchParallelism : cpuCap;
        return Math.max(1, Math.min(Math.max(1, itemCount), Math.min(cpuCap, requested)));
    }

    private static Path resolveItemClassesDir(Path requestClassesDir, WorkerIpc.BatchItemSpec item) {
        if (item != null && item.classesDir() != null && !item.classesDir().isBlank()) {
            return Path.of(item.classesDir());
        }
        return requestClassesDir;
    }

    private SerializedInvocationOutcome runScenarioItem(Path classesDir,
                                                        WorkerIpc.BatchItemSpec item,
                                                        int stdoutCap) {
        if (classesDir == null || !Files.isDirectory(classesDir)) {
            return SerializedInvocationOutcome.error("Missing compiled classes directory");
        }
        try (URLClassLoader loader = studentLoader(classesDir)) {
            List<WorkerIpc.ScenarioStepSpec> steps = item == null ? null : item.steps();
            List<String> snapshots = item == null ? null : item.snapshotFieldNames();
            Map<String, List<String>> snapshotAliases = item == null ? null : item.snapshotFieldAliases();
            return scenario(loader, steps, snapshots, snapshotAliases, stdoutCap);
        } catch (Exception e) {
            return SerializedInvocationOutcome.error(messageOrSimpleName(e));
        }
    }

    private SerializedInvocationOutcome scenario(URLClassLoader loader,
                                                 List<WorkerIpc.ScenarioStepSpec> steps,
                                                 List<String> snapshotFieldNames,
                                                 Map<String, List<String>> snapshotFieldAliases,
                                                 int stdoutCap) {
        Map<String, Object> registry = new LinkedHashMap<>();
        List<SerializedInvocationOutcome> outcomes = new ArrayList<>();
        for (WorkerIpc.ScenarioStepSpec step : steps) {
            SerializedInvocationOutcome outcome = executeStep(
                    loader, step, snapshotFieldNames, snapshotFieldAliases, stdoutCap, registry);
            outcomes.add(outcome);
            if (shouldStopScenario(outcome)) {
                break;
            }
        }
        return new SerializedInvocationOutcome(
                SerializedInvocationOutcome.KIND_NORMAL,
                null,
                "",
                false,
                Map.of(),
                null,
                List.of(),
                null,
                null,
                List.copyOf(outcomes));
    }

    /**
     * {@link System#setOut} is process-global. Each invoke publishes its own
     * {@link BoundedStdout} on a thread-local so parallel batch items do not share a buffer.
     * A timed-out call that ignores interrupt never reaches {@code finally}; a monitor held
     * across that call would block every later invoke in the same JVM (Surefire).
     */
    private static final ThreadLocal<BoundedStdout> CAPTURED_STDOUT = new ThreadLocal<>();
    private static final Object STDOUT_INSTALL_LOCK = new Object();
    private static volatile java.io.PrintStream originalStdout;
    private static volatile java.io.PrintStream delegatingStdout;
    private static int activeStdoutCaptures;

    private static final class DelegatingStdout extends java.io.OutputStream {
        @Override
        public void write(int b) {
            BoundedStdout captured = CAPTURED_STDOUT.get();
            if (captured != null) {
                captured.write(b);
                return;
            }
            java.io.PrintStream fallback = originalStdout;
            if (fallback != null) {
                fallback.write(b);
            }
        }

        @Override
        public void write(byte[] b, int off, int len) {
            BoundedStdout captured = CAPTURED_STDOUT.get();
            if (captured != null) {
                captured.write(b, off, len);
                return;
            }
            java.io.PrintStream fallback = originalStdout;
            if (fallback != null) {
                fallback.write(b, off, len);
            }
        }

        @Override
        public void flush() {
            if (CAPTURED_STDOUT.get() != null) {
                return;
            }
            java.io.PrintStream fallback = originalStdout;
            if (fallback != null) {
                fallback.flush();
            }
        }
    }

    private static void beginStdoutCapture(BoundedStdout stdout) {
        synchronized (STDOUT_INSTALL_LOCK) {
            if (delegatingStdout == null) {
                delegatingStdout = new java.io.PrintStream(new DelegatingStdout(), true);
            }
            if (System.out != delegatingStdout) {
                originalStdout = System.out;
                System.setOut(delegatingStdout);
            }
            activeStdoutCaptures++;
        }
        CAPTURED_STDOUT.set(stdout);
    }

    private static void endStdoutCapture() {
        CAPTURED_STDOUT.remove();
        synchronized (STDOUT_INSTALL_LOCK) {
            if (activeStdoutCaptures > 0) {
                activeStdoutCaptures--;
            }
            if (activeStdoutCaptures == 0 && originalStdout != null) {
                System.setOut(originalStdout);
                originalStdout = null;
            }
        }
    }

    private SerializedInvocationOutcome executeStep(URLClassLoader loader,
                                                    WorkerIpc.ScenarioStepSpec spec,
                                                    List<String> snapshotFieldNames,
                                                    Map<String, List<String>> snapshotFieldAliases,
                                                    int stdoutCap,
                                                    Map<String, Object> registry) {
        if (spec == null || spec.className() == null || spec.className().isBlank()) {
            return SerializedInvocationOutcome.error("Missing invocation rubric");
        }
        Function<String, Object> namedInstances = name -> {
            if (!registry.containsKey(name)) {
                throw new IllegalArgumentException("Unknown named instance: " + name);
            }
            return registry.get(name);
        };
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(loader);
        BoundedStdout stdout = new BoundedStdout(stdoutCap);
        Object receiver = null;
        boolean capturing = false;
        try {
            Class<?> clazz = findClass(loader, spec.className());
            List<String> parameterTypes = spec.parameterTypes() != null ? spec.parameterTypes() : List.of();
            beginStdoutCapture(stdout);
            capturing = true;
            if (kind(spec) == InvocationKind.CONSTRUCTOR) {
                Object instance;
                if (spec.implicitDefaultConstructor()) {
                    instance = instantiateHiddenReceiver(loader, clazz);
                } else {
                    ResolvedConstructor resolved = resolveConstructor(
                            clazz, parameterTypes, spec.parameterTypeAliases(), loader);
                    Object[] args = coercer.coerceParams(
                            spec.paramsJson(), resolved.parameterTypes(), namedInstances, loader);
                    instance = resolved.constructor().newInstance(args);
                }
                ObjectFacts facts = describeObject(loader, instance, registry);
                register(registry, spec.instanceName(), instance);
                List<String> scopedFields = spec.snapshotFieldNames() != null
                        ? spec.snapshotFieldNames() : snapshotFieldNames;
                Map<String, List<String>> scopedAliases = spec.snapshotFieldAliases() != null
                        ? spec.snapshotFieldAliases() : snapshotFieldAliases;
                return normal(
                        instance, instance, stdout, scopedFields, scopedAliases, facts, registry);
            }
            ResolvedMethod resolved = resolveMethod(loader, spec, clazz, parameterTypes);
            Method method = resolved.method();
            Object[] args = coercer.coerceParams(
                    spec.paramsJson(), resolved.parameterTypes(), namedInstances, loader);
            if (!Modifier.isStatic(method.getModifiers())) {
                receiver = resolveReceiver(loader, spec, clazz, registry, namedInstances);
            }
            int repeats = effectiveRepeatCount(spec);
            Object returnValue = null;
            for (int iteration = 0; iteration < repeats; iteration++) {
                returnValue = method.invoke(receiver, args);
            }
            ObjectFacts facts = describeObject(loader, returnValue, registry);
            maybeRegisterMethodProduct(loader, method, spec, receiver, returnValue, registry);
            List<String> scopedFields = spec.snapshotFieldNames() != null
                    ? spec.snapshotFieldNames() : snapshotFieldNames;
            Map<String, List<String>> scopedAliases = spec.snapshotFieldAliases() != null
                    ? spec.snapshotFieldAliases() : snapshotFieldAliases;
            return normal(
                    receiver, returnValue, stdout, scopedFields, scopedAliases, facts, registry);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            List<String> scopedFields = spec.snapshotFieldNames() != null
                    ? spec.snapshotFieldNames() : snapshotFieldNames;
            Map<String, List<String>> scopedAliases = spec.snapshotFieldAliases() != null
                    ? spec.snapshotFieldAliases() : snapshotFieldAliases;
            return threw(receiver, cause, stdout, scopedFields, scopedAliases, registry);
        } catch (Exception e) {
            return SerializedInvocationOutcome.error(messageOrSimpleName(e));
        } finally {
            if (capturing) {
                endStdoutCapture();
            }
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    private static boolean shouldStopScenario(SerializedInvocationOutcome outcome) {
        if (outcome == null || outcome.kind() == null) {
            return true;
        }
        if (SerializedInvocationOutcome.KIND_ERROR.equals(outcome.kind())
                || SerializedInvocationOutcome.KIND_TIMED_OUT.equals(outcome.kind())) {
            return true;
        }
        return SerializedInvocationOutcome.KIND_THREW.equals(outcome.kind());
    }

    private Object resolveReceiver(URLClassLoader loader,
                                   WorkerIpc.ScenarioStepSpec spec,
                                   Class<?> clazz,
                                   Map<String, Object> registry,
                                   Function<String, Object> namedInstances) throws Exception {
        if (hasInstanceName(spec)) {
            if (!registry.containsKey(spec.instanceName())) {
                throw new IllegalArgumentException("Unknown named instance: " + spec.instanceName());
            }
            return registry.get(spec.instanceName());
        }
        if (hasReceiver(spec)) {
            return instantiateReceiver(loader, spec, namedInstances);
        }
        return instantiateHiddenReceiver(loader, clazz);
    }

    private ResolvedMethod resolveMethod(URLClassLoader loader,
                                         WorkerIpc.ScenarioStepSpec spec,
                                         Class<?> concrete,
                                         List<String> parameterTypes)
            throws NoSuchMethodException, ClassNotFoundException {
        List<String> names = spec.methodNameAliases() == null || spec.methodNameAliases().isEmpty()
                ? List.of(spec.methodName())
                : spec.methodNameAliases();
        List<List<String>> typeAliases = normalizedTypeAliases(parameterTypes, spec.parameterTypeAliases());
        Class<?> dispatchType = spec.dispatchClassName() != null && !spec.dispatchClassName().isBlank()
                ? findClass(loader, spec.dispatchClassName())
                : null;
        IllegalArgumentException firstTypeResolutionFailure = null;
        for (String name : names) {
            if (name == null || name.isBlank()) {
                continue;
            }
            TypeCombinationCursor combinations = new TypeCombinationCursor(typeAliases);
            while (combinations.hasNext()) {
                List<String> combination = combinations.next();
                try {
                    Class<?>[] types = JavaTypeResolver.resolveAll(combination, loader);
                    Method method = dispatchType != null
                            ? findDispatchMethod(dispatchType, name, types)
                            : findDeclaredMethod(concrete, name, types);
                    return new ResolvedMethod(method, combination);
                } catch (IllegalArgumentException failure) {
                    if (firstTypeResolutionFailure == null) {
                        firstTypeResolutionFailure = failure;
                    }
                } catch (NoSuchMethodException ignored) {
                    // Try the next lecturer-authored Cartesian alias combination.
                }
            }
        }
        if (firstTypeResolutionFailure != null) {
            throw firstTypeResolutionFailure;
        }
        throw new NoSuchMethodException(
                concrete.getName() + "." + spec.methodName() + "(" + String.join(",", parameterTypes) + ")");
    }

    private static List<List<String>> normalizedTypeAliases(
            List<String> primaryTypes,
            List<List<String>> aliases) {
        if (aliases == null || aliases.size() != primaryTypes.size()) {
            return primaryTypes.stream().map(List::of).toList();
        }
        List<List<String>> normalized = new ArrayList<>(aliases.size());
        for (int i = 0; i < aliases.size(); i++) {
            List<String> choices = aliases.get(i);
            normalized.add(choices == null || choices.isEmpty() ? List.of(primaryTypes.get(i)) : choices);
        }
        return normalized;
    }

    private SerializedInvocationOutcome normal(Object instance,
                                               Object returnValue,
                                               BoundedStdout stdout,
                                               List<String> snapshotFieldNames,
                                               Map<String, List<String>> snapshotFieldAliases,
                                               ObjectFacts facts,
                                               Map<String, Object> registry) {
        Object snapshotTarget = fieldSnapshotTarget(instance, returnValue, facts);
        return new SerializedInvocationOutcome(
                SerializedInvocationOutcome.KIND_NORMAL,
                coercer.toJson(returnValue),
                stdout.text(),
                stdout.truncated(),
                snapshotFields(snapshotTarget, snapshotFieldNames, snapshotFieldAliases, registry),
                null,
                List.of(),
                null,
                null,
                facts.typeName(),
                facts.fields(),
                facts.equalsNamed(),
                null,
                null);
    }

    private SerializedInvocationOutcome threw(Object instance,
                                              Throwable cause,
                                              BoundedStdout stdout,
                                              List<String> snapshotFieldNames,
                                              Map<String, List<String>> snapshotFieldAliases,
                                              Map<String, Object> registry) {
        List<String> supers = new ArrayList<>();
        for (Class<?> type = cause.getClass().getSuperclass(); type != null; type = type.getSuperclass()) {
            supers.add(type.getSimpleName());
        }
        Object snapshotTarget = instance;
        return new SerializedInvocationOutcome(
                SerializedInvocationOutcome.KIND_THREW,
                null,
                stdout.text(),
                stdout.truncated(),
                snapshotFields(snapshotTarget, snapshotFieldNames, snapshotFieldAliases, registry),
                cause.getClass().getSimpleName(),
                List.copyOf(supers),
                null,
                null);
    }

    /**
     * FIELD_STATE reads from the constructed instance, the method return (when it is a rubric object),
     * or the receiver for void / primitive returns.
     */
    private static Object fieldSnapshotTarget(Object receiver, Object returnValue, ObjectFacts returnFacts) {
        if (returnValue != null && returnFacts != null && returnFacts.typeName() != null) {
            return returnValue;
        }
        return receiver != null ? receiver : returnValue;
    }

    private Map<String, String> snapshotFields(Object target,
                                               List<String> fieldNames,
                                               Map<String, List<String>> fieldAliases,
                                               Map<String, Object> registry) {
        Map<String, String> snapshots = new LinkedHashMap<>();
        if (target == null || fieldNames == null) {
            return snapshots;
        }
        for (String fieldName : fieldNames) {
            if (fieldName == null || fieldName.isBlank()) {
                continue;
            }
            try {
                List<String> aliases = fieldAliases != null
                        ? fieldAliases.getOrDefault(fieldName, List.of(fieldName))
                        : List.of(fieldName);
                snapshots.put(fieldName, encodeFieldSnapshot(readField(target, aliases), registry));
            } catch (ReflectiveOperationException ignored) {
                // Absence is distinct from a present field whose value is null.
            }
        }
        return snapshots;
    }

    private String encodeFieldSnapshot(Object value, Map<String, Object> registry) {
        if (value == null) {
            return "null";
        }
        if (registry != null) {
            for (Map.Entry<String, Object> entry : registry.entrySet()) {
                if (entry.getValue() == value) {
                    return coercer.toJson(Map.of("$sameInstance", entry.getKey()));
                }
            }
        }
        return coercer.toJson(value);
    }

    private Object readField(Object target, List<String> fieldNames) throws ReflectiveOperationException {
        for (String fieldName : fieldNames) {
            Class<?> type = target.getClass();
            while (type != null) {
                try {
                    Field field = type.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    return field.get(target);
                } catch (NoSuchFieldException ignored) {
                    type = type.getSuperclass();
                }
            }
        }
        throw new NoSuchFieldException(String.join(", ", fieldNames));
    }

    private Object instantiateReceiver(URLClassLoader loader,
                                       WorkerIpc.ScenarioStepSpec spec,
                                       Function<String, Object> namedInstances) throws Exception {
        return instantiateWithConstructor(
                loader,
                spec.receiverClassName(),
                spec.receiverParameterTypes() != null ? spec.receiverParameterTypes() : List.of(),
                spec.receiverParameterTypeAliases(),
                spec.receiverParamsJson(),
                namedInstances);
    }

    private Object instantiateWithConstructor(URLClassLoader loader,
                                            String className,
                                            List<String> parameterTypes,
                                            List<List<String>> parameterTypeAliases,
                                            String paramsJson,
                                            Function<String, Object> namedInstances) throws Exception {
        Class<?> clazz = findClass(loader, className);
        if (parameterTypes == null || parameterTypes.isEmpty()) {
            return instantiateHiddenReceiver(loader, clazz);
        }
        ResolvedConstructor resolved = resolveConstructor(
                clazz, parameterTypes, parameterTypeAliases, loader);
        Object[] args = coercer.coerceParams(
                paramsJson, resolved.parameterTypes(), namedInstances, loader);
        return resolved.constructor().newInstance(args);
    }

    /**
     * Unit OT hidden receiver: use a no-arg constructor when bytecode provides one; otherwise
     * the shortest constructor whose parameters can be filled with JVM-style literal defaults
     * (0, false, '\0', null reference types, empty arrays).
     */
    private Object instantiateHiddenReceiver(URLClassLoader loader, Class<?> clazz) throws ReflectiveOperationException {
        try {
            Constructor<?> noArg = clazz.getDeclaredConstructor();
            noArg.setAccessible(true);
            return noArg.newInstance();
        } catch (NoSuchMethodException ignored) {
            // fall through to default-arg constructor
        }
        Constructor<?> chosen = chooseDefaultConstructor(clazz, loader);
        if (chosen == null) {
            throw new NoSuchMethodException(
                    "Instance method requires a constructible hidden receiver on " + clazz.getSimpleName());
        }
        chosen.setAccessible(true);
        Class<?>[] paramTypes = chosen.getParameterTypes();
        Object[] args = new Object[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            args[i] = defaultLiteralArgument(paramTypes[i], loader);
        }
        try {
            return chosen.newInstance(args);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof ReflectiveOperationException) {
                throw (ReflectiveOperationException) cause;
            }
            throw e;
        }
    }

    private static Constructor<?> chooseDefaultConstructor(Class<?> clazz, URLClassLoader loader) {
        Constructor<?> best = null;
        int bestArity = Integer.MAX_VALUE;
        for (Constructor<?> candidate : clazz.getDeclaredConstructors()) {
            Class<?>[] paramTypes = candidate.getParameterTypes();
            if (!acceptsLiteralDefaultArgs(paramTypes, loader)) {
                continue;
            }
            if (paramTypes.length < bestArity) {
                best = candidate;
                bestArity = paramTypes.length;
            }
        }
        return best;
    }

    private static boolean acceptsLiteralDefaultArgs(Class<?>[] paramTypes, URLClassLoader loader) {
        for (Class<?> type : paramTypes) {
            if (!acceptsLiteralDefaultArg(type, loader)) {
                return false;
            }
        }
        return true;
    }

    private static boolean acceptsLiteralDefaultArg(Class<?> type, URLClassLoader loader) {
        if (type.isPrimitive()) {
            return true;
        }
        if (type == String.class) {
            return true;
        }
        if (Number.class.isAssignableFrom(type) || type == Boolean.class || type == Character.class) {
            return true;
        }
        if (type.isArray()) {
            return acceptsLiteralDefaultArg(type.getComponentType(), loader);
        }
        if (type == java.time.LocalDate.class) {
            return true;
        }
        if (loader != null && type.getClassLoader() == loader) {
            return false;
        }
        return false;
    }

    private static Object defaultLiteralArgument(Class<?> type, URLClassLoader loader) {
        if (type.isPrimitive()) {
            if (type == boolean.class) {
                return false;
            }
            if (type == char.class) {
                return '\0';
            }
            if (type == float.class) {
                return 0f;
            }
            if (type == double.class) {
                return 0d;
            }
            if (type == long.class) {
                return 0L;
            }
            return 0;
        }
        if (type == String.class) {
            return null;
        }
        if (Number.class.isAssignableFrom(type) || type == Boolean.class || type == Character.class) {
            return null;
        }
        if (type.isArray()) {
            return Array.newInstance(type.getComponentType(), 0);
        }
        if (type == java.time.LocalDate.class) {
            return null;
        }
        return null;
    }

    static URLClassLoader studentLoader(Path classesDir) throws Exception {
        URL url = classesDir.toUri().toURL();
        return new URLClassLoader(new URL[] {url}, ClassLoader.getPlatformClassLoader());
    }

    private Class<?> findClass(URLClassLoader loader, String className) throws ClassNotFoundException {
        return JavaTypeResolver.loadStudentClass(className, loader);
    }

    private ResolvedConstructor resolveConstructor(
            Class<?> clazz,
            List<String> parameterTypes,
            List<List<String>> parameterTypeAliases,
            URLClassLoader loader) throws NoSuchMethodException {
        TypeCombinationCursor combinations = new TypeCombinationCursor(
                normalizedTypeAliases(parameterTypes, parameterTypeAliases));
        IllegalArgumentException firstTypeResolutionFailure = null;
        while (combinations.hasNext()) {
            List<String> combination = combinations.next();
            try {
                Class<?>[] types = JavaTypeResolver.resolveAll(combination, loader);
                Constructor<?> constructor = clazz.getDeclaredConstructor(types);
                constructor.setAccessible(true);
                return new ResolvedConstructor(constructor, combination);
            } catch (IllegalArgumentException failure) {
                if (firstTypeResolutionFailure == null) {
                    firstTypeResolutionFailure = failure;
                }
            } catch (NoSuchMethodException ignored) {
                // One missing alias is a miss, not a reason to abandon later valid aliases.
            }
        }
        if (firstTypeResolutionFailure != null) {
            throw firstTypeResolutionFailure;
        }
        throw new NoSuchMethodException(
                clazz.getName() + "(" + String.join(",", parameterTypes) + ")");
    }

    private Method findDeclaredMethod(Class<?> clazz, String name, Class<?>[] types)
            throws NoSuchMethodException {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                Method method = current.getDeclaredMethod(name, types);
                if (!method.isSynthetic() && !method.isBridge()) {
                    method.setAccessible(true);
                    return method;
                }
            } catch (NoSuchMethodException ignored) {
                // Fall through to a same-types scan, then the Extends superclass.
            }
            for (Method candidate : current.getDeclaredMethods()) {
                if (candidate.isSynthetic() || candidate.isBridge()) {
                    continue;
                }
                if (candidate.getName().equals(name) && sameTypes(candidate.getParameterTypes(), types)) {
                    candidate.setAccessible(true);
                    return candidate;
                }
            }
            current = current.getSuperclass();
        }
        throw new NoSuchMethodException(
                clazz.getName() + "#" + name + "(" + String.join(",", typeNames(types)) + ")");
    }

    private static String[] typeNames(Class<?>[] types) {
        String[] names = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            names[i] = types[i].getName();
        }
        return names;
    }

    private Method findDispatchMethod(Class<?> dispatchType, String name, Class<?>[] types)
            throws NoSuchMethodException {
        try {
            Method method = dispatchType.getMethod(name, types);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            Method walked = walkDeclaredMethods(dispatchType, name, types, new HashSet<>());
            if (walked != null) {
                return walked;
            }
            throw e;
        }
    }

    private Method walkDeclaredMethods(Class<?> type, String name, Class<?>[] types, Set<Class<?>> seen) {
        if (type == null || !seen.add(type)) {
            return null;
        }
        for (Method candidate : type.getDeclaredMethods()) {
            if (candidate.getName().equals(name) && sameTypes(candidate.getParameterTypes(), types)) {
                candidate.setAccessible(true);
                return candidate;
            }
        }
        Method fromSuper = walkDeclaredMethods(type.getSuperclass(), name, types, seen);
        if (fromSuper != null) {
            return fromSuper;
        }
        for (Class<?> iface : type.getInterfaces()) {
            Method fromIface = walkDeclaredMethods(iface, name, types, seen);
            if (fromIface != null) {
                return fromIface;
            }
        }
        return null;
    }

    private boolean sameTypes(Class<?>[] actual, Class<?>[] expected) {
        if (actual.length != expected.length) {
            return false;
        }
        for (int i = 0; i < actual.length; i++) {
            if (!actual[i].equals(expected[i]) && !wrap(actual[i]).equals(wrap(expected[i]))) {
                return false;
            }
        }
        return true;
    }

    private Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == double.class) return Double.class;
        if (type == float.class) return Float.class;
        if (type == boolean.class) return Boolean.class;
        if (type == byte.class) return Byte.class;
        if (type == short.class) return Short.class;
        if (type == char.class) return Character.class;
        return type;
    }

    private static WorkerIpc.ScenarioStepSpec toStep(WorkerIpc.InvokeSpec spec) {
        return new WorkerIpc.ScenarioStepSpec(
                spec.kind(),
                spec.className(),
                spec.methodName(),
                spec.parameterTypes(),
                spec.paramsJson(),
                spec.receiverClassName(),
                spec.receiverParameterTypes(),
                spec.receiverParamsJson(),
                null,
                null,
                1,
                null,
                spec.methodNameAliases(),
                spec.parameterTypeAliases(),
                spec.receiverParameterTypeAliases(),
                null,
                null);
    }

    private ObjectFacts describeObject(URLClassLoader loader, Object object, Map<String, Object> registry) {
        if (!isStudentObject(loader, object)) {
            return ObjectFacts.none();
        }
        Map<String, Boolean> equalsNamed = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : registry.entrySet()) {
            try {
                equalsNamed.put(entry.getKey(), Boolean.valueOf(object.equals(entry.getValue())));
            } catch (Exception ignored) {
                equalsNamed.put(entry.getKey(), Boolean.FALSE);
            }
        }
        return new ObjectFacts(
                object.getClass().getSimpleName(),
                snapshotAllLiteralFields(object),
                equalsNamed);
    }

    private void maybeRegisterMethodProduct(URLClassLoader loader,
                                            Method method,
                                            WorkerIpc.ScenarioStepSpec spec,
                                            Object receiver,
                                            Object returnValue,
                                            Map<String, Object> registry) {
        if (!isStudentObject(loader, returnValue) || !hasInstanceName(spec)) {
            return;
        }
        if (Modifier.isStatic(method.getModifiers())) {
            register(registry, spec.instanceName(), returnValue);
            return;
        }
        if (receiver != null && registry.get(spec.instanceName()) == receiver) {
            return;
        }
        register(registry, spec.instanceName(), returnValue);
    }

    private Map<String, String> snapshotAllLiteralFields(Object target) {
        Map<String, String> snapshots = new LinkedHashMap<>();
        if (target == null) {
            return snapshots;
        }
        Class<?> type = target.getClass();
        while (type != null && type.getClassLoader() != null) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || snapshots.containsKey(field.getName())) {
                    continue;
                }
                field.setAccessible(true);
                try {
                    Object value = field.get(target);
                    if (isLiteralValue(value)) {
                        snapshots.put(field.getName(), coercer.toJson(value));
                    }
                } catch (IllegalAccessException ignored) {
                    // skip unreadable fields
                }
            }
            type = type.getSuperclass();
        }
        return snapshots;
    }

    private static boolean isLiteralValue(Object value) {
        if (value == null) {
            return true;
        }
        Class<?> type = value.getClass();
        if (type.isArray()) {
            return isLiteralArray(value);
        }
        return type.isEnum()
                || type == java.time.LocalDate.class
                || type == String.class
                || type == Integer.class
                || type == Long.class
                || type == Double.class
                || type == Float.class
                || type == Boolean.class
                || type == Byte.class
                || type == Short.class
                || type == Character.class;
    }

    private static boolean isLiteralArray(Object value) {
        Class<?> component = value.getClass().getComponentType();
        if (component == null) {
            return false;
        }
        if (component.isPrimitive() || component == String.class
                || Number.class.isAssignableFrom(component)
                || component == Boolean.class
                || component == Character.class) {
            return true;
        }
        return false;
    }

    private static boolean isStudentObject(URLClassLoader loader, Object value) {
        return value != null && value.getClass().getClassLoader() == loader;
    }

    private static void register(Map<String, Object> registry, String instanceName, Object instance) {
        if (instanceName == null || instanceName.isBlank()) {
            return;
        }
        registry.put(instanceName, instance);
    }

    private static InvocationKind kind(WorkerIpc.ScenarioStepSpec spec) {
        try {
            return InvocationKind.valueOf(spec.kind());
        } catch (Exception e) {
            return InvocationKind.METHOD;
        }
    }

    private static boolean hasReceiver(WorkerIpc.ScenarioStepSpec spec) {
        return spec.receiverClassName() != null && !spec.receiverClassName().isBlank();
    }

    private static boolean hasInstanceName(WorkerIpc.ScenarioStepSpec spec) {
        return spec.instanceName() != null && !spec.instanceName().isBlank();
    }

    private static int effectiveRepeatCount(WorkerIpc.ScenarioStepSpec spec) {
        if (spec == null || spec.repeatCount() < 1) {
            return 1;
        }
        return spec.repeatCount();
    }

    private static String messageOrSimpleName(Throwable throwable) {
        String message = throwable.getMessage();
        return message != null ? message : throwable.getClass().getSimpleName();
    }

    private record ObjectFacts(
            String typeName,
            Map<String, String> fields,
            Map<String, Boolean> equalsNamed) {

        static ObjectFacts none() {
            return new ObjectFacts(null, Map.of(), Map.of());
        }
    }

    private static final class TypeCombinationCursor {
        private final List<List<String>> aliases;
        private final int[] indexes;
        private boolean hasNext;

        private TypeCombinationCursor(List<List<String>> aliases) {
            this.aliases = aliases;
            this.indexes = new int[aliases.size()];
            this.hasNext = aliases.stream().noneMatch(List::isEmpty);
        }

        private boolean hasNext() {
            return hasNext;
        }

        private List<String> next() {
            List<String> combination = new ArrayList<>(aliases.size());
            for (int i = 0; i < aliases.size(); i++) {
                combination.add(aliases.get(i).get(indexes[i]));
            }
            advance();
            return List.copyOf(combination);
        }

        private void advance() {
            if (indexes.length == 0) {
                hasNext = false;
                return;
            }
            for (int i = indexes.length - 1; i >= 0; i--) {
                indexes[i]++;
                if (indexes[i] < aliases.get(i).size()) {
                    return;
                }
                indexes[i] = 0;
            }
            hasNext = false;
        }
    }

    private record ResolvedConstructor(
            Constructor<?> constructor,
            List<String> parameterTypes) {}

    private record ResolvedMethod(Method method, List<String> parameterTypes) {}
}
