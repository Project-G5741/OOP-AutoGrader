package com.eiu.capstone.backend.grading;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.eiu.capstone.backend.grading.ParsedSubmissionSnapshot.ChallengeSnapshot;
import com.eiu.capstone.backend.grading.ParsedSubmissionSnapshot.ClassConstructorEntry;
import com.eiu.capstone.backend.grading.ParsedSubmissionSnapshot.ClassFieldEntry;
import com.eiu.capstone.backend.grading.ParsedSubmissionSnapshot.ClassMethodEntry;
import com.eiu.capstone.backend.grading.ParsedSubmissionSnapshot.ClassSnapshot;
import com.eiu.capstone.backend.grading.ParsedSubmissionSnapshot.MmdRelationEntry;
import com.eiu.capstone.backend.grading.ParsedSubmissionSnapshot.MmdSnapshot;
import com.eiu.capstone.backend.grading.rubric.ChallengeRubric;
import com.eiu.capstone.backend.grading.rubric.ClassRubric;
import com.eiu.capstone.backend.grading.rubric.ConstructorRubric;
import com.eiu.capstone.backend.grading.rubric.FieldRubric;
import com.eiu.capstone.backend.grading.rubric.MethodRubric;
import com.eiu.capstone.backend.grading.rubric.RelationRubric;

@Component
public class ParsedSubmissionSnapshotBuilder {

    public ChallengeSnapshot build(ChallengeRubric rubric,
                                   List<ParsedClass> parsedClasses,
                                   ParsedMmdDiagram diagram) {
        ChallengeSnapshot snapshot = new ChallengeSnapshot();
        Map<String, ParsedClass> parsedByName = parsedClasses.stream()
                .collect(Collectors.toMap(pc -> pc.simpleName, pc -> pc, (a, b) -> a));
        snapshot.classSnapshot = buildClassSnapshot(rubric, parsedByName);
        snapshot.mmdSnapshot = buildMmdSnapshot(rubric, diagram);
        return snapshot;
    }

    private ClassSnapshot buildClassSnapshot(ChallengeRubric rubric, Map<String, ParsedClass> parsedByName) {
        ClassSnapshot classSnapshot = new ClassSnapshot();
        for (ClassRubric expectedClass : rubric.classes()) {
            ParsedClass parsed = parsedByName.get(expectedClass.name());
            if (parsed == null) {
                continue;
            }

            Map<String, ParsedField> parsedFields = parsed.fields.stream()
                    .collect(Collectors.toMap(f -> f.name, f -> f, (a, b) -> a));
            for (FieldRubric expectedField : expectedClass.fields()) {
                ParsedField pf = parsedFields.get(expectedField.name());
                if (pf == null) {
                    continue;
                }
                ClassFieldEntry entry = new ClassFieldEntry();
                entry.name = pf.name;
                entry.scope = pf.scope;
                entry.dataType = pf.dataType;
                classSnapshot.fields.put(expectedField.id().toString(), entry);
            }

            for (MethodRubric expectedMethod : expectedClass.methods()) {
                ParsedMethod match = findMatchingMethod(parsed.methods, expectedMethod.name(), expectedMethod.parameterTypes());
                if (match == null) {
                    continue;
                }
                ClassMethodEntry entry = new ClassMethodEntry();
                entry.name = match.name;
                entry.scope = match.scope;
                entry.returnType = match.returnType;
                entry.isStatic = match.isStatic;
                entry.isAbstract = match.isAbstract;
                entry.isFinal = match.isFinal;
                classSnapshot.methods.put(expectedMethod.id().toString(), entry);
            }

            for (ConstructorRubric expectedConstructor : expectedClass.constructors()) {
                ParsedConstructor match = findMatchingConstructor(parsed.constructors, expectedConstructor.parameterTypes());
                if (match == null) {
                    continue;
                }
                ClassConstructorEntry entry = new ClassConstructorEntry();
                entry.name = expectedClass.name();
                entry.scope = match.scope;
                entry.params = formatParamTypes(match.parameterTypes);
                classSnapshot.constructors.put(expectedConstructor.id().toString(), entry);
            }
        }
        return classSnapshot;
    }

