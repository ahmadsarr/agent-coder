package org.sl.coderia.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sl.coderia.core.tools.ToolSandbox;
import org.sl.coderia.core.tools.Tools;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ToolsTest {
    private Tools tools;

    @BeforeEach
    void setUp() {
        tools = new Tools(new ToolSandbox(EnumSet.allOf(ToolSandbox.Permission.class)));
    }

    @Test
    void writeFileRejectsBlankPath() {
        String result = tools.writeFile("   ", "hello");
        assertTrue(result.contains("\"success\":false"));
        assertTrue(result.contains("path cannot be blank"));
    }

    @Test
    void writeFileRejectsNullContent() {
        String result = tools.writeFile("target/test-tmp/null-content.txt", null);
        assertTrue(result.contains("\"success\":false"));
        assertTrue(result.contains("content cannot be null"));
    }

    @Test
    void writeFileRejectsSymlinkTraversal() throws Exception {
        Path base = Path.of("target/test-tmp/symlink-check");
        Files.createDirectories(base);
        Path outside = Files.createTempDirectory("coderia-outside-");
        Path link = base.resolve("outside-link");
        Files.deleteIfExists(link);
        try {
            Files.createSymbolicLink(link, outside);
        } catch (UnsupportedOperationException e) {
            assumeTrue(false, "Symbolic links are not supported on this filesystem");
        }

        String result = tools.writeFile(link.resolve("escape.txt").toString(), "secret");

        assertTrue(result.contains("\"success\":false"));
        assertTrue(result.contains("Path traverses symlink"));
        assertTrue(Files.notExists(outside.resolve("escape.txt")));
    }
}
