package com.eiu.capstone.backend.grading.mmd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.grading.ParsedMmdDiagram;
import com.eiu.capstone.backend.grading.mmd.ast.MmdDiagramAst;
import com.eiu.capstone.backend.grading.mmd.ast.MmdIgnoredDirectiveNode;
import com.eiu.capstone.backend.grading.mmd.ast.MmdNamespaceNode;
import com.eiu.capstone.backend.grading.mmd.ast.MmdTopLevelNode;

class MmdMiscDirectiveTest {

    private final MmdAstParser astParser = new MmdAstParser();
    private final MmdAstToParsedMapper mapper = new MmdAstToParsedMapper();

    private static String diagram(String body) {
        String trimmed = body.stripLeading();
        if (trimmed.startsWith("classDiagram")) {
            return body;
        }
        return "classDiagram\n" + body;
    }

    private ParsedMmdDiagram parseDiagram(String source) {
        return mapper.map(astParser.parse(source));
    }

    @Test
    void namespaceBlockRegistersContainedClasses() {
        ParsedMmdDiagram diagram = parseDiagram(diagram("""
                namespace Company {
                  class Employee
                  class Manager
                }
                """));

        assertEquals(2, diagram.classes.size());
        assertClassPresent(diagram, "Employee");
        assertClassPresent(diagram, "Manager");
    }

    @Test
    void namespaceBlockProducesNamespaceAstNode() {
        MmdDiagramAst ast = astParser.parse(diagram("""
                namespace Company {
                  class Employee
                }
                """));

        assertEquals(1, ast.nodes().size());
        MmdNamespaceNode namespace = (MmdNamespaceNode) ast.nodes().get(0);
        assertEquals("Company", namespace.name());
        assertEquals(1, namespace.children().size());
    }

    @Test
    void noteAndDirectionLinesAreIgnoredForGrading() {
        MmdDiagramAst ast = astParser.parse(diagram("""
                class Animal
                note for Animal "mammal"
                note "general note"
                direction TB
                """));

        List<MmdTopLevelNode> ignored = ast.nodes().stream()
                .filter(MmdIgnoredDirectiveNode.class::isInstance)
                .toList();
        assertEquals(3, ignored.size());
        assertEquals(1, substantiveNodeCount(ast));
        assertEquals(1, parseDiagram(diagram("""
                class Animal
                note for Animal "mammal"
                note "general note"
                direction TB
                """)).classes.size());
    }

    @Test
    void styleAndClassDefLinesAreIgnoredForGrading() {
        MmdDiagramAst ast = astParser.parse(diagram("""
                class Animal
                style Animal fill:#f9f
                classDef important fill:#f96
                cssClass Animal important
                """));

        List<MmdIgnoredDirectiveNode> ignored = ast.nodes().stream()
                .filter(MmdIgnoredDirectiveNode.class::isInstance)
                .map(MmdIgnoredDirectiveNode.class::cast)
                .toList();
        assertEquals(3, ignored.size());
        assertTrue(ignored.stream().anyMatch(node -> node.rawLine().startsWith("style ")));
        assertTrue(ignored.stream().anyMatch(node -> node.rawLine().startsWith("classDef ")));
        assertTrue(ignored.stream().anyMatch(node -> node.rawLine().startsWith("cssClass ")));
        assertEquals(1, substantiveNodeCount(ast));
    }

    @Test
    void nestedNamespaceRegistersInnerClassesWithoutDuplication() {
        ParsedMmdDiagram diagram = parseDiagram(diagram("""
                namespace Company {
                  namespace HR {
                    class Employee
                  }
                }
                """));

        assertEquals(1, diagram.classes.size());
        assertClassPresent(diagram, "Employee");

        MmdNamespaceNode company = (MmdNamespaceNode) astParser.parse(diagram("""
                namespace Company {
                  namespace HR {
                    class Employee
                  }
                }
                """)).nodes().get(0);
        MmdNamespaceNode hr = (MmdNamespaceNode) company.children().get(0);
        assertEquals("HR", hr.name());
    }

    private static long substantiveNodeCount(MmdDiagramAst ast) {
        return ast.nodes().stream().filter(node -> !(node instanceof MmdIgnoredDirectiveNode)).count();
    }

    private static void assertClassPresent(ParsedMmdDiagram diagram, String name) {
        boolean found = diagram.classes.stream().anyMatch(clazz -> name.equals(clazz.name));
        assertTrue(found, "Expected class " + name);
        assertNotNull(diagram.classes.stream().filter(clazz -> name.equals(clazz.name)).findFirst().orElse(null));
    }
}
