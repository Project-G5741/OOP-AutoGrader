package com.eiu.capstone.backend.grading.mmd;

import java.util.ArrayList;
import java.util.List;

import com.eiu.capstone.backend.grading.MmdParseException;

/**
 * Character-level tokenizer for Mermaid {@code classDiagram} source text.
 */
public final class MmdTokenizer {

    public List<MmdToken> tokenize(String source) {
        if (source == null || source.isEmpty()) {
            return List.of(MmdToken.of(MmdTokenType.EOF, "", 1, 1));
        }

        List<MmdToken> tokens = new ArrayList<>();
        int line = 1;
        int column = 1;
        int index = 0;

        while (index < source.length()) {
            char ch = source.charAt(index);

            if (ch == '\r') {
                index++;
                if (index < source.length() && source.charAt(index) == '\n') {
                    index++;
                }
                tokens.add(MmdToken.of(MmdTokenType.NEWLINE, "\n", line, column));
                line++;
                column = 1;
                continue;
            }
            if (ch == '\n') {
                index++;
                tokens.add(MmdToken.of(MmdTokenType.NEWLINE, "\n", line, column));
                line++;
                column = 1;
                continue;
            }

            if (Character.isWhitespace(ch)) {
                index++;
                column++;
                continue;
            }

            if (ch == '%' && index + 1 < source.length() && source.charAt(index + 1) == '%') {
                while (index < source.length() && source.charAt(index) != '\n' && source.charAt(index) != '\r') {
                    index++;
                }
                continue;
            }

            if (ch == '"') {
                TokenSpan span = readQuotedString(source, index, line, column);
                tokens.add(MmdToken.of(MmdTokenType.STRING, span.text(), span.startLine(), span.startColumn()));
                index = span.endIndex();
                line = span.endLine();
                column = span.endColumn();
                continue;
            }

            if (ch == '{') {
                tokens.add(MmdToken.of(MmdTokenType.LBRACE, "{", line, column));
                index++;
                column++;
                continue;
            }
            if (ch == '}') {
                tokens.add(MmdToken.of(MmdTokenType.RBRACE, "}", line, column));
                index++;
                column++;
                continue;
            }
            if (ch == '(') {
                tokens.add(MmdToken.of(MmdTokenType.LPAREN, "(", line, column));
                index++;
                column++;
                continue;
            }
            if (ch == ')') {
                tokens.add(MmdToken.of(MmdTokenType.RPAREN, ")", line, column));
                index++;
                column++;
                continue;
            }
            if (ch == ':') {
                tokens.add(MmdToken.of(MmdTokenType.COLON, ":", line, column));
                index++;
                column++;
                continue;
            }

            String arrow = MmdRelationTypes.matchArrowAt(source, index);
            if (arrow != null) {
                tokens.add(MmdToken.of(MmdTokenType.ARROW, arrow, line, column));
                index += arrow.length();
                column += arrow.length();
                continue;
            }

            if (ch == '<' && index + 1 < source.length() && source.charAt(index + 1) == '<') {
                int startLine = line;
                int startColumn = column;
                index += 2;
                column += 2;
                StringBuilder stereotype = new StringBuilder();
                while (index < source.length()) {
                    if (source.charAt(index) == '>' && index + 1 < source.length()
                            && source.charAt(index + 1) == '>') {
                        index += 2;
                        column += 2;
                        break;
                    }
                    stereotype.append(source.charAt(index));
                    index++;
                    column++;
                }
                tokens.add(MmdToken.of(
                        MmdTokenType.STEREOTYPE,
                        stereotype.toString().trim(),
                        startLine,
                        startColumn));
                continue;
            }

            if (ch == '+' || ch == '-' || ch == '#' || ch == '~') {
                tokens.add(MmdToken.of(MmdTokenType.VISIBILITY, String.valueOf(ch), line, column));
                index++;
                column++;
                continue;
            }
            if (ch == '$' || ch == '*') {
                tokens.add(MmdToken.of(MmdTokenType.IDENTIFIER, String.valueOf(ch), line, column));
                index++;
                column++;
                continue;
            }

            if (isIdentifierStart(ch)) {
                int startLine = line;
                int startColumn = column;
                StringBuilder identifier = new StringBuilder();
                while (index < source.length()) {
                    char current = source.charAt(index);
                    if (!isIdentifierPart(current)) {
                        break;
                    }
                    identifier.append(current);
                    index++;
                    column++;
                }
                String text = identifier.toString();
                MmdTokenType type = keywordType(text);
                tokens.add(MmdToken.of(type, text, startLine, startColumn));
                continue;
            }

            throw new MmdParseException("Unexpected character '" + ch + "' at line " + line);
        }

        tokens.add(MmdToken.of(MmdTokenType.EOF, "", line, column));
        return tokens;
    }

    private static TokenSpan readQuotedString(String source, int startIndex, int line, int column) {
        int index = startIndex + 1;
        int currentLine = line;
        int currentColumn = column + 1;
        StringBuilder value = new StringBuilder();
        while (index < source.length()) {
            char current = source.charAt(index);
            if (current == '"') {
                return new TokenSpan(value.toString(), line, column, index + 1, currentLine, currentColumn + 1);
            }
            if (current == '\r') {
                index++;
                if (index < source.length() && source.charAt(index) == '\n') {
                    index++;
                }
                currentLine++;
                currentColumn = 1;
                continue;
            }
            if (current == '\n') {
                index++;
                currentLine++;
                currentColumn = 1;
                continue;
            }
            value.append(current);
            index++;
            currentColumn++;
        }
        throw new MmdParseException("Unclosed string at line " + line);
    }

    private record TokenSpan(
            String text,
            int startLine,
            int startColumn,
            int endIndex,
            int endLine,
            int endColumn) {}

    private static MmdTokenType keywordType(String text) {
        return switch (text) {
            case "classDiagram" -> MmdTokenType.CLASS_DIAGRAM;
            case "class" -> MmdTokenType.CLASS;
            case "namespace" -> MmdTokenType.NAMESPACE;
            default -> MmdTokenType.IDENTIFIER;
        };
    }

    private static boolean isIdentifierStart(char ch) {
        return Character.isLetter(ch) || ch == '_';
    }

    private static boolean isIdentifierPart(char ch) {
        return Character.isLetterOrDigit(ch) || ch == '_' || ch == '-';
    }
}
