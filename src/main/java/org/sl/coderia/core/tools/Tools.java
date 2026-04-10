package org.sl.coderia.core.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.sl.coderia.core.tools.ToolSandbox.Permission;
import org.sl.coderia.core.tools.ToolSandbox.RiskLevel;
import org.sl.coderia.core.utils.TerminalIO;

import java.lang.reflect.Method;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Callable;

import static java.nio.file.StandardOpenOption.APPEND;

public class Tools implements FileOperations, CommandOps {

    private final ToolSandbox sandbox;

    public Tools(ToolSandbox sandbox) {
        this.sandbox = sandbox;
    }

    @Tool("Write text content to a file. Creates the file if it does not exist, overwrites it if it does.")
    @ToolPolicy(requires = {Permission.FILE_WRITE}, risk = RiskLevel.SAFE)
    public String writeFile(
            @P("Absolute or relative file path") String path,
            @P("Full text content to write") String content) {
        return run("writeFile", () -> writeOps(path, content));
    }

    @Tool("Read and return the full text content of a file.")
    @ToolPolicy(requires = {Permission.READ_ONLY}, risk = RiskLevel.SAFE, timeoutMs = 2_000)
    public String readFile(
            @P("Absolute or relative file path to read") String path) {
        return run("readFile", () -> readOps(path, true));
    }

    @Tool("Read the raw and complete content of a file without any compression or truncation. Use only when you need the exact content for editing or when readFile returned [TRUNCATED].")
    @ToolPolicy(requires = {Permission.READ_ONLY}, risk = RiskLevel.SAFE, timeoutMs = 5_000)
    public String readFileRaw(
            @P("Absolute or relative file path to read") String path) {
        return run("readFileRaw", () -> readOps(path, false));
    }


    @Tool("Replace an exact block of text in a file. Fails if oldContent is not found exactly — match must be character for character.")
    @ToolPolicy(requires = {Permission.FILE_WRITE}, risk = RiskLevel.SAFE)
    public String editFile(
            @P("Absolute or relative file path to edit") String path,
            @P("Exact text to find and replace — must match character for character, including indentation and newlines") String oldContent,
            @P("Replacement text") String newContent) {
        return run("editFile", () -> editOpts(path, oldContent, newContent));
    }

    @Tool("Execute a shell command in the working directory and return stdout+stderr. e.g. 'mvn test', 'python -m pytest', 'ls -la'.")
    @ToolPolicy(requires = {Permission.SHELL_EXEC}, risk = RiskLevel.SAFE, timeoutMs = 15_000)
    public String execCommand(
            @P("Shell command to run, e.g. 'mvn test' or 'python -m pytest'") String command) {
        return run("execCommand", () -> execOps(command));
    }

    @Tool("Manage memory.md — persist or retrieve the current plan and state across steps.")
    @ToolPolicy(requires = {Permission.FILE_WRITE}, risk = RiskLevel.SAFE)
    public String memory(
            @P("Action: 'read' to retrieve full content, 'write' to overwrite, 'append' to add at the end") String action,
            @P("Content to write or append. Pass empty string when action is 'read'") String content) {
        return run("memory", () -> {
            Path path = Path.of("memory.md");
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
                default -> "unknown action: " + action;
            };
        });
    }

    @Tool("Print a message to the terminal as an agent status update.")
    @ToolPolicy(risk = RiskLevel.SAFE)
    public String print(
            @P("Message to display") String message) {
        return run("print", () -> {
            TerminalIO.getInstance().printLine("[AGENT] " + message);
            return "ok";
        });
    }

    @Tool("Ask the human a question and wait for their response. Use when you need clarification, a decision, or missing information before proceeding.")
    @ToolPolicy(risk = RiskLevel.SAFE, timeoutMs = 1_000_000)
    public String askHuman(
            @P("Clear and concise question to ask the user") String question) {
        return run("askHuman", () -> TerminalIO.getInstance().read("YOU:" + question));
    }


    private String run(String toolName, Callable<String> action) {
        Method method = resolveMethod(toolName);
        try {
            String result = sandbox.run(method, action);
            return result;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Method resolveMethod(String name) {
        return Arrays.stream(this.getClass().getMethods())
                .filter(m -> m.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Method not found: " + name));
    }

    private String preview(String value, int maxLen) {
        if (value == null) {
            return "<null>";
        }
        String compact = value.replace("\n", "\\n").replace("\r", "\\r").trim();
        if (compact.length() <= maxLen) {
            return compact;
        }
        return compact.substring(0, maxLen) + "...";
    }

}
