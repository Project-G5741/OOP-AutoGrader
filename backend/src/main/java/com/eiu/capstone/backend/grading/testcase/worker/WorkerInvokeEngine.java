package com.eiu.capstone.backend.grading.testcase.worker;

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
import java.util.function.Function;

import com.eiu.capstone.backend.grading.testcase.SerializedInvocationOutcome;
import com.eiu.capstone.backend.grading.testcase.kernel.JavaTypeResolver;
import com.eiu.capstone.backend.grading.testcase.kernel.JsonValueCoercer;
import com.eiu.capstone.backend.model.InvocationKind;
import com.eiu.capstone.backend.model.TestcaseComparisonMethod;

public final class WorkerInvokeEngine {

    private final JsonValueCoercer coercer = new JsonValueCoercer();

    public SerializedInvocationOutcome invoke(Path classesDir,
                                              WorkerIpc.InvokeSpec spec,
                                              List<String> snapshotFieldNames,
                                              int stdoutCap) {
        if (!Files.isDirectory(classesDir)) {
            return SerializedInvocationOutcome.error("Missing compiled classes directory");
        }
        if (spec == null || spec.className() == null || spec.className().isBlank()) {
            return SerializedInvocationOutcome.error("Missing invocation rubric");
        }
        try (URLClassLoader loader = studentLoader(classesDir)) {
            return executeStep(loader, toStep(spec), snapshotFieldNames, stdoutCap, new LinkedHashMap<>());
        } catch (Exception e) {
            return SerializedInvocationOutcome.error(messageOrSimpleName(e));
        }
    }

