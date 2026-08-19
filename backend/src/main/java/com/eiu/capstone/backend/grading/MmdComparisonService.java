package com.eiu.capstone.backend.grading;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.eiu.capstone.backend.grading.rubric.ChallengeRubric;
import com.eiu.capstone.backend.grading.rubric.ClassRubric;
import com.eiu.capstone.backend.grading.rubric.ConstructorRubric;
import com.eiu.capstone.backend.grading.rubric.FieldRubric;
import com.eiu.capstone.backend.grading.rubric.MethodRubric;
import com.eiu.capstone.backend.grading.rubric.RelationRubric;

@Component
public class MmdComparisonService {

    public MmdGradingOutcome compare(ChallengeRubric rubric, ParsedMmdDiagram diagram) {
        MmdGradingOutcome.ChallengeRubricElements elements = collectElementIds(rubric);
        if (diagram == null) {
            return MmdGradingOutcome.allIncorrect(elements);
        }

        MmdGradingOutcome outcome = new MmdGradingOutcome();
        Map<String, ParsedMmdClass> classIndex = classIndex(diagram);

        for (ClassRubric expectedClass : rubric.classes()) {
            ParsedMmdClass parsed = resolveClass(classIndex, expectedClass.name());
            outcome.setClassPresent(expectedClass.id(), parsed != null);
            outcome.setClass(expectedClass.id(), parsed != null && classTypeMatches(expectedClass, parsed));

            if (parsed == null) {
                expectedClass.fields().forEach(f -> outcome.setField(f.id(), false));
                expectedClass.methods().forEach(m -> outcome.setMethod(m.id(), false));
                expectedClass.constructors().forEach(c -> outcome.setConstructor(c.id(), false));
                continue;
            }

            boolean hasGetterShorthand = parsed.methods.stream()
                    .anyMatch(m -> "__getter_shorthand__".equals(m.name));
            boolean hasSetterShorthand = parsed.methods.stream()
                    .anyMatch(m -> "__setter_shorthand__".equals(m.name));
            boolean solutionHasGetters = expectedClass.methods().stream().anyMatch(this::isGetter);
            boolean solutionHasSetters = expectedClass.methods().stream().anyMatch(this::isSetter);

            Map<String, ParsedField> fieldsByName = parsed.fields.stream()
                    .collect(Collectors.toMap(f -> f.name.toLowerCase(Locale.ROOT), f -> f, (a, b) -> a));

            for (FieldRubric expectedField : expectedClass.fields()) {
                ParsedField pf = fieldsByName.get(expectedField.name().toLowerCase(Locale.ROOT));
                boolean correct = pf != null
                        && scopesMatch(expectedField.scope(), pf.scope)
                        && MmdTypeEquivalence.typesMatch(expectedField.dataType(), pf.dataType);
                outcome.setField(expectedField.id(), correct);
            }

            for (MethodRubric expectedMethod : expectedClass.methods()) {
                if (isGetter(expectedMethod) && solutionHasGetters && hasGetterShorthand) {
                    outcome.setMethod(expectedMethod.id(), true);
                    continue;
                }
                if (isSetter(expectedMethod) && solutionHasSetters && hasSetterShorthand) {
                    outcome.setMethod(expectedMethod.id(), true);
                    continue;
                }
                ParsedMethod match = findMatchingMethod(parsed.methods, expectedMethod.name(), expectedMethod.parameterTypes());
                boolean correct = match != null && methodMatches(expectedMethod, match, parsed);
                outcome.setMethod(expectedMethod.id(), correct);
            }

            for (ConstructorRubric expectedConstructor : expectedClass.constructors()) {
                ParsedConstructor match = findMatchingConstructor(parsed.constructors, expectedConstructor.parameterTypes());
                boolean correct = match != null
                        && scopesMatch(expectedConstructor.scope(), match.scope)
                        && constructorTypesMatch(expectedConstructor.parameterTypes(), match.parameterTypes);
                outcome.setConstructor(expectedConstructor.id(), correct);
            }
        }

        for (RelationRubric expectedRelation : rubric.relations()) {
            boolean correct = diagram.relations.stream().anyMatch(parsed ->
                    relationMatches(expectedRelation, parsed, classIndex));
            outcome.setRelation(expectedRelation.id(), correct);
        }

        return outcome;
    }

