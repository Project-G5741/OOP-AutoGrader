package com.eiu.capstone.backend.grading.mmd;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.eiu.capstone.backend.grading.MmdParseException;
import com.eiu.capstone.backend.grading.ParsedConstructor;
import com.eiu.capstone.backend.grading.ParsedField;
import com.eiu.capstone.backend.grading.ParsedMethod;
import com.eiu.capstone.backend.grading.ParsedMmdClass;
import com.eiu.capstone.backend.grading.ParsedMmdDiagram;
import com.eiu.capstone.backend.grading.ParsedMmdRelation;
import com.eiu.capstone.backend.grading.mmd.ast.MmdClassNode;
import com.eiu.capstone.backend.grading.mmd.ast.MmdColonMemberNode;
import com.eiu.capstone.backend.grading.mmd.ast.MmdDiagramAst;
import com.eiu.capstone.backend.grading.mmd.ast.MmdNamespaceNode;
import com.eiu.capstone.backend.grading.mmd.ast.MmdRelationNode;
import com.eiu.capstone.backend.grading.mmd.ast.MmdStandaloneStereotypeNode;
import com.eiu.capstone.backend.grading.mmd.ast.MmdTopLevelNode;

/**
 * Maps a diagram AST to the legacy {@link ParsedMmdDiagram} comparison model.
 */
public final class MmdAstToParsedMapper {

    private static final Pattern STEREOTYPE = Pattern.compile(
            "^<<\\s*(enumeration|enumerate|enum|interface|abstract|final)\\s*>>\\s*$",
            Pattern.CASE_INSENSITIVE);

    private static final Set<String> PRIMITIVE_TYPES = Set.of(
            "byte", "short", "int", "long", "float", "double", "boolean", "char", "void");

    public ParsedMmdDiagram map(MmdDiagramAst ast) {
        if (ast == null) {
            return new ParsedMmdDiagram(List.of(), List.of());
        }

        Map<String, ParsedMmdClass> classesByName = new LinkedHashMap<>();
        Map<ParsedMmdClass, Boolean> uniqueClasses = new IdentityHashMap<>();
        List<ParsedMmdRelation> relations = new ArrayList<>();

        for (MmdTopLevelNode node : ast.nodes()) {
            mapNode(node, classesByName, uniqueClasses, relations, null);
        }

        return new ParsedMmdDiagram(new ArrayList<>(uniqueClasses.keySet()), relations, Map.copyOf(classesByName));
    }

    private void mapNode(
            MmdTopLevelNode node,
            Map<String, ParsedMmdClass> classesByName,
            Map<ParsedMmdClass, Boolean> uniqueClasses,
            List<ParsedMmdRelation> relations,
            String namespacePrefix) {
        if (node instanceof MmdClassNode classNode) {
            mergeClassNode(classesByName, uniqueClasses, classNode);
            registerQualifiedAlias(classesByName, namespacePrefix, classNode.name());
        } else if (node instanceof MmdRelationNode relationNode) {
            relations.add(mapRelation(relationNode));
            registerClass(classesByName, uniqueClasses, relationNode.leftClass());
            registerClass(classesByName, uniqueClasses, relationNode.rightClass());
        } else if (node instanceof MmdColonMemberNode colonMember) {
            ParsedMmdClass target = registerClass(classesByName, uniqueClasses, colonMember.className());
            parseClassBodyLine(target, colonMember.memberLine());
        } else if (node instanceof MmdStandaloneStereotypeNode stereotypeNode) {
            ParsedMmdClass target = registerClass(classesByName, uniqueClasses, stereotypeNode.className());
            target.stereotypeType = stereotypeName(stereotypeNode.stereotype());
        } else if (node instanceof MmdNamespaceNode namespaceNode) {
            String prefix = qualifyNamespace(namespacePrefix, namespaceNode.name());
            for (MmdTopLevelNode child : namespaceNode.children()) {
                mapNode(child, classesByName, uniqueClasses, relations, prefix);
            }
        }
    }