    private MmdSnapshot buildMmdSnapshot(ChallengeRubric rubric, ParsedMmdDiagram diagram) {
        MmdSnapshot mmdSnapshot = new MmdSnapshot();
        if (diagram == null) {
            return mmdSnapshot;
        }

        Map<String, ParsedMmdClass> parsedByName = diagram.classes.stream()
                .collect(Collectors.toMap(c -> c.name, c -> c, (a, b) -> a));

        for (ClassRubric expectedClass : rubric.classes()) {
            ParsedMmdClass parsed = parsedByName.get(expectedClass.name());
            if (parsed != null) {
                String stereotype = parsed.stereotypeType != null ? parsed.stereotypeType : "class";
                mmdSnapshot.stereotypes.put(
                        expectedClass.id().toString(),
                        "<<" + stereotype.toLowerCase(Locale.ROOT) + ">>");
            }

            if (parsed == null) {
                continue;
            }

            Map<String, ParsedField> fieldsByName = parsed.fields.stream()
                    .collect(Collectors.toMap(f -> f.name.toLowerCase(Locale.ROOT), f -> f, (a, b) -> a));
            for (FieldRubric expectedField : expectedClass.fields()) {
                ParsedField pf = fieldsByName.get(expectedField.name().toLowerCase(Locale.ROOT));
                if (pf != null) {
                    mmdSnapshot.attributes.put(expectedField.id().toString(), pf.name + ": " + pf.dataType);
                }
            }

            for (ConstructorRubric expectedConstructor : expectedClass.constructors()) {
                ParsedConstructor match = findMatchingMmdConstructor(parsed.constructors, expectedConstructor.parameterTypes());
                if (match != null) {
                    String paramList = match.parameterTypes != null
                            ? String.join(", ", match.parameterTypes)
                            : "";
                    mmdSnapshot.attributes.put(
                            expectedConstructor.id().toString(),
                            expectedClass.name() + "(" + paramList + ")");
                }
            }

            for (MethodRubric expectedMethod : expectedClass.methods()) {
                ParsedMethod match = findMatchingMethod(parsed.methods, expectedMethod.name(), expectedMethod.parameterTypes());
                if (match != null) {
                    String paramList = match.parameterTypes != null
                            ? String.join(", ", match.parameterTypes)
                            : "";
                    mmdSnapshot.attributes.put(
                            expectedMethod.id().toString(),
                            match.name + "(" + paramList + ") " + match.returnType);
                }
            }
        }

        for (RelationRubric expectedRelation : rubric.relations()) {
            ParsedMmdRelation parsedRelation = findConnectingRelation(diagram, expectedRelation);
            if (parsedRelation != null) {
                MmdRelationEntry entry = new MmdRelationEntry();
                entry.from = parsedRelation.sourceClassName;
                entry.to = parsedRelation.targetClassName;
                entry.relType = MmdComparisonService.displayRelationTypeName(parsedRelation.relationType);
                mmdSnapshot.relations.put(expectedRelation.id().toString(), entry);
            }
        }

        return mmdSnapshot;
    }

    private ParsedMmdRelation findConnectingRelation(ParsedMmdDiagram diagram, RelationRubric expected) {
        for (ParsedMmdRelation parsed : diagram.relations) {
            if (classesConnected(parsed, expected.sourceClassName(), expected.targetClassName())) {
                return parsed;
            }
        }
        return null;
    }

    private boolean classesConnected(ParsedMmdRelation parsed, String source, String target) {
        return (namesEqual(parsed.sourceClassName, source) && namesEqual(parsed.targetClassName, target))
                || (namesEqual(parsed.sourceClassName, target) && namesEqual(parsed.targetClassName, source));
    }

    private boolean namesEqual(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return a.trim().equalsIgnoreCase(b.trim());
    }

    private ParsedMethod findMatchingMethod(List<ParsedMethod> candidates, String name, List<String> expectedParamTypes) {
        for (ParsedMethod pm : candidates) {
            if (pm.name != null && pm.name.equals(name) && sameTypes(pm.parameterTypes, expectedParamTypes)) {
                return pm;
            }
        }
        return null;
    }

    private ParsedConstructor findMatchingConstructor(List<ParsedConstructor> candidates, List<String> expectedParamTypes) {
        for (ParsedConstructor pc : candidates) {
            if (sameTypes(pc.parameterTypes, expectedParamTypes)) {
                return pc;
            }
        }
        return null;
    }

    private ParsedConstructor findMatchingMmdConstructor(List<ParsedConstructor> candidates, List<String> expectedParamTypes) {
        return findMatchingConstructor(candidates, expectedParamTypes);
    }

    private boolean sameTypes(List<String> a, List<String> b) {
        if (a == null || b == null) {
            return a == b;
        }
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!equalsIgnoreCase(a.get(i), b.get(i))) {
                return false;
            }
        }
        return true;
    }

    private boolean equalsIgnoreCase(String a, String b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.trim().equalsIgnoreCase(b.trim());
    }

    private String formatParamTypes(List<String> parameterTypes) {
        if (parameterTypes == null || parameterTypes.isEmpty()) {
            return "";
        }
        return String.join(", ", parameterTypes);
    }
}
