package com.eiu.capstone.backend.grading.testcase;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.eiu.capstone.backend.grading.rubric.InstanceRubric;
import com.eiu.capstone.backend.grading.rubric.InvocationRubric;
import com.eiu.capstone.backend.model.InvocationKind;
import com.eiu.capstone.backend.model.TestcaseComparisonMethod;

@Component
public class InvocationRunner {

    private final JsonValueCoercer jsonValueCoercer;
    private final ExecutorService invokeExecutor;
    private final int timeoutSeconds;

    public InvocationRunner(JsonValueCoercer jsonValueCoercer,
                            @Qualifier("testcaseInvokeExecutor") ExecutorService invokeExecutor,
                            @Value("${app.grading.testcase-invoke-timeout-seconds:5}") int timeoutSeconds) {
        this.jsonValueCoercer = jsonValueCoercer;
        this.invokeExecutor = invokeExecutor;
        this.timeoutSeconds = timeoutSeconds;
    }

    public InvocationOutcome invokeSingle(Path classesDir, InvocationRubric invocation) {
        if (!isRunnableClassesDir(classesDir)) {
            return InvocationOutcome.error("Missing compiled classes directory");
        }
        if (invocation == null) {
            return InvocationOutcome.error("Missing invocation rubric");
        }
        try {
            return runWithTimeout(() -> {
                try (URLClassLoader loader = classLoader(classesDir)) {
                    return invokeSingleInternal(loader, invocation);
                }
            });
        } catch (TimeoutException e) {
            return InvocationOutcome.timedOut("");
        } catch (Exception e) {
            return InvocationOutcome.error(messageOrSimpleName(e));
        }
    }

    public ComparisonOutcome invokeComparison(Path classesDir,
                                              TestcaseComparisonMethod comparisonMethod,
                                              List<InstanceRubric> instances) {
        if (!isRunnableClassesDir(classesDir)) {
            return ComparisonOutcome.error("Missing compiled classes directory");
        }
        if (instances == null || instances.size() < 2) {
            return ComparisonOutcome.error("Comparison testcase requires two instances");
        }
        try {
            return runWithTimeout(() -> {
                try (URLClassLoader loader = classLoader(classesDir)) {
                    Object instanceA = instantiate(loader, instances.get(0));
                    Object instanceB = instantiate(loader, instances.get(1));
                    Object result = comparisonMethod == TestcaseComparisonMethod.EQUALS
                            ? Boolean.valueOf(instanceA.equals(instanceB))
                            : Integer.valueOf(((Comparable<Object>) instanceA).compareTo(instanceB));
                    return ComparisonOutcome.normal(result);
                } catch (ReflectiveOperationException e) {
                    return ComparisonOutcome.error(messageOrSimpleName(e));
                }
            });
        } catch (TimeoutException e) {
            return ComparisonOutcome.error("Comparison invocation timed out");
        } catch (Exception e) {
            return ComparisonOutcome.error(messageOrSimpleName(e));
        }
    }

    private InvocationOutcome invokeSingleInternal(URLClassLoader loader, InvocationRubric invocation)
            throws ReflectiveOperationException {
        Class<?> clazz = findClass(loader, invocation.className());
        Object[] args = jsonValueCoercer.coerceParams(invocation.paramsJson(), invocation.parameterTypes());
        PrintStream originalOut = System.out;
        ByteArrayOutputStream stdoutBuffer = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(stdoutBuffer, true));
            if (invocation.kind() == InvocationKind.CONSTRUCTOR) {
                Constructor<?> constructor = findConstructor(clazz, invocation.parameterTypes());
                Object instance = constructor.newInstance(args);
                return InvocationOutcome.normal(instance, instance, stdoutBuffer.toString());
            }
            Method method = findMethod(clazz, invocation.methodName(), invocation.parameterTypes());
            Object receiver = null;
            if (!Modifier.isStatic(method.getModifiers())) {
                receiver = instantiateDefault(clazz);
            }
            Object returnValue = method.invoke(receiver, args);
            return InvocationOutcome.normal(receiver, returnValue, stdoutBuffer.toString());
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            return InvocationOutcome.threw(null, cause, stdoutBuffer.toString());
        } finally {
            System.setOut(originalOut);
        }
    }

    private Object instantiate(URLClassLoader loader, InstanceRubric instanceRubric)
            throws ReflectiveOperationException {
        Class<?> clazz = findClass(loader, instanceRubric.className());
        Constructor<?> constructor = findConstructor(clazz, instanceRubric.parameterTypes());
        Object[] args = jsonValueCoercer.coerceParams(instanceRubric.paramsJson(), instanceRubric.parameterTypes());
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

    private <T> T runWithTimeout(Callable<T> task) throws Exception {
        Future<T> future = invokeExecutor.submit(task);
        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw e;
        }
    }

    private boolean isRunnableClassesDir(Path classesDir) {
        return classesDir != null && Files.isDirectory(classesDir);
    }

    private URLClassLoader classLoader(Path classesDir) throws Exception {
        URL url = classesDir.toUri().toURL();
        return new URLClassLoader(new URL[] {url}, InvocationRunner.class.getClassLoader());
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

    private static String messageOrSimpleName(Throwable throwable) {
        String message = throwable.getMessage();
        return message != null ? message : throwable.getClass().getSimpleName();
    }
}
