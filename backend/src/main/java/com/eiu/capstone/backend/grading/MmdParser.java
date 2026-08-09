package com.eiu.capstone.backend.grading;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class MmdParser {

    private static final Pattern CLASS_START = Pattern.compile("^class\\s+([A-Za-z_]\\w*)\\s*\\{\\s*$");
    private static final Pattern STEREOTYPE = Pattern.compile("^<<\\s*(enumerate|interface)\\s*>>\\s*$", Pattern.CASE_INSENSITIVE);

    private static final String[] RELATION_ARROWS = {
            "..|>", "<|..", "*--", "--*", "o--", "--o", "<-->", "<|--", "--|>",
            "..>", "<..", "-->", "<--", "--"
    };

    public ParsedMmdDiagram parseBytes(byte[] content) {
        if (content == null || content.length == 0) {
            return new ParsedMmdDiagram(List.of(), List.of());
        }
        String text = new String(content, StandardCharsets.UTF_8);
        return parse(text);
    }

    public ParsedMmdDiagram parseFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return new ParsedMmdDiagram(List.of(), List.of());
        }
        return parseBytes(file.getBytes());
    }

    ParsedMmdDiagram parse(String text) {
        List<ParsedMmdClass> classes = new ArrayList<>();
        List<ParsedMmdRelation> relations = new ArrayList<>();

        String[] lines = text.split("\\R");
        ParsedMmdClass current = null;
        int braceDepth = 0;

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("%%")) {
                continue;
            }

            Matcher classStart = CLASS_START.matcher(line);
            if (classStart.matches()) {
                current = new ParsedMmdClass();
                current.name = classStart.group(1);
                braceDepth = 1;
                classes.add(current);
                continue;
            }

            if (current != null && braceDepth > 0) {
                if (line.equals("}")) {
                    braceDepth--;
                    if (braceDepth == 0) {
                        current = null;
                    }
                    continue;
                }
                if (line.contains("{")) {
                    throw new MmdParseException("Nested braces are not supported: " + line);
                }
                parseClassBodyLine(current, line);
                continue;
            }

            RelationMatch relationMatch = matchRelation(line);
            if (relationMatch != null) {
                relations.add(parseRelation(
                        relationMatch.left(),
                        relationMatch.arrow(),
                        relationMatch.right()));
            }
        }

        if (braceDepth > 0) {
            throw new MmdParseException("Unclosed class block");
        }

        return new ParsedMmdDiagram(classes, relations);
    }

    private void parseClassBodyLine(ParsedMmdClass current, String line) {
        Matcher stereotype = STEREOTYPE.matcher(line);
        if (stereotype.matches()) {
            current.stereotypeType = capitalize(stereotype.group(1));
            return;
        }

        char first = line.charAt(0);
        if (first != '-' && first != '+' && first != '#') {
            return;
        }

        String scope = scopeSymbol(first);
        String rest = line.substring(1).trim();

        if (rest.equals("getter()")) {
            ParsedMethod getter = new ParsedMethod();
            getter.name = "__getter_shorthand__";
            getter.scope = scope;
            getter.returnType = "void";
            getter.parameterTypes = List.of();
            current.methods.add(getter);
            return;
        }
        if (rest.equals("setter()")) {
            ParsedMethod setter = new ParsedMethod();
            setter.name = "__setter_shorthand__";
            setter.scope = scope;
            setter.returnType = "void";
            setter.parameterTypes = List.of();
            current.methods.add(setter);
            return;
        }

        int parenOpen = rest.indexOf('(');
        if (parenOpen >= 0) {
            String beforeParen = rest.substring(0, parenOpen).trim();
            int parenClose = rest.indexOf(')', parenOpen);
            if (parenClose < 0) {
                throw new MmdParseException("Unclosed parameter list: " + line);
            }
            String paramsPart = rest.substring(parenOpen + 1, parenClose).trim();
            String afterParen = rest.substring(parenClose + 1).trim();
            List<String> paramTypes = parseParameterTypes(paramsPart);

            if (beforeParen.equals(current.name)) {
                ParsedConstructor ctor = new ParsedConstructor();
                ctor.scope = scope;
                ctor.parameterTypes = paramTypes;
                current.constructors.add(ctor);
            } else {
                ParsedMethod method = new ParsedMethod();
                method.name = beforeParen;
                method.scope = scope;
                method.parameterTypes = paramTypes;
                method.returnType = afterParen.isEmpty() ? "void" : afterParen;
                current.methods.add(method);
            }
            return;
        }

        int colon = rest.indexOf(':');
        if (colon > 0) {
            ParsedField field = new ParsedField();
            field.scope = scope;
            field.name = rest.substring(0, colon).trim();
            field.dataType = rest.substring(colon + 1).trim();
            current.fields.add(field);
            return;
        }

        if (tryParseTypeNameField(current, scope, rest)) {
            return;
        }
    }

    /**
     * Mermaid-style member line: {@code -int yearModel} ({@code visibility type name}).
     */
    private boolean tryParseTypeNameField(ParsedMmdClass current, String scope, String rest) {
        int lastSpace = rest.lastIndexOf(' ');
        if (lastSpace <= 0) {
            return false;
        }
        String dataType = rest.substring(0, lastSpace).trim();
        String name = rest.substring(lastSpace + 1).trim();
        if (dataType.isEmpty() || !isJavaIdentifier(name)) {
            return false;
        }
        ParsedField field = new ParsedField();
        field.scope = scope;
        field.name = name;
        field.dataType = dataType;
        current.fields.add(field);
        return true;
    }

    private static boolean isJavaIdentifier(String value) {
        return value != null && value.matches("[A-Za-z_]\\w*");
    }

    private List<String> parseParameterTypes(String paramsPart) {
        if (paramsPart.isEmpty()) {
            return List.of();
        }
        List<String> types = new ArrayList<>();
        for (String segment : splitParams(paramsPart)) {
            String trimmed = segment.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            types.add(extractParameterType(trimmed));
        }
        return types;
    }

    /**
     * Supports {@code name: type} ({@code yearModel: int}) and {@code type name} ({@code int yearModel}).
     */
    private String extractParameterType(String param) {
        int colon = param.indexOf(':');
        if (colon > 0) {
            return param.substring(colon + 1).trim();
        }
        int lastSpace = param.lastIndexOf(' ');
        if (lastSpace > 0) {
            return param.substring(0, lastSpace).trim();
        }
        return param;
    }

    private List<String> splitParams(String paramsPart) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < paramsPart.length(); i++) {
            char c = paramsPart.charAt(i);
            if (c == '<') depth++;
            else if (c == '>') depth--;
            else if (c == ',' && depth == 0) {
                parts.add(paramsPart.substring(start, i));
                start = i + 1;
            }
        }
        parts.add(paramsPart.substring(start));
        return parts;
    }

    private RelationMatch matchRelation(String line) {
        for (String arrow : RELATION_ARROWS) {
            Pattern pattern = Pattern.compile(
                    "^([A-Za-z_]\\w*)\\s*" + Pattern.quote(arrow) + "\\s*([A-Za-z_]\\w*)\\s*$");
            Matcher matcher = pattern.matcher(line);
            if (matcher.matches()) {
                return new RelationMatch(matcher.group(1), arrow, matcher.group(2));
            }
        }
        return null;
    }

    private record RelationMatch(String left, String arrow, String right) {}

    private ParsedMmdRelation parseRelation(String left, String arrow, String right) {
        ParsedMmdRelation relation = new ParsedMmdRelation();
        relation.relationType = canonicalRelationType(arrow);

        boolean symbolOnLeft = arrow.startsWith("*") || arrow.startsWith("o")
                || arrow.startsWith("<") || arrow.startsWith(".");
        boolean symbolOnRight = arrow.endsWith("*") || arrow.endsWith("o")
                || arrow.endsWith(">") || arrow.endsWith("|");

        if (relation.relationType.equals("link")) {
            relation.sourceClassName = left;
            relation.targetClassName = right;
            return relation;
        }

        if (symbolOnLeft && !symbolOnRight) {
            relation.targetClassName = left;
            relation.sourceClassName = right;
        } else if (symbolOnRight && !symbolOnLeft) {
            relation.targetClassName = right;
            relation.sourceClassName = left;
        } else if (arrow.equals("<-->")) {
            relation.sourceClassName = left;
            relation.targetClassName = right;
        } else {
            relation.targetClassName = left;
            relation.sourceClassName = right;
        }
        return relation;
    }

    static String canonicalRelationType(String arrow) {
        return switch (arrow) {
            case "<|--", "--|>" -> "inheritance";
            case "*--", "--*" -> "composition";
            case "o--", "--o" -> "aggregation";
            case "-->", "<--" -> "association";
            case "<-->" -> "bidirectional_association";
            case "--" -> "link";
            case "..>", "<.." -> "dependency";
            case "..|>", "<|.." -> "realization";
            default -> "association";
        };
    }

    private static String scopeSymbol(char symbol) {
        return switch (symbol) {
            case '-' -> "private";
            case '+' -> "public";
            case '#' -> "protected";
            default -> "";
        };
    }

    private static String capitalize(String value) {
        if (value == null || value.isEmpty()) return value;
        return Character.toUpperCase(value.charAt(0)) + value.substring(1).toLowerCase();
    }
}
