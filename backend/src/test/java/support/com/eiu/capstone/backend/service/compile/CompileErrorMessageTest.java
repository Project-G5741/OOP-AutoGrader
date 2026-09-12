package support.com.eiu.capstone.backend.service.compile;

import com.eiu.capstone.backend.service.compile.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import org.junit.jupiter.api.Test;

class CompileErrorMessageTest {

    @Test
    void missingSemicolonIncludesLine() {
        assertEquals("Missing ; on line 4", CompileErrorMessage.summarize("';' expected", 4));
    }

    @Test
    void cannotFindSymbolUsesSymbolName() {
        String raw = """
                cannot find symbol
                  symbol:   class Observer
                  location: class SMSSubscriber
                """;
        assertEquals("Observer not found", CompileErrorMessage.summarize(raw, 1));
        assertEquals(
                "Observer not found",
                CompileErrorMessage.summarize("cannot find symbol: class Observer", 1));
    }

    @Test
    void publicClassMustMatchFile() {
        assertEquals(
                "Class name must match file",
                CompileErrorMessage.summarize(
                        "class EmailSubscriber is public, should be declared in a file named EmailSubscriber.java",
                        1));
        assertEquals(
                "Wrong class in this file",
                CompileErrorMessage.forFileRoot(
                        "Observer", "Observer", Set.of("NewsAgency"), "Class name must match file"));
        assertEquals(
                "Declared in Observer.java",
                CompileErrorMessage.forFileRoot(
                        "NewsAgency", "Observer", Set.of("NewsAgency"), "Class name must match file"));
    }

    @Test
    void nestedTypeInMatchingOuterFileIsNotWrongFile() {
        assertEquals(
                "Class name must match file",
                CompileErrorMessage.forFileRoot(
                        "Inner", "Outer", Set.of("Outer", "Inner"), "Class name must match file"));
        assertEquals(
                "Unclosed class on line 1",
                CompileErrorMessage.forFileRoot(
                        "Inner", "Outer", Set.of("Outer", "Inner"), "Unclosed class on line 1"));
        assertEquals(
                "Unclosed class on line 1",
                CompileErrorMessage.forFileRoot(
                        "Outer.Inner", "Outer", Set.of("Outer", "Inner"), "Unclosed class on line 1"));
    }

    @Test
    void unclosedClass() {
        assertEquals("Unclosed class on line 1", CompileErrorMessage.summarize("reached end of file while parsing", 1));
    }

    @Test
    void dependentPointer() {
        assertEquals("See Subject", CompileErrorMessage.see("Subject"));
        assertEquals("See Subject", CompileErrorMessage.summarize("See Subject"));
        assertEquals("See Subject", CompileErrorMessage.summarize("Compilation Error on Subject"));
    }

    @Test
    void storedJavacDumpIsRewritten() {
        assertEquals(
                "Missing ; on line 1",
                CompileErrorMessage.summarize("ERROR: line 1: ';' expected"));
    }

    @Test
    void invalidSyntaxIncludesLine() {
        assertEquals("Invalid syntax on line 3", CompileErrorMessage.summarize("illegal start of expression", 3));
    }

    @Test
    void duplicateClass() {
        assertEquals("Duplicate class", CompileErrorMessage.summarize("duplicate class: Foo", 1));
    }
}
