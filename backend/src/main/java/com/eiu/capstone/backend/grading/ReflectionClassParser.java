package com.eiu.capstone.backend.grading;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.eiu.capstone.backend.exception.SubmissionProcessingException;
import com.eiu.capstone.backend.grading.testcase.ProcessTreeKiller;
import com.eiu.capstone.backend.grading.testcase.WorkerEnvironment;
import com.eiu.capstone.backend.grading.testcase.WorkerProcessClient;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Parses compiled student classes into {@link ParsedClass} DTOs. Production prefers a one-shot
 * worker JVM ({@code --reflect}) so student types never enter the API Metaspace. Falls back to
 * in-process reflection when the worker jar is unavailable or the reflect child fails
 * (unit tests / misconfigured local / transient worker errors).
 */
@Component
public class ReflectionClassParser {

    private static final Logger log = LoggerFactory.getLogger(ReflectionClassParser.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final long REFLECT_TIMEOUT_SECONDS = 30L;
    private static final int STDERR_CAP_BYTES = 4096;

    private final WorkerProcessClient workerProcessClient;
    private final Semaphore workerJvmSlot;

    /** Test helper when no worker client / slot is wired. */
    public ReflectionClassParser(WorkerProcessClient workerProcessClient) {
        this(workerProcessClient, null);
    }

    @Autowired
    public ReflectionClassParser(
            WorkerProcessClient workerProcessClient,
            @Qualifier("workerJvmSlot") Semaphore workerJvmSlot) {
        this.workerProcessClient = workerProcessClient;
        this.workerJvmSlot = workerJvmSlot;
    }

    public List<ParsedClass> parseClasses(Path classesDir) {
        if (classesDir == null || !Files.isDirectory(classesDir)) {
            return List.of();
        }
        try {
            if (workerProcessClient != null && workerJarAvailable()) {
                return parseViaWorker(classesDir);
            }
            return parseInProcess(classesDir);
        } catch (SubmissionProcessingException e) {
            throw e;
        } catch (Exception e) {
            throw new SubmissionProcessingException("Could not parse compiled classes in " + classesDir, e);
        }
    }

    private boolean workerJarAvailable() {
        try {
            workerProcessClient.productionCommand("--self-check");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private List<ParsedClass> parseViaWorker(Path classesDir) throws Exception {
        boolean acquired = false;
        if (workerJvmSlot != null) {
            workerJvmSlot.acquire();
            acquired = true;
        }
        try {
            return runReflectProcess(classesDir);
        } catch (Exception e) {
            log.warn("Class reflect worker failed for {}; falling back to in-process parse: {}",
                    classesDir, e.toString());
            return parseInProcess(classesDir);
        } finally {
            if (acquired) {
                workerJvmSlot.release();
            }
        }
    }

    private List<ParsedClass> runReflectProcess(Path classesDir) throws Exception {
        List<String> command = workerProcessClient.productionCommand(
                "--reflect", classesDir.toAbsolutePath().normalize().toString());
        ProcessBuilder builder = new ProcessBuilder(command);
        WorkerEnvironment.apply(builder);
        builder.redirectErrorStream(false);
        Process process = builder.start();
        AtomicInteger stderrKept = new AtomicInteger();
        Thread drain = new Thread(() -> drainStderr(process, stderrKept), "class-reflect-stderr");
        drain.setDaemon(true);
        drain.start();

        StringBuilder stdout = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (stdout.length() > 0) {
                    stdout.append('\n');
                }
                stdout.append(line);
            }
        } catch (IOException e) {
            ProcessTreeKiller.kill(process);
            throw new SubmissionProcessingException(
                    "Class reflect worker stdout read failed for " + classesDir, e);
        }

        boolean finished = process.waitFor(REFLECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!finished) {
            ProcessTreeKiller.kill(process);
            throw new SubmissionProcessingException("Class reflect worker timed out for " + classesDir);
        }
        if (process.exitValue() != 0) {
            throw new SubmissionProcessingException(
                    "Class reflect worker failed (exit " + process.exitValue() + ") for " + classesDir);
        }
        String json = stdout.toString().trim();
        if (json.isEmpty()) {
            return List.of();
        }
        ParsedClass[] parsed = MAPPER.readValue(json, ParsedClass[].class);
        return parsed == null ? List.of() : Arrays.asList(parsed);
    }

    private static void drainStderr(Process process, AtomicInteger stderrKept) {
        byte[] buffer = new byte[4096];
        try (InputStream err = process.getErrorStream()) {
            int read;
            while ((read = err.read(buffer)) != -1) {
                int already = stderrKept.get();
                int room = Math.max(0, STDERR_CAP_BYTES - already);
                if (room > 0) {
                    stderrKept.addAndGet(Math.min(room, read));
                }
            }
        } catch (Exception ignored) {
            // process closed
        }
    }

    /** In-process path for tests when worker.jar is absent, and fallback after reflect failure. */
    public List<ParsedClass> parseInProcess(Path classesDir) {
        List<Path> classFiles;
        try (var stream = Files.list(classesDir)) {
            classFiles = stream
                    .filter(p -> p.toString().endsWith(".class"))
                    .filter(this::isSupportedClassFile)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new SubmissionProcessingException("Could not list compiled classes in " + classesDir, e);
        }

        if (classFiles.isEmpty()) {
            return List.of();
        }

        List<ParsedClass> result = new ArrayList<>();
        try (URLClassLoader loader = new URLClassLoader(
                new URL[] { classesDir.toUri().toURL() }, getClass().getClassLoader())) {

            for (Path classFile : classFiles) {
                String binaryName = classFile.getFileName().toString().replace(".class", "");
                try {
                    Class<?> clazz = Class.forName(binaryName, false, loader);
                    result.add(parseClass(clazz));
                } catch (ClassNotFoundException | LinkageError | TypeNotPresentException e) {
                    System.out.println("  [warn] Could not load compiled class '" + binaryName
                            + "' (" + e.getClass().getSimpleName() + ") - treating as missing.");
                }
            }
        } catch (IOException e) {
            throw new SubmissionProcessingException("Could not create class loader for " + classesDir, e);
        }
        return result;
    }

    private boolean isSupportedClassFile(Path classFile) {
        String baseName = classFile.getFileName().toString().replace(".class", "");
        int firstDollar = baseName.indexOf('$');
        if (firstDollar < 0) {
            return true;
        }
        return baseName.indexOf('$', firstDollar + 1) < 0;
    }

    private ParsedClass parseClass(Class<?> clazz) {
        ParsedClass parsed = new ParsedClass();
        parsed.simpleName = clazz.getSimpleName();
        Class<?> enclosing = clazz.getEnclosingClass();
        if (enclosing != null) {
            parsed.outerSimpleName = enclosing.getSimpleName();
        }

        int modifiers = clazz.getModifiers();
        parsed.scope = scopeOf(modifiers);
        parsed.declaringType = declaringTypeOf(clazz);
        parsed.isAbstract = Modifier.isAbstract(modifiers) && !clazz.isInterface();
        parsed.isStatic = Modifier.isStatic(modifiers);
        parsed.superclassSimpleName = declaredSuperclassSimpleName(clazz);
        parsed.interfaceSimpleNames = declaredInterfaceSimpleNames(clazz);

        parsed.fields = new ArrayList<>();
        for (Field f : clazz.getDeclaredFields()) {
            if (f.isSynthetic()) {
                continue;
            }
            ParsedField pf = new ParsedField();
            pf.name = f.getName();
            pf.isEnumConstant = f.isEnumConstant();
            pf.dataType = genericTypeName(f);
            pf.scope = scopeOf(f.getModifiers());
            pf.isStatic = Modifier.isStatic(f.getModifiers());
            pf.isFinal = Modifier.isFinal(f.getModifiers());
            parsed.fields.add(pf);
        }

        parsed.methods = new ArrayList<>();
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.isSynthetic() || m.isBridge()) {
                continue;
            }
            ParsedMethod pm = new ParsedMethod();
            pm.name = m.getName();
            pm.returnType = genericReturnTypeName(m);
            pm.scope = scopeOf(m.getModifiers());
            pm.isStatic = Modifier.isStatic(m.getModifiers());
            pm.isAbstract = Modifier.isAbstract(m.getModifiers());
            pm.isFinal = Modifier.isFinal(m.getModifiers());
            pm.parameterTypes = genericParameterTypeNames(m);
            pm.parameterNames = parameterNames(m.getParameters());
            parsed.methods.add(pm);
        }

