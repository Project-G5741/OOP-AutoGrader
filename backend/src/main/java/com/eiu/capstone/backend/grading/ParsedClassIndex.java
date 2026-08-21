package com.eiu.capstone.backend.grading;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.eiu.capstone.backend.grading.rubric.ClassRubric;

public record ParsedClassIndex(
        Map<String, ParsedClass> byName,
        Map<String, ParsedClass> byQualifiedName) {

    public static ParsedClassIndex of(List<ParsedClass> parsedClasses) {
        Map<String, ParsedClass> byName = new HashMap<>();
        Map<String, ParsedClass> byQualified = new HashMap<>();
        for (ParsedClass parsedClass : parsedClasses) {
            if (parsedClass.outerSimpleName != null && !parsedClass.outerSimpleName.isBlank()) {
                byQualified.put(parsedClass.outerSimpleName + "." + parsedClass.simpleName, parsedClass);
            } else {
                byName.put(parsedClass.simpleName, parsedClass);
            }
        }
        return new ParsedClassIndex(byName, byQualified);
    }

    public ParsedClass resolve(ClassRubric expectedClass) {
        if (expectedClass.isNested()) {
            return byQualifiedName.get(expectedClass.qualifiedName());
        }
        return byName.get(expectedClass.name());
    }
}
