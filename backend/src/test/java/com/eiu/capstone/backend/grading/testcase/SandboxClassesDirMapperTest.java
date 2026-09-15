package com.eiu.capstone.backend.grading.testcase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SandboxClassesDirMapperTest {

    @TempDir
    Path tempDir;

    @Test
    void mapsPathUnderSubmissionRoot() throws Exception {
        Path root = tempDir.resolve("submission");
        Path classes = root.resolve("challenge_1").resolve("classes");
        java.nio.file.Files.createDirectories(classes);
        String mapped = SandboxClassesDirMapper.map(
                classes.toString(), root.toAbsolutePath().normalize(), "/work/submission");
        assertEquals("/work/submission/challenge_1/classes", mapped);
    }

    @Test
    void mapsBareClassesDirNameForDryRun() {
        String mapped = SandboxClassesDirMapper.map(
                "classes", tempDir.toAbsolutePath().normalize(), "/work/submission");
        assertEquals("/work/submission/classes", mapped);
    }

    @Test
    void rejectsPathOutsideSubmissionRoot() {
        assertThrows(IllegalArgumentException.class, () -> SandboxClassesDirMapper.map(
                "/other/classes",
                tempDir.toAbsolutePath().normalize(),
                "/work/submission"));
    }
}
