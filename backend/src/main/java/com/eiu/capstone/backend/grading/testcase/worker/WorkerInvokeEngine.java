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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.eiu.capstone.backend.grading.rubric.InstanceRubric;
import com.eiu.capstone.backend.grading.rubric.InvocationRubric;
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
        InvocationRubric invocation = toInvocation(spec);
        try (URLClassLoader loader = studentLoader(classesDir)) {
            return invokeSingleInternal(loader, invocation, snapshotFieldNames, stdoutCap);
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
            return new SerializedInvocationOutcome(
                    "NORMAL",
                    coercer.toJson(result),
                    stdout.text(),
                    stdout.truncated(),
                    Map.of(),
                    null,
                    List.of(),
                    coercer.toJson(result),
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

    private SerializedInvocationOutcome invokeSingleInternal(URLClassLoader loader,
                                                            InvocationRubric invocation,
                                                            List<String> snapshotFieldNames,
                                                            int stdoutCap)
            throws Exception {
        Class<?> clazz = findClass(loader, invocation.className());
        Object[] args = coercer.coerceParams(invocation.paramsJson(), invocation.parameterTypes());
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(loader);
        BoundedStdout stdout = new BoundedStdout(stdoutCap);
        java.io.PrintStream originalOut = System.out;
        try (java.io.PrintStream captured = new java.io.PrintStream(stdout, true)) {
            System.setOut(captured);
            if (invocation.kind() == InvocationKind.CONSTRUCTOR) {
                Constructor<?> constructor = findConstructor(clazz, invocation.parameterTypes());
                Object instance = constructor.newInstance(args);
                return normal(instance, instance, stdout, snapshotFieldNames);
            }
            Method method = findMethod(clazz, invocation.methodName(), invocation.parameterTypes());
            Object receiver = null;
            if (!Modifier.isStatic(method.getModifiers())) {
                receiver = invocation.hasReceiver()
                        ? instantiateReceiver(loader, invocation)
                        : instantiateDefault(clazz);
            }
            Object returnValue = method.invoke(receiver, args);
            return normal(receiver, returnValue, stdout, snapshotFieldNames);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            return threw(null, cause, stdout, snapshotFieldNames);
        } finally {
            System.setOut(originalOut);
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    private SerializedInvocationOutcome normal(Object instance,
                                               Object returnValue,
                                               BoundedStdout stdout,
                                               List<String> snapshotFieldNames) {
        Object snapshotTarget = instance != null ? instance : returnValue;
        return new SerializedInvocationOutcome(
                "NORMAL",
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
                "THREW",
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
        return instantiateWithConstructor(loader, spec.className(), spec.parameterTypes(), spec.paramsJson());
    }

    private Object instantiateReceiver(URLClassLoader loader, InvocationRubric invocation) throws Exception {
        return instantiateWithConstructor(
                loader,
                invocation.receiverClassName(),
                invocation.receiverParameterTypes(),
                invocation.receiverParamsJson());
    }

    private Object instantiateWithConstructor(URLClassLoader loader,
                                            String className,
                                            List<String> parameterTypes,
                                            String paramsJson) throws Exception {
        Class<?> clazz = findClass(loader, className);
        Constructor<?> constructor = findConstructor(clazz, parameterTypes);
        Object[] args = coercer.coerceParams(paramsJson, parameterTypes);
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

    private Constructor<?> findConstructor(Class<?> clazz, List<String> parameterTypes)
            throws NoSuchMethodException {
        Class<?>[] types = JavaTypeResolver.resolveAll(parameterTypes);
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

    private Method findMethod(Class<?> clazz, String name, List<String> parameterTypes)
            throws NoSuchMethodException {
        Class<?>[] types = JavaTypeResolver.resolveAll(parameterTypes);
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

    private static InvocationRubric toInvocation(WorkerIpc.InvokeSpec spec) {
        InvocationKind kind;
        try {
            kind = InvocationKind.valueOf(spec.kind());
        } catch (Exception e) {
            kind = InvocationKind.METHOD;
        }
        List<String> params = spec.parameterTypes() != null ? spec.parameterTypes() : List.of();
        List<String> receiverParams = spec.receiverParameterTypes() != null
                ? spec.receiverParameterTypes() : List.of();
        UUID receiverId = spec.receiverClassName() != null && !spec.receiverClassName().isBlank()
                ? UUID.randomUUID() : null;
        return new InvocationRubric(
                UUID.randomUUID(),
                kind,
                null,
                UUID.randomUUID(),
                spec.className(),
                spec.methodName(),
                params,
                spec.paramsJson(),
                receiverId,
                spec.receiverClassName(),
                receiverParams,
                spec.receiverParamsJson());
    }

    private static String messageOrSimpleName(Throwable throwable) {
        String message = throwable.getMessage();
        return message != null ? message : throwable.getClass().getSimpleName();
    }
}
