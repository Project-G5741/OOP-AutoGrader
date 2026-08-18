package com.eiu.capstone.backend.grading.mmd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.grading.MmdParseException;
import com.eiu.capstone.backend.grading.mmd.ast.MmdDiagramAst;

class MmdTokenizerTest {

    private final MmdTokenizer tokenizer = new MmdTokenizer();

    @Test
    void tokenizesClassDiagramHeader() {
        List<MmdToken> tokens = tokenizer.tokenize("classDiagram\nclass Animal");

        assertEquals(MmdTokenType.CLASS_DIAGRAM, tokens.get(0).type());
        assertEquals("classDiagram", tokens.get(0).text());
        assertTrue(tokens.stream().anyMatch(token -> token.type() == MmdTokenType.CLASS));
        assertTrue(tokens.stream().anyMatch(token ->
                token.type() == MmdTokenType.IDENTIFIER && "Animal".equals(token.text())));
    }

    @Test
    void skipsCommentLines() {
        List<MmdToken> tokens = tokenizer.tokenize("%% diagram comment\nclassDiagram");

        assertTrue(tokens.stream().noneMatch(token -> token.text().contains("diagram comment")));
        assertTrue(tokens.stream().anyMatch(token -> token.type() == MmdTokenType.CLASS_DIAGRAM));
    }

    @Test
    void tokenizesRelationArrow() {
        List<MmdToken> tokens = tokenizer.tokenize("Animal <|-- Dog");

        assertTrue(tokens.stream().anyMatch(token ->
                token.type() == MmdTokenType.ARROW && "<|--".equals(token.text())));
    }

    @Test
    void tokenizesQuotedCardinality() {
        List<MmdToken> tokens = tokenizer.tokenize("Company \"1\" --> \"0..1\" Employee");

        long stringCount = tokens.stream().filter(token -> token.type() == MmdTokenType.STRING).count();
        assertEquals(2, stringCount);
    }
}