    public SerializedInvocationOutcome scenario(Path classesDir,
                                                List<WorkerIpc.ScenarioStepSpec> steps,
                                                List<String> snapshotFieldNames,
                                                int stdoutCap) {
        if (!Files.isDirectory(classesDir)) {
            return SerializedInvocationOutcome.error("Missing compiled classes directory");
        }
        if (steps == null || steps.isEmpty()) {
            return SerializedInvocationOutcome.error("Missing scenario steps");
        }
        try (URLClassLoader loader = studentLoader(classesDir)) {
            Map<String, Object> registry = new LinkedHashMap<>();
            List<SerializedInvocationOutcome> outcomes = new ArrayList<>();
            for (WorkerIpc.ScenarioStepSpec step : steps) {
                SerializedInvocationOutcome outcome = executeStep(
                        loader, step, snapshotFieldNames, stdoutCap, registry);
                outcomes.add(outcome);
                if (shouldStopScenario(step, outcome)) {
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
        } catch (Exception e) {
            return SerializedInvocationOutcome.error(messageOrSimpleName(e));
        }
    }

    public SerializedInvocationOutcome compare(Path classesDir,
                                               WorkerIpc.CompareSpec spec,
                                               int stdoutCap) {
        if (!Files.isDirectory(classesDir)) {
            return SerializedInvocationOutcome.error("Missing compiled classes directory");
        }
        if (spec == null || spec.a() == null || spec.b() == null) {
            return SerializedInvocationOutcome.error("Comparison testcase requires two instances");
        }
        TestcaseComparisonMethod method;
        try {
            method = TestcaseComparisonMethod.valueOf(spec.comparisonMethod());
        } catch (Exception e) {
            return SerializedInvocationOutcome.error("Unknown comparison method");
        }
        BoundedStdout stdout = new BoundedStdout(stdoutCap);
        java.io.PrintStream originalOut = System.out;
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        try (URLClassLoader loader = studentLoader(classesDir);
             java.io.PrintStream captured = new java.io.PrintStream(stdout, true)) {
            Thread.currentThread().setContextClassLoader(loader);
            System.setOut(captured);
            Object instanceA = instantiate(loader, spec.a());
            Object instanceB = instantiate(loader, spec.b());
            Object result = method == TestcaseComparisonMethod.EQUALS
                    ? Boolean.valueOf(instanceA.equals(instanceB))
                    : Integer.valueOf(((Comparable<Object>) instanceA).compareTo(instanceB));
            String resultJson = coercer.toJson(result);
            return new SerializedInvocationOutcome(
                    SerializedInvocationOutcome.KIND_NORMAL,
                    resultJson,
                    stdout.text(),
                    stdout.truncated(),
                    Map.of(),
                    null,
                    List.of(),
                    resultJson,
                    null);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            return threw(null, cause, stdout, List.of());
        } catch (Exception e) {
            return SerializedInvocationOutcome.error(messageOrSimpleName(e));
        } finally {
            System.setOut(originalOut);
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    private SerializedInvocationOutcome executeStep(URLClassLoader loader,
                                                    WorkerIpc.ScenarioStepSpec spec,
                                                    List<String> snapshotFieldNames,
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
        java.io.PrintStream originalOut = System.out;
        Object receiver = null;
        try {
            Class<?> clazz = findClass(loader, spec.className());
            List<String> parameterTypes = spec.parameterTypes() != null ? spec.parameterTypes() : List.of();
            Object[] args = coercer.coerceParams(spec.paramsJson(), parameterTypes, namedInstances);
            try (java.io.PrintStream captured = new java.io.PrintStream(stdout, true)) {
                System.setOut(captured);
                if (kind(spec) == InvocationKind.CONSTRUCTOR) {
                    Constructor<?> constructor = findConstructor(clazz, parameterTypes, loader);
                    Object instance = constructor.newInstance(args);
                    register(registry, spec.instanceName(), instance);
                    return normal(instance, instance, stdout, snapshotFieldNames);
                }
                Method method = resolveMethod(loader, spec, clazz, parameterTypes);
                if (!Modifier.isStatic(method.getModifiers())) {
                    receiver = resolveReceiver(loader, spec, clazz, registry, namedInstances);
                }
                Object returnValue = method.invoke(receiver, args);
                return normal(receiver, returnValue, stdout, snapshotFieldNames);
            }
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            return threw(receiver, cause, stdout, snapshotFieldNames);
        } catch (Exception e) {
            return SerializedInvocationOutcome.error(messageOrSimpleName(e));
        } finally {
            System.setOut(originalOut);
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    private static boolean shouldStopScenario(WorkerIpc.ScenarioStepSpec step, SerializedInvocationOutcome outcome) {
        if (outcome == null || outcome.kind() == null) {
            return true;
        }
        if (SerializedInvocationOutcome.KIND_ERROR.equals(outcome.kind())
                || SerializedInvocationOutcome.KIND_TIMED_OUT.equals(outcome.kind())) {
            return true;
        }
        return SerializedInvocationOutcome.KIND_THREW.equals(outcome.kind())
                && kind(step) == InvocationKind.CONSTRUCTOR;
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
        return instantiateDefault(clazz);
    }

    private Method resolveMethod(URLClassLoader loader,
                                 WorkerIpc.ScenarioStepSpec spec,
                                 Class<?> concrete,
                                 List<String> parameterTypes) throws NoSuchMethodException, ClassNotFoundException {
        Class<?>[] types = JavaTypeResolver.resolveAll(parameterTypes, loader);
        if (spec.dispatchClassName() != null && !spec.dispatchClassName().isBlank()) {
            Class<?> dispatchType = findClass(loader, spec.dispatchClassName());
            return findDispatchMethod(dispatchType, spec.methodName(), types);
        }
        return findDeclaredMethod(concrete, spec.methodName(), types);
    }

    private SerializedInvocationOutcome normal(Object instance,
                                               Object returnValue,
                                               BoundedStdout stdout,
                                               List<String> snapshotFieldNames) {
        Object snapshotTarget = instance != null ? instance : returnValue;
        return new SerializedInvocationOutcome(
                SerializedInvocationOutcome.KIND_NORMAL,
                coercer.toJson(returnValue),
                stdout.text(),
                stdout.truncated(),
                snapshotFields(snapshotTarget, snapshotFieldNames),
                null,
                List.of(),
                null,
                null);
    }

    private SerializedInvocationOutcome threw(Object instance,
                                              Throwable cause,
                                              BoundedStdout stdout,
                                              List<String> snapshotFieldNames) {
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
                snapshotFields(snapshotTarget, snapshotFieldNames),
                cause.getClass().getSimpleName(),
                List.copyOf(supers),
                null,
                null);
    }

    private Map<String, String> snapshotFields(Object target, List<String> fieldNames) {
        Map<String, String> snapshots = new LinkedHashMap<>();
        if (target == null || fieldNames == null) {
            return snapshots;
        }
        for (String fieldName : fieldNames) {
            if (fieldName == null || fieldName.isBlank()) {
                continue;
            }
            try {
                snapshots.put(fieldName, coercer.toJson(readField(target, fieldName)));
            } catch (ReflectiveOperationException e) {
                snapshots.put(fieldName, "null");
            }
        }
        return snapshots;
    }

    private Object readField(Object target, String fieldName) throws ReflectiveOperationException {
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
        throw new NoSuchFieldException(fieldName);
    }

    private Object instantiate(URLClassLoader loader, WorkerIpc.InstanceSpec spec) throws Exception {
        return instantiateWithConstructor(
                loader, spec.className(), spec.parameterTypes(), spec.paramsJson(), null);
    }

    private Object instantiateReceiver(URLClassLoader loader,
                                       WorkerIpc.ScenarioStepSpec spec,
                                       Function<String, Object> namedInstances) throws Exception {
        return instantiateWithConstructor(
                loader,
                spec.receiverClassName(),
                spec.receiverParameterTypes() != null ? spec.receiverParameterTypes() : List.of(),
                spec.receiverParamsJson(),
                namedInstances);
    }

    private Object instantiateWithConstructor(URLClassLoader loader,
                                            String className,
                                            List<String> parameterTypes,
                                            String paramsJson,
                                            Function<String, Object> namedInstances) throws Exception {
        Class<?> clazz = findClass(loader, className);
        Constructor<?> constructor = findConstructor(clazz, parameterTypes, loader);
        Object[] args = coercer.coerceParams(paramsJson, parameterTypes, namedInstances);
        return constructor.newInstance(args);
    }

    private Object instantiateDefault(Class<?> clazz) throws ReflectiveOperationException {
        try {
            Constructor<?> noArg = clazz.getDeclaredConstructor();
            noArg.setAccessible(true);
            return noArg.newInstance();
        } catch (NoSuchMethodException e) {
            throw new NoSuchMethodException(
                    "Instance method requires a no-argument constructor on " + clazz.getSimpleName());
        }
    }

    static URLClassLoader studentLoader(Path classesDir) throws Exception {
        URL url = classesDir.toUri().toURL();
        return new URLClassLoader(new URL[] {url}, ClassLoader.getPlatformClassLoader());
    }

    private Class<?> findClass(URLClassLoader loader, String className) throws ClassNotFoundException {
        return Class.forName(className, true, loader);
    }

    private Constructor<?> findConstructor(Class<?> clazz, List<String> parameterTypes, URLClassLoader loader)
            throws NoSuchMethodException {
        Class<?>[] types = JavaTypeResolver.resolveAll(parameterTypes, loader);
        try {
            Constructor<?> constructor = clazz.getDeclaredConstructor(types);
            constructor.setAccessible(true);
            return constructor;
        } catch (NoSuchMethodException e) {
            for (Constructor<?> candidate : clazz.getDeclaredConstructors()) {
                if (sameTypes(candidate.getParameterTypes(), types)) {
                    candidate.setAccessible(true);
                    return candidate;
                }
            }
            throw e;
        }
    }

    private Method findDeclaredMethod(Class<?> clazz, String name, Class<?>[] types)
            throws NoSuchMethodException {
        try {
            Method method = clazz.getDeclaredMethod(name, types);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            for (Method candidate : clazz.getDeclaredMethods()) {
                if (candidate.getName().equals(name) && sameTypes(candidate.getParameterTypes(), types)) {
                    candidate.setAccessible(true);
                    return candidate;
                }
            }
            throw e;
        }
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
                null);
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

    private static String messageOrSimpleName(Throwable throwable) {
        String message = throwable.getMessage();
        return message != null ? message : throwable.getClass().getSimpleName();
    }
}
