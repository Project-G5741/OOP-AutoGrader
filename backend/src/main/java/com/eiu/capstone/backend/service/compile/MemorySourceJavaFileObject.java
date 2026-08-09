package com.eiu.capstone.backend.service.compile;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import javax.tools.SimpleJavaFileObject;

/**
 * In-memory Java source for {@link javax.tools.JavaCompiler} without writing .java files to disk.
 */
public class MemorySourceJavaFileObject extends SimpleJavaFileObject {

    private final String source;

    public MemorySourceJavaFileObject(String logicalPath, byte[] content) {
        super(toSourceUri(logicalPath), Kind.SOURCE);
        this.source = new String(content, StandardCharsets.UTF_8);
    }

    static URI toSourceUri(String logicalPath) {
        String normalized = logicalPath.replace('\\', '/').trim();
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Empty Java source path");
        }

        String[] segments = normalized.split("/");
        StringBuilder encodedPath = new StringBuilder();
        for (String segment : segments) {
            if (segment.isEmpty()) {
                continue;
            }
            encodedPath.append('/')
                    .append(URLEncoder.encode(segment, StandardCharsets.UTF_8).replace("+", "%20"));
        }
        if (encodedPath.length() == 0) {
            throw new IllegalArgumentException("Empty Java source path");
        }

        try {
            return new URI("string", "", encodedPath.toString(), null);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid Java source path: " + logicalPath, e);
        }
    }

    @Override
    public String getCharContent(boolean ignoreEncodingErrors) {
        return source;
    }
}
