package org.sl.coderia.core.tools;

import dev.langchain4j.agent.tool.Tool;
import org.sl.coderia.core.sandbox.ToolPolicy;
import org.sl.coderia.core.sandbox.ToolSandbox;
import org.sl.coderia.core.sandbox.ToolSandbox.Permission;
import org.sl.coderia.core.utils.TerminalIO;

import java.lang.reflect.Method;
import java.nio.file.*;
import java.util.*;
import java.util.function.Function;

import static java.nio.file.StandardOpenOption.APPEND;

public class Tools implements FileOperations, CommandOps {


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
        return exec("readFile", m -> this.sandbox.safeRun(m, () -> readOps(path)));
    }

    @Tool("Edit a file at the given path, replacing oldContent with newContent")
    public String editFile(String path, String oldContent, String newContent) {
        return exec("editFile", m -> this.sandbox.safeRun(m, () -> editOpts(path, oldContent, newContent)));
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
            return """
                    {
                    "success":false,
                     "error":"%s"
                    }
                    """.formatted(e.getMessage());
        }
    }

    @Tool("Execute a shell command and return output")
    public String ExecCommand(String command) {
        return execOps(command);
    }
    @Tool("Print a message to the terminal")
    public String print(String message) {
        try {
            TerminalIO.getInstance().printLine("[AGENT] " + message);
            return """
                    {"success":true}
                    """;
        } catch (Exception e) {
            return """
                    {
                    "success":false,
                     "error":"%s"
                    }
                    """.formatted(e.getMessage());
        }
    }


}
