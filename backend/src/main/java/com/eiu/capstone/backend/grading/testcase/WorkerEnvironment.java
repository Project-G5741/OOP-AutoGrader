package com.eiu.capstone.backend.grading.testcase;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Clears the child environment then copies only JVM/OS launch keys. Does not copy API secrets.
 */
public final class WorkerEnvironment {

    private static final Set<String> ALLOWED = Set.of(
            "PATH",
            "PATHEXT",
            "SYSTEMROOT",
            "WINDIR",
            "SYSTEMDRIVE",
            "OS",
            "TMP",
            "TEMP",
            "TMPDIR",
            "TEMPDIR",
            "COMSPEC",
            "LANG",
            "LC_ALL",
            "LC_CTYPE",
            "TZ");

    private WorkerEnvironment() {}

    public static void apply(ProcessBuilder processBuilder) {
        Map<String, String> env = processBuilder.environment();
        Map<String, String> parent = Map.copyOf(env);
        env.clear();
        for (Map.Entry<String, String> entry : parent.entrySet()) {
            if (allowed(entry.getKey())) {
                env.put(entry.getKey(), entry.getValue());
            }
        }
    }

    public static boolean allowed(String name) {
        if (name == null) {
            return false;
        }
        return ALLOWED.contains(name.toUpperCase(Locale.ROOT));
    }
}