    private Map<String, ParsedMmdClass> classIndex(ParsedMmdDiagram diagram) {
        if (diagram.classByName != null && !diagram.classByName.isEmpty()) {
            return diagram.classByName;
        }
        return diagram.classes.stream()
                .collect(Collectors.toMap(c -> c.name, c -> c, (a, b) -> a, LinkedHashMap::new));
    }

    private ParsedMmdClass resolveClass(Map<String, ParsedMmdClass> classIndex, String name) {
        if (name == null) {
            return null;
        }
        String trimmed = name.trim();
        ParsedMmdClass direct = classIndex.get(trimmed);
        if (direct != null) {
            return direct;
        }
        int dot = trimmed.lastIndexOf('.');
        if (dot >= 0) {
            return classIndex.get(trimmed.substring(dot + 1));
        }
        return null;
    }

    private MmdGradingOutcome.ChallengeRubricElements collectElementIds(ChallengeRubric rubric) {
        return new MmdGradingOutcome.ChallengeRubricElements(
                rubric.classes().stream().map(ClassRubric::id).toList(),
                rubric.classes().stream().flatMap(c -> c.fields().stream()).map(FieldRubric::id).toList(),
                rubric.classes().stream().flatMap(c -> c.methods().stream()).map(MethodRubric::id).toList(),
                rubric.classes().stream().flatMap(c -> c.constructors().stream()).map(ConstructorRubric::id).toList(),
                rubric.relations().stream().map(RelationRubric::id).toList());
    }

    private boolean classTypeMatches(ClassRubric expected, ParsedMmdClass parsed) {
        String expectedType = normalizeDeclaringType(expected.declaringType());
        String actualType = parsed.stereotypeType == null ? "CLASS" : parsed.stereotypeType.toUpperCase(Locale.ROOT);
        if ("ENUMERATE".equals(actualType)) {
            actualType = "ENUM";
        }
        if ("ABSTRACT".equals(actualType)) {
            actualType = "CLASS";
        }
        return expectedType.equals(actualType);
    }

    private String normalizeDeclaringType(String declaringType) {
        if (declaringType == null) return "CLASS";
        return declaringType.trim().toUpperCase(Locale.ROOT);
    }

    private boolean methodMatches(MethodRubric expected, ParsedMethod actual, ParsedMmdClass parsedClass) {
        boolean effectiveAbstract = actual.isAbstract || interfaceMethodIsImplicitlyAbstract(parsedClass, actual);
        return scopesMatch(expected.scope(), actual.scope)
                && MmdTypeEquivalence.typesMatch(expected.returnType(), actual.returnType)
                && constructorTypesMatch(expected.parameterTypes(), actual.parameterTypes)
                && mmdModifierMatches(expected.isStatic(), actual.isStatic)
                && mmdModifierMatches(expected.isAbstract(), effectiveAbstract)
                && mmdModifierMatches(expected.isFinal(), actual.isFinal);
    }

    private boolean interfaceMethodIsImplicitlyAbstract(ParsedMmdClass parsedClass, ParsedMethod actual) {
        if (parsedClass.stereotypeType == null || actual.isStatic) {
            return false;
        }
        return "interface".equalsIgnoreCase(parsedClass.stereotypeType);
    }

    /** Rubric-required modifiers must appear in the diagram; optional markers are ignored when not required. */
    private boolean mmdModifierMatches(boolean required, boolean present) {
        return !required || present;
    }

    public boolean relationPresentInDiagram(RelationRubric expected, ParsedMmdDiagram diagram) {
        if (diagram == null) {
            return false;
        }
        Map<String, ParsedMmdClass> classIndex = classIndex(diagram);
        return diagram.relations.stream().anyMatch(parsed ->
                classesConnected(parsed, expected.sourceClassName(), expected.targetClassName(), classIndex));
    }

    private boolean classesConnected(
            ParsedMmdRelation parsed, String source, String target, Map<String, ParsedMmdClass> classIndex) {
        return (classNamesMatch(parsed.sourceClassName, source, classIndex)
                && classNamesMatch(parsed.targetClassName, target, classIndex))
                || (classNamesMatch(parsed.sourceClassName, target, classIndex)
                && classNamesMatch(parsed.targetClassName, source, classIndex));
    }

