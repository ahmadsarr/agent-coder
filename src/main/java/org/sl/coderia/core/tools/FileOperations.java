package org.sl.coderia.core.tools;

import org.sl.coderia.core.utils.TerminalIO;
import org.sl.coderia.core.utils.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.stream.Collectors;

public interface FileOperations {
    Path WORKSPACE_ROOT = Paths.get("").toAbsolutePath().normalize();
    int MAX_CHARS = 12_000; // ~3k tokens


    default String writeOps(String path, String content) {
        TerminalIO.getInstance().printLine("Writing file:"+path);
        if (content == null) {
            return errorJson("content cannot be null");
        }
        try {
            Path resolved = resolveWithinWorkspace(path);

            if (resolved.getParent() != null) {
                Files.createDirectories(resolved.getParent());
            }
            Files.writeString(resolved, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            TerminalIO.getInstance().printLine("[TOOL] writeFile: Written " + content.length() + " chars to " + resolved);
            return """
                    {"success":true, "message":"File written successfully to %s"}
                    """.formatted(resolved);

        } catch (Exception e) {
            return errorJson(e.getMessage());
        }
    }

    default String readOps(String path) {
        TerminalIO.getInstance().printLine("Reading file:"+path);
        try {
            Path resolved = resolveWithinWorkspace(path);

            String content = Files.readString(resolved);
            TerminalIO.getInstance().printLine("[TOOL] readFile: Read " + content.length() + " chars from " + resolved);
            return compress(content);

        } catch (Exception e) {
            return errorJson(e.getMessage());
        }
    }
    private String compress(String content) {
        content = content.lines()
                .map(String::stripTrailing)
                .filter(l -> !l.isBlank())
                .collect(Collectors.joining("\n"));

        if (content.length() <= MAX_CHARS) return content;

        String head = content.substring(0, MAX_CHARS / 2);
        String tail = content.substring(content.length() - MAX_CHARS / 2);
        return head + "\n\n... [TRUNCATED] ...\n\n" + tail;
    }

    default String editOpts(String path, String oldContent, String newContent) {
        TerminalIO.getInstance().printLine("Editing file:"+path);
        try {
            Path resolved = resolveWithinWorkspace(path);
            String content = Files.readString(resolved);
            if (oldContent == null || oldContent.isEmpty()) {
                return """
                        {"success":false, "error":"oldContent cannot be empty"}
                        """;
            }
            int count = countOccurrences(content, oldContent);
            if (count == 0) {
                return """
                        {"success":false, "error":"No matching content found"}
                        """;
            }
            if (count > 1) {
                return """
                        {"success":false, "error":"Found %d occurrences. Be more specific."}
                        """.formatted(count);
            }
            int idx = content.indexOf(oldContent);
            String updated = content.substring(0, idx) + newContent + content.substring(idx + oldContent.length());
            Files.writeString(resolved, updated, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return """
                    {"success":true, "message":"File edited successfully to %s"}
                    """.formatted(resolved);
        } catch (Exception e) {
            return errorJson(e.getMessage());
        }
    }

    private int countOccurrences(String content, String needle) {
        int count = 0;
        int from = 0;
        while (true) {
            int idx = content.indexOf(needle, from);
            if (idx < 0) {
                return count;
            }
            count++;
            from = idx + needle.length();
        }
    }

    private Path resolveWithinWorkspace(String inputPath) throws IOException {
        if (inputPath == null || inputPath.isBlank()) {
            throw new IllegalArgumentException("path cannot be blank");
        }
        Path candidate = Paths.get(inputPath.trim());
        Path resolved = candidate.isAbsolute() ? candidate.normalize() : WORKSPACE_ROOT.resolve(candidate).normalize();
        if (!resolved.startsWith(WORKSPACE_ROOT)) {
            throw new IllegalArgumentException("Path escapes workspace: " + inputPath);
        }
        Path relative = WORKSPACE_ROOT.relativize(resolved);
        Path cursor = WORKSPACE_ROOT;
        for (Path segment : relative) {
            cursor = cursor.resolve(segment);
            if (Files.exists(cursor, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(cursor)) {
                throw new IllegalArgumentException("Path traverses symlink: " + inputPath);
            }
        }
        return resolved;
    }

    private String errorJson(String message) {
        return """
                {"success":false, "error":"%s"}
                """.formatted(Utils.escapeJson(message));
    }
}
