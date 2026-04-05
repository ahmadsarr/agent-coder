package org.sl.coderia.core;

import dev.langchain4j.agent.tool.Tool;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

import static java.nio.file.StandardOpenOption.APPEND;

public class Tools {

    private static final List<String> ALLOWED_COMMANDS = List.of("ls", "cat", "echo", "pwd");
    private static final Path WORKSPACE_ROOT = Paths.get("").toAbsolutePath().normalize();

    // ---------------- FILE TOOLS ----------------
    @Tool("Write content to a file at the given path")
    public String writeFile(String path, String content) {
        try {
            Path resolved = resolveWithinWorkspace(path);

            if (resolved.getParent() != null) {
                Files.createDirectories(resolved.getParent());
            }
            Files.writeString(resolved, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            TerminalIO.getInstance().printLine("[TOOL] writeFile: Written " + content.length() + " chars to " + resolved);
            return "{\"success\":true, \"message\":\"File written successfully to " + resolved + "\"}";

        } catch (Exception e) {
            return "{\"success\":false, \"error\":\"" + e.getMessage() + "\"}";
        }
    }

    @Tool("Read content from a file at the given path")
    public String readFile(String path) {
        try {
            Path resolved = resolveWithinWorkspace(path);

            String content = Files.readString(resolved);
            TerminalIO.getInstance().printLine("[TOOL] readFile: Read " + content.length() + " chars from " + resolved);
            return content;

        } catch (Exception e) {
            return "{\"success\":false, \"error\":\"" + e.getMessage() + "\"}";
        }
    }

    @Tool("Read or update memory.md scratchpad action =read|write|append")
    public String memory(String action, String content) {
        // action = "read" | "write" | "append"
        try {
            String memory = "memory.md";
            final Path path = Path.of(memory);
            return switch (action) {
                case "read" -> Files.readString(path);
                case "write" -> {
                    Files.writeString(path, content);
                    yield "ok";
                }
                case "append" -> {
                    Files.writeString(path, content, APPEND);
                    yield "ok";
                }
                default -> "unknown action";
            };
        } catch (Exception e) {
            return "{\"success\":false, \"error\":\"" + e.getMessage() + "\"}";
        }
    }

    @Tool("Edit a file at the given path, replacing oldContent with newContent")
    public String editFile(String path, String oldContent, String newContent) {
        try {
            Path resolved = resolveWithinWorkspace(path);
            String content = Files.readString(resolved);
            if (oldContent == null || oldContent.isEmpty()) {
                return "{\"success\":false, \"error\":\"oldContent cannot be empty\"}";
            }
            int count = countOccurrences(content, oldContent);
            if (count == 0) {
                return "{\"success\":false, \"error\":\"No matching content found\"}";
            }
            if (count > 1) {
                return "{\"success\":false, \"error\":\"Found %d occurrences. Be more specific.\"}".formatted(count);
            }
            int idx = content.indexOf(oldContent);
            String updated = content.substring(0, idx) + newContent + content.substring(idx + oldContent.length());
            Files.writeString(resolved, updated, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return "{\"success\":true, \"message\":\"File edited successfully to " + resolved + "\"}";
        } catch (IOException e) {
            return "{\"success\":false, \"error\":\"" + e.getMessage() + "\"}";
        }
    }

    // ---------------- COMMAND TOOL ----------------
    @Tool("Execute a shell command and return output")
    public String command(String command) {
        try {
            List<String> args = tokenizeCommand(command);
            if (args.isEmpty()) {
                return "{\"success\":false, \"error\":\"Empty command\"}";
            }

            String cmdName = args.get(0);
            if (!ALLOWED_COMMANDS.contains(cmdName)) {
                return "{\"success\":false, \"error\":\"Command not allowed: " + cmdName + "\"}";
            }

            TerminalIO.getInstance().printLine("[TOOL] Executing command: " + command);

            ProcessBuilder pb = new ProcessBuilder(args);
            pb.directory(WORKSPACE_ROOT.toFile());
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                TerminalIO.getInstance().printLine("[COMMAND] " + line); // stream to terminal
            }

            int exitCode = process.waitFor();
            return "{\"success\":true, \"exitCode\":" + exitCode + ", \"output\":\"" + escapeJson(output.toString()) + "\"}";

        } catch (Exception e) {
            return "{\"success\":false, \"error\":\"" + e.getMessage() + "\"}";
        }
    }

    @Tool("Print a message to the terminal")
    public String print(String message) {
        try {
            TerminalIO.getInstance().printLine("[AGENT] " + message);
            return "{\"success\":true}";
        } catch (Exception e) {
            return "{\"success\":false, \"error\":\"" + e.getMessage() + "\"}";
        }
    }

    // ---------------- HELPERS ----------------
    private String escapeJson(String s) {
        return s.replace("\"", "\\\"").replace("\n", "\\n");
    }

    private Path resolveWithinWorkspace(String inputPath) {
        Path candidate = Paths.get(inputPath);
        Path resolved = candidate.isAbsolute() ? candidate.normalize() : WORKSPACE_ROOT.resolve(candidate).normalize();
        if (!resolved.startsWith(WORKSPACE_ROOT)) {
            throw new IllegalArgumentException("Path escapes workspace: " + inputPath);
        }
        return resolved;
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

    private List<String> tokenizeCommand(String raw) {
        List<String> parts = new ArrayList<>();
        if (raw == null) {
            return parts;
        }
        String command = raw.trim();
        if (command.isEmpty()) {
            return parts;
        }

        StringBuilder current = new StringBuilder();
        boolean inSingle = false;
        boolean inDouble = false;

        for (int i = 0; i < command.length(); i++) {
            char c = command.charAt(i);

            if (c == '\'' && !inDouble) {
                inSingle = !inSingle;
                continue;
            }
            if (c == '"' && !inSingle) {
                inDouble = !inDouble;
                continue;
            }

            if (!inSingle && !inDouble && Character.isWhitespace(c)) {
                if (current.length() > 0) {
                    parts.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }

            if (c == '\\' && i + 1 < command.length() && (inSingle || inDouble)) {
                i++;
                current.append(command.charAt(i));
                continue;
            }

            current.append(c);
        }

        if (inSingle || inDouble) {
            throw new IllegalArgumentException("Unclosed quote in command");
        }
        if (current.length() > 0) {
            parts.add(current.toString());
        }
        return parts;
    }
}
