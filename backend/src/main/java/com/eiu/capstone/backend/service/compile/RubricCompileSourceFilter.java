package com.eiu.capstone.backend.service.compile;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.eiu.capstone.backend.grading.rubric.ChallengeRubric;
import com.eiu.capstone.backend.grading.rubric.ClassRubric;
import com.eiu.capstone.backend.service.compile.StudentSourceNormalizer.SourceEntry;

/**
 * Keeps only challenge sources that declare at least one rubric class simple name.
 * Lecturers must put every type needed to compile graded work on the rubric; extras
 * such as {@code Application} are omitted from javac.
 */
public final class RubricCompileSourceFilter {

    private RubricCompileSourceFilter() {
    }

    public static Set<String> allowedSimpleNames(ChallengeRubric challenge) {
        Set<String> names = new LinkedHashSet<>();
        if (challenge == null || challenge.classes() == null) {
            return names;
        }
        for (ClassRubric classRubric : challenge.classes()) {
            if (classRubric == null) {
                continue;
            }
            addIfPresent(names, classRubric.name());
            addIfPresent(names, classRubric.outerClassName());
        }
        return names;
    }

    /**
     * @param allowedSimpleNames {@code null} disables filtering; empty keeps nothing
     */
    public static List<SourceEntry> keepRubricSources(List<SourceEntry> sources,
                                                      Set<String> allowedSimpleNames) {
        if (sources == null || sources.isEmpty()) {
            return List.of();
        }
        if (allowedSimpleNames == null) {
            return List.copyOf(sources);
        }
        if (allowedSimpleNames.isEmpty()) {
            return List.of();
        }
        List<SourceEntry> kept = new ArrayList<>();
        for (SourceEntry entry : sources) {
            if (entry == null) {
                continue;
            }
            Set<String> declared = StudentSourceNormalizer.extractDeclaredSimpleNames(entry.source());
            for (String name : declared) {
                if (allowedSimpleNames.contains(name)) {
                    kept.add(entry);
                    break;
                }
            }
        }
        return kept;
    }

    private static void addIfPresent(Set<String> names, String value) {
        if (value != null && !value.isBlank()) {
            names.add(value);
        }
    }
}
