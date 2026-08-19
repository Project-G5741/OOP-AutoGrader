package com.eiu.capstone.backend.grading.mmd;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.eiu.capstone.backend.grading.MmdParseException;
import com.eiu.capstone.backend.grading.MmdParser;
import com.eiu.capstone.backend.grading.ParsedMmdDiagram;

/**
 * Reference-doc matrix for {@code grading-mermaid-oop-class-diagrams.md} §1.1–§1.12.
 */
class MmdReferenceDocMatrixTest {

    private final MmdParser parser = new MmdParser();

    private static String diagram(String body) {
        String trimmed = body.stripLeading();
        if (trimmed.startsWith("classDiagram")) {
            return body;
        }
        return "classDiagram\n" + body;
    }

    private ParsedMmdDiagram parse(String source) {
        return parser.parseBytes(source.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void section11_explicitAndImplicitClasses() {
        ParsedMmdDiagram explicit = parse(diagram("class Animal"));
        ParsedMmdDiagram implicit = parse(diagram("Animal <|-- Dog"));

        assertEquals(1, explicit.classes.size());
        assertEquals("Animal", explicit.classes.get(0).name);
        assertEquals(2, implicit.classes.size());
    }

    @Test
    void section12_classDisplayLabelUsesIdentifier() {
        ParsedMmdDiagram diagram = parse(diagram("class Person[\"Person Entity\"]"));
        assertEquals("Person", diagram.classes.get(0).name);
    }

    @Test
    void section13_colonSyntaxMatchesBlockMembers() {
        ParsedMmdDiagram block = parse(diagram("""
                class BankAccount {
                  +double balance
                }
                """));
        ParsedMmdDiagram colon = parse(diagram("""
                class BankAccount
                BankAccount : +double balance
                """));

        assertEquals(block.classes.get(0).fields.size(), colon.classes.get(0).fields.size());
        assertEquals(block.classes.get(0).fields.get(0).name, colon.classes.get(0).fields.get(0).name);
    }

    @Test
    void section13_missingSpaceBeforeReturnTypeFails() {
        assertThrows(MmdParseException.class, () -> parse(diagram("""
                class Account {
                  +getBalance()double
                }
                """)));
    }

    @ParameterizedTest
    @MethodSource("visibilitySymbols")
    void section14_visibilitySymbols(char symbol, String scope) {
        ParsedMmdDiagram diagram = parse(diagram("""
                class Demo {
                  %cint count
                }
                """.formatted(symbol)));
        assertEquals(scope, diagram.classes.get(0).fields.get(0).scope);
    }

    private static Stream<Arguments> visibilitySymbols() {
        return Stream.of(
                Arguments.of('+', "public"),
                Arguments.of('-', "private"),
                Arguments.of('#', "protected"),
                Arguments.of('~', "package"));
    }

    @Test
    void section15_staticAndAbstractMarkers() {
        ParsedMmdDiagram diagram = parse(diagram("""
                class Example {
                  +instance$
                  +run()* void
                }
                """));
        assertEquals("instance", diagram.classes.get(0).fields.get(0).name);
        assertTrue(diagram.classes.get(0).methods.get(0).isAbstract);
    }

    @ParameterizedTest
    @MethodSource("referenceArrows")
    void section16_relationshipArrows(String line, String expectedType) {
        ParsedMmdDiagram diagram = parse(diagram(line));
        assertEquals(1, diagram.relations.size());
        assertEquals(expectedType, diagram.relations.get(0).relationType);
    }

    private static Stream<Arguments> referenceArrows() {
        return Stream.of(
                Arguments.of("A <|-- B", "inheritance"),
                Arguments.of("A *-- B", "composition"),
                Arguments.of("A o-- B", "aggregation"),
                Arguments.of("A --> B", "association"),
                Arguments.of("A -- B", "link"),
                Arguments.of("A ..> B", "dependency"),
                Arguments.of("A ..|> B", "realization"),
                Arguments.of("A .. B", "dashed_link"));
    }

    @Test
    void section17_stereotypesInBlockAndStandalone() {
        ParsedMmdDiagram block = parse(diagram("""
                class Shape {
                  <<abstract>>
                }
                """));
        ParsedMmdDiagram standalone = parse(diagram("""
                <<Interface>> Coffee
                """));

        assertEquals("Abstract", block.classes.get(0).stereotypeType);
        assertEquals("Interface", standalone.classes.get(0).stereotypeType);
    }

    @Test
    void section18_cardinalityParsed() {
        ParsedMmdDiagram diagram = parse(diagram("Company \"1\" --> \"1..*\" Employee : employs"));
        assertEquals("1", diagram.relations.get(0).sourceCardinality);
        assertEquals("1..*", diagram.relations.get(0).targetCardinality);
    }

    @Test
    void section19_twoWayInheritance() {
        assertDoesNotThrow(() -> parse(diagram("Animal \"many\" <|--|> \"many\" Zebra")));
    }

    @Test
    void section110_lollipopRealization() {
        ParsedMmdDiagram diagram = parse(diagram("Report --() Printable"));
        assertEquals("realization", diagram.relations.get(0).relationType);
    }

    @Test
    void section111_namespaceRegistersClasses() {
        ParsedMmdDiagram diagram = parse(diagram("""
                namespace Company {
                  class Employee
                }
                """));
        assertEquals(1, diagram.classes.size());
        assertEquals("Employee", diagram.classes.get(0).name);
    }

    @Test
    void section112_notesDirectionAndStyleIgnored() {
        assertDoesNotThrow(() -> parse(diagram("""
                class Animal
                note for Animal "mammal"
                direction TB
                style Animal fill:#f9f
                classDef important fill:#f96
                cssClass Animal important
                """)));
    }

    @Test
    void failure_missingHeaderOnSubstantiveContent() {
        assertThrows(MmdParseException.class, () -> parse("class Animal"));
    }
}
