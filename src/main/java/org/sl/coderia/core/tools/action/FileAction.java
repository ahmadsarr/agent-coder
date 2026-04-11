package org.sl.coderia.core.tools.action;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.sl.coderia.core.tools.ToolPolicy;
import org.sl.coderia.core.tools.ToolSandbox;
import org.sl.coderia.core.utils.TerminalIO;
import org.sl.coderia.core.utils.Utils;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.stream.Collectors;

public class FileAction {
    Path WORKSPACE_ROOT = Paths.get("").toAbsolutePath().normalize();
    int MAX_CHARS = 12_000; // ~3k tokens



    @Tool("Write text content to a file. Creates the file if it does not exist, overwrites it if it does.")
    @ToolPolicy(requires = {ToolSandbox.Permission.FILE_WRITE}, risk = ToolSandbox.RiskLevel.SAFE)
    public String writeFile(
            @P("Absolute or relative file path") String path,
            @P("Full text content to write") String content) {
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

            return """
                    {"success":true, "message":"File written successfully to %s"}
                    """.formatted(resolved);

        } catch (Exception e) {
            return errorJson(e.getMessage());
        }
    }

    @Tool("Read and return the full text content of a file.")
    @ToolPolicy(requires = {ToolSandbox.Permission.READ_ONLY}, risk = ToolSandbox.RiskLevel.SAFE, timeoutMs = 2_000)
    public String readFile(
            @P("Absolute or relative file path to read") String path) {
        TerminalIO.getInstance().printLine("Reading file:"+path);
        return readOps(path, true);
    }

    @Tool("Read the raw and complete content of a file without any compression or truncation. Use only when you need the exact content for editing or when readFile returned [TRUNCATED].")
    @ToolPolicy(requires = {ToolSandbox.Permission.READ_ONLY}, risk = ToolSandbox.RiskLevel.SAFE, timeoutMs = 5_000)
    public String readFileRaw(
            @P("Absolute or relative file path to read") String path) {
        TerminalIO.getInstance().printLine("Reading whole file:"+path);

        return readOps(path, false);
    }


    private String readOps(String path, boolean compress) {
        try {
            Path resolved = resolveWithinWorkspace(path);
            String content = Files.readString(resolved);
            return compress ? compress(content) : content;
        } catch (Exception e) {
            return errorJson(e.getMessage());
        }
    }

    @Tool("Replace an exact block of text in a file. Fails if oldContent is not found exactly — match must be character for character.")
    @ToolPolicy(requires = {ToolSandbox.Permission.FILE_WRITE}, risk = ToolSandbox.RiskLevel.SAFE)
    public String editFile(
            @P("Absolute or relative file path to edit") String path,
            @P("Exact text to find and replace — must match character for character, including indentation and newlines") String oldContent,
            @P("Replacement text") String newContent) {
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

    private Path resolveWithinWorkspace(String inputPath)  {
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
