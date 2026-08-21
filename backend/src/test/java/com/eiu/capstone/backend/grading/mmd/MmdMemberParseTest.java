package com.eiu.capstone.backend.grading.mmd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.eiu.capstone.backend.grading.MmdParseException;
import com.eiu.capstone.backend.grading.MmdTypeEquivalence;
import com.eiu.capstone.backend.grading.ParsedField;
import com.eiu.capstone.backend.grading.ParsedMethod;
import com.eiu.capstone.backend.grading.ParsedMmdClass;
import com.eiu.capstone.backend.grading.ParsedMmdDiagram;

class MmdMemberParseTest {

    private final MmdAstParser astParser = new MmdAstParser();
    private final MmdAstToParsedMapper mapper = new MmdAstToParsedMapper();

    private static String diagram(String body) {
        String trimmed = body.stripLeading();
        if (trimmed.startsWith("classDiagram")) {
            return body;
        }
        return "classDiagram\n" + body;
    }

    private ParsedMmdDiagram parse(String source) {
        return mapper.map(astParser.parse(source));
    }

    @Test
    void blockAndColonSyntaxProduceIdenticalMembers() {
        ParsedMmdClass blockClass = classNamed(parse(diagram("""
                class BankAccount {
                  +double balance
                  +deposit(amount) void
                }
                """)), "BankAccount");
        ParsedMmdClass colonClass = classNamed(parse(diagram("""
                class BankAccount
                BankAccount : +double balance
                BankAccount : +deposit(amount) void
                """)), "BankAccount");

        assertMembersEqual(blockClass, colonClass);
    }

    @Test
    void classDisplayLabelUsesIdentifierForMatching() {
        ParsedMmdClass person = classNamed(
                parse(diagram("class Person[\"Person Entity\"]")),
                "Person");

        assertEquals("Person", person.name);
    }

    @ParameterizedTest
    @MethodSource("visibilityScopes")
    void mapsVisibilitySymbolsToScopes(char symbol, String expectedScope) {
        ParsedMmdClass parsed = classNamed(parse(diagram("""
                class Demo {
                  %cint count
                }
                """.formatted(symbol))), "Demo");

        assertEquals(1, parsed.fields.size());
        assertEquals(expectedScope, parsed.fields.get(0).scope);
        assertEquals("count", parsed.fields.get(0).name);
    }

    private static Stream<Arguments> visibilityScopes() {
        return Stream.of(
                Arguments.of('+', "public"),
                Arguments.of('-', "private"),
                Arguments.of('#', "protected"),
                Arguments.of('~', "package"));
    }

    @Test
    void rejectsMissingSpaceBeforeReturnType() {
        MmdParseException ex = assertThrows(
                MmdParseException.class,
                () -> parse(diagram("""
                        class Account {
                          +getBalance()double
                        }
                        """)));

        assertEquals("Missing space before return type: +getBalance()double", ex.getMessage());
    }

    @Test
    void tildeAndAngleBracketGenericsNormalizeEquivalently() {
        ParsedMmdClass tilde = classNamed(parse(diagram("""
                class Team {
                  -List~Employee~ members
                }
                """)), "Team");
        ParsedMmdClass angles = classNamed(parse(diagram("""
                class Team {
                  -List<Employee> members
                }
                """)), "Team");

        assertTrue(MmdTypeEquivalence.typesMatch(
                tilde.fields.get(0).dataType, angles.fields.get(0).dataType));
        assertEquals("List~Employee~", tilde.fields.get(0).dataType);
        assertEquals("List<Employee>", angles.fields.get(0).dataType);
    }

    @Test
    void standaloneStereotypeSetsClassType() {
        ParsedMmdClass coffee = classNamed(parse(diagram("""
                <<Interface>> Coffee
                Coffee : +getCost() double
                """)), "Coffee");

        assertEquals("Interface", coffee.stereotypeType);
        assertEquals(1, coffee.methods.size());
    }

    @Test
    void enumStereotypeVariantsSetEnumerationType() {
        for (String stereotype : List.of("enum", "enumerate", "enumeration")) {
            ParsedMmdClass cakeType = classNamed(parse(diagram("""
                    class CakeType {
                      <<%s>>
                    }
                    """.formatted(stereotype))), "CakeType");

            assertEquals("Enumeration", cakeType.stereotypeType, stereotype);
        }
    }

    @Test
    void inBlockStereotypeSetsClassType() {
        ParsedMmdClass shape = classNamed(parse(diagram("""
                class Shape {
                  <<abstract>>
                  +draw()* void
                }
                """)), "Shape");

        assertEquals("Abstract", shape.stereotypeType);
        ParsedMethod draw = shape.methods.get(0);
        assertEquals("draw", draw.name);
        assertTrue(draw.isAbstract);
        assertEquals("void", draw.returnType);
    }

    @Test
    void parsesStaticFieldAndAbstractMethodMarkers() {
        ParsedMmdClass parsed = classNamed(parse(diagram("""
                class Example {
                  +instance$
                  +run()* void
                }
                """)), "Example");

        assertEquals(1, parsed.fields.size());
        assertEquals("instance", parsed.fields.get(0).name);

        ParsedMethod run = parsed.methods.get(0);
        assertEquals("run", run.name);
        assertTrue(run.isAbstract);
    }

    private static ParsedMmdClass classNamed(ParsedMmdDiagram diagram, String name) {
        return diagram.classes.stream()
                .filter(clazz -> name.equals(clazz.name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing class " + name));
    }

    private static void assertMembersEqual(ParsedMmdClass left, ParsedMmdClass right) {
        assertEquals(left.fields.size(), right.fields.size());
        for (int i = 0; i < left.fields.size(); i++) {
            assertFieldEqual(left.fields.get(i), right.fields.get(i));
        }
        assertEquals(left.methods.size(), right.methods.size());
        for (int i = 0; i < left.methods.size(); i++) {
            assertMethodEqual(left.methods.get(i), right.methods.get(i));
        }
    }

    private static void assertFieldEqual(ParsedField left, ParsedField right) {
        assertEquals(left.name, right.name);
        assertEquals(left.dataType, right.dataType);
        assertEquals(left.scope, right.scope);
    }

    private static void assertMethodEqual(ParsedMethod left, ParsedMethod right) {
        assertEquals(left.name, right.name);
        assertEquals(left.returnType, right.returnType);
        assertEquals(left.scope, right.scope);
        assertEquals(left.parameterTypes, right.parameterTypes);
        assertEquals(left.isStatic, right.isStatic);
        assertEquals(left.isAbstract, right.isAbstract);
    }
}
