package com.eiu.capstone.backend.grading.testcase.worker;

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

/**
 * Spring-free class reflection for the worker JVM ({@code --reflect}). Shape matches API
 * {@code ParsedClass} / members so Jackson can deserialize into those types.
 */
public final class WorkerClassReflector {

    private WorkerClassReflector() {}

    public static List<ReflectClass> parseClasses(Path classesDir) throws Exception {
        List<Path> classFiles;
        try (var stream = Files.list(classesDir)) {
            classFiles = stream
                    .filter(p -> p.toString().endsWith(".class"))
                    .filter(WorkerClassReflector::isSupportedClassFile)
                    .collect(Collectors.toList());
        }
        if (classFiles.isEmpty()) {
            return List.of();
        }
        List<ReflectClass> result = new ArrayList<>();
        try (URLClassLoader loader = new URLClassLoader(
                new URL[] { classesDir.toUri().toURL() }, ClassLoader.getPlatformClassLoader())) {
            for (Path classFile : classFiles) {
                String binaryName = classFile.getFileName().toString().replace(".class", "");
                try {
                    Class<?> clazz = Class.forName(binaryName, false, loader);
                    result.add(parseClass(clazz));
                } catch (ClassNotFoundException | LinkageError | TypeNotPresentException ignored) {
                    // treat as missing
                }
            }
        }
        return result;
    }

    private static boolean isSupportedClassFile(Path classFile) {
        String baseName = classFile.getFileName().toString().replace(".class", "");
        int firstDollar = baseName.indexOf('$');
        if (firstDollar < 0) {
            return true;
        }
        return baseName.indexOf('$', firstDollar + 1) < 0;
    }

    private static ReflectClass parseClass(Class<?> clazz) {
        ReflectClass parsed = new ReflectClass();
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
            ReflectField pf = new ReflectField();
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
            ReflectMethod pm = new ReflectMethod();
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
            ReflectConstructor pc = new ReflectConstructor();
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

    private static String scopeOf(int modifiers) {
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

    private static String declaringTypeOf(Class<?> clazz) {
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

    private static String declaredSuperclassSimpleName(Class<?> clazz) {
        Class<?> superclass = clazz.getSuperclass();
        if (superclass == null || superclass == Object.class) {
            return null;
        }
        return superclass.getSimpleName();
    }

    private static List<String> declaredInterfaceSimpleNames(Class<?> clazz) {
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

    private static String genericTypeName(Field field) {
        try {
            return simpleGenericName(field.getGenericType());
        } catch (TypeNotPresentException e) {
            return erasureWithMissingTypeArgument(field.getType(), e);
        }
    }

    private static String genericReturnTypeName(Method method) {
        try {
            return simpleGenericName(method.getGenericReturnType());
        } catch (TypeNotPresentException e) {
            return erasureWithMissingTypeArgument(method.getReturnType(), e);
        }
    }

    private static List<String> genericParameterTypeNames(Method method) {
        try {
            return Arrays.stream(method.getGenericParameterTypes())
                    .map(WorkerClassReflector::simpleGenericName)
                    .collect(Collectors.toList());
        } catch (TypeNotPresentException e) {
            return Arrays.stream(method.getParameterTypes())
                    .map(Class::getSimpleName)
                    .collect(Collectors.toList());
        }
    }

    private static List<String> genericConstructorParameterTypeNames(java.lang.reflect.Constructor<?> constructor) {
        try {
            return Arrays.stream(constructor.getGenericParameterTypes())
                    .map(WorkerClassReflector::simpleGenericName)
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

    private static String simpleGenericName(java.lang.reflect.Type type) {
        try {
            if (type instanceof Class<?> clazz) {
                return clazz.getSimpleName();
            }
            if (type instanceof java.lang.reflect.ParameterizedType pt) {
                String raw = simpleGenericName(pt.getRawType());
                String args = Arrays.stream(pt.getActualTypeArguments())
                        .map(WorkerClassReflector::simpleGenericName)
                        .collect(Collectors.joining(", "));
                return raw + "<" + args + ">";
            }
            return type.getTypeName();
        } catch (TypeNotPresentException e) {
            return simpleNameFromBinaryName(e.typeName());
        }
    }

    public static final class ReflectClass {
        public String simpleName;
        public String outerSimpleName;
        public String scope;
        public String declaringType;
        public boolean isAbstract;
        public boolean isStatic;
        public String superclassSimpleName;
        public List<String> interfaceSimpleNames;
        public List<ReflectField> fields;
        public List<ReflectMethod> methods;
        public List<ReflectConstructor> constructors;
    }

    public static final class ReflectField {
        public String name;
        public boolean isEnumConstant;
        public String dataType;
        public String scope;
        public boolean isStatic;
        public boolean isFinal;
    }

    public static final class ReflectMethod {
        public String name;
        public String returnType;
        public String scope;
        public boolean isStatic;
        public boolean isAbstract;
        public boolean isFinal;
        public List<String> parameterTypes;
        public List<String> parameterNames;
    }

    public static final class ReflectConstructor {
        public String scope;
        public List<String> parameterTypes;
        public List<String> parameterNames;
    }
}
