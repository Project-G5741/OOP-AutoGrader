package com.eiu.capstone.backend.service.compile;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Strips {@code package} declarations and same-challenge cross-imports so student
 * submissions compile in the default package for rubric reflection grading.
 */
public final class StudentSourceNormalizer {

    public static final String PACKAGE_IGNORED_NOTICE =
            "Package declarations were ignored for grading. Your class structure below the package line was evaluated in the default package.";

    private static final Pattern PACKAGE_LINE =
            Pattern.compile("(?m)^\\s*package\\s+[\\w.]+(?:\\.\\*)?\\s*;\\s*(?:\\r\\n|\\n|\\r)?");
    private static final Pattern IMPORT_LINE =
            Pattern.compile("(?m)^\\s*import\\s+(?:static\\s+)?([\\w.]+)\\s*;\\s*(?:\\r\\n|\\n|\\r)?");
    private static final Pattern TOP_LEVEL_TYPE =
            Pattern.compile("(?:^|[\\s;{}])(?:public\\s+)?(?:abstract\\s+)?(?:class|interface|enum|record)\\s+(\\w+)");

    private StudentSourceNormalizer() {
    }

    public record SourceEntry(String logicalPath, String source) {}

    public record NormalizationResult(List<SourceEntry> sources, boolean packageStripped) {}

    public static NormalizationResult normalizeChallengeSources(List<SourceEntry> sources) {
        if (sources == null || sources.isEmpty()) {
            return new NormalizationResult(List.of(), false);
        }

        Set<String> declaredSimpleNames = new LinkedHashSet<>();
        boolean packageStripped = false;
        List<String> rawSources = new ArrayList<>(sources.size());

        for (SourceEntry entry : sources) {
            String source = entry.source() != null ? entry.source() : "";
            if (PACKAGE_LINE.matcher(source).find()) {
                packageStripped = true;
            }
            declaredSimpleNames.addAll(extractDeclaredSimpleNames(source));
            rawSources.add(source);
        }

        List<SourceEntry> normalized = new ArrayList<>(sources.size());
        for (int i = 0; i < sources.size(); i++) {
            SourceEntry entry = sources.get(i);
            String transformed = transformSource(rawSources.get(i), declaredSimpleNames);
            normalized.add(new SourceEntry(entry.logicalPath(), transformed));
        }

        return new NormalizationResult(normalized, packageStripped);
    }

    private static String transformSource(String source, Set<String> declaredSimpleNames) {
        String withoutPackage = PACKAGE_LINE.matcher(source).replaceAll("");
        return stripSameChallengeImports(withoutPackage, declaredSimpleNames);
    }

    private static String stripSameChallengeImports(String source, Set<String> declaredSimpleNames) {
        Matcher matcher = IMPORT_LINE.matcher(source);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String imported = matcher.group(1);
            if (imported == null) {
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(matcher.group(0)));
                continue;
            }
            String lower = imported.toLowerCase(Locale.ROOT);
            if (lower.startsWith("java.") || lower.startsWith("javax.")) {
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(matcher.group(0)));
                continue;
            }
            String simpleName = simpleNameOfImport(imported);
            if (declaredSimpleNames.contains(simpleName)) {
                matcher.appendReplacement(buffer, "");
            } else {
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String simpleNameOfImport(String imported) {
        int dot = imported.lastIndexOf('.');
        return dot >= 0 ? imported.substring(dot + 1) : imported;
    }

    private static Set<String> extractDeclaredSimpleNames(String source) {
        Set<String> names = new LinkedHashSet<>();
        Matcher matcher = TOP_LEVEL_TYPE.matcher(source);
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return names;
    }
}
