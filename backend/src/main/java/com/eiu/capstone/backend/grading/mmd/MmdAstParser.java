package com.eiu.capstone.backend.grading.mmd;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.eiu.capstone.backend.grading.MmdParseException;
import com.eiu.capstone.backend.grading.mmd.ast.MmdClassNode;
import com.eiu.capstone.backend.grading.mmd.ast.MmdColonMemberNode;
import com.eiu.capstone.backend.grading.mmd.ast.MmdDiagramAst;
import com.eiu.capstone.backend.grading.mmd.ast.MmdIgnoredDirectiveNode;
import com.eiu.capstone.backend.grading.mmd.ast.MmdNamespaceNode;
import com.eiu.capstone.backend.grading.mmd.ast.MmdRelationNode;
import com.eiu.capstone.backend.grading.mmd.ast.MmdStandaloneStereotypeNode;
import com.eiu.capstone.backend.grading.mmd.ast.MmdTopLevelNode;

/**
 * Builds a diagram AST from source text and enforces the {@code classDiagram} header contract.
 * Top-level structure is line-scanned; the {@link MmdTokenizer} validates tokenization separately.
 */
public final class MmdAstParser {

    private final MmdRelationLineParser relationLineParser = new MmdRelationLineParser();

    private static final Pattern CLASS_BLOCK_START = Pattern.compile(
            "^class\\s+([A-Za-z_]\\w*)(?:\\[\"([^\"]*)\"\\])?\\s*\\{\\s*$");
    private static final Pattern CLASS_DECLARATION = Pattern.compile(
            "^class\\s+([A-Za-z_]\\w*)(?:\\[\"([^\"]*)\"\\])?\\s*$");
    private static final Pattern NAMESPACE_BLOCK_START = Pattern.compile(
            "^namespace\\s+([\\w.]+)\\s*\\{\\s*$");
    private static final Pattern COLON_MEMBER = Pattern.compile(
            "^([A-Za-z_]\\w*)\\s*:\\s+(.+)$");
    private static final Pattern STANDALONE_STEREOTYPE = Pattern.compile(
            "^<<\\s*(enumeration|enumerate|enum|interface|abstract|final|service)\\s*>>\\s+([A-Za-z_]\\w*)\\s*$",
            Pattern.CASE_INSENSITIVE);

    public MmdDiagramAst parse(String source) {
        if (source == null || source.isBlank()) {
            return new MmdDiagramAst(false, List.of());
        }

        String[] rawLines = source.split("\\R", -1);
        boolean classDiagramDeclared = false;
        ParseAccumulator accumulator = new ParseAccumulator();

        int index = 0;
        while (index < rawLines.length) {
            String line = rawLines[index].trim();
            if (line.isEmpty() || line.startsWith("%%")) {
                index++;
                continue;
            }

            if (line.equals("classDiagram")) {
                classDiagramDeclared = true;
                index++;
                continue;
            }

            index = parseLine(rawLines, index, accumulator, false);
        }

        if (accumulator.substantiveContent && !classDiagramDeclared) {
            throw new MmdParseException("Missing classDiagram header");
        }

        return new MmdDiagramAst(classDiagramDeclared, List.copyOf(accumulator.nodes));
    }

    private int parseLine(String[] rawLines, int index, ParseAccumulator accumulator, boolean insideBlock) {
        String line = rawLines[index].trim();
        if (line.isEmpty() || line.startsWith("%%")) {
            return index + 1;
        }

        if (insideBlock && line.equals("}")) {
            return index + 1;
        }

        Matcher classBlock = CLASS_BLOCK_START.matcher(line);
        if (classBlock.matches()) {
            accumulator.markSubstantive();
            int classLine = index + 1;
            List<String> bodyLines = new ArrayList<>();
            index++;
            while (index < rawLines.length) {
                String bodyLine = rawLines[index].trim();
                if (bodyLine.equals("}")) {
                    index++;
                    break;
                }
                if (bodyLine.contains("{")) {
                    throw new MmdParseException("Nested braces are not supported: " + bodyLine);
                }
                if (!bodyLine.isEmpty() && !bodyLine.startsWith("%%")) {
                    bodyLines.add(bodyLine);
                }
                index++;
            }
            accumulator.nodes.add(new MmdClassNode(
                    classBlock.group(1),
                    classBlock.group(2),
                    List.copyOf(bodyLines),
                    classLine));
            return index;
        }

        Matcher namespaceBlock = NAMESPACE_BLOCK_START.matcher(line);
        if (namespaceBlock.matches()) {
            accumulator.markSubstantive();
            int namespaceLine = index + 1;
            String namespaceName = namespaceBlock.group(1);
            ParseAccumulator children = new ParseAccumulator();
            index++;
            boolean closed = false;
            while (index < rawLines.length) {
                String childLine = rawLines[index].trim();
                if (childLine.equals("}")) {
                    index++;
                    closed = true;
                    break;
                }
                index = parseLine(rawLines, index, children, true);
            }
            if (!closed) {
                throw new MmdParseException("Unclosed namespace block");
            }
            accumulator.nodes.add(new MmdNamespaceNode(
                    namespaceName, List.copyOf(children.nodes), namespaceLine));
            return index;
        }

        Matcher classDecl = CLASS_DECLARATION.matcher(line);
        if (classDecl.matches()) {
            accumulator.markSubstantive();
            accumulator.nodes.add(new MmdClassNode(
                    classDecl.group(1),
                    classDecl.group(2),
                    List.of(),
                    index + 1));
            return index + 1;
        }

        MmdRelationNode relation = relationLineParser.parse(line, index + 1);
        if (relation != null) {
            accumulator.markSubstantive();
            accumulator.nodes.add(relation);
            return index + 1;
        }

        Matcher colonMember = COLON_MEMBER.matcher(line);
        if (colonMember.matches()) {
            accumulator.markSubstantive();
            accumulator.nodes.add(new MmdColonMemberNode(
                    colonMember.group(1),
                    colonMember.group(2).trim(),
                    index + 1));
            return index + 1;
        }

        Matcher standaloneStereotype = STANDALONE_STEREOTYPE.matcher(line);
        if (standaloneStereotype.matches()) {
            accumulator.markSubstantive();
            accumulator.nodes.add(new MmdStandaloneStereotypeNode(
                    standaloneStereotype.group(1),
                    standaloneStereotype.group(2),
                    index + 1));
            return index + 1;
        }

        if (isIgnorableDirective(line)) {
            accumulator.nodes.add(new MmdIgnoredDirectiveNode(line, index + 1));
            return index + 1;
        }

        throw new MmdParseException("Unexpected line at " + (index + 1) + ": " + line);
    }

    private static boolean isIgnorableDirective(String line) {
        return line.startsWith("note ")
                || line.startsWith("note for ")
                || line.startsWith("direction ")
                || line.startsWith("style ")
                || line.startsWith("classDef ")
                || line.startsWith("cssClass ");
    }

    private static final class ParseAccumulator {
        private final List<MmdTopLevelNode> nodes = new ArrayList<>();
        private boolean substantiveContent;

        private void markSubstantive() {
            substantiveContent = true;
        }
    }
}
