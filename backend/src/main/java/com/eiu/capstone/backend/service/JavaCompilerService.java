package com.eiu.capstone.backend.service;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.springframework.stereotype.Service;

import com.eiu.capstone.backend.exception.SubmissionProcessingException;

@Service
public class JavaCompilerService {

    /**
     * Compiles the given .java source files, writing the resulting .class files
     * directly into outputDir (via the "-d" compiler flag).
     *
     * IMPORTANT: this requires the backend to run on a full JDK, not a JRE —
     * ToolProvider.getSystemJavaCompiler() returns null on a JRE-only runtime.
     * Check your Dockerfile's base image (e.g. eclipse-temurin:21-jdk, not -jre).
     *
     * @return diagnostic messages (warnings included) collected during compilation.
     * @throws SubmissionProcessingException if compilation fails or no compiler is available.
     */
    public List<String> compile(List<Path> javaSourceFiles, Path outputDir) {
        if (javaSourceFiles.isEmpty()) {
            return List.of();
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new SubmissionProcessingException(
                    "No system Java compiler available — the backend must run on a JDK, not a JRE.");
        }

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager fileManager =
                     compiler.getStandardFileManager(diagnostics, Locale.getDefault(), null)) {

            Iterable<? extends JavaFileObject> compilationUnits =
                    fileManager.getJavaFileObjectsFromPaths(javaSourceFiles);

            List<String> options = List.of(
                    "-d", outputDir.toString(),
                    "-encoding", "UTF-8"
            );

            StringWriter errorOutput = new StringWriter();
            JavaCompiler.CompilationTask task = compiler.getTask(
                    errorOutput, fileManager, diagnostics, options, null, compilationUnits);

            boolean success = task.call();

            List<String> messages = diagnostics.getDiagnostics().stream()
                    .map(d -> String.format("%s: line %d: %s",
                            d.getKind(), d.getLineNumber(), d.getMessage(Locale.getDefault())))
                    .collect(Collectors.toList());

            if (!success) {
                throw new SubmissionProcessingException("Compilation failed:\n" + String.join("\n", messages));
            }

            return messages;
        } catch (IOException e) {
            throw new SubmissionProcessingException("Failed to compile submitted Java files", e);
        }
    }
}