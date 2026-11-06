package com.eiu.capstone.backend.grading.pipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.eiu.capstone.backend.grading.MmdComparisonService;
import com.eiu.capstone.backend.grading.ParsedClass;
import com.eiu.capstone.backend.grading.rubric.ChallengeRubric;
import com.eiu.capstone.backend.grading.rubric.ClassRubric;
import com.eiu.capstone.backend.grading.rubric.RelationRubric;

/**
 * Declared-clause Extends/Implements check for the Java class shell.
 * Looks up at most one inheritance or realization row whose source is the expected class.
 */
public final class HeritageShellMatcher {

    private HeritageShellMatcher() {}

    static boolean heritageMatchesOrSkipped(ClassRubric expectedClass, ParsedClass parsed, ChallengeRubric rubric) {
        List<String> interfaces = parsed.interfaceSimpleNames != null
                ? parsed.interfaceSimpleNames
                : List.of();
        return heritageMatchesOrSkipped(
                expectedClass.id(),
                parsed.superclassSimpleName,
                interfaces,
                rubric.relations(),
                rubric.classes());
    }

    public static boolean heritageMatchesOrSkipped(
            UUID sourceClassId,
            String superclassSimpleName,
            List<String> interfaceSimpleNames,
            List<RelationRubric> relations,
            List<ClassRubric> classes) {
        RelationRubric pair = soleHeritageRow(sourceClassId, relations);
        if (pair == null) {
            return true;
        }
        ClassRubric target = findClass(classes, pair.targetClassId());
        String targetSimple = target != null ? target.name() : pair.targetClassName();
        String targetQualified = target != null ? target.qualifiedName() : pair.targetClassName();
        String kind = MmdComparisonService.normalizeRelationTypeName(pair.relationTypeName());
        if ("inheritance".equals(kind)) {
            return nameMatches(superclassSimpleName, targetSimple, targetQualified);
        }
        if ("realization".equals(kind)) {
            List<String> interfaces = interfaceSimpleNames != null ? interfaceSimpleNames : List.of();
            for (String iface : interfaces) {
                if (nameMatches(iface, targetSimple, targetQualified)) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    static RelationRubric soleHeritageRow(UUID sourceClassId, List<RelationRubric> relations) {
        if (sourceClassId == null || relations == null) {
            return null;
        }
        List<RelationRubric> heritage = new ArrayList<>();
        for (RelationRubric relation : relations) {
            if (!sourceClassId.equals(relation.sourceClassId())) {
                continue;
            }
            String kind = MmdComparisonService.normalizeRelationTypeName(relation.relationTypeName());
            if ("inheritance".equals(kind) || "realization".equals(kind)) {
                heritage.add(relation);
            }
        }
        if (heritage.size() != 1) {
            return null;
        }
        return heritage.get(0);
    }

    private static ClassRubric findClass(List<ClassRubric> classes, UUID classId) {
        if (classes == null || classId == null) {
            return null;
        }
        for (ClassRubric classRubric : classes) {
            if (classId.equals(classRubric.id())) {
                return classRubric;
            }
        }
        return null;
    }

    private static boolean nameMatches(String actualSimple, String targetSimple, String targetQualified) {
        if (actualSimple == null || actualSimple.isBlank()) {
            return false;
        }
        if (equalsIgnoreCase(actualSimple, targetSimple)) {
            return true;
        }
        return equalsIgnoreCase(actualSimple, targetQualified);
    }

    private static boolean equalsIgnoreCase(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return a.trim().equalsIgnoreCase(b.trim());
    }
}
