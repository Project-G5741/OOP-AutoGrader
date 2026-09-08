package com.eiu.capstone.backend.service.compile;

import java.net.URI;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import com.eiu.capstone.backend.service.compile.StudentSourceNormalizer.SourceEntry;

/**
 * Maps first-pass javac ERROR diagnostics onto declared types, then closes
 * dependents from source type positions. Remainder-compile diagnostics are not
 * used: those mark missing symbols, not root syntax errors.
 */
public final class CompileClassAttribution {

    private static final Pattern LINE_COMMENT = Pattern.compile("//.*");
    private static final Pattern BLOCK_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    private static final Pattern STRING_LITERAL = Pattern.compile("\"(?:\\\\.|[^\"])*\"");
    private static final Pattern CHAR_LITERAL = Pattern.compile("'(?:\\\\.|[^'])*'");

    private CompileClassAttribution() {
    }

    public record Result(Set<String> failedClassNames, Map<String, String> compileErrorsByClassName) {
        public Result {
            failedClassNames = failedClassNames == null
                    ? Set.of()
                    : Set.copyOf(failedClassNames);
            compileErrorsByClassName = compileErrorsByClassName == null
                    ? Map.of()
                    : Map.copyOf(compileErrorsByClassName);
        }
    }

    public static Result attribute(CompileOutcome outcome, List<SourceEntry> sources) {
        return attribute(outcome, sources, List.of());
    }

    public static Result attribute(CompileOutcome outcome,
                                   List<SourceEntry> sources,
                                   List<String> preferredRootOrder) {
        if (outcome == null || outcome.succeeded() || sources == null || sources.isEmpty()) {
            return new Result(Set.of(), Map.of());
        }

        List<SourceEntry> sourceList = List.copyOf(sources);
        Map<URI, SourceEntry> sourcesByUri = new LinkedHashMap<>();
        Map<SourceEntry, Set<String>> declaredByFile = new LinkedHashMap<>();
        List<String> declaredOrder = new ArrayList<>();
        for (SourceEntry entry : sourceList) {
            URI uri = MemorySourceJavaFileObject.toSourceUri(entry.logicalPath());
            sourcesByUri.put(uri, entry);
            Set<String> declared = StudentSourceNormalizer.extractDeclaredSimpleNames(entry.source());
            declaredByFile.put(entry, declared);
            declaredOrder.addAll(declared);
        }

        Set<SourceEntry> errorFiles = new LinkedHashSet<>();
        Map<SourceEntry, List<String>> messagesByFile = new LinkedHashMap<>();
        for (Diagnostic<? extends JavaFileObject> diagnostic : outcome.diagnostics()) {
            if (diagnostic.getKind() != Diagnostic.Kind.ERROR) {
                continue;
            }
            SourceEntry file = resolveSource(diagnostic, sourcesByUri);
            if (file == null) {
                continue;
            }
            errorFiles.add(file);
            messagesByFile.computeIfAbsent(file, key -> new ArrayList<>()).add(formatDiagnostic(diagnostic));
        }

        Set<String> roots = new LinkedHashSet<>();
        for (SourceEntry errorFile : errorFiles) {
            roots.addAll(declaredByFile.getOrDefault(errorFile, Set.of()));
        }
        List<String> order = (preferredRootOrder == null || preferredRootOrder.isEmpty())
                ? declaredOrder
                : preferredRootOrder;
        for (String name : order) {
            int dot = name.lastIndexOf('.');
            if (dot > 0 && roots.contains(name.substring(0, dot))) {
                roots.add(name);
            }
        }

        Map<String, Set<String>> referencedTypes = new LinkedHashMap<>();
        Map<String, SourceEntry> fileByType = new LinkedHashMap<>();
        for (SourceEntry entry : sourceList) {
            Set<String> declared = declaredByFile.getOrDefault(entry, Set.of());
            Set<String> referenced = referencedTypeNames(entry.source(), declaredOrder);
            for (String typeName : declared) {
                fileByType.put(typeName, entry);
                referencedTypes.put(typeName, referenced);
            }
        }

        Set<String> failed = new LinkedHashSet<>(roots);
        boolean changed = true;
        while (changed) {
            changed = false;
            for (SourceEntry entry : sourceList) {
                if (errorFiles.contains(entry)) {
                    continue;
                }
                Set<String> declared = declaredByFile.getOrDefault(entry, Set.of());
                if (declared.isEmpty() || failed.containsAll(declared)) {
                    continue;
                }
                if (referencesAny(entry.source(), failed, declared)) {
                    if (failed.addAll(declared)) {
                        changed = true;
                    }
                }
            }
        }

        Map<String, String> errorsByClass = new LinkedHashMap<>();
        for (String failedName : failed) {
            if (roots.contains(failedName)) {
                SourceEntry file = fileByType.get(failedName);
                List<String> messages = file == null ? List.of() : messagesByFile.getOrDefault(file, List.of());
                errorsByClass.put(failedName, messages.isEmpty()
                        ? String.join("\n", outcome.messages())
                        : String.join("\n", messages));
            } else {
                String root = firstReachableRoot(failedName, roots, referencedTypes, order);
                errorsByClass.put(failedName, "Compilation Error on " + root);
            }
        }

        return new Result(failed, errorsByClass);
    }

