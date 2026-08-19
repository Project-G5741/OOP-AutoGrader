package com.eiu.capstone.backend.grading.mmd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.eiu.capstone.backend.grading.ParsedMmdClass;
import com.eiu.capstone.backend.grading.ParsedMmdDiagram;
import com.eiu.capstone.backend.grading.ParsedMmdRelation;

class MmdRelationParseTest {

    private final MmdAstParser astParser = new MmdAstParser();
    private final MmdAstToParsedMapper mapper = new MmdAstToParsedMapper();

    private ParsedMmdDiagram parse(String source) {
        return mapper.map(astParser.parse(source));
    }

    private static String diagram(String body) {
        String trimmed = body.stripLeading();
        if (trimmed.startsWith("classDiagram")) {
            return body;
        }
        return "classDiagram\n" + body;
    }

    @Test
    void implicitClassesFromInheritance() {
        ParsedMmdDiagram diagram = parse(diagram("Animal <|-- Dog"));

        assertEquals(2, diagram.classes.size());
        assertClassPresent(diagram, "Animal");
        assertClassPresent(diagram, "Dog");
        assertEquals(1, diagram.relations.size());
        ParsedMmdRelation relation = diagram.relations.get(0);
        assertEquals("inheritance", relation.relationType);
        assertEquals("Dog", relation.sourceClassName);
        assertEquals("Animal", relation.targetClassName);
    }

    @ParameterizedTest
    @MethodSource("referenceSection16Arrows")
    void parsesCanonicalArrowVariants(String arrow, String expectedType) {
        ParsedMmdDiagram diagram = parse(diagram("ClassA " + arrow + " ClassB"));

        assertEquals(1, diagram.relations.size(), "arrow: " + arrow);
        assertEquals(expectedType, diagram.relations.get(0).relationType);
    }

    private static Stream<Arguments> referenceSection16Arrows() {
        return Stream.of(
                Arguments.of("<|--", "inheritance"),
                Arguments.of("*--", "composition"),
                Arguments.of("o--", "aggregation"),
                Arguments.of("-->", "association"),
                Arguments.of("--", "link"),
                Arguments.of("..>", "dependency"),
                Arguments.of("..|>", "realization"),
                Arguments.of("..", "dashed_link"));
    }

    @Test
    void parsesCardinalityAndStripsLabel() {
        ParsedMmdDiagram diagram = parse(diagram("Company \"1\" --> \"1..*\" Employee : employs"));

        assertEquals(1, diagram.relations.size());
        ParsedMmdRelation relation = diagram.relations.get(0);
        assertEquals("association", relation.relationType);
        assertEquals("Company", relation.sourceClassName);
        assertEquals("Employee", relation.targetClassName);
        assertEquals("1", relation.sourceCardinality);
        assertEquals("1..*", relation.targetCardinality);
    }

    @Test
    void parsesLollipopInterfaceOnLeft() {
        ParsedMmdDiagram diagram = parse(diagram("Printable ()-- Report"));

        assertEquals(1, diagram.relations.size());
        ParsedMmdRelation relation = diagram.relations.get(0);
        assertEquals("realization", relation.relationType);
        assertEquals("Report", relation.sourceClassName);
        assertEquals("Printable", relation.targetClassName);
    }

    @Test
    void parsesLollipopInterfaceOnRight() {
        ParsedMmdDiagram diagram = parse(diagram("Report --() Printable"));

        assertEquals(1, diagram.relations.size());
        ParsedMmdRelation relation = diagram.relations.get(0);
        assertEquals("realization", relation.relationType);
        assertEquals("Report", relation.sourceClassName);
        assertEquals("Printable", relation.targetClassName);
    }

    @Test
    void parsesTwoWayInheritanceRelation() {
        ParsedMmdDiagram diagram = parse(diagram("Animal \"many\" <|--|> \"many\" Zebra"));

        assertEquals(2, diagram.classes.size());
        assertEquals(1, diagram.relations.size());
        ParsedMmdRelation relation = diagram.relations.get(0);
        assertEquals("bidirectional_inheritance", relation.relationType);
        assertEquals("many", relation.sourceCardinality);
        assertEquals("many", relation.targetCardinality);
        assertNotNull(relation.sourceClassName);
        assertNotNull(relation.targetClassName);
    }

    @Test
    void relationLineParserReturnsNullForClassDeclaration() {
        MmdRelationLineParser lineParser = new MmdRelationLineParser();

        assertNull(lineParser.parse("class Animal {", 1));
    }

    private static void assertClassPresent(ParsedMmdDiagram diagram, String name) {
        boolean found = diagram.classes.stream().map(c -> c.name).anyMatch(name::equals);
        if (!found) {
            throw new AssertionError("Expected class " + name);
        }
    }
}
