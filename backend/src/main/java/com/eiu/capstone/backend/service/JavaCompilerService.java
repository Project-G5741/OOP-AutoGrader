package com.eiu.capstone.backend.service;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import jakarta.annotation.PostConstruct;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import org.springframework.stereotype.Service;

import com.eiu.capstone.backend.service.compile.CompileClassAttribution;
import com.eiu.capstone.backend.service.compile.CompileOutcome;
import com.eiu.capstone.backend.service.compile.MemorySourceJavaFileObject;
import com.eiu.capstone.backend.service.compile.StudentSourceNormalizer;
import com.eiu.capstone.backend.service.compile.StudentSourceNormalizer.SourceEntry;

@Service
public class JavaCompilerService {

    private JavaCompiler compiler;
    private final ThreadLocal<StandardJavaFileManager> fileManagerHolder = new ThreadLocal<>();

    @PostConstruct
    public void initCompiler() {
        compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException(
                    "No system Java compiler available — the backend must run on a JDK, not a JRE.");
        }
        warmCompiler();
    }

    private void warmCompiler() {
        try {
            Path dir = Files.createTempDirectory("javac-warmup");
            try {
                CompileOutcome warmed = compileSources(List.of(new MemorySourceJavaFileObject(
                        "Warmup.java", "public class Warmup {}".getBytes(StandardCharsets.UTF_8))), dir);
                if (!warmed.succeeded()) {
                    throw new IllegalStateException("Java compiler warmup failed: " + warmed.messages());
                }
            } finally {
                deleteRecursively(dir);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to warm Java compiler", e);
        }
    }

    public CompileOutcome compileSources(List<JavaFileObject> sources, Path outputDir) {
        if (sources.isEmpty()) {
            return CompileOutcome.skipped();
        }

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        boolean success;
        try {
            success = runTask(sources, outputDir, diagnostics);
        } catch (RuntimeException e) {
            resetFileManager();
            throw e;
        }

        List<Diagnostic<? extends JavaFileObject>> firstPass = List.copyOf(diagnostics.getDiagnostics());
        if (success) {
            return new CompileOutcome(true, firstPass, countClassFiles(outputDir));
        }

        resetFileManager();
        if (!hasAnyClassFile(outputDir)) {
            List<JavaFileObject> remainder = remainderSources(sources, firstPass);
            if (!remainder.isEmpty()) {
                DiagnosticCollector<JavaFileObject> remainderDiagnostics = new DiagnosticCollector<>();
                try {
                    boolean remainderOk = runTask(remainder, outputDir, remainderDiagnostics);
                    if (!remainderOk) {
                        resetFileManager();
                    }
                } catch (RuntimeException e) {
                    resetFileManager();
                    throw e;
                }
            }
        }

        return new CompileOutcome(false, firstPass, countClassFiles(outputDir));
    }

    private boolean runTask(List<JavaFileObject> sources,
                            Path outputDir,
                            DiagnosticCollector<JavaFileObject> diagnostics) {
        StandardJavaFileManager fileManager = fileManager();
        List<String> options = List.of("-d", outputDir.toString(), "-encoding", "UTF-8", "-proc:none");
        StringWriter errorOutput = new StringWriter();
        JavaCompiler.CompilationTask task = compiler.getTask(
                errorOutput, fileManager, diagnostics, options, null, sources);
        return Boolean.TRUE.equals(task.call());
    }

    private static List<JavaFileObject> remainderSources(
            List<JavaFileObject> sources,
            List<Diagnostic<? extends JavaFileObject>> firstPass) {
        List<JavaFileObject> withoutErrors = sourcesWithoutErrorDiagnostics(sources, firstPass);
        CompileClassAttribution.Result attributed = CompileClassAttribution.attribute(
                new CompileOutcome(false, firstPass, 0), toSourceEntries(sources));
        Set<String> failed = attributed.failedClassNames();
        if (failed.isEmpty()) {
            return withoutErrors;
        }
        List<JavaFileObject> remainder = new ArrayList<>();
        for (JavaFileObject source : withoutErrors) {
            if (!declaresFailedType(source, failed)) {
                remainder.add(source);
            }
        }
        return remainder;
    }

    private static List<SourceEntry> toSourceEntries(List<JavaFileObject> sources) {
        List<SourceEntry> entries = new ArrayList<>();
        for (JavaFileObject source : sources) {
            String name = source.getName();
            if (name == null || name.isBlank()) {
                continue;
            }
            String path = name.startsWith("/") ? name.substring(1) : name;
            try {
                entries.add(new SourceEntry(path, source.getCharContent(true).toString()));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return entries;
    }

    private static boolean declaresFailedType(JavaFileObject source, Set<String> failed) {
        try {
            Set<String> declared = StudentSourceNormalizer.extractDeclaredSimpleNames(
                    source.getCharContent(true).toString());
            for (String name : declared) {
                if (failed.contains(name)) {
                    return true;
                }
            }
            return false;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<JavaFileObject> sourcesWithoutErrorDiagnostics(
            List<JavaFileObject> sources,
            List<Diagnostic<? extends JavaFileObject>> diagnostics) {
        Set<JavaFileObject> errorSources = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<URI> errorUris = new HashSet<>();
        for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics) {
            if (diagnostic.getKind() != Diagnostic.Kind.ERROR) {
                continue;
            }
            JavaFileObject reported = diagnostic.getSource();
            if (reported == null) {
                continue;
            }
            errorSources.add(reported);
            errorUris.add(reported.toUri());
        }
        List<JavaFileObject> remainder = new ArrayList<>();
        for (JavaFileObject source : sources) {
            if (!errorSources.contains(source) && !errorUris.contains(source.toUri())) {
                remainder.add(source);
            }
        }
        return remainder;
    }

    private static boolean hasAnyClassFile(Path outputDir) {
        if (!Files.isDirectory(outputDir)) {
            return false;
        }
        try (var stream = Files.walk(outputDir)) {
            return stream.anyMatch(path -> Files.isRegularFile(path)
                    && path.getFileName().toString().endsWith(".class"));
        } catch (IOException e) {
            return false;
        }
    }

    private static int countClassFiles(Path outputDir) {
        if (!Files.isDirectory(outputDir)) {
            return 0;
        }
        try (var stream = Files.walk(outputDir)) {
            return (int) stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".class"))
                    .count();
        } catch (IOException e) {
            return 0;
        }
    }

    private StandardJavaFileManager fileManager() {
        StandardJavaFileManager fileManager = fileManagerHolder.get();
        if (fileManager != null) {
            return fileManager;
        }
        fileManager = compiler.getStandardFileManager(null, Locale.getDefault(), null);
        try {
            fileManager.setLocation(StandardLocation.CLASS_PATH, List.of());
        } catch (IOException e) {
            try {
                fileManager.close();
            } catch (IOException ignored) {
            }
            throw new UncheckedIOException(e);
        }
        fileManagerHolder.set(fileManager);
        return fileManager;
    }

    private static void deleteRecursively(Path path) {
        if (!Files.exists(path)) {
            return;
        }
        try (var stream = Files.walk(path)) {
            stream.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }

    private void resetFileManager() {
        StandardJavaFileManager manager = fileManagerHolder.get();
        if (manager != null) {
            try {
                manager.close();
            } catch (IOException ignored) {
            }
            fileManagerHolder.remove();
        }
    }
}