    private static SourceEntry resolveSource(Diagnostic<? extends JavaFileObject> diagnostic,
                                             Map<URI, SourceEntry> sourcesByUri) {
        JavaFileObject reported = diagnostic.getSource();
        if (reported == null) {
            return null;
        }
        SourceEntry byUri = sourcesByUri.get(reported.toUri());
        if (byUri != null) {
            return byUri;
        }
        String name = reported.getName();
        if (name == null || name.isBlank()) {
            return null;
        }
        String normalized = name.replace('\\', '/');
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        try {
            return sourcesByUri.get(MemorySourceJavaFileObject.toSourceUri(normalized));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static String formatDiagnostic(Diagnostic<? extends JavaFileObject> diagnostic) {
        return String.format("%s: line %d: %s",
                diagnostic.getKind(),
                diagnostic.getLineNumber(),
                diagnostic.getMessage(Locale.getDefault()));
    }

    private static boolean referencesAny(String source, Set<String> failedNames, Set<String> declaredInFile) {
        String stripped = stripCommentsAndStrings(source);
        for (String failed : failedNames) {
            String simple = simpleName(failed);
            if (declaredInFile.contains(failed) || declaredInFile.contains(simple)) {
                continue;
            }
            if (containsTypeUse(stripped, failed)) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> referencedTypeNames(String source, List<String> declaredOrder) {
        String stripped = stripCommentsAndStrings(source);
        Set<String> referenced = new LinkedHashSet<>();
        Set<String> self = StudentSourceNormalizer.extractDeclaredSimpleNames(source);
        for (String name : declaredOrder) {
            if (self.contains(name) || self.contains(simpleName(name))) {
                continue;
            }
            if (containsTypeUse(stripped, name)) {
                referenced.add(name);
            }
        }
        return referenced;
    }

    private static boolean containsTypeUse(String strippedSource, String typeName) {
        if (typeName == null || typeName.isBlank()) {
            return false;
        }
        if (matchesWord(strippedSource, typeName)) {
            return true;
        }
        String simple = simpleName(typeName);
        return !simple.equals(typeName) && matchesWord(strippedSource, simple);
    }

    private static boolean matchesWord(String source, String name) {
        Matcher matcher = Pattern.compile("\\b" + Pattern.quote(name) + "\\b").matcher(source);
        return matcher.find();
    }

    private static String simpleName(String typeName) {
        int dot = typeName.lastIndexOf('.');
        return dot >= 0 ? typeName.substring(dot + 1) : typeName;
    }

    private static String firstReachableRoot(String dependent,
                                             Set<String> roots,
                                             Map<String, Set<String>> referencedTypes,
                                             List<String> preferredRootOrder) {
        Set<String> reachableRoots = new LinkedHashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        queue.add(dependent);
        Set<String> seen = new LinkedHashSet<>();
        while (!queue.isEmpty()) {
            String current = queue.removeFirst();
            if (!seen.add(current)) {
                continue;
            }
            if (roots.contains(current) && !current.equals(dependent)) {
                reachableRoots.add(current);
                continue;
            }
            for (String next : referencedTypes.getOrDefault(current, Set.of())) {
                if (!seen.contains(next)) {
                    queue.add(next);
                }
            }
        }
        if (reachableRoots.isEmpty()) {
            reachableRoots.addAll(roots);
        }
        for (String candidate : preferredRootOrder) {
            if (reachableRoots.contains(candidate)) {
                return candidate;
            }
        }
        return reachableRoots.iterator().hasNext() ? reachableRoots.iterator().next() : dependent;
    }

    static String stripCommentsAndStrings(String source) {
        String withoutBlocks = BLOCK_COMMENT.matcher(source == null ? "" : source).replaceAll(" ");
        String withoutLines = LINE_COMMENT.matcher(withoutBlocks).replaceAll(" ");
        String withoutStrings = STRING_LITERAL.matcher(withoutLines).replaceAll("\"\"");
        return CHAR_LITERAL.matcher(withoutStrings).replaceAll("''");
    }
}
