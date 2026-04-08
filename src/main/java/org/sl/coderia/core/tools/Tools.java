package org.sl.coderia.core.tools;

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

    @Tool(value = "Write content to a file at the given path")
    @ToolPolicy(requires = {Permission.FILE_WRITE}, risk = RiskLevel.SAFE)
    public String writeFile(String path, String content) {
        return run("writeFile", () -> writeOps(path, content));
    }

    @Tool(value = "Read content from a file at the given path")
    @ToolPolicy(requires = {Permission.READ_ONLY}, risk = RiskLevel.SAFE, timeoutMs = 2_000)
    public String readFile(String path) {

        return run("readFile", () -> readOps(path));
    }

    @Tool(value = "Edit a file replacing oldContent with newContent")
    @ToolPolicy(requires = {Permission.FILE_WRITE}, risk = RiskLevel.SAFE)
    public String editFile(String path, String oldContent, String newContent) {
        return run("editFile", () -> editOpts(path, oldContent, newContent));
    }

    @Tool(value = "Execute a shell command and return output")
    @ToolPolicy(requires = {Permission.SHELL_EXEC}, risk = RiskLevel.SAFE, timeoutMs = 15_000)
    public String execCommand(String command) {
        return run("execCommand", () -> execOps(command));
    }

    @Tool(value = "Read or update memory.md — action = read|write|append")
    @ToolPolicy(requires = {Permission.FILE_WRITE}, risk = RiskLevel.SAFE)
    public String memory(String action, String content) {
        return run("memory", () -> {
            Path path = Path.of("memory.md");
            return switch (action) {
                case "read"   -> Files.readString(path);
                case "write"  -> { Files.writeString(path, content);         yield "ok"; }
                case "append" -> { Files.writeString(path, content, APPEND); yield "ok"; }
                default       -> "unknown action: " + action;
            };
        });
    }

    @Tool(value = "Print a message to the terminal")
    @ToolPolicy(risk = RiskLevel.SAFE)
    public String print(String message) {
        return run("print", () -> {
            TerminalIO.getInstance().printLine("[AGENT] " + message);
            return "ok";
        });
    }


    private String run(String toolName, Callable<String> action) {
        Method method = resolveMethod(toolName);
        long startedAt = System.nanoTime();
        TerminalIO io = TerminalIO.getInstance();
        io.printLine("[TOOL][" + toolName + "] start");
        try {
            String result = sandbox.run(method, action);
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000L;
            io.printLine("[TOOL][" + toolName + "] success durationMs=" + durationMs + " result=" + preview(result, 180));
            return result;

        } catch (Exception e) {
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000L;
            io.printLine("[TOOL][" + toolName + "] failure durationMs=" + durationMs + " error=" + preview(e.getMessage(), 180));
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