    private static String qualifyNamespace(String parentPrefix, String namespaceName) {
        if (parentPrefix == null || parentPrefix.isEmpty()) {
            return namespaceName;
        }
        return parentPrefix + "." + namespaceName;
    }

    private static void registerQualifiedAlias(
            Map<String, ParsedMmdClass> classesByName, String namespacePrefix, String simpleName) {
        if (namespacePrefix == null || namespacePrefix.isEmpty()) {
            return;
        }
        ParsedMmdClass parsed = classesByName.get(simpleName);
        if (parsed == null) {
            return;
        }
        classesByName.putIfAbsent(namespacePrefix + "." + simpleName, parsed);
    }

    private static ParsedMmdClass registerClass(
            Map<String, ParsedMmdClass> classesByName,
            Map<ParsedMmdClass, Boolean> uniqueClasses,
            String name) {
        ParsedMmdClass parsed = classesByName.computeIfAbsent(name, className -> {
            ParsedMmdClass created = new ParsedMmdClass();
            created.name = className;
            return created;
        });
        uniqueClasses.putIfAbsent(parsed, Boolean.TRUE);
        return parsed;
    }

    private void mergeClassNode(
            Map<String, ParsedMmdClass> classesByName,
            Map<ParsedMmdClass, Boolean> uniqueClasses,
            MmdClassNode classNode) {
        ParsedMmdClass existing = classesByName.get(classNode.name());
        if (existing == null) {
            ParsedMmdClass parsed = mapClass(classNode);
            classesByName.put(classNode.name(), parsed);
            uniqueClasses.put(parsed, Boolean.TRUE);
            return;
        }
        uniqueClasses.putIfAbsent(existing, Boolean.TRUE);
        for (String bodyLine : classNode.bodyLines()) {
            parseClassBodyLine(existing, bodyLine);
        }
    }

    private ParsedMmdClass mapClass(MmdClassNode classNode) {
        ParsedMmdClass parsed = new ParsedMmdClass();
        parsed.name = classNode.name();
        for (String bodyLine : classNode.bodyLines()) {
            parseClassBodyLine(parsed, bodyLine);
        }
        return parsed;
    }


    private ParsedMmdRelation mapRelation(MmdRelationNode relationNode) {
        ParsedMmdRelation relation = parseRelation(
                relationNode.leftClass(), relationNode.arrow(), relationNode.rightClass());
        relation.sourceCardinality = cardinalityForClass(relation.sourceClassName, relationNode);
        relation.targetCardinality = cardinalityForClass(relation.targetClassName, relationNode);
        return relation;
    }

    private static String cardinalityForClass(String className, MmdRelationNode node) {
        if (className == null) {
            return null;
        }
        if (className.equals(node.leftClass())) {
            return node.leftCardinality();
        }
        if (className.equals(node.rightClass())) {
            return node.rightCardinality();
        }
        return null;
    }

    private void parseClassBodyLine(ParsedMmdClass current, String line) {
        line = line.trim();
        if (line.isEmpty()) {
            return;
        }

        Matcher stereotype = STEREOTYPE.matcher(line);
        if (stereotype.matches()) {
            current.stereotypeType = stereotypeName(stereotype.group(1));
            return;
        }

        char first = line.charAt(0);
        if (first != '-' && first != '+' && first != '#' && first != '~') {
            return;
        }

        String scope = scopeSymbol(first);
        String rest = line.substring(1).trim();
        rest = stripLeadingStaticKeyword(rest);

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
            if (parenClose + 1 < rest.length()) {
                char afterClose = rest.charAt(parenClose + 1);
                if (!Character.isWhitespace(afterClose) && afterClose != '*' && afterClose != '$') {
                    throw new MmdParseException("Missing space before return type: " + line);
                }
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
                MermaidMemberSuffix nameSuffix = parseMermaidSuffixes(beforeParen);
                MermaidMemberSuffix returnSuffix = parseMermaidSuffixes(afterParen);
                ParsedMethod method = new ParsedMethod();
                method.name = nameSuffix.value();
                method.scope = scope;
                method.parameterTypes = paramTypes;
                method.isStatic = nameSuffix.isStatic() || returnSuffix.isStatic();
                method.isAbstract = nameSuffix.isAbstract() || returnSuffix.isAbstract();
                method.returnType = returnSuffix.value().isEmpty() ? "void" : returnSuffix.value();
                current.methods.add(method);
            }
            return;
        }

