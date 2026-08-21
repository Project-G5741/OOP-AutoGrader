package com.eiu.capstone.backend.grading;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.eiu.capstone.backend.grading.mmd.MmdAstParser;
import com.eiu.capstone.backend.grading.mmd.MmdAstToParsedMapper;
import com.eiu.capstone.backend.grading.mmd.ast.MmdDiagramAst;

/**
 * Facade for MMD parsing: tokenizer → AST → {@link ParsedMmdDiagram}.
 */
@Component
public class MmdParser {

    private final MmdAstParser astParser = new MmdAstParser();
    private final MmdAstToParsedMapper astMapper = new MmdAstToParsedMapper();

    public ParsedMmdDiagram parseBytes(byte[] content) {
        if (content == null || content.length == 0) {
            return new ParsedMmdDiagram(java.util.List.of(), java.util.List.of());
        }
        String text = new String(content, StandardCharsets.UTF_8);
        return parse(text);
    }

    public ParsedMmdDiagram parseFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return new ParsedMmdDiagram(java.util.List.of(), java.util.List.of());
        }
        return parseBytes(file.getBytes());
    }

    ParsedMmdDiagram parse(String text) {
        MmdDiagramAst ast = astParser.parse(text);
        return astMapper.map(ast);
    }

    /** @deprecated Use {@link com.eiu.capstone.backend.grading.mmd.MmdRelationTypes#canonicalRelationType}. */
    @Deprecated
    static String canonicalRelationType(String arrow) {
        return com.eiu.capstone.backend.grading.mmd.MmdRelationTypes.canonicalRelationType(arrow);
    }
}
