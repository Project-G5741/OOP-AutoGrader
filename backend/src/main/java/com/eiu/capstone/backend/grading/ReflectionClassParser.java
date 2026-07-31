package com.eiu.capstone.backend.grading;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.eiu.capstone.backend.exception.SubmissionProcessingException;

@Component
public class ReflectionClassParser {

    public List<ParsedClass> parseClasses(Path classesDir) {
        List<Path> classFiles;
        try (var stream = Files.list(classesDir)) {
            classFiles = stream
                    .filter(p -> p.toString().endsWith(".class"))
                    .filter(p -> !p.getFileName().toString().contains("$"))
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
                String className = classFile.getFileName().toString().replace(".class", "");
                try {
                    Class<?> clazz = Class.forName(className, false, loader);
                    result.add(parseClass(clazz));
                } catch (ClassNotFoundException | LinkageError e) {
                    System.out.println("  [warn] Could not load compiled class '" + className
                            + "' (" + e.getClass().getSimpleName() + ") — treating as missing.");
                }
            }
        } catch (IOException e) {
            throw new SubmissionProcessingException("Could not create class loader for " + classesDir, e);
        }
        return result;
    }

    private ParsedClass parseClass(Class<?> clazz) {
        ParsedClass parsed = new ParsedClass();
        parsed.simpleName = clazz.getSimpleName();

        int modifiers = clazz.getModifiers();
        parsed.scope = scopeOf(modifiers);
        parsed.declaringType = declaringTypeOf(clazz);
        parsed.isAbstract = Modifier.isAbstract(modifiers) && !clazz.isInterface();

        parsed.fields = new ArrayList<>();
        for (Field f : clazz.getDeclaredFields()) {
            if (f.isSynthetic())
                continue;
            ParsedField pf = new ParsedField();
            pf.name = f.getName();
            pf.dataType = simpleGenericName(f.getGenericType());
            pf.scope = scopeOf(f.getModifiers());
            parsed.fields.add(pf);
        }

        parsed.methods = new ArrayList<>();
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.isSynthetic() || m.isBridge())
                continue;
            ParsedMethod pm = new ParsedMethod();
            pm.name = m.getName();
            pm.returnType = simpleGenericName(m.getGenericReturnType());
            pm.scope = scopeOf(m.getModifiers());
            pm.isStatic = Modifier.isStatic(m.getModifiers());
            pm.isAbstract = Modifier.isAbstract(m.getModifiers());
            pm.isFinal = Modifier.isFinal(m.getModifiers());
            pm.parameterTypes = Arrays.stream(m.getGenericParameterTypes())
                    .map(this::simpleGenericName)
                    .collect(Collectors.toList());
            parsed.methods.add(pm);
        }

        parsed.constructors = new ArrayList<>();
        for (java.lang.reflect.Constructor<?> c : clazz.getDeclaredConstructors()) {
            if (c.isSynthetic())
                continue;
            ParsedConstructor pc = new ParsedConstructor();
            pc.scope = scopeOf(c.getModifiers());
            pc.parameterTypes = Arrays.stream(c.getGenericParameterTypes())
        .map(this::simpleGenericName)
        .collect(Collectors.toList());
            parsed.constructors.add(pc);
        }

        return parsed;
    }

    private String scopeOf(int modifiers) {
        if (Modifier.isPublic(modifiers))
            return "public";
        if (Modifier.isPrivate(modifiers))
            return "private";
        if (Modifier.isProtected(modifiers))
            return "protected";
        // package-private — there's no corresponding master_data row today, so this
        // will simply never match an expected scope, which is the correct behavior.
        return "default";
    }

    private String declaringTypeOf(Class<?> clazz) {
        if (clazz.isInterface())
            return "interface";
        if (clazz.isEnum())
            return "enum";
        if (clazz.isRecord())
            return "record";
        return "class";
    }

    private String simpleGenericName(java.lang.reflect.Type type) {
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
        return type.getTypeName(); // fallback for arrays, wildcards, etc.
    }
}