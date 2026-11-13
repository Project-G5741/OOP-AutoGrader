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
import com.eiu.capstone.backend.grading.ParsedSubmissionSnapshot.ClassShellEntry;
import com.eiu.capstone.backend.grading.ParsedSubmissionSnapshot.ClassSnapshot;
import com.eiu.capstone.backend.grading.ParsedSubmissionSnapshot.MmdRelationEntry;
import com.eiu.capstone.backend.grading.ParsedSubmissionSnapshot.MmdSnapshot;
import com.eiu.capstone.backend.grading.pipeline.ClassReflectionGrader;
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
        return build(rubric, ParsedClassIndex.of(parsedClasses), diagram);
    }

    public ChallengeSnapshot build(ChallengeRubric rubric,
                                   Map<String, ParsedClass> parsedByName,
                                   Map<String, ParsedClass> parsedByQualifiedName,
                                   ParsedMmdDiagram diagram) {
        return build(rubric, new ParsedClassIndex(parsedByName, parsedByQualifiedName), diagram);
    }

    public ChallengeSnapshot build(ChallengeRubric rubric,
                                   ParsedClassIndex parsedClassIndex,
                                   ParsedMmdDiagram diagram) {
        ChallengeSnapshot snapshot = new ChallengeSnapshot();
        snapshot.classSnapshot = buildClassSnapshot(rubric, parsedClassIndex);
        snapshot.mmdSnapshot = buildMmdSnapshot(rubric, diagram);
        return snapshot;
    }

    private ClassSnapshot buildClassSnapshot(ChallengeRubric rubric, ParsedClassIndex parsedClassIndex) {
        ClassSnapshot classSnapshot = new ClassSnapshot();
        for (ClassRubric expectedClass : rubric.classes()) {
            ParsedClass parsed = parsedClassIndex.resolve(expectedClass);
            if (parsed == null) {
                continue;
            }

            ClassShellEntry shell = new ClassShellEntry();
            shell.scope = parsed.scope;
            shell.declaringType = parsed.declaringType;
            shell.isAbstract = parsed.isAbstract;
            shell.isStatic = parsed.isStatic;
            shell.superclassSimpleName = parsed.superclassSimpleName;
            shell.interfaceSimpleNames = parsed.interfaceSimpleNames != null
                    ? parsed.interfaceSimpleNames
                    : List.of();
            classSnapshot.shells.put(expectedClass.id().toString(), shell);

            for (FieldRubric expectedField : expectedClass.fields()) {
                ParsedField pf = CartesianAliasMatcher.findBestDeclarationField(
                        parsed.fields,
                        expectedField,
                        EnumDeclaringTypes.isEnum(expectedClass.declaringType()));
                if (pf == null) {
                    continue;
                }
                ClassFieldEntry entry = new ClassFieldEntry();
                entry.name = pf.name;
                entry.scope = pf.scope;
                entry.dataType = pf.dataType;
                entry.isStatic = pf.isStatic;
                entry.isFinal = pf.isFinal;
                classSnapshot.fields.put(expectedField.id().toString(), entry);
            }

            for (MethodRubric expectedMethod : expectedClass.methods()) {
                ParsedMethod match = findMatchingMethod(parsed.methods, expectedMethod, false);
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
                ParsedConstructor match = findMatchingConstructor(
                        parsed.constructors, expectedConstructor, false);
                if (match == null) {
                    continue;
                }
                ClassConstructorEntry entry = new ClassConstructorEntry();
                entry.name = expectedClass.qualifiedName();
                entry.scope = match.scope;
                entry.params = formatParamTypes(match.parameterTypes);
                classSnapshot.constructors.put(expectedConstructor.id().toString(), entry);
            }
        }
        return classSnapshot;
    }

    public void enrichMemberGrades(ClassSnapshot classSnapshot,
                                   ClassReflectionGrader.ClassPillarResult classResult) {
        if (classSnapshot == null || classResult == null) {
            return;
        }
        for (ClassReflectionGrader.PendingFieldResult field : classResult.fields()) {
            classSnapshot.fieldGrades.put(field.fieldId().toString(), memberGradeLabel(field));
        }
        for (ClassReflectionGrader.PendingMethodResult method : classResult.methods()) {
            classSnapshot.methodGrades.put(method.methodId().toString(), memberGradeLabel(method));
        }
        for (ClassReflectionGrader.PendingConstructorResult constructor : classResult.constructors()) {
            classSnapshot.constructorGrades.put(constructor.constructorId().toString(), memberGradeLabel(constructor));
        }
    }

    private static String memberGradeLabel(ClassReflectionGrader.PendingFieldResult result) {
        return result.correct() ? "pass" : "fail";
    }

    private static String memberGradeLabel(ClassReflectionGrader.PendingMethodResult result) {
        return result.correct() ? "pass" : "fail";
    }

    private static String memberGradeLabel(ClassReflectionGrader.PendingConstructorResult result) {
        return result.correct() ? "pass" : "fail";
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

            for (FieldRubric expectedField : expectedClass.fields()) {
                ParsedField pf = CartesianAliasMatcher.findBestMmdField(
                        parsed.fields,
                        expectedField,
                        EnumDeclaringTypes.isEnum(expectedClass.declaringType()));
                if (pf != null) {
                    mmdSnapshot.attributes.put(expectedField.id().toString(), pf.name + ": " + pf.dataType);
                }
            }

            for (ConstructorRubric expectedConstructor : expectedClass.constructors()) {
                ParsedConstructor match = findMatchingConstructor(
                        parsed.constructors, expectedConstructor, true);
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
                ParsedMethod match = findMatchingMethod(parsed.methods, expectedMethod, true);
                if (match != null) {
                    String paramList = match.parameterTypes != null
                            ? String.join(", ", match.parameterTypes)
                            : "";
                    mmdSnapshot.attributes.put(
                            expectedMethod.id().toString(),
                            match.name + "(" + paramList + "): " + match.returnType);
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

    private ParsedMethod findMatchingMethod(
            List<ParsedMethod> candidates, MethodRubric expected, boolean mmd) {
        for (ParsedMethod pm : candidates) {
            boolean paramsMatch = mmd
                    ? CartesianAliasMatcher.mmdParametersMatch(
                            pm.parameterNames, pm.parameterTypes, expected.parameters())
                    : CartesianAliasMatcher.declarationParametersMatch(
                            pm.parameterNames, pm.parameterTypes, expected.parameters());
            if (CartesianAliasMatcher.nameMatches(pm.name, expected.names()) && paramsMatch) {
                return pm;
            }
        }
        return null;
    }

    private ParsedConstructor findMatchingConstructor(
            List<ParsedConstructor> candidates, ConstructorRubric expected, boolean mmd) {
        for (ParsedConstructor pc : candidates) {
            boolean matches = mmd
                    ? CartesianAliasMatcher.mmdParametersMatch(
                            pc.parameterNames, pc.parameterTypes, expected.parameters())
                    : CartesianAliasMatcher.declarationParametersMatch(
                            pc.parameterNames, pc.parameterTypes, expected.parameters());
            if (matches) {
                return pc;
            }
        }
        return null;
    }

    private String formatParamTypes(List<String> parameterTypes) {
        if (parameterTypes == null || parameterTypes.isEmpty()) {
            return "";
        }
        return String.join(", ", parameterTypes);
    }
}