        parsed.constructors = new ArrayList<>();
        for (java.lang.reflect.Constructor<?> c : clazz.getDeclaredConstructors()) {
            if (c.isSynthetic()) {
                continue;
            }
            ParsedConstructor pc = new ParsedConstructor();
            pc.scope = scopeOf(c.getModifiers());
            pc.parameterTypes = genericConstructorParameterTypeNames(c);
            pc.parameterNames = parameterNames(c.getParameters());
            parsed.constructors.add(pc);
        }

        return parsed;
    }

    private static List<String> parameterNames(java.lang.reflect.Parameter[] parameters) {
        if (parameters == null || parameters.length == 0) {
            return List.of();
        }
        List<String> names = new ArrayList<>(parameters.length);
        for (int i = 0; i < parameters.length; i++) {
            java.lang.reflect.Parameter parameter = parameters[i];
            if (parameter.isNamePresent() && parameter.getName() != null && !parameter.getName().isBlank()) {
                names.add(parameter.getName());
            } else {
                names.add("arg" + i);
            }
        }
        return names;
    }

    private String scopeOf(int modifiers) {
        if (Modifier.isPublic(modifiers)) {
            return "public";
        }
        if (Modifier.isPrivate(modifiers)) {
            return "private";
        }
        if (Modifier.isProtected(modifiers)) {
            return "protected";
        }
        return "default";
    }

    private String declaringTypeOf(Class<?> clazz) {
        if (clazz.isInterface()) {
            return "interface";
        }
        if (clazz.isEnum()) {
            return "enum";
        }
        if (clazz.isRecord()) {
            return "record";
        }
        return "class";
    }

    private String declaredSuperclassSimpleName(Class<?> clazz) {
        Class<?> superclass = clazz.getSuperclass();
        if (superclass == null || superclass == Object.class) {
            return null;
        }
        return superclass.getSimpleName();
    }

    private List<String> declaredInterfaceSimpleNames(Class<?> clazz) {
        Class<?>[] interfaces = clazz.getInterfaces();
        if (interfaces.length == 0) {
            return List.of();
        }
        List<String> names = new ArrayList<>(interfaces.length);
        for (Class<?> iface : interfaces) {
            names.add(iface.getSimpleName());
        }
        return names;
    }

    private String genericTypeName(Field field) {
        try {
            return simpleGenericName(field.getGenericType());
        } catch (TypeNotPresentException e) {
            return erasureWithMissingTypeArgument(field.getType(), e);
        }
    }

    private String genericReturnTypeName(Method method) {
        try {
            return simpleGenericName(method.getGenericReturnType());
        } catch (TypeNotPresentException e) {
            return erasureWithMissingTypeArgument(method.getReturnType(), e);
        }
    }

    private List<String> genericParameterTypeNames(Method method) {
        try {
            return Arrays.stream(method.getGenericParameterTypes())
                    .map(this::simpleGenericName)
                    .collect(Collectors.toList());
        } catch (TypeNotPresentException e) {
            return Arrays.stream(method.getParameterTypes())
                    .map(Class::getSimpleName)
                    .collect(Collectors.toList());
        }
    }

    private List<String> genericConstructorParameterTypeNames(java.lang.reflect.Constructor<?> constructor) {
        try {
            return Arrays.stream(constructor.getGenericParameterTypes())
                    .map(this::simpleGenericName)
                    .collect(Collectors.toList());
        } catch (TypeNotPresentException e) {
            return Arrays.stream(constructor.getParameterTypes())
                    .map(Class::getSimpleName)
                    .collect(Collectors.toList());
        }
    }

    private static String erasureWithMissingTypeArgument(Class<?> erasure, TypeNotPresentException missing) {
        String missingSimple = simpleNameFromBinaryName(missing.typeName());
        if (erasure == null) {
            return missingSimple;
        }
        if (List.class.equals(erasure)) {
            return "List<" + missingSimple + ">";
        }
        return erasure.getSimpleName();
    }

    private static String simpleNameFromBinaryName(String binaryName) {
        if (binaryName == null || binaryName.isBlank()) {
            return "?";
        }
        int dot = binaryName.lastIndexOf('.');
        return dot >= 0 ? binaryName.substring(dot + 1) : binaryName;
    }

    private String simpleGenericName(java.lang.reflect.Type type) {
        try {
            if (type instanceof Class<?> clazz) {
                return clazz.getSimpleName();
            }
            if (type instanceof java.lang.reflect.ParameterizedType pt) {
                String raw = simpleGenericName(pt.getRawType());
                String args = Arrays.stream(pt.getActualTypeArguments())
                        .map(this::simpleGenericName)
                        .collect(Collectors.joining(", "));
                return raw + "<" + args + ">";
            }
            return type.getTypeName();
        } catch (TypeNotPresentException e) {
            return simpleNameFromBinaryName(e.typeName());
        }
    }
}
