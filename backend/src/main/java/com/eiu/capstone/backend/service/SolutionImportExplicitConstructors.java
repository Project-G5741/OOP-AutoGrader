package com.eiu.capstone.backend.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects constructor declarations written in solution-import Java source.
 * Reflection alone cannot distinguish compiler-generated members from authored ones;
 * this scanner drives omission of implicit constructors on import.
 */
public final class SolutionImportExplicitConstructors {

    private static final Pattern TYPE_DECLARATION = Pattern.compile(
            "(?:^|[\\s;{}])"
                    + "(?:public|protected|private|static|abstract|final|sealed|non-sealed|strictfp|\\s)*"
                    + "(class|enum|record)\\s+(\\w+)\\b");

    private SolutionImportExplicitConstructors() {
    }

    public static Set<String> classesWithExplicitConstructors(List<String> sources) {
        Set<String> explicit = new LinkedHashSet<>();
        if (sources == null) {
            return explicit;
        }
        for (String raw : sources) {
            String source = raw != null ? raw : "";
            Matcher matcher = TYPE_DECLARATION.matcher(source);
            while (matcher.find()) {
                String kind = matcher.group(1);
                String typeName = matcher.group(2);
                if (hasExplicitConstructor(source, typeName, kind)) {
                    explicit.add(typeName);
                }
            }
        }
        return explicit;
    }

    /**
     * Matches a constructor header in a type body: optional access modifiers, then {@code ClassName(}.
     * Methods with a return type before the name (e.g. {@code int Foo()}) do not match.
     */
    public static boolean hasExplicitConstructor(String source, String className) {
        return hasExplicitConstructor(source, className, "class");
    }

    public static boolean hasExplicitConstructor(String source, String className, String declaringKind) {
        if (className == null || className.isBlank()) {
            return false;
        }
        if (declaringKind != null && "record".equalsIgnoreCase(declaringKind.trim())
                && hasRecordHeaderConstructor(source, className)) {
            return true;
        }
        return hasBodyConstructorDeclaration(source, className);
    }

    private static boolean hasRecordHeaderConstructor(String source, String className) {
        String quoted = Pattern.quote(className);
        Pattern recordHeader = Pattern.compile(
                "(?m)(?:^|[\\s;{}])"
                        + "(?:public|protected|private|static|final|sealed|non-sealed|strictfp|\\s)*"
                        + "record\\s+" + quoted + "\\s*\\(");
        return recordHeader.matcher(source).find();
    }

    private static boolean hasBodyConstructorDeclaration(String source, String className) {
        String quoted = Pattern.quote(className);
        Pattern ctorHead = Pattern.compile(
                "(?m)(?:^|[;{}])\\s*(?:(?:public|protected|private)\\s+)*" + quoted + "\\s*\\(");
        return ctorHead.matcher(source).find();
    }
}
