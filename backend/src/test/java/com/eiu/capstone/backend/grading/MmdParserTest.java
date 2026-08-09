package com.eiu.capstone.backend.grading;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.grading.rubric.ChallengeRubric;
import com.eiu.capstone.backend.grading.rubric.ClassRubric;
import com.eiu.capstone.backend.grading.rubric.FieldRubric;

class MmdParserTest {

  private final MmdParser parser = new MmdParser();
  private final MmdComparisonService comparisonService = new MmdComparisonService();

  @Test
  void parsesMermaidStyleFieldsTypeThenName() {
    String mmd = """
        class Car {
          -int yearModel
          -String make
          -int speed
        }
        """;

    ParsedMmdDiagram diagram = parser.parse(mmd);
    ParsedMmdClass car = diagram.classes.get(0);

    assertEquals(3, car.fields.size());
    assertEquals("yearModel", car.fields.get(0).name);
    assertEquals("int", car.fields.get(0).dataType);
    assertEquals("private", car.fields.get(0).scope);
    assertEquals("make", car.fields.get(1).name);
    assertEquals("String", car.fields.get(1).dataType);
    assertEquals("speed", car.fields.get(2).name);
  }

  @Test
  void gradesMermaidStylePrivateFieldsAgainstRubric() {
    String mmd = """
        class Car {
          -int yearModel
          -String make
          -int speed
        }
        """;

    var yearModelId = java.util.UUID.randomUUID();
    var makeId = java.util.UUID.randomUUID();
    var speedId = java.util.UUID.randomUUID();
    var classId = java.util.UUID.randomUUID();
    ChallengeRubric rubric = new ChallengeRubric(
        java.util.UUID.randomUUID(),
        1,
        "Car",
        java.util.List.of(new ClassRubric(
            classId,
            "Car",
            "public",
            "CLASS",
            false,
            java.util.List.of(
                new FieldRubric(yearModelId, "yearModel", "private", "int"),
                new FieldRubric(makeId, "make", "private", "String"),
                new FieldRubric(speedId, "speed", "private", "int")),
            java.util.List.of(),
            java.util.List.of())),
        java.util.List.of(),
        java.util.List.of());

    MmdGradingOutcome outcome = comparisonService.compare(rubric, parser.parse(mmd));

    assertTrue(outcome.isFieldCorrect(yearModelId));
    assertTrue(outcome.isFieldCorrect(makeId));
    assertTrue(outcome.isFieldCorrect(speedId));
    assertTrue(outcome.isClassPresent(classId));
  }

  @Test
  void parsesConstructorParametersNameColonType() {
    String mmd = """
        class Car {
          +Car(yearModel: int, make: String)
        }
        """;

    ParsedMmdDiagram diagram = parser.parse(mmd);
    ParsedMmdClass car = diagram.classes.get(0);

    assertEquals(1, car.constructors.size());
    assertEquals("public", car.constructors.get(0).scope);
    assertEquals(List.of("int", "String"), car.constructors.get(0).parameterTypes);
  }

  @Test
  void gradesConstructorWithNameColonTypeParameters() {
    String mmd = """
        class Car {
          +Car(yearModel: int, make: String)
        }
        """;

    var ctorId = java.util.UUID.randomUUID();
    var classId = java.util.UUID.randomUUID();
    ChallengeRubric rubric = new ChallengeRubric(
        java.util.UUID.randomUUID(),
        1,
        "Car",
        java.util.List.of(new ClassRubric(
            classId,
            "Car",
            "public",
            "CLASS",
            false,
            java.util.List.of(),
            java.util.List.of(),
            java.util.List.of(new com.eiu.capstone.backend.grading.rubric.ConstructorRubric(
                ctorId, "public", false, java.util.List.of("int", "String"))))),
        java.util.List.of(),
        java.util.List.of());

    MmdGradingOutcome outcome = comparisonService.compare(rubric, parser.parse(mmd));

    assertTrue(outcome.isConstructorCorrect(ctorId));
    assertTrue(outcome.isClassPresent(classId));
  }
}
