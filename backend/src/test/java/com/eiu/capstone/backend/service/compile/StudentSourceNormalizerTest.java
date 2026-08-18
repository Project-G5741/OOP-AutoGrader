package com.eiu.capstone.backend.service.compile;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.service.compile.StudentSourceNormalizer.NormalizationResult;
import com.eiu.capstone.backend.service.compile.StudentSourceNormalizer.SourceEntry;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StudentSourceNormalizerTest {

    @Test
    void stripsPackageLine() {
        NormalizationResult result = StudentSourceNormalizer.normalizeChallengeSources(List.of(
                new SourceEntry("Employee.java", """
                        package ch2_employees;
                        public class Employee {}
                        """)));

        assertTrue(result.packageStripped());
        assertFalse(result.sources().get(0).source().contains("package"));
        assertTrue(result.sources().get(0).source().contains("public class Employee"));
    }

    @Test
    void leavesDefaultPackageSourcesUnchanged() {
        NormalizationResult result = StudentSourceNormalizer.normalizeChallengeSources(List.of(
                new SourceEntry("Good.java", "public class Good {}")));

        assertFalse(result.packageStripped());
        assertTrue(result.sources().get(0).source().contains("public class Good"));
    }

    @Test
    void removesSameChallengeImportButKeepsJavaUtil() {
        NormalizationResult result = StudentSourceNormalizer.normalizeChallengeSources(List.of(
                new SourceEntry("Employee.java", """
                        package ch2;
                        public class Employee {}
                        """),
                new SourceEntry("Main.java", """
                        package ch2;
                        import ch2.Employee;
                        import java.util.ArrayList;
                        public class Main {
                          Employee e;
                          ArrayList<String> list;
                        }
                        """)));

        assertTrue(result.packageStripped());
        String mainSource = result.sources().get(1).source();
        assertFalse(mainSource.contains("import ch2.Employee"));
        assertTrue(mainSource.contains("import java.util.ArrayList"));
        assertFalse(mainSource.contains("package"));
    }
}
