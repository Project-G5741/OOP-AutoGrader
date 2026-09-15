package com.eiu.capstone.sandboxrunner.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

public final class TarGz {

    private TarGz() {}

    public static byte[] directoryToGzipTar(Path root) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(bytes);
             TarArchiveOutputStream tar = new TarArchiveOutputStream(gzip)) {
            tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            try (Stream<Path> walk = Files.walk(root)) {
                walk.filter(path -> Files.isRegularFile(path))
                        .forEach(path -> {
                            try {
                                String entryName = root.relativize(path).toString().replace('\\', '/');
                                TarArchiveEntry entry = new TarArchiveEntry(path, entryName);
                                tar.putArchiveEntry(entry);
                                Files.copy(path, tar);
                                tar.closeArchiveEntry();
                            } catch (IOException e) {
                                throw new IllegalStateException("Failed to tar " + path, e);
                            }
                        });
            }
            tar.finish();
        }
        return bytes.toByteArray();
    }

    public static void extractGzipTar(InputStream gzipTar, Path destDir) throws IOException {
        try (InputStream in = new java.util.zip.GZIPInputStream(gzipTar);
             TarArchiveOutputStream ignored = null) {
            org.apache.commons.compress.archivers.tar.TarArchiveInputStream tar =
                    new org.apache.commons.compress.archivers.tar.TarArchiveInputStream(in);
            TarArchiveEntry entry;
            while ((entry = tar.getNextEntry()) != null) {
                Path outPath = destDir.resolve(entry.getName()).normalize();
                if (!outPath.startsWith(destDir)) {
                    throw new IOException("Tar entry escapes destination: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(outPath);
                } else {
                    Files.createDirectories(outPath.getParent());
                    try (OutputStream out = Files.newOutputStream(outPath)) {
                        tar.transferTo(out);
                    }
                }
            }
        }
    }
}
