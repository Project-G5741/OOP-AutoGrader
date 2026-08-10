package com.eiu.capstone.backend.service;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import jakarta.annotation.PostConstruct;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.springframework.stereotype.Service;

import com.eiu.capstone.backend.exception.SubmissionProcessingException;

@Service
public class JavaCompilerService {

    private JavaCompiler compiler;
    private final ThreadLocal<StandardJavaFileManager> fileManagerHolder = new ThreadLocal<>();

    @PostConstruct
    void initCompiler() {
        compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException(
                    "No system Java compiler available — the backend must run on a JDK, not a JRE.");
        }
    }

    public List<String> compileSources(List<JavaFileObject> sources, Path outputDir) {
        if (sources.isEmpty()) {
            return List.of();
        }

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager fileManager = fileManagerHolder.get();
        if (fileManager == null) {
            fileManager = compiler.getStandardFileManager(null, Locale.getDefault(), null);
            fileManagerHolder.set(fileManager);
        }

        List<String> options = List.of("-d", outputDir.toString(), "-encoding", "UTF-8");

        StringWriter errorOutput = new StringWriter();
        JavaCompiler.CompilationTask task = compiler.getTask(
                errorOutput, fileManager, diagnostics, options, null, sources);

        boolean success;
        try {
            success = task.call();
        } catch (RuntimeException e) {
            resetFileManager();
            throw e;
        }

        List<String> messages = diagnostics.getDiagnostics().stream()
                .map(d -> String.format("%s: line %d: %s",
                        d.getKind(), d.getLineNumber(), d.getMessage(Locale.getDefault())))
                .toList();

        if (!success) {
            resetFileManager();
            throw new SubmissionProcessingException("Compilation failed:\n" + String.join("\n", messages));
        }

        return messages;
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
