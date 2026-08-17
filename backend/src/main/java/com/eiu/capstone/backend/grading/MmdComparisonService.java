package com.eiu.capstone.backend.grading;

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
        Map<String, ParsedMmdClass> parsedByName = diagram.classes.stream()
                .collect(Collectors.toMap(c -> c.name, c -> c, (a, b) -> a));

        for (ClassRubric expectedClass : rubric.classes()) {
            ParsedMmdClass parsed = parsedByName.get(expectedClass.name());
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
                    relationMatches(expectedRelation, parsed));
            outcome.setRelation(expectedRelation.id(), correct);
        }

        return outcome;
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
        return diagram.relations.stream().anyMatch(parsed ->
                classesConnected(parsed, expected.sourceClassName(), expected.targetClassName()));
    }

    private boolean classesConnected(ParsedMmdRelation parsed, String source, String target) {
        return (namesEqual(parsed.sourceClassName, source) && namesEqual(parsed.targetClassName, target))
                || (namesEqual(parsed.sourceClassName, target) && namesEqual(parsed.targetClassName, source));
    }

    private boolean relationMatches(RelationRubric expected, ParsedMmdRelation parsed) {
        if (!relationTypesMatch(expected.relationTypeName(), parsed.relationType)) {
            return false;
        }
        boolean forward = namesEqual(expected.sourceClassName(), parsed.sourceClassName)
                && namesEqual(expected.targetClassName(), parsed.targetClassName);
        if (forward) return true;
        if ("link".equals(parsed.relationType)) {
            return namesEqual(expected.sourceClassName(), parsed.targetClassName)
                    && namesEqual(expected.targetClassName(), parsed.sourceClassName);
        }
        return false;
    }

    private boolean relationTypesMatch(String expectedName, String parsedCanonical) {
        String normalizedExpected = normalizeRelationTypeName(expectedName);
        return normalizedExpected.equals(parsedCanonical);
    }

    public static String normalizeRelationTypeName(String name) {
        if (name == null) return "";
        String lower = name.trim().toLowerCase(Locale.ROOT);
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

    private boolean namesEqual(String a, String b) {
        if (a == null || b == null) return false;
        return a.trim().equals(b.trim());
    }
}
