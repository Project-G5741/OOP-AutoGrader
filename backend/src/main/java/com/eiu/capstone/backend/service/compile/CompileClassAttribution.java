package com.eiu.capstone.backend.service.compile;

import java.net.URI;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
        Map<SourceEntry, String> messageByFile = new LinkedHashMap<>();
        for (Diagnostic<? extends JavaFileObject> diagnostic : outcome.diagnostics()) {
            if (diagnostic.getKind() != Diagnostic.Kind.ERROR) {
                continue;
            }
            SourceEntry file = resolveSource(diagnostic, sourcesByUri);
            if (file == null) {
                continue;
            }
            errorFiles.add(file);
            messageByFile.putIfAbsent(file, CompileErrorMessage.forDiagnostic(diagnostic));
        }

        Set<String> roots = new LinkedHashSet<>();
        for (SourceEntry errorFile : errorFiles) {
            Set<String> declared = declaredByFile.getOrDefault(errorFile, Set.of());
            roots.addAll(declared);
            String stem = sourceFileStem(errorFile.logicalPath());
            if (stem != null && !declared.contains(stem)) {
                roots.add(stem);
            }
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

        Map<String, Pattern> wordPatterns = new HashMap<>();
        Map<String, Set<String>> referencedTypes = new LinkedHashMap<>();
        Map<SourceEntry, Set<String>> referencedByFile = new LinkedHashMap<>();
        Map<String, SourceEntry> fileByType = new LinkedHashMap<>();
        for (SourceEntry entry : sourceList) {
            Set<String> declared = declaredByFile.getOrDefault(entry, Set.of());
            String stripped = stripCommentsAndStrings(entry.source());
            Set<String> referenced = referencedTypeNames(stripped, declared, declaredOrder, wordPatterns);
            referencedByFile.put(entry, referenced);
            for (String typeName : declared) {
                fileByType.put(typeName, entry);
                referencedTypes.put(typeName, referenced);
            }
            String stem = sourceFileStem(entry.logicalPath());
            if (stem != null && !declared.contains(stem)) {
                fileByType.putIfAbsent(stem, entry);
                referencedTypes.putIfAbsent(stem, referenced);
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
                if (referencesFailed(referencedByFile.getOrDefault(entry, Set.of()), failed)) {
                    if (failed.addAll(declared)) {
                        changed = true;
                    }
                }
            }
        }

        Map<String, String> errorsByClass = new LinkedHashMap<>();
        for (String failedName : failed) {
            if (roots.contains(failedName)) {
                errorsByClass.put(failedName, rootMessage(
                        failedName, errorFiles, declaredByFile, fileByType, messageByFile, outcome));
            } else {
                String root = firstReachableRoot(failedName, roots, referencedTypes, order);
                errorsByClass.put(failedName, CompileErrorMessage.see(root));
            }
        }

        return new Result(failed, errorsByClass);
    }

    private static String rootMessage(String failedName,
                                      Set<SourceEntry> errorFiles,
                                      Map<SourceEntry, Set<String>> declaredByFile,
                                      Map<String, SourceEntry> fileByType,
                                      Map<SourceEntry, String> messageByFile,
                                      CompileOutcome outcome) {
        SourceEntry file = errorFileForRoot(failedName, errorFiles, declaredByFile, fileByType);
        String fileDiagnostic = file == null
                ? firstDiagnosticLine(outcome)
                : messageByFile.getOrDefault(file, firstDiagnosticLine(outcome));
        Set<String> declared = file == null ? Set.of() : declaredByFile.getOrDefault(file, Set.of());
        String stem = file == null ? null : sourceFileStem(file.logicalPath());
        return CompileErrorMessage.forFileRoot(failedName, stem, declared, fileDiagnostic);
    }

    private static SourceEntry errorFileForRoot(String failedName,
                                                Set<SourceEntry> errorFiles,
                                                Map<SourceEntry, Set<String>> declaredByFile,
                                                Map<String, SourceEntry> fileByType) {
        SourceEntry stemMismatch = null;
        SourceEntry declaredElsewhere = null;
        for (SourceEntry errorFile : errorFiles) {
            Set<String> declared = declaredByFile.getOrDefault(errorFile, Set.of());
            String stem = sourceFileStem(errorFile.logicalPath());
            if (failedName.equals(stem) && declared.contains(failedName)) {
                return errorFile;
            }
            if (failedName.equals(stem)) {
                stemMismatch = errorFile;
            } else if (declared.contains(failedName)) {
                declaredElsewhere = errorFile;
            }
        }
        if (stemMismatch != null) {
            return stemMismatch;
        }
        return declaredElsewhere != null ? declaredElsewhere : fileByType.get(failedName);
    }

    private static String firstDiagnosticLine(CompileOutcome outcome) {
        for (Diagnostic<? extends JavaFileObject> diagnostic : outcome.diagnostics()) {
            if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                return CompileErrorMessage.forDiagnostic(diagnostic);
            }
        }
        return "Compile error";
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
        try {
            return sourcesByUri.get(MemorySourceJavaFileObject.toSourceUri(name));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static boolean referencesFailed(Set<String> referenced, Set<String> failedNames) {
        for (String ref : referenced) {
            if (failedNames.contains(ref) || failedNames.contains(simpleName(ref))) {
                return true;
            }
        }
        for (String failed : failedNames) {
            String simple = simpleName(failed);
            if (!simple.equals(failed) && referenced.contains(simple)) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> referencedTypeNames(String stripped,
                                                   Set<String> declaredInFile,
                                                   List<String> declaredOrder,
                                                   Map<String, Pattern> wordPatterns) {
        Set<String> referenced = new LinkedHashSet<>();
        for (String name : declaredOrder) {
            if (declaredInFile.contains(name) || declaredInFile.contains(simpleName(name))) {
                continue;
            }
            if (containsTypeUse(stripped, name, wordPatterns)) {
                referenced.add(name);
            }
        }
        return referenced;
    }

    private static boolean containsTypeUse(String strippedSource,
                                           String typeName,
                                           Map<String, Pattern> wordPatterns) {
        if (typeName == null || typeName.isBlank()) {
            return false;
        }
        if (matchesWord(strippedSource, typeName, wordPatterns)) {
            return true;
        }
        String simple = simpleName(typeName);
        return !simple.equals(typeName) && matchesWord(strippedSource, simple, wordPatterns);
    }

    private static boolean matchesWord(String source, String name, Map<String, Pattern> wordPatterns) {
        Pattern pattern = wordPatterns.computeIfAbsent(
                name, key -> Pattern.compile("\\b" + Pattern.quote(key) + "\\b"));
        return pattern.matcher(source).find();
    }

    private static String simpleName(String typeName) {
        int dot = typeName.lastIndexOf('.');
        return dot >= 0 ? typeName.substring(dot + 1) : typeName;
    }

    private static String sourceFileStem(String logicalPath) {
        if (logicalPath == null || logicalPath.isBlank()) {
            return null;
        }
        String name = logicalPath.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        if (name.toLowerCase(Locale.ENGLISH).endsWith(".java")) {
            name = name.substring(0, name.length() - 5);
        }
        return name.isBlank() ? null : name;
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

    private static String stripCommentsAndStrings(String source) {
        String withoutBlocks = BLOCK_COMMENT.matcher(source == null ? "" : source).replaceAll(" ");
        String withoutLines = LINE_COMMENT.matcher(withoutBlocks).replaceAll(" ");
        String withoutStrings = STRING_LITERAL.matcher(withoutLines).replaceAll("\"\"");
        return CHAR_LITERAL.matcher(withoutStrings).replaceAll("''");
    }
}
