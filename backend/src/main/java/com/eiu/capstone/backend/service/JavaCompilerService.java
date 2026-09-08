package com.eiu.capstone.backend.service;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jakarta.annotation.PostConstruct;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.springframework.stereotype.Service;

import com.eiu.capstone.backend.service.compile.CompileOutcome;

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
        int classFileCount = countClassFiles(outputDir);
        if (classFileCount == 0) {
            List<JavaFileObject> remainder = sourcesWithoutErrorDiagnostics(sources, firstPass);
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
                classFileCount = countClassFiles(outputDir);
            }
        }

        return new CompileOutcome(false, firstPass, classFileCount);
    }

    private boolean runTask(List<JavaFileObject> sources,
                            Path outputDir,
                            DiagnosticCollector<JavaFileObject> diagnostics) {
        StandardJavaFileManager fileManager = fileManagerHolder.get();
        if (fileManager == null) {
            fileManager = compiler.getStandardFileManager(null, Locale.getDefault(), null);
            fileManagerHolder.set(fileManager);
        }

        List<String> options = List.of("-d", outputDir.toString(), "-encoding", "UTF-8");
        StringWriter errorOutput = new StringWriter();
        JavaCompiler.CompilationTask task = compiler.getTask(
                errorOutput, fileManager, diagnostics, options, null, sources);
        return Boolean.TRUE.equals(task.call());
    }

    private static List<JavaFileObject> sourcesWithoutErrorDiagnostics(
            List<JavaFileObject> sources,
            List<Diagnostic<? extends JavaFileObject>> diagnostics) {
        List<JavaFileObject> remainder = new ArrayList<>();
        for (JavaFileObject source : sources) {
            if (!sourceHasError(source, diagnostics)) {
                remainder.add(source);
            }
        }
        return remainder;
    }

    private static boolean sourceHasError(
            JavaFileObject source,
            List<Diagnostic<? extends JavaFileObject>> diagnostics) {
        URI uri = source.toUri();
        for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics) {
            if (diagnostic.getKind() != Diagnostic.Kind.ERROR) {
                continue;
            }
            JavaFileObject reported = diagnostic.getSource();
            if (reported == null) {
                continue;
            }
            if (reported == source || uri.equals(reported.toUri())) {
                return true;
            }
        }
        return false;
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
            throw new UncheckedIOException(e);
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
