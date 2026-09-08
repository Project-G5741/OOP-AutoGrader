package com.eiu.capstone.backend.service.compile;

import java.util.List;
import java.util.Locale;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/**
 * Result of compiling one challenge source list. {@code succeeded} is true only when
 * the first group javac completed without errors. Mixed failure keeps first-pass
 * diagnostics so callers can attribute ERROR files, and may still emit survivor
 * {@code .class} files via a remainder compile.
 */
public record CompileOutcome(
        boolean succeeded,
        List<Diagnostic<? extends JavaFileObject>> diagnostics,
        int classFileCount) {

    public CompileOutcome {
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
    }

    public static CompileOutcome skipped() {
        return new CompileOutcome(true, List.of(), 0);
    }

    public List<String> messages() {
        return diagnostics.stream()
                .map(d -> String.format("%s: line %d: %s",
                        d.getKind(), d.getLineNumber(), d.getMessage(Locale.getDefault())))
                .toList();
    }
}
