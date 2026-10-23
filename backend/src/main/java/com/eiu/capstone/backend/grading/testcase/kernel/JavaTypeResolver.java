package com.eiu.capstone.backend.grading.testcase.kernel;

import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class JavaTypeResolver {

    private JavaTypeResolver() {}

    public static Class<?> resolve(String typeName) {
        if (typeName.endsWith("[]")) {
            return resolveArrayClass(typeName.substring(0, typeName.length() - 2));
        }
        return switch (typeName) {
            case "int" -> int.class;
            case "long" -> long.class;
            case "double" -> double.class;
            case "float" -> float.class;
            case "boolean" -> boolean.class;
            case "byte" -> byte.class;
            case "short" -> short.class;
            case "char" -> char.class;
            case "Integer" -> Integer.class;
            case "Long" -> Long.class;
            case "Double" -> Double.class;
            case "Float" -> Float.class;
            case "Boolean" -> Boolean.class;
            case "Byte" -> Byte.class;
            case "Short" -> Short.class;
            case "Character" -> Character.class;
            case "String" -> String.class;
            default -> throw new IllegalArgumentException("Unsupported type in v1: " + typeName);
        };
    }

    public static Class<?>[] resolveAll(List<String> parameterTypes) {
        return resolveAll(parameterTypes, null);
    }

    public static Class<?> resolve(String typeName, ClassLoader loader) {
        try {
            return resolve(typeName);
        } catch (IllegalArgumentException unsupported) {
            if (loader == null || typeName == null || typeName.isBlank()) {
                throw unsupported;
            }
            boolean array = typeName.endsWith("[]");
            String element = array ? typeName.substring(0, typeName.length() - 2) : typeName;
            try {
                Class<?> clazz = loadStudentClass(element, loader);
                return array ? clazz.arrayType() : clazz;
            } catch (ClassNotFoundException e) {
                throw unsupported;
            }
        }
    }

    public static Class<?>[] resolveAll(List<String> parameterTypes, ClassLoader loader) {
        if (parameterTypes == null) {
            return new Class<?>[0];
        }
        Class<?>[] types = new Class<?>[parameterTypes.size()];
        for (int i = 0; i < parameterTypes.size(); i++) {
            types[i] = resolve(parameterTypes.get(i), loader);
        }
        return types;
    }

    /**
     * Load a student class by rubric name. Accepts binary names ({@code Outer$Inner}),
     * dotted nested names ({@code Outer.Inner}), and simple nested names ({@code Inner}
     * when exactly one {@code *$Inner.class} exists on a directory URLClassLoader).
     */
    public static Class<?> loadStudentClass(String className, ClassLoader loader)
            throws ClassNotFoundException {
        if (className == null || className.isBlank()) {
            throw new ClassNotFoundException(className);
        }
        if (loader == null) {
            return Class.forName(className);
        }
        ClassNotFoundException primary = null;
        for (String candidate : binaryNameCandidates(className)) {
            try {
                return Class.forName(candidate, true, loader);
            } catch (ClassNotFoundException e) {
                if (primary == null) {
                    primary = e;
                }
            }
        }
        Class<?> nested = findNestedBySimpleName(loader, className);
        if (nested != null) {
            return nested;
        }
        throw primary != null ? primary : new ClassNotFoundException(className);
    }

    private static List<String> binaryNameCandidates(String className) {
        List<String> candidates = new ArrayList<>(2);
        candidates.add(className);
        if (className.indexOf('.') >= 0 && className.indexOf('$') < 0) {
            candidates.add(className.replace('.', '$'));
        }
        return candidates;
    }

    private static Class<?> findNestedBySimpleName(ClassLoader loader, String simpleName) {
        if (simpleName.indexOf('.') >= 0 || simpleName.indexOf('$') >= 0) {
            return null;
        }
        if (!(loader instanceof URLClassLoader urlLoader)) {
            return null;
        }
        String suffix = "$" + simpleName + ".class";
        String match = null;
        for (URL url : urlLoader.getURLs()) {
            Path root = directoryRoot(url);
            if (root == null) {
                continue;
            }
            try (Stream<Path> walk = Files.walk(root, 2)) {
                List<String> found = walk
                        .filter(path -> path.getFileName().toString().endsWith(suffix))
                        .map(path -> binaryNameFromClassFile(root, path))
                        .toList();
                for (String binary : found) {
                    if (match != null && !match.equals(binary)) {
                        return null;
                    }
                    match = binary;
                }
            } catch (Exception ignored) {
                // try next URL
            }
        }
        if (match == null) {
            return null;
        }
        try {
            return Class.forName(match, true, loader);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private static String binaryNameFromClassFile(Path root, Path classFile) {
        String binary = root.relativize(classFile).toString()
                .replace('\\', '/')
                .replace('/', '.');
        if (binary.endsWith(".class")) {
            return binary.substring(0, binary.length() - ".class".length());
        }
        return binary;
    }

    private static Path directoryRoot(URL url) {
        try {
            Path path = Path.of(url.toURI());
            return Files.isDirectory(path) ? path : null;
        } catch (URISyntaxException | RuntimeException e) {
            return null;
        }
    }

    private static Class<?> resolveArrayClass(String elementType) {
        return switch (elementType) {
            case "int", "Integer" -> int[].class;
            case "long", "Long" -> long[].class;
            case "double", "Double" -> double[].class;
            case "float", "Float" -> float[].class;
            case "boolean", "Boolean" -> boolean[].class;
            case "byte", "Byte" -> byte[].class;
            case "short", "Short" -> short[].class;
            case "char", "Character" -> char[].class;
            case "String" -> String[].class;
            default -> throw new IllegalArgumentException("Unsupported array type: " + elementType + "[]");
        };
    }
}