        int colon = rest.indexOf(':');
        if (colon > 0) {
            MermaidMemberSuffix nameSuffix = parseMermaidSuffixes(rest.substring(0, colon).trim());
            MermaidMemberSuffix typeSuffix = parseMermaidSuffixes(rest.substring(colon + 1).trim());
            ParsedField field = new ParsedField();
            field.scope = scope;
            field.name = nameSuffix.value();
            field.dataType = typeSuffix.value();
            current.fields.add(field);
            return;
        }

        if (tryParseStaticOnlyField(current, scope, rest)) {
            return;
        }

        tryParseTypeNameField(current, scope, rest);
    }

    private boolean tryParseStaticOnlyField(ParsedMmdClass current, String scope, String rest) {
        if (rest.contains(" ") || rest.contains("(") || rest.contains(":")) {
            return false;
        }
        MermaidMemberSuffix suffix = parseMermaidSuffixes(rest);
        if (!suffix.isStatic() || !isJavaIdentifier(suffix.value())) {
            return false;
        }
        ParsedField field = new ParsedField();
        field.scope = scope;
        field.name = suffix.value();
        field.dataType = "";
        current.fields.add(field);
        return true;
    }

    private boolean tryParseTypeNameField(ParsedMmdClass current, String scope, String rest) {
        int lastSpace = rest.lastIndexOf(' ');
        if (lastSpace <= 0) {
            return false;
        }
        MermaidMemberSuffix left = parseMermaidSuffixes(rest.substring(0, lastSpace).trim());
        MermaidMemberSuffix right = parseMermaidSuffixes(rest.substring(lastSpace + 1).trim());
        String dataType;
        String name;
        if (looksLikeType(left.value())) {
            dataType = left.value();
            name = right.value();
        } else {
            name = left.value();
            dataType = right.value();
        }
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

    private String extractParameterType(String param) {
        param = param.trim();
        int colon = param.indexOf(':');
        if (colon > 0) {
            return parseMermaidSuffixes(param.substring(colon + 1).trim()).value();
        }
        int lastSpace = param.lastIndexOf(' ');
        if (lastSpace > 0) {
            String left = param.substring(0, lastSpace).trim();
            String right = param.substring(lastSpace + 1).trim();
            if (looksLikeType(left)) {
                return parseMermaidSuffixes(left).value();
            }
            return parseMermaidSuffixes(right).value();
        }
        return parseMermaidSuffixes(param).value();
    }

    private String stripLeadingStaticKeyword(String value) {
        if (value.regionMatches(true, 0, "static ", 0, 7)) {
            return value.substring(7).trim();
        }
        return value;
    }

    private MermaidMemberSuffix parseMermaidSuffixes(String token) {
        boolean isStatic = false;
        boolean isAbstract = false;
        String stripped = token == null ? "" : token.trim();
        while (!stripped.isEmpty()) {
            boolean changed = false;
            if (stripped.startsWith("$")) {
                isStatic = true;
                stripped = stripped.substring(1).trim();
                changed = true;
            } else if (stripped.startsWith("*")) {
                isAbstract = true;
                stripped = stripped.substring(1).trim();
                changed = true;
            } else if (stripped.endsWith("$")) {
                isStatic = true;
                stripped = stripped.substring(0, stripped.length() - 1).trim();
                changed = true;
            } else if (stripped.endsWith("*")) {
                isAbstract = true;
                stripped = stripped.substring(0, stripped.length() - 1).trim();
                changed = true;
            }
            if (!changed) {
                break;
            }
        }
        return new MermaidMemberSuffix(stripped, isStatic, isAbstract);
    }

    private boolean looksLikeType(String token) {
        String base = parseMermaidSuffixes(token).value();
        if (base.endsWith("[]")) {
            base = base.substring(0, base.length() - 2).trim();
        }
        if (PRIMITIVE_TYPES.contains(base)) {
            return true;
        }
        if (base.contains("<") || base.contains(".")) {
            return true;
        }
        return !base.isEmpty() && Character.isUpperCase(base.charAt(0));
    }

    private record MermaidMemberSuffix(String value, boolean isStatic, boolean isAbstract) {}

    private List<String> splitParams(String paramsPart) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < paramsPart.length(); i++) {
            char c = paramsPart.charAt(i);
            if (c == '<') {
                depth++;
            } else if (c == '>') {
                depth--;
            } else if (c == ',' && depth == 0) {
                parts.add(paramsPart.substring(start, i));
                start = i + 1;
            }
        }
        parts.add(paramsPart.substring(start));
        return parts;
    }

    private ParsedMmdRelation parseRelation(String left, String arrow, String right) {
        ParsedMmdRelation relation = new ParsedMmdRelation();
        relation.relationType = MmdRelationTypes.canonicalRelationType(arrow);

        if ("()--".equals(arrow)) {
            relation.relationType = "realization";
            relation.targetClassName = left;
            relation.sourceClassName = right;
            return relation;
        }
        if ("--()".equals(arrow)) {
            relation.relationType = "realization";
            relation.targetClassName = right;
            relation.sourceClassName = left;
            return relation;
        }

        if (relation.relationType.equals("link") || relation.relationType.equals("dashed_link")) {
            relation.sourceClassName = left;
            relation.targetClassName = right;
            return relation;
        }

        if (relation.relationType.equals("aggregation") || relation.relationType.equals("composition")) {
            if (arrow.startsWith("o") || arrow.startsWith("*")) {
                relation.sourceClassName = left;
                relation.targetClassName = right;
            } else {
                relation.sourceClassName = right;
                relation.targetClassName = left;
            }
            return relation;
        }

        if (relation.relationType.equals("dependency")) {
            relation.sourceClassName = left;
            relation.targetClassName = right;
            return relation;
        }

        if (relation.relationType.equals("realization")) {
            assignRealizationEndpoints(left, arrow, right, relation);
            return relation;
        }

        boolean symbolOnLeft = arrow.startsWith("*") || arrow.startsWith("o")
                || arrow.startsWith("<") || arrow.startsWith(".");
        boolean symbolOnRight = arrow.endsWith("*") || arrow.endsWith("o")
                || arrow.endsWith(">") || arrow.endsWith("|");

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

    private static void assignRealizationEndpoints(
            String left, String arrow, String right, ParsedMmdRelation relation) {
        if ("<|..".equals(arrow)) {
            relation.targetClassName = left;
            relation.sourceClassName = right;
        } else {
            relation.targetClassName = right;
            relation.sourceClassName = left;
        }
    }

    private static String scopeSymbol(char symbol) {
        return switch (symbol) {
            case '-' -> "private";
            case '+' -> "public";
            case '#' -> "protected";
            case '~' -> "package";
            default -> "";
        };
    }

    private static String stereotypeName(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        if (isEnumStereotypeKeyword(value)) {
            return "Enumeration";
        }
        return capitalize(value);
    }

    private static boolean isEnumStereotypeKeyword(String value) {
        return "enum".equalsIgnoreCase(value)
                || "enumerate".equalsIgnoreCase(value)
                || "enumeration".equalsIgnoreCase(value);
    }

    private static String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1).toLowerCase(Locale.ROOT);
    }
}
