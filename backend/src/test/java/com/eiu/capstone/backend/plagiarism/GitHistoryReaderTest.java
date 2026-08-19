package com.eiu.capstone.backend.plagiarism;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GitHistoryReaderTest {

    @Test
    void parseReflog_keepsCommitOrder() {
        String log = """
                0000000000000000000000000000000000000000 aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa Alice <alice@eiu.edu.vn> 1700000000 +0700\tcommit: first
                aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb Alice <alice@eiu.edu.vn> 1700000100 +0700\tcommit: second
                """;
        List<GitCommitRecord> commits = GitHistoryReader.parseReflog(log);
        assertEquals(2, commits.size());
        assertEquals("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", commits.get(0).hash());
        assertEquals("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb", commits.get(1).hash());
        assertEquals("alice@eiu.edu.vn", commits.get(0).authorEmail());
    }

    @Test
    void read_usesConfigAndReflog(@TempDir Path gitRoot) throws IOException {
        Path gitDir = gitRoot.resolve(".git");
        Files.createDirectories(gitDir.resolve("logs"));
        Files.writeString(gitDir.resolve("config"), """
                [user]
                \tname = Alice
                \temail = alice@eiu.edu.vn
                """);
        Files.writeString(gitDir.resolve("logs").resolve("HEAD"),
                "0000000000000000000000000000000000000000 cccccccccccccccccccccccccccccccccccccccc Alice <alice@eiu.edu.vn> 1700000000 +0700\tcommit: only\n");

        GitHistory history = GitHistoryReader.read(gitDir);
        assertEquals("Alice", history.userName());
        assertEquals("alice@eiu.edu.vn", history.userEmail());
        assertTrue(history.hasCommits());
        assertEquals("cccccccccccccccccccccccccccccccccccccccc", history.commits().get(0).hash());
    }
}
