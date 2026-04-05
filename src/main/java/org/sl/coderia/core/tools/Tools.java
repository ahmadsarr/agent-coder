package org.sl.coderia.core.tools;

import dev.langchain4j.agent.tool.Tool;
import org.sl.coderia.core.sandbox.ToolPolicy;
import org.sl.coderia.core.sandbox.ToolSandbox;
import org.sl.coderia.core.sandbox.ToolSandbox.Permission;
import org.sl.coderia.core.utils.TerminalIO;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.nio.file.StandardOpenOption.APPEND;

public class Tools implements FileOperations {

    private static final List<String> ALLOWED_COMMANDS = List.of(
            "ls", "cat", "echo", "pwd", "git",
            "find", "grep", "head", "tail", "wc",
            "mkdir", "cp", "mv", "rm",
            "mvn", "gradle", "java", "javac",
            "curl", "wget",
            "ps", "env", "which", "whoami"
    );
    private static final Path WORKSPACE_ROOT = Paths.get("").toAbsolutePath().normalize();
    private final ToolSandbox sandbox;

    public Tools(ToolSandbox sandbox) {
        this.sandbox = sandbox;
    }

    @Tool("Write content to a file at the given path")
    @ToolPolicy(requires = {Permission.FILE_WRITE})
    public String writeFile(String path, String content) {
        return exec("writeFile", m -> this.sandbox.safeRun(m, () -> writeOps(path, content)));
    }

    @Tool("Read content from a file at the given path")
    @ToolPolicy(requires = {Permission.READ_ONLY})
    public String readFile(String path) {
        return exec("writeFile", m -> this.sandbox.safeRun(m, () -> readOps(path)));
    }

    @Tool("Edit a file at the given path, replacing oldContent with newContent")
    public String editFile(String path, String oldContent, String newContent) {

        return exec("writeFile", m -> this.sandbox.safeRun(m, () -> editOpts(path,oldContent,newContent)));
    }


    private String exec(String name, Function<Method, String> action) {
        return Arrays.stream(this.getClass().getMethods())
                .filter(m -> m.getName().equals(name))
                .findFirst()
                .map(action)
                .orElse("writeFile not found");
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
