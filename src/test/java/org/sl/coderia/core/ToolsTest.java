package org.sl.coderia.core;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolsTest {

    private final Tools tools = new Tools();

    @Test
    void commandRejectsInjectionLikeInput() {
        String result = tools.command("ls; pwd");
        assertTrue(result.contains("\"success\":false"));
        assertTrue(result.contains("Command not allowed"));
    }

    @Test
    void writeFileRejectsPathOutsideWorkspace() {
        String result = tools.writeFile("../escape.txt", "hello");
        assertTrue(result.contains("\"success\":false"));
        assertTrue(result.contains("Path escapes workspace"));
    }

    @Test
    void editFileFailsWhenMultipleLiteralMatchesFound() throws Exception {
        Path file = Path.of("target/test-tmp/multi.txt");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "abc abc");

        String result = tools.editFile(file.toString(), "abc", "x");

        assertTrue(result.contains("\"success\":false"));
        assertTrue(result.contains("Found 2 occurrences"));
        assertEquals("abc abc", Files.readString(file));
    }

    @Test
    void editFileReplacesSingleLiteralMatch() throws Exception {
        Path file = Path.of("target/test-tmp/single.txt");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "before needle after");

        String result = tools.editFile(file.toString(), "needle", "VALUE");

        assertTrue(result.contains("\"success\":true"));
        assertEquals("before VALUE after", Files.readString(file));
    }
}
