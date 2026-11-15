package unit.com.eiu.capstone.backend.grading.testcase.worker;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FilePermission;
import java.net.SocketPermission;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Permission;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.eiu.capstone.backend.grading.testcase.worker.StudentDenyPolicy;

class StudentDenyPolicyTest {

    private static Class<?> studentType;

    @BeforeAll
    static void loadStudentType() throws Exception {
        Path dir = Files.createTempDirectory("student-deny-probe");
        dir.toFile().deleteOnExit();
        Path source = dir.resolve("StudentProbe.java");
        Files.writeString(source, "public class StudentProbe {}");
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "JDK compiler is required");
        assertTrue(compiler.run(null, null, null, "-d", dir.toString(), source.toString()) == 0);
        URLClassLoader loader = new URLClassLoader(
                new URL[] {dir.toUri().toURL()}, ClassLoader.getPlatformClassLoader());
        studentType = loader.loadClass("StudentProbe");
    }

    @Test
    void harnessSocketIsAllowed() {
        assertFalse(StudentDenyPolicy.isDenied(
                new SocketPermission("localhost:80", "connect"),
                new Class<?>[] {String.class}));
    }

    @Test
    void studentSocketIsDenied() {
        assertTrue(denied(new SocketPermission("localhost:80", "connect")));
    }

    @Test
    void studentProcessStartIsDenied() {
        assertTrue(denied(new FilePermission("C:\\Windows\\notepad.exe", "execute")));
    }

    @Test
    void studentFileOutsideClassesDirIsDenied(@TempDir Path dir) {
        StudentDenyPolicy.allowClassRoot(dir);
        assertTrue(denied(new FilePermission("C:\\outside\\secret.txt", "read")));
    }

    @Test
    void studentClassFileReadUnderRootIsAllowed(@TempDir Path dir) {
        StudentDenyPolicy.allowClassRoot(dir);
        Path classFile = dir.resolve("StudentProbe.class");
        assertFalse(denied(new FilePermission(classFile.toString(), "read")));
    }

    @Test
    void studentSourceReadUnderRootIsDenied(@TempDir Path dir) {
        StudentDenyPolicy.allowClassRoot(dir);
        assertTrue(denied(new FilePermission(dir.resolve("StudentProbe.java").toString(), "read")));
    }

    @Test
    void studentWriteUnderRootIsDenied(@TempDir Path dir) {
        StudentDenyPolicy.allowClassRoot(dir);
        assertTrue(denied(new FilePermission(dir.resolve("StudentProbe.class").toString(), "write")));
    }

    @Test
    void studentExitIsDenied() {
        assertTrue(denied(new RuntimePermission("exitVM.0")));
    }

    private static boolean denied(Permission permission) {
        return StudentDenyPolicy.isDenied(permission, new Class<?>[] {studentType});
    }
}
