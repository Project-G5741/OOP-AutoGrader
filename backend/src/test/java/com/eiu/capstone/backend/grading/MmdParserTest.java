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

  private static String withHeader(String body) {
    String trimmed = body.stripLeading();
    if (trimmed.startsWith("classDiagram")) {
      return body;
    }
    return "classDiagram\n" + body;
  }

  @Test
  void parsesMermaidStyleFieldsTypeThenName() {
    String mmd = withHeader("""
        class Car {
          -int yearModel
          -String make
          -int speed
        }
        """);

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
    String mmd = withHeader("""
        class Car {
          -int yearModel
          -String make
          -int speed
        }
        """);

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
    String mmd = withHeader("""
        class Car {
          +Car(yearModel: int, make: String)
        }
        """);

    ParsedMmdDiagram diagram = parser.parse(mmd);
    ParsedMmdClass car = diagram.classes.get(0);

    assertEquals(1, car.constructors.size());
    assertEquals("public", car.constructors.get(0).scope);
    assertEquals(List.of("int", "String"), car.constructors.get(0).parameterTypes);
  }

  @Test
  void gradesConstructorWithNameColonTypeParameters() {
    String mmd = withHeader("""
        class Car {
          +Car(yearModel: int, make: String)
        }
        """);

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

  @Test
  void parsesMermaidStaticAndAbstractMethodModifiers() {
    String mmd = withHeader("""
        class Logger {
          -static Logger instance$
          +getInstance() Logger$
          +log(message String) void
        }
        """);

    ParsedMmdDiagram diagram = parser.parse(mmd);
    ParsedMmdClass logger = diagram.classes.get(0);

    assertEquals("instance", logger.fields.get(0).name);
    assertEquals("Logger", logger.fields.get(0).dataType);

    ParsedMethod getInstance = logger.methods.stream()
        .filter(method -> "getInstance".equals(method.name))
        .findFirst()
        .orElseThrow();
    assertEquals("Logger", getInstance.returnType);
    assertTrue(getInstance.isStatic);

    ParsedMethod log = logger.methods.stream()
        .filter(method -> "log".equals(method.name))
        .findFirst()
        .orElseThrow();
    assertEquals(List.of("String"), log.parameterTypes);
  }

  @Test
  void parsesMethodParametersWithNameColonType() {
    String mmd = withHeader("""
        class Logger {
          +log(message: String) void
        }
        """);

    ParsedMmdDiagram diagram = parser.parse(mmd);
    ParsedMethod log = diagram.classes.get(0).methods.get(0);

    assertEquals(List.of("String"), log.parameterTypes);
  }

  @Test
  void gradesLoggerSingletonDiagramAgainstRubric() {
    String mmd = withHeader("""
        classDiagram
            class Logger {
                -static Logger instance
                -Logger()
                +getInstance() Logger$
                +log(message: String) void
            }
        """);

    var instanceId = java.util.UUID.randomUUID();
    var ctorId = java.util.UUID.randomUUID();
    var getInstanceId = java.util.UUID.randomUUID();
    var logId = java.util.UUID.randomUUID();
    var classId = java.util.UUID.randomUUID();
    ChallengeRubric rubric = new ChallengeRubric(
        java.util.UUID.randomUUID(),
        1,
        "Logger",
        java.util.List.of(new ClassRubric(
            classId,
            "Logger",
            "public",
            "CLASS",
            false,
            java.util.List.of(new FieldRubric(instanceId, "instance", "private", "Logger")),
            java.util.List.of(
                new com.eiu.capstone.backend.grading.rubric.MethodRubric(
                    getInstanceId, "getInstance", "public", "Logger", true, false, false, java.util.List.of()),
                new com.eiu.capstone.backend.grading.rubric.MethodRubric(
                    logId, "log", "public", "void", false, false, false, java.util.List.of("String"))),
            java.util.List.of(new com.eiu.capstone.backend.grading.rubric.ConstructorRubric(
                ctorId, "private", false, java.util.List.of())))),
        java.util.List.of(),
        java.util.List.of());

    MmdGradingOutcome outcome = comparisonService.compare(rubric, parser.parse(mmd));

    assertTrue(outcome.isFieldCorrect(instanceId));
    assertTrue(outcome.isMethodCorrect(getInstanceId));
    assertTrue(outcome.isMethodCorrect(logId));
    assertTrue(outcome.isConstructorCorrect(ctorId));
  }

  @Test
  void gradesStaticMarkerWhenRubricDoesNotRequireStatic() {
    String mmd = withHeader("""
        class Logger {
          +getInstance() Logger$
        }
        """);

    var getInstanceId = java.util.UUID.randomUUID();
    var classId = java.util.UUID.randomUUID();
    ChallengeRubric rubric = new ChallengeRubric(
        java.util.UUID.randomUUID(),
        1,
        "Logger",
        java.util.List.of(new ClassRubric(
            classId,
            "Logger",
            "public",
            "CLASS",
            false,
            java.util.List.of(),
            java.util.List.of(new com.eiu.capstone.backend.grading.rubric.MethodRubric(
                getInstanceId, "getInstance", "public", "Logger", false, false, false, java.util.List.of())),
            java.util.List.of())),
        java.util.List.of(),
        java.util.List.of());

    MmdGradingOutcome outcome = comparisonService.compare(rubric, parser.parse(mmd));

    assertTrue(outcome.isMethodCorrect(getInstanceId));
  }

  @Test
  void parsesCanonicalMermaidStaticMarkerAfterParentheses() {
    String mmd = withHeader("""
        class Logger {
          +getInstance()$ Logger
        }
        """);

    ParsedMethod getInstance = parser.parse(mmd).classes.get(0).methods.get(0);

    assertEquals("getInstance", getInstance.name);
    assertEquals("Logger", getInstance.returnType);
    assertTrue(getInstance.isStatic);
  }

  @Test
  void gradesInterfaceMethodsWithoutExplicitAbstractMarker() {
    String mmd = withHeader("""
        class Coffee {
          <<interface>>
          +getCost() double
          +getDescription() String
        }
        """);

    var getCostId = java.util.UUID.randomUUID();
    var getDescriptionId = java.util.UUID.randomUUID();
    var classId = java.util.UUID.randomUUID();
    ChallengeRubric rubric = new ChallengeRubric(
        java.util.UUID.randomUUID(),
        1,
        "Decorator",
        java.util.List.of(new ClassRubric(
            classId,
            "Coffee",
            "public",
            "INTERFACE",
            false,
            java.util.List.of(),
            java.util.List.of(
                new com.eiu.capstone.backend.grading.rubric.MethodRubric(
                    getCostId, "getCost", "public", "double", false, true, false, java.util.List.of()),
                new com.eiu.capstone.backend.grading.rubric.MethodRubric(
                    getDescriptionId, "getDescription", "public", "String", false, true, false, java.util.List.of())),
            java.util.List.of())),
        java.util.List.of(),
        java.util.List.of());

    MmdGradingOutcome outcome = comparisonService.compare(rubric, parser.parse(mmd));

    assertTrue(outcome.isMethodCorrect(getCostId));
    assertTrue(outcome.isMethodCorrect(getDescriptionId));
  }

  @Test
  void parsesAggregationArrowWithRelationLabel() {
    String mmd = withHeader("""
        BaseDecorator o--> Coffee : wraps
        """);

    ParsedMmdDiagram diagram = parser.parse(mmd);

    assertEquals(1, diagram.relations.size());
    ParsedMmdRelation relation = diagram.relations.get(0);
    assertEquals("aggregation", relation.relationType);
    assertEquals("BaseDecorator", relation.sourceClassName);
    assertEquals("Coffee", relation.targetClassName);
  }

  @Test
  void gradesAggregationRelationWithLabelAgainstRubric() {
    String mmd = withHeader("""
        BaseDecorator o--> Coffee : wraps
        """);

    var relationId = java.util.UUID.randomUUID();
    var baseId = java.util.UUID.randomUUID();
    var coffeeId = java.util.UUID.randomUUID();
    ChallengeRubric rubric = new ChallengeRubric(
        java.util.UUID.randomUUID(),
        1,
        "Decorator",
        java.util.List.of(
            new ClassRubric(baseId, "BaseDecorator", "public", "CLASS", true, java.util.List.of(), java.util.List.of(), java.util.List.of()),
            new ClassRubric(coffeeId, "Coffee", "public", "INTERFACE", false, java.util.List.of(), java.util.List.of(), java.util.List.of())),
        java.util.List.of(new com.eiu.capstone.backend.grading.rubric.RelationRubric(
            relationId, baseId, "BaseDecorator", coffeeId, "Coffee", "aggregation")),
        java.util.List.of());

    MmdGradingOutcome outcome = comparisonService.compare(rubric, parser.parse(mmd));

    assertTrue(outcome.isRelationCorrect(relationId));
  }

  @Test
  void parsesDependencyRelationWithLabel() {
    String mmd = withHeader("""
        Main ..> Logger : uses
        """);

    ParsedMmdDiagram diagram = parser.parse(mmd);

    assertEquals(1, diagram.relations.size());
    ParsedMmdRelation relation = diagram.relations.get(0);
    assertEquals("dependency", relation.relationType);
    assertEquals("Main", relation.sourceClassName);
    assertEquals("Logger", relation.targetClassName);
  }

  @Test
  void parsesRealizationArrowsAsEquivalentImplementorToInterface() {
    ParsedMmdRelation triangleOnInterface = parser.parse(withHeader("Coffee <|.. SimpleCoffee")).relations.get(0);
    ParsedMmdRelation arrowToInterface = parser.parse(withHeader("SimpleCoffee ..|> Coffee")).relations.get(0);

    assertEquals("realization", triangleOnInterface.relationType);
    assertEquals("realization", arrowToInterface.relationType);
    assertEquals("SimpleCoffee", triangleOnInterface.sourceClassName);
    assertEquals("Coffee", triangleOnInterface.targetClassName);
    assertEquals(triangleOnInterface.sourceClassName, arrowToInterface.sourceClassName);
    assertEquals(triangleOnInterface.targetClassName, arrowToInterface.targetClassName);
  }

  @Test
  void gradesImplementationAndRealizationRubricTypesAsEquivalent() {
    String mmd = withHeader("SimpleCoffee ..|> Coffee");

    var relationId = java.util.UUID.randomUUID();
    var coffeeId = java.util.UUID.randomUUID();
    var simpleCoffeeId = java.util.UUID.randomUUID();
    ChallengeRubric implementationRubric = new ChallengeRubric(
        java.util.UUID.randomUUID(),
        1,
        "Decorator",
        java.util.List.of(
            new ClassRubric(coffeeId, "Coffee", "public", "INTERFACE", false, java.util.List.of(), java.util.List.of(), java.util.List.of()),
            new ClassRubric(simpleCoffeeId, "SimpleCoffee", "public", "CLASS", false, java.util.List.of(), java.util.List.of(), java.util.List.of())),
        java.util.List.of(new com.eiu.capstone.backend.grading.rubric.RelationRubric(
            relationId, simpleCoffeeId, "SimpleCoffee", coffeeId, "Coffee", "Implementation")),
        java.util.List.of());

    ChallengeRubric realizationRubric = new ChallengeRubric(
        java.util.UUID.randomUUID(),
        1,
        "Decorator",
        java.util.List.of(
            new ClassRubric(coffeeId, "Coffee", "public", "INTERFACE", false, java.util.List.of(), java.util.List.of(), java.util.List.of()),
            new ClassRubric(simpleCoffeeId, "SimpleCoffee", "public", "CLASS", false, java.util.List.of(), java.util.List.of(), java.util.List.of())),
        java.util.List.of(new com.eiu.capstone.backend.grading.rubric.RelationRubric(
            relationId, simpleCoffeeId, "SimpleCoffee", coffeeId, "Coffee", "Realization")),
        java.util.List.of());

    ParsedMmdDiagram diagram = parser.parse(mmd);

    assertTrue(comparisonService.compare(implementationRubric, diagram).isRelationCorrect(relationId));
    assertTrue(comparisonService.compare(realizationRubric, diagram).isRelationCorrect(relationId));
  }
}
