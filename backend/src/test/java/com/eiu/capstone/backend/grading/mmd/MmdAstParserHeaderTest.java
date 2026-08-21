package com.eiu.capstone.backend.grading.mmd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.grading.MmdParseException;
import com.eiu.capstone.backend.grading.mmd.ast.MmdDiagramAst;

class MmdAstParserHeaderTest {

    private final MmdAstParser parser = new MmdAstParser();

    @Test
    void acceptsValidClassDiagramHeader() {
        MmdDiagramAst ast = parser.parse("classDiagram\nclass Animal");

        assertTrue(ast.classDiagramDeclared());
        assertEquals(1, ast.nodes().size());
    }

    @Test
    void emptyDiagramReturnsEmptyAstWithoutHeader() {
        MmdDiagramAst ast = parser.parse("   \n%% only comments\n");

        assertFalse(ast.classDiagramDeclared());
        assertTrue(ast.nodes().isEmpty());
    }

    @Test
    void missingHeaderThrowsForSubstantiveContent() {
        MmdParseException ex = assertThrows(
                MmdParseException.class,
                () -> parser.parse("class Animal"));

        assertEquals("Missing classDiagram header", ex.getMessage());
    }

    @Test
    void missingHeaderThrowsForRelationOnlyDiagram() {
        assertThrows(
                MmdParseException.class,
                () -> parser.parse("Animal <|-- Dog"));
    }
}