    private boolean relationMatches(
            RelationRubric expected, ParsedMmdRelation parsed, Map<String, ParsedMmdClass> classIndex) {
        if (!relationTypesMatch(expected.relationTypeName(), parsed.relationType)) {
            return false;
        }
        boolean forward = classNamesMatch(expected.sourceClassName(), parsed.sourceClassName, classIndex)
                && classNamesMatch(expected.targetClassName(), parsed.targetClassName, classIndex);
        if (forward) {
            return true;
        }
        if (isUndirectedRelationType(parsed.relationType)) {
            return classNamesMatch(expected.sourceClassName(), parsed.targetClassName, classIndex)
                    && classNamesMatch(expected.targetClassName(), parsed.sourceClassName, classIndex);
        }
        return false;
    }

    private static boolean isUndirectedRelationType(String relationType) {
        return "link".equals(relationType)
                || "dashed_link".equals(relationType)
                || "bidirectional_association".equals(relationType)
                || "bidirectional_inheritance".equals(relationType);
    }

    private boolean relationTypesMatch(String expectedName, String parsedCanonical) {
        String normalizedExpected = normalizeRelationTypeName(expectedName);
        return normalizedExpected.equals(parsedCanonical);
    }

    public static String normalizeRelationTypeName(String name) {
        if (name == null) return "";
        String lower = name.trim().toLowerCase(Locale.ROOT);
        if (lower.contains("bidirectional") && lower.contains("inherit")) {
            return "bidirectional_inheritance";
        }
        if (lower.contains("bidirectional")) {
            return "bidirectional_association";
        }
        if (lower.contains("dashed")) {
            return "dashed_link";
        }
        if (lower.contains("general") || lower.contains("inherit") || lower.contains("extend")) return "inheritance";
        if (lower.contains("compos")) return "composition";
        if (lower.contains("aggreg")) return "aggregation";
        if (lower.contains("bidirectional")) return "bidirectional_association";
        if (lower.contains("associ")) return "association";
        if (lower.contains("depend")) return "dependency";
        if (lower.contains("realiz") || lower.contains("implement")) return "realization";
        if (lower.contains("link")) return "link";
        return lower.replace(' ', '_');
    }

    /** Preferred UI label for canonical relation types (realization → implementation). */
    public static String displayRelationTypeName(String name) {
        String canonical = normalizeRelationTypeName(name);
        if ("realization".equals(canonical)) {
            return "implementation";
        }
        return canonical;
    }

    private boolean isGetter(MethodRubric method) {
        String name = method.name();
        return name.startsWith("get") || name.startsWith("is");
    }

    private boolean isSetter(MethodRubric method) {
        return method.name().startsWith("set");
    }

    private ParsedMethod findMatchingMethod(List<ParsedMethod> candidates, String name, List<String> expectedParamTypes) {
        for (ParsedMethod method : candidates) {
            if ("__getter_shorthand__".equals(method.name) || "__setter_shorthand__".equals(method.name)) {
                continue;
            }
            if (method.name.equals(name) && constructorTypesMatch(expectedParamTypes, method.parameterTypes)) {
                return method;
            }
        }
        return null;
    }

    private ParsedConstructor findMatchingConstructor(List<ParsedConstructor> candidates, List<String> expectedParamTypes) {
        for (ParsedConstructor constructor : candidates) {
            if (constructorTypesMatch(expectedParamTypes, constructor.parameterTypes)) {
                return constructor;
            }
        }
        return null;
    }

    private boolean constructorTypesMatch(List<String> expected, List<String> actual) {
        if (expected.size() != actual.size()) return false;
        for (int i = 0; i < expected.size(); i++) {
            if (!MmdTypeEquivalence.typesMatch(expected.get(i), actual.get(i))) {
                return false;
            }
        }
        return true;
    }

    private boolean scopesMatch(String expected, String actual) {
        if (expected == null && actual == null) return true;
        if (expected == null || actual == null) return false;
        return expected.trim().equalsIgnoreCase(actual.trim());
    }

    private boolean classNamesMatch(String left, String right, Map<String, ParsedMmdClass> classIndex) {
        if (left == null || right == null) {
            return false;
        }
        if (left.trim().equals(right.trim())) {
            return true;
        }
        ParsedMmdClass leftClass = resolveClass(classIndex, left);
        ParsedMmdClass rightClass = resolveClass(classIndex, right);
        if (leftClass != null && rightClass != null) {
            return leftClass == rightClass;
        }
        return simpleName(left).equals(simpleName(right));
    }

    private static String simpleName(String name) {
        String trimmed = name.trim();
        int dot = trimmed.lastIndexOf('.');
        return dot >= 0 ? trimmed.substring(dot + 1) : trimmed;
    }
}
