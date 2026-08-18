package com.eiu.capstone.backend.grading;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.grading.rubric.ChallengeRubric;
import com.eiu.capstone.backend.grading.rubric.ClassRubric;
import com.eiu.capstone.backend.grading.rubric.FieldRubric;
import com.eiu.capstone.backend.grading.rubric.RelationRubric;

class MmdComparisonServiceTest {

    private final MmdParser parser = new MmdParser();
    private final MmdComparisonService comparisonService = new MmdComparisonService();

    private static String diagram(String body) {
        String trimmed = body.stripLeading();
        if (trimmed.startsWith("classDiagram")) {
            return body;
        }
        return "classDiagram\n" + body;
    }

    @Test
    void ae1_implicitClassesViaInheritanceGradeRelationCorrect() {
        String mmd = diagram("Animal <|-- Dog");

        UUID animalId = UUID.randomUUID();
        UUID dogId = UUID.randomUUID();
        UUID relationId = UUID.randomUUID();
        ChallengeRubric rubric = new ChallengeRubric(
                UUID.randomUUID(),
                1,
                "Inheritance",
                List.of(
                        new ClassRubric(animalId, "Animal", "public", "CLASS", false, List.of(), List.of(), List.of()),
                        new ClassRubric(dogId, "Dog", "public", "CLASS", false, List.of(), List.of(), List.of())),
                List.of(new RelationRubric(
                        relationId, dogId, "Dog", animalId, "Animal", "inheritance")),
                List.of());

        MmdGradingOutcome outcome = comparisonService.compare(rubric, parser.parse(mmd));

        assertTrue(outcome.isClassPresent(animalId));
        assertTrue(outcome.isClassPresent(dogId));
        assertTrue(outcome.isRelationCorrect(relationId));
    }

    @Test
    void ae2_colonSyntaxMemberGradesSameAsBlockForm() {
        UUID balanceId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        ChallengeRubric rubric = new ChallengeRubric(
                UUID.randomUUID(),
                1,
                "BankAccount",
                List.of(new ClassRubric(
                        classId,
                        "BankAccount",
                        "public",
                        "CLASS",
                        false,
                        List.of(new FieldRubric(balanceId, "balance", "public", "double")),
                        List.of(),
                        List.of())),
                List.of(),
                List.of());

        String blockForm = diagram("""
                class BankAccount {
                  +double balance
                }
                """);
        String colonForm = diagram("""
                class BankAccount
                BankAccount : +double balance
                """);

        assertTrue(comparisonService.compare(rubric, parser.parse(blockForm)).isFieldCorrect(balanceId));
        assertTrue(comparisonService.compare(rubric, parser.parse(colonForm)).isFieldCorrect(balanceId));
    }

    @Test
    void ae4_angleBracketGenericsMatchTildeGenericsInRubric() {
        UUID membersId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        ChallengeRubric rubric = new ChallengeRubric(
                UUID.randomUUID(),
                1,
                "Team",
                List.of(new ClassRubric(
                        classId,
                        "Team",
                        "public",
                        "CLASS",
                        false,
                        List.of(new FieldRubric(membersId, "members", "private", "List~Member~")),
                        List.of(),
                        List.of())),
                List.of(),
                List.of());

        String student = diagram("""
                class Team {
                  -List<Member> members
                }
                """);

        assertTrue(comparisonService.compare(rubric, parser.parse(student)).isFieldCorrect(membersId));
    }

    @Test
    void ae5_realizationDirectionMatchesBothArrowForms() {
        UUID reportId = UUID.randomUUID();
        UUID printableId = UUID.randomUUID();
        UUID relationId = UUID.randomUUID();
        ChallengeRubric rubric = new ChallengeRubric(
                UUID.randomUUID(),
                1,
                "Report",
                List.of(
                        new ClassRubric(printableId, "Printable", "public", "INTERFACE", false, List.of(), List.of(), List.of()),
                        new ClassRubric(reportId, "Report", "public", "CLASS", false, List.of(), List.of(), List.of())),
                List.of(new RelationRubric(
                        relationId, reportId, "Report", printableId, "Printable", "Realization")),
                List.of());

        String arrowToInterface = diagram("Report ..|> Printable");
        String triangleOnInterface = diagram("Printable <|.. Report");

        assertTrue(comparisonService.compare(rubric, parser.parse(arrowToInterface)).isRelationCorrect(relationId));
        assertTrue(comparisonService.compare(rubric, parser.parse(triangleOnInterface)).isRelationCorrect(relationId));
    }

    @Test
    void abstractStereotypeMatchesClassDeclaringTypeInRubric() {
        UUID drawId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        ChallengeRubric rubric = new ChallengeRubric(
                UUID.randomUUID(),
                1,
                "Shape",
                List.of(new ClassRubric(
                        classId,
                        "Shape",
                        "public",
                        "CLASS",
                        false,
                        List.of(),
                        List.of(),
                        List.of())),
                List.of(),
                List.of());

        String mmd = diagram("""
                class Shape {
                  <<abstract>>
                  +draw()* void
                }
                """);

        MmdGradingOutcome outcome = comparisonService.compare(rubric, parser.parse(mmd));

        assertTrue(outcome.isClassPresent(classId));
        assertTrue(outcome.isClassCorrect(classId));
    }

    @Test
    void namespaceQualifiedRubricNameResolvesToSimpleDiagramClass() {
        UUID employeeId = UUID.randomUUID();
        ChallengeRubric rubric = new ChallengeRubric(
                UUID.randomUUID(),
                1,
                "Company",
                List.of(new ClassRubric(
                        employeeId,
                        "Company.Employee",
                        "public",
                        "CLASS",
                        false,
                        List.of(),
                        List.of(),
                        List.of())),
                List.of(),
                List.of());

        String mmd = diagram("""
                namespace Company {
                  class Employee
                }
                """);

        MmdGradingOutcome outcome = comparisonService.compare(rubric, parser.parse(mmd));

        assertTrue(outcome.isClassPresent(employeeId));
    }
}
