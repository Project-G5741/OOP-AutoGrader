package unit.com.eiu.capstone.backend.grading;

import com.eiu.capstone.backend.grading.*;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.grading.rubric.ChallengeRubric;
import com.eiu.capstone.backend.grading.rubric.ClassRubric;
import com.eiu.capstone.backend.grading.rubric.ConstructorRubric;
import com.eiu.capstone.backend.grading.rubric.FieldRubric;
import com.eiu.capstone.backend.grading.rubric.MethodRubric;
import com.eiu.capstone.backend.grading.rubric.ParameterRubric;
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
                        List.of(new FieldRubric(balanceId, "balance", "public", "double", false, false)),
                        List.of(),
                        List.of())),
                List.of(),
                List.of());

        String blockForm = diagram("""
                class BankAccount {
                  +balance: double
                }
                """);
        String colonForm = diagram("""
                class BankAccount
                BankAccount : +balance: double
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
                        List.of(new FieldRubric(membersId, "members", "private", "List~Member~", false, false)),
                        List.of(),
                        List.of())),
                List.of(),
                List.of());

        String student = diagram("""
                class Team {
                  -members: List<Member>
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
    void enumConstantWithBlankTypeAndScopeMatchesBareDiagramName() {
        UUID classId = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();
        UUID missingId = UUID.randomUUID();
        ChallengeRubric rubric = new ChallengeRubric(
                UUID.randomUUID(),
                1,
                "SessionType",
                List.of(new ClassRubric(
                        classId,
                        "SessionType",
                        "public",
                        "ENUM",
                        false,
                        List.of(
                                new FieldRubric(familyId, "FAMILY", null, null, false, false),
                                new FieldRubric(missingId, "MISSING", null, null, false, false)),
                        List.of(),
                        List.of())),
                List.of(),
                List.of());

        String mmd = diagram("""
                class SessionType {
                  <<enumeration>>
                  FAMILY
                  EVENT
                }
                """);

        MmdGradingOutcome outcome = comparisonService.compare(rubric, parser.parse(mmd));

        assertTrue(outcome.isFieldCorrect(familyId));
        assertFalse(outcome.isFieldCorrect(missingId));
    }

    @Test
    void enumConstantIgnoresStoredTypeAndScopeOnBareDiagramName() {
        UUID classId = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();
        ChallengeRubric rubric = new ChallengeRubric(
                UUID.randomUUID(),
                1,
                "SessionType",
                List.of(new ClassRubric(
                        classId,
                        "SessionType",
                        "public",
                        "ENUM",
                        false,
                        List.of(new FieldRubric(familyId, "FAMILY", "public", "SessionType", false, false)),
                        List.of(),
                        List.of())),
                List.of(),
                List.of());

        String mmd = diagram("""
                class SessionType {
                  <<enumeration>>
                  FAMILY
                }
                """);

        MmdGradingOutcome outcome = comparisonService.compare(rubric, parser.parse(mmd));

        assertTrue(outcome.isFieldCorrect(familyId));
    }

    @Test
    void enumStereotypeVariantsMatchEnumDeclaringTypeInRubric() {
        UUID classId = UUID.randomUUID();
        ChallengeRubric rubric = new ChallengeRubric(
                UUID.randomUUID(),
                1,
                "CakeType",
                List.of(new ClassRubric(
                        classId,
                        "CakeType",
                        "public",
                        "ENUM",
                        false,
                        List.of(),
                        List.of(),
                        List.of())),
                List.of(),
                List.of());

        for (String stereotype : List.of("enum", "enumerate", "enumeration")) {
            String mmd = diagram("""
                    class CakeType {
                      <<%s>>
                    }
                    """.formatted(stereotype));

            MmdGradingOutcome outcome = comparisonService.compare(rubric, parser.parse(mmd));

            assertTrue(outcome.isClassPresent(classId), stereotype);
            assertTrue(outcome.isClassCorrect(classId), stereotype);
        }
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
                  +draw()*: void
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

    @Test
    void staticFinalRubricFieldPassesMmdWhenDiagramShowsStaticOnly() {
        UUID classId = UUID.randomUUID();
        UUID firstFineId = UUID.randomUUID();
        UUID additionalFineId = UUID.randomUUID();
        ChallengeRubric rubric = new ChallengeRubric(
                UUID.randomUUID(),
                1,
                "Const",
                List.of(new ClassRubric(
                        classId,
                        "Const",
                        "public",
                        "CLASS",
                        false,
                        List.of(
                                new FieldRubric(firstFineId, "FIRST_HOUR_FINE", "public", "int", true, true),
                                new FieldRubric(additionalFineId, "ADDITIONAL_HOUR_FINE", "public", "int", true, true)),
                        List.of(),
                        List.of())),
                List.of(),
                List.of());

        String mmd = diagram("""
                class Const {
                  + FIRST_HOUR_FINE$ : int
                  + ADDITIONAL_HOUR_FINE$ : int
                }
                """);

        MmdGradingOutcome outcome = comparisonService.compare(rubric, parser.parse(mmd));

        assertTrue(outcome.isFieldCorrect(firstFineId));
        assertTrue(outcome.isFieldCorrect(additionalFineId));
    }

    @Test
    void fieldAndMethodAliases_useSameCartesianRulesAsDeclaration() {
        UUID classId = UUID.randomUUID();
        UUID fieldId = UUID.randomUUID();
        UUID methodId = UUID.randomUUID();
        ChallengeRubric rubric = new ChallengeRubric(
                UUID.randomUUID(),
                1,
                "Invoice",
                List.of(new ClassRubric(
                        classId,
                        "Invoice",
                        "public",
                        "CLASS",
                        false,
                        List.of(new FieldRubric(
                                fieldId,
                                List.of("cost", "fee"),
                                "private",
                                List.of("double", "float"),
                                false,
                                false)),
                        List.of(new MethodRubric(
                                methodId,
                                List.of("calculateTotal", "calcTotal"),
                                "public",
                                List.of("double", "float"),
                                false,
                                false,
                                false,
                                List.of(new ParameterRubric(
                                        List.of("cost", "fee"),
                                        List.of("double", "float"))))),
                        List.of())),
                List.of(),
                List.of());

        String mmd = diagram("""
                class Invoice {
                  -fee: float
                  +calcTotal(fee: float): float
                }
                """);

        MmdGradingOutcome outcome = comparisonService.compare(rubric, parser.parse(mmd));

        assertTrue(outcome.isFieldCorrect(fieldId));
        assertTrue(outcome.isMethodCorrect(methodId));
    }

    @Test
    void methodParameterNameMustMatchAliasAtSamePosition() {
        UUID methodId = UUID.randomUUID();
        ChallengeRubric rubric = new ChallengeRubric(
                UUID.randomUUID(),
                1,
                "Invoice",
                List.of(new ClassRubric(
                        UUID.randomUUID(),
                        "Invoice",
                        "public",
                        "CLASS",
                        false,
                        List.of(),
                        List.of(new MethodRubric(
                                methodId,
                                List.of("calculate"),
                                "public",
                                List.of("double"),
                                false,
                                false,
                                false,
                                List.of(new ParameterRubric(
                                        List.of("cost", "fee"),
                                        List.of("double", "float"))))),
                        List.of())),
                List.of(),
                List.of());

        MmdGradingOutcome outcome = comparisonService.compare(
                rubric,
                parser.parse(diagram("""
                        class Invoice {
                          +calculate(price: float): double
                        }
                        """)));

        assertFalse(outcome.isMethodCorrect(methodId));
    }

    @Test
    void constructorParameterNameMustMatchAliasAtSamePosition() {
        UUID constructorId = UUID.randomUUID();
        ChallengeRubric rubric = new ChallengeRubric(
                UUID.randomUUID(),
                1,
                "Invoice",
                List.of(new ClassRubric(
                        UUID.randomUUID(),
                        "Invoice",
                        "public",
                        "CLASS",
                        false,
                        List.of(),
                        List.of(),
                        List.of(new ConstructorRubric(
                                constructorId,
                                "public",
                                false,
                                List.of("double"),
                                List.of(new ParameterRubric(
                                        List.of("cost", "fee"),
                                        List.of("double", "float"))))))),
                List.of(),
                List.of());

        MmdGradingOutcome outcome = comparisonService.compare(
                rubric,
                parser.parse(diagram("""
                        class Invoice {
                          +Invoice(price: float)
                        }
                        """)));

        assertFalse(outcome.isConstructorCorrect(constructorId));
    }

    @Test
    void typeOnlyParameterRubricIgnoresDiagramParameterNames() {
        UUID methodId = UUID.randomUUID();
        UUID constructorId = UUID.randomUUID();
        ChallengeRubric rubric = new ChallengeRubric(
                UUID.randomUUID(),
                1,
                "Car",
                List.of(new ClassRubric(
                        UUID.randomUUID(),
                        "Car",
                        "public",
                        "CLASS",
                        false,
                        List.of(),
                        List.of(new MethodRubric(
                                methodId, "drive", "public", "void", false, false, false, List.of("int"))),
                        List.of(new ConstructorRubric(
                                constructorId, "public", false, List.of("int", "String"))))),
                List.of(),
                List.of());

        MmdGradingOutcome outcome = comparisonService.compare(
                rubric,
                parser.parse(diagram("""
                        class Car {
                          +Car(yearModel: int, make: String)
                          +drive(speed: int): void
                        }
                        """)));

        assertTrue(outcome.isConstructorCorrect(constructorId));
        assertTrue(outcome.isMethodCorrect(methodId));
    }

    @Test
    void fieldMatchingSkipsEarlierNameOnlyCandidateForCompleteMatch() {
        UUID fieldId = UUID.randomUUID();
        ChallengeRubric rubric = new ChallengeRubric(
                UUID.randomUUID(),
                1,
                "Invoice",
                List.of(new ClassRubric(
                        UUID.randomUUID(),
                        "Invoice",
                        "public",
                        "CLASS",
                        false,
                        List.of(new FieldRubric(
                                fieldId,
                                List.of("cost", "fee"),
                                "private",
                                List.of("double"),
                                false,
                                false)),
                        List.of(),
                        List.of())),
                List.of(),
                List.of());

        MmdGradingOutcome outcome = comparisonService.compare(
                rubric,
                parser.parse(diagram("""
                        class Invoice {
                          -cost: String
                          -fee: double
                        }
                        """)));

        assertTrue(outcome.isFieldCorrect(fieldId));
    }
}
