package com.eiu.capstone.backend.grading.testcase;

import java.nio.file.Path;

final class SandboxClassesDirMapper {

    private SandboxClassesDirMapper() {
    }

    static String map(String classesDir, Path normalizedRoot, String containerRoot) {
        Path p = Path.of(classesDir).toAbsolutePath().normalize();
        if (p.startsWith(normalizedRoot)) {
            Path rel = normalizedRoot.relativize(p);
            return containerRoot + "/" + rel.toString().replace('\\', '/');
        }
        Path raw = Path.of(classesDir);
        if (!raw.isAbsolute() && raw.normalize().toString().replace('\\', '/').equals("classes")) {
            return containerRoot + "/classes";
        }
        throw new IllegalArgumentException("Classes directory is outside submission root: " + classesDir);
    }
}
