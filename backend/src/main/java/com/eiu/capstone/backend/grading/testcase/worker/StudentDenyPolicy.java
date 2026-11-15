package com.eiu.capstone.backend.grading.testcase.worker;

import java.io.FilePermission;
import java.net.NetPermission;
import java.net.SocketPermission;
import java.nio.file.Path;
import java.security.Permission;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Java-level deny for student operational-test code on the Render worker.
 * The harness is allowed through. Student frames cannot use the network,
 * start a process, or touch the filesystem except to read their own {@code .class} files.
 */
public final class StudentDenyPolicy {

    public static final String BLOCKED_MESSAGE = "This call was blocked.";
    public static final String SYSPROP = "app.grading.student-deny";

    private static final List<Path> CLASS_ROOTS = new ArrayList<>();

    private StudentDenyPolicy() {}

    public static void allowClassRoot(Path classesDir) {
        if (classesDir == null) {
            return;
        }
        Path root = classesDir.toAbsolutePath().normalize();
        synchronized (CLASS_ROOTS) {
            if (!CLASS_ROOTS.contains(root)) {
                CLASS_ROOTS.add(root);
            }
        }
    }

    public static void install() {
        if (System.getSecurityManager() instanceof DenyManager) {
            return;
        }
        System.setSecurityManager(new DenyManager());
    }

    public static boolean isDenied(Permission permission, Class<?>[] stack) {
        if (permission == null || !sensitive(permission)) {
            return false;
        }
        if (!studentOnStack(stack)) {
            return false;
        }
        return !allowedClassFileRead(permission);
    }

    private static boolean sensitive(Permission permission) {
        if (permission instanceof SocketPermission || permission instanceof NetPermission) {
            return true;
        }
        if (permission instanceof FilePermission) {
            return true;
        }
        if (permission instanceof RuntimePermission) {
            String name = permission.getName() == null ? "" : permission.getName();
            return "setSecurityManager".equals(name)
                    || name.startsWith("createClassLoader")
                    || name.startsWith("loadLibrary")
                    || name.startsWith("exitVM");
        }
        return false;
    }

    private static boolean studentOnStack(Class<?>[] stack) {
        if (stack == null) {
            return false;
        }
        ClassLoader system = ClassLoader.getSystemClassLoader();
        ClassLoader platform = ClassLoader.getPlatformClassLoader();
        for (Class<?> type : stack) {
            if (type == null) {
                continue;
            }
            ClassLoader loader = type.getClassLoader();
            if (loader != null && loader != system && loader != platform) {
                return true;
            }
        }
        return false;
    }

    /** Class files, and extension-less directories the loader stats while defining them. */
    private static boolean readableUnderRoot(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        return lower.endsWith(".class") || !fileName.contains(".");
    }

    private static boolean allowedClassFileRead(Permission permission) {
        if (!(permission instanceof FilePermission filePermission)) {
            return false;
        }
        String actions = filePermission.getActions() == null ? "" : filePermission.getActions();
        if (!"read".equals(actions)) {
            return false;
        }
        String name = filePermission.getName();
        if (name == null || name.isBlank() || name.contains("*")) {
            return false;
        }
        Path path;
        try {
            path = Path.of(name).toAbsolutePath().normalize();
        } catch (Exception e) {
            return false;
        }
        if (path.getFileName() == null || !readableUnderRoot(path.getFileName().toString())) {
            return false;
        }
        synchronized (CLASS_ROOTS) {
            for (Path root : CLASS_ROOTS) {
                if (path.startsWith(root)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static final class DenyManager extends SecurityManager {
        @Override
        public void checkPermission(Permission perm) {
            if (isDenied(perm, getClassContext())) {
                throw new SecurityException(BLOCKED_MESSAGE);
            }
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            checkPermission(perm);
        }
    }
}
